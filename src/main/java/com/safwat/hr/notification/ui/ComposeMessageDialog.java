package com.safwat.hr.notification.ui;

import com.safwat.hr.notification.service.MessageClientService;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * =====================================================
 * ComposeMessageDialog — واجهة إرسال / رد رسالة
 * =====================================================
 * <p>
 * وضعان:
 * 1. رسالة جديدة:  ComposeMessageDialog.show(stage)
 * 2. رد على رسالة: ComposeMessageDialog.showReply(stage, recipient, subject, parentId)
 * <p>
 * في وضع الرد:
 * - حقل المستقبل والموضوع مملوءان ومقفلان
 * - الإرسال يذهب لـ replyToMessage() مع parentId
 */
public class ComposeMessageDialog {

    private final MessageClientService msgService = MessageClientService.getInstance();
    private final List<Path> attachments = new ArrayList<>();
    // وضع الرد
    private final Long parentId;   // null = رسالة جديدة
    private final boolean isReply;
    private Stage stage;
    private FlowPane attachmentsPane;
    private Label attachCountLbl;

    // ===================== Constructors =====================
    private ComposeMessageDialog(Long parentId) {
        this.parentId = parentId;
        this.isReply = parentId != null;
    }

    // ===================== Entry points =====================

    /**
     * رسالة جديدة فارغة
     */
    public static void show(Stage owner) {
        new ComposeMessageDialog(null).showDialog(owner, null, null);
    }

    /**
     * رسالة جديدة مع مستقبل محدد
     */
    public static void show(Stage owner, String recipientUsername) {
        new ComposeMessageDialog(null).showDialog(owner, recipientUsername, null);
    }

    /**
     * رد على رسالة — يُستدعى من MessageDetailView.
     *
     * @param recipientUsername اسم مرسل الرسالة الأصلية
     * @param subject           "رد: [الموضوع الأصلي]"
     * @param parentId          ID الرسالة الأصلية
     */
    public static void showReply(Stage owner, String recipientUsername,
                                 String subject, Long parentId) {
        new ComposeMessageDialog(parentId).showDialog(owner, recipientUsername, subject);
    }

    // ===================== بناء الواجهة =====================
    private void showDialog(Stage owner, String prefilledRecipient, String prefilledSubject) {
        stage = new Stage();
        stage.initStyle(StageStyle.UNDECORATED);
        stage.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) stage.initOwner(owner);

        // حقل المستقبل
        MFXTextField recipientField = new MFXTextField();
        recipientField.setPromptText("اسم المستخدم...");
        recipientField.setStyle("-fx-font-size:13px;");
        if (prefilledRecipient != null) recipientField.setText(prefilledRecipient);
        // في وضع الرد — حقل المستقبل للقراءة فقط
        if (isReply) recipientField.setEditable(false);

        // حقل الموضوع
        MFXTextField subjectField = new MFXTextField();
        subjectField.setPromptText("موضوع الرسالة...");
        subjectField.setStyle("-fx-font-size:13px;");
        if (prefilledSubject != null) subjectField.setText(prefilledSubject);
        // في وضع الرد — الموضوع محدد ويمكن تعديله
        if (isReply) subjectField.setEditable(true);

        // منطقة النص
        TextArea bodyArea = new TextArea();
        bodyArea.setPromptText(isReply ? "اكتب ردك هنا..." : "اكتب رسالتك هنا...");
        bodyArea.setStyle("-fx-font-size:13px;");
        bodyArea.setWrapText(true);
        bodyArea.setPrefHeight(180);
        VBox.setVgrow(bodyArea, Priority.ALWAYS);

        // المرفقات
        attachmentsPane = new FlowPane(8, 8);
        attachmentsPane.setPrefWrapLength(500);
        attachmentsPane.setVisible(false);
        attachmentsPane.setManaged(false);

        attachCountLbl = new Label();
        attachCountLbl.setStyle("-fx-font-size:11px;-fx-text-fill:#0F6E56;");
        attachCountLbl.setVisible(false);
        attachCountLbl.setManaged(false);

