package com.safwat.hr.payroll.changeCard.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.safwat.hr.network.ApiClient;
import com.safwat.hr.network.ApiEndpoints;
import com.safwat.hr.network.ApiResponse;
import com.safwat.hr.payroll.dto.ChangeCardView;
import com.safwat.hr.payroll.dto.SearchEmp;
import com.safwat.hr.shared.PayrollRequest;
import com.safwat.hr.ui.controls.SAFNotification;

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


    public ApiResponse<ChangeCardView> getChangeCardData(PayrollRequest request) {

        try {
            return ApiClient.post(
                    ApiEndpoints.PayrollChange.EMPLOYEE_RECORD,
                    request,
                    ChangeCardView.class

            );

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
        // 4. تحميل الملف
        try {

            return ApiClient.downloadFileViaPostWithBody(
                    ApiEndpoints.PayrollChange.DOWNLOAD_CARD,
                    request,
                    targetPath
            );
        } catch (IOException | InterruptedException e) {
            SAFNotification.error(e.getMessage());
            e.printStackTrace();
            return false;

        }

    }

    public int updateNote(PayrollRequest request) {
        try {
            return ApiClient.post(
                    ApiEndpoints.PayrollChange.UPDATE_NOTE,
                    request,
                    Integer.class
            ).getData();
        } catch (IOException | InterruptedException e) {
            SAFNotification.error(e.getMessage());
            e.printStackTrace();
            return 0;
        }

    }

    public int deleteOneRecord(PayrollRequest request) {

        try {
            return ApiClient.post(
                    ApiEndpoints.PayrollChange.DELETE_RECORD,
                    request,
                    Integer.class
            ).getData();
        } catch (IOException | InterruptedException e) {
            SAFNotification.error(e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }
}
