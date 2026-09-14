package com.safwat.hr.controller.backendSetting;

import com.fasterxml.jackson.core.type.TypeReference;
import com.safwat.hr.network.ApiClient;
import com.safwat.hr.network.ApiResponse;
import com.safwat.hr.network.HttpCore;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * ══════════════════════════════════════════════════════════════════
 * AppConfigApiClient — إعدادات app_config.json
 * ══════════════════════════════════════════════════════════════════
 */
public final class AppConfigApiClient {

    private AppConfigApiClient() {
    }

    private static final String BASE = "/app-config";

    // ══════════════════════════════════════════════
    //  DTOs
    // ══════════════════════════════════════════════
    public record JsonEntry(
            String path,
            String key,
            String value,
            String type,
            String parentPath,
            String category,
            boolean editable,
            int depth
    ) {
    }

    public record UpdateRequest(String value) {
    }

    public record AddRequest(String path, String value, String type) {
    }

    // ══════════════════════════════════════════════
    //  Read
    // ══════════════════════════════════════════════
    public static ApiResponse<List<JsonEntry>> getAll()
            throws Exception {
        return ApiClient.getWithTypeRef(BASE, new TypeReference<List<JsonEntry>>() {
        });
    }

    public static ApiResponse<Map<String, List<JsonEntry>>> getGrouped()
            throws Exception {
        return ApiClient.getWithTypeRef(BASE + "/grouped",
                new TypeReference<Map<String, List<JsonEntry>>>() {
                });
    }

    public static ApiResponse<Map<String, Object>> getFileInfo()
            throws Exception {
        return ApiClient.getWithTypeRef(BASE + "/file-info",
                new TypeReference<Map<String, Object>>() {
                });
    }

    // ══════════════════════════════════════════════
    //  Write
    // ══════════════════════════════════════════════
    public static ApiResponse<JsonEntry> update(String path, String value)
            throws Exception {
        return ApiClient.put(BASE + "?path=" + encode(path),
                new UpdateRequest(value), JsonEntry.class);
    }

    public static ApiResponse<JsonEntry> add(String path, String value, String type)
            throws Exception {
        return ApiClient.post(BASE, new AddRequest(path, value, type), JsonEntry.class);
    }

    public static ApiResponse<Void> delete(String path) throws Exception {
        return ApiClient.delete(BASE + "?path=" + encode(path), Void.class);
    }

    // ══════════════════════════════════════════════
    //  Backup / Restore
    // ══════════════════════════════════════════════
    public static ApiResponse<String> backup() throws Exception {
        return ApiClient.post(BASE + "/backup", null, String.class);
    }

    public static ApiResponse<List<String>> listBackups() throws Exception {
        return ApiClient.getWithTypeRef(BASE + "/backups",
                new TypeReference<List<String>>() {
                });
    }

    public static ApiResponse<Void> restore(String fileName) throws Exception {
        return ApiClient.post(BASE + "/restore?fileName=" + encode(fileName),
                null, Void.class);
    }

    // ══════════════════════════════════════════════
    //  Async Wrappers
    // ══════════════════════════════════════════════
    public static CompletableFuture<ApiResponse<Map<String, List<JsonEntry>>>> getGroupedAsync() {
        return async(AppConfigApiClient::getGrouped);
    }

    public static CompletableFuture<ApiResponse<Map<String, Object>>> getFileInfoAsync() {
        return async(AppConfigApiClient::getFileInfo);
    }

    public static CompletableFuture<ApiResponse<JsonEntry>> updateAsync(String path, String value) {
        return async(() -> update(path, value));
    }

    public static CompletableFuture<ApiResponse<JsonEntry>> addAsync(String path,
                                                                     String value,
                                                                     String type) {
        return async(() -> add(path, value, type));
    }

    public static CompletableFuture<ApiResponse<Void>> deleteAsync(String path) {
        return async(() -> delete(path));
    }

    public static CompletableFuture<ApiResponse<String>> backupAsync() {
        return async(AppConfigApiClient::backup);
    }

    public static CompletableFuture<ApiResponse<List<String>>> listBackupsAsync() {
        return async(AppConfigApiClient::listBackups);
    }

    public static CompletableFuture<ApiResponse<Void>> restoreAsync(String fileName) {
        return async(() -> restore(fileName));
    }

    // ══════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════
    private static String encode(String value) {
        try {
            return java.net.URLEncoder.encode(value,
                    java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }

    @FunctionalInterface
    interface CheckedSupplier<T> {
        T get() throws Exception;
    }

    private static <T> CompletableFuture<ApiResponse<T>> async(CheckedSupplier<ApiResponse<T>> s) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return s.get();
            } catch (Exception e) {
                return HttpCore.getInstance().createErrorResponse(e);
            }
        });
    }
}