package com.safwat.hr.controller.wages.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * ناتج احتساب الأجر المتغير الخاضع للمعاشات لشهر واحد
 * (نسخة الفرونت من {@code WageMonthResultDto} في الباك اند).
 */
public record WageMonthResultDto(
        int year,
        int month,
        Map<String, BigDecimal> breakdown,
        Map<String, BigDecimal> allowanceBreakdown,
        BigDecimal rawTotal,
        BigDecimal pensionCeiling,
        BigDecimal totalPensionableWage,
        boolean ceilingApplied,
        List<String> documentNotes,
        WageSource source
) {

    public enum WageSource {
        ALLOWANCE_ENGINE,
        WAGE_DOCUMENTS,
        WAGE_DOCUMENTS_GROSS
    }

    public String monthLabel() {
        return String.format("%04d-%02d", year, month);
    }

    public String sourceLabel() {
        return switch (source) {
            case ALLOWANCE_ENGINE -> "بدلات";
            case WAGE_DOCUMENTS -> "مستندات خاضعة";
            case WAGE_DOCUMENTS_GROSS -> "مستندات (إجمالي)";
        };
    }
}
