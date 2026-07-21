package com.safwat.hr.notification.event;

import com.safwat.hr.notification.model.HRNotification;
import javafx.application.Platform;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * =====================================================
 * HREventBus — حافلة الأحداث المركزية — معدّل
 * =====================================================
 */
public class HREventBus {

    private static final HREventBus INSTANCE = new HREventBus();
    // ✅ زوّد الحجم واستخدم put بدل offer
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

    public static HREventBus getInstance() {
        return INSTANCE;
    }

    // ===================== نشر — معدّل =====================
    public void publish(HRNotification notification) {
        try {
            queue.put(notification);  // ✅ blocking — ما بيضيعش
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("[HREventBus] تم مقاطعة النشر: " + e.getMessage());
        }
    }

    // ===================== الاشتراك =====================
    public void subscribe(HRNotification.NotificationType type,
                          Consumer<HRNotification> handler) {
        typeSubscribers
                .computeIfAbsent(type, k -> new CopyOnWriteArrayList<>())
                .add(handler);
    }

    public void subscribeAll(Consumer<HRNotification> handler) {
        globalSubscribers.add(handler);
    }

    public void unsubscribeAll(Consumer<HRNotification> handler) {
        globalSubscribers.remove(handler);
        typeSubscribers.values().forEach(list -> list.remove(handler));
    }

    // ===================== التوزيع =====================
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

    private void dispatch(HRNotification n) {
        globalSubscribers.forEach(h -> safeCall(h, n));
        List<Consumer<HRNotification>> typed = typeSubscribers.get(n.getType());
        if (typed != null) typed.forEach(h -> safeCall(h, n));
    }

    private void safeCall(Consumer<HRNotification> handler, HRNotification n) {
        try {
            handler.accept(n);
        } catch (Exception e) {
            System.err.println("[HREventBus] خطأ في المعالج: " + e.getMessage());
        }
    }

    public void shutdown() {
        dispatcher.shutdownNow();
    }
}