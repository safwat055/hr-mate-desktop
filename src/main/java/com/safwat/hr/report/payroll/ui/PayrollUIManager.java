package com.safwat.hr.report.payroll.ui;

import com.safwat.hr.controller.report.payroll.PayrollReportController;
import javafx.scene.Node;
import javafx.scene.layout.HBox;

import java.util.Map;
import java.util.function.Supplier;

/**
 * مدير واجهة نموذج التقارير.
 *
 * <p>يفصل منطق إظهار وإخفاء حقول النموذج عن {@link PayrollReportController}،
 * ويُطبِّق إعدادات {@link UiConfiguration} على مكونات الواجهة الفعلية.
 *
 * <p>يعمل كـ Mediator بين الاستراتيجية (التي تُحدِّد ماذا تُظهر) والـ Controller
 * (الذي يملك المكونات الفعلية).
 *
 * <p><b>إضافة حقل جديد للواجهة:</b>
 * <ol>
 *   <li>أضف قيمة في {@link UiField}</li>
 *   <li>أضف HBox مقابلًا في الـ FXML وأضف getter له في الـ Controller</li>
 *   <li>أضف السطر المقابل في {@code fieldToComponent} هنا</li>
 * </ol>
 */
public class PayrollUIManager {

    private final PayrollReportController controller;

    /**
     * خريطة تربط كل {@link UiField} بالـ HBox المقابل في الـ Controller.
     * تُمكِّن إظهار وإخفاء أي حقل بكود موحَّد دون if/else متكررة.
     */
    private final Map<UiField, Supplier<HBox>> fieldToComponent;

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
    }

    /**
     * يخفي جميع حقول النموذج دفعةً واحدة.
     *
     * <p>يُستدعى قبل تطبيق أي إعداد جديد لضمان بدء نظيف.
     * يتجاهل عمدًا {@code H_report} لأنه container الـ ComboBox الفرعي
     * ويُدار بشكل مستقل في الـ Controller.
     */
    public void hideAll() {
        for (Node node : controller.getMainCont().getChildren()) {
            if (node instanceof HBox hBox) {
                if (hBox == controller.getH_report()) continue; // ← الفرعي يُدار منفصلاً
                hBox.setManaged(false);
                hBox.setVisible(false);
            }
        }
    }

    /**
     * يُطبِّق إعدادات الاستراتيجية على الواجهة.
     *
     * <p>يخفي كل الحقول أولاً ثم يُظهر فقط ما تحدده {@code config.visibleFields}.
     * يُحدِّث أيضًا عنوان النموذج إذا كان محددًا.
     *
     * @param config إعدادات الواجهة القادمة من الاستراتيجية المختارة
     */
    public void apply(UiConfiguration config) {
        hideAll();

        if (config.getVisibleFields() != null) {
            config.getVisibleFields().forEach(this::showField);
        }

        if (config.getTitle() != null && !config.getTitle().isBlank()) {
            controller.getLbl_name().setText(config.getTitle());
        }
    }

    /**
     * يُظهر الحقل المحدد في النموذج.
     *
     * @param field الحقل المطلوب إظهاره
     */
    private void showField(UiField field) {
        Supplier<HBox> supplier = fieldToComponent.get(field);
        if (supplier != null) {
            HBox box = supplier.get();
            box.setManaged(true);
            box.setVisible(true);
        }
    }
}