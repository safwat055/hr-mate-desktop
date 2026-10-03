package com.safwat.hr.controller.payroll.payrollApi;

import com.fasterxml.jackson.core.type.TypeReference;
import com.safwat.hr.controller.payroll.payrollApi.dto.EmployeeSearchResult;
import com.safwat.hr.controller.payroll.payrollApi.dto.PaymentsView;
import com.safwat.hr.controller.payroll.payrollManager.PayrollManagerController;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ⭐ ملاحظة threading: كل دوال الكلاس ده blocking — لازم تتنادّى من
 * background thread (SmartSearchHelper / UiAsync / ASYNC_EXECUTOR).
 * أي إشعار UI بيتعمل عبر {@link #fxError(String)} عشان يشتغل صح
 * سواء اتنادى من الـ FX thread أو من thread خلفي.
 */
@Slf4j
public class PayrollYearlyApi {

    private static PayrollYearlyApi instance;

    private PayrollYearlyApi() {
    }

    public static synchronized PayrollYearlyApi getInstance() {
        if (instance == null) {
            instance = new PayrollYearlyApi();
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

    public List<String> getAllMonthsForYearly() {
        try {
            return ApiClient.post(
                    ApiEndpoints.PayrollYearly.PAY_MONTHS_List,
                    null,
                    new TypeReference<List<String>>() {
                    }
            ).getData();
        } catch (IOException | InterruptedException e) {
            fxError(e.getMessage());
            log.error("getAllMonthsForYearly failed", e);
            return null;
        }
    }

    public Integer deleteFullMonthYearly(LocalDate payMonth) {
        try {
            Map<String, String> data = Map.of("payMonth", payMonth.toString());
            return ApiClient.delete(
                    ApiEndpoints.PayrollYearly.DELETE_ONE_MONTH,
                    data,
                    Integer.class
            ).getData();
        } catch (IOException | InterruptedException e) {
            fxError(e.getMessage());
            log.error("deleteFullMonthYearly failed", e);
            throw new RuntimeException(e);
        }
    }

    public List<String> getAvailablePayGroupForMonth(LocalDate payMonth) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("payMonth", payMonth != null ? payMonth.toString() : null);
            return ApiClient.post(
                    ApiEndpoints.PayrollYearly.PAY_GROUP_LIST_MONTH,
                    data,
                    new TypeReference<List<String>>() {
                    }
            ).getData();
        } catch (IOException | InterruptedException e) {
            fxError(e.getMessage());
            log.error("getAvailablePayGroupForMonth failed", e);
            return new ArrayList<>();
        }
    }

    public Integer deleteTargetGroupByMonth(LocalDate payMonth, String payGroup) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("payMonth", payMonth != null ? payMonth.toString() : null);
            data.put("payGroup", payGroup);
            return ApiClient.delete(
                    ApiEndpoints.PayrollYearly.DELETE_TARGET_PAY_GROUP,
                    data,
                    Integer.class
            ).getData();
        } catch (IOException | InterruptedException e) {
            fxError(e.getMessage());
            log.error("deleteTargetGroupByMonth failed", e);
            throw new RuntimeException(e);
        }
    }

    /** ⭐ البحث بـ SEARCH_TIMEOUT. */
    public List<EmployeeSearchResult> searchInEmployee(PayrollRequest request) {
        try {
            return ApiClient.post(
                    ApiEndpoints.PayrollYearly.SEARCH,
                    request,
                    HttpCore.SEARCH_TIMEOUT,
                    new TypeReference<List<EmployeeSearchResult>>() {
                    }
            ).getData();
        } catch (IOException | InterruptedException e) {
            fxError(e.getMessage());
            log.error("searchInEmployee failed", e);
            throw new RuntimeException(e);
        }
    }

    public List<String> getEmployeeMonths(PayrollRequest request) {
        try {
            return ApiClient.post(
                    ApiEndpoints.PayrollYearly.PAY_EMPLOYEE_MONTHS_List,
                    request,
                    new TypeReference<List<String>>() {
                    }
            ).getData();
        } catch (IOException | InterruptedException e) {
            fxError(e.getMessage());
            log.error("getEmployeeMonths failed", e);
            return new ArrayList<>();
        }
    }

    public Integer deleteMonthForEmployee(PayrollRequest request) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("nationalId", request.getNationalId());
            data.put("payMonth", request.getStartDate().toString());
            return ApiClient.delete(
                    ApiEndpoints.PayrollYearly.DELETE_TARGET_EMPLOYEE_MONTH,
                    data,
                    Integer.class
            ).getData();
        } catch (IOException | InterruptedException e) {
            fxError(e.getMessage());
            log.error("deleteMonthForEmployee failed", e);
            throw new RuntimeException(e);
        }
    }

    public List<String> getPayGroupForEmployeeInMonth(PayrollRequest request) {
        try {
            return ApiClient.post(
                    ApiEndpoints.PayrollYearly.PAY_GROUP_EMPLOYEE_LIST_MONTH,
                    request,
                    new TypeReference<List<String>>() {
                    }
            ).getData();
        } catch (IOException | InterruptedException e) {
            fxError(e.getMessage());
            log.error("getPayGroupForEmployeeInMonth failed", e);
            return new ArrayList<>();
        }
    }

    public Integer deletePayGroupInTargetMonthAndEmployee(PayrollRequest request) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("nationalId", request.getNationalId());
            data.put("payMonth", request.getStartDate().toString());
            data.put("payGroup", request.getPayGroup());

            return ApiClient.delete(
                    ApiEndpoints.PayrollYearly.DELETE_TARGET_GROUP_MONTH_EMPLOYEE,
                    data,
                    Integer.class
            ).getData();
        } catch (IOException | InterruptedException e) {
            fxError(e.getMessage());
            log.error("deletePayGroupInTargetMonthAndEmployee failed", e);
            throw new RuntimeException(e);
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
            fxError(e.getMessage());
            log.error("deleteOneEmployeeRecord failed", e);
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
            fxError(e.getMessage());
            log.error("updatePayGroupName failed", e);
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
            fxError(e.getMessage());
            log.error("updateEmployeeNote failed", e);
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
            fxError(e.getMessage());
            log.error("getPayGroup failed", e);
            return new ArrayList<>();
        }
    }

    public List<PayrollManagerController.GroupDescription> getDescriptions(PayrollRequest request) {
        try {
            return ApiClient.post(
                    ApiEndpoints.PayrollYearly.GET_DESCRIPTIONS,
                    request,
                    new TypeReference<List<PayrollManagerController.GroupDescription>>() {
                    }
            ).getData();
        } catch (IOException | InterruptedException e) {
            fxError(e.getMessage());
            log.error("getDescriptions failed", e);
            return new ArrayList<>();
        }
    }

    public PaymentsView getPaymentsData(PayrollRequest request) {
        try {
            return ApiClient.post(
                    ApiEndpoints.PayrollYearly.EMPLOYEE_RECORD,
                    request,
                    PaymentsView.class
            ).getData();
        } catch (IOException | InterruptedException e) {
            fxError(e.getMessage());
            log.error("getPaymentsData failed", e);
            throw new RuntimeException(e);
        }
    }

    public boolean downloadPaymentsPDF(PayrollRequest request, Path targetPath) {
        try {
            return FileTransferClient.downloadFileViaPost(
                    ApiEndpoints.PayrollYearly.DOWNLOAD_PAYMENTS,
                    request,
                    targetPath
            );
        } catch (IOException | InterruptedException e) {
            fxError(e.getMessage());
            log.error("downloadPaymentsPDF failed", e);
            return false;
        }
    }
}