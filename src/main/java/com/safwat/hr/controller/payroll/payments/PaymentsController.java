package com.safwat.hr.controller.payroll.payments;

import com.safwat.hr.controller.payroll.payments.service.PaymentsResult;
import com.safwat.hr.controller.payroll.payments.service.PayrollPaymentsService;
import com.safwat.hr.controller.payroll.payrollApi.dto.EmployeeSearchResult;
import com.safwat.hr.shared.PayrollRequest;
import com.safwat.hr.shared.ui.SearchDialog;
import com.safwat.hr.shared.ui.SmartSearchHelper;
import com.safwat.hr.shared.util.DateUtils;
import com.safwat.hr.ui.controls.SAFDialog;
import com.safwat.hr.ui.controls.SAFNotification;
import com.safwat.hr.ui.icons.Icons;
import com.safwat.hr.ui.util.PDFView;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static com.safwat.hr.shared.util.StringUtil.convertArabicToEnglishNumbers;

/**
 * this is controller to payments view
 */
@Slf4j
public class PaymentsController implements Initializable {
    private final ObservableList<PaymentsResult> resultList = FXCollections.observableArrayList();
    private final PayrollPaymentsService paymentsService = PayrollPaymentsService.getInstance();

    @FXML
    private Button btn_back;
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

    @FXML
    private VBox resultBox;
    @FXML
    private WebView webView;

    private TableColumn<PaymentsResult, Void> colActions;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setUpSearchEmployee();
        setTextActions();
        setButtonsAction();
        setupTable();
        showForm();
    }

    /**
     * Use To Set Buttons Actions
     */
    void setButtonsAction() {
        Icons.getInstance().getPDFImage(btn_pdf);

        btn_view.setOnAction(_ -> getEmployeeData());
        btn_pdf.setOnAction(_ -> exportToPDF());
        btn_clear.setOnAction(_ -> clear());
        btn_back.setOnAction(_ -> showForm());

        chk_showAction.selectedProperty().addListener((_, _, newValue) -> colActions.setVisible(newValue));
    }

    /**
     * use to clear all components in view
     */
    void clear() {
        resultList.clear();
        txt_searchValue.clear();
        txt_empCode.clear();
        txt_empName.clear();
        txt_nationalID.clear();
        txt_startMonth.clear();
        txt_endMonth.clear();

        showForm();
    }

    // ═════════════════════════════════════════════
    //  حالة الواجهة (جدول / معاينة PDF)
    // ═════════════════════════════════════════════

    /**
     * يعرض الجدول ويخفي المعاينة، ويفضّي الـ WebView من الذاكرة
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
     * يعرض المعاينة ويخفي الجدول. البيانات بتفضل في الجدول عشان زر الرجوع يرجعها
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
    //  PDF
    // ═════════════════════════════════════════════

    private void exportToPDF() {
        showPdf(paymentsService.downloadPaymentsPDFToFile(
                txt_nationalID.getText(),
                txt_startMonth.getText(),
                txt_endMonth.getText(),
                txt_empName.getText()));
    }

    private void customExportToPDF(String month, String payGroup) {
        showPdf(paymentsService.downloadCustomReviewToFile(
                txt_nationalID.getText(),
                month,
                payGroup,
                txt_empName.getText()));
    }

    private void compareExportToPDF(String month) {
        showPdf(paymentsService.downloadComparePDFToFile(
                month,
                txt_nationalID.getText(),
                txt_empName.getText()));
    }

    /**
     * يستنى الملف يتحمّل، وبعدين يعرضه في المعاينة. المعاينة بتظهر بس لو الملف نجح.
     */
    private void showPdf(CompletableFuture<Path> request) {
        request.whenComplete((path, ex) -> Platform.runLater(() -> {
            if (ex != null) {
                SAFNotification.error("فشل إنشاء الملف: " + friendly(ex));
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

    // ═════════════════════════════════════════════
    //  الجدول
    // ═════════════════════════════════════════════

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
        table_payments.getColumns().addAll(colSelected, colMonth, colGroup, colTotal, colTax,
                colStampTax, colNet, colDescription, colNote, colActions);

        table_payments.setEditable(true);
    }

    // ═════════════════════════════════════════════
    //  البحث
    // ═════════════════════════════════════════════

    void setTextActions() {
        SmartSearchHelper.bind(
                txt_startMonth, btn_view,
                () -> paymentsService.getEmployeeMonths(txt_nationalID.getText()),
                SearchDialog.builder(String.class)
                        .title("اختر الشهر")
                        .column("التاريخ", s -> s == null ? "" : s)
                        .searchPlaceholder("اكتب جزءاً من التاريخ..."),
                _ -> getEmployeeData(),
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
                _ -> getEmployeeData(),
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
                SearchDialog.builder(EmployeeSearchResult.class)
                        .title("بحث عن موظف")
                        .column("رقم قومى", EmployeeSearchResult::getNational_id)
                        .column("رقم موظف", EmployeeSearchResult::getPay_id)
                        .column("الاسم", EmployeeSearchResult::getEmp_name)
                        .column("الوظيفة", EmployeeSearchResult::getJob)
                        .column("الادارة", EmployeeSearchResult::getPay_management)
                        .column("فئة التعيين", EmployeeSearchResult::getAssignment_class),
                _ -> getEmployeeData(),
                SmartSearchHelper.FieldBind.of(txt_nationalID, EmployeeSearchResult::getNational_id),
                SmartSearchHelper.FieldBind.of(txt_empName, EmployeeSearchResult::getEmp_name),
                SmartSearchHelper.FieldBind.of(txt_empCode, EmployeeSearchResult::getPay_id)
        );
    }

    /**
     * use to get employee payments data in view
     */
    private void getEmployeeData() {
        showForm();
        resultList.clear();
        List<PaymentsResult> data = paymentsService.getPaymentsData(
                txt_nationalID.getText(), txt_startMonth.getText(), txt_endMonth.getText());
        resultList.addAll(data);
        SAFNotification.success("تم تحميل " + resultList.size() + " سجل بنجاح");
    }

    // ═════════════════════════════════════════════
    //  عمليات الصفوف
    // ═════════════════════════════════════════════

    /**
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