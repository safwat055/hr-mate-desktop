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
import java.util.List;

public class PayrollPaymentsService {


    public ApiResponse<List<SearchEmp>> searchInEmployees(PayrollRequest request) {
        try {
            return ApiClient.post(
                    ApiEndpoints.PAYROLL_YEARLY + "/get/searchEmployee",
                    request,
                    new TypeReference<List<SearchEmp>>() {
                    }

            );
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public ApiResponse<DTO.PaymentsView> getPaymentsData(PayrollRequest request) {
        try {
            return ApiClient.post(
                    ApiEndpoints.PAYROLL_YEARLY + "/get/employeeYearly",
                    request,
                    DTO.PaymentsView.class
            );
        } catch (IOException | InterruptedException e) {
            HRNotification.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
