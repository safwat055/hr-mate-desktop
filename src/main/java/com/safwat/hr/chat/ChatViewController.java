package com.safwat.hr.chat;


import com.safwat.hr.notification.model.HRNotification;
import com.safwat.hr.notification.service.NotificationService;
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
import javafx.stage.Window;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class ChatViewController implements Initializable {

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

    private ChatService chatService;
    private FilteredList<ChatDTOs.ConversationSummaryDTO> filteredConversations;
    private ChatDTOs.ConversationSummaryDTO currentConversation;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        chatService = ChatService.getInstance();

        // ١. Init ChatService
        chatService.init(
                ApiClient.getUserName(),
                err -> Platform.runLater(() ->
                        NotificationService.getInstance().send(
                                HRNotification.builder()
                                        .title("خطأ في الاتصال")
                                        .message(err)
                                        .type(HRNotification.NotificationType.SYSTEM)
                                        .build()
                        )
                )
        );

        // ٢. Bind conversations list
        filteredConversations = new FilteredList<>(chatService.getConversations(), p -> true);
        conversationList.setItems(filteredConversations);
        conversationList.setCellFactory(lv -> new ConversationCell());

        // ٣. Bind messages → UI — ✅ مش commented
        chatService.getMessages().addListener(
                (javafx.collections.ListChangeListener<ChatDTOs.ChatMessageDTO>) change -> {
                    while (change.next()) {
                        if (change.wasAdded()) {
                            change.getAddedSubList().forEach(this::addMessageBubble);
                        }
                    }
                }
        );

        // ٤. Scroll لأسفل لما تيجي رسالة جديدة
        chatService.setOnNewMessageInOpenConv(this::scrollToBottom);

        // ٥. Enter يرسل، Shift+Enter سطر جديد
        messageInput.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER && !event.isShiftDown()) {
                event.consume();
                onSendMessage();
            }
        });

        // ٦. حالة أولية
        //  showEmptyState();
    }

    // ══════════════════════════════════════════════════════
    //  Conversation Selection
    // ══════════════════════════════════════════════════════

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
        headerAvatarInitials.setText(conv.getAvatarInitials() != null ? conv.getAvatarInitials() : "?");
        String color = conv.getAvatarColor() != null ? conv.getAvatarColor() : "#185FA5";
        headerAvatar.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 20;");
        headerConvName.setText(conv.getName() != null ? conv.getName() : "");
        headerConvMeta.setText(
                "PRIVATE".equals(conv.getType()) ? "محادثة خاصة" :
                        "GROUP".equals(conv.getType()) ? "مجموعة" : "بث عام"
        );

        boolean isGroup = !"PRIVATE".equals(conv.getType());
        btnConvInfo.setVisible(isGroup);
        btnConvInfo.setManaged(isGroup);

        // ✅ setManaged ضروري عشان الـ layout يتعدل
        emptyState.setVisible(false);
        emptyState.setManaged(false);
        chatContent.setVisible(true);
        chatContent.setManaged(true);

        messagesContainer.getChildren().clear();
    }

    private void loadConversation(long convId) {
        setMessagesLoading(true);

        chatService.openConversation(convId, () -> {
            setMessagesLoading(false);
            scrollToBottom();
        });
    }

    private void showEmptyState() {
        // ✅ setManaged ضروري
        emptyState.setVisible(true);
        emptyState.setManaged(true);
        chatContent.setVisible(false);
        chatContent.setManaged(false);
    }

    // ══════════════════════════════════════════════════════
    //  Messages
    // ══════════════════════════════════════════════════════

    private void addMessageBubble(ChatDTOs.ChatMessageDTO msg) {
        MessageBubble bubble = new MessageBubble(msg);
        messagesContainer.getChildren().add(bubble);
    }

    private void scrollToBottom() {
        // runLater مرتين: الأولى تضيف الـ nodes، الثانية بعد الـ layout pass
        Platform.runLater(() ->
                Platform.runLater(() -> messagesScroll.setVvalue(1.0))
        );
    }

    private void setMessagesLoading(boolean loading) {
        messagesLoading.setVisible(loading);
        messagesLoading.setManaged(loading);
    }

    // ══════════════════════════════════════════════════════
    //  Send Message
    // ══════════════════════════════════════════════════════

    @FXML
    private void onSendMessage() {
        String content = messageInput.getText();
        if (content == null || content.isBlank()) return;
        if (chatService.getOpenConversationId() == null) return;

        messageInput.clear();
        btnSend.setDisable(true);

        chatService.sendMessage(content, err -> {
            Platform.runLater(() -> {
                btnSend.setDisable(false);
                NotificationService.getInstance().send(
                        HRNotification.builder()
                                .title("فشل الإرسال")
                                .message(err)
                                .type(HRNotification.NotificationType.SYSTEM)
                                .build()
                );
            });
        });

        Platform.runLater(() -> btnSend.setDisable(false));
    }

    // ══════════════════════════════════════════════════════
    //  Attach File
    // ══════════════════════════════════════════════════════

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
                        NotificationService.getInstance().send(
                                HRNotification.builder()
                                        .title("فشل إرسال الملف")
                                        .message(res.getMessage())
                                        .type(HRNotification.NotificationType.SYSTEM)
                                        .build()
                        );
                    }
                }));
    }

    // ══════════════════════════════════════════════════════
    //  New Conversation
    // ══════════════════════════════════════════════════════

    @FXML
    private void onNewConversation() {
        Window window = btnNewConversation.getScene().getWindow();

        // ١. اختار نوع المحادثة
        new NewConversationTypeDialog(window).showAndWait().ifPresent(type -> {
            switch (type) {
                case "PRIVATE" -> openPrivateDialog(window);
                case "GROUP" -> openGroupDialog(window);
                case "BROADCAST" -> openBroadcastDialog(window);
            }
        });
    }

    private void openPrivateDialog(Window window) {
        new NewConversationDialog(window).showAndWait().ifPresent(userId ->
                chatService.startPrivateConversation(userId,
                        convId -> Platform.runLater(() -> openConversationById(convId)),
                        err -> showError("خطأ", err)
                )
        );
    }

    private void openGroupDialog(Window window) {
        new NewGroupDialog(window).showAndWait().ifPresent(req ->
                chatService.startGroupConversation(req.name(), req.participantIds(),
                        convId -> Platform.runLater(() -> openConversationById(convId)),
                        err -> showError("خطأ في إنشاء المجموعة", err)
                )
        );
    }

    private void openBroadcastDialog(Window window) {
        new NewBroadcastDialog(window).showAndWait().ifPresent(req ->
                chatService.startBroadcast(req.name(), req.targetDepartmentId(),
                        convId -> Platform.runLater(() -> openConversationById(convId)),
                        err -> showError("خطأ في إرسال الرسالة", err)
                )
        );
    }

    private void showError(String title, String msg) {
        NotificationService.getInstance().send(
                HRNotification.builder()
                        .title(title)
                        .message(msg)
                        .type(HRNotification.NotificationType.SYSTEM)
                        .build()
        );
    }

    private void openConversationById(long convId) {
        ChatDTOs.ConversationSummaryDTO existing = chatService.getConversations().stream()
                .filter(c -> c.getId() != null && c.getId().equals(convId))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            conversationList.getSelectionModel().select(existing);
            currentConversation = existing;
            showChatContent(existing);
            loadConversation(convId);
            return;
        }

        // placeholder لو القائمة لسه ما اتحدثتش
        ChatDTOs.ConversationSummaryDTO placeholder = new ChatDTOs.ConversationSummaryDTO();
        placeholder.setId(convId);
        placeholder.setName("جاري التحميل...");
        placeholder.setType("PRIVATE");
        placeholder.setAvatarInitials("?");
        placeholder.setAvatarColor("#185FA5");

        currentConversation = placeholder;
        showChatContent(placeholder);
        loadConversation(convId);

        javafx.collections.ListChangeListener<ChatDTOs.ConversationSummaryDTO> listener =
                new javafx.collections.ListChangeListener<>() {
                    @Override
                    public void onChanged(Change<? extends ChatDTOs.ConversationSummaryDTO> change) {
                        chatService.getConversations().stream()
                                .filter(c -> c.getId() != null && c.getId().equals(convId))
                                .findFirst()
                                .ifPresent(conv -> Platform.runLater(() -> {
                                    chatService.getConversations().removeListener(this);
                                    currentConversation = conv;
                                    headerConvName.setText(conv.getName());
                                    headerAvatarInitials.setText(conv.getAvatarInitials());
                                    String color = conv.getAvatarColor() != null
                                            ? conv.getAvatarColor() : "#185FA5";
                                    headerAvatar.setStyle("-fx-background-color: " + color
                                            + "; -fx-background-radius: 20;");
                                    conversationList.getSelectionModel().select(conv);
                                }));
                    }
                };
        chatService.getConversations().addListener(listener);
        chatService.refreshConversations();
    }

    // ══════════════════════════════════════════════════════
    //  Search
    // ══════════════════════════════════════════════════════

    @FXML
    private void onSearchConversations() {
        String query = searchField.getText().toLowerCase().trim();
        filteredConversations.setPredicate(query.isEmpty() ? p -> true :
                conv -> conv.getName() != null &&
                        conv.getName().toLowerCase().contains(query));
    }

    // ══════════════════════════════════════════════════════
    //  Conv Info
    // ══════════════════════════════════════════════════════

    @FXML
    private void onShowConvInfo() {
        if (currentConversation == null) return;
        // TODO: dialog تفاصيل المجموعة
    }

    // ══════════════════════════════════════════════════════
    //  Cleanup
    // ══════════════════════════════════════════════════════

    public void onClose() {
        chatService.shutdown();
    }
}