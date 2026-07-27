package com.safwat.hr.model.message.service;

import com.safwat.hr.notification.model.HRNotification;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * =====================================================================
 * MessageThread
 * =====================================================================
 * يمثل محادثة واحدة في صندوق الوارد.
 * تحتوي على الرسالة الأساسية (root) وقائمة الردود (replies).
 * تستخدم ObservableList للردود لدعم التحديث التلقائي في واجهة المستخدم.
 */
public class MessageThread {
    private final ObservableList<HRNotification> replies = FXCollections.observableArrayList();
    private HRNotification rootMessage;

    /**
     * إنشاء محادثة جديدة برسالة أساسية.
     *
     * @param rootMessage الرسالة الأساسية للمحادثة
     */
    public MessageThread(HRNotification rootMessage) {
        this.rootMessage = rootMessage;
    }

    public HRNotification getRootMessage() {
        return rootMessage;
    }

    public void setRootMessage(HRNotification rootMessage) {
        this.rootMessage = rootMessage;
    }

    /**
     * ترجع قائمة الردود القابلة للملاحظة.
     *
     * @return ObservableList من الردود
     */
    public ObservableList<HRNotification> getReplies() {
        return replies;
    }

    /**
     * استخراج معرف المحادثة من actionTarget.
     *
     * @return معرف الرسالة أو null
     */
    public Long getId() {
        String target = rootMessage.getActionTarget();
        if (target != null && target.startsWith("messages/")) {
            try {
                return Long.parseLong(target.substring(9));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public String getSubject() {
        return rootMessage.getTitle();
    }

    public String getPreview() {
        return rootMessage.getMessage();
    }

    public String getSenderName() {
        return rootMessage.getSenderName();
    }

    public boolean isRead() {
        return rootMessage.isRead();
    }

    public void markAsRead() {
        rootMessage.markAsRead();
    }

    public LocalDateTime getTimestamp() {
        return rootMessage.getTimestamp();
    }

    /**
     * تنسيق وقت المحادثة للعرض في القائمة.
     * - اليوم: الساعة فقط (مثلاً 10:30 ص)
     * - الأمس: "أمس"
     * - أقدم: "dd/MM"
     *
     * @return النص المنسق للوقت
     */
    public String getFormattedTime() {
        LocalDateTime timestamp = rootMessage.getTimestamp();
        if (timestamp == null) return "";

        LocalDate today = LocalDate.now();
        LocalDate msgDate = timestamp.toLocalDate();

        if (msgDate.equals(today)) {
            return timestamp.format(DateTimeFormatter.ofPattern("h:mm a"));
        } else if (msgDate.equals(today.minusDays(1))) {
            return "أمس";
        } else {
            return timestamp.format(DateTimeFormatter.ofPattern("dd/MM"));
        }
    }
}