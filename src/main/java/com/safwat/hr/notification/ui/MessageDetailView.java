package com.safwat.hr.notification.ui;

import com.safwat.hr.notification.model.HRNotification;
import com.safwat.hr.notification.service.MessageClientService;
import com.safwat.hr.notification.service.NotificationService;
import com.safwat.hr.notification.util.FileOpener;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * =====================================================
 * MessageDetailView — عرض الرسالة الكاملة
 * =====================================================
 * <p>
 * تُفتح عند الضغط على "فتح" في HRNotificationPanel أو HRToast.
 * <p>
 * تدعم:
 * - عرض نص الرسالة الكامل (messageBody)
 * - عرض المرفقات مع تحميل فردي أو جماعي
 * - الرد مع مرفقات
 * - تعليم مقروء تلقائياً عند الفتح
 * <p>
 * الاستخدام:
 * MessageDetailView.show(primaryStage, notification);
 */
public class MessageDetailView {

    private final HRNotification message;
    private final NotificationService notifService = NotificationService.getInstance();
    private final MessageClientService msgService = MessageClientService.getInstance();
    // مرفقات الرد المختارة
    private final List<Path> replyAttachments = new ArrayList<>();
    private Stage stage;
    private Label replyAttachCountLbl;

    private MessageDetailView(HRNotification message) {
        this.message = message;
    }

    public static void show(Stage owner, HRNotification message) {
        new MessageDetailView(message).showDialog(owner);
    }

    private void showDialog(Stage owner) {
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
                buildReplySection()
        );

