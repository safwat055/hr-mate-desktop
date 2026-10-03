package com.safwat.hr.controller.payroll.payrollApi;

import com.fasterxml.jackson.core.type.TypeReference;
import com.safwat.hr.controller.payroll.payrollApi.dto.EmployeeSearchResult;
import com.safwat.hr.controller.payroll.payrollApi.dto.ViewMainRecordForRangeDate;
import com.safwat.hr.controller.payroll.payrollApi.dto.ViewNonPrimaryRangeDate;
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
public class PayrollReviewApi {
    private static PayrollReviewApi instance;

    private PayrollReviewApi() {
    }

    public static synchronized PayrollReviewApi getInstance() {
        if (instance == null) {
            instance = new PayrollReviewApi();
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

    public List<String> getAllMonthForReview() {
        try {
            return ApiClient.post(
                    ApiEndpoints.PayrollReview.ALL_MONTHS_List,
                    null,
                    new TypeReference<List<String>>() {
                    }
            ).getData();
        } catch (IOException | InterruptedException e) {
            fxError(e.getMessage());
            log.error("getAllMonthForReview failed", e);
            return null;
        }
    }

    public List<String> getAllKeys() {
        try {
            return ApiClient.post(
                    ApiEndpoints.PayrollReview.ALL_GROUP_KEYS,
                    null,
                    new TypeReference<List<String>>() {
                    }
            ).getData();
        } catch (IOException | InterruptedException e) {
            fxError(e.getMessage());
            log.error("getAllKeys failed", e);
            return null;
        }
    }

    public List<String> getAllKeysForMonth(PayrollRequest request) {
        try {
            return ApiClient.post(
                    ApiEndpoints.PayrollReview.MONTH_GROUP_KEYS,
                    request,
                    new TypeReference<List<String>>() {
                    }
            ).getData();
        } catch (IOException | InterruptedException e) {
            fxError(e.getMessage());
            log.error("getAllKeysForMonth failed", e);
            return null;
        }
    }

    public List<String> getEmployeeMonthKeys(PayrollRequest request) {
        try {
            return ApiClient.post(
                    ApiEndpoints.PayrollReview.EMPLOYEE_MONTH_GROUP_KEYS,
                    request,
                    new TypeReference<List<String>>() {
                    }
            ).getData();
        } catch (IOException | InterruptedException e) {
            fxError(e.getMessage());
            log.error("getEmployeeMonthKeys failed", e);
            return null;
        }
    }

    public List<String> getEmployeeMonthsReview(PayrollRequest request) {
        try {
            return ApiClient.post(
                    ApiEndpoints.PayrollReview.EMPLOYEE_MONTHS,
                    request,
                    new TypeReference<List<String>>() {
                    }
            ).getData();
        } catch (IOException | InterruptedException e) {
            fxError(e.getMessage());
            log.error("getEmployeeMonthsReview failed", e);
            return null;
        }
    }

    /** ⭐ البحث بـ SEARCH_TIMEOUT. */
    public List<EmployeeSearchResult> searchInEmployee(PayrollRequest request) {
        try {
            return ApiClient.post(
                    ApiEndpoints.PayrollReview.SEARCH2,
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

    public Integer deleteFullMonthReview(PayrollRequest request) {
        try {
            return ApiClient.post(
                    ApiEndpoints.PayrollReview.DELETE_MONTH_ALL,
                    request,
                    Integer.class
            ).getData();
        } catch (IOException | InterruptedException e) {
            fxError(e.getMessage());
            log.error("deleteFullMonthReview failed", e);
            throw new RuntimeException(e);
        }
    }

    public Integer deletePayGroupReview(PayrollRequest request) {
        try {
            return ApiClient.post(
                    ApiEndpoints.PayrollReview.DELETE_GROUP_ALL,
                    request,
                    Integer.class
            ).getData();
        } catch (IOException | InterruptedException e) {
            fxError(e.getMessage());
            log.error("deletePayGroupReview failed", e);
            throw new RuntimeException(e);
        }
    }

    public Integer deleteEmployeeMonthReview(PayrollRequest request) {
        try {
            return ApiClient.post(
                    ApiEndpoints.PayrollReview.DELETE_EMPLOYEE_MONTH,
                    request,
                    Integer.class
            ).getData();
        } catch (IOException | InterruptedException e) {
            fxError(e.getMessage());
            log.error("deleteEmployeeMonthReview failed", e);
            throw new RuntimeException(e);
        }
    }

    public Integer deleteEmployeePayGroup(PayrollRequest request) {
        try {
            return ApiClient.post(
                    ApiEndpoints.PayrollReview.DELETE_EMPLOYEE_GROUP_MONTH,
                    request,
                    Integer.class
            ).getData();
        } catch (IOException | InterruptedException e) {
            fxError(e.getMessage());
            log.error("deleteEmployeePayGroup failed", e);
            throw new RuntimeException(e);
        }
    }

    /** fire-and-forget — بيشتغل على ASYNC_EXECUTOR. */
    public void updateKeysReviewAll() {
        ApiClient.postAsync(
                ApiEndpoints.PayrollReview.UPDATE_REVIEW_KEYS_ALL,
                null,
                Integer.class
        ).whenComplete((resp, ex) -> {
            if (ex != null) {
                log.error("updateKeysReviewAll failed", ex);
            } else if (resp != null && !resp.isSuccess()) {
                log.warn("updateKeysReviewAll returned failure: {}", resp.getMessage());
            }
        });
    }

    /** fire-and-forget — بيشتغل على ASYNC_EXECUTOR. */
    public void updateKeysReviewMonth(PayrollRequest request) {
        ApiClient.postAsync(
                ApiEndpoints.PayrollReview.UPDATE_REVIEW_KEYS_MONTH,
                request,
                Integer.class
        ).whenComplete((resp, ex) -> {
            if (ex != null) {
                log.error("updateKeysReviewMonth failed", ex);
            } else if (resp != null && !resp.isSuccess()) {
                log.warn("updateKeysReviewMonth returned failure: {}", resp.getMessage());
            }
        });
    }

    public ViewMainRecordForRangeDate getMainMonthRecords(PayrollRequest request) {
        try {
            return ApiClient.post(
                    ApiEndpoints.PayrollReview.MAIN_MONTH_RECORDS,
                    request,
                    ViewMainRecordForRangeDate.class
            ).getData();
        } catch (IOException | InterruptedException e) {
            fxError(e.getMessage());
            log.error("getMainMonthRecords failed", e);
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
            fxError(e.getMessage());
            log.error("getNonPrimaryMonthRecords failed", e);
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
            fxError(e.getMessage());
            log.error("downloadMainReviewReport failed", e);
            return false;
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
            fxError(e.getMessage());
            log.error("downloadComparePDF failed", e);
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
            fxError(e.getMessage());
            log.error("downloadCustomReviewPDF failed", e);
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
            fxError(e.getMessage());
            log.error("downloadRecord_129 failed", e);
            throw new RuntimeException(e);
        }
    }
}