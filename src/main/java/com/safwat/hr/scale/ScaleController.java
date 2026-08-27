package com.safwat.hr.scale;

import com.safwat.hr.network.ApiClient;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Controller شاشة احتساب السلم الوظيفي.
 *
 * <p><b>العمليات الثلاث:</b>
 * <ol>
 *   <li><b>بحث</b>: جلب بيانات موظف من DB وملء الحقول والجداول</li>
 *   <li><b>حفظ</b>: إرسال البيانات للخلفية لحفظها أو تحديثها</li>
 *   <li><b>احتساب</b>: إرسال البيانات للخلفية وعرض النتيجة بدون حفظ</li>
 * </ol>
 */
public class ScaleController implements Initializable {

    // ─────────────────────────────────────────────
    //  ثوابت
    // ─────────────────────────────────────────────

    private static final String API_BASE = "/salary-scale";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_DISPLAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ─────────────────────────────────────────────
    //  ObservableLists للجداول
    // ─────────────────────────────────────────────

    private final ObservableList<DateValueRow> mogardAddData = FXCollections.observableArrayList();
    private final ObservableList<DateValueRow> mogardRivalData = FXCollections.observableArrayList();
    private final ObservableList<DateValueRow> bounsAddData = FXCollections.observableArrayList();
    private final ObservableList<DateValueRow> bounsRivalData = FXCollections.observableArrayList();

    // ─────────────────────────────────────────────
    //  FXML — حقول البحث
    // ─────────────────────────────────────────────

    @FXML
    private TextField txt_Management;
    @FXML
    private TextField txt_empName;
    @FXML
    private TextField txt_empCode;
    @FXML
    private TextField txt_nationalId;
    @FXML
    private Button btn_search;

    // ─────────────────────────────────────────────
    //  FXML — بيانات الموظف
    // ─────────────────────────────────────────────

    @FXML
    private TextField txt_group;
    @FXML
    private TextField txt_law;
    @FXML
    private TextField txt_code;
    @FXML
    private TextField txt_startDegree;
    @FXML
    private TextField txt_startDate;
    @FXML
    private TextField txt_backStart;
    @FXML
    private TextField txt_debloma;
    @FXML
    private TextField txt_magester;
    @FXML
    private TextField txt_doctoraa;
    @FXML
    private TextField txt_ectra;

    // ─────────────────────────────────────────────
    //  FXML — نتائج التسكين
    // ─────────────────────────────────────────────

    @FXML
    private TextField txt_regrade3;
    @FXML
    private TextField txt_regrade4;
    @FXML
    private TextField txt_regrade5;
    @FXML
    private TextField txt_backRegrade;
    @FXML
    private TextField yearUp;
    @FXML
    private TextField yearNoUp;
    @FXML
    private TextField gpUp;
    @FXML
    private TextField gpNoUp;
    @FXML
    private TextField yearsBack;

    // ─────────────────────────────────────────────
    //  FXML — جداول الترقيات والتشجيعيات
    // ─────────────────────────────────────────────

    @FXML
    private TextField date_kader;
    @FXML
    private TextField end_day;
    @FXML
    private TextField txt_startCut;
    @FXML
    private TextField txt_endCut;

    // ─────────────────────────────────────────────
    //  FXML — جداول الإضافة والخصم
    // ─────────────────────────────────────────────

    @FXML
    private TableView<String[]> table_upgrade;
    @FXML
    private TableView<String[]> table_encourge;
    @FXML
    private TableView<String[]> table_promotion;
    @FXML
    private TableView<DateValueRow> table_mogardAdd;
    @FXML
    private TableView<DateValueRow> table_mogardRival;
    @FXML
    private TableView<DateValueRow> table_bounsAdd;
    @FXML
    private TableView<DateValueRow> table_bounsRival;

    // ─────────────────────────────────────────────
    //  FXML — أزرار الصفوف
    // ─────────────────────────────────────────────

    @FXML
    private Button btn_mogardAddRow;
    @FXML
    private Button btn_mogardDelRow;
    @FXML
    private Button btn_mogardRivalAddRow;
    @FXML
    private Button btn_mogardRivalDelRow;
    @FXML
    private Button btn_bounsAddRow;
    @FXML
    private Button btn_bounsDelRow;
    @FXML
    private Button btn_bounsRivalAddRow;
    @FXML
    private Button btn_bounsRivalDelRow;

    // ─────────────────────────────────────────────
    //  FXML — أزرار الإجراءات
    // ─────────────────────────────────────────────

    @FXML
    private Button btn_calculate;
    @FXML
    private Button btn_pdf;
    @FXML
    private Button btn_save;
    @FXML
    private Button btn_clear;

    // ─────────────────────────────────────────────
    //  FXML — جدول النتائج
    // ─────────────────────────────────────────────

    @FXML
    private TableView<?> table_result;

    // ─────────────────────────────────────────────
    //  State
    // ─────────────────────────────────────────────

    /**
     * آخر record تم جلبه أو بناؤه — نستخدمه في الحفظ والاحتساب
     */
    private EmployeeFullRecord currentRecord = null;

    // ─────────────────────────────────────────────
    //  Initialize
    // ─────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupEditableTable(table_mogardAdd, mogardAddData);
        setupEditableTable(table_mogardRival, mogardRivalData);
        setupEditableTable(table_bounsAdd, bounsAddData);
        setupEditableTable(table_bounsRival, bounsRivalData);

        setupRowButtons(btn_mogardAddRow, btn_mogardDelRow, table_mogardAdd, mogardAddData);
        setupRowButtons(btn_mogardRivalAddRow, btn_mogardRivalDelRow, table_mogardRival, mogardRivalData);
        setupRowButtons(btn_bounsAddRow, btn_bounsDelRow, table_bounsAdd, bounsAddData);
        setupRowButtons(btn_bounsRivalAddRow, btn_bounsRivalDelRow, table_bounsRival, bounsRivalData);

        btn_search.setOnAction(e -> doSearch());
        btn_calculate.setOnAction(e -> doCalculate());
        btn_save.setOnAction(e -> doSave());
        btn_pdf.setOnAction(e -> doPdf());
        btn_clear.setOnAction(e -> doClear());

        // البحث بضغط Enter في حقل الرقم القومي
        txt_nationalId.setOnAction(e -> doSearch());
    }

    // ─────────────────────────────────────────────
    //  Table Setup
    // ─────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void setupEditableTable(TableView<DateValueRow> table,
                                    ObservableList<DateValueRow> data) {
        table.setEditable(true);
        table.setItems(data);

        TableColumn<DateValueRow, String> dateCol =
                (TableColumn<DateValueRow, String>) table.getColumns().get(0);
        dateCol.setCellValueFactory(cell -> cell.getValue().dateProperty());
        dateCol.setCellFactory(TextFieldTableCell.forTableColumn());
        dateCol.setOnEditCommit(e -> e.getRowValue().setDate(e.getNewValue()));

        TableColumn<DateValueRow, String> valueCol =
                (TableColumn<DateValueRow, String>) table.getColumns().get(1);
        valueCol.setCellValueFactory(cell -> cell.getValue().valueProperty());
        valueCol.setCellFactory(TextFieldTableCell.forTableColumn());
        valueCol.setOnEditCommit(e -> e.getRowValue().setValue(e.getNewValue()));

        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(DateValueRow item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setStyle("");
                else setStyle(getIndex() % 2 == 0
                        ? "-fx-background-color: #F9FAFB;"
                        : "-fx-background-color: white;");
            }
        });
    }

    private void setupRowButtons(Button addBtn, Button delBtn,
                                 TableView<DateValueRow> table,
                                 ObservableList<DateValueRow> data) {
        addBtn.setOnAction(e -> {
            DateValueRow newRow = new DateValueRow("", "");
            data.add(newRow);
            table.getSelectionModel().select(newRow);
            table.scrollTo(newRow);
            table.edit(data.size() - 1, table.getColumns().get(0));
        });
        delBtn.setOnAction(e -> {
            DateValueRow selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) data.remove(selected);
        });
    }

    // ─────────────────────────────────────────────
    //  Actions — البحث
    // ─────────────────────────────────────────────

    /**
     * يبحث عن الموظف بالرقم القومي ويملأ الشاشة بالبيانات.
     * يستدعي: GET /api/salary-scale/{nationalId}
     */
    private void doSearch() {
        String nationalId = txt_nationalId.getText().trim();
        if (nationalId.isBlank()) {
            showWarning("تنبيه", "أدخل الرقم القومي أولاً");
            return;
        }

        setFormDisabled(true);

        ApiClient.getAsync(API_BASE + "/" + nationalId, EmployeeFullRecord.class)
                .thenAcceptAsync(response -> Platform.runLater(() -> {
                    setFormDisabled(false);
                    if (!response.isSuccess() || response.getData() == null) {
                        showError("بحث", "الموظف غير موجود أو حدث خطأ:\n" + response.getMessage());
                        return;
                    }
                    currentRecord = response.getData();
                    fillForm(currentRecord);
                    showInfo("تم العثور على الموظف: " + currentRecord.getEmpName());
                }));
    }

    // ─────────────────────────────────────────────
    //  Actions — الحفظ
    // ─────────────────────────────────────────────

    /**
     * يحفظ أو يحدث بيانات الموظف.
     * - لو الموظف موجود (currentRecord != null) → update
     * - لو مش موجود → insert جديد
     * يستدعي: POST /api/salary-scale/save
     */
    private void doSave() {
        if (!validateForm()) return;

        EmployeeFullRecord record = buildRecord();
        if (record == null) return;

        setFormDisabled(true);

        ApiClient.postAsync(API_BASE + "/save", record, Void.class)
                .thenAcceptAsync(response -> Platform.runLater(() -> {
                    setFormDisabled(false);
                    if (response.isSuccess()) {
                        currentRecord = record;
                        showInfo("تم حفظ بيانات الموظف بنجاح ✓");
                    } else {
                        showError("حفظ", "فشل الحفظ:\n" + response.getMessage());
                    }
                }));
    }

    // ─────────────────────────────────────────────
    //  Actions — الاحتساب بدون حفظ
    // ─────────────────────────────────────────────

    /**
     * يحسب تدرج المرتب بدون حفظ في DB.
     * يستدعي: POST /api/salary-scale/calculate
     */
    private void doCalculate() {
        if (!validateForm()) return;

        EmployeeFullRecord record = buildRecord();
        if (record == null) return;

        setFormDisabled(true);

        ApiClient.postAsync(
                API_BASE + "/calculate",
                record,
                ScaleResult.class
        ).thenAcceptAsync(response -> Platform.runLater(() -> {
            setFormDisabled(false);
            if (!response.isSuccess() || response.getData() == null) {
                showError("احتساب", "فشل الاحتساب:\n" + response.getMessage());
                return;
            }
            fillResult(response.getData());
        }));
    }

    // ─────────────────────────────────────────────
    //  Actions — PDF و Clear
    // ─────────────────────────────────────────────

    private void doPdf() {
        // TODO: توليد PDF من currentRecord والنتائج
    }

    private void doClear() {
        List.of(txt_nationalId, txt_empCode, txt_empName, txt_Management,
                        txt_startDate, txt_backStart, txt_debloma, txt_magester,
                        txt_doctoraa, txt_ectra, txt_regrade3, txt_regrade4,
                        txt_regrade5, txt_backRegrade, txt_group, txt_law,
                        txt_code, txt_startDegree, yearUp, yearNoUp, gpUp,
                        gpNoUp, yearsBack, date_kader, end_day,
                        txt_startCut, txt_endCut)
                .forEach(TextField::clear);

        mogardAddData.clear();
        mogardRivalData.clear();
        bounsAddData.clear();
        bounsRivalData.clear();

        currentRecord = null;
    }

    // ─────────────────────────────────────────────
    //  Form Filling — ملء الشاشة من EmployeeFullRecord
    // ─────────────────────────────────────────────

    /**
     * يملأ كل حقول الشاشة من بيانات الموظف المجلوبة.
     *
     * <p>التحويل:
     * <ul>
     *   <li>LocalDate → String بصيغة dd/MM/yyyy</li>
     *   <li>BigDecimal → String</li>
     *   <li>List<AdjustmentRecord> → ObservableList<DateValueRow></li>
     * </ul>
     */
    private void fillForm(EmployeeFullRecord r) {
        // البيانات الأساسية
        setText(txt_nationalId, r.getId());
        setText(txt_empName, r.getEmpName());
        setText(txt_group, r.getQualitativeGroup());
        setText(txt_law, r.getLaw());
        setText(txt_code, r.getLawCode());
        setText(txt_startDegree, r.getStartDegree());
        setText(txt_startDate, formatDate(r.getStartDate()));
        setText(txt_backStart, formatDate(r.getRestartDate()));

        // فترة القطع
        if (r.getCutoff() != null) {
            setText(txt_startCut, formatDate(r.getCutoff().start()));
            setText(txt_endCut, formatDate(r.getCutoff().end()));
        } else {
            txt_startCut.clear();
            txt_endCut.clear();
        }

        // الجداول الأربعة
        fillAdjustmentTable(mogardAddData, r.getMogardAdditions());
        fillAdjustmentTable(mogardRivalData, r.getMogardRemovals());
        fillAdjustmentTable(bounsAddData, r.getBonusAdditions());
        fillAdjustmentTable(bounsRivalData, r.getBonusRemovals());
    }

    /**
     * يملأ ObservableList من List<AdjustmentRecord>
     */
    private void fillAdjustmentTable(ObservableList<DateValueRow> target,
                                     List<AdjustmentRecord> source) {
        target.clear();
        if (source == null) return;
        source.forEach(adj -> target.add(
                new DateValueRow(
                        formatDate(adj.date()),
                        adj.amount() != null ? adj.amount().toPlainString() : ""
                )
        ));
    }

    // ─────────────────────────────────────────────
    //  Form Building — بناء EmployeeFullRecord من الشاشة
    // ─────────────────────────────────────────────

    /**
     * يجمع كل بيانات الشاشة في {@link EmployeeFullRecord}.
     *
     * <p>التحويل:
     * <ul>
     *   <li>String → LocalDate (yyyy-MM-dd أو dd/MM/yyyy)</li>
     *   <li>String → BigDecimal</li>
     *   <li>DateValueRow → AdjustmentRecord</li>
     * </ul>
     *
     * @return EmployeeFullRecord أو null لو في خطأ في التحويل
     */
    private EmployeeFullRecord buildRecord() {
        try {
            EmployeeFullRecord r = new EmployeeFullRecord();

            r.setId(parseLong(txt_nationalId.getText()));
            r.setEmpName(txt_empName.getText().trim());
            r.setQualitativeGroup(txt_group.getText().trim());
            r.setLaw(parseInt(txt_law.getText()));
            r.setLawCode(parseBigDecimal(txt_code.getText()));
            r.setStartDegree(parseInt(txt_startDegree.getText()));
            r.setStartDate(parseDate(txt_startDate.getText()));
            r.setRestartDate(parseDate(txt_backStart.getText()));

            // فترة القطع
            LocalDate cutStart = parseDate(txt_startCut.getText());
            LocalDate cutEnd = parseDate(txt_endCut.getText());
            if (cutStart != null && cutEnd != null) {
                r.setCutoff(new CutoffPeriod(cutStart, cutEnd));
            }

            // الجداول
            r.setMogardAdditions(toAdjustmentList(mogardAddData));
            r.setMogardRemovals(toAdjustmentList(mogardRivalData));
            r.setBonusAdditions(toAdjustmentList(bounsAddData));
            r.setBonusRemovals(toAdjustmentList(bounsRivalData));

            // البيانات اللي ما فيهاش حقول في الشاشة — محافظ على القيم الأصلية
            if (currentRecord != null) {
                r.setUpgrades(currentRecord.getUpgrades());
                r.setEncouragements(currentRecord.getEncouragements());
                r.setGroupChanges(currentRecord.getGroupChanges());
                r.setPromotionIncentives(currentRecord.getPromotionIncentives());
                r.setExtraInfo(currentRecord.getExtraInfo());
                r.setBasic30Date(currentRecord.getBasic30Date());
                r.setBasic30From(currentRecord.getBasic30From());
            } else {
                // موظف جديد — قوائم فارغة
                r.setUpgrades(List.of());
                r.setEncouragements(List.of());
                r.setGroupChanges(List.of());
                r.setPromotionIncentives(List.of());
            }

            return r;

        } catch (IllegalArgumentException e) {
            showError("خطأ في البيانات", e.getMessage());
            return null;
        }
    }

    // ─────────────────────────────────────────────
    //  Result Filling — ملء نتائج الاحتساب
    // ─────────────────────────────────────────────

    /**
     * يملأ حقول النتائج من ScaleResult.
     * يأخذ آخر نقطة في الـ timeline كنتيجة حالية.
     */
    private void fillResult(ScaleResult result) {
        if (result == null) return;

        // آخر نقطة في الـ timeline = الوضع الحالي
        result.lastPoint().ifPresent(point -> {
            setText(txt_regrade3, safeStr(point.mogard()));
            setText(txt_regrade4, safeStr(point.periodicBonus()));
            setText(txt_regrade5, safeStr(point.upgradeBonus()));
            setText(txt_backRegrade, safeStr(point.encourageBonus()));
            setText(yearUp, safeStr(point.spBonusNotSubject()));
            setText(yearNoUp, safeStr(point.spBonusSubject()));
            setText(gpUp, safeStr(point.currentBasic()));
            setText(date_kader, formatDate(point.date()));
        });

        // عدد نقاط الـ timeline (عدد الأحداث)
        setText(yearsBack, String.valueOf(result.timeline().size()));
    }

    // ─────────────────────────────────────────────
    //  Validation
    // ─────────────────────────────────────────────

    private boolean validateForm() {
        if (txt_nationalId.getText().trim().isBlank()) {
            showWarning("تحقق", "الرقم القومي مطلوب");
            txt_nationalId.requestFocus();
            return false;
        }
        if (txt_startDate.getText().trim().isBlank()) {
            showWarning("تحقق", "تاريخ التعيين مطلوب");
            txt_startDate.requestFocus();
            return false;
        }
        if (txt_law.getText().trim().isBlank()) {
            showWarning("تحقق", "القانون مطلوب (47 أو 81)");
            txt_law.requestFocus();
            return false;
        }
        return true;
    }

    // ─────────────────────────────────────────────
    //  Conversion Helpers
    // ─────────────────────────────────────────────

    /**
     * يحول ObservableList<DateValueRow> إلى List<AdjustmentRecord>.
     * يتجاهل الصفوف الفارغة، ويتحقق من صحة التاريخ والمبلغ.
     */
    private List<AdjustmentRecord> toAdjustmentList(ObservableList<DateValueRow> rows) {
        return rows.stream()
                .filter(r -> !r.getDate().isBlank() && !r.getValue().isBlank())
                .map(r -> {
                    LocalDate date = parseDate(r.getDate());
                    BigDecimal amount = parseBigDecimal(r.getValue());
                    if (date == null || amount == null) return null;
                    return new AdjustmentRecord(date, amount);
                })
                .filter(adj -> adj != null)
                .collect(Collectors.toList());
    }

    /**
     * يحول LocalDate → String بصيغة dd/MM/yyyy (أو "" لو null)
     */
    private String formatDate(LocalDate date) {
        if (date == null) return "";
        return date.format(DATE_DISPLAY);
    }

    /**
     * يحول String → LocalDate.
     * يقبل صيغتين: dd/MM/yyyy أو yyyy-MM-dd.
     * يرجع null لو النص فارغ أو غير صالح (بدون exception).
     */
    private LocalDate parseDate(String text) {
        if (text == null || text.trim().isBlank()) return null;
        text = text.trim();
        try {
            // dd/MM/yyyy
            if (text.contains("/")) return LocalDate.parse(text, DATE_DISPLAY);
            // yyyy-MM-dd
            return LocalDate.parse(text, DATE_FORMAT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private Long parseLong(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            return Long.parseLong(text.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("قيمة غير صالحة: " + text);
        }
    }

    private Integer parseInt(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("قيمة رقمية غير صالحة: " + text);
        }
    }

    private BigDecimal parseBigDecimal(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            return new BigDecimal(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String safeStr(Object value) {
        return value != null ? value.toString() : "";
    }

    // ─────────────────────────────────────────────
    //  UI Helpers
    // ─────────────────────────────────────────────

    private void setText(TextField field, Object value) {
        if (field == null) return;
        field.setText(value != null ? value.toString() : "");
    }

    private void setFormDisabled(boolean disabled) {
        btn_search.setDisable(disabled);
        btn_save.setDisable(disabled);
        btn_calculate.setDisable(disabled);
        btn_clear.setDisable(disabled);
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("معلومة");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }

    // ─────────────────────────────────────────────
    //  Table Setup — Row Buttons
    // ─────────────────────────────────────────────

    // ─────────────────────────────────────────────
    //  Inner Classes / Records
    // ─────────────────────────────────────────────

    /**
     * نموذج صف الجدول — تاريخ + قيمة
     */
    public static class DateValueRow {
        private final SimpleStringProperty date;
        private final SimpleStringProperty value;

        public DateValueRow(String date, String value) {
            this.date = new SimpleStringProperty(date);
            this.value = new SimpleStringProperty(value);
        }

        public SimpleStringProperty dateProperty() {
            return date;
        }

        public SimpleStringProperty valueProperty() {
            return value;
        }

        public String getDate() {
            return date.get();
        }

        public void setDate(String v) {
            date.set(v);
        }

        public String getValue() {
            return value.get();
        }

        public void setValue(String v) {
            value.set(v);
        }
    }
}