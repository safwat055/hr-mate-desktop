package com.safwat.hr.controller.payroll.payrollData;

import com.fasterxml.jackson.core.type.TypeReference;
import com.safwat.hr.network.ApiClient;
import com.safwat.hr.network.ApiResponse;
import com.safwat.hr.network.HttpCore;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * المسارات نسبية (من غير /api) لأن ApiClient بيضيفه.
 */
public final class PayrollDataApi {

    private static final String BASE = "/payroll-data";

    private PayrollDataApi() {
    }

    public record AddRequest(String name) {
    }

    public static CompletableFuture<ApiResponse<String>> lastPayNumberAsync() {
        return ApiClient.getAsync(BASE + "/last-pay-number", String.class);
    }

    public static CompletableFuture<ApiResponse<String>> newPayNumberAsync() {
        return ApiClient.getAsync(BASE + "/new-pay-number", String.class);
    }

    public static CompletableFuture<ApiResponse<String>> addUserManagementAsync(String name) {
        return ApiClient.postAsync(BASE + "/user-managements", new AddRequest(name), String.class);
    }

    /**
     * blocking: للاستخدام مع SmartSearchHelper (بيشتغل على background thread)
     */
    public static List<String> indexManagements() {
        return fetchList(BASE + "/index-managements");
    }

    /**
     * blocking: للاستخدام مع SmartSearchHelper
     */
    public static List<String> userManagements() {
        return fetchList(BASE + "/user-managements");
    }

    private static List<String> fetchList(String path) {
        ApiResponse<List<String>> resp = ApiClient
                .getAsync(path, HttpCore.TIMEOUT, new TypeReference<List<String>>() {
                })
                .join();
        return resp.isSuccess() && resp.getData() != null ? resp.getData() : List.of();
    }
}