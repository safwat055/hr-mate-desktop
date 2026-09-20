package com.safwat.hr.controller.entitlements.allowance;

import com.fasterxml.jackson.core.type.TypeReference;
import com.safwat.hr.controller.entitlements.allowance.AllowanceDefinition.ElementType;
import com.safwat.hr.controller.entitlements.statutory.StatutoryDialogController;
import com.safwat.hr.controller.scale.scale.dto.ScaleDto;
import com.safwat.hr.ui.TextFieldSetupHelper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class AllowanceFxController implements Initializable {

    // ── شريط البحث ──────────────────────────────────────────────
    @FXML
    private Button btn_supplementaryPdf;
    @FXML
    private Button btn_statutory;
    @FXML
    private TextField txt_nationalId;
    @FXML
    private Button btn_search;
    @FXML
    private ProgressIndicator progress_indicator;
    @FXML
    private Label lbl_error;
    @FXML
    private Button btn_reset;
    @FXML
    private Button btn_manageDefinitions;

    // ── تاب بيانات الموظف ───────────────────────────────────────
    @FXML
    private TextField txt_empName;
    @FXML
    private TextField txt_empNationalId;
    @FXML
    private TextField txt_empLawCode;
    @FXML
    private TextField txt_empLaw;
    @FXML
    private TextField txt_empStartDate;
    @FXML
    private TextField txt_empDegree;
    @FXML
    private TextField txt_empBasic30;
    @FXML
    private TextField txt_empBasic30From;
    @FXML
    private TextField txt_empGroup;

    // ── تاب البدلات ─────────────────────────────────────────────
    @FXML
    private TextField txt_calculationDate;          // 🆕 بدل DatePicker
    @FXML
    private Button btn_recalculate;
    @FXML
    private TextField txt_newAllowance;             // 🆕 بدل ComboBox
    @FXML
    private Button btn_showAllowances;              // 🆕 زر عرض كل البدلات
    @FXML
    private Button btn_addAllowance;
    @FXML
    private Label lbl_total;

    // ── جدول البدلات ────────────────────────────────────────────
    @FXML
    private TableView<AllowanceResultDto.AllowanceLineDto> table_allowances;
    @FXML
    private TableColumn<AllowanceResultDto.AllowanceLineDto, String> col_nameAr;
    @FXML
    private TableColumn<AllowanceResultDto.AllowanceLineDto, ElementType> col_elementType;
    @FXML
    private TableColumn<AllowanceResultDto.AllowanceLineDto, BigDecimal> col_value;
    @FXML
    private TableColumn<AllowanceResultDto.AllowanceLineDto, LocalDate> col_effectiveFrom;
    @FXML
    private TableColumn<AllowanceResultDto.AllowanceLineDto, AllowanceResultDto.AllowanceLineDto.Source> col_source;
    @FXML
    private TableColumn<AllowanceResultDto.AllowanceLineDto, Void> col_actions;
    @FXML
    private Button btn_manageSectors;
    // ── State ────────────────────────────────────────────────────
    private String currentNationalId;
    private AllowanceResultDto currentResult;
    private ScaleDto cachedScaleDto;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 🆕 البدلات المتاحة للإضافة — تُملأ من loadDefinitions.
     */
    private final ObservableList<AllowanceDefinition> availableAllowances =
            FXCollections.observableArrayList();

    /**
     * 🆕 قائمة الاقتراحات المنسدلة.
     */
    private final ContextMenu suggestionsMenu = new ContextMenu();

    /**
     * 🆕 كود البدل اللي المستخدم اختاره من الاقتراحات.
     */
    private String selectedAllowanceCode = null;

    // أكواد السطور الوهمية
    private static final String CODE_SUBTOTAL_ENT = "SUBTOTAL_ENTITLEMENTS";
    private static final String CODE_SUBTOTAL_DED = "SUBTOTAL_DEDUCTIONS";
    private static final String CODE_INFO_BASIC = "INFO_BASIC";
    private static final String CODE_INFO_VARIABLE = "INFO_VARIABLE";
    private static final String CODE_INFO_COMBINED = "INFO_COMBINED";
    private static final String CODE_NET = "NET";

    // ════════════════════════════════════════════════════════════
    //  Initialize
    // ════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        setupDateField();
        setupAllowanceAutocomplete();
        btn_manageSectors.setOnAction(e -> openSectorsDialog());
        // القيمة الافتراضية لتاريخ الاحتساب = اليوم
        txt_calculationDate.setText(LocalDate.now().format(DATE_FMT));

        btn_statutory.setOnAction(e -> openStatutoryDialog());
        txt_nationalId.setOnAction(e -> handleSearch());
        btn_search.setOnAction(e -> handleSearch());
        btn_recalculate.setOnAction(e -> loadAllowances());
        btn_addAllowance.setOnAction(e -> handleAddAllowance());
        btn_showAllowances.setOnAction(e -> showAllAllowancesMenu());
        btn_reset.setOnAction(e -> handleReset());
        btn_supplementaryPdf.setOnAction(e -> handleExportSupplementaryPdf());
        if (btn_manageDefinitions != null) {
            btn_manageDefinitions.setOnAction(e -> openDefinitionsDialog());
        }
    }

    // ════════════════════════════════════════════════════════════
    //  🆕 Date Field — TextField + TextFieldSetupHelper
    // ════════════════════════════════════════════════════════════

    private void setupDateField() {
        TextFieldSetupHelper.setupDateFields(txt_calculationDate);
    }

    /**
     * يقرأ التاريخ من الحقل، أو null لو فاضي/غير صحيح.
     */
    private LocalDate getCalculationDate() {
        return TextFieldSetupHelper.parseDateInput(txt_calculationDate.getText());
    }

    // ════════════════════════════════════════════════════════════
    //  🆕 Allowance Autocomplete
    // ════════════════════════════════════════════════════════════

    private void setupAllowanceAutocomplete() {
        // راقب الكتابة، اعرض اقتراحات
        txt_newAllowance.textProperty().addListener((obs, oldVal, newVal) -> {
            selectedAllowanceCode = null; // أي كتابة تلغي الاختيار السابق
            if (newVal == null || newVal.isBlank()) {
                suggestionsMenu.hide();
                return;
            }
            String q = newVal.trim().toLowerCase();

            List<AllowanceDefinition> matched = availableAllowances.stream()
                    .filter(a -> a.getNameAr() != null
                            && a.getNameAr().toLowerCase().contains(q)
                            || (a.getCode() != null
                            && a.getCode().toLowerCase().contains(q)))
                    .limit(12)
                    .toList();

            if (matched.isEmpty()) {
                suggestionsMenu.hide();
                return;
            }

            suggestionsMenu.getItems().clear();
            for (AllowanceDefinition a : matched) {
                MenuItem item = new MenuItem(a.getNameAr());
                item.setOnAction(e -> {
                    txt_newAllowance.setText(a.getNameAr());
                    selectedAllowanceCode = a.getCode();
                    // حرّك المؤشر للآخر
                    Platform.runLater(() ->
                            txt_newAllowance.positionCaret(txt_newAllowance.getText().length()));
                    suggestionsMenu.hide();
                });
                suggestionsMenu.getItems().add(item);
            }

            if (txt_newAllowance.isFocused() && !suggestionsMenu.isShowing()) {
                suggestionsMenu.show(txt_newAllowance, Side.BOTTOM, 0, 0);
            }
        });

        // إخفاء القائمة عند فقدان التركيز (مع تأخير بسيط للسماح بالضغط)
        txt_newAllowance.focusedProperty().addListener((obs, was, isNow) -> {
            if (!isNow) {
                Platform.runLater(suggestionsMenu::hide);
            }
        });

        // ESC يقفل القائمة
        txt_newAllowance.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case ESCAPE -> suggestionsMenu.hide();
                case DOWN -> {
                    if (!suggestionsMenu.getItems().isEmpty()) {
                        suggestionsMenu.getItems().get(0).fire();
                    }
                }
                default -> { /* no-op */ }
            }
        });
    }

    /**
     * 🆕 زر البحث: يعرض كل البدلات المتاحة (بدون فلترة).
     */
    private void showAllAllowancesMenu() {
        if (availableAllowances.isEmpty()) {
            showError("لا توجد بدلات متاحة للإضافة");
            return;
        }

        suggestionsMenu.getItems().clear();
        for (AllowanceDefinition a : availableAllowances) {
            MenuItem item = new MenuItem(a.getNameAr());
            item.setOnAction(e -> {
                txt_newAllowance.setText(a.getNameAr());
                selectedAllowanceCode = a.getCode();
                suggestionsMenu.hide();
            });
            suggestionsMenu.getItems().add(item);
        }

        if (!suggestionsMenu.isShowing()) {
            suggestionsMenu.show(txt_newAllowance, Side.BOTTOM, 0, 0);
        }
    }

    /**
     * 🆕 يحاول إيجاد البدل المُختار:
     * 1) من selectedAllowanceCode (لو المستخدم اختار من القائمة)
     * 2) من نص الحقل (مطابقة بالاسم أو الكود)
     */
    private AllowanceDefinition resolveSelectedAllowance() {
        String typed = txt_newAllowance.getText();
        if (typed == null || typed.isBlank()) return null;
        typed = typed.trim();

        // 1) من الاختيار المباشر
        if (selectedAllowanceCode != null) {
            final String code = selectedAllowanceCode;
            AllowanceDefinition byCode = availableAllowances.stream()
                    .filter(a -> code.equals(a.getCode()))
                    .findFirst()
                    .orElse(null);
            if (byCode != null) return byCode;
        }

        // 2) مطابقة دقيقة بالاسم أو الكود
        final String q = typed;
        return availableAllowances.stream()
                .filter(a -> q.equals(a.getNameAr()) || q.equals(a.getCode()))
                .findFirst()
                .orElse(null);
    }

    // ════════════════════════════════════════════════════════════
    //  PDF
    // ════════════════════════════════════════════════════════════

    private void handleExportSupplementaryPdf() {
        if (currentNationalId == null || currentNationalId.isBlank()) {
            showError("ابحث عن موظف أولاً");
            return;
        }

        btn_supplementaryPdf.setDisable(true);
        clearError();

        String defaultFileName = "supplementary-" + currentNationalId + ".pdf";

        FxApiSupport.downloadBinary(
                "/entitlements/employee/" + currentNationalId + "/supplementary-bonus/pdf",
                defaultFileName,
                savedPath -> {
                    btn_supplementaryPdf.setDisable(false);
                    showInfo("✅ تم حفظ التقرير: " + savedPath);
                },
                err -> {
                    btn_supplementaryPdf.setDisable(false);
                    showError("خطأ في تصدير التقرير: " + err);
                }
        );
    }

    // ════════════════════════════════════════════════════════════
    //  Search / Load
    // ════════════════════════════════════════════════════════════

    private void handleSearch() {
        String id = txt_nationalId.getText().trim();
        if (id.isBlank()) {
            showError("أدخل الرقم القومي أولاً");
            return;
        }

        if (!id.equals(currentNationalId)) {
            cachedScaleDto = null;
            currentResult = null;
        }
        currentNationalId = id;
        loadAllowances();
    }

    private void loadAllowances() {
        if (currentNationalId == null) return;

        // 🆕 اقرأ التاريخ من الـ TextField
        LocalDate calcDate = getCalculationDate();
        String dateParam = calcDate != null
                ? "?date=" + calcDate.format(DATE_FMT)
                : "";

        showLoading(true);
        clearError();

        FxApiSupport.get(
                "/entitlements/employee/" + currentNationalId + dateParam,
                AllowanceResultDto.class,
                result -> {
                    showLoading(false);
                    currentResult = result;
                    fillAllowancesTable(result);
                    btn_reset.setVisible(true);
                    loadDefinitions();
                },
                err -> {
                    showLoading(false);
                    showError(err);
                }
        );

        ensureEmployeeDetailsLoaded();
    }

    private void loadDefinitions() {
        FxApiSupport.getList(
                "/entitlements/allowances/definitions",
                new TypeReference<List<AllowanceDefinition>>() {
                },
                defs -> {
                    List<String> existing = currentResult != null
                            ? currentResult.allowances().stream()
                            .map(AllowanceResultDto.AllowanceLineDto::code).toList()
                            : List.of();

                    // 🆕 امتل الـ ObservableList بدل الـ ComboBox
                    availableAllowances.setAll(
                            defs.stream()
                                    .filter(d -> !existing.contains(d.getCode()))
                                    .sorted(Comparator.comparing(AllowanceDefinition::getNameAr))
                                    .collect(Collectors.toList())
                    );
                },
                err -> {
                }
        );
    }

    // ════════════════════════════════════════════════════════════
    //  Employee Details
    // ════════════════════════════════════════════════════════════

    private void ensureEmployeeDetailsLoaded() {
        if (cachedScaleDto != null) {
            applyEmployeeDetails(cachedScaleDto);
            return;
        }
        FxApiSupport.get(
                "/salary-scale/" + currentNationalId,
                ScaleDto.class,
                dto -> {
                    cachedScaleDto = dto;
                    applyEmployeeDetails(dto);
                },
                err -> {
                }
        );
    }

    private void applyEmployeeDetails(ScaleDto dto) {
        txt_empName.setText(dto.getEmpName());
        txt_empNationalId.setText(dto.getNationalId());
        txt_empLawCode.setText(dto.getLawCode() != null
                ? dto.getLawCode() : "—");
        txt_empLaw.setText(dto.getLaw() != null ? dto.getLaw().toString() : "—");
        txt_empStartDate.setText(dto.getStartDate() != null
                ? dto.getStartDate().format(DATE_FMT) : "—");
        txt_empBasic30.setText(dto.getBasic30Value() != null
                ? dto.getBasic30Value().toPlainString() : "—");
        txt_empBasic30From.setText(dto.getBasic30From() != null
                ? dto.getBasic30From().format(DATE_FMT) : "—");
        txt_empGroup.setText(dto.getQualitativeGroup() != null
                ? dto.getQualitativeGroup() : "—");
        if (dto.getTimeline() != null && !dto.getTimeline().isEmpty()) {
            txt_empDegree.setText(dto.getTimeline().getLast().getDegreeLabel());
        }
    }

    // ════════════════════════════════════════════════════════════
    //  Fill Table
    // ════════════════════════════════════════════════════════════

    private void fillAllowancesTable(AllowanceResultDto result) {
        List<AllowanceResultDto.AllowanceLineDto> displayLines = new ArrayList<>();

        // 1) الاستحقاقات
        List<AllowanceResultDto.AllowanceLineDto> entitlements = result.allowances().stream()
                .filter(l -> !l.displayOnly())
                .filter(l -> l.elementType() == ElementType.ENTITLEMENT)
                .filter(l -> l.value() != null && l.value().signum() > 0)
                .sorted(Comparator.comparing(AllowanceResultDto.AllowanceLineDto::nameAr))
                .toList();
        displayLines.addAll(entitlements);

        // 2) جملة المستحق
        if (!entitlements.isEmpty()) {
            displayLines.add(buildSubtotal(
                    CODE_SUBTOTAL_ENT,
                    "جملة المستحق",
                    nvl(result.totalEntitlements()),
                    ElementType.ENTITLEMENT));
        }

        // 3) الأوعية التأمينية
        BigDecimal basic = nvl(result.insurableBasic());
        BigDecimal variable = nvl(result.insurableVariable());
        BigDecimal combined = nvl(result.insurableCombined());

        if (basic.signum() > 0) {
            displayLines.add(buildInfo(CODE_INFO_BASIC, "الأجر الأساسي", basic));
        }
        if (variable.signum() > 0) {
            displayLines.add(buildInfo(CODE_INFO_VARIABLE, "الأجر المتغير", variable));
        }
        if (combined.signum() > 0) {
            displayLines.add(buildInfo(CODE_INFO_COMBINED, "الأجر الاشتراكي", combined));
        }

        // 4) الاستقطاعات
        List<AllowanceResultDto.AllowanceLineDto> deductions = result.allowances().stream()
                .filter(l -> !l.displayOnly())
                .filter(l -> l.value() != null && l.value().signum() < 0)
                .filter(l -> l.elementType() == ElementType.DEDUCTION
                        || l.elementType() == ElementType.TAX
                        || l.elementType() == ElementType.STAMP
                        || l.elementType() == ElementType.INSURANCE)
                .sorted(Comparator.comparing(AllowanceResultDto.AllowanceLineDto::nameAr))
                .toList();
        displayLines.addAll(deductions);

        // 5) جملة الاستقطاعات
        if (!deductions.isEmpty()) {
            displayLines.add(buildSubtotal(
                    CODE_SUBTOTAL_DED,
                    "جملة الاستقطاعات",
                    nvl(result.totalDeductions()).negate(),
                    ElementType.DEDUCTION));
        }

        // 6) تأمينات الحكومة (display only)
        List<AllowanceResultDto.AllowanceLineDto> govLines = result.allowances().stream()
                .filter(AllowanceResultDto.AllowanceLineDto::displayOnly)
                .sorted(Comparator.comparing(AllowanceResultDto.AllowanceLineDto::nameAr))
                .toList();
        displayLines.addAll(govLines);

        // 7) الصافي
        displayLines.add(buildNet(CODE_NET, "الصافي", nvl(result.netAmount())));

        table_allowances.setItems(FXCollections.observableArrayList(displayLines));
        lbl_total.setText(nvl(result.netAmount()).toPlainString() + " ج");
    }

    // ── Builders للسطور الوهمية ──

    private AllowanceResultDto.AllowanceLineDto buildSubtotal(
            String code, String nameAr, BigDecimal value, ElementType et) {
        return new AllowanceResultDto.AllowanceLineDto(
                code, nameAr, value,
                null,
                AllowanceResultDto.AllowanceLineDto.Source.AUTO,
                null, et,
                false, false, false,
                true);
    }

    private AllowanceResultDto.AllowanceLineDto buildInfo(
            String code, String nameAr, BigDecimal value) {
        return new AllowanceResultDto.AllowanceLineDto(
                code, nameAr, value,
                null,
                AllowanceResultDto.AllowanceLineDto.Source.AUTO,
                null, ElementType.ENTITLEMENT,
                false, false, false,
                true);
    }

    private AllowanceResultDto.AllowanceLineDto buildNet(
            String code, String nameAr, BigDecimal value) {
        return new AllowanceResultDto.AllowanceLineDto(
                code, nameAr, value,
                null,
                AllowanceResultDto.AllowanceLineDto.Source.AUTO,
                null, ElementType.ENTITLEMENT,
                false, false, false,
                true);
    }

    // ════════════════════════════════════════════════════════════
    //  Setup Table
    // ════════════════════════════════════════════════════════════

    private void setupTable() {
        // اسم البدل
        col_nameAr.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(d.getValue().nameAr()));
        col_nameAr.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String name, boolean empty) {
                super.updateItem(name, empty);
                if (empty || name == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(name);
                AllowanceResultDto.AllowanceLineDto item =
                        getTableView().getItems().get(getIndex());
                setStyle(nameStyle(item));
            }
        });

        // عمود النوع
        col_elementType.setCellValueFactory(d ->
                new javafx.beans.property.SimpleObjectProperty<>(d.getValue().elementType()));
        col_elementType.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(ElementType et, boolean empty) {
                super.updateItem(et, empty);
                if (empty || et == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                AllowanceResultDto.AllowanceLineDto item =
                        getTableView().getItems().get(getIndex());
                if (isVirtualLine(item.code())) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label badge = new Label(elementTypeLabel(et));
                badge.setStyle(elementTypeBadgeStyle(et));
                badge.setPadding(new Insets(2, 8, 2, 8));
                setGraphic(badge);
                setText(null);
                setStyle("-fx-alignment: CENTER;");
            }
        });

        // عمود القيمة
        col_value.setCellValueFactory(d ->
                new javafx.beans.property.SimpleObjectProperty<>(d.getValue().value()));
        col_value.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) {
                    setText(null);
                    setStyle("-fx-alignment: CENTER;");
                    return;
                }
                AllowanceResultDto.AllowanceLineDto line =
                        getTableView().getItems().get(getIndex());

                setText(val.abs().toPlainString() + " ج");
                setStyle("-fx-alignment: CENTER; " + valueStyle(line));
            }
        });

        // من تاريخ
        col_effectiveFrom.setCellValueFactory(d ->
                new javafx.beans.property.SimpleObjectProperty<>(d.getValue().effectiveFrom()));
        col_effectiveFrom.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setText(empty || date == null ? null : date.format(DATE_FMT));
                setStyle("-fx-alignment: CENTER;");
            }
        });

        // المصدر
        col_source.setCellValueFactory(d ->
                new javafx.beans.property.SimpleObjectProperty<>(d.getValue().source()));
        col_source.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(AllowanceResultDto.AllowanceLineDto.Source src, boolean empty) {
                super.updateItem(src, empty);
                if (empty || src == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                AllowanceResultDto.AllowanceLineDto item =
                        getTableView().getItems().get(getIndex());
                if (isVirtualLine(item.code())) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label badge = new Label(sourceLabel(src));
                badge.setStyle(sourceBadgeStyle(src));
                badge.setPadding(new Insets(2, 8, 2, 8));
                setGraphic(badge);
                setText(null);
                setStyle("-fx-alignment: CENTER;");
            }
        });

        // الإجراءات
        col_actions.setCellFactory(tc -> new TableCell<>() {
            private final Button btnEdit = new Button("✏ تعديل");
            private final Button btnDelete = new Button("🗑");

            {
                btnEdit.getStyleClass().add("btn-purple");
                btnEdit.setPrefHeight(26);
                btnEdit.setPrefWidth(75);
                btnDelete.getStyleClass().add("btn-danger");
                btnDelete.setPrefHeight(26);
                btnDelete.setPrefWidth(36);
                btnEdit.setOnAction(e -> openOverrideDialog(
                        getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> handleDeleteAllowance(
                        getTableView().getItems().get(getIndex()).code()));
            }

            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                AllowanceResultDto.AllowanceLineDto item =
                        getTableView().getItems().get(getIndex());

                if (isVirtualLine(item.code())) {
                    setGraphic(null);
                    return;
                }

                boolean excluded = item.source() == AllowanceResultDto.AllowanceLineDto.Source.EXCLUDED;
                boolean displayOnly = item.displayOnly();
                btnEdit.setDisable(excluded || displayOnly);
                btnDelete.setDisable(displayOnly);
                HBox box = new HBox(6, btnEdit, btnDelete);
                box.setStyle("-fx-alignment: CENTER;");
                setGraphic(box);
            }
        });
    }

    // ════════════════════════════════════════════════════════════
    //  Dialogs
    // ════════════════════════════════════════════════════════════

    private void openOverrideDialog(AllowanceResultDto.AllowanceLineDto line) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/safwat/hr/controller/entitlements/allowance/AllowanceOverrideDialog.fxml"
            ));
            Parent root = loader.load();
            AllowanceOverrideDialogController ctrl = loader.getController();
            ctrl.init(line, currentNationalId, () -> loadAllowances());

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("تعديل فترات: " + line.nameAr());
            dialog.setScene(new Scene(root));
            dialog.setResizable(false);
            dialog.showAndWait();
        } catch (Exception ex) {
            showError("خطأ في فتح نافذة التعديل: " + ex.getMessage());
        }
    }

    private void openDefinitionsDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/safwat/hr/controller/entitlements/allowance/AllowanceDefinitionDialog.fxml"
            ));
            Parent root = loader.load();
            AllowanceDefinitionDialogController ctrl = loader.getController();
            ctrl.init(() -> {
                if (currentNationalId != null) loadDefinitions();
            });

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("إدارة قواعد البدلات");
            dialog.setScene(new Scene(root));
            dialog.setResizable(true);
            dialog.showAndWait();
        } catch (Exception ex) {
            showError("خطأ في فتح شاشة إدارة القواعد: " + ex.getMessage());
        }
    }

    private void openStatutoryDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/safwat/hr/controller/entitlements/allowance/StatutoryDialog.fxml"
            ));
            Parent root = loader.load();
            StatutoryDialogController controller = loader.getController();
            controller.init(() -> {
            });

            Stage dialog = new Stage();
            dialog.setTitle("إدارة الاستقطاعات القانونية");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(new Scene(root));
            dialog.showAndWait();
        } catch (IOException ex) {
            showError("خطأ في فتح شاشة الاستقطاعات: " + ex.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════
    //  API Ops
    // ════════════════════════════════════════════════════════════

    private void handleAddAllowance() {
        // 🆕 تحديد البدل من الـ TextField
        AllowanceDefinition selected = resolveSelectedAllowance();
        if (selected == null) {
            String typed = txt_newAllowance.getText();
            if (typed == null || typed.isBlank()) {
                showError("اكتب اسم البدل أو اختره من القائمة");
            } else {
                showError("لم يتم العثور على بدل مطابق: " + typed);
            }
            return;
        }

        // 🆕 تحديد التاريخ من الـ TextField
        LocalDate fromDate = getCalculationDate();
        if (fromDate == null) {
            showError("اكتب تاريخ احتساب صحيح أولاً (سيُستخدم كتاريخ بداية البدل)");
            return;
        }

        AllowanceResultDto.AddAllowanceRequest req = new AllowanceResultDto.AddAllowanceRequest(
                selected.getCode(),
                fromDate,
                null,
                BigDecimal.ZERO
        );

        btn_addAllowance.setDisable(true);
        clearError();

        FxApiSupport.post(
                "/entitlements/allowances/employee/" + currentNationalId + "/allowance",
                req,
                Boolean.class,
                saved -> {
                    btn_addAllowance.setDisable(false);
                    txt_newAllowance.clear();
                    selectedAllowanceCode = null;
                    loadAllowances();
                },
                err -> {
                    btn_addAllowance.setDisable(false);
                    showError("خطأ في الإضافة: " + err);
                }
        );
    }

    private void handleDeleteAllowance(String code) {
        if (isVirtualLine(code)) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "هل تريد حذف البدل من قائمة الموظف؟\nيمكن استرجاعه بعد إعادة الضبط.",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("تأكيد الحذف");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                FxApiSupport.delete(
                        "/entitlements/allowances/employee/" + currentNationalId
                                + "/allowance/" + code,
                        this::loadAllowances,
                        this::showError
                );
            }
        });
    }

    private void handleReset() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "إعادة الضبط ستحذف كل التعديلات اليدوية وتعيد البناء التلقائي.",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("تأكيد إعادة الضبط");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                FxApiSupport.delete(
                        "/entitlements/allowances/employee/" + currentNationalId + "/reset",
                        this::loadAllowances,
                        this::showError
                );
            }
        });
    }

    // ════════════════════════════════════════════════════════════
    //  Helpers
    // ════════════════════════════════════════════════════════════

    private boolean isVirtualLine(String code) {
        if (code == null) return false;
        return code.startsWith("SUBTOTAL_")
                || code.startsWith("INFO_")
                || CODE_NET.equals(code);
    }

    private String nameStyle(AllowanceResultDto.AllowanceLineDto line) {
        String code = line.code();
        if (code == null) return "";

        if (CODE_NET.equals(code)) {
            return "-fx-font-weight:bold; -fx-font-size:14; -fx-text-fill:#1b5e20;";
        }
        if (code.startsWith("SUBTOTAL_")) {
            return "-fx-font-weight:bold; -fx-font-size:13; -fx-text-fill:#000;";
        }
        if (code.startsWith("INFO_")) {
            return "-fx-font-weight:bold; -fx-text-fill:#0d47a1;";
        }
        if (line.displayOnly()) {
            return "-fx-text-fill:#888; -fx-font-style:italic;";
        }
        return "";
    }

    private String valueStyle(AllowanceResultDto.AllowanceLineDto line) {
        String code = line.code();

        if (CODE_NET.equals(code)) {
            return "-fx-text-fill:#1b5e20; -fx-font-weight:bold; -fx-font-size:14;";
        }
        if (code != null && code.startsWith("SUBTOTAL_")) {
            return "-fx-text-fill:#000; -fx-font-weight:bold; -fx-font-size:13;";
        }
        if (code != null && code.startsWith("INFO_")) {
            return "-fx-text-fill:#0d47a1; -fx-font-weight:bold;";
        }
        if (line.displayOnly()) {
            return "-fx-text-fill:#888; -fx-font-style:italic;";
        }
        if (line.value() != null && line.value().signum() < 0) {
            return "-fx-text-fill:#c62828; -fx-font-weight:bold;";
        }
        return "-fx-text-fill:#2e7d32; -fx-font-weight:bold;";
    }

    private String elementTypeLabel(ElementType et) {
        return switch (et) {
            case ENTITLEMENT -> "استحقاق";
            case DEDUCTION -> "استقطاع";
            case INSURANCE -> "تأمينات";
            case TAX -> "ضريبة";
            case STAMP -> "دمغة";
        };
    }

    private String elementTypeBadgeStyle(ElementType et) {
        String base = "-fx-background-radius:4; -fx-font-weight:bold; -fx-text-fill:white; -fx-font-size:11;";
        return base + switch (et) {
            case ENTITLEMENT -> "-fx-background-color:#2e7d32;";
            case DEDUCTION -> "-fx-background-color:#c62828;";
            case INSURANCE -> "-fx-background-color:#1565c0;";
            case TAX -> "-fx-background-color:#6a1b9a;";
            case STAMP -> "-fx-background-color:#e65100;";
        };
    }

    private String sourceLabel(AllowanceResultDto.AllowanceLineDto.Source src) {
        return switch (src) {
            case AUTO -> "تلقائي";
            case MANUAL -> "يدوي";
            case EXCLUDED -> "مستثنى";
        };
    }

    private String sourceBadgeStyle(AllowanceResultDto.AllowanceLineDto.Source src) {
        String base = "-fx-background-radius:4; -fx-font-weight:bold; -fx-text-fill:white; -fx-font-size:11;";
        return base + switch (src) {
            case AUTO -> "-fx-background-color:#607d8b;";
            case MANUAL -> "-fx-background-color:#1976d2;";
            case EXCLUDED -> "-fx-background-color:#e65100;";
        };
    }

    private void showLoading(boolean show) {
        progress_indicator.setVisible(show);
        btn_search.setDisable(show);
    }

    private void showInfo(String msg) {
        lbl_error.setStyle("-fx-text-fill:#2e7d32; -fx-font-weight:bold;");
        lbl_error.setText(msg);
        lbl_error.setVisible(true);

        javafx.animation.PauseTransition pause =
                new javafx.animation.PauseTransition(javafx.util.Duration.seconds(5));
        pause.setOnFinished(e -> clearError());
        pause.play();
    }

    private void showError(String msg) {
        lbl_error.setStyle("-fx-text-fill:#c62828; -fx-font-weight:bold;");
        lbl_error.setText(msg);
        lbl_error.setVisible(true);
    }

    private void clearError() {
        lbl_error.setText("");
        lbl_error.setVisible(false);
        lbl_error.setStyle(null);
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    /**
     * يُستدعى من TabManager بعد ما التاب يفتح — لتمرير الرقم القومي
     * من أي تاب تاني.
     */
    public void setInitialNationalId(String nationalId) {
        if (nationalId == null || nationalId.isBlank()) return;
        if (txt_nationalId != null) {
            txt_nationalId.setText(nationalId.trim());
        }
        handleSearch();
    }

    private void openSectorsDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/safwat/hr/controller/entitlements/allowance/SectorJobTitleDialog.fxml"
            ));
            Parent root = loader.load();

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("إدارة القطاعات والوظائف");
            dialog.setScene(new Scene(root));
            dialog.setResizable(true);
            dialog.showAndWait();


        } catch (Exception ex) {
            showError("خطأ في فتح شاشة القطاعات: " + ex.getMessage());
        }
    }
}