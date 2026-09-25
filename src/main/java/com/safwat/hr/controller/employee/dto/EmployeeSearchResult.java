package com.safwat.hr.controller.employee.dto;

/**
 * نتيجة بحث مبسطة ترجع من GET /api/employees/search?q=...
 * بتحتوي بس على البيانات الكافية لعرضها في SearchDialog.
 */
public record EmployeeSearchResult(
        Long   id,
        String employeeNumber,
        String fullName,
        String nationalId
) {}
