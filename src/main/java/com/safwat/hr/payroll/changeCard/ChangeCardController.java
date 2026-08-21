package com.safwat.hr.payroll.changeCard;

import com.safwat.hr.notification.model.HRNotification;
import com.safwat.hr.notification.service.NotificationService;
import com.safwat.hr.payroll.changeCard.service.PayrollChangeService;
import com.safwat.hr.payroll.dto.ChangeCardView;
import com.safwat.hr.payroll.dto.SearchEmp;
import com.safwat.hr.shared.PayrollRequest;
import com.safwat.hr.shared.util.DateUtils;
import com.safwat.hr.ui.controls.*;
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

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class ChangeCardController implements Initializable {
    private final PayrollChangeService changeService = new PayrollChangeService();
    private final ObservableList<changeCardResult> resultList = FXCollections.observableArrayList();
    private TableColumn<changeCardResult, Void> colActions;
    @FXML
    private Button btn_clear, btn_pdf, btn_search, btn_view;
    @FXML
    private TextField txt_empCode, txt_empName, txt_nationalID, txt_searchValue;
    @FXML
    private TextField txt_startMonth, txt_endMonth;
    @FXML
    private Label pathLabel;
    @FXML
    private WebView webView;
    @FXML
    private CheckBox chk_showAction;
    @FXML
    private TableView<changeCardResult> resultTable;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setView();
        pathLabel.setVisible(false);

        setButtonsAction();
        setTextFieldsAction();
        setupTable();
    }

    /**
     * use to setting ui component
     */
    void setView() {
        SAFTextField.apply(txt_empCode, txt_empName, txt_nationalID, txt_searchValue, txt_startMonth, txt_endMonth);
        SAFButton.flat(true, btn_clear, btn_search, btn_view);
        Icons.getInstance().getPDFImage(btn_pdf);

        SAFTooltip.install(btn_pdf, "استخراج او عرض بطافة اجر الاشتراك");
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
        resultTable.getItems().clear();
        txt_searchValue.clear();
        txt_empCode.clear();
        txt_empName.clear();
        txt_nationalID.clear();
        txt_startMonth.clear();
        txt_endMonth.clear();
        pathLabel.setText("");
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

        TableColumn<changeCardResult, Boolean> colSelected = new TableColumn<>("*");
        colSelected.setCellValueFactory(new PropertyValueFactory<>("selected"));
        colSelected.setCellFactory(CheckBoxTableCell.forTableColumn(colSelected));
        colSelected.setEditable(true);
        colSelected.setPrefWidth(80);

        TableColumn<changeCardResult, String> colMonth = new TableColumn<>("الشهر");
        colMonth.setCellValueFactory(new PropertyValueFactory<>("month"));
        colMonth.setEditable(false);
        colMonth.setPrefWidth(120);

        TableColumn<changeCardResult, String> colValue = new TableColumn<>("اجر الاشتراك");
        colValue.setCellValueFactory(new PropertyValueFactory<>("value"));
        colValue.setEditable(false);
        colValue.setPrefWidth(120);

        TableColumn<changeCardResult, String> colNotes = new TableColumn<>("الملاحظات");
        colNotes.setCellValueFactory(new PropertyValueFactory<>("notes"));
        colNotes.setCellFactory(TextFieldTableCell.forTableColumn());
        colNotes.setEditable(true);
        colNotes.setPrefWidth(300);

        colNotes.setOnEditCommit(event -> {
            changeCardResult row = event.getRowValue();
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
                    changeCardResult row = getTableView().getItems().get(getIndex());
                    if (row.isSelected()) {
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
                    changeCardResult row = getTableView().getItems().get(getIndex());
                    if (row.isSelected()) {
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

        colActions.setVisible(false);

        resultTable.getColumns().addAll(colSelected, colMonth, colValue, colNotes, colActions);

        resultTable.setEditable(true);
    }

    private int updateNote(changeCardResult row) {
        PayrollRequest request = PayrollRequest.builder().build();
        request.setNationalId(txt_nationalID.getText());

        request.setStartDate(DateUtils.fromArabicMonthYearFlexible(row.getMonth()));
        request.setNote(row.getNotes());
        return changeService.updateNote(request);
    }

    private int deleteOneEmployeeRecord(changeCardResult row) {
        PayrollRequest request = PayrollRequest.builder().build();
        request.setNationalId(txt_nationalID.getText());

        request.setStartDate(DateUtils.fromArabicMonthYearFlexible(row.getMonth()));

        return changeService.deleteOneRecord(request);
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
        List<SearchEmp> data = changeService.searchInEmployee(request).getData();

        if (data.size() == 1) {
            txt_nationalID.setText(data.getFirst().getNational_id());
            txt_empCode.setText(data.getFirst().getPay_id());
            txt_empName.setText(data.getFirst().getEmp_name());
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
     * get employee change card data
     */
    private void getEmployeeData() {
        if (txt_nationalID.getText().isEmpty() || txt_nationalID.getText().length() != 14) {
            SAFNotification.warning("يجب إدخال الرقم القومى او البحث عن قيمة أولا");
            return;
        }

        try {
            showResultTable();
            PayrollRequest request = PayrollRequest.builder().build();
            request.setNationalId(txt_nationalID.getText());
            request.setStartDate(DateUtils.getFirstDayOfMonth(txt_startMonth.getText()));
            request.setEndDate(DateUtils.getLastDayOfMonth(txt_endMonth.getText()));

            ChangeCardView data = changeService.getChangeCardData(request).getData();

            if (data == null) {
                SAFNotification.warning("لا توجد بيانات للعرض");
                return;
            }

            // ====== تعبئة الجدول ======
            resultList.clear();
            List<Object[]> subData = data.rows();
            for (Object[] row : subData) {
                changeCardResult result = new changeCardResult(
                        (String) row[0],
                        (String) row[1],
                        row[2] != null ? row[2].toString() : ""
                );
                resultList.add(result);
            }

            SAFNotification.success("تم تحميل " + resultList.size() + " سجل بنجاح");

        } catch (Exception e) {
            SAFNotification.error("حدث خطأ: " + e.getMessage());

        }
    }

    private void exportToPDF() {

        try {
            if (txt_nationalID.getText().isEmpty() || txt_nationalID.getText().length() != 14) {
                SAFNotification.warning("يجب إدخال الرقم القومى او البحث عن قيمة أولا");
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

            String fileName = "بطاقة اجر الاشتراك_" + System.currentTimeMillis() + ".pdf";

            String workingDir = System.getProperty("user.dir");

            Path tempDownloadsDir = Paths.get(workingDir, "temp_downloads");
            if (!Files.exists(tempDownloadsDir)) {
                Files.createDirectories(tempDownloadsDir);
            }

            Path targetPath = tempDownloadsDir.resolve(fileName);
            pathLabel.setText(targetPath.toString());
            SAFNotification.success("جاري تحميل الملف...");

            boolean success = changeService.downloadChangeCardPDF(request, targetPath);

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
                        .title("بطاقة اجر الاشتراك")
                        .message(txt_empName.getText())
                        .messageBody("تم الانتهاء من تحميل التقرير.")
                        .file(targetPath.toString())
                        .sender("system")
                        .build()
        );

        SAFNotification.withAction(targetPath.toString(), targetPath.toFile());

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

    public class changeCardResult {
        private final BooleanProperty selected;
        private final StringProperty month;
        private final StringProperty value;
        private final StringProperty notes;

        public changeCardResult(String month, String value, String notes) {
            this.selected = new SimpleBooleanProperty(false);
            this.month = new SimpleStringProperty(month);
            this.value = new SimpleStringProperty(value);
            this.notes = new SimpleStringProperty(notes);
        }

        public boolean isSelected() {
            return selected.get();
        }

        public void setSelected(boolean selected) {
            this.selected.set(selected);
        }

        public BooleanProperty selectedProperty() {
            return selected;
        }

        public String getMonth() {
            return month.get();
        }

        public void setMonth(String month) {
            this.month.set(month);
        }

        public StringProperty monthProperty() {
            return month;
        }

        public String getValue() {
            return value.get();
        }

        public void setValue(String value) {
            this.value.set(value);
        }

        public StringProperty valueProperty() {
            return value;
        }

        public String getNotes() {
            return notes.get();
        }

        public void setNotes(String notes) {
            this.notes.set(notes);
        }

        public StringProperty notesProperty() {
            return notes;
        }
    }
}