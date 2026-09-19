package com.safwat.hr.controller.entitlements.statutory;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class StatutoryDtos {

    private StatutoryDtos() {
    }

    /**
     * إعدادات التأمينات — بعد الـ refactor: الـ ceilings + floor فقط.
     * النسب بقت جزء من AllowanceDefinition.
     */
    public record InsuranceRateConfigDto(
            Long id,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            BigDecimal wageFloor,
            BigDecimal basicCeiling,
            BigDecimal variableCeiling,
            String notes
    ) {
    }

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