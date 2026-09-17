package com.safwat.hr.controller.entitlements.statutory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.safwat.hr.controller.entitlements.allowance.FxApiSupport;
import com.safwat.hr.controller.entitlements.statutory.StatutoryDtos.InsuranceRateConfigDto;
import com.safwat.hr.controller.entitlements.statutory.StatutoryDtos.StampDutyBracket;
import com.safwat.hr.controller.entitlements.statutory.StatutoryDtos.TaxBracket;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class StatutoryDialogController implements Initializable {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

    // ══════════════════════════════════════════════════════════════
    //  تاب التأمينات
    // ══════════════════════════════════════════════════════════════
    @FXML
    private TableView<InsuranceRateConfigDto> table_insurance;
    @FXML
    private TableColumn<InsuranceRateConfigDto, LocalDate> col_ins_from;
    @FXML
    private TableColumn<InsuranceRateConfigDto, LocalDate> col_ins_to;
    @FXML
    private TableColumn<InsuranceRateConfigDto, String> col_ins_mode;
    @FXML
    private TableColumn<InsuranceRateConfigDto, String> col_ins_employee;
    @FXML
    private TableColumn<InsuranceRateConfigDto, String> col_ins_employer;
    @FXML
    private TableColumn<InsuranceRateConfigDto, BigDecimal> col_ins_floor;
    @FXML
    private TableColumn<InsuranceRateConfigDto, BigDecimal> col_ins_ceiling;
    @FXML
    private TableColumn<InsuranceRateConfigDto, Void> col_ins_actions;

    @FXML
    private VBox pane_ins_form;
    @FXML
    private Label lbl_ins_form_title;
    @FXML
    private TextField txt_ins_from;
    @FXML
    private TextField txt_ins_to;

    // نظام موحّد
    @FXML
    private TextField txt_ins_employee;
    @FXML
    private TextField txt_ins_employer;

    // نظام وعائين
    @FXML
    private TextField txt_ins_basic_employee;
    @FXML
    private TextField txt_ins_basic_employer;
    @FXML
    private TextField txt_ins_variable_employee;
    @FXML
    private TextField txt_ins_variable_employer;

    @FXML
    private TextField txt_ins_floor;
    @FXML
    private TextField txt_ins_ceiling;
    @FXML
    private TextField txt_ins_notes;
    @FXML
    private Label lbl_ins_error;
    @FXML
    private Button btn_ins_save;
    @FXML
    private Button btn_ins_cancel;

    // ══════════════════════════════════════════════════════════════
    //  تاب الضريبة
    // ══════════════════════════════════════════════════════════════
    @FXML
    private TableView<TaxBracket> table_tax;
    @FXML
    private TableColumn<TaxBracket, LocalDate> col_tax_from;
    @FXML
    private TableColumn<TaxBracket, LocalDate> col_tax_to;
    @FXML
    private TableColumn<TaxBracket, Integer> col_tax_order;
    @FXML
    private TableColumn<TaxBracket, BigDecimal> col_tax_fromAmt;
    @FXML
    private TableColumn<TaxBracket, BigDecimal> col_tax_toAmt;
    @FXML
    private TableColumn<TaxBracket, BigDecimal> col_tax_rate;
    @FXML
    private TableColumn<TaxBracket, Void> col_tax_actions;

    @FXML
    private VBox pane_tax_form;
    @FXML
    private Label lbl_tax_form_title;
    @FXML
    private TextField txt_tax_from;
    @FXML
    private TextField txt_tax_to;
    @FXML
    private TextField txt_tax_order;
    @FXML
    private TextField txt_tax_fromAmt;
    @FXML
    private TextField txt_tax_toAmt;
    @FXML
    private TextField txt_tax_rate;
    @FXML
    private TextField txt_tax_notes;
    @FXML
    private Label lbl_tax_error;
    @FXML
    private Button btn_tax_save;
    @FXML
    private Button btn_tax_cancel;

    // ══════════════════════════════════════════════════════════════
    //  تاب الدمغة
    // ══════════════════════════════════════════════════════════════
    @FXML
    private TableView<StampDutyBracket> table_stamp;
    @FXML
    private TableColumn<StampDutyBracket, LocalDate> col_stamp_from;
    @FXML
    private TableColumn<StampDutyBracket, LocalDate> col_stamp_to;
    @FXML
    private TableColumn<StampDutyBracket, Integer> col_stamp_order;
    @FXML
    private TableColumn<StampDutyBracket, BigDecimal> col_stamp_fromAmt;
    @FXML
    private TableColumn<StampDutyBracket, BigDecimal> col_stamp_toAmt;
    @FXML
    private TableColumn<StampDutyBracket, BigDecimal> col_stamp_rate;
    @FXML
    private TableColumn<StampDutyBracket, Void> col_stamp_actions;

    @FXML
    private VBox pane_stamp_form;
    @FXML
    private Label lbl_stamp_form_title;
    @FXML
    private TextField txt_stamp_from;
    @FXML
    private TextField txt_stamp_to;
    @FXML
    private TextField txt_stamp_order;
    @FXML
    private TextField txt_stamp_fromAmt;
    @FXML
    private TextField txt_stamp_toAmt;
    @FXML
    private TextField txt_stamp_rate;
    @FXML
    private TextField txt_stamp_notes;
    @FXML
    private Label lbl_stamp_error;
    @FXML
    private Button btn_stamp_save;
    @FXML
    private Button btn_stamp_cancel;

    @FXML
    private Button btn_close;

    // ══════════════════════════════════════════════════════════════
    //  State
    // ══════════════════════════════════════════════════════════════
    private final ObservableList<InsuranceRateConfigDto> insItems = FXCollections.observableArrayList();
    private final ObservableList<TaxBracket> taxItems = FXCollections.observableArrayList();
    private final ObservableList<StampDutyBracket> stampItems = FXCollections.observableArrayList();

    private Long editingInsuranceId = null;
    private Long editingTaxId = null;
    private Long editingStampId = null;

    private Runnable onChanged;

    // ══════════════════════════════════════════════════════════════
    //  Initialize
    // ══════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupInsuranceTable();
        setupTaxTable();
        setupStampTable();

        btn_ins_save.setOnAction(e -> saveInsurance());
        btn_ins_cancel.setOnAction(e -> hideInsuranceForm());
        btn_tax_save.setOnAction(e -> saveTax());
        btn_tax_cancel.setOnAction(e -> hideTaxForm());
        btn_stamp_save.setOnAction(e -> saveStamp());
        btn_stamp_cancel.setOnAction(e -> hideStampForm());
        btn_close.setOnAction(e -> closeDialog());

        hideInsuranceForm();
        hideTaxForm();
        hideStampForm();

        loadAll();
    }

    public void init(Runnable onChanged) {
        this.onChanged = onChanged;
    }

    // ══════════════════════════════════════════════════════════════
    //  Load
    // ══════════════════════════════════════════════════════════════

    private void loadAll() {
        loadInsurance();
        loadTax();
        loadStamp();
    }

    private void loadInsurance() {
        FxApiSupport.getList("/entitlements/statutory/insurance",
                new TypeReference<List<InsuranceRateConfigDto>>() {
                },
                list -> {
                    insItems.setAll(list);
                    table_insurance.setItems(insItems);
                },
                err -> showError(lbl_ins_error, "خطأ: " + err));
    }

    private void loadTax() {
        FxApiSupport.getList("/entitlements/statutory/tax",
                new TypeReference<List<TaxBracket>>() {
                },
                list -> {
                    taxItems.setAll(list);
                    table_tax.setItems(taxItems);
                },
                err -> showError(lbl_tax_error, "خطأ: " + err));
    }

    private void loadStamp() {
        FxApiSupport.getList("/entitlements/statutory/stamp",
                new TypeReference<List<StampDutyBracket>>() {
                },
                list -> {
                    stampItems.setAll(list);
                    table_stamp.setItems(stampItems);
                },
                err -> showError(lbl_stamp_error, "خطأ: " + err));
    }

    // ══════════════════════════════════════════════════════════════
    //  Insurance
    // ══════════════════════════════════════════════════════════════

    private void setupInsuranceTable() {
        col_ins_from.setCellValueFactory(d ->
                new javafx.beans.property.SimpleObjectProperty<>(d.getValue().effectiveFrom()));
        col_ins_from.setCellFactory(tc -> dateCell());

        col_ins_to.setCellValueFactory(d ->
                new javafx.beans.property.SimpleObjectProperty<>(d.getValue().effectiveTo()));
        col_ins_to.setCellFactory(tc -> dateCell());

        col_ins_mode.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(
                        d.getValue().isSplitMode() ? "وعائين" : "موحّد"));

        col_ins_employee.setCellValueFactory(d -> {
            InsuranceRateConfigDto c = d.getValue();
            return new javafx.beans.property.SimpleStringProperty(
                    c.isSplitMode()
                            ? toPercent(c.basicEmployeeRate()) + "% + " + toPercent(c.variableEmployeeRate()) + "%"
                            : toPercent(c.employeeRate()) + "%");
        });

        col_ins_employer.setCellValueFactory(d -> {
            InsuranceRateConfigDto c = d.getValue();
            return new javafx.beans.property.SimpleStringProperty(
                    c.isSplitMode()
                            ? toPercent(c.basicEmployerRate()) + "% + " + toPercent(c.variableEmployerRate()) + "%"
                            : toPercent(c.employerRate()) + "%");
        });

        col_ins_floor.setCellValueFactory(d ->
                new javafx.beans.property.SimpleObjectProperty<>(d.getValue().wageFloor()));
        col_ins_floor.setCellFactory(tc -> moneyCell());

        col_ins_ceiling.setCellValueFactory(d ->
                new javafx.beans.property.SimpleObjectProperty<>(d.getValue().wageCeiling()));
        col_ins_ceiling.setCellFactory(tc -> moneyCell());

        col_ins_actions.setCellFactory(tc -> actionsCell(
                this::showInsuranceFormForEdit,
                this::deleteInsurance));

        table_insurance.setItems(insItems);
    }

    private void showInsuranceFormForNew() {
        clearInsuranceForm();
        editingInsuranceId = null;
        lbl_ins_form_title.setText("إضافة إعداد تأمينات جديد");
        show(pane_ins_form);
    }

    private void showInsuranceFormForEdit(InsuranceRateConfigDto cfg) {
        clearInsuranceForm();
        editingInsuranceId = cfg.id();
        lbl_ins_form_title.setText("تعديل إعداد التأمينات #" + cfg.id());
        txt_ins_from.setText(cfg.effectiveFrom() == null ? "" : cfg.effectiveFrom().format(DATE_FMT));
        txt_ins_to.setText(cfg.effectiveTo() == null ? "" : cfg.effectiveTo().format(DATE_FMT));

        // النظام الموحّد
        txt_ins_employee.setText(cfg.employeeRate() == null ? "" : toPercent(cfg.employeeRate()));
        txt_ins_employer.setText(cfg.employerRate() == null ? "" : toPercent(cfg.employerRate()));

        // نظام الوعائين
        txt_ins_basic_employee.setText(cfg.basicEmployeeRate() == null ? "" : toPercent(cfg.basicEmployeeRate()));
        txt_ins_basic_employer.setText(cfg.basicEmployerRate() == null ? "" : toPercent(cfg.basicEmployerRate()));
        txt_ins_variable_employee.setText(cfg.variableEmployeeRate() == null ? "" : toPercent(cfg.variableEmployeeRate()));
        txt_ins_variable_employer.setText(cfg.variableEmployerRate() == null ? "" : toPercent(cfg.variableEmployerRate()));

        txt_ins_floor.setText(cfg.wageFloor() == null ? "" : cfg.wageFloor().toPlainString());
        txt_ins_ceiling.setText(cfg.wageCeiling() == null ? "" : cfg.wageCeiling().toPlainString());
        txt_ins_notes.setText(cfg.notes() == null ? "" : cfg.notes());
        show(pane_ins_form);
    }

    private void saveInsurance() {
        lbl_ins_error.setVisible(false);
        try {
            LocalDate from = parseDate(txt_ins_from.getText(), "تاريخ السريان مطلوب", true);
            LocalDate to = parseDate(txt_ins_to.getText(), null, false);

            // النظام الموحّد
            BigDecimal empRate = parsePercentOrNull(txt_ins_employee.getText(), "نسبة الموظف");
            BigDecimal emprRate = parsePercentOrNull(txt_ins_employer.getText(), "نسبة صاحب العمل");

            // نظام الوعائين
            BigDecimal basicEmp = parsePercentOrNull(txt_ins_basic_employee.getText(), "نسبة الموظف (أساسي)");
            BigDecimal basicGov = parsePercentOrNull(txt_ins_basic_employer.getText(), "نسبة صاحب العمل (أساسي)");
            BigDecimal varEmp = parsePercentOrNull(txt_ins_variable_employee.getText(), "نسبة الموظف (متغير)");
            BigDecimal varGov = parsePercentOrNull(txt_ins_variable_employer.getText(), "نسبة صاحب العمل (متغير)");

            // تحقق: لازم نستخدم نظام واحد بس
            boolean hasCombined = empRate != null && emprRate != null;
            boolean hasSplit = basicEmp != null && basicGov != null && varEmp != null && varGov != null;
            boolean hasPartialCombined = (empRate == null) != (emprRate == null);
            boolean hasPartialSplit = !hasSplit && (basicEmp != null || basicGov != null || varEmp != null || varGov != null);

            if (hasCombined && hasSplit) {
                throw new IllegalArgumentException("املا النظام الموحّد أو نظام الوعائين — مش الاتنين");
            }
            if (hasPartialCombined) {
                throw new IllegalArgumentException("النظام الموحّد يحتاج النسبتين مع بعض");
            }
            if (hasPartialSplit) {
                throw new IllegalArgumentException("نظام الوعائين يحتاج الأربع نسب مع بعض");
            }
            if (!hasCombined && !hasSplit) {
                throw new IllegalArgumentException("املا نسب النظام الموحّد أو نسب نظام الوعائين");
            }

            BigDecimal floor = parseMoney(txt_ins_floor.getText(), "الحد الأدنى");
            BigDecimal ceiling = parseMoney(txt_ins_ceiling.getText(), "الحد الأقصى");
            String notes = txt_ins_notes.getText();

            InsuranceRateConfigDto dto = new InsuranceRateConfigDto(
                    editingInsuranceId, from, to,
                    empRate, emprRate,
                    basicEmp, basicGov, varEmp, varGov,
                    floor, ceiling,
                    notes == null || notes.isBlank() ? null : notes.trim()
            );

            btn_ins_save.setDisable(true);
            if (editingInsuranceId == null) {
                FxApiSupport.post("/entitlements/statutory/insurance", dto, InsuranceRateConfigDto.class,
                        saved -> onInsSaved(), this::onInsError);
            } else {
                FxApiSupport.put("/entitlements/statutory/insurance/" + editingInsuranceId, dto,
                        InsuranceRateConfigDto.class,
                        saved -> onInsSaved(), this::onInsError);
            }
        } catch (IllegalArgumentException ex) {
            showError(lbl_ins_error, ex.getMessage());
        }
    }

    private void onInsSaved() {
        btn_ins_save.setDisable(false);
        hideInsuranceForm();
        loadInsurance();
        if (onChanged != null) onChanged.run();
    }

    private void onInsError(String err) {
        btn_ins_save.setDisable(false);
        showError(lbl_ins_error, "خطأ في الحفظ: " + err);
    }

    private void deleteInsurance(InsuranceRateConfigDto cfg) {
        if (!confirmDelete("إعداد التأمينات #" + cfg.id())) return;
        FxApiSupport.delete("/entitlements/statutory/insurance/" + cfg.id(),
                () -> {
                    loadInsurance();
                    if (onChanged != null) onChanged.run();
                },
                err -> showError(lbl_ins_error, "خطأ في الحذف: " + err));
    }

    private void clearInsuranceForm() {
        txt_ins_from.clear();
        txt_ins_to.clear();
        txt_ins_employee.clear();
        txt_ins_employer.clear();
        txt_ins_basic_employee.clear();
        txt_ins_basic_employer.clear();
        txt_ins_variable_employee.clear();
        txt_ins_variable_employer.clear();
        txt_ins_floor.clear();
        txt_ins_ceiling.clear();
        txt_ins_notes.clear();
        lbl_ins_error.setVisible(false);
    }

    private void hideInsuranceForm() {
        hide(pane_ins_form);
        editingInsuranceId = null;
    }

    // ══════════════════════════════════════════════════════════════
    //  Tax — نفس اللي كان
    // ══════════════════════════════════════════════════════════════

    private void setupTaxTable() {
        col_tax_from.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().effectiveFrom()));
        col_tax_from.setCellFactory(tc -> dateCell());
        col_tax_to.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().effectiveTo()));
        col_tax_to.setCellFactory(tc -> dateCell());
        col_tax_order.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().bracketOrder()));
        col_tax_fromAmt.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().fromAmount()));
        col_tax_fromAmt.setCellFactory(tc -> moneyCell());
        col_tax_toAmt.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().toAmount()));
        col_tax_toAmt.setCellFactory(tc -> moneyCell());
        col_tax_rate.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().rate()));
        col_tax_rate.setCellFactory(tc -> percentCell());
        col_tax_actions.setCellFactory(tc -> actionsCell(this::showTaxFormForEdit, this::deleteTax));
        table_tax.setItems(taxItems);
    }

    private void showTaxFormForNew() {
        clearTaxForm();
        editingTaxId = null;
        lbl_tax_form_title.setText("إضافة شريحة ضريبة جديدة (قيم سنوية)");
        show(pane_tax_form);
    }

    private void showTaxFormForEdit(TaxBracket b) {
        clearTaxForm();
        editingTaxId = b.id();
        lbl_tax_form_title.setText("تعديل شريحة الضريبة #" + b.id());
        txt_tax_from.setText(b.effectiveFrom() == null ? "" : b.effectiveFrom().format(DATE_FMT));
        txt_tax_to.setText(b.effectiveTo() == null ? "" : b.effectiveTo().format(DATE_FMT));
        txt_tax_order.setText(String.valueOf(b.bracketOrder()));
        txt_tax_fromAmt.setText(b.fromAmount() == null ? "" : b.fromAmount().toPlainString());
        txt_tax_toAmt.setText(b.toAmount() == null ? "" : b.toAmount().toPlainString());
        txt_tax_rate.setText(b.rate() == null ? "" : toPercent(b.rate()));
        txt_tax_notes.setText(b.notes() == null ? "" : b.notes());
        show(pane_tax_form);
    }

    private void saveTax() {
        lbl_tax_error.setVisible(false);
        try {
            LocalDate from = parseDate(txt_tax_from.getText(), "تاريخ السريان مطلوب", true);
            LocalDate to = parseDate(txt_tax_to.getText(), null, false);
            int order = parseInt(txt_tax_order.getText(), "ترتيب الشريحة");
            BigDecimal fromAmt = parseMoney(txt_tax_fromAmt.getText(), "المبلغ من");
            BigDecimal toAmt = parseMoney(txt_tax_toAmt.getText(), "المبلغ إلى");
            BigDecimal rate = parsePercent(txt_tax_rate.getText(), "النسبة");
            String notes = txt_tax_notes.getText();

            TaxBracket dto = new TaxBracket(
                    editingTaxId, from, to, order, fromAmt, toAmt, rate,
                    notes == null || notes.isBlank() ? null : notes.trim());

            btn_tax_save.setDisable(true);
            if (editingTaxId == null) {
                FxApiSupport.post("/entitlements/statutory/tax", dto, TaxBracket.class,
                        saved -> onTaxSaved(), this::onTaxError);
            } else {
                FxApiSupport.put("/entitlements/statutory/tax/" + editingTaxId, dto, TaxBracket.class,
                        saved -> onTaxSaved(), this::onTaxError);
            }
        } catch (IllegalArgumentException ex) {
            showError(lbl_tax_error, ex.getMessage());
        }
    }

    private void onTaxSaved() {
        btn_tax_save.setDisable(false);
        hideTaxForm();
        loadTax();
        if (onChanged != null) onChanged.run();
    }

    private void onTaxError(String err) {
        btn_tax_save.setDisable(false);
        showError(lbl_tax_error, "خطأ في الحفظ: " + err);
    }

    private void deleteTax(TaxBracket b) {
        if (!confirmDelete("شريحة الضريبة #" + b.id())) return;
        FxApiSupport.delete("/entitlements/statutory/tax/" + b.id(),
                () -> {
                    loadTax();
                    if (onChanged != null) onChanged.run();
                },
                err -> showError(lbl_tax_error, "خطأ في الحذف: " + err));
    }

    private void clearTaxForm() {
        txt_tax_from.clear();
        txt_tax_to.clear();
        txt_tax_order.clear();
        txt_tax_fromAmt.clear();
        txt_tax_toAmt.clear();
        txt_tax_rate.clear();
        txt_tax_notes.clear();
        lbl_tax_error.setVisible(false);
    }

    private void hideTaxForm() {
        hide(pane_tax_form);
        editingTaxId = null;
    }

    // ══════════════════════════════════════════════════════════════
    //  Stamp
    // ══════════════════════════════════════════════════════════════

    private void setupStampTable() {
        col_stamp_from.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().effectiveFrom()));
        col_stamp_from.setCellFactory(tc -> dateCell());
        col_stamp_to.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().effectiveTo()));
        col_stamp_to.setCellFactory(tc -> dateCell());
        col_stamp_order.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().bracketOrder()));
        col_stamp_fromAmt.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().fromAmount()));
        col_stamp_fromAmt.setCellFactory(tc -> moneyCell());
        col_stamp_toAmt.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().toAmount()));
        col_stamp_toAmt.setCellFactory(tc -> moneyCell());
        col_stamp_rate.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().rate()));
        col_stamp_rate.setCellFactory(tc -> percentCell());
        col_stamp_actions.setCellFactory(tc -> actionsCell(this::showStampFormForEdit, this::deleteStamp));
        table_stamp.setItems(stampItems);
    }

    private void showStampFormForNew() {
        clearStampForm();
        editingStampId = null;
        lbl_stamp_form_title.setText("إضافة شريحة دمغة جديدة");
        show(pane_stamp_form);
    }

    private void showStampFormForEdit(StampDutyBracket b) {
        clearStampForm();
        editingStampId = b.id();
        lbl_stamp_form_title.setText("تعديل شريحة الدمغة #" + b.id());
        txt_stamp_from.setText(b.effectiveFrom() == null ? "" : b.effectiveFrom().format(DATE_FMT));
        txt_stamp_to.setText(b.effectiveTo() == null ? "" : b.effectiveTo().format(DATE_FMT));
        txt_stamp_order.setText(String.valueOf(b.bracketOrder()));
        txt_stamp_fromAmt.setText(b.fromAmount() == null ? "" : b.fromAmount().toPlainString());
        txt_stamp_toAmt.setText(b.toAmount() == null ? "" : b.toAmount().toPlainString());
        txt_stamp_rate.setText(b.rate() == null ? "" : toPercent(b.rate()));
        txt_stamp_notes.setText(b.notes() == null ? "" : b.notes());
        show(pane_stamp_form);
    }

    private void saveStamp() {
        lbl_stamp_error.setVisible(false);
        try {
            LocalDate from = parseDate(txt_stamp_from.getText(), "تاريخ السريان مطلوب", true);
            LocalDate to = parseDate(txt_stamp_to.getText(), null, false);
            int order = parseInt(txt_stamp_order.getText(), "ترتيب الشريحة");
            BigDecimal fromAmt = parseMoney(txt_stamp_fromAmt.getText(), "المبلغ من");
            BigDecimal toAmt = parseMoney(txt_stamp_toAmt.getText(), "المبلغ إلى");
            BigDecimal rate = parsePercent(txt_stamp_rate.getText(), "النسبة");
            String notes = txt_stamp_notes.getText();

            StampDutyBracket dto = new StampDutyBracket(
                    editingStampId, from, to, order, fromAmt, toAmt, rate,
                    notes == null || notes.isBlank() ? null : notes.trim());

            btn_stamp_save.setDisable(true);
            if (editingStampId == null) {
                FxApiSupport.post("/entitlements/statutory/stamp", dto, StampDutyBracket.class,
                        saved -> onStampSaved(), this::onStampError);
            } else {
                FxApiSupport.put("/entitlements/statutory/stamp/" + editingStampId, dto,
                        StampDutyBracket.class,
                        saved -> onStampSaved(), this::onStampError);
            }
        } catch (IllegalArgumentException ex) {
            showError(lbl_stamp_error, ex.getMessage());
        }
    }

    private void onStampSaved() {
        btn_stamp_save.setDisable(false);
        hideStampForm();
        loadStamp();
        if (onChanged != null) onChanged.run();
    }

    private void onStampError(String err) {
        btn_stamp_save.setDisable(false);
        showError(lbl_stamp_error, "خطأ في الحفظ: " + err);
    }

    private void deleteStamp(StampDutyBracket b) {
        if (!confirmDelete("شريحة الدمغة #" + b.id())) return;
        FxApiSupport.delete("/entitlements/statutory/stamp/" + b.id(),
                () -> {
                    loadStamp();
                    if (onChanged != null) onChanged.run();
                },
                err -> showError(lbl_stamp_error, "خطأ في الحذف: " + err));
    }

    private void clearStampForm() {
        txt_stamp_from.clear();
        txt_stamp_to.clear();
        txt_stamp_order.clear();
        txt_stamp_fromAmt.clear();
        txt_stamp_toAmt.clear();
        txt_stamp_rate.clear();
        txt_stamp_notes.clear();
        lbl_stamp_error.setVisible(false);
    }

    private void hideStampForm() {
        hide(pane_stamp_form);
        editingStampId = null;
    }

    // ══════════════════════════════════════════════════════════════
    //  Handlers
    // ══════════════════════════════════════════════════════════════

    @FXML
    public void onAddInsurance() {
        showInsuranceFormForNew();
    }

    @FXML
    public void onAddTax() {
        showTaxFormForNew();
    }

    @FXML
    public void onAddStamp() {
        showStampFormForNew();
    }

    // ══════════════════════════════════════════════════════════════
    //  Helpers — parsing
    // ══════════════════════════════════════════════════════════════

    private LocalDate parseDate(String text, String requiredMsg, boolean required) {
        if (text == null || text.isBlank()) {
            if (required) throw new IllegalArgumentException(requiredMsg);
            return null;
        }
        text = text.trim();
        if (!DATE_PATTERN.matcher(text).matches())
            throw new IllegalArgumentException("الصيغة لازم تكون yyyy-MM-dd: " + text);
        try {
            return LocalDate.parse(text, DATE_FMT);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("تاريخ غير صحيح: " + text);
        }
    }

    private int parseInt(String text, String label) {
        if (text == null || text.isBlank())
            throw new IllegalArgumentException(label + " مطلوب");
        try {
            int v = Integer.parseInt(text.trim());
            if (v <= 0) throw new IllegalArgumentException(label + " لازم يكون أكبر من صفر");
            return v;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " لازم يكون رقماً صحيحاً");
        }
    }

    private BigDecimal parseMoney(String text, String label) {
        if (text == null || text.isBlank()) return null;
        try {
            return new BigDecimal(text.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " لازم يكون رقماً: " + text);
        }
    }

    /**
     * نسبة عشرية مطلوبة — لازم تتعبى.
     */
    private BigDecimal parsePercent(String text, String label) {
        if (text == null || text.isBlank())
            throw new IllegalArgumentException(label + " مطلوبة");
        return parsePercentInternal(text.trim(), label);
    }

    /**
     * نسبة عشرية اختيارية — null لو فاضية.
     */
    private BigDecimal parsePercentOrNull(String text, String label) {
        if (text == null || text.isBlank()) return null;
        return parsePercentInternal(text.trim(), label);
    }

    private BigDecimal parsePercentInternal(String trimmed, String label) {
        try {
            BigDecimal pct = new BigDecimal(trimmed);
            if (pct.signum() < 0) throw new IllegalArgumentException(label + " لازم تكون موجبة");
            return pct.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " لازم تكون رقماً: " + trimmed);
        }
    }

    private String toPercent(BigDecimal decimal) {
        if (decimal == null) return "";
        return decimal.multiply(BigDecimal.valueOf(100))
                .stripTrailingZeros()
                .toPlainString();
    }

    // ══════════════════════════════════════════════════════════════
    //  Helpers — cells
    // ══════════════════════════════════════════════════════════════

    private <S> TableCell<S, LocalDate> dateCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(LocalDate v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? "—" : v.format(DATE_FMT));
                setStyle("-fx-alignment: CENTER;");
            }
        };
    }

    private <S> TableCell<S, BigDecimal> moneyCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? "—" : v.toPlainString());
                setStyle("-fx-alignment: CENTER;");
            }
        };
    }

    private <S> TableCell<S, BigDecimal> percentCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? "—" : toPercent(v) + "%");
                setStyle("-fx-alignment: CENTER;");
            }
        };
    }

    private <S> TableCell<S, Void> actionsCell(Consumer<S> onEdit, Consumer<S> onDelete) {
        return new TableCell<>() {
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
                    int idx = getIndex();
                    if (idx >= 0 && idx < getTableView().getItems().size())
                        onEdit.accept(getTableView().getItems().get(idx));
                });
                btnDelete.setOnAction(e -> {
                    int idx = getIndex();
                    if (idx >= 0 && idx < getTableView().getItems().size())
                        onDelete.accept(getTableView().getItems().get(idx));
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
        };
    }

    private boolean confirmDelete(String name) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "حذف " + name + "؟", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("تأكيد الحذف");
        confirm.setHeaderText(null);
        return confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    private void showError(Label label, String msg) {
        label.setText(msg);
        label.setVisible(true);
    }

    private void show(VBox pane) {
        pane.setVisible(true);
        pane.setManaged(true);
    }

    private void hide(VBox pane) {
        pane.setVisible(false);
        pane.setManaged(false);
    }

    private void closeDialog() {
        ((Stage) btn_close.getScene().getWindow()).close();
    }
}