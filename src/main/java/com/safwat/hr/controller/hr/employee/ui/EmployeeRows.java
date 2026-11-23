package com.safwat.hr.controller.hr.employee.ui;

import com.fasterxml.jackson.core.type.TypeReference;
import com.safwat.hr.controller.hr.employee.dto.JobTitleEntryDto;
import com.safwat.hr.controller.hr.employee.dto.JobTitleEntryRequest;
import com.safwat.hr.controller.hr.employee.dto.ManagementEntryDto;
import com.safwat.hr.controller.hr.employee.dto.ManagementEntryRequest;
import com.safwat.hr.controller.hr.employee.dto.SocialStatusEntryDto;
import com.safwat.hr.controller.hr.employee.dto.SocialStatusEntryRequest;
import com.safwat.hr.controller.hr.employee.enums.SocialStatus;
import com.safwat.hr.controller.hr.scale.dto.EncouragementRecord;
import com.safwat.hr.controller.hr.scale.dto.PromotionIncentiveRecord;
import com.safwat.hr.controller.hr.scale.dto.UpgradeRecord;
import com.safwat.hr.network.ApiClient;
import com.safwat.hr.shared.ui.SearchDialog;
import com.safwat.hr.ui.table.TableSetupHelper;
import com.safwat.hr.ui.table.TableSetupHelper.ComboOption;
import com.safwat.hr.ui.table.TableSetupHelper.EditorType;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public final class EmployeeRows {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private EmployeeRows() {
    }

    // ═══════════════════════════════════════════════════════
    //  SocialStatusRow
    // ═══════════════════════════════════════════════════════

    @Data
    @NoArgsConstructor
    public static class SocialStatusRow {
        private String effectiveFrom = "";
        private String statusCode = "";
        private String statusLabelAr = "";

        public static SocialStatusRow from(SocialStatusEntryDto dto) {
            SocialStatusRow r = new SocialStatusRow();
            r.effectiveFrom = dto.effectiveFrom() != null ? dto.effectiveFrom().format(DATE_FMT) : "";
            r.statusCode = dto.code() != null ? dto.code() : "";
            r.statusLabelAr = dto.labelAr() != null ? dto.labelAr() : "";
            return r;
        }

        public SocialStatusEntryRequest toRequest() {
            LocalDate date = TableSetupHelper.parseDateInput(effectiveFrom);
            SocialStatus status = statusCode != null && !statusCode.isBlank()
                    ? SocialStatus.valueOf(statusCode.trim().toUpperCase())
                    : null;
            return new SocialStatusEntryRequest(date, status);
        }

        @Override
        public String toString() {
            return effectiveFrom + statusCode;
        }

        public static List<TableSetupHelper.ColumnConfig<SocialStatusRow>> columns() {

            Supplier<List<ComboOption>> statusOptions = () ->
                    Arrays.stream(SocialStatus.values())
                            .map(s -> ComboOption.of(s.name(), s.getLabelAr()))
                            .toList();

            return List.of(
                    new TableSetupHelper.ColumnConfig<>(
                            "تاريخ السريان", 150,
                            SocialStatusRow::getEffectiveFrom,
                            SocialStatusRow::setEffectiveFrom,
                            true, EditorType.DATE,
                            TableSetupHelper.ColumnAlign.CENTER, false, null
                    ),
                    new TableSetupHelper.ColumnConfig<>(
                            "الحالة الاجتماعية", 320,
                            SocialStatusRow::getStatusCode,
                            SocialStatusRow::setStatusCode,
                            true, EditorType.COMBO,
                            TableSetupHelper.ColumnAlign.RIGHT, true,
                            statusOptions
                    )
            );
        }
    }

    // ═══════════════════════════════════════════════════════
    //  JobTitleRow
    // ═══════════════════════════════════════════════════════

    @Data
    @NoArgsConstructor
    public static class JobTitleRow {
        private String effectiveFrom = "";
        private String jobTitleId = "";
        private String jobTitleCode = "";
        private String jobTitleNameAr = "";

        public static JobTitleRow from(JobTitleEntryDto dto) {
            JobTitleRow r = new JobTitleRow();
            r.effectiveFrom = dto.effectiveFrom() != null ? dto.effectiveFrom().format(DATE_FMT) : "";
            r.jobTitleId = dto.jobTitleId() != null ? dto.jobTitleId().toString() : "";
            r.jobTitleCode = dto.jobTitleCode() != null ? dto.jobTitleCode() : "";
            r.jobTitleNameAr = dto.jobTitleNameAr() != null ? dto.jobTitleNameAr() : "";
            return r;
        }

        public JobTitleEntryRequest toRequest() {
            LocalDate date = TableSetupHelper.parseDateInput(effectiveFrom);
            Long id = jobTitleId != null && !jobTitleId.isBlank()
                    ? Long.parseLong(jobTitleId.trim()) : null;
            return new JobTitleEntryRequest(date, id);
        }

        @Override
        public String toString() {
            return effectiveFrom + jobTitleId;
        }

        public static List<TableSetupHelper.ColumnConfig<JobTitleRow>> columns(
                List<JobTitleOption> jobTitles) {

            Supplier<List<ComboOption>> titleOptions = () ->
                    jobTitles.stream()
                            .map(jt -> ComboOption.of(
                                    jt.id().toString(),
                                    jt.nameAr() != null ? jt.nameAr() : ""))
                            .toList();

            return List.of(
                    new TableSetupHelper.ColumnConfig<>(
                            "تاريخ السريان", 150,
                            JobTitleRow::getEffectiveFrom,
                            JobTitleRow::setEffectiveFrom,
                            true, EditorType.DATE,
                            TableSetupHelper.ColumnAlign.CENTER, false, null
                    ),
                    new TableSetupHelper.ColumnConfig<>(
                            "الوظيفة", 420,
                            JobTitleRow::getJobTitleId,
                            JobTitleRow::setJobTitleId,
                            true, EditorType.COMBO,
                            TableSetupHelper.ColumnAlign.CENTER, true,
                            titleOptions
                    )
            );
        }
    }

    // ═══════════════════════════════════════════════════════
    //  ManagementRow ★ جديد — 3 أعمدة (تاريخ + إدارة + بحث)
    //  + عمود حذف تلقائي من EmployeeHistoryTable
    // ═══════════════════════════════════════════════════════

    @Data
    @NoArgsConstructor
    public static class ManagementRow {
        private String effectiveFrom = "";
        private String managementName = "";

        public static ManagementRow from(ManagementEntryDto dto) {
            ManagementRow r = new ManagementRow();
            r.effectiveFrom = dto.effectiveFrom() != null ? dto.effectiveFrom().format(DATE_FMT) : "";
            r.managementName = dto.managementName() != null ? dto.managementName() : "";
            return r;
        }

        public ManagementEntryRequest toRequest() {
            LocalDate date = TableSetupHelper.parseDateInput(effectiveFrom);
            return new ManagementEntryRequest(date,
                    managementName != null ? managementName.trim() : "");
        }

        @Override
        public String toString() {
            return effectiveFrom + managementName;
        }

        public static List<TableSetupHelper.ColumnConfig<ManagementRow>> columns() {
            return List.of(
                    // ① تاريخ السريان
                    new TableSetupHelper.ColumnConfig<>(
                            "تاريخ السريان", 140,
                            ManagementRow::getEffectiveFrom,
                            ManagementRow::setEffectiveFrom,
                            true, EditorType.DATE,
                            TableSetupHelper.ColumnAlign.CENTER, false, null
                    ),
                    // ② الإدارة (HBox: TextField + زر 🔍)
                    new TableSetupHelper.ColumnConfig<>(
                            "الإدارة", 380,
                            tc -> new ManagementSearchCell()
                    )
            );
        }
    }

    /**
     * خلية الإدارة — HBox فيه TextField (قابل للكتابة اليدوية) + زر 🔍 (بحث).
     * بتتزامن تلقائياً مع {@link ManagementRow#getManagementName()}.
     */
    private static class ManagementSearchCell extends TableCell<ManagementRow, String> {
        private final TextField field = new TextField();
        private final Button searchBtn = new Button("🔍");
        private final HBox box = new HBox(4, field, searchBtn);
        private boolean syncing = false;

        ManagementSearchCell() {
            field.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(field, Priority.ALWAYS);
            searchBtn.getStyleClass().add("btn-icon");
            searchBtn.setTooltip(new Tooltip("بحث في الإدارات"));

            field.textProperty().addListener((obs, o, n) -> {
                if (syncing) return;
                ManagementRow row = currentRow();
                if (row != null) row.setManagementName(n == null ? "" : n);
            });

            searchBtn.setOnAction(e -> {
                ManagementRow row = currentRow();
                if (row == null) return;
                try {
                    List<String> all = fetchManagements();
                    SearchDialog.forStrings()
                            .title("اختر الإدارة")
                            .data(all)
                            .show()
                            .ifPresent(sel -> {
                                row.setManagementName(sel);
                                syncing = true;
                                field.setText(sel);
                                syncing = false;
                            });
                } catch (Exception ex) {
                    new Alert(Alert.AlertType.ERROR,
                            "تعذر جلب الإدارات: " + ex.getMessage(),
                            ButtonType.OK).showAndWait();
                }
            });
        }

        private ManagementRow currentRow() {
            TableRow<ManagementRow> tr = getTableRow();
            return tr != null ? tr.getItem() : null;
        }

        private static List<String> fetchManagements() throws Exception {
            return ApiClient.getWithTypeRef(
                    "/payroll-data/user-managements",
                    new TypeReference<List<String>>() {}
            ).getData();
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setGraphic(null);
                return;
            }
            ManagementRow row = currentRow();
            if (row == null) {
                setGraphic(null);
                return;
            }
            String expected = row.getManagementName() == null ? "" : row.getManagementName();
            if (!expected.equals(field.getText())) {
                syncing = true;
                field.setText(expected);
                syncing = false;
            }
            setAlignment(Pos.CENTER_LEFT);
            setGraphic(box);
        }
    }

    // ═══════════════════════════════════════════════════════
    //  PromotionRow / EncouragementRow / PromotionIncentiveRow
    // ═══════════════════════════════════════════════════════

    @Data
    @NoArgsConstructor
    public static class PromotionRow {
        private String date = "";
        private String decisionNumber = "";
        private String degree = "";

        public static PromotionRow from(UpgradeRecord dto) {
            PromotionRow r = new PromotionRow();
            r.date = dto.getDate() != null ? dto.getDate().format(DATE_FMT) : "";
            r.decisionNumber = dto.getDecisionNumber() != null ? dto.getDecisionNumber() : "";
            r.degree = dto.getDegree() != null ? dto.getDegree() : "";
            return r;
        }

        public static List<TableSetupHelper.ColumnConfig<PromotionRow>> columns() {
            return List.of(
                    new TableSetupHelper.ColumnConfig<>(
                            "التاريخ", 130, PromotionRow::getDate, null, false),
                    new TableSetupHelper.ColumnConfig<>(
                            "رقم القرار", 160, PromotionRow::getDecisionNumber, null, false),
                    new TableSetupHelper.ColumnConfig<>(
                            "الدرجة", 120, PromotionRow::getDegree, null, false, false,
                            TableSetupHelper.ColumnAlign.CENTER, true)
            );
        }
    }

    @Data
    @NoArgsConstructor
    public static class EncouragementRow {
        private String date = "";
        private String decisionNumber = "";

        public static EncouragementRow from(EncouragementRecord dto) {
            EncouragementRow r = new EncouragementRow();
            r.date = dto.getDate() != null ? dto.getDate().format(DATE_FMT) : "";
            r.decisionNumber = dto.getDecisionNumber() != null ? dto.getDecisionNumber() : "";
            return r;
        }

        public static List<TableSetupHelper.ColumnConfig<EncouragementRow>> columns() {
            return List.of(
                    new TableSetupHelper.ColumnConfig<>(
                            "التاريخ", 130, EncouragementRow::getDate, null, false),
                    new TableSetupHelper.ColumnConfig<>(
                            "رقم القرار", 200, EncouragementRow::getDecisionNumber, null, false, false,
                            TableSetupHelper.ColumnAlign.CENTER, true)
            );
        }
    }

    @Data
    @NoArgsConstructor
    public static class PromotionIncentiveRow {
        private String date = "";
        private String decisionNumber = "";

        public static PromotionIncentiveRow from(PromotionIncentiveRecord dto) {
            PromotionIncentiveRow r = new PromotionIncentiveRow();
            r.date = dto.getDate() != null ? dto.getDate().format(DATE_FMT) : "";
            r.decisionNumber = dto.getDecisionNumber() != null ? dto.getDecisionNumber() : "";
            return r;
        }

        public static List<TableSetupHelper.ColumnConfig<PromotionIncentiveRow>> columns() {
            return List.of(
                    new TableSetupHelper.ColumnConfig<>(
                            "التاريخ", 130, PromotionIncentiveRow::getDate, null, false),
                    new TableSetupHelper.ColumnConfig<>(
                            "رقم القرار", 200, PromotionIncentiveRow::getDecisionNumber, null, false, false,
                            TableSetupHelper.ColumnAlign.CENTER, true)
            );
        }
    }
}