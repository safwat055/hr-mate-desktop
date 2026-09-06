package com.safwat.hr.controller.payroll.payments;

import com.safwat.hr.controller.payroll.payments.service.PaymentsResult;
import com.safwat.hr.controller.payroll.payments.service.PayrollPaymentsService;
import com.safwat.hr.controller.payroll.payrollApi.dto.SearchEmp;
import com.safwat.hr.shared.PayrollRequest;
import com.safwat.hr.shared.ui.SearchDialog;
import com.safwat.hr.shared.ui.SmartSearchHelper;
import com.safwat.hr.shared.util.DateUtils;
import com.safwat.hr.ui.controls.SAFDialog;
import com.safwat.hr.ui.controls.SAFNotification;
import com.safwat.hr.ui.icons.Icons;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import static com.safwat.hr.shared.util.StringUtil.convertArabicToEnglishNumbers;

/**
 * this is controller to payments view
 */
@Slf4j
public class PaymentsController implements Initializable {
    private final ObservableList<PaymentsResult> resultList = FXCollections.observableArrayList();
    private final PayrollPaymentsService paymentsService = PayrollPaymentsService.getInstance();
    @FXML
    private Button btn_clear;
    @FXML
    private Button btn_pdf;

    @FXML
    private Button btn_search;
    @FXML
    private Button btn_view;
    @FXML
    private TableView<PaymentsResult> table_payments;
    @FXML
    private TextField txt_empCode;
    @FXML
    private TextField txt_empName;
    @FXML
    private TextField txt_endMonth;
    @FXML
    private TextField txt_nationalID;
    @FXML
    private TextField txt_searchValue;
    @FXML
    private TextField txt_startMonth;


