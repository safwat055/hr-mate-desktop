package com.safwat.hr.notification.service;

import com.safwat.hr.notification.event.HREventBus;
import com.safwat.hr.notification.model.HRNotification;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * الخدمة المركزية لإدارة الإشعارات.
 * تخزنها، تصنفها، وتوفرها للواجهة.
 */
public class NotificationService {

    private static final NotificationService INSTANCE = new NotificationService();
    private static final int MAX_NOTIFICATIONS = 200;
    // قائمة الإشعارات - Observable للربط بالواجهة
    private final ObservableList<HRNotification> notifications =
            FXCollections.observableArrayList();

    // عداد الإشعارات غير المقروءة
    private final IntegerProperty unreadCount = new SimpleIntegerProperty(0);

    private NotificationService() {
        // الاشتراك في Event Bus لاستقبال كل الأحداث
        HREventBus.getInstance().subscribeAll(this::receive);
    }

    public static NotificationService getInstance() {
        return INSTANCE;
    }

    // ===================== الاستقبال =====================
    private void receive(HRNotification n) {
        // إضافة في المقدمة (الأحدث أولاً)
        notifications.add(0, n);
        updateUnreadCount();

        // تنظيف القديم إذا تجاوز الحد
        if (notifications.size() > MAX_NOTIFICATIONS) {
            notifications.remove(MAX_NOTIFICATIONS, notifications.size());
        }
    }

    // ===================== النشر من الخدمات =====================

    /**
     * تُستخدم من أي خدمة خلفية لإرسال إشعار.
     */
    public void send(HRNotification notification) {
        HREventBus.getInstance().publish(notification);
    }

    // ===================== العمليات =====================
    public void markAsRead(HRNotification n) {
        if (!n.isRead()) {
            n.markAsRead();
            updateUnreadCount();
        }
    }

    public void markAllAsRead() {
        notifications.forEach(n -> n.markAsRead());
        unreadCount.set(0);
    }

    public void remove(HRNotification n) {
        notifications.remove(n);
        updateUnreadCount();
    }

    public void clearAll() {
        notifications.clear();
        unreadCount.set(0);
    }

    // ===================== التصفية =====================
    public List<HRNotification> getByType(HRNotification.NotificationType type) {
        return notifications.stream()
                .filter(n -> n.getType() == type)
                .collect(Collectors.toList());
    }

    public List<HRNotification> getUnread() {
        return notifications.stream()
                .filter(n -> !n.isRead())
                .collect(Collectors.toList());
    }

    public List<HRNotification> getByPriority(HRNotification.Priority p) {
        return notifications.stream()
                .filter(n -> n.getPriority() == p)
                .sorted(Comparator.comparing(HRNotification::getTimestamp).reversed())
                .collect(Collectors.toList());
    }

    // ===================== Getters =====================
    public ObservableList<HRNotification> getAll() {
        return notifications;
    }

    public IntegerProperty unreadCountProperty() {
        return unreadCount;
    }

    public int getUnreadCount() {
        return unreadCount.get();
    }

    private void updateUnreadCount() {
        long count = notifications.stream().filter(n -> !n.isRead()).count();
        unreadCount.set((int) count);
    }
}
