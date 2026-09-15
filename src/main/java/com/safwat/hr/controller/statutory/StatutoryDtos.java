package com.safwat.hr.controller.statutory;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTOs لإدارة الاستقطاعات القانونية — نسخة الفرونت.
 */
public final class StatutoryDtos {

    private StatutoryDtos() {
    }

    // ══════════════════════════════════════════════════════════════
    //  التأمينات
    // ══════════════════════════════════════════════════════════════

    /**
     * إعدادات التأمينات — نسخة الفرونت.
     *
     * <p><b>نظامان:</b>
     * <ul>
     *   <li><b>موحّد</b> (بعد 2020):
     *       {@code employeeRate} + {@code employerRate} فقط.</li>
     *   <li><b>وعائين</b> (قبل 2020):
     *       {@code basicXxxRate} + {@code variableXxxRate} فقط.</li>
     * </ul>
     *
     * <p>النسب عشرية (0.11 = 11%). الواجهة بتحوّلها لنسبة مئوية للعرض.
     */
    public record InsuranceRateConfigDto(
            Long id,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            // نظام موحّد (بعد 2020)
            BigDecimal employeeRate,
            BigDecimal employerRate,
            // نظام وعائين (قبل 2020)
            BigDecimal basicEmployeeRate,
            BigDecimal basicEmployerRate,
            BigDecimal variableEmployeeRate,
            BigDecimal variableEmployerRate,
            // مشترك
            BigDecimal wageFloor,
            BigDecimal wageCeiling,
            String notes
    ) {
        /**
         * هل الإعداد ده نظام وعائين؟
         */
        public boolean isSplitMode() {
            return basicEmployeeRate != null;
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  الضريبة
    // ══════════════════════════════════════════════════════════════

    public record TaxBracket(
            Long id,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            int bracketOrder,
            BigDecimal fromAmount,
            BigDecimal toAmount,
            BigDecimal rate,
            String notes
    ) {
    }

    // ══════════════════════════════════════════════════════════════
    //  الدمغة
    // ══════════════════════════════════════════════════════════════

    public record StampDutyBracket(
            Long id,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            
            int bracketOrder,
            BigDecimal fromAmount,
            BigDecimal toAmount,
            BigDecimal rate,
            String notes
    ) {
    }
}