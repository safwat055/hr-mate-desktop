package com.safwat.hr.controller.allowance;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OverrideEntry(
        LocalDate from,
        LocalDate to,       // null = مفتوح لحد دلوقتي
        BigDecimal value
) {
    /**
     * هل التاريخ المطلوب يقع داخل هذه الفترة؟
     */
    public boolean covers(LocalDate targetDate) {
        if (targetDate.isBefore(from)) return false;
        if (to == null) return true;
        return !targetDate.isAfter(to);
    }
}