package com.safwat.hr.controller.employee.ui;

import com.safwat.hr.controller.employee.dto.*;
import com.safwat.hr.controller.employee.enums.SocialStatus;
import com.safwat.hr.controller.scale.scale.dto.EncouragementRecord;
import com.safwat.hr.controller.scale.scale.dto.PromotionIncentiveRecord;
import com.safwat.hr.controller.scale.scale.dto.UpgradeRecord;
import com.safwat.hr.ui.table.TableSetupHelper;
import com.safwat.hr.ui.table.TableSetupHelper.ComboOption;
import com.safwat.hr.ui.table.TableSetupHelper.EditorType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * كلاسات الصفوف المتغيرة (mutable) المستخدمة في الجداول القابلة للتعديل.
 */
public final class EmployeeRows {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private EmployeeRows() {}

    // ═══════════════════════════════════════════════════════
    //  SocialStatusRow — قابل للتعديل
    // ═══════════════════════════════════════════════════════

    @Data
    @NoArgsConstructor
    public static class SocialStatusRow {
        private String effectiveFrom = "";   // yyyy-MM-dd
        private String statusCode    = "";   // SINGLE / MARRIED_1_CHILD / ...
        private String statusLabelAr = "";   // للعرض الداخلي فقط

        public static SocialStatusRow from(SocialStatusEntryDto dto) {
            SocialStatusRow r = new SocialStatusRow();
            r.effectiveFrom  = dto.effectiveFrom() != null ? dto.effectiveFrom().format(DATE_FMT) : "";
            r.statusCode     = dto.code()    != null ? dto.code()    : "";
            r.statusLabelAr  = dto.labelAr() != null ? dto.labelAr() : "";
            return r;
        }

        public SocialStatusEntryRequest toRequest() {
            LocalDate date = TableSetupHelper.parseDateInput(effectiveFrom);
            SocialStatus status = statusCode != null && !statusCode.isBlank()
                    ? SocialStatus.valueOf(statusCode.trim().toUpperCase())
                    : null;
            return new SocialStatusEntryRequest(date, status);
        }

        @Override public String toString() { return effectiveFrom + statusCode; }

        public static List<TableSetupHelper.ColumnConfig<SocialStatusRow>> columns() {

            Supplier<List<ComboOption>> statusOptions = () ->
                    Arrays.stream(SocialStatus.values())
                            .map(s -> ComboOption.of(s.name(), s.getLabelAr()))
                            .toList();

            return List.of(
                    // تاريخ السريان — TextField (بيتعامل معاه TableSetupHelper كتاريخ)
                    new TableSetupHelper.ColumnConfig<>(
                            "تاريخ السريان", 150,
                            SocialStatusRow::getEffectiveFrom,
                            SocialStatusRow::setEffectiveFrom,
                            true, EditorType.DATE,
                            TableSetupHelper.ColumnAlign.CENTER, false, null
                    ),
                    // الحالة الاجتماعية — ComboBox
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
    //  JobTitleRow — قابل للتعديل
    // ═══════════════════════════════════════════════════════

    @Data
    @NoArgsConstructor
    public static class JobTitleRow {
        private String effectiveFrom  = "";
        private String jobTitleId     = "";   // Long كـ String
        private String jobTitleCode   = "";   // للعرض الداخلي
        private String jobTitleNameAr = "";   // للعرض الداخلي

        public static JobTitleRow from(JobTitleEntryDto dto) {
            JobTitleRow r = new JobTitleRow();
            r.effectiveFrom  = dto.effectiveFrom() != null ? dto.effectiveFrom().format(DATE_FMT) : "";
            r.jobTitleId     = dto.jobTitleId()   != null ? dto.jobTitleId().toString() : "";
            r.jobTitleCode   = dto.jobTitleCode() != null ? dto.jobTitleCode() : "";
            r.jobTitleNameAr = dto.jobTitleNameAr() != null ? dto.jobTitleNameAr() : "";
            return r;
        }

        public JobTitleEntryRequest toRequest() {
            LocalDate date = TableSetupHelper.parseDateInput(effectiveFrom);
            Long id = jobTitleId != null && !jobTitleId.isBlank()
                    ? Long.parseLong(jobTitleId.trim()) : null;
            return new JobTitleEntryRequest(date, id);
        }

        @Override public String toString() { return effectiveFrom + jobTitleId; }

        /**
         * @param jobTitles قائمة وظائف القطاع (تُحمَّل قبل بناء الجدول)
         */
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
    //  PromotionRow — عرض فقط
    // ═══════════════════════════════════════════════════════

    @Data
    @NoArgsConstructor
    public static class PromotionRow {
        private String date           = "";
        private String decisionNumber = "";
        private String degree         = "";

        /** ★ يقبل UpgradeRecord الجديد من الباك */
        public static PromotionRow from(UpgradeRecord dto) {
            PromotionRow r = new PromotionRow();
            r.date           = dto.getDate() != null ? dto.getDate().format(DATE_FMT) : "";
            r.decisionNumber = dto.getDecisionNumber() != null ? dto.getDecisionNumber() : "";
            r.degree         = dto.getDegree() != null ? dto.getDegree() : "";
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

    // ═══════════════════════════════════════════════════════
    //  EncouragementRow — عرض فقط
    // ═══════════════════════════════════════════════════════

    @Data
    @NoArgsConstructor
    public static class EncouragementRow {
        private String date           = "";
        private String decisionNumber = "";

        /** ★ يقبل EncouragementRecord الجديد من الباك */
        public static EncouragementRow from(EncouragementRecord dto) {
            EncouragementRow r = new EncouragementRow();
            r.date           = dto.getDate() != null ? dto.getDate().format(DATE_FMT) : "";
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

    // ═══════════════════════════════════════════════════════
    //  PromotionIncentiveRow — عرض فقط
    // ═══════════════════════════════════════════════════════

    @Data
    @NoArgsConstructor
    public static class PromotionIncentiveRow {
        private String date           = "";
        private String decisionNumber = "";

        /** ★ يقبل PromotionIncentiveRecord الجديد من الباك */
        public static PromotionIncentiveRow from(PromotionIncentiveRecord dto) {
            PromotionIncentiveRow r = new PromotionIncentiveRow();
            r.date           = dto.getDate() != null ? dto.getDate().format(DATE_FMT) : "";
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