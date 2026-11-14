package com.safwat.hr.ui.print;

import com.safwat.hr.ui.controls.SAFNotification;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;

/**
 * نافذة معاينة: بتعرض كل ورقة كما ستخرج بالإعدادات المختارة.
 */
public final class PrintPreviewWindow {

    private static final double PREVIEW_DPI = 96.0;

    private PrintPreviewWindow() {
    }

    public static void show(Window owner, Path pdfPath, PrintOptions o) {
        final PDDocument doc;
        try {
            doc = Loader.loadPDF(pdfPath.toFile());
        } catch (IOException ex) {
            SAFNotification.error("تعذّر فتح الملف: " + ex.getMessage());
            return;
        }

        final PdfSheetPrintable sheets;
        try {
            sheets = PdfRenderer.sheets(doc, o);
        } catch (IllegalArgumentException ex) {
            closeQuietly(doc);
            SAFNotification.error(ex.getMessage());
            return;
        }

        final int total = sheets.getSheetCount();
        if (total == 0) {
            closeQuietly(doc);
            SAFNotification.error("لا توجد صفحات للمعاينة");
            return;
        }

        ImageView view = new ImageView();
        view.setPreserveRatio(true);
        view.setFitWidth(500);
        view.setFitHeight(680);

        Label pageLabel = new Label();
        Button prev = new Button("السابقة");
        Button next = new Button("التالية");

        final int[] index = {0};
        Runnable refresh = () -> {
            try {
                BufferedImage img = PdfRenderer.renderSheet(o, sheets, index[0], PREVIEW_DPI);
                view.setImage(toFxImage(img));
            } catch (Exception ex) {
                SAFNotification.error("فشلت المعاينة: " + ex.getMessage());
            }
            pageLabel.setText((index[0] + 1) + " / " + total);
            prev.setDisable(index[0] == 0);
            next.setDisable(index[0] >= total - 1);
        };

        prev.setOnAction(e -> {
            index[0]--;
            refresh.run();
        });
        next.setOnAction(e -> {
            index[0]++;
            refresh.run();
        });

        HBox nav = new HBox(12, prev, pageLabel, next);
        nav.setAlignment(Pos.CENTER);
        nav.setPadding(new Insets(10));

        BorderPane root = new BorderPane(view);
        root.setBottom(nav);
        root.setPadding(new Insets(10));

        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("معاينة الطباعة");
        stage.setOnHidden(e -> closeQuietly(doc));

        refresh.run();
        stage.setScene(new Scene(root));
        stage.show();
    }

    /**
     * يحوّل BufferedImage لـ JavaFX Image عن طريق PNG في الذاكرة،
     * عشان مش نحتاج module javafx.swing.
     */
    private static Image toFxImage(BufferedImage img) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", bos);
        return new Image(new ByteArrayInputStream(bos.toByteArray()));
    }

    private static void closeQuietly(PDDocument doc) {
        try {
            doc.close();
        } catch (IOException ignored) {
        }
    }
}