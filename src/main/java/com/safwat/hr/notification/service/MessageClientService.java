package com.safwat.hr.notification.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.safwat.hr.notification.model.HRNotification;
import com.safwat.hr.notification.model.HRNotification.NotificationCategory;
import com.safwat.hr.notification.model.HRNotification.NotificationType;
import com.safwat.hr.notification.model.HRNotification.Priority;
import com.safwat.hr.notification.util.InboxStatsDTO;
import com.safwat.hr.notification.util.MessageSummaryDTO;
import com.safwat.hr.utils.ApiClient;
import com.safwat.hr.utils.ApiResponse;
import javafx.application.Platform;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * =====================================================
 * MessageClientService — خدمة الرسائل على جانب JavaFX
 * =====================================================
 * <p>
 * المسؤوليات:
 * 1. الاتصال بـ WebSocket واستقبال الرسائل الجديدة فوراً
 * 2. تحويل الرسالة الواردة إلى HRNotification وإرسالها للـ EventBus
 * 3. إرسال رسائل جديدة مع مرفقات عبر REST
 * 4. الرد على رسائل
 * 5. تحميل المرفقات
 * <p>
 * الاستخدام:
 * // بعد تسجيل الدخول مباشرة
 * MessageClientService.getInstance().connect();
 * <p>
 * // إرسال رسالة
 * MessageClientService.getInstance().sendMessage(
 * "ahmed", "موضوع", "نص الرسالة",
 * listOfFiles,
 * () -> showSuccess(),
 * err -> showError(err)
 * );
 */
public class MessageClientService {

    private static final MessageClientService INSTANCE = new MessageClientService();
    private final NotificationService notifService = NotificationService.getInstance();
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private ApiClient.WebSocketClient wsClient;
    private boolean connected = false;

    private MessageClientService() {
    }

    public static MessageClientService getInstance() {
        return INSTANCE;
    }

    // ===================== الاتصال =====================

    /**
     * يُستدعى مرة واحدة بعد تسجيل الدخول.
     * يتصل بـ WebSocket ويستمع للرسائل الواردة.
     */
    public void connect() {
        if (connected) {
            System.out.println("ℹ️ WebSocket متصل بالفعل");
            return;
        }

        String token = ApiClient.getAuthToken();
        String username = ApiClient.getUserName();

        if (token == null || token.isEmpty()) {
            System.err.println("❌ لا يوجد token للاتصال");
            scheduleReconnect();
            return;
        }

        System.out.println("🔗 [WebSocket] محاولة الاتصال للمستخدم: " + username);

        wsClient = new ApiClient.WebSocketClient(
                "",
                this::onMessageReceived,
                this::onError,
                this::onClose,
                token
        );

        wsClient.connect()
                .thenRun(() -> {
                    connected = true;
                    System.out.println("✅ [WebSocket] متصل بنجاح للمستخدم: " + username);

                })
                .exceptionally(e -> {
                    System.err.println("❌ [WebSocket] فشل الاتصال: " + e.getMessage());
                    scheduleReconnect();
                    return null;
                });
    }
// في MessageClientService.java

    /**
     * تحديث كل الرسائل (مقروء وغير مقروء) من الخادم
     * ودمجها مع الإشعارات المحلية
     */
    public void refreshAllMessages() {
        CompletableFuture.supplyAsync(() -> {
            try {
                // جلب كل الرسائل من الخادم
                var response = ApiClient.get("/messages/inbox?page=0&size=100", MessageSummaryDTO[].class);
                if (response.isSuccess() && response.getData() != null) {
                    return Arrays.asList(response.getData());
                }
                return List.<MessageSummaryDTO>of();
            } catch (Exception e) {
                System.err.println("❌ فشل جلب الرسائل: " + e.getMessage());
                return List.<MessageSummaryDTO>of();
            }
        }).thenAccept(messages -> {
            Platform.runLater(() -> {
                // إزالة جميع الرسائل القديمة من نوع MESSAGE
                NotificationService.getInstance().getAll().removeIf(HRNotification::isMessage);

                // إضافة الرسائل الجديدة كإشعارات
                for (MessageSummaryDTO msg : messages) {
                    HRNotification notification = HRNotification.builder()
                            .category(NotificationCategory.MESSAGE)
                            .type(NotificationType.MESSAGE)
                            .title(msg.getSubject() != null ? msg.getSubject() : "رسالة جديدة")
                            .message(msg.getPreview() != null ? msg.getPreview() : "")
                            .sender(msg.getSenderDisplayName() != null ? msg.getSenderDisplayName() : msg.getSenderUsername())
                            .timestamp(msg.getCreatedAt() != null ? msg.getCreatedAt() : LocalDateTime.now())
                            .read(msg.isRead()) // مهم: نحافظ على حالة القراءة من الخادم
                            .action("فتح الرسالة", "messages/" + msg.getId())
                            .build();
                    NotificationService.getInstance().send(notification);
                }

                // تحديث العداد
                NotificationService.getInstance().updateUnreadCount();
                System.out.println("✅ تم تحديث الرسائل: " + messages.size() + " رسالة");
            });
        });
    }

    /**
     * تحميل الرسائل غير المقروءة من قاعدة البيانات
     */
    public CompletableFuture<List<MessageSummaryDTO>> getUnreadMessages() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                TypeReference<ApiResponse<List<MessageSummaryDTO>>> typeRef =
                        new TypeReference<>() {
                        };

                var response = ApiClient.getWithTypeRef("/messages/inbox?page=0&size=100", typeRef);

                System.out.println("📦 === RESPONSE ===");
                System.out.println("Success: " + response.isSuccess());
                System.out.println("Message: " + response.getMessage());

                if (response.isSuccess() && response.getData() != null) {
                    // ✅ هنا response.getData() هي القائمة مباشرة
                    List<MessageSummaryDTO> allMessages = response.getData().getData();

                    List<MessageSummaryDTO> unread = allMessages.stream()
                            .filter(msg -> !msg.isRead())
                            .collect(Collectors.toList());

                    System.out.println("📬 Total messages: " + allMessages.size());
                    System.out.println("📬 Unread messages: " + unread.size());
                    return unread;
                }
                return List.of();
            } catch (Exception e) {
                System.err.println("❌ فشل جلب الرسائل غير المقروءة: " + e.getMessage());
                e.printStackTrace();
                return List.of();
            }
        });
    }

    public void testRawResponse() {
        try {
            // ✅ استخدم HttpClient مباشرة عشان تشوف الـ Response من غير تحويل
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(ApiClient.BASE_URL + "/messages/inbox?page=0&size=100"))
                    .header("Authorization", "Bearer " + ApiClient.getAuthToken())
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            java.net.http.HttpResponse<String> response = ApiClient.httpClient.send(
                    request,
                    java.net.http.HttpResponse.BodyHandlers.ofString()
            );

            System.out.println("📦 === RAW RESPONSE ===");
            System.out.println("Status Code: " + response.statusCode());
            System.out.println("Headers: " + response.headers().map());
            System.out.println("Body: " + response.body());
            System.out.println("======================");

        } catch (Exception e) {
            System.err.println("❌ خطأ: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * جلب إحصائيات الصندوق (عدد الرسائل غير المقروءة)
     */
    public CompletableFuture<InboxStatsDTO> getInboxStats() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var response = ApiClient.get("/messages/stats", InboxStatsDTO.class);
                if (response.isSuccess() && response.getData() != null) {
                    return response.getData();
                }
                return null;
            } catch (Exception e) {
                System.err.println("❌ فشل جلب الإحصائيات: " + e.getMessage());
                return null;
            }
        });
    }

    /**
     * تحميل وعرض جميع الرسائل غير المقروءة
     */
    public void loadUnreadMessagesAndNotify() {
        getInboxStats().thenAccept(stats -> {
            if (stats != null && stats.getUnreadCount() > 0) {
                System.out.println("📬 " + stats.getUnreadCount() + " رسائل غير مقروءة للمستخدم: " + ApiClient.getUserName());

                getUnreadMessages().thenAccept(messages -> {
                    Platform.runLater(() -> {
                        if (!messages.isEmpty()) {
                            // عرض كل رسالة كإشعار
                            for (MessageSummaryDTO msg : messages) {
                                showMessageNotification(msg);
                            }

                            // تحديث عداد الجرس
                            NotificationService.getInstance().updateUnreadCount((int) stats.getUnreadCount());
                            System.out.println("✅ تم عرض " + messages.size() + " رسائل غير مقروءة");
                        }
                    });
                });
            } else {
                System.out.println("📭 لا توجد رسائل غير مقروءة للمستخدم: " + ApiClient.getUserName());
            }
        });
    }

    /**
     * عرض رسالة كإشعار
     */
    private void showMessageNotification(MessageSummaryDTO msg) {
        // تحويل MessageSummaryDTO إلى HRNotification
        HRNotification notification = HRNotification.builder()
                .category(NotificationCategory.MESSAGE)
                .type(NotificationType.MESSAGE)
                .priority(Priority.NORMAL)
                .title(msg.getSubject() != null ? msg.getSubject() : "رسالة جديدة")
                .message(msg.getPreview() != null ? msg.getPreview() : "")
                .sender(msg.getSenderDisplayName() != null ? msg.getSenderDisplayName() : msg.getSenderUsername())
                .timestamp(msg.getCreatedAt() != null ? msg.getCreatedAt() : LocalDateTime.now())
                .action("فتح الرسالة", "messages/" + msg.getId())
                .build();

        NotificationService.getInstance().send(notification);
    }

    public void printStatus() {
        System.out.println("=== WebSocket Status ===");
        System.out.println("Connected: " + connected);
        System.out.println("Token: " + (ApiClient.getAuthToken() != null ? "✅" : "❌"));
        System.out.println("=========================");
    }

    // ===================== استقبال رسائل WebSocket =====================
    private void onMessageReceived(String json) {
        try {
            MessageNotificationDTO dto = mapper.readValue(json, MessageNotificationDTO.class);

            Platform.runLater(() -> {
                HRNotification notification = HRNotification.builder()
                        .category(NotificationCategory.MESSAGE)
                        .type(NotificationType.MESSAGE)
                        .priority(Priority.NORMAL)
                        .title(dto.subject != null ? dto.subject : "رسالة جديدة")
                        .message(dto.preview != null ? dto.preview : "")
                        .sender(dto.senderDisplayName != null
                                ? dto.senderDisplayName : dto.senderUsername)
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
        scheduleReconnect();
    }

    private void onClose() {
        System.out.println("[MessageClientService] WebSocket مغلق");
        connected = false;
    }

    private void scheduleReconnect() {
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                connect();
            } catch (InterruptedException ignored) {
            }
        }, "ws-reconnect").start();
    }

    // ===================== إرسال رسالة =====================

    /**
     * @param recipientUsername اسم المستخدم المستقبل
     * @param subject           موضوع الرسالة
     * @param body              نص الرسالة
     * @param attachments       قائمة مسارات الملفات (يمكن أن تكون فارغة)
     * @param onSuccess         يُنفَّذ على JavaFX Thread عند النجاح
     * @param onError           يُنفَّذ على JavaFX Thread عند الخطأ
     */
    public void sendMessage(String recipientUsername,
                            String subject,
                            String body,
                            List<Path> attachments,
                            Runnable onSuccess,
                            Consumer<String> onError) {

        new Thread(() -> {
            try {
                // دائماً استخدم multipart/form-data
                Map<String, Object> formData = new java.util.HashMap<>();

                // البيانات كـ Map (سيتم تحويلها إلى JSON في ApiClient.uploadFile)
                Map<String, String> data = new java.util.HashMap<>();
                data.put("recipientUsername", recipientUsername);
                data.put("subject", subject != null ? subject : "");
                data.put("body", body != null ? body : "");
                formData.put("data", data);

                // إضافة المرفقات إن وجدت
                if (attachments != null && !attachments.isEmpty()) {
                    for (int i = 0; i < attachments.size(); i++) {
                        formData.put("files", attachments.get(i));
                    }
                }

                // دائماً استخدم uploadFile
                var response = ApiClient.uploadFile("/messages", formData, Object.class);
                handleResponse(response.isSuccess(), response.getMessage(), onSuccess, onError);

            } catch (Exception e) {
                Platform.runLater(() -> onError.accept(e.getMessage()));
            }
        }, "send-message").start();
    }

    // ===================== الرد على رسالة =====================
    public void replyToMessage(Long parentId,
                               String body,
                               List<Path> attachments,
                               Runnable onSuccess,
                               Consumer<String> onError) {

        new Thread(() -> {
            try {
                // دائماً استخدم multipart/form-data
                Map<String, Object> formData = new java.util.HashMap<>();

                // البيانات
                Map<String, Object> data = new java.util.HashMap<>();
                data.put("parentId", parentId);
                data.put("body", body != null ? body : "");
                formData.put("data", data);

                // إضافة المرفقات إن وجدت
                if (attachments != null && !attachments.isEmpty()) {
                    for (Path file : attachments) {
                        formData.put("files", file);
                    }
                }

                // دائماً استخدم uploadFile
                var response = ApiClient.uploadFile("/messages/reply", formData, Object.class);
                handleResponse(response.isSuccess(), response.getMessage(), onSuccess, onError);

            } catch (Exception e) {
                Platform.runLater(() -> onError.accept(e.getMessage()));
            }
        }, "reply-message").start();
    }

    public CompletableFuture<Void> markMessageAsRead(Long messageId) {
        return CompletableFuture.runAsync(() -> {
            try {
                var response = ApiClient.put("/messages/" + messageId + "/read", null, Void.class);
                if (response.isSuccess()) {
                    System.out.println("✅ تم تعليم الرسالة " + messageId + " كمقروءة");
                } else {
                    System.err.println("❌ فشل تعليم الرسالة كمقروءة: " + response.getMessage());
                }
            } catch (Exception e) {
                System.err.println("❌ خطأ في تعليم الرسالة كمقروءة: " + e.getMessage());
            }
        });
    }

    // ===================== تحميل مرفق =====================
    public void downloadAttachment(String token,
                                   Path targetPath,
                                   Runnable onSuccess,
                                   Consumer<String> onError) {

        ApiClient.downloadFileAsync("/messages/attachments/" + token, null, targetPath)
                .thenAccept(ok -> {
                    if (ok) Platform.runLater(onSuccess);
                    else Platform.runLater(() -> onError.accept("فشل التحميل"));
                })
                .exceptionally(e -> {
                    Platform.runLater(() -> onError.accept(e.getMessage()));
                    return null;
                });
    }

    // ===================== مساعدات =====================
    private void handleResponse(boolean success, String errorMsg,
                                Runnable onSuccess, Consumer<String> onError) {
        if (success) Platform.runLater(onSuccess);
        else Platform.runLater(() -> onError.accept(errorMsg));
    }

    private String buildAvatar(String displayName) {
        if (displayName == null || displayName.isBlank()) return "؟";
        String[] parts = displayName.trim().split("\\s+");
        if (parts.length == 1) return String.valueOf(parts[0].charAt(0));
        return "" + parts[0].charAt(0) + parts[1].charAt(0);
    }

    // DTO داخلي لتحليل رسائل WebSocket الواردة
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class MessageNotificationDTO {
        public Long messageId;
        public String senderUsername;
        public String senderDisplayName;
        public String subject;
        public String preview;
        public int attachmentsCount;
        public LocalDateTime createdAt;
    }
}
