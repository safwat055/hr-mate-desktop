package com.safwat.hr.controller.leave.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Mirror لـ AnnualBalanceSummaryDto من الباك.
 * <p>
 * الفرق عن النسخة القديمة: لا نستخدم carriedFromPrevious في الواجهة،
 * وأضفنا currentYearRemaining لعرض "باقي" في بطاقة الاعتيادي.
 */
@Getter
@Setter
public class AnnualBalanceSummaryDto {
    private int leaveYear;

    private int entitledThisYear;
    private int carriedFromPrevious;   // موجود في الباك — مش بنعرضه
    private int used;
    private int usedViaLate;
    private int manualAdjustment;

    /**
     * الإجمالي المتاح = متبقي السنة الحالية + متبقي السنوات السابقة (مقصوص بـ 60).
     */
    private int available;

    /**
     * ★ متبقي السنة الحالية = entitledThisYear − used − usedViaLate (لا يقل عن 0).
     */
    private int currentYearRemaining;

    private int cashCompensationEligible;

    /**
     * كل السنين من التعيين حتى السنة الحالية.
     */
    private List<YearSlice> slices = new ArrayList<>();

    private EntitlementBreakdown breakdown;

    @Getter
    @Setter
    public static class YearSlice {
        private int leaveYear;
        private int entitled;
        private int used;
        private int remaining;
        private boolean expired;
        private boolean eligibleForCash;
    }
}