package com.safwat.hr.notification.event;

import com.safwat.hr.notification.model.HRNotification;
import javafx.application.Platform;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * حافلة الأحداث المركزية.
 * تستقبل الأحداث من الخدمات الخلفية وتوزعها على المشتركين.
 */
public class HREventBus {

    // Singleton
    private static final HREventBus INSTANCE = new HREventBus();
    // طابور الأحداث - thread-safe
    private final BlockingQueue<HRNotification> queue =
            new LinkedBlockingQueue<>(500);
    // المشتركون مصنفون حسب نوع الإشعار
    private final Map<HRNotification.NotificationType, List<Consumer<HRNotification>>>
            subscribers = new ConcurrentHashMap<>();
    // مشتركون يستقبلون كل الأنواع
    private final List<Consumer<HRNotification>> globalSubscribers =
            new CopyOnWriteArrayList<>();
    private final ExecutorService dispatcher =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "hr-event-dispatcher");
                t.setDaemon(true);
                return t;
            });

    private HREventBus() {
        startDispatching();
    }

    public static HREventBus getInstance() {
        return INSTANCE;
    }

    // ===================== نشر حدث =====================

    /**
     * ينشر إشعاراً من أي thread - آمن تماماً.
     */
    public void publish(HRNotification notification) {
        queue.offer(notification);
    }

    // ===================== الاشتراك =====================
    public void subscribe(HRNotification.NotificationType type,
                          Consumer<HRNotification> handler) {
        subscribers.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>())
                .add(handler);
    }

    public void subscribeAll(Consumer<HRNotification> handler) {
        globalSubscribers.add(handler);
    }

    public void unsubscribeAll(Consumer<HRNotification> handler) {
        globalSubscribers.remove(handler);
        subscribers.values().forEach(list -> list.remove(handler));
    }

    // ===================== التوزيع =====================
    private void startDispatching() {
        dispatcher.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    HRNotification n = queue.take();  // ينتظر حتى يجد حدثاً
                    dispatch(n);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    private void dispatch(HRNotification n) {
        // الإرسال دائماً على JavaFX thread
        Platform.runLater(() -> {
            // المشتركون العامون
            globalSubscribers.forEach(h -> safeCall(h, n));

            // المشتركون حسب النوع
            List<Consumer<HRNotification>> typed = subscribers.get(n.getType());
            if (typed != null) typed.forEach(h -> safeCall(h, n));
        });
    }

    private void safeCall(Consumer<HRNotification> handler, HRNotification n) {
        try {
            handler.accept(n);
        } catch (Exception e) {
            System.err.println("[HREventBus] خطأ في معالج الإشعار: " + e.getMessage());
        }
    }

    public void shutdown() {
        dispatcher.shutdownNow();
    }
}
