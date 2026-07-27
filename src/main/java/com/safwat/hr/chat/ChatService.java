package com.safwat.hr.chat;

import com.safwat.hr.notification.model.HRNotification;
import com.safwat.hr.utils.ApiClient;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * الطبقة الوسيطة بين الـ UI وبين (ChatApiService + ChatStompClient).
 */
public class ChatService {

    private static final int TYPING_TIMEOUT_SECONDS = 3;
    private static final int DEFAULT_PAGE_SIZE = 100;

    private static volatile ChatService instance;

    private final ObservableList<ChatDTOs.ConversationSummaryDTO> conversations =
            FXCollections.observableArrayList();
    private final ObservableList<ChatDTOs.ChatMessageDTO> messages =
            FXCollections.observableArrayList();

    private final Map<Long, Integer> conversationPageMap = new HashMap<>();
    private final Map<Long, Boolean> conversationHasMoreMap = new HashMap<>();
    private final ObservableList<String> typingUsers = FXCollections.observableArrayList();
    private final Map<String, javafx.animation.Timeline> typingTimers = new HashMap<>();

    private Long openConversationId = null;
    private String currentUsername = null;
    private boolean isLoadingMessages = false;

    private Runnable onNewMessageInOpenConv;
    private Consumer<String> onConnectionError;
    private Consumer<java.util.List<String>> onTypingChanged;
    private Consumer<ChatDTOs.ChatMessageDTO> onMessageStatusChanged;
    private Runnable onMessagesLoaded;

    private ChatService() {
    }

    public static ChatService getInstance() {
        if (instance == null) {
            synchronized (ChatService.class) {
                if (instance == null) instance = new ChatService();
            }
        }
        return instance;
    }

    // ═════════════════════════════════════════════════════════════════
    //  Init
    // ═════════════════════════════════════════════════════════════════

    public void init(String username, Consumer<String> onConnectionError) {
        this.currentUsername = username;
        this.onConnectionError = onConnectionError;

        ChatStompClient.getInstance().connect(
                username,
                this::handleIncomingNotification,
                err -> {
                    System.err.println("[ChatService] WS Error: " + err);
                    if (onConnectionError != null)
                        Platform.runLater(() -> onConnectionError.accept(err));
                }
        );

        refreshConversations();
    }

    public void refreshConversations() {
        ChatApiService.getConversations().thenAccept(res -> {
            Platform.runLater(() -> {
                if (res.isSuccess() && res.getData() != null) {
                    conversations.setAll(res.getData());
                }
            });
        });
    }

    // ═════════════════════════════════════════════════════════════════
    //  Open / Close Conversation
    // ═════════════════════════════════════════════════════════════════

    public void openConversation(long conversationId, Runnable onLoaded) {
        if (openConversationId != null && openConversationId != conversationId) {
            ChatStompClient.getInstance().unsubscribeFromConversation(openConversationId);
            typingUsers.clear();
            typingTimers.values().forEach(t -> t.stop());
            typingTimers.clear();
        }

        openConversationId = conversationId;
        messages.clear();
        conversationPageMap.put(conversationId, 0);
        conversationHasMoreMap.put(conversationId, true);
        isLoadingMessages = false;

        ChatStompClient.getInstance().subscribeToConversation(conversationId, wsMsg -> {
            handleWsMessage(conversationId, wsMsg);
        });

        ChatStompClient.getInstance().subscribeToTyping(conversationId, this::handleTypingEvent);

        loadMoreMessages(conversationId, onLoaded);

        ChatApiService.markAsRead(conversationId).thenAccept(res -> {
            Platform.runLater(this::refreshConversations);
        });
    }

    public void loadMoreMessages(long conversationId, Runnable onLoaded) {
        if (isLoadingMessages) return;

        Boolean hasMore = conversationHasMoreMap.get(conversationId);
        if (hasMore != null && !hasMore) return;

        int page = conversationPageMap.getOrDefault(conversationId, 0);
        isLoadingMessages = true;

        ChatApiService.getMessages(conversationId, page, DEFAULT_PAGE_SIZE).thenAccept(res -> {
            Platform.runLater(() -> {
                isLoadingMessages = false;
                if (res.isSuccess() && res.getData() != null) {
                    String me = getCurrentUsername();

                    var newMessages = res.getData();
                    newMessages.forEach(msg ->
                            msg.setMine(me != null && me.equals(msg.getSenderUsername()))
                    );

                    if (page == 0) {
                        messages.setAll(newMessages);
                    } else {
                        messages.addAll(0, newMessages);
                    }

                    conversationPageMap.put(conversationId, page + 1);
                    conversationHasMoreMap.put(conversationId, newMessages.size() >= DEFAULT_PAGE_SIZE);
                }
                if (onLoaded != null) onLoaded.run();
                if (onMessagesLoaded != null) onMessagesLoaded.run();
            });
        });
    }

