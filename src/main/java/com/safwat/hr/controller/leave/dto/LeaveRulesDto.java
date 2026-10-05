package com.safwat.hr.controller.leave.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LeaveRulesDto {
    private Integer maxDaysPerRequest;
    private Integer maxOccurrencesPerService;
    private Integer maxMonthsPerOccurrence;
    private Integer maxYearsPerService;
    private Integer yearlyQuotaDays;
    private Integer hoursPerDeductedDay;
}