        Scene scene = new Scene(root, 580, 600);  // ✅ ارتفاع أكبر قليلاً
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);

        if (owner != null) {
            stage.setX(owner.getX() + (owner.getWidth() - 580) / 2);
            stage.setY(owner.getY() + (owner.getHeight() - 600) / 2);
        }

        notifService.markAsRead(message);
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
        title.setStyle(
                "-fx-font-size:14px;-fx-font-weight:700;" +
                        "-fx-text-fill:#1A1A1A;-fx-padding:0 0 0 8;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label closeBtn = new Label("X");
        styleCloseBtn(closeBtn);
        closeBtn.setOnMouseClicked(e -> stage.close());

        // سحب النافذة
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

        // صورة رمزية
        Circle avatarCircle = new Circle(24);
        avatarCircle.setFill(Color.web("#0F6E56"));
        Label avatarLbl = new Label(message.getAvatarInitials());
        avatarLbl.setStyle("-fx-font-size:14px;-fx-font-weight:700;-fx-text-fill:white;");
        StackPane avatarBox = new StackPane(avatarCircle, avatarLbl);
        avatarBox.setMinSize(48, 48);
        avatarBox.setMaxSize(48, 48);

        // تفاصيل المرسل
        Label senderLbl = new Label(
                message.getSenderName() != null ? message.getSenderName() : "مجهول");
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
        bodyLbl.setStyle(
                "-fx-font-size:13px;-fx-text-fill:#333333;" +
                        "-fx-line-spacing:4px;"
        );
        bodyLbl.setWrapText(true);
        bodyLbl.setMaxWidth(520);

        VBox wrapper = new VBox(bodyLbl);
        wrapper.setPadding(new Insets(4, 20, 12, 20));

        ScrollPane scroll = new ScrollPane(wrapper);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(160);        // ✅ ارتفاع مناسب
        scroll.setMaxHeight(260);         // ✅ حد أقصى للارتفاع
        scroll.setStyle("-fx-background-color:transparent;-fx-border-color:transparent;");
        VBox.setVgrow(scroll, Priority.SOMETIMES);
        return scroll;
    }

    // ===================== قسم المرفقات =====================
    private VBox buildAttachmentsSection() {
        if (!message.hasAttachments()) return new VBox(0);

        VBox section = new VBox(8);
        section.setPadding(new Insets(10, 20, 10, 20));
        section.setStyle(
                "-fx-background-color:#F8F8F8;" +
                        "-fx-border-color:#EBEBEB transparent #EBEBEB transparent;" +
                        "-fx-border-width:0.5 0 0.5 0;"
        );

        Label title = new Label(
                "المرفقات (" + message.getAttachments().size() + ")");
        title.setStyle("-fx-font-size:12px;-fx-font-weight:700;-fx-text-fill:#555555;");

        FlowPane flow = new FlowPane(8, 8);
        flow.setPrefWrapLength(520);
        flow.setHgap(8);
        flow.setVgap(8);
        // ✅ اجعل الـ FlowPane يأخذ مساحة كافية
        flow.setMinHeight(Region.USE_COMPUTED_SIZE);
        flow.setPrefHeight(Region.USE_COMPUTED_SIZE);

        message.getAttachments().forEach(att ->
                flow.getChildren().add(buildAttachmentCard(att)));

        section.getChildren().addAll(title, flow);

        // زر تحميل الكل
        if (message.getAttachments().size() > 1) {
            MFXButton downloadAll = new MFXButton("تحميل الكل");
            downloadAll.setStyle(
                    "-fx-background-color:#0F6E56;-fx-text-fill:white;" +
                            "-fx-font-size:12px;-fx-background-radius:6px;" +
                            "-fx-padding:5 14 5 14;-fx-cursor:hand;"
            );
            downloadAll.setOnAction(e ->
                    message.getAttachments().forEach(this::downloadAndOpen)
            );
            HBox btnRow = new HBox(downloadAll);
            btnRow.setAlignment(Pos.CENTER);
            section.getChildren().add(btnRow);
        }

        // ✅ اجعل القسم يأخذ مساحة مرنة
        VBox.setVgrow(flow, Priority.ALWAYS);
        return section;
    }

    private HBox buildAttachmentCard(HRNotification.Attachment att) {
        Label iconLbl = new Label(att.getIcon());
        iconLbl.setStyle(
                "-fx-font-size:10px;-fx-font-weight:700;-fx-text-fill:#0F6E56;" +
                        "-fx-background-color:#E6F5F1;-fx-background-radius:4px;" +
                        "-fx-padding:3 6 3 6;"
        );
        iconLbl.setMinWidth(Region.USE_PREF_SIZE);

        Label nameLbl = new Label(att.getFileName());
        nameLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#333333;");
        nameLbl.setMaxWidth(150);  // ✅ حد أقصى للاسم

        Label sizeLbl = new Label(att.getFormattedSize());
        sizeLbl.setStyle("-fx-font-size:10px;-fx-text-fill:#AAAAAA;");

        MFXButton dlBtn = new MFXButton("تحميل");
        dlBtn.setStyle(
                "-fx-background-color:transparent;-fx-text-fill:#185FA5;" +
                        "-fx-font-size:11px;-fx-cursor:hand;-fx-padding:0;"
        );
        dlBtn.setOnAction(e -> downloadAndOpen(att));

        VBox info = new VBox(2, nameLbl, sizeLbl);
        info.setAlignment(Pos.CENTER_LEFT);

        HBox card = new HBox(8, iconLbl, info, dlBtn);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(6, 10, 6, 10));  // ✅ تقليل الحشو
        card.setStyle(
                "-fx-background-color:#FFFFFF;-fx-background-radius:8px;" +
                        "-fx-border-color:#E0E0E0;-fx-border-width:0.5px;" +
                        "-fx-border-radius:8px;-fx-cursor:hand;"
        );
        card.setMaxWidth(Region.USE_PREF_SIZE);  // ✅ لا تتمدد
        card.setPrefWidth(Region.USE_COMPUTED_SIZE);

        card.setOnMouseEntered(e ->
                card.setStyle(card.getStyle().replace("#FFFFFF", "#F0FAF7")));
        card.setOnMouseExited(e ->
                card.setStyle(card.getStyle().replace("#F0FAF7", "#FFFFFF")));

        return card;
    }

    // ===================== قسم الرد =====================
    private HBox buildReplySection() {
        HBox replyBar = new HBox(8);
        replyBar.setAlignment(Pos.CENTER_LEFT);
        replyBar.setPadding(new Insets(10, 16, 12, 16));  // ✅ تقليل الحشو
        replyBar.setStyle(
                "-fx-border-color:#EBEBEB transparent transparent transparent;" +
                        "-fx-border-width:0.5 0 0 0;"
        );

        // حقل الرد
        MFXTextField replyField = new MFXTextField();
        replyField.setPromptText("اكتب ردك هنا...");
        replyField.setPrefWidth(Double.MAX_VALUE);
        replyField.setStyle("-fx-font-size:13px;");
        HBox.setHgrow(replyField, Priority.ALWAYS);

        // زر إضافة مرفق
        MFXButton attachBtn = new MFXButton("[+]");
        attachBtn.setStyle(
                "-fx-background-color:#F0F0F0;-fx-text-fill:#555555;" +
                        "-fx-font-size:13px;-fx-font-weight:700;" +
                        "-fx-background-radius:8px;-fx-cursor:hand;-fx-padding:6 10 6 10;"
        );
        attachBtn.setOnAction(e -> pickReplyAttachment());

        // عداد المرفقات المختارة
        replyAttachCountLbl = new Label();
        replyAttachCountLbl.setStyle("-fx-font-size:11px;-fx-text-fill:#0F6E56;");
        replyAttachCountLbl.setVisible(false);
        replyAttachCountLbl.setManaged(false);

        // زر إرسال
        MFXButton sendBtn = new MFXButton("إرسال");
        sendBtn.setStyle(
                "-fx-background-color:#0F6E56;-fx-text-fill:white;" +
                        "-fx-font-size:13px;-fx-font-weight:700;" +
                        "-fx-background-radius:8px;-fx-cursor:hand;-fx-padding:6 16 6 16;"
        );
        sendBtn.disableProperty().bind(replyField.textProperty().isEmpty());
        sendBtn.setOnAction(e -> sendReply(replyField, sendBtn));

        replyBar.getChildren().addAll(
                replyField, replyAttachCountLbl, attachBtn, sendBtn);
        return replyBar;
    }

    private void pickReplyAttachment() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("اختر ملف للإرفاق");
        java.io.File file = chooser.showOpenDialog(stage);
        if (file != null) {
            replyAttachments.add(Paths.get(file.getAbsolutePath()));
            replyAttachCountLbl.setText("[" + replyAttachments.size() + " مرفق]");
            replyAttachCountLbl.setVisible(true);
            replyAttachCountLbl.setManaged(true);
        }
    }

    private void sendReply(MFXTextField replyField, MFXButton sendBtn) {
        String replyText = replyField.getText().trim();
        if (replyText.isBlank()) return;

        sendBtn.setText("جاري الإرسال...");

        // استخرج message ID من actionTarget: "messages/123"
        Long parentId = extractMessageId(message.getActionTarget());

        msgService.replyToMessage(
                parentId,
                replyText,
                new ArrayList<>(replyAttachments),
                () -> {
                    // نجاح
                    replyField.clear();
                    replyAttachments.clear();
                    replyAttachCountLbl.setVisible(false);
                    replyAttachCountLbl.setManaged(false);
                    sendBtn.setText("إرسال");

                    stage.close();
                },
                err -> {
                    // خطأ
                    sendBtn.setText("إرسال");

                    showError("فشل الإرسال", err);
                }
        );
    }

    // ===================== مساعدات =====================
    private Long extractMessageId(String actionTarget) {
        if (actionTarget == null) return null;
        try {
            String[] parts = actionTarget.split("/");
            return Long.parseLong(parts[parts.length - 1]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Separator buildDivider() {
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color:#EBEBEB;");
        return sep;
    }

    private void styleCloseBtn(Label btn) {
        btn.setStyle(
                "-fx-font-size:14px;-fx-text-fill:#AAAAAA;-fx-cursor:hand;" +
                        "-fx-padding:4 8 4 8;-fx-background-radius:6px;"
        );
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-font-size:14px;-fx-text-fill:#CC3333;-fx-cursor:hand;" +
                        "-fx-padding:4 8 4 8;-fx-background-radius:6px;" +
                        "-fx-background-color:#FFE8E8;"
        ));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-font-size:14px;-fx-text-fill:#AAAAAA;-fx-cursor:hand;" +
                        "-fx-padding:4 8 4 8;-fx-background-radius:6px;"
        ));
    }

    private void showError(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.initOwner(stage);
        alert.show();
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

    /**
     * تحميل المرفق وحفظه في مكان يختاره المستخدم، ثم فتحه
     */
    private void downloadAndOpen(HRNotification.Attachment att) {
        // اختيار مكان الحفظ
        FileChooser chooser = new FileChooser();
        chooser.setTitle("حفظ المرفق");
        chooser.setInitialFileName(att.getFileName());

        // تحديد المجلد الافتراضي (مجلد المستندات)
        String userHome = System.getProperty("user.home");
        chooser.setInitialDirectory(new java.io.File(userHome + "/Documents"));

        java.io.File targetFile = chooser.showSaveDialog(stage);
        if (targetFile == null) return; // المستخدم ألغى

        Path targetPath = targetFile.toPath();

        // بدء التحميل
        String token = att.getDownloadToken();
        if (token == null || token.isEmpty()) {
            showError("خطأ", "لا يوجد رمز تحميل لهذا المرفق");
            return;
        }

        // تغيير نص الزر إلى "جاري التحميل..."
        // يمكننا إضافة تعطيل مؤقت للزر، لكننا سنكتفي بتغيير النص
        MFXButton dlBtn = new MFXButton("جاري...");
        dlBtn.setDisable(true);

        msgService.downloadAttachment(
                token,
                targetPath,
                () -> {
                    // نجاح التحميل
                    Platform.runLater(() -> {
                        dlBtn.setText("فتح");
                        dlBtn.setDisable(false);
                        dlBtn.setOnAction(e -> FileOpener.open(targetPath.toString()));
                        showError("تم التحميل", "تم تحميل الملف بنجاح");
                    });
                },
                err -> {
                    // فشل التحميل
                    Platform.runLater(() -> {
                        dlBtn.setText("تحميل");
                        dlBtn.setDisable(false);
                        showError("فشل التحميل", err);
                    });
                }
        );
    }
}
