//package com.safwat.hr.controller.allowance;
//
//
//import lombok.Getter;
//import lombok.Setter;
//
//import java.math.BigDecimal;
//import java.time.LocalDate;
//import java.util.List;
//import java.util.Map;
//
//
//@Getter
//@Setter
//
//public class AllowanceDefinition {
//
//
//    private Long id;
//    private String code;
//
//
//    private String nameAr;
//
//
//    private String nameEn;
//
//
//    private LocalDate effectiveFrom;
//
//    /**
//     * سلوك هذا الـ snapshot:
//     * REPLACE — يبدأ من الصفر ويلغي أي REPLACE قبله
//     * ADD     — يُضاف على ناتج آخر REPLACE
//     */
//
//    private Behavior behavior;
//
//    /**
//     * نوع الاحتساب:
//     * <ul>
//     *   <li>PERCENT_BY_DEGREE     — نسبة % حسب الدرجة</li>
//     *   <li>AMOUNT_BY_DEGREE      — مبلغ ثابت حسب الدرجة</li>
//     *   <li>FIXED_AMOUNT          — مبلغ ثابت لكل الدرجات</li>
//     *   <li>SALARY_ENGINE         — نسبة % من إجمالي المحرك (Phase 3)</li>
//     *   <li>DEPENDS_SALARY_ENGINE — مبلغ بعد اكتمال المحرك (Phase 3)</li>
//     * </ul>
//     */
//
//    private CalcType calcType;
//
//
//    private BaseSource baseSource;
//
//
//    private Scope scope = Scope.GENERAL;
//
//    /**
//     * قيم البدل — الـ key هو الدرجة الوظيفية كـ String:
//     * <pre>
//     *   PERCENT_BY_DEGREE:    { "6": 45, "7": 50, "8": 55 }
//     *   AMOUNT_BY_DEGREE:     { "6": 150, "7": 200, "8": 250 }
//     *   FIXED_AMOUNT:         { "all": 300 }
//     *   SALARY_ENGINE:        { "percent": 10 }
//     *   DEPENDS_SALARY_ENGINE:{ "percent": 5 }
//     * </pre>
//     * الدرجة بتيجي من ScaleTimelinePoint.degree (int → String)
//     */
//
//    private Map<String, BigDecimal> valuesMap;
//
//    /**
//     * الشهور المستثناة — قيمة البدل فيها صفر بغض النظر عن القيمة أوتوماتيك أو يدوية.
//     * تنسيق: ["yyyy-MM"] مثال: ["2023-08", "2023-09"]
//     */
//
//    private List<String> excludedMonths;
//
//    /**
//     * أكواد القوانين المستحقة للبدل — يقابل SalaryScale.law (47 | 81).
//     * null = ينطبق على كل القوانين.
//     * مثال: ["47", "81"]
//     */
//
//    private List<String> eligibleLaws;
//
//    /**
//     * أكواد الوظائف المستحقة للبدل — يقابل SalaryScale.lawCode (7 | 7.1 | 7.2 ...).
//     * null = ينطبق على كل الأكواد الوظيفية.
//     * مثال: ["7", "7.1", "7.2"]
//     */
//
//    private List<String> eligibleLawCodes;
//
//
//    private String notes;
//
//    // ══════════════════════════════════════════════════════════
//    //  Enums
//    // ══════════════════════════════════════════════════════════
//
//    public enum Behavior {
//        REPLACE,
//        ADD
//    }
//
//    public enum CalcType {
//        PERCENT_BY_DEGREE,
//        AMOUNT_BY_DEGREE,
//        FIXED_AMOUNT,
//        SALARY_ENGINE,
//        DEPENDS_SALARY_ENGINE
//    }
//
//    public enum BaseSource {
//        FROM_BASIC,
//        FROM_STEP_SALARY,
//        CURRENT_BASIC   // صريح بدل الاعتماد على null
//    }
//
//    public enum Scope {
//        GENERAL,
//        SPECIAL
//    }
//}