package com.safwat.hr.ui;

import com.safwat.hr.network.HttpCore;
import com.safwat.hr.ui.controls.SAFNotification;
import javafx.application.Platform;

import java.net.http.HttpTimeoutException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * أداة عامة لتشغيل أي شغل blocking (نداء شبكة / DB / ملفات) بعيدًا عن
 * JavaFX Application Thread، وإرجاع النتيجة للـ UI على الـ FX thread.
 * <p>
 * مثال:
 * <pre>
 *   UiAsync.run(
 *       () -> api.getAllMonthForReview(),
 *       months -> comboBox.getItems().setAll(months),
 *       ex -> SAFNotification.error(UiAsync.friendly(ex)));
 * </pre>
 * لو ما مررتش onError، بيظهر SAFNotification.error تلقائيًا.
 * <p>
 * للشغل اللي مالوش نتيجة (delete/update) استخدم {@link #runVoid(Runnable)}.
 */
public final class UiAsync {

    private UiAsync() {
    }

    // ═══════════════════════════════════════════════════════════
    //  Supplier<T> — شغل ليه نتيجة
    // ═══════════════════════════════════════════════════════════

    public static <T> void run(Supplier<T> work, Consumer<T> onSuccess) {
        run(work, onSuccess, ex -> SAFNotification.error(friendly(ex)));
    }

    public static <T> void run(Supplier<T> work,
                               Consumer<T> onSuccess,
                               Consumer<Throwable> onError) {
        CompletableFuture
                .supplyAsync(work, HttpCore.ASYNC_EXECUTOR)
                .whenComplete((res, ex) -> Platform.runLater(() -> {
                    if (ex != null) {
                        if (onError != null) onError.accept(ex);
                    } else if (onSuccess != null) {
                        onSuccess.accept(res);
                    }
                }));
    }

    // ═══════════════════════════════════════════════════════════
    //  Runnable — شغل مالوش نتيجة (delete/update)
    // ═══════════════════════════════════════════════════════════

    /** شغل مالوش نتيجة، مع onError تلقائي. */
    public static void runVoid(Runnable work) {
        runVoid(work, null, ex -> SAFNotification.error(friendly(ex)));
    }

    /** شغل مالوش نتيجة، مع onDone بيتنادى بعد النجاح. */
    public static void runVoid(Runnable work, Runnable onDone) {
        runVoid(work, onDone, ex -> SAFNotification.error(friendly(ex)));
    }

    /** شغل مالوش نتيجة، مع onDone و onError. */
    public static void runVoid(Runnable work, Runnable onDone, Consumer<Throwable> onError) {
        run(() -> {
            work.run();
            return null;
        }, _ -> {
            if (onDone != null) onDone.run();
        }, onError);
    }

    // ═══════════════════════════════════════════════════════════
    //  Helpers
    // ═══════════════════════════════════════════════════════════

    /** رسالة عربية مفهومة من أي exception (بتفك CompletionException). */
    public static String friendly(Throwable ex) {
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) root = root.getCause();
        if (root instanceof HttpTimeoutException) return "انتهت مهلة الاتصال بالسيرفر";
        return root.getMessage() != null ? root.getMessage() : root.getClass().getSimpleName();
    }
}