package com.safwat.hr.notification.ui;

import com.safwat.hr.notification.model.HRNotification;
import com.safwat.hr.notification.service.NotificationService;
import com.safwat.hr.notification.util.FileOpener;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.*;
import javafx.util.Duration;
import javafx.animation.*;

/**
 * واجهة عرض الرسالة الكاملة.
 * تُفتح كـ Dialog مستقل عند الضغط على "فتح الرسالة" في الـ Panel أو Toast.
 *
 * تدعم:
 *   - عرض نص الرسالة الكامل (messageBody)
 *   - عرض المرفقات مع أزرار تحميل فردية
 *   - زر رد بسيط
 *   - تعليم مقروء تلقائياً عند الفتح
 */
public class MessageDetailView {

    private final HRNotification message;
    private final NotificationService service = NotificationService.getInstance();
    private Stage stage;

    public MessageDetailView(HRNotification message) {
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

        Scene scene = new Scene(root, 580, 520);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);

        if (owner != null) {
            stage.setX(owner.getX() + (owner.getWidth()  - 580) / 2);
            stage.setY(owner.getY() + (owner.getHeight() - 520) / 2);
        }

        // تعليم مقروء تلقائياً
        service.markAsRead(message);

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
        icon.setStyle("-fx-font-size:13px;-fx-font-weight:700;-fx-text-fill:#0F6E56;");

