package com.safwat.hr.controller.employee.dto;

import com.safwat.hr.controller.employee.enums.TerminationReason;
import com.safwat.hr.controller.scale.scale.dto.EncouragementRecord;
import com.safwat.hr.controller.scale.scale.dto.PromotionIncentiveRecord;
import com.safwat.hr.controller.scale.scale.dto.UpgradeRecord;

import java.time.LocalDate;
import java.util.List;

/**
 * الـ DTO المجمّع للموظف — يشمل كل السجلات التاريخية.
 *
 * البيانات الحالية (current*) محسوبة بالباك بـ floor lookup على تاريخ اليوم.
 *
 * الـ history lists الجديدة (promotions / encouragements / promotionIncentives)
 * مصدرها ScaleDto اللي في الباك — الباك يجمعها ويرجعها هنا عشان الفرونت
 * ما يعملش استدعاءين منفصلين.
 */
public record EmployeeProfileDto(
        Long            id,
        String          employeeNumber,
        String          fullName,
        String          nationalId,
        LocalDate       hireDate,
        LocalDate       terminationDate,
        TerminationReason terminationReason,
        Long            sectorId,
        String          sectorNameAr,

        // الحالة الاجتماعية الحالية
        String          currentSocialStatusCode,
        String          currentSocialStatusLabelAr,

        // الوظيفة الحالية
        Long            currentJobTitleId,
        String          currentJobTitleNameAr,

        // السجلات القابلة للتعديل
        List<SocialStatusEntryDto>        socialStatusHistory,
        List<JobTitleEntryDto>            jobTitleHistory,

        // السجلات للعرض فقط — مصدرها ScaleDto
        List<UpgradeRecord> upgradeRecords,
        List<EncouragementRecord> encouragementRecords,
        List<PromotionIncentiveRecord> promotionIncentiveRecords
) {}
