package com.safwat.hr.chat;

import com.fasterxml.jackson.core.type.TypeReference;

import com.safwat.hr.utils.ApiClient;
import com.safwat.hr.utils.ApiResponse;


import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * كل REST calls الخاصة بنظام الشات.
 * كل الـ methods async — بترجع CompletableFuture.
 * <p>
 * الاستخدام:
 * <pre>
 *   ChatApiService.getConversations()
 *       .thenAccept(res -> Platform.runLater(() -> {
 *           if (res.isSuccess()) list.setAll(res.getData());
 *       }));
 * </pre>
 */
public class ChatApiService {

    private static final String BASE = "/chat";

    // ═════════════════════════════════════════════════════════════════
    //  Users
    // ═════════════════════════════════════════════════════════════════

    /**
     * بحث عن مستخدمين — يحتاج حرفين على الأقل
     */
    public static CompletableFuture<ApiResponse<List<ChatDTOs.UserSearchDTO>>> searchUsers(String query) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return ApiClient.getWithTypeRef(
                        BASE + "/users/search?q=" + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8),
                        new TypeReference<List<ChatDTOs.UserSearchDTO>>() {
                        }
                );
            } catch (Exception e) {
                return errorResponse(e);
            }
        });
    }

    // ═════════════════════════════════════════════════════════════════
    //  Conversations
    // ═════════════════════════════════════════════════════════════════

    /**
     * قائمة محادثات المستخدم الحالي
     */
    public static CompletableFuture<ApiResponse<List<ChatDTOs.ConversationSummaryDTO>>> getConversations() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return ApiClient.getWithTypeRef(
                        BASE + "/conversations",
                        new TypeReference<List<ChatDTOs.ConversationSummaryDTO>>() {
                        }
                );
            } catch (Exception e) {
                return errorResponse(e);
            }
        });
    }

    /**
     * تفاصيل محادثة واحدة مع المشاركين
     */
    public static CompletableFuture<ApiResponse<ChatDTOs.ConversationDetailDTO>> getConversationDetail(long id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return ApiClient.get(
                        BASE + "/conversations/" + id,
                        ChatDTOs.ConversationDetailDTO.class
                );
            } catch (Exception e) {
                return errorResponse(e);
            }
        });
    }

    /**
     * إنشاء محادثة خاصة مع مستخدم آخر
     */
    public static CompletableFuture<ApiResponse<ChatDTOs.ConversationDetailDTO>> createPrivateConversation(long otherUserId) {
        ChatDTOs.CreateConversationRequest req = new ChatDTOs.CreateConversationRequest(
                "PRIVATE", List.of(otherUserId)
        );
        return CompletableFuture.supplyAsync(() -> {
            try {
                return ApiClient.post(
                        BASE + "/conversations",
                        req,
                        ChatDTOs.ConversationDetailDTO.class
                );
            } catch (Exception e) {
                return errorResponse(e);
            }
        });
    }

    /**
     * إنشاء محادثة جماعية (يحتاج صلاحية MANAGER أو ADMIN)
     */
    public static CompletableFuture<ApiResponse<ChatDTOs.ConversationDetailDTO>> createGroupConversation(
            String name, List<Long> participantIds) {

        ChatDTOs.CreateConversationRequest req = new ChatDTOs.CreateConversationRequest();
        req.setType("GROUP");
        req.setName(name);
        req.setParticipantIds(participantIds);

        return CompletableFuture.supplyAsync(() -> {
            try {
                return ApiClient.post(
                        BASE + "/conversations",
                        req,
                        ChatDTOs.ConversationDetailDTO.class
                );
            } catch (Exception e) {
                return errorResponse(e);
            }
        });
    }

    // ═════════════════════════════════════════════════════════════════
    //  Messages
    // ═════════════════════════════════════════════════════════════════

    /**
     * رسائل محادثة مع pagination — page=0 أول صفحة (100 رسالة)
     */
    public static CompletableFuture<ApiResponse<List<ChatDTOs.ChatMessageDTO>>> getMessages(
            long conversationId, int page) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                return ApiClient.getWithTypeRef(
                        BASE + "/conversations/" + conversationId + "/messages?page=" + page + "&size=100",
                        new TypeReference<List<ChatDTOs.ChatMessageDTO>>() {
                        }
                );
            } catch (Exception e) {
                return errorResponse(e);
            }
        });
    }

    /**
     * إرسال رسالة نصية بدون مرفقات
     */
    public static CompletableFuture<ApiResponse<ChatDTOs.ChatMessageDTO>> sendTextMessage(
            long conversationId, String content) {

        ChatDTOs.SendMessageRequest req = new ChatDTOs.SendMessageRequest(content);

        // multipart مع JSON part فقط (بدون files)
        Map<String, Object> formData = new HashMap<>();
        formData.put("data", req);  // ApiClient.uploadFile بيـ serialize الـ objects كـ JSON

        return CompletableFuture.supplyAsync(() -> {
            try {
                return ApiClient.uploadFile(
                        BASE + "/conversations/" + conversationId + "/messages",
                        formData,
                        ChatDTOs.ChatMessageDTO.class
                );
            } catch (Exception e) {
                return errorResponse(e);
            }
        });
    }

    /**
     * إرسال رسالة مع مرفقات
     */
    public static CompletableFuture<ApiResponse<ChatDTOs.ChatMessageDTO>> sendMessageWithFiles(
            long conversationId, String content, List<Path> files) {

        ChatDTOs.SendMessageRequest req = new ChatDTOs.SendMessageRequest(
                content.isBlank() ? "📎 ملف" : content
        );

        Map<String, Object> formData = new HashMap<>();
        formData.put("data", req);
        for (int i = 0; i < files.size(); i++) {
            formData.put("files", files.get(i)); // Spring بيستقبل "files" as List
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                return ApiClient.uploadFile(
                        BASE + "/conversations/" + conversationId + "/messages",
                        formData,
                        ChatDTOs.ChatMessageDTO.class
                );
            } catch (Exception e) {
                return errorResponse(e);
            }
        });
    }

    /**
     * حذف رسالة (المرسل فقط)
     */
    public static CompletableFuture<ApiResponse<Void>> deleteMessage(long messageId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return ApiClient.delete(
                        BASE + "/messages/" + messageId,
                        Void.class
                );
            } catch (Exception e) {
                return errorResponse(e);
            }
        });
    }

    /**
     * تعليم رسائل المحادثة كمقروءة — يُستدعى لما المستخدم يفتح المحادثة
     */
    public static CompletableFuture<ApiResponse<Void>> markAsRead(long conversationId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return ApiClient.put(
                        BASE + "/conversations/" + conversationId + "/read",
                        null,
                        Void.class
                );
            } catch (Exception e) {
                return errorResponse(e);
            }
        });
    }

    // ═════════════════════════════════════════════════════════════════
    //  Attachments
    // ═════════════════════════════════════════════════════════════════

    /**
     * تحميل مرفق إلى مسار معين
     */
    public static CompletableFuture<Boolean> downloadAttachment(String downloadToken, Path targetPath) {
        return ApiClient.downloadFileAsync(
                BASE + "/attachments/" + downloadToken,
                null,
                targetPath
        );
    }

    // ═════════════════════════════════════════════════════════════════
    //  Helpers — ApiClient methods لـ TypeReference
    // ═════════════════════════════════════════════════════════════════

    private static <T> ApiResponse<T> errorResponse(Exception e) {
        ApiResponse<T> err = new ApiResponse<>();
        err.setSuccess(false);
        err.setMessage(e.getMessage());
        return err;
    }
}
