package com.safwat.hr.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safwat.hr.utils.ApiClient;
import javafx.application.Platform;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * STOMP client للشات — يتعامل مع:
 * <p>
 * /topic/conversation/{id}         ← رسائل المحادثة المفتوحة
 * /user/{username}/queue/chat      ← إشعارات رسائل جديدة من محادثات تانية
 * <p>
 * الاستخدام:
 * <pre>
 *   ChatStompClient.getInstance().connect(
 *       username,
 *       notification -> { ... },   // إشعار رسالة جديدة
 *       error -> { ... }           // خطأ في الاتصال
 *   );
 *
 *   // لما المستخدم يفتح محادثة
 *   ChatStompClient.getInstance().subscribeToConversation(convId, msg -> { ... });
 *
 *   // لما يسيب المحادثة
 *   ChatStompClient.getInstance().unsubscribeFromConversation(convId);
 * </pre>
 */
public class ChatStompClient {

    // ── Singleton ─────────────────────────────────────────────────────
    private static volatile ChatStompClient instance;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean connecting = new AtomicBoolean(false);
    /**
     * subscriptions نشطة: conversationId → StompSession.Subscription
     */
    private final Map<Long, StompSession.Subscription> convSubscriptions = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = ApiClient.mapper;
    // ── State ─────────────────────────────────────────────────────────
    private WebSocketStompClient stompClient;
    private StompSession session;
    private ThreadPoolTaskScheduler scheduler;
    /**
     * subscription إشعارات المستخدم
     */
    private StompSession.Subscription notificationSub;

    // Callbacks
    private Consumer<ChatDTOs.WsNotificationDTO> onNotification;
    private Consumer<String> onError;
    private String currentUsername;

    private ChatStompClient() {
    }

    public static ChatStompClient getInstance() {
        if (instance == null) {
            synchronized (ChatStompClient.class) {
                if (instance == null) instance = new ChatStompClient();
            }
        }
        return instance;
    }

    // ═════════════════════════════════════════════════════════════════
    //  Connect / Disconnect
    // ═════════════════════════════════════════════════════════════════

    /**
     * يبني الاتصال مع الـ STOMP server.
     * آمن للاستدعاء من أي thread — الـ callbacks بترجع على JavaFX thread.
     *
     * @param username       اسم المستخدم الحالي (للـ subscription الخاصة)
     * @param onNotification callback لما تيجي إشعار رسالة جديدة
     * @param onError        callback لما يحصل خطأ
     */
    public void connect(String username,
                        Consumer<ChatDTOs.WsNotificationDTO> onNotification,
                        Consumer<String> onError) {

        if (connected.get() || connecting.getAndSet(true)) {
            System.out.println("[ChatStompClient] Already connected/connecting");
            return;
        }

        this.currentUsername = username;
        this.onNotification = onNotification;
        this.onError = onError;

        // Scheduler للـ heartbeat
        scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("stomp-heartbeat-");
        scheduler.initialize();

        // بناء الـ STOMP client
        StandardWebSocketClient wsClient = new StandardWebSocketClient();
        stompClient = new WebSocketStompClient(wsClient);
        stompClient.setTaskScheduler(scheduler);

        // Jackson converter — نفس الـ mapper بتاع ApiClient
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(mapper);
        stompClient.setMessageConverter(converter);

        // Headers — Authorization
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        String token = ApiClient.getAuthToken();
        if (token != null && !token.isEmpty()) {
            headers.add("Authorization", "Bearer " + token);
        }

        String wsUrl = ApiClient.BASE_URL2;

        System.out.println("[ChatStompClient] Connecting to: " + wsUrl);

        stompClient.connectAsync(wsUrl, headers, new StompSessionHandlerAdapter() {

            @Override
            public void afterConnected(StompSession s, StompHeaders connectedHeaders) {
                session = s;
                connected.set(true);
                connecting.set(false);
                System.out.println("[ChatStompClient] ✅ Connected");

                // Subscribe لإشعارات المستخدم
                subscribeToUserNotifications();
            }

            @Override
            public void handleException(StompSession s,
                                        StompCommand command,
                                        StompHeaders headers,
                                        byte[] payload,
                                        Throwable exception) {
                System.err.println("[ChatStompClient] ❌ Exception: " + exception.getMessage());
                notifyError("STOMP exception: " + exception.getMessage());
            }

            @Override
            public void handleTransportError(StompSession s, Throwable exception) {
                connected.set(false);
                connecting.set(false);
                System.err.println("[ChatStompClient] ❌ Transport error: " + exception.getMessage());
                notifyError("Connection lost: " + exception.getMessage());
            }
        });
    }

