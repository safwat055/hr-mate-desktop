package com.safwat.hr.notification.model;

import javafx.beans.property.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * =====================================================================
 * HRNotification
 * =====================================================================
 * نموذج بيانات الإشعار المركزي في النظام.
 * يمثل أي إشعار (نظام أو رسالة) مع كل خصائصه مثل العنوان والمحتوى والمرسل والوقت.
 * يستخدم JavaFX Properties لدعم الربط التلقائي مع واجهة المستخدم.
 * يدعم نمط البناء (Builder Pattern) لتسهيل إنشاء الكائنات.
 * يحتوي على كلاس داخلي Attachment لتمثيل المرفقات.
 * <p>
 * الأقسام الرئيسية:
 * - خصائص الإشعار (Properties)
 * - Builder للبناء
 * - Enums: NotificationCategory, NotificationType, Priority
 * - Attachment: بيانات الملف المرفق
 */
public class HRNotification {

    // ===================== خصائص الإشعار =====================
    private final StringProperty id = new SimpleStringProperty(UUID.randomUUID().toString());
    private final StringProperty title = new SimpleStringProperty();
    private final StringProperty message = new SimpleStringProperty();
    private final StringProperty messageBody = new SimpleStringProperty();
    private final ObjectProperty<NotificationType> type = new SimpleObjectProperty<>();
    private final ObjectProperty<NotificationCategory> category = new SimpleObjectProperty<>(NotificationCategory.SYSTEM);
    private final ObjectProperty<Priority> priority = new SimpleObjectProperty<>(Priority.NORMAL);
    private final BooleanProperty read = new SimpleBooleanProperty(false);
    private final ObjectProperty<LocalDateTime> timestamp = new SimpleObjectProperty<>(LocalDateTime.now());
    private final StringProperty actionLabel = new SimpleStringProperty();
    private final StringProperty actionTarget = new SimpleStringProperty();
    private final StringProperty senderName = new SimpleStringProperty();
    private final StringProperty senderUsername = new SimpleStringProperty();
    private final StringProperty senderAvatar = new SimpleStringProperty();
    private final List<Attachment> attachments = new ArrayList<>();

    private HRNotification() {
    }

    /**
     * إنشاء Builder جديد لبناء كائن HRNotification.
     *
     * @return Builder فارغ
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * ترجع قائمة المرفقات المرتبطة بالإشعار.
     *
     * @return قائمة المرفقات
     */
    public List<Attachment> getAttachments() {
        return attachments;
    }

    // ===================== Getters =====================

    public String getId() {
        return id.get();
    }

    public String getTitle() {
        return title.get();
    }

    public String getMessage() {
        return message.get();
    }

    public String getMessageBody() {
        return messageBody.get();
    }

    public NotificationType getType() {
        return type.get();
    }

    public NotificationCategory getCategory() {
        return category.get();
    }

    public Priority getPriority() {
        return priority.get();
    }

    public boolean isRead() {
        return read.get();
    }

    public LocalDateTime getTimestamp() {
        return timestamp.get();
    }

    public String getActionLabel() {
        return actionLabel.get();
    }

    public String getActionTarget() {
        return actionTarget.get();
    }

    public String getSenderName() {
        return senderName.get();
    }

    public String getSenderUsername() {
        return senderUsername.get();
    }

    public String getSenderAvatar() {
        return senderAvatar.get();
    }

    /**
     * التحقق من وجود مرفقات.
     *
     * @return true إذا كان الإشعار يحتوي على مرفقات
     */
    public boolean hasAttachments() {
        return !attachments.isEmpty();
    }

    public BooleanProperty readProperty() {
        return read;
    }

    /**
     * للتوافق مع الكود القديم.
     *
     * @return true إذا كان يحتوي على ملف
     */
    public boolean hasFile() {
        return !attachments.isEmpty();
    }

    /**
     * ترجع مسار أول ملف مرفق.
     *
     * @return مسار الملف أو null
     */
    public String getFilePath() {
        return attachments.isEmpty() ? null : attachments.get(0).getFilePath();
    }

