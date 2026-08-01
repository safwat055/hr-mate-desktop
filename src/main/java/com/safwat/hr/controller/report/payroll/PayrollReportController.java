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
 * كل هذا المنطق في الاستراتيجيات. دوره فقط التنسيق بين المكونات.
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
 *   → hasSubReports = false : يُطبِّق UiConfiguration مباشرةً
 *
 * اختيار تقرير فرعي
 *   → يُطبِّق UiConfiguration عبر PayrollUIManager
 *     (يُظهر الحقول + يُفعِّل أزرار البحث)
 *
 * الضغط على "استعلام"
 *   → doReport() : يبني ReportContext ← يتحقق ← يبني الطلب ← يُرسِل
 * </pre>
 *
 * <hr>
 *
 * <h2>تحديث configureSearchForCurrentStrategy</h2>
 * <p>استُبدِل منطق if/else للبحث القديم بـ {@link PayrollUIManager#apply(UiConfiguration)}
 * الذي يمشي على {@link UiConfiguration#getSearchFields()} تلقائيًا،
 * مما يجعل Controller نظيفًا تمامًا من أي منطق خاص بحقل بعينه.
 */
@Getter
public class PayrollReportController implements Initializable {

    // ─────────────────────────────────────────────
    //  Services & State
    // ─────────────────────────────────────────────
    private final PayrollPaymentsService paymentsService = PayrollPaymentsService.getInstance();
    /**
     * سجل الاستراتيجيات المسجَّلة — يُنشأ مرة واحدة عبر الـ Factory
     */
    private final ReportStrategyRegistry registry;

    /**
     * مدير الواجهة — مسؤول عن إظهار/إخفاء الحقول وتفعيل البحث
     */
    private final PayrollUIManager uiManager;

    /**
     * خدمة الإرسال — تُنفِّذ الطلب على خيط خلفي
     */
    private final ReportSubmissionService submissionService;

    /**
     * الاستراتيجية الحالية المختارة من المستخدم.
     * قد تكون تقريرًا فرعيًا (بعد اختيار من ComboBox الفرعي)
     * أو رئيسيًا مباشرًا (ليس حاوٍ).
     * تكون {@code null} إذا اختار المستخدم تقريرًا حاويًا ولم يختر فرعيًا بعد.
     */
    private ReportStrategy currentStrategy;

    // ─────────────────────────────────────────────
    //  FXML Components
    // ─────────────────────────────────────────────

    /**
     * حقول HBox للعناصر المتفرقة (وصف، ملاحظة، بحث حر)
     */
    @FXML
    private HBox H_1, H_2, H_3, H_4, H_5, H_6;

    /**
     * حقول HBox للمدخلات الرئيسية
     */
    @FXML
    private HBox H_element, H_employee, H_endDate, H_management, H_payGroup, H_startDate;

    /**
     * حقل HBox لـ ComboBox التقارير الفرعية — يُدار بشكل مستقل عن hideAll()
     */
    @FXML
    private HBox H_report;

    /**
     * قوائم الاختيار
     */
    @FXML
    private ComboBox<String> combo_Format, combo_report, combo_reportName;

    /**
     * Labels
     */
    @FXML
    private Label lbl_elementName, lbl_endDate, lbl_name, lbl_nationalId, lbl_payId, lbl_startDate;

    /**
     * حقول الإدخال
     */
    @FXML
    private TextField txt_startDate, txt_endDate, txt_element, txt_search, txt_payGroup, txt_management;

    /**
     * الـ Container الرئيسي للنموذج
     */
    @FXML
    private VBox mainCont;

    /**
     * الأزرار
     */
    @FXML
    private Button btn_PayGroupSearch, btn_managementSearch, btn_searchMonth, btnDoReport, btn_searchMonthEnd, btn_SearchEmpolyee;

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

    /**
     * يُغلِق الـ Stage الحاوي لأي مكوّن في الشاشة.
     *
     * @param targetComponent أي مكوّن داخل الـ Stage المطلوب إغلاقه
     */
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

    /**
     * يملأ القائمة المنسدلة الرئيسية بالتقارير ذات الفئة {@code main_*}.
     *
     * <p>الاستراتيجيات تُصفَّى بالفئة ({@code category.startsWith("main_")})
     * لضمان ظهور التقارير الرئيسية فقط وليس الفرعيات.
     */
    void fillMainCombo() {
       /* List<String> mainReports = registry.getAll().stream()
                .filter(s -> s.getCategory().startsWith("main_"))
                .map(ReportStrategy::getDisplayName)
                .toList();*/
        List<String> mainReports = null;
        try {
            mainReports = ApiClient.getAvailableReports().getData().stream().map(AvailableReportInfo::getArabicName).distinct().toList();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        combo_reportName.getItems().addAll(mainReports);
    }

    // ─────────────────────────────────────────────
    //  Listeners
    // ─────────────────────────────────────────────

    /**
     * يملأ قائمة الصيغ ويضبط PDF كاختيار افتراضي
     */
    void fillFormatCombo() {
        combo_Format.getItems().addAll("PDF", "EXCEL");
        combo_Format.getSelectionModel().select(0);
    }

    // ─────────────────────────────────────────────
    //  Search Dialog
    // ─────────────────────────────────────────────

    /**
     * يُعدِّل Listeners للقائمتين الرئيسية والفرعية.
     *
     * <p><b>القائمة الرئيسية ({@code combo_reportName}):</b>
     * <ul>
     *   <li>إذا كان التقرير حاوٍ ({@code hasSubReports = true}):
     *       يخفي الحقول ويُظهر {@code H_report} بقائمة الفرعيات.</li>
     *   <li>إذا كان مباشرًا: يُطبِّق {@link UiConfiguration} مباشرةً.</li>
     * </ul>
     *
     * <p><b>القائمة الفرعية ({@code combo_report}):</b>
     * يُطبِّق إعدادات الاستراتيجية الفرعية المختارة.
     */
    void setupListeners() {
        combo_reportName.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, newVal) -> {
                    if (newVal == null) return;
                    ReportStrategy mainStrategy = registry.getByDisplayName(newVal);

                    if (mainStrategy.hasSubReports()) {
                        // تقرير حاوٍ: أخفِ كل شيء ثم اعرض ComboBox الفرعيات
                        uiManager.hideAll();
                        combo_report.getItems().clear();
                        combo_report.getItems().addAll(
                                registry.getDisplayNamesByCategory(mainStrategy.getMainReport())
                        );
                        H_report.setManaged(true);
                        H_report.setVisible(true);
                        currentStrategy = null; // ينتظر اختيار الفرعي

                    } else {
                        // تقرير مباشر: أخفِ الفرعي وطبِّق الإعدادات
                        H_report.setManaged(false);
                        H_report.setVisible(false);
                        combo_report.getItems().clear();
                        combo_report.getSelectionModel().clearSelection();
                        currentStrategy = mainStrategy;
                        uiManager.apply(currentStrategy.getUiConfig());
                    }
                });

        combo_report.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, newVal) -> {
                    if (newVal == null) return;
                    currentStrategy = registry.getByDisplayName(newVal);
                    uiManager.apply(currentStrategy.getUiConfig());
                });

        // زر اختيار الشهر — مشترك بين جميع التقارير
        searchInMonths(btn_searchMonth, txt_startDate, lbl_startDate);

        searchInMonths(btn_searchMonthEnd, txt_endDate, lbl_endDate);
        btn_SearchEmpolyee.setOnAction(event -> {
            PayrollRequest request = PayrollRequest.builder().build();
            request.setSearchValue(txt_search.getText());
            List<SearchEmp> data = paymentsService.searchInEmployees(request).getData();

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
        });


    }

    private void searchInMonths(Button btnSearchMonthEnd, TextField txtEndDate, Label lblEndDate) {
        btnSearchMonthEnd.setOnAction(e -> {
            Optional<String> month = SearchDialog.forStrings()
                    .title("اختر شهر")
                    .data(DataSourceResolver.get("monthsYearly"))
                    .show();
            month.ifPresent(m -> {
                txtEndDate.setText(m);
                lblEndDate.setText(DateUtils.toArabicMonthYear(DateUtils.getFirstDayOfMonth(m)));
            });
        });
    }

    // ─────────────────────────────────────────────
    //  Report Submission
    // ─────────────────────────────────────────────

    /**
     * يفتح نافذة بحث ويضع النتيجة في الـ TextField المستهدف.
     *
     * <p>إذا كان الـ TextField يحتوي على نص، تُصفَّى القائمة لإظهار
     * المطابقات فقط؛ وإلا تظهر القائمة كاملةً.
     *
     * <p>يُستدعى من {@link com.safwat.hr.report.payroll.ui.PayrollUIManager}
     * عبر {@code controller.openSearchDialog(...)}.
     *
     * @param title       عنوان نافذة البحث
     * @param data        قائمة البيانات الكاملة
     * @param targetField الـ TextField الذي يستقبل النتيجة
     */
    public void openSearchDialog(String title, List<String> data, TextField targetField) {
        String currentText = targetField.getText();
        List<String> dialogData = (currentText == null || currentText.isBlank())
                ? data
                : data.stream().filter(s -> s.contains(currentText)).toList();

        // لو التصفية أفرغت القائمة نعرض الكل
        if (dialogData.isEmpty()) dialogData = data;

        SearchDialog.forStrings()
                .title(title)
                .data(dialogData)
                .show()
                .ifPresent(targetField::setText);
    }

    // ─────────────────────────────────────────────
    //  Utility
    // ─────────────────────────────────────────────

    /**
     * يُنفِّذ عملية إرسال التقرير.
     *
     * <p>التسلسل:
     * <ol>
     *   <li>التحقق من اختيار استراتيجية.</li>
     *   <li>بناء {@link ReportContext} من حقول الـ UI.</li>
     *   <li>التحقق من صحة المدخلات عبر {@code strategy.validate()}.</li>
     *   <li>بناء الطلب عبر {@code strategy.buildRequest()}.</li>
     *   <li>الإرسال عبر {@link ReportSubmissionService#submit} على خيط خلفي.</li>
     * </ol>
     *
     * <p>عند النجاح: تُغلَق النافذة وتظهر رسالة نجاح.
     * عند فشل التحقق: تظهر رسالة تحذير دون إغلاق.
     * عند خطأ HTTP: تظهر رسالة خطأ.
     */
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
}