package com.safwat.hr.controller.allowance;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * ناتج احتساب بدلات موظف — يُرسل للفرونت.
 *
 * <p>كل بدل يظهر كسطر واحد في الجدول بالقيمة المحسوبة في التاريخ المطلوب.
 */
public record AllowanceResultDto(

        String nationalId,
        LocalDate calculatedAt,
        List<AllowanceLineDto> allowances,
        BigDecimal totalAllowances
) {

    /**
     * سطر بدل واحد في الجدول.
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
             * الفترات اليدوية المخزنة — null لو source = AUTO أو EXCLUDED
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
    //  Request DTOs
    // ══════════════════════════════════════════════════════════════

    /**
     * تعديل أو إضافة فترات يدوية لبدل معين.
     */
    public record OverrideRequest(
            String allowanceCode,
            List<OverrideEntry> entries
    ) {
    }

    /**
     * إضافة بدل جديد غير مضاف للموظف مع أول فترة يدوية.
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
     * Map<تاريخ نقطة التغيير، إجمالي البدلات>
     */
    public record AllowanceTimelineDto(
            String nationalId,
            Map<LocalDate, BigDecimal> timeline
    ) {
    }
}