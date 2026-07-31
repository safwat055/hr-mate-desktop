package com.safwat.hr.chat;

import com.safwat.hr.notification.util.FileOpener;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * فقاعة رسالة في واجهة الشات.
 */
public class MessageBubble extends HBox {

    private static final double MAX_BUBBLE_WIDTH = 420;
    private static final double MAX_IMAGE_WIDTH = 300;
    private static final double MAX_IMAGE_HEIGHT = 250;

    private ChatDTOs.ChatMessageDTO message;
    private Label statusLabel;
    private VBox bubble;
    private boolean mine;

    public MessageBubble(ChatDTOs.ChatMessageDTO msg) {
        super(8);
        this.message = msg;
        this.mine = msg.isMine();

        if (mine) {
            buildOutgoing(msg);
        } else {
            buildIncoming(msg);
        }

        setMargin(this, new Insets(2, 0, 2, 0));
    }

    public Long getMessageId() {
        return message != null ? message.getId() : null;
    }

    /**
     * ✅ جديد: تحديث الـ bubble بالـ DTO الجديد (edit/delete/status)
     */
    public void refreshMessage(ChatDTOs.ChatMessageDTO newMsg) {
        this.message = newMsg;
        getChildren().clear();
        if (bubble != null) {
            bubble.getChildren().clear();
        }
        statusLabel = null;

        if (mine) {
            buildOutgoing(newMsg);
        } else {
            buildIncoming(newMsg);
        }
    }

    public void updateStatus(ChatDTOs.MessageStatus status) {
        if (statusLabel == null || message == null || !mine) return;

        String statusText = switch (status) {
            case SENDING -> "⏳";
            case SENT -> "✓";
            case DELIVERED -> "✓✓";
            case READ -> "✓✓";
        };

        String color = switch (status) {
            case READ -> "-fx-text-fill: #34B7F1;";
            default -> "-fx-text-fill: #9CA3AF;";
        };

        statusLabel.setText(statusText);
        statusLabel.setStyle(color);
    }

    // ═════════════════════════════════════════════════════════════════
    //  رسالة صادرة (mine)
    // ═════════════════════════════════════════════════════════════════

    private void buildOutgoing(ChatDTOs.ChatMessageDTO msg) {
        setAlignment(Pos.CENTER_RIGHT);

        bubble = new VBox(4);
        bubble.setMaxWidth(MAX_BUBBLE_WIDTH);
        bubble.getStyleClass().addAll("message-bubble", "bubble-outgoing");
        bubble.setPadding(new Insets(8, 12, 8, 12));

        if (msg.isDeleted()) {
            Label deleted = new Label("[محذوف] تم حذف هذه الرسالة");
            deleted.getStyleClass().add("msg-deleted");
            bubble.getChildren().add(deleted);
        } else {
            if (msg.getReplyToId() != null) {
                bubble.getChildren().add(buildReplyQuoteBox(msg));
            }
            if (msg.getContent() != null && !msg.getContent().isBlank()) {
                Label content = new Label(msg.getContent());
                content.setWrapText(true);
                content.getStyleClass().add("msg-content");
                bubble.getChildren().add(content);
            }
            addAttachments(bubble, msg.getAttachments());
        }

        HBox timeRow = new HBox(4);
        timeRow.setAlignment(Pos.CENTER_RIGHT);

        if (msg.isEdited()) {
            Label editedLabel = new Label("(مُعدّلة)");
            editedLabel.getStyleClass().add("msg-edited-indicator");
            timeRow.getChildren().add(editedLabel);
        }

        Label timeLabel = new Label(msg.getTimeAgo() != null ? msg.getTimeAgo() : "");
        timeLabel.getStyleClass().add("msg-time");

        statusLabel = new Label();
        statusLabel.getStyleClass().add("msg-status");
        updateStatus(msg.getStatus() != null ? msg.getStatus() : ChatDTOs.MessageStatus.SENT);

        timeRow.getChildren().addAll(timeLabel, statusLabel);
        bubble.getChildren().add(timeRow);

        getChildren().add(bubble);
    }

    // ═════════════════════════════════════════════════════════════════
    //  رسالة واردة
    // ═════════════════════════════════════════════════════════════════

