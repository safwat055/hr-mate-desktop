package com.safwat.hr.report.payroll.ui;

import com.safwat.hr.controller.report.payroll.PayrollReportController;
import com.safwat.hr.report.payroll.DataSourceResolver;
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
 * <p>يعمل كـ <b>Mediator</b> بين الاستراتيجية (التي تُحدِّد ماذا تُظهر)
 * والـ Controller (الذي يملك المكونات الفعلية في الـ FXML).
 *
 * <hr>
 *
 * <h2>آلية تفعيل البحث</h2>
 * <p>يمشي على {@link UiConfiguration#getSearchFields()} —
 * قائمة من {@link SearchFieldConfig} — ولكل عنصر يربط:
 * <ul>
 *   <li>الزر المناسب ({@code btn_managementSearch}, {@code btn_PayGroupSearch}, ...)</li>
 *   <li>الـ TextField المناسب بـ {@code setOnAction}</li>
 * </ul>
 * الحقول التي ليس لها إعداد بحث يُخفى زرها تلقائيًا.
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
     * @param controller الـ Controller المالك للمكونات الفعلية
     */
    public PayrollUIManager(PayrollReportController controller) {
        this.controller = controller;

        this.fieldToComponent = Map.of(
                UiField.START_DATE, controller::getH_startDate,
                UiField.END_DATE, controller::getH_endDate,
                UiField.MANAGEMENT, controller::getH_management,
                UiField.PAY_GROUP, controller::getH_payGroup,
                UiField.NATIONAL_ID, controller::getH_employee,
                UiField.CUSTOM_GROUP, controller::getH_element,
                UiField.DESCRIPTION, controller::getH_1,
                UiField.NOTE, controller::getH_2,
                UiField.SEARCH_VALUE, controller::getH_3
        );

        this.fieldToSearchBinding = Map.of(
                UiField.MANAGEMENT, new SearchBinding(
                        controller::getBtn_managementSearch,
                        controller::getTxt_management
                ),
                UiField.PAY_GROUP, new SearchBinding(
                        controller::getBtn_PayGroupSearch,
                        controller::getTxt_payGroup
                )
                // لإضافة حقل بحث جديد: أضف سطرًا هنا فقط
        );
    }

    /**
     * يخفي جميع حقول النموذج دفعةً واحدة ويُعطِّل جميع أزرار البحث.
     *
     * <p>يُستدعى قبل تطبيق أي إعداد جديد لضمان بدء نظيف.
     * يتجاهل {@code H_report} لأنه container الـ ComboBox الفرعي
     * ويُدار بشكل مستقل في الـ Controller.
     */
    public void hideAll() {
        for (Node node : controller.getMainCont().getChildren()) {
            if (node instanceof HBox hBox) {
                if (hBox == controller.getH_report()) continue;
                hBox.setManaged(false);
                hBox.setVisible(false);
            }
        }
        // إخفاء جميع أزرار البحث وتنظيف الـ handlers
        fieldToSearchBinding.values().forEach(SearchBinding::hide);
    }

    /**
     * يُطبِّق إعدادات الاستراتيجية على الواجهة.
     *
     * <ol>
     *   <li>يخفي كل الحقول والأزرار</li>
     *   <li>يُظهر الحقول المحددة في {@code visibleFields}</li>
     *   <li>يُفعِّل البحث لكل عنصر في {@code searchFields}</li>
     *   <li>يُحدِّث عنوان النموذج</li>
     * </ol>
     *
     * @param config إعدادات الواجهة القادمة من الاستراتيجية المختارة
     */
    public void apply(UiConfiguration config) {
        hideAll();

        if (config.getVisibleFields() != null) {
            config.getVisibleFields().forEach(this::showField);
        }

        // كل SearchFieldConfig يُفعِّل الزر والـ TextField المقابلَين بشكل مستقل
        if (config.getSearchFields() != null) {
            config.getSearchFields().forEach(this::activateSearch);
        }

        if (config.getTitle() != null && !config.getTitle().isBlank()) {
            controller.getLbl_name().setText(config.getTitle());
        }
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