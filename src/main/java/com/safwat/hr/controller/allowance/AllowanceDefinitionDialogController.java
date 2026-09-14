package com.safwat.hr.controller.allowance;

import com.fasterxml.jackson.core.type.TypeReference;
import com.safwat.hr.hrScale.allowance.dto.AllowanceDefinitionRequest;
import com.safwat.hr.hrScale.allowance.entity.AllowanceDefinition;
import com.safwat.hr.hrScale.allowance.entity.AllowanceDefinition.BaseSource;
import com.safwat.hr.hrScale.allowance.entity.AllowanceDefinition.Behavior;
import com.safwat.hr.hrScale.allowance.entity.AllowanceDefinition.CalcType;
import com.safwat.hr.hrScale.allowance.entity.AllowanceDefinition.Scope;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
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
 * </ul>
 *
 * <p>الفورم مرن مع {@code valuesMap} — بيتعامل معاه كـ قائمة مفتاح/قيمة
 * حرة، فمش محتاجين نعرف مسبقًا هل المفتاح رقم درجة أو "all" أو "percent".
 *
 * <p>نداءات الشبكة كلها عن طريق {@link FxApiSupport} فوق
 * {@code com.safwat.hr.network.ApiClient} الـ static.
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
    private TableColumn<AllowanceDefinition, Behavior> col_def_behavior;
    @FXML
    private TableColumn<AllowanceDefinition, CalcType> col_def_calcType;
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
    private TableColumn<AllowanceDefinition, Behavior> col_hist_behavior;
    @FXML
    private TableColumn<AllowanceDefinition, CalcType> col_hist_calcType;
    @FXML
    private TableColumn<AllowanceDefinition, BaseSource> col_hist_baseSource;
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
    private DatePicker date_effectiveFrom;
    @FXML
    private ComboBox<Behavior> combo_behavior;
    @FXML
    private ComboBox<CalcType> combo_calcType;
    @FXML
    private ComboBox<BaseSource> combo_baseSource;
    @FXML
    private ComboBox<Scope> combo_scope;
    @FXML
    private TextField txt_excludedMonths;
    @FXML
    private TextField txt_eligibleLaws;
    @FXML
    private TextField txt_eligibleLawCodes;
    @FXML
    private TextArea txt_notes;

    // valuesMap — مفتاح/قيمة حر
    @FXML
    private TableView<MapEntryRow> table_values;
    @FXML
    private TableColumn<MapEntryRow, String> col_map_key;
    @FXML
    private TableColumn<MapEntryRow, BigDecimal> col_map_value;
    @FXML
    private TableColumn<MapEntryRow, Void> col_map_actions;
    @FXML
    private TextField txt_map_key;
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

    // ── State ────────────────────────────────────────────────────
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final ObservableList<MapEntryRow> valueRows = FXCollections.observableArrayList();
    private Long editingId = null; // null = إنشاء جديد
    private Runnable onChanged; // بينادى لما نقفل الدايلوج لو حصل أي تعديل

    /**
     * سطر مفتاح/قيمة واحد في valuesMap
     */
    private record MapEntryRow(String key, BigDecimal value) {
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
                combo_baseSource.setDisable(val != CalcType.PERCENT_BY_DEGREE));

        hideForm();
        loadDefinitions();
    }

    /**
     * يُستدعى من الـ Controller الرئيسي قبل فتح الـ Dialog.
     *
     * @param onChanged بينادى لما يحصل أي إنشاء/تعديل/حذف، عشان الشاشة
     *                  الرئيسية تحدّث الـ Dropdown بتاعها
     */
    public void init(Runnable onChanged) {
        this.onChanged = onChanged;
    }

    // ════════════════════════════════════════════════════════════
    //  تحميل البيانات
    // ════════════════════════════════════════════════════════════

    private void loadDefinitions() {
        FxApiSupport.getList(
                "/api/allowances/definitions",
                new TypeReference<List<AllowanceDefinition>>() {
                },
                defs -> table_definitions.setItems(FXCollections.observableArrayList(defs)),
                err -> showFormError("خطأ في تحميل القواعد: " + err)
        );
    }

    private void loadHistory(String code) {
        FxApiSupport.getList(
                "/api/allowances/definitions/" + code + "/history",
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
        col_def_calcType.setCellValueFactory(d ->
                new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getCalcType()));
        col_def_scope.setCellValueFactory(d ->
                new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getScope()));

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
        col_hist_calcType.setCellValueFactory(d ->
                new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getCalcType()));
        col_hist_baseSource.setCellValueFactory(d ->
                new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getBaseSource()));

        col_hist_actions.setCellFactory(tc -> new TableCell<>() {
            private final Button btnEdit = new Button("✏");
            private final Button btnDelete = new Button("🗑");

            {
                btnEdit.getStyleClass().add("btn-purple");
                btnEdit.setPrefHeight(26);
                btnEdit.setPrefWidth(36);
                btnDelete.getStyleClass().add("btn-danger");
                btnDelete.setPrefHeight(26);
                btnDelete.setPrefWidth(36);

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
                HBox box = new HBox(6, btnEdit, btnDelete);
                box.setStyle("-fx-alignment: CENTER;");
                setGraphic(box);
            }
        });
    }

    private void setupValuesTable() {
        col_map_key.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(d.getValue().key()));
        col_map_value.setCellValueFactory(d ->
                new javafx.beans.property.SimpleObjectProperty<>(d.getValue().value()));
        col_map_actions.setCellFactory(tc -> new TableCell<>() {
            private final Button btnDelete = new Button("🗑");

            {
                btnDelete.getStyleClass().add("btn-danger");
                btnDelete.setPrefHeight(24);
                btnDelete.setPrefWidth(32);
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
        combo_calcType.setItems(FXCollections.observableArrayList(CalcType.values()));
        combo_baseSource.setItems(FXCollections.observableArrayList(BaseSource.values()));
        combo_scope.setItems(FXCollections.observableArrayList(Scope.values()));
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

    // ════════════════════════════════════════════════════════════
    //  الفورم — إظهار/إخفاء
    // ════════════════════════════════════════════════════════════

    private void showFormForNewCode() {
        clearForm();
        editingId = null;
        txt_code.setDisable(false);
        lbl_form_title.setText("إضافة بدل جديد بالكامل");
        showForm();
    }

    private void showFormForNewSnapshot() {
        AllowanceDefinition selected = table_definitions.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        clearForm();
        editingId = null;
        txt_code.setText(selected.getCode());
        txt_code.setDisable(true); // نفس الكود إجباري
        txt_nameAr.setText(selected.getNameAr());
        txt_nameEn.setText(selected.getNameEn());
        lbl_form_title.setText("إضافة تاريخ جديد لبدل: " + selected.getCode());
        showForm();
    }

    private void showFormForEdit(AllowanceDefinition snap) {
        clearForm();
        editingId = snap.getId();
        txt_code.setText(snap.getCode());
        txt_code.setDisable(true); // الكود مش قابل للتغيير في تعديل موجود
        txt_nameAr.setText(snap.getNameAr());
        txt_nameEn.setText(snap.getNameEn());
        date_effectiveFrom.setValue(snap.getEffectiveFrom());
        combo_behavior.setValue(snap.getBehavior());
        combo_calcType.setValue(snap.getCalcType());
        combo_baseSource.setValue(snap.getBaseSource());
        combo_scope.setValue(snap.getScope() != null ? snap.getScope() : Scope.GENERAL);
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
        txt_code.setDisable(false);
        txt_nameAr.clear();
        txt_nameEn.clear();
        date_effectiveFrom.setValue(null);
        combo_behavior.setValue(null);
        combo_calcType.setValue(null);
        combo_baseSource.setValue(null);
        combo_scope.setValue(Scope.GENERAL);
        txt_excludedMonths.clear();
        txt_eligibleLaws.clear();
        txt_eligibleLawCodes.clear();
        txt_notes.clear();
        valueRows.clear();
        txt_map_key.clear();
        txt_map_value.clear();
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
        String key = txt_map_key.getText().trim();
        String valStr = txt_map_value.getText().trim();

        if (key.isBlank() || valStr.isBlank()) {
            showFormError("المفتاح والقيمة مطلوبين لإضافة صف في valuesMap");
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
        valueRows.removeIf(r -> r.key().equals(key));
        valueRows.add(new MapEntryRow(key, value));

        txt_map_key.clear();
        txt_map_value.clear();
        lbl_form_error.setVisible(false);
    }

    // ════════════════════════════════════════════════════════════
    //  حفظ
    // ════════════════════════════════════════════════════════════

    private void handleSave() {
        String code = txt_code.getText().trim();
        String nameAr = txt_nameAr.getText().trim();
        LocalDate effectiveFrom = date_effectiveFrom.getValue();
        Behavior behavior = combo_behavior.getValue();
        CalcType calcType = combo_calcType.getValue();
        BaseSource baseSource = combo_baseSource.getValue();
        Scope scope = combo_scope.getValue();

        if (code.isBlank()) {
            showFormError("كود البدل مطلوب");
            return;
        }
        if (nameAr.isBlank()) {
            showFormError("اسم البدل بالعربي مطلوب");
            return;
        }
        if (effectiveFrom == null) {
            showFormError("تاريخ السريان مطلوب");
            return;
        }
        if (behavior == null || calcType == null) {
            showFormError("سلوك الاحتساب ونوعه مطلوبين");
            return;
        }
        if (calcType == CalcType.PERCENT_BY_DEGREE && baseSource == null) {
            showFormError("PERCENT_BY_DEGREE يحتاج تحديد مصدر الأساسي");
            return;
        }
        if (valueRows.isEmpty()) {
            showFormError("أضف على الأقل قيمة واحدة في valuesMap");
            return;
        }

        Map<String, BigDecimal> valuesMap = new LinkedHashMap<>();
        valueRows.forEach(r -> valuesMap.put(r.key(), r.value()));

        AllowanceDefinitionRequest request = new AllowanceDefinitionRequest(
                code, nameAr,
                txt_nameEn.getText().isBlank() ? null : txt_nameEn.getText().trim(),
                effectiveFrom, behavior, calcType, baseSource, scope,
                valuesMap,
                splitOrNull(txt_excludedMonths.getText()),
                splitOrNull(txt_eligibleLaws.getText()),
                splitOrNull(txt_eligibleLawCodes.getText()),
                txt_notes.getText().isBlank() ? null : txt_notes.getText().trim()
        );

        btn_save_definition.setDisable(true);

        if (editingId != null) {
            FxApiSupport.put(
                    "/api/allowances/definitions/" + editingId,
                    request,
                    AllowanceDefinition.class,
                    saved -> onSaveSuccess(),
                    this::onSaveError
            );
        } else {
            FxApiSupport.post(
                    "/api/allowances/definitions",
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
                        "/api/allowances/definitions/" + snap.getId(),
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