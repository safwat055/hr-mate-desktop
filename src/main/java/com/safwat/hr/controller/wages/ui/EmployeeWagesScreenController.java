package com.safwat.hr.controller.wages.ui;

import com.safwat.hr.controller.wages.dto.EmployeeProfileDto;
import com.safwat.hr.controller.wages.dto.VariableWageDocumentDto;
import com.safwat.hr.controller.wages.dto.WageCardExportRequest;
import com.safwat.hr.controller.wages.dto.WageMonthResultDto;
import com.safwat.hr.network.ApiClient;
import com.safwat.hr.network.DownloadWithNotification;
import com.safwat.hr.ui.TextFieldSetupHelper;
import com.safwat.hr.ui.icons.Icons;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * كنترولر موحَّد لشاشة الأجور المتغيرة بالكامل.
 *
 * <p>يدمج منطق 4 كنترولرز سابقة في ملف واحد:
 * <ul>
 *   <li>البحث عن الموظف وعرض بياناته</li>
 *   <li>جدول مستندات الأجور المتغيرة (CRUD)</li>
 *   <li>جدول نتيجة محرك الأجور + التصدير</li>
 * </ul>
 *
 * <p><b>تحسين التصدير:</b> لو المستخدم احتسب الدورة الأول، النتيجة
 * ({@code lastCycleResult}) بتتبعت مع طلب التصدير مباشرة بدون ما الباك
 * يحسب تاني — لو مش محتسبة يحسب الباك من جديد تلقائيًا. نفس المنطق
 * بيتطبق على تصدير بطاقة الأجور وتصدير نموذج (5) — الاتنين بيستخدموا
 * نفس {@link WageCardExportRequest} ونفس {@code lastCycleResult}.
 *
 * <h2>لازم تضيف في الـ FXML:</h2>
 * <pre>{@code
 *   <Button fx:id="exportAllowanceMatrixButton" disable="true"
 *           text="مصفوفة البدلات" style="-fx-font-weight: bold;"/>
 * }</pre>
 */
public class EmployeeWagesScreenController {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

    // ══ بيانات الموظف ══
    @FXML
    private TextField searchField;
    @FXML
    private Button searchButton, clearButton;
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
    private Button exportAllowanceMatrixButton;   // ⭐ جديد
    @FXML
    private Label exportStatusLabel;

    // ── State ──
    private String currentNationalId;

    /**
     * آخر نتيجة احتساب — تُبعَت مع طلب التصدير مباشرة إن وُجدت،
     * تُصفَّر عند البحث عن موظف جديد.
     */
    private List<WageMonthResultDto> lastCycleResult;

    // ══════════════════════════════════════════════════════
    //  Initialize
    // ══════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        // ── البحث ──
        searchButton.setOnAction(e -> onSearch());
        searchField.setOnAction(e -> onSearch());
        clearButton.setOnAction(_ -> clearEmployee(""));

