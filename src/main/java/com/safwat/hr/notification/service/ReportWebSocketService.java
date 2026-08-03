package com.safwat.hr.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safwat.hr.notification.event.HREventBus;
import com.safwat.hr.notification.model.HRNotification;
import com.safwat.hr.notification.ui.HRToast;
import com.safwat.hr.utils.ApiClient;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * خدمة WebSocket لاستقبال إشعارات التقارير.
 *
 * <p><b>إصلاحات:</b>
 * <ol>
 *   <li><b>تجميع الـ frames:</b> الـ STOMP frame ممكن يجي على أجزاء متعددة
 *       ({@code last=false}). الكود القديم كان يعالج كل جزء لوحده ويضيع الـ frame.
 *       الآن نجمع الأجزاء في {@code frameBuffer} حتى {@code last=true}.</li>
 *   <li><b>إعادة الاتصال التلقائي:</b> لو الـ connection انقطع، يعيد الاتصال
 *       بعد 5 ثواني تلقائياً.</li>
 *   <li><b>STOMP CONNECT format:</b> فراغ بعد ":" في الـ headers للتوافق مع Spring.</li>
 * </ol>
 */
public class ReportWebSocketService {

    private static final ObjectMapper mapper = ApiClient.mapper;
    private static final int RECONNECT_DELAY_SEC = 5;

    private final Stage primaryStage;
    private final String token;
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
    private WebSocket webSocket;

    public ReportWebSocketService(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.token = ApiClient.getAuthToken();
    }

    // ─────────────────────────────────────────────
    //  Connect
    // ─────────────────────────────────────────────

