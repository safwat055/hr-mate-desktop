package com.safwat.hr.service.payroll;

import com.fasterxml.jackson.core.type.TypeReference;
import com.safwat.hr.service.payroll.dto.PayrollRequest;
import com.safwat.hr.service.payroll.dto.SearchEmp;
import com.safwat.hr.ui.controls.SAFNotification;
import com.safwat.hr.utils.ApiClient;
import com.safwat.hr.utils.ApiEndpoints;
import com.safwat.hr.utils.ApiResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PayrollService {
    private static PayrollService instance;

    public static PayrollService getInstance() {
        if (instance == null) {
            instance = new PayrollService();
        }
        return instance;
    }

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

    public List<String> getPayGroup() {

        try {
            return ApiClient.post(
                    ApiEndpoints.PayrollYearly.PAY_GROUP_LIST,
                    null,
                    new TypeReference<List<String>>() {
                    }
            ).getData();
        } catch (IOException | InterruptedException e) {
            return new ArrayList<>();

        }
    }

    public List<String> getManagement() {
        try {

            PayrollRequest request = PayrollRequest.builder()
                    .searchValue("management")
                    .build();

            return ApiClient.post(
                    ApiEndpoints.PayrollYearly.PAY_MANAGEMENT_LIST,
                    request,
                    new TypeReference<List<String>>() {
                    }
            ).getData();
        } catch (Exception e) {
            return new ArrayList<>();
        }

    }

    public List<String> getAllMonthsYearly() {
        try {
            return ApiClient.post(
                    ApiEndpoints.PayrollYearly.PAY_MONTHS_List,
                    null,
                    new TypeReference<List<String>>() {
                    }
            ).getData();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
