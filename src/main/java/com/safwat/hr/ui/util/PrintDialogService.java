package com.safwat.hr.ui.util;

import com.safwat.hr.ui.controls.SAFNotification;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.Sides;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;

/**
 * طباعة ملف PDF على طابعة عادية، مع اختيار الطابعة وعدد النسخ ومقاس الورق.
 */
public final class PrintDialogService {

    private PrintDialogService() {
    }

    /**
     * يفتح حوار الطباعة. لازم يتنادى على الـ FX thread.
     */
    public static void openDialog(Path pdfPath) {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(
                DocFlavor.INPUT_STREAM.PDF, null);

        if (services.length == 0) {
            SAFNotification.error("لا توجد طابعات تدعم طباعة ملفات PDF");
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
        printerBox.getSelectionModel().selectFirst();

        // ── المقاس ──
        ChoiceBox<PaperOption> paperBox = new ChoiceBox<>();
        paperBox.getItems().addAll(PaperOption.values());
        paperBox.getSelectionModel().select(PaperOption.A4);

        // ── عدد النسخ ──
        Spinner<Integer> copiesSpinner = new Spinner<>(1, 99, 1);
        copiesSpinner.setEditable(true);

        Button printBtn = new Button("طباعة");
        Button cancelBtn = new Button("إلغاء");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));
        grid.addRow(0, new Label("الطابعة:"), printerBox);
        grid.addRow(1, new Label("مقاس الورق:"), paperBox);
        grid.addRow(2, new Label("عدد النسخ:"), copiesSpinner);
        grid.addRow(3, printBtn, cancelBtn);

        printBtn.setOnAction(e -> {
            PrintService selected = printerBox.getValue();
            PaperOption paper = paperBox.getValue();
            int copies = copiesSpinner.getValue();
            stage.close();
            printAsync(pdfPath, selected, copies, paper);
        });
        cancelBtn.setOnAction(e -> stage.close());

        stage.setScene(new Scene(grid));
        stage.showAndWait();
    }

    /**
     * يطبع في thread منفصل عشان الواجهة ما تهنّجش.
     */
    public static void printAsync(Path pdfPath, PrintService service, int copies, PaperOption paper) {
        Thread t = new Thread(() -> {
            try {
                print(pdfPath, service, copies, paper);
                Platform.runLater(() -> SAFNotification.success("تمت الطباعة"));
            } catch (Exception ex) {
                Platform.runLater(() -> SAFNotification.error("فشلت الطباعة: " + ex.getMessage()));
            }
        }, "pdf-print");
        t.setDaemon(true);
        t.start();
    }

    private static void print(Path pdfPath, PrintService service, int copies, PaperOption paper)
            throws IOException, PrintException {

        if (!service.isDocFlavorSupported(DocFlavor.INPUT_STREAM.PDF)) {
            throw new PrintException("الطابعة لا تدعم PDF مباشرة");
        }

        PrintRequestAttributeSet attrs = new HashPrintRequestAttributeSet();
        attrs.add(new Copies(copies));
        attrs.add(Sides.ONE_SIDED);

        // ── المقاس: نتأكد إن الطابعة بتدعمه قبل ما نبعت ──
        Media media = paper.media;
        if (service.isAttributeValueSupported(media, DocFlavor.INPUT_STREAM.PDF, attrs)) {
            attrs.add(media);
        } else {
            throw new PrintException("الطابعة \"" + service.getName()
                    + "\" لا تدعم مقاس " + paper.label);
        }

        DocPrintJob job = service.createPrintJob();
        try (FileInputStream in = new FileInputStream(pdfPath.toFile())) {
            SimpleDoc doc = new SimpleDoc(in, DocFlavor.INPUT_STREAM.PDF, null);
            job.print(doc, attrs);
        }
    }

    /**
     * المقاسات المتاحة في الحوار.
     */
    public enum PaperOption {
        A4("A4", MediaSizeName.ISO_A4),
        A3("A3", MediaSizeName.ISO_A3);

        final String label;
        final Media media;

        PaperOption(String label, Media media) {
            this.label = label;
            this.media = media;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}