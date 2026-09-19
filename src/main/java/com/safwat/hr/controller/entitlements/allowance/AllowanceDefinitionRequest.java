package com.safwat.hr.controller.entitlements.allowance;

import com.safwat.hr.controller.entitlements.allowance.AllowanceDefinition.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record AllowanceDefinitionRequest(
        String code,
        String nameAr,
        String nameEn,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        LocalDate referenceDate,
        Behavior behavior,
        CalcType calcType,
        BaseSource baseSource,
        InsuranceBase insuranceBase,        // 🆕
        Scope scope,
        ElementType elementType,
        boolean subjectToInsurance,

        boolean subjectToTaxAndStamp,
        boolean inMinimumWageBase,
        boolean displayOnly,                // 🆕
        boolean appliesToNewHires,          // 🆕
        Map<String, BigDecimal> valuesMap,
        String eligibleSectorCode,
        List<String> excludedMonths,
        List<String> eligibleLaws,
        List<String> eligibleLawCodes,
        TimelineAnchor timelineAnchor,
        String notes
) {
}