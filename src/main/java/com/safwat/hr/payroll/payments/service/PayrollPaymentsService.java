package com.safwat.hr.payroll.payments.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.safwat.hr.network.ApiClient;
import com.safwat.hr.network.ApiEndpoints;
import com.safwat.hr.network.ApiResponse;
import com.safwat.hr.payroll.dto.PaymentsView;
import com.safwat.hr.payroll.dto.SearchEmp;
import com.safwat.hr.shared.PayrollRequest;
import com.safwat.hr.ui.controls.SAFNotification;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Slf4j
public class PayrollPaymentsService {

    private static PayrollPaymentsService instance;

    private PayrollPaymentsService() {

    }

    public static PayrollPaymentsService getInstance() {
        if (instance == null) {
            instance = new PayrollPaymentsService();
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

    public ApiResponse<PaymentsView> getPaymentsData(PayrollRequest request) {
        try {
            return ApiClient.post(
                    ApiEndpoints.PayrollYearly.EMPLOYEE_RECORD,
                    request,
                    PaymentsView.class
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


}