        // ── أيقونات PDF ──
        Icons.getInstance().getPDFImage(exportButton);
        Icons.getInstance().getPDFImage(exportForm5Button);
        Icons.getInstance().getPDFImage(exportBreakdownButton);
        Icons.getInstance().getPDFImage(exportAllowanceMatrixButton);   // ⭐ جديد

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
        exportAllowanceMatrixButton.setOnAction(e -> onExportAllowanceMatrix());   // ⭐ جديد
        exportAllowanceMatrixButton.setDisable(true);
    }

    // ══════════════════════════════════════════════════════
    //  البحث عن الموظف
    // ══════════════════════════════════════════════════════

    private void onSearch() {
        String q = searchField.getText() == null ? "" : searchField.getText().trim();
        if (q.isEmpty()) {
            searchStatusLabel.setText("اكتب الرقم القومي أو رقم الموظف الأول");
            return;
        }
        try {
            var res = ApiClient.get("/wages/employees/search?query=" + q, EmployeeProfileDto.class);
            if (!res.isSuccess()) {
                clearEmployee("مفيش موظف بهذا الرقم");
                return;
            }
            searchStatusLabel.setText("");
            bindEmployee(res.getData());
        } catch (Exception ex) {
            clearEmployee("مفيش موظف بهذا الرقم");
        }
    }

    private void bindEmployee(EmployeeProfileDto p) {
        currentNationalId = p.nationalId();
        lastCycleResult = null;   // نتيجة قديمة لموظف سابق — تُصفَّر
        employeeNameLabel.setText(p.fullName());
        employeeNumberLabel.setText(p.employeeNumber());
        employeeNationalIdLabel.setText(p.nationalId());
        addButton.setDisable(false);
        calculateButton.setDisable(false);
        exportButton.setDisable(false);
        exportForm5Button.setDisable(false);
        exportBreakdownButton.setDisable(false);
        exportAllowanceMatrixButton.setDisable(false);   // ⭐ جديد
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
        exportAllowanceMatrixButton.setDisable(true);   // ⭐ جديد
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
    //  تصدير PDF — كل زر له endpoint خاص
    // ══════════════════════════════════════════════════════

    /**
     * تصدير بطاقة الأجور.
     */
    private void onExport() {
        exportPdf("/wages/card/export", "wage_card_",
                exportButton, "بطاقة الأجور");
    }

    /**
     * تصدير نموذج (5) — التأمينات.
     */
    private void onExportForm5() {
        exportPdf("/wages/form5/export", "insurance_form5_",
                exportForm5Button, "نموذج (5) - التأمينات");
    }

    /**
     * تصدير بنود الأجر المتغير شهر بشهر.
     */
    private void onExportBreakdown() {
        exportPdf("/wages/breakdown/export", "wage_breakdown_",
                exportBreakdownButton, "بنود الأجر المتغير");
    }

    /**
     * ⭐ تصدير مصفوفة البدلات — سنة لكل صفحة.
     */
    private void onExportAllowanceMatrix() {
        exportPdf("/wages/allowance-matrix/export", "allowance_matrix_",
                exportAllowanceMatrixButton, "مصفوفة البدلات");
    }

    /**
     * منطق مشترك لأي تصدير PDF من نفس الشاشة — الفرق بينهم بس الإند
     * بوينت واسم الملف الافتراضي. الاتنين بيبعتوا نفس {@code lastCycleResult}
     * لو موجودة عشان الباك ميحسبش الدورة تاني.
     *
     * <p><b>تنزيل + إشعار:</b> الملف يُحفظ في {@code temp_downloads/}
     * بدون فتح نافذة اختيار — والمستخدم يستلم إشعار فيه رابط الملف.</p>
     */
    private void exportPdf(String endpoint,
                           String fileNamePrefix,
                           Button triggerButton,
                           String notificationTitle) {

        if (currentNationalId == null) return;

        // ── 1. قراءة التواريخ ──
        String fromText = fromField.getText() == null ? "" : fromField.getText().trim();
        String toText = toField.getText() == null ? "" : toField.getText().trim();

        // ── 2. تحقق من الصيغة ──
        if (!fromText.isEmpty() && !isValidDate(fromText)) {
            setExportMsg("صيغة تاريخ «من» غير صحيحة (yyyy-MM-dd)", "#b00020");
            return;
        }
        if (!toText.isEmpty() && !isValidDate(toText)) {
            setExportMsg("صيغة تاريخ «إلى» غير صحيحة (yyyy-MM-dd)", "#b00020");
            return;
        }

        // ── 3. بناء الطلب ──
        //    نفس شكل الـ request لكل الـ endpoints (nationalId + from + to + precomputedMonths)
        WageCardExportRequest req = new WageCardExportRequest(
                currentNationalId,
                fromText.isEmpty() ? null : LocalDate.parse(fromText, ISO),
                toText.isEmpty() ? null : LocalDate.parse(toText, ISO),
                lastCycleResult
        );

        // ── 4. اسم الموظف للعرض في الإشعار ──
        String empName = employeeNameLabel.getText() != null
                ? employeeNameLabel.getText()
                : currentNationalId;

        // ── 5. فترة التصدير (للإشعار) ──
        String period = (!fromText.isEmpty() || !toText.isEmpty())
                ? " (" + (fromText.isEmpty() ? "..." : fromText)
                + " → " + (toText.isEmpty() ? "..." : toText) + ")"
                : "";

        // ── 6. حالة الواجهة ──
        setExportMsg("جاري التصدير...", "#555555");
        triggerButton.setDisable(true);

        // ── 7. التنزيل + الإشعار ──
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

    public void setInitialNationalId(String nationalId) {
        if (nationalId == null || nationalId.isBlank()) return;
        if (searchField != null) {
            searchField.setText(nationalId.trim());
        }
        onSearch();
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