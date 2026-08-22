package com.safwat.hr.payroll.payrollManager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.safwat.hr.network.ApiClient;
import com.safwat.hr.network.ApiEndpoints;
import com.safwat.hr.payroll.dto.SearchEmp;
import com.safwat.hr.shared.PayrollRequest;
import com.safwat.hr.ui.controls.SAFNotification;
import lombok.SneakyThrows;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PayrollManagerApiService {

    private static PayrollManagerApiService instance;

    public static PayrollManagerApiService getInstance() {
        if (instance == null) {
            instance = new PayrollManagerApiService();
        }
        return instance;
    }

    @SneakyThrows
    public List<String> getAllMonthsForYearly() {
        return ApiClient.post(ApiEndpoints.PayrollYearly.PAY_MONTHS_List, null, new TypeReference<List<String>>() {
        }).getData();
    }

    @SneakyThrows
    public List<String> getAllMonthForReview() {
        return ApiClient.post(
                ApiEndpoints.PayrollReview.ALL_MONTHS_List,
                null,
                new TypeReference<List<String>>() {
                }
        ).getData();
    }

    @SneakyThrows
    public List<String> getAllMonthForChangeCard() {
        return ApiClient.post(
                ApiEndpoints.PayrollChange.ALL_MONTHS_List,
                null,
                new TypeReference<List<String>>() {
                }
        ).getData();
    }

    @SneakyThrows
    public Integer deleteFullMonthYearly(LocalDate payMonth) {
        Map<String, String> data = Map.of("payMonth", payMonth.toString());

        return ApiClient.delete(ApiEndpoints.PayrollYearly.DELETE_ONE_MONTH, data, Integer.class).getData();
    }

    @SneakyThrows
    public List<String> getAvailablePayGroupForMonth(LocalDate payMonth) {
        Map<String, String> data = new HashMap<>();
        data.put("payMonth", payMonth != null ? payMonth.toString() : null);
        return ApiClient.post(
                ApiEndpoints.PayrollYearly.PAY_GROUP_LIST_MONTH,
                data,
                new TypeReference<List<String>>() {
                }
        ).getData();
    }

    @SneakyThrows
    public Integer deleteTargetGroupByMonth(LocalDate payMonth, String payGroup) {
        Map<String, String> data = new HashMap<>();
        data.put("payMonth", payMonth != null ? payMonth.toString() : null);
        data.put("payGroup", payGroup);
        return ApiClient.delete(
                ApiEndpoints.PayrollYearly.DELETE_TARGET_PAY_GROUP,
                data,
                Integer.class
        ).getData();

    }

    @SneakyThrows
    public List<SearchEmp> getEmployeeInYearly(PayrollRequest request) {
        return ApiClient.post(
                ApiEndpoints.PayrollYearly.SEARCH,
                request,
                new TypeReference<List<SearchEmp>>() {
                }
        ).getData();
    }

    @SneakyThrows
    public List<String> getEmployeeMonths(PayrollRequest request) {
        return ApiClient.post(
                ApiEndpoints.PayrollYearly.PAY_EMPLOYEE_MONTHS_List,
                request,
                new TypeReference<List<String>>() {
                }
        ).getData();
    }

    @SneakyThrows
    public Integer deleteMonthForEmployee(PayrollRequest request) {
        Map<String, String> data = new HashMap<>();
        data.put("nationalId", request.getNationalId());
        data.put("payMonth", request.getStartDate().toString());
        return ApiClient.delete(
                ApiEndpoints.PayrollYearly.DELETE_TARGET_EMPLOYEE_MONTH,
                data,
                Integer.class
        ).getData();
    }

    @SneakyThrows
    public List<String> getPayGroupForEmployeeInMonth(PayrollRequest request) {
        return ApiClient.post(
                ApiEndpoints.PayrollYearly.PAY_GROUP_EMPLOYEE_LIST_MONTH,
                request,
                new TypeReference<List<String>>() {
                }
        ).getData();
    }

    @SneakyThrows
    public Integer deletePayGroupInTargetMonthAndEmployee(PayrollRequest request) {
        Map<String, String> data = new HashMap<>();
        data.put("nationalId", request.getNationalId());
        data.put("payMonth", request.getStartDate().toString());
        data.put("payGroup", request.getPayGroup());

        return ApiClient.delete(
                ApiEndpoints.PayrollYearly.DELETE_TARGET_GROUP_MONTH_EMPLOYEE,
                data,
                Integer.class
        ).getData();
    }


    public Integer updatePayGroupName(PayrollRequest request) {
        if (request.getPayGroup() == null || request.getPayGroup().isBlank()) {
            throw new RuntimeException("يجب تحديد مجموعة أولا");
        }
        if (request.getDescription() == null || request.getDescription().isBlank()) {
            throw new RuntimeException("يجب إدخال اسم جديد أولا");
        }

        try {
            return ApiClient.post(
                    ApiEndpoints.PayrollYearly.UPDATE_PAY_GROUP_NAME,
                    request,
                    Integer.class
            ).getData();
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

    @SneakyThrows
    public List<PayrollManagerController.GroupDescription> getDescriptions(PayrollRequest request) {
        return ApiClient.post(
                ApiEndpoints.PayrollYearly.GET_DESCRIPTIONS,
                request,
                new TypeReference<List<PayrollManagerController.GroupDescription>>() {
                }
        ).getData();
    }
}