    public boolean hasMoreMessages(long conversationId) {
        Boolean hasMore = conversationHasMoreMap.get(conversationId);
        return hasMore == null || hasMore;
    }

    public boolean isLoadingMessages() {
        return isLoadingMessages;
    }

    public void closeConversation() {
        if (openConversationId != null) {
            ChatStompClient.getInstance().unsubscribeFromConversation(openConversationId);
            openConversationId = null;
            messages.clear();
            typingUsers.clear();
            typingTimers.values().forEach(t -> t.stop());
            typingTimers.clear();
        }
    }

    // ═════════════════════════════════════════════════════════════════
    //  Send Message
    // ═════════════════════════════════════════════════════════════════

    public void sendMessage(String content, Consumer<String> onError) {
        if (openConversationId == null) return;
        if (content == null || content.isBlank()) return;

        String trimmed = content.trim();
        if (trimmed.length() > 4000) {
            if (onError != null) onError.accept("الرسالة طويلة جداً (الحد الأقصى 4000 حرف)");
            return;
        }

        ChatApiService.sendTextMessage(openConversationId, trimmed)
                .thenAccept(res -> {
                    if (!res.isSuccess()) {
                        Platform.runLater(() -> {
                            if (onError != null) onError.accept(res.getMessage());
                        });
                    }
                });
    }

    public void sendMessageWithFiles(String content, java.util.List<java.nio.file.Path> files,
                                     Consumer<String> onError) {
        if (openConversationId == null) return;
        if ((content == null || content.isBlank()) && (files == null || files.isEmpty())) return;

        ChatApiService.sendMessageWithFiles(openConversationId, content, files)
                .thenAccept(res -> {
                    Platform.runLater(() -> {
                        if (!res.isSuccess()) {
                            if (onError != null) onError.accept(res.getMessage());
                        }
                    });
                });
    }

    public void sendTyping(boolean typing) {
        if (openConversationId == null) return;
        ChatApiService.sendTypingIndicator(openConversationId, typing);
    }

    public void deleteMessage(long messageId, Consumer<String> onError) {
        ChatApiService.deleteMessage(messageId).thenAccept(res -> {
            Platform.runLater(() -> {
                if (!res.isSuccess() && onError != null) {
                    onError.accept(res.getMessage());
                }
            });
        });
    }

    public void editMessage(long messageId, String newContent, Consumer<String> onError) {
        if (newContent == null || newContent.isBlank()) {
            if (onError != null) onError.accept("الرسالة لا يمكن أن تكون فارغة");
            return;
        }
        if (newContent.length() > 4000) {
            if (onError != null) onError.accept("الرسالة طويلة جداً (الحد الأقصى 4000 حرف)");
            return;
        }

        ChatApiService.editMessage(messageId, newContent.trim()).thenAccept(res -> {
            Platform.runLater(() -> {
                if (!res.isSuccess() && onError != null) {
                    onError.accept(res.getMessage());
                }
            });
        });
    }

    public void deleteConversation(long conversationId, boolean forEveryone,
                                   Runnable onSuccess, Consumer<String> onError) {
        ChatApiService.deleteConversation(conversationId, forEveryone).thenAccept(res -> {
            Platform.runLater(() -> {
                if (res.isSuccess()) {
                    conversations.removeIf(c -> c.getId() != null && c.getId().equals(conversationId));
                    if (openConversationId != null && openConversationId.equals(conversationId)) {
                        closeConversation();
                    }
                    if (onSuccess != null) onSuccess.run();
                } else {
                    if (onError != null) onError.accept(res.getMessage());
                }
            });
        });
    }

    public void markAllAsRead(Runnable onSuccess, Consumer<String> onError) {
        ChatApiService.markAllAsRead().thenAccept(res -> {
            Platform.runLater(() -> {
                if (res.isSuccess()) {
                    conversations.forEach(c -> c.setUnreadCount(0));
                    refreshConversations();
                    if (onSuccess != null) onSuccess.run();
                } else {
                    if (onError != null) onError.accept(res.getMessage());
                }
            });
        });
    }

