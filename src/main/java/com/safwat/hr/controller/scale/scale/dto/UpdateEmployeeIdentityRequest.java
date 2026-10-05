package com.safwat.hr.controller.scale.scale.dto;

import lombok.Data;

/**
 * طلب تعديل البيانات الأساسية للموظف من شاشة السلم.
 */
@Data
public class UpdateEmployeeIdentityRequest {
    private String newNationalId;   // الرقم القومي الجديد (لو مش هيتغير، ابعته زي القديم)
    private String newCodeId;       // رقم الموظف الجديد
    private String newEmpName;      // الاسم الجديد
}