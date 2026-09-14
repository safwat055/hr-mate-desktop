package com.safwat.hr.controller.allowance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * ناتج احتساب بدلات موظف — نسخة الفرونت (client-side).
 *
 * <p>يُستقبل من GET /api/allowances/employee/{nationalId}
 * ويُملأ به {@code table_allowances} في {@code AllowanceFxController}.
 *
 * <p>يعكس بالضبط {@code AllowanceResultDto} في الباك —
 * بما فيها {@code OverrideEntry} من هذا الـ package
 * (مش من entity الباك).
 */
public record AllowanceResultDto(

        String nationalId,
        LocalDate calculatedAt,
        List<AllowanceLineDto> allowances,
        BigDecimal totalAllowances
) {

    /**
     * سطر بدل واحد في الجدول.
     *
     * <p>بيُستخدم كـ item في {@code TableView<AllowanceLineDto>}
     * في {@code AllowanceFxController}.
     *
     * <p>ملاحظة: الـ record fields محتاجة getters بالشكل الاعتيادي
     * (بدون is/get) عشان {@code PropertyValueFactory} يشتغل معها
     * صح في جدول JavaFX — وده السلوك الافتراضي للـ records.
     */
    public record AllowanceLineDto(

            /** كود البدل */
            String code,

            /** اسم البدل بالعربي */
            String nameAr,

            /** القيمة المحسوبة في التاريخ المطلوب */
            BigDecimal value,

            /** تاريخ بداية القيمة الحالية */
            LocalDate effectiveFrom,

            /** مصدر القيمة */
            Source source,

            /**
             * الفترات اليدوية المخزنة.
             * null لو source = AUTO أو EXCLUDED.
             * بيُعرض في {@code AllowanceOverrideDialogController}.
             */
            List<OverrideEntry> overrides
    ) {

        public enum Source {
            /**
             * محسوب تلقائياً من allowance_definition
             */
            AUTO,
            /**
             * قيمة يدوية من المستخدم
             */
            MANUAL,
            /**
             * شهر مستثنى — القيمة صفر
             */
            EXCLUDED
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  Request DTOs — بتُبنى في الفرونت وتُرسل للباك
    // ══════════════════════════════════════════════════════════════

    /**
     * تعديل أو إضافة فترات يدوية لبدل معين.
     *
     * <p>يُرسل في PUT /api/allowances/employee/{nationalId}/override
     * من {@code AllowanceOverrideDialogController#handleSaveAll()}.
     */
    public record OverrideRequest(
            String allowanceCode,
            List<OverrideEntry> entries
    ) {
    }

    /**
     * إضافة بدل جديد للموظف مع أول فترة يدوية.
     *
     * <p>يُرسل في POST /api/allowances/employee/{nationalId}/allowance
     * من {@code AllowanceFxController#handleAddAllowance()} عبر
     * {@code AllowanceOverrideDialogController}.
     */
    public record AddAllowanceRequest(
            String allowanceCode,
            LocalDate from,
            LocalDate to,
            BigDecimal value
    ) {
    }

    // ══════════════════════════════════════════════════════════════
    //  Timeline
    // ══════════════════════════════════════════════════════════════

    /**
     * ناتج الاحتساب على فترة كاملة — للكشف الشهري.
     *
     * <p>يُستقبل من GET /api/allowances/employee/{nationalId}/timeline
     * Map<تاريخ نقطة التغيير، إجمالي البدلات>
     */
    public record AllowanceTimelineDto(
            String nationalId,
            Map<LocalDate, BigDecimal> timeline
    ) {
    }
}