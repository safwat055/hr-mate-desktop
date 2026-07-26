package com.safwat.hr.chat;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * Dialog اختيار نوع المحادثة الجديدة:
 * - محادثة خاصة   (PRIVATE)
 * - مجموعة         (GROUP)
 * - رسالة لقسم    (BROADCAST)
 * <p>
 * يُستدعى من زر "+" في الـ sidebar.
 * النتيجة: "PRIVATE" | "GROUP" | "BROADCAST"
 */
public class NewConversationTypeDialog extends Dialog<String> {

    public NewConversationTypeDialog(Window owner) {
        initOwner(owner);
        setTitle("محادثة جديدة");
        setHeaderText("اختر نوع المحادثة");

        getDialogPane().getButtonTypes().add(ButtonType.CANCEL);

        VBox content = new VBox(10);
        content.setPadding(new Insets(16));
        content.setAlignment(Pos.CENTER);

        Button btnPrivate = buildOption("محادثة خاصة", "ابدأ محادثة مع موظف", "PRIVATE");
        Button btnGroup = buildOption("مجموعة", "أضف أكثر من موظف في محادثة واحدة", "GROUP");
        Button btnBroadcast = buildOption("رسالة لقسم", "أرسل لكل موظفي قسم أو الجميع", "BROADCAST");

        content.getChildren().addAll(btnPrivate, btnGroup, btnBroadcast);
        getDialogPane().setContent(content);

        setResultConverter(btn -> null); // الأزرار الداخلية بتعمل setResult
    }

    private Button buildOption(String title, String subtitle, String type) {
        VBox box = new VBox(3);
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6B7280;");
        box.getChildren().addAll(titleLabel, subtitleLabel);

        Button btn = new Button();
        btn.setGraphic(box);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setPadding(new Insets(10, 16, 10, 16));
        btn.setStyle("""
                -fx-background-color: #F5F7FA;
                -fx-background-radius: 8px;
                -fx-border-color: #E5E7EB;
                -fx-border-radius: 8px;
                -fx-cursor: hand;
                """);
        btn.setOnMouseEntered(e -> btn.setStyle("""
                -fx-background-color: #EBF3FC;
                -fx-background-radius: 8px;
                -fx-border-color: #185FA5;
                -fx-border-radius: 8px;
                -fx-cursor: hand;
                """));
        btn.setOnMouseExited(e -> btn.setStyle("""
                -fx-background-color: #F5F7FA;
                -fx-background-radius: 8px;
                -fx-border-color: #E5E7EB;
                -fx-border-radius: 8px;
                -fx-cursor: hand;
                """));
        btn.setOnAction(e -> {
            setResult(type);
            close();
        });
        return btn;
    }
}