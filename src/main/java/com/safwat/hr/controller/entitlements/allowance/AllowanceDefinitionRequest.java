package com.safwat.hr.controller.entitlements.allowance;

import com.safwat.hr.controller.entitlements.allowance.AllowanceDefinition.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Request لإنشاء أو تعديل snapshot — نسخة الفرونت.
 *
 * <p>يعكس بالضبط {@code AllowanceDefinitionRequest} record في الباك:
 * <ul>
 *   <li>POST /entitlements/allowances/definitions</li>
 *   <li>PUT  /entitlements/allowances/definitions/{id}</li>
 * </ul>
 */
public record AllowanceDefinitionRequest(
        String code,
        String nameAr,
        String nameEn,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,               // 🆕
        Behavior behavior,
        CalcType calcType,
        BaseSource baseSource,
        Scope scope,
        ElementType elementType,
        boolean subjectToInsurance,
        boolean subjectToTaxAndStamp,
        boolean inMinimumWageBase,           // 🆕
        Map<String, BigDecimal> valuesMap,
        LocalDate referenceDate,             // 🆕
        List<String> excludedMonths,
        List<String> eligibleLaws,
        List<String> eligibleLawCodes,
        TimelineAnchor timelineAnchor,
        String notes
) {
}