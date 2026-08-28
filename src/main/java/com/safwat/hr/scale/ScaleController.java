package com.safwat.hr.scale;

import com.safwat.hr.network.ApiClient;
import com.safwat.hr.shared.util.DateUtils;
import com.safwat.hr.ui.TextFieldSetupHelper;
import com.safwat.hr.ui.table.TableSetupHelper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

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
    private TableView<AdjustmentRecord> table_mogardAdd;
    @FXML
    private TableView<AdjustmentRecord> table_mogardRival;
    @FXML
    private TableView<AdjustmentRecord> table_bounsAdd;
    @FXML
    private TableView<AdjustmentRecord> table_bounsRival;
    @FXML
    private TableView<ScaleTimelinePoint> table_result;

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


        btn_search.setOnAction(_ -> doSearch());
        btn_calculate.setOnAction(_ -> doCalculate());
        btn_save.setOnAction(_ -> doSave());
        btn_pdf.setOnAction(_ -> doPdf());
        btn_clear.setOnAction(_ -> doClear());

        txt_nationalId.setOnAction(_ -> doSearch());
        setupUpgradeTable();
        setupEncouragementTable();
        setupPromotionTable();
        setupDateFields();
        setTable_result();

        setupAdjustmentTable(table_mogardAdd);
        setupAdjustmentTable(table_mogardRival);
        setupAdjustmentTable(table_bounsAdd);
        setupAdjustmentTable(table_bounsRival);
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
                new TableSetupHelper.ColumnConfig<>("رقم القرار", 120,
                        UpgradeRecord::getDecisionNumber,
                        UpgradeRecord::setDecisionNumber,
                        true, false, ColumnAlign.RIGHT, true)                                  // عمود نصي عادي
        );
        setupGenericTable(table_upgrade, cols, 10, UpgradeRecord::new);
    }

    private void setupEncouragementTable() {
        List<ColumnConfig<EncouragementRecord>> cols = List.of(
                new ColumnConfig<>("تاريخ التشجيعية", 100,
                        r -> formatDateOutput(r.getDate()),
                        (r, v) -> r.setDate(parseDateInput(v)),
                        true, true),
                new ColumnConfig<>("رقم القرار", 120,
                        EncouragementRecord::getDecisionNumber,
                        EncouragementRecord::setDecisionNumber,
                        true, false, ColumnAlign.RIGHT, true)
        );
        setupGenericTable(table_encourge, cols, 10, EncouragementRecord::new);
    }

    private void setupPromotionTable() {
        List<ColumnConfig<PromotionIncentiveRecord>> cols = List.of(
                new ColumnConfig<>("تاريخ الحافز", 100,
                        r -> formatDateOutput(r.getDate()),
                        (r, v) -> r.setDate(parseDateInput(v)),
                        true, true),
                new ColumnConfig<>("رقم القرار", 120,
                        PromotionIncentiveRecord::getDecisionNumber,
                        PromotionIncentiveRecord::setDecisionNumber,
                        true, false, ColumnAlign.RIGHT, true)
        );
        setupGenericTable(table_promotion, cols, 10, PromotionIncentiveRecord::new);
    }

    private void setupAdjustmentTable(TableView<AdjustmentRecord> table) {
        List<ColumnConfig<AdjustmentRecord>> cols = List.of(
                new ColumnConfig<>("التاريخ", 100,
                        r -> formatDateOutput(r.getDate()),
                        (r, v) -> r.setDate(parseDateInput(v)),
                        true, true),
                new ColumnConfig<>("المبلغ", 100,
                        r -> r.getAmount() != null ? r.getAmount().toString() : "",
                        (r, v) -> r.setAmount(parseBigDecimal(v)),
                        true, false, ColumnAlign.RIGHT, true)
        );
        setupGenericTable(table, cols, 5, AdjustmentRecord::new);
    }

    void setTable_result() {
        List<ColumnConfig<ScaleTimelinePoint>> cols = new ArrayList<>();
        cols.add(new ColumnConfig<>("التاريخ", 90,
                r -> formatDateOutput(r.getDate()),
                (r, v) -> r.setDate(parseDateInput(v)),
                false, true));
        cols.add(new ColumnConfig<>("الاساسي", 90,
                r -> r.getCurrentBasic() != null ? r.getCurrentBasic().toString() : "",
                (r, v) -> r.setCurrentBasic(parseBigDecimal(v)),
                false, false));
        cols.add(new ColumnConfig<>("الدورية", 70,
                r -> r.getPeriodicBonus() != null && !r.getPeriodicBonus().equals(BigDecimal.ZERO) ? r.getPeriodicBonus().toString() : "",
                (r, v) -> r.setPeriodicBonus(parseBigDecimal(v)),
                false, false));

        //spBonusSubject
        cols.add(new ColumnConfig<>("خاصة خاضعة", 90,
                r -> r.getSpBonusSubject() != null && !r.getSpBonusSubject().equals(BigDecimal.ZERO) ? r.getSpBonusSubject().toString() : "",
                (r, v) -> r.setSpBonusSubject(parseBigDecimal(v)),
                false, false));
        //spBonusNotSubject
        cols.add(new ColumnConfig<>("خاصة غير خ", 90,
                r -> r.getSpBonusNotSubject() != null && !r.getSpBonusNotSubject().equals(BigDecimal.ZERO) ? r.getSpBonusNotSubject().toString() : "",
                (r, v) -> r.setSpBonusNotSubject(parseBigDecimal(v)),
                false, false));
        //other_sp_subject
        cols.add(new ColumnConfig<>("أخرى خاضعة", 90,
                r -> r.getOther_sp_subject() != null && !r.getOther_sp_subject().equals(BigDecimal.ZERO) ? r.getOther_sp_subject().toString() : "",
                (r, v) -> r.setOther_sp_subject(parseBigDecimal(v)),
                false, false));
        //upgradeBonus
        cols.add(new ColumnConfig<>("ترقية", 90,
                r -> r.getUpgradeBonus() != null && !r.getUpgradeBonus().equals(BigDecimal.ZERO) ? r.getUpgradeBonus().toString() : "",
                (r, v) -> r.setUpgradeBonus(parseBigDecimal(v)),
                false, false));
        //encourageBonus
        cols.add(new ColumnConfig<>("تشجيعية", 90,
                r -> r.getEncourageBonus() != null && !r.getEncourageBonus().equals(BigDecimal.ZERO) ? r.getEncourageBonus().toString() : "",
                (r, v) -> r.setEncourageBonus(parseBigDecimal(v)),
                false, false));
        //otherBonus
        cols.add(new ColumnConfig<>("اخرى", 90,
                r -> r.getOtherBonus() != null && !r.getOtherBonus().equals(BigDecimal.ZERO) ? r.getOtherBonus().toString() : "",
                (r, v) -> r.setOtherBonus(parseBigDecimal(v)),
                false, false));
        //mogard
        cols.add(new ColumnConfig<>("المجرد", 90,
                r -> r.getMogard() != null && !r.getMogard().equals(BigDecimal.ZERO) ? r.getMogard().toString() : "",
                (r, v) -> r.setMogard(parseBigDecimal(v)),
                false, false));
        //basic30_6
        cols.add(new ColumnConfig<>("الأساس 30-6", 90,
                r -> r.getBasic30_6() != null && !r.getBasic30_6().equals(BigDecimal.ZERO) ? r.getBasic30_6().toString() : "",
                (r, v) -> r.setBasic30_6(parseBigDecimal(v)),
                false, false));

        //degreeLabel
        cols.add(new ColumnConfig<>("الدرجة", 90,
                ScaleTimelinePoint::getDegreeLabel,
                ScaleTimelinePoint::setDegreeLabel,
                false, false));
        //extraIncentive
        cols.add(new ColumnConfig<>("حوافز إضافية", 90,
                r -> r.getExtraIncentive() != null && !r.getExtraIncentive().equals(BigDecimal.ZERO) ? r.getExtraIncentive().toString() : "",
                (r, v) -> r.setExtraIncentive(parseBigDecimal(v)),
                false, false));
        //socialPackage
        cols.add(new ColumnConfig<>("الحزمة الاجتماعية", 90,
                r -> r.getSocialPackage() != null && !r.getSocialPackage().equals(BigDecimal.ZERO) ? r.getSocialPackage().toString() : "",
                (r, v) -> r.setSocialPackage(parseBigDecimal(v)),
                false, false));
        setupGenericTable(table_result, cols, 1, ScaleTimelinePoint::new);

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


        fillUpgradeTable(dto.getUpgrades());
        fillEncouragementTable(dto.getEncouragements());
        fillPromotionTable(dto.getPromotionIncentives());
        fillResultTable(dto.getResult().getTimeline());
        fillAdjustmentTable(table_mogardAdd, dto.getMogardAdditions());
        fillAdjustmentTable(table_mogardRival, dto.getMogardRemovals());
        fillAdjustmentTable(table_bounsAdd, dto.getBonusAdditions());
        fillAdjustmentTable(table_bounsRival, dto.getBonusRemovals());
    }


    void fillUpgradeTable(List<UpgradeRecord> upgrades) {
        table_upgrade.getItems().clear();
        if (upgrades != null && !upgrades.isEmpty()) {
            table_upgrade.getItems().addAll(upgrades);
        } else {
            // لو null أو فاضي → ضيف صفين فاضيين
            table_upgrade.getItems().addAll(new UpgradeRecord(), new UpgradeRecord());
        }
    }

    void fillEncouragementTable(List<EncouragementRecord> encouragements) {
        table_encourge.getItems().clear();
        if (encouragements != null && !encouragements.isEmpty()) {
            table_encourge.getItems().addAll(encouragements);
        } else {
            // لو null أو فاضي → ضيف صفين فاضيين
            table_encourge.getItems().addAll(new EncouragementRecord(), new EncouragementRecord());
        }
    }

    void fillPromotionTable(List<PromotionIncentiveRecord> promotions) {
        table_promotion.getItems().clear();
        if (promotions != null && !promotions.isEmpty()) {
            table_promotion.getItems().addAll(promotions);
        } else {
            // لو null أو فاضي → ضيف صفين فاضيين
            table_promotion.getItems().addAll(new PromotionIncentiveRecord(), new PromotionIncentiveRecord());
        }
    }

    void fillResultTable(List<ScaleTimelinePoint> result) {
        table_result.getItems().clear();
        if (result != null && !result.isEmpty()) {
            List<ScaleTimelinePoint> resultCopy = new ArrayList<>();
            // filter all points currentBasic > 0
            for (ScaleTimelinePoint point : result) {
                if (point.getCurrentBasic() != null && point.getCurrentBasic().compareTo(BigDecimal.ZERO) > 0) {
                    resultCopy.add(point);
                }
            }
            table_result.getItems().addAll(resultCopy);
        } else {
            // لو null أو فاضي → ضيف صفين فاضيين
            table_result.getItems().addAll(new ScaleTimelinePoint(), new ScaleTimelinePoint());
        }
    }

    void fillAdjustmentTable(TableView<AdjustmentRecord> table, List<AdjustmentRecord> adjustments) {
        table.getItems().clear();

        if (adjustments != null && !adjustments.isEmpty()) {
            table.getItems().addAll(adjustments);
        } else {
            // لو null أو فاضي → ضيف صفين فاضيين
            table.getItems().addAll(new AdjustmentRecord(), new AdjustmentRecord());
        }
    }


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


}