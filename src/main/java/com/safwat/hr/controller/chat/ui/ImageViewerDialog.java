package com.safwat.hr.controller.chat.ui;

import com.safwat.hr.controller.chat.cach.AttachmentCache;
import com.safwat.hr.controller.chat.dto.ChatDTOs;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.*;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

/**
 * ✅ جديد: عارض صور داخلي (Lightbox) زي واتساب — بدل ما الضغط على صورة
 * يحاول (ويفشل) يفتحها في المتصفح الخارجي، دلوقتي بيفتح نافذة داخل التطبيق
 * فيها تكبير للصورة، تنقل بين كل صور المحادثة بالسهام، وزرار تحميل.
 */
public class ImageViewerDialog {

    private final Stage stage;
    private final List<ChatDTOs.ChatAttachmentDTO> images;
    private final StackPane imageHost = new StackPane();
    private final Label counterLabel = new Label();
    private final Label nameLabel = new Label();
    private int index;

    public ImageViewerDialog(Window owner, List<ChatDTOs.ChatAttachmentDTO> images, int startIndex) {
        this.images = images;
        this.index = Math.max(0, Math.min(startIndex, images.size() - 1));

        stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initStyle(StageStyle.UNDECORATED);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: rgba(15,15,20,0.96);");

        root.setTop(buildTopBar());
        root.setCenter(imageHost);
        root.setBottom(buildBottomBar());

        imageHost.setPadding(new Insets(10, 60, 10, 60));
        imageHost.setOnMouseClicked(e -> {
            if (e.getTarget() == imageHost) close();
        });

        Scene scene = new Scene(root, 900, 650);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) close();
            else if (e.getCode() == KeyCode.LEFT) showPrev();
            else if (e.getCode() == KeyCode.RIGHT) showNext();
        });

        stage.setScene(scene);
        loadCurrent();
    }

    private HBox buildTopBar() {
        HBox bar = new HBox(10);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(14, 16, 10, 16));

        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");
        counterLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.6); -fx-font-size: 12px;");

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Button btnDownload = iconButton("⬇");
        btnDownload.setOnAction(e -> downloadCurrent());

        Button btnClose = iconButton("✕");
        btnClose.setOnAction(e -> close());

        bar.getChildren().addAll(nameLabel, counterLabel, spacer, btnDownload, btnClose);
        return bar;
    }

    private HBox buildBottomBar() {
        HBox bar = new HBox();
        bar.setAlignment(Pos.CENTER);
        bar.setPadding(new Insets(6, 16, 16, 16));

        if (images.size() <= 1) return bar;

        Button prev = iconButton("‹ السابق");
        prev.setOnAction(e -> showPrev());

        Button next = iconButton("التالي ›");
        next.setOnAction(e -> showNext());

        bar.setSpacing(16);
        bar.getChildren().addAll(prev, next);
        return bar;
    }

    private Button iconButton(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; " +
                "-fx-background-radius: 16px; -fx-padding: 6 14 6 14; -fx-cursor: hand; -fx-font-size: 13px;");
        b.setOnMouseEntered(e -> b.setStyle(b.getStyle() + "-fx-background-color: rgba(255,255,255,0.2);"));
        return b;
    }

    private void showPrev() {
        if (index > 0) {
            index--;
            loadCurrent();
        }
    }

    private void showNext() {
        if (index < images.size() - 1) {
            index++;
            loadCurrent();
        }
    }

    private void loadCurrent() {
        ChatDTOs.ChatAttachmentDTO att = images.get(index);
        nameLabel.setText(att.getFileName());
        counterLabel.setText((index + 1) + " / " + images.size());

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(48, 48);
        imageHost.getChildren().setAll(spinner);

        AttachmentCache.ensureDownloaded(att).thenAccept(path -> Platform.runLater(() -> {
            if (path == null) {
                Label err = new Label("❌ تعذر تحميل الصورة");
                err.setStyle("-fx-text-fill: white;");
                imageHost.getChildren().setAll(err);
                return;
            }
            try {
                Image image = new Image(path.toUri().toString());
                ImageView view = new ImageView(image);
                view.setPreserveRatio(true);
                view.setFitWidth(Math.min(image.getWidth(), 780));
                view.setFitHeight(Math.min(image.getHeight(), 520));
                imageHost.getChildren().setAll(view);
            } catch (Exception ex) {
                Label err = new Label("❌ تعذر عرض الصورة");
                err.setStyle("-fx-text-fill: white;");
                imageHost.getChildren().setAll(err);
            }
        }));
    }

    private void downloadCurrent() {
        ChatDTOs.ChatAttachmentDTO att = images.get(index);
        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName(att.getFileName());
        File target = chooser.showSaveDialog(stage);
        if (target == null) return;

        AttachmentCache.ensureDownloaded(att).thenAccept(path -> {
            if (path != null) {
                try {
                    Files.copy(path, target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception ignored) {
                }
            }
        });
    }

    private void close() {
        stage.close();
    }

    public void show() {
        stage.showAndWait();
    }
}