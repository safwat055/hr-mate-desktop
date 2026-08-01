package com.safwat.hr.controller.report.payroll;

import com.safwat.hr.report.payroll.DataSourceResolver;
import com.safwat.hr.report.payroll.ReportContext;
import com.safwat.hr.report.payroll.ReportSubmissionService;
import com.safwat.hr.report.payroll.ValidationException;
import com.safwat.hr.report.payroll.strategies.ReportRegistryFactory;
import com.safwat.hr.report.payroll.strategies.ReportStrategy;
import com.safwat.hr.report.payroll.strategies.ReportStrategyRegistry;
import com.safwat.hr.report.payroll.ui.PayrollUIManager;
import com.safwat.hr.service.payroll.PayrollPaymentsService;
import com.safwat.hr.service.payroll.dto.PayrollRequest;
import com.safwat.hr.service.payroll.dto.SearchEmp;
import com.safwat.hr.shared.util.DateUtils;
import com.safwat.hr.ui.controls.SAFNotification;
import com.safwat.hr.ui.util.SearchDialog;
import com.safwat.hr.utils.ApiClient;
import com.safwat.hr.utils.dto.AvailableReportInfo;
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

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * الـ Controller الرئيسي لشاشة تقارير الرواتب.
 *
 * <p>يتولى:
 * <ul>
 *   <li>تهيئة القوائم المنسدلة عند فتح الشاشة.</li>
 *   <li>الاستماع لاختيارات المستخدم وتفويض تحديث الواجهة إلى {@link PayrollUIManager}.</li>
 *   <li>بناء {@link ReportContext} من حقول الـ UI وتمريره للاستراتيجية.</li>
 *   <li>إطلاق عملية الإرسال عبر {@link ReportSubmissionService}.</li>
 * </ul>
 *
 * <p><b>الـ Controller لا يعرف شيئًا عن منطق أي تقرير بعينه</b> —
 * كل منطق الواجهة والتخصيص موجود في الاستراتيجية عبر {@link ReportStrategy#onApply}.
 * دور الـ Controller فقط التنسيق بين المكونات.
 *
 * <hr>
 *
 * <h2>تدفق العمل</h2>
 * <pre>
 * فتح الشاشة
 *   → initialize() : يملأ القوائم ويخفي الحقول
 *
 * اختيار تقرير رئيسي
 *   → hasSubReports = true  : يُظهر ComboBox الفرعيات
 *   → hasSubReports = false : يستدعي applyStrategy() مباشرةً
 *
 * اختيار تقرير فرعي
 *   → applyStrategy() : يُطبِّق UiConfiguration + onApply()
 *
 * الضغط على "استعلام"
 *   → doReport() : يبني ReportContext ← يتحقق ← يبني الطلب ← يُرسِل
 * </pre>
 */
@Getter
public class PayrollReportController implements Initializable {

    // ─────────────────────────────────────────────
    //  Services & State
    // ─────────────────────────────────────────────

    private final PayrollPaymentsService paymentsService = PayrollPaymentsService.getInstance();
    private final ReportStrategyRegistry registry;
    private final PayrollUIManager uiManager;
    private final ReportSubmissionService submissionService;

    /**
     * الاستراتيجية الحالية المختارة من المستخدم.
     * {@code null} إذا اختار تقريرًا حاويًا ولم يختر فرعيًا بعد.
     */
    private ReportStrategy currentStrategy;

    // ─────────────────────────────────────────────
    //  FXML Components
    // ─────────────────────────────────────────────

    @FXML
    private HBox H_1, H_2, H_3, H_4, H_5, H_6;
    @FXML
    private HBox H_Search, H_employee, H_endDate, H_management, H_payGroup, H_startDate;

    /**
     * يُدار بشكل مستقل عن hideAll()
     */
    @FXML
    private HBox H_report;

    @FXML
    private ComboBox<String> combo_Format, combo_report, combo_reportName;

    @FXML
    private Label lbl_endDate, lbl_name, lbl_nationalId, lbl_payId, lbl_startDate, lbl_searchValue, lbl_start, lbl_end, lbl_search;

    @FXML
    private TextField txt_startDate, txt_endDate, txt_search, txt_searchEmp, txt_payGroup, txt_management;

    @FXML
    private VBox mainCont;

    @FXML
    private Button btn_PayGroupSearch, btn_managementSearch,
            btn_searchMonth, btn_searchMonthEnd,
            btn_SearchEmpolyee, btnDoReport;

    // ─────────────────────────────────────────────
    //  Constructor
    // ─────────────────────────────────────────────

    public PayrollReportController() {
        this.registry = ReportRegistryFactory.create();
        this.uiManager = new PayrollUIManager(this);
        this.submissionService = new ReportSubmissionService();
    }

    // ─────────────────────────────────────────────
    //  Initialization
    // ─────────────────────────────────────────────

    public static void closeStage(Node targetComponent) {
        ((Stage) targetComponent.getScene().getWindow()).close();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        fillFormatCombo();
        fillMainCombo();
        setupListeners();
        uiManager.hideAll();
    }

    void fillMainCombo() {
        try {
            List<String> mainReports = ApiClient.getAvailableReports()
                    .getData().stream()
                    .map(AvailableReportInfo::getArabicName)
                    .distinct()
                    .toList();
            combo_reportName.getItems().addAll(mainReports);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("فشل تحميل قائمة التقارير", e);
        }
    }

    // ─────────────────────────────────────────────
    //  Listeners
    // ─────────────────────────────────────────────

    void fillFormatCombo() {
        combo_Format.getItems().addAll("PDF", "EXCEL");
        combo_Format.getSelectionModel().select(0);
    }

    void setupListeners() {
        // ── القائمة الرئيسية ──
        combo_reportName.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, newVal) -> {
                    if (newVal == null) return;
                    ReportStrategy mainStrategy = registry.getByDisplayName(newVal);

                    if (mainStrategy.hasSubReports()) {
                        uiManager.hideAll();
                        combo_report.getItems().clear();
                        combo_report.getItems().addAll(
                                registry.getDisplayNamesByCategory(mainStrategy.getMainReport())
                        );
                        H_report.setManaged(true);
                        H_report.setVisible(true);
                        currentStrategy = null;
                    } else {
                        H_report.setManaged(false);
                        H_report.setVisible(false);
                        combo_report.getItems().clear();
                        combo_report.getSelectionModel().clearSelection();
                        applyStrategy(mainStrategy);
                    }
                });

        // ── القائمة الفرعية ──
        combo_report.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, newVal) -> {
                    if (newVal == null) return;
                    applyStrategy(registry.getByDisplayName(newVal));
                });

        // ── أزرار اختيار الشهر (المشتركة) ──
        // ملاحظة: هذه الأزرار الافتراضية — الاستراتيجية تقدر تـ override في onApply لو احتاجت
        setupMonthButton(btn_searchMonth, txt_startDate, lbl_startDate);
        setupMonthButton(btn_searchMonthEnd, txt_endDate, lbl_endDate);

        // ── زر البحث عن موظف (مشترك) ──
        btn_SearchEmpolyee.setOnAction(event -> searchEmployee());
    }

    // ─────────────────────────────────────────────
    //  Public Helpers — متاحة للاستراتيجيات عبر onApply
    // ─────────────────────────────────────────────

    /**
     * يُطبِّق استراتيجية على الواجهة.
     *
     * <p>نقطة التحكم المركزية — كل تبديل تقرير يمر من هنا.
     * بعد {@code uiManager.apply()} تُستدعى {@code onApply()} للتخصيص الكامل.
     *
     * @param strategy الاستراتيجية المختارة
     */
    private void applyStrategy(ReportStrategy strategy) {
        currentStrategy = strategy;
        // apply تمرر الاستراتيجية عشان تستدعي onApply في النهاية
        uiManager.apply(strategy.getUiConfig(), strategy);
    }

    /**
     * يفتح نافذة بحث ويضع النتيجة في الـ TextField المستهدف.
     *
     * <p>يُستدعى من:
     * <ul>
     *   <li>{@link PayrollUIManager} لأزرار البحث الأساسية</li>
     *   <li>الاستراتيجيات مباشرةً في {@code onApply} لأزرار بحث مخصصة</li>
     * </ul>
     *
     * @param title       عنوان نافذة البحث
     * @param data        قائمة البيانات
     * @param targetField الـ TextField الذي يستقبل النتيجة
     */
    public void openSearchDialog(String title, List<String> data, TextField targetField) {
        String currentText = targetField.getText();
        List<String> dialogData = (currentText == null || currentText.isBlank())
                ? data
                : data.stream().filter(s -> s.contains(currentText)).toList();

        if (dialogData.isEmpty()) dialogData = data;

        SearchDialog.forStrings()
                .title(title)
                .data(dialogData)
                .show()
                .ifPresent(targetField::setText);
    }

    // ─────────────────────────────────────────────
    //  Report Submission
    // ─────────────────────────────────────────────

    /**
     * يربط زر بحث شهر على TextField و Label محددين.
     *
     * <p>متاح للاستراتيجيات في {@code onApply} لإعادة ربط أزرار الشهر
     * بـ Label مختلف أو مصدر بيانات مختلف.
     *
     * <p><b>مثال من استراتيجية:</b>
     * <pre>{@code
     * @Override
     * public void onApply(PayrollReportController c) {
     *     // تغيير Label الشهر
     *     c.getLbl_startDate().setText("من شهر");
     *     c.getLbl_endDate().setText("إلى شهر");
     *
     *     // إعادة ربط الزر بمصدر بيانات مختلف
     *     c.setupMonthButton(c.getBtn_searchMonth(), c.getTxt_startDate(), c.getLbl_startDate());
     * }
     * }</pre>
     *
     * @param btn   زر الاختيار
     * @param field حقل الإدخال
     * @param label Label العرض
     */
    public void setupMonthButton(Button btn, TextField field, Label label) {
        btn.setOnAction(e -> {
            Optional<String> month = SearchDialog.forStrings()
                    .title("اختر شهر")
                    .data(DataSourceResolver.get("monthsYearly"))
                    .show();
            month.ifPresent(m -> {
                field.setText(m);
                label.setText(DateUtils.toArabicMonthYear(DateUtils.getFirstDayOfMonth(m)));
            });
        });

        field.setOnAction(_ -> {
            if (DateUtils.toArabicMonthYear(DateUtils.getFirstDayOfMonth(field.getText())) != null
                    && DateUtils.getFirstDayOfMonth(field.getText()) != null) {
                label.setText(DateUtils.toArabicMonthYear(DateUtils.getFirstDayOfMonth(field.getText())));
            } else {
                Optional<String> month = SearchDialog.forStrings()
                        .title("اختر شهر")
                        .data(DataSourceResolver.get("monthsYearly"))
                        .show();
                month.ifPresent(m -> {
                    field.setText(m);
                    label.setText(DateUtils.toArabicMonthYear(DateUtils.getFirstDayOfMonth(m)));
                });
            }
        });


    }

    // ─────────────────────────────────────────────
    //  Private Helpers
    // ─────────────────────────────────────────────

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
                    .nationalId(lbl_nationalId.getText())
                    .format(combo_Format.getSelectionModel().getSelectedItem())
                    .build();

            currentStrategy.validate(context);

            PayrollRequest request = currentStrategy.buildRequest(context);

            submissionService.submit(request,
                    reportId -> Platform.runLater(() -> {
                        closeStage(btnDoReport);
                        SAFNotification.success("تم تقديم الطلب بنجاح — رقم الطلب: " + reportId);
                    }),
                    error -> Platform.runLater(() ->
                            SAFNotification.error(error.getMessage())
                    )
            );

        } catch (ValidationException ve) {
            SAFNotification.warning(ve.getMessage());
        }
    }

    public void searchEmployee() {
        PayrollRequest request = PayrollRequest.builder().build();
        request.setSearchValue(txt_searchEmp.getText());
        List<SearchEmp> data = paymentsService.searchInEmployees(request).getData();
        if (data.size() == 1) {
            SearchEmp row = data.getFirst();
            lbl_nationalId.setText(row.getNational_id());
            lbl_payId.setText(row.getPay_id());
            lbl_name.setText(row.getEmp_name());
            return;
        }
        Optional<SearchEmp> result = SearchDialog.builder(SearchEmp.class)
                .title("نتائج البحث")
                .column("رقم قومي", SearchEmp::getNational_id)
                .column("رقم موظف", SearchEmp::getPay_id)
                .column("اسم", SearchEmp::getEmp_name)
                .data(data)
                .searchPlaceholder("ابحث للتصفية")
                .show();

        result.ifPresent(row -> {
            lbl_nationalId.setText(row.getNational_id());
            lbl_payId.setText(row.getPay_id());
            lbl_name.setText(row.getEmp_name());
        });
    }

    public void setStartAndEndActions() {
        setupMonthButton(btn_searchMonth, txt_startDate, lbl_startDate);
        setupMonthButton(btn_searchMonthEnd, txt_endDate, lbl_endDate);
        lbl_start.setText("بداية التقرير");
        lbl_end.setText("نهاية التقرير");
    }

    public void setChoseMonth() {
        setupMonthButton(btn_searchMonth, txt_startDate, lbl_startDate);
        lbl_start.setText("اختر شهر");
    }

    public void setSearchEmployeeActions() {
        btn_SearchEmpolyee.setOnAction(_ -> searchEmployee());
        txt_searchEmp.setOnAction(_ -> searchEmployee());
    }
}