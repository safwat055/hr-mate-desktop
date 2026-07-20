package com.safwat.hr.notification.model;

import javafx.beans.property.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * نموذج بيانات الإشعار — يدعم إشعارات النظام والرسائل بين المستخدمين.
 *
 * التغييرات عن النسخة السابقة:
 *  - إضافة NotificationCategory (SYSTEM / MESSAGE)
 *  - إضافة قائمة Attachment للمرفقات المتعددة
 *  - إضافة senderAvatar و messageBody للرسائل الطويلة
 */
public class HRNotification {

    // ===================== التصنيف الرئيسي =====================
    public enum NotificationCategory {
        SYSTEM,   // إشعارات تلقائية من خدمات النظام
        MESSAGE   // رسائل من مستخدمين
    }

    // ===================== أنواع إشعارات النظام =====================
    public enum NotificationType {
        EMPLOYEE ("موظفون", "#185FA5", "#E6F1FB"),
        SALARY   ("رواتب",  "#3B6D11", "#EAF3DE"),
        LEAVE    ("إجازات", "#854F0B", "#FAEEDA"),
        TRAINING ("تدريب",  "#534AB7", "#EEEDFE"),
        TASK     ("مهام",   "#A32D2D", "#FCEBEB"),
        SYSTEM   ("النظام", "#5F5E5A", "#F1EFE8"),
        MESSAGE  ("رسالة",  "#0F6E56", "#E6F5F1");  // للرسائل

        public final String label;
        public final String color;
        public final String bgColor;

        NotificationType(String label, String color, String bgColor) {
            this.label   = label;
            this.color   = color;
            this.bgColor = bgColor;
        }
    }

    // ===================== مستوى الأهمية =====================
    public enum Priority { LOW, NORMAL, HIGH, URGENT }

    // ===================== المرفق =====================
    public static class Attachment {
        private final String fileName;
        private final String filePath;
        private final String mimeType;
        private final long   fileSize;       // بالبايت
        private final String downloadToken;  // توكن آمن للتحميل

        public Attachment(String fileName, String filePath,
                          String mimeType, long fileSize) {
            this.fileName      = fileName;
            this.filePath      = filePath;
            this.mimeType      = mimeType;
            this.fileSize      = fileSize;
            this.downloadToken = UUID.randomUUID().toString();
        }

        public String getFileName()      { return fileName; }
        public String getFilePath()      { return filePath; }
        public String getMimeType()      { return mimeType; }
        public long   getFileSize()      { return fileSize; }
        public String getDownloadToken() { return downloadToken; }

        public String getFormattedSize() {
            if (fileSize < 1024)             return fileSize + " B";
            if (fileSize < 1024 * 1024)      return String.format("%.1f KB", fileSize / 1024.0);
            return String.format("%.1f MB", fileSize / (1024.0 * 1024));
        }

        public String getIcon() {
            if (mimeType == null) return "[FILE]";
            return switch (mimeType) {
                case "application/pdf"  -> "[PDF]";
                case "image/jpeg",
                     "image/png"        -> "[IMG]";
                case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                     "application/vnd.ms-excel" -> "[XLS]";
                case "application/zip"  -> "[ZIP]";
                default                 -> "[FILE]";
            };
        }
    }

    // ===================== الحقول =====================
    private final StringProperty  id           = new SimpleStringProperty(UUID.randomUUID().toString());
    private final StringProperty  title        = new SimpleStringProperty();
    private final StringProperty  message      = new SimpleStringProperty();
    private final StringProperty  messageBody  = new SimpleStringProperty();  // للرسائل الطويلة
    private final ObjectProperty<NotificationType>     type     = new SimpleObjectProperty<>();
    private final ObjectProperty<NotificationCategory> category = new SimpleObjectProperty<>(NotificationCategory.SYSTEM);
    private final ObjectProperty<Priority>             priority = new SimpleObjectProperty<>(Priority.NORMAL);
    private final BooleanProperty read         = new SimpleBooleanProperty(false);
    private final ObjectProperty<LocalDateTime> timestamp = new SimpleObjectProperty<>(LocalDateTime.now());
    private final StringProperty  actionLabel  = new SimpleStringProperty();
    private final StringProperty  actionTarget = new SimpleStringProperty();
    private final StringProperty  senderName   = new SimpleStringProperty();
    private final StringProperty  senderAvatar = new SimpleStringProperty();  // أحرف الاسم أو مسار صورة
    private final List<Attachment> attachments = new ArrayList<>();

