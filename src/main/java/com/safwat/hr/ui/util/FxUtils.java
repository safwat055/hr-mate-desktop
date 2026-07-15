package com.safwat.hr.ui.util;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * FxUtils — مساعدات JavaFX Thread والـ async.
 *
 * <pre>
 *   // تشغيل آمن على FX Thread
 *   FxUtils.runOnFx(() -> table.refresh());
 *
 *   // عملية ثقيلة في الخلفية ثم تحديث الـ UI
 *   FxUtils.runAsync(
 *       () -> employeeService.fetchAll(),      // background thread
 *       list -> table.setItems(list)           // FX thread
 *   );
 *
 *   // مع معالجة الخطأ
 *   FxUtils.runAsync(
 *       () -> service.save(employee),
 *       result -> HRNotification.success("تم الحفظ"),
 *       err    -> HRNotification.error("فشل الحفظ: " + err.getMessage())
 *   );
 * </pre>
 */
public final class FxUtils {

    private FxUtils() {
    }

    /**
     * تشغيل على FX Thread بأمان سواء كنت عليه أو لا
     */
    public static void runOnFx(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }

    /**
     * تشغيل في background thread، نتيجته تصل على FX Thread
     */
    public static <T> void runAsync(Supplier<T> background, Consumer<T> onResult) {
        CompletableFuture
                .supplyAsync(background)
                .thenAcceptAsync(onResult, Platform::runLater);
    }

    /**
     * تشغيل في background thread مع معالجة الخطأ
     */
    public static <T> void runAsync(Supplier<T> background,
                                    Consumer<T> onResult,
                                    Consumer<Throwable> onError) {
        CompletableFuture
                .supplyAsync(background)
                .whenCompleteAsync((result, error) -> {
                    if (error != null) {
                        if (onError != null) onError.accept(error.getCause() != null
                                ? error.getCause() : error);
                    } else {
                        onResult.accept(result);
                    }
                }, Platform::runLater);
    }

    /**
     * تشغيل عملية في الخلفية بدون نتيجة
     */
    public static void runAsync(Runnable background, Runnable onDone) {
        CompletableFuture
                .runAsync(background)
                .thenRunAsync(onDone, Platform::runLater);
    }

    /**
     * البحث عن node بالـ ID داخل parent
     */
    @SuppressWarnings("unchecked")
    public static <T extends Node> T lookup(Parent root, String id) {
        return (T) root.lookup("#" + id);
    }
}
