package com.safwat.hr.network;

import java.net.URI;
import java.net.http.WebSocket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/**
 * عميل WebSocket مستقل للتواصل مع Backend (STOMP over WebSocket).
 * <p>
 * يستخدم {@link ApiClient} للحصول على الـ Token والـ HttpClient المشترك.
 *
 * @see ApiClient
 */
public class WebSocketClient {

    private final URI serverUri;
    private final Consumer<String> onMessage;
    private final Consumer<Throwable> onError;
    private final Runnable onClose;
    private final String authToken;
    private WebSocket webSocket;

    /**
     * ينشئ عميل WebSocket جديد.
     *
     * @param path      المسار النسبي (مثل: "/ws") — يتم دمجه مع {@link ApiClient#BASE_URL2}
     * @param onMessage callback عند استلام رسالة نصية
     * @param onError   callback عند حدوث خطأ
     * @param onClose   callback عند إغلاق الاتصال
     */
    public WebSocketClient(String path,
                           Consumer<String> onMessage,
                           Consumer<Throwable> onError,
                           Runnable onClose) {
        this(path, onMessage, onError, onClose, null);
    }

    /**
     * ينشئ عميل WebSocket مع Token مخصص.
     *
     * @param path      المسار النسبي
     * @param onMessage callback عند استلام رسالة
     * @param onError   callback عند خطأ
     * @param onClose   callback عند إغلاق
     * @param token     توكن المصادقة (null = يستخدم {@link ApiClient#getAuthToken()})
     */
    public WebSocketClient(String path,
                           Consumer<String> onMessage,
                           Consumer<Throwable> onError,
                           Runnable onClose,
                           String token) {
        this.authToken = token != null ? token : ApiClient.getAuthToken();
        this.serverUri = URI.create(ApiClient.BASE_URL2());
        this.onMessage = onMessage;
        this.onError = onError;
        this.onClose = onClose;

        System.out.println("[WebSocketClient] Connecting to: " + serverUri);
        System.out.println("[WebSocketClient] Token present: " + (authToken != null && !authToken.isEmpty()));
    }

    /**
     * يفتح اتصال WebSocket.
     *
     * @return CompletableFuture<Void> ينتهي عند نجاح الاتصال
     */
    public CompletableFuture<Void> connect() {
        WebSocket.Builder builder = ApiClient.httpClient.newWebSocketBuilder();

        if (authToken != null && !authToken.isEmpty()) {
            builder.header("Authorization", "Bearer " + authToken);
            System.out.println("[WebSocketClient] ✅ Authorization header added");
        } else {
            System.out.println("[WebSocketClient] ⚠️ No token available");
        }

        builder.header("Origin", "http://localhost:8080");
        builder.header("User-Agent", "JavaFX-Client");

        return builder
                .buildAsync(serverUri, new WebSocket.Listener() {
                    @Override
                    public void onOpen(WebSocket ws) {
                        webSocket = ws;
                        System.out.println("[WebSocketClient] ✅ Connection opened");
                        ws.request(1);
                    }

                    @Override
                    public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
                        if (onMessage != null) {
                            System.out.println("[WebSocketClient] 📩 Received: " + data);
                            onMessage.accept(data.toString());
                        }
                        ws.request(1);
                        return null;
                    }

                    @Override
                    public void onError(WebSocket ws, Throwable error) {
                        System.err.println("[WebSocketClient] ❌ Error: " + error.getMessage());
                        if (onError != null) onError.accept(error);
                    }

                    @Override
                    public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
                        System.out.println("[WebSocketClient] 🔌 Closed: " + reason + " (code: " + statusCode + ")");
                        if (onClose != null) onClose.run();
                        return null;
                    }
                })
                .thenAccept(ws -> webSocket = ws)
                .exceptionally(e -> {
                    System.err.println("[WebSocketClient] ❌ Connection failed: " + e.getMessage());
                    if (onError != null) onError.accept(e);
                    return null;
                });
    }

    /**
     * يرسل رسالة نصية للـ Server.
     *
     * @param message النص المراد إرساله
     */
    public void sendMessage(String message) {
        if (webSocket != null) {
            webSocket.sendText(message, true);
            System.out.println("[WebSocketClient] 📤 Sent: " + message);
        } else {
            System.err.println("[WebSocketClient] ⚠️ Cannot send, WebSocket is null");
        }
    }

    /**
     * يغلق الاتصال بشكل نظيف.
     */
    public void close() {
        if (webSocket != null) {
            webSocket.sendClose(1000, "Closing");
            System.out.println("[WebSocketClient] 🔌 Closing connection");
        }
    }

    /**
     * يرجع حالة الاتصال.
     *
     * @return true إذا كان الاتصال مفتوح
     */
    public boolean isConnected() {
        return webSocket != null && !webSocket.isInputClosed();
    }
}