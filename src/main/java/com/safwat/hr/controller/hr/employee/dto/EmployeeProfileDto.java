package com.safwat.hr.controller.hr.employee.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.safwat.hr.controller.hr.employee.enums.TerminationReason;
import com.safwat.hr.controller.hr.scale.dto.EncouragementRecord;
import com.safwat.hr.controller.hr.scale.dto.PromotionIncentiveRecord;
import com.safwat.hr.controller.hr.scale.dto.UpgradeRecord;

import java.time.LocalDate;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
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

        // ★ الإدارة
        String management,
        List<ManagementEntryDto> managementHistory,

        LocalDate disabilityStartDate,
        String disabilityDecisionNo,
        Boolean worksInRemoteGovernorate,

        String currentSocialStatusCode,
        String currentSocialStatusLabelAr,
        Long currentJobTitleId,
        String currentJobTitleNameAr,

        List<SocialStatusEntryDto> socialStatusHistory,
        List<JobTitleEntryDto> jobTitleHistory,

        List<UpgradeRecord> upgradeRecords,
        List<EncouragementRecord> encouragementRecords,
        List<PromotionIncentiveRecord> promotionIncentiveRecords
) {
}