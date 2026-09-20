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
        InsuranceBase insuranceBase,
        Scope scope,
        ElementType elementType,
        boolean subjectToInsurance,
        boolean subjectToTaxAndStamp,
        boolean inMinimumWageBase,
        boolean displayOnly,
        boolean appliesToNewHires,
        Map<String, BigDecimal> valuesMap,
        List<String> eligibleSectorCodes,      // 🆕 multi
        List<String> eligibleJobTitles,        // 🆕 بديل eligibleLawCodes
        List<String> excludedMonths,
        List<String> eligibleLaws,
        TimelineAnchor timelineAnchor,
        String notes
) {
}