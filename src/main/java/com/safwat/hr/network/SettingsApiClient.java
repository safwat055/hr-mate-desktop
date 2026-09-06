package com.safwat.hr.network;

import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * API Client خاص بشاشة إعدادات التطبيق.
 * يُغلّف كل endpoints الـ /api/settings ويستخدم ApiClient الموجود.
 */
public class SettingsApiClient {

    private static final String BASE = "/settings";

    // ══════════════════════════════════════════════
    //  DTOs
    // ══════════════════════════════════════════════

    public record PropertyEntry(
            String key,
            String rawValue,
            String resolvedValue,
            String envVarName,
            String defaultValue,
            String parentKey,
            String category,
            String comment,
            int lineOrder,
            boolean envBound,
            boolean nested,
            boolean editable,
            boolean sensitive,
            boolean commentOnly,
            boolean blank,
            String displayValue
    ) {
    }

    public record UpdateRequest(String value) {
    }

    public record AddRequest(String key, String value, String category, String comment) {
    }

    public record BatchUpdateRequest(Map<String, String> updates) {
    }

    public record RestoreRequest(String backupFileName) {
    }

    // ══════════════════════════════════════════════
    //  Sync Methods
    // ══════════════════════════════════════════════

    public static ApiResponse<List<PropertyEntry>> getAll()
            throws IOException, InterruptedException {
        return ApiClient.getWithTypeRef(BASE,
                new TypeReference<List<PropertyEntry>>() {
                });
    }

    public static ApiResponse<Map<String, List<PropertyEntry>>> getGrouped()
            throws IOException, InterruptedException {
        return ApiClient.getWithTypeRef(BASE + "/grouped",
                new TypeReference<Map<String, List<PropertyEntry>>>() {
                });
    }

    public static ApiResponse<PropertyEntry> getOne(String key)
            throws IOException, InterruptedException {
        return ApiClient.get(BASE + "/" + key, PropertyEntry.class);
    }

    public static ApiResponse<Map<String, Object>> getFileInfo()
            throws IOException, InterruptedException {
        return ApiClient.getWithTypeRef(BASE + "/file-info",
                new TypeReference<Map<String, Object>>() {
                });
    }

    public static ApiResponse<PropertyEntry> update(String key, String newValue)
            throws IOException, InterruptedException {
        return ApiClient.put(BASE + "/" + key, new UpdateRequest(newValue), PropertyEntry.class);
    }

    public static ApiResponse<List<PropertyEntry>> batchUpdate(Map<String, String> updates)
            throws IOException, InterruptedException {
        return ApiClient.post(BASE + "/batch", new BatchUpdateRequest(updates),
                new TypeReference<List<PropertyEntry>>() {
                });
    }

    public static ApiResponse<PropertyEntry> add(String key, String value,
                                                 String category, String comment)
            throws IOException, InterruptedException {
        return ApiClient.post(BASE, new AddRequest(key, value, category, comment),
                PropertyEntry.class);
    }

    public static ApiResponse<Void> delete(String key)
            throws IOException, InterruptedException {
        return ApiClient.delete(BASE + "/" + key, Void.class);
    }

    public static ApiResponse<String> backup()
            throws IOException, InterruptedException {
        return ApiClient.post(BASE + "/backup", null, String.class);
    }

    public static ApiResponse<List<String>> listBackups()
            throws IOException, InterruptedException {
        return ApiClient.getWithTypeRef(BASE + "/backups",
                new TypeReference<List<String>>() {
                });
    }

    public static ApiResponse<Void> restore(String backupFileName)
            throws IOException, InterruptedException {
        return ApiClient.post(BASE + "/restore",
                new RestoreRequest(backupFileName), Void.class);
    }

    // ══════════════════════════════════════════════
    //  Async Methods — كلها بنفس النمط
    // ══════════════════════════════════════════════

    public static CompletableFuture<ApiResponse<Map<String, List<PropertyEntry>>>> getGroupedAsync() {
        return async(() -> getGrouped());
    }

    public static CompletableFuture<ApiResponse<Map<String, Object>>> getFileInfoAsync() {
        return async(() -> getFileInfo());
    }

    public static CompletableFuture<ApiResponse<PropertyEntry>> updateAsync(
            String key, String value) {
        return async(() -> update(key, value));
    }

    public static CompletableFuture<ApiResponse<List<PropertyEntry>>> batchUpdateAsync(
            Map<String, String> updates) {
        return async(() -> batchUpdate(updates));
    }

    public static CompletableFuture<ApiResponse<PropertyEntry>> addAsync(
            String key, String value, String category, String comment) {
        return async(() -> add(key, value, category, comment));
    }

    public static CompletableFuture<ApiResponse<Void>> deleteAsync(String key) {
        return async(() -> delete(key));
    }

    public static CompletableFuture<ApiResponse<String>> backupAsync() {
        return async(() -> backup());
    }

    public static CompletableFuture<ApiResponse<List<String>>> listBackupsAsync() {
        return async(() -> listBackups());
    }

    public static CompletableFuture<ApiResponse<Void>> restoreAsync(String backupFileName) {
        return async(() -> restore(backupFileName));
    }

    // ══════════════════════════════════════════════
    //  Helper — يلف أي call في CompletableFuture
    // ══════════════════════════════════════════════

    @FunctionalInterface
    private interface CheckedSupplier<T> {
        T get() throws Exception;
    }

    private static <T> CompletableFuture<ApiResponse<T>> async(CheckedSupplier<ApiResponse<T>> supplier) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return supplier.get();
            } catch (Exception e) {
                ApiResponse<T> err = new ApiResponse<>();
                err.setSuccess(false);
                err.setMessage(e.getMessage());
                return err;
            }
        });
    }
}