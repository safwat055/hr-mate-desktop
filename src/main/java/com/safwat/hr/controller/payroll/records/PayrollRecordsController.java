package com.safwat.hr.controller.payroll.records;

import com.safwat.hr.controller.payroll.payrollApi.dto.EmployeeSearchResult;
import com.safwat.hr.controller.payroll.payrollApi.dto.ViewMainRecordForRangeDate;
import com.safwat.hr.controller.payroll.payrollApi.dto.ViewNonPrimaryRangeDate;
import com.safwat.hr.shared.ui.SearchDialog;
import com.safwat.hr.shared.ui.SmartSearchHelper;
import com.safwat.hr.shared.util.DateUtils;
import com.safwat.hr.ui.controls.SAFNotification;
import com.safwat.hr.ui.util.PDFView;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebView;

import java.net.URL;
import java.nio.file.Path;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class PayrollRecordsController implements Initializable {

    private static final String NATIONAL_ID_REQUIRED = "يجب إدخال الرقم القومى او البحث عن قيمة أولا";

    @FXML
    private HBox backBar;
    @FXML
    private Button btn_back;
    @FXML
    private TabPane mainTabs;
    @FXML
    private WebView webView;

    @FXML
    private Button btn_Note;
    @FXML
    private Button btn_PDF_Record;
    @FXML
    private Button btn_PDF_Review;
    @FXML
    private Button btn_PDF_Review2;
    @FXML
    private Button btn_Record, btn_Record2;
    @FXML
    private Button btn_search;

    @FXML
    private RadioButton rb_emp_code;
    @FXML
    private RadioButton rb_emp_name;
    @FXML
    private RadioButton rb_national_id;
    @FXML
    private ToggleGroup searchGroup;

    @FXML
    private TableView<javafx.collections.ObservableList<String>> t_allowances;
    @FXML
    private TableView<javafx.collections.ObservableList<String>> t_allowances2;
    @FXML
    private TableView<javafx.collections.ObservableList<String>> t_deductions;
    @FXML
    private TableView<javafx.collections.ObservableList<String>> t_deductions2;

    @FXML
    private TextField txt_bank;
    @FXML
    private TextField txt_basic_30_6;
    @FXML
    private TextField txt_branch;
    @FXML
    private TextField txt_code;
    @FXML
    private TextField txt_degree;
    @FXML
    private TextField txt_endMonth, txt_endMonth2;
    @FXML
    private TextField txt_id;
    @FXML
    private TextField txt_management;
    @FXML
    private TextField txt_month;
    @FXML
    private TextField txt_month2;
    @FXML
    private TextField txt_monthRecord;
    @FXML
    private TextField txt_name;
    @FXML
    private TextField txt_search;
    @FXML
    private TextField txt_startMonth, txt_startMonth2;

    private final PayrollRecordsService service = new PayrollRecordsService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setUpSearchEmployee();
        setAvailableMonth();
        setButtonsActions();
        showForm();
    }

    // ═════════════════════════════════════════════
    //  حالة الواجهة (تابات / معاينة PDF)
    // ═════════════════════════════════════════════

    /**
     * يعرض التابات ويخفي المعاينة، ويفضّي الـ WebView من الذاكرة
     */
    private void showForm() {
        setShown(backBar, false);
        setShown(mainTabs, true);
        setShown(webView, false);
        if (webView != null) {
            webView.getEngine().load("about:blank");
        }
    }

    /**
     * يعرض المعاينة ويخفي التابات
     */
    private void showPreview() {
        setShown(mainTabs, false);
        setShown(webView, true);
        setShown(backBar, true);
    }

    private static void setShown(Node node, boolean shown) {
        if (node == null) return;
        node.setVisible(shown);
        node.setManaged(shown);
    }

    /**
     * يستنى التحميل، وبعدين يعرض الملف. المعاينة بتظهر بس لو التحميل نجح.
     */
    private void showPdf(CompletableFuture<Path> request, Button trigger) {
        SAFNotification.success("جاري تحميل الملف...");
        trigger.setDisable(true);

        request.whenComplete((path, ex) -> Platform.runLater(() -> {
            trigger.setDisable(false);
            if (ex != null) {
                SAFNotification.error("حدث خطأ أثناء التحميل: " + friendly(ex));
                return;
            }
            showPreview();
            PDFView.showIN(path.toString(), webView);
        }));
    }

    private static String friendly(Throwable ex) {
        Throwable root = ex instanceof CompletionException && ex.getCause() != null ? ex.getCause() : ex;
        return root.getMessage() != null ? root.getMessage() : root.getClass().getSimpleName();
    }

    private boolean hasNationalId() {
        String nid = txt_id.getText();
        if (nid == null || nid.length() != 14) {
            SAFNotification.warning(NATIONAL_ID_REQUIRED);
            return false;
        }
        return true;
    }

    // ═════════════════════════════════════════════
    //  Months / Records
    // ═════════════════════════════════════════════

    void setAvailableMonth() {

        SmartSearchHelper.bind(
                txt_month,
                () -> service.getEmployeeMonthsReview(txt_id.getText()),
                SearchDialog.builder(String.class)
                        .title("اختر الشهر")
                        .column("التاريخ", s -> s == null ? "" : s)
                        .searchPlaceholder("اكتب جزءاً من التاريخ..."),
                selectedMonth -> {
                    ViewMainRecordForRangeDate data = service.getMainMonthRecords(txt_id.getText(), selectedMonth, selectedMonth);
                    fillMonthRecord(data);
                },
                SmartSearchHelper.FieldBind.of(txt_month, date ->
                        DateUtils.toArabicMonthYear(DateUtils.getFirstDayOfMonth(date)))
        );

        SmartSearchHelper.bind(
                txt_month2,
                () -> service.getEmployeeMonthsReview(txt_id.getText()),
                SearchDialog.builder(String.class)
                        .title("اختر الشهر")
                        .column("التاريخ", s -> s == null ? "" : s)
                        .searchPlaceholder("اكتب جزءاً من التاريخ..."),
                selectedMonth -> {
                    ViewNonPrimaryRangeDate data = service.getNonPrimaryMonthRecords(txt_id.getText(), selectedMonth, selectedMonth);
                    fillMonthRecord(data);
                },
                SmartSearchHelper.FieldBind.of(txt_month2, date ->
                        DateUtils.toArabicMonthYear(DateUtils.getFirstDayOfMonth(date)))
        );

        SmartSearchHelper.bind(
                txt_endMonth,
                () -> service.getEmployeeMonthsReview(txt_id.getText()),
                SearchDialog.builder(String.class)
                        .title("اختر الشهر")
                        .column("التاريخ", s -> s == null ? "" : s)
                        .searchPlaceholder("اكتب جزءاً من التاريخ..."),
                selectedMonth -> {
                    ViewMainRecordForRangeDate data = service.getMainMonthRecords(txt_id.getText(), txt_startMonth.getText(), selectedMonth);
                    fillMonthRecord(data);
                },
                SmartSearchHelper.FieldBind.of(txt_endMonth, date ->
                        DateUtils.toArabicMonthYear(DateUtils.getFirstDayOfMonth(date)))
        );

        SmartSearchHelper.bind(
                txt_endMonth2,
                () -> service.getEmployeeMonthsReview(txt_id.getText()),
                SearchDialog.builder(String.class)
                        .title("اختر الشهر")
                        .column("التاريخ", s -> s == null ? "" : s)
                        .searchPlaceholder("اكتب جزءاً من التاريخ..."),
                selectedMonth -> {
                    ViewNonPrimaryRangeDate data = service.getNonPrimaryMonthRecords(txt_id.getText(), txt_startMonth2.getText(), selectedMonth);
                    fillMonthRecord(data);
                },
                SmartSearchHelper.FieldBind.of(txt_endMonth2, date ->
                        DateUtils.toArabicMonthYear(DateUtils.getFirstDayOfMonth(date)))
        );

        SmartSearchHelper.bind(txt_startMonth,
                () -> service.getEmployeeMonthsReview(txt_id.getText()),
                val -> txt_startMonth.setText(
                        DateUtils.toArabicMonthYear(DateUtils.getFirstDayOfMonth(val))
                )
        );
        SmartSearchHelper.bind(txt_startMonth2,
                () -> service.getEmployeeMonthsReview(txt_id.getText()),
                val -> txt_startMonth2.setText(
                        DateUtils.toArabicMonthYear(DateUtils.getFirstDayOfMonth(val))
                )
        );

        SmartSearchHelper.bind(txt_monthRecord,
                () -> service.getMonthReviewPayments(txt_id.getText(), txt_month2.getText()),
                val -> txt_monthRecord.setText(val)
        );
    }

    // ═════════════════════════════════════════════
    //  الأزرار
    // ═════════════════════════════════════════════

    void setButtonsActions() {
        btn_back.setOnAction(_ -> showForm());

        btn_Record.setOnAction(_ -> {
            if (txt_startMonth.getText().isEmpty() || txt_endMonth.getText().isEmpty()) {
                SAFNotification.warning("يجب تحديد فترة بداية ونهاية اولا");
                return;
            }
            ViewMainRecordForRangeDate data = service.getMainMonthRecords(txt_id.getText(), txt_startMonth.getText(), txt_endMonth.getText());
            fillMonthRecord(data);
        });

        btn_Record2.setOnAction(_ -> {
            if (txt_startMonth2.getText().isEmpty() || txt_endMonth2.getText().isEmpty()) {
                SAFNotification.warning("يجب تحديد فترة بداية ونهاية اولا");
                return;
            }
            ViewNonPrimaryRangeDate data = service.getNonPrimaryMonthRecords(txt_id.getText(), txt_startMonth2.getText(), txt_endMonth2.getText());
            fillMonthRecord(data);
        });

        btn_Note.setOnAction(_ -> {
            if (!hasNationalId()) return;
            showPdf(service.compareExportToPDF(txt_month.getText(), txt_id.getText(), txt_name.getText()), btn_Note);
        });

        btn_PDF_Review.setOnAction(_ -> {
            if (!hasNationalId()) return;
            showPdf(service.downloadMainReviewReport(txt_id.getText(), txt_month.getText(), txt_name.getText()), btn_PDF_Review);
        });

        btn_PDF_Review2.setOnAction(_ -> {
            if (!hasNationalId()) return;
            showPdf(service.downloadCustomReviewToPDF(txt_month2.getText(), txt_monthRecord.getText(), txt_id.getText(), txt_name.getText()), btn_PDF_Review2);
        });

        // تقرير السجل (129) — طلب بيرجع رقم، مش PDF، فبيفضل زي ما هو
        btn_PDF_Record.setOnAction(_ -> service.downloadRecord_129(txt_id.getText(), txt_startMonth.getText(), txt_endMonth.getText()));
    }

    // ═════════════════════════════════════════════
    //  عرض البيانات في الجداول
    // ═════════════════════════════════════════════

    void fillMonthRecord(ViewMainRecordForRangeDate data) {
        if (data == null) {
            return;
        }
        txt_degree.setText(data.degree());
        txt_management.setText(data.department());
        txt_basic_30_6.setText(data.basic30_6());
        txt_bank.setText(data.bank());
        txt_branch.setText(data.branch());
        TableUtils.fillTable(t_allowances, data.allowancesHeader(), data.allowancesValues(), false, 100.00);
        TableUtils.fillTable(t_deductions, data.deductionsHeader(), data.deductionsValues(), false, 100.00);
    }

    void fillMonthRecord(ViewNonPrimaryRangeDate data) {
        if (data == null) {
            return;
        }
        TableUtils.fillTable(t_allowances2, data.allowanceHeaders(), data.allowanceRows(), false, 100.00, false);
        TableUtils.fillTable(t_deductions2, data.deductionHeaders(), data.deductionRows(), false, 100.00, false);
    }

    // ═════════════════════════════════════════════
    //  البحث عن موظف
    // ═════════════════════════════════════════════

    void setUpSearchEmployee() {
        SmartSearchHelper.bind(
                txt_search, btn_search,
                () -> service.searchEmployee(txt_search.getText()),
                SearchDialog.builder(EmployeeSearchResult.class)
                        .title("بحث عن موظف")
                        .column("رقم قومى", EmployeeSearchResult::getNational_id)
                        .column("رقم موظف", EmployeeSearchResult::getPay_id)
                        .column("الاسم", EmployeeSearchResult::getEmp_name)
                        .column("الوظيفة", EmployeeSearchResult::getJob)
                        .column("الادارة", EmployeeSearchResult::getPay_management)
                        .column("فئة التعيين", EmployeeSearchResult::getAssignment_class),
                _ -> {
                },
                SmartSearchHelper.FieldBind.of(txt_id, EmployeeSearchResult::getNational_id),
                SmartSearchHelper.FieldBind.of(txt_name, EmployeeSearchResult::getEmp_name),
                SmartSearchHelper.FieldBind.of(txt_code, EmployeeSearchResult::getPay_id)
        );
    }
}