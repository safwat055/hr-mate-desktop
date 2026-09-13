package com.safwat.hr.controller.payroll.payrollApi;

import com.fasterxml.jackson.core.type.TypeReference;
import com.safwat.hr.controller.payroll.payrollApi.dto.SearchEmp;
import com.safwat.hr.controller.payroll.payrollApi.dto.ViewMainRecordForRangeDate;
import com.safwat.hr.controller.payroll.payrollApi.dto.ViewNonPrimaryRangeDate;
import com.safwat.hr.network.ApiClient;
import com.safwat.hr.network.ApiEndpoints;
import com.safwat.hr.network.FileTransferClient;
import com.safwat.hr.shared.PayrollRequest;
import com.safwat.hr.ui.controls.SAFNotification;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Slf4j
public class PayrollReviewApi {
    private static PayrollReviewApi instance;

    private PayrollReviewApi() {
    }

    public static PayrollReviewApi getInstance() {
        if (instance == null) {
            instance = new PayrollReviewApi();
        }
        return instance;
    }

    public List<String> getAllMonthForReview() {
        try {
            return ApiClient.post(
                    ApiEndpoints.PayrollReview.ALL_MONTHS_List,
                    null,
                    new TypeReference<List<String>>() {
                    }
            ).getData();
        } catch (IOException | InterruptedException e) {
            SAFNotification.error(e.getMessage());
            return null;

        }
    }


    @SneakyThrows
    public List<String> getAllKeys() {
        return ApiClient.post(
                ApiEndpoints.PayrollReview.ALL_GROUP_KEYS,
                null,
                new TypeReference<List<String>>() {
                }
        ).getData();
    }

    @SneakyThrows
    public List<String> getAllKeysForMonth(PayrollRequest request) {
        return ApiClient.post(
                ApiEndpoints.PayrollReview.MONTH_GROUP_KEYS,
                request,
                new TypeReference<List<String>>() {
                }
        ).getData();
    }

    @SneakyThrows
    public List<String> getEmployeeMonthKeys(PayrollRequest request) {
        return ApiClient.post(
                ApiEndpoints.PayrollReview.EMPLOYEE_MONTH_GROUP_KEYS,
                request,
                new TypeReference<List<String>>() {
                }
        ).getData();
    }

    @SneakyThrows
    public List<String> getEmployeeMonthsReview(PayrollRequest request) {
        return ApiClient.post(
                ApiEndpoints.PayrollReview.EMPLOYEE_MONTHS,
                request,
                new TypeReference<List<String>>() {
                }
        ).getData();
    }

    @SneakyThrows
    public List<SearchEmp> searchInEmployee(PayrollRequest request) {
        return ApiClient.post(
                ApiEndpoints.PayrollReview.SEARCH2,
                request,
                new TypeReference<List<SearchEmp>>() {
                }
        ).getData();
    }

    @SneakyThrows
    public Integer deleteFullMonthReview(PayrollRequest request) {
        return ApiClient.post(
                ApiEndpoints.PayrollReview.DELETE_MONTH_ALL,
                request,
                Integer.class
        ).getData();
    }

    @SneakyThrows
    public Integer deletePayGroupReview(PayrollRequest request) {
        return ApiClient.post(
                ApiEndpoints.PayrollReview.DELETE_GROUP_ALL,
                request,
                Integer.class
        ).getData();
    }

    @SneakyThrows
    public Integer deleteEmployeeMonthReview(PayrollRequest request) {
        return ApiClient.post(
                ApiEndpoints.PayrollReview.DELETE_EMPLOYEE_MONTH,
                request,
                Integer.class
        ).getData();
    }

    @SneakyThrows
    public Integer deleteEmployeePayGroup(PayrollRequest request) {
        return ApiClient.post(
                ApiEndpoints.PayrollReview.DELETE_EMPLOYEE_GROUP_MONTH,
                request,
                Integer.class
        ).getData();
    }

    @SneakyThrows
    public void updateKeysReviewAll() {
        ApiClient.postAsync(
                ApiEndpoints.PayrollReview.UPDATE_REVIEW_KEYS_ALL,
                null,
                Integer.class
        );
    }

    @SneakyThrows
    public void updateKeysReviewMonth(PayrollRequest request) {
        ApiClient.postAsync(
                ApiEndpoints.PayrollReview.UPDATE_REVIEW_KEYS_MONTH,
                request,
                Integer.class
        );
    }


    public ViewMainRecordForRangeDate getMainMonthRecords(PayrollRequest request) {

        try {
            return ApiClient.post(
                    ApiEndpoints.PayrollReview.MAIN_MONTH_RECORDS,
                    request,
                    ViewMainRecordForRangeDate.class
            ).getData();
        } catch (IOException | InterruptedException e) {
            SAFNotification.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public ViewNonPrimaryRangeDate getNonPrimaryMonthRecords(PayrollRequest request) {
        try {
            return ApiClient.post(
                    ApiEndpoints.PayrollReview.NON_PRIMARY_RECORDS,
                    request,
                    ViewNonPrimaryRangeDate.class
            ).getData();
        } catch (IOException | InterruptedException e) {
            SAFNotification.error(e.getMessage());
            throw new RuntimeException(e);
        }

    }

    public boolean downloadMainReviewReport(PayrollRequest request, Path filePath) {
        try {
            return FileTransferClient.downloadFileViaPost(
                    ApiEndpoints.PayrollReview.downloadReview,
                    request,
                    filePath

            );
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean downloadComparePDF(PayrollRequest request, Path targetPath) {
        try {
            return FileTransferClient.downloadFileViaPost(
                    ApiEndpoints.PayrollReview.downloadCompare,
                    request,
                    targetPath
            );
        } catch (IOException | InterruptedException e) {
            SAFNotification.error(e.getMessage());
            log.error(e.getMessage());
            return false;
        }
    }

    public boolean downloadCustomReviewPDF(PayrollRequest request, Path targetPath) {
        try {
            return FileTransferClient.downloadFileViaPost(
                    ApiEndpoints.PayrollReview.downloadCustomReview,
                    request,
                    targetPath
            );
        } catch (IOException | InterruptedException e) {
            SAFNotification.error(e.getMessage());
            log.error(e.getMessage());
            return false;
        }
    }

    public Long downloadRecord_129(PayrollRequest request) {
        try {
            return ApiClient.post(
                    ApiEndpoints.PayrollReview.FULL_RECORD_129,
                    request,
                    Long.class
            ).getData();
        } catch (IOException | InterruptedException e) {
            SAFNotification.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
