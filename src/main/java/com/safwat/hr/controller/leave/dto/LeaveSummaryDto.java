package com.safwat.hr.controller.leave.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class LeaveSummaryDto {
    private int leaveYear;
    private String nationalId;
    private String employeeNumber;
    private String employeeName;
    private String gender;

    /**
     * ★ جديد — تاريخ التعيين (لتفلتر السنوات في السجل).
     */
    private LocalDate hireDate;

    private AnnualBalanceSummaryDto annual;
    private CasualSummaryDto casual;
    private List<LatePermissionMonthDto> lateByMonth = new ArrayList<>();
    private PeriodicUsageDto maternity;
    private PeriodicUsageDto childCare;
    private List<LeaveRecordDto> openEnded = new ArrayList<>();
}