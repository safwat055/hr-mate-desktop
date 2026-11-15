package com.safwat.hr.controller.hr.employee.dto;

import java.time.LocalDate;

/**
 * ترقية — عرض فقط، مصدرها ScaleDto.upgrades
 */
public record PromotionEntryDto(
        LocalDate date,
        String decisionNumber,
        String degree          // الدرجة بعد الترقية
) {
}
