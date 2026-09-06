package com.safwat.hr.controller.payroll.payrollApi;

import com.fasterxml.jackson.core.type.TypeReference;
import com.safwat.hr.network.ApiClient;
import com.safwat.hr.network.ApiEndpoints;
import com.safwat.hr.controller.payroll.payrollApi.dto.PaymentsView;
import com.safwat.hr.controller.payroll.payrollApi.dto.SearchEmp;
import com.safwat.hr.controller.payroll.payrollManager.PayrollManagerController;
import com.safwat.hr.shared.PayrollRequest;
import com.safwat.hr.ui.controls.SAFNotification;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class PayrollYearlyApi {

    private static PayrollYearlyApi instance;

    private PayrollYearlyApi() {
    }

    public static PayrollYearlyApi getInstance() {
        if (instance == null) {
            instance = new PayrollYearlyApi();
        }
        return instance;
    }

    public List<String> getAllMonthsForYearly() throws IOException {
        try {
            return ApiClient.post(ApiEndpoints.PayrollYearly.PAY_MONTHS_List, null, new TypeReference<List<String>>() {
            }).getData();
        } catch (InterruptedException e) {
            SAFNotification.error(e.getMessage());
            return null;
        }
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
    public List<SearchEmp> searchInEmployee(PayrollRequest request) {
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

    public Integer deleteOneEmployeeRecord(PayrollRequest request) {
        try {
            return ApiClient.post(
                    ApiEndpoints.PayrollYearly.DELETE_ONE_EMPLOYEE_RECORD,
                    request,
                    Integer.class
            ).getData();
        } catch (IOException | InterruptedException e) {
            SAFNotification.error(e.getMessage());
            log.error(e.getMessage());
            return 0;
        }
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

    public Integer updateEmployeeNote(PayrollRequest request) {
        try {
            return ApiClient.post(
                    ApiEndpoints.PayrollYearly.UPDATE_EMPLOYEE_NOTE,
                    request,
                    Integer.class
            ).getData();
        } catch (IOException | InterruptedException e) {
            SAFNotification.error(e.getMessage());
            log.error(e.getMessage());
            return 0;
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


    public PaymentsView getPaymentsData(PayrollRequest request) {
        try {
            return ApiClient.post(
                    ApiEndpoints.PayrollYearly.EMPLOYEE_RECORD,
                    request,
                    PaymentsView.class
            ).getData();
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
            log.error(e.getMessage());
            return false;
        }
    }
}
