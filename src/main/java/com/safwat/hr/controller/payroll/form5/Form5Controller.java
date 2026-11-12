package com.safwat.hr.controller.payroll.form5;

import com.safwat.hr.controller.payroll.form5.Form5Models.*;
import com.safwat.hr.notification.model.HRNotification;
import com.safwat.hr.notification.service.NotificationService;
import com.safwat.hr.shared.ui.SearchDialog;
import com.safwat.hr.shared.ui.SmartSearchHelper;
import com.safwat.hr.ui.controls.SAFNotification;
import com.safwat.hr.ui.util.PDFView;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.util.Callback;
import javafx.util.StringConverter;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.net.http.HttpTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Form5Controller implements Initializable {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private static final StringConverter<LocalDate> DATE_CONVERTER = new StringConverter<>() {
        @Override
        public String toString(LocalDate d) {
            return d == null ? "" : d.format(DATE_FMT);
        }

        @Override
        public LocalDate fromString(String s) {
            if (s == null || s.isBlank()) return null;
            try {
                return LocalDate.parse(s.trim().replace('-', '/'), DATE_FMT);
            } catch (DateTimeParseException ex) {
                return null;
            }
        }
    };

    private static final StringConverter<BigDecimal> AMOUNT_CONVERTER = new StringConverter<>() {
        @Override
        public String toString(BigDecimal v) {
            return v == null ? "" : v.toPlainString();
        }

        @Override
        public BigDecimal fromString(String s) {
            if (s == null || s.isBlank()) return null;
            try {
                return new BigDecimal(s.trim());
            } catch (NumberFormatException ex) {
                return null;
            }
        }
    };

    // ── بحث وبيانات الموظف ──
    @FXML
    private TextField txt_query, txt_nationalId, txt_empNumber, txt_empName;
    @FXML
    private Label lbl_birthDate, lbl_job, lbl_region, lbl_hireDate, lbl_termDate;

    // ── أزرار ──
    @FXML
    private Button btn_search, btn_calculate, btn_export, btn_restore, btn_back;
    @FXML
    private Button btn_addBasic, btn_delBasic, btn_addVar, btn_delVar;

    // ── الجداول ──
    @FXML
    private TableView<EditableRow> tblBasic, tblVariable;
    @FXML
    private TableColumn<EditableRow, LocalDate> colBasicDate, colVarDate;
    @FXML
    private TableColumn<EditableRow, BigDecimal> colBasicAmount, colVarAmount;

    // ── إجماليات وحالة ومعاينة ──
    @FXML
    private Label lbl_totalVar, lbl_totalSub, lbl_status;
    @FXML
    private VBox tablesBox;
    @FXML
    private WebView webView;

    private final ObservableList<EditableRow> basicItems =
            FXCollections.observableArrayList(EditableRow.extractor());
    private final ObservableList<EditableRow> variableItems =
            FXCollections.observableArrayList(EditableRow.extractor());

    /**
     * يمنع تشغيل أكتر من استخراج في نفس الوقت
     */
    private final AtomicBoolean exporting = new AtomicBoolean(false);

    /**
     * آخر نتيجة احتساب — عشان زر "استعادة"
     */
    private Data lastData;
    /**
     * الرقم القومي اللي اتحسب آخر مرة — الاستخراج بيستخدمه مش حقل النص
     */
    private String lastNationalId;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTables();
        setupSearch();
        setupButtons();
        setBusy(false, "");
        showTables();
    }

    // ══════════════════════════════════════════════════════
    //  Setup
    // ══════════════════════════════════════════════════════

    private void setupTables() {
        tblBasic.setItems(basicItems);
        tblVariable.setItems(variableItems);

        colBasicDate.setCellValueFactory(c -> c.getValue().dateProperty());
        colBasicAmount.setCellValueFactory(c -> c.getValue().amountProperty());
        colVarDate.setCellValueFactory(c -> c.getValue().dateProperty());
        colVarAmount.setCellValueFactory(c -> c.getValue().amountProperty());

        colBasicDate.setCellFactory(TextFieldTableCell.forTableColumn(DATE_CONVERTER));
        colBasicAmount.setCellFactory(TextFieldTableCell.forTableColumn(AMOUNT_CONVERTER));
        colVarDate.setCellFactory(TextFieldTableCell.forTableColumn(DATE_CONVERTER));
        colVarAmount.setCellFactory(TextFieldTableCell.forTableColumn(AMOUNT_CONVERTER));

        colBasicDate.setOnEditCommit(e -> commitDate(e, tblBasic));
        colVarDate.setOnEditCommit(e -> commitDate(e, tblVariable));
        colBasicAmount.setOnEditCommit(e -> commitAmount(e, tblBasic));
        colVarAmount.setOnEditCommit(e -> commitAmount(e, tblVariable));

        tblBasic.setEditable(true);
        tblVariable.setEditable(true);
    }

    private void setupSearch() {
        SmartSearchHelper.bind(
                txt_query, btn_search,
                () -> Form5Api.search(txt_query.getText()),
                SearchDialog.builder(EmployeeHit.class)
                        .title("بحث عن موظف")
                        .column("الرقم القومي", EmployeeHit::nationalId)
                        .column("رقم الصرف", EmployeeHit::employeeNumber)
                        .column("الاسم", EmployeeHit::fullName),
                this::onEmployeeSelected,
                SmartSearchHelper.FieldBind.of(txt_nationalId, EmployeeHit::nationalId),
                SmartSearchHelper.FieldBind.of(txt_empNumber, EmployeeHit::employeeNumber),
                SmartSearchHelper.FieldBind.of(txt_empName, EmployeeHit::fullName)
        );
    }

    private void setupButtons() {
        btn_calculate.setOnAction(e -> calculate());
        btn_export.setOnAction(e -> export());
        btn_restore.setOnAction(e -> restoreCalculated());
        btn_back.setOnAction(e -> showTables());

        btn_addBasic.setOnAction(e -> basicItems.add(new EditableRow(null, null)));
        btn_addVar.setOnAction(e -> variableItems.add(new EditableRow(null, null)));
        btn_delBasic.setOnAction(e -> deleteSelected(tblBasic, basicItems));
        btn_delVar.setOnAction(e -> deleteSelected(tblVariable, variableItems));
    }

    // ══════════════════════════════════════════════════════
    //  البحث والاحتساب
    // ══════════════════════════════════════════════════════

    private void onEmployeeSelected(EmployeeHit hit) {
        setStatus("تم اختيار الموظف — اضغط احتساب");
    }

    private void calculate() {
        String nid = txt_nationalId.getText() == null ? "" : txt_nationalId.getText().trim();
        if (nid.length() != 14) {
            SAFNotification.warning("اختر موظف أولاً أو ادخل رقم قومي صحيح (14 رقم)");
            return;
        }

        setBusy(true, "جارٍ الاحتساب...");

        Form5Api.calculateAsync(nid)
                .thenAccept(resp -> Platform.runLater(() -> {
                    setBusy(false, "");
                    if (resp != null && resp.isSuccess() && resp.getData() != null) {
                        lastNationalId = nid;
                        applyData(resp.getData());
                    } else {
                        SAFNotification.error(resp != null && resp.getMessage() != null
                                ? resp.getMessage()
                                : "تعذر الاحتساب");
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        setBusy(false, "");
                        SAFNotification.error("تعذر الاتصال بالسيرفر");
                    });
                    return null;
                });
    }

    private void applyData(Data data) {
        lastData = data;
        fillInfo(data.employeeInfo());
        basicItems.setAll(basicToEditable(data.basicRows()));
        variableItems.setAll(variableToEditable(data.variableRows()));
        showTables();
        showTotals(data.totalVariableWage(), data.totalSubscriptionWage());
        setStatus("تم الاحتساب");
    }

    private void restoreCalculated() {
        if (lastData == null) {
            SAFNotification.warning("احتسب الموظف أولاً");
            return;
        }
        applyData(lastData);
        SAFNotification.info("تم استعادة القيم المحسوبة");
    }

    private void fillInfo(Info info) {
        if (info == null) return;
        lbl_birthDate.setText(formatDate(info.birthDate()));
        lbl_job.setText(nz(info.jobTitle()));
        lbl_region.setText(nz(info.region()));
        lbl_hireDate.setText(formatDate(info.hireDate()));
        lbl_termDate.setText(formatDate(info.terminationDate()));
        if (info.fullName() != null) txt_empName.setText(info.fullName());
        if (info.nationalId() != null) txt_nationalId.setText(info.nationalId());
    }

    // ══════════════════════════════════════════════════════
    //  التعديل على الجداول
    // ══════════════════════════════════════════════════════

    private void commitDate(TableColumn.CellEditEvent<EditableRow, LocalDate> e,
                            TableView<EditableRow> table) {
        if (e.getNewValue() == null) {
            SAFNotification.warning("تاريخ غير صالح — الشكل المطلوب yyyy/MM/dd");
            table.refresh();
            return;
        }
        e.getRowValue().setDate(e.getNewValue());
    }

    private void commitAmount(TableColumn.CellEditEvent<EditableRow, BigDecimal> e,
                              TableView<EditableRow> table) {
        BigDecimal v = e.getNewValue();
        if (v == null) {
            SAFNotification.warning("قيمة غير صالحة — اكتب رقم");
            table.refresh();
            return;
        }
        if (v.signum() < 0) {
            SAFNotification.warning("القيمة لازم تكون صفر أو أكبر");
            table.refresh();
            return;
        }
        e.getRowValue().setAmount(v);
    }

    private void deleteSelected(TableView<EditableRow> table, ObservableList<EditableRow> items) {
        EditableRow row = table.getSelectionModel().getSelectedItem();
        if (row == null) {
            SAFNotification.warning("اختر صف أولاً");
            return;
        }
        items.remove(row);
    }

    // ══════════════════════════════════════════════════════
    //  الإجماليات (من الحساب الكامل — مش بتتغير مع تعديل الجدول)
    // ══════════════════════════════════════════════════════

    private void showTotals(BigDecimal variable, BigDecimal subscription) {
        lbl_totalVar.setText(formatMoney(variable));
        lbl_totalSub.setText(formatMoney(subscription));
    }

    // ══════════════════════════════════════════════════════
    //  الاستخراج (non-blocking)
    // ══════════════════════════════════════════════════════

    private void export() {
        if (lastData == null || lastNationalId == null) {
            SAFNotification.warning("احتسب الموظف أولاً");
            return;
        }

        String error = validateTables();
        if (error != null) {
            SAFNotification.warning(error);
            return;
        }

        if (!exporting.compareAndSet(false, true)) {
            SAFNotification.warning("جاري استخراج النموذج، برجاء الانتظار...");
            return;
        }

        // ── نجهّز الطلب والبيانات على الـ FX thread قبل أي async ──
        // كده أي تعديل على الجداول بعد الضغط ما يأثرش على النموذج
        ExportRequest request = buildExportRequest();
        String nationalId = lastNationalId;
        String employeeName = currentEmployeeName();

        setBusy(true, "جارٍ استخراج النموذج...");
        SAFNotification.success("جاري استخراج النموذج...");

        Form5Api.exportAsync(request)
                .whenComplete((bytes, ex) -> Platform.runLater(() -> {
                    exporting.set(false);
                    setBusy(false, "");

                    if (ex != null) {
                        SAFNotification.error("فشل استخراج النموذج: " + friendly(ex));
                        return;
                    }
                    if (bytes == null || bytes.length == 0) {
                        SAFNotification.error("الملف الناتج فارغ");
                        return;
                    }
                    savePdfAndShow(bytes, nationalId, employeeName);
                }));
    }

    private String validateTables() {
        String basicError = validateRows(basicItems, "جدول الأجر الأساسي");
        if (basicError != null) return basicError;
        return validateRows(variableItems, "جدول الأجر المتغير / الشامل");
    }

    private static String validateRows(List<EditableRow> rows, String table) {
        Set<LocalDate> seen = new HashSet<>();
        for (EditableRow r : rows) {
            if (r.getDate() == null) return "يوجد صف بدون تاريخ في " + table;
            if (r.getAmount() == null) return "يوجد صف بدون قيمة في " + table;
            if (r.getAmount().signum() < 0) return "القيمة لازم تكون صفر أو أكبر في " + table;
            if (!seen.add(r.getDate())) {
                return "يوجد تاريخ مكرر في " + table + ": " + r.getDate().format(DATE_FMT);
            }
        }
        return null;
    }

    private ExportRequest buildExportRequest() {
        List<BasicRow> basic = basicItems.stream()
                .sorted(Comparator.comparing(EditableRow::getDate))
                .map(r -> new BasicRow(r.getDate(), r.getAmount()))
                .toList();

        List<VariableRow> variable = variableItems.stream()
                .sorted(Comparator.comparing(EditableRow::getDate))
                .map(r -> new VariableRow(r.getDate(), r.getAmount()))
                .toList();

        return new ExportRequest(lastNationalId, basic, variable);
    }

    /**
     * يحفظ الـ PDF ويعرضه ويرسل الإشعار. لازم يتنادى على الـ FX thread.
     */
    private void savePdfAndShow(byte[] bytes, String nationalId, String employeeName) {
        try {
            Path dir = Paths.get(System.getProperty("user.dir"), "temp_downloads");
            Files.createDirectories(dir);
            Path target = dir.resolve("FORM5_" + nationalId + "_" + System.currentTimeMillis() + ".pdf");
            Files.write(target, bytes);

            showPreview();
            openPDF(target, webView, employeeName);
        } catch (IOException e) {
            SAFNotification.error("تعذر حفظ الملف: " + e.getMessage());
        }
    }

    /**
     * يعرض الـ PDF في الـ WebView ويرسل إشعار النظام. لازم يتنادى على الـ FX thread.
     */
    private void openPDF(Path targetPath, WebView webView, String name) {
        PDFView.showIN(targetPath.toString(), webView);
        NotificationService.getInstance().send(
                HRNotification.builder()
                        .type(HRNotification.NotificationType.SYSTEM)
                        .priority(HRNotification.Priority.HIGH)
                        .title("نموذج (5) — استمارة التغطية التأمينية")
                        .message(name)
                        .file(targetPath.toString())
                        .sender("system")
                        .build()
        );
    }

    /**
     * اسم الموظف من الـ Data المحسوبة (أدق من حقل النص لو اتعدل).
     */
    private String currentEmployeeName() {
        if (lastData != null && lastData.employeeInfo() != null
                && lastData.employeeInfo().fullName() != null) {
            return lastData.employeeInfo().fullName();
        }
        return txt_empName.getText();
    }

    /**
     * يترجم الأخطاء لرسالة مفهومة.
     */
    private static String friendly(Throwable ex) {
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        if (root instanceof HttpTimeoutException) {
            return "انتهت مهلة الاتصال بالسيرفر";
        }
        return root.getMessage() != null ? root.getMessage() : root.getClass().getSimpleName();
    }

    // ══════════════════════════════════════════════════════
    //  حالة الواجهة
    // ══════════════════════════════════════════════════════

    private void showTables() {
        tablesBox.setVisible(true);
        tablesBox.setManaged(true);
        webView.setVisible(false);
        webView.setManaged(false);
        btn_back.setVisible(false);
        btn_back.setManaged(false);
    }

    private void showPreview() {
        tablesBox.setVisible(false);
        tablesBox.setManaged(false);
        webView.setVisible(true);
        webView.setManaged(true);
        btn_back.setVisible(true);
        btn_back.setManaged(true);
    }

    private void setBusy(boolean busy, String status) {
        btn_search.setDisable(busy);
        btn_calculate.setDisable(busy);
        btn_export.setDisable(busy);
        btn_restore.setDisable(busy);
        setStatus(status);
    }

    private void setStatus(String status) {
        lbl_status.setText(status == null ? "" : status);
    }

    // ══════════════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════════════

    private static List<EditableRow> basicToEditable(List<BasicRow> rows) {
        if (rows == null) return List.of();
        return rows.stream()
                .map(r -> new EditableRow(r.date(), r.basicSalary()))
                .toList();
    }

    private static List<EditableRow> variableToEditable(List<VariableRow> rows) {
        if (rows == null) return List.of();
        return rows.stream()
                .map(r -> new EditableRow(r.date(), r.averageVariableWage()))
                .toList();
    }

    private static String formatDate(LocalDate d) {
        return d == null ? "" : d.format(DATE_FMT);
    }

    private static String formatMoney(BigDecimal v) {
        BigDecimal safe = v == null ? BigDecimal.ZERO : v;
        return safe.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    // ══════════════════════════════════════════════════════
    //  صف قابل للتعديل (مشترك بين الجدولين)
    // ══════════════════════════════════════════════════════

    public static final class EditableRow {

        private final ObjectProperty<LocalDate> date = new SimpleObjectProperty<>();
        private final ObjectProperty<BigDecimal> amount = new SimpleObjectProperty<>();

        public EditableRow(LocalDate date, BigDecimal amount) {
            this.date.set(date);
            this.amount.set(amount);
        }

        public ObjectProperty<LocalDate> dateProperty() {
            return date;
        }

        public ObjectProperty<BigDecimal> amountProperty() {
            return amount;
        }

        public LocalDate getDate() {
            return date.get();
        }

        public void setDate(LocalDate d) {
            date.set(d);
        }

        public BigDecimal getAmount() {
            return amount.get();
        }

        public void setAmount(BigDecimal a) {
            amount.set(a);
        }

        static Callback<EditableRow, Observable[]> extractor() {
            return r -> new Observable[]{r.date, r.amount};
        }
    }
}