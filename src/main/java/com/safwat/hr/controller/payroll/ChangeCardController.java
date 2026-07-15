package com.safwat.hr.controller.payroll;

import com.safwat.hr.service.payroll.PayrollChangeService;
import com.safwat.hr.service.payroll.dto.DTO;
import com.safwat.hr.service.payroll.dto.PayrollRequest;
import com.safwat.hr.service.payroll.dto.SearchEmp;
import com.safwat.hr.shared.util.DateUtils;
import com.safwat.hr.ui.controls.SAFButton;
import com.safwat.hr.ui.controls.SAFNotification;
import com.safwat.hr.ui.controls.SAFTextField;
import com.safwat.hr.ui.controls.SAFTooltip;
import com.safwat.hr.ui.icons.Icons;
import com.safwat.hr.ui.util.PDFView;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.web.WebView;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;

public class ChangeCardController implements Initializable {
    private final PayrollChangeService changeService = new PayrollChangeService();


    private final ObservableList<changeCardResult> resultList = FXCollections.observableArrayList();

    @FXML
    private Button btn_clear, btn_save, btn_pdf, btn_search, btn_view;
    @FXML
    private TextField txt_empCode, txt_empName, txt_nationalID, txt_searchValue;

    @FXML
    private TextField txt_startMonth, txt_endMonth;

    @FXML
    private RadioButton RB_open, RB_view;
    @FXML
    private WebView webView;

    @FXML
    private TableView<changeCardResult> resultTable;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setView();
        setButtonsAction();

        setupTable(); // إعداد الجدول
    }

    /**
     * use to setting ui component
     */
    void setView() {
        SAFTextField.apply(txt_empCode, txt_empName, txt_nationalID, txt_searchValue, txt_startMonth, txt_endMonth);
        SAFButton.flat(true, btn_clear, btn_search, btn_view);
        Icons.getInstance().getPDFImage(btn_pdf);

        Icons.getInstance().getSaveImage(btn_save);

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
        btn_save.setOnAction(_ -> saveNotes());

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
    }

    void saveNotes() {
        SAFNotification.info("ستتم الإضافة في الإصدارات المستقبلية");
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

        resultTable.getColumns().addAll(colSelected, colMonth, colValue, colNotes);

        resultTable.setEditable(true);
    }

    /**
     *
     */
    private void searchEmployee() {
        if (txt_searchValue.getText().isEmpty()) {
            SAFNotification.error("ادخل قيمة للبحث لا تقل عن حرفين او رقمين");
            return;
        }

        PayrollRequest request = new PayrollRequest();
        request.setSearchValue(txt_searchValue.getText());
        List<SearchEmp> data = changeService.searchInEmployee(request).getData();

        if (data.size() == 1) {
            txt_nationalID.setText(data.getFirst().getNational_id());
            txt_empCode.setText(data.getFirst().getPay_id());
            txt_empName.setText(data.getFirst().getEmp_name());
            SAFNotification.success("تم العثور على بيانات");
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
            PayrollRequest request = new PayrollRequest();
            request.setNationalId(txt_nationalID.getText());
            request.setStartDate(DateUtils.getFirstDayOfMonth(txt_startMonth.getText()));
            request.setEndDate(DateUtils.getLastDayOfMonth(txt_endMonth.getText()));

            DTO.ChangeCardView data = changeService.getChangeCardData(request).getData();

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

            PayrollRequest request = new PayrollRequest();

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

            SAFNotification.success("جاري تحميل الملف...");

            boolean success = changeService.downloadChangeCardPDF(request, targetPath);

            if (success) {
                if (RB_view.isSelected()) {
                    PDFView.showIN(targetPath.toString(), webView);
                    SAFNotification.success("تم عرض الملف بنجاح");
                } else {
                    File downloadedFile = targetPath.toFile();
                    SAFNotification.withAction("تم تحميل الملف بنجاح", downloadedFile);
                }

            } else {
                SAFNotification.error("فشل تحميل الملف");
            }

        } catch (Exception e) {
            SAFNotification.error("حدث خطأ أثناء التحميل: " + e.getMessage());

        }
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