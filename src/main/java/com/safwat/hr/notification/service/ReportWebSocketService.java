package com.safwat.hr.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safwat.hr.network.HttpCore;
import com.safwat.hr.notification.event.HREventBus;
import com.safwat.hr.notification.model.HRNotification;
import com.safwat.hr.notification.ui.HRToast;
import com.safwat.hr.shared.AppConfig;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ══════════════════════════════════════════════════════════════════
 * ReportWebSocketService — إشعارات التقارير عبر STOMP/WebSocket
 * ══════════════════════════════════════════════════════════════════
 * <p>
 * هذا الكلاس متخصص في STOMP protocol ويختلف عن {@link com.safwat.hr.network.WebSocketClient}
 * العام — لذلك يُدار بشكل مستقل ولا يرث منه.
 *
 * <p><b>التغيير الرئيسي:</b> بقى يستخدم {@link HttpCore} بدل
 * {@code ApiClient} مباشرة — لأن {@code HttpCore} هو المالك
 * الحقيقي للـ {@code HttpClient} والـ token.
 *
 * <p><b>المميزات:</b>
 * <ol>
 *   <li><b>Frame buffering:</b> يجمع أجزاء الـ STOMP frame حتى {@code last=true}
 *       — بيحل مشكلة الـ frames الكبيرة اللي بتجي على أجزاء.</li>
 *   <li><b>Auto-reconnect:</b> بعد 5 ثواني من أي انقطاع غير متعمد.</li>
 *   <li><b>Intentional close:</b> {@link #disconnect()} يوقف الـ reconnect.</li>
 * </ol>
 */
public class ReportWebSocketService {

    // ─────────────────────────────────────────────
    //  Constants
    // ─────────────────────────────────────────────

    private static final int RECONNECT_DELAY_SEC = 5;
    private static final String STOMP_DESTINATION = "/user/queue/reports";
    private static final String SUB_ID = "sub-reports";

    // ─────────────────────────────────────────────
    //  Dependencies — من HttpCore مباشرة
    // ─────────────────────────────────────────────

    /**
     * ObjectMapper مشترك مع باقي الـ network layer
     */
    private static final ObjectMapper mapper = HttpCore.getInstance().mapper;

    // ─────────────────────────────────────────────
    //  State
    // ─────────────────────────────────────────────

    private final Stage primaryStage;
    private final String token;
    private final String wsUrl;

    /**
     * يجمع أجزاء الـ STOMP frame حتى last=true
     */
    private final StringBuilder frameBuffer = new StringBuilder();

    /**
     * يمنع تشغيل reconnect بعد disconnect متعمد
     */
    private final AtomicBoolean intentionalClose = new AtomicBoolean(false);

    private final ScheduledExecutorService reconnectScheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "ws-reconnect");
                t.setDaemon(true);
                return t;
            });

    /**
     * الاتصال الفعلي — null قبل connect() أو بعد disconnect()
     */
    private volatile WebSocket webSocket;

    // ─────────────────────────────────────────────
    //  Constructor
    // ─────────────────────────────────────────────

    public ReportWebSocketService(Stage primaryStage) {
        this.primaryStage = primaryStage;
        // نأخذ token و URL من HttpCore مرة واحدة عند الإنشاء
        HttpCore core = HttpCore.getInstance();
        this.token = core.getAuthToken();
        this.wsUrl = core.getBaseWsUrl();
    }

    // ─────────────────────────────────────────────
    //  Public API
    // ─────────────────────────────────────────────

    /**
     * يُنشئ اتصال WebSocket ويُرسِل STOMP CONNECT عند الفتح.
     * آمن للاستدعاء مرات متعددة — كل استدعاء يبدأ اتصالاً جديداً.
     */
    public void connect() {
        if (!AppConfig.getBoolean("notifications", "reportsEnabled", true)) {
            System.out.println("[ReportWS] إشعارات التقارير معطّلة — تجاهل الاتصال");
            return;
        }
        intentionalClose.set(false);

        // نأخذ HttpClient من HttpCore — نفس الـ instance المشترك
        HttpCore.getInstance().httpClient
                .newWebSocketBuilder()
                .header("Authorization", "Bearer " + token)
                .buildAsync(URI.create(wsUrl), new StompListener())
                .exceptionally(e -> {
                    System.err.println("[ReportWS] ❌ فشل الاتصال: " + e.getMessage());
                    scheduleReconnect();
                    return null;
                });
    }

    /**
     * يُغلق الاتصال بشكل نظيف ويوقف الـ auto-reconnect.
     */
    public void disconnect() {
        intentionalClose.set(true);
        reconnectScheduler.shutdown();

        if (webSocket != null) {
            webSocket.sendText(StompFrames.disconnect(), true);
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Client closing");
        }
    }

    // ─────────────────────────────────────────────
    //  STOMP Listener (Inner Class)
    // ─────────────────────────────────────────────

    /**
     * يُعالج أحداث WebSocket ويُفسّر STOMP frames.
     * مفصول في inner class لتنظيف كود connect().
     */
    private class StompListener implements WebSocket.Listener {

        @Override
        public void onOpen(WebSocket ws) {
            webSocket = ws;
            frameBuffer.setLength(0);
            // STOMP CONNECT — الـ JWT في Native Header عشان WebSocketAuthInterceptor يقرأه
            ws.sendText(StompFrames.connect(token), true);
            ws.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
            // ── تجميع أجزاء الـ frame ──
            frameBuffer.append(data);

            if (!last) {
                // الـ frame لسه ناقص — طلب الجزء التالي
                ws.request(1);
                return null;
            }

            // الـ frame اكتمل
            String frame = frameBuffer.toString();
            frameBuffer.setLength(0);

            handleFrame(ws, frame);
            ws.request(1);
            return null;
        }

        @Override
        public void onError(WebSocket ws, Throwable error) {
            System.err.println("[ReportWS] ❌ Error: " + error.getMessage());
            scheduleReconnect();
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
            if (!intentionalClose.get()) scheduleReconnect();
            return null;
        }
    }

    // ─────────────────────────────────────────────
    //  Frame Handling
    // ─────────────────────────────────────────────

    private void handleFrame(WebSocket ws, String frame) {
        if (frame.startsWith("CONNECTED")) {
            // STOMP handshake ناجح — اشترك في الـ queue
            ws.sendText(StompFrames.subscribe(SUB_ID, STOMP_DESTINATION), true);

        } else if (frame.startsWith("MESSAGE")) {
            String body = extractStompBody(frame);
            if (body != null && !body.isBlank())
                handleReportNotification(body);

        } else if (frame.startsWith("ERROR")) {
            System.err.println("[ReportWS] ⚠️ STOMP ERROR:\n" + frame);

        } else if (frame.isBlank() || frame.startsWith("HEARTBEAT")) {
            // Heartbeat — تجاهل بصمت

        } else {
            System.err.println("[ReportWS] ⚠️ Unknown frame: "
                    + frame.substring(0, Math.min(60, frame.length())));
        }
    }

    /**
     * يستخرج الـ body من STOMP frame.
     *
     * <p>بنية الـ STOMP frame:
     * <pre>
     * COMMAND\n
     * header1:value1\n
     * \n               ← سطر فارغ يفصل headers عن body
     * {body}\u0000
     * </pre>
     */
    private String extractStompBody(String frame) {
        int bodyStart = frame.indexOf("\n\n");
        if (bodyStart == -1) return null;

        String body = frame.substring(bodyStart + 2);

        // حذف الـ null terminator
        int nullIndex = body.indexOf('\u0000');
        if (nullIndex >= 0) body = body.substring(0, nullIndex);

        return body.trim();
    }

    // ─────────────────────────────────────────────
    //  Notification Handling
    // ─────────────────────────────────────────────

    private void handleReportNotification(String json) {
        try {
            ReportNotificationPayload payload =
                    mapper.readValue(json, ReportNotificationPayload.class);

            Platform.runLater(() -> publishNotification(payload));

        } catch (Exception e) {
            System.err.println("[ReportWS] ⚠️ فشل قراءة الإشعار: " + e.getMessage());
            System.err.println("[ReportWS] Raw JSON: " + json);
        }
    }

    private void publishNotification(ReportNotificationPayload payload) {
        boolean failed = "FAILED".equals(payload.status);

        HRNotification notification = HRNotification.builder()
                .title("تقرير: " + payload.reportName)
                .message(payload.message)
                .type(failed
                        ? HRNotification.NotificationType.TASK
                        : HRNotification.NotificationType.SYSTEM)
                .category(HRNotification.NotificationCategory.SYSTEM)
                .priority(failed
                        ? HRNotification.Priority.HIGH
                        : HRNotification.Priority.NORMAL)
                .build();

        HREventBus.getInstance().publish(notification);

        if (primaryStage != null && primaryStage.isShowing())
            HRToast.show(primaryStage, notification);
    }

    // ─────────────────────────────────────────────
    //  Reconnect
    // ─────────────────────────────────────────────

    private void scheduleReconnect() {
        if (intentionalClose.get()) return;
        reconnectScheduler.schedule(this::connect, RECONNECT_DELAY_SEC, TimeUnit.SECONDS);
    }

    // ─────────────────────────────────────────────
    //  STOMP Frame Builder (Static Utility)
    // ─────────────────────────────────────────────

    /**
     * يبني STOMP frames كـ strings — مفصولة عن الـ logic لسهولة الاختبار.
     *
     * <p><b>ملاحظة:</b> فراغ بعد ":" في الـ headers مهم
     * للتوافق مع Spring STOMP parser.
     */
    private static final class StompFrames {

        private StompFrames() {
        }

        static String connect(String token) {
            return "CONNECT\n"
                    + "accept-version:1.2\n"
                    + "heart-beat:0,0\n"
                    + "Authorization:Bearer " + token + "\n"
                    + "\n\u0000";
        }

        static String subscribe(String id, String destination) {
            return "SUBSCRIBE\n"
                    + "id:" + id + "\n"
                    + "destination:" + destination + "\n"
                    + "ack:auto\n"
                    + "\n\u0000";
        }

        static String disconnect() {
            return "DISCONNECT\n\n\u0000";
        }
    }

    /**
     * إيقاف اتصال إشعارات التقارير — يُستدعى براحتك من أي مكان
     * (مثلاً لما المستخدم يوقف الإعداد من شاشة الإعدادات وهو شغال بالفعل).
     * بديل واضح الاسم عن disconnect() لنفس الغرض.
     */
    public void stop() {
        disconnect();
    }

    /**
     * تفعيل/تعطيل الخدمة حسب قيمة منطقية — يتجاهل الاتصال لو already في نفس الحالة.
     * مفيد لو عندك toggle في شاشة الإعدادات وعايز تطبّقه فورًا بدون reLogin.
     *
     * @param enabled true = اتصل (لو مش متصل بالفعل), false = افصل
     */
    public void setEnabled(boolean enabled) {
        if (enabled) {
            connect();
        } else {
            stop();
        }
    }

    /**
     * @return true لو الاتصال شغال حاليًا
     */
    public boolean isConnected() {
        return webSocket != null && !intentionalClose.get();
    }
    // ─────────────────────────────────────────────
    //  DTO
    // ─────────────────────────────────────────────

    public static class ReportNotificationPayload {
        public Long reportId;
        public String reportName;
        public String status;
        public String message;
        public String timestamp;
    }
}