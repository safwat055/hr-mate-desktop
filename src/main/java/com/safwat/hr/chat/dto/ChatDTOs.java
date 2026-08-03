package com.safwat.hr.chat.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * نسخة JavaFX من ChatDTOs الخاصة بالباك إند.
 * يجب أن تطابق الحقول تماماً لأن Jackson بيعمل deserialization تلقائي.
 */
public class ChatDTOs {

    public enum MessageStatus {
        SENDING, SENT, DELIVERED, READ
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ConversationSummaryDTO {
        private Long id;
        private String name;
        private String type;
        private String avatarInitials;
        private String avatarColor;
        private String lastMessage;
        private long unreadCount;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime lastMessageAt;

        private String timeAgo;
        private boolean muted;

        // ✅ جديد: حالة الاتصال — للمحادثات الخاصة فقط
        private Long otherUserId;
        private boolean online;
        private String lastSeenText;
    }

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
        private String role;
        private boolean online;
    }

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
        private String messageType;
        private boolean deleted;
        private List<ChatAttachmentDTO> attachments;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;

        private String timeAgo;
        private boolean mine;
        private MessageStatus status = MessageStatus.SENT;
        private Set<Long> readBy;
        private boolean edited;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime editedAt;

        // ✅ جديد: الرد على رسالة (Reply/Quote)
        private Long replyToId;
        private String replyToSenderName;
        private String replyToPreview;
        private boolean replyToDeleted;
    }

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
        private String thumbnailUrl;
    }

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

    @Data
    @NoArgsConstructor
    public static class SendMessageRequest {
        private String content;
        private String messageType;
        private Long replyToId;

        public SendMessageRequest(String content) {
            this.content = content;
            this.messageType = "TEXT";
        }

        public SendMessageRequest(String content, Long replyToId) {
            this.content = content;
            this.messageType = "TEXT";
            this.replyToId = replyToId;
        }
    }

    @Data
    @NoArgsConstructor
    public static class EditMessageRequest {
        private String content;
    }

    @Data
    @NoArgsConstructor
    public static class DeleteConversationRequest {
        private Long conversationId;
        private boolean forEveryone;
    }

    /**
     * ✅ تم الإصلاح: شُل conversationId (موجود في الـ URL)
     */
    @Data
    @NoArgsConstructor
    public static class TypingRequest {
        private String username;
        private boolean typing;
    }

    @Data
    @NoArgsConstructor
    public static class AddMembersRequest {
        private List<Long> userIds;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WsMessageDTO {
        private String type;
        private Long conversationId;
        private ChatMessageDTO message;

        private String username;
        private boolean typing;

        private Long messageId;
        private MessageStatus newStatus;
        private Set<Long> readBy;

        // ✅ جديد: MESSAGES_READ — دفعة تحديثات قراءة لعدة رسائل
        private List<MessageReadUpdateDTO> readUpdates;

        private String newContent;

        private LocalDateTime editedAt;

        private boolean forEveryone;
        private String deletedBy;
    }

    /**
     * ✅ جديد: تحديث حالة قراءة رسالة واحدة
     */
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MessageReadUpdateDTO {
        private Long messageId;
        private MessageStatus status;
        private Set<Long> readBy;
    }

    /**
     * ✅ جديد: حدث اتصال/آخر ظهور — عبر /topic/presence
     */
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PresenceEventDTO {
        private Long userId;
        private boolean online;
        private String lastSeenText;
    }

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

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DepartmentDTO {
        private Long id;
        private String name;
        private String code;
    }
}