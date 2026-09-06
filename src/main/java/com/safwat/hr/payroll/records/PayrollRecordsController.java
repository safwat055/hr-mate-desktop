package com.safwat.hr.payroll.records;

import com.safwat.hr.payroll.payrollApi.dto.SearchEmp;
import com.safwat.hr.payroll.payrollApi.dto.ViewMainRecordForRangeDate;
import com.safwat.hr.shared.ui.SearchDialog;
import com.safwat.hr.shared.ui.SmartSearchHelper;
import com.safwat.hr.shared.util.DateUtils;
import com.safwat.hr.ui.controls.SAFNotification;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

public class PayrollRecordsController implements Initializable {
    @FXML
    private Button btn_Note;

    @FXML
    private Button btn_PDF_Record;

    @FXML
    private Button btn_PDF_Review;

    @FXML
    private Button btn_PDF_Review2;

    @FXML
    private Button btn_Record;

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
    private TableView<ObservableList<String>> t_allownces;

    @FXML
    private TableView<ObservableList<String>> t_allownces2;

    @FXML
    private TableView<ObservableList<String>> t_deductions;

    @FXML
    private TableView<ObservableList<String>> t_deductions2;

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
    private TextField txt_endMonth;

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
    private TextField txt_startMonth;

    private PayrollRecordsService service = new PayrollRecordsService();

    /**
     * Called to initialize a controller after its root element has been
     * completely processed.
     *
     * @param location  The location used to resolve relative paths for the root object, or
     *                  {@code null} if the location is not known.
     * @param resources The resources used to localize the root object, or {@code null} if
     *                  the root object was not localized.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setUpSearchEmployee();
        setAvailableMonth();
        setButtonsActions();
    }

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
        SmartSearchHelper.bind(txt_month2,
                () -> service.getEmployeeMonthsReview(txt_id.getText()),
                val -> txt_month2.setText(
                        DateUtils.toArabicMonthYear(DateUtils.getFirstDayOfMonth(val))
                )
        );
        SmartSearchHelper.bind(txt_startMonth,
                () -> service.getEmployeeMonthsReview(txt_id.getText()),
                val -> txt_startMonth.setText(
                        DateUtils.toArabicMonthYear(DateUtils.getFirstDayOfMonth(val))
                )
        );

        SmartSearchHelper.bind(txt_monthRecord,
                () -> service.getMonthReviewPayments(txt_id.getText(), txt_month2.getText()),
                val -> txt_monthRecord.setText(
                        val
                )
        );


    }

    void setButtonsActions() {
        btn_Record.setOnAction(_ -> {
            if (txt_startMonth.getText().isEmpty() || txt_endMonth.getText().isEmpty()) {
                SAFNotification.warning("يجب تحديد فترة بداية ونهاية اولا");
                return;
            }
            ViewMainRecordForRangeDate data = service.getMainMonthRecords(txt_id.getText(), txt_startMonth.getText(), txt_endMonth.getText());
            fillMonthRecord(data);
        });

        btn_Note.setOnAction(_ -> {
            service.compareExportToPDF(txt_month.getText(), txt_id.getText());
        });
        btn_PDF_Review.setOnAction(_ -> {
            service.downloadMainReviewReport(txt_id.getText(), txt_month.getText());
        });

        btn_PDF_Review2.setOnAction(_ -> {
            service.downloadCustomReviewToPDF(txt_month2.getText(), txt_monthRecord.getText(), txt_id.getText());
        });

        btn_PDF_Record.setOnAction(_ -> {
            service.downloadRecord_129(txt_id.getText(), txt_startMonth.getText(), txt_endMonth.getText());
        });
    }

    void fillMonthRecord(ViewMainRecordForRangeDate data) {
        if (data == null) {
            return;
        }
        txt_degree.setText(data.degree());
        txt_management.setText(data.department());
        txt_basic_30_6.setText(data.basic30_6());
        txt_bank.setText(data.bank());
        txt_branch.setText(data.branch());
        TableUtils.fillTable(t_allownces, data.allowancesHeader(), data.allowancesValues(), false, 100.00);
        TableUtils.fillTable(t_deductions, data.deductionsHeader(), data.deductionsValues(), false, 100.00);
    }

    void setUpSearchEmployee() {
        SmartSearchHelper.bind(
                txt_search, btn_search,
                () -> service.searchEmployee(txt_search.getText()),
                SearchDialog.builder(SearchEmp.class)
                        .title("بحث عن موظف")
                        .column("رقم قومى", SearchEmp::getNational_id)
                        .column("رقم موظف", SearchEmp::getPay_id)
                        .column("الاسم", SearchEmp::getEmp_name),
                _ -> {
                },
                SmartSearchHelper.FieldBind.of(txt_id, SearchEmp::getNational_id),
                SmartSearchHelper.FieldBind.of(txt_name, SearchEmp::getEmp_name),
                SmartSearchHelper.FieldBind.of(txt_code, SearchEmp::getPay_id)
        );
    }


    @FXML
    void singleRecord(ActionEvent event) {

    }
}
