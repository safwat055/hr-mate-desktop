package com.safwat.hr.controller.message.service;

import com.safwat.hr.controller.message.dto.MessageSummaryDTO;
import org.jetbrains.annotations.NotNull;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.function.Consumer;

/**
 * =====================================================================
 * MessageStompClient
 * =====================================================================
 * عميل WebSocket STOMP بسيط للرسائل.
 * يتصل بخادم WebSocket ويستقبل الرسائل في الوقت الفعلي
 * من خلال الاشتراك في القناة /user/queue/messages.
 * <p>
 * الاستخدام:
 * MessageStompClient.getInstance().connect(username, onMessage, onError);
 */
public class MessageStompClient {
    private static final MessageStompClient INSTANCE = new MessageStompClient();
    private StompSession session;

    private MessageStompClient() {
    }

    /**
     * ترجع النسخة الوحيدة من العميل.
     *
     * @return INSTANCE
     */
    public static MessageStompClient getInstance() {
        return INSTANCE;
    }

    /**
     * الاتصال بخادم WebSocket STOMP.
     *
     * @param username  اسم المستخدم للاشتراك في قناته
     * @param onMessage callback عند استقبال رسالة جديدة
     * @param onError   callback عند حدوث خطأ
     */
    public void connect(String username,
                        Consumer<MessageSummaryDTO> onMessage,
                        Consumer<String> onError) {
        WebSocketClient client = new StandardWebSocketClient();
        WebSocketStompClient stompClient = new WebSocketStompClient(client);

        StompSessionHandler handler = new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders headers) {
                session.subscribe("/user/queue/messages", new StompFrameHandler() {
                    @NotNull
                    @Override
                    public Type getPayloadType(@NotNull StompHeaders headers) {
                        return MessageSummaryDTO.class;
                    }

                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        onMessage.accept((MessageSummaryDTO) payload);
                    }
                });
            }
        };

        stompClient.connect("ws://localhost:8080/ws", handler);
    }
}