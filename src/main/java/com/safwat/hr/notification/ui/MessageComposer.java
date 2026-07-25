package com.safwat.hr.notification.ui;

import io.github.palexdev.materialfx.controls.MFXButton;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * =====================================================
 * MessageComposer — منطقة كتابة الرد أو رسالة جديدة
 * =====================================================
 * <p>
 * وضعان:
 * 1. Reply: بيظهر context (الموضوع) + text area + send
 * 2. New Message: بيظهر recipient + subject + text area + send
 */
public class MessageComposer extends VBox {

    // Fields for NEW MESSAGE mode
    private final TextField recipientField;
    private final TextField subjectField;
    private final HBox recipientRow;
    private final HBox subjectRow;

    // Common fields
    private final TextArea textArea;
    private final MFXButton sendBtn;
    private final MFXButton attachBtn;
    private final Label attachCountLbl;
    private final Label contextLbl;
    private final List<Path> attachments = new ArrayList<>();

    private Consumer<String> onReply;
    private BiConsumer<String, String> onNewMessage; // recipient, text
    private Runnable onAttach;

    private boolean isReplyMode = true;

    public MessageComposer() {
        setSpacing(10);
        setPadding(new Insets(12, 20, 16, 20));
        setStyle(
                "-fx-background-color:#FFFFFF;" +
                        "-fx-border-color:#EBEBEB transparent transparent transparent;" +
                        "-fx-border-width:1 0 0 0;"
        );

        // === Context label (for reply) ===
        contextLbl = new Label();
        contextLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#555555;-fx-font-weight:600;");
        contextLbl.setVisible(false);
        contextLbl.setManaged(false);

        // === Recipient field (NEW MESSAGE only) ===
        Label recipientLbl = new Label("المستقبل:");
        recipientLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#555555;-fx-font-weight:600;");
        recipientLbl.setMinWidth(55);

        recipientField = new TextField();
        recipientField.setPromptText("اسم المستخدم...");
        recipientField.setStyle("-fx-font-size:13px;-fx-background-radius:6px;");
        HBox.setHgrow(recipientField, Priority.ALWAYS);

        recipientRow = new HBox(8, recipientLbl, recipientField);
        recipientRow.setAlignment(Pos.CENTER_LEFT);
        recipientRow.setVisible(false);
        recipientRow.setManaged(false);

        // === Subject field (NEW MESSAGE only) ===
        Label subjectLbl = new Label("الموضوع:");
        subjectLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#555555;-fx-font-weight:600;");
        subjectLbl.setMinWidth(55);

        subjectField = new TextField();
        subjectField.setPromptText("موضوع الرسالة...");
        subjectField.setStyle("-fx-font-size:13px;-fx-background-radius:6px;");
        HBox.setHgrow(subjectField, Priority.ALWAYS);

        subjectRow = new HBox(8, subjectLbl, subjectField);
        subjectRow.setAlignment(Pos.CENTER_LEFT);
        subjectRow.setVisible(false);
        subjectRow.setManaged(false);

        // === Text area ===
        textArea = new TextArea();
        textArea.setPromptText("اكتب ردك هنا...");
        textArea.setWrapText(true);
        textArea.setPrefRowCount(3);
        textArea.setMaxHeight(120);
        textArea.setStyle("-fx-font-size:13px;-fx-background-radius:8px;");
        VBox.setVgrow(textArea, Priority.NEVER);

        // === Attachments ===
        attachCountLbl = new Label();
        attachCountLbl.setStyle("-fx-font-size:11px;-fx-text-fill:#0F6E56;");
        attachCountLbl.setVisible(false);
        attachCountLbl.setManaged(false);

        // === Buttons ===
        attachBtn = new MFXButton("📎 إرفاق");
        attachBtn.setStyle(
                "-fx-background-color:#F0F0F0;-fx-text-fill:#555555;" +
                        "-fx-font-size:12px;-fx-background-radius:8px;" +
                        "-fx-padding:8 16 8 16;-fx-cursor:hand;"
        );
        attachBtn.setOnAction(e -> {
            if (onAttach != null) onAttach.run();
        });

        sendBtn = new MFXButton("إرسال ↩");
        sendBtn.setStyle(
                "-fx-background-color:#0F6E56;-fx-text-fill:white;" +
                        "-fx-font-size:13px;-fx-font-weight:700;" +
                        "-fx-background-radius:8px;-fx-padding:8 24 8 24;-fx-cursor:hand;"
        );
        sendBtn.setOnAction(e -> doSend());
        sendBtn.setDisable(true);

        // Enable/disable send
        textArea.textProperty().addListener((obs, old, nw) -> updateSendButton());
        recipientField.textProperty().addListener((obs, old, nw) -> updateSendButton());
        subjectField.textProperty().addListener((obs, old, nw) -> updateSendButton());

        HBox actions = new HBox(10, attachBtn, attachCountLbl, sendBtn);
        actions.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(
                contextLbl,
                recipientRow,
                subjectRow,
                textArea,
                actions
        );
    }

