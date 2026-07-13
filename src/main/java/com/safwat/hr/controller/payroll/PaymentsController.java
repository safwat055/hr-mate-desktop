package com.safwat.hr.controller.payroll;

import com.safwat.hr.service.payroll.PayrollPaymentsService;
import com.safwat.hr.service.payroll.dto.DTO;
import com.safwat.hr.service.payroll.dto.PayrollRequest;
import com.safwat.hr.service.payroll.dto.SearchEmp;
import com.safwat.hr.shared.util.DateUtils;
import com.safwat.hr.ui.controls.HRButton;
import com.safwat.hr.ui.controls.HRNotification;
import com.safwat.hr.ui.controls.HRTextField;
import com.safwat.hr.ui.icons.Icons;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * this is controller to payments view
 */
public class PaymentsController implements Initializable {
    private final ObservableList<PaymentsResult> resultList = FXCollections.observableArrayList();
    @FXML
    private Button btn_clear;
    @FXML
    private Button btn_pdf;
    @FXML
    private Button btn_save;
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
    private PayrollPaymentsService paymentsService = new PayrollPaymentsService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setView();
        setButtonsAction();
        setupTable();
    }

    /**
     * use to setting ui component
     */
    void setView() {
        HRTextField.apply(txt_empCode, txt_empName, txt_nationalID, txt_searchValue, txt_startMonth, txt_endMonth);
        HRButton.flat(true, btn_clear, btn_search, btn_view);
        Icons.getInstance().getPDFImage(btn_pdf);
        Icons.getInstance().getSaveImage(btn_save);
    }

    /**
     * Use To Set Buttons Actions
     */
    void setButtonsAction() {
        btn_search.setOnAction(_ -> searchEmployee());
        btn_view.setOnAction(_ -> getEmployeeData());
        //btn_pdf.setOnAction(_ -> exportToPDF());
        btn_clear.setOnAction(_ -> clear());
        btn_save.setOnAction(_ -> saveNotes());

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

    /**
     *
     */
    void saveNotes() {
        HRNotification.info("ستتم الاضافة في الاصدارت المستقبلية");
    }

    /**
     *
     */
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
            HRNotification.info("تم تحديث الملاحظات");
        });
        table_payments.setStyle("""
                    -fx-font-family: "DejaVu Sans";
                    -fx-font-size: 13px;
                """);

        boolean a = table_payments.getColumns().addAll(colSelected, colMonth, colGroup, colTotal, colTax, colStampTax, colNet, colDescription, colNote);
        table_payments.setEditable(true);

    }

    /**
     *
     */
    private void searchEmployee() {
        if (txt_searchValue.getText().isEmpty()) {
            HRNotification.error("ادخل قيمة للبحث لا تقل عن حرفين او رقمين");
            return;
        }
        PayrollRequest request = new PayrollRequest();
        request.setSearchValue(txt_searchValue.getText());
        List<SearchEmp> data = paymentsService.searchInEmployees(request).getData();

        if (data.size() == 1) {
            txt_nationalID.setText(data.getFirst().getNational_id());
            txt_empCode.setText(data.getFirst().getPay_id());
            txt_empName.setText(data.getFirst().getEmp_name());
            HRNotification.success("تم العثور على بيانات");
        }
    }

    /**
     * use to get employee payments data in view
     */
    private void getEmployeeData() {
        if (txt_nationalID.getText().isEmpty() || txt_nationalID.getText().length() != 14) {
            HRNotification.warning("يجب ادخال الرقم القومى او البحث عن قيمة اولا");
            return;
        }

        try {
            PayrollRequest request = new PayrollRequest();
            request.setNationalId(txt_nationalID.getText());
            request.setStartDate(DateUtils.getFirstDayOfMonth(txt_startMonth.getText()));
            request.setEndDate(DateUtils.getLastDayOfMonth(txt_endMonth.getText()));

            DTO.PaymentsView data = paymentsService.getPaymentsData(request).getData();
            if (data == null || data.rows().isEmpty()) {
                HRNotification.warning("لا توجد بيانات للعرض");
                return;
            }
            resultList.clear();
            List<Object[]> subData = data.rows();
            for (Object[] row : subData) {
                PaymentsResult result = new PaymentsResult(
                        (String) row[0],
                        row[1].toString(),
                        (String) row[2],
                        (String) row[3],
                        (String) row[4],
                        (String) row[5],
                        (String) row[6],
                        (String) row[7]
                );
                System.out.println(row[1].toString());
                resultList.add(result);
            }
            HRNotification.success("تم تحميل " + resultList.size() + " سجل بنجاح");
        } catch (Exception e) {
            HRNotification.error("حدث خطأ: " + e.getMessage());
            e.printStackTrace();

        }
    }


    public class PaymentsResult {
        private final BooleanProperty selected;
        private final StringProperty month;
        private final StringProperty payGroup;
        private final StringProperty total;
        private final StringProperty tax;
        private final StringProperty stampTax;
        private final StringProperty net;
        private final StringProperty description;
        private final StringProperty note;

        public PaymentsResult(String month, String payGroup, String total, String tax,
                              String stampTax, String net, String description, String note) {
            this.selected = new SimpleBooleanProperty(false);
            this.month = new SimpleStringProperty(month);
            this.payGroup = new SimpleStringProperty(payGroup);
            this.total = new SimpleStringProperty(total);
            this.tax = new SimpleStringProperty(tax);
            this.stampTax = new SimpleStringProperty(stampTax);
            this.net = new SimpleStringProperty(net);
            this.description = new SimpleStringProperty(description);
            this.note = new SimpleStringProperty(note);
        }

        // Getters للقيم النصية العادية
        public boolean getSelected() {
            return selected.get();
        }

        // Setters
        public void setSelected(boolean selected) {
            this.selected.set(selected);
        }

        public String getMonth() {
            return month.get();
        }

        public void setMonth(String month) {
            this.month.set(month);
        }

        public String getPayGroup() {
            return payGroup.get();
        }

        public void setPayGroup(String payGroup) {
            this.payGroup.set(payGroup);
        }

        public String getTotal() {
            return total.get();
        }

        public void setTotal(String total) {
            this.total.set(total);
        }

        public String getTax() {
            return tax.get();
        }

        public void setTax(String tax) {
            this.tax.set(tax);
        }

        public String getStampTax() {
            return stampTax.get();
        }

        public void setStampTax(String stampTax) {
            this.stampTax.set(stampTax);
        }

        public String getNet() {
            return net.get();
        }

        public void setNet(String net) {
            this.net.set(net);
        }

        public String getDescription() {
            return description.get();
        }

        public void setDescription(String description) {
            this.description.set(description);
        }

        public String getNote() {
            return note.get();
        }

        public void setNote(String note) {
            this.note.set(note);
        }

        // Property getters (التي يحتاجها PropertyValueFactory)
        public BooleanProperty selectedProperty() {
            return selected;
        }

        public StringProperty monthProperty() {
            return month;
        }

        public StringProperty payGroupProperty() {
            return payGroup;
        }


        public StringProperty totalProperty() {
            return total;
        }

        public StringProperty taxProperty() {
            return tax;
        }

        public StringProperty stampTaxProperty() {
            return stampTax;
        }

        public StringProperty netProperty() {
            return net;
        }

        public StringProperty descriptionProperty() {
            return description;
        }

        public StringProperty noteProperty() {
            return note;
        }

        public void setNotes(String notes) {
            this.note.set(notes);
        }
    }
}
