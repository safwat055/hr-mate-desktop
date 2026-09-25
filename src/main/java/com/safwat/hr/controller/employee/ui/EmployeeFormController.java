package com.safwat.hr.controller.employee.ui;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.safwat.hr.controller.employee.dto.*;
import com.safwat.hr.controller.employee.enums.TerminationReason;
import com.safwat.hr.controller.employee.ui.EmployeeRows.*;
import com.safwat.hr.network.ApiClient;
import com.safwat.hr.shared.ui.SearchDialog;
import com.safwat.hr.ui.TextFieldSetupHelper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.List;

public class EmployeeFormController {

    // ─────────────────────────────────────────────────────────────
    //  FXML — شريط البحث
    // ─────────────────────────────────────────────────────────────

    @FXML private Button    newEmployeeButton;
    @FXML private TextField searchField;
    @FXML private Button    searchButton;

    // ─────────────────────────────────────────────────────────────
    //  FXML — البيانات الأساسية
    // ─────────────────────────────────────────────────────────────

    @FXML private TextField employeeNumberField;
    @FXML private TextField fullNameField;
    @FXML private TextField nationalIdField;
    @FXML private TextField hireDateField;
    @FXML private TextField terminationDateField;
    @FXML private ComboBox<TerminationReason> terminationReasonCombo;
    @FXML private ComboBox<SectorOption>      sectorCombo;
    @FXML private Label    currentSocialStatusLabel;
    @FXML private Label    currentJobTitleLabel;
    @FXML private Button   saveBasicButton;
    @FXML private Button   deleteButton;

    // ─────────────────────────────────────────────────────────────
    //  FXML — containers (٣ + ٤ في HBox، ٥ + ٦ + ٧ في HBox)
    // ─────────────────────────────────────────────────────────────

    @FXML private HBox      historyRow1;
    @FXML private Separator historyRow1Separator;
    @FXML private HBox      historyRow2;

    @FXML private VBox socialStatusContainer;
    @FXML private VBox jobTitleContainer;
    @FXML private VBox promotionsContainer;
    @FXML private VBox encouragementsContainer;
    @FXML private VBox incentivesContainer;

    // ─────────────────────────────────────────────────────────────
    //  State
    // ─────────────────────────────────────────────────────────────

    private Long               employeeId;
    private EmployeeProfileDto current;
    private EmployeeHistoryTable<SocialStatusRow> socialTbl;
    private EmployeeHistoryTable<JobTitleRow>     jobTbl;

    // ─────────────────────────────────────────────────────────────
    //  Initialize
    // ─────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        terminationReasonCombo.getItems().setAll(TerminationReason.values());
        terminationReasonCombo.setCellFactory(cb -> reasonCell());
        terminationReasonCombo.setButtonCell(reasonCell());

        // حقول التاريخ — TextField مع TextFieldSetupHelper
        TextFieldSetupHelper.setupDateFields(
                "yyyy-MM-dd أو dd/MM/yyyy",
                hireDateField, terminationDateField);

        saveBasicButton.setOnAction(e -> onSaveBasic());
        deleteButton.setOnAction(e -> onDelete());
        newEmployeeButton.setOnAction(e -> onNewEmployee());

        searchField.setOnAction(e -> onSearch());
        searchButton.setOnAction(e -> onSearch());

        loadSectors();

