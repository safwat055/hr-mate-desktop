package com.safwat.hr.controller.leave.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class EntitlementBreakdown {
    private List<MonthTier> months = new ArrayList<>();
    private double rawTotal;
    private int baseEntitlement;
    private int remoteBonus;
    private int total;
    private String notes;

    @Getter
    @Setter
    public static class MonthTier {
        private String month;
        private int tier;
    }
}