    /**
     * يُنشئ اتصال WebSocket ويُرسِل STOMP CONNECT عند الفتح.
     */
    public void connect() {
        intentionalClose.set(false);
        String wsUrl = ApiClient.BASE_URL2();
        System.out.println("[ReportWS] 🔌 Connecting to: " + wsUrl);

        HttpClient client = ApiClient.httpClient;
        WebSocket.Builder builder = client.newWebSocketBuilder();

        // الـ JWT في HTTP Upgrade header — بيُستخدَم لو الـ Security بتشيك هنا
        if (token != null && !token.isEmpty()) {
            builder.header("Authorization", "Bearer " + token);
        }

        builder.buildAsync(URI.create(wsUrl), new WebSocket.Listener() {

            @Override
            public void onOpen(WebSocket ws) {
                webSocket = ws;
                frameBuffer.setLength(0);
                System.out.println("[ReportWS] ✅ WebSocket opened — sending STOMP CONNECT");

                // STOMP CONNECT — الـ JWT في Native Header عشان WebSocketAuthInterceptor يقرأه
                // فراغ بعد ":" مهم للتوافق مع Spring STOMP parser
                String connectFrame =
                        "CONNECT\n" +
                                "accept-version:1.2\n" +
                                "heart-beat:0,0\n" +
                                "Authorization:Bearer " + token + "\n" +
                                "\n\u0000";

                ws.sendText(connectFrame, true);
                ws.request(1);
            }

            @Override
            public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
                // ── تجميع الأجزاء ──
                frameBuffer.append(data);

                if (!last) {
                    // الـ frame لسه ناقص — طلب الجزء التالي وانتظر
                    ws.request(1);
                    return null;
                }

                // الـ frame اكتمل — عالجه
                String frame = frameBuffer.toString();
                frameBuffer.setLength(0);

                System.out.println("[ReportWS] 📩 Frame received:\n" + frame.substring(0, Math.min(200, frame.length())));

                handleFrame(frame);
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
                System.out.println("[ReportWS] 🔌 Closed: " + reason + " (code: " + statusCode + ")");
                if (!intentionalClose.get()) {
                    scheduleReconnect();
                }
                return null;
            }
        }).exceptionally(e -> {
            System.err.println("[ReportWS] ❌ Connection failed: " + e.getMessage());
            scheduleReconnect();
            return null;
        });
    }

    // ─────────────────────────────────────────────
    //  Frame Handling
    // ─────────────────────────────────────────────

    private void handleFrame(String frame) {
        if (frame.startsWith("CONNECTED")) {
            System.out.println("[ReportWS] ✅ STOMP connected — subscribing to /user/queue/reports");

            // STOMP SUBSCRIBE
            String subscribeFrame =
                    "SUBSCRIBE\n" +
                            "id:sub-reports\n" +
                            "destination:/user/queue/reports\n" +
                            "ack:auto\n" +
                            "\n\u0000";

            if (webSocket != null) {
                webSocket.sendText(subscribeFrame, true);
            }

        } else if (frame.startsWith("MESSAGE")) {
            System.out.println("[ReportWS] 📨 MESSAGE received");
            String body = extractStompBody(frame);
            if (body != null && !body.isBlank()) {
                handleReportNotification(body);
            }

        } else if (frame.startsWith("ERROR")) {
            System.err.println("[ReportWS] ⚠️ STOMP ERROR: " + frame);

        } else if (frame.startsWith("HEARTBEAT") || frame.isBlank() || frame.equals("\n")) {
            // Heartbeat — تجاهل
        } else {
            System.out.println("[ReportWS] ❓ Unknown frame type: " +
                    frame.substring(0, Math.min(50, frame.length())));
        }
    }

    /**
     * يستخرج الـ body من STOMP frame.
     *
     * <p>STOMP frame structure:
     * <pre>
     * COMMAND\n
     * header1:value1\n
     * header2:value2\n
     * \n                ← سطر فارغ يفصل الـ headers عن الـ body
     * {body}\u0000
     * </pre>
     */
    private String extractStompBody(String frame) {
        // الـ body يبدأ بعد أول سطر فارغ (\n\n)
        int bodyStart = frame.indexOf("\n\n");
        if (bodyStart == -1) return null;

        String body = frame.substring(bodyStart + 2);

        // حذف الـ null terminator من النهاية
        int nullIndex = body.indexOf('\u0000');
        if (nullIndex >= 0) {
            body = body.substring(0, nullIndex);
        }

        return body.trim();
    }

    // ─────────────────────────────────────────────
    //  Notification Handling
    // ─────────────────────────────────────────────

    private void handleReportNotification(String json) {
        try {
            ReportNotificationPayload payload = mapper.readValue(json, ReportNotificationPayload.class);
            System.out.println("[ReportWS] 📋 Notification: reportId=" + payload.reportId +
                    ", status=" + payload.status);

            Platform.runLater(() -> {
                var type = switch (payload.status != null ? payload.status : "") {
                    case "COMPLETED" -> HRNotification.NotificationType.SYSTEM;
                    case "FAILED" -> HRNotification.NotificationType.TASK;
                    default -> HRNotification.NotificationType.SYSTEM;
                };

                var priority = "FAILED".equals(payload.status)
                        ? HRNotification.Priority.HIGH
                        : HRNotification.Priority.NORMAL;

                HRNotification notification = HRNotification.builder()
                        .title("تقرير: " + payload.reportName)
                        .message(payload.message)
                        .type(type)
                        .category(HRNotification.NotificationCategory.SYSTEM)
                        .priority(priority)
                        // .action("عرض التقرير", "/reports/" + payload.reportId)
                        .build();

                HREventBus.getInstance().publish(notification);

                if (primaryStage != null && primaryStage.isShowing()) {
                    HRToast.show(primaryStage, notification);
                }
            });

        } catch (Exception e) {
            System.err.println("[ReportWS] ⚠️ فشل قراءة الإشعار: " + e.getMessage());
            System.err.println("[ReportWS] Raw JSON: " + json);
        }
    }

    // ─────────────────────────────────────────────
    //  Reconnect
    // ─────────────────────────────────────────────

    private void scheduleReconnect() {
        if (intentionalClose.get()) return;
        System.out.println("[ReportWS] ⏳ إعادة الاتصال بعد " + RECONNECT_DELAY_SEC + " ثوانٍ...");
        reconnectScheduler.schedule(this::connect, RECONNECT_DELAY_SEC, TimeUnit.SECONDS);
    }

    // ─────────────────────────────────────────────
    //  Disconnect
    // ─────────────────────────────────────────────

    public void disconnect() {
        intentionalClose.set(true);
        reconnectScheduler.shutdown();
        if (webSocket != null) {
            webSocket.sendText("DISCONNECT\n\n\u0000", true);
            webSocket.sendClose(1000, "Client closing");
            System.out.println("[ReportWS] 🔌 Disconnected");
        }
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