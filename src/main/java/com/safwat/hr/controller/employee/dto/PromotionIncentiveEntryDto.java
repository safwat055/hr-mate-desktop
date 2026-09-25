package com.safwat.hr.controller.employee.dto;

import java.time.LocalDate;

/** حافز ترقية — عرض فقط، مصدرها ScaleDto.promotionIncentives */
public record PromotionIncentiveEntryDto(
        LocalDate date,
        String    decisionNumber
) {}
