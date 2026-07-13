package com.safwat.hr.service.payroll;

import com.fasterxml.jackson.core.type.TypeReference;
import com.safwat.hr.service.payroll.dto.DTO;
import com.safwat.hr.service.payroll.dto.PayrollRequest;
import com.safwat.hr.service.payroll.dto.SearchEmp;
import com.safwat.hr.shared.ApiEndpoints;
import com.safwat.hr.ui.controls.HRNotification;
import com.safwat.hr.utils.ApiClient;
import com.safwat.hr.utils.ApiResponse;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class PayrollChangeService {


    public ApiResponse<List<SearchEmp>> searchInEmployee(PayrollRequest request) {
        try {
            return ApiClient.post(
                    ApiEndpoints.PAYROLL_CHANGE + "/get/searchEmployee",
                    request,
                    new TypeReference<List<SearchEmp>>() {
                    }
            );
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    public ApiResponse<DTO.ChangeCardView> getChangeCardData(PayrollRequest request) {

        try {
            return ApiClient.post(
                    ApiEndpoints.PAYROLL_CHANGE + "/get/employeeRecord",
                    request,
                    DTO.ChangeCardView.class

            );

        } catch (IOException | InterruptedException e) {
            HRNotification.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public boolean getChangeCardPDF(PayrollRequest request, Path targetPath) {
        // 4. تحميل الملف
        try {

            return ApiClient.downloadFileViaPostWithBody(
                    "/payrollChange/download-changeCard",  // المسار
                    request,                          // الـ Body
                    targetPath                        // مكان الحفظ
            );
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;

        }

    }
}