        Label title = new Label("رسالة جديدة");
        title.setStyle(
            "-fx-font-size:14px;-fx-font-weight:700;" +
            "-fx-text-fill:#1A1A1A;-fx-padding:0 0 0 8;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label closeBtn = new Label("X");
        closeBtn.setStyle(
            "-fx-font-size:14px;-fx-text-fill:#AAAAAA;-fx-cursor:hand;" +
            "-fx-padding:4 8 4 8;-fx-background-radius:6px;"
        );
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle(
            "-fx-font-size:14px;-fx-text-fill:#CC3333;-fx-cursor:hand;" +
            "-fx-padding:4 8 4 8;-fx-background-radius:6px;" +
            "-fx-background-color:#FFE8E8;"
        ));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle(
            "-fx-font-size:14px;-fx-text-fill:#AAAAAA;-fx-cursor:hand;" +
            "-fx-padding:4 8 4 8;-fx-background-radius:6px;"
        ));
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

    // ===================== رأس الرسالة (المرسل + الموضوع) =====================
    private HBox buildMessageHeader() {
        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 20, 14, 20));

        // صورة رمزية
        Circle avatarCircle = new Circle(24);
        avatarCircle.setFill(Color.web("#0F6E56"));
        Label avatarLbl = new Label(message.getAvatarInitials());
        avatarLbl.setStyle(
            "-fx-font-size:14px;-fx-font-weight:700;-fx-text-fill:white;"
        );
        StackPane avatarBox = new StackPane(avatarCircle, avatarLbl);
        avatarBox.setMinSize(48, 48);
        avatarBox.setMaxSize(48, 48);

        // تفاصيل المرسل
        Label senderLbl = new Label(
            message.getSenderName() != null ? message.getSenderName() : "مجهول"
        );
        senderLbl.setStyle(
            "-fx-font-size:15px;-fx-font-weight:700;-fx-text-fill:#1A1A1A;"
        );

        Label subjectLbl = new Label("الموضوع: " + message.getTitle());
        subjectLbl.setStyle("-fx-font-size:13px;-fx-text-fill:#555555;");

        Label timeLbl = new Label(message.getFormattedTime());
        timeLbl.setStyle("-fx-font-size:11px;-fx-text-fill:#AAAAAA;");

        VBox senderInfo = new VBox(3, senderLbl, subjectLbl, timeLbl);
        senderInfo.setAlignment(Pos.CENTER_LEFT);

        header.getChildren().addAll(avatarBox, senderInfo);
        return header;
    }

    // ===================== محتوى الرسالة =====================
    private ScrollPane buildMessageBody() {
        // نص الرسالة الكامل أو الـ preview لو مفيش body
        String body = message.getMessageBody() != null && !message.getMessageBody().isBlank()
            ? message.getMessageBody()
            : message.getMessage();

        Label bodyLbl = new Label(body);
        bodyLbl.setStyle(
            "-fx-font-size:13px;-fx-text-fill:#333333;" +
            "-fx-line-spacing:4px;"
        );
        bodyLbl.setWrapText(true);
        bodyLbl.setMaxWidth(520);

        VBox bodyWrapper = new VBox(bodyLbl);
        bodyWrapper.setPadding(new Insets(4, 20, 12, 20));

        ScrollPane scroll = new ScrollPane(bodyWrapper);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(200);
        scroll.setStyle(
            "-fx-background-color:transparent;" +
            "-fx-border-color:transparent;"
        );
        VBox.setVgrow(scroll, Priority.ALWAYS);
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

        Label title = new Label("المرفقات (" + message.getAttachments().size() + ")");
        title.setStyle("-fx-font-size:12px;-fx-font-weight:700;-fx-text-fill:#555555;");

        FlowPane attachFlow = new FlowPane(8, 8);
        attachFlow.setPrefWrapLength(520);

        for (HRNotification.Attachment att : message.getAttachments()) {
            HBox attCard = buildAttachmentCard(att);
            attachFlow.getChildren().add(attCard);
        }

        // زر تحميل الكل لو أكثر من مرفق
        if (message.getAttachments().size() > 1) {
            MFXButton downloadAll = new MFXButton("تحميل الكل");
            downloadAll.setStyle(
                "-fx-background-color:#0F6E56;-fx-text-fill:white;" +
                "-fx-font-size:12px;-fx-background-radius:6px;" +
                "-fx-padding:5 14 5 14;-fx-cursor:hand;"
            );
            downloadAll.setOnAction(e ->
                message.getAttachments().forEach(a -> FileOpener.open(a.getFilePath()))
            );
            HBox btnRow = new HBox(downloadAll);
            btnRow.setAlignment(Pos.CENTER_LEFT);
            section.getChildren().addAll(title, attachFlow, btnRow);
        } else {
            section.getChildren().addAll(title, attachFlow);
        }

        return section;
    }

    private HBox buildAttachmentCard(HRNotification.Attachment att) {
        Label iconLbl = new Label(att.getIcon());
        iconLbl.setStyle(
            "-fx-font-size:11px;-fx-font-weight:700;-fx-text-fill:#0F6E56;" +
            "-fx-background-color:#E6F5F1;-fx-background-radius:4px;" +
            "-fx-padding:3 6 3 6;"
        );

        Label nameLbl = new Label(att.getFileName());
        nameLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#333333;");
        nameLbl.setMaxWidth(180);

        Label sizeLbl = new Label(att.getFormattedSize());
        sizeLbl.setStyle("-fx-font-size:11px;-fx-text-fill:#AAAAAA;");

        MFXButton dlBtn = new MFXButton("تحميل");
        dlBtn.setStyle(
            "-fx-background-color:transparent;-fx-text-fill:#185FA5;" +
            "-fx-font-size:11px;-fx-cursor:hand;-fx-padding:0;"
        );
        dlBtn.setOnAction(e -> FileOpener.open(att.getFilePath()));

        VBox info = new VBox(2, nameLbl, sizeLbl);
        info.setAlignment(Pos.CENTER_LEFT);

        HBox card = new HBox(8, iconLbl, info, dlBtn);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(8, 12, 8, 10));
        card.setStyle(
            "-fx-background-color:#FFFFFF;" +
            "-fx-background-radius:8px;" +
            "-fx-border-color:#E0E0E0;-fx-border-width:0.5px;" +
            "-fx-border-radius:8px;" +
            "-fx-cursor:hand;"
        );
        card.setOnMouseEntered(e ->
            card.setStyle(card.getStyle().replace("#FFFFFF", "#F0FAF7")));
        card.setOnMouseExited(e ->
            card.setStyle(card.getStyle().replace("#F0FAF7", "#FFFFFF")));

        return card;
    }

    // ===================== قسم الرد =====================
    private HBox buildReplySection() {
        HBox replyBar = new HBox(10);
        replyBar.setAlignment(Pos.CENTER_LEFT);
        replyBar.setPadding(new Insets(12, 16, 14, 16));
        replyBar.setStyle(
            "-fx-border-color:#EBEBEB transparent transparent transparent;" +
            "-fx-border-width:0.5 0 0 0;"
        );

        MFXTextField replyField = new MFXTextField();
        replyField.setPromptText("اكتب ردك هنا...");
        replyField.setPrefWidth(Double.MAX_VALUE);
        replyField.setStyle("-fx-font-size:13px;");
        HBox.setHgrow(replyField, Priority.ALWAYS);

        // زر مرفق
        MFXButton attachBtn = new MFXButton("[+]");
        attachBtn.setStyle(
            "-fx-background-color:#F0F0F0;-fx-text-fill:#555555;" +
            "-fx-font-size:13px;-fx-font-weight:700;" +
            "-fx-background-radius:8px;-fx-cursor:hand;-fx-padding:8 12 8 12;"
        );
        attachBtn.setOnAction(e -> {
            // في التطبيق الحقيقي: FileChooser لاختيار مرفق
            System.out.println("[Reply] إضافة مرفق");
        });

        // زر إرسال
        MFXButton sendBtn = new MFXButton("إرسال");
        sendBtn.setStyle(
            "-fx-background-color:#0F6E56;-fx-text-fill:white;" +
            "-fx-font-size:13px;-fx-font-weight:700;" +
            "-fx-background-radius:8px;-fx-cursor:hand;-fx-padding:8 20 8 20;"
        );
        sendBtn.disableProperty().bind(replyField.textProperty().isEmpty());
        sendBtn.setOnAction(e -> {
            String replyText = replyField.getText().trim();
            if (!replyText.isBlank()) {
                // في التطبيق الحقيقي: MessageService.getInstance().reply(message, replyText)
                System.out.println("[Reply] إرسال رد: " + replyText);
                replyField.clear();
                stage.close();
            }
        });

        replyBar.getChildren().addAll(replyField, attachBtn, sendBtn);
        return replyBar;
    }

    // ===================== مساعدات =====================
    private Separator buildDivider() {
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color:#EBEBEB;");
        return sep;
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
