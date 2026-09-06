package com.safwat.hr.controller.payroll.changeCard;

import com.safwat.hr.controller.payroll.changeCard.service.ChangeCardResult;
import com.safwat.hr.controller.payroll.changeCard.service.PayrollChangeService;
import com.safwat.hr.controller.payroll.payrollApi.dto.SearchEmp;
import com.safwat.hr.shared.ui.SearchDialog;
import com.safwat.hr.shared.ui.SmartSearchHelper;
import com.safwat.hr.shared.util.DateUtils;
import com.safwat.hr.ui.controls.SAFDialog;
import com.safwat.hr.ui.controls.SAFNotification;
import com.safwat.hr.ui.controls.SAFTextField;
import com.safwat.hr.ui.controls.SAFTooltip;
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
import javafx.scene.web.WebView;

import java.net.URL;
import java.util.ResourceBundle;

public class ChangeCardController implements Initializable {
    private final PayrollChangeService changeService = new PayrollChangeService();
    private final ObservableList<ChangeCardResult> resultList = FXCollections.observableArrayList();
    private TableColumn<ChangeCardResult, Void> colActions;
    @FXML
    private Button btn_clear, btn_pdf, btn_search, btn_view;
    @FXML
    private TextField txt_empCode, txt_empName, txt_nationalID, txt_searchValue;
    @FXML
    private TextField txt_startMonth, txt_endMonth;

    @FXML
    private WebView webView;
    @FXML
    private CheckBox chk_showAction;
    @FXML
    private TableView<ChangeCardResult> resultTable;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setView();
        setUpSearchEmployee();
        setTextFieldsAction();


        setButtonsAction();

