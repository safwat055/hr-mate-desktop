package com.safwat.hr.network;

import java.net.URI;
import java.net.http.WebSocket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/**
 * ══════════════════════════════════════════════════════════════════
 * WebSocketClient — اتصال WebSocket مع Backend
 * ══════════════════════════════════════════════════════════════════
 * <p>
 * يفتح اتصال WebSocket مستمر مع السيرفر ويدير:
 * — استلام الرسائل (onMessage)
 * — إرسال الرسائل (sendMessage)
 * — معالجة الأخطاء والإغلاق
 * <p>
 * يستخدم {@link HttpCore} للحصول على:
 * — الـ Token تلقائيًا (أو يقبل token مخصص في الـ constructor)
 * — الـ HttpClient المشترك (لضمان connection pool موحد)
 * — الـ WS URL
 * <p>
 * الاستخدام:
 * <pre>
 *   WebSocketClient ws = new WebSocketClient(
 *       "/notifications",
 *       msg  -> Platform.runLater(() -> handleMessage(msg)),
 *       err  -> logger.error("WS error", err),
 *       ()   -> reconnect()
 *   );
 *   ws.connect().thenRun(() -> ws.sendMessage("SUBSCRIBE /topic/alerts"));
 * </pre>
 *
 * @see HttpCore
 * @see ApiClient
 */
public class WebSocketClient {

    // ─────────────────────────────────────────────
    //  State
    // ─────────────────────────────────────────────

    private final URI serverUri;
    private final Consumer<String> onMessage;
    private final Consumer<Throwable> onError;
    private final Runnable onClose;
    private final String authToken;

    /**
     * الاتصال الفعلي — null قبل الـ connect()
     */
    private volatile WebSocket webSocket;

    // ─────────────────────────────────────────────
    //  Constructors
    // ─────────────────────────────────────────────

    /**
     * ينشئ عميل WebSocket — يأخذ التوكن تلقائيًا من {@link HttpCore}.
     *
     * @param path      المسار النسبي (مثل "/notifications") —
     *                  يُدمج مع Base WS URL تلقائيًا
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
     * ينشئ عميل WebSocket مع توكن مخصص (مفيد لاتصالات متعددة بمستخدمين مختلفين).
     *
     * @param path      المسار النسبي
     * @param onMessage callback عند استلام رسالة
     * @param onError   callback عند خطأ
     * @param onClose   callback عند إغلاق
     * @param token     توكن مخصص — null يعني: خذ من HttpCore
     */
    public WebSocketClient(String path,
                           Consumer<String> onMessage,
                           Consumer<Throwable> onError,
                           Runnable onClose,
                           String token) {
        HttpCore core = HttpCore.getInstance();
        this.authToken = (token != null) ? token : core.getAuthToken();
        // path النسبي + base WS URL
        this.serverUri = URI.create(core.getBaseWsUrl() + (path != null ? path : ""));
        this.onMessage = onMessage;
        this.onError = onError;
        this.onClose = onClose;
    }

    // ─────────────────────────────────────────────
    //  Connect
    // ─────────────────────────────────────────────

    /**
     * يفتح اتصال WebSocket بشكل async.
     *
     * @return CompletableFuture ينتهي عند نجاح الاتصال أو يفشل بـ exception
     */
    public CompletableFuture<Void> connect() {
        HttpCore core = HttpCore.getInstance();

        WebSocket.Builder builder = core.httpClient.newWebSocketBuilder();

        // إضافة Authorization header لو التوكن موجود
        if (authToken != null && !authToken.isBlank())
            builder.header("Authorization", "Bearer " + authToken);

        builder.header("Origin", "http://localhost:8080");
        builder.header("User-Agent", "HR-JavaFX-Client");

        return builder
                .buildAsync(serverUri, new InternalListener())
                .thenAccept(ws -> this.webSocket = ws)
                .exceptionally(e -> {
                    if (onError != null) onError.accept(e);
                    return null;
                });
    }

    // ─────────────────────────────────────────────
    //  Send
    // ─────────────────────────────────────────────

    /**
     * يرسل رسالة نصية للسيرفر.
     * لا يفعل شيئًا لو الاتصال مش مفتوح.
     *
     * @param message النص المراد إرساله
     */
    public void sendMessage(String message) {
        if (isConnected())
            webSocket.sendText(message, true);
    }

    // ─────────────────────────────────────────────
    //  Close
    // ─────────────────────────────────────────────

    /**
     * يغلق الاتصال بشكل نظيف (Close Code 1000).
     */
    public void close() {
        if (webSocket != null && !webSocket.isOutputClosed())
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Closing");
    }

    // ─────────────────────────────────────────────
    //  Status
    // ─────────────────────────────────────────────

    /**
     * @return true لو الاتصال مفتوح وقادر على استلام رسائل
     */
    public boolean isConnected() {
        return webSocket != null && !webSocket.isInputClosed();
    }

    /**
     * @return الـ URI اللي الكلاس متصل بيه
     */
    public URI getServerUri() {
        return serverUri;
    }

    // ─────────────────────────────────────────────
    //  Internal Listener
    // ─────────────────────────────────────────────

    /**
     * Listener داخلي — يوجّه الأحداث لـ callbacks الخارجية.
     * مفصول في inner class لتنظيف كود الـ connect().
     */
    private class InternalListener implements WebSocket.Listener {

        @Override
        public void onOpen(WebSocket ws) {
            webSocket = ws;
            ws.request(1); // اطلب الرسالة الأولى
        }

        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
            if (onMessage != null)
                onMessage.accept(data.toString());
            ws.request(1); // اطلب الرسالة التالية
            return null;
        }

        @Override
        public void onError(WebSocket ws, Throwable error) {
            if (onError != null) onError.accept(error);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
            if (onClose != null) onClose.run();
            return null;
        }
    }
}