    /**
     * التحقق مما إذا كان الإشعار من نوع رسالة.
     *
     * @return true إذا كانت الفئة MESSAGE
     */
    public boolean isMessage() {
        return category.get() == NotificationCategory.MESSAGE;
    }

    /**
     * تعليم الإشعار كمقروء.
     */
    public void markAsRead() {
        read.set(true);
    }

    /**
     * تنسيق وقت الإشعار للعرض.
     * - أقل من دقيقة: "الآن"
     * - أقل من ساعة: "منذ X دقيقة"
     * - أقل من يوم: "منذ X ساعة"
     * - أقل من أسبوع: "منذ X يوم"
     * - أقدم: "dd/MM/yyyy"
     *
     * @return النص المنسق للوقت
     */
    public String getFormattedTime() {
        LocalDateTime now = LocalDateTime.now();
        Duration dur = Duration.between(timestamp.get(), now);
        long mins = dur.toMinutes();
        if (mins < 1) return "الآن";
        if (mins < 60) return "منذ " + mins + " دقيقة";
        long hrs = mins / 60;
        if (hrs < 24) return "منذ " + hrs + " ساعة";
        long days = hrs / 24;
        if (days < 7) return "منذ " + days + " يوم";
        return timestamp.get().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    /**
     * ترجع الحروف الأولى من اسم المرسل لعرضها كصورة رمزية.
     * إذا كان الاسم مكون من كلمة واحدة ترجع أول حرف، وإذا كان من كلمتين ترجع أول حرف من كل واحدة.
     *
     * @return الحروف الأولى أو "؟" إذا لم يوجد اسم
     */
    public String getAvatarInitials() {
        if (senderAvatar.get() != null && !senderAvatar.get().isBlank())
            return senderAvatar.get();
        String name = senderName.get();
        if (name == null || name.isBlank()) return "؟";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return String.valueOf(parts[0].charAt(0));
        return "" + parts[0].charAt(0) + parts[1].charAt(0);
    }

    // ===================== Enums =====================

    /**
     * فئات الإشعارات الرئيسية.
     */
    public enum NotificationCategory {SYSTEM, MESSAGE}

    /**
     * أنواع الإشعارات مع ألوانها وخلفياتها.
     */
    public enum NotificationType {
        EMPLOYEE("موظفون", "#185FA5", "#E6F1FB"),
        SALARY("رواتب", "#3B6D11", "#EAF3DE"),
        LEAVE("إجازات", "#854F0B", "#FAEEDA"),
        TRAINING("تدريب", "#534AB7", "#EEEDFE"),
        TASK("مهام", "#A32D2D", "#FCEBEB"),
        SYSTEM("النظام", "#5F5E5A", "#F1EFE8"),
        MESSAGE("رسالة", "#0F6E56", "#E6F5F1");

        public final String label, color, bgColor;

        NotificationType(String label, String color, String bgColor) {
            this.label = label;
            this.color = color;
            this.bgColor = bgColor;
        }
    }

    /**
     * مستويات أولوية الإشعار.
     */
    public enum Priority {LOW, NORMAL, HIGH, URGENT}

    // ===================== Attachment =====================

    /**
     * يمثل ملفاً مرفقاً برسالة أو إشعار.
     */
    public static class Attachment {
        private final String fileName;
        private final String filePath;
        private final String mimeType;
        private final long fileSize;
        private final String downloadToken;

        /**
         * إنشاء مرفق جديد.
         *
         * @param fileName      اسم الملف
         * @param filePath      مسار الملف
         * @param mimeType      نوع MIME
         * @param fileSize      حجم الملف بالبايت
         * @param downloadToken رمز التحميل من الخادم
         */
        public Attachment(String fileName, String filePath,
                          String mimeType, long fileSize, String downloadToken) {
            this.fileName = fileName;
            this.filePath = filePath;
            this.mimeType = mimeType;
            this.fileSize = fileSize;
            this.downloadToken = downloadToken;
        }

        public String getFileName() {
            return fileName;
        }

        public String getFilePath() {
            return filePath;
        }

        public String getMimeType() {
            return mimeType;
        }

        public long getFileSize() {
            return fileSize;
        }

        public String getDownloadToken() {
            return downloadToken;
        }

        /**
         * تنسيق حجم الملف للعرض (B, KB, MB).
         *
         * @return النص المنسق للحجم
         */
        public String getFormattedSize() {
            if (fileSize <= 0) return "—";
            if (fileSize < 1024) return fileSize + " B";
            if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
            return String.format("%.1f MB", fileSize / (1024.0 * 1024));
        }

        /**
         * ترجع أيقونة نصية حسب نوع الملف.
         *
         * @return أيقونة الملف
         */
        public String getIcon() {
            if (mimeType == null) return "[FILE]";
            return switch (mimeType) {
                case "application/pdf" -> "[PDF]";
                case "image/jpeg", "image/png" -> "[IMG]";
                case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                     "application/vnd.ms-excel" -> "[XLS]";
                case "application/zip" -> "[ZIP]";
                default -> "[FILE]";
            };
        }
    }

    // ===================== Builder =====================

    /**
     * Builder لبناء كائن HRNotification بسهولة.
     */
    public static class Builder {
        private final HRNotification n = new HRNotification();

        public Builder title(String v) {
            n.title.set(v);
            return this;
        }

        public Builder read(boolean v) {
            n.read.set(v);
            return this;
        }

        public Builder message(String v) {
            n.message.set(v);
            return this;
        }

        public Builder messageBody(String v) {
            n.messageBody.set(v);
            return this;
        }

        public Builder type(NotificationType v) {
            n.type.set(v);
            return this;
        }

        public Builder category(NotificationCategory v) {
            n.category.set(v);
            return this;
        }

        public Builder priority(Priority v) {
            n.priority.set(v);
            return this;
        }

        public Builder timestamp(LocalDateTime v) {
            n.timestamp.set(v);
            return this;
        }

        public Builder sender(String name) {
            n.senderName.set(name);
            return this;
        }

        public Builder senderUsername(String username) {
            n.senderUsername.set(username);
            return this;
        }

        public Builder senderAvatar(String avatar) {
            n.senderAvatar.set(avatar);
            return this;
        }

        public Builder action(String label, String target) {
            n.actionLabel.set(label);
            n.actionTarget.set(target);
            return this;
        }

        public Builder file(String path) {
            String name = path.contains("/")
                    ? path.substring(path.lastIndexOf('/') + 1) : path;
            n.attachments.add(new Attachment(name, path, guessMime(path), 0, null));
            return this;
        }

        public Builder attachment(String name, String path, String mime, long size, String downloadToken) {
            n.attachments.add(new Attachment(name, path, mime, size, downloadToken));
            return this;
        }

        public Builder attachment(String name, String path, String mime, long size) {
            n.attachments.add(new Attachment(name, path, mime, size, null));
            return this;
        }

        /**
         * بناء الكائن النهائي مع تعيين الأنواع الافتراضية إذا لم تُحدد.
         *
         * @return كائن HRNotification جاهز
         */
        public HRNotification build() {
            if (n.category.get() == NotificationCategory.MESSAGE && n.type.get() == null) {
                n.type.set(NotificationType.MESSAGE);
            }
            if (n.category.get() == NotificationCategory.SYSTEM && n.type.get() == null) {
                n.type.set(NotificationType.SYSTEM);
            }
            return n;
        }

        private String guessMime(String path) {
            if (path.endsWith(".pdf")) return "application/pdf";
            if (path.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            if (path.endsWith(".xls")) return "application/vnd.ms-excel";
            if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
            if (path.endsWith(".png")) return "image/png";
            if (path.endsWith(".zip")) return "application/zip";
            return "application/octet-stream";
        }
    }
}