package com.safwat.hr.controller.report.payroll;

import com.safwat.hr.report.payroll.DataSourceResolver;
import com.safwat.hr.report.payroll.ReportContext;
import com.safwat.hr.report.payroll.ReportSubmissionService;
import com.safwat.hr.report.payroll.ValidationException;
import com.safwat.hr.report.payroll.strategies.ReportRegistryFactory;
import com.safwat.hr.report.payroll.strategies.ReportStrategy;
import com.safwat.hr.report.payroll.strategies.ReportStrategyRegistry;
import com.safwat.hr.report.payroll.ui.PayrollUIManager;
import com.safwat.hr.report.payroll.ui.UiConfiguration;
import com.safwat.hr.service.payroll.dto.PayrollRequest;
import com.safwat.hr.shared.util.DateUtils;
import com.safwat.hr.ui.controls.SAFNotification;
import com.safwat.hr.ui.util.SearchDialog;
import com.safwat.hr.utils.ApiClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.Getter;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

@Getter
public class PayrollReportController implements Initializable {

    private final ReportStrategyRegistry registry;
    private final PayrollUIManager uiManager;
    private final ReportSubmissionService submissionService;

    private ReportStrategy currentStrategy; // ممكن يكون رئيسي أو فرعي

    @FXML
    private HBox H_1, H_2, H_3, H_4, H_5, H_6;
    @FXML
    private HBox H_element, H_employee, H_endDate, H_management, H_payGroup, H_report, H_startDate;
    @FXML
    private ComboBox<String> combo_Format, combo_report, combo_reportName;
    @FXML
    private Label lbl_elementName, lbl_endDate, lbl_name, lbl_nationalId, lbl_payId, lbl_statDate;
    @FXML
    private TextField txt_startDate, txt_endDate, txt_element, txt_search, txt_payGroup, txt_management;
    @FXML
    private VBox mainCont;
    @FXML
    private Button btn_PayGroupSearch, btn_managementSearch, btn_searchMonth, btnDoReport;

    public PayrollReportController() {
        this.registry = ReportRegistryFactory.create();
        this.uiManager = new PayrollUIManager(this);
        this.submissionService = new ReportSubmissionService();
    }

    public static void closeStage(Node targetComponent) {
        ((Stage) targetComponent.getScene().getWindow()).close();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        fillFormatCombo();
        fillMainCombo();        // ← ربطناها بالـ Registry
        setupListeners();
        uiManager.hideAll();
    }

    /**
     * التقارير الرئيسية من الـ Registry
     */
    void fillMainCombo() {
        // category = "main_container" أو "main_direct"
        List<String> mainReports = registry.getAll().stream()
                .filter(s -> s.getCategory().startsWith("main_"))
                .map(ReportStrategy::getDisplayName)
                .toList();
        combo_reportName.getItems().addAll(mainReports);
    }

    void fillFormatCombo() {
        combo_Format.getItems().addAll("PDF", "EXCEL");
        combo_Format.getSelectionModel().select(0);
    }

    void setupListeners() {
        // ====== لما يختار التقرير الرئيسي ======
        combo_reportName.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal == null) return;

            ReportStrategy mainStrategy = registry.getByDisplayName(newVal);

