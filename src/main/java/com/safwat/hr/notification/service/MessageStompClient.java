package com.safwat.hr.notification.service;


import com.safwat.hr.notification.util.MessageSummaryDTO;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.function.Consumer;

public class MessageStompClient {
    private static final MessageStompClient INSTANCE = new MessageStompClient();
    private StompSession session;

    public static MessageStompClient getInstance() {
        return INSTANCE;
    }

    public void connect(String username,
                        Consumer<MessageSummaryDTO> onMessage,
                        Consumer<String> onError) {
        // STOMP over WebSocket
        WebSocketClient client = new StandardWebSocketClient();
        WebSocketStompClient stompClient = new WebSocketStompClient(client);

        StompSessionHandler handler = new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders headers) {
                // Subscribe لـ /user/queue/messages
                session.subscribe("/user/queue/messages", new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) {
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
