package com.safwat.hr.controller.leave.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LeaveTypeDto {
    private Long id;
    private String code;
    private String nameAr;
    private String category;
    private String genderRestriction;
    private boolean openEnded;
    private boolean deductsFromAnnual;
    private LeaveRulesDto rules;

    @Override
    public String toString() {
        return nameAr != null ? nameAr : code;
    }
}