package com.safwat.hr.controller.entitlements.allowance;

import com.safwat.hr.controller.entitlements.allowance.AllowanceDefinition.ElementType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * ناتج احتساب بدلات موظف — نسخة الفرونت.
 *
 * <p><b>مهم:</b> بيستخدم {@link AllowanceDefinition.ElementType} الموحّد —
 * مفيش enum مكرر هنا عشان نمنع أي تعارض في الأنواع.
 */
public record AllowanceResultDto(
        String nationalId,
        LocalDate calculatedAt,
        List<AllowanceLineDto> allowances,
        BigDecimal totalAllowances,

        // 🆕 الأوعية التأمينية
        BigDecimal insurableBasic,      // الأجر الأساسي
        BigDecimal insurableVariable,   // الأجر المتغير
        BigDecimal insurableCombined,   // الأجر الاشتراكي (BASIC + VARIABLE)

        // 🆕 المجاميع
        BigDecimal totalEntitlements,   // جملة المستحق
        BigDecimal totalDeductions,     // جملة الاستقطاعات (موجبة)
        BigDecimal netAmount            // الصافي
) {

    public record AllowanceLineDto(
            String code,
            String nameAr,
            BigDecimal value,
            LocalDate effectiveFrom,
            Source source,
            List<OverrideEntry> overrides,
            ElementType elementType,
            boolean subjectToInsurance,
            boolean subjectToTaxAndStamp,
            boolean inMinimumWageBase,
            boolean displayOnly
    ) {

        /**
         * Constructor قصير — كله false.
         */
        public AllowanceLineDto(String code, String nameAr, BigDecimal value,
                                LocalDate effectiveFrom, Source source,
                                List<OverrideEntry> overrides) {
            this(code, nameAr, value, effectiveFrom, source, overrides,
                    ElementType.ENTITLEMENT, false, false, false, false);
        }

        public enum Source {AUTO, MANUAL, EXCLUDED}
    }

    // ══════════════════════════════════════════════════════════════
    //  Requests
    // ══════════════════════════════════════════════════════════════

    public record OverrideRequest(String allowanceCode, List<OverrideEntry> entries) {
    }

    public record AddAllowanceRequest(
            String allowanceCode, LocalDate from, LocalDate to, BigDecimal value) {
    }

    public record AllowanceTimelineDto(
            String nationalId, Map<LocalDate, BigDecimal> timeline) {
    }
}