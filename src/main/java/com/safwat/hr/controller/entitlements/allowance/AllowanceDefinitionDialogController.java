package com.safwat.hr.controller.entitlements.allowance;

import com.fasterxml.jackson.core.type.TypeReference;
import com.safwat.hr.controller.entitlements.allowance.AllowanceDefinition.*;
import com.safwat.hr.controller.entitlements.allowance.dto.JobTitleDto;
import com.safwat.hr.controller.entitlements.allowance.dto.SectorDto;
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
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AllowanceDefinitionDialogController implements Initializable {

    // ── جدول أحدث القواعد ────────────────────────────────────────
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

    // ── جدول تاريخ الكود المختار ─────────────────────────────────
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

    // ── فورم ─────────────────────────────────────────────────────
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
    private ComboBox<Behavior> combo_behavior;
    @FXML
    private ComboBox<CalcType> combo_calcType;
    @FXML
    private ComboBox<BaseSource> combo_baseSource;
    @FXML
    private ComboBox<InsuranceBase> combo_insuranceBase;
    @FXML
    private HBox pane_insuranceBase;
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
    @FXML
    private HBox pane_referenceDate;
    @FXML
    private TextField txt_referenceDate;
    @FXML
    private TextField txt_excludedMonths;
    @FXML
    private TextField txt_eligibleLaws;
    @FXML
    private TextField txt_eligibleLawCodes;
    @FXML
    private TextArea txt_notes;

    // ── 🆕 القطاع ──
    @FXML
    private ComboBox<SectorDto> combo_sector;
    @FXML
    private HBox pane_sector;

    // ── valuesMap ────────────────────────────────────────────────
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
    @FXML
    private VBox pane_valuesMap;

    @FXML
    private Label lbl_form_error;
    @FXML
    private Button btn_save_definition;
    @FXML
    private Button btn_cancel_form;
    @FXML
    private Button btn_close;

    // ── State ──
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private static final Pattern MONTH_PATTERN = Pattern.compile("^\\d{4}-\\d{2}$");

    private final ObservableList<MapEntryRow> valueRows = FXCollections.observableArrayList();
    private Long editingId = null;
    private Runnable onChanged;

    private static final Comparator<AllowanceDefinition> ARABIC_NAME_COMPARATOR =
            Comparator.comparing(
                    d -> d.getNameAr() != null ? d.getNameAr() : "",
                    Collator.getInstance(new Locale("ar"))
            );

    private record MapEntryRow(String key, BigDecimal value) {
    }

    private record MapKeyOption(String value, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  مجموعات مفاتيح valuesMap
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

    private static final List<MapKeyOption> ALL_KEY = List.of(
            new MapKeyOption("all", "كل الدرجات")
    );

    private static final List<MapKeyOption> PERCENT_KEY = List.of(
            new MapKeyOption("percent", "نسبة من المحرك")
    );

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

    private static final List<MapKeyOption> INSURANCE_KEYS = List.of(
            new MapKeyOption("rate", "النسبة")
    );

    private static final Map<String, String> KEY_LABELS = buildKeyLabels();

    private static Map<String, String> buildKeyLabels() {
        Map<String, String> m = new LinkedHashMap<>();
        DEGREE_KEYS.forEach(k -> m.put(k.value(), k.label()));
        ALL_KEY.forEach(k -> m.put(k.value(), k.label()));
        PERCENT_KEY.forEach(k -> m.put(k.value(), k.label()));
        MARITAL_KEYS.forEach(k -> m.put(k.value(), k.label()));
        INSURANCE_KEYS.forEach(k -> m.put(k.value(), k.label()));
        return Map.copyOf(m);
    }

    private static List<MapKeyOption> mapKeyOptionsFor(CalcType calcType) {
        if (calcType == null) return List.of();
        return switch (calcType) {
            case PERCENT_BY_DEGREE, AMOUNT_BY_DEGREE, SUPPLEMENTARY_BONUS -> DEGREE_KEYS;
            case FIXED_AMOUNT, PERCENT_ALL_DEGREE -> ALL_KEY;
            case SALARY_ENGINE, DEPENDS_SALARY_ENGINE -> PERCENT_KEY;
            case FIXED_AMOUNT_BY_MARITAL_STATUS -> MARITAL_KEYS;
            case INSURANCE_RATE_EMPLOYEE, INSURANCE_RATE_EMPLOYER -> INSURANCE_KEYS;
            case PERCENT_ALL_JOB -> ALL_KEY;                 // 🆕
            case PERCENT_BY_JOB, AMOUNT_BY_JOB -> List.of(); // 🆕 ديناميكية
            case COMPENSATORY_BONUS,
                 SPECIAL_ALLOWANCE_ADDED, SPECIAL_ALLOWANCE_NOT_ADDED,
                 SOCIAL_PACKAGE_MINIMUM,
                 PROMOTION_INCENTIVE -> List.of();
        };
    }

    private static boolean calcTypeNeedsValues(CalcType calcType) {
        if (calcType == null) return false;
        // 🆕 الأنواع الجديدة محتاجة قيم دايماً
        if (isJobType(calcType)) return true;
        return !mapKeyOptionsFor(calcType).isEmpty();
    }

    private static boolean isInsuranceType(CalcType calcType) {
        return calcType == CalcType.INSURANCE_RATE_EMPLOYEE
                || calcType == CalcType.INSURANCE_RATE_EMPLOYER;
    }

    private static boolean isEmployeeInsurance(CalcType calcType) {
        return calcType == CalcType.INSURANCE_RATE_EMPLOYEE;
    }

    private static boolean isEmployerInsurance(CalcType calcType) {
        return calcType == CalcType.INSURANCE_RATE_EMPLOYER;
    }

    /**
     * 🆕 هل النوع ده بيعتمد على الوظيفة؟
     */
    private static boolean isJobType(CalcType calcType) {
        return calcType == CalcType.PERCENT_BY_JOB
                || calcType == CalcType.PERCENT_ALL_JOB
                || calcType == CalcType.AMOUNT_BY_JOB;
    }

    /**
     * 🆕 هل محتاج يفتح القطاع (يحتاج وظائف)؟
     */
    private static boolean needsSectorPicker(CalcType calcType) {
        return isJobType(calcType);
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
            case PERCENT_BY_JOB -> "نسبة % حسب الوظيفة";        // 🆕
            case PERCENT_ALL_JOB -> "نسبة لكل الوظائف";         // 🆕
            case AMOUNT_BY_JOB -> "مبلغ ثابت حسب الوظيفة";      // 🆕
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

    // ════════════════════════════════════════════════════════════
    //  Initialize
    // ════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupDefinitionsTable();
        setupHistoryTable();
        setupValuesTable();
        setupCombos();

        btn_new_code.setOnAction(e -> showFormForNewCode());
        btn_new_snapshot.setOnAction(e -> showFormForNewSnapshot());
        btn_save_definition.setOnAction(e -> handleSave());
        btn_cancel_form.setOnAction(e -> hideForm());
        btn_add_map_entry.setOnAction(e -> handleAddMapEntry());
        btn_close.setOnAction(e -> closeDialog());

        // listener على calcType — يظبط الواجهة
        combo_calcType.valueProperty().addListener((obs, old, val) -> {
            autoAdjustForCalcType(val);
        });

        // 🆕 listener على القطاع — لما يتغير، نحمّل الوظائف
        if (combo_sector != null) {
            combo_sector.valueProperty().addListener((obs, old, val) -> {
                if (val != null) {
                    loadJobTitles(val.getCode());
                } else {
                    // نرجّع combo_map_key للقائمة الفاضية
                    if (isJobType(combo_calcType.getValue())) {
                        combo_map_key.setItems(FXCollections.observableArrayList());
                    }
                }
            });
        }

        txt_effectiveFrom.focusedProperty().addListener((obs, was, isNow) -> {
            if (!isNow) validateDateField(txt_effectiveFrom, true);
        });
        txt_effectiveTo.focusedProperty().addListener((obs, was, isNow) -> {
            if (!isNow) validateDateField(txt_effectiveTo, false);
        });
        txt_referenceDate.focusedProperty().addListener((obs, was, isNow) -> {
            if (!isNow) validateDateField(txt_referenceDate, false);
        });
        txt_excludedMonths.focusedProperty().addListener((obs, was, isNow) -> {
            if (!isNow) validateExcludedMonthsField();
        });

        hideForm();
        loadDefinitions();
    }

    /**
     * يظبط الواجهة حسب النوع المختار.
     */
    private void autoAdjustForCalcType(CalcType val) {
        if (val == null) return;

        // ── 1) baseSource ──
        boolean needsBaseSource = val == CalcType.PERCENT_BY_DEGREE
                || val == CalcType.PERCENT_ALL_DEGREE
                || val == CalcType.PERCENT_BY_JOB           // 🆕
                || val == CalcType.PERCENT_ALL_JOB;         // 🆕
        combo_baseSource.setDisable(!needsBaseSource);
        if (!needsBaseSource) combo_baseSource.setValue(null);

        // ── 2) referenceDate ──
        boolean needsRefDate = val == CalcType.SPECIAL_ALLOWANCE_ADDED
                || val == CalcType.SPECIAL_ALLOWANCE_NOT_ADDED
                || val == CalcType.SOCIAL_PACKAGE_MINIMUM;
        pane_referenceDate.setVisible(needsRefDate);
        pane_referenceDate.setManaged(needsRefDate);
        if (!needsRefDate) txt_referenceDate.clear();

        // ── 3) insuranceBase ──
        boolean needsInsBase = isInsuranceType(val);
        if (pane_insuranceBase != null) {
            pane_insuranceBase.setVisible(needsInsBase);
            pane_insuranceBase.setManaged(needsInsBase);
        }
        if (needsInsBase && combo_insuranceBase != null
                && combo_insuranceBase.getValue() == null) {
            combo_insuranceBase.setValue(InsuranceBase.COMBINED);
        }

        // ── 4) 🆕 القطاع (للأنواع الوظيفية) ──
        boolean needsSector = needsSectorPicker(val);
        if (pane_sector != null) {
            pane_sector.setVisible(needsSector);
            pane_sector.setManaged(needsSector);
        }
        if (needsSector && combo_sector != null && combo_sector.getItems().isEmpty()) {
            loadSectors();
        }
        if (!needsSector && combo_sector != null) {
            // نمسح القطاع لو النوع مش محتاجه
            // (لكن نسيبه لو بنعمل Edit لسجل موجود)
            if (editingId == null) {
                combo_sector.setValue(null);
            }
        }

        // ── 5) elementType + displayOnly (auto) ──
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

        // ── 6) checkboxes للتأمينات ──
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

        // ── 7) valuesMap keys ──
        updateMapKeyOptionsFor(val);
    }

    public void init(Runnable onChanged) {
        this.onChanged = onChanged;
    }

    // ════════════════════════════════════════════════════════════
    //  Load Definitions / History
    // ════════════════════════════════════════════════════════════

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

    // ════════════════════════════════════════════════════════════
    //  🆕 Load Sectors / Job Titles
    // ════════════════════════════════════════════════════════════

    private void loadSectors() {
        if (combo_sector == null) return;
        FxApiSupport.getList(
                "/sectors",
                new TypeReference<List<SectorDto>>() {
                },
                list -> combo_sector.setItems(FXCollections.observableArrayList(list)),
                err -> showFormError("خطأ في تحميل القطاعات: " + err)
        );
    }

    private void loadJobTitles(String sectorCode) {
        if (sectorCode == null || sectorCode.isBlank()) return;
        FxApiSupport.getList(
                "/sectors/" + sectorCode + "/jobs",
                new TypeReference<List<JobTitleDto>>() {
                },
                list -> {
                    // نبني options للـ combo_map_key من أسماء الوظائف
                    List<MapKeyOption> options = list.stream()
                            .map(j -> new MapKeyOption(j.getNameAr(), j.getNameAr()))
                            .collect(Collectors.toList());

                    if (isJobType(combo_calcType.getValue())) {
                        combo_map_key.setItems(FXCollections.observableArrayList(options));
                    }
                },
                err -> showFormError("خطأ في تحميل الوظائف: " + err)
        );
    }

    // ════════════════════════════════════════════════════════════
    //  Setup Tables
    // ════════════════════════════════════════════════════════════

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
        // 🆕 للأنواع الوظيفية: نبني القائمة من الوظائف المحمّلة (لو موجودة)
        if (isJobType(calcType)) {
            // القائمة بتيجي من loadJobTitles
            // لو القطاع مش مختار → قائمة فاضية
            if (combo_sector == null || combo_sector.getValue() == null) {
                combo_map_key.setItems(FXCollections.observableArrayList());
            }
            // نبقيها زي ما هي لو الوظايف اتحمّلت قبل كده

            boolean needsValues = true;
            if (pane_valuesMap != null) {
                pane_valuesMap.setVisible(needsValues);
                pane_valuesMap.setManaged(needsValues);
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

    // ════════════════════════════════════════════════════════════
    //  Form — show / hide
    // ════════════════════════════════════════════════════════════

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
        chk_subjectToInsurance.setSelected(selected.isSubjectToInsurance());
        chk_subjectToTaxAndStamp.setSelected(selected.isSubjectToTaxAndStamp());
        chk_inMinimumWageBase.setSelected(selected.isInMinimumWageBase());
        chk_displayOnly.setSelected(selected.isDisplayOnly());
        chk_appliesToNewHires.setSelected(selected.isAppliesToNewHires());

        // 🆕 نعرض القطاع لو موجود
        if (selected.getEligibleSectorCode() != null && combo_sector != null) {
            ensureSectorLoaded(selected.getEligibleSectorCode());
        }

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
        txt_eligibleLaws.setText(joinOrEmpty(snap.getEligibleLaws()));
        txt_eligibleLawCodes.setText(joinOrEmpty(snap.getEligibleLawCodes()));
        txt_notes.setText(snap.getNotes());

        // 🆕 نعرض القطاع ونحمّل وظائفه
        if (snap.getEligibleSectorCode() != null && combo_sector != null) {
            ensureSectorLoaded(snap.getEligibleSectorCode());
        }

        updateMapKeyOptionsFor(snap.getCalcType());

        valueRows.clear();
        if (snap.getValuesMap() != null) {
            snap.getValuesMap().forEach((k, v) -> valueRows.add(new MapEntryRow(k, v)));
        }

        autoAdjustForCalcType(snap.getCalcType());

        lbl_form_title.setText("تعديل: " + snap.getCode() + " @ " + snap.getEffectiveFrom());
        showForm();
    }

    /**
     * 🆕 يحمّل القطاعات (لو لسه) ويختار القطاع المطلوب.
     * بعد كده، بيحمّل الوظائف بتاعته.
     */
    private void ensureSectorLoaded(String sectorCode) {
        if (combo_sector == null || sectorCode == null) return;

        // لو القطاعات لسه ما اتحمّلتش
        if (combo_sector.getItems().isEmpty()) {
            FxApiSupport.getList(
                    "/sectors",
                    new TypeReference<List<SectorDto>>() {
                    },
                    list -> {
                        combo_sector.setItems(FXCollections.observableArrayList(list));
                        selectSectorByCode(sectorCode);
                    },
                    err -> showFormError("خطأ في تحميل القطاعات: " + err)
            );
        } else {
            selectSectorByCode(sectorCode);
        }
    }

    private void selectSectorByCode(String code) {
        combo_sector.getItems().stream()
                .filter(s -> code.equals(s.getCode()))
                .findFirst()
                .ifPresent(combo_sector::setValue);
        // ده هيشغّل listener اللي هيحمّل الوظائف
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
        txt_eligibleLaws.clear();
        txt_eligibleLawCodes.clear();
        txt_notes.clear();
        valueRows.clear();
        combo_map_key.setItems(FXCollections.observableArrayList());
        combo_map_key.setValue(null);
        txt_map_value.clear();
        combo_timelineAnchor.setValue(TimelineAnchor.TARGET_DATE);
        pane_referenceDate.setVisible(false);
        pane_referenceDate.setManaged(false);
        if (pane_insuranceBase != null) {
            pane_insuranceBase.setVisible(false);
            pane_insuranceBase.setManaged(false);
        }
        if (pane_valuesMap != null) {
            pane_valuesMap.setVisible(false);
            pane_valuesMap.setManaged(false);
        }

        // 🆕 صفّر القطاع
        if (combo_sector != null) combo_sector.setValue(null);
        if (pane_sector != null) {
            pane_sector.setVisible(false);
            pane_sector.setManaged(false);
        }

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

    // ════════════════════════════════════════════════════════════
    //  valuesMap
    // ════════════════════════════════════════════════════════════

    private void handleAddMapEntry() {
        MapKeyOption keyOpt = combo_map_key.getValue();
        String valStr = txt_map_value.getText().trim();

        // 🆕 لو النوع وظيفي و combo_map_key فاضي — نسمح بكتابة الاسم يدوياً
        String key;
        if (keyOpt != null) {
            key = keyOpt.value();
        } else if (isJobType(combo_calcType.getValue())) {
            showFormError("اختر وظيفة من القائمة (اختر القطاع أولاً)");
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

    // ════════════════════════════════════════════════════════════
    //  Validation
    // ════════════════════════════════════════════════════════════

    private LocalDate validateDateField(TextField field, boolean required) {
        String text = field.getText();
        if (text == null || text.isBlank()) {
            if (required) {
                showFormError("التاريخ مطلوب");
                return null;
            }
            return null;
        }
        text = text.trim();
        if (!DATE_PATTERN.matcher(text).matches()) {
            showFormError("التاريخ لازم يكون بصيغة yyyy-MM-dd");
            return null;
        }
        try {
            return LocalDate.parse(text, DATE_FMT);
        } catch (DateTimeParseException ex) {
            showFormError("تاريخ غير صحيح: " + text);
            return null;
        }
    }

    private boolean validateExcludedMonthsField() {
        String text = txt_excludedMonths.getText();
        if (text == null || text.isBlank()) return true;
        for (String part : text.split(",")) {
            String s = part.trim();
            if (s.isBlank()) continue;
            if (!MONTH_PATTERN.matcher(s).matches()) {
                showFormError("الشهور المستثناة لازم تكون بصيغة yyyy-MM مفصولة بفواصل");
                return false;
            }
        }
        return true;
    }

    // ════════════════════════════════════════════════════════════
    //  Save
    // ════════════════════════════════════════════════════════════

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

        // 🆕 القطاع
        String eligibleSectorCode = combo_sector != null && combo_sector.getValue() != null
                ? combo_sector.getValue().getCode()
                : null;

        LocalDate effectiveFrom = validateDateField(txt_effectiveFrom, true);
        if (effectiveFrom == null) {
            if (txt_effectiveFrom.getText() == null || txt_effectiveFrom.getText().isBlank())
                showFormError("تاريخ السريان مطلوب");
            return;
        }
        LocalDate effectiveTo = validateDateField(txt_effectiveTo, false);
        if (effectiveTo != null && !effectiveTo.isAfter(effectiveFrom)) {
            showFormError("تاريخ النهاية لازم يكون بعد تاريخ البداية");
            return;
        }

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
        // 🆕 الأنواع الوظيفية محتاجة قطاع
        if (needsSectorPicker(calcType) && (eligibleSectorCode == null || eligibleSectorCode.isBlank())) {
            showFormError(calcTypeLabel(calcType) + " يحتاج تحديد القطاع");
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
        if (!validateExcludedMonthsField()) return;

        LocalDate referenceDate = null;
        boolean needsRefDate = calcType == CalcType.SPECIAL_ALLOWANCE_ADDED
                || calcType == CalcType.SPECIAL_ALLOWANCE_NOT_ADDED
                || calcType == CalcType.SOCIAL_PACKAGE_MINIMUM;
        if (needsRefDate) {
            referenceDate = validateDateField(txt_referenceDate, true);
            if (referenceDate == null) {
                showFormError("التاريخ المرجعي مطلوب للنوع المختار");
                return;
            }
        }

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
                effectiveFrom,
                effectiveTo,
                referenceDate,
                behavior, calcType, baseSource, insuranceBase,
                scope, elementType,
                chk_subjectToInsurance.isSelected(),
                chk_subjectToTaxAndStamp.isSelected(),
                chk_inMinimumWageBase.isSelected(),
                chk_displayOnly.isSelected(),
                chk_appliesToNewHires.isSelected(),
                valuesMap,
                eligibleSectorCode,                              // 🆕
                splitOrNull(txt_excludedMonths.getText()),
                splitOrNull(txt_eligibleLaws.getText()),
                splitOrNull(txt_eligibleLawCodes.getText()),
                timelineAnchor,
                txt_notes.getText().isBlank() ? "" : txt_notes.getText().trim()
        );

        btn_save_definition.setDisable(true);

        if (editingId != null) {
            FxApiSupport.put(
                    "/entitlements/allowances/definitions/" + editingId,
                    request, AllowanceDefinition.class,
                    saved -> onSaveSuccess(), this::onSaveError);
        } else {
            FxApiSupport.post(
                    "/entitlements/allowances/definitions",
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

    // ════════════════════════════════════════════════════════════
    //  Delete
    // ════════════════════════════════════════════════════════════

    private void handleDelete(AllowanceDefinition snap) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "حذف السجل ده هيأثر على احتساب البدل في التواريخ اللي بيغطيها.\nمتأكد؟",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("تأكيد الحذف");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                FxApiSupport.delete(
                        "/entitlements/allowances/definitions/" + snap.getId(),
                        () -> {
                            loadDefinitions();
                            loadHistory(snap.getCode());
                            if (onChanged != null) onChanged.run();
                        },
                        err -> showFormError("خطأ في الحذف: " + err));
            }
        });
    }

    // ════════════════════════════════════════════════════════════
    //  Helpers
    // ════════════════════════════════════════════════════════════

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