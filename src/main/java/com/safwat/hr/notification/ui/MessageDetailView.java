package com.safwat.hr.notification.ui;

import com.safwat.hr.notification.model.HRNotification;
import com.safwat.hr.notification.service.MessageClientService;
import com.safwat.hr.notification.service.NotificationService;
import com.safwat.hr.notification.util.FileOpener;
import io.github.palexdev.materialfx.controls.MFXButton;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.function.Consumer;

/**
 * =====================================================
 * MessageDetailView — عرض الرسالة الكاملة
 * =====================================================
 * <p>
 * التغييرات:
 * - زر الرد يفتح ComposeMessageDialog مع اسم المرسل والموضوع مسبقاً
 * - قسم المرفقات يظهر دائماً لو hasAttachments() == true
 * - تحميل المرفقات عبر token من الخادم مع FileChooser لاختيار مكان الحفظ
 */
public class MessageDetailView {

    private final HRNotification message;
    private final NotificationService notifService = NotificationService.getInstance();
    private Stage stage;
    private Stage ownerStage;

    private MessageDetailView(HRNotification message) {
        this.message = message;
    }

    public static void show(Stage owner, HRNotification message) {
        new MessageDetailView(message).showDialog(owner);
    }

    private void showDialog(Stage owner) {
        this.ownerStage = owner;
        stage = new Stage();
        stage.initStyle(StageStyle.UNDECORATED);
        stage.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) stage.initOwner(owner);

        VBox root = new VBox(0);
        root.setStyle(
                "-fx-background-color:#FFFFFF;" +
                        "-fx-background-radius:12px;" +
                        "-fx-border-color:#E0E0E0;-fx-border-width:0.5px;" +
                        "-fx-border-radius:12px;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.22),24,0,0,8);"
        );

        root.getChildren().addAll(
                buildTitleBar(),
                buildMessageHeader(),
                buildDivider(),
                buildMessageBody(),
                buildAttachmentsSection(),
                buildActionBar()
        );

