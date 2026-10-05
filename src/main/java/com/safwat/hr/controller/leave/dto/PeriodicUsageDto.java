package com.safwat.hr.controller.leave.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PeriodicUsageDto {
    private String typeCode;
    private String typeNameAr;
    private Integer occurrencesUsed;
    private Integer occurrencesMax;
    private Integer totalDaysUsed;
    private Integer totalDaysMax;
    private String usedFormatted;
    private String maxFormatted;
}