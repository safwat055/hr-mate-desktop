package com.safwat.hr.chat;

import com.safwat.hr.notification.util.FileOpener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

/**
 * فقاعة رسالة في واجهة الشات.
 * <p>
 * رسائلي  → يمين (HBox.alignment = CENTER_RIGHT)
 * رسائل غيري → يسار مع صورة رمزية
 * <p>
 * التصميم:
 * <p>
 * رسالة واردة:
 * [AV] ┌─────────────────┐
 * │  نص الرسالة     │
 * │  📎 اسم ملف ↓  │
 * └─────────────────┘
 * 11:30 AM
 * <p>
 * رسالة صادرة:
 * ┌─────────────────┐
 * │  نص الرسالة    │
 * └─────────────────┘
 * 11:30 AM ✓
 */
public class MessageBubble extends HBox {

    private static final double MAX_BUBBLE_WIDTH = 420;

    public MessageBubble(ChatDTOs.ChatMessageDTO msg) {
        super(8);

        boolean mine = msg.isMine();

        if (mine) {
            buildOutgoing(msg);
        } else {
            buildIncoming(msg);
        }

        // padding بين الفقاعات
        setMargin(this, new Insets(2, 0, 2, 0));
    }

    // ═════════════════════════════════════════════════════════════════
    //  رسالة صادرة (mine)
    // ═════════════════════════════════════════════════════════════════

    private void buildOutgoing(ChatDTOs.ChatMessageDTO msg) {
        setAlignment(Pos.CENTER_RIGHT);

        VBox bubble = new VBox(4);
        bubble.setMaxWidth(MAX_BUBBLE_WIDTH);
        bubble.getStyleClass().addAll("message-bubble", "bubble-outgoing");
        bubble.setPadding(new Insets(8, 12, 8, 12));

        // نص الرسالة
        if (msg.isDeleted()) {
            Label deleted = new Label("🚫 تم حذف هذه الرسالة");
            deleted.getStyleClass().add("msg-deleted");
            bubble.getChildren().add(deleted);
        } else {
            if (msg.getContent() != null && !msg.getContent().isBlank()) {
                Label content = new Label(msg.getContent());
                content.setWrapText(true);
                content.getStyleClass().add("msg-content");
                bubble.getChildren().add(content);
            }

            // مرفقات
            addAttachments(bubble, msg.getAttachments());
        }

        // وقت + علامة إرسال
        HBox timeRow = new HBox(4);
        timeRow.setAlignment(Pos.CENTER_RIGHT);
        Label timeLabel = new Label((msg.getTimeAgo() != null ? msg.getTimeAgo() : "") + " ✓");
        timeLabel.getStyleClass().add("msg-time");
        timeRow.getChildren().add(timeLabel);
        bubble.getChildren().add(timeRow);

        getChildren().add(bubble);
    }

    // ═════════════════════════════════════════════════════════════════
    //  رسالة واردة
    // ═════════════════════════════════════════════════════════════════

    private void buildIncoming(ChatDTOs.ChatMessageDTO msg) {
        setAlignment(Pos.CENTER_LEFT);

        // صورة رمزية المرسل
        StackPane avatar = new StackPane();
        avatar.setPrefSize(36, 36);
        avatar.setMinSize(36, 36);
        avatar.setMaxSize(36, 36);
        avatar.getStyleClass().add("msg-avatar");
        String color = msg.getSenderAvatarColor() != null
                ? msg.getSenderAvatarColor() : "#185FA5";
        avatar.setStyle("-fx-background-color: " + color +
                "; -fx-background-radius: 18;");

        Label initials = new Label(
                msg.getSenderAvatarInitials() != null ? msg.getSenderAvatarInitials() : "?"
        );
        initials.getStyleClass().add("msg-avatar-initials");
        avatar.getChildren().add(initials);
        avatar.setAlignment(Pos.BOTTOM_CENTER);

        // Bubble
        VBox bubble = new VBox(4);
        bubble.setMaxWidth(MAX_BUBBLE_WIDTH);
        bubble.getStyleClass().addAll("message-bubble", "bubble-incoming");
        bubble.setPadding(new Insets(8, 12, 8, 12));

        // اسم المرسل (في المجموعات)
        String senderName = msg.getSenderDisplayName() != null
                ? msg.getSenderDisplayName() : msg.getSenderUsername();
        Label senderLabel = new Label(senderName);
        senderLabel.getStyleClass().add("msg-sender-name");
        senderLabel.setStyle("-fx-text-fill: " + color + ";");
        bubble.getChildren().add(senderLabel);

        // نص الرسالة
        if (msg.isDeleted()) {
            Label deleted = new Label("🚫 تم حذف هذه الرسالة");
            deleted.getStyleClass().add("msg-deleted");
            bubble.getChildren().add(deleted);
        } else {
            if (msg.getContent() != null && !msg.getContent().isBlank()) {
                Label content = new Label(msg.getContent());
                content.setWrapText(true);
                content.getStyleClass().add("msg-content");
                bubble.getChildren().add(content);
            }

            addAttachments(bubble, msg.getAttachments());
        }

        // وقت
        Label timeLabel = new Label(msg.getTimeAgo() != null ? msg.getTimeAgo() : "");
        timeLabel.getStyleClass().add("msg-time");
        bubble.getChildren().add(timeLabel);

        getChildren().addAll(avatar, bubble);
    }