    // ===================== Constructor خاص =====================
    private HRNotification() {}

    // ===================== Builder =====================
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final HRNotification n = new HRNotification();

        public Builder title(String v)                   { n.title.set(v);        return this; }
        public Builder message(String v)                 { n.message.set(v);      return this; }
        public Builder messageBody(String v)             { n.messageBody.set(v);  return this; }
        public Builder type(NotificationType v)          { n.type.set(v);         return this; }
        public Builder category(NotificationCategory v)  { n.category.set(v);     return this; }
        public Builder priority(Priority v)              { n.priority.set(v);     return this; }
        public Builder timestamp(LocalDateTime v)        { n.timestamp.set(v);    return this; }
        public Builder sender(String name)               { n.senderName.set(name); return this; }
        public Builder senderAvatar(String avatar)       { n.senderAvatar.set(avatar); return this; }
        public Builder action(String label, String target) {
            n.actionLabel.set(label);
            n.actionTarget.set(target);
            return this;
        }
        // مرفق واحد (للتوافق مع الكود القديم)
        public Builder file(String path) {
            String name = path.contains("/")
                ? path.substring(path.lastIndexOf('/') + 1) : path;
            n.attachments.add(new Attachment(name, path, guessMime(path), 0));
            return this;
        }
        // مرفق كامل التفاصيل
        public Builder attachment(String name, String path, String mime, long size) {
            n.attachments.add(new Attachment(name, path, mime, size));
            return this;
        }

        public HRNotification build() {
            // لو category رسالة — type يكون MESSAGE تلقائياً
            if (n.category.get() == NotificationCategory.MESSAGE
                    && n.type.get() == null) {
                n.type.set(NotificationType.MESSAGE);
            }
            return n;
        }

        private String guessMime(String path) {
            if (path.endsWith(".pdf"))  return "application/pdf";
            if (path.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
            if (path.endsWith(".png"))  return "image/png";
            if (path.endsWith(".zip"))  return "application/zip";
            return "application/octet-stream";
        }
    }

    // ===================== Getters =====================
    public String              getId()           { return id.get(); }
    public String              getTitle()        { return title.get(); }
    public String              getMessage()      { return message.get(); }
    public String              getMessageBody()  { return messageBody.get(); }
    public NotificationType    getType()         { return type.get(); }
    public NotificationCategory getCategory()   { return category.get(); }
    public Priority            getPriority()     { return priority.get(); }
    public boolean             isRead()          { return read.get(); }
    public LocalDateTime       getTimestamp()    { return timestamp.get(); }
    public String              getActionLabel()  { return actionLabel.get(); }
    public String              getActionTarget() { return actionTarget.get(); }
    public String              getSenderName()   { return senderName.get(); }
    public String              getSenderAvatar() { return senderAvatar.get(); }
    public List<Attachment>    getAttachments()  { return Collections.unmodifiableList(attachments); }
    public boolean             hasAttachments()  { return !attachments.isEmpty(); }

    // للتوافق مع الكود القديم
    public boolean hasFile() { return !attachments.isEmpty(); }
    public String  getFilePath() {
        return attachments.isEmpty() ? null : attachments.get(0).getFilePath();
    }

    public BooleanProperty readProperty() { return read; }
    public void markAsRead()              { read.set(true); }

    public boolean isMessage() {
        return category.get() == NotificationCategory.MESSAGE;
    }

    public String getFormattedTime() {
        LocalDateTime now = LocalDateTime.now();
        long minutes = java.time.Duration.between(timestamp.get(), now).toMinutes();
        if (minutes < 1)  return "الآن";
        if (minutes < 60) return "منذ " + minutes + " دقيقة";
        long hours = minutes / 60;
        if (hours < 24)   return "منذ " + hours + " ساعة";
        return timestamp.get().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    /**
     * يولد أحرف الصورة الرمزية من اسم المرسل
     * مثال: "أحمد محمد" → "أم"
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
}
