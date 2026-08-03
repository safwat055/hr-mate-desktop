package com.safwat.hr.report.payroll.ui;

import com.safwat.hr.report.controller.PayrollReportController;
import com.safwat.hr.report.payroll.DataSourceResolver;
import com.safwat.hr.report.payroll.strategies.ReportStrategy;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

import java.util.Map;
import java.util.function.Supplier;

/**
 * مدير واجهة نموذج التقارير.
 *
 * <p>يفصل منطق إظهار/إخفاء الحقول وتفعيل أزرار البحث
 * عن {@link PayrollReportController}، ويُطبِّق إعدادات
 * {@link UiConfiguration} على مكونات الواجهة الفعلية.
 *
 * <p>يعمل كـ <b>Mediator</b> بين الاستراتيجية (التي تُحدِّد ماذا تُظهر وكيف تتصرف)
 * والـ Controller (الذي يملك المكونات الفعلية في الـ FXML).
 *
 * <hr>
 *
 * <h2>تسلسل apply()</h2>
 * <ol>
 *   <li>{@link #hideAll()} — يخفي كل الحقول ويمسح جميع الـ handlers</li>
 *   <li>إظهار الحقول المحددة في {@code visibleFields}</li>
 *   <li>تفعيل حقول البحث من {@code searchFields}</li>
 *   <li>تحديث العنوان</li>
 *   <li>{@link ReportStrategy#onApply(PayrollReportController)} — تخصيص كامل للاستراتيجية</li>
 * </ol>
 *
 * <hr>
 *
 * <h2>إضافة حقل بحث جديد</h2>
 * <ol>
 *   <li>أضف القيمة في {@link UiField}</li>
 *   <li>أضف HBox وزر في الـ FXML وأضف getters في الـ Controller</li>
 *   <li>أضف سطرًا في {@code fieldToComponent} و{@code fieldToSearchBinding} هنا</li>
 *   <li>في الاستراتيجية: {@code .searchField(SearchFieldConfig.of(UiField.X, "عنوان", "source"))}</li>
 * </ol>
 */
public class PayrollUIManager {

    private final PayrollReportController controller;

    /**
     * خريطة تربط كل {@link UiField} بالـ HBox Container المقابل له.
     * تُمكِّن إظهار/إخفاء أي حقل بكود موحَّد دون if/else.
     */
    private final Map<UiField, Supplier<HBox>> fieldToComponent;

    /**
     * خريطة تربط كل {@link UiField} القابل للبحث بـ {@link SearchBinding} الخاص به.
     * تُمكِّن تفعيل/تعطيل البحث لأي حقل بكود موحَّد.
     */
    private final Map<UiField, SearchBinding> fieldToSearchBinding;

    /**
     * قائمة بجميع الـ TextFields التي قد يُضاف عليها handlers من الاستراتيجيات.
     * تُستخدَم في {@link #clearAllHandlers()} لضمان تنظيف كامل عند التبديل.
     */
    private final Map<UiField, Supplier<TextField>> fieldToTextField;

    /**
     * @param controller الـ Controller المالك للمكونات الفعلية
     */
    public PayrollUIManager(PayrollReportController controller) {
        this.controller = controller;

        this.fieldToComponent = Map.of(
                UiField.H_START_DATE, controller::getH_startDate,
                UiField.H_END_DATE, controller::getH_endDate,
                UiField.H_MANAGEMENT, controller::getH_management,
                UiField.H_PAY_GROUP, controller::getH_payGroup,
                UiField.H_EMPLOYEE, controller::getH_employee,
                UiField.H_SEARCH, controller::getH_Search,
                UiField.H_FILES, controller::getH_files

        );

        this.fieldToSearchBinding = Map.of(
                UiField.H_MANAGEMENT, new SearchBinding(
                        controller::getBtn_managementSearch,
                        controller::getTxt_management
                ),
                UiField.H_PAY_GROUP, new SearchBinding(
                        controller::getBtn_PayGroupSearch,
                        controller::getTxt_payGroup
                ),
                UiField.H_SEARCH, new SearchBinding(
                        controller::getBtn_Search,
                        controller::getTxt_search
                )

                // لإضافة حقل بحث جديد: أضف سطرًا هنا فقط
        );
        this.fieldToTextField = Map.of();
        // جميع الـ TextFields التي قد تحمل handlers من الاستراتيجيات
        /*this.fieldToTextField = Map.of(
                UiField.START_DATE, controller::getTxt_startDate,
                UiField.END_DATE, controller::getTxt_endDate,
                UiField.MANAGEMENT, controller::getTxt_management,
                UiField.PAY_GROUP, controller::getTxt_payGroup,
                UiField.SEARCH_VALUE, controller::getTxt_search
        );*/
    }

    // ─────────────────────────────────────────────
    //  Public API
    // ─────────────────────────────────────────────

    /**
     * يخفي جميع حقول النموذج ويمسح جميع الـ handlers.
     *
     * <p>يُستدعى قبل تطبيق أي إعداد جديد لضمان بدء نظيف تماماً —
     * لا handlers قديمة، لا نصوص Labels متبقية من تقرير سابق.
     *
     * <p>يتجاهل {@code H_report} لأنه container الـ ComboBox الفرعي
     * ويُدار بشكل مستقل في الـ Controller.
     */
    public void hideAll() {
        // إخفاء جميع الـ HBoxes
        for (Node node : controller.getMainCont().getChildren()) {
            if (node instanceof HBox hBox) {
                if (hBox == controller.getH_report()) continue;
                hBox.setManaged(false);
                hBox.setVisible(false);
            }
        }
        // مسح handlers أزرار البحث
        fieldToSearchBinding.values().forEach(SearchBinding::hide);

        // مسح جميع الـ handlers على الـ TextFields
        // عشان لو استراتيجية سابقة أضافت مستمع، يتمسح قبل الاستراتيجية الجديدة
        clearAllHandlers();
    }

