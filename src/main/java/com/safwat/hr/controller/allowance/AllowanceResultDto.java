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

            /**
             * القيمة المحسوبة في التاريخ المطلوب.
             * <p><b>ملاحظة:</b> القيمة بإشارتها الأصلية:
             * موجبة للاستحقاقات، سالبة للاستقطاعات.
             * الواجهة هي اللي بتعرض القيمة المطلقة (abs) مع عمود النوع.
             */
            BigDecimal value,

            /** تاريخ بداية القيمة الحالية */
            LocalDate effectiveFrom,

            /** مصدر القيمة */
            Source source,

            /**
             * الفترات اليدوية المخزنة.
             * null لو source = AUTO أو EXCLUDED.
             */
            List<OverrideEntry> overrides,

            /**
             * نوع العنصر — لتحديد العرض (لون، ترتيب).
             * <ul>
             *   <li>ENTITLEMENT — استحقاق</li>
             *   <li>DEDUCTION   — استقطاع عادي</li>
             *   <li>INSURANCE   — تأمينات</li>
             *   <li>TAX         — ضريبة</li>
             *   <li>STAMP       — دمغة</li>
             * </ul>
             */
            ElementType elementType,

            /**
             * هل السطر ده في وعاء التأمينات؟
             */
            boolean subjectToInsurance,

            /**
             * هل السطر ده في وعاء الضريبة/الدمغة؟
             */
            boolean subjectToTaxAndStamp,

            /**
             * <b>عرض فقط</b> — لو true، السطر مش بيتحسب في الإجمالي.
             * <p>بيُستخدم لحصة الحكومة من التأمينات (مش بتتخصم من الموظف،
             * بس بتتعرض للتوضيح).
             */
            boolean displayOnly
    ) {

        public enum Source {
            AUTO,
            MANUAL,
            EXCLUDED
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  Enums — مطابقة للباك
    // ══════════════════════════════════════════════════════════════

    /**
     * نوع العنصر — نفس enum الباك بالظبط.
     */
    public enum ElementType {
        ENTITLEMENT,
        DEDUCTION,
        INSURANCE,
        TAX,
        STAMP
    }

    // ══════════════════════════════════════════════════════════════
    //  Request DTOs
    // ══════════════════════════════════════════════════════════════

    public record OverrideRequest(
            String allowanceCode,
            List<OverrideEntry> entries
    ) {
    }

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

    public record AllowanceTimelineDto(
            String nationalId,
            Map<LocalDate, BigDecimal> timeline
    ) {
    }
}