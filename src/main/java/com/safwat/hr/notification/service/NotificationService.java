package com.safwat.hr.notification.service;

import com.safwat.hr.notification.event.HREventBus;
import com.safwat.hr.notification.model.HRNotification;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;
import java.util.stream.Collectors;

/**
 * =====================================================
 * NotificationService — الخدمة المركزية للإشعارات
 * =====================================================
 * <p>
 * - تخزن كل الإشعارات في ObservableList (مرتبطة بالواجهة)
 * - تحسب عداد الغير مقروءة تلقائياً
 * - تستقبل من HREventBus وتضيف للقائمة
 * <p>
 * Singleton — استخدمها هكذا:
 * NotificationService.getInstance().send(notification);
 */
public class NotificationService {

    private static final NotificationService INSTANCE = new NotificationService();
    private static final int MAX_NOTIFICATIONS = 200;
    private final ObservableList<HRNotification> notifications =
            FXCollections.observableArrayList();
    private final IntegerProperty unreadCount = new SimpleIntegerProperty(0);

    private NotificationService() {
        // الاشتراك في EventBus لاستقبال كل الأحداث
        HREventBus.getInstance().subscribeAll(this::receive);
    }

    public static NotificationService getInstance() {
        return INSTANCE;
    }

    // ===================== الاستقبال من EventBus =====================
    private void receive(HRNotification n) {
        notifications.add(0, n);  // الأحدث أولاً
        updateUnreadCount();

        // تنظيف القديم
        if (notifications.size() > MAX_NOTIFICATIONS)
            notifications.remove(MAX_NOTIFICATIONS, notifications.size());
    }

    // ===================== الإرسال =====================

    /**
     * الطريقة الرئيسية لإرسال إشعار من أي مكان في التطبيق.
     * آمنة من أي Thread.
     */
    public void send(HRNotification notification) {
        HREventBus.getInstance().publish(notification);
    }

    // ===================== العمليات =====================
    public void markAsRead(HRNotification notification) {
        if (!notification.isRead()) {
            notification.markAsRead();
            // ✅ إذا كانت رسالة، أرسل طلباً إلى الخادم
            if (notification.isMessage() && notification.getActionTarget() != null) {
                String target = notification.getActionTarget();
                if (target.startsWith("messages/")) {
                    try {
                        // استخراج المعرف الرقمي من "messages/123"
                        String idStr = target.substring(9);
                        Long id = Long.parseLong(idStr);
                        MessageClientService.getInstance().markMessageAsRead(id);
                    } catch (NumberFormatException e) {
                        System.err.println("⚠️ معرف رسالة غير صالح: " + target);
                    }
                }
            }

            updateUnreadCount();
        }
    }

    public void markAllAsRead() {
        notifications.forEach(HRNotification::markAsRead);
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

    public List<HRNotification> getMessages() {
        return notifications.stream()
                .filter(HRNotification::isMessage)
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

    public void updateUnreadCount() {
        long count = notifications.stream().filter(n -> !n.isRead()).count();
        unreadCount.set((int) count);
    }

    public void updateUnreadCount(int a) {

        unreadCount.set((int) a);
    }
}
