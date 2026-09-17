package com.safwat.hr.controller.entitlements.allowance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * نسخة الفرونت من AllowanceDefinition entity في الباك.
 *
 * <p>بتحمل كل الحقول عشان الـ deserialization من JSON، والـ enums
 * عشان الـ ComboBoxes في الـ dialogs.
 *
 * <p>مفيش أي business logic — DTO بحت.
 */
public class AllowanceDefinition {

    private Long id;
    private String code;
    private String nameAr;
    private String nameEn;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Behavior behavior;
    private CalcType calcType;
    private BaseSource baseSource;
    private Scope scope;
    private ElementType elementType;
    private boolean subjectToInsurance;
    private boolean subjectToTaxAndStamp;
    private boolean inMinimumWageBase;
    private Map<String, BigDecimal> valuesMap;
    private LocalDate referenceDate;
    private List<String> excludedMonths;
    private List<String> eligibleLaws;
    private List<String> eligibleLawCodes;
    private TimelineAnchor timelineAnchor;
    private String notes;

    // ══════════════════════════════════════════════════════════════
    //  Getters / Setters (مطلوبة لـ Jackson)
    // ══════════════════════════════════════════════════════════════

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

    public void setEffectiveFrom(LocalDate effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public LocalDate getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(LocalDate effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    public Behavior getBehavior() {
        return behavior;
    }

    public void setBehavior(Behavior behavior) {
        this.behavior = behavior;
    }

    public CalcType getCalcType() {
        return calcType;
    }

    public void setCalcType(CalcType calcType) {
        this.calcType = calcType;
    }

    public BaseSource getBaseSource() {
        return baseSource;
    }

    public void setBaseSource(BaseSource baseSource) {
        this.baseSource = baseSource;
    }

    public Scope getScope() {
        return scope;
    }

    public void setScope(Scope scope) {
        this.scope = scope;
    }

    public ElementType getElementType() {
        return elementType;
    }

    public void setElementType(ElementType elementType) {
        this.elementType = elementType;
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

    public Map<String, BigDecimal> getValuesMap() {
        return valuesMap;
    }

    public void setValuesMap(Map<String, BigDecimal> valuesMap) {
        this.valuesMap = valuesMap;
    }

    public LocalDate getReferenceDate() {
        return referenceDate;
    }

    public void setReferenceDate(LocalDate referenceDate) {
        this.referenceDate = referenceDate;
    }

    public List<String> getExcludedMonths() {
        return excludedMonths;
    }

    public void setExcludedMonths(List<String> excludedMonths) {
        this.excludedMonths = excludedMonths;
    }

    public List<String> getEligibleLaws() {
        return eligibleLaws;
    }

    public void setEligibleLaws(List<String> eligibleLaws) {
        this.eligibleLaws = eligibleLaws;
    }

    public List<String> getEligibleLawCodes() {
        return eligibleLawCodes;
    }

    public void setEligibleLawCodes(List<String> eligibleLawCodes) {
        this.eligibleLawCodes = eligibleLawCodes;
    }

    public TimelineAnchor getTimelineAnchor() {
        return timelineAnchor;
    }

    public void setTimelineAnchor(TimelineAnchor timelineAnchor) {
        this.timelineAnchor = timelineAnchor;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    // ══════════════════════════════════════════════════════════════
    //  Enums — مطابقة للباك
    // ══════════════════════════════════════════════════════════════

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
        SPECIAL_ALLOWANCE_ADDED,       // 🆕
        SPECIAL_ALLOWANCE_NOT_ADDED,    // 🆕

        SOCIAL_PACKAGE_MINIMUM,
        FIXED_AMOUNT_BY_MARITAL_STATUS
    }

    public enum BaseSource {FROM_BASIC, FROM_STEP_SALARY, CURRENT_BASIC}

    public enum Scope {GENERAL, SPECIAL}

    public enum TimelineAnchor {TARGET_DATE, EFFECTIVE_FROM}

    public enum ElementType {ENTITLEMENT, DEDUCTION, INSURANCE, TAX, STAMP}
}