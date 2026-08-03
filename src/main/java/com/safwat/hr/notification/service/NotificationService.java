package com.safwat.hr.notification.service;

import com.safwat.hr.message.service.MessageClientService;
import com.safwat.hr.notification.event.HREventBus;
import com.safwat.hr.notification.model.HRNotification;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;
import java.util.stream.Collectors;

/**
 * =====================================================================
 * NotificationService
 * =====================================================================
 * الخدمة المركزية لإدارة الإشعارات في التطبيق.
 * تحتفظ بقائمة الإشعارات في ObservableList لدعم الربط مع واجهة المستخدم.
 * تستقبل الإشعارات من HREventBus وتقوم بتحديث العدد غير المقروء.
 * تدعم تعليم الإشعارات كمقروءة وإرسال التحديثات للخادم.
 * <p>
 * الاستخدام:
 * NotificationService.getInstance().send(notification);
 */
public class NotificationService {

    private static final NotificationService INSTANCE = new NotificationService();
    private static final int MAX_NOTIFICATIONS = 200;
    private final ObservableList<HRNotification> notifications =
            FXCollections.observableArrayList();
    private final IntegerProperty unreadCount = new SimpleIntegerProperty(0);

    private NotificationService() {
        HREventBus.getInstance().subscribeAll(this::receive);
    }

    /**
     * ترجع النسخة الوحيدة من الخدمة.
     *
     * @return INSTANCE
     */
    public static NotificationService getInstance() {
        return INSTANCE;
    }

    /**
     * استقبال إشعار من EventBus وإضافته للقائمة.
     * يتم الحفاظ على الحد الأقصى للإشعارات.
     *
     * @param n الإشعار المستلم
     */
    private void receive(HRNotification n) {
        notifications.add(0, n);
        updateUnreadCount();

        if (notifications.size() > MAX_NOTIFICATIONS)
            notifications.remove(MAX_NOTIFICATIONS, notifications.size());
    }

    /**
     * إرسال إشعار جديد عبر EventBus.
     *
     * @param notification الإشعار المراد إرساله
     */
    public void send(HRNotification notification) {
        HREventBus.getInstance().publish(notification);
    }

    /**
     * تعليم إشعار كمقروء محلياً وإرسال التحديث للخادم إذا كانت رسالة.
     *
     * @param notification الإشعار المراد تعليمه
     */
    public void markAsRead(HRNotification notification) {
        if (!notification.isRead()) {
            notification.markAsRead();

            if (notification.isMessage() && notification.getActionTarget() != null) {
                String target = notification.getActionTarget();
                if (target.startsWith("messages/")) {
                    try {
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

    /**
     * تعليم كل الإشعارات كمقروءة.
     */
    public void markAllAsRead() {
        notifications.forEach(HRNotification::markAsRead);
        unreadCount.set(0);
    }

    /**
     * إزالة إشعار محدد.
     *
     * @param n الإشعار المراد إزالته
     */
    public void remove(HRNotification n) {
        notifications.remove(n);
        updateUnreadCount();
    }

    /**
     * مسح كل الإشعارات.
     */
    public void clearAll() {
        notifications.clear();
        unreadCount.set(0);
    }

    /**
     * ترجع الإشعارات حسب النوع.
     *
     * @param type نوع الإشعار
     * @return قائمة بالإشعارات
     */
    public List<HRNotification> getByType(HRNotification.NotificationType type) {
        return notifications.stream()
                .filter(n -> n.getType() == type)
                .collect(Collectors.toList());
    }

    /**
     * ترجع الإشعارات غير المقروءة.
     *
     * @return قائمة بالإشعارات غير المقروءة
     */
    public List<HRNotification> getUnread() {
        return notifications.stream()
                .filter(n -> !n.isRead())
                .collect(Collectors.toList());
    }

    /**
     * ترجع رسائل المستخدمين فقط.
     *
     * @return قائمة بالرسائل
     */
    public List<HRNotification> getMessages() {
        return notifications.stream()
                .filter(HRNotification::isMessage)
                .collect(Collectors.toList());
    }

    public ObservableList<HRNotification> getAll() {
        return notifications;
    }

    public IntegerProperty unreadCountProperty() {
        return unreadCount;
    }

    public int getUnreadCount() {
        return unreadCount.get();
    }

    /**
     * تحديث عدد الإشعارات غير المقروءة من القائمة المحلية.
     */
    public void updateUnreadCount() {
        long count = notifications.stream().filter(n -> !n.isRead()).count();
        unreadCount.set((int) count);
    }

    /**
     * تعيين عدد غير المقروءة مباشرة (يستخدم عند التهيئة من الخادم).
     *
     * @param a العدد الجديد
     */
    public void updateUnreadCount(int a) {
        unreadCount.set(a);
    }
}