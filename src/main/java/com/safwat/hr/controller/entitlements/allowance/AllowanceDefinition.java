package com.safwat.hr.controller.entitlements.allowance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * نسخة الفرونت من AllowanceDefinition.
 */
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
    private InsuranceBase insuranceBase;       // 🆕
    private Scope scope;
    private ElementType elementType;
    private boolean subjectToInsurance;
    private boolean subjectToTaxAndStamp;
    private boolean inMinimumWageBase;
    private boolean displayOnly;               // 🆕
    private boolean appliesToNewHires = true;  // 🆕
    private Map<String, BigDecimal> valuesMap;
    private List<String> excludedMonths;
    private List<String> eligibleLaws;
    private List<String> eligibleLawCodes;
    private TimelineAnchor timelineAnchor;
    private String notes;

    // Getters/Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getNameAr() {
        return nameAr;
    }

    public void setNameAr(String nameAr) {
        this.nameAr = nameAr;
    }

    public String getNameEn() {
        return nameEn;
    }

    public void setNameEn(String nameEn) {
        this.nameEn = nameEn;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDate v) {
        this.effectiveFrom = v;
    }

    public LocalDate getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(LocalDate v) {
        this.effectiveTo = v;
    }

    public LocalDate getReferenceDate() {
        return referenceDate;
    }

    public void setReferenceDate(LocalDate v) {
        this.referenceDate = v;
    }

    public Behavior getBehavior() {
        return behavior;
    }

    public void setBehavior(Behavior v) {
        this.behavior = v;
    }

    public CalcType getCalcType() {
        return calcType;
    }

    public void setCalcType(CalcType v) {
        this.calcType = v;
    }

    public BaseSource getBaseSource() {
        return baseSource;
    }

    public void setBaseSource(BaseSource v) {
        this.baseSource = v;
    }

    public InsuranceBase getInsuranceBase() {
        return insuranceBase;
    }

    public void setInsuranceBase(InsuranceBase v) {
        this.insuranceBase = v;
    }

    public Scope getScope() {
        return scope;
    }

    public void setScope(Scope v) {
        this.scope = v;
    }

    public ElementType getElementType() {
        return elementType;
    }

    public void setElementType(ElementType v) {
        this.elementType = v;
    }

    public boolean isSubjectToInsurance() {
        return subjectToInsurance;
    }

    public void setSubjectToInsurance(boolean v) {
        this.subjectToInsurance = v;
    }

    public boolean isSubjectToTaxAndStamp() {
        return subjectToTaxAndStamp;
    }

    public void setSubjectToTaxAndStamp(boolean v) {
        this.subjectToTaxAndStamp = v;
    }

    public boolean isInMinimumWageBase() {
        return inMinimumWageBase;
    }

    public void setInMinimumWageBase(boolean v) {
        this.inMinimumWageBase = v;
    }

    public boolean isDisplayOnly() {
        return displayOnly;
    }

    public void setDisplayOnly(boolean v) {
        this.displayOnly = v;
    }

    public boolean isAppliesToNewHires() {
        return appliesToNewHires;
    }

    public void setAppliesToNewHires(boolean v) {
        this.appliesToNewHires = v;
    }

    public Map<String, BigDecimal> getValuesMap() {
        return valuesMap;
    }

    public void setValuesMap(Map<String, BigDecimal> v) {
        this.valuesMap = v;
    }

    public List<String> getExcludedMonths() {
        return excludedMonths;
    }

    public void setExcludedMonths(List<String> v) {
        this.excludedMonths = v;
    }

    public List<String> getEligibleLaws() {
        return eligibleLaws;
    }

    public void setEligibleLaws(List<String> v) {
        this.eligibleLaws = v;
    }

    public List<String> getEligibleLawCodes() {
        return eligibleLawCodes;
    }

    public void setEligibleLawCodes(List<String> v) {
        this.eligibleLawCodes = v;
    }

    public TimelineAnchor getTimelineAnchor() {
        return timelineAnchor;
    }

    public void setTimelineAnchor(TimelineAnchor v) {
        this.timelineAnchor = v;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String v) {
        this.notes = v;
    }

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
        PROMOTION_INCENTIVE   // 🆕
    }

    public enum InsuranceBase {          // 🆕
        BASIC,
        VARIABLE,
        COMBINED
    }

    public enum BaseSource {FROM_BASIC, FROM_STEP_SALARY, CURRENT_BASIC}

    public enum Scope {GENERAL, SPECIAL}

    public enum TimelineAnchor {TARGET_DATE, EFFECTIVE_FROM}

    public enum ElementType {ENTITLEMENT, DEDUCTION, INSURANCE, TAX, STAMP}
}