package com.safwat.hr.controller.payroll.form5;

import com.fasterxml.jackson.core.type.TypeReference;
import com.safwat.hr.controller.payroll.form5.Form5Models.Data;
import com.safwat.hr.controller.payroll.form5.Form5Models.EmployeeHit;
import com.safwat.hr.controller.payroll.form5.Form5Models.ExportRequest;
import com.safwat.hr.network.ApiClient;
import com.safwat.hr.network.ApiResponse;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * المسارات نسبية (من غير /api) لأن ApiClient بيضيفه تلقائياً في getBaseUrl().
 */
public final class Form5Api {

    private static final String BASE = "/wages/form5";
    private static final Duration EXPORT_TIMEOUT = Duration.ofSeconds(120);

    private Form5Api() {
    }

    public static CompletableFuture<ApiResponse<List<EmployeeHit>>> searchAsync(String query) {
        return ApiClient.getAsync(
                BASE + "/search",
                
                Map.of("query", query == null ? "" : query),
                new TypeReference<List<EmployeeHit>>() {
                });
    }

    /**
     * بحث blocking للاستخدام مع SmartSearchHelper (على background thread).
     */
    public static List<EmployeeHit> search(String query) {
        ApiResponse<List<EmployeeHit>> resp = searchAsync(query).join();
        return resp.isSuccess() && resp.getData() != null ? resp.getData() : List.of();
    }

    public static CompletableFuture<ApiResponse<Data>> calculateAsync(String nationalId) {
        return ApiClient.getAsync(
                BASE + "/calculate",
                Map.of("nationalId", nationalId),
                Data.class);
    }

    public static CompletableFuture<byte[]> exportAsync(ExportRequest request) {
        return ApiClient.downloadBinaryPostAsync(
                BASE + "/export-edited",
                request,
                EXPORT_TIMEOUT);
    }
}