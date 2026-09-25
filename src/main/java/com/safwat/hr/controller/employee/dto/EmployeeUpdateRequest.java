package com.safwat.hr.controller.employee.dto;

import com.safwat.hr.controller.employee.enums.TerminationReason;

import java.time.LocalDate;

/**
 * employeeNumber غير قابل للتعديل بعد الإنشاء.
 * sectorId اختياري — لو null، الباك مش هيغيّر القطاع.
 * (يتخطاه عشان يسمح للموظفين المستوردين من النظام القديم بتعيين قطاع لاحقاً)
 */
public record EmployeeUpdateRequest(
        String fullName,
        String nationalId,
        LocalDate hireDate,
        LocalDate terminationDate,
        TerminationReason terminationReason,
        Long sectorId
) {}