package com.safwat.hr.notification.ui;

import io.github.palexdev.materialfx.controls.MFXButton;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * =====================================================================
 * MessageComposer
 * =====================================================================
 * منطقة كتابة الرد أو الرسالة الجديدة.
 * تدعم وضعين:
 * 1. وضع الرد: يعرض سياق الموضوع + منطقة نص + زر إرسال
 * 2. وضع الرسالة الجديدة: يعرض المستلمين (chips) + الموضوع + منطقة نص + زر إرسال
 * <p>
 * تدعم إرفاق ملفات متعددة وعرضها.
 * تتوسع منطقة النص تلقائياً حسب المحتوى.
 */
public class MessageComposer extends VBox {

    private static final int MIN_ROWS = 3;
    private static final int MAX_ROWS = 10;
    private static final int APPROX_CHARS_PER_LINE = 55;

    private final FlowPane recipientsChips;
    private final MFXButton searchRecipientBtn;
    private final Label recipientsCountLbl;
    private final TextField subjectField;
    private final HBox recipientRow;
    private final HBox subjectRow;

    private final TextArea textArea;
    private final MFXButton sendBtn;
    private final MFXButton attachBtn;
    private final Label attachCountLbl;
    private final Label contextLbl;
    private final List<Path> attachments = new ArrayList<>();
    private final List<UserInfo> selectedRecipients = new ArrayList<>();

    private Consumer<String> onReply;
    private Consumer<String> onNewMessage;
    private Runnable onAttach;
    private Runnable onSearchRecipients;

    private boolean isReplyMode = true;

