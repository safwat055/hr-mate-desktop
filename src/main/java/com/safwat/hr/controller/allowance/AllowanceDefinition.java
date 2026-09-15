package com.safwat.hr.controller.allowance;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * DTO لتعريف البدل — نسخة الفرونت (client-side).
 *
 * <p>بيعكس بالضبط الـ response اللي بيرجعه الباك من:
 * <ul>
 *   <li>GET  /api/allowances/definitions          (أحدث snapshot لكل كود)</li>
 *   <li>GET  /api/allowances/definitions/{code}/history (التاريخ الكامل)</li>
 *   <li>POST /api/allowances/definitions          (ناتج الإنشاء)</li>
 *   <li>PUT  /api/allowances/definitions/{id}     (ناتج التعديل)</li>
 * </ul>
 *
 * <p>مبني على {@code AllowanceDefinition} entity في الباك —
 * بدون أي annotation JPA أو Spring (هذا فرونت JavaFX خالص).
 */
@Getter
@Setter
public class AllowanceDefinition {

    private Long id;

    private String code;

    private String nameAr;

    private String nameEn;

    private LocalDate effectiveFrom;

    /**
     * سلوك هذا الـ snapshot:
     * REPLACE — يبدأ من الصفر ويلغي أي REPLACE قبله
     * ADD     — يُضاف على ناتج آخر REPLACE
     */
    private Behavior behavior;

    /**
     * نوع الاحتساب:
     * <ul>
     *   <li>PERCENT_BY_DEGREE      — نسبة % حسب الدرجة</li>
     *   <li>PERCENT_ALL_DEGREE     — نسبة % موحدة لكل الدرجات</li>
     *   <li>AMOUNT_BY_DEGREE       — مبلغ ثابت حسب الدرجة</li>
     *   <li>FIXED_AMOUNT           — مبلغ ثابت لكل الدرجات</li>
     *   <li>SALARY_ENGINE          — نسبة % من إجمالي المحرك (Phase 3)</li>
     *   <li>DEPENDS_SALARY_ENGINE  — مبلغ بعد اكتمال المحرك (Phase 3)</li>
     *   <li>COMPENSATORY_BONUS     — الحافز التعويضي (محسوب تلقائياً)</li>
     *   <li>SUPPLEMENTARY_BONUS    — الحافز التكميلي (Phase 4)</li>
     * </ul>
     */
    private CalcType calcType;

    /**
     * مصدر الأساسي — يُستخدم مع PERCENT_BY_DEGREE و PERCENT_ALL_DEGREE فقط.
     * null = مش مهم مع باقي الأنواع.
     */
    private BaseSource baseSource;

    /**
     * نطاق البدل:
     * GENERAL — يُضاف تلقائياً لكل موظف مستحق.
     * SPECIAL — لا يُضاف تلقائياً، لازم يتضاف يدوياً.
     */
    private Scope scope;

    /**
     * قيم البدل حسب CalcType:
     * <pre>
     *   PERCENT_BY_DEGREE:    { "6": 45, "7": 50 }
     *   PERCENT_ALL_DEGREE:   { "all": 10 }
     *   AMOUNT_BY_DEGREE:     { "6": 150, "7": 200 }
     *   FIXED_AMOUNT:         { "all": 300 }
     *   SALARY_ENGINE:        { "percent": 10 }
     *   DEPENDS_SALARY_ENGINE:{ "percent": 5 }
     *   COMPENSATORY_BONUS:   { "all": 0 }   ← placeholder
     *   SUPPLEMENTARY_BONUS:  { "6": 500, "7": 600 }
     * </pre>
     */
    private Map<String, BigDecimal> valuesMap;

    /**
     * الشهور المستثناة — قيمة البدل فيها صفر.
     * تنسيق: ["yyyy-MM"] مثال: ["2023-08", "2023-09"]
     */
    private List<String> excludedMonths;

    /**
     * أكواد القوانين المستحقة — يقابل SalaryScale.law (47 | 81).
     * null = ينطبق على كل القوانين.
     */
    private List<String> eligibleLaws;

    /**
     * أكواد الوظائف المستحقة — يقابل SalaryScale.lawCode (7 | 7.1 | 7.2 ...).
     * null = ينطبق على كل الأكواد الوظيفية.
     */
    private List<String> eligibleLawCodes;

    /**
     * طبيعة العنصر:
     * ENTITLEMENT (افتراضي) | DEDUCTION | INSURANCE | TAX | STAMP
     */
    private ElementType elementType;

    /**
     * هل داخل في وعاء اشتراك التأمينات؟
     */
    private boolean subjectToInsurance;

    /**
     * هل داخل في وعاء ضريبة الدخل والدمغة؟
     */
    private boolean subjectToTaxAndStamp;
    
    private TimelineAnchor timelineAnchor;
    private String notes;

    // ══════════════════════════════════════════════════════════════
    //  Enums — نفس أسماء الباك حرفياً عشان الـ JSON deserialization يشتغل
    // ══════════════════════════════════════════════════════════════

    public enum Behavior {
        REPLACE,
        ADD
    }

    public enum CalcType {
        PERCENT_BY_DEGREE,
        PERCENT_ALL_DEGREE,
        AMOUNT_BY_DEGREE,
        FIXED_AMOUNT,
        SALARY_ENGINE,
        DEPENDS_SALARY_ENGINE,
        /**
         * الحافز التعويضي — محسوب تلقائياً من فرق الصافي في 2015
         */
        COMPENSATORY_BONUS,
        /**
         * الحافز التكميلي — Phase 4
         */
        SUPPLEMENTARY_BONUS
    }

    public enum BaseSource {
        FROM_BASIC,
        FROM_STEP_SALARY,
        CURRENT_BASIC
    }

    public enum Scope {
        GENERAL,
        SPECIAL
    }

    public enum TimelineAnchor {
        TARGET_DATE,       // الحالة عند targetDate (الافتراضي)
        EFFECTIVE_FROM     // الحالة عند تاريخ سريان البدل
    }

    public enum ElementType {
        /**
         * استحقاق — الافتراضي
         */
        ENTITLEMENT,
        /**
         * استقطاع
         */
        DEDUCTION,
        /**
         * عنصر تأمينات
         */
        INSURANCE,
        /**
         * عنصر ضريبة
         */
        TAX,
        /**
         * عنصر دمغة
         */
        STAMP
    }
}