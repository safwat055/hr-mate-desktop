package com.safwat.hr.model.message;

import com.safwat.hr.model.message.service.MessageClientService;
import com.safwat.hr.model.message.service.MessageThread;
import com.safwat.hr.notification.model.HRNotification;
import com.safwat.hr.notification.model.HRNotification.Attachment;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * =====================================================================
 * MessageConversationView
 * =====================================================================
 * عرض المحادثة بشكل متراكك (Gmail-style).
 * يعرض الرسالة الأساسية ثم الردود تحتها.
 * يدعم عرض المرفقات مع التحقق من وجودها قبل التحميل.
 * يميز بين الرسائل الواردة والصادرة بالألوان والمحاذاة.
 */
public class MessageConversationView extends VBox {

    private final VBox messagesContainer;
    private final ScrollPane scrollPane;
    private final Label emptyLabel;

    /**
     * إنشاء منطقة عرض المحادثة.
     */
    public MessageConversationView() {
        setSpacing(0);
        setFillWidth(true);
        setStyle("-fx-background-color:#FAFAFA;");

        emptyLabel = new Label("اختر رسالة لعرضها");
        emptyLabel.setStyle("-fx-font-size:14px;-fx-text-fill:#AAAAAA;");

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
     * عرض محادثة كاملة (الرسالة الأساسية + الردود).
     *
     * @param thread كائن المحادثة
     */
    public void displayThread(MessageThread thread) {
        messagesContainer.getChildren().clear();
        emptyLabel.setVisible(false);
        emptyLabel.setManaged(false);

        messagesContainer.getChildren().add(buildMessageBubble(thread.getRootMessage(), false));

        if (!thread.getReplies().isEmpty()) {
            Label sep = new Label("الردود");
            sep.setStyle("-fx-font-size:11px;-fx-text-fill:#888888;-fx-padding:8 0 0 0;");
            sep.setMaxWidth(520);
            sep.setAlignment(Pos.CENTER_LEFT);
            messagesContainer.getChildren().add(sep);
        }

        for (HRNotification reply : thread.getReplies()) {
            messagesContainer.getChildren().add(buildMessageBubble(reply, true));
        }

        scrollToBottom();
    }

    /**
     * إفراغ منطقة العرض وإظهار حالة الفراغ.
     */
    public void clear() {
        messagesContainer.getChildren().clear();
        emptyLabel.setVisible(true);
        emptyLabel.setManaged(true);
    }

    /**
     * بناء فقاعة رسالة (bubble) لعرضها في المحادثة.
     *
     * @param msg     كائن الرسالة
     * @param isReply true إذا كانت رداً
     * @return HBox يمثل الفقاعة
     */
    private HBox buildMessageBubble(HRNotification msg, boolean isReply) {
        boolean isFromMe = isFromCurrentUser(msg);

        Circle avatar = new Circle(16);
        avatar.setFill(Color.web(isFromMe ? "#185FA5" : "#0F6E56"));
        Label avatarLbl = new Label(msg.getAvatarInitials());
        avatarLbl.setStyle("-fx-font-size:11px;-fx-font-weight:700;-fx-text-fill:white;");
        javafx.scene.layout.StackPane avatarBox = new javafx.scene.layout.StackPane(avatar, avatarLbl);
        avatarBox.setMinSize(32, 32);
        avatarBox.setMaxSize(32, 32);

        String displayName = msg.getSenderName() != null ? msg.getSenderName() : "مجهول";
        Label nameLbl = new Label(displayName);
        nameLbl.setStyle("-fx-font-size:13px;-fx-font-weight:700;-fx-text-fill:#1A1A1A;");

        String senderUsername = msg.getSenderUsername();
        VBox nameBox = new VBox(1, nameLbl);
        if (senderUsername != null && !senderUsername.isBlank() && !senderUsername.equals(displayName)) {
            Label usernameLbl = new Label("@" + senderUsername);
            usernameLbl.setStyle("-fx-font-size:10px;-fx-text-fill:#888888;");
            nameBox.getChildren().add(usernameLbl);
        }

        String timeText = formatMessageTime(msg.getTimestamp());
        Label timeLbl = new Label(timeText);
        timeLbl.setStyle("-fx-font-size:10px;-fx-text-fill:#AAAAAA;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(8, avatarBox, nameBox, spacer, timeLbl);
        header.setAlignment(Pos.CENTER_LEFT);

        String bodyText = msg.getMessageBody() != null && !msg.getMessageBody().isBlank()
                ? msg.getMessageBody()
                : msg.getMessage();
        Label bodyLbl = new Label(bodyText);
        bodyLbl.setWrapText(true);
        bodyLbl.setMaxWidth(480);
        bodyLbl.setStyle("-fx-font-size:13px;-fx-text-fill:#333333;-fx-line-spacing:5px;");

        VBox bubbleContent = new VBox(8, header, bodyLbl);

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

        HBox wrapper = new HBox(bubbleContent);
        wrapper.setAlignment(isFromMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        HBox.setHgrow(wrapper, Priority.ALWAYS);
        wrapper.setMaxWidth(560);

        return wrapper;
    }

    /**
     * بناء صف مرفق مع أيقونة واسم وحجم وزر تحميل.
     *
     * @param att كائن المرفق
     * @return HBox يمثل صف المرفق
     */
    private HBox buildAttachmentRow(Attachment att) {
        Label icon = new Label(att.getIcon());
        icon.setStyle("-fx-font-size:10px;-fx-font-weight:700;-fx-text-fill:#185FA5;" +
                "-fx-background-color:#E6F1FB;-fx-background-radius:4px;-fx-padding:3 6;");

        Label name = new Label(att.getFileName());
        name.setStyle("-fx-font-size:12px;-fx-text-fill:#333333;");
        name.setMaxWidth(200);

        Label size = new Label(att.getFormattedSize());
        size.setStyle("-fx-font-size:10px;-fx-text-fill:#888888;");

        io.github.palexdev.materialfx.controls.MFXButton dlBtn =
                new io.github.palexdev.materialfx.controls.MFXButton("⬇ تحميل");
        dlBtn.setStyle(
                "-fx-background-color:transparent;-fx-text-fill:#185FA5;" +
                        "-fx-font-size:11px;-fx-cursor:hand;-fx-padding:0 4;"
        );
        dlBtn.setOnAction(e -> startDownloadWithCheck(att));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(8, icon, name, size, spacer, dlBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color:#F8F8F8;-fx-background-radius:6px;-fx-padding:6 10;-fx-cursor:hand;");
        row.setMaxWidth(480);

        row.setOnMouseEntered(_ -> row.setStyle(row.getStyle().replace("#F8F8F8", "#E6F1FB")));
        row.setOnMouseExited(e -> row.setStyle(row.getStyle().replace("#E6F1FB", "#F8F8F8")));
        row.setOnMouseClicked(e -> startDownloadWithCheck(att));

        return row;
    }

    /**
     * التحقق مما إذا كانت الرسالة من المستخدم الحالي.
     *
     * @param msg كائن الرسالة
     * @return true إذا كانت من المستخدم الحالي
     */
    private boolean isFromCurrentUser(HRNotification msg) {
        String currentUser = ApiClient.getUserName();
        return currentUser != null && currentUser.equals(msg.getSenderUsername());
    }

    /**
     * التمرير لأسفل منطقة المحادثة.
     */
    public void scrollToBottom() {
        javafx.application.Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }

    /**
     * تنسيق وقت الرسالة للعرض.
     * - اليوم: الساعة فقط
     * - الأمس: "أمس" + الساعة
     * - أقدم: التاريخ الكامل
     *
     * @param timestamp وقت الرسالة
     * @return النص المنسق
     */
    private String formatMessageTime(LocalDateTime timestamp) {
        if (timestamp == null) return "";

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalDate msgDate = timestamp.toLocalDate();

        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("h:mm a");

        if (msgDate.equals(today)) {
            return timestamp.format(timeFmt);
        } else if (msgDate.equals(today.minusDays(1))) {
            return "أمس " + timestamp.format(timeFmt);
        } else {
            return timestamp.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }
    }

    /**
     * التحقق من وجود الملف على الخادم قبل عرض خيارات الحفظ.
     *
     * @param att كائن المرفق
     */
    private void startDownloadWithCheck(Attachment att) {
        if (att.getDownloadToken() == null || att.getDownloadToken().isBlank()) {
            showError("لا يوجد رابط تحميل لهذا الملف");
            return;
        }

        MessageClientService.getInstance().checkAttachmentExists(att.getDownloadToken())
                .thenAccept(exists -> {
                    javafx.application.Platform.runLater(() -> {
                        if (exists) {
                            showDownloadDialog(att);
                        } else {
                            showError("الملف غير موجود على السيرفر أو تم حذفه");
                        }
                    });
                })
                .exceptionally(e -> {
                    javafx.application.Platform.runLater(() ->
                            showError("تعذر التحقق من وجود الملف: " + e.getMessage()));
                    return null;
                });
    }

    /**
     * عرض حوار حفظ الملف.
     *
     * @param att كائن المرفق
     */
    private void showDownloadDialog(Attachment att) {
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
                        System.out.println("[Conversation] Downloaded: " + att.getFileName());
                        try {
                            java.awt.Desktop.getDesktop().open(targetFile);
                        } catch (Exception ignored) {
                        }
                    });
                },
                err -> {
                    javafx.application.Platform.runLater(() -> {
                        System.err.println("[Conversation] Download failed: " + err);
                        showError("فشل التحميل: " + err);
                    });
                }
        );
    }

    private void showError(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("خطأ");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}