    /**
     * إنشاء محرر الرسائل مع تهيئة كل العناصر.
     */
    public MessageComposer() {
        setSpacing(10);
        setPadding(new Insets(12, 20, 16, 20));
        setStyle(
                "-fx-background-color:#FFFFFF;" +
                        "-fx-border-color:#EBEBEB transparent transparent transparent;" +
                        "-fx-border-width:1 0 0 0;"
        );

        contextLbl = new Label();
        contextLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#555555;-fx-font-weight:600;");
        contextLbl.setVisible(false);
        contextLbl.setManaged(false);

        Label recipientLbl = new Label("المستلمون:");
        recipientLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#555555;-fx-font-weight:600;");
        recipientLbl.setMinWidth(60);

        recipientsChips = new FlowPane(6, 6);
        recipientsChips.setPrefWrapLength(300);
        recipientsChips.setStyle("-fx-padding:4 0;");
        HBox.setHgrow(recipientsChips, Priority.ALWAYS);

        searchRecipientBtn = new MFXButton("🔍 اختيار مستلمين");
        searchRecipientBtn.setStyle(
                "-fx-background-color:#E6F1FB;-fx-text-fill:#185FA5;" +
                        "-fx-font-size:12px;-fx-background-radius:6px;" +
                        "-fx-padding:6 14 6 14;-fx-cursor:hand;"
        );
        searchRecipientBtn.setOnAction(e -> {
            if (onSearchRecipients != null) onSearchRecipients.run();
        });
        searchRecipientBtn.setTooltip(new javafx.scene.control.Tooltip("اختيار أكثر من مستلم"));

        recipientsCountLbl = new Label("(0)");
        recipientsCountLbl.setStyle("-fx-font-size:11px;-fx-text-fill:#0F6E56;-fx-font-weight:600;");

        recipientRow = new HBox(8, recipientLbl, recipientsChips, searchRecipientBtn, recipientsCountLbl);
        recipientRow.setAlignment(Pos.CENTER_LEFT);
        recipientRow.setVisible(false);
        recipientRow.setManaged(false);

        Label subjectLbl = new Label("الموضوع:");
        subjectLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#555555;-fx-font-weight:600;");
        subjectLbl.setMinWidth(60);

        subjectField = new TextField();
        subjectField.setPromptText("موضوع الرسالة...");
        subjectField.setStyle("-fx-font-size:13px;-fx-background-radius:6px;");
        HBox.setHgrow(subjectField, Priority.ALWAYS);

        subjectRow = new HBox(8, subjectLbl, subjectField);
        subjectRow.setAlignment(Pos.CENTER_LEFT);
        subjectRow.setVisible(false);
        subjectRow.setManaged(false);

        textArea = new TextArea();
        textArea.setPromptText("اكتب ردك هنا...");
        textArea.setWrapText(true);
        textArea.setPrefRowCount(MIN_ROWS);
        textArea.setStyle("-fx-font-size:13px;-fx-background-radius:8px;");
        VBox.setVgrow(textArea, Priority.NEVER);

        textArea.textProperty().addListener((obs, old, nw) -> {
            updateSendButton();
            adjustTextAreaHeight();
        });

        attachCountLbl = new Label();
        attachCountLbl.setStyle("-fx-font-size:11px;-fx-text-fill:#0F6E56;");
        attachCountLbl.setVisible(false);
        attachCountLbl.setManaged(false);

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

    /**
     * تعديل ارتفاع منطقة النص تلقائياً حسب عدد الأسطر.
     */
    private void adjustTextAreaHeight() {
        String text = textArea.getText();
        if (text == null) text = "";

        String[] paragraphs = text.split("\n", -1);
        int totalLines = 0;

        for (String para : paragraphs) {
            totalLines++;
            if (para.length() > APPROX_CHARS_PER_LINE) {
                totalLines += (para.length() - 1) / APPROX_CHARS_PER_LINE;
            }
        }

        int newRows = Math.min(Math.max(MIN_ROWS, totalLines), MAX_ROWS);
        textArea.setPrefRowCount(newRows);
    }

    /**
     * تحديث حالة زر الإرسال حسب الوضع والمحتوى.
     */
    private void updateSendButton() {
        boolean hasText = textArea.getText() != null && !textArea.getText().isBlank();
        if (isReplyMode) {
            sendBtn.setDisable(!hasText);
        } else {
            boolean hasRecipients = !selectedRecipients.isEmpty();
            boolean hasSubject = subjectField.getText() != null && !subjectField.getText().isBlank();
            sendBtn.setDisable(!hasText || !hasRecipients || !hasSubject);
        }
    }

    /**
     * تنفيذ الإرسال حسب الوضع الحالي (رد أو رسالة جديدة).
     */
    private void doSend() {
        String text = textArea.getText().trim();
        if (text.isBlank()) return;

        if (isReplyMode) {
            if (onReply != null) {
                onReply.accept(text);
            }
        } else {
            String subject = subjectField.getText().trim();
            if (selectedRecipients.isEmpty() || subject.isBlank()) return;
            if (onNewMessage != null) {
                onNewMessage.accept(text);
            }
        }

        textArea.clear();
        textArea.setPrefRowCount(MIN_ROWS);
        clearAttachments();
    }

    public void setOnReply(Consumer<String> handler) {
        this.onReply = handler;
    }

    public void setOnNewMessage(Consumer<String> handler) {
        this.onNewMessage = handler;
    }

    public void setOnAttach(Runnable handler) {
        this.onAttach = handler;
    }

    public void setOnSearchRecipients(Runnable handler) {
        this.onSearchRecipients = handler;
    }

    /**
     * تفعيل وضع الرد وإظهار سياق الموضوع.
     *
     * @param subject موضوع الرسالة المردود عليها
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
        textArea.setPrefRowCount(MIN_ROWS);
        updateSendButton();
    }

    /**
     * تفعيل وضع الرسالة الجديدة وإظهار حقول المستلمين والموضوع.
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
        textArea.setPrefRowCount(MIN_ROWS);
        updateSendButton();
    }

    // ===================== Recipients (UserInfo Chips) =====================

    /**
     * إضافة مستلم جديد.
     *
     * @param user بيانات المستخدم
     */
    public void addRecipient(UserInfo user) {
        if (user == null) return;
        if (!selectedRecipients.contains(user)) {
            selectedRecipients.add(user);
            recipientsChips.getChildren().add(buildChip(user));
            recipientsCountLbl.setText("(" + selectedRecipients.size() + ")");
            updateSendButton();
        }
    }

    /**
     * إزالة مستلم.
     *
     * @param user المستخدم المراد إزالته
     */
    public void removeRecipient(UserInfo user) {
        selectedRecipients.remove(user);
        setRecipients(new ArrayList<>(selectedRecipients));
    }

    /**
     * ترجع أسماء المستخدمين المختارين.
     *
     * @return قائمة بأسماء المستخدمين
     */
    public List<String> getRecipientUsernames() {
        return selectedRecipients.stream()
                .map(UserInfo::getUsername)
                .toList();
    }

    public List<UserInfo> getRecipients() {
        return new ArrayList<>(selectedRecipients);
    }

    /**
     * تعيين قائمة المستلمين بالكامل.
     *
     * @param users قائمة المستخدمين
     */
    public void setRecipients(List<UserInfo> users) {
        selectedRecipients.clear();
        if (users != null) {
            selectedRecipients.addAll(users);
        }
        recipientsChips.getChildren().clear();

        for (UserInfo u : selectedRecipients) {
            HBox chip = buildChip(u);
            recipientsChips.getChildren().add(chip);
        }

        recipientsCountLbl.setText("(" + selectedRecipients.size() + ")");
        updateSendButton();
    }

    /**
     * بناء chip لعرض المستلم.
     * يعرض الاسم الحقيقي فوق واسم المستخدم تحته.
     *
     * @param user بيانات المستخدم
     * @return HBox يمثل الـ chip
     */
    private HBox buildChip(UserInfo user) {
        Label displayNameLbl = new Label(user.getDisplayName());
        displayNameLbl.setStyle("-fx-font-size:11px;-fx-font-weight:600;-fx-text-fill:#185FA5;");

        Label usernameLbl = new Label("@" + user.getUsername());
        usernameLbl.setStyle("-fx-font-size:9px;-fx-text-fill:#888888;");

        VBox namesBox = new VBox(0, displayNameLbl, usernameLbl);
        namesBox.setAlignment(Pos.CENTER_RIGHT);

        MFXButton remove = new MFXButton("✕");
        remove.setStyle(
                "-fx-background-color:transparent;-fx-text-fill:#999;" +
                        "-fx-font-size:10px;-fx-padding:0 2 0 6;-fx-cursor:hand;"
        );
        remove.setOnAction(e -> removeRecipient(user));

        HBox chip = new HBox(4, namesBox, remove);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.setStyle(
                "-fx-background-color:#E6F1FB;" +
                        "-fx-background-radius:10px;" +
                        "-fx-padding:4 10 4 10;"
        );
        return chip;
    }

    // ===================== Attachments =====================

    /**
     * إضافة ملف مرفق.
     *
     * @param file مسار الملف
     */
    public void addAttachment(Path file) {
        attachments.add(file);
        attachCountLbl.setText("[" + attachments.size() + " مرفق]");
        attachCountLbl.setVisible(true);
        attachCountLbl.setManaged(true);
    }

    /**
     * ترجع قائمة الملفات المرفقة.
     *
     * @return قائمة بالمسارات
     */
    public List<Path> getAttachments() {
        return new ArrayList<>(attachments);
    }

    /**
     * مسح المرفقات.
     */
    public void clearAttachments() {
        attachments.clear();
        attachCountLbl.setVisible(false);
        attachCountLbl.setManaged(false);
    }

    /**
     * ترجع نص الموضوع.
     *
     * @return نص الموضوع
     */
    public String getSubject() {
        return subjectField.getText() != null ? subjectField.getText().trim() : "";
    }

    /**
     * مسح كل الحقول والمستلمين والمرفقات.
     */
    public void clearAll() {
        textArea.clear();
        textArea.setPrefRowCount(MIN_ROWS);
        selectedRecipients.clear();
        recipientsChips.getChildren().clear();
        recipientsCountLbl.setText("(0)");
        subjectField.clear();
        clearAttachments();
        updateSendButton();
    }
}