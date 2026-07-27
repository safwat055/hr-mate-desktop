package com.safwat.hr.model.message.service;

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
 * =====================================================================
 * MessageThreadListCell
 * =====================================================================
 * خلية مخصصة لعرض الرسائل في قائمة المحادثات (الجانب الأيمن).
 * تعرض نقطة المقروءية، اسم المرسل، الموضوع، معاينة النص، والوقت.
 * تغير خلفيتها حسب حالة القراءة وتدعم تأثيرات التمرير والاختيار.
 */
public class MessageThreadListCell extends ListCell<MessageThread> {

    /**
     * تحديث محتوى الخلية عند تغير العنصر.
     * إذا كانت الخلية فارغة يتم مسح المحتوى.
     * إذا كانت تحتوي على عنصر يتم بناء الواجهة المناسبة.
     *
     * @param item  عنصر المحادثة
     * @param empty true إذا كانت الخلية فارغة
     */
    @Override
    protected void updateItem(MessageThread item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
            setText(null);
            setStyle("-fx-background-color:transparent;");
            return;
        }

        Circle dot = new Circle(4);
        dot.setFill(item.isRead() ? Color.TRANSPARENT : Color.web("#0F6E56"));
        dot.setStroke(item.isRead() ? Color.web("#CCCCCC") : Color.TRANSPARENT);
        dot.setStrokeWidth(item.isRead() ? 1.5 : 0);

        Label senderLbl = new Label(
                item.getSenderName() != null ? item.getSenderName() : "مجهول");
        senderLbl.setStyle(
                "-fx-font-size:13px;-fx-font-weight:" + (item.isRead() ? "400" : "700") +
                        ";-fx-text-fill:#1A1A1A;"
        );

        String subject = item.getSubject();
        if (subject == null || subject.isBlank()) subject = "(بدون موضوع)";
        Label subjectLbl = new Label(subject);
        subjectLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#333333;");
        subjectLbl.setMaxWidth(180);

        String preview = item.getPreview();
        if (preview == null) preview = "";
        if (preview.length() > 40) preview = preview.substring(0, 38) + "...";
        Label previewLbl = new Label(preview);
        previewLbl.setStyle("-fx-font-size:11px;-fx-text-fill:#888888;");
        previewLbl.setMaxWidth(180);

        VBox texts = new VBox(3, senderLbl, subjectLbl, previewLbl);
        HBox.setHgrow(texts, Priority.ALWAYS);

        Label timeLbl = new Label(item.getFormattedTime());
        timeLbl.setStyle("-fx-font-size:10px;-fx-text-fill:#AAAAAA;");
        timeLbl.setMinWidth(40);
        timeLbl.setAlignment(Pos.TOP_LEFT);

        HBox root = new HBox(10, dot, texts, timeLbl);
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new Insets(10, 14, 10, 14));

        String bg = item.isRead() ? "#FFFFFF" : "#F0FAF7";
        root.setStyle("-fx-background-color:" + bg + ";");

        setGraphic(root);

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

        if (isSelected()) {
            root.setStyle("-fx-background-color:#E6F5F1;-fx-border-color:#0F6E56;-fx-border-width:0 3 0 0;");
        }
    }
}