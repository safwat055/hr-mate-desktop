package com.safwat.hr.ui.util;

import com.safwat.hr.ui.controls.SAFNotification;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPrintable;
import org.apache.pdfbox.printing.Scaling;

import javax.print.DocFlavor;
import javax.print.PrintService;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Chromaticity;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.Sides;
import javax.print.attribute.standard.SheetCollate;
import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * حوار طباعة PDF مع الخيارات: الطابعة، المقاس، الوجه/الوجهين، الاتجاه،
 * نطاق الصفحات، عدد النسخ، التجميع، واللون. كل الإعدادات محفوظة في PrintSettings.
 */
public final class PrintDialogService {

    private PrintDialogService() {
    }

    // ══════════════════════════ الحوار ══════════════════════════

    /**
     * يفتح حوار الطباعة. لازم يتنادى على الـ FX thread.
     */
    public static void openDialog(Path pdfPath) {
        PrintService[] services = PrinterJob.lookupPrintServices();
        if (services.length == 0) {
            SAFNotification.error("لا توجد طابعات متاحة");
            return;
        }

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("طباعة");

        // ── الطابعة ──
        ChoiceBox<PrintService> printerBox = new ChoiceBox<>();
        printerBox.getItems().addAll(services);
        printerBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(PrintService s) {
                return s == null ? "" : s.getName();
            }

            @Override
            public PrintService fromString(String s) {
                return null;
            }
        });
        printerBox.getSelectionModel().select(resolveInitialPrinter(services));

        // ── المقاس ──
        ChoiceBox<PaperOption> paperBox = new ChoiceBox<>();
        paperBox.getItems().addAll(PaperOption.values());
        paperBox.getSelectionModel().select(PaperOption.fromName(PrintSettings.getPaper()));

        // ── الوجه / الوجهين ──
        ChoiceBox<SidesOption> sidesBox = new ChoiceBox<>();
        sidesBox.getItems().addAll(SidesOption.values());
        sidesBox.getSelectionModel().select(SidesOption.fromName(PrintSettings.getSides()));

        // ── الاتجاه ──
        ChoiceBox<OrientationOption> orientationBox = new ChoiceBox<>();
        orientationBox.getItems().addAll(OrientationOption.values());
        orientationBox.getSelectionModel().select(OrientationOption.fromName(PrintSettings.getOrientation()));

        // ── نطاق الصفحات ──
        TextField pageRangeField = new TextField(PrintSettings.getPageRange());
        pageRangeField.setPromptText("مثال: 1-3,5 أو اتركه فاضي للكل");

        // ── عدد النسخ ──
        Spinner<Integer> copiesSpinner = new Spinner<>(1, 99, PrintSettings.getCopies());
        copiesSpinner.setEditable(true);

        // ── التجميع ──
        CheckBox collateBox = new CheckBox("تجميع النسخ (1-2-3 ثم 1-2-3)");
        collateBox.setSelected(PrintSettings.isCollate());

        // ── اللون ──
        ChoiceBox<ColorOption> colorBox = new ChoiceBox<>();
        colorBox.getItems().addAll(ColorOption.values());
        colorBox.getSelectionModel().select(ColorOption.fromName(PrintSettings.getColor()));

        Button printBtn = new Button("طباعة");
        Button cancelBtn = new Button("إلغاء");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));
        grid.addRow(0, new Label("الطابعة:"), printerBox);
        grid.addRow(1, new Label("مقاس الورق:"), paperBox);
        grid.addRow(2, new Label("وضع الطباعة:"), sidesBox);
        grid.addRow(3, new Label("الاتجاه:"), orientationBox);
        grid.addRow(4, new Label("الصفحات:"), pageRangeField);
        grid.addRow(5, new Label("عدد النسخ:"), copiesSpinner);
        grid.add(collateBox, 1, 6);
        grid.addRow(7, new Label("اللون:"), colorBox);
        grid.addRow(8, printBtn, cancelBtn);

        printBtn.setOnAction(e -> {
            copiesSpinner.increment(0); // يثبّت الرقم اللي اتكتب يدوي

            PrintService selected = printerBox.getValue();
            PaperOption paper = paperBox.getValue();
            SidesOption sides = sidesBox.getValue();
            OrientationOption orientation = orientationBox.getValue();
            String pageRange = pageRangeField.getText().trim();
            int copies = copiesSpinner.getValue();
            boolean collate = collateBox.isSelected();
            ColorOption color = colorBox.getValue();

            // نتأكد من صحة نطاق الصفحات قبل الحفظ والطباعة
            List<Integer> pages;
            try {
                pages = parsePageRange(pageRange);
            } catch (IllegalArgumentException ex) {
                SAFNotification.error(ex.getMessage());
                return;
            }

            PrintSettings.save(selected.getName(), paper.name(), sides.name(), copies,
                    orientation.name(), pageRange.isEmpty() ? "ALL" : pageRange, collate, color.name());

            stage.close();
            printAsync(pdfPath, selected, copies, paper, sides, orientation, pages, collate, color);
        });
        cancelBtn.setOnAction(e -> stage.close());

        stage.setScene(new Scene(grid));
        stage.showAndWait();
    }

    private static PrintService resolveInitialPrinter(PrintService[] services) {
        String saved = PrintSettings.getPrinter();
        for (PrintService s : services) {
            if (s.getName().equals(saved)) return s;
        }
        PrintService systemDefault = PrinterJob.getPrinterJob().getPrintService();
        if (systemDefault != null) {
            for (PrintService s : services) {
                if (s.getName().equals(systemDefault.getName())) return s;
            }
        }
        return services[0];
    }

    // ══════════════════════════ نطاق الصفحات ══════════════════════════

    /**
     * يحوّل نص مثل "1-3,5" لقائمة أرقام صفحات (1-based).
     * نص فاضي = null (يعني كل الصفحات).
     */
    static List<Integer> parsePageRange(String text) {
        if (text == null || text.isBlank()) return null;

        List<Integer> pages = new ArrayList<>();
        for (String part : text.split(",")) {
            part = part.trim();
            if (part.isEmpty()) continue;
            try {
                if (part.contains("-")) {
                    String[] bounds = part.split("-", 2);
                    int from = Integer.parseInt(bounds[0].trim());
                    int to = Integer.parseInt(bounds[1].trim());
                    if (from < 1 || to < from) throw new IllegalArgumentException();
                    for (int i = from; i <= to; i++) pages.add(i);
                } else {
                    int p = Integer.parseInt(part);
                    if (p < 1) throw new IllegalArgumentException();
                    pages.add(p);
                }
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("نطاق الصفحات غير صحيح: " + part);
            }
        }
        return pages.isEmpty() ? null : pages;
    }

    // ══════════════════════════ الطباعة ══════════════════════════

    public static void printAsync(Path pdfPath, PrintService service, int copies,
                                  PaperOption paper, SidesOption sides, OrientationOption orientation,
                                  List<Integer> pages, boolean collate, ColorOption color) {
        Thread t = new Thread(() -> {
            try {
                print(pdfPath, service, copies, paper, sides, orientation, pages, collate, color);
                Platform.runLater(() -> SAFNotification.success("تمت الطباعة"));
            } catch (Exception ex) {
                Platform.runLater(() -> SAFNotification.error("فشلت الطباعة: " + ex.getMessage()));
            }
        }, "pdf-print");
        t.setDaemon(true);
        t.start();
    }

    private static void print(Path pdfPath, PrintService service, int copies,
                              PaperOption paper, SidesOption sides, OrientationOption orientation,
                              List<Integer> pages, boolean collate, ColorOption color)
            throws IOException, PrinterException {

        // نتأكد إن الطابعة تدعم الوجهين واللون قبل الإرسال
        if (sides.attribute != null
                && !service.isAttributeValueSupported(sides.attribute, DocFlavor.SERVICE_FORMATTED.PRINTABLE, null)) {
            throw new PrinterException("الطابعة \"" + service.getName() + "\" لا تدعم: " + sides.label);
        }
        if (!service.isAttributeValueSupported(color.attribute, DocFlavor.SERVICE_FORMATTED.PRINTABLE, null)) {
            throw new PrinterException("الطابعة \"" + service.getName() + "\" لا تدعم: " + color.label);
        }

        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPrintService(service);

            PageFormat pageFormat = createPageFormat(job, paper, orientation);

            Book book = new Book();
            PDFPrintable printable = new PDFPrintable(document, Scaling.SCALE_TO_FIT);

            if (pages == null) {
                book.append(printable, pageFormat, document.getNumberOfPages());
            } else {
                // نطبع الصفحات المطلوبة بس، بالترتيب، صفحة صفحة
                for (int pageNo : pages) {
                    if (pageNo > document.getNumberOfPages()) {
                        throw new PrinterException("الصفحة " + pageNo + " أكبر من عدد صفحات الملف");
                    }
                    book.append(new PageRangePrintable(printable, pageNo - 1), pageFormat);
                }
            }
            job.setPageable(book);

            PrintRequestAttributeSet attrs = new HashPrintRequestAttributeSet();
            attrs.add(new Copies(copies));
            if (sides.attribute != null) attrs.add(sides.attribute);
            attrs.add(color.attribute);
            attrs.add(collate ? SheetCollate.COLLATED : SheetCollate.UNCOLLATED);

            job.print(attrs);
        }
    }

    private static PageFormat createPageFormat(PrinterJob job, PaperOption paper, OrientationOption orientation) {
        PageFormat pageFormat = job.defaultPage();

        double width = paper.widthPt;
        double height = paper.heightPt;
        double margin = 18; // حوالي 6 مم

        Paper p = new Paper();
        p.setSize(width, height);
        p.setImageableArea(margin, margin, width - 2 * margin, height - 2 * margin);
        pageFormat.setPaper(p);
        pageFormat.setOrientation(orientation.value);
        return pageFormat;
    }

    /**
     * يطبع صفحة واحدة محددة من الـ Printable الأصلي.
     */
    private static final class PageRangePrintable implements java.awt.print.Printable {
        private final java.awt.print.Printable delegate;
        private final int pageIndex;

        PageRangePrintable(java.awt.print.Printable delegate, int pageIndex) {
            this.delegate = delegate;
            this.pageIndex = pageIndex;
        }

        @Override
        public int print(java.awt.Graphics graphics, PageFormat format, int page) throws PrinterException {
            if (page != 0) return NO_SUCH_PAGE;
            return delegate.print(graphics, format, pageIndex);
        }
    }

    // ══════════════════════════ الخيارات ══════════════════════════

    public enum PaperOption {
        A4("A4", 595, 842),
        A3("A3", 842, 1191);

        final String label;
        final double widthPt;
        final double heightPt;

        PaperOption(String label, double widthPt, double heightPt) {
            this.label = label;
            this.widthPt = widthPt;
            this.heightPt = heightPt;
        }

        public static PaperOption fromName(String name) {
            try { return valueOf(name); } catch (Exception e) { return A4; }
        }

        @Override
        public String toString() { return label; }
    }

    public enum SidesOption {
        ONE_SIDED("وجه واحد", Sides.ONE_SIDED),
        TWO_SIDED_LONG("وجهين - قلب على الحرف الطويل", Sides.TWO_SIDED_LONG_EDGE),
        TWO_SIDED_SHORT("وجهين - قلب على الحرف القصير", Sides.TWO_SIDED_SHORT_EDGE);

        final String label;
        final Sides attribute;

        SidesOption(String label, Sides attribute) {
            this.label = label;
            this.attribute = attribute;
        }

        public static SidesOption fromName(String name) {
            try { return valueOf(name); } catch (Exception e) { return ONE_SIDED; }
        }

        @Override
        public String toString() { return label; }
    }

    public enum OrientationOption {
        PORTRAIT("طولي", PageFormat.PORTRAIT),
        LANDSCAPE("عرضي", PageFormat.LANDSCAPE);

        final String label;
        final int value;

        OrientationOption(String label, int value) {
            this.label = label;
            this.value = value;
        }

        public static OrientationOption fromName(String name) {
            try { return valueOf(name); } catch (Exception e) { return PORTRAIT; }
        }

        @Override
        public String toString() { return label; }
    }

    public enum ColorOption {
        COLOR("ملون", Chromaticity.COLOR),
        MONOCHROME("أبيض وأسود", Chromaticity.MONOCHROME);

        final String label;
        final Chromaticity attribute;

        ColorOption(String label, Chromaticity attribute) {
            this.label = label;
            this.attribute = attribute;
        }

        public static ColorOption fromName(String name) {
            try { return valueOf(name); } catch (Exception e) { return COLOR; }
        }

        @Override
        public String toString() { return label; }
    }
}