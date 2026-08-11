package com.safwat.hr.payroll;

import com.fasterxml.jackson.core.type.TypeReference;
import com.safwat.hr.network.ApiClient;
import com.safwat.hr.network.ApiEndpoints;
import com.safwat.hr.network.ApiResponse;
import com.safwat.hr.payroll.dto.SearchEmp;
import com.safwat.hr.shared.PayrollRequest;
import com.safwat.hr.ui.controls.SAFNotification;

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

    public List<String> getAllElementNames() {
        try {
            return ApiClient.post(
                    ApiEndpoints.PayrollElement.GET_NAMES,
                    null,
                    new TypeReference<List<String>>() {
                    }
            ).getData();
        } catch (IOException | InterruptedException e) {
            return new ArrayList<>();

        }
    }

    public List<String> getAllElementCodes() {
        try {
            return ApiClient.post(
                    ApiEndpoints.PayrollElement.GET_CODES,
                    null,
                    new TypeReference<List<String>>() {
                    }
            ).getData();
        } catch (IOException | InterruptedException e) {
            return new ArrayList<>();

        }
    }
}
