package com.safwat.hr.controller.message.dto;

import com.safwat.hr.controller.message.service.MessageClientService;
import com.safwat.hr.controller.message.service.MessageThread;
import com.safwat.hr.network.SessionManager;
import com.safwat.hr.notification.model.HRNotification;
import com.safwat.hr.notification.model.HRNotification.Attachment;
import com.safwat.hr.ui.controls.SAFNotification;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * =====================================================================
 * MessageConversationView
 * =====================================================================
 * عرض المحادثة بأسلوب Gmail: الموضوع مرة واحدة فوق الترِد،
 * وكل رسالة كتلة نص عادية (بدون فقاعات ملوّنة) قابلة للتحديد والنسخ،
 * مفصولة عن اللي بعدها بخط رفيع.
 */
public class MessageConversationView extends VBox {

    private static final double CONTENT_WIDTH = 560;

    private final VBox messagesContainer;
    private final ScrollPane scrollPane;
    private final Label emptyLabel;

    public MessageConversationView() {
        setSpacing(0);
        setFillWidth(true);
        setStyle("-fx-background-color:#FFFFFF;");

        emptyLabel = new Label("اختر رسالة لعرضها");
        emptyLabel.setStyle("-fx-font-size:14px;-fx-text-fill:#AAAAAA;");

        messagesContainer = new VBox(0);
        messagesContainer.setPadding(new Insets(24, 24, 24, 24));
        messagesContainer.setFillWidth(true);
        messagesContainer.setAlignment(Pos.TOP_CENTER);

        scrollPane = new ScrollPane(messagesContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        getChildren().addAll(emptyLabel, scrollPane);
        setAlignment(Pos.CENTER);
    }

    /**
     * عرض محادثة كاملة (الموضوع + الرسالة الأساسية + الردود).
     */
    public void displayThread(MessageThread thread) {
        messagesContainer.getChildren().clear();
        emptyLabel.setVisible(false);
        emptyLabel.setManaged(false);

        // ── الموضوع: يظهر مرة واحدة فوق الترِد كله زي Gmail ──
        String subject = thread.getSubject();
        Label subjectHeader = new Label(subject != null && !subject.isBlank() ? subject : "(بدون موضوع)");
        subjectHeader.setStyle("-fx-font-size:19px;-fx-font-weight:700;-fx-text-fill:#1A1A1A;");
        subjectHeader.setWrapText(true);
        subjectHeader.setMaxWidth(CONTENT_WIDTH);
        subjectHeader.setPadding(new Insets(0, 0, 16, 0));
        messagesContainer.getChildren().add(subjectHeader);

        messagesContainer.getChildren().add(buildMessageBlock(thread.getRootMessage()));

        for (HRNotification reply : thread.getReplies()) {
            messagesContainer.getChildren().add(buildSeparator());
            messagesContainer.getChildren().add(buildMessageBlock(reply));
        }

        scrollToBottom();
    }

    public void clear() {
        messagesContainer.getChildren().clear();
        emptyLabel.setVisible(true);
        emptyLabel.setManaged(true);
    }

    /**
     * خط فاصل رفيع بين رسالة والتانية (بدل الفقاعات المنفصلة).
     */
    private Region buildSeparator() {
        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setMaxWidth(CONTENT_WIDTH);
        sep.setStyle("-fx-background-color:#EBEBEB;");
        VBox.setMargin(sep, new Insets(14, 0, 14, 0));
        return sep;
    }

    /**
     * بناء كتلة رسالة واحدة: هيدر (صورة+اسم+وقت) + نص قابل للتحديد/النسخ + مرفقات.
     * بدون فقاعة ملوّنة وبدون محاذاة يمين/شمال — كله بعرض ثابت زي Gmail.
     */
    private VBox buildMessageBlock(HRNotification msg) {
        boolean isFromMe = isFromCurrentUser(msg);

        Circle avatar = new Circle(16);
        avatar.setFill(Color.web(isFromMe ? "#185FA5" : "#0F6E56"));
        Label avatarLbl = new Label(msg.getAvatarInitials());
        avatarLbl.setStyle("-fx-font-size:11px;-fx-font-weight:700;-fx-text-fill:white;");
        StackPane avatarBox = new StackPane(avatar, avatarLbl);
        avatarBox.setMinSize(32, 32);
        avatarBox.setMaxSize(32, 32);

        String displayName = msg.getSenderName() != null ? msg.getSenderName() : "مجهول";
        Label nameLbl = new Label(isFromMe ? "أنت" : displayName);
        nameLbl.setStyle("-fx-font-size:13px;-fx-font-weight:700;-fx-text-fill:#1A1A1A;");

        String senderUsername = msg.getSenderUsername();
        VBox nameBox = new VBox(1, nameLbl);
        if (!isFromMe && senderUsername != null && !senderUsername.isBlank()) {
            Label usernameLbl = new Label("@" + senderUsername);
            usernameLbl.setStyle("-fx-font-size:10px;-fx-text-fill:#888888;");
            nameBox.getChildren().add(usernameLbl);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label timeLbl = new Label(formatMessageTime(msg.getTimestamp()));
        timeLbl.setStyle("-fx-font-size:11px;-fx-text-fill:#AAAAAA;");

        HBox header = new HBox(10, avatarBox, nameBox, spacer, timeLbl);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setMaxWidth(CONTENT_WIDTH);

        String bodyText = msg.getMessageBody() != null && !msg.getMessageBody().isBlank()
                ? msg.getMessageBody()
                : msg.getMessage();

        TextArea bodyArea = buildSelectableBody(bodyText != null ? bodyText : "");

        VBox block = new VBox(10, header, bodyArea);
        block.setMaxWidth(CONTENT_WIDTH);
        block.setFillWidth(true);

        if (msg.hasAttachments()) {
            VBox attBox = new VBox(6);
            attBox.setPadding(new Insets(4, 0, 0, 0));
            for (Attachment att : msg.getAttachments()) {
                attBox.getChildren().add(buildAttachmentRow(att));
            }
            block.getChildren().add(attBox);
        }

        return block;
    }

    /**
     * ✅ نص عادي قابل للتحديد والنسخ (Ctrl+C) بدون ما يبان شكله TextArea.
     * الحل: TextArea غير قابلة للتعديل، بدون حدود ولا خلفية ولا scrollbar،
     * وبيتحسب ارتفاعها تلقائيًا حسب طول النص عشان تبان زي فقرة نص عادية.
     */
    private TextArea buildSelectableBody(String text) {
        TextArea area = new TextArea(text);
        area.setEditable(false);
        area.setWrapText(true);
        area.setMaxWidth(CONTENT_WIDTH);
        area.setPrefWidth(CONTENT_WIDTH);
        area.setStyle(
                "-fx-background-color:transparent;" +
                        "-fx-text-fill:#333333;" +
                        "-fx-font-size:13px;" +
                        "-fx-padding:0;" +
                        "-fx-background-insets:0;" +
                        "-fx-border-width:0;" +
                        "-fx-highlight-fill:#B3D7F0;" + // لون التحديد
                        "-fx-highlight-text-fill:#000000;"
        );

        // حساب الارتفاع المناسب للنص عشان الـ TextArea تبان مضبوطة على المحتوى
        // بدون فراغ زيادة وبدون احتياج scrollbar داخلي.
        Text measurer = new Text(text);
        measurer.setFont(Font.font(13));
        measurer.setWrappingWidth(CONTENT_WIDTH - 16);
        double measuredHeight = measurer.getLayoutBounds().getHeight();
        double finalHeight = Math.max(24, measuredHeight + 26); // padding أمان يمنع ظهور scrollbar
        area.setPrefHeight(finalHeight);
        area.setMinHeight(finalHeight);

        // إخفاء الـ scrollbar الداخلي بعد ما الـ TextArea تتركب فعليًا في الـ Scene
        Platform.runLater(() -> {
            area.lookupAll(".scroll-bar").forEach(n -> {
                n.setVisible(false);
                n.setManaged(false);
            });
        });

        // تمرير حركة الـ scroll wheel لسكرول المحادثة الخارجي بدل ما تتحبس هنا
        area.addEventFilter(ScrollEvent.SCROLL, e -> {
            double delta = e.getDeltaY();
            scrollPane.setVvalue(scrollPane.getVvalue() - delta / 800.0);
            e.consume();
        });

        return area;
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
        row.setMaxWidth(CONTENT_WIDTH);

        row.setOnMouseEntered(e -> row.setStyle(row.getStyle().replace("#F8F8F8", "#E6F1FB")));
        row.setOnMouseExited(e -> row.setStyle(row.getStyle().replace("#E6F1FB", "#F8F8F8")));
        row.setOnMouseClicked(e -> startDownloadWithCheck(att));

        return row;
    }

    private boolean isFromCurrentUser(HRNotification msg) {
        String currentUser = SessionManager.getInstance().getUsername();
        return currentUser != null && currentUser.equals(msg.getSenderUsername());
    }

    public void scrollToBottom() {
        Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }

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

    private void startDownloadWithCheck(Attachment att) {
        if (att.getDownloadToken() == null || att.getDownloadToken().isBlank()) {
            showError("لا يوجد رابط تحميل لهذا الملف");
            return;
        }

        MessageClientService.getInstance().checkAttachmentExists(att.getDownloadToken())
                .thenAccept(exists -> Platform.runLater(() -> {
                    if (exists) {
                        showDownloadDialog(att);
                    } else {
                        showError("الملف غير موجود على السيرفر أو تم حذفه");
                    }
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> showError("تعذر التحقق من وجود الملف: " + e.getMessage()));
                    return null;
                });
    }

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

        MessageClientService.getInstance().downloadAttachment(
                att.getDownloadToken(),
                targetFile.toPath(),
                () -> Platform.runLater(() -> {
                    try {
                        SAFNotification.withAction("Do you want open this FILE ?", targetFile);
                    } catch (Exception ignored) {
                    }
                }),
                err -> Platform.runLater(() -> {
                    System.err.println("[Conversation] Download failed: " + err);
                    showError("فشل التحميل: " + err);
                })
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