    private void buildIncoming(ChatDTOs.ChatMessageDTO msg) {
        setAlignment(Pos.CENTER_LEFT);

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

        bubble = new VBox(4);
        bubble.setMaxWidth(MAX_BUBBLE_WIDTH);
        bubble.getStyleClass().addAll("message-bubble", "bubble-incoming");
        bubble.setPadding(new Insets(8, 12, 8, 12));

        String senderName = msg.getSenderDisplayName() != null
                ? msg.getSenderDisplayName() : msg.getSenderUsername();
        Label senderLabel = new Label(senderName);
        senderLabel.getStyleClass().add("msg-sender-name");
        senderLabel.setStyle("-fx-text-fill: " + color + ";");
        bubble.getChildren().add(senderLabel);

        if (msg.isDeleted()) {
            Label deleted = new Label("[محذوف] تم حذف هذه الرسالة");
            deleted.getStyleClass().add("msg-deleted");
            bubble.getChildren().add(deleted);
        } else {
            if (msg.getReplyToId() != null) {
                bubble.getChildren().add(buildReplyQuoteBox(msg));
            }
            if (msg.getContent() != null && !msg.getContent().isBlank()) {
                Label content = new Label(msg.getContent());
                content.setWrapText(true);
                content.getStyleClass().add("msg-content");
                bubble.getChildren().add(content);
            }
            addAttachments(bubble, msg.getAttachments());
        }

        HBox timeRow = new HBox(4);
        timeRow.setAlignment(Pos.CENTER_LEFT);

        Label timeLabel = new Label(msg.getTimeAgo() != null ? msg.getTimeAgo() : "");
        timeLabel.getStyleClass().add("msg-time");
        timeRow.getChildren().add(timeLabel);

        if (msg.isEdited()) {
            Label editedLabel = new Label("(مُعدّلة)");
            editedLabel.getStyleClass().add("msg-edited-indicator");
            timeRow.getChildren().add(editedLabel);
        }

        bubble.getChildren().add(timeRow);

        getChildren().addAll(avatar, bubble);
    }

    // ═════════════════════════════════════════════════════════════════
    //  الرد على رسالة (Reply/Quote)
    // ═════════════════════════════════════════════════════════════════

    /**
     * ✅ جديد: صندوق صغير فوق الرسالة بيعرض معاينة الرسالة المردود عليها، زي واتساب.
     */
    private VBox buildReplyQuoteBox(ChatDTOs.ChatMessageDTO msg) {
        VBox box = new VBox(2);
        box.getStyleClass().add("reply-quote-box");
        box.setPadding(new Insets(5, 8, 5, 8));

        Label senderLabel = new Label(
                msg.getReplyToSenderName() != null ? msg.getReplyToSenderName() : "رسالة"
        );
        senderLabel.getStyleClass().add("reply-quote-sender");

        String preview = msg.isReplyToDeleted()
                ? "[رسالة محذوفة]"
                : (msg.getReplyToPreview() != null && !msg.getReplyToPreview().isBlank()
                ? msg.getReplyToPreview() : "📎 مرفق");
        Label previewLabel = new Label(preview);
        previewLabel.getStyleClass().add("reply-quote-text");
        previewLabel.setWrapText(true);

        box.getChildren().addAll(senderLabel, previewLabel);
        return box;
    }

    // ═════════════════════════════════════════════════════════════════
    //  المرفقات
    // ═════════════════════════════════════════════════════════════════

    private void addAttachments(VBox bubble, List<ChatDTOs.ChatAttachmentDTO> attachments) {
        if (attachments == null || attachments.isEmpty()) return;

        for (ChatDTOs.ChatAttachmentDTO att : attachments) {
            if (att.getMimeType() != null && att.getMimeType().startsWith("image/")) {
                bubble.getChildren().add(buildImagePreview(att));
            } else {
                bubble.getChildren().add(buildAttachmentRow(att));
            }
        }
    }