    // ═════════════════════════════════════════════════════════════════
    //  المرفقات
    // ═════════════════════════════════════════════════════════════════

    private void addAttachments(VBox bubble, List<ChatDTOs.ChatAttachmentDTO> attachments) {
        if (attachments == null || attachments.isEmpty()) return;

        for (ChatDTOs.ChatAttachmentDTO att : attachments) {
            HBox attRow = buildAttachmentRow(att);
            bubble.getChildren().add(attRow);
        }
    }

    private HBox buildAttachmentRow(ChatDTOs.ChatAttachmentDTO att) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("attachment-row");
        row.setPadding(new Insets(6, 8, 6, 8));

        // Icon حسب نوع الملف
        String icon = getFileIcon(att.getMimeType());
        Label iconLabel = new Label(icon);
        iconLabel.getStyleClass().add("attachment-icon");

        // اسم + حجم
        VBox info = new VBox(2);
        Label nameLabel = new Label(att.getFileName() != null ? att.getFileName() : "ملف");
        nameLabel.getStyleClass().add("attachment-name");

        Label sizeLabel = new Label(att.getFormattedSize() != null ? att.getFormattedSize() : "");
        sizeLabel.getStyleClass().add("attachment-size");

        info.getChildren().addAll(nameLabel, sizeLabel);
        HBox.setHgrow(info, Priority.ALWAYS);

        // زر تحميل
        Button downloadBtn = new Button("⬇");
        downloadBtn.getStyleClass().add("btn-download-attachment");
        downloadBtn.setOnAction(e -> downloadAttachment(att, downloadBtn));

        row.getChildren().addAll(iconLabel, info, downloadBtn);
        return row;
    }

    private void downloadAttachment(ChatDTOs.ChatAttachmentDTO att, Button btn) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("اختر مجلد الحفظ");
        File dir = chooser.showDialog(btn.getScene().getWindow());
        if (dir == null) return;

        Path targetPath = dir.toPath().resolve(att.getFileName());
        btn.setDisable(true);
        btn.setText("⏳");

        ChatApiService.downloadAttachment(att.getDownloadToken(), targetPath)
                .thenAccept(success -> javafx.application.Platform.runLater(() -> {
                    btn.setDisable(false);
                    if (success) {
                        btn.setText("✅");
                        // فتح الملف مباشرة بعد التحميل
                        FileOpener.open(targetPath.toString());
                    } else {
                        btn.setText("❌");
                    }
                }));
    }

    private String getFileIcon(String mimeType) {
        if (mimeType == null) return "📄";
        if (mimeType.startsWith("image/")) return "🖼";
        if (mimeType.startsWith("video/")) return "🎬";
        if (mimeType.startsWith("audio/")) return "🎵";
        if (mimeType.contains("pdf")) return "📕";
        if (mimeType.contains("word") || mimeType.contains("document")) return "📝";
        if (mimeType.contains("sheet") || mimeType.contains("excel")) return "📊";
        if (mimeType.contains("zip") || mimeType.contains("compressed")) return "🗜";
        return "📄";
    }
}
