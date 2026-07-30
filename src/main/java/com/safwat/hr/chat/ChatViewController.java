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

    // ✅ جديد: حد أقصى لحجم الملف على الفرونت (بنفس قيمة الباك إند) — نمنع
    // محاولة الرفع من الأساس بدل ما ننتظر رد فشل من السيرفر
    private static final long MAX_ATTACHMENT_SIZE_BYTES = 50L * 1024 * 1024; // 50 MB
    @FXML
    private ListView<ChatDTOs.ConversationSummaryDTO> conversationList;
    @FXML
    private TextField searchField;
    @FXML
    private Button btnNewConversation;
    @FXML
    private Button btnMarkAllRead;
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
    @FXML
    private HBox typingIndicator;
    @FXML
    private Label typingLabel;
    private ChatService chatService;
    private FilteredList<ChatDTOs.ConversationSummaryDTO> filteredConversations;
    private ChatDTOs.ConversationSummaryDTO currentConversation;
    private javafx.collections.ListChangeListener<ChatDTOs.ChatMessageDTO> messagesListener;
    private javafx.collections.ListChangeListener<String> typingListener;
    private javafx.event.EventHandler<KeyEvent> escapeHandler;
    private boolean userScrolledUp = false;
    private double lastVvalue = 1.0;
    private ChatDTOs.ChatMessageDTO editingMessage = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        chatService = ChatService.getInstance();

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

        filteredConversations = new FilteredList<>(chatService.getConversations(), p -> true);
        conversationList.setItems(filteredConversations);
        conversationList.setCellFactory(lv -> {
            ConversationCell cell = new ConversationCell();

            cell.setOnContextMenuRequested(e -> {
                ChatDTOs.ConversationSummaryDTO conv = cell.getItem();
                if (conv == null) return;

                ContextMenu menu = new ContextMenu();

                MenuItem deleteForMe = new MenuItem("🗑️ حذف لدي");
                deleteForMe.setOnAction(ev -> {
                    chatService.deleteConversation(conv.getId(), false,
                            () -> {
                            },
                            err -> showError("فشل الحذف", err)
                    );
                });

                MenuItem deleteForAll = new MenuItem("🗑️ حذف للجميع");
                deleteForAll.setOnAction(ev -> {
                    chatService.deleteConversation(conv.getId(), true,
                            () -> {
                            },
                            err -> showError("فشل الحذف", err)
                    );
                });

                menu.getItems().addAll(deleteForMe, deleteForAll);
                menu.show(cell, e.getScreenX(), e.getScreenY());
            });

            return cell;
        });

        messagesListener = (javafx.collections.ListChangeListener<ChatDTOs.ChatMessageDTO>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (ChatDTOs.ChatMessageDTO msg : change.getAddedSubList()) {
                        addMessageBubble(msg);
                    }
                }
            }
        };
        chatService.getMessages().addListener(messagesListener);

        chatService.setOnNewMessageInOpenConv(this::scrollToBottom);

        setupInfiniteScroll();
        setupTypingIndicator();

        // ✅ تم الإصلاح: استخدام updateMessageBubble بدل updateMessageStatus
        chatService.setOnMessageStatusChanged(this::updateMessageBubble);

        messageInput.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (event.isShiftDown()) {
                    int pos = messageInput.getCaretPosition();
                    String text = messageInput.getText();
                    messageInput.setText(text.substring(0, pos) + "\n" + text.substring(pos));
                    messageInput.positionCaret(pos + 1);
                    event.consume();
                } else {
                    event.consume();
                    if (editingMessage != null) {
                        onEditMessage();
                    } else {
                        onSendMessage();
                    }
                }
            }
        });

        messageInput.textProperty().addListener((obs, old, newVal) -> {
            if (chatService.getOpenConversationId() != null) {
                chatService.sendTyping(true);
            }
        });

        showEmptyState();
    }

    private void setupInfiniteScroll() {
        messagesScroll.vvalueProperty().addListener((obs, old, newVal) -> {
            double v = (Double) newVal;
            userScrolledUp = v < 0.95;
            lastVvalue = v;

            Long convId = chatService.getOpenConversationId();
            if (convId == null) return;
            if (chatService.isLoadingMessages()) return;
            if (!chatService.hasMoreMessages(convId)) return;

            if (v < 0.1) {
                loadOlderMessages(convId);
            }
        });
    }

    private void loadOlderMessages(long convId) {
        double oldContentHeight = messagesContainer.getHeight();

        chatService.loadMoreMessages(convId, () -> {
            Platform.runLater(() -> {
                messagesContainer.requestLayout();
                Platform.runLater(() -> {
                    double newContentHeight = messagesContainer.getHeight();
                    double heightAdded = newContentHeight - oldContentHeight;

                    if (heightAdded > 0 && oldContentHeight > 0) {
                        messagesScroll.setVvalue(0.02);
                    }
                });
            });
        });
    }

    private void setupTypingIndicator() {
        typingListener = (javafx.collections.ListChangeListener<String>) change -> {
            while (change.next()) {
                Platform.runLater(() -> updateTypingIndicator());
            }
        };
        chatService.getTypingUsers().addListener(typingListener);
    }

    private void updateTypingIndicator() {
        List<String> users = chatService.getTypingUsers();
        if (users.isEmpty()) {
            typingIndicator.setVisible(false);
            typingIndicator.setManaged(false);
        } else {
            typingIndicator.setVisible(true);
            typingIndicator.setManaged(true);
            String text;
            if (users.size() == 1) {
                text = users.get(0) + " يكتب...";
            } else if (users.size() == 2) {
                text = users.get(0) + " و " + users.get(1) + " يكتبان...";
            } else {
                text = users.size() + " أشخاص يكتبون...";
            }
            typingLabel.setText(text);
        }
    }

    /**
     * ✅ تم الإصلاح: بتعالج edit + delete + status update
     */
    private void updateMessageBubble(ChatDTOs.ChatMessageDTO msg) {
        for (var node : messagesContainer.getChildren()) {
            if (node instanceof MessageBubble bubble) {
                if (bubble.getMessageId() != null && bubble.getMessageId().equals(msg.getId())) {
                    Platform.runLater(() -> bubble.refreshMessage(msg));
                    break;
                }
            }
        }
    }

    @FXML
    private void onConversationSelected() {
        ChatDTOs.ConversationSummaryDTO selected = conversationList.getSelectionModel().getSelectedItem();
        if (selected == null || selected.equals(currentConversation)) return;

        currentConversation = selected;
        showChatContent(selected);
        loadConversation(selected.getId());
    }

    private void showChatContent(ChatDTOs.ConversationSummaryDTO conv) {
        String initials = conv.getAvatarInitials();
        headerAvatarInitials.setText(initials != null && !initials.isEmpty() ? initials : "?");

        String color = conv.getAvatarColor();
        headerAvatar.setStyle("-fx-background-color: " + (color != null && !color.isEmpty() ? color : "#185FA5") + "; -fx-background-radius: 20;");

        String name = conv.getName();
        headerConvName.setText(name != null ? name : "");

        String type = conv.getType();
        headerConvMeta.setText(
                "PRIVATE".equals(type) ? "محادثة خاصة" :
                        "GROUP".equals(type) ? "مجموعة" : "بث عام"
        );

        boolean isGroup = !"PRIVATE".equals(type);
        btnConvInfo.setVisible(isGroup);
        btnConvInfo.setManaged(isGroup);

        emptyState.setVisible(false);
        emptyState.setOpacity(0);
        emptyState.setMinHeight(0);
        emptyState.setMaxHeight(0);
        emptyState.setPrefHeight(0);

        chatContent.setVisible(true);
        chatContent.setOpacity(1);
        chatContent.setMinHeight(-1);
        chatContent.setMaxHeight(Double.MAX_VALUE);
        chatContent.setPrefHeight(-1);

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
        emptyState.setVisible(true);
        emptyState.setOpacity(1);
        emptyState.setMinHeight(-1);
        emptyState.setMaxHeight(Double.MAX_VALUE);
        emptyState.setPrefHeight(-1);

        chatContent.setVisible(false);
        chatContent.setOpacity(0);
        chatContent.setMinHeight(0);
        chatContent.setMaxHeight(0);
        chatContent.setPrefHeight(0);

        headerAvatarInitials.setText("?");
        headerAvatar.setStyle("-fx-background-color: #185FA5; -fx-background-radius: 20;");
        headerConvName.setText("");
        headerConvMeta.setText("");
    }

    private void addMessageBubble(ChatDTOs.ChatMessageDTO msg) {
        MessageBubble bubble = new MessageBubble(msg);

        bubble.setOnContextMenuRequested(e -> {
            if (!msg.isMine() || msg.isDeleted()) return;

            ContextMenu menu = new ContextMenu();

            MenuItem editItem = new MenuItem("✏️ تعديل");
            editItem.setOnAction(ev -> startEditingMessage(msg));

            MenuItem deleteItem = new MenuItem("🗑️ حذف");
            deleteItem.setOnAction(ev -> {
                chatService.deleteMessage(msg.getId(), err ->
                        Platform.runLater(() ->
                                NotificationService.getInstance().send(
                                        HRNotification.builder()
                                                .title("فشل الحذف")
                                                .message(err)
                                                .type(HRNotification.NotificationType.SYSTEM)
                                                .build()
                                )
                        )
                );
            });

            menu.getItems().addAll(editItem, deleteItem);
            menu.show(bubble, e.getScreenX(), e.getScreenY());
        });

        messagesContainer.getChildren().add(bubble);
    }

    private void startEditingMessage(ChatDTOs.ChatMessageDTO msg) {
        editingMessage = msg;
        messageInput.setText(msg.getContent());
        messageInput.requestFocus();
        messageInput.positionCaret(msg.getContent().length());

        messageInput.getStyleClass().add("editing");
        btnSend.getStyleClass().add("editing");
        btnSend.setText("تعديل");

        escapeHandler = event -> {
            if (event.getCode() == KeyCode.ESCAPE && editingMessage != null) {
                event.consume();
                cancelEditing();
            }
        };
        messageInput.addEventFilter(KeyEvent.KEY_PRESSED, escapeHandler);
    }

    private void cancelEditing() {
        editingMessage = null;
        messageInput.clear();

        messageInput.getStyleClass().remove("editing");
        btnSend.getStyleClass().remove("editing");
        btnSend.setText("إرسال");

        if (escapeHandler != null) {
            messageInput.removeEventFilter(KeyEvent.KEY_PRESSED, escapeHandler);
            escapeHandler = null;
        }
    }

    private void onEditMessage() {
        if (editingMessage == null) return;
        String content = messageInput.getText();
        if (content == null || content.isBlank()) return;

        long msgId = editingMessage.getId();
        messageInput.clear();
        btnSend.setDisable(true);

        chatService.editMessage(msgId, content, err -> {
            Platform.runLater(() -> {
                btnSend.setDisable(false);
                cancelEditing();
                if (err != null) {
                    NotificationService.getInstance().send(
                            HRNotification.builder()
                                    .title("فشل التعديل")
                                    .message(err)
                                    .type(HRNotification.NotificationType.SYSTEM)
                                    .build()
                    );
                }
            });
        });
    }

    private void scrollToBottom() {
        if (!userScrolledUp) {
            messagesScroll.requestLayout();
            Platform.runLater(() -> messagesScroll.setVvalue(1.0));
        }
    }

    private void setMessagesLoading(boolean loading) {
        messagesLoading.setVisible(loading);
        messagesLoading.setManaged(loading);
    }

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
                if (err != null) {
                    NotificationService.getInstance().send(
                            HRNotification.builder()
                                    .title("فشل الإرسال")
                                    .message(err)
                                    .type(HRNotification.NotificationType.SYSTEM)
                                    .build()
                    );
                }
            });
        });
    }

    @FXML
    private void onAttachFile() {
        if (chatService.getOpenConversationId() == null) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("اختر ملف أو أكتر للإرسال");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("كل الملفات", "*.*"),
                new FileChooser.ExtensionFilter("صور", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"),
                new FileChooser.ExtensionFilter("مستندات", "*.pdf", "*.docx", "*.xlsx")
        );

        // ✅ جديد: اختيار أكتر من ملف مرة واحدة زي واتساب
        List<File> selected = chooser.showOpenMultipleDialog(btnAttach.getScene().getWindow());
        if (selected == null || selected.isEmpty()) return;

        List<File> oversized = selected.stream()
                .filter(f -> f.length() > MAX_ATTACHMENT_SIZE_BYTES)
                .toList();
        if (!oversized.isEmpty()) {
            String names = oversized.stream()
                    .map(File::getName)
                    .reduce((a, b) -> a + "، " + b)
                    .orElse("");
            NotificationService.getInstance().send(
                    HRNotification.builder()
                            .title("الملف كبير جداً")
                            .message("الحد الأقصى 50 ميجابايت لكل ملف: " + names)
                            .type(HRNotification.NotificationType.SYSTEM)
                            .build()
            );
            return;
        }

        String caption = messageInput.getText().trim();
        messageInput.clear();
        btnAttach.setDisable(true);

        List<java.nio.file.Path> paths = selected.stream().map(File::toPath).toList();

        chatService.sendMessageWithFiles(caption, paths, err -> {
            Platform.runLater(() -> {
                btnAttach.setDisable(false);
                if (err != null) {
                    NotificationService.getInstance().send(
                            HRNotification.builder()
                                    .title("فشل إرسال الملف")
                                    .message(err)
                                    .type(HRNotification.NotificationType.SYSTEM)
                                    .build()
                    );
                }
            });
        });
    }

    @FXML
    private void onMarkAllAsRead() {
        chatService.markAllAsRead(
                () -> {
                },
                err -> Platform.runLater(() ->
                        NotificationService.getInstance().send(
                                HRNotification.builder()
                                        .title("خطأ")
                                        .message(err)
                                        .type(HRNotification.NotificationType.SYSTEM)
                                        .build()
                        )
                )
        );
    }

    @FXML
    private void onNewConversation() {
        Window window = btnNewConversation.getScene().getWindow();

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

        ChatDTOs.ConversationSummaryDTO placeholder = new ChatDTOs.ConversationSummaryDTO();
        placeholder.setId(convId);
        placeholder.setName("جاري التحميل...");
        placeholder.setType("PRIVATE");
        placeholder.setAvatarInitials("?");
        placeholder.setAvatarColor("#185FA5");

        currentConversation = placeholder;
        showChatContent(placeholder);
        loadConversation(convId);

        javafx.collections.ListChangeListener<ChatDTOs.ConversationSummaryDTO> listener = new javafx.collections.ListChangeListener<>() {
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
        javafx.collections.WeakListChangeListener<ChatDTOs.ConversationSummaryDTO> weakListener =
                new javafx.collections.WeakListChangeListener<>(listener);
        chatService.getConversations().addListener(weakListener);
        chatService.refreshConversations();
    }

    @FXML
    private void onSearchConversations() {
        String query = searchField.getText().toLowerCase().trim();
        filteredConversations.setPredicate(query.isEmpty() ? p -> true :
                conv -> conv.getName() != null &&
                        conv.getName().toLowerCase().contains(query));
    }

    @FXML
    private void onShowConvInfo() {
        if (currentConversation == null) return;
    }

    public void onClose() {
        if (messagesListener != null) {
            chatService.getMessages().removeListener(messagesListener);
        }
        if (typingListener != null) {
            chatService.getTypingUsers().removeListener(typingListener);
        }
        if (escapeHandler != null) {
            messageInput.removeEventFilter(KeyEvent.KEY_PRESSED, escapeHandler);
        }
        chatService.shutdown();
    }
}