    @FXML
    private CheckBox chk_showAction;
    private TableColumn<PaymentsResult, Void> colActions;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setUpSearchEmployee();
        setTextActions();
        setButtonsAction();
        setupTable();

    }


    /**
     * Use To Set Buttons Actions
     */
    void setButtonsAction() {

        Icons.getInstance().getPDFImage(btn_pdf);

        btn_view.setOnAction(_ -> getEmployeeData());
        btn_pdf.setOnAction(_ -> exportToPDF());
        btn_clear.setOnAction(_ -> clear());

        chk_showAction.selectedProperty().addListener((_, _, newValue) -> colActions.setVisible(newValue));

    }


    /**
     * use to clear all components in view
     */
    void clear() {
        resultList.clear();
        table_payments.getItems().clear();
        txt_searchValue.clear();
        txt_empCode.clear();
        txt_empName.clear();
        txt_nationalID.clear();
        txt_startMonth.clear();
        txt_endMonth.clear();


    }


    private void exportToPDF() {

        paymentsService.downloadPaymentsPDF(txt_nationalID.getText(), txt_startMonth.getText(), txt_endMonth.getText(), txt_empName.getText());

    }

    private void customExportToPDF(String month, String payGroup) {

        paymentsService.downLoadCustomReview(txt_nationalID.getText(), month, payGroup, txt_empName.getText());
    }

    private void compareExportToPDF(String month) {
        paymentsService.downloadComparePDF(month, txt_nationalID.getText(), txt_empName.getText());

    }


    /**
     *
     */
    @SuppressWarnings("unchecked")
    private void setupTable() {
        table_payments.setItems(resultList);
        table_payments.getColumns().clear();
        TableColumn<PaymentsResult, Boolean> colSelected = new TableColumn<>("*");
        colSelected.setCellValueFactory(new PropertyValueFactory<>("selected"));
        colSelected.setCellFactory(CheckBoxTableCell.forTableColumn(colSelected));
        colSelected.setEditable(true);
        colSelected.setPrefWidth(50);

        TableColumn<PaymentsResult, String> colMonth = new TableColumn<>("الشهر");
        colMonth.setCellValueFactory(new PropertyValueFactory<>("month"));
        colMonth.setEditable(false);
        colMonth.setPrefWidth(100);

        TableColumn<PaymentsResult, String> colGroup = new TableColumn<>("اسم المجموعة");
        colGroup.setCellValueFactory(new PropertyValueFactory<>("payGroup"));
        colGroup.setEditable(true);
        colGroup.setPrefWidth(240);

        TableColumn<PaymentsResult, String> colTotal = new TableColumn<>("الاجمالى");
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colTotal.setEditable(false);
        colTotal.setPrefWidth(100);

        TableColumn<PaymentsResult, String> colTax = new TableColumn<>("ضريبة دخل");
        colTax.setCellValueFactory(new PropertyValueFactory<>("tax"));
        colTax.setEditable(false);
        colTax.setPrefWidth(100);

        TableColumn<PaymentsResult, String> colStampTax = new TableColumn<>("ضريبة دمغة");
        colStampTax.setCellValueFactory(new PropertyValueFactory<>("stampTax"));
        colStampTax.setEditable(false);
        colStampTax.setPrefWidth(100);

        TableColumn<PaymentsResult, String> colNet = new TableColumn<>("صافى");
        colNet.setCellValueFactory(new PropertyValueFactory<>("net"));
        colNet.setEditable(false);
        colNet.setPrefWidth(100);

        TableColumn<PaymentsResult, String> colDescription = new TableColumn<>("وصف المجموعة");
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colDescription.setEditable(false);
        colDescription.setPrefWidth(150);

        TableColumn<PaymentsResult, String> colNote = new TableColumn<>("ملاحظات");
        // هذا هو الحل المهم:
        colNote.setCellValueFactory(cellData -> cellData.getValue().noteProperty());
        colNote.setCellFactory(TextFieldTableCell.forTableColumn());
        colNote.setEditable(true);
        colNote.setPrefWidth(150);

        colNote.setOnEditCommit(event -> {
            PaymentsResult row = event.getRowValue();
            row.setNotes(event.getNewValue());
            SAFNotification.info("تم تحديث الملاحظات");
        });

        colActions = new TableColumn<>("الإجراءات");
        //  colActions.setPrefWidth(180);

        colActions.setCellFactory(_ -> new TableCell<>() {

            private final Button btnEdit = new Button("تعديل");
            private final Button btnDelete = new Button("حذف");
            private final Button btnPDF = new Button("");
            private final Button btnComparePDF = new Button("ملاحظات");

            {
                btnEdit.getStyleClass().add("btn-primary");
                btnComparePDF.getStyleClass().add("btn-primary");
                btnDelete.getStyleClass().add("btn-danger");
                btnPDF.getStyleClass().add("btn-iconFlat");
                Icons.getInstance().getPDFImage(btnPDF);


                btnEdit.setOnAction(_ -> {
                    PaymentsResult row = getTableView().getItems().get(getIndex());
                    if (row.getSelected()) {
                        boolean a = SAFDialog.confirm("تأكيد", "هل تريد تعديل الملاحظة ؟");
                        if (a) {
                            int x = updateNote(row);
                            SAFNotification.success("تم تعديل عدد..." + x + " ملاحظة");
                        }
                    } else {
                        SAFNotification.error("يجب تحديد الصف أولا ...");
                    }

                });

                btnPDF.setOnAction(_ -> {
                    PaymentsResult row = getTableView().getItems().get(getIndex());
                    customExportToPDF(row.getMonth(), row.getPayGroup());

                });

                btnComparePDF.setOnAction(_ -> {

                    PaymentsResult row = getTableView().getItems().get(getIndex());
                    if (!row.getPayGroup().contains("رئيسية")) {
                        SAFNotification.error("الملاحظات للصرفية الرئيسية فقط");
                        return;
                    }
                    compareExportToPDF(row.getMonth());

                });

                btnDelete.setOnAction(_ -> {
                    PaymentsResult row = getTableView().getItems().get(getIndex());
                    if (row.getSelected()) {
                        if (SAFDialog.confirm("تأكيد", "هل تريد حذف هذا القيد ؟")) {

                            int a = deleteOneEmployeeRecord(row);
                            SAFNotification.success("تم حذف " + a + " قيد");
                        }
                    } else {
                        SAFNotification.error("يجب تحديد الصف أولا ...");
                    }

                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                } else {
                    HBox box = new HBox(8, btnPDF, btnEdit, btnComparePDF, btnDelete);
                    box.setAlignment(Pos.CENTER);
                    setGraphic(box);
                }
            }
        });

        colActions.setVisible(false);
        boolean _ = table_payments.getColumns().addAll(colSelected, colMonth, colGroup, colTotal, colTax, colStampTax, colNet, colDescription, colNote, colActions);

        table_payments.setEditable(true);

    }

    void setTextActions() {
        SmartSearchHelper.bind(
                txt_startMonth, btn_view,
                () -> paymentsService.getEmployeeMonths(txt_nationalID.getText()),
                SearchDialog.builder(String.class)
                        .title("اختر الشهر")
                        .column("التاريخ", s -> s == null ? "" : s)
                        .searchPlaceholder("اكتب جزءاً من التاريخ..."),
                _ -> {
                    getEmployeeData();
                },
                SmartSearchHelper.FieldBind.of(txt_startMonth, date ->
                        DateUtils.toArabicMonthYear(DateUtils.getFirstDayOfMonth(date)))
        );
        SmartSearchHelper.bind(
                txt_endMonth, btn_view,
                () -> paymentsService.getEmployeeMonths(txt_nationalID.getText()),
                SearchDialog.builder(String.class)
                        .title("اختر الشهر")
                        .column("التاريخ", s -> s == null ? "" : s)
                        .searchPlaceholder("اكتب جزءاً من التاريخ..."),
                _ -> {
                    getEmployeeData();
                },
                SmartSearchHelper.FieldBind.of(txt_endMonth, date ->
                        DateUtils.toArabicMonthYear(DateUtils.getFirstDayOfMonth(date)))
        );


    }

    /**
     *
     */
    void setUpSearchEmployee() {
        SmartSearchHelper.bind(
                txt_searchValue, btn_search,
                () -> paymentsService.searchEmployee(txt_searchValue.getText()),
                SearchDialog.builder(SearchEmp.class)
                        .title("بحث عن موظف")
                        .column("رقم قومى", SearchEmp::getNational_id)
                        .column("رقم موظف", SearchEmp::getPay_id)
                        .column("الاسم", SearchEmp::getEmp_name),
                _ -> {
                    getEmployeeData();
                },
                SmartSearchHelper.FieldBind.of(txt_nationalID, SearchEmp::getNational_id),
                SmartSearchHelper.FieldBind.of(txt_empName, SearchEmp::getEmp_name),
                SmartSearchHelper.FieldBind.of(txt_empCode, SearchEmp::getPay_id)
        );
    }

    /**
     * use to get employee payments data in view
     */
    private void getEmployeeData() {

        resultList.clear();
        List<PaymentsResult> data = paymentsService.getPaymentsData(txt_nationalID.getText(), txt_startMonth.getText(), txt_endMonth.getText());
        resultList.addAll(data);
        SAFNotification.success("تم تحميل " + resultList.size() + " سجل بنجاح");

    }

    /**
     *
     * @param row .
     * @return .
     */
    private Integer updateNote(PaymentsResult row) {
        PayrollRequest request = PayrollRequest.builder().build();
        request.setNationalId(txt_nationalID.getText());
        request.setPayGroup(convertArabicToEnglishNumbers(row.getPayGroup()));
        request.setStartDate(DateUtils.fromArabicMonthYear(row.getMonth()));
        request.setNote(row.getNote());

        return paymentsService.updateEmployeeNote(request);
    }

    private Integer deleteOneEmployeeRecord(PaymentsResult row) {
        PayrollRequest request = PayrollRequest.builder().build();
        request.setNationalId(txt_nationalID.getText());
        request.setPayGroup(convertArabicToEnglishNumbers(row.getPayGroup()));
        request.setStartDate(DateUtils.fromArabicMonthYear(row.getMonth()));

        return paymentsService.deleteOneEmployeeRecord(request);
    }


}
