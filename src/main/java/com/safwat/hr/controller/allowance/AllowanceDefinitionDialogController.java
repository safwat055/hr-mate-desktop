package com.safwat.hr.controller.allowance;

import com.fasterxml.jackson.core.type.TypeReference;
import com.safwat.hr.controller.allowance.AllowanceDefinition.*;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * Controller لشاشة إدارة قواعد البدلات (allowance_definition).
 *
 * <p>بتسمح بـ:
 * <ul>
 *   <li>عرض أحدث snapshot لكل كود بدل (الجدول العلوي)</li>
 *   <li>عرض التاريخ الكامل لكود مُختار (الجدول السفلي)</li>
 *   <li>إضافة كود جديد بالكامل، أو snapshot جديد (تاريخ سريان مختلف)
 *       لكود موجود — تعديل طريقة الاحتساب مع الوقت</li>
 *   <li>تعديل أو حذف snapshot موجود</li>
 *   <li>تصنيف العنصر (استحقاق/استقطاع/تأمينات/ضريبة/دمغة) وخضوعه
 *       لوعاء التأمينات و/أو الضريبة والدمغة</li>
 * </ul>
 *
 * <p><b>عرض عربي:</b> كل الـ enums بتتعرض بأسماء عربي مفهومة للمستخدم،
 * والقيم الفعلية (الإنجليزية) هي اللي بتتبعت للباك.
 *
 * <p><b>التحكم في الإدخال:</b>
 * <ul>
 *   <li>{@code valuesMap} key → ComboBox (الدرجات + all + percent)</li>
 *   <li>التاريخ → TextField بصيغة {@code yyyy-MM-dd} + validation</li>
 *   <li>الشهور المستثناة → TextField بصيغة {@code yyyy-MM} + validation</li>
 * </ul>
 */
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
    private TableColumn<AllowanceDefinition, AllowanceDefinition.Behavior> col_def_behavior;
    @FXML
    private TableColumn<AllowanceDefinition, AllowanceDefinition.CalcType> col_def_calcType;
    @FXML
    private TableColumn<AllowanceDefinition, ElementType> col_def_elementType;
    @FXML
    private TableColumn<AllowanceDefinition, AllowanceDefinition.Scope> col_def_scope;

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
    private TableColumn<AllowanceDefinition, AllowanceDefinition.Behavior> col_hist_behavior;
    @FXML
    private TableColumn<AllowanceDefinition, AllowanceDefinition.CalcType> col_hist_calcType;
    @FXML
    private TableColumn<AllowanceDefinition, AllowanceDefinition.BaseSource> col_hist_baseSource;
    @FXML
    private TableColumn<AllowanceDefinition, ElementType> col_hist_elementType;
    @FXML
    private TableColumn<AllowanceDefinition, Void> col_hist_actions;

    // ── فورم الإضافة/التعديل ─────────────────────────────────────
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
    private TextField txt_effectiveFrom;      // ← نصي بدل DatePicker
    @FXML
    private ComboBox<Behavior> combo_behavior;
    @FXML
    private ComboBox<CalcType> combo_calcType;
    @FXML
    private ComboBox<BaseSource> combo_baseSource;
    @FXML
    private ComboBox<Scope> combo_scope;
    @FXML
    private ComboBox<ElementType> combo_elementType;
    @FXML
    private CheckBox chk_subjectToInsurance;
    @FXML
    private CheckBox chk_subjectToTaxAndStamp;
    @FXML
    private TextField txt_excludedMonths;
    @FXML
    private TextField txt_eligibleLaws;
    @FXML
    private TextField txt_eligibleLawCodes;
    @FXML
    private TextArea txt_notes;

    // valuesMap — مفتاح (ComboBox) / قيمة (TextField)
    @FXML
    private TableView<MapEntryRow> table_values;
    @FXML
    private TableColumn<MapEntryRow, String> col_map_key;
    @FXML
    private TableColumn<MapEntryRow, BigDecimal> col_map_value;
    @FXML
    private TableColumn<MapEntryRow, Void> col_map_actions;
    @FXML
    private ComboBox<MapKeyOption> combo_map_key;   // ← ComboBox بدل TextField
    @FXML
    private TextField txt_map_value;
    @FXML
    private Button btn_add_map_entry;

    @FXML
    private Label lbl_form_error;
    @FXML
    private Button btn_save_definition;
    @FXML
    private Button btn_cancel_form;
    @FXML
    private Button btn_close;
    @FXML
    private ComboBox<AllowanceDefinition.TimelineAnchor> combo_timelineAnchor;
    // ── State ────────────────────────────────────────────────────
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private static final Pattern MONTH_PATTERN = Pattern.compile("^\\d{4}-\\d{2}$");

    private final ObservableList<MapEntryRow> valueRows = FXCollections.observableArrayList();
    private Long editingId = null; // null = إنشاء جديد
    private Runnable onChanged;

    /**
     * سطر مفتاح/قيمة واحد في valuesMap.
     * المفتاح String (بيتبعت للباك كـ String).
     */
    private record MapEntryRow(String key, BigDecimal value) {
    }

    /**
     * خيار في ComboBox الـ valuesMap key.
     * {@code value} هي القيمة المُرسلة للباك ("6", "all", "percent")
     * {@code label} هو العرض العربي
     */
    private record MapKeyOption(String value, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    private static String timelineAnchorLabel(AllowanceDefinition.TimelineAnchor a) {
        return switch (a) {
            case TARGET_DATE -> "شهرى";
            case EFFECTIVE_FROM -> "شهر القرار";
        };
    }

    /**
     * قائمة الخيارات المتاحة للـ valuesMap key.
     * الدرجات من 6 لـ -2 بالأسماء العربية + all + percent.
     */
    private static final List<MapKeyOption> MAP_KEY_OPTIONS = List.of(
            new MapKeyOption("6", "السادسة"),
            new MapKeyOption("5", "الخامسة"),
            new MapKeyOption("4", "الرابعة"),
            new MapKeyOption("3", "الثالثة"),
            new MapKeyOption("2", "الثانية"),
            new MapKeyOption("1", "الأولى"),
            new MapKeyOption("0", "مدير عام"),
            new MapKeyOption("-1", "العالية"),
            new MapKeyOption("-2", "الممتازة"),
            new MapKeyOption("all", "كل الدرجات"),
            new MapKeyOption("percent", "نسبة من المحرك")
    );

    // ════════════════════════════════════════════════════════════
    //  قاموس التسميات العربية — عرض بس، القيمة المُرسلة تفضل enum name
    // ════════════════════════════════════════════════════════════

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

    @SuppressWarnings("unchecked")
    private static <E> StringConverter<E> arabicConverter(Function<Object, String> labelFn) {
        return new StringConverter<>() {
            @Override
            public String toString(E val) {
                return val == null ? "" : labelFn.apply(val);
            }

            @Override
            public E fromString(String s) {
                return null; // الـ ComboBoxes هنا مش editable
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

        combo_calcType.valueProperty().addListener((obs, old, val) ->
                combo_baseSource.setDisable(
                        val != CalcType.PERCENT_BY_DEGREE && val != CalcType.PERCENT_ALL_DEGREE));

        // validation فوري لحقول التاريخ عند فقدان التركيز
        txt_effectiveFrom.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) validateEffectiveFromField();
        });
        txt_excludedMonths.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) validateExcludedMonthsField();
        });

        hideForm();
        loadDefinitions();
    }

    public void init(Runnable onChanged) {
        this.onChanged = onChanged;
    }

    // ════════════════════════════════════════════════════════════
    //  تحميل البيانات
    // ════════════════════════════════════════════════════════════

    private void loadDefinitions() {
        FxApiSupport.getList(
                "/allowances/definitions",
                new TypeReference<List<AllowanceDefinition>>() {
                },
                defs -> table_definitions.setItems(FXCollections.observableArrayList(defs)),
                err -> showFormError("خطأ في تحميل القواعد: " + err)
        );
    }

    private void loadHistory(String code) {
        FxApiSupport.getList(
                "/allowances/definitions/" + code + "/history",
                new TypeReference<List<AllowanceDefinition>>() {
                },
                history -> table_history.setItems(FXCollections.observableArrayList(history)),
                err -> showFormError("خطأ في تحميل التاريخ: " + err)
        );
    }

    // ════════════════════════════════════════════════════════════
    //  إعداد الجداول
    // ════════════════════════════════════════════════════════════

    private void setupDefinitionsTable() {
        col_def_code.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(d.getValue().getCode()));
        col_def_nameAr.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(d.getValue().getNameAr()));
        col_def_effectiveFrom.setCellValueFactory(d ->
                new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getEffectiveFrom()));
        col_def_effectiveFrom.setCellFactory(tc -> dateCell());

        col_def_behavior.setCellValueFactory(d ->
                new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getBehavior()));
        col_def_behavior.setCellFactory(tc -> behaviorCell());

        col_def_calcType.setCellValueFactory(d ->
                new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getCalcType()));
        col_def_calcType.setCellFactory(tc -> calcTypeCell());

        col_def_elementType.setCellValueFactory(d ->
                new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getElementType()));
        col_def_elementType.setCellFactory(tc -> elementTypeCell());

        col_def_scope.setCellValueFactory(d ->
                new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getScope()));
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
        col_hist_effectiveFrom.setCellValueFactory(d ->
                new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getEffectiveFrom()));
        col_hist_effectiveFrom.setCellFactory(tc -> dateCell());

        col_hist_behavior.setCellValueFactory(d ->
                new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getBehavior()));
        col_hist_behavior.setCellFactory(tc -> behaviorCell());

        col_hist_calcType.setCellValueFactory(d ->
                new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getCalcType()));
        col_hist_calcType.setCellFactory(tc -> calcTypeCell());

        col_hist_baseSource.setCellValueFactory(d ->
                new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getBaseSource()));
        col_hist_baseSource.setCellFactory(tc -> baseSourceCell());

        col_hist_elementType.setCellValueFactory(d ->
                new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getElementType()));
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

                btnEdit.setOnAction(e -> {
                    AllowanceDefinition snap = getTableView().getItems().get(getIndex());
                    showFormForEdit(snap);
                });
                btnDelete.setOnAction(e -> {
                    AllowanceDefinition snap = getTableView().getItems().get(getIndex());
                    handleDelete(snap);
                });
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
        col_map_key.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(
                        mapKeyDisplay(d.getValue().key())
                ));
        col_map_value.setCellValueFactory(d ->
                new javafx.beans.property.SimpleObjectProperty<>(d.getValue().value()));
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

        combo_scope.setItems(FXCollections.observableArrayList(Scope.values()));
        combo_scope.setConverter(arabicConverter(v -> scopeLabel((Scope) v)));

        combo_elementType.setItems(FXCollections.observableArrayList(ElementType.values()));
        combo_elementType.setConverter(arabicConverter(v -> elementTypeLabel((ElementType) v)));

        // ComboBox الـ valuesMap key
        combo_map_key.setItems(FXCollections.observableArrayList(MAP_KEY_OPTIONS));
        combo_map_key.setVisibleRowCount(12);

        combo_timelineAnchor.setItems(FXCollections.observableArrayList(
                AllowanceDefinition.TimelineAnchor.values()));
        combo_timelineAnchor.setConverter(arabicConverter(v -> timelineAnchorLabel((AllowanceDefinition.TimelineAnchor) v)));
    }

    /**
     * عرض الـ key في الجدول — بيدور على الـ label العربي، ولو مش موجود
     * (قيمة قديمة مثلاً) يعرض القيمة الخام.
     */
    private static String mapKeyDisplay(String rawKey) {
        if (rawKey == null) return "";
        return MAP_KEY_OPTIONS.stream()
                .filter(o -> o.value().equals(rawKey))
                .findFirst()
                .map(MapKeyOption::label)
                .orElse(rawKey);
    }

    private TableCell<AllowanceDefinition, LocalDate> dateCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setText(empty || date == null ? null : date.format(DATE_FMT));
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
    //  الفورم — إظهار/إخفاء
    // ════════════════════════════════════════════════════════════

    private void showFormForNewCode() {
        clearForm();
        editingId = null;
        txt_code.setDisable(true);   // الباك بيولّد UUID
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
        combo_elementType.setValue(
                selected.getElementType() != null ? selected.getElementType() : ElementType.ENTITLEMENT
        );
        chk_subjectToInsurance.setSelected(selected.isSubjectToInsurance());
        chk_subjectToTaxAndStamp.setSelected(selected.isSubjectToTaxAndStamp());
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
        combo_behavior.setValue(snap.getBehavior());
        combo_calcType.setValue(snap.getCalcType());
        combo_baseSource.setValue(snap.getBaseSource());
        combo_scope.setValue(snap.getScope() != null ? snap.getScope() : Scope.GENERAL);
        combo_elementType.setValue(
                snap.getElementType() != null ? snap.getElementType() : ElementType.ENTITLEMENT);
        combo_timelineAnchor.setValue(
                snap.getTimelineAnchor() != null
                        ? snap.getTimelineAnchor()
                        : AllowanceDefinition.TimelineAnchor.TARGET_DATE
        );
        chk_subjectToInsurance.setSelected(snap.isSubjectToInsurance());
        chk_subjectToTaxAndStamp.setSelected(snap.isSubjectToTaxAndStamp());
        txt_excludedMonths.setText(joinOrEmpty(snap.getExcludedMonths()));
        txt_eligibleLaws.setText(joinOrEmpty(snap.getEligibleLaws()));
        txt_eligibleLawCodes.setText(joinOrEmpty(snap.getEligibleLawCodes()));
        txt_notes.setText(snap.getNotes());

        valueRows.clear();
        if (snap.getValuesMap() != null) {
            snap.getValuesMap().forEach((k, v) -> valueRows.add(new MapEntryRow(k, v)));
        }

        lbl_form_title.setText("تعديل: " + snap.getCode() + " @ " + snap.getEffectiveFrom());
        showForm();

    }

    private void clearForm() {
        txt_code.clear();
        txt_code.setDisable(true);
        txt_nameAr.clear();
        txt_nameEn.clear();
        txt_effectiveFrom.clear();
        combo_behavior.setValue(null);
        combo_calcType.setValue(null);
        combo_baseSource.setValue(null);
        combo_scope.setValue(Scope.GENERAL);
        combo_elementType.setValue(ElementType.ENTITLEMENT);
        chk_subjectToInsurance.setSelected(false);
        chk_subjectToTaxAndStamp.setSelected(false);
        txt_excludedMonths.clear();
        txt_eligibleLaws.clear();
        txt_eligibleLawCodes.clear();
        txt_notes.clear();
        valueRows.clear();
        combo_map_key.setValue(null);
        txt_map_value.clear();
        combo_timelineAnchor.setValue(AllowanceDefinition.TimelineAnchor.TARGET_DATE);
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
    //  valuesMap — إضافة صف
    // ════════════════════════════════════════════════════════════

    private void handleAddMapEntry() {
        MapKeyOption keyOpt = combo_map_key.getValue();
        String valStr = txt_map_value.getText().trim();

        if (keyOpt == null) {
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

        // لو المفتاح موجود بالفعل، استبدله بدل ما نكرره
        valueRows.removeIf(r -> r.key().equals(keyOpt.value()));
        valueRows.add(new MapEntryRow(keyOpt.value(), value));

        combo_map_key.setValue(null);
        txt_map_value.clear();
        lbl_form_error.setVisible(false);
    }

    // ════════════════════════════════════════════════════════════
    //  Validation
    // ════════════════════════════════════════════════════════════

    /**
     * يتحقق من صيغة {@code yyyy-MM-dd} في {@code txt_effectiveFrom}.
     *
     * @return التاريخ المُحلل، أو {@code null} لو فيه خطأ (وبيعرض رسالة)
     */
    private LocalDate validateEffectiveFromField() {
        String text = txt_effectiveFrom.getText();
        if (text == null || text.isBlank()) return null;
        text = text.trim();

        if (!DATE_PATTERN.matcher(text).matches()) {
            showFormError("تاريخ السريان لازم يكون بصيغة yyyy-MM-dd");
            return null;
        }
        try {
            return LocalDate.parse(text, DATE_FMT);
        } catch (DateTimeParseException ex) {
            showFormError("تاريخ السريان غير صحيح: " + text);
            return null;
        }
    }

    /**
     * يتحقق من صيغة الشهور المستثناة: {@code yyyy-MM} مفصولة بفواصل.
     *
     * @return {@code true} لو صحيحة أو فاضية
     */
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
    //  حفظ
    // ════════════════════════════════════════════════════════════

    private void handleSave() {
        lbl_form_error.setVisible(false);

        String code = txt_code.getText().trim();
        String nameAr = txt_nameAr.getText().trim();
        Behavior behavior = combo_behavior.getValue();
        CalcType calcType = combo_calcType.getValue();
        BaseSource baseSource = combo_baseSource.getValue();
        Scope scope = combo_scope.getValue();
        ElementType elementType = combo_elementType.getValue();

        // validation التاريخ
        LocalDate effectiveFrom = validateEffectiveFromField();
        if (effectiveFrom == null) {
            if (txt_effectiveFrom.getText() == null || txt_effectiveFrom.getText().isBlank())
                showFormError("تاريخ السريان مطلوب");
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
                || calcType == CalcType.PERCENT_ALL_DEGREE) && baseSource == null) {
            showFormError(calcTypeLabel(calcType) + " يحتاج تحديد مصدر الأساسي");
            return;
        }
        if (elementType == null) {
            showFormError("طبيعة العنصر (استحقاق/استقطاع/...) مطلوبة");
            return;
        }
        if (valueRows.isEmpty()) {
            showFormError("أضف على الأقل قيمة واحدة في valuesMap");
            return;
        }
        if (!validateExcludedMonthsField()) return;

        Map<String, BigDecimal> valuesMap = new LinkedHashMap<>();
        valueRows.forEach(r -> valuesMap.put(r.key(), r.value()));

        AllowanceDefinition.TimelineAnchor timelineAnchor = combo_timelineAnchor.getValue();

        AllowanceDefinitionRequest request = new AllowanceDefinitionRequest(
                code, nameAr,
                txt_nameEn.getText().isBlank() ? null : txt_nameEn.getText().trim(),
                effectiveFrom, behavior, calcType, baseSource, scope,
                elementType,
                chk_subjectToInsurance.isSelected(),
                chk_subjectToTaxAndStamp.isSelected(),
                valuesMap,
                splitOrNull(txt_excludedMonths.getText()),
                splitOrNull(txt_eligibleLaws.getText()),
                splitOrNull(txt_eligibleLawCodes.getText()),

                timelineAnchor,                                   // ← جديد
                txt_notes.getText().isBlank() ? "" : txt_notes.getText().trim()
        );


        if (timelineAnchor == null) {
            throw new IllegalStateException(
                    "timelineAnchor = null — الـ ComboBox فاضي أو مفيش اختيار. " +
                            "شوف الـ diagnostics فوق."
            );
        }
        btn_save_definition.setDisable(true);

        if (editingId != null) {
            FxApiSupport.put(
                    "/allowances/definitions/" + editingId,
                    request,
                    AllowanceDefinition.class,
                    saved -> onSaveSuccess(),
                    this::onSaveError
            );
        } else {
            FxApiSupport.post(
                    "/allowances/definitions",
                    request,
                    AllowanceDefinition.class,
                    saved -> onSaveSuccess(),

                    this::onSaveError
            );
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
    //  حذف
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
                        "/allowances/definitions/" + snap.getId(),
                        () -> {
                            loadDefinitions();
                            loadHistory(snap.getCode());
                            if (onChanged != null) onChanged.run();
                        },
                        err -> showFormError("خطأ في الحذف: " + err)
                );
            }
        });
    }

    // ════════════════════════════════════════════════════════════
    //  Helpers
    // ════════════════════════════════════════════════════════════

    private List<String> splitOrNull(String text) {
        if (text == null || text.isBlank()) return null;
        return Arrays.stream(text.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
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