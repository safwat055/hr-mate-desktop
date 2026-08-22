package com.safwat.hr.report.core.ui;

import com.safwat.hr.report.controller.PayrollReportController;
import com.safwat.hr.report.core.DataSourceResolver;
import com.safwat.hr.report.core.strategies.ReportStrategy;
import com.safwat.hr.shared.ui.MultiSelectSearchDialog;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class PayrollUIManager {

    private final PayrollReportController controller;

    private final Map<UiField, Supplier<HBox>> fieldToComponent;
    private final Map<UiField, SearchBinding> fieldToSearchBinding;

    // ═══════════════════════════════════════════════════════
    //  جديد — خريطة TextFields عشان نقرأ قيم الحقول
    // ═══════════════════════════════════════════════════════
    private final Map<UiField, Supplier<TextField>> fieldToTextField;

    public PayrollUIManager(PayrollReportController controller) {
        this.controller = controller;

        this.fieldToComponent = Map.of(

                UiField.H_REPORT_TYPE, controller::getH_ReportType,
                UiField.H_START_DATE, controller::getH_startDate,
                UiField.H_END_DATE, controller::getH_endDate,
                UiField.H_MANAGEMENT, controller::getH_management,
                UiField.H_PAY_GROUP, controller::getH_payGroup,
                UiField.H_EMPLOYEE, controller::getH_employee,
                UiField.H_SEARCH, controller::getH_Search,
                UiField.H_FILES, controller::getH_files

        );

        this.fieldToSearchBinding = Map.of(
                UiField.H_MANAGEMENT, new SearchBinding(controller::getBtn_managementSearch, controller::getTxt_management),
                UiField.H_PAY_GROUP, new SearchBinding(controller::getBtn_PayGroupSearch, controller::getTxt_payGroup),
                UiField.H_SEARCH, new SearchBinding(controller::getBtn_Search, controller::getTxt_search)
        );

        // ═══════════════════════════════════════════════════════
        //  جديد — ربط الحقول بـ TextFields عشان نقرأ قيمهم
        // ═══════════════════════════════════════════════════════
        this.fieldToTextField = Map.of(
                UiField.H_START_DATE, controller::getTxt_startDate,
                UiField.H_END_DATE, controller::getTxt_endDate,
                UiField.H_MANAGEMENT, controller::getTxt_management,
                UiField.H_PAY_GROUP, controller::getTxt_payGroup,
                UiField.H_SEARCH, controller::getTxt_search,
                UiField.TXT_START_DATE, controller::getTxt_startDate
        );
    }

    // ─── Public API ───

    public void hideAll() {
        for (Node node : controller.getMainCont().getChildren()) {
            if (node instanceof HBox hBox) {
                if (hBox == controller.getH_report()) continue;
                hBox.setManaged(false);
                hBox.setVisible(false);
            }
        }
        fieldToSearchBinding.values().forEach(SearchBinding::hide);
        clearAllHandlers();
    }

    public void apply(UiConfiguration config, ReportStrategy strategy) {
        hideAll();
        if (config == null) return;

        if (config.getVisibleFields() != null) {
            config.getVisibleFields().forEach(this::showField);
        }

        if (config.getSearchFields() != null) {
            // ═══════════════════════════════════════════════════════
            //  جديد — فرقنا بين Single Select و Multi Select
            // ═══════════════════════════════════════════════════════
            config.getSearchFields().forEach(cfg -> {
                if (cfg.isMultiSelect()) {
                    activateMultiSelectSearch(cfg);
                } else {
                    activateSingleSelectSearch(cfg);
                }
            });
        }

        if (config.getTitle() != null && !config.getTitle().isBlank()) {
            controller.getLbl_name().setText(config.getTitle());
        }

        strategy.onApply(controller);
    }

    // ─── Private Helpers ───

    private void showField(UiField field) {
        Supplier<HBox> supplier = fieldToComponent.get(field);
        if (supplier == null) return;
        HBox box = supplier.get();
        box.setManaged(true);
        box.setVisible(true);
    }

    /**
     * الطريقة القديمة — Single Select
     */
    private void activateSingleSelectSearch(SearchFieldConfig cfg) {
        SearchBinding binding = fieldToSearchBinding.get(cfg.getField());
        if (binding == null) return;
        binding.activate(cfg, controller);
    }

    /**
     * جديد — تفعيل البحث المتعدد (MultiSelect) مع دعم الـ Dependent Field
     */
    private void activateMultiSelectSearch(SearchFieldConfig cfg) {
        SearchBinding binding = fieldToSearchBinding.get(cfg.getField());
        if (binding == null) return;

        Button btn = binding.buttonSupplier.get();
        TextField textField = binding.fieldSupplier.get();

        btn.setManaged(true);
        btn.setVisible(true);

        EventHandler<ActionEvent> handler = e -> {
            // 1️⃣ قراءة قيمة الحقل المعتمد
            String dependentValue = null;
            if (cfg.getDependentField() != null) {
                dependentValue = getFieldValue(cfg.getDependentField());

                // ═══════════════════════════════════════════════════════
                //  جديد — تطبيق المحول لو موجود
                // ═══════════════════════════════════════════════════════
                if (dependentValue != null && cfg.getDependentValueConverter() != null) {
                    dependentValue = cfg.getDependentValueConverter().apply(dependentValue);
                }
            }

            // 2️⃣ إعداد الـ params
            String[] params = (dependentValue != null && !dependentValue.isBlank())
                    ? new String[]{dependentValue}
                    : new String[0];

            // 3️⃣ جلب الداتا
            List<String> data = DataSourceResolver.get(cfg.getDataSource(), params);

            // 4️⃣ فتح MultiSelectSearchDialog
            List<String> selected = MultiSelectSearchDialog.forStrings()
                    .title(cfg.getDialogTitle())
                    .data(data)
                    .searchPlaceholder("ابحث...")
                    .owner(getStage())
                    .showAndWait();
            controller.getSelectedGroups().addAll(selected);
            // 5️⃣ كتابة النتيجة
            if (selected != null && !selected.isEmpty()) {
                textField.setText(String.join(":", selected));
            }
        };

        btn.setOnAction(handler);
    }

    /**
     * قراءة قيمة أي حقل في الواجهة
     */
    private String getFieldValue(UiField field) {
        Supplier<TextField> supplier = fieldToTextField.get(field);
        if (supplier == null) return null;
        TextField tf = supplier.get();
        return tf != null ? tf.getText() : null;
    }

    /**
     * جلب الـ Stage الحالي
     */
    private Stage getStage() {
        return (Stage) controller.getLbl_name().getScene().getWindow();
    }

    private void clearAllHandlers() {
        fieldToTextField.values().forEach(supplier -> {
            TextField tf = supplier.get();
            if (tf != null) {
                tf.setOnAction(null);
                tf.getProperties().remove("strategyListener");
            }
        });
        controller.getBtn_searchMonth().setOnAction(null);
        controller.getBtn_searchMonthEnd().setOnAction(null);
        controller.getBtn_SearchEmployee().setOnAction(null);
    }

    // ─── Inner Class: SearchBinding ───

    static class SearchBinding {
        private final Supplier<Button> buttonSupplier;
        private final Supplier<TextField> fieldSupplier;

        SearchBinding(Supplier<Button> buttonSupplier, Supplier<TextField> fieldSupplier) {
            this.buttonSupplier = buttonSupplier;
            this.fieldSupplier = fieldSupplier;
        }

        void hide() {
            Button btn = buttonSupplier.get();
            btn.setManaged(false);
            btn.setVisible(false);
            btn.setOnAction(null);
            fieldSupplier.get().setOnAction(null);
        }

        /**
         * تفعيل البحث العادي (Single Select)
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