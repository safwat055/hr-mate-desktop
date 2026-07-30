package com.safwat.hr.report.payroll.ui;

/**
 * تعداد يُمثِّل جميع حقول الإدخال المتاحة في نموذج التقارير.
 *
 * <p>يُستخدَم في {@link UiConfiguration} لتحديد:
 * <ul>
 *   <li>الحقول الظاهرة ({@code visibleFields})</li>
 *   <li>الحقول الإلزامية ({@code requiredFields})</li>
 * </ul>
 *
 * <p><b>دمج:</b> استبدل هذا الـ enum كلًّا من {@code UiField} و{@code RequiredField}
 * السابقين، لأنهما كانا متطابقَين تمامًا مما أدى إلى تكرار غير ضروري.
 *
 * <p><b>إضافة حقل جديد:</b> أضف قيمة هنا ثم اربطها في {@link PayrollUIManager}.
 */
public enum UiField {

    /**
     * تاريخ البداية (الشهر المطلوب)
     */
    START_DATE,

    /**
     * تاريخ النهاية
     */
    END_DATE,

    /**
     * الإدارة
     */
    MANAGEMENT,

    /**
     * مجموعة التعيين
     */
    PAY_GROUP,

    /**
     * الرقم القومي للموظف
     */
    NATIONAL_ID,

    /**
     * المجموعة المخصصة / العنصر
     */
    CUSTOM_GROUP,

    /**
     * الوصف
     */
    DESCRIPTION,

    /**
     * ملاحظة
     */
    NOTE,

    /**
     * قيمة البحث الحر
     */
    SEARCH_VALUE
}