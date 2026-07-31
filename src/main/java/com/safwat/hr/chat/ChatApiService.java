package com.safwat.hr.chat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.safwat.hr.utils.ApiClient;
import com.safwat.hr.utils.ApiResponse;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * كل REST calls الخاصة بنظام الشات.
 * كل الـ methods async — بترجع CompletableFuture.
 * <p>
 * ملاحظة: ApiClient.BASE_URL = "http://host:port/api"
 * فـ BASE = "/chat" → الـ URL النهائي = "/api/chat/..."
 */
public class ChatApiService {

    // ✅ تم الإصلاح: BASE = "/chat" (ApiClient بيضيف "/api" أصلاً)
    private static final String BASE = "/chat";

    private static final ExecutorService IO_EXECUTOR = Executors.newCachedThreadPool(
            r -> {
                Thread t = new Thread(r, "chat-io-" + System.currentTimeMillis());
                t.setDaemon(true);
                return t;
            }
    );

    // ═════════════════════════════════════════════════════════════════
    //  Users
    // ═════════════════════════════════════════════════════════════════

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
        }, IO_EXECUTOR);
    }

    // ═════════════════════════════════════════════════════════════════
    //  Conversations
    // ═════════════════════════════════════════════════════════════════

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
        }, IO_EXECUTOR);
    }

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
        }, IO_EXECUTOR);
    }

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
        }, IO_EXECUTOR);
    }

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
        }, IO_EXECUTOR);
    }

    // ✅ جديد: addMembers — كانت ناقصة خالص
    public static CompletableFuture<ApiResponse<Void>> addMembers(
            long conversationId, List<Long> userIds) {

        ChatDTOs.AddMembersRequest req = new ChatDTOs.AddMembersRequest();
        req.setUserIds(userIds);

        return CompletableFuture.supplyAsync(() -> {
            try {
                return ApiClient.put(
                        BASE + "/conversations/" + conversationId + "/members",
                        req,
                        Void.class
                );
            } catch (Exception e) {
                return errorResponse(e);
            }
        }, IO_EXECUTOR);
    }

    // ═════════════════════════════════════════════════════════════════
    //  Messages
    // ═════════════════════════════════════════════════════════════════

    /**
     * ✅ تم الإصلاح: إضافة size parameter
     */
    public static CompletableFuture<ApiResponse<List<ChatDTOs.ChatMessageDTO>>> getMessages(
            long conversationId, int page, int size) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                return ApiClient.getWithTypeRef(
                        BASE + "/conversations/" + conversationId + "/messages?page=" + page + "&size=" + size,
                        new TypeReference<List<ChatDTOs.ChatMessageDTO>>() {
                        }
                );
            } catch (Exception e) {
                return errorResponse(e);
            }
        }, IO_EXECUTOR);
    }

    public static CompletableFuture<ApiResponse<ChatDTOs.ChatMessageDTO>> sendTextMessage(
            long conversationId, String content, Long replyToId) {

        ChatDTOs.SendMessageRequest req = new ChatDTOs.SendMessageRequest(content, replyToId);

        Map<String, Object> formData = new HashMap<>();
        formData.put("data", req);

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
        }, IO_EXECUTOR);
    }

    /**
     * ✅ تم الإصلاح: multiple files بيتبعتوا صح
     */
    public static CompletableFuture<ApiResponse<ChatDTOs.ChatMessageDTO>> sendMessageWithFiles(
            long conversationId, String content, List<Path> files, Long replyToId) {

        String defaultCaption = files.size() > 1 ? "📎 " + files.size() + " مرفقات" : "📎 ملف";
        ChatDTOs.SendMessageRequest req = new ChatDTOs.SendMessageRequest(
                content == null || content.isBlank() ? defaultCaption : content,
                replyToId
        );

        Map<String, Object> formData = new HashMap<>();
        formData.put("data", req);

        // ملاحظة: كل ملف بيتبعت بمفتاح مختلف (files_0, files_1...) عشان
        // Map مش بيقبل نفس المفتاح مرتين من نفس الـ Map. الباك إند
        // (ChatController) بيقرأ كل أجزاء الملفات دي بشكل يدوي بغض النظر
        // عن اسم كل جزء، فمفيش داعي إن الأسماء دي تبقى "files" بالظبط.
        for (int i = 0; i < files.size(); i++) {
            formData.put("files_" + i, files.get(i));
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
        }, IO_EXECUTOR);
    }

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
        }, IO_EXECUTOR);
    }

    public static CompletableFuture<ApiResponse<ChatDTOs.ChatMessageDTO>> editMessage(
            long messageId, String newContent) {

        ChatDTOs.EditMessageRequest req = new ChatDTOs.EditMessageRequest();
        req.setContent(newContent);

        return CompletableFuture.supplyAsync(() -> {
            try {
                return ApiClient.put(
                        BASE + "/messages/" + messageId,
                        req,
                        ChatDTOs.ChatMessageDTO.class
                );
            } catch (Exception e) {
                return errorResponse(e);
            }
        }, IO_EXECUTOR);
    }

    public static CompletableFuture<ApiResponse<Void>> deleteConversation(
            long conversationId, boolean forEveryone) {

        String url = BASE + "/conversations/" + conversationId + "/delete?forEveryone=" + forEveryone;

        return CompletableFuture.supplyAsync(() -> {
            try {
                return ApiClient.delete(url, Void.class);
            } catch (Exception e) {
                return errorResponse(e);
            }
        }, IO_EXECUTOR);
    }

    public static CompletableFuture<ApiResponse<Void>> markAllAsRead() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return ApiClient.put(
                        BASE + "/conversations/read-all",
                        null,
                        Void.class
                );
            } catch (Exception e) {
                return errorResponse(e);
            }
        }, IO_EXECUTOR);
    }

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
        }, IO_EXECUTOR);
    }

    /**
     * ✅ تم الإصلاح: شلنا conversationId من الـ body (موجود في الـ URL)
     */
    public static CompletableFuture<ApiResponse<Void>> sendTypingIndicator(long conversationId, boolean typing) {
        ChatDTOs.TypingRequest req = new ChatDTOs.TypingRequest();
        req.setTyping(typing);

        return CompletableFuture.supplyAsync(() -> {
            try {
                return ApiClient.post(
                        BASE + "/conversations/" + conversationId + "/typing",
                        req,
                        Void.class
                );
            } catch (Exception e) {
                return errorResponse(e);
            }
        }, IO_EXECUTOR);
    }

    // ═════════════════════════════════════════════════════════════════
    //  Attachments
    // ═════════════════════════════════════════════════════════════════

    public static CompletableFuture<Boolean> downloadAttachment(String downloadToken, Path targetPath) {
        return ApiClient.downloadFileAsync(
                BASE + "/attachments/" + downloadToken,
                null,
                targetPath
        );
    }

    // ═════════════════════════════════════════════════════════════════
    //  Broadcast & Departments
    // ═════════════════════════════════════════════════════════════════

    public static CompletableFuture<ApiResponse<ChatDTOs.ConversationDetailDTO>> createBroadcastConversation(
            String name, Long targetDepartmentId) {

        ChatDTOs.CreateConversationRequest req = new ChatDTOs.CreateConversationRequest();
        req.setType("BROADCAST");
        req.setName(name);
        req.setTargetDepartmentId(targetDepartmentId);

        return CompletableFuture.supplyAsync(() -> {
            try {
                return ApiClient.post(BASE + "/conversations", req,
                        ChatDTOs.ConversationDetailDTO.class);
            } catch (Exception e) {
                return errorResponse(e);
            }
        }, IO_EXECUTOR);
    }

    /**
     * ✅ تم الإصلاح: "/departments" بدون BASE
     * لأن DepartmentController عنده @RequestMapping("/api/departments")
     * فالـ URL = BASE_URL + "/departments" = ".../api/departments"
     */
    public static CompletableFuture<ApiResponse<List<ChatDTOs.DepartmentDTO>>> getDepartments() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return ApiClient.getWithTypeRef(
                        "/departments",
                        new TypeReference<List<ChatDTOs.DepartmentDTO>>() {
                        }
                );
            } catch (Exception e) {
                return errorResponse(e);
            }
        }, IO_EXECUTOR);
    }

    private static <T> ApiResponse<T> errorResponse(Exception e) {
        ApiResponse<T> err = new ApiResponse<>();
        err.setSuccess(false);
        err.setMessage(e.getMessage());
        return err;
    }
}