    /**
     * قطع الاتصال وتنظيف كل الـ subscriptions
     */
    public void disconnect() {
        convSubscriptions.values().forEach(sub -> {
            try {
                sub.unsubscribe();
            } catch (Exception ignored) {
            }
        });
        convSubscriptions.clear();

        if (notificationSub != null) {
            try {
                notificationSub.unsubscribe();
            } catch (Exception ignored) {
            }
            notificationSub = null;
        }

        if (session != null && session.isConnected()) {
            try {
                session.disconnect();
            } catch (Exception ignored) {
            }
            session = null;
        }

        if (stompClient != null) {
            stompClient.stop();
            stompClient = null;
        }

        if (scheduler != null) {
            scheduler.destroy();
            scheduler = null;
        }

        connected.set(false);
        connecting.set(false);
        System.out.println("[ChatStompClient] 🔌 Disconnected");
    }

    // ═════════════════════════════════════════════════════════════════
    //  Subscriptions
    // ═════════════════════════════════════════════════════════════════

    /**
     * Subscribe لإشعارات المستخدم الخاصة.
     * يُستدعى تلقائياً بعد الاتصال.
     */
    private void subscribeToUserNotifications() {
        if (!isReady()) return;

        String destination = "/user/" + currentUsername + "/queue/chat";

        notificationSub = session.subscribe(destination, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatDTOs.WsNotificationDTO.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                if (payload instanceof ChatDTOs.WsNotificationDTO dto) {
                    System.out.println("[ChatStompClient] 🔔 Notification from: " + dto.getSenderDisplayName());
                    Platform.runLater(() -> {
                        if (onNotification != null) onNotification.accept(dto);
                    });
                }
            }
        });

        System.out.println("[ChatStompClient] 📡 Subscribed to: " + destination);
    }

    /**
     * Subscribe لرسائل محادثة معينة.
     * يُستدعى لما المستخدم يفتح المحادثة.
     *
     * @param conversationId ID المحادثة
     * @param onMessage      callback لكل رسالة جديدة
     */
    public void subscribeToConversation(long conversationId,
                                        Consumer<ChatDTOs.WsMessageDTO> onMessage) {
        if (!isReady()) {
            System.err.println("[ChatStompClient] ⚠️ Not connected, can't subscribe to conv " + conversationId);
            return;
        }

        // إلغاء الـ subscription القديمة لو موجودة
        unsubscribeFromConversation(conversationId);

        String destination = "/topic/conversation/" + conversationId;

        StompSession.Subscription sub = session.subscribe(destination, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatDTOs.WsMessageDTO.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                if (payload instanceof ChatDTOs.WsMessageDTO dto) {
                    System.out.println("[ChatStompClient] 💬 New message in conv " + conversationId);
                    Platform.runLater(() -> onMessage.accept(dto));
                }
            }
        });

        convSubscriptions.put(conversationId, sub);
        System.out.println("[ChatStompClient] 📡 Subscribed to: " + destination);
    }

    /**
     * إلغاء الـ subscription من محادثة.
     * يُستدعى لما المستخدم يسيب المحادثة أو يفتح محادثة تانية.
     */
    public void unsubscribeFromConversation(long conversationId) {
        StompSession.Subscription sub = convSubscriptions.remove(conversationId);
        if (sub != null) {
            try {
                sub.unsubscribe();
                System.out.println("[ChatStompClient] 🚫 Unsubscribed from conv " + conversationId);
            } catch (Exception e) {
                System.err.println("[ChatStompClient] Error unsubscribing: " + e.getMessage());
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════
    //  Helpers
    // ═════════════════════════════════════════════════════════════════

    public boolean isConnected() {
        return connected.get() && session != null && session.isConnected();
    }

    private boolean isReady() {
        return connected.get() && session != null && session.isConnected();
    }

    private void notifyError(String message) {
        Platform.runLater(() -> {
            if (onError != null) onError.accept(message);
        });
    }
}
