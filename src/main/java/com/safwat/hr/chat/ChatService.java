package com.safwat.hr.chat;


import com.safwat.hr.notification.model.HRNotification;
import com.safwat.hr.utils.ApiClient;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.function.Consumer;

/**
 * الطبقة الوسيطة بين الـ UI وبين (ChatApiService + ChatStompClient).
 * <p>
 * المسؤوليات:
 * - يحمل ObservableList للمحادثات والرسائل
 * - يربط WebSocket notifications بنظام HRNotification
 * - يحتفظ بالمحادثة المفتوحة حالياً
 * <p>
 * الاستخدام من الـ Controller:
 * <pre>
 *   ChatService chat = ChatService.getInstance();
 *   chat.init(username);
 *   conversationList.setItems(chat.getConversations());
 *   chat.openConversation(id, () -> messageList.scrollTo(bottom));
 * </pre>
 */
public class ChatService {

    // ── Singleton ─────────────────────────────────────────────────────
    private static volatile ChatService instance;
    // ── State ─────────────────────────────────────────────────────────
    private final ObservableList<ChatDTOs.ConversationSummaryDTO> conversations =
            FXCollections.observableArrayList();
    private final ObservableList<ChatDTOs.ChatMessageDTO> messages =
            FXCollections.observableArrayList();
    private Long openConversationId = null;
    private String currentUsername = null;
    /**
     * Callback يُستدعى لما تيجي رسالة جديدة في المحادثة المفتوحة
     */
    private Runnable onNewMessageInOpenConv;
    /**
     * Callback لأخطاء الاتصال
     */
    private Consumer<String> onConnectionError;

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

    /**
     * يُستدعى مرة واحدة بعد تسجيل الدخول.
     * يبني اتصال WebSocket ويحمل قائمة المحادثات.
     */
    public void init(String username, Consumer<String> onConnectionError) {
        this.currentUsername = username;
        this.onConnectionError = onConnectionError;

        // ١. اتصال WebSocket
        ChatStompClient.getInstance().connect(
                username,
                this::handleIncomingNotification,  // إشعار من محادثة تانية
                err -> {
                    System.err.println("[ChatService] WS Error: " + err);
                    if (onConnectionError != null)
                        Platform.runLater(() -> onConnectionError.accept(err));
                }
        );

        // ٢. تحميل المحادثات
        refreshConversations();
    }

    /**
     * تحديث قائمة المحادثات من الـ server
     */
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

    /**
     * فتح محادثة — يحمل الرسائل ويبدأ الاستماع على WebSocket.
     *
     * @param conversationId ID المحادثة
     * @param onLoaded       callback بعد ما تتحمل الرسائل (على FX thread)
     */
    public void openConversation(long conversationId, Runnable onLoaded) {
        // إلغاء الـ subscription القديمة
        if (openConversationId != null && openConversationId != conversationId) {
            ChatStompClient.getInstance().unsubscribeFromConversation(openConversationId);
        }

        openConversationId = conversationId;
        messages.clear();

        // Subscribe على WebSocket أولاً
        ChatStompClient.getInstance().subscribeToConversation(conversationId, wsMsg -> {
            // رسالة جديدة وصلت عبر WebSocket
            if (wsMsg.getMessage() != null && wsMsg.getConversationId() == conversationId) {
                messages.add(wsMsg.getMessage());
                // تحديث قائمة المحادثات (unread count + lastMessage)
                refreshConversations();
                if (onNewMessageInOpenConv != null) onNewMessageInOpenConv.run();
            }
        });

        // تحميل الرسائل السابقة
        ChatApiService.getMessages(conversationId, 0).thenAccept(res -> {
            Platform.runLater(() -> {
                if (res.isSuccess() && res.getData() != null) {
                    messages.setAll(res.getData());
                }
                if (onLoaded != null) onLoaded.run();
            });
        });

        // تعليم المحادثة كمقروءة
        ChatApiService.markAsRead(conversationId).thenAccept(res -> {
            Platform.runLater(this::refreshConversations);
        });
    }

    /**
     * إغلاق المحادثة الحالية
     */
    public void closeConversation() {
        if (openConversationId != null) {
            ChatStompClient.getInstance().unsubscribeFromConversation(openConversationId);
            openConversationId = null;
            messages.clear();
        }
    }

    // ═════════════════════════════════════════════════════════════════
    //  Send Message
    // ═════════════════════════════════════════════════════════════════

    /**
     * إرسال رسالة نصية في المحادثة المفتوحة حالياً.
     * الرسالة هتيجي تلقائياً عبر WebSocket — مش محتاج تضيفها يدوياً.
     */
    public void sendMessage(String content, Consumer<String> onError) {
        if (openConversationId == null) return;
        if (content == null || content.isBlank()) return;

        ChatApiService.sendTextMessage(openConversationId, content.trim())
                .thenAccept(res -> {
                    if (!res.isSuccess()) {
                        Platform.runLater(() -> {
                            if (onError != null) onError.accept(res.getMessage());
                        });
                    }
                    // الرسالة هتيجي عبر WebSocket — مش محتاج نضيفها هنا
                });
    }

    // ═════════════════════════════════════════════════════════════════
    //  Create Conversation
    // ═════════════════════════════════════════════════════════════════

    /**
     * بدء محادثة خاصة مع مستخدم — لو موجودة يفتحها، لو جديدة يُنشئها
     */
    public void startPrivateConversation(long userId,
                                         Consumer<Long> onSuccess,
                                         Consumer<String> onError) {
        ChatApiService.createPrivateConversation(userId).thenAccept(res -> {
            Platform.runLater(() -> {
                if (res.isSuccess() && res.getData() != null) {
                    refreshConversations();
                    if (onSuccess != null) onSuccess.accept(res.getData().getId());
                } else {
                    if (onError != null) onError.accept(res.getMessage());
                }
            });
        });
    }

    // ═════════════════════════════════════════════════════════════════
    //  WebSocket Notification Handler
    // ═════════════════════════════════════════════════════════════════

    /**
     * يُستدعى لما تيجي رسالة جديدة في محادثة مش مفتوحة حالياً.
     * يحدّث القائمة ويظهر HRNotification Toast.
     */
    private void handleIncomingNotification(ChatDTOs.WsNotificationDTO notification) {
        // تحديث قائمة المحادثات (unread count)
        refreshConversations();

        // إظهار HRNotification Toast
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

    public Long getOpenConversationId() {
        return openConversationId;
    }

    public String getCurrentUsername() {
        return currentUsername != null ? currentUsername : ApiClient.getUserName();
    }

    public void setOnNewMessageInOpenConv(Runnable callback) {
        this.onNewMessageInOpenConv = callback;
    }

    /**
     * تنظيف عند إغلاق التطبيق
     */
    public void shutdown() {
        closeConversation();
        ChatStompClient.getInstance().disconnect();
    }
}
