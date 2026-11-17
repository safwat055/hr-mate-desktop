package com.safwat.hr.shared.ui;

import com.safwat.hr.network.HttpCore;
import com.safwat.hr.ui.controls.SAFNotification;
import javafx.application.Platform;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;

import java.net.http.HttpTimeoutException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * ────────────────────────────────────────────────────────────
 * SmartSearchHelper
 * ────────────────────────────────────────────────────────────
 * أداة بحث ذكية عامة، تُستخدم في أي Controller دون تبعية.
 * <p>
 * الأنماط المدعومة:
 * <ol>
 *   <li>String فقط (backward compatible)</li>
 *   <li>Generic Object + حقل واحد</li>
 *   <li>Generic Object + تحديث متعدد الحقول (Multi-Field Bind)</li>
 *   <li>نفس السابق + زرار</li>
 *   <li>نسخ تستقبل نص الحقل مباشرة ({@code bindWithInput})</li>
 * </ol>
 * <p>
 * المشغلات: Enter | Double-Click | زرار (لو اتمرر)
 * <p>
 * ⭐ threading:
 * الـ dataSupplier بيتنفذ على {@link HttpCore#ASYNC_EXECUTOR} (مش على الـ FX thread)،
 * فمسموح يعمل نداء شبكة blocking. الـ Dialog وتحديث الحقول بيرجعوا على الـ FX thread.
 * <p>
 * المطابقة الفورية: لو فيه نص مكتوب وعنصر واحد بس بيطابقه {@link SearchDialog#matches(Object, String)}،
 * الاختيار بيتم من غير Dialog.
 * ────────────────────────────────────────────────────────────
 */
public final class SmartSearchHelper {

    private SmartSearchHelper() {
    } // utility class

    // ═══════════════════════════════════════════════════════════
    //  1. STRING-ONLY (backward compatible)
    // ═══════════════════════════════════════════════════════════

    public static void bind(
            TextField triggerField,
            Supplier<List<String>> dataSupplier,
            Consumer<String> onSelect) {

        bind(triggerField, dataSupplier, s -> s, onSelect,
                SearchDialog.forStrings().title("اختر"));
    }

    // ═══════════════════════════════════════════════════════════
    //  2. GENERIC — حقل واحد
    // ═══════════════════════════════════════════════════════════

    public static <T> void bind(
            TextField triggerField,
            Supplier<List<T>> dataSupplier,
            Function<T, String> displayMapper,
            Consumer<T> onSelect,
            SearchDialog<T> dialogConfig) {

        wire(triggerField, null, true,
                in -> dataSupplier.get(),
                dialogConfig, onSelect,
                new FieldBind[]{new FieldBind<>(triggerField, displayMapper)});
    }

    // ═══════════════════════════════════════════════════════════
    //  3. GENERIC — Multi-Field Bind (Enter + Double Click)
    // ═══════════════════════════════════════════════════════════

    @SafeVarargs
    public static <T> void bind(
            TextField triggerField,
            Supplier<List<T>> dataSupplier,
            SearchDialog<T> dialogConfig,
            Consumer<T> onSelect,
            FieldBind<T>... bindings) {

        wire(triggerField, null, true,
                in -> dataSupplier.get(),
                dialogConfig, onSelect, bindings);
    }

    // ═══════════════════════════════════════════════════════════
    //  4. Multi-Field + زرار (Enter + Button)
    // ═══════════════════════════════════════════════════════════

    @SafeVarargs
    public static <T> void bind(
            TextField triggerField, Button actionButton,
            Supplier<List<T>> dataSupplier,
            SearchDialog<T> dialogConfig,
            Consumer<T> onSelect,
            FieldBind<T>... bindings) {

        wire(triggerField, actionButton, false,
                in -> dataSupplier.get(),
                dialogConfig, onSelect, bindings);
    }

    // ═══════════════════════════════════════════════════════════
    //  5. overload من غير onSelect
    // ═══════════════════════════════════════════════════════════

    @SafeVarargs
    public static <T> void bind(
            TextField triggerField,
            Supplier<List<T>> dataSupplier,
            SearchDialog<T> dialogConfig,
            FieldBind<T>... bindings) {

        wire(triggerField, null, true,
                in -> dataSupplier.get(),
                dialogConfig, null, bindings);
    }

    // ═══════════════════════════════════════════════════════════
    //  6. نسخ تستقبل نص الحقل (للبحث على السيرفر)
    // ═══════════════════════════════════════════════════════════

    @SafeVarargs
    public static <T> void bindWithInput(
            TextField triggerField,
            Function<String, List<T>> dataSupplier,
            SearchDialog<T> dialogConfig,
            Consumer<T> onSelect,
            FieldBind<T>... bindings) {

        wire(triggerField, null, false, dataSupplier, dialogConfig, onSelect, bindings);
    }

    @SafeVarargs
    public static <T> void bindWithInput(
            TextField triggerField, Button actionButton,
            Function<String, List<T>> dataSupplier,
            SearchDialog<T> dialogConfig,
            Consumer<T> onSelect,
            FieldBind<T>... bindings) {

        wire(triggerField, actionButton, false, dataSupplier, dialogConfig, onSelect, bindings);
    }

    // ═══════════════════════════════════════════════════════════
    //  Core
    // ═══════════════════════════════════════════════════════════

    private static <T> void wire(
            TextField triggerField,
            Button actionButton,                      // nullable
            boolean doubleClick,
            Function<String, List<T>> dataSupplier,   // بياخد نص الحقل
            SearchDialog<T> dialogConfig,
            Consumer<T> onSelect,                     // nullable
            FieldBind<T>[] bindings) {

        if (bindings == null || bindings.length == 0) {
            throw new IllegalArgumentException("يجب تمرير FieldBind واحد على الأقل");
        }

        // ── تحديث كل الحقول بالعنصر المختار ──
        Consumer<T> updateAll = obj -> {
            for (FieldBind<T> b : bindings) {
                String val = b.extractor().apply(obj);
                b.field().setText(val != null ? val : "");
            }
            if (onSelect != null) onSelect.accept(obj);
        };

        Node[] busyNodes = actionButton == null
                ? new Node[]{triggerField}
                : new Node[]{triggerField, actionButton};

        AtomicBoolean busy = new AtomicBoolean(false);

        Runnable run = () -> {
            if (!busy.compareAndSet(false, true)) return; // فيه طلب شغال

            final String input = triggerField.getText(); // يتقري على الـ FX thread
            setBusy(busyNodes, true);

            CompletableFuture
                    .supplyAsync(() -> dataSupplier.apply(input), HttpCore.ASYNC_EXECUTOR)
                    .whenComplete((list, ex) -> Platform.runLater(() -> {
                        busy.set(false);
                        setBusy(busyNodes, false);

                        if (ex != null) {
                            notifyError(ex);
                            return;
                        }
                        if (list == null || list.isEmpty()) {
                            SAFNotification.warning("لا توجد بيانات متاحة");
                            return;
                        }

                        // مطابقة فورية: لو عنصر واحد بس بيطابق النص
                        if (input != null && !input.isBlank()) {
                            List<T> matches = list.stream()
                                    .filter(t -> dialogConfig.matches(t, input))
                                    .toList();
                            if (matches.size() == 1) {
                                updateAll.accept(matches.get(0));
                                return;
                            }
                        }

                        dialogConfig.data(list).show().ifPresent(updateAll);
                    }));
        };

        // ── Enter ──
        triggerField.setOnAction(_ -> run.run());

        // ── زرار ──
        if (actionButton != null) {
            actionButton.setOnAction(_ -> run.run());
        }

        // ── دبل كليك ──
        if (doubleClick) {
            triggerField.setOnMouseClicked((MouseEvent event) -> {
                if (event.getClickCount() == 2) run.run();
            });
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  Helpers
    // ═══════════════════════════════════════════════════════════

    private static void setBusy(Node[] nodes, boolean busy) {
        for (Node n : nodes) {
            n.setCursor(busy ? Cursor.WAIT : Cursor.DEFAULT);
            if (n instanceof Button b) b.setDisable(busy);
        }
    }

    private static void notifyError(Throwable ex) {
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }

        if (root instanceof HttpTimeoutException) {
            SAFNotification.error("انتهت مهلة الاتصال بالسيرفر، حاول مرة أخرى");
        } else {
            SAFNotification.error("تعذر الاتصال بالسيرفر: " + root.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  FieldBind — بيربط حقل بقيمة من الـ Object
    // ═══════════════════════════════════════════════════════════

    public record FieldBind<T>(TextField field, Function<T, String> extractor) {
        public static <T> FieldBind<T> of(TextField f, Function<T, String> e) {
            return new FieldBind<>(f, e);
        }
    }
}