        // بناء الـ root
        VBox root = new VBox(0,
                buildTitleBar(),
                buildFormRow("المستقبل", recipientField),
                buildFormRow("الموضوع", subjectField),
                buildBodySection(bodyArea),
                buildAttachmentsSection(),
                buildActionsBar(recipientField, subjectField, bodyArea)
        );
        root.setStyle(
                "-fx-background-color:#FFFFFF;" +
                        "-fx-background-radius:12px;" +
                        "-fx-border-color:#E0E0E0;-fx-border-width:0.5px;" +
                        "-fx-border-radius:12px;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.22),24,0,0,8);"
        );

        Scene scene = new Scene(root, 560, 500);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);

        if (owner != null) {
            stage.setX(owner.getX() + (owner.getWidth() - 560) / 2);
            stage.setY(owner.getY() + (owner.getHeight() - 500) / 2);
        }

        stage.show();
        animateIn(root);

        // فوكس على الحقل المناسب
        if (isReply) bodyArea.requestFocus();
        else if (prefilledRecipient != null) subjectField.requestFocus();
        else recipientField.requestFocus();
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

        Label icon = new Label(isReply ? "↩" : "[+]");
        icon.setStyle("-fx-font-size:14px;-fx-font-weight:700;-fx-text-fill:#0F6E56;");

        Label title = new Label(isReply ? "رد على رسالة" : "رسالة جديدة");
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

    // ===================== صف الحقل =====================
    private HBox buildFormRow(String labelText, MFXTextField field) {
        Label lbl = new Label(labelText + ":");
        lbl.setStyle("-fx-font-size:12px;-fx-text-fill:#555555;-fx-font-weight:600;");
        lbl.setMinWidth(60);

        field.setPrefWidth(Double.MAX_VALUE);
        HBox.setHgrow(field, Priority.ALWAYS);

        HBox row = new HBox(10, lbl, field);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 16, 6, 16));
        row.setStyle("-fx-border-color:transparent transparent #F0F0F0 transparent;-fx-border-width:0 0 0.5 0;");
        return row;
    }

    // ===================== منطقة النص =====================
    private VBox buildBodySection(TextArea bodyArea) {
        VBox section = new VBox(4, bodyArea);
        section.setPadding(new Insets(10, 16, 8, 16));
        VBox.setVgrow(section, Priority.ALWAYS);
        return section;
    }

    // ===================== قسم المرفقات =====================
    private VBox buildAttachmentsSection() {
        VBox section = new VBox(6, attachCountLbl, attachmentsPane);
        section.setPadding(new Insets(0, 16, 6, 16));
        return section;
    }

    // ===================== شريط الأزرار =====================
    private HBox buildActionsBar(MFXTextField recipientField,
                                 MFXTextField subjectField,
                                 TextArea bodyArea) {
        HBox bar = new HBox(8);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(10, 16, 14, 16));
        bar.setStyle("-fx-border-color:#EBEBEB transparent transparent transparent;-fx-border-width:0.5 0 0 0;");

        MFXButton attachBtn = new MFXButton("[+] إرفاق");
        attachBtn.setStyle(
                "-fx-background-color:#F0F0F0;-fx-text-fill:#555555;" +
                        "-fx-font-size:12px;-fx-background-radius:8px;" +
                        "-fx-cursor:hand;-fx-padding:8 14 8 14;"
        );
        attachBtn.setOnAction(e -> pickAttachment());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        MFXButton cancelBtn = new MFXButton("إلغاء");
        cancelBtn.setStyle(
                "-fx-background-color:#F0F0F0;-fx-text-fill:#666666;" +
                        "-fx-font-size:13px;-fx-background-radius:8px;" +
                        "-fx-padding:8 18 8 18;-fx-cursor:hand;"
        );
        cancelBtn.setOnAction(e -> stage.close());

        MFXButton sendBtn = new MFXButton(isReply ? "إرسال الرد" : "إرسال");
        sendBtn.setStyle(
                "-fx-background-color:#0F6E56;-fx-text-fill:white;" +
                        "-fx-font-size:13px;-fx-font-weight:700;" +
                        "-fx-background-radius:8px;-fx-padding:8 22 8 22;-fx-cursor:hand;"
        );
        sendBtn.disableProperty().bind(
                recipientField.textProperty().isEmpty()
                        .or(subjectField.textProperty().isEmpty())
                        .or(bodyArea.textProperty().isEmpty())
        );
        sendBtn.setOnAction(e -> doSend(recipientField, subjectField, bodyArea, sendBtn));

        bar.getChildren().addAll(attachBtn, spacer, cancelBtn, sendBtn);
        return bar;
    }

    // ===================== اختيار مرفق =====================
    private void pickAttachment() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("اختر ملف للإرفاق");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("كل الملفات", "*.*"),
                new FileChooser.ExtensionFilter("PDF", "*.pdf"),
                new FileChooser.ExtensionFilter("Excel", "*.xlsx", "*.xls"),
                new FileChooser.ExtensionFilter("صور", "*.jpg", "*.jpeg", "*.png"),
                new FileChooser.ExtensionFilter("ZIP", "*.zip")
        );

        List<File> files = chooser.showOpenMultipleDialog(stage);
        if (files == null || files.isEmpty()) return;

        files.forEach(f -> {
            Path path = Paths.get(f.getAbsolutePath());
            attachments.add(path);
            attachmentsPane.getChildren().add(buildAttachmentChip(f.getName(), path));
        });

        attachCountLbl.setText("[" + attachments.size() + " مرفق]");
        attachCountLbl.setVisible(true);
        attachCountLbl.setManaged(true);
        attachmentsPane.setVisible(true);
        attachmentsPane.setManaged(true);
    }

    private HBox buildAttachmentChip(String name, Path path) {
        Label nameLbl = new Label(name.length() > 20 ? name.substring(0, 18) + "..." : name);
        nameLbl.setStyle("-fx-font-size:11px;-fx-text-fill:#333333;");

        Label removeBtn = new Label(" x");
        removeBtn.setStyle("-fx-font-size:11px;-fx-text-fill:#AA3333;-fx-cursor:hand;");
        removeBtn.setOnMouseClicked(e -> {
            attachments.remove(path);
            attachmentsPane.getChildren().remove(removeBtn.getParent());
            if (attachments.isEmpty()) {
                attachCountLbl.setVisible(false);
                attachCountLbl.setManaged(false);
                attachmentsPane.setVisible(false);
                attachmentsPane.setManaged(false);
            } else {
                attachCountLbl.setText("[" + attachments.size() + " مرفق]");
            }
        });

        HBox chip = new HBox(4, nameLbl, removeBtn);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.setPadding(new Insets(4, 8, 4, 8));
        chip.setStyle(
                "-fx-background-color:#E6F5F1;-fx-background-radius:12px;" +
                        "-fx-border-color:#0F6E56;-fx-border-width:0.5px;-fx-border-radius:12px;"
        );
        return chip;
    }

    // ===================== الإرسال =====================
    private void doSend(MFXTextField recipientField,
                        MFXTextField subjectField,
                        TextArea bodyArea,
                        MFXButton sendBtn) {

        String recipient = recipientField.getText().trim();
        String subject = subjectField.getText().trim();
        String body = bodyArea.getText().trim();

        if (recipient.isBlank() || subject.isBlank() || body.isBlank()) return;

        sendBtn.setText("جاري الإرسال...");
        sendBtn.setDisable(true);

        Runnable onSuccess = () -> stage.close();
        java.util.function.Consumer<String> onError = err -> {
            sendBtn.setText(isReply ? "إرسال الرد" : "إرسال");
            sendBtn.setDisable(false);
            showError("فشل الإرسال", err);
        };

        if (isReply && parentId != null) {
            // وضع الرد
            msgService.replyToMessage(
                    parentId, subject, body,
                    new ArrayList<>(attachments),
                    onSuccess, onError
            );
        } else {
            // رسالة جديدة
            msgService.sendMessage(
                    recipient, subject, body,
                    new ArrayList<>(attachments),
                    onSuccess, onError
            );
        }
    }

    // ===================== مساعدات =====================
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