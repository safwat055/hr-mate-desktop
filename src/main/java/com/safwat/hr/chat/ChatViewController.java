package com.safwat.hr.chat;


import com.safwat.hr.notification.model.HRNotification;
import com.safwat.hr.utils.ApiClient;
import javafx.application.Platform;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class ChatViewController implements Initializable {

    // ── FXML ──────────────────────────────────────────────────────────
    @FXML
    private ListView<ChatDTOs.ConversationSummaryDTO> conversationList;
    @FXML
    private TextField searchField;
    @FXML
    private Button btnNewConversation;

    @FXML
    private VBox emptyState;
    @FXML
    private VBox chatContent;

    @FXML
    private StackPane headerAvatar;
    @FXML
    private Label headerAvatarInitials;
    @FXML
    private Label headerConvName;
    @FXML
    private Label headerConvMeta;
    @FXML
    private Button btnConvInfo;

    @FXML
    private ScrollPane messagesScroll;
    @FXML
    private VBox messagesContainer;
    @FXML
    private HBox messagesLoading;

    @FXML
    private TextArea messageInput;
    @FXML
    private Button btnSend;
    @FXML
    private Button btnAttach;

    // ── State ─────────────────────────────────────────────────────────
    private ChatService chatService;
    private FilteredList<ChatDTOs.ConversationSummaryDTO> filteredConversations;
    private ChatDTOs.ConversationSummaryDTO currentConversation;

    // ═════════════════════════════════════════════════════════════════
    //  Initialize
    // ═════════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        chatService = ChatService.getInstance();

        // ١. Init ChatService (WebSocket + conversations)
        chatService.init(
                ApiClient.getUserName(),
                err -> HRNotification.builder()
                        .title("خطأ في الاتصال")
                        .message(err)
                        .type(HRNotification.NotificationType.SYSTEM)
                        .build()

        );

        // ٢. Bind conversations list
        filteredConversations = new FilteredList<>(chatService.getConversations(), p -> true);
        conversationList.setItems(filteredConversations);
        conversationList.setCellFactory(lv -> new ConversationCell());

        // ٣. Bind messages → UI
        chatService.getMessages().addListener(
                (javafx.collections.ListChangeListener<ChatDTOs.ChatMessageDTO>) change -> {
                    while (change.next()) {
                        if (change.wasAdded()) {
                            // change.getAddedSubList().forEach(this::addMessageBubble);
                        }
                    }
                }
        );

        // ٤. Scroll لأسفل لما تيجي رسالة جديدة
        chatService.setOnNewMessageInOpenConv(this::scrollToBottom);

        // ٥. Enter يرسل الرسالة (Shift+Enter سطر جديد)
        messageInput.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER && !event.isShiftDown()) {
                event.consume();
                onSendMessage();
            }
        });

        // ٦. حالة أولية
        showEmptyState();
    }

    // ═════════════════════════════════════════════════════════════════
    //  Conversation Selection
    // ═════════════════════════════════════════════════════════════════

    @FXML
    private void onConversationSelected() {
        ChatDTOs.ConversationSummaryDTO selected = conversationList.getSelectionModel().getSelectedItem();
        if (selected == null || selected.equals(currentConversation)) return;

        currentConversation = selected;
        showChatContent(selected);
        loadConversation(selected.getId());
    }

    private void showChatContent(ChatDTOs.ConversationSummaryDTO conv) {
        // Header
        headerAvatarInitials.setText(conv.getAvatarInitials());
        headerAvatar.setStyle("-fx-background-color: " + conv.getAvatarColor() + ";");
        headerConvName.setText(conv.getName());
        headerConvMeta.setText(conv.getType().equals("PRIVATE") ? "محادثة خاصة" :
                conv.getType().equals("GROUP") ? "مجموعة" : "بث عام");

        // إظهار زر التفاصيل للمجموعات فقط
        boolean isGroup = !conv.getType().equals("PRIVATE");
        btnConvInfo.setVisible(isGroup);
        btnConvInfo.setManaged(isGroup);

        // Switch visibility
        emptyState.setVisible(false);

        chatContent.setVisible(true);


        messagesContainer.getChildren().clear();
    }

    private void loadConversation(long convId) {
        setMessagesLoading(true);

        chatService.openConversation(convId, () -> {
            setMessagesLoading(false);
            // الرسائل اتحملت — addMessageBubble اتستدعت تلقائياً عبر الـ listener
            scrollToBottom();
        });
    }

    private void showEmptyState() {
        emptyState.setVisible(true);

        chatContent.setVisible(false);

    }

    // ═════════════════════════════════════════════════════════════════
    //  Messages
    // ═════════════════════════════════════════════════════════════════

    /**
     * يضيف MessageBubble في الـ messagesContainer.
     * يُستدعى من الـ listener على chatService.getMessages().
     */
    private void addMessageBubble(ChatDTOs.ChatMessageDTO msg) {
        MessageBubble bubble = new MessageBubble(msg);
        messagesContainer.getChildren().add(bubble);
    }

    private void scrollToBottom() {
        Platform.runLater(() ->
                messagesScroll.setVvalue(1.0)
        );
    }

    private void setMessagesLoading(boolean loading) {
        messagesLoading.setVisible(loading);
        messagesLoading.setManaged(loading);
    }

    // ═════════════════════════════════════════════════════════════════
    //  Send Message
    // ═════════════════════════════════════════════════════════════════

    @FXML
    private void onSendMessage() {
        String content = messageInput.getText();
        if (content == null || content.isBlank()) return;
        if (chatService.getOpenConversationId() == null) return;

        messageInput.clear();
        btnSend.setDisable(true);

        chatService.sendMessage(content, err -> {
            btnSend.setDisable(false);
            HRNotification.builder()
                    .title("فشل الإرسال")
                    .message(err)
                    .type(HRNotification.NotificationType.SYSTEM)
                    .build()
            ;
        });

        // إعادة تفعيل الزر بعد ثانية (لو الرسالة وصلت والـ WS رد)
        Platform.runLater(() -> btnSend.setDisable(false));
    }

    // ═════════════════════════════════════════════════════════════════
    //  Attach File
    // ═════════════════════════════════════════════════════════════════

    @FXML
    private void onAttachFile() {
        if (chatService.getOpenConversationId() == null) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("اختر ملفاً للإرسال");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("كل الملفات", "*.*"),
                new FileChooser.ExtensionFilter("صور", "*.png", "*.jpg", "*.jpeg"),
                new FileChooser.ExtensionFilter("مستندات", "*.pdf", "*.docx", "*.xlsx")
        );

        File file = chooser.showOpenDialog(btnAttach.getScene().getWindow());
        if (file == null) return;

        long convId = chatService.getOpenConversationId();
        String caption = messageInput.getText().trim();
        messageInput.clear();
        btnAttach.setDisable(true);

        ChatApiService.sendMessageWithFiles(convId, caption, List.of(file.toPath()))
                .thenAccept(res -> Platform.runLater(() -> {
                    btnAttach.setDisable(false);
                    if (!res.isSuccess()) {
                        HRNotification.builder()
                                .title("فشل إرسال الملف")
                                .message(res.getMessage())
                                .type(HRNotification.NotificationType.SYSTEM)
                                .build()
                        ;
                    }
                }));
    }

    // ═════════════════════════════════════════════════════════════════
    //  New Conversation
    // ═════════════════════════════════════════════════════════════════

    @FXML
    private void onNewConversation() {
        // SearchDialog للبحث عن مستخدمين
        NewConversationDialog dialog = new NewConversationDialog(
                btnNewConversation.getScene().getWindow()
        );
        dialog.showAndWait().ifPresent(userId -> {
            chatService.startPrivateConversation(
                    userId,
                    convId -> {
                        // ابحث عن المحادثة في القائمة وافتحها
                        chatService.getConversations().stream()
                                .filter(c -> c.getId().equals(convId))
                                .findFirst()
                                .ifPresent(conv -> {
                                    conversationList.getSelectionModel().select(conv);
                                    currentConversation = conv;
                                    showChatContent(conv);
                                    loadConversation(convId);
                                });
                    },
                    err -> HRNotification.builder()
                            .title("خطأ")
                            .message(err)
                            .type(HRNotification.NotificationType.SYSTEM)
                            .build()

            );
        });
    }

    // ═════════════════════════════════════════════════════════════════
    //  Search Conversations
    // ═════════════════════════════════════════════════════════════════

    @FXML
    private void onSearchConversations() {
        String query = searchField.getText().toLowerCase().trim();
        if (query.isEmpty()) {
            filteredConversations.setPredicate(p -> true);
        } else {
            filteredConversations.setPredicate(conv ->
                    conv.getName() != null &&
                            conv.getName().toLowerCase().contains(query)
            );
        }
    }

    // ═════════════════════════════════════════════════════════════════
    //  Conv Info (للمجموعات)
    // ═════════════════════════════════════════════════════════════════

    @FXML
    private void onShowConvInfo() {
        if (currentConversation == null) return;
        // TODO: عرض dialog بتفاصيل المجموعة والمشاركين
    }

    // ═════════════════════════════════════════════════════════════════
    //  Cleanup
    // ═════════════════════════════════════════════════════════════════

    /**
     * يُستدعى لما الـ view يُغلق.
     * اربطه بـ stage.setOnCloseRequest أو بزر الخروج.
     */
    public void onClose() {
        chatService.shutdown();
    }
}
