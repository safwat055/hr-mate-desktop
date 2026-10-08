package com.safwat.hr.controller.wages.ui;

import com.safwat.hr.controller.employee.dto.EmployeeSearchResult;
import com.safwat.hr.controller.leave.service.LeaveApiClient;
import com.safwat.hr.controller.wages.dto.EmployeeProfileDto;
import com.safwat.hr.controller.wages.dto.VariableWageDocumentDto;
import com.safwat.hr.controller.wages.dto.WageCardExportRequest;
import com.safwat.hr.controller.wages.dto.WageMonthResultDto;
import com.safwat.hr.network.ApiClient;
import com.safwat.hr.network.DownloadWithNotification;
import com.safwat.hr.shared.ui.SearchDialog;
import com.safwat.hr.shared.ui.SmartSearchHelper;
import com.safwat.hr.ui.TextFieldSetupHelper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * كنترولر موحَّد لشاشة الأجور المتغيرة بالكامل.
 *
 * <p>البحث عن الموظف بيتم بنفس طريقة شاشة الإجازات والبدلات:
 * بالاسم أو الرقم القومي أو رقم الموظف، ومن نتيجة البحث بيتختار الموظف.
 *
 * <p>لو المستخدم احتسب الدورة الأول، النتيجة ({@code lastCycleResult})
 * بتتبعت مع طلب التصدير، والباك ما يحسبهاش تاني.
 */
public class EmployeeWagesScreenController {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

    // ══ بيانات الموظف / البحث ══
    @FXML
    private TextField searchField;
    @FXML
    private Button clearButton, searchButton;
    @FXML
    private Label searchStatusLabel;
    @FXML
    private Label employeeNameLabel;
    @FXML
    private Label employeeNumberLabel;
    @FXML
    private Label employeeNationalIdLabel;

    // ══ تبويب المستندات ══
    @FXML
    private Button addButton;
    @FXML
    private TableView<VariableWageDocumentDto> documentsTable;
    @FXML
    private TableColumn<VariableWageDocumentDto, String> monthColumn;
    @FXML
    private TableColumn<VariableWageDocumentDto, String> nameColumn;
    @FXML
    private TableColumn<VariableWageDocumentDto, String> numberColumn;
    @FXML
    private TableColumn<VariableWageDocumentDto, String> totalColumn;
    @FXML
    private TableColumn<VariableWageDocumentDto, String> pensionColumn;
    @FXML
    private TableColumn<VariableWageDocumentDto, String> taxColumn;
    @FXML
    private TableColumn<VariableWageDocumentDto, Void> actionsColumn;

    // ══ تبويب المحرك ══
    @FXML
    private Button calculateButton;
    @FXML
    private Label cycleStatusLabel;
    @FXML
    private TableView<WageMonthResultDto> cycleTable;
    @FXML
    private TableColumn<WageMonthResultDto, String> cycleMonthColumn;
    @FXML
    private TableColumn<WageMonthResultDto, String> rawTotalColumn;
    @FXML
    private TableColumn<WageMonthResultDto, String> ceilingColumn;
    @FXML
    private TableColumn<WageMonthResultDto, String> pensionableColumn;
    @FXML
    private TableColumn<WageMonthResultDto, String> ceilingAppliedColumn;
    @FXML
    private TableColumn<WageMonthResultDto, String> sourceColumn;
    @FXML
    private TableColumn<WageMonthResultDto, Void> allowanceDetailsColumn;
    @FXML
    private TableColumn<WageMonthResultDto, Void> documentDetailsColumn;

    // ══ شريط التصدير ══
    @FXML
    private TextField fromField;
    @FXML
    private TextField toField;
    @FXML
    private Button exportButton;
    @FXML
    private Button exportForm5Button;
    @FXML
    private Button exportBreakdownButton;
    @FXML
    private Button exportAllowanceMatrixButton;
    @FXML
    private Label exportStatusLabel;

    // ── Services ──
    private final LeaveApiClient employeeSearchApi = new LeaveApiClient();

    // ── State ──
    private String currentNationalId;

