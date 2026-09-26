package com.safwat.hr.controller.entitlements.allowance;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * نسخة الفرونت من AllowanceDefinition.
 */
@Setter
@Getter
public class AllowanceDefinition {

    private Long id;
    private String code;
    private String nameAr;
    private String nameEn;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private LocalDate referenceDate;
    private Behavior behavior;
    private CalcType calcType;
    private BaseSource baseSource;
    private InsuranceBase insuranceBase;
    private Scope scope;
    private ElementType elementType;

    private boolean subjectToInsurance;
    private boolean subjectToTaxAndStamp;
    private boolean inMinimumWageBase;
    private boolean displayOnly;
    private boolean appliesToNewHires = true;
    private Map<String, BigDecimal> valuesMap;
    private List<String> excludedMonths;
    private List<String> eligibleLaws;
    private List<String> eligibleSectorCodes;
    private List<String> eligibleJobTitles;
    private TimelineAnchor timelineAnchor;
    private String notes;

    // ══════════════════════════════════════════
    //  Enums
    // ══════════════════════════════════════════

    public enum Behavior {REPLACE, ADD}

    public enum CalcType {
        PERCENT_BY_DEGREE,
        PERCENT_ALL_DEGREE,
        AMOUNT_BY_DEGREE,
        FIXED_AMOUNT,
        SALARY_ENGINE,
        DEPENDS_SALARY_ENGINE,
        COMPENSATORY_BONUS,
        SUPPLEMENTARY_BONUS,
        SPECIAL_ALLOWANCE_ADDED,
        SPECIAL_ALLOWANCE_NOT_ADDED,
        SOCIAL_PACKAGE_MINIMUM,
        FIXED_AMOUNT_BY_MARITAL_STATUS,
        INSURANCE_RATE_EMPLOYEE,
        INSURANCE_RATE_EMPLOYER,
        PROMOTION_INCENTIVE,
        PERCENT_BY_JOB,
        PERCENT_ALL_JOB,
        AMOUNT_BY_JOB,
        ANNUAL_BONUS
    }

    public enum InsuranceBase {BASIC, VARIABLE, COMBINED}

    public enum BaseSource {FROM_BASIC, FROM_STEP_SALARY, CURRENT_BASIC}

    public enum Scope {GENERAL, SPECIAL}

    public enum TimelineAnchor {TARGET_DATE, EFFECTIVE_FROM}

    public enum ElementType {ENTITLEMENT, DEDUCTION, INSURANCE, TAX, STAMP}
}