            if (mainStrategy.hasSubReports()) {
                // === حاوي: اخفي كل حاجة الأول، وبعدين اظهر الفرعي ===
                uiManager.hideAll();           // ← اخفي كل حاجة الأول
                combo_report.getItems().clear();
                combo_report.getItems().addAll(registry.getDisplayNamesByCategory(mainStrategy.getMainReport()));
                H_report.setManaged(true);     // ← اظهر الفرعي بعد الـ hideAll
                H_report.setVisible(true);
                currentStrategy = null;        // لسه ماختارش فرعي

            } else {
                // === مباشر: اخفي الفرعي واظهر الحقول ===
                H_report.setManaged(false);
                H_report.setVisible(false);
                combo_report.getItems().clear();
                combo_report.getSelectionModel().clearSelection();

                currentStrategy = mainStrategy;
                uiManager.apply(currentStrategy.getUiConfig());
                configureSearchForCurrentStrategy();
            }
        });
        // ====== لما يختار التقرير الفرعي (لو ظاهر) ======
        combo_report.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal == null) return;
            currentStrategy = registry.getByDisplayName(newVal);
            uiManager.apply(currentStrategy.getUiConfig());
            configureSearchForCurrentStrategy();
        });
    }

    void configureSearchForCurrentStrategy() {
        if (currentStrategy == null) return;
        UiConfiguration config = currentStrategy.getUiConfig();

        // زر اختيار الشهر
        btn_searchMonth.setOnAction(e -> {
            Optional<String> month = SearchDialog.forStrings()
                    .title("اختر شهر")
                    .data(DataSourceResolver.get("monthsYearly"))
                    .show();
            month.ifPresent(m -> {
                txt_startDate.setText(m);
                lbl_statDate.setText(DateUtils.toArabicMonthYear(DateUtils.getFirstDayOfMonth(m)));
            });
        });

        // إدارة
        if (config.isNeedsSearchDialog() && "management".equals(config.getSearchDataSource())) {
            btn_managementSearch.setManaged(true);
            btn_managementSearch.setVisible(true);
            btn_managementSearch.setOnAction(e -> openSearchDialog(config.getSearchDialogTitle(), DataSourceResolver.get("management"), txt_management));
            txt_management.setOnAction(e -> openSearchDialog(config.getSearchDialogTitle(), DataSourceResolver.get("management"), txt_management));
        } else {
            btn_managementSearch.setManaged(false);
            btn_managementSearch.setVisible(false);
            txt_management.setOnAction(null);
        }

        // مجموعة تعيين
        if (config.isNeedsSearchDialog() && "payGroup".equals(config.getSearchDataSource())) {
            btn_PayGroupSearch.setManaged(true);
            btn_PayGroupSearch.setVisible(true);
            btn_PayGroupSearch.setOnAction(e -> openSearchDialog(config.getSearchDialogTitle(), DataSourceResolver.get("payGroup"), txt_payGroup));
            txt_payGroup.setOnAction(e -> openSearchDialog(config.getSearchDialogTitle(), DataSourceResolver.get("payGroup"), txt_payGroup));
        } else {
            btn_PayGroupSearch.setManaged(false);
            btn_PayGroupSearch.setVisible(false);
            txt_payGroup.setOnAction(null);
        }
    }

    void openSearchDialog(String title, List<String> data, TextField targetField) {
        List<String> filtered = data.stream()
                .filter(s -> s.contains(targetField.getText()))
                .toList();
        List<String> dialogData = filtered.isEmpty() ? data : filtered;

        SearchDialog.forStrings()
                .title(title)
                .data(dialogData)
                .show()
                .ifPresent(targetField::setText);
    }

    @FXML
    void doReport() {
        if (currentStrategy == null) {
            SAFNotification.warning("اختر تقرير أولاً");
            return;
        }

        try {
            ReportContext context = ReportContext.builder()
                    .user(ApiClient.getUserName())
                    .reportName(combo_reportName.getSelectionModel().getSelectedItem())
                    .startDate(txt_startDate.getText())
                    .endDate(txt_endDate.getText())
                    .management(txt_management.getText())
                    .payGroup(txt_payGroup.getText())
                    .nationalId(txt_search.getText())
                    .format(combo_Format.getSelectionModel().getSelectedItem())
                    .build();

            currentStrategy.validate(context);

            PayrollRequest request = currentStrategy.buildRequest(context);

            submissionService.submit(request,
                    reportId -> Platform.runLater(() -> {
                        closeStage(btnDoReport);
                        SAFNotification.success("تم تقديم الطلب بنجاح رقم الطلب: " + reportId);
                    }),
                    error -> Platform.runLater(() -> SAFNotification.error(error.getMessage()))
            );

        } catch (ValidationException ve) {
            SAFNotification.warning(ve.getMessage());
        }
    }
}