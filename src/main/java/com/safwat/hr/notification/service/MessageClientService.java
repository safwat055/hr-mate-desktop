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
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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

    private String buildApiUrl(String path) {
        // ✅ بس نضف الـ path — ApiClient هيتعامل مع BASE_URL
        path = path.trim();
        if (!path.startsWith("/")) path = "/" + path;
        path = path.replaceAll("//+", "/");
        return path;
    }

    // =====================================================================
    //  WebSocket
    // =====================================================================

    public void connect() {
        if (connected) return;

        String token = ApiClient.getAuthToken();
        String username = ApiClient.getUserName();

        if (token == null || token.isEmpty()) {
            System.err.println("[MessageClientService] لا يوجد token");
            scheduleReconnect();
            return;
        }

        String wsUrl = getWebSocketUrl();
        System.out.println("[MessageClientService] WebSocket URL: " + wsUrl);

        wsClient = new ApiClient.WebSocketClient(
                wsUrl,
                this::onMessageReceived,
                this::onError,
                this::onClose,
                token
        );

        wsClient.connect()
                .thenRun(() -> {
                    connected = true;
                    System.out.println("[MessageClientService] متصل بنجاح");
                    loadUnreadMessagesAndNotify();
                })
                .exceptionally(e -> {
                    System.err.println("[MessageClientService] فشل الاتصال: " + e.getMessage());
                    scheduleReconnect();
                    return null;
                });
    }

    private String getWebSocketUrl() {
        String baseUrl = ApiClient.BASE_URL2;  // "ws://localhost:8080/ws"

        // ✅ تأكد إن مفيش / trailing
        baseUrl = baseUrl.replaceAll("/+$", "");

        return baseUrl;
    }

    public void disconnect() {
        if (wsClient != null) wsClient.close();
        connected = false;
    }

    // =====================================================================
    //  WebSocket Receiver
    // =====================================================================

    private void onMessageReceived(String json) {
        try {
            MessageNotificationDTO dto = mapper.readValue(json, MessageNotificationDTO.class);

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

    // =====================================================================
    //  Load Messages
    // =====================================================================

    public void loadUnreadMessagesAndNotify() {
        getInboxStats().thenAccept(stats -> {
            if (stats != null && stats.getUnreadCount() > 0) {
                getUnreadMessages().thenAccept(messages -> {
                    Platform.runLater(() -> {
                        for (MessageSummaryDTO msg : messages) {
                            notifService.send(toNotification(msg));
                        }
                        notifService.updateUnreadCount((int) stats.getUnreadCount());
                    });
                });
            }
        });
    }

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

                messages.stream()
                        .filter(msg -> !existingIds.contains(msg.getId()))
                        .forEach(msg -> notifService.send(toNotification(msg)));

                notifService.updateUnreadCount();
            });
        });
    }

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

    // ✅ معدّل — يجيب تفاصيل الرسالة
    public CompletableFuture<Map<String, Object>> getMessageDetails(Long messageId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = buildApiUrl("/messages/" + messageId);
                System.out.println("[CLIENT] جاري جلب تفاصيل الرسالة: " + messageId);

                // جيب JSON خام
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

                // حوّل لـ Map
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
    //  Send / Reply
    // =====================================================================

    public void sendMessage(String recipientUsername,
                            String subject,
                            String body,
                            List<Path> attachments,
                            Runnable onSuccess,
                            Consumer<String> onError) {

        new Thread(() -> {
            try {
                Map<String, Object> formData = new java.util.HashMap<>();
                Map<String, String> data = new java.util.HashMap<>();
                data.put("recipientUsername", recipientUsername);
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
        }, "send-message").start();
    }

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
    //  Download Attachment — معدّل
    // =====================================================================

    public void downloadAttachment(String token,
                                   Path targetPath,
                                   Runnable onSuccess,
                                   Consumer<String> onError) {

        String url = buildApiUrl("/messages/attachments/" + token);
        System.out.println("[DOWNLOAD] Token: " + token);
        System.out.println("[DOWNLOAD] Target: " + targetPath);

        ApiClient.downloadFileAsync(url, null, targetPath)
                .thenAccept(ok -> {
                    if (ok) Platform.runLater(onSuccess);
                    else Platform.runLater(() -> onError.accept("فشل التحميل"));
                })
                .exceptionally(e -> {
                    Platform.runLater(() -> onError.accept(e.getMessage()));
                    return null;
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

    private String buildAvatar(String displayName) {
        if (displayName == null || displayName.isBlank()) return "؟";
        String[] parts = displayName.trim().split("\\s+");
        if (parts.length == 1) return String.valueOf(parts[0].charAt(0));
        return "" + parts[0].charAt(0) + parts[1].charAt(0);
    }

    public void printStatus() {
        System.out.println("=== WebSocket Status ===");
        System.out.println("Connected: " + connected);
        System.out.println("Token: " + (ApiClient.getAuthToken() != null ? "✅" : "❌"));
        System.out.println("========================");
    }

    // =====================================================================
    //  DTOs
    // =====================================================================

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class MessageNotificationDTO {
        public Long messageId;
        public String senderUsername;
        public String senderDisplayName;
        public String subject;
        public String preview;
        public int attachmentsCount;
        public List<String> attachmentTokens;
        public LocalDateTime createdAt;
    }
}
