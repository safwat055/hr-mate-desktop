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
                    ApiEndpoints.PayrollChange.SEARCH,
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
                    ApiEndpoints.PayrollChange.EMPLOYEE_RECORD,
                    request,
                    DTO.ChangeCardView.class

            );

        } catch (IOException | InterruptedException e) {
            HRNotification.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * use to
     *
     * @param request
     * @param targetPath
     * @return
     */
    public boolean downloadChangeCardPDF(PayrollRequest request, Path targetPath) {
        // 4. تحميل الملف
        try {

            return ApiClient.downloadFileViaPostWithBody(
                    ApiEndpoints.PayrollChange.DOWNLOAD_CARD,
                    request,
                    targetPath
            );
        } catch (IOException | InterruptedException e) {
            HRNotification.error(e.getMessage());
            e.printStackTrace();
            return false;

        }

    }
}
