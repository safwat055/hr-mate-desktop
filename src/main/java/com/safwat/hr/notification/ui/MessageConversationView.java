package com.safwat.hr.notification.ui;

import com.safwat.hr.notification.model.HRNotification;
import com.safwat.hr.notification.model.HRNotification.Attachment;
import com.safwat.hr.notification.service.MessageClientService;
import com.safwat.hr.utils.ApiClient;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/**
 * =====================================================
 * MessageConversationView — عرض المحادثة متراكمة (Gmail-style)
 * =====================================================
 */
public class MessageConversationView extends VBox {

    private final VBox messagesContainer;
    private final ScrollPane scrollPane;
    private final Label emptyLabel;

    public MessageConversationView() {
        setSpacing(0);
        setFillWidth(true);
        setStyle("-fx-background-color:#FAFAFA;");

        // Empty state
        emptyLabel = new Label("اختر رسالة لعرضها");
        emptyLabel.setStyle("-fx-font-size:14px;-fx-text-fill:#AAAAAA;");

        // Messages container
        messagesContainer = new VBox(16);
        messagesContainer.setPadding(new Insets(20));
        messagesContainer.setFillWidth(true);
        messagesContainer.setAlignment(Pos.TOP_CENTER);

        scrollPane = new ScrollPane(messagesContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color:transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        getChildren().addAll(emptyLabel, scrollPane);
        setAlignment(Pos.CENTER);
    }

    /**
     * عرض thread كامل (root + replies)
     */
    public void displayThread(MessageThread thread) {
        messagesContainer.getChildren().clear();
        emptyLabel.setVisible(false);
        emptyLabel.setManaged(false);

        // Root message
        messagesContainer.getChildren().add(buildMessageBubble(thread.getRootMessage(), false));

        // Separator
        if (!thread.getReplies().isEmpty()) {
            Label sep = new Label("الردود");
            sep.setStyle("-fx-font-size:11px;-fx-text-fill:#888888;-fx-padding:8 0 0 0;");
            sep.setMaxWidth(520);
            sep.setAlignment(Pos.CENTER_LEFT);
            messagesContainer.getChildren().add(sep);
        }

        // Replies
        for (HRNotification reply : thread.getReplies()) {
            messagesContainer.getChildren().add(buildMessageBubble(reply, true));
        }

        scrollToBottom();
    }

    public void clear() {
        messagesContainer.getChildren().clear();
        emptyLabel.setVisible(true);
        emptyLabel.setManaged(true);
    }

    /**
     * بناء bubble للرسالة
     */
    private HBox buildMessageBubble(HRNotification msg, boolean isReply) {
        boolean isFromMe = isFromCurrentUser(msg);

        // Header: Avatar + Name + Time
        Circle avatar = new Circle(16);
        avatar.setFill(Color.web(isFromMe ? "#185FA5" : "#0F6E56"));
        Label avatarLbl = new Label(msg.getAvatarInitials());
        avatarLbl.setStyle("-fx-font-size:11px;-fx-font-weight:700;-fx-text-fill:white;");
        javafx.scene.layout.StackPane avatarBox = new javafx.scene.layout.StackPane(avatar, avatarLbl);
        avatarBox.setMinSize(32, 32);
        avatarBox.setMaxSize(32, 32);

        Label nameLbl = new Label(
                msg.getSenderName() != null ? msg.getSenderName() : "مجهول");
        nameLbl.setStyle("-fx-font-size:13px;-fx-font-weight:700;-fx-text-fill:#1A1A1A;");

        Label timeLbl = new Label(msg.getFormattedTime());
        timeLbl.setStyle("-fx-font-size:10px;-fx-text-fill:#AAAAAA;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(8, avatarBox, nameLbl, spacer, timeLbl);
        header.setAlignment(Pos.CENTER_LEFT);

        // Body
        String bodyText = msg.getMessageBody() != null && !msg.getMessageBody().isBlank()
                ? msg.getMessageBody()
                : msg.getMessage();
        Label bodyLbl = new Label(bodyText);
        bodyLbl.setWrapText(true);
        bodyLbl.setMaxWidth(480);
        bodyLbl.setStyle("-fx-font-size:13px;-fx-text-fill:#333333;-fx-line-spacing:5px;");

        VBox bubbleContent = new VBox(8, header, bodyLbl);

        // Attachments
        if (msg.hasAttachments()) {
            VBox attBox = new VBox(6);
            for (Attachment att : msg.getAttachments()) {
                HBox attRow = buildAttachmentRow(att);
                attBox.getChildren().add(attRow);
            }
            bubbleContent.getChildren().add(attBox);
        }

        bubbleContent.setPadding(new Insets(14, 16, 14, 16));
        bubbleContent.setMaxWidth(540);

        String bg = isFromMe ? "#E6F1FB" : "#FFFFFF";
        String border = isFromMe ? "#185FA5" : "#E0E0E0";
        bubbleContent.setStyle(
                "-fx-background-color:" + bg + ";" +
                        "-fx-background-radius:12px;" +
                        "-fx-border-color:" + border + ";" +
                        "-fx-border-width:0.5px;" +
                        "-fx-border-radius:12px;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),8,0,0,2);"
        );

        // Align right if from me
        HBox wrapper = new HBox(bubbleContent);
        wrapper.setAlignment(isFromMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        HBox.setHgrow(wrapper, Priority.ALWAYS);
        wrapper.setMaxWidth(560);

        return wrapper;
    }

    private HBox buildAttachmentRow(Attachment att) {
        Label icon = new Label(att.getIcon());
        icon.setStyle("-fx-font-size:10px;-fx-font-weight:700;-fx-text-fill:#185FA5;" +
                "-fx-background-color:#E6F1FB;-fx-background-radius:4px;-fx-padding:3 6;");

        Label name = new Label(att.getFileName());
        name.setStyle("-fx-font-size:12px;-fx-text-fill:#333333;");
        name.setMaxWidth(200);

        Label size = new Label(att.getFormattedSize());
        size.setStyle("-fx-font-size:10px;-fx-text-fill:#888888;");

        // ✅ زر تحميل واضح
        io.github.palexdev.materialfx.controls.MFXButton dlBtn =
                new io.github.palexdev.materialfx.controls.MFXButton("⬇ تحميل");
        dlBtn.setStyle(
                "-fx-background-color:transparent;-fx-text-fill:#185FA5;" +
                        "-fx-font-size:11px;-fx-cursor:hand;-fx-padding:0 4;"
        );
        dlBtn.setOnAction(e -> downloadAttachment(att));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(8, icon, name, size, spacer, dlBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color:#F8F8F8;-fx-background-radius:6px;-fx-padding:6 10;-fx-cursor:hand;");
        row.setMaxWidth(480);

        // Hover effect
        row.setOnMouseEntered(_ -> row.setStyle(row.getStyle().replace("#F8F8F8", "#E6F1FB")));
        row.setOnMouseExited(e -> row.setStyle(row.getStyle().replace("#E6F1FB", "#F8F8F8")));
        row.setOnMouseClicked(e -> downloadAttachment(att));

        return row;
    }

    private boolean isFromCurrentUser(HRNotification msg) {
        String currentUser = ApiClient.getUserName();
        return currentUser != null && currentUser.equals(msg.getSenderUsername());
    }

    public void scrollToBottom() {
        javafx.application.Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }

    /**
     * ✅ تحميل مرفق — بيستخدم token من السيرفر
     */
    private void downloadAttachment(Attachment att) {
        if (att.getDownloadToken() == null || att.getDownloadToken().isBlank()) {
            System.err.println("[Conversation] No download token for: " + att.getFileName());
            return;
        }

        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setTitle("حفظ المرفق");
        chooser.setInitialFileName(att.getFileName());

        String userHome = System.getProperty("user.home");
        java.io.File docsDir = new java.io.File(userHome + "/Documents");
        if (!docsDir.exists()) docsDir = new java.io.File(userHome);
        chooser.setInitialDirectory(docsDir);

        java.io.File targetFile = chooser.showSaveDialog(this.getScene().getWindow());
        if (targetFile == null) return;

        System.out.println("[Conversation] Downloading: " + att.getFileName() + " | token=" + att.getDownloadToken());

        MessageClientService.getInstance().downloadAttachment(
                att.getDownloadToken(),
                targetFile.toPath(),
                () -> {
                    javafx.application.Platform.runLater(() -> {
                        System.out.println("[Conversation] ✅ Downloaded: " + att.getFileName());
                        // Optional: open file
                        try {
                            java.awt.Desktop.getDesktop().open(targetFile);
                        } catch (Exception ignored) {
                        }
                    });
                },
                err -> {
                    javafx.application.Platform.runLater(() -> {
                        System.err.println("[Conversation] ❌ Download failed: " + err);
                    });
                }
        );
    }
}