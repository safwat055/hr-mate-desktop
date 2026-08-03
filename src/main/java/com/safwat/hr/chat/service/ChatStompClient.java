package com.safwat.hr.chat.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.safwat.hr.chat.dto.ChatDTOs;
import com.safwat.hr.network.ApiClient;
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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * STOMP client للشات.
 */
public class ChatStompClient {

    private static final int MAX_RECONNECT_ATTEMPTS = 10;
    private static final long INITIAL_RECONNECT_DELAY_MS = 1000;
    private static final long MAX_RECONNECT_DELAY_MS = 30000;

    private static volatile ChatStompClient instance;

    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean connecting = new AtomicBoolean(false);
    private final Map<Long, StompSession.Subscription> convSubscriptions = new ConcurrentHashMap<>();
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
    private final AtomicBoolean shouldReconnect = new AtomicBoolean(true);
    private final Map<Long, StompSession.Subscription> typingSubscriptions = new ConcurrentHashMap<>();

    // ✅ تم الإصلاح: ObjectMapper منفصل للـ WebSocket بيدعم ISO format
    private final ObjectMapper stompMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private WebSocketStompClient stompClient;
    private StompSession session;
    private ThreadPoolTaskScheduler scheduler;
    private StompSession.Subscription notificationSub;
    private ScheduledExecutorService reconnectScheduler;

    private Consumer<ChatDTOs.WsNotificationDTO> onNotification;
    private Consumer<ChatDTOs.PresenceEventDTO> onPresence;
    private Consumer<String> onError;
    private String currentUsername;
    private StompSession.Subscription presenceSub;

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

    public void connect(String username,
                        Consumer<ChatDTOs.WsNotificationDTO> onNotification,
                        Consumer<ChatDTOs.PresenceEventDTO> onPresence,
                        Consumer<String> onError) {

        if (connected.get() || connecting.getAndSet(true)) {
            System.out.println("[ChatStompClient] Already connected/connecting");
            return;
        }

        this.currentUsername = username;
        this.onNotification = onNotification;
        this.onPresence = onPresence;
        this.onError = onError;
        this.shouldReconnect.set(true);
        this.reconnectAttempts.set(0);

        if (reconnectScheduler == null || reconnectScheduler.isShutdown()) {
            reconnectScheduler = new ScheduledThreadPoolExecutor(1, r -> {
                Thread t = new Thread(r, "stomp-reconnect");
                t.setDaemon(true);
                return t;
            });
        }

        doConnect();
    }

    private void doConnect() {
        scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("stomp-heartbeat-");
        scheduler.initialize();

        StandardWebSocketClient wsClient = new StandardWebSocketClient();
        stompClient = new WebSocketStompClient(wsClient);
        stompClient.setTaskScheduler(scheduler);

        // ✅ تم الإصلاح: استخدام stompMapper (ISO format) بدل ApiClient.mapper
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(stompMapper);
        stompClient.setMessageConverter(converter);

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
                reconnectAttempts.set(0);
                System.out.println("[ChatStompClient] ✅ Connected");

                subscribeToUserNotifications();
                subscribeToPresence();
                resubscribeConversations();
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
                scheduleReconnect();
            }
        });
    }

    private void scheduleReconnect() {
        if (!shouldReconnect.get()) return;

        int attempt = reconnectAttempts.incrementAndGet();
        if (attempt > MAX_RECONNECT_ATTEMPTS) {
            System.err.println("[ChatStompClient] Max reconnection attempts reached. Giving up.");
            notifyError("فشل الاتصال بعد " + MAX_RECONNECT_ATTEMPTS + " محاولات. يرجى التحقق من الشبكة.");
            return;
        }

        long delay = Math.min(INITIAL_RECONNECT_DELAY_MS * (1L << (attempt - 1)), MAX_RECONNECT_DELAY_MS);
        System.out.println("[ChatStompClient] ⏳ Reconnecting in " + delay + "ms (attempt " + attempt + "/" + MAX_RECONNECT_ATTEMPTS + ")");

        reconnectScheduler.schedule(() -> {
            if (shouldReconnect.get() && !connected.get()) {
                Platform.runLater(() -> {
                    connecting.set(false);
                    doConnect();
                });
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    public void disconnect() {
        shouldReconnect.set(false);

        if (reconnectScheduler != null) {
            reconnectScheduler.shutdownNow();
            reconnectScheduler = null;
        }

        convSubscriptions.values().forEach(sub -> {
            try {
                sub.unsubscribe();
            } catch (Exception ignored) {
            }
        });
        convSubscriptions.clear();

        typingSubscriptions.values().forEach(sub -> {
            try {
                sub.unsubscribe();
            } catch (Exception ignored) {
            }
        });
        typingSubscriptions.clear();

        if (notificationSub != null) {
            try {
                notificationSub.unsubscribe();
            } catch (Exception ignored) {
            }
            notificationSub = null;
        }

        if (presenceSub != null) {
            try {
                presenceSub.unsubscribe();
            } catch (Exception ignored) {
            }
            presenceSub = null;
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
        reconnectAttempts.set(0);
        System.out.println("[ChatStompClient] 🔌 Disconnected");
    }

    // ═════════════════════════════════════════════════════════════════
    //  Subscriptions
    // ═════════════════════════════════════════════════════════════════

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
     * ✅ جديد: اشتراك عام (مش مرتبط بمحادثة معينة) في حالة اتصال المستخدمين
     */
    private void subscribeToPresence() {
        if (!isReady()) return;

        presenceSub = session.subscribe("/topic/presence", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatDTOs.PresenceEventDTO.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                if (payload instanceof ChatDTOs.PresenceEventDTO dto) {
                    Platform.runLater(() -> {
                        if (onPresence != null) onPresence.accept(dto);
                    });
                }
            }
        });

        System.out.println("[ChatStompClient] 📡 Subscribed to: /topic/presence");
    }

    public void subscribeToConversation(long conversationId,
                                        Consumer<ChatDTOs.WsMessageDTO> onMessage) {
        if (!isReady()) {
            System.err.println("[ChatStompClient] ⚠️ Not connected, can't subscribe to conv " + conversationId);
            return;
        }

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

    public void subscribeToTyping(long conversationId, Consumer<ChatDTOs.WsMessageDTO> onTypingEvent) {
        if (!isReady()) return;

        StompSession.Subscription oldSub = typingSubscriptions.remove(conversationId);
        if (oldSub != null) {
            try {
                oldSub.unsubscribe();
            } catch (Exception ignored) {
            }
        }

        String destination = "/topic/conversation/" + conversationId + "/typing";

        StompSession.Subscription sub = session.subscribe(destination, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatDTOs.WsMessageDTO.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                if (payload instanceof ChatDTOs.WsMessageDTO dto) {
                    Platform.runLater(() -> onTypingEvent.accept(dto));
                }
            }
        });

        typingSubscriptions.put(conversationId, sub);
        System.out.println("[ChatStompClient] 📡 Subscribed to typing: " + destination);
    }

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

        StompSession.Subscription typingSub = typingSubscriptions.remove(conversationId);
        if (typingSub != null) {
            try {
                typingSub.unsubscribe();
            } catch (Exception ignored) {
            }
        }
    }

    private void resubscribeConversations() {
        // Called after reconnection — ChatService handles resubscription
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