    private void updateSendButton() {
        boolean hasText = textArea.getText() != null && !textArea.getText().isBlank();
        if (isReplyMode) {
            sendBtn.setDisable(!hasText);
        } else {
            boolean hasRecipient = recipientField.getText() != null && !recipientField.getText().isBlank();
            boolean hasSubject = subjectField.getText() != null && !subjectField.getText().isBlank();
            sendBtn.setDisable(!hasText || !hasRecipient || !hasSubject);
        }
    }

    private void doSend() {
        String text = textArea.getText().trim();
        if (text.isBlank()) return;

        if (isReplyMode) {
            if (onReply != null) {
                onReply.accept(text);
            }
        } else {
            String recipient = recipientField.getText().trim();
            String subject = subjectField.getText().trim();
            if (recipient.isBlank() || subject.isBlank()) return;
            if (onNewMessage != null) {
                onNewMessage.accept(recipient, text);
            }
        }

        textArea.clear();
        clearAttachments();
    }

    public void setOnReply(Consumer<String> handler) {
        this.onReply = handler;
    }

    public void setOnNewMessage(BiConsumer<String, String> handler) {
        this.onNewMessage = handler;
    }

    public void setOnAttach(Runnable handler) {
        this.onAttach = handler;
    }

    /**
     * وضع الرد — بيخفي recipient/subject وبيظهر context
     */
    public void setReplyMode(String subject) {
        isReplyMode = true;

        contextLbl.setText("رد على: " + (subject != null ? subject : "(بدون موضوع)"));
        contextLbl.setVisible(true);
        contextLbl.setManaged(true);

        recipientRow.setVisible(false);
        recipientRow.setManaged(false);

        subjectRow.setVisible(false);
        subjectRow.setManaged(false);

        textArea.setPromptText("اكتب ردك هنا...");
        updateSendButton();
    }

    /**
     * وضع رسالة جديدة — بيظهر recipient + subject
     */
    public void setNewMessageMode() {
        isReplyMode = false;

        contextLbl.setVisible(false);
        contextLbl.setManaged(false);

        recipientRow.setVisible(true);
        recipientRow.setManaged(true);

        subjectRow.setVisible(true);
        subjectRow.setManaged(true);

        textArea.setPromptText("اكتب رسالتك هنا...");
        updateSendButton();
    }

    public void addAttachment(Path file) {
        attachments.add(file);
        attachCountLbl.setText("[" + attachments.size() + " مرفق]");
        attachCountLbl.setVisible(true);
        attachCountLbl.setManaged(true);
    }

    public List<Path> getAttachments() {
        return new ArrayList<>(attachments);
    }

    public void clearAttachments() {
        attachments.clear();
        attachCountLbl.setVisible(false);
        attachCountLbl.setManaged(false);
    }

    public String getSubject() {
        return subjectField.getText() != null ? subjectField.getText().trim() : "";
    }

    public void clearAll() {
        textArea.clear();
        recipientField.clear();
        subjectField.clear();
        clearAttachments();
    }
}