        setupTable();
    }

    /**
     * use to setting ui component
     */
    void setView() {
        SAFTextField.apply(txt_empCode, txt_empName, txt_nationalID, txt_searchValue, txt_startMonth, txt_endMonth);

        Icons.getInstance().getPDFImage(btn_pdf);

        SAFTooltip.install(btn_pdf, "استخراج او عرض بطافة اجر الاشتراك");
    }

    /**
     * Use To Set Buttons Actions
     */
    void setButtonsAction() {

        btn_view.setOnAction(_ -> getEmployeeDataView());
        btn_pdf.setOnAction(_ -> exportToPDF());
        btn_clear.setOnAction(_ -> clear());
        chk_showAction.selectedProperty().addListener((_, _, newValue) -> colActions.setVisible(newValue));


    }


    /**
     * use to clear all components in view
     */
    void clear() {
        resultList.clear();
        resultTable.getItems().clear();
        txt_searchValue.clear();
        txt_empCode.clear();
        txt_empName.clear();
        txt_nationalID.clear();
        txt_startMonth.clear();
        txt_endMonth.clear();

        webView.setManaged(false);
        resultTable.setManaged(false);
    }


    /**
     * use to set result table
     */
    @SuppressWarnings("unchecked")
    private void setupTable() {

        resultTable.setItems(resultList);

        resultTable.getColumns().clear();

        TableColumn<ChangeCardResult, Boolean> colSelected = new TableColumn<>("*");
        colSelected.setCellValueFactory(new PropertyValueFactory<>("selected"));
        colSelected.setCellFactory(CheckBoxTableCell.forTableColumn(colSelected));
        colSelected.setEditable(true);
        colSelected.setPrefWidth(80);

        TableColumn<ChangeCardResult, String> colMonth = new TableColumn<>("الشهر");
        colMonth.setCellValueFactory(new PropertyValueFactory<>("month"));
        colMonth.setEditable(false);
        colMonth.setPrefWidth(120);

        TableColumn<ChangeCardResult, String> colValue = new TableColumn<>("اجر الاشتراك");
        colValue.setCellValueFactory(new PropertyValueFactory<>("value"));
        colValue.setEditable(false);
        colValue.setPrefWidth(120);

        TableColumn<ChangeCardResult, String> colNotes = new TableColumn<>("الملاحظات");
        colNotes.setCellValueFactory(new PropertyValueFactory<>("notes"));
        colNotes.setCellFactory(TextFieldTableCell.forTableColumn());
        colNotes.setEditable(true);
        colNotes.setPrefWidth(300);

        colNotes.setOnEditCommit(event -> {
            ChangeCardResult row = event.getRowValue();
            row.setNotes(event.getNewValue());
            SAFNotification.info("تم تحديث الملاحظات");
        });

        colActions = new TableColumn<>("الإجراءات");
        colActions.setPrefWidth(180);

        colActions.setCellFactory(_ -> new TableCell<>() {

            private final Button btnEdit = new Button("تعديل");
            private final Button btnDelete = new Button("حذف");

            {
                btnEdit.getStyleClass().add("btn-primary");
                btnDelete.getStyleClass().add("btn-danger");

                btnEdit.setOnAction(_ -> {
                    ChangeCardResult row = getTableView().getItems().get(getIndex());
                    if (row.isSelected()) {
                        boolean a = SAFDialog.confirm("تأكيد", "هل تريد تعديل الملاحظة ؟");
                        if (a) {
                            updateNote(row);
                        }
                    } else {
                        SAFNotification.error("يجب تحديد الصف أولا ...");
                    }

                });

                btnDelete.setOnAction(_ -> {
                    ChangeCardResult row = getTableView().getItems().get(getIndex());
                    if (row.isSelected()) {
                        if (SAFDialog.confirm("تأكيد", "هل تريد حذف هذا القيد ؟")) {

                            deleteOneEmployeeRecord(row);

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
                    HBox box = new HBox(8, btnEdit, btnDelete);
                    box.setAlignment(Pos.CENTER);
                    setGraphic(box);
                }
            }
        });

        colActions.setVisible(false);

        resultTable.getColumns().addAll(colSelected, colMonth, colValue, colNotes, colActions);

        resultTable.setEditable(true);
    }


    private void showWebView() {
        resultTable.getItems().clear();
        resultTable.setManaged(false);
        resultTable.setVisible(false);
        webView.setManaged(true);
        webView.setVisible(true);
    }

    private void showResultTable() {
        webView.setManaged(false);
        webView.setVisible(false);
        resultTable.setManaged(true);
        resultTable.setVisible(true);
    }

    void setTextFieldsAction() {
        SmartSearchHelper.bind(
                txt_startMonth, btn_view,
                () -> changeService.getEmployeeAvailableMonths(txt_nationalID.getText()),
                SearchDialog.builder(String.class)
                        .title("اختر الشهر")
                        .column("التاريخ", s -> s == null ? "" : s)
                        .searchPlaceholder("اكتب جزءاً من التاريخ..."),
                _ -> getEmployeeDataView(),

                SmartSearchHelper.FieldBind.of(txt_startMonth, date ->
                        DateUtils.toArabicMonthYear(DateUtils.getFirstDayOfMonth(date)))
        );

        SmartSearchHelper.bind(
                txt_endMonth, btn_view,
                () -> changeService.getEmployeeAvailableMonths(txt_nationalID.getText()),
                SearchDialog.builder(String.class)
                        .title("اختر الشهر")
                        .column("التاريخ", s -> s == null ? "" : s)
                        .searchPlaceholder("اكتب جزءاً من التاريخ..."),
                _ -> getEmployeeDataView(),

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
                () -> changeService.searchInEmployee(txt_searchValue.getText()),
                SearchDialog.builder(SearchEmp.class)
                        .title("بحث عن موظف")
                        .column("رقم قومى", SearchEmp::getNational_id)
                        .column("رقم موظف", SearchEmp::getPay_id)
                        .column("الاسم", SearchEmp::getEmp_name),
                _ -> {
                    getEmployeeDataView();
                },
                SmartSearchHelper.FieldBind.of(txt_nationalID, SearchEmp::getNational_id),
                SmartSearchHelper.FieldBind.of(txt_empName, SearchEmp::getEmp_name),
                SmartSearchHelper.FieldBind.of(txt_empCode, SearchEmp::getPay_id)
        );
    }

    /**
     * get employee change card data
     */
    private void getEmployeeDataView() {
        if (txt_nationalID.getText().isEmpty() || txt_nationalID.getText().length() != 14) {
            SAFNotification.warning("يجب إدخال الرقم القومى او البحث عن قيمة أولا");
            return;
        }
        resultList.clear();
        showResultTable();
        ObservableList<ChangeCardResult> data = changeService.getChangeCardData(txt_nationalID.getText(), txt_startMonth.getText(), txt_endMonth.getText());
        if (data == null) {
            SAFNotification.warning("لا توجد بيانات للعرض");
            return;
        }
        resultList.addAll(data);
    }

    private void exportToPDF() {

        if (txt_nationalID.getText().isEmpty() || txt_nationalID.getText().length() != 14) {
            SAFNotification.warning("يجب إدخال الرقم القومى او البحث عن قيمة أولا");
            return;
        }
        showWebView();
        changeService.downloadChangeCardPDF(
                txt_nationalID.getText(),
                txt_startMonth.getText(),
                txt_endMonth.getText(),
                txt_empName.getText(),
                webView);
    }

    private void updateNote(ChangeCardResult row) {
        changeService.updateNote(txt_nationalID.getText(), row.getMonth());
    }

    private void deleteOneEmployeeRecord(ChangeCardResult row) {
        changeService.deleteOneRecord(txt_nationalID.getText(), row.getMonth());
    }
}