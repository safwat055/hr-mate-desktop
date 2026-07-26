package com.safwat.hr.notification.event;

import com.safwat.hr.notification.model.HRNotification;
import javafx.application.Platform;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * =====================================================================
 * HREventBus
 * =====================================================================
 * حافلة الأحداث المركزية (Event Bus) للإشعارات.
 * تستقبل الإشعارات من أي مصدر وتقوم بتوزيعها على المشتركين
 * بشكل غير متزامن وآمن للـ Threads.
 * <p>
 * المميزات:
 * - استخدام BlockingQueue لتجنب فقدان الأحداث
 * - دعم الاشتراك العام (global) أو حسب النوع
 * - تشغيل المعالجات على JavaFX Application Thread
 * - معالجة الأخطاء في كل معالج على حدة
 */
public class HREventBus {

    private static final HREventBus INSTANCE = new HREventBus();
    private final BlockingQueue<HRNotification> queue = new LinkedBlockingQueue<>(2000);
    private final Map<HRNotification.NotificationType, List<Consumer<HRNotification>>>
            typeSubscribers = new ConcurrentHashMap<>();
    private final List<Consumer<HRNotification>> globalSubscribers = new CopyOnWriteArrayList<>();
    private final ExecutorService dispatcher = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "hr-event-dispatcher");
        t.setDaemon(true);
        return t;
    });

    private HREventBus() {
        startDispatching();
    }

    /**
     * ترجع النسخة الوحيدة من الحافلة.
     *
     * @return INSTANCE
     */
    public static HREventBus getInstance() {
        return INSTANCE;
    }

    /**
     * نشر إشعار في الحافلة.
     * يتم وضعه في الطابور وتوزيعه لاحقاً على المشتركين.
     * تستخدم put (blocking) لضمان عدم فقدان الإشعار حتى لو امتلأ الطابور.
     *
     * @param notification الإشعار المراد نشره
     */
    public void publish(HRNotification notification) {
        try {
            queue.put(notification);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("[HREventBus] تم مقاطعة النشر: " + e.getMessage());
        }
    }

    /**
     * الاشتراك في إشعارات نوع محدد.
     *
     * @param type    نوع الإشعار
     * @param handler الدالة المعالجة
     */
    public void subscribe(HRNotification.NotificationType type,
                          Consumer<HRNotification> handler) {
        typeSubscribers
                .computeIfAbsent(type, k -> new CopyOnWriteArrayList<>())
                .add(handler);
    }

    /**
     * الاشتراك في كل الإشعارات بغض النظر عن النوع.
     *
     * @param handler الدالة المعالجة
     */
    public void subscribeAll(Consumer<HRNotification> handler) {
        globalSubscribers.add(handler);
    }

    /**
     * إلغاء الاشتراك من كل القوائم.
     *
     * @param handler الدالة المراد إزالتها
     */
    public void unsubscribeAll(Consumer<HRNotification> handler) {
        globalSubscribers.remove(handler);
        typeSubscribers.values().forEach(list -> list.remove(handler));
    }

    /**
     * بدء حلقة التوزيع في Thread منفصل.
     * تأخذ الإشعارات من الطابور وتنفذها على JavaFX Thread.
     */
    private void startDispatching() {
        dispatcher.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    HRNotification n = queue.take();
                    Platform.runLater(() -> dispatch(n));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    /**
     * توزيع الإشعار على المشتركين العامين والمشتركين حسب النوع.
     *
     * @param n الإشعار
     */
    private void dispatch(HRNotification n) {
        globalSubscribers.forEach(h -> safeCall(h, n));
        List<Consumer<HRNotification>> typed = typeSubscribers.get(n.getType());
        if (typed != null) typed.forEach(h -> safeCall(h, n));
    }

    /**
     * استدعاء معالج مع عزل الأخطاء.
     * إذا فشل معالج واحد لا يؤثر على البقية.
     *
     * @param handler المعالج
     * @param n       الإشعار
     */
    private void safeCall(Consumer<HRNotification> handler, HRNotification n) {
        try {
            handler.accept(n);
        } catch (Exception e) {
            System.err.println("[HREventBus] خطأ في المعالج: " + e.getMessage());
        }
    }

    /**
     * إيقاف حلقة التوزيع وتحرير الموارد.
     */
    public void shutdown() {
        dispatcher.shutdownNow();
    }
}