        setFormEnabled(false);
        setHistorySectionsVisible(false);
    }

    // ─────────────────────────────────────────────────────────────
    //  تحميل القطاعات
    // ─────────────────────────────────────────────────────────────

    private void loadSectors() {
        try {
            SectorOption[] sectors = ApiClient.get(
                    "/sectors", SectorOption[].class).getData();
            sectorCombo.getItems().clear();
            if (sectors != null) sectorCombo.getItems().addAll(sectors);
        } catch (Exception ex) {
            showError("تعذر تحميل القطاعات", ex);
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  البحث
    // ─────────────────────────────────────────────────────────────

    private void onSearch() {
        String q = searchField.getText().trim();
        if (q.isBlank()) {
            showWarn("اكتب جزء من الاسم أو الرقم القومي أو رقم الموظف");
            return;
        }

        try {
            List<EmployeeSearchResult> results = ApiClient.getWithTypeRef(
                    "/employees/search?q=" + q,
                    new TypeReference<List<EmployeeSearchResult>>() {}
            ).getData();

            if (results == null || results.isEmpty()) {
                showWarn("لا توجد نتائج لـ \"" + q + "\"");
                return;
            }

            if (results.size() == 1) {
                loadEmployee(results.get(0).id());
                return;
            }

            Stage owner = (Stage) searchField.getScene().getWindow();
            SearchDialog.builder(EmployeeSearchResult.class)
                    .title("نتائج البحث")
                    .column("رقم الموظف",  EmployeeSearchResult::employeeNumber)
                    .column("الاسم",       EmployeeSearchResult::fullName)
                    .column("الرقم القومي", EmployeeSearchResult::nationalId)
                    .data(results)
                    .owner(owner)
                    .show()
                    .ifPresent(sel -> loadEmployee(sel.id()));

        } catch (Exception ex) {
            ex.printStackTrace();
            showError("فشل البحث", ex);
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  تحميل / موظف جديد
    // ─────────────────────────────────────────────────────────────

    public void loadEmployee(Long id) {
        this.employeeId = id;
        try {
            current = ApiClient.get("/employees/" + id, EmployeeProfileDto.class).getData();
            bindAll(current);
            setFormEnabled(true);
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("تعذر تحميل بيانات الموظف", ex);
        }
    }

    public void newEmployee() {
        this.employeeId = null;
        this.current    = null;
        clearBasicForm();
        rebuildTables(null);
        setFormEnabled(true);
        deleteButton.setDisable(true);
        setHistorySectionsVisible(false);
        Platform.runLater(employeeNumberField::requestFocus);
    }

    private void onNewEmployee() {
        if (employeeId != null) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "فيه موظف مفتوح حالياً — متأكد إنك عايز تبدأ موظف جديد؟",
                    ButtonType.YES, ButtonType.NO);
            if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        }
        newEmployee();
    }

    // ─────────────────────────────────────────────────────────────
    //  Bind
    // ─────────────────────────────────────────────────────────────

    private void bindAll(EmployeeProfileDto dto) {
        employeeNumberField.setText(dto.employeeNumber());
        employeeNumberField.setDisable(true);
        fullNameField.setText(dto.fullName());
        nationalIdField.setText(dto.nationalId());

        hireDateField.setText(TextFieldSetupHelper.formatDateOutput(dto.hireDate()));
        terminationDateField.setText(TextFieldSetupHelper.formatDateOutput(dto.terminationDate()));

        terminationReasonCombo.setValue(dto.terminationReason());

        // ★ القطاع:
        //  - لو معروف → اختاره + اقفله (مايتغيرش)
        //  - لو null (موظف مستورد) → افتحه عشان المستخدم يعيّن قطاع
        if (dto.sectorId() != null) {
            sectorCombo.getItems().stream()
                    .filter(s -> s.id().equals(dto.sectorId()))
                    .findFirst()
                    .ifPresent(sectorCombo::setValue);
            sectorCombo.setDisable(true);
        } else {
            sectorCombo.setValue(null);
            sectorCombo.setDisable(false);
        }

        currentSocialStatusLabel.setText(
                dto.currentSocialStatusLabelAr() != null ? dto.currentSocialStatusLabelAr() : "—");
        currentJobTitleLabel.setText(
                dto.currentJobTitleNameAr() != null ? dto.currentJobTitleNameAr() : "—");

        rebuildTables(dto);
        setHistorySectionsVisible(true);
    }

    // ─────────────────────────────────────────────────────────────
    //  بناء الجداول
    // ─────────────────────────────────────────────────────────────

    private void rebuildTables(EmployeeProfileDto dto) {
        // ── ③ الحالة الاجتماعية — ComboBox ──
        List<SocialStatusRow> statusRows = dto == null ? List.of() :
                dto.socialStatusHistory().stream().map(SocialStatusRow::from).toList();

        socialTbl = new EmployeeHistoryTable<>(
                "الحالة الاجتماعية",
                SocialStatusRow::new,
                SocialStatusRow.columns(),
                statusRows,
                rows -> saveSocialStatus(rows)
        );
        socialStatusContainer.getChildren().setAll(socialTbl.build());

        // ── ④ الوظائف — ComboBox (لازم نحمّل وظائف القطاع الأول) ──
        List<JobTitleOption> sectorJobTitles = List.of();
        if (dto != null && dto.sectorId() != null) {
            sectorJobTitles = loadJobTitlesForSector(dto.sectorId());
        }

        List<JobTitleRow> jobRows = dto == null ? List.of() :
                dto.jobTitleHistory().stream().map(JobTitleRow::from).toList();

        jobTbl = new EmployeeHistoryTable<>(
                "الوظائف",
                JobTitleRow::new,
                JobTitleRow.columns(sectorJobTitles),
                jobRows,
                rows -> saveJobTitles(rows)
        );
        jobTitleContainer.getChildren().setAll(jobTbl.build());

        // ── ⑤ الترقيات ──
        buildReadOnlyTable(
                promotionsContainer, "الترقيات",
                dto == null ? List.of() :
                        (dto.upgradeRecords() == null ? List.of() :
                                dto.upgradeRecords().stream().map(PromotionRow::from).toList()),
                PromotionRow::new, PromotionRow.columns()
        );

        // ── ⑥ التشجيعيات ──
        buildReadOnlyTable(
                encouragementsContainer, "التشجيعيات",
                dto == null ? List.of() :
                        (dto.encouragementRecords() == null ? List.of() :
                                dto.encouragementRecords().stream().map(EncouragementRow::from).toList()),
                EncouragementRow::new, EncouragementRow.columns()
        );

        // ── ⑦ حوافز الترقية ──
        buildReadOnlyTable(
                incentivesContainer, "حوافز الترقية",
                dto == null ? List.of() :
                        (dto.promotionIncentiveRecords() == null ? List.of() :
                                dto.promotionIncentiveRecords().stream().map(PromotionIncentiveRow::from).toList()),
                PromotionIncentiveRow::new, PromotionIncentiveRow.columns()
        );
    }

    private List<JobTitleOption> loadJobTitlesForSector(Long sectorId) {
        try {
            JobTitleOption[] opts = ApiClient.get(
                    "/job-titles?sectorId=" + sectorId + "&active=true",
                    JobTitleOption[].class).getData();
            return opts == null ? List.of() : List.of(opts);
        } catch (Exception ex) {
            showError("تعذر تحميل وظائف القطاع", ex);
            return List.of();
        }
    }

    private <R> void buildReadOnlyTable(
            VBox container,
            String sectionTitle,
            List<R> rows,
            java.util.function.Supplier<R> rowFactory,
            List<com.safwat.hr.ui.table.TableSetupHelper.ColumnConfig<R>> columns) {

        Label title = new Label(sectionTitle);
        title.getStyleClass().add("section-title");

        TableView<R> table = new TableView<>();
        table.setPrefHeight(160);
        com.safwat.hr.ui.table.TableSetupHelper.setupGenericTable(table, columns, 0, rowFactory);
        table.getItems().setAll(rows);

        VBox box = new VBox(6, title, table);
        box.setPadding(new javafx.geometry.Insets(10, 0, 10, 0));
        container.getChildren().setAll(box);
    }

    // ─────────────────────────────────────────────────────────────
    //  ② حفظ البيانات الأساسية
    // ─────────────────────────────────────────────────────────────

    private void onSaveBasic() {
        saveBasicButton.setDisable(true);
        try {
            if (employeeId == null) {
                String empNum = employeeNumberField.getText().trim();
                String fullNm = fullNameField.getText().trim();
                String natId  = nationalIdField.getText().trim();
                LocalDate hire = TextFieldSetupHelper.parseDateInput(hireDateField.getText());
                SectorOption sector = sectorCombo.getValue();

                if (empNum.isBlank())          { showWarn("رقم الموظف مطلوب"); return; }
                if (fullNm.isBlank())          { showWarn("الاسم مطلوب"); return; }
                if (!natId.matches("\\d{14}")) { showWarn("الرقم القومي لازم 14 رقم"); return; }
                if (hire == null)              { showWarn("تاريخ التعيين مطلوب أو صيغته غير صحيحة"); return; }
                if (sector == null)            { showWarn("اختر القطاع"); return; }

                EmployeeCreateRequest req = new EmployeeCreateRequest(
                        empNum, fullNm, natId, hire, sector.id());

                current    = ApiClient.post("/employees", req, EmployeeProfileDto.class).getData();
                employeeId = current.id();

                bindAll(current);
                showInfo("تم إنشاء الموظف بنجاح — تقدر دلوقتي تضيف سجلات الحالة والوظائف");
            } else {
                // ★ نمرر القطاع لو المستخدم عيّنه (مثلاً موظف مستورد كان sectorId = null)
                Long sectorId = sectorCombo.getValue() != null
                        ? sectorCombo.getValue().id()
                        : null;

                EmployeeUpdateRequest req = new EmployeeUpdateRequest(
                        fullNameField.getText(),
                        nationalIdField.getText(),
                        TextFieldSetupHelper.parseDateInput(hireDateField.getText()),
                        TextFieldSetupHelper.parseDateInput(terminationDateField.getText()),
                        terminationReasonCombo.getValue(),
                        sectorId
                );
                current = ApiClient.put("/employees/" + employeeId, req,
                        EmployeeProfileDto.class).getData();
                bindAll(current);
                showInfo("تم حفظ البيانات الأساسية بنجاح");
            }
        } catch (Exception ex) {
            showError("تعذر حفظ البيانات", ex);
        } finally {
            saveBasicButton.setDisable(false);
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  ③ / ④ حفظ الجداول
    // ─────────────────────────────────────────────────────────────

    private void saveSocialStatus(List<SocialStatusRow> rows) throws Exception {
        if (employeeId == null)
            throw new IllegalStateException("لازم تحفظ بيانات الموظف الأساسية الأول");

        List<SocialStatusEntryRequest> requests = rows.stream()
                .map(SocialStatusRow::toRequest)
                .toList();

        current = ApiClient.put(
                "/employees/" + employeeId + "/social-status/bulk",
                requests,
                EmployeeProfileDto.class
        ).getData();

        currentSocialStatusLabel.setText(
                current.currentSocialStatusLabelAr() != null
                        ? current.currentSocialStatusLabelAr() : "—");

        socialTbl.reload(
                current.socialStatusHistory().stream().map(SocialStatusRow::from).toList());
    }

    private void saveJobTitles(List<JobTitleRow> rows) throws Exception {
        if (employeeId == null)
            throw new IllegalStateException("لازم تحفظ بيانات الموظف الأساسية الأول");

        List<JobTitleEntryRequest> requests = rows.stream()
                .map(JobTitleRow::toRequest)
                .toList();

        current = ApiClient.put(
                "/employees/" + employeeId + "/job-title/bulk",
                requests,
                EmployeeProfileDto.class
        ).getData();

        currentJobTitleLabel.setText(
                current.currentJobTitleNameAr() != null
                        ? current.currentJobTitleNameAr() : "—");

        jobTbl.reload(
                current.jobTitleHistory().stream().map(JobTitleRow::from).toList());
    }

    // ─────────────────────────────────────────────────────────────
    //  حذف
    // ─────────────────────────────────────────────────────────────

    private void onDelete() {
        if (employeeId == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "متأكد من حذف الموظف؟", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    ApiClient.delete("/employees/" + employeeId, Void.class);
                    newEmployee();
                    showInfo("تم الحذف");
                } catch (Exception ex) {
                    showError("تعذر حذف الموظف", ex);
                }
            }
        });
    }

    // ─────────────────────────────────────────────────────────────
    //  مساعدات
    // ─────────────────────────────────────────────────────────────

    private void clearBasicForm() {
        employeeNumberField.clear();
        employeeNumberField.setDisable(false);
        fullNameField.clear();
        nationalIdField.clear();
        hireDateField.clear();
        terminationDateField.clear();
        terminationReasonCombo.setValue(null);
        sectorCombo.setValue(null);
        sectorCombo.setDisable(false);
        currentSocialStatusLabel.setText("—");
        currentJobTitleLabel.setText("—");
    }

    private void setFormEnabled(boolean enabled) {
        saveBasicButton.setDisable(!enabled);
        deleteButton.setDisable(!enabled || employeeId == null);
    }

    /**
     * يظهر/يخفي قسمي السجلات:
     *  - historyRow1: الحالة الاجتماعية + الوظائف
     *  - historyRow2: الترقيات + التشجيعيات + الحوافز
     * مع الفاصل الأفقي بينهم.
     */
    private void setHistorySectionsVisible(boolean visible) {
        historyRow1.setVisible(visible);
        historyRow1.setManaged(visible);

        historyRow1Separator.setVisible(visible);
        historyRow1Separator.setManaged(visible);

        historyRow2.setVisible(visible);
        historyRow2.setManaged(visible);
    }

    private ListCell<TerminationReason> reasonCell() {
        return new ListCell<>() {
            @Override protected void updateItem(TerminationReason item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getLabelAr());
            }
        };
    }

    private void showInfo(String msg)  {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }
    private void showWarn(String msg)  {
        new Alert(Alert.AlertType.WARNING,     msg, ButtonType.OK).showAndWait();
    }
    private void showError(String msg, Exception ex) {
        new Alert(Alert.AlertType.ERROR, msg + "\n" + ex.getMessage(), ButtonType.OK).showAndWait();
    }

    // ─────────────────────────────────────────────────────────────
    //  inner record — خيار القطاع في ComboBox
    // ─────────────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SectorOption(Long id, String nameAr) {
        @Override public String toString() { return nameAr; }
    }
}