    // ═════════════════════════════════════════════════════════════════
    //  Create Conversation
    // ═════════════════════════════════════════════════════════════════

    public void startPrivateConversation(long userId,
                                         Consumer<Long> onSuccess,
                                         Consumer<String> onError) {
        ChatApiService.createPrivateConversation(userId).thenAccept(res ->
                Platform.runLater(() -> {
                    if (res.isSuccess() && res.getData() != null) {
                        refreshConversations();
                        if (onSuccess != null) onSuccess.accept(res.getData().getId());
                    } else {
                        if (onError != null) onError.accept(res.getMessage());
                    }
                })
        );
    }

    public void startGroupConversation(String name,
                                       java.util.List<Long> participantIds,
                                       Consumer<Long> onSuccess,
                                       Consumer<String> onError) {
        ChatApiService.createGroupConversation(name, participantIds).thenAccept(res ->
                Platform.runLater(() -> {
                    if (res.isSuccess() && res.getData() != null) {
                        refreshConversations();
                        if (onSuccess != null) onSuccess.accept(res.getData().getId());
                    } else {
                        if (onError != null) onError.accept(res.getMessage());
                    }
                })
        );
    }

    public void startBroadcast(String name,
                               Long targetDepartmentId,
                               Consumer<Long> onSuccess,
                               Consumer<String> onError) {
        ChatApiService.createBroadcastConversation(name, targetDepartmentId).thenAccept(res ->
                Platform.runLater(() -> {
                    if (res.isSuccess() && res.getData() != null) {
                        refreshConversations();
                        if (onSuccess != null) onSuccess.accept(res.getData().getId());
                    } else {
                        if (onError != null) onError.accept(res.getMessage());
                    }
                })
        );
    }

    // ═════════════════════════════════════════════════════════════════
    //  WebSocket Handlers
    // ═════════════════════════════════════════════════════════════════

    private void handleWsMessage(long conversationId, ChatDTOs.WsMessageDTO wsMsg) {
        switch (wsMsg.getType()) {
            case "NEW_MESSAGE" -> handleNewMessage(conversationId, wsMsg);
            case "MESSAGE_STATUS" -> handleMessageStatus(wsMsg);
            case "MESSAGE_EDITED" -> handleMessageEdited(wsMsg);
            case "MESSAGE_DELETED" -> handleMessageDeleted(wsMsg);
            case "CONVERSATION_DELETED" -> handleConversationDeleted(wsMsg);
            case "TYPING" -> handleTypingEvent(wsMsg);
            default -> System.out.println("[ChatService] Unknown WS type: " + wsMsg.getType());
        }
    }

    private void handleNewMessage(long conversationId, ChatDTOs.WsMessageDTO wsMsg) {
        if (wsMsg.getMessage() != null && wsMsg.getConversationId() == conversationId) {
            ChatDTOs.ChatMessageDTO msg = wsMsg.getMessage();

            String me = getCurrentUsername();
            msg.setMine(me != null && me.equals(msg.getSenderUsername()));

            messages.add(msg);
            refreshConversations();

            if (onNewMessageInOpenConv != null) {
                Platform.runLater(() -> onNewMessageInOpenConv.run());
            }
        }
    }

    private void handleMessageStatus(ChatDTOs.WsMessageDTO wsMsg) {
        if (wsMsg.getMessageId() == null || wsMsg.getNewStatus() == null) return;

        Platform.runLater(() -> {
            for (ChatDTOs.ChatMessageDTO msg : messages) {
                if (msg.getId() != null && msg.getId().equals(wsMsg.getMessageId())) {
                    msg.setStatus(wsMsg.getNewStatus());
                    msg.setReadBy(wsMsg.getReadBy());
                    if (onMessageStatusChanged != null) {
                        onMessageStatusChanged.accept(msg);
                    }
                    break;
                }
            }
        });
    }

    /**
     * ✅ تم الإصلاح: بيستدعي onMessageStatusChanged عشان ChatViewController يحدث الـ UI
     */
    private void handleMessageEdited(ChatDTOs.WsMessageDTO wsMsg) {
        if (wsMsg.getMessageId() == null) return;
        Platform.runLater(() -> {
            for (ChatDTOs.ChatMessageDTO msg : messages) {
                if (msg.getId() != null && msg.getId().equals(wsMsg.getMessageId())) {
                    msg.setContent(wsMsg.getNewContent());
                    msg.setEdited(true);
                    msg.setEditedAt(wsMsg.getEditedAt());
                    // ✅ notify UI to refresh
                    if (onMessageStatusChanged != null) {
                        onMessageStatusChanged.accept(msg);
                    }
                    break;
                }
            }
        });
    }

