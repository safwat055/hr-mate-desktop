package com.safwat.hr.notification.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/**
 * =====================================================
 * MessageThreadListCell — خلية الرسالة في القائمة اليمين
 * =====================================================
 */
public class MessageThreadListCell extends ListCell<MessageThread> {

    @Override
    protected void updateItem(MessageThread item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
            setText(null);
            setStyle("-fx-background-color:transparent;");
            return;
        }

        // نقطة مقروء/غير مقروء
        Circle dot = new Circle(4);
        dot.setFill(item.isRead() ? Color.TRANSPARENT : Color.web("#0F6E56"));
        dot.setStroke(item.isRead() ? Color.web("#CCCCCC") : Color.TRANSPARENT);
        dot.setStrokeWidth(item.isRead() ? 1.5 : 0);

        // اسم المرسل
        Label senderLbl = new Label(
                item.getSenderName() != null ? item.getSenderName() : "مجهول");
        senderLbl.setStyle(
                "-fx-font-size:13px;-fx-font-weight:" + (item.isRead() ? "400" : "700") +
                        ";-fx-text-fill:#1A1A1A;"
        );

        // الموضوع
        String subject = item.getSubject();
        if (subject == null || subject.isBlank()) subject = "(بدون موضوع)";
        Label subjectLbl = new Label(subject);
        subjectLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#333333;");
        subjectLbl.setMaxWidth(200);

        // معاينة
        String preview = item.getPreview();
        if (preview == null) preview = "";
        if (preview.length() > 40) preview = preview.substring(0, 38) + "...";
        Label previewLbl = new Label(preview);
        previewLbl.setStyle("-fx-font-size:11px;-fx-text-fill:#888888;");
        previewLbl.setMaxWidth(200);

        VBox texts = new VBox(3, senderLbl, subjectLbl, previewLbl);
        HBox.setHgrow(texts, Priority.ALWAYS);

        HBox root = new HBox(10, dot, texts);
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new Insets(10, 14, 10, 14));

        // Background: غير مقروء = أخضر فاتح
        String bg = item.isRead() ? "#FFFFFF" : "#F0FAF7";
        root.setStyle("-fx-background-color:" + bg + ";");

        setGraphic(root);

        // Hover effect
        setOnMouseEntered(e -> {
            if (!isSelected()) {
                root.setStyle("-fx-background-color:#F5F5F5;");
            }
        });
        setOnMouseExited(e -> {
            if (!isSelected()) {
                root.setStyle("-fx-background-color:" + bg + ";");
            }
        });

        // Selected style
        if (isSelected()) {
            root.setStyle("-fx-background-color:#E6F5F1;-fx-border-color:#0F6E56;-fx-border-width:0 3 0 0;");
        }
    }
}