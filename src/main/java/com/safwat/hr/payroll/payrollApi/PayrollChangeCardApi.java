package com.safwat.hr.payroll.payrollApi;

import com.fasterxml.jackson.core.type.TypeReference;
import com.safwat.hr.network.ApiClient;
import com.safwat.hr.network.ApiEndpoints;
import com.safwat.hr.payroll.payrollApi.dto.SearchEmp;
import com.safwat.hr.shared.PayrollRequest;
import com.safwat.hr.ui.controls.SAFNotification;

import java.io.IOException;
import java.util.List;

public class PayrollChangeCardApi {

    private static PayrollChangeCardApi instance;

    private PayrollChangeCardApi() {
    }

    public static PayrollChangeCardApi getInstance() {
        if (instance == null) {
            instance = new PayrollChangeCardApi();
        }
        return instance;
    }


    public List<String> getAllMonthForChangeCard() {
        try {
            return ApiClient.post(
                    ApiEndpoints.PayrollChange.ALL_MONTHS_List,
                    null,
                    new TypeReference<List<String>>() {
                    }
            ).getData();
        } catch (IOException | InterruptedException e) {
            SAFNotification.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }


    public List<SearchEmp> getEmployeeInChangeCard(PayrollRequest request) {
        try {
            return ApiClient.post(
                    ApiEndpoints.PayrollChange.SEARCH,
                    request,
                    new TypeReference<List<SearchEmp>>() {
                    }
            ).getData();
        } catch (IOException | InterruptedException e) {
            SAFNotification.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public List<String> getEmployeeMonthsChangeCard(PayrollRequest request) {
        try {
            return ApiClient.post(
                    ApiEndpoints.PayrollChange.EMPLOYEE_MONTHS,
                    request,
                    new TypeReference<List<String>>() {
                    }
            ).getData();
        } catch (IOException | InterruptedException e) {
            SAFNotification.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public Integer deleteEmployeeMonthChangeCard(PayrollRequest request) {
        try {
            return ApiClient.post(
                    ApiEndpoints.PayrollChange.DELETE_EMPLOYEE_MONTH,
                    request,
                    Integer.class
            ).getData();
        } catch (IOException | InterruptedException e) {
            SAFNotification.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public Integer deleteFullMonthChangeCard(PayrollRequest request) {
        try {
            return ApiClient.post(
                    ApiEndpoints.PayrollChange.DELETE_MONTH,
                    request,
                    Integer.class
            ).getData();
        } catch (IOException | InterruptedException e) {
            SAFNotification.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
