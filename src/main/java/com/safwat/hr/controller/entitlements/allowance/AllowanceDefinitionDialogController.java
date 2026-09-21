package com.safwat.hr.controller.entitlements.allowance;

import com.fasterxml.jackson.core.type.TypeReference;
import com.safwat.hr.controller.entitlements.allowance.AllowanceDefinition.*;
import com.safwat.hr.controller.entitlements.allowance.dto.JobTitleDto;
import com.safwat.hr.controller.entitlements.allowance.dto.SectorDto;
import com.safwat.hr.shared.ui.MultiSelectSearchDialog;
import com.safwat.hr.ui.TextFieldSetupHelper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.net.URL;
import java.text.Collator;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AllowanceDefinitionDialogController implements Initializable {

    // ══════════════════════════════════════════════════════════════
    //  FXML Fields
    // ══════════════════════════════════════════════════════════════

    @FXML
    private TableView<AllowanceDefinition> table_definitions;
    @FXML
    private TableColumn<AllowanceDefinition, String> col_def_code;
    @FXML
    private TableColumn<AllowanceDefinition, String> col_def_nameAr;
    @FXML
    private TableColumn<AllowanceDefinition, LocalDate> col_def_effectiveFrom;
    @FXML
    private TableColumn<AllowanceDefinition, LocalDate> col_def_effectiveTo;
    @FXML
    private TableColumn<AllowanceDefinition, Behavior> col_def_behavior;
    @FXML
    private TableColumn<AllowanceDefinition, CalcType> col_def_calcType;
    @FXML
    private TableColumn<AllowanceDefinition, ElementType> col_def_elementType;
    @FXML
    private TableColumn<AllowanceDefinition, Scope> col_def_scope;
    @FXML
    private Button btn_new_code;
    @FXML
    private Button btn_new_snapshot;

    @FXML
    private Label lbl_history_title;
    @FXML
    private TableView<AllowanceDefinition> table_history;
    @FXML
    private TableColumn<AllowanceDefinition, LocalDate> col_hist_effectiveFrom;
    @FXML
    private TableColumn<AllowanceDefinition, LocalDate> col_hist_effectiveTo;
    @FXML
    private TableColumn<AllowanceDefinition, Behavior> col_hist_behavior;
    @FXML
    private TableColumn<AllowanceDefinition, CalcType> col_hist_calcType;
    @FXML
    private TableColumn<AllowanceDefinition, BaseSource> col_hist_baseSource;
    @FXML
    private TableColumn<AllowanceDefinition, ElementType> col_hist_elementType;
    @FXML
    private TableColumn<AllowanceDefinition, Void> col_hist_actions;

    // الفورم
    @FXML
    private VBox pane_form;
    @FXML
    private Label lbl_form_title;
    @FXML
    private TextField txt_code;
    @FXML
    private TextField txt_nameAr;
    @FXML
    private TextField txt_nameEn;
    @FXML
    private TextField txt_effectiveFrom;
    @FXML
    private TextField txt_effectiveTo;
    @FXML
    private VBox pane_referenceDate;
    @FXML
    private TextField txt_referenceDate;

    @FXML
    private ComboBox<Behavior> combo_behavior;
    @FXML
    private ComboBox<CalcType> combo_calcType;
    @FXML
    private ComboBox<BaseSource> combo_baseSource;
    @FXML
    private VBox pane_insuranceBase;
    @FXML
    private ComboBox<InsuranceBase> combo_insuranceBase;
    @FXML
    private ComboBox<Scope> combo_scope;
    @FXML
    private ComboBox<ElementType> combo_elementType;
    @FXML
    private ComboBox<TimelineAnchor> combo_timelineAnchor;

    @FXML
    private CheckBox chk_subjectToInsurance;
    @FXML
    private CheckBox chk_subjectToTaxAndStamp;
    @FXML
    private CheckBox chk_inMinimumWageBase;
    @FXML
    private CheckBox chk_displayOnly;
    @FXML
    private CheckBox chk_appliesToNewHires;

    // القطاعات
    @FXML
    private VBox pane_sector;
    @FXML
    private Label lbl_sectorTitle;
    @FXML
    private TextField txt_selectedSectors;
    @FXML
    private Button btn_pick_sectors;

    // الوظائف
    @FXML
    private TextField txt_eligibleJobTitles;
    @FXML
    private Button btn_pick_jobTitles;
    @FXML
    private Label lbl_jobTitlesHint;

    // القوانين
    @FXML
    private TextField txt_eligibleLaws;
    @FXML
    private Button btn_pick_laws;

    // valuesMap
    @FXML
    private VBox pane_valuesMap;
    @FXML
    private TableView<MapEntryRow> table_values;
    @FXML
    private TableColumn<MapEntryRow, String> col_map_key;
    @FXML
    private TableColumn<MapEntryRow, BigDecimal> col_map_value;
    @FXML
    private TableColumn<MapEntryRow, Void> col_map_actions;
    @FXML
    private ComboBox<MapKeyOption> combo_map_key;
    @FXML
    private TextField txt_map_value;
    @FXML
    private Button btn_add_map_entry;

    // الاستثناءات
    @FXML
    private TextField txt_excludedMonths;
    @FXML
    private TextArea txt_notes;

    // أزرار
    @FXML
    private Label lbl_form_error;
    @FXML
    private Button btn_save_definition;
    @FXML
    private Button btn_cancel_form;
    @FXML
    private Button btn_close;

    // ══════════════════════════════════════════════════════════════
    //  Constants & State
    // ══════════════════════════════════════════════════════════════

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * قائمة القوانين المتاحة — بدّلها بـ endpoint لو عندك.
     */
    private static final List<String> AVAILABLE_LAWS = List.of(
            "47", "81"
    );

    private static final Comparator<AllowanceDefinition> ARABIC_NAME_COMPARATOR =
            Comparator.comparing(
                    d -> d.getNameAr() != null ? d.getNameAr() : "",
                    Collator.getInstance(new Locale("ar"))
            );

    private final ObservableList<MapEntryRow> valueRows = FXCollections.observableArrayList();

    // الاختيارات المتعددة
    private final Set<String> selectedSectorCodes = new LinkedHashSet<>();
    private final Set<String> selectedJobTitles = new LinkedHashSet<>();
    private final Set<String> selectedLaws = new LinkedHashSet<>();

    // كاش
    private final List<SectorDto> allSectors = new ArrayList<>();
    private final List<JobTitleDto> allJobTitles = new ArrayList<>();
    private final Map<String, String> sectorNameByCode = new HashMap<>();

    private Long editingId = null;
    private Runnable onChanged;

    private record MapEntryRow(String key, BigDecimal value) {
    }

    private record MapKeyOption(String value, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  Map keys
    // ══════════════════════════════════════════════════════════════

    private static final List<MapKeyOption> DEGREE_KEYS = List.of(
            new MapKeyOption("6", "السادسة"),
            new MapKeyOption("5", "الخامسة"),
            new MapKeyOption("4", "الرابعة"),
            new MapKeyOption("3", "الثالثة"),
            new MapKeyOption("2", "الثانية"),
            new MapKeyOption("1", "الأولى"),
            new MapKeyOption("0", "مدير عام"),
            new MapKeyOption("-1", "العالية"),
            new MapKeyOption("-2", "الممتازة")
    );
    private static final List<MapKeyOption> ALL_JOBS_KEY = List.of(new MapKeyOption("all", "كل الوظائف"));
    private static final List<MapKeyOption> ALL_KEY = List.of(new MapKeyOption("all", "كل الدرجات"));
    private static final List<MapKeyOption> PERCENT_KEY = List.of(new MapKeyOption("percent", "نسبة من المحرك"));
    private static final List<MapKeyOption> MARITAL_KEYS = List.of(
            new MapKeyOption("SINGLE", "أعزب"),
            new MapKeyOption("MARRIED", "متزوج"),
            new MapKeyOption("MARRIED_1_CHILD", "متزوج + طفل"),
            new MapKeyOption("MARRIED_2_CHILDREN", "متزوج + طفلين أو أكثر"),
            new MapKeyOption("WIDOWED", "أرمل"),
            new MapKeyOption("WIDOWED_1_CHILD", "أرمل + طفل"),
            new MapKeyOption("WIDOWED_2_CHILDREN", "أرمل + طفلين أو أكثر"),
            new MapKeyOption("DIVORCED", "مطلق"),
            new MapKeyOption("DIVORCED_1_CHILD", "مطلق + طفل"),
            new MapKeyOption("DIVORCED_2_CHILDREN", "مطلق + طفلين أو أكثر")
    );
    private static final List<MapKeyOption> INSURANCE_KEYS = List.of(new MapKeyOption("rate", "النسبة"));

    private static final Map<String, String> KEY_LABELS = buildKeyLabels();

    private static Map<String, String> buildKeyLabels() {
        Map<String, String> m = new LinkedHashMap<>();
        DEGREE_KEYS.forEach(k -> m.put(k.value(), k.label()));
        ALL_KEY.forEach(k -> m.put(k.value(), k.label()));
        ALL_JOBS_KEY.forEach(k -> m.put(k.value(), k.label()));
        PERCENT_KEY.forEach(k -> m.put(k.value(), k.label()));
        MARITAL_KEYS.forEach(k -> m.put(k.value(), k.label()));
        INSURANCE_KEYS.forEach(k -> m.put(k.value(), k.label()));
        return Map.copyOf(m);
    }

    private static List<MapKeyOption> mapKeyOptionsFor(CalcType t) {
        if (t == null) return List.of();
        return switch (t) {
            case PERCENT_BY_DEGREE, AMOUNT_BY_DEGREE, SUPPLEMENTARY_BONUS -> DEGREE_KEYS;
            case FIXED_AMOUNT, PERCENT_ALL_DEGREE -> ALL_KEY;
            case SALARY_ENGINE, DEPENDS_SALARY_ENGINE -> PERCENT_KEY;
            case FIXED_AMOUNT_BY_MARITAL_STATUS -> MARITAL_KEYS;
            case INSURANCE_RATE_EMPLOYEE, INSURANCE_RATE_EMPLOYER -> INSURANCE_KEYS;
            case PERCENT_ALL_JOB -> ALL_JOBS_KEY;   // 🆕 بدل ALL_KEY
            case PERCENT_BY_JOB, AMOUNT_BY_JOB -> List.of();
            case COMPENSATORY_BONUS, SPECIAL_ALLOWANCE_ADDED, SPECIAL_ALLOWANCE_NOT_ADDED,
                 SOCIAL_PACKAGE_MINIMUM, PROMOTION_INCENTIVE -> List.of();
        };
    }

    private static boolean calcTypeNeedsValues(CalcType t) {
        if (t == null) return false;
        if (isJobType(t)) return true;
        return !mapKeyOptionsFor(t).isEmpty();
    }

    private static boolean isInsuranceType(CalcType t) {
        return t == CalcType.INSURANCE_RATE_EMPLOYEE || t == CalcType.INSURANCE_RATE_EMPLOYER;
    }

    private static boolean isEmployeeInsurance(CalcType t) {
        return t == CalcType.INSURANCE_RATE_EMPLOYEE;
    }

    private static boolean isEmployerInsurance(CalcType t) {
        return t == CalcType.INSURANCE_RATE_EMPLOYER;
    }

    private static boolean isJobType(CalcType t) {
        return t == CalcType.PERCENT_BY_JOB || t == CalcType.AMOUNT_BY_JOB;
    }


    // ══════════════════════════════════════════════════════════════
    //  Labels
    // ══════════════════════════════════════════════════════════════

    private static String behaviorLabel(Behavior b) {
        return switch (b) {

            case REPLACE -> "استبدال (REPLACE)";
            case ADD -> "إضافة (ADD)";
        };
    }

    private static String calcTypeLabel(CalcType c) {
        return switch (c) {
            case PERCENT_BY_DEGREE -> "نسبة % حسب الدرجة";
            case PERCENT_ALL_DEGREE -> "نسبة لكل الدرجات";
            case AMOUNT_BY_DEGREE -> "مبلغ ثابت حسب الدرجة";
            case FIXED_AMOUNT -> "مبلغ ثابت لكل الدرجات";
            case SALARY_ENGINE -> "نسبة من إجمالي المحرك";
            case DEPENDS_SALARY_ENGINE -> "مبلغ بعد اكتمال المحرك";
            case COMPENSATORY_BONUS -> "الحافز التعويضي";
            case SUPPLEMENTARY_BONUS -> "الحافز التكميلي";
            case SPECIAL_ALLOWANCE_ADDED -> "علاوة خاصة مضافة";
            case SPECIAL_ALLOWANCE_NOT_ADDED -> "علاوة خاصة غير مضافة";
            case SOCIAL_PACKAGE_MINIMUM -> "علاوة الحزمة الاجتماعية";
            case FIXED_AMOUNT_BY_MARITAL_STATUS -> "مبلغ ثابت حسب الحالة الاجتماعية";
            case INSURANCE_RATE_EMPLOYEE -> "تأمين — حصة الموظف";
            case INSURANCE_RATE_EMPLOYER -> "تأمين — حصة الحكومة";
            case PROMOTION_INCENTIVE -> "حافز ترقية";
            case PERCENT_BY_JOB -> "نسبة % حسب الوظيفة";
            case PERCENT_ALL_JOB -> "نسبة لكل الوظائف";
            case AMOUNT_BY_JOB -> "مبلغ ثابت حسب الوظيفة";
        };
    }

    private static String insuranceBaseLabel(InsuranceBase b) {
        return switch (b) {
            case BASIC -> "الأجر الأساسي";
            case VARIABLE -> "الأجر المتغير";
            case COMBINED -> "الأجر الاشتراكي (موحد)";
        };
    }

    private static String baseSourceLabel(BaseSource s) {
        return switch (s) {
            case FROM_BASIC -> "من الأساسي 30/6";
            case FROM_STEP_SALARY -> "من مربوط الدرجة";
            case CURRENT_BASIC -> "من الأساسي الحالي";
        };
    }

    private static String scopeLabel(Scope s) {
        return switch (s) {
            case GENERAL -> "عام (لكل المستحقين)";
            case SPECIAL -> "خاص (يدوي فقط)";
        };
    }

    private static String elementTypeLabel(ElementType e) {
        return switch (e) {
            case ENTITLEMENT -> "استحقاق";
            case DEDUCTION -> "استقطاع";
            case INSURANCE -> "عنصر تأمينات";
            case TAX -> "عنصر ضريبة";
            case STAMP -> "عنصر دمغة";
        };
    }

    private static String timelineAnchorLabel(TimelineAnchor a) {
        return switch (a) {
            case TARGET_DATE -> "شهرى";
            case EFFECTIVE_FROM -> "شهر القرار";
        };
    }

    @SuppressWarnings("unchecked")
    private static <E> StringConverter<E> arabicConverter(Function<Object, String> labelFn) {
        return new StringConverter<>() {
            @Override
            public String toString(E val) {
                return val == null ? "" : labelFn.apply(val);
            }

            @Override
            public E fromString(String s) {
                return null;
            }
        };
    }

    // ══════════════════════════════════════════════════════════════
    //  Initialize
    // ══════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupDefinitionsTable();
        setupHistoryTable();
        setupValuesTable();
        setupCombos();

        TextFieldSetupHelper.setupDateFields(
                txt_effectiveFrom, txt_effectiveTo, txt_referenceDate);

        btn_new_code.setOnAction(e -> showFormForNewCode());
        btn_new_snapshot.setOnAction(e -> showFormForNewSnapshot());
        btn_save_definition.setOnAction(e -> handleSave());
        btn_cancel_form.setOnAction(e -> hideForm());
        btn_add_map_entry.setOnAction(e -> handleAddMapEntry());
        btn_close.setOnAction(e -> closeDialog());

        btn_pick_sectors.setOnAction(e -> openSectorsDialog());
        btn_pick_jobTitles.setOnAction(e -> openJobTitlesDialog());
        btn_pick_laws.setOnAction(e -> openLawsDialog());

        combo_calcType.valueProperty().addListener((o, old, val) -> autoAdjustForCalcType(val));

        // تحميل البيانات المرجعية
        loadAllSectorsOnce();
        loadAllJobTitlesOnce();

        hideForm();
        loadDefinitions();
    }

    public void init(Runnable onChanged) {
        this.onChanged = onChanged;
    }

    private Stage getStage() {
        return (Stage) btn_close.getScene().getWindow();
    }

    // ══════════════════════════════════════════════════════════════
    //  Auto Adjust
    // ══════════════════════════════════════════════════════════════

    private void autoAdjustForCalcType(CalcType val) {
        if (val == null) return;

        // 1) baseSource
        boolean needsBaseSource = val == CalcType.PERCENT_BY_DEGREE
                || val == CalcType.PERCENT_ALL_DEGREE
                || val == CalcType.PERCENT_BY_JOB
                || val == CalcType.PERCENT_ALL_JOB;
        combo_baseSource.setDisable(!needsBaseSource);
        if (!needsBaseSource) combo_baseSource.setValue(null);

        // 2) referenceDate
        boolean needsRefDate = val == CalcType.SPECIAL_ALLOWANCE_ADDED
                || val == CalcType.SPECIAL_ALLOWANCE_NOT_ADDED
                || val == CalcType.SOCIAL_PACKAGE_MINIMUM;
        if (pane_referenceDate != null) {
            pane_referenceDate.setVisible(needsRefDate);
            pane_referenceDate.setManaged(needsRefDate);
        }
        if (!needsRefDate) txt_referenceDate.clear();

        // 3) insuranceBase
        boolean needsInsBase = isInsuranceType(val);
        if (pane_insuranceBase != null) {
            pane_insuranceBase.setVisible(needsInsBase);
            pane_insuranceBase.setManaged(needsInsBase);
        }
        if (needsInsBase && combo_insuranceBase != null
                && combo_insuranceBase.getValue() == null) {
            combo_insuranceBase.setValue(InsuranceBase.COMBINED);
        }

        // 4) القطاع — دايماً مرئي، بنغيّر العنوان بس
        updateSectorTitle(val);

        // 5) elementType + displayOnly
        if (isEmployeeInsurance(val)) {
            combo_elementType.setValue(ElementType.DEDUCTION);
            combo_elementType.setDisable(true);
            chk_displayOnly.setSelected(false);
            chk_displayOnly.setDisable(true);
        } else if (isEmployerInsurance(val)) {
            combo_elementType.setValue(ElementType.ENTITLEMENT);
            combo_elementType.setDisable(true);
            chk_displayOnly.setSelected(true);
            chk_displayOnly.setDisable(true);
        } else {
            combo_elementType.setDisable(false);
            chk_displayOnly.setDisable(false);
        }

        // 6) checkboxes التأمينات
        if (isInsuranceType(val)) {
            chk_subjectToInsurance.setSelected(false);
            chk_subjectToInsurance.setDisable(true);
            chk_subjectToTaxAndStamp.setSelected(false);
            chk_subjectToTaxAndStamp.setDisable(true);
            chk_inMinimumWageBase.setSelected(false);
            chk_inMinimumWageBase.setDisable(true);
        } else {
            chk_subjectToInsurance.setDisable(false);
            chk_subjectToTaxAndStamp.setDisable(false);
            chk_inMinimumWageBase.setDisable(false);
        }

        // 7) valuesMap keys + تحديث hint
        updateMapKeyOptionsFor(val);
        updateJobTitlesHint();
    }

    private void updateSectorTitle(CalcType val) {
        if (lbl_sectorTitle == null) return;
        if (isJobCalcType(val)) {
            lbl_sectorTitle.setText("القطاعات (اختياري — لفلترة قائمة الوظائف)");
        } else {
            lbl_sectorTitle.setText("القطاعات (اختياري — فارغ = الكل)");
        }
    }

    /**
     * الأنواع الوظيفية بتحتاج قطاع أو وظائف — لكن مش إلزامي
     */
    private static boolean isJobCalcType(CalcType t) {
        return isJobType(t);
    }

    private void updateJobTitlesHint() {
        if (lbl_jobTitlesHint == null) return;
        CalcType ct = combo_calcType.getValue();
        boolean isJob = isJobCalcType(ct);
        boolean hasSectors = !selectedSectorCodes.isEmpty();

        if (isJob) {
            if (hasSectors) {
                lbl_jobTitlesHint.setText("القائمة مقيّدة بالقطاعات المختارة ("
                        + selectedSectorCodes.size() + " قطاع).");
            } else {
                lbl_jobTitlesHint.setText(
                        "بدون قطاع → القائمة تعرض كل الوظائف. اختار وظائف محددة لتفعيل الفلترة.");
            }
        } else {
            if (hasSectors) {
                lbl_jobTitlesHint.setText("القائمة مقيّدة بالقطاعات المختارة ("
                        + selectedSectorCodes.size() + " قطاع).");
            } else {
                lbl_jobTitlesHint.setText(
                        "اتركه فارغاً لتطبيقه على كل الوظائف، أو اختر وظائف محددة.");
            }
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  Data Loading
    // ══════════════════════════════════════════════════════════════

    private void loadDefinitions() {
        FxApiSupport.getList(
                "/entitlements/allowances/definitions",
                new TypeReference<List<AllowanceDefinition>>() {
                },
                defs -> {
                    List<AllowanceDefinition> sorted = defs.stream()
                            .sorted(ARABIC_NAME_COMPARATOR).toList();
                    table_definitions.setItems(FXCollections.observableArrayList(sorted));
                },
                err -> showFormError("خطأ في تحميل القواعد: " + err)
        );
    }

    private void loadHistory(String code) {
        FxApiSupport.getList(
                "/entitlements/allowances/definitions/" + code + "/history",
                new TypeReference<List<AllowanceDefinition>>() {
                },
                history -> table_history.setItems(FXCollections.observableArrayList(history)),
                err -> showFormError("خطأ في تحميل التاريخ: " + err)
        );
    }

    private void loadAllSectorsOnce() {
        FxApiSupport.getList("/sectors",
                new TypeReference<List<SectorDto>>() {
                },
                list -> {
                    allSectors.clear();
                    allSectors.addAll(list);
                    sectorNameByCode.clear();
                    for (SectorDto s : list) {
                        sectorNameByCode.put(s.getCode(),
                                s.getNameAr() != null ? s.getNameAr() : s.getCode());
                    }
                },
                err -> { /* silent */ });
    }

    private void loadAllJobTitlesOnce() {
        FxApiSupport.getList("/entitlements/allowances/job-titles",
                new TypeReference<List<JobTitleDto>>() {
                },
                list -> {
                    allJobTitles.clear();
                    allJobTitles.addAll(list);
                    updateJobKeyOptions();
                },
                err -> showFormError("خطأ في تحميل الوظائف: " + err));
    }

    // ══════════════════════════════════════════════════════════════
    //  Multi-Select Dialogs
    // ══════════════════════════════════════════════════════════════

    private void openSectorsDialog() {
        if (allSectors.isEmpty()) {
            showFormError("جاري تحميل القطاعات، حاول بعد لحظات");
            return;
        }

        List<SectorDto> preSelected = allSectors.stream()
                .filter(s -> selectedSectorCodes.contains(s.getCode()))
                .collect(Collectors.toList());

        List<SectorDto> picked = MultiSelectSearchDialog.builder(SectorDto.class)
                .title("اختيار القطاعات")
                .column("الكود", SectorDto::getCode)
                .column("الاسم", SectorDto::getNameAr)
                .data(allSectors)
                .preSelected(preSelected)
                .searchPlaceholder("ابحث عن قطاع...")
                .owner(getStage())
                .showAndWait();

        selectedSectorCodes.clear();
        for (SectorDto s : picked) selectedSectorCodes.add(s.getCode());

        // حذف الوظائف المختارة اللي بقت خارج النطاق (لو في قطاع محدد)
        if (!selectedSectorCodes.isEmpty()) {
            Set<String> validNames = getAvailableJobTitles().stream()
                    .map(JobTitleDto::getNameAr)
                    .collect(Collectors.toSet());
            selectedJobTitles.retainAll(validNames);
        }

        updateSectorsLabel();
        updateJobTitlesLabel();
        updateJobKeyOptions();
        updateJobTitlesHint();
    }

    private void openJobTitlesDialog() {
        List<JobTitleDto> available = getAvailableJobTitles();
        if (available.isEmpty()) {
            showFormError(isJobType(combo_calcType.getValue())
                    ? "اختر قطاعاً أولاً لتحميل الوظائف"
                    : "لا توجد وظائف متاحة");
            return;
        }

        List<JobTitleDto> preSelected = available.stream()
                .filter(j -> selectedJobTitles.contains(j.getNameAr()))
                .collect(Collectors.toList());

        List<JobTitleDto> picked = MultiSelectSearchDialog.builder(JobTitleDto.class)
                .title("اختيار الوظائف المستحقة")
                .column("اسم الوظيفة", JobTitleDto::getDisplayLabel)
                .column("القطاع", JobTitleDto::getSectorNameAr)
                .column("قانون 81", j -> Boolean.TRUE.equals(j.getSubjectToLaw81())
                        ? "✔ خاضع" : "✗ غير خاضع")
                .data(available)
                .preSelected(preSelected)
                .searchPlaceholder("ابحث عن وظيفة...")
                .owner(getStage())
                .showAndWait();

        selectedJobTitles.clear();
        for (JobTitleDto j : picked) selectedJobTitles.add(j.getNameAr());
        updateJobTitlesLabel();
    }

    private void openLawsDialog() {
        List<String> preSelected = new ArrayList<>(selectedLaws);
        List<String> picked = MultiSelectSearchDialog.forStrings()
                .title("اختيار القوانين")
                .data(AVAILABLE_LAWS)
                .preSelected(preSelected)
                .searchPlaceholder("ابحث عن قانون...")
                .owner(getStage())
                .showAndWait();

        selectedLaws.clear();
        selectedLaws.addAll(picked);
        updateLawsLabel();
    }

    // ══════════════════════════════════════════════════════════════
    //  Job Titles — Available Source
    // ══════════════════════════════════════════════════════════════

    /**
     * الوظائف المتاحة:
     * - لو في قطاع مختار → وظائف القطاع فقط
     * - غير كده → كل الوظائف
     */
    private List<JobTitleDto> getAvailableJobTitles() {
        if (!selectedSectorCodes.isEmpty()) {
            return allJobTitles.stream()
                    .filter(j -> selectedSectorCodes.contains(j.getSectorCode()))
                    .collect(Collectors.toList());
        }
        return allJobTitles;
    }

    private void updateJobKeyOptions() {
        if (!isJobType(combo_calcType.getValue())) return;
        List<MapKeyOption> options = getAvailableJobTitles().stream()
                .map(j -> new MapKeyOption(j.getNameAr(), j.getDisplayLabel()))
                .distinct()
                .collect(Collectors.toList());
        combo_map_key.setItems(FXCollections.observableArrayList(options));
    }

    // ══════════════════════════════════════════════════════════════
    //  Labels
    // ══════════════════════════════════════════════════════════════

    private void updateSectorsLabel() {
        if (selectedSectorCodes.isEmpty()) {
            txt_selectedSectors.setText("");
            txt_selectedSectors.setPromptText("(فارغ = الكل)");
        } else {
            String names = selectedSectorCodes.stream()
                    .map(c -> sectorNameByCode.getOrDefault(c, c))
                    .collect(Collectors.joining("، "));
            txt_selectedSectors.setText(names);
        }
    }

    private void updateJobTitlesLabel() {
        if (selectedJobTitles.isEmpty()) {
            txt_eligibleJobTitles.setText("");
            txt_eligibleJobTitles.setPromptText("(فارغ = الكل)");
            return;
        }
        String display = selectedJobTitles.stream()
                .map(key -> allJobTitles.stream()
                        .filter(j -> key.equals(j.getNameAr()))
                        .findFirst()
                        .map(JobTitleDto::getDisplayLabel)
                        .orElse(key))
                .collect(Collectors.joining("، "));
        txt_eligibleJobTitles.setText(display);
    }

    private void updateLawsLabel() {
        if (selectedLaws.isEmpty()) {
            txt_eligibleLaws.setText("");
            txt_eligibleLaws.setPromptText("(فارغ = الكل)");
        } else {
            txt_eligibleLaws.setText(String.join("، ", selectedLaws));
        }
    }

    private void clearSectorSelection() {
        selectedSectorCodes.clear();
        updateSectorsLabel();
        updateJobKeyOptions();
    }

    // ══════════════════════════════════════════════════════════════
    //  Tables Setup
    // ══════════════════════════════════════════════════════════════

    private void setupDefinitionsTable() {
        col_def_code.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getCode()));
        col_def_nameAr.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getNameAr()));
        col_def_effectiveFrom.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getEffectiveFrom()));
        col_def_effectiveFrom.setCellFactory(tc -> dateCell());
        col_def_effectiveTo.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getEffectiveTo()));
        col_def_effectiveTo.setCellFactory(tc -> dateCell());
        col_def_behavior.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getBehavior()));
        col_def_behavior.setCellFactory(tc -> behaviorCell());
        col_def_calcType.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getCalcType()));
        col_def_calcType.setCellFactory(tc -> calcTypeCell());
        col_def_elementType.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getElementType()));
        col_def_elementType.setCellFactory(tc -> elementTypeCell());
        col_def_scope.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getScope()));
        col_def_scope.setCellFactory(tc -> scopeCell());

        table_definitions.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) {
                lbl_history_title.setText("تاريخ: " + sel.getNameAr() + " (" + sel.getCode() + ")");
                btn_new_snapshot.setDisable(false);
                loadHistory(sel.getCode());
            } else {
                lbl_history_title.setText("اختر بدل من الأعلى لعرض تاريخه");
                btn_new_snapshot.setDisable(true);
                table_history.getItems().clear();
            }
        });
        btn_new_snapshot.setDisable(true);
    }

    private void setupHistoryTable() {
        col_hist_effectiveFrom.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getEffectiveFrom()));
        col_hist_effectiveFrom.setCellFactory(tc -> dateCell());
        col_hist_effectiveTo.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getEffectiveTo()));
        col_hist_effectiveTo.setCellFactory(tc -> dateCell());
        col_hist_behavior.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getBehavior()));
        col_hist_behavior.setCellFactory(tc -> behaviorCell());
        col_hist_calcType.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getCalcType()));
        col_hist_calcType.setCellFactory(tc -> calcTypeCell());
        col_hist_baseSource.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getBaseSource()));
        col_hist_baseSource.setCellFactory(tc -> baseSourceCell());
        col_hist_elementType.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getElementType()));
        col_hist_elementType.setCellFactory(tc -> elementTypeCell());

        col_hist_actions.setCellFactory(tc -> new TableCell<>() {
            private final Button btnEdit = new Button("✏");
            private final Button btnDelete = new Button("🗑");

            {
                btnEdit.getStyleClass().add("btn-purple");
                btnEdit.setPrefHeight(28);
                btnEdit.setPrefWidth(40);
                btnDelete.getStyleClass().add("btn-danger");
                btnDelete.setPrefHeight(28);
                btnDelete.setPrefWidth(40);
                btnEdit.setOnAction(e -> showFormForEdit(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> handleDelete(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                HBox box = new HBox(8, btnEdit, btnDelete);
                box.setStyle("-fx-alignment: CENTER;");
                setGraphic(box);
            }
        });
    }

    private void setupValuesTable() {
        col_map_key.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(mapKeyDisplay(d.getValue().key())));
        col_map_value.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().value()));
        col_map_actions.setCellFactory(tc -> new TableCell<>() {
            private final Button btnDelete = new Button("🗑");

            {
                btnDelete.getStyleClass().add("btn-danger");
                btnDelete.setPrefHeight(26);
                btnDelete.setPrefWidth(36);
                btnDelete.setOnAction(e -> valueRows.remove(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : btnDelete);
            }
        });
        table_values.setItems(valueRows);
    }

    private void setupCombos() {
        combo_behavior.setItems(FXCollections.observableArrayList(Behavior.values()));
        combo_behavior.setConverter(arabicConverter(v -> behaviorLabel((Behavior) v)));
        combo_calcType.setItems(FXCollections.observableArrayList(CalcType.values()));
        combo_calcType.setConverter(arabicConverter(v -> calcTypeLabel((CalcType) v)));
        combo_baseSource.setItems(FXCollections.observableArrayList(BaseSource.values()));
        combo_baseSource.setConverter(arabicConverter(v -> baseSourceLabel((BaseSource) v)));
        if (combo_insuranceBase != null) {
            combo_insuranceBase.setItems(FXCollections.observableArrayList(InsuranceBase.values()));
            combo_insuranceBase.setConverter(arabicConverter(v -> insuranceBaseLabel((InsuranceBase) v)));
        }
        combo_scope.setItems(FXCollections.observableArrayList(Scope.values()));
        combo_scope.setConverter(arabicConverter(v -> scopeLabel((Scope) v)));
        combo_elementType.setItems(FXCollections.observableArrayList(ElementType.values()));
        combo_elementType.setConverter(arabicConverter(v -> elementTypeLabel((ElementType) v)));
        combo_timelineAnchor.setItems(FXCollections.observableArrayList(TimelineAnchor.values()));
        combo_timelineAnchor.setConverter(arabicConverter(v -> timelineAnchorLabel((TimelineAnchor) v)));
        combo_map_key.setVisibleRowCount(12);
    }

    private void updateMapKeyOptionsFor(CalcType calcType) {
        if (isJobType(calcType)) {
            updateJobKeyOptions();
            if (pane_valuesMap != null) {
                pane_valuesMap.setVisible(true);
                pane_valuesMap.setManaged(true);
            }
            return;
        }
        List<MapKeyOption> options = mapKeyOptionsFor(calcType);
        combo_map_key.setItems(FXCollections.observableArrayList(options));
        combo_map_key.setValue(null);
        boolean needsValues = !options.isEmpty();
        if (pane_valuesMap != null) {
            pane_valuesMap.setVisible(needsValues);
            pane_valuesMap.setManaged(needsValues);
        }
        if (!needsValues) valueRows.clear();
    }

    private static String mapKeyDisplay(String rawKey) {
        if (rawKey == null) return "";
        return KEY_LABELS.getOrDefault(rawKey, rawKey);
    }

    // ══════════════════════════════════════════════════════════════
    //  Cells
    // ══════════════════════════════════════════════════════════════

    private TableCell<AllowanceDefinition, LocalDate> dateCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setText(empty || date == null ? "—" : date.format(DATE_FMT));
                setStyle("-fx-alignment: CENTER;");
            }
        };
    }

    private TableCell<AllowanceDefinition, Behavior> behaviorCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Behavior val, boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null ? null : behaviorLabel(val));
                setStyle("-fx-alignment: CENTER;");
            }
        };
    }

    private TableCell<AllowanceDefinition, CalcType> calcTypeCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(CalcType val, boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null ? null : calcTypeLabel(val));
                setStyle("-fx-alignment: CENTER;");
            }
        };
    }

    private TableCell<AllowanceDefinition, BaseSource> baseSourceCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(BaseSource val, boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null ? "—" : baseSourceLabel(val));
                setStyle("-fx-alignment: CENTER;");
            }
        };
    }

    private TableCell<AllowanceDefinition, ElementType> elementTypeCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(ElementType val, boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null ? null : elementTypeLabel(val));
                setStyle("-fx-alignment: CENTER;");
            }
        };
    }

    private TableCell<AllowanceDefinition, Scope> scopeCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Scope val, boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null ? null : scopeLabel(val));
                setStyle("-fx-alignment: CENTER;");
            }
        };
    }

    // ══════════════════════════════════════════════════════════════
    //  Form — Show / Hide
    // ══════════════════════════════════════════════════════════════

    private void showFormForNewCode() {
        clearForm();
        editingId = null;
        txt_code.setDisable(true);
        lbl_form_title.setText("إضافة بدل جديد بالكامل");
        showForm();
    }

    private void showFormForNewSnapshot() {
        AllowanceDefinition selected = table_definitions.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        clearForm();
        editingId = null;
        txt_code.setText(selected.getCode());
        txt_code.setDisable(true);
        txt_nameAr.setText(selected.getNameAr());
        txt_nameEn.setText(selected.getNameEn());
        combo_elementType.setValue(selected.getElementType() != null
                ? selected.getElementType() : ElementType.ENTITLEMENT);
        combo_calcType.setValue(selected.getCalcType());
        if (combo_insuranceBase != null) combo_insuranceBase.setValue(selected.getInsuranceBase());

        applySelectionsFromDefinition(selected);

        lbl_form_title.setText("إضافة تاريخ جديد لبدل: " + selected.getCode());
        showForm();
    }

    private void showFormForEdit(AllowanceDefinition snap) {
        clearForm();
        editingId = snap.getId();
        txt_code.setText(snap.getCode());
        txt_code.setDisable(true);
        txt_nameAr.setText(snap.getNameAr());
        txt_nameEn.setText(snap.getNameEn());
        if (snap.getEffectiveFrom() != null)
            txt_effectiveFrom.setText(snap.getEffectiveFrom().format(DATE_FMT));
        if (snap.getEffectiveTo() != null)
            txt_effectiveTo.setText(snap.getEffectiveTo().format(DATE_FMT));

        combo_behavior.setValue(snap.getBehavior());
        combo_calcType.setValue(snap.getCalcType());
        combo_baseSource.setValue(snap.getBaseSource());
        if (combo_insuranceBase != null) combo_insuranceBase.setValue(snap.getInsuranceBase());
        combo_scope.setValue(snap.getScope() != null ? snap.getScope() : Scope.GENERAL);
        combo_elementType.setValue(snap.getElementType() != null
                ? snap.getElementType() : ElementType.ENTITLEMENT);
        combo_timelineAnchor.setValue(snap.getTimelineAnchor() != null
                ? snap.getTimelineAnchor() : TimelineAnchor.TARGET_DATE);
        chk_subjectToInsurance.setSelected(snap.isSubjectToInsurance());
        chk_subjectToTaxAndStamp.setSelected(snap.isSubjectToTaxAndStamp());
        chk_inMinimumWageBase.setSelected(snap.isInMinimumWageBase());
        chk_displayOnly.setSelected(snap.isDisplayOnly());
        chk_appliesToNewHires.setSelected(snap.isAppliesToNewHires());
        if (snap.getReferenceDate() != null)
            txt_referenceDate.setText(snap.getReferenceDate().format(DATE_FMT));
        txt_excludedMonths.setText(joinOrEmpty(snap.getExcludedMonths()));
        txt_notes.setText(snap.getNotes());

        applySelectionsFromDefinition(snap);

        updateMapKeyOptionsFor(snap.getCalcType());

        valueRows.clear();
        if (snap.getValuesMap() != null) {
            snap.getValuesMap().forEach((k, v) -> valueRows.add(new MapEntryRow(k, v)));
        }

        autoAdjustForCalcType(snap.getCalcType());

        lbl_form_title.setText("تعديل: " + snap.getCode() + " @ " + snap.getEffectiveFrom());
        showForm();
    }

    private void applySelectionsFromDefinition(AllowanceDefinition def) {
        // القطاعات
        selectedSectorCodes.clear();
        if (def.getEligibleSectorCodes() != null) {
            selectedSectorCodes.addAll(def.getEligibleSectorCodes());
        }
        updateSectorsLabel();

        // الوظائف
        selectedJobTitles.clear();
        if (def.getEligibleJobTitles() != null) {
            selectedJobTitles.addAll(def.getEligibleJobTitles());
        }
        updateJobTitlesLabel();

        // القوانين
        selectedLaws.clear();
        if (def.getEligibleLaws() != null) {
            selectedLaws.addAll(def.getEligibleLaws());
        }
        updateLawsLabel();

        updateJobKeyOptions();
        updateJobTitlesHint();
    }

    private void clearForm() {
        txt_code.clear();
        txt_code.setDisable(true);
        txt_nameAr.clear();
        txt_nameEn.clear();
        txt_effectiveFrom.clear();
        txt_effectiveTo.clear();
        txt_referenceDate.clear();
        combo_behavior.setValue(null);
        combo_calcType.setValue(null);
        combo_baseSource.setValue(null);
        if (combo_insuranceBase != null) combo_insuranceBase.setValue(null);
        combo_scope.setValue(Scope.GENERAL);
        combo_elementType.setValue(ElementType.ENTITLEMENT);
        combo_elementType.setDisable(false);
        chk_subjectToInsurance.setSelected(false);
        chk_subjectToInsurance.setDisable(false);
        chk_subjectToTaxAndStamp.setSelected(false);
        chk_subjectToTaxAndStamp.setDisable(false);
        chk_inMinimumWageBase.setSelected(false);
        chk_inMinimumWageBase.setDisable(false);
        chk_displayOnly.setSelected(false);
        chk_displayOnly.setDisable(false);
        chk_appliesToNewHires.setSelected(true);
        txt_excludedMonths.clear();
        txt_notes.clear();
        valueRows.clear();
        combo_map_key.setItems(FXCollections.observableArrayList());
        combo_map_key.setValue(null);
        txt_map_value.clear();
        combo_timelineAnchor.setValue(TimelineAnchor.TARGET_DATE);
        if (pane_referenceDate != null) {
            pane_referenceDate.setVisible(false);
            pane_referenceDate.setManaged(false);
        }
        if (pane_insuranceBase != null) {
            pane_insuranceBase.setVisible(false);
            pane_insuranceBase.setManaged(false);
        }
        if (pane_valuesMap != null) {
            pane_valuesMap.setVisible(false);
            pane_valuesMap.setManaged(false);
        }

        selectedSectorCodes.clear();
        selectedJobTitles.clear();
        selectedLaws.clear();
        updateSectorsLabel();
        updateJobTitlesLabel();
        updateLawsLabel();
        updateJobTitlesHint();

        lbl_form_error.setVisible(false);
    }

    private void showForm() {
        pane_form.setVisible(true);
        pane_form.setManaged(true);
    }

    private void hideForm() {
        pane_form.setVisible(false);
        pane_form.setManaged(false);
        editingId = null;
    }

    // ══════════════════════════════════════════════════════════════
    //  valuesMap — Add entry
    // ══════════════════════════════════════════════════════════════

    private void handleAddMapEntry() {
        MapKeyOption keyOpt = combo_map_key.getValue();
        String valStr = txt_map_value.getText().trim();

        String key;
        if (keyOpt != null) {
            key = keyOpt.value();
        } else if (isJobType(combo_calcType.getValue())) {
            showFormError(selectedSectorCodes.isEmpty()
                    ? "اختر القطاع أولاً لتحميل الوظائف"
                    : "اختر وظيفة من القائمة");
            return;
        } else {
            showFormError("اختر المفتاح من القائمة");
            return;
        }

        if (valStr.isBlank()) {
            showFormError("القيمة مطلوبة");
            return;
        }

        BigDecimal value;
        try {
            value = new BigDecimal(valStr);
        } catch (NumberFormatException ex) {
            showFormError("القيمة يجب أن تكون رقماً");
            return;
        }

        valueRows.removeIf(r -> r.key().equals(key));
        valueRows.add(new MapEntryRow(key, value));
        combo_map_key.setValue(null);
        txt_map_value.clear();
        lbl_form_error.setVisible(false);
    }

    // ══════════════════════════════════════════════════════════════
    //  Save
    // ══════════════════════════════════════════════════════════════

    private void handleSave() {
        lbl_form_error.setVisible(false);

        String code = txt_code.getText().trim();
        String nameAr = txt_nameAr.getText().trim();
        Behavior behavior = combo_behavior.getValue();
        CalcType calcType = combo_calcType.getValue();
        BaseSource baseSource = combo_baseSource.getValue();
        InsuranceBase insuranceBase = combo_insuranceBase != null
                ? combo_insuranceBase.getValue() : null;
        Scope scope = combo_scope.getValue();
        ElementType elementType = combo_elementType.getValue();
        TimelineAnchor timelineAnchor = combo_timelineAnchor.getValue();

        // الاختيارات
        List<String> eligibleSectorCodes = selectedSectorCodes.isEmpty()
                ? null : new ArrayList<>(selectedSectorCodes);
        List<String> eligibleJobTitles = selectedJobTitles.isEmpty()
                ? null : new ArrayList<>(selectedJobTitles);
        List<String> eligibleLaws = selectedLaws.isEmpty()
                ? null : new ArrayList<>(selectedLaws);

        // التواريخ
        LocalDate effectiveFrom = TextFieldSetupHelper.parseDateInput(txt_effectiveFrom.getText());
        if (effectiveFrom == null) {
            showFormError("تاريخ السريان مطلوب أو غير صحيح");
            return;
        }
        LocalDate effectiveTo = TextFieldSetupHelper.parseDateInput(txt_effectiveTo.getText());
        if (effectiveTo != null && !effectiveTo.isAfter(effectiveFrom)) {
            showFormError("تاريخ النهاية لازم يكون بعد تاريخ البداية");
            return;
        }

        // تحقق أساسي
        if (nameAr.isBlank()) {
            showFormError("اسم البدل بالعربي مطلوب");
            return;
        }
        if (behavior == null || calcType == null) {
            showFormError("سلوك الاحتساب ونوعه مطلوبين");
            return;
        }
        if ((calcType == CalcType.PERCENT_BY_DEGREE
                || calcType == CalcType.PERCENT_ALL_DEGREE
                || calcType == CalcType.PERCENT_BY_JOB
                || calcType == CalcType.PERCENT_ALL_JOB) && baseSource == null) {
            showFormError(calcTypeLabel(calcType) + " يحتاج تحديد مصدر الأساسي");
            return;
        }
        if (isInsuranceType(calcType) && insuranceBase == null) {
            showFormError(calcTypeLabel(calcType) + " يحتاج تحديد وعاء التأمين");
            return;
        }

        if (elementType == null) {
            showFormError("طبيعة العنصر مطلوبة");
            return;
        }
        if (timelineAnchor == null) {
            showFormError("مصدر الدرجة مطلوب");
            return;
        }
        if (isJobCalcType(calcType)) {
            boolean hasSectors = eligibleSectorCodes != null && !eligibleSectorCodes.isEmpty();
            boolean hasJobs = eligibleJobTitles != null && !eligibleJobTitles.isEmpty();
            if (!hasSectors && !hasJobs) {
                showFormError(calcTypeLabel(calcType)
                        + " يحتاج تحديد قطاع واحد على الأقل أو وظيفة واحدة على الأقل");
                return;
            }
        }
        if (!validateExcludedMonths()) return;

        // referenceDate
        LocalDate referenceDate = null;
        boolean needsRefDate = calcType == CalcType.SPECIAL_ALLOWANCE_ADDED
                || calcType == CalcType.SPECIAL_ALLOWANCE_NOT_ADDED
                || calcType == CalcType.SOCIAL_PACKAGE_MINIMUM;
        if (needsRefDate) {
            referenceDate = TextFieldSetupHelper.parseDateInput(txt_referenceDate.getText());
            if (referenceDate == null) {
                showFormError("التاريخ المرجعي مطلوب للنوع المختار");
                return;
            }
        }

        // valuesMap
        boolean needsValues = calcTypeNeedsValues(calcType);
        Map<String, BigDecimal> valuesMap = new LinkedHashMap<>();
        valueRows.forEach(r -> valuesMap.put(r.key(), r.value()));
        if (needsValues && valuesMap.isEmpty()) {
            showFormError("أضف على الأقل قيمة واحدة في valuesMap");
            return;
        }
        if (!needsValues && valuesMap.isEmpty()) {
            valuesMap.put("all", BigDecimal.ZERO);
        }

        AllowanceDefinitionRequest request = new AllowanceDefinitionRequest(
                code, nameAr,
                txt_nameEn.getText().isBlank() ? null : txt_nameEn.getText().trim(),
                effectiveFrom, effectiveTo, referenceDate,
                behavior, calcType, baseSource, insuranceBase,
                scope, elementType,
                chk_subjectToInsurance.isSelected(),
                chk_subjectToTaxAndStamp.isSelected(),
                chk_inMinimumWageBase.isSelected(),
                chk_displayOnly.isSelected(),
                chk_appliesToNewHires.isSelected(),
                valuesMap,
                eligibleSectorCodes,
                eligibleJobTitles,
                splitOrNull(txt_excludedMonths.getText()),
                eligibleLaws,
                timelineAnchor,
                txt_notes.getText().isBlank() ? "" : txt_notes.getText().trim()
        );

        btn_save_definition.setDisable(true);

        if (editingId != null) {
            FxApiSupport.put("/entitlements/allowances/definitions/" + editingId,
                    request, AllowanceDefinition.class,
                    saved -> onSaveSuccess(), this::onSaveError);
        } else {
            FxApiSupport.post("/entitlements/allowances/definitions",
                    request, AllowanceDefinition.class,
                    saved -> onSaveSuccess(), this::onSaveError);
        }
    }

    private void onSaveSuccess() {
        btn_save_definition.setDisable(false);
        hideForm();
        loadDefinitions();
        AllowanceDefinition selected = table_definitions.getSelectionModel().getSelectedItem();
        if (selected != null) loadHistory(selected.getCode());
        if (onChanged != null) onChanged.run();
    }

    private void onSaveError(String err) {
        btn_save_definition.setDisable(false);
        showFormError("خطأ في الحفظ: " + err);
    }

    // ══════════════════════════════════════════════════════════════
    //  Delete
    // ══════════════════════════════════════════════════════════════

    private void handleDelete(AllowanceDefinition snap) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "حذف السجل ده هيأثر على احتساب البدل في التواريخ اللي بيغطيها.\nمتأكد؟",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("تأكيد الحذف");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                FxApiSupport.delete("/entitlements/allowances/definitions/" + snap.getId(),
                        () -> {
                            loadDefinitions();
                            loadHistory(snap.getCode());
                            if (onChanged != null) onChanged.run();
                        },
                        err -> showFormError("خطأ في الحذف: " + err));
            }
        });
    }

    // ══════════════════════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════════════════════

    /**
     * يقبل صيغتين:
     * - "08"       → الشهر في كل السنين
     * - "2023-08"  → شهر وسنة محددين
     */
    private boolean validateExcludedMonths() {
        String text = txt_excludedMonths.getText();
        if (text == null || text.isBlank()) return true;
        for (String raw : text.split(",")) {
            String s = raw.trim();
            if (s.isBlank()) continue;
            boolean ok = s.matches("\\d{1,2}") && Integer.parseInt(s) >= 1
                    && Integer.parseInt(s) <= 12
                    || s.matches("\\d{4}-\\d{2}");
            if (!ok) {
                showFormError("صيغة الشهر المستثنى غير صحيحة: \"" + s
                        + "\" — استخدم MM (مثل 08) أو yyyy-MM (مثل 2023-08)");
                return false;
            }
        }
        return true;
    }

    private List<String> splitOrNull(String text) {
        if (text == null || text.isBlank()) return null;
        return Arrays.stream(text.split(","))
                .map(String::trim).filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }

    private String joinOrEmpty(List<String> list) {
        return list == null ? "" : String.join(", ", list);
    }

    private void showFormError(String msg) {
        lbl_form_error.setText(msg);
        lbl_form_error.setVisible(true);
    }

    private void closeDialog() {
        ((Stage) btn_close.getScene().getWindow()).close();
    }
}