        Scene scene = new Scene(root, 580, 560);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);

        if (owner != null) {
            stage.setX(owner.getX() + (owner.getWidth() - 580) / 2);
            stage.setY(owner.getY() + (owner.getHeight() - 560) / 2);
        }

        // تعليم مقروء محلياً وعلى الخادم
        notifService.markAsRead(message);
        extractMessageId(message.getActionTarget(), id -> {
            if (id != null)
                MessageClientService.getInstance().markMessageAsRead(id);
        });

        stage.show();
        animateIn(root);
    }

    // ===================== شريط العنوان =====================
    private HBox buildTitleBar() {
        HBox bar = new HBox();
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(12, 16, 10, 16));
        bar.setStyle(
                "-fx-background-color:#F8F8F8;" +
                        "-fx-background-radius:12px 12px 0 0;" +
                        "-fx-border-color:transparent transparent #EBEBEB transparent;" +
                        "-fx-border-width:0 0 0.5 0;"
        );

        Label icon = new Label("[MSG]");
        icon.setStyle("-fx-font-size:12px;-fx-font-weight:700;-fx-text-fill:#0F6E56;");

        Label title = new Label("رسالة");
        title.setStyle("-fx-font-size:14px;-fx-font-weight:700;-fx-text-fill:#1A1A1A;-fx-padding:0 0 0 8;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label closeBtn = new Label("X");
        styleCloseBtn(closeBtn);
        closeBtn.setOnMouseClicked(e -> stage.close());

        final double[] drag = new double[2];
        bar.setOnMousePressed(e -> {
            drag[0] = stage.getX() - e.getScreenX();
            drag[1] = stage.getY() - e.getScreenY();
        });
        bar.setOnMouseDragged(e -> {
            stage.setX(e.getScreenX() + drag[0]);
            stage.setY(e.getScreenY() + drag[1]);
        });

        bar.getChildren().addAll(icon, title, spacer, closeBtn);
        return bar;
    }

    // ===================== رأس الرسالة =====================
    private HBox buildMessageHeader() {
        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 20, 14, 20));

        Circle avatarCircle = new Circle(24);
        avatarCircle.setFill(Color.web("#0F6E56"));
        Label avatarLbl = new Label(message.getAvatarInitials());
        avatarLbl.setStyle("-fx-font-size:14px;-fx-font-weight:700;-fx-text-fill:white;");
        StackPane avatarBox = new StackPane(avatarCircle, avatarLbl);
        avatarBox.setMinSize(48, 48);
        avatarBox.setMaxSize(48, 48);

        Label senderLbl = new Label(message.getSenderName() != null ? message.getSenderName() : "مجهول");
        senderLbl.setStyle("-fx-font-size:15px;-fx-font-weight:700;-fx-text-fill:#1A1A1A;");

        Label subjectLbl = new Label("الموضوع: " + message.getTitle());
        subjectLbl.setStyle("-fx-font-size:13px;-fx-text-fill:#555555;");

        Label timeLbl = new Label(message.getFormattedTime());
        timeLbl.setStyle("-fx-font-size:11px;-fx-text-fill:#AAAAAA;");

        VBox info = new VBox(3, senderLbl, subjectLbl, timeLbl);
        info.setAlignment(Pos.CENTER_LEFT);

        header.getChildren().addAll(avatarBox, info);
        return header;
    }

    // ===================== نص الرسالة =====================
    private ScrollPane buildMessageBody() {
        String body = (message.getMessageBody() != null && !message.getMessageBody().isBlank())
                ? message.getMessageBody()
                : message.getMessage();

        Label bodyLbl = new Label(body);
        bodyLbl.setStyle("-fx-font-size:13px;-fx-text-fill:#333333;-fx-line-spacing:4px;");
        bodyLbl.setWrapText(true);
        bodyLbl.setMaxWidth(520);

        VBox wrapper = new VBox(bodyLbl);
        wrapper.setPadding(new Insets(4, 20, 12, 20));

        ScrollPane scroll = new ScrollPane(wrapper);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(180);
        scroll.setMaxHeight(260);
        scroll.setStyle("-fx-background-color:transparent;-fx-border-color:transparent;");
        VBox.setVgrow(scroll, Priority.SOMETIMES);
        return scroll;
    }

    // ===================== قسم المرفقات =====================

    /**
     * المشكلة الأصلية: المرفقات كانت placeholders بمسارات فارغة.
     * الحل: نعرض اسم المرفق وزر تحميل يستخدم downloadToken للتحميل من الخادم.
     * لو downloadToken فارغ نفتح FileOpener مباشرة بالمسار المحلي.
     */
    private VBox buildAttachmentsSection() {
        if (!message.hasAttachments()) return new VBox(0);

        VBox section = new VBox(8);
        section.setPadding(new Insets(10, 20, 10, 20));
        section.setStyle(
                "-fx-background-color:#F8F8F8;" +
                        "-fx-border-color:#EBEBEB transparent #EBEBEB transparent;" +
                        "-fx-border-width:0.5 0 0.5 0;"
        );

        Label title = new Label("المرفقات (" + message.getAttachments().size() + ")");
        title.setStyle("-fx-font-size:12px;-fx-font-weight:700;-fx-text-fill:#555555;");

        FlowPane flow = new FlowPane(8, 8);
        flow.setPrefWrapLength(520);

        message.getAttachments().forEach(att ->
                flow.getChildren().add(buildAttachmentCard(att)));

        section.getChildren().addAll(title, flow);

        if (message.getAttachments().size() > 1) {
            MFXButton downloadAll = new MFXButton("تحميل الكل");
            downloadAll.setStyle(
                    "-fx-background-color:#0F6E56;-fx-text-fill:white;" +
                            "-fx-font-size:12px;-fx-background-radius:6px;" +
                            "-fx-padding:5 14 5 14;-fx-cursor:hand;"
            );
            downloadAll.setOnAction(e ->
                    message.getAttachments().forEach(this::downloadAttachment));
            HBox btnRow = new HBox(downloadAll);
            btnRow.setAlignment(Pos.CENTER_LEFT);
            section.getChildren().add(btnRow);
        }

        return section;
    }

    private HBox buildAttachmentCard(HRNotification.Attachment att) {
        Label iconLbl = new Label(att.getIcon());
        iconLbl.setStyle(
                "-fx-font-size:10px;-fx-font-weight:700;-fx-text-fill:#0F6E56;" +
                        "-fx-background-color:#E6F5F1;-fx-background-radius:4px;" +
                        "-fx-padding:3 6 3 6;"
        );

        // اسم الملف — لو placeholder اعرض "مرفق"
        String displayName = att.getFileName();
        Label nameLbl = new Label(displayName);
        nameLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#333333;");
        nameLbl.setMaxWidth(160);

        Label sizeLbl = new Label(att.getFormattedSize());
        sizeLbl.setStyle("-fx-font-size:10px;-fx-text-fill:#AAAAAA;");

        MFXButton dlBtn = new MFXButton("تحميل");
        dlBtn.setStyle(
                "-fx-background-color:transparent;-fx-text-fill:#185FA5;" +
                        "-fx-font-size:11px;-fx-cursor:hand;-fx-padding:0;"
        );
        dlBtn.setOnAction(e -> downloadAttachment(att));

        VBox info = new VBox(2, nameLbl, sizeLbl);
        info.setAlignment(Pos.CENTER_LEFT);

        HBox card = new HBox(8, iconLbl, info, dlBtn);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(6, 10, 6, 10));
        card.setStyle(
                "-fx-background-color:#FFFFFF;-fx-background-radius:8px;" +
                        "-fx-border-color:#E0E0E0;-fx-border-width:0.5px;" +
                        "-fx-border-radius:8px;-fx-cursor:hand;"
        );
        card.setOnMouseEntered(e -> card.setStyle(card.getStyle().replace("#FFFFFF", "#F0FAF7")));
        card.setOnMouseExited(e -> card.setStyle(card.getStyle().replace("#F0FAF7", "#FFFFFF")));
        return card;
    }

    /**
     * تحميل مرفق:
     * - لو عنده downloadToken → حمّل من الخادم عبر API
     * - لو عنده filePath محلي → افتح مباشرة
     * - لو placeholder (مسار فارغ) → أظهر رسالة خطأ
     */
    private void downloadAttachment(HRNotification.Attachment att) {
        String token = att.getDownloadToken();
        String filePath = att.getFilePath();

        // لو المسار محلي موجود — افتح مباشرة
        if (filePath != null && !filePath.isBlank()) {
            java.io.File localFile = new java.io.File(filePath);
            if (localFile.exists()) {
                FileOpener.open(filePath);
                return;
            }
        }

        // لو token موجود — حمّل من الخادم
        if (token != null && !token.isBlank()) {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("حفظ المرفق");
            chooser.setInitialFileName(att.getFileName());

            String userHome = System.getProperty("user.home");
            java.io.File docsDir = new java.io.File(userHome + "/Documents");
            if (!docsDir.exists()) docsDir = new java.io.File(userHome);
            chooser.setInitialDirectory(docsDir);

            java.io.File targetFile = chooser.showSaveDialog(stage);
            if (targetFile == null) return;

            MessageClientService.getInstance().downloadAttachment(
                    token,
                    targetFile.toPath(),
                    () -> {
                        // نجاح
                        showInfo("تم التحميل", "تم حفظ الملف في:\n" + targetFile.getAbsolutePath());
                        FileOpener.open(targetFile.getAbsolutePath());
                    },
                    err -> showError("فشل التحميل", err)
            );
            return;
        }

        // placeholder بدون بيانات حقيقية
        showError("تعذر التحميل",
                "لم يتم تحديد رابط التحميل لهذا المرفق.\nأعد فتح الرسالة أو تحقق من الاتصال.");
    }

    // ===================== شريط الإجراءات (رد + إغلاق) =====================

    /**
     * زر الرد يفتح ComposeMessageDialog مع:
     * - اسم المرسل مسبقاً في حقل المستقبل
     * - "رد: [الموضوع الأصلي]" في حقل الموضوع
     */
    private HBox buildActionBar() {
        HBox bar = new HBox(8);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(10, 16, 14, 16));
        bar.setStyle(
                "-fx-border-color:#EBEBEB transparent transparent transparent;" +
                        "-fx-border-width:0.5 0 0 0;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        MFXButton closeBtn2 = new MFXButton("إغلاق");
        closeBtn2.setStyle(
                "-fx-background-color:#F0F0F0;-fx-text-fill:#666666;" +
                        "-fx-font-size:13px;-fx-background-radius:8px;" +
                        "-fx-padding:8 18 8 18;-fx-cursor:hand;"
        );
        closeBtn2.setOnAction(e -> stage.close());

        MFXButton replyBtn = new MFXButton("رد ↩");
        replyBtn.setStyle(
                "-fx-background-color:#0F6E56;-fx-text-fill:white;" +
                        "-fx-font-size:13px;-fx-font-weight:700;" +
                        "-fx-background-radius:8px;-fx-padding:8 22 8 22;-fx-cursor:hand;"
        );
        replyBtn.setOnAction(e -> openReplyDialog());

        bar.getChildren().addAll(spacer, closeBtn2, replyBtn);
        return bar;
    }

    /**
     * يفتح ComposeMessageDialog مع بيانات الرسالة الأصلية مسبقة الملء.
     */
    private void openReplyDialog() {
        stage.close();  // أغلق التفاصيل

        String recipient = message.getSenderName() != null
                ? message.getSenderName() : "";
        String subject = "رد: " + (message.getTitle() != null ? message.getTitle() : "");
        Long parentId = null;
        String target = message.getActionTarget();
        if (target != null) {
            try {
                parentId = Long.parseLong(target.substring(target.lastIndexOf('/') + 1));
            } catch (NumberFormatException ignored) {
            }
        }

        ComposeMessageDialog.showReply(ownerStage, recipient, subject, parentId);
    }

    // ===================== مساعدات =====================

    private void extractMessageId(String actionTarget, Consumer<Long> callback) {
        if (actionTarget == null) {
            callback.accept(null);
            return;
        }
        try {
            callback.accept(Long.parseLong(
                    actionTarget.substring(actionTarget.lastIndexOf('/') + 1)));
        } catch (NumberFormatException e) {
            callback.accept(null);
        }
    }

    private Separator buildDivider() {
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color:#EBEBEB;");
        return sep;
    }

    private void styleCloseBtn(Label btn) {
        btn.setStyle("-fx-font-size:14px;-fx-text-fill:#AAAAAA;-fx-cursor:hand;-fx-padding:4 8 4 8;-fx-background-radius:6px;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-font-size:14px;-fx-text-fill:#CC3333;-fx-cursor:hand;-fx-padding:4 8 4 8;-fx-background-radius:6px;-fx-background-color:#FFE8E8;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-font-size:14px;-fx-text-fill:#AAAAAA;-fx-cursor:hand;-fx-padding:4 8 4 8;-fx-background-radius:6px;"));
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.initOwner(stage);
        a.show();
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.initOwner(stage);
        a.show();
    }

    private void animateIn(VBox root) {
        root.setOpacity(0);
        root.setTranslateY(-8);
        FadeTransition fade = new FadeTransition(Duration.millis(200), root);
        fade.setToValue(1.0);
        TranslateTransition slide = new TranslateTransition(Duration.millis(200), root);
        slide.setToY(0);
        new ParallelTransition(fade, slide).play();
    }
}