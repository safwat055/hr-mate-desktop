package com.safwat.hr.controller.payroll.payrollApi;

import com.fasterxml.jackson.core.type.TypeReference;
import com.safwat.hr.controller.payroll.payrollApi.dto.ChangeCardView;
import com.safwat.hr.controller.payroll.payrollApi.dto.SearchEmp;
import com.safwat.hr.network.ApiClient;
import com.safwat.hr.network.ApiEndpoints;
import com.safwat.hr.shared.PayrollRequest;
import com.safwat.hr.ui.controls.SAFNotification;

import java.io.IOException;
import java.nio.file.Path;
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
                    ApiEndpoints.PayrollChange.EMPLOYEE_MONTHS,
                    null,
                    new TypeReference<List<String>>() {
                    }
            ).getData();
        } catch (IOException | InterruptedException e) {
            SAFNotification.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }


    public List<SearchEmp> searchInEmployees(PayrollRequest request) {
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

    /**
     *
     * @param request
     * @return
     */
    public ChangeCardView getChangeCardDataView(PayrollRequest request) {

        try {
            return ApiClient.post(
                    ApiEndpoints.PayrollChange.EMPLOYEE_RECORD,
                    request,
                    ChangeCardView.class

            ).getData();

        } catch (IOException | InterruptedException e) {
            SAFNotification.error(e.getMessage());
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

        try {

            return ApiClient.downloadFileViaPostWithBody(
                    ApiEndpoints.PayrollChange.DOWNLOAD_CARD,
                    request,
                    targetPath
            );
        } catch (IOException | InterruptedException e) {
            SAFNotification.error(e.getMessage());

            return false;

        }
    }

    public int updateEmployeeMonthNote(PayrollRequest request) {
        try {
            return ApiClient.post(
                    ApiEndpoints.PayrollChange.UPDATE_NOTE,
                    request,
                    Integer.class
            ).getData();
        } catch (IOException | InterruptedException e) {
            SAFNotification.error(e.getMessage());
            return 0;
        }

    }
}
