package com.safwat.hr.ui.print;

import com.safwat.hr.ui.controls.SAFNotification;
import com.safwat.hr.ui.print.PrintChoices.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import javax.print.PrintService;
import java.awt.print.PrinterJob;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

/**
 * حوار الطباعة: واجهة فقط. التنفيذ في PdfPrinter، والمعاينة في PrintPreviewWindow.
 */
public final class PrintDialogService {

    private PrintDialogService() {
    }

    /**
     * يفتح حوار الطباعة. لازم يتنادى على الـ FX thread.
     */
    public static void openDialog(Path pdfPath) {
        PrintService[] services = PrinterJob.lookupPrintServices();
        boolean hasPrinters = services.length > 0;

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

        ChoiceBox<PaperOption> paperBox = new ChoiceBox<>();
        paperBox.getItems().addAll(PaperOption.values());
        paperBox.getSelectionModel().select(PaperOption.fromName(PrintSettings.getPaper()));

        ChoiceBox<SidesOption> sidesBox = new ChoiceBox<>();
        sidesBox.getItems().addAll(SidesOption.values());
        sidesBox.getSelectionModel().select(SidesOption.fromName(PrintSettings.getSides()));

        ChoiceBox<OrientationOption> orientationBox = new ChoiceBox<>();
        orientationBox.getItems().addAll(OrientationOption.values());
        orientationBox.getSelectionModel().select(OrientationOption.fromName(PrintSettings.getOrientation()));

        ChoiceBox<PerSheetOption> perSheetBox = new ChoiceBox<>();
        perSheetBox.getItems().addAll(PerSheetOption.values());
        perSheetBox.getSelectionModel().select(PerSheetOption.fromName(PrintSettings.getPerSheet()));

        ChoiceBox<MarginOption> marginBox = new ChoiceBox<>();
        marginBox.getItems().addAll(MarginOption.values());
        marginBox.getSelectionModel().select(MarginOption.fromName(PrintSettings.getMargin()));

        String savedRange = PrintSettings.getPageRange();
        TextField pageRangeField = new TextField(
                "ALL".equalsIgnoreCase(savedRange) ? "" : savedRange);
        pageRangeField.setPromptText("مثال: 1-3,5 أو اتركه فاضي للكل");

        Spinner<Integer> copiesSpinner = new Spinner<>(1, 99, PrintSettings.getCopies());
        copiesSpinner.setEditable(true);

        CheckBox collateBox = new CheckBox("تجميع النسخ (1-2-3 ثم 1-2-3)");
        collateBox.setSelected(PrintSettings.isCollate());

        ChoiceBox<ColorOption> colorBox = new ChoiceBox<>();
        colorBox.getItems().addAll(ColorOption.values());
        colorBox.getSelectionModel().select(ColorOption.fromName(PrintSettings.getColor()));

        CheckBox saveAsPdfBox = new CheckBox("حفظ كملف PDF بدل الطباعة");
        saveAsPdfBox.setSelected(!hasPrinters || PrintSettings.isSaveAsPdf());
        saveAsPdfBox.setDisable(!hasPrinters);

        Button printBtn = new Button("طباعة");
        Button previewBtn = new Button("معاينة");
        Button cancelBtn = new Button("إلغاء");

        // الخيارات الخاصة بالطابعة بتتعطل في وضع الحفظ أو لو مفيش طابعات
        List<Control> printControls = List.of(printerBox, sidesBox, copiesSpinner, collateBox, colorBox);
        Runnable applyMode = () -> {
            boolean pdfMode = saveAsPdfBox.isSelected();
            printControls.forEach(c -> c.setDisable(pdfMode || !hasPrinters));
            printBtn.setText(pdfMode ? "حفظ" : "طباعة");
        };
        saveAsPdfBox.selectedProperty().addListener((obs, was, is) -> applyMode.run());
        applyMode.run();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));

        int r = 0;
        if (!hasPrinters) {
            grid.add(new Label("لا توجد طابعات متصلة، سيتم حفظ الملف كـ PDF"), 0, r++, 2, 1);
        }
        grid.addRow(r++, new Label("الطابعة:"), printerBox);
        grid.addRow(r++, new Label("مقاس الورق:"), paperBox);
        grid.addRow(r++, new Label("وضع الطباعة:"), sidesBox);
        grid.addRow(r++, new Label("الاتجاه:"), orientationBox);
        grid.addRow(r++, new Label("صفحات في الورقة:"), perSheetBox);
        grid.addRow(r++, new Label("الهامش:"), marginBox);
        grid.addRow(r++, new Label("الصفحات:"), pageRangeField);
        grid.addRow(r++, new Label("عدد النسخ:"), copiesSpinner);
        grid.add(collateBox, 1, r++);
        grid.addRow(r++, new Label("اللون:"), colorBox);
        grid.add(saveAsPdfBox, 1, r++);
        grid.addRow(r, printBtn, previewBtn, cancelBtn);

        // ── تجميع الخيارات من الواجهة (null لو فيه خطأ في نطاق الصفحات) ──
        Supplier<PrintOptions> collect = () -> {
            copiesSpinner.increment(0); // يثبّت الرقم اللي اتكتب يدوياً

            PrintOptions o = new PrintOptions();
            o.service = printerBox.getValue();
            o.paper = paperBox.getValue();
            o.sides = sidesBox.getValue();
            o.orientation = orientationBox.getValue();
            o.perSheet = perSheetBox.getValue();
            o.margin = marginBox.getValue();
            o.copies = copiesSpinner.getValue();
            o.collate = collateBox.isSelected();
            o.color = colorBox.getValue();
            o.saveAsPdf = saveAsPdfBox.isSelected();
            try {
                o.pages = PrintRange.parse(pageRangeField.getText());
            } catch (IllegalArgumentException ex) {
                SAFNotification.error(ex.getMessage());
                return null;
            }
            return o;
        };

        previewBtn.setOnAction(e -> {
            PrintOptions o = collect.get();
            if (o != null) PrintPreviewWindow.show(stage, pdfPath, o);
        });

        printBtn.setOnAction(e -> {
            PrintOptions o = collect.get();
            if (o == null) return;

            File target = null;
            if (o.saveAsPdf) {
                target = chooseFile(stage, pdfPath);
                if (target == null) return; // المستخدم لغى، الحوار يفضل مفتوح
            }

            PrintSettings.save(
                    o.service == null ? "" : o.service.getName(),
                    o.paper.name(),
                    o.sides.name(),
                    o.copies,
                    o.orientation.name(),
                    pageRangeField.getText().trim(),
                    o.collate,
                    o.color.name(),
                    o.perSheet.name(),
                    o.margin.name(),
                    o.saveAsPdf
            );

            stage.close();

            if (o.saveAsPdf) {
                File out = target;
                runAsync(() -> PdfPrinter.export(pdfPath, out, o),
                        "تم حفظ الملف: " + out.getName(), "فشل الحفظ: ");
            } else {
                runAsync(() -> PdfPrinter.print(pdfPath, o),
                        "تمت الطباعة", "فشلت الطباعة: ");
            }
        });

        cancelBtn.setOnAction(e -> stage.close());

        stage.setScene(new Scene(grid));
        stage.showAndWait();
    }

    // ══════════════════════════ Helpers ══════════════════════════

    private static PrintService resolveInitialPrinter(PrintService[] services) {
        if (services.length == 0) return null;

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

    private static File chooseFile(Stage owner, Path pdfPath) {
        FileChooser fc = new FileChooser();
        fc.setTitle("حفظ كملف PDF");
        fc.setInitialFileName(baseName(pdfPath) + "_print.pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));

        File f = fc.showSaveDialog(owner);
        if (f == null) return null;
        return f.getName().toLowerCase().endsWith(".pdf") ? f : new File(f.getPath() + ".pdf");
    }

    private static String baseName(Path p) {
        String n = p.getFileName().toString();
        int dot = n.lastIndexOf('.');
        return dot > 0 ? n.substring(0, dot) : n;
    }

    @FunctionalInterface
    private interface Task {
        void run() throws Exception;
    }

    private static void runAsync(Task task, String successMsg, String failPrefix) {
        Thread t = new Thread(() -> {
            try {
                task.run();
                Platform.runLater(() -> SAFNotification.success(successMsg));
            } catch (Exception ex) {
                Platform.runLater(() -> SAFNotification.error(failPrefix + ex.getMessage()));
            }
        }, "pdf-job");
        t.setDaemon(true);
        t.start();
    }
}