package com.safwat.hr.controller.payroll.vocab;

import com.safwat.hr.controller.payroll.payrollApi.dto.EmployeeSearchResult;
import com.safwat.hr.controller.payroll.vocab.service.PayrollVocabService;
import com.safwat.hr.shared.ui.SearchDialog;
import com.safwat.hr.shared.ui.SmartSearchHelper;
import com.safwat.hr.shared.util.DateUtils;
import com.safwat.hr.ui.controls.SAFNotification;
import com.safwat.hr.ui.icons.Icons;
import com.safwat.hr.ui.util.PDFView;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;

import java.net.URL;
import java.nio.file.Path;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class PayrollVocabController implements Initializable {

    private final PayrollVocabService vocabService = new PayrollVocabService();

    @FXML
    private Button btn_back;
    @FXML
    private Button btn_open;
    @FXML
    private Button btn_search;
    @FXML
    private Button btn_clear;
    @FXML
    private Label lbl_path;
    @FXML
    private TextField txt_management;
    @FXML
    private TextField txt_month;
    @FXML
    private TextField txt_name;
    @FXML
    private TextField txt_nationalID;
    @FXML
    private TextField txt_payID;
    @FXML
    private TextField txt_search;
    @FXML
    private TextField txt_recordName;

    @FXML
    private VBox resultBox;
    @FXML
    private WebView webView;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setUi();
        setUpSearchEmployee();
        setButtonActions();
        setTextFieldsAction();
        showForm();
    }

    void setUi() {
        Icons.getInstance().getPDFImage(btn_open);
    }

    void setButtonActions() {
        btn_open.setOnAction(_ -> exportToPDF());
        btn_clear.setOnAction(_ -> clear());
        btn_back.setOnAction(_ -> showForm());
    }

    /**
     * يمسح كل الحقول ويرجّع الشاشة للحالة الافتراضية
     */
    void clear() {
        txt_search.clear();
        txt_nationalID.clear();
        txt_name.clear();
        txt_payID.clear();
        txt_month.clear();
        txt_management.clear();
        txt_recordName.clear();

        showForm();
    }

    // ═════════════════════════════════════════════
    //  حالة الواجهة (إطار النتائج / معاينة PDF)
    // ═════════════════════════════════════════════

    /**
     * يعرض إطار النتائج ويخفي المعاينة، ويفضّي الـ WebView من الذاكرة
     */
    private void showForm() {
        setShown(resultBox, true);
        setShown(webView, false);
        setShown(btn_back, false);
        if (webView != null) {
            webView.getEngine().load("about:blank");
        }
    }

    /**
     * يعرض المعاينة ويخفي إطار النتائج
     */
    private void showPreview() {
        setShown(resultBox, false);
        setShown(webView, true);
        setShown(btn_back, true);
    }

    private static void setShown(Node node, boolean shown) {
        if (node == null) return;
        node.setVisible(shown);
        node.setManaged(shown);
    }

    // ═════════════════════════════════════════════
    //  البحث
    // ═════════════════════════════════════════════

    void setTextFieldsAction() {
        SmartSearchHelper.bind(
                txt_month,
                () -> vocabService.getEmployeeMonthsReview(txt_nationalID.getText()),
                SearchDialog.builder(String.class)
                        .title("اختر الشهر")
                        .column("التاريخ", s -> s == null ? "" : s)
                        .searchPlaceholder("اكتب جزءاً من التاريخ..."),
                this::generateMainReport,
                SmartSearchHelper.FieldBind.of(txt_month, date ->
                        DateUtils.toArabicMonthYear(DateUtils.getFirstDayOfMonth(date)))
        );

        SmartSearchHelper.bind(
                txt_recordName,
                () -> vocabService.getMonthRecordsName(txt_nationalID.getText(), txt_month.getText()),
                SearchDialog.builder(String.class)
                        .title("اختر صرفية")
                        .column("الصرفية", s -> s == null ? "" : s)
                        .searchPlaceholder("اكتب جزءاً من الصرفية..."),
                this::generateCustomReport,
                SmartSearchHelper.FieldBind.of(txt_recordName, x -> x)
        );
    }

    void setUpSearchEmployee() {
        SmartSearchHelper.bind(
                txt_search, btn_search,
                () -> vocabService.searchEmployee(txt_search.getText()),
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
                SmartSearchHelper.FieldBind.of(txt_nationalID, EmployeeSearchResult::getNational_id),
                SmartSearchHelper.FieldBind.of(txt_name, EmployeeSearchResult::getEmp_name),
                SmartSearchHelper.FieldBind.of(txt_payID, EmployeeSearchResult::getPay_id)
        );
    }

    // ═════════════════════════════════════════════
    //  PDF
    // ═════════════════════════════════════════════

    private void exportToPDF() {
        if (!hasNationalId()) return;
        generateMainReport(txt_month.getText());
    }

    /**
     * تقرير المراجعة الرئيسي للشهر المختار
     */
    private void generateMainReport(String month) {
        if (!hasNationalId()) return;
        showPdf(vocabService.downloadMainReviewReport(
                txt_nationalID.getText(),
                month,
                txt_name.getText()));
    }

    /**
     * تقرير مراجعة لصرفية معينة داخل الشهر
     */
    private void generateCustomReport(String payGroup) {
        if (!hasNationalId()) return;
        showPdf(vocabService.downloadCustomReviewReport(
                txt_nationalID.getText(),
                payGroup,
                txt_month.getText(),
                txt_name.getText()));
    }

    private boolean hasNationalId() {
        String nid = txt_nationalID.getText();
        if (nid == null || nid.length() != 14) {
            SAFNotification.warning("يجب إدخال الرقم القومى او البحث عن قيمة أولا");
            return false;
        }
        return true;
    }

    /**
     * يستنى التحميل، وبعدين يعرض الملف في المعاينة. المعاينة بتظهر بس لو التحميل نجح.
     */
    private void showPdf(CompletableFuture<Path> request) {
        request.whenComplete((path, ex) -> Platform.runLater(() -> {
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
}