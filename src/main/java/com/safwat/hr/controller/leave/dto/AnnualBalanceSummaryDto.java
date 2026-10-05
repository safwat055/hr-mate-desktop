package com.safwat.hr.controller.leave.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class AnnualBalanceSummaryDto {
    private int leaveYear;
    private int entitledThisYear;
    private int carriedFromPrevious;
    private int used;
    private int usedViaLate;
    private int manualAdjustment;
    private int available;
    private int cashCompensationEligible;
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