    /**
     * آخر نتيجة احتساب — تُبعَت مع طلب التصدير إن وُجدت،
     * وتتصفّر عند اختيار موظف جديد.
     */
    private List<WageMonthResultDto> lastCycleResult;

    // ══════════════════════════════════════════════════════
    //  Initialize
    // ══════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        setupEmployeeSearch();
        clearButton.setOnAction(_ -> clearEmployee(""));

        TextFieldSetupHelper.setupDateFields(fromField, toField);

        // ── جدول المستندات ──
        monthColumn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().periodMonth().toString()));
        nameColumn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().documentName()));
        numberColumn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().documentNumber()));
        totalColumn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().totalAmount().toPlainString()));
        pensionColumn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().subjectToPension() ? "خاضع" : "غير خاضع"));
        taxColumn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().subjectToTax() ? "خاضع" : "غير خاضع"));
        addDocumentActionButtons();
        addButton.setOnAction(e -> onAddDocument());
        addButton.setDisable(true);

        // ── جدول المحرك ──
        cycleMonthColumn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().monthLabel()));
        rawTotalColumn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().rawTotal().toPlainString()));
        ceilingColumn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().pensionCeiling() == null
                        ? "—" : c.getValue().pensionCeiling().toPlainString()));
        pensionableColumn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().totalPensionableWage().toPlainString()));
        ceilingAppliedColumn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().ceilingApplied() ? "مقطوع" : "—"));
        sourceColumn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().sourceLabel()));
        addCycleActionButtons();
        calculateButton.setOnAction(e -> onCalculate());
        calculateButton.setDisable(true);

        // ── التصدير ──
        exportButton.setOnAction(e -> onExport());
        exportButton.setDisable(true);
        exportForm5Button.setOnAction(e -> onExportForm5());
        exportForm5Button.setDisable(true);
        exportBreakdownButton.setOnAction(e -> onExportBreakdown());
        exportBreakdownButton.setDisable(true);
        exportAllowanceMatrixButton.setOnAction(e -> onExportAllowanceMatrix());
        exportAllowanceMatrixButton.setDisable(true);
    }

    // ══════════════════════════════════════════════════════
    //  البحث عن الموظف (اسم / رقم قومي / رقم وظيفي)
    // ══════════════════════════════════════════════════════

    private void setupEmployeeSearch() {
        SearchDialog<EmployeeSearchResult> dialog = SearchDialog
                .builder(EmployeeSearchResult.class)
                .title("اختر موظفًا")
                .searchPlaceholder("اسم / رقم قومي / رقم وظيفي...")
                .column("الاسم", EmployeeSearchResult::fullName)
                .column("الرقم الوظيفي", EmployeeSearchResult::employeeNumber)
                .column("الرقم القومي", EmployeeSearchResult::nationalId);

        SmartSearchHelper.bind(
                searchField,searchButton,
                () -> {
                    String q = searchField.getText();
                    return q == null ? List.of() : employeeSearchApi.searchEmployees(q.trim());
                },
                dialog,
                this::onEmployeeSelected,
                SmartSearchHelper.FieldBind.of(searchField, EmployeeSearchResult::fullName));
    }

    private void onEmployeeSelected(EmployeeSearchResult emp) {
        loadEmployee(emp.nationalId());
    }

    /**
     * يحمّل بيانات الموظف بالرقم القومي. لو الموظف اتغير بيمسح الحالة القديمة.
     */
    private void loadEmployee(String nationalId) {
        if (nationalId == null || nationalId.isBlank()) return;
        try {
            String url = "/wages/employees/search?query="
                    + URLEncoder.encode(nationalId.trim(), StandardCharsets.UTF_8);
            var res = ApiClient.get(url, EmployeeProfileDto.class);
            if (!res.isSuccess() || res.getData() == null) {
                clearEmployee("تعذر تحميل بيانات الموظف");
                return;
            }
            searchStatusLabel.setText("");
            bindEmployee(res.getData());
        } catch (Exception ex) {
            clearEmployee("تعذر تحميل بيانات الموظف");
        }
    }

    private void bindEmployee(EmployeeProfileDto p) {
        currentNationalId = p.nationalId();
        lastCycleResult = null;
        searchField.setText(p.fullName());
        employeeNameLabel.setText(p.fullName());
        employeeNumberLabel.setText(p.employeeNumber());
        employeeNationalIdLabel.setText(p.nationalId());
        addButton.setDisable(false);
        calculateButton.setDisable(false);
        exportButton.setDisable(false);
        exportForm5Button.setDisable(false);
        exportBreakdownButton.setDisable(false);
        exportAllowanceMatrixButton.setDisable(false);
        cycleStatusLabel.setText("");
        exportStatusLabel.setText("");
        cycleTable.setItems(FXCollections.observableArrayList());
        refreshDocuments();
    }

    private void clearEmployee(String msg) {
        currentNationalId = null;
        lastCycleResult = null;
        searchStatusLabel.setText(msg);
        employeeNameLabel.setText("—");
        employeeNumberLabel.setText("—");
        employeeNationalIdLabel.setText("—");
        addButton.setDisable(true);
        calculateButton.setDisable(true);
        exportButton.setDisable(true);
        exportForm5Button.setDisable(true);
        exportBreakdownButton.setDisable(true);
        exportAllowanceMatrixButton.setDisable(true);
        documentsTable.setItems(FXCollections.observableArrayList());
        cycleTable.setItems(FXCollections.observableArrayList());
    }

    // ══════════════════════════════════════════════════════
    //  مستندات الأجور
    // ══════════════════════════════════════════════════════

    private void refreshDocuments() {
        if (currentNationalId == null) return;
        try {
            var res = ApiClient.get(
                    "/wages/documents?nationalId=" + currentNationalId,
                    VariableWageDocumentDto[].class);
            if (!res.isSuccess()) {
                alert(Alert.AlertType.ERROR, "تعذر تحميل المستندات: " + res.getMessage());
                return;
            }
            documentsTable.setItems(FXCollections.observableArrayList(List.of(res.getData())));
        } catch (Exception ex) {
            alert(Alert.AlertType.ERROR, "تعذر تحميل المستندات: " + ex.getMessage());
        }
    }

    private void onAddDocument() {
        if (currentNationalId == null) return;
        AddVariableWageDocumentController.open(currentNationalId)
                .ifPresent(created -> refreshDocuments());
    }

    private void onDeleteDocument(VariableWageDocumentDto row) {
        if (row == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "حذف مستند \"" + row.documentName() + "\" برقم شطب " + row.documentNumber() + "؟",
                ButtonType.YES, ButtonType.NO);
        confirm.showAndWait()
                .filter(bt -> bt == ButtonType.YES)
                .ifPresent(bt -> {
                    try {
                        ApiClient.delete("/wages/documents/" + row.id());
                        refreshDocuments();
                    } catch (Exception ex) {
                        alert(Alert.AlertType.ERROR, "تعذر الحذف: " + ex.getMessage());
                    }
                });
    }

    private void addDocumentActionButtons() {
        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button del = new Button("حذف");

            {
                del.setOnAction(e -> onDeleteDocument(getTableRow().getItem()));
            }

            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : new HBox(del));
            }
        });
    }

    // ══════════════════════════════════════════════════════
    //  محرك الأجور
    // ══════════════════════════════════════════════════════

    private void onCalculate() {
        if (currentNationalId == null) return;
        try {
            var res = ApiClient.get(
                    "/wages/engine/cycle?nationalId=" + currentNationalId,
                    WageMonthResultDto[].class);
            if (!res.isSuccess()) {
                cycleStatusLabel.setText("تعذر حساب الدورة: " + res.getMessage());
                return;
            }
            lastCycleResult = List.of(res.getData());
            cycleStatusLabel.setText("");
            cycleTable.setItems(FXCollections.observableArrayList(lastCycleResult));
        } catch (Exception ex) {
            cycleStatusLabel.setText("تعذر حساب الدورة: " + ex.getMessage());
        }
    }

    private void addCycleActionButtons() {
        allowanceDetailsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("تفاصيل البدلات");

            {
                btn.setOnAction(e -> {
                    WageMonthResultDto row = getTableRow().getItem();
                    if (row != null) CycleMonthDetailsController.openAllowances(row);
                });
            }

            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : new HBox(btn));
            }
        });

        documentDetailsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("تفاصيل المستندات");

            {
                btn.setOnAction(e -> {
                    WageMonthResultDto row = getTableRow().getItem();
                    if (row != null) CycleMonthDetailsController.openDocuments(row);
                });
            }

            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                WageMonthResultDto row = empty ? null : getTableRow().getItem();
                boolean hasDocs = row != null && !row.documentNotes().isEmpty();
                setGraphic(hasDocs ? new HBox(btn) : null);
            }
        });
    }

    // ══════════════════════════════════════════════════════
    //  تصدير PDF
    // ══════════════════════════════════════════════════════

    private void onExport() {
        exportPdf("/wages/card/export", "wage_card_",
                exportButton, "بطاقة الأجور");
    }

    private void onExportForm5() {
        exportPdf("/wages/form5/export", "insurance_form5_",
                exportForm5Button, "نموذج (5) - التأمينات");
    }

    private void onExportBreakdown() {
        exportPdf("/wages/breakdown/export", "wage_breakdown_",
                exportBreakdownButton, "بنود الأجر المتغير");
    }

    private void onExportAllowanceMatrix() {
        exportPdf("/wages/allowance-matrix/export", "allowance_matrix_",
                exportAllowanceMatrixButton, "مصفوفة البدلات");
    }

    /**
     * منطق مشترك لكل تصدير PDF من الشاشة.
     * الفرق بينهم الإند بوينت واسم الملف الافتراضي.
     */
    private void exportPdf(String endpoint,
                           String fileNamePrefix,
                           Button triggerButton,
                           String notificationTitle) {

        if (currentNationalId == null) return;

        String fromText = fromField.getText() == null ? "" : fromField.getText().trim();
        String toText = toField.getText() == null ? "" : toField.getText().trim();

        if (!fromText.isEmpty() && !isValidDate(fromText)) {
            setExportMsg("صيغة تاريخ «من» غير صحيحة (yyyy-MM-dd)", "#b00020");
            return;
        }
        if (!toText.isEmpty() && !isValidDate(toText)) {
            setExportMsg("صيغة تاريخ «إلى» غير صحيحة (yyyy-MM-dd)", "#b00020");
            return;
        }

        WageCardExportRequest req = new WageCardExportRequest(
                currentNationalId,
                fromText.isEmpty() ? null : LocalDate.parse(fromText, ISO),
                toText.isEmpty() ? null : LocalDate.parse(toText, ISO),
                lastCycleResult
        );

        String empName = employeeNameLabel.getText() != null
                ? employeeNameLabel.getText()
                : currentNationalId;

        String period = (!fromText.isEmpty() || !toText.isEmpty())
                ? " (" + (fromText.isEmpty() ? "..." : fromText)
                + " → " + (toText.isEmpty() ? "..." : toText) + ")"
                : "";

        setExportMsg("جاري التصدير...", "#555555");
        triggerButton.setDisable(true);

        DownloadWithNotification.downloadPdfToTempAndNotifyPost(
                endpoint,
                req,
                fileNamePrefix + currentNationalId,
                empName + period,
                notificationTitle,
                () -> {
                    triggerButton.setDisable(false);
                    setExportMsg("تم حفظ التقرير — راجع الإشعارات", "#1a7a1a");
                }
        );
    }

    // ══════════════════════════════════════════════════════
    //  فتح الشاشة على موظف محدد (من شاشة البدلات أو غيرها)
    // ══════════════════════════════════════════════════════

    public void setInitialNationalId(String nationalId) {
        if (nationalId == null || nationalId.isBlank()) return;
        loadEmployee(nationalId.trim());
    }

    // ══════════════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════════════

    private boolean isValidDate(String s) {
        try {
            LocalDate.parse(s, ISO);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private void setExportMsg(String msg, String color) {
        exportStatusLabel.setStyle("-fx-text-fill: " + color + ";");
        exportStatusLabel.setText(msg);
    }

    private void alert(Alert.AlertType type, String msg) {
        new Alert(type, msg).showAndWait();
    }
}