    /**
     * ✅ تم الإصلاح: بيستدعي onMessageStatusChanged عشان ChatViewController يحدث الـ UI
     */
    private void handleMessageDeleted(ChatDTOs.WsMessageDTO wsMsg) {
        if (wsMsg.getMessageId() == null) return;
        Platform.runLater(() -> {
            for (ChatDTOs.ChatMessageDTO msg : messages) {
                if (msg.getId() != null && msg.getId().equals(wsMsg.getMessageId())) {
                    msg.setDeleted(true);
                    msg.setContent("[محذوف] تم حذف هذه الرسالة");
                    // ✅ notify UI to refresh
                    if (onMessageStatusChanged != null) {
                        onMessageStatusChanged.accept(msg);
                    }
                    break;
                }
            }
        });
    }

    private void handleConversationDeleted(ChatDTOs.WsMessageDTO wsMsg) {
        if (wsMsg.getConversationId() == null) return;
        Platform.runLater(() -> {
            if (wsMsg.isForEveryone()) {
                conversations.removeIf(c -> c.getId() != null && c.getId().equals(wsMsg.getConversationId()));
            }
            if (openConversationId != null && openConversationId.equals(wsMsg.getConversationId())) {
                closeConversation();
            }
        });
    }

    private void handleTypingEvent(ChatDTOs.WsMessageDTO wsMsg) {
        if (wsMsg.getUsername() == null) return;
        if (wsMsg.getUsername().equals(currentUsername)) return;

        String username = wsMsg.getUsername();
        boolean isTyping = wsMsg.isTyping();

        Platform.runLater(() -> {
            javafx.animation.Timeline oldTimer = typingTimers.remove(username);
            if (oldTimer != null) oldTimer.stop();

            if (isTyping) {
                if (!typingUsers.contains(username)) {
                    typingUsers.add(username);
                }
                javafx.animation.Timeline timer = new javafx.animation.Timeline(
                        new javafx.animation.KeyFrame(
                                javafx.util.Duration.seconds(TYPING_TIMEOUT_SECONDS),
                                e -> {
                                    typingUsers.remove(username);
                                    typingTimers.remove(username);
                                    if (onTypingChanged != null) onTypingChanged.accept(typingUsers);
                                }
                        )
                );
                timer.setCycleCount(1);
                timer.play();
                typingTimers.put(username, timer);
            } else {
                typingUsers.remove(username);
            }

            if (onTypingChanged != null) onTypingChanged.accept(typingUsers);
        });
    }

    private void handleIncomingNotification(ChatDTOs.WsNotificationDTO notification) {
        refreshConversations();

        HRNotification.builder()
                .title("رسالة جديدة من " + notification.getSenderDisplayName())
                .message(notification.getPreview())
                .type(HRNotification.NotificationType.SYSTEM)
                .build();
    }

    // ═════════════════════════════════════════════════════════════════
    //  Getters
    // ═════════════════════════════════════════════════════════════════

    public ObservableList<ChatDTOs.ConversationSummaryDTO> getConversations() {
        return conversations;
    }

    public ObservableList<ChatDTOs.ChatMessageDTO> getMessages() {
        return messages;
    }

    public ObservableList<String> getTypingUsers() {
        return typingUsers;
    }

    public Long getOpenConversationId() {
        return openConversationId;
    }

    public String getCurrentUsername() {
        return currentUsername != null ? currentUsername : ApiClient.getUserName();
    }

    public void setOnNewMessageInOpenConv(Runnable callback) {
        this.onNewMessageInOpenConv = callback;
    }

    public void setOnTypingChanged(Consumer<java.util.List<String>> callback) {
        this.onTypingChanged = callback;
    }

    public void setOnMessageStatusChanged(Consumer<ChatDTOs.ChatMessageDTO> callback) {
        this.onMessageStatusChanged = callback;
    }

    public void setOnMessagesLoaded(Runnable callback) {
        this.onMessagesLoaded = callback;
    }

    public void shutdown() {
        closeConversation();
        ChatStompClient.getInstance().disconnect();
    }
}