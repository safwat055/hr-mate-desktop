package com.safwat.hr.chat;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * نسخة JavaFX من ChatDTOs الخاصة بالباك إند.
 * يجب أن تطابق الحقول تماماً لأن Jackson بيعمل deserialization تلقائي.
 * <p>
 * ملاحظة: استخدمنا @Data + @NoArgsConstructor بدل @Builder
 * لأن Jackson محتاج default constructor.
 */
public class ChatDTOs {

    // ─────────────────────────────────────────────────────────────────
    //  Response DTOs
    // ─────────────────────────────────────────────────────────────────

    /**
     * ملخص المحادثة في القائمة الجانبية
     */
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ConversationSummaryDTO {
        private Long id;
        private String name;
        private String type;            // PRIVATE / GROUP / BROADCAST
        private String avatarInitials;
        private String avatarColor;
        private String lastMessage;
        private long unreadCount;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime lastMessageAt;

        private String timeAgo;
        private boolean muted;
    }

    /**
     * تفاصيل المحادثة مع المشاركين
     */
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ConversationDetailDTO {
        private Long id;
        private String name;
        private String type;
        private List<ParticipantDTO> participants;
        private String createdBy;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;
    }

    /**
     * مشارك في المحادثة
     */
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ParticipantDTO {
        private Long userId;
        private String username;
        private String displayName;
        private String jobTitle;
        private String departmentName;
        private String avatarInitials;
        private String avatarColor;
        private String role;           // MEMBER / ADMIN
        private boolean online;
    }

    /**
     * رسالة شات واحدة
     */
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChatMessageDTO {
        private Long id;
        private Long conversationId;
        private Long senderId;
        private String senderUsername;
        private String senderDisplayName;
        private String senderAvatarInitials;
        private String senderAvatarColor;
        private String content;
        private String messageType;    // TEXT / FILE / SYSTEM
        private boolean deleted;
        private List<ChatAttachmentDTO> attachments;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;

        private String timeAgo;
        private boolean mine;           // هل الرسالة من المستخدم الحالي؟
    }

    /**
     * مرفق
     */
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChatAttachmentDTO {
        private Long id;
        private String fileName;
        private String mimeType;
        private Long fileSize;
        private String formattedSize;
        private String downloadToken;
        private String downloadUrl;
    }

    /**
     * نتيجة بحث مستخدم
     */
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserSearchDTO {
        private Long id;
        private String username;
        private String displayName;
        private String jobTitle;
        private String departmentName;
        private String avatarInitials;
        private String avatarColor;
    }

    // ─────────────────────────────────────────────────────────────────
    //  Request DTOs
    // ─────────────────────────────────────────────────────────────────

    /**
     * إنشاء محادثة جديدة
     */
    @Data
    @NoArgsConstructor
    public static class CreateConversationRequest {
        private String type;
        private String name;
        private List<Long> participantIds;
        private Long targetDepartmentId;

        public CreateConversationRequest(String type, List<Long> participantIds) {
            this.type = type;
            this.participantIds = participantIds;
        }
    }

    /**
     * إرسال رسالة — يُرسل كـ JSON part داخل multipart
     */
    @Data
    @NoArgsConstructor
    public static class SendMessageRequest {
        private String content;
        private String messageType;  // TEXT / FILE

        public SendMessageRequest(String content) {
            this.content = content;
            this.messageType = "TEXT";
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  WebSocket DTOs
    // ─────────────────────────────────────────────────────────────────

    /**
     * رسالة WebSocket واردة من /topic/conversation/{id}
     */
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WsMessageDTO {
        private String type;            // "NEW_MESSAGE" / "USER_JOINED" / "USER_LEFT"
        private Long conversationId;
        private ChatMessageDTO message;
    }

    /**
     * إشعار WebSocket وارد من /user/{username}/queue/chat
     */
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WsNotificationDTO {
        private Long conversationId;
        private String conversationName;
        private String senderDisplayName;
        private String preview;
        private long unreadCount;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;
    }
}
