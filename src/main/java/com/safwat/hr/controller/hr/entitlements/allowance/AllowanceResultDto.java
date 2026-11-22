package com.safwat.hr.controller.hr.entitlements.allowance;

import com.safwat.hr.controller.hr.entitlements.allowance.AllowanceDefinition.CalcType;
import com.safwat.hr.controller.hr.entitlements.allowance.AllowanceDefinition.ElementType;
// import ... OverrideEntry  ← المسار الصح محتاج تأكيد

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record AllowanceResultDto(
        String nationalId,
        LocalDate calculatedAt,
        List<AllowanceLineDto> allowances,
        BigDecimal totalAllowances,
        BigDecimal insurableBasic,
        BigDecimal insurableVariable,
        BigDecimal insurableCombined,
        BigDecimal totalEntitlements,
        BigDecimal totalDeductions,
        BigDecimal netAmount
) {

    public record AllowanceLineDto(
            String code,
            String nameAr,
            BigDecimal value,
            LocalDate effectiveFrom,
            Source source,
            List<OverrideEntry> overrides,
            ElementType elementType,
            CalcType calcType,              // 🆕 لازم يطابق الباك إند
            boolean subjectToInsurance,
            boolean subjectToTaxAndStamp,
            boolean inMinimumWageBase,
            boolean displayOnly
    ) {

        public AllowanceLineDto(String code, String nameAr, BigDecimal value,
                                LocalDate effectiveFrom, Source source,
                                List<OverrideEntry> overrides) {
            this(code, nameAr, value, effectiveFrom, source, overrides,
                    ElementType.ENTITLEMENT, null, false, false, false, false);
        }

        public enum Source {AUTO, MANUAL, EXCLUDED}
    }

    public record OverrideRequest(String allowanceCode, List<OverrideEntry> entries) {
    }

    public record AddAllowanceRequest(
            String allowanceCode, LocalDate from, LocalDate to, BigDecimal value) {
    }

    public record AllowanceTimelineDto(
            String nationalId, Map<LocalDate, BigDecimal> timeline) {
    }
}