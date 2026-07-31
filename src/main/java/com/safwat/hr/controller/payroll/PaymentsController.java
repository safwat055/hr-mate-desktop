package com.safwat.hr.controller.payroll;

import com.safwat.hr.notification.model.HRNotification;
import com.safwat.hr.notification.service.NotificationService;
import com.safwat.hr.service.payroll.PayrollPaymentsService;
import com.safwat.hr.service.payroll.dto.DTO;
import com.safwat.hr.service.payroll.dto.PayrollRequest;
import com.safwat.hr.service.payroll.dto.SearchEmp;
import com.safwat.hr.shared.util.DateUtils;
import com.safwat.hr.ui.controls.SAFButton;
import com.safwat.hr.ui.controls.SAFDialog;
import com.safwat.hr.ui.controls.SAFNotification;
import com.safwat.hr.ui.controls.SAFTextField;
import com.safwat.hr.ui.icons.Icons;
import com.safwat.hr.ui.util.PDFView;
import com.safwat.hr.ui.util.SearchDialog;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
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
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    private WebView webView;
    @FXML
    private Label pathLabel;
    @FXML
    private CheckBox chk_showAction;
    private TableColumn<PaymentsResult, Void> colActions;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setView();
        setButtonsAction();
        setTextFieldsAction();
        setupTable();
        pathLabel.setVisible(false);
    }

    /**
     * use to setting ui component
     */
    void setView() {
        webView.setManaged(false);
        table_payments.setManaged(false);
        SAFTextField.apply(txt_empCode, txt_empName, txt_nationalID, txt_searchValue, txt_startMonth, txt_endMonth);
        SAFButton.flat(true, btn_clear, btn_search, btn_view);
        Icons.getInstance().getPDFImage(btn_pdf);

    }

    /**
     * Use To Set Buttons Actions
     */
    void setButtonsAction() {
        btn_search.setOnAction(_ -> searchEmployee());
        btn_view.setOnAction(_ -> getEmployeeData());
        btn_pdf.setOnAction(_ -> exportToPDF());
        btn_clear.setOnAction(_ -> clear());

        chk_showAction.selectedProperty().addListener((_, _, newValue) -> colActions.setVisible(newValue));

    }


    void setTextFieldsAction() {
        txt_searchValue.setOnAction(_ -> searchEmployee());
        txt_nationalID.setOnAction(_ -> exportToPDF());

        txt_nationalID.textProperty().addListener(
                (_, _, _) -> pathLabel.setText("")
        );
        txt_startMonth.textProperty().addListener((_, _, _) -> pathLabel.setText(""));
        txt_endMonth.textProperty().addListener((_, _, _) -> pathLabel.setText(""));
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
        pathLabel.setText("");
        webView.setManaged(false);
        table_payments.setManaged(false);
    }

    private void exportToPDF() {

        try {
            if (txt_nationalID.getText().isEmpty() || txt_nationalID.getText().length() != 14) {
                SAFNotification.warning("يجب ادخال الرقم القومى او البحث عن قيمة اولا");
                return;
            }
            if (!pathLabel.getText().isEmpty()) {
                Path targetPath = Path.of(pathLabel.getText());

                openPDF(targetPath);

                return;
            }
            showWebView();
            PayrollRequest request = PayrollRequest.builder().build();

            request.setNationalId(txt_nationalID.getText());
            request.setStartDate(DateUtils.getFirstDayOfMonth(txt_startMonth.getText()));
            request.setEndDate(DateUtils.getLastDayOfMonth(txt_endMonth.getText()));
            request.setFormat("PDF");
            String fileName = "PAYMENTS_REPORT" + System.currentTimeMillis() + ".pdf";

            String workingDir = System.getProperty("user.dir");

            Path tempDownloadsDir = Paths.get(workingDir, "temp_downloads");
            if (!Files.exists(tempDownloadsDir)) {
                Files.createDirectories(tempDownloadsDir);
            }

            Path targetPath = tempDownloadsDir.resolve(fileName);
            pathLabel.setText(targetPath.toString());
            SAFNotification.success("جاري تحميل الملف...");

            boolean success = paymentsService.downloadPaymentsPDF(request, targetPath);

            if (success) {

                openPDF(targetPath);

            } else {

                SAFNotification.error("فشل تحميل الملف");
            }

        } catch (Exception e) {
            SAFNotification.error("حدث خطأ أثناء التحميل: " + e.getMessage());

        }
    }

    private void openPDF(Path targetPath) {

        showWebView();
        PDFView.showIN(targetPath.toString(), webView);
        NotificationService.getInstance().send(
                HRNotification.builder()
                        .type(HRNotification.NotificationType.SYSTEM)
                        .priority(HRNotification.Priority.NORMAL)
                        .title("تقرير صرفيات")
                        .message(txt_empName.getText())
                        .file(targetPath.toString())
                        .sender("system")
                        .build()
        );
    }

    private void showWebView() {
        table_payments.getItems().clear();
        table_payments.setManaged(false);
        table_payments.setVisible(false);
        webView.setManaged(true);
        webView.setVisible(true);
    }

    private void showPaymentsTable() {
        webView.setManaged(false);
        webView.setVisible(false);
        table_payments.setManaged(true);
        table_payments.setVisible(true);
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
        colActions.setPrefWidth(180);

        colActions.setCellFactory(_ -> new TableCell<>() {

            private final Button btnEdit = new Button("تعديل");
            private final Button btnDelete = new Button("حذف");

            {
                btnEdit.setStyle("""
                        -fx-background-color: #1976D2;
                        -fx-text-fill: white;
                        -fx-background-radius: 5;
                        -fx-cursor: hand;
                        """);

                btnDelete.setStyle("""
                        -fx-background-color: #D32F2F;
                        -fx-text-fill: white;
                        -fx-background-radius: 5;
                        -fx-cursor: hand;
                        """);

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
                    HBox box = new HBox(8, btnEdit, btnDelete);
                    box.setAlignment(Pos.CENTER);
                    setGraphic(box);
                }
            }
        });
        table_payments.setStyle("""
                    -fx-background-color: transparent;
                    -fx-font-family: "DejaVu Sans";
                    -fx-font-size: 13px;
                """);
        colActions.setVisible(false);
        boolean _ = table_payments.getColumns().addAll(colSelected, colMonth, colGroup, colTotal, colTax, colStampTax, colNet, colDescription, colNote, colActions);

        table_payments.setEditable(true);

    }

    /**
     *
     */
    private void searchEmployee() {
        if (txt_searchValue.getText().isEmpty()) {
            SAFNotification.error("ادخل قيمة للبحث لا تقل عن حرفين او رقمين");
            return;
        }
        PayrollRequest request = PayrollRequest.builder().build();
        request.setSearchValue(txt_searchValue.getText());
        List<SearchEmp> data = paymentsService.searchInEmployees(request).getData();

        if (data.size() == 1) {
            txt_nationalID.setText(data.getFirst().getNational_id());
            txt_empCode.setText(data.getFirst().getPay_id());
            txt_empName.setText(data.getFirst().getEmp_name());
            getEmployeeData();
            SAFNotification.success("تم العثور على بيانات");
        } else {
            List<Object[]> searchData = new ArrayList<>();
            for (SearchEmp e : data) {
                searchData.add(new Object[]{e.getNational_id(), e.getPay_id(), e.getEmp_name()});

            }
            Optional<Object[]> result = SearchDialog.builder().title("نتائج البحث")
                    .data(searchData)
                    .headers(new String[]{"رقم قومي", "رقم موظف", "اسم"})
                    .searchPlaceholder("ابحث للتصفية")
                    .show();
            result.ifPresent(row -> {
                txt_nationalID.setText(row[0].toString());
                txt_empCode.setText(row[1].toString());
                txt_empName.setText(row[2].toString());
            });

        }
    }

    /**
     * use to get employee payments data in view
     */
    private void getEmployeeData() {
        if (txt_nationalID.getText().isEmpty() || txt_nationalID.getText().length() != 14) {
            SAFNotification.warning("يجب ادخال الرقم القومى او البحث عن قيمة اولا");
            return;
        }

        try {
            //   table_payments.getColumns().clear();

            showPaymentsTable();

            PayrollRequest request = PayrollRequest.builder().build();
            request.setNationalId(txt_nationalID.getText());
            request.setStartDate(DateUtils.getFirstDayOfMonth(txt_startMonth.getText()));
            request.setEndDate(DateUtils.getLastDayOfMonth(txt_endMonth.getText()));

            DTO.PaymentsView data = paymentsService.getPaymentsData(request).getData();
            if (data == null || data.rows().isEmpty()) {
                SAFNotification.warning("لا توجد بيانات للعرض");
                return;
            }
            resultList.clear();
            List<Object[]> subData = data.rows();
            for (Object[] row : subData) {
                PaymentsResult result = new PaymentsResult(
                        (String) row[0],
                        (String) row[1],
                        (String) row[2],
                        (String) row[3],
                        (String) row[4],
                        (String) row[5],
                        (String) row[6],
                        (String) row[7]
                );

                resultList.add(result);
            }
            SAFNotification.success("تم تحميل " + resultList.size() + " سجل بنجاح");
        } catch (Exception e) {
            SAFNotification.error("حدث خطأ: " + e.getMessage());
            log.error(e.getMessage());

        }
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
        System.out.println(request.getNationalId());
        System.out.println(request.getPayGroup());
        System.out.println(request.getStartDate());
        System.out.println(request.getNote());
        return paymentsService.updateEmployeeNote(request);
    }

    private Integer deleteOneEmployeeRecord(PaymentsResult row) {
        PayrollRequest request = PayrollRequest.builder().build();
        request.setNationalId(txt_nationalID.getText());
        request.setPayGroup(convertArabicToEnglishNumbers(row.getPayGroup()));
        request.setStartDate(DateUtils.fromArabicMonthYear(row.getMonth()));

        return paymentsService.deleteOneEmployeeRecord(request);
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
