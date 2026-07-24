package com.safwat.hr.chat;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Cell خاصة بقائمة المحادثات.
 * <p>
 * التصميم:
 * ┌─────────────────────────────────────────┐
 * │ [AV]  اسم المحادثة         وقت   [3]  │
 * │       آخر رسالة...                      │
 * └─────────────────────────────────────────┘
 */
public class ConversationCell extends ListCell<ChatDTOs.ConversationSummaryDTO> {

    // ── Nodes (نُنشئها مرة وnُعيد استخدامها) ─────────────────────────
    private final HBox root = new HBox(10);
    private final StackPane avatarBox = new StackPane();
    private final Label avatarLabel = new Label();
    private final VBox textBox = new VBox(3);
    private final HBox topRow = new HBox();
    private final Label nameLabel = new Label();
    private final Label timeLabel = new Label();
    private final HBox bottomRow = new HBox();
    private final Label lastMsgLabel = new Label();
    private final Label unreadBadge = new Label();

    public ConversationCell() {
        buildLayout();
    }

    private void buildLayout() {
        // صورة رمزية
        avatarBox.setPrefSize(44, 44);
        avatarBox.setMinSize(44, 44);
        avatarBox.setMaxSize(44, 44);
        avatarBox.getStyleClass().add("conv-avatar");
        avatarLabel.getStyleClass().add("conv-avatar-initials");
        avatarBox.getChildren().add(avatarLabel);

        // اسم + وقت
        nameLabel.getStyleClass().add("conv-name");
        nameLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        timeLabel.getStyleClass().add("conv-time");

        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.getChildren().addAll(nameLabel, timeLabel);

        // آخر رسالة + badge
        lastMsgLabel.getStyleClass().add("conv-last-msg");
        lastMsgLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(lastMsgLabel, Priority.ALWAYS);

        unreadBadge.getStyleClass().add("unread-badge");
        unreadBadge.setVisible(false);
        unreadBadge.setManaged(false);

        bottomRow.setAlignment(Pos.CENTER_LEFT);
        bottomRow.getChildren().addAll(lastMsgLabel, unreadBadge);

        // Text box
        textBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textBox, Priority.ALWAYS);
        textBox.getChildren().addAll(topRow, bottomRow);

        // Root
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new Insets(10, 12, 10, 12));
        root.getStyleClass().add("conv-cell");
        root.getChildren().addAll(avatarBox, textBox);
    }

    @Override
    protected void updateItem(ChatDTOs.ConversationSummaryDTO conv, boolean empty) {
        super.updateItem(conv, empty);

        if (empty || conv == null) {
            setGraphic(null);
            return;
        }

        // صورة رمزية
        avatarLabel.setText(conv.getAvatarInitials() != null ? conv.getAvatarInitials() : "?");
        String color = conv.getAvatarColor() != null ? conv.getAvatarColor() : "#185FA5";
        avatarBox.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 22;");

        // اسم + وقت
        nameLabel.setText(conv.getName() != null ? conv.getName() : "محادثة");
        timeLabel.setText(conv.getTimeAgo() != null ? conv.getTimeAgo() : "");

        // آخر رسالة
        String preview = conv.getLastMessage() != null ? conv.getLastMessage() : "";
        lastMsgLabel.setText(preview);

        // Badge رسائل غير مقروءة
        long unread = conv.getUnreadCount();
        if (unread > 0) {
            unreadBadge.setText(unread > 99 ? "99+" : String.valueOf(unread));
            unreadBadge.setVisible(true);
            unreadBadge.setManaged(true);
            // Bold اسم المحادثة لو فيه رسائل جديدة
            nameLabel.setStyle("-fx-font-weight: bold;");
            lastMsgLabel.setStyle("-fx-font-weight: bold;");
        } else {
            unreadBadge.setVisible(false);
            unreadBadge.setManaged(false);
            nameLabel.setStyle("-fx-font-weight: normal;");
            lastMsgLabel.setStyle("-fx-font-weight: normal;");
        }

        // Icon حسب نوع المحادثة
        String typeIcon = switch (conv.getType() != null ? conv.getType() : "") {
            case "GROUP" -> "[G] ";
            case "BROADCAST" -> "[B] ";
            default -> "";
        };
        if (!typeIcon.isEmpty() && conv.getName() != null) {
            nameLabel.setText(typeIcon + conv.getName());
        }

        setGraphic(root);
    }
}
