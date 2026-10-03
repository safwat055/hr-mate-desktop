package com.safwat.hr.controller.payroll.payrollApi;

import com.fasterxml.jackson.core.type.TypeReference;
import com.safwat.hr.controller.payroll.payrollApi.dto.ChangeCardView;
import com.safwat.hr.controller.payroll.payrollApi.dto.EmployeeSearchResult;
import com.safwat.hr.network.ApiClient;
import com.safwat.hr.network.ApiEndpoints;
import com.safwat.hr.network.FileTransferClient;
import com.safwat.hr.network.HttpCore;
import com.safwat.hr.shared.PayrollRequest;
import com.safwat.hr.ui.controls.SAFNotification;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * ⭐ ملاحظة threading: كل دوال الكلاس ده blocking — لازم تتنادّى من
 * background thread (SmartSearchHelper / UiAsync / ASYNC_EXECUTOR).
 * أي إشعار UI بيتعمل عبر {@link #fxError(String)} عشان يشتغل صح
 * سواء اتنادى من الـ FX thread أو من thread خلفي.
 */
@Slf4j
public class PayrollChangeCardApi {

    private static PayrollChangeCardApi instance;

    private PayrollChangeCardApi() {
    }

    public static synchronized PayrollChangeCardApi getInstance() {
        if (instance == null) {
            instance = new PayrollChangeCardApi();
        }
        return instance;
    }

    /** يعرض إشعار خطأ على الـ FX thread بغض النظر عن الـ thread المنادي. */
    private static void fxError(String message) {
        if (Platform.isFxApplicationThread()) {
            SAFNotification.error(message);
        } else {
            Platform.runLater(() -> SAFNotification.error(message));
        }
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
            fxError(e.getMessage());
            return null;
        }
    }

    /** ⭐ البحث بـ SEARCH_TIMEOUT بدل الـ 45s الافتراضي. */
    public List<EmployeeSearchResult> searchInEmployees(PayrollRequest request) {
        try {
            return ApiClient.post(
                    ApiEndpoints.PayrollChange.SEARCH,
                    request,
                    HttpCore.SEARCH_TIMEOUT,
                    new TypeReference<List<EmployeeSearchResult>>() {
                    }
            ).getData();
        } catch (IOException | InterruptedException e) {
            fxError(e.getMessage());
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
            fxError(e.getMessage());
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
            fxError(e.getMessage());
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
            fxError(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public ChangeCardView getChangeCardDataView(PayrollRequest request) {
        try {
            return ApiClient.post(
                    ApiEndpoints.PayrollChange.EMPLOYEE_RECORD,
                    request,
                    ChangeCardView.class
            ).getData();
        } catch (IOException | InterruptedException e) {
            fxError(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public boolean downloadChangeCardPDF(PayrollRequest request, Path targetPath) {
        try {
            return FileTransferClient.downloadFileViaPost(
                    ApiEndpoints.PayrollChange.DOWNLOAD_CARD,
                    request,
                    targetPath
            );
        } catch (IOException | InterruptedException e) {
            fxError(e.getMessage());
            log.error("downloadChangeCardPDF failed", e);
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
            fxError(e.getMessage());
            return 0;
        }
    }
}