    /**
     * ✅ تم الإصلاح: الصور كانت مش بتظهر أبداً لأن الكود كان بيحمّلها مباشرة
     * برابط نسبي (مش فيه http://host:port) ومن غير الـ Authorization header
     * بتاع الـ API، فأي طلب كان بيفشل فوراً (❌ فشل التحميل).
     * الحل: نحمّل الصورة أولاً بشكل آمن عن طريق AttachmentCache (اللي بيستخدم
     * ChatApiService.downloadAttachment اللي بالفعل بيبعت التوثيق صح)،
     * ونعرضها من الملف المحلي.
     * ✅ جديد: الضغط على الصورة بيفتح عارض صور داخلي (Lightbox) بدل محاولة
     * فتحها كرابط نسبي في المتصفح الخارجي (اللي كان دايماً بيفشل).
     */
    private javafx.scene.Node buildImagePreview(ChatDTOs.ChatAttachmentDTO att) {
        VBox previewBox = new VBox(4);
        previewBox.setAlignment(Pos.CENTER);

        StackPane imageContainer = new StackPane();
        imageContainer.setStyle("-fx-background-color: #F3F4F6; -fx-background-radius: 8;");
        imageContainer.setPrefSize(MAX_IMAGE_WIDTH, 180);

        Label loadingLabel = new Label("⏳ جاري التحميل...");
        loadingLabel.setStyle("-fx-text-fill: #9CA3AF;");
        imageContainer.getChildren().add(loadingLabel);

        AttachmentCache.ensureDownloaded(att).thenAccept(cacheFile -> Platform.runLater(() -> {
            if (cacheFile == null) {
                loadingLabel.setText("❌ فشل التحميل");
                return;
            }
            try {
                Image image = new Image(cacheFile.toUri().toString(),
                        MAX_IMAGE_WIDTH, MAX_IMAGE_HEIGHT, true, true, false);

                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(MAX_IMAGE_WIDTH);
                imageView.setPreserveRatio(true);
                imageView.setStyle("-fx-background-radius: 8; -fx-cursor: hand;");
                imageView.setOnMouseClicked(e -> openLightbox(att));

                imageContainer.getChildren().clear();
                imageContainer.getChildren().add(imageView);
                imageContainer.setPrefSize(-1, -1);
            } catch (Exception e) {
                loadingLabel.setText("❌ فشل عرض الصورة");
            }
        }));

        Label nameLabel = new Label(att.getFileName());
        nameLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6B7280;");

        previewBox.getChildren().addAll(imageContainer, nameLabel);
        return previewBox;
    }

    /**
     * ✅ جديد: يفتح عارض الصور الداخلي على كل صور المحادثة الحالية (مش بس الصورة
     * دي)، عشان المستخدم يقدر يتنقل بينها بالسهام زي أي معرض صور احترافي.
     */
    private void openLightbox(ChatDTOs.ChatAttachmentDTO clicked) {
        List<ChatDTOs.ChatAttachmentDTO> allImages = new ArrayList<>();
        int startIndex = 0;

        for (ChatDTOs.ChatMessageDTO m : ChatService.getInstance().getMessages()) {
            if (m.getAttachments() == null) continue;
            for (ChatDTOs.ChatAttachmentDTO a : m.getAttachments()) {
                if (a.getMimeType() != null && a.getMimeType().startsWith("image/")) {
                    if (a.getDownloadToken() != null && a.getDownloadToken().equals(clicked.getDownloadToken())) {
                        startIndex = allImages.size();
                    }
                    allImages.add(a);
                }
            }
        }

        if (allImages.isEmpty()) allImages.add(clicked);

        new ImageViewerDialog(getScene().getWindow(), allImages, startIndex).show();
    }

    private HBox buildAttachmentRow(ChatDTOs.ChatAttachmentDTO att) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("attachment-row");
        row.setPadding(new Insets(6, 8, 6, 8));

        String icon = getFileIcon(att.getMimeType());
        Label iconLabel = new Label(icon);
        iconLabel.getStyleClass().add("attachment-icon");

        VBox info = new VBox(2);
        Label nameLabel = new Label(att.getFileName() != null ? att.getFileName() : "ملف");
        nameLabel.getStyleClass().add("attachment-name");

        Label sizeLabel = new Label(att.getFormattedSize() != null ? att.getFormattedSize() : "");
        sizeLabel.getStyleClass().add("attachment-size");

        info.getChildren().addAll(nameLabel, sizeLabel);
        HBox.setHgrow(info, Priority.ALWAYS);

        Button downloadBtn = new Button("⬇️");
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
                        FileOpener.open(targetPath.toString());
                    } else {
                        btn.setText("❌");
                    }
                }));
    }

    private String getFileIcon(String mimeType) {
        if (mimeType == null) return "📄";
        if (mimeType.startsWith("image/")) return "🖼️";
        if (mimeType.startsWith("video/")) return "🎬";
        if (mimeType.startsWith("audio/")) return "🎵";
        if (mimeType.contains("pdf")) return "📕";
        if (mimeType.contains("word") || mimeType.contains("document")) return "📝";
        if (mimeType.contains("sheet") || mimeType.contains("excel")) return "📊";
        if (mimeType.contains("zip") || mimeType.contains("compressed")) return "🗜️";
        return "📎";
    }
}