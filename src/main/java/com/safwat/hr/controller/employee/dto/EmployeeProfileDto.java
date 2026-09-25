package com.safwat.hr.controller.employee.dto;

import com.safwat.hr.controller.employee.enums.TerminationReason;

import java.time.LocalDate;
import java.util.List;

/**
 * الـ DTO المجمّع اللي بيرجعه GET /api/employees/{id}.
 * currentSocialStatus* و currentJobTitle* محسوبين بالباك (floor lookup على تاريخ اليوم)
 * عشان الفرونت يعرضهم مباشرة في الشاشة الرئيسية من غير أي حسبة من جانبه.
 * socialStatusHistory و jobTitleHistory هما السجل الكامل، مرتبين من الأحدث للأقدم،
 * ويتستخدموا في البوباب بتاع السجل.
 */
public record EmployeeProfileDto(
        Long id,
        String employeeNumber,
        String fullName,
        String nationalId,
        LocalDate hireDate,
        LocalDate terminationDate,
        TerminationReason terminationReason,
        Long sectorId,
        String sectorNameAr,
        String currentSocialStatusCode,
        String currentSocialStatusLabelAr,
        Long currentJobTitleId,
        String currentJobTitleNameAr,
        List<SocialStatusEntryDto> socialStatusHistory,
        List<JobTitleEntryDto> jobTitleHistory
) {
}