    /**
     * يُطبِّق إعدادات الاستراتيجية على الواجهة.
     *
     * <p><b>التسلسل:</b>
     * <ol>
     *   <li>يخفي كل الحقول والأزرار ويمسح الـ handlers</li>
     *   <li>يُظهر الحقول المحددة في {@code visibleFields}</li>
     *   <li>يُفعِّل البحث لكل عنصر في {@code searchFields}</li>
     *   <li>يُحدِّث عنوان النموذج</li>
     *   <li>يستدعي {@link ReportStrategy#onApply} للتخصيص الكامل</li>
     * </ol>
     *
     * @param config   إعدادات الواجهة القادمة من الاستراتيجية
     * @param strategy الاستراتيجية الحالية — تحصل على فرصة التخصيص الكامل في النهاية
     */
    public void apply(UiConfiguration config, ReportStrategy strategy) {
        hideAll();
        if (config == null) {
            return;
        }
        if (config.getVisibleFields() != null) {
            config.getVisibleFields().forEach(this::showField);
        }

        if (config.getSearchFields() != null) {
            config.getSearchFields().forEach(this::activateSearch);
        }

        if (config.getTitle() != null && !config.getTitle().isBlank()) {
            controller.getLbl_name().setText(config.getTitle());
        }

        // التخصيص الكامل — الاستراتيجية تتحكم في أي شيء تريده
        strategy.onApply(controller);
    }

    // ─────────────────────────────────────────────
    //  Private Helpers
    // ─────────────────────────────────────────────

    private void showField(UiField field) {
        Supplier<HBox> supplier = fieldToComponent.get(field);
        if (supplier == null) return;
        HBox box = supplier.get();
        box.setManaged(true);
        box.setVisible(true);
    }

    private void activateSearch(SearchFieldConfig cfg) {
        SearchBinding binding = fieldToSearchBinding.get(cfg.getField());
        if (binding == null) return;
        binding.activate(cfg, controller);
    }

    /**
     * يمسح جميع الـ handlers على الـ TextFields وأزرار البحث.
     *
     * <p>يضمن عدم تراكم مستمعين من استراتيجيات سابقة عند التبديل بين التقارير.
     * يُستدعى في بداية كل {@link #hideAll()}.
     */
    private void clearAllHandlers() {
        // مسح handlers الـ TextFields
        fieldToTextField.values().forEach(supplier -> {
            TextField tf = supplier.get();
            tf.setOnAction(null);
            // مسح listeners الـ textProperty المضافة من الاستراتيجيات
            // بالاستبدال بـ listener فارغ ثم إزالته — الطريقة الأضمن في JavaFX
            tf.getProperties().remove("strategyListener");
        });

        // مسح handlers الأزرار الإضافية (غير أزرار البحث المُدارة في SearchBinding)
        controller.getBtn_searchMonth().setOnAction(null);
        controller.getBtn_searchMonthEnd().setOnAction(null);
        controller.getBtn_SearchEmployee().setOnAction(null);
    }

    // ─────────────────────────────────────────────
    //  Inner Class — SearchBinding
    // ─────────────────────────────────────────────

    /**
     * يُمثِّل الرابط بين {@link UiField} وعناصر الواجهة المقابلة له (الزر + TextField).
     *
     * <p>كلاس داخلي مساعد يُغلِّف منطق إظهار/إخفاء وربط الـ handlers
     * لكل حقل بحث، بحيث يبقى {@link PayrollUIManager} نظيفًا وبدون تكرار.
     */
    static class SearchBinding {

        private final Supplier<Button> buttonSupplier;
        private final Supplier<TextField> fieldSupplier;

        SearchBinding(Supplier<Button> buttonSupplier, Supplier<TextField> fieldSupplier) {
            this.buttonSupplier = buttonSupplier;
            this.fieldSupplier = fieldSupplier;
        }

        /**
         * يُخفي الزر ويمسح الـ handlers تمهيدًا لإعداد جديد
         */
        void hide() {
            Button btn = buttonSupplier.get();
            btn.setManaged(false);
            btn.setVisible(false);
            btn.setOnAction(null);
            fieldSupplier.get().setOnAction(null);
        }

        /**
         * يُظهر الزر ويربط الـ handlers بمصدر البيانات المحدد.
         *
         * <p>فتح نافذة البحث يحدث عند:
         * <ul>
         *   <li>الضغط على الزر</li>
         *   <li>الضغط على Enter داخل الـ TextField</li>
         * </ul>
         *
         * @param cfg        إعداد البحث (العنوان + مفتاح المصدر)
         * @param controller الـ Controller لفتح نافذة البحث
         */
        void activate(SearchFieldConfig cfg, PayrollReportController controller) {
            Button btn = buttonSupplier.get();
            TextField textField = fieldSupplier.get();

            btn.setManaged(true);
            btn.setVisible(true);

            EventHandler<ActionEvent> handler = e ->
                    controller.openSearchDialog(
                            cfg.getDialogTitle(),
                            DataSourceResolver.get(cfg.getDataSource()),
                            textField
                    );

            btn.setOnAction(handler);
            textField.setOnAction(handler);
        }
    }
}