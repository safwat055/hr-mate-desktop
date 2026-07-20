package com.safwat.hr.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.safwat.hr.notification.event.HREventBus;
import com.safwat.hr.notification.model.HRNotification;
import com.safwat.hr.notification.model.HRNotification.*;
import com.safwat.hr.utils.ApiClient;
import javafx.application.Platform;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * خدمة الرسائل على جانب JavaFX.
 *
 * المسؤوليات:
 *  1. الاتصال بـ WebSocket واستقبال الرسائل الجديدة
 *  2. تحويل الرسائل الواردة إلى HRNotification وإرسالها لـ HREventBus
 *  3. إرسال رسائل جديدة عبر REST API
 *  4. تحميل المرفقات
 */
public class MessageClientService {

    private static final MessageClientService INSTANCE = new MessageClientService();
    public static MessageClientService getInstance() { return INSTANCE; }

    private final NotificationService notifService = NotificationService.getInstance();
    private final ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private ApiClient.WebSocketClient wsClient;
    private boolean connected = false;

    private MessageClientService() {}

    // ===================== الاتصال بـ WebSocket =====================
    /**
     * يُستدعى بعد تسجيل الدخول مباشرة.
     */
    public void connect() {
        if (connected) return;

        wsClient = new ApiClient.WebSocketClient(
            "/ws/messages",          // المسار
            this::onMessageReceived, // عند وصول رسالة
            this::onError,           // عند خطأ
            this::onClose            // عند الإغلاق
        );

        wsClient.connect().thenRun(() -> {
            connected = true;
            System.out.println("[MessageClientService] WebSocket متصل");
        });
    }

    public void disconnect() {
        if (wsClient != null) wsClient.close();
        connected = false;
    }

    // ===================== استقبال رسائل WebSocket =====================
    private void onMessageReceived(String json) {
        try {
            // تحويل JSON لـ DTO
            MessageNotificationDTO dto = mapper.readValue(json, MessageNotificationDTO.class);

            // تحويل لـ HRNotification وإرساله لـ EventBus على JavaFX thread
            Platform.runLater(() -> {
                HRNotification notification = HRNotification.builder()
                    .category(NotificationCategory.MESSAGE)
                    .priority(Priority.NORMAL)
                    .title(dto.subject)
                    .message(dto.preview)
                    .sender(dto.senderDisplayName)
                    .senderAvatar(buildAvatar(dto.senderDisplayName))
                    .timestamp(dto.createdAt != null ? dto.createdAt : LocalDateTime.now())
                    .action("فتح الرسالة", "messages/" + dto.messageId)
                    .build();

                notifService.send(notification);
            });

        } catch (Exception e) {
            System.err.println("[MessageClientService] خطأ في تحليل الرسالة: " + e.getMessage());
        }
    }

    private void onError(Throwable error) {
        System.err.println("[MessageClientService] WebSocket خطأ: " + error.getMessage());
        connected = false;
        // إعادة الاتصال بعد 5 ثوانٍ
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                connect();
            } catch (InterruptedException ignored) {}
        }, "ws-reconnect").start();
    }

    private void onClose() {
        connected = false;
        System.out.println("[MessageClientService] WebSocket مغلق");
    }

    // ===================== إرسال رسالة =====================
    /**
     * إرسال رسالة مع مرفقات.
     */
    public void sendMessage(String recipientUsername,
                            String subject,
                            String body,
                            java.util.List<Path> attachments,
                            Runnable onSuccess,
                            java.util.function.Consumer<String> onError) {

        new Thread(() -> {
            try {
                if (attachments != null && !attachments.isEmpty()) {
                    // إرسال مع مرفقات عبر multipart
                    Map<String, Object> formData = new java.util.HashMap<>();
                    formData.put("data", Map.of(
                        "recipientUsername", recipientUsername,
                        "subject", subject,
                        "body", body
                    ));
                    for (int i = 0; i < attachments.size(); i++) {
                        formData.put("files", attachments.get(i));
                    }
                    var response = ApiClient.uploadFile("/messages", formData, Object.class);
                    if (response.isSuccess()) {
                        Platform.runLater(onSuccess);
                    } else {
                        Platform.runLater(() -> onError.accept(response.getMessage()));
                    }
                } else {
                    // إرسال بدون مرفقات
                    var body2 = Map.of(
                        "recipientUsername", recipientUsername,
                        "subject", subject,
                        "body", body
                    );
                    var response = ApiClient.post("/messages", body2, Object.class);
                    if (response.isSuccess()) {
                        Platform.runLater(onSuccess);
                    } else {
                        Platform.runLater(() -> onError.accept(response.getMessage()));
                    }
                }
            } catch (Exception e) {
                Platform.runLater(() -> onError.accept(e.getMessage()));
            }
        }, "send-message").start();
    }

    // ===================== تحميل مرفق =====================
    public void downloadAttachment(String token,
                                   Path targetPath,
                                   Runnable onSuccess,
                                   java.util.function.Consumer<String> onError) {

        ApiClient.downloadFileAsync("/messages/attachments/" + token, null, targetPath)
            .thenAccept(ok -> {
                if (ok) Platform.runLater(onSuccess);
                else    Platform.runLater(() -> onError.accept("فشل التحميل"));
            })
            .exceptionally(e -> {
                Platform.runLater(() -> onError.accept(e.getMessage()));
                return null;
            });
    }

    // ===================== مساعدات =====================
    private String buildAvatar(String displayName) {
        if (displayName == null || displayName.isBlank()) return "؟";
        String[] parts = displayName.trim().split("\\s+");
        if (parts.length == 1) return String.valueOf(parts[0].charAt(0));
        return "" + parts[0].charAt(0) + parts[1].charAt(0);
    }

    // DTO داخلي لتحليل رسائل WebSocket
    private static class MessageNotificationDTO {
        public Long          messageId;
        public String        senderUsername;
        public String        senderDisplayName;
        public String        subject;
        public String        preview;
        public int           attachmentsCount;
        public LocalDateTime createdAt;
    }
}
