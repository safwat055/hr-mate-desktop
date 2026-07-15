package com.safwat.hr.service.payroll;

import com.fasterxml.jackson.core.type.TypeReference;
import com.safwat.hr.service.payroll.dto.DTO;
import com.safwat.hr.service.payroll.dto.PayrollRequest;
import com.safwat.hr.service.payroll.dto.SearchEmp;
import com.safwat.hr.ui.controls.SAFNotification;
import com.safwat.hr.utils.ApiClient;
import com.safwat.hr.utils.ApiEndpoints;
import com.safwat.hr.utils.ApiResponse;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class PayrollPaymentsService {


    public ApiResponse<List<SearchEmp>> searchInEmployees(PayrollRequest request) {
        try {
            return ApiClient.post(
                    ApiEndpoints.PayrollYearly.SEARCH,
                    request,
                    new TypeReference<List<SearchEmp>>() {
                    }

            );
        } catch (IOException | InterruptedException e) {
            SAFNotification.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public ApiResponse<DTO.PaymentsView> getPaymentsData(PayrollRequest request) {
        try {
            return ApiClient.post(
                    ApiEndpoints.PayrollYearly.EMPLOYEE_RECORD,
                    request,
                    DTO.PaymentsView.class
            );
        } catch (IOException | InterruptedException e) {
            SAFNotification.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public boolean downloadPaymentsPDF(PayrollRequest request, Path targetPath) {
        try {
            return ApiClient.downloadFileViaPostWithBody(
                    ApiEndpoints.PayrollYearly.DOWNLOAD_PAYMENTS,
                    request,
                    targetPath
            );
        } catch (IOException | InterruptedException e) {
            SAFNotification.error(e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
