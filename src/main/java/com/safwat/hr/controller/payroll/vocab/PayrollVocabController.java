package com.safwat.hr.controller.payroll.vocab;

import com.safwat.hr.controller.payroll.payrollApi.dto.SearchEmp;
import com.safwat.hr.controller.payroll.vocab.service.PayrollVocabService;
import com.safwat.hr.shared.ui.SearchDialog;
import com.safwat.hr.shared.ui.SmartSearchHelper;
import com.safwat.hr.shared.util.DateUtils;
import com.safwat.hr.ui.controls.SAFNotification;
import com.safwat.hr.ui.icons.Icons;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.web.WebView;

import java.net.URL;
import java.util.ResourceBundle;

public class PayrollVocabController implements Initializable {


    private final PayrollVocabService vocabService = new PayrollVocabService();
    @FXML
    private Button btn_open;
    @FXML
    private Button btn_search, btn_clear;
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
    private TextField txt_search, txt_recordName;
    @FXML
    private WebView webView;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setUi();

        setUpSearchEmployee();
        setButtonActions();
        setTextFieldsAction();
    }

    void setUi() {
        Icons.getInstance().getPDFImage(btn_open);
    }

    void setButtonActions() {
        btn_open.setOnAction(_ -> exportToPDF());
        btn_clear.setOnAction(_ -> {
            txt_search.clear();
            txt_nationalID.clear();
            txt_name.clear();
            txt_payID.clear();
            txt_month.clear();
            txt_management.clear();
        });
    }

    void setTextFieldsAction() {
        SmartSearchHelper.bind(
                txt_month,
                () -> vocabService.getEmployeeMonthsReview(txt_nationalID.getText()),
                SearchDialog.builder(String.class)
                        .title("اختر الشهر")
                        .column("التاريخ", s -> s == null ? "" : s)
                        .searchPlaceholder("اكتب جزءاً من التاريخ..."),
                selectedMonth -> vocabService.downloadMainReviewReport(txt_nationalID.getText(), selectedMonth, webView, txt_name.getText()),

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
                selectedMonth -> vocabService.downloadCustomReviewReport(txt_nationalID.getText(), selectedMonth, txt_month.getText(), webView, txt_name.getText()),

                SmartSearchHelper.FieldBind.of(txt_recordName, x -> x)
        );
    }

    void setUpSearchEmployee() {
        SmartSearchHelper.bind(
                txt_search, btn_search,
                () -> vocabService.searchEmployee(txt_search.getText()),
                SearchDialog.builder(SearchEmp.class)
                        .title("بحث عن موظف")
                        .column("رقم قومى", SearchEmp::getNational_id)
                        .column("رقم موظف", SearchEmp::getPay_id)
                        .column("الاسم", SearchEmp::getEmp_name),
                _ -> {
                },
                SmartSearchHelper.FieldBind.of(txt_nationalID, SearchEmp::getNational_id),
                SmartSearchHelper.FieldBind.of(txt_name, SearchEmp::getEmp_name),
                SmartSearchHelper.FieldBind.of(txt_payID, SearchEmp::getPay_id)
        );
    }


    private void exportToPDF() {

        try {
            if (txt_nationalID.getText().isEmpty() || txt_nationalID.getText().length() != 14) {
                SAFNotification.warning("يجب إدخال الرقم القومى او البحث عن قيمة أولا");
                return;
            }


            vocabService.downloadMainReviewReport(txt_nationalID.getText(), txt_month.getText(), webView, txt_name.getText());


        } catch (Exception e) {
            SAFNotification.error("حدث خطأ أثناء التحميل: " + e.getMessage());

        }
    }


}
