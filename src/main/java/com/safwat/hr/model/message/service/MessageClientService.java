package com.safwat.hr.model.message.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.safwat.hr.model.message.InboxStatsDTO;
import com.safwat.hr.model.message.MessageSummaryDTO;
import com.safwat.hr.model.message.UserInfo;
import com.safwat.hr.notification.model.HRNotification;
import com.safwat.hr.notification.model.HRNotification.NotificationCategory;
import com.safwat.hr.notification.model.HRNotification.NotificationType;
import com.safwat.hr.notification.model.HRNotification.Priority;
import com.safwat.hr.notification.service.NotificationService;
import com.safwat.hr.utils.ApiClient;
import com.safwat.hr.utils.ApiResponse;
import javafx.application.Platform;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * =====================================================================
 * MessageClientService
 * =====================================================================
 * الخدمة الرئيسية للتواصل مع الخادم فيما يتعلق بالرسائل.
 * تدير اتصال WebSocket STOMP للإشعارات الفورية،
 * وتنفذ طلبات REST API لجلب وإرسال وتحديث الرسائل.
 * تعمل كـ Singleton.
 * <p>
 * المسؤوليات:
 * - الاتصال بالـ WebSocket وإعادة الاتصال تلقائياً
 * - استقبال الرسائل الجديدة عبر STOMP وتحويلها لإشعارات
 * - جلب تفاصيل الرسائل والمحادثات كاملة
 * - إرسال رسائل جديدة لعدة مستلمين
 * - الرد على الرسائل
 * - تحميل المرفقات
 * - جلب قائمة المستخدمين
 */
public class MessageClientService {

    private static final MessageClientService INSTANCE = new MessageClientService();
    private static final DateTimeFormatter SERVER_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final NotificationService notifService = NotificationService.getInstance();
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule()
                    .addDeserializer(LocalDateTime.class,
                            new LocalDateTimeDeserializer(SERVER_DATE_FORMAT))
            )
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean connecting = new AtomicBoolean(false);
    private WebSocketStompClient stompClient;
    private StompSession stompSession;
    private ThreadPoolTaskScheduler scheduler;
    private StompSession.Subscription messageSubscription;

    private MessageClientService() {
    }

    /**
     * ترجع النسخة الوحيدة من الخدمة.
     *
     * @return INSTANCE
     */
    public static MessageClientService getInstance() {
        return INSTANCE;
    }

    /**
     * بناء مسار API نسبي وتوحيد الشرطات المائلة.
     *
     * @param path المسار النسبي
     * @return المسار بعد التنظيف
     */
    private String buildApiUrl(String path) {
        path = path.trim();
        if (!path.startsWith("/")) path = "/" + path;
        path = path.replaceAll("//+", "/");
        return path;
    }

    // =====================================================================
    //  STOMP WebSocket Connect
    // =====================================================================

    /**
     * بدء الاتصال بخادم WebSocket STOMP.
     * إذا كان هناك اتصال قائم أو جاري لا يتم فعل شيء.
     * بعد الاتصال يتم الاشتراك في قناة الرسائل وجلب الإشعارات غير المقروءة.
     */
    public void connect() {
        if (connected.get() || connecting.getAndSet(true)) {
            System.out.println("[MessageClientService] Already connected/connecting");
            return;
        }

        String token = ApiClient.getAuthToken();
        String username = ApiClient.getUserName();

        if (token == null || token.isEmpty()) {
            System.err.println("[MessageClientService] لا يوجد token");
            scheduleReconnect();
            return;
        }

        scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("msg-stomp-heartbeat-");
        scheduler.initialize();

        StandardWebSocketClient wsClient = new StandardWebSocketClient();
        stompClient = new WebSocketStompClient(wsClient);
        stompClient.setTaskScheduler(scheduler);

        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(mapper);
        stompClient.setMessageConverter(converter);

        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.add("Authorization", "Bearer " + token);

        String wsUrl = getWebSocketUrl();

        System.out.println("[MessageClientService] Connecting STOMP to: " + wsUrl);

        stompClient.connectAsync(wsUrl, headers, new StompSessionHandlerAdapter() {

            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                stompSession = session;
                connected.set(true);
                connecting.set(false);
                System.out.println("[MessageClientService] STOMP Connected");

                subscribeToMessages();
                loadUnreadMessagesAndNotify();
                refreshAllMessages();
            }

            @Override
            public void handleException(StompSession s,
                                        StompCommand command,
                                        StompHeaders headers,
                                        byte[] payload,
                                        Throwable exception) {
                System.err.println("[MessageClientService] STOMP Exception: " + exception.getMessage());
            }

            @Override
            public void handleTransportError(StompSession s, Throwable exception) {
                connected.set(false);
                connecting.set(false);
                System.err.println("[MessageClientService] Transport error: " + exception.getMessage());
                scheduleReconnect();
            }
        });
    }

    /**
     * الاشتراك في قناة /user/queue/messages لاستقبال الرسائل الجديدة.
     */
    private void subscribeToMessages() {
        if (!isReady()) return;

        String destination = "/user/queue/messages";

        messageSubscription = stompSession.subscribe(destination, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return MessageNotificationDTO.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                if (payload instanceof MessageNotificationDTO dto) {
                    System.out.println("[MessageClientService] New message via STOMP: id=" + dto.messageId);
                    handleIncomingMessage(dto);
                }
            }
        });

        System.out.println("[MessageClientService] Subscribed to: " + destination);
    }

    /**
     * معالجة الرسالة الواردة من STOMP.
     * تتحقق أن الرسالة للمستخدم الحالي ثم تضيفها كإشعار.
     *
     * @param dto بيانات الرسالة الواردة
     */
    private void handleIncomingMessage(MessageNotificationDTO dto) {
        String currentUser = ApiClient.getUserName();

        if (currentUser == null || !currentUser.equals(dto.recipientUsername)) {
            System.out.println("[MessageClientService] Not for me — ignoring");
            return;
        }

        boolean exists = notifService.getAll().stream()
                .filter(HRNotification::isMessage)
                .anyMatch(n -> {
                    Long id = extractId(n.getActionTarget());
                    return id != null && id.equals(dto.messageId);
                });

        if (exists) {
            System.out.println("[MessageClientService] Message " + dto.messageId + " already exists — ignoring");
            return;
        }

        Platform.runLater(() -> {
            HRNotification.Builder builder = HRNotification.builder()
                    .category(NotificationCategory.MESSAGE)
                    .type(NotificationType.MESSAGE)
                    .priority(Priority.NORMAL)
                    .title(dto.subject != null ? dto.subject : "رسالة جديدة")
                    .message(dto.preview != null ? dto.preview : "")
                    .sender(dto.senderDisplayName != null
                            ? dto.senderDisplayName : dto.senderUsername)
                    .senderUsername(dto.senderUsername)
                    .senderAvatar(buildAvatar(dto.senderDisplayName))
                    .timestamp(dto.createdAt != null ? dto.createdAt : LocalDateTime.now())
                    .action("فتح الرسالة", "messages/" + dto.messageId);

            if (dto.attachmentTokens != null && !dto.attachmentTokens.isEmpty()) {
                for (int i = 0; i < dto.attachmentTokens.size(); i++) {
                    builder.attachment(
                            "مرفق " + (i + 1),
                            "",
                            "application/octet-stream",
                            0,
                            dto.attachmentTokens.get(i)
                    );
                }
            }

            notifService.send(builder.build());
            notifService.updateUnreadCount();
            System.out.println("[MessageClientService] Notification added for message " + dto.messageId);
        });
    }

    /**
     * قطع الاتصال بخادم WebSocket وتحرير الموارد.
     */
    public void disconnect() {
        if (messageSubscription != null) {
            try {
                messageSubscription.unsubscribe();
            } catch (Exception ignored) {
            }
            messageSubscription = null;
        }

        if (stompSession != null && stompSession.isConnected()) {
            try {
                stompSession.disconnect();
            } catch (Exception ignored) {
            }
            stompSession = null;
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
        System.out.println("[MessageClientService] Disconnected");
    }

    /**
     * التحقق من جاهزية الاتصال.
     *
     * @return true إذا كان متصلاً وجاهزاً
     */
    private boolean isReady() {
        return connected.get() && stompSession != null && stompSession.isConnected();
    }

    /**
     * ترجع عنوان WebSocket من الإعدادات.
     *
     * @return عنوان WebSocket كامل
     */
    private String getWebSocketUrl() {
        String baseUrl = ApiClient.BASE_URL2;
        baseUrl = baseUrl.replaceAll("/+$", "");
        return baseUrl;
    }

    /**
     * جدولة إعادة الاتصال بعد 5 ثوانٍ في حالة فشل الاتصال.
     */
    private void scheduleReconnect() {
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                connect();
            } catch (InterruptedException ignored) {
            }
        }, "msg-ws-reconnect").start();
    }

    // =====================================================================
    //  Initial Load
    // =====================================================================

    /**
     * جلب عدد الرسائل غير المقروءة من الخادم وتحديث الشارة.
     */
    public void loadUnreadMessagesAndNotify() {
        getInboxStats().thenAccept(stats -> {
            if (stats != null) {
                Platform.runLater(() -> {
                    int unread = stats.getUnreadCount() > 0 ? (int) stats.getUnreadCount() : 0;
                    notifService.updateUnreadCount(unread);
                    System.out.println("[LOAD] " + unread + " unread messages — badge updated");
                });
            }
        });
    }

    // =====================================================================
    //  Manual Refresh
    // =====================================================================

    /**
     * تحديث قائمة الرسائل من الخادم وإضافة الجديدة للإشعارات.
     */
    public void refreshAllMessages() {
        CompletableFuture.supplyAsync(() -> {
            try {
                var response = ApiClient.get(buildApiUrl("/messages/inbox?page=0&size=100"), MessageSummaryDTO[].class);
                if (response.isSuccess() && response.getData() != null)
                    return Arrays.asList(response.getData());
                return List.<MessageSummaryDTO>of();
            } catch (Exception e) {
                System.err.println("[MessageClientService] فشل التحديث: " + e.getMessage());
                return List.<MessageSummaryDTO>of();
            }
        }).thenAccept(messages -> {
            Platform.runLater(() -> {
                Set<Long> existingIds = notifService.getAll().stream()
                        .filter(HRNotification::isMessage)
                        .map(n -> extractId(n.getActionTarget()))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

                for (MessageSummaryDTO msg : messages) {
                    if (!existingIds.contains(msg.getId())) {
                        notifService.getAll().add(toNotification(msg));
                    }
                }

                notifService.updateUnreadCount();
                System.out.println("[REFRESH] Loaded " + messages.size() + " messages");
            });
        });
    }

    /**
     * استخراج معرف الرسالة من actionTarget.
     *
     * @param actionTarget نص الهدف (مثل messages/123)
     * @return معرف الرسالة أو null
     */
    private Long extractId(String actionTarget) {
        if (actionTarget == null || !actionTarget.startsWith("messages/")) return null;
        try {
            return Long.parseLong(actionTarget.substring(9));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // =====================================================================
    //  toNotification
    // =====================================================================

    /**
     * تحويل MessageSummaryDTO إلى HRNotification.
     *
     * @param msg بيانات الملخص من الخادم
     * @return كائن HRNotification
     */
    private HRNotification toNotification(MessageSummaryDTO msg) {
        HRNotification.Builder builder = HRNotification.builder()
                .category(NotificationCategory.MESSAGE)
                .type(NotificationType.MESSAGE)
                .priority(Priority.NORMAL)
                .title(msg.getSubject() != null ? msg.getSubject() : "رسالة جديدة")
                .message(msg.getPreview() != null ? msg.getPreview() : "")
                .sender(msg.getSenderDisplayName() != null
                        ? msg.getSenderDisplayName() : msg.getSenderUsername())
                .senderUsername(msg.getSenderUsername())
                .senderAvatar(buildAvatar(msg.getSenderDisplayName()))
                .timestamp(msg.getCreatedAt() != null ? msg.getCreatedAt() : LocalDateTime.now())
                .read(msg.isRead())
                .action("فتح الرسالة", "messages/" + msg.getId());

        if (msg.getAttachmentsCount() > 0) {
            for (int i = 0; i < msg.getAttachmentsCount(); i++) {
                builder.attachment(
                        "مرفق " + (i + 1),
                        "",
                        "application/octet-stream",
                        0
                );
            }
        }

        return builder.build();
    }

    // =====================================================================
    //  REST API
    // =====================================================================

    /**
     * جلب إحصائيات صندوق الوارد (عدد غير المقروءة).
     *
     * @return CompletableFuture يحتوي على InboxStatsDTO
     */
    public CompletableFuture<InboxStatsDTO> getInboxStats() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var response = ApiClient.get(buildApiUrl("/messages/stats"), InboxStatsDTO.class);
                if (response.isSuccess() && response.getData() != null)
                    return response.getData();
                return null;
            } catch (Exception e) {
                System.err.println("[MessageClientService] فشل جلب الإحصائيات: " + e.getMessage());
                return null;
            }
        });
    }

    /**
     * جلب الرسائل غير المقروءة من الخادم.
     *
     * @return CompletableFuture يحتوي على قائمة MessageSummaryDTO
     */
    public CompletableFuture<List<MessageSummaryDTO>> getUnreadMessages() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                TypeReference<ApiResponse<List<MessageSummaryDTO>>> typeRef = new TypeReference<>() {
                };
                var response = ApiClient.getWithTypeRef(buildApiUrl("/messages/inbox?page=0&size=100"), typeRef);

                if (response.isSuccess() && response.getData() != null) {
                    List<MessageSummaryDTO> all = response.getData().getData();
                    return all.stream().filter(m -> !m.isRead()).collect(Collectors.toList());
                }
                return List.of();
            } catch (Exception e) {
                System.err.println("[MessageClientService] فشل جلب الرسائل: " + e.getMessage());
                return List.of();
            }
        });
    }

    /**
     * بناء URL كامل بدون شرطات مائلة مكررة.
     *
     * @param path المسار النسبي
     * @return URL كامل
     */
    private String buildFullUrl(String path) {
        String base = ApiClient.BASE_URL;
        if (base == null) base = "";

        base = base.replaceAll("/+$", "");

        if (!path.startsWith("/")) path = "/" + path;
        path = path.replaceAll("//+", "/");

        String full = base + path;

        String result = full;
        result = result.replace("http://", "HTTP");
        result = result.replace("https://", "HTTPS");
        result = result.replaceAll("/{2,}", "/");
        result = result.replace("HTTP", "http://");
        result = result.replace("HTTPS", "https://");

        return result;
    }

    /**
     * جلب تفاصيل رسالة واحدة بالمعرف.
     *
     * @param messageId معرف الرسالة
     * @return CompletableFuture يحتوي على Map بالبيانات
     */
    public CompletableFuture<Map<String, Object>> getMessageDetails(Long messageId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = buildFullUrl("/messages/" + messageId);

                System.out.println("[CLIENT] جاري جلب تفاصيل الرسالة: " + messageId);
                System.out.println("[CLIENT] URL: " + url);

                java.net.URL u = new java.net.URL(url);
                java.net.HttpURLConnection c = (java.net.HttpURLConnection) u.openConnection();
                c.setRequestProperty("Authorization", "Bearer " + ApiClient.getAuthToken());
                c.setRequestMethod("GET");

                java.io.BufferedReader r = new java.io.BufferedReader(
                        new java.io.InputStreamReader(c.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
                r.close();

                String rawJson = sb.toString();
                System.out.println("[CLIENT] Raw: " + rawJson);

                @SuppressWarnings("unchecked")
                Map<String, Object> root = mapper.readValue(rawJson, Map.class);
                Object data = root.get("data");

                if (data instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> dataMap = (Map<String, Object>) data;
                    return dataMap;
                }
                return null;

            } catch (Exception e) {
                System.err.println("[CLIENT] فشل: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        });
    }

    // =====================================================================
    //  Thread (Parent + Replies)
    // =====================================================================

    /**
     * جلب المحادثة كاملة (الرسالة الأساسية + الردود).
     *
     * @param messageId معرف الرسالة الأساسية
     * @return CompletableFuture يحتوي على Map بالمحادثة
     */
    public CompletableFuture<Map<String, Object>> getThread(Long messageId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = buildFullUrl("/messages/" + messageId + "/thread");
                System.out.println("[CLIENT] جاري جلب المحادثة: " + messageId);
                System.out.println("[CLIENT] URL: " + url);

                java.net.URL u = new java.net.URL(url);
                java.net.HttpURLConnection c = (java.net.HttpURLConnection) u.openConnection();
                c.setRequestProperty("Authorization", "Bearer " + ApiClient.getAuthToken());
                c.setRequestMethod("GET");
                c.setConnectTimeout(10000);
                c.setReadTimeout(10000);

                int status = c.getResponseCode();
                System.out.println("[CLIENT] Thread HTTP Status: " + status);

                if (status != 200) {
                    throw new RuntimeException("HTTP " + status);
                }

                java.io.BufferedReader r = new java.io.BufferedReader(
                        new java.io.InputStreamReader(c.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
                r.close();

                String rawJson = sb.toString();
                System.out.println("[CLIENT] Thread Raw: " + rawJson.substring(0, Math.min(200, rawJson.length())) + "...");

                @SuppressWarnings("unchecked")
                Map<String, Object> root = mapper.readValue(rawJson, Map.class);
                Object data = root.get("data");

                if (data instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> dataMap = (Map<String, Object>) data;
                    return dataMap;
                }
                return null;

            } catch (Exception e) {
                System.err.println("[CLIENT] فشل جلب المحادثة: " + e.getMessage());
                return null;
            }
        });
    }

    // =====================================================================
    //  Send / Reply
    // =====================================================================

    /**
     * إرسال رسالة جديدة لعدة مستلمين.
     *
     * @param recipientUsernames أسماء المستخدمين المستلمين
     * @param subject            موضوع الرسالة
     * @param body               محتوى الرسالة
     * @param attachments        قائمة ملفات مرفقة (يمكن أن تكون null)
     * @param onSuccess          callback عند النجاح
     * @param onError            callback عند الفشل مع نص الخطأ
     */
    public void sendMessageToMultiple(List<String> recipientUsernames,
                                      String subject,
                                      String body,
                                      List<Path> attachments,
                                      Runnable onSuccess,
                                      Consumer<String> onError) {

        new Thread(() -> {
            try {
                Map<String, Object> formData = new java.util.HashMap<>();
                Map<String, Object> data = new java.util.HashMap<>();
                data.put("recipientUsernames", recipientUsernames);
                data.put("subject", subject != null ? subject : "");
                data.put("body", body != null ? body : "");
                formData.put("data", data);

                if (attachments != null && !attachments.isEmpty()) {
                    for (Path file : attachments)
                        formData.put("files", file);
                }

                var response = ApiClient.uploadFile(buildApiUrl("/messages"), formData, Object.class);
                handleResponse(response.isSuccess(), response.getMessage(), onSuccess, onError);

            } catch (Exception e) {
                Platform.runLater(() -> onError.accept(e.getMessage()));
            }
        }, "send-multi-message").start();
    }

    /**
     * الرد على رسالة موجودة.
     *
     * @param parentId    معرف الرسالة الأصلية
     * @param subject     موضوع الرد
     * @param body        محتوى الرد
     * @param attachments قائمة ملفات مرفقة (يمكن أن تكون null)
     * @param onSuccess   callback عند النجاح
     * @param onError     callback عند الفشل
     */
    public void replyToMessage(Long parentId,
                               String subject,
                               String body,
                               List<Path> attachments,
                               Runnable onSuccess,
                               Consumer<String> onError) {

        new Thread(() -> {
            try {
                Map<String, Object> formData = new java.util.HashMap<>();
                Map<String, Object> data = new java.util.HashMap<>();
                data.put("parentId", parentId);
                data.put("subject", subject != null ? subject : "");
                data.put("body", body != null ? body : "");
                formData.put("data", data);

                if (attachments != null && !attachments.isEmpty()) {
                    for (Path file : attachments)
                        formData.put("files", file);
                }

                var response = ApiClient.uploadFile(buildApiUrl("/messages/reply"), formData, Object.class);
                handleResponse(response.isSuccess(), response.getMessage(), onSuccess, onError);

            } catch (Exception e) {
                Platform.runLater(() -> onError.accept(e.getMessage()));
            }
        }, "reply-message").start();
    }

    // =====================================================================
    //  Mark Read
    // =====================================================================

    /**
     * تعليم رسالة كمقروءة في الخادم.
     *
     * @param messageId معرف الرسالة
     * @return CompletableFuture فارغ
     */
    public CompletableFuture<Void> markMessageAsRead(Long messageId) {
        return CompletableFuture.runAsync(() -> {
            try {
                ApiClient.put(buildApiUrl("/messages/" + messageId + "/read"), null, Void.class);
            } catch (Exception e) {
                System.err.println("[MessageClientService] فشل تعليم مقروء: " + e.getMessage());
            }
        });
    }

    // =====================================================================
    //  Download Attachment
    // =====================================================================

    /**
     * تحميل مرفق من الخادم وحفظه في مسار محدد.
     *
     * @param token      رمز الملف في الخادم
     * @param targetPath المسار المحلي للحفظ
     * @param onSuccess  callback عند اكتمال التحميل
     * @param onError    callback عند الفشل
     */
    public void downloadAttachment(String token,
                                   Path targetPath,
                                   Runnable onSuccess,
                                   Consumer<String> onError) {

        new Thread(() -> {
            try {
                String url = buildFullUrl("/messages/attachments/" + token);
                System.out.println("[DOWNLOAD] Token: " + token);
                System.out.println("[DOWNLOAD] URL: " + url);
                System.out.println("[DOWNLOAD] Target: " + targetPath);

                java.net.URL u = new java.net.URL(url);
                java.net.HttpURLConnection c = (java.net.HttpURLConnection) u.openConnection();
                c.setRequestProperty("Authorization", "Bearer " + ApiClient.getAuthToken());
                c.setRequestMethod("GET");
                c.setConnectTimeout(30000);
                c.setReadTimeout(30000);

                int status = c.getResponseCode();
                System.out.println("[DOWNLOAD] HTTP Status: " + status);

                if (status != 200) {
                    String errMsg = "HTTP " + status;
                    try (java.io.BufferedReader r = new java.io.BufferedReader(
                            new java.io.InputStreamReader(c.getErrorStream()))) {
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = r.readLine()) != null) sb.append(line);
                        errMsg += ": " + sb;
                    } catch (Exception ignored) {
                    }

                    String finalErrMsg = errMsg;
                    Platform.runLater(() -> onError.accept(finalErrMsg));
                    return;
                }

                Files.createDirectories(targetPath.getParent());
                try (java.io.InputStream in = c.getInputStream()) {
                    Files.copy(in, targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }

                System.out.println("[DOWNLOAD] Saved to: " + targetPath);
                Platform.runLater(onSuccess);

            } catch (Exception e) {
                System.err.println("[DOWNLOAD] Error: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> onError.accept(e.getMessage()));
            }
        }, "download-attachment").start();
    }

    // =====================================================================
    //  Check Attachment Exists
    // =====================================================================

    /**
     * التحقق من وجود ملف مرفق على الخادم باستخدام طلب HEAD.
     *
     * @param token رمز الملف
     * @return CompletableFuture يحتوي على true إذا كان الملف موجوداً
     */
    public CompletableFuture<Boolean> checkAttachmentExists(String token) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = buildFullUrl("/messages/attachments/" + token);
                java.net.URL u = new java.net.URL(url);
                java.net.HttpURLConnection c = (java.net.HttpURLConnection) u.openConnection();
                c.setRequestProperty("Authorization", "Bearer " + ApiClient.getAuthToken());
                c.setRequestMethod("HEAD");
                c.setConnectTimeout(5000);
                c.setReadTimeout(5000);

                int status = c.getResponseCode();
                System.out.println("[CHECK] Attachment " + token + " exists: " + (status == 200));
                return status == 200;

            } catch (Exception e) {
                System.err.println("[CHECK] Failed to check attachment: " + e.getMessage());
                return false;
            }
        });
    }

    // =====================================================================
    //  Get All Users
    // =====================================================================

    /**
     * جلب كل المستخدمين من الخادم مع أسمائهم المعروضة.
     *
     * @return CompletableFuture يحتوي على قائمة UserInfo
     */
    public CompletableFuture<List<UserInfo>> getAllUsers() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = buildFullUrl("/users");
                System.out.println("[USERS] Fetching from: " + url);

                java.net.URL u = new java.net.URL(url);
                java.net.HttpURLConnection c = (java.net.HttpURLConnection) u.openConnection();
                c.setRequestProperty("Authorization", "Bearer " + ApiClient.getAuthToken());
                c.setRequestMethod("GET");
                c.setConnectTimeout(10000);
                c.setReadTimeout(10000);

                int status = c.getResponseCode();
                if (status != 200) {
                    System.err.println("[USERS] HTTP " + status);
                    return List.of();
                }

                java.io.BufferedReader r = new java.io.BufferedReader(
                        new java.io.InputStreamReader(c.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
                r.close();

                String rawJson = sb.toString();
                System.out.println("[USERS] Raw: " + rawJson.substring(0, Math.min(200, rawJson.length())) + "...");

                @SuppressWarnings("unchecked")
                Map<String, Object> root = mapper.readValue(rawJson, Map.class);
                Object data = root.get("data");

                if (data instanceof List) {
                    List<UserInfo> users = new ArrayList<>();
                    for (Object o : (List<?>) data) {
                        if (o instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> userMap = (Map<String, Object>) o;
                            String username = userMap.get("username") != null ? userMap.get("username").toString() : "";
                            String displayName = userMap.get("displayName") != null ? userMap.get("displayName").toString() : "";
                            users.add(new UserInfo(username, displayName));
                        }
                    }
                    return users;
                }
                return List.of();

            } catch (Exception e) {
                System.err.println("[USERS] Failed: " + e.getMessage());
                return List.of();
            }
        });
    }

    // =====================================================================
    //  Helpers
    // =====================================================================

    private void handleResponse(boolean success, String errorMsg,
                                Runnable onSuccess, Consumer<String> onError) {
        if (success) Platform.runLater(onSuccess);
        else Platform.runLater(() -> onError.accept(errorMsg));
    }

    /**
     * بناء نص الصورة الرمزية من اسم العرض.
     *
     * @param displayName اسم العرض
     * @return الحروف الأولى
     */
    private String buildAvatar(String displayName) {
        if (displayName == null || displayName.isBlank()) return "؟";
        String[] parts = displayName.trim().split("\s+");
        if (parts.length == 1) return String.valueOf(parts[0].charAt(0));
        return "" + parts[0].charAt(0) + parts[1].charAt(0);
    }

    /**
     * طباعة حالة الاتصال الحالية.
     */
    public void printStatus() {
        System.out.println("=== Message WebSocket Status ===");
        System.out.println("Connected: " + connected.get());
        System.out.println("Token: " + (ApiClient.getAuthToken() != null ? "YES" : "NO"));
        System.out.println("========================");
    }

    // =====================================================================
    //  DTOs
    // =====================================================================

    /**
     * كائن نقل بيانات للرسائل الواردة عبر STOMP.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class MessageNotificationDTO {
        public Long messageId;
        public String senderUsername;
        public String senderDisplayName;
        public String recipientUsername;
        public String subject;
        public String preview;
        public int attachmentsCount;
        public List<String> attachmentTokens;
        public LocalDateTime createdAt;
    }
}