package com.safwat.hr.shared;

import com.safwat.hr.shared.ui.SearchDialog;
import com.safwat.hr.shared.util.DateUtils;
import com.safwat.hr.ui.controls.SAFNotification;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * ────────────────────────────────────────────────────────────
 * SmartSearchHelper
 * ────────────────────────────────────────────────────────────
 * أداة بحث ذكية عامة — تُستخدم في أي Controller دون تبعية.
 * <p>
 * تدعم ثلاثة أنماط:
 * 1. String فقط  (backward compatible)
 * 2. Generic Object + حقل واحد
 * 3. Generic Object + تحديث متعدد الحقول (Multi-Field Bind)
 * <p>
 * المشغلات: Enter  |  Double-Click على الحقل الفارغ/المملوء
 * ────────────────────────────────────────────────────────────
 */
public final class SmartSearchHelper {

    private SmartSearchHelper() {
    } // utility class

    // ═══════════════════════════════════════════════════════════
    //  1. STRING-ONLY  (النسخة القديمة — backward compatible)
    // ═══════════════════════════════════════════════════════════

    /**
     * ربط TextField بقائمة نصية.
     *
     * @param triggerField الحقل اللي يفتح البحث
     * @param dataSupplier Supplier بيرجع List<String> في اللحظة
     * @param onSelect     callback لما يتم الاختيار (يمكن null)
     */
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

    /**
     * ربط TextField بـ Object واحد — يتحدث حقل واحد فقط.
     *
     * @param triggerField  الحقل اللي يفتح البحث
     * @param dataSupplier  Supplier بيرجع List<T>
     * @param displayMapper T → String (النص اللي يتعرض)
     * @param onSelect      callback لما يتم الاختيار
     * @param dialogConfig  SearchDialog<T> جاهز (من builder/forStrings/...)
     */
    public static <T> void bind(
            TextField triggerField,
            Supplier<List<T>> dataSupplier,
            Function<T, String> displayMapper,
            Consumer<T> onSelect,
            SearchDialog<T> dialogConfig) {

        bind(triggerField, dataSupplier, dialogConfig, onSelect,
                new FieldBind<>(triggerField, displayMapper));
    }

    // ═══════════════════════════════════════════════════════════
    //  3. GENERIC — Multi-Field Bind  (الجديد)
    // ═══════════════════════════════════════════════════════════

    /**
     * ربط TextField بـ Object — يتحدث أكتر من حقل دفعة واحدة.
     *
     * @param triggerField الحقل اللي يفتح البحث (Enter / Double Click)
     * @param dataSupplier Supplier بيرجع List<T>
     * @param dialogConfig SearchDialog<T> جاهز (من builder/forStrings/...)
     * @param onSelect     callback إضافي بعد الاختيار (يمكن null)
     * @param bindings     FieldBind[] — كل حقل + قيمته من الـ Object
     */
    @SafeVarargs
    public static <T> void bind(
            TextField triggerField,
            Supplier<List<T>> dataSupplier,
            SearchDialog<T> dialogConfig,
            Consumer<T> onSelect,
            FieldBind<T>... bindings) {

        if (bindings.length == 0) {
            throw new IllegalArgumentException("يجب تمرير FieldBind واحد على الأقل");
        }

        // ── دالة تحديث كل الحقول ──
        Consumer<T> updateAll = obj -> {
            for (FieldBind<T> b : bindings) {
                String val = b.extractor().apply(obj);
                b.field().setText(val != null ? val : "");
            }
            if (onSelect != null) onSelect.accept(obj);
        };

        // ── نص البحث = مجموع كل الحقول ──
        Function<T, String> combinedSearch = obj -> {
            StringBuilder sb = new StringBuilder();
            for (FieldBind<T> b : bindings) {
                String v = b.extractor().apply(obj);
                if (v != null) sb.append(v).append(' ');
            }
            return sb.toString().trim();
        };

        // ── فتح الـ Dialog ──
        Runnable openSearch = () -> {
            List<T> dataList = dataSupplier.get();
            if (dataList == null || dataList.isEmpty()) {
                SAFNotification.warning("لا توجد بيانات متاحة");
                return;
            }
            dialogConfig.data(dataList).show().ifPresent(updateAll);
        };

        // ── Enter ──
        triggerField.setOnAction(_ -> {
            if (isBlank(triggerField)) {
                openSearch.run();
                return;
            }
            handleInput(triggerField, dataSupplier, combinedSearch, updateAll, openSearch);
        });

        // ── Double Click ──
        triggerField.setOnMouseClicked((MouseEvent event) -> {
            if (event.getClickCount() == 2) {
                if (isBlank(triggerField)) {
                    openSearch.run();
                    return;
                }
                handleInput(triggerField, dataSupplier, combinedSearch, updateAll, openSearch);
            }
        });
    }

    /**
     * overload من غير onSelect
     */
    @SafeVarargs
    public static <T> void bind(
            TextField triggerField,
            Supplier<List<T>> dataSupplier,
            SearchDialog<T> dialogConfig,
            FieldBind<T>... bindings) {
        bind(triggerField, dataSupplier, dialogConfig, null, bindings);
    }

    // ═══════════════════════════════════════════════════════════
    //  Helpers
    // ═══════════════════════════════════════════════════════════

    private static <T> void handleInput(
            TextField triggerField,
            Supplier<List<T>> dataSupplier,
            Function<T, String> searchTextExtractor,
            Consumer<T> onSingleMatch,
            Runnable openSearch) {

        List<T> dataList = dataSupplier.get();
        if (dataList == null || dataList.isEmpty()) {
            openSearch.run();
            return;
        }

        String input = normalize(triggerField.getText());
        List<T> matches = dataList.stream()
                .filter(t -> normalize(searchTextExtractor.apply(t)).contains(input))
                .toList();

        if (matches.size() == 1) {
            onSingleMatch.accept(matches.get(0));
        } else {
            openSearch.run();
        }
    }

    private static boolean isBlank(TextField tf) {
        String t = tf.getText();
        return t == null || t.isBlank();
    }

    private static String normalize(String text) {
        if (text == null) return "";
        return DateUtils.normalizeArabicText(text);
    }

    // ═══════════════════════════════════════════════════════════
    //  FieldBind — Record بيربط حقل بقيمة من Object
    // ═══════════════════════════════════════════════════════════

    /**
     * ربط بين TextField وقيمة من Object من نوع T.
     *
     * @param field     الـ TextField اللي هيتحدث
     * @param extractor Function<T, String> بتستخرج القيمة
     */
    public record FieldBind<T>(TextField field, Function<T, String> extractor) {
        public static <T> FieldBind<T> of(TextField f, Function<T, String> e) {
            return new FieldBind<>(f, e);
        }
    }
}