package com.safwat.hr.scale;

import com.safwat.hr.network.ApiClient;

import com.safwat.hr.shared.util.DateUtils;
import com.safwat.hr.ui.TextFieldSetupHelper;
import com.safwat.hr.ui.table.TableSetupHelper;
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
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import static com.safwat.hr.ui.table.TableSetupHelper.*;

/**
 * Controller شاشة السلم الوظيفي.
 *
 * <p>يتعامل مع {@link ScaleDto} فقط في الاتجاهين — كلاس واحد موحد.
 *
 * <p><b>العمليات:</b>
 * <ol>
 *   <li><b>بحث</b>  → GET  /salary-scale/{nationalId}  → يملأ الشاشة + النتيجة</li>
 *   <li><b>احتساب</b> → POST /salary-scale/calculate     → يملأ النتيجة بدون حفظ</li>
 *   <li><b>حفظ</b>  → POST /salary-scale/save           → يحفظ + يملأ النتيجة</li>
 * </ol>
 */
public class ScaleController implements Initializable {
    private static final String API_BASE = "/salary-scale";
    // ─────────────────────────────────────────────
    //  ثوابت التنسيق
    // ─────────────────────────────────────────────
    private static final DateTimeFormatter FMT_ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter FMT_DISPLAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");


    // ─────────────────────────────────────────────
    //  ObservableLists للجداول الأربعة
    // ─────────────────────────────────────────────
    private final ObservableList<DateValueRow> mogardAddData = FXCollections.observableArrayList();
    private final ObservableList<DateValueRow> mogardRivalData = FXCollections.observableArrayList();
    private final ObservableList<DateValueRow> bounsAddData = FXCollections.observableArrayList();
    private final ObservableList<DateValueRow> bounsRivalData = FXCollections.observableArrayList();

    // ─────────────────────────────────────────────
    //  FXML Fields
    // ─────────────────────────────────────────────

    @FXML
    private TextField txt_nationalId;
    @FXML
    private TextField txt_empName;
    @FXML
    private TextField txt_empCode;
    @FXML
    private TextField txt_Management;
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
    @FXML
    private TextField txt_startCut;
    @FXML
    private TextField txt_endCut;

    // نتائج الاحتساب
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
    @FXML
    private TextField date_kader;
    @FXML
    private TextField end_day;

    // الجداول
    @FXML
    private TableView<UpgradeRecord> table_upgrade;
    @FXML
    private TableView<EncouragementRecord> table_encourge;
    @FXML
    private TableView<PromotionIncentiveRecord> table_promotion;

    @FXML
    private TableView<DateValueRow> table_mogardAdd;
    @FXML
    private TableView<DateValueRow> table_mogardRival;
    @FXML
    private TableView<DateValueRow> table_bounsAdd;
    @FXML
    private TableView<DateValueRow> table_bounsRival;
    @FXML
    private TableView<ScaleTimelinePoint> table_result;

    // أزرار الصفوف
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

    // أزرار الإجراءات
    @FXML
    private Button btn_search;
    @FXML
    private Button btn_calculate;
    @FXML
    private Button btn_save;
    @FXML
    private Button btn_pdf;
    @FXML
    private Button btn_clear;

    // ─────────────────────────────────────────────
    //  State — الـ DTO الحالي
    // ─────────────────────────────────────────────

    /**
     * الـ DTO الحالي المعروض في الشاشة.
     * يُحدَّث بعد كل عملية بحث أو حفظ أو احتساب.
     */
    private ScaleDto currentDto = null;

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

        txt_nationalId.setOnAction(e -> doSearch());
        setupUpgradeTable();
        setupEncouragementTable();
        setupPromotionTable();
        setupDateFields();
    }

    void setupDateFields() {
        TextFieldSetupHelper.setupDateFields("yyyy-MM-dd", txt_startDate, txt_backStart,
                txt_startCut, txt_endCut, txt_regrade3, txt_regrade4, txt_regrade5,
                txt_backRegrade, txt_debloma, txt_magester, txt_doctoraa, date_kader);
    }
    // ─────────────────────────────────────────────
    //  Action — بحث
    // ─────────────────────────────────────────────

    private void doSearch() {
        String id = txt_nationalId.getText().trim();
        if (id.isBlank()) {
            showWarning("أدخل الرقم القومي أولاً");
            return;
        }
        setButtonsDisabled(true);

        ApiClient.getAsync(API_BASE + "/" + id, ScaleDto.class)
                .thenAcceptAsync(response -> Platform.runLater(() -> {
                    setButtonsDisabled(false);
                    if (!response.isSuccess() || response.getData() == null) {
                        showError("الموظف غير موجود:\n" + response.getMessage());
                        return;
                    }
                    currentDto = response.getData();
                    fillForm(currentDto);
                }));
    }

    // ─────────────────────────────────────────────
    //  Action — احتساب بدون حفظ
    // ─────────────────────────────────────────────

    private void doCalculate() {
        if (!validateForm()) return;
        ScaleDto dto = buildDto();
        if (dto == null) return;

        setButtonsDisabled(true);

        ApiClient.postAsync(API_BASE + "/calculate", dto, ScaleDto.class)
                .thenAcceptAsync(response -> Platform.runLater(() -> {
                    setButtonsDisabled(false);
                    if (!response.isSuccess() || response.getData() == null) {
                        showError("فشل الاحتساب:\n" + response.getMessage());
                        return;
                    }
                    // نحدث النتيجة فقط — البيانات تفضل زي ما هي
                    currentDto = response.getData();
                    // fillResult(currentDto);
                }));
    }

    // ─────────────────────────────────────────────
    //  Action — حفظ
    // ─────────────────────────────────────────────

    private void doSave() {
        if (!validateForm()) return;
        ScaleDto dto = buildDto();
        if (dto == null) return;

        setButtonsDisabled(true);

        ApiClient.postAsync(API_BASE + "/save", dto, ScaleDto.class)
                .thenAcceptAsync(response -> Platform.runLater(() -> {
                    setButtonsDisabled(false);
                    if (!response.isSuccess() || response.getData() == null) {
                        showError("فشل الحفظ:\n" + response.getMessage());
                        return;
                    }
                    // الخلفية بترجع الـ DTO كامل بعد الحفظ والاحتساب
                    currentDto = response.getData();
                    fillForm(currentDto);
                    showInfo("تم الحفظ بنجاح ✓");
                }));
    }

    private void doPdf() {
        // TODO: توليد PDF من currentDto
    }

    private void setupUpgradeTable() {
        List<TableSetupHelper.ColumnConfig<UpgradeRecord>> cols = List.of(
                new TableSetupHelper.ColumnConfig<>("تاريخ الترقية", 100,
                        r -> formatDateOutput(r.getDate()),           // getter: LocalDate → String
                        (r, v) -> r.setDate(parseDateInput(v)),       // setter: String → LocalDate
                        true, true),                                  // editable, isDateColumn
                new TableSetupHelper.ColumnConfig<>("رقم القرار", 140,
                        UpgradeRecord::getDecisionNumber,
                        UpgradeRecord::setDecisionNumber,
                        true, false)                                  // عمود نصي عادي
        );
        setupGenericTable(table_upgrade, cols, 10, UpgradeRecord::new);
    }

    private void setupEncouragementTable() {
        List<ColumnConfig<EncouragementRecord>> cols = List.of(
                new ColumnConfig<>("تاريخ التشجيعية", 100,
                        r -> formatDateOutput(r.getDate()),
                        (r, v) -> r.setDate(parseDateInput(v)),
                        true, true),
                new ColumnConfig<>("رقم القرار", 140,
                        EncouragementRecord::getDecisionNumber,
                        EncouragementRecord::setDecisionNumber,
                        true, false)
        );
        setupGenericTable(table_encourge, cols, 10, EncouragementRecord::new);
    }

    private void setupPromotionTable() {
        List<ColumnConfig<PromotionIncentiveRecord>> cols = List.of(
                new ColumnConfig<>("تاريخ الحافز", 100,
                        r -> formatDateOutput(r.getDate()),
                        (r, v) -> r.setDate(parseDateInput(v)),
                        true, true),
                new ColumnConfig<>("رقم القرار", 140,
                        PromotionIncentiveRecord::getDecisionNumber,
                        PromotionIncentiveRecord::setDecisionNumber,
                        true, false)
        );
        setupGenericTable(table_promotion, cols, 10, PromotionIncentiveRecord::new);
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

        currentDto = null;
    }

    // ─────────────────────────────────────────────
    //  Fill Form — ملء الشاشة كاملة من ScaleDto
    // ─────────────────────────────────────────────

    /**
     * يملأ كل حقول الشاشة والجداول والنتيجة من ScaleDto واحد.
     */
    private void fillForm(ScaleDto dto) {
        Map<String, Object> extraInfo = dto.getExtraInfo();
        // البيانات الأساسية
        setText(txt_nationalId, dto.getNationalId());
        setText(txt_empCode, dto.getCodeId());
        setText(txt_empName, dto.getEmpName());
        setText(txt_group, dto.getQualitativeGroup());
        setText(txt_law, dto.getLaw());
        setText(txt_code, dto.getLawCode());
        setText(txt_startDegree, dto.getStartDegree());
        setText(txt_startDate, fmt(dto.getStartDate()));
        setText(txt_backStart, fmt(dto.getRestartDate()));
        setText(txt_startCut, fmt(dto.getCutStart()));
        setText(txt_endCut, fmt(dto.getCutEnd()));
        setText(yearUp, extraInfo != null ? extraInfo.getOrDefault("year_up", null) : null);
        setText(yearNoUp, extraInfo != null ? extraInfo.getOrDefault("year_no_up", null) : null);
        setText(gpUp, extraInfo != null ? extraInfo.getOrDefault("gp_up", null) : null);
        setText(gpNoUp, extraInfo != null ? extraInfo.getOrDefault("gp_no_up", null) : null);
        setText(yearsBack, extraInfo != null ? extraInfo.getOrDefault("year_back", null) : null);
        setText(txt_regrade3, fmt(extraInfo != null ? DateUtils.parseDate((String) extraInfo.getOrDefault("regrade_3", null)) : null));
        setText(txt_regrade4, fmt(extraInfo != null ? DateUtils.parseDate((String) extraInfo.getOrDefault("regrade_4", null)) : null));
        setText(txt_regrade5, fmt(extraInfo != null ? DateUtils.parseDate((String) extraInfo.getOrDefault("regrade_5", null)) : null));
        setText(txt_debloma, fmt(extraInfo != null ? DateUtils.parseDate((String) extraInfo.getOrDefault("debloma", null)) : null));
        setText(txt_magester, fmt(extraInfo != null ? DateUtils.parseDate((String) extraInfo.getOrDefault("magester", null)) : null));
        setText(txt_doctoraa, fmt(extraInfo != null ? DateUtils.parseDate((String) extraInfo.getOrDefault("doctoraa", null)) : null));
        setText(txt_backRegrade, fmt(extraInfo != null ? DateUtils.parseDate((String) extraInfo.getOrDefault("back_regrade", null)) : null));
        setText(date_kader, dto.getBasic30Date() != null ? fmt(dto.getBasic30Date()) : null);

        // الجداول الأربعة
        fillTable(mogardAddData, dto.getMogardAdditions());
        fillTable(mogardRivalData, dto.getMogardRemovals());
        fillTable(bounsAddData, dto.getBonusAdditions());
        fillTable(bounsRivalData, dto.getBonusRemovals());

        fillUpgradeTable(dto.getUpgrades());
        fillEncouragementTable(dto.getEncouragements());
        fillPromotionTable(dto.getPromotionIncentives());
        fillResultTable(dto.getResult().getTimeline());
    }

    // ─── fillTable ───
    private void fillTable(ObservableList<DateValueRow> target,
                           List<AdjustmentRecord> source) {
        target.clear();
        if (source == null) return;
        source.forEach(adj -> target.add(new DateValueRow(
                fmt(adj.getDate()),
                adj.getAmount() != null ? adj.getAmount().toPlainString() : ""
        )));
    }

    void fillUpgradeTable(List<UpgradeRecord> upgrades) {
        table_upgrade.getItems().clear();
        if (upgrades != null) {
            table_upgrade.getItems().addAll(upgrades);
        }
    }

    void fillEncouragementTable(List<EncouragementRecord> encouragements) {
        table_encourge.getItems().clear();
        if (encouragements != null) {
            table_encourge.getItems().addAll(encouragements);
        }
    }

    void fillPromotionTable(List<PromotionIncentiveRecord> promotions) {
        table_promotion.getItems().clear();
        if (promotions != null) {
            table_promotion.getItems().addAll(promotions);
        }
    }

    void fillResultTable(List<ScaleTimelinePoint> result) {
        table_result.getItems().clear();
        if (result != null) {
            table_result.getItems().addAll(result);
        }
    }
    // ─────────────────────────────────────────────
    //  Fill Result — ملء حقول النتيجة فقط
    // ─────────────────────────────────────────────


    /**
     * يجمع كل بيانات الشاشة في ScaleDto جاهز للإرسال.
     * النتيجة تُترك null — الخلفية هي اللي تملأها.
     *
     * @return ScaleDto أو null لو في خطأ في التحويل
     */
    private ScaleDto buildDto() {
        try {
            ScaleDto dto = new ScaleDto();

            dto.setNationalId(txt_nationalId.getText());
            dto.setEmpName(txt_empName.getText().trim());
            dto.setQualitativeGroup(txt_group.getText().trim());
            dto.setLaw(parseInt(txt_law.getText()));
            dto.setLawCode(parseBigDecimal(txt_code.getText()));
            dto.setStartDegree(parseInt(txt_startDegree.getText()));
            dto.setStartDate(parseDate(txt_startDate.getText()));
            dto.setRestartDate(parseDate(txt_backStart.getText()));
            dto.setCutStart(parseDate(txt_startCut.getText()));
            dto.setCutEnd(parseDate(txt_endCut.getText()));

            // الجداول الأربعة
            dto.setMogardAdditions(toAdjustmentList(mogardAddData));
            dto.setMogardRemovals(toAdjustmentList(mogardRivalData));
            dto.setBonusAdditions(toAdjustmentList(bounsAddData));
            dto.setBonusRemovals(toAdjustmentList(bounsRivalData));

            // البيانات اللي مفيهاش حقول في الشاشة — محافظ عليها من currentDto
            if (currentDto != null) {
                dto.setUpgrades(currentDto.getUpgrades());
                dto.setEncouragements(currentDto.getEncouragements());
                dto.setPromotionIncentives(currentDto.getPromotionIncentives());
                dto.setGroupChanges(currentDto.getGroupChanges());
                dto.setBasic30Date(currentDto.getBasic30Date());
                dto.setBasic30From(currentDto.getBasic30From());
                dto.setCodeId(currentDto.getCodeId());
            } else {
                dto.setUpgrades(List.of());
                dto.setEncouragements(List.of());
                dto.setPromotionIncentives(List.of());
                dto.setGroupChanges(List.of());
            }

            return dto;

        } catch (IllegalArgumentException e) {
            showError("خطأ في البيانات: " + e.getMessage());
            return null;
        }
    }

    // ─────────────────────────────────────────────
    //  Validation
    // ─────────────────────────────────────────────

    private boolean validateForm() {
        if (txt_nationalId.getText().trim().isBlank()) {
            showWarning("الرقم القومي مطلوب");
            txt_nationalId.requestFocus();
            return false;
        }
        if (txt_startDate.getText().trim().isBlank()) {
            showWarning("تاريخ التعيين مطلوب");
            txt_startDate.requestFocus();
            return false;
        }
        if (txt_law.getText().trim().isBlank()) {
            showWarning("القانون مطلوب (47 أو 81)");
            txt_law.requestFocus();
            return false;
        }
        return true;
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
        dateCol.setCellValueFactory(c -> c.getValue().dateProperty());
        dateCol.setCellFactory(TextFieldTableCell.forTableColumn());
        dateCol.setOnEditCommit(e -> e.getRowValue().setDate(e.getNewValue()));

        TableColumn<DateValueRow, String> valueCol =
                (TableColumn<DateValueRow, String>) table.getColumns().get(1);
        valueCol.setCellValueFactory(c -> c.getValue().valueProperty());
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
            DateValueRow row = new DateValueRow("", "");
            data.add(row);
            table.getSelectionModel().select(row);
            table.scrollTo(row);
            table.edit(data.size() - 1, table.getColumns().get(0));
        });
        delBtn.setOnAction(e -> {
            DateValueRow selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) data.remove(selected);
        });
    }

    // ─────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────

    private List<AdjustmentRecord> toAdjustmentList(ObservableList<DateValueRow> rows) {
        return rows.stream()
                .filter(r -> !r.getDate().isBlank() && !r.getValue().isBlank())
                .map(r -> {
                    LocalDate date = parseDate(r.getDate());
                    BigDecimal amount = parseBigDecimal(r.getValue());
                    if (date == null || amount == null) return null;
                    return new AdjustmentRecord(date, amount);
                })
                .filter(a -> a != null)
                .collect(Collectors.toList());
    }

    private String fmt(LocalDate date) {
        return date != null ? date.format(FMT_DISPLAY) : "";
    }

    private LocalDate parseDate(String text) {
        if (text == null || text.isBlank()) return null;
        text = text.trim();
        try {
            return text.contains("/")
                    ? LocalDate.parse(text, FMT_DISPLAY)
                    : LocalDate.parse(text, FMT_ISO);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private Long parseLong(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            return Long.parseLong(text.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("الرقم القومي غير صالح: " + text);
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

    private String safeStr(Object v) {
        return v != null ? v.toString() : "";
    }

    private void setText(TextField f, Object v) {
        if (f != null) f.setText(v != null ? v.toString() : "");
    }

    private void setButtonsDisabled(boolean b) {
        btn_search.setDisable(b);
        btn_save.setDisable(b);
        btn_calculate.setDisable(b);
        btn_clear.setDisable(b);
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.show();
    }

    private void showWarning(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.show();
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.show();
    }

    // ─────────────────────────────────────────────
    //  Inner Class — DateValueRow
    // ─────────────────────────────────────────────

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