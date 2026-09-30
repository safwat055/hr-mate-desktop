package com.safwat.hr.controller.entitlements.allowance;

import com.fasterxml.jackson.core.type.TypeReference;
import com.safwat.hr.controller.employee.dto.EmployeeProfileDto;
import com.safwat.hr.controller.employee.dto.JobTitleEntryRequest;
import com.safwat.hr.controller.employee.dto.SocialStatusEntryRequest;
import com.safwat.hr.controller.employee.ui.EmployeeHistoryTable;
import com.safwat.hr.controller.employee.ui.EmployeeRows.JobTitleRow;
import com.safwat.hr.controller.employee.ui.EmployeeRows.SocialStatusRow;
import com.safwat.hr.controller.employee.ui.JobTitleOption;
import com.safwat.hr.controller.entitlements.allowance.AllowanceDefinition.ElementType;
import com.safwat.hr.controller.entitlements.statutory.StatutoryDialogController;
import com.safwat.hr.controller.scale.scale.dto.ScaleDto;
import com.safwat.hr.controller.wages.ui.EmployeeWagesScreenController;
import com.safwat.hr.network.ApiClient;
import com.safwat.hr.shared.FXMLPaths;
import com.safwat.hr.ui.TextFieldSetupHelper;
import com.safwat.hr.ui.util.TabManager;
import com.safwat.hr.ui.util.ViewManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import static com.safwat.hr.network.DownloadWithNotification.downloadPdfToTempAndNotify;

public class AllowanceFxController implements Initializable {

    // ── شريط البحث ──────────────────────────────────────────────
    @FXML
    private Button btn_supplementaryPdf;
    @FXML
    private Button btn_statutory, btn_openWage;
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
    private TextField txt_calculationDate;
    @FXML
    private Button btn_recalculate, btn_exportPDF;
    @FXML
    private TextField txt_newAllowance;
    @FXML
    private Button btn_showAllowances;
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

    // ── جداول الحالة الاجتماعية والوظائف (جديد) ─────────────────
    @FXML
    private HBox historyRow;
    @FXML
    private VBox socialStatusContainer;
    @FXML
    private VBox jobTitleContainer;

    // ── State ────────────────────────────────────────────────────
    private String currentNationalId;
    private AllowanceResultDto currentResult;
    private ScaleDto cachedScaleDto;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ★ جديد — id الموظف + الجداول القابلة للتعديل
    private Long employeeId;
    private EmployeeHistoryTable<SocialStatusRow> socialTbl;
    private EmployeeHistoryTable<JobTitleRow> jobTbl;

    /**
     * البدلات المتاحة للإضافة — تُملأ من loadDefinitions.
     */
    private final ObservableList<AllowanceDefinition> availableAllowances =
            FXCollections.observableArrayList();

    /**
     * قائمة الاقتراحات المنسدلة.
     */
    private final ContextMenu suggestionsMenu = new ContextMenu();

    /**
     * كود البدل اللي المستخدم اختاره من الاقتراحات.
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
        btn_manageSectors.setOnAction(_ -> openSectorsDialog());
        txt_calculationDate.setText(LocalDate.now().format(DATE_FMT));

        btn_statutory.setOnAction(_ -> openStatutoryDialog());
        txt_nationalId.setOnAction(_ -> handleSearch());
        btn_search.setOnAction(_ -> handleSearch());
        btn_recalculate.setOnAction(_ -> loadAllowances());
        btn_addAllowance.setOnAction(_ -> handleAddAllowance());
        btn_showAllowances.setOnAction(_ -> showAllAllowancesMenu());
        btn_reset.setOnAction(_ -> handleReset());
        btn_supplementaryPdf.setOnAction(_ -> handleExportSupplementaryPdf());
        if (btn_manageDefinitions != null) {
            btn_manageDefinitions.setOnAction(_ -> openDefinitionsDialog());
        }
        loadDefinitions();
        btn_exportPDF.setOnAction(_ -> handleExportPayslipPdf());   // ⭐ جديد
        // في البداية جداول الـ history مخفية
        setHistoryVisible(false);
        btn_openWage.setOnAction(_ -> openWageView());
    }


    /**
     * ════════════════════════════════════════════════════════════════
     * تصدير PDF لمفردات المرتب
     * ════════════════════════════════════════════════════════════════
     *
     * <p>الشروط:</p>
     * <ul>
     *   <li>لازم يكون فيه رقم قومي (إما بُحث عنه أو مكتوب في الحقل).</li>
     *   <li>لازم يكون فيه تاريخ احتساب في {@code txt_calculationDate}.</li>
     * </ul>
     */
    private void handleExportPayslipPdf() {

        // ── 1. التحقق من الرقم القومي ──
        String nationalId = currentNationalId;
        if (nationalId == null || nationalId.isBlank()) {
            nationalId = txt_nationalId.getText() != null
                    ? txt_nationalId.getText().trim() : "";
        }
        if (nationalId.isBlank()) {
            showError("أدخل الرقم القومي أولاً");
            return;
        }

        // ── 2. التحقق من التاريخ ──
        LocalDate calcDate = getCalculationDate();
        if (calcDate == null) {
            showError("أدخل تاريخ احتساب صحيح أولاً");
            return;
        }

        // ── 3. اسم الموظف (للعرض في الإشعار) ──
        String empName = (txt_empName != null && txt_empName.getText() != null)
                ? txt_empName.getText().trim()
                : nationalId;

        // ── 4. تعطيل الزر ──
        btn_exportPDF.setDisable(true);
        clearError();

        // ── 5. بناء الـ URL ──
        String path = "/entitlements/employee/" + nationalId
                + "/payslip/pdf?date=" + calcDate.format(DATE_FMT);

        // ── 6. التنزيل + الإشعار ──
        downloadPdfToTempAndNotify(
                path,
                "PAYSLIP_" + nationalId,
                empName + " — " + calcDate.format(DATE_FMT),
                "مفردات المرتب",
                () -> btn_exportPDF.setDisable(false)
        );
    }
    // ════════════════════════════════════════════════════════════
    //  Date Field — TextField + TextFieldSetupHelper
    // ════════════════════════════════════════════════════════════

    private void setupDateField() {
        TextFieldSetupHelper.setupDateFields(txt_calculationDate);
    }

    private LocalDate getCalculationDate() {
        return TextFieldSetupHelper.parseDateInput(txt_calculationDate.getText());
    }

    // ════════════════════════════════════════════════════════════
    //  Allowance Autocomplete
    // ════════════════════════════════════════════════════════════

    private void setupAllowanceAutocomplete() {
        txt_newAllowance.textProperty().addListener((_, _, newVal) -> {
            selectedAllowanceCode = null;
            if (newVal == null || newVal.isBlank()) {
                suggestionsMenu.hide();
                return;
            }
            String q = newVal.trim().toLowerCase();

            List<AllowanceDefinition> matched = availableAllowances.stream()
                    .filter(a -> {
                        boolean nameMatch = a.getNameAr() != null
                                && a.getNameAr().toLowerCase().contains(q);
                        boolean codeMatch = a.getCode() != null
                                && a.getCode().toLowerCase().contains(q);
                        return nameMatch || codeMatch;
                    })
                    .limit(12)
                    .toList();

            if (matched.isEmpty()) {
                suggestionsMenu.hide();
                return;
            }

            suggestionsMenu.getItems().clear();
            for (AllowanceDefinition a : matched) {
                MenuItem item = new MenuItem(a.getNameAr());
                item.setOnAction(_ -> {
                    txt_newAllowance.setText(a.getNameAr());
                    selectedAllowanceCode = a.getCode();
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

        txt_newAllowance.focusedProperty().addListener((obs, was, isNow) -> {
            if (!isNow) {
                Platform.runLater(suggestionsMenu::hide);
            }
        });

        txt_newAllowance.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case ESCAPE -> suggestionsMenu.hide();
                case DOWN -> {
                    if (!suggestionsMenu.getItems().isEmpty()) {
                        suggestionsMenu.getItems().getFirst().fire();
                    }
                }
                default -> { /* no-op */ }
            }
        });
    }

    private void showAllAllowancesMenu() {
        if (availableAllowances.isEmpty()) {
            showError("لا توجد بدلات متاحة للإضافة");
            return;
        }

        suggestionsMenu.getItems().clear();
        for (AllowanceDefinition a : availableAllowances) {
            MenuItem item = new MenuItem(a.getNameAr());
            item.setOnAction(_ -> {
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

    private AllowanceDefinition resolveSelectedAllowance() {
        String typed = txt_newAllowance.getText();
        if (typed == null || typed.isBlank()) return null;
        typed = typed.trim();

        if (selectedAllowanceCode != null) {
            final String code = selectedAllowanceCode;
            AllowanceDefinition byCode = availableAllowances.stream()
                    .filter(a -> code.equals(a.getCode()))
                    .findFirst()
                    .orElse(null);
            if (byCode != null) return byCode;
        }

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

        // ── اسم الموظف (للعرض في الإشعار) ──
        String empName = (txt_empName != null && txt_empName.getText() != null)
                ? txt_empName.getText().trim()
                : currentNationalId;

        btn_supplementaryPdf.setDisable(true);
        clearError();

        String path = "/entitlements/employee/" + currentNationalId
                + "/supplementary-bonus/pdf";

        downloadPdfToTempAndNotify(
                path,
                "SUPPLEMENTARY_" + currentNationalId,
                empName,
                "الحافز التكميلي",
                () -> btn_supplementaryPdf.setDisable(false)
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
            // ★ مسح بيانات الموظف القديم
            employeeId = null;
            socialTbl = null;
            jobTbl = null;
            socialStatusContainer.getChildren().clear();
            jobTitleContainer.getChildren().clear();
            setHistoryVisible(false);
        }
        currentNationalId = id;
        loadAllowances();
    }

    private void loadAllowances() {
        if (currentNationalId == null) return;

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
            loadEmployeeProfile();      // ★
            return;
        }
        FxApiSupport.get(
                "/salary-scale/" + currentNationalId,
                ScaleDto.class,
                dto -> {
                    cachedScaleDto = dto;
                    applyEmployeeDetails(dto);
                    loadEmployeeProfile();  // ★
                },
                err -> {
                }
        );
    }

    private void applyEmployeeDetails(ScaleDto dto) {
        txt_empName.setText(dto.getEmpName());
        txt_empNationalId.setText(dto.getNationalId());
        txt_empLawCode.setText(dto.getJobTitleHistory() != null
                && !dto.getJobTitleHistory().isEmpty()
                ? dto.getJobTitleHistory().lastEntry().getValue() : "—");
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
    //  ★ جداول الحالة الاجتماعية والوظائف
    // ════════════════════════════════════════════════════════════

    private void loadEmployeeProfile() {
        if (currentNationalId == null || currentNationalId.isBlank()) return;

        FxApiSupport.get(
                "/employees/by-national-id/" + currentNationalId,
                EmployeeProfileDto.class,
                profile -> {
                    employeeId = profile.id();
                    rebuildHistoryTables(profile);
                    setHistoryVisible(true);
                },
                err -> {
                    // الموظف مش موجود في employee — نخفي الجداول
                    setHistoryVisible(false);
                }
        );
    }

    private void rebuildHistoryTables(EmployeeProfileDto profile) {

        // ── ① الحالة الاجتماعية ──
        List<SocialStatusRow> statusRows =
                profile.socialStatusHistory() == null ? List.of() :
                        profile.socialStatusHistory().stream()
                                .map(SocialStatusRow::from)
                                .toList();

        socialTbl = new EmployeeHistoryTable<>(
                "الحالة الاجتماعية",
                SocialStatusRow::new,
                SocialStatusRow.columns(),
                statusRows,
                this::saveSocialStatus
        );
        socialStatusContainer.getChildren().setAll(socialTbl.build());

        // ── ② الوظائف — محتاجين قائمة وظائف القطاع ──
        List<JobTitleOption> sectorJobTitles = List.of();
        if (profile.sectorId() != null) {
            sectorJobTitles = loadJobTitlesForSector(profile.sectorId());
        }

        List<JobTitleRow> jobRows =
                profile.jobTitleHistory() == null ? List.of() :
                        profile.jobTitleHistory().stream()
                                .map(JobTitleRow::from)
                                .toList();

        jobTbl = new EmployeeHistoryTable<>(
                "الوظائف",
                JobTitleRow::new,
                JobTitleRow.columns(sectorJobTitles),
                jobRows,
                this::saveJobTitles
        );
        jobTitleContainer.getChildren().setAll(jobTbl.build());
    }

    private List<JobTitleOption> loadJobTitlesForSector(Long sectorId) {
        try {
            JobTitleOption[] opts = ApiClient.get(
                    "/job-titles?sectorId=" + sectorId + "&active=true",
                    JobTitleOption[].class).getData();
            return opts == null ? List.of() : List.of(opts);
        } catch (Exception ex) {
            return List.of();
        }
    }

    private void setHistoryVisible(boolean visible) {
        if (historyRow == null) return;
        historyRow.setVisible(visible);
        historyRow.setManaged(visible);
    }

    // ════════════════════════════════════════════════════════════
    //  ★ حفظ الحالة الاجتماعية
    // ════════════════════════════════════════════════════════════

    private void saveSocialStatus(List<SocialStatusRow> rows) throws Exception {
        if (employeeId == null) {
            throw new IllegalStateException("الموظف غير محمّل — أعد البحث أولاً");
        }

        List<SocialStatusEntryRequest> requests = rows.stream()
                .map(SocialStatusRow::toRequest)
                .toList();

        EmployeeProfileDto updated = ApiClient.put(
                "/employees/" + employeeId + "/social-status/bulk",
                requests,
                EmployeeProfileDto.class
        ).getData();

        if (socialTbl != null && updated.socialStatusHistory() != null) {
            socialTbl.reload(
                    updated.socialStatusHistory().stream()
                            .map(SocialStatusRow::from)
                            .toList());
        }

        // invalidate cache عشان البيانات الحالية تتحمّل من جديد
        cachedScaleDto = null;
    }

    // ════════════════════════════════════════════════════════════
    //  ★ حفظ الوظائف
    // ════════════════════════════════════════════════════════════

    private void saveJobTitles(List<JobTitleRow> rows) throws Exception {
        if (employeeId == null) {
            throw new IllegalStateException("الموظف غير محمّل — أعد البحث أولاً");
        }

        List<JobTitleEntryRequest> requests = rows.stream()
                .map(JobTitleRow::toRequest)
                .toList();

        EmployeeProfileDto updated = ApiClient.put(
                "/employees/" + employeeId + "/job-title/bulk",
                requests,
                EmployeeProfileDto.class
        ).getData();

        if (jobTbl != null && updated.jobTitleHistory() != null) {
            jobTbl.reload(
                    updated.jobTitleHistory().stream()
                            .map(JobTitleRow::from)
                            .toList());
        }

        cachedScaleDto = null;
    }

    // ════════════════════════════════════════════════════════════
    //  Fill Table
    // ════════════════════════════════════════════════════════════

    private void fillAllowancesTable(AllowanceResultDto result) {
        List<AllowanceResultDto.AllowanceLineDto> displayLines = new ArrayList<>();

        // ⭐ (1) بنود الاستحقاق — كلها بدون فلتر قيمة
        List<AllowanceResultDto.AllowanceLineDto> entitlements = result.allowances().stream()
                .filter(l -> !l.displayOnly())
                .filter(l -> l.elementType() == ElementType.ENTITLEMENT)
                .sorted(Comparator.comparing(AllowanceResultDto.AllowanceLineDto::nameAr))
                .toList();
        displayLines.addAll(entitlements);

        // (2) جملة المستحق
        if (!entitlements.isEmpty()) {
            displayLines.add(buildSubtotal(
                    CODE_SUBTOTAL_ENT,
                    "جملة المستحق",
                    nvl(result.totalEntitlements()),
                    ElementType.ENTITLEMENT));
        }

        // (3) الأوعية التأمينية
        BigDecimal basic = nvl(result.insurableBasic());
        BigDecimal variable = nvl(result.insurableVariable());
        BigDecimal combined = nvl(result.insurableCombined());

        if (basic.signum() > 0) {
            displayLines.add(buildInfo(CODE_INFO_BASIC, "الاجر الاساسى", basic));
        }
        if (variable.signum() > 0) {
            displayLines.add(buildInfo(CODE_INFO_VARIABLE, "الاجر المتغير", variable));
        }
        if (combined.signum() > 0) {
            displayLines.add(buildInfo(CODE_INFO_COMBINED, "اجر الاشتراك", combined));
        }

        // ⭐ (4) بنود الاستقطاع — كلها بدون فلتر قيمة
        List<AllowanceResultDto.AllowanceLineDto> deductions = result.allowances().stream()
                .filter(l -> !l.displayOnly())
                .filter(l -> l.elementType() == ElementType.DEDUCTION
                        || l.elementType() == ElementType.TAX
                        || l.elementType() == ElementType.STAMP
                        || l.elementType() == ElementType.INSURANCE)
                .sorted(Comparator.comparing(AllowanceResultDto.AllowanceLineDto::nameAr))
                .toList();
        displayLines.addAll(deductions);

        // (5) جملة الاستقطاعات
        if (!deductions.isEmpty()) {
            displayLines.add(buildSubtotal(
                    CODE_SUBTOTAL_DED,
                    "جملة الاستقطاعات",
                    nvl(result.totalDeductions()).negate(),
                    ElementType.DEDUCTION));
        }

        // (6) تأمينات الحكومة (display only)
        List<AllowanceResultDto.AllowanceLineDto> govLines = result.allowances().stream()
                .filter(AllowanceResultDto.AllowanceLineDto::displayOnly)
                .sorted(Comparator.comparing(AllowanceResultDto.AllowanceLineDto::nameAr))
                .toList();
        displayLines.addAll(govLines);

        // (7) الصافي
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

        // ★ عمود النوع — نص عادي بدون badge
        col_elementType.setCellValueFactory(d ->
                new javafx.beans.property.SimpleObjectProperty<>(d.getValue().elementType()));
        col_elementType.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(ElementType et, boolean empty) {
                super.updateItem(et, empty);
                if (empty || et == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                AllowanceResultDto.AllowanceLineDto item =
                        getTableView().getItems().get(getIndex());
                if (isVirtualLine(item.code())) {
                    setText(null);
                    setStyle("-fx-alignment: CENTER;");
                    return;
                }
                setText(elementTypeLabel(et));
                setStyle("-fx-alignment: CENTER;");
            }
        });

        // ★ عمود القيمة — بدون تنسيق ألوان
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
                setText(val.abs().toPlainString());
                setStyle("-fx-alignment: CENTER;");
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
    //  API Ops
    // ════════════════════════════════════════════════════════════

    private void handleAddAllowance() {

        // ⭐ (1) تحقق من الرقم القومي
        if (currentNationalId == null || currentNationalId.isBlank()) {
            showError("ابحث عن موظف أولاً قبل إضافة أي بدل");
            return;
        }

        // ⭐ (2) تحقق من البيانات المحمّلة
        if (currentResult == null) {
            showError("لم يتم تحميل بيانات الموظف بعد — اضغط إعادة الحساب");
            return;
        }

        // ⭐ (3) تحقق من البدل المختار
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

        // ⭐ (4) تحقق من التاريخ
        LocalDate fromDate = getCalculationDate();
        if (fromDate == null) {
            showError("اكتب تاريخ احتساب صحيح أولاً (سيُستخدم كتاريخ بداية البدل)");
            return;
        }

        // ⭐ (5) إرسال الطلب
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
                    showError("خطأ في الإضافة: " + err);   // ← تأكد إن دي بتظهر
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
        pause.setOnFinished(_ -> clearError());
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
    // ════════════════════════════════════════════════════════════
    //  Dialogs
    // ════════════════════════════════════════════════════════════

    // ════════════════════════════════════════════════════════════
//  Dialogs — عبر ViewManager
// ════════════════════════════════════════════════════════════

    /**
     * Dialog تعديل فترات البدل — Modal + Resizable = false.
     * <p>الـ viewId ثابت لتسجيل الخطوط/الزوم/الألوان، بينما العنوان
     * الديناميكي يُضبط داخل الـ callback.</p>
     */
    private void openOverrideDialog(AllowanceResultDto.AllowanceLineDto line) {
        ViewManager.openIndependentView(
                "/com/safwat/hr/controller/entitlements/allowance/AllowanceOverrideDialog.fxml",
                "تعديل فترات البدل",             // viewId (ثابت)
                null,                            // owner
                Modality.APPLICATION_MODAL,
                false,                           // resizable
                (ctrl, stage) -> {
                    stage.setTitle("تعديل فترات: " + line.nameAr());   // عنوان ديناميكي
                    ((AllowanceOverrideDialogController) ctrl)
                            .init(line, currentNationalId, () -> loadAllowances());
                }
        );
    }

    /**
     * Dialog إدارة قواعد البدلات — Modal + Resizable = true.
     */
    private void openDefinitionsDialog() {
        ViewManager.openIndependentView(
                "/com/safwat/hr/controller/entitlements/allowance/AllowanceDefinitionDialog.fxml",
                "إدارة قواعد البدلات",
                null,
                Modality.APPLICATION_MODAL,
                true,                            // resizable
                (ctrl, stage) -> ((AllowanceDefinitionDialogController) ctrl)
                        .init(() -> {
                            if (currentNationalId != null) loadDefinitions();
                        })
        );
    }

    private void openWageView() {
        String nationalId = txt_nationalId.getText();
        if (nationalId.isEmpty()) {
            return;
        }
        TabManager.loadFXMLInMainTab(new FXMLPaths().getWagesView(), "سجل الاجور", true,
                controller -> {
                    if (controller instanceof EmployeeWagesScreenController c) {
                        c.setInitialNationalId(nationalId);

                    }
                });
    }

    /**
     * Dialog إدارة الاستقطاعات القانونية — Modal + Resizable = false.
     */
    private void openStatutoryDialog() {
        ViewManager.openIndependentView(
                "/com/safwat/hr/controller/entitlements/allowance/StatutoryDialog.fxml",
                "إدارة الاستقطاعات القانونية",
                null,
                Modality.APPLICATION_MODAL,
                true,
                (ctrl, stage) -> ((StatutoryDialogController) ctrl).init(() -> {
                })
        );
    }

    /**
     * Dialog إدارة القطاعات والوظائف — Modal + Resizable = true.
     * <p>المسار ديناميك من {@link FXMLPaths}.</p>
     */
    private void openSectorsDialog() {
        ViewManager.openIndependentView(
                new FXMLPaths().getSectorView(),
                "إدارة القطاعات والوظائف",
                null,
                Modality.APPLICATION_MODAL,
                true,
                null
        );
    }
}