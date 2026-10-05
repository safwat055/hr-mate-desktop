package com.safwat.hr.controller.employee.dto;

import com.safwat.hr.controller.employee.enums.TerminationReason;
import com.safwat.hr.controller.scale.scale.dto.EncouragementRecord;
import com.safwat.hr.controller.scale.scale.dto.PromotionIncentiveRecord;
import com.safwat.hr.controller.scale.scale.dto.UpgradeRecord;

import java.time.LocalDate;
import java.util.List;

/**
 * الـ DTO المجمّع للموظف — يشمل كل السجلات التاريخية.
 * <p>
 * البيانات الحالية (current*) محسوبة بالباك بـ floor lookup على تاريخ اليوم.
 * <p>
 * الـ history lists الجديدة (promotions / encouragements / promotionIncentives)
 * مصدرها ScaleDto اللي في الباك — الباك يجمعها ويرجعها هنا عشان الفرونت
 * ما يعملش استدعاءين منفصلين.
 */
public record EmployeeProfileDto(
        // ... الحقول الحالية
        Long id,
        String employeeNumber,
        String fullName,
        String nationalId,
        LocalDate hireDate,
        LocalDate terminationDate,
        TerminationReason terminationReason,
        Long sectorId,
        String sectorNameAr,

        // جديد — إعاقة
        LocalDate disabilityStartDate,
        String disabilityDecisionNo,

        // جديد — محافظة نائية
        boolean worksInRemoteGovernorate,

        // ... باقي الحقول الحالية (الحالة الاجتماعية، الوظيفة، السجلات)
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