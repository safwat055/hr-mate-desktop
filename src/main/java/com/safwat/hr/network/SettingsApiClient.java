package com.safwat.hr.network;

import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * API Client خاص بشاشة إعدادات التطبيق.
 * يُغلّف كل endpoints الـ /api/settings ويستخدم ApiClient الموجود.
 * <p>
 * الاستخدام:
 * SettingsApiClient.getGrouped()
 * SettingsApiClient.update("app.storage.root", "./data")
 * SettingsApiClient.batchUpdate(Map.of("key1","val1"))
 */
public class SettingsApiClient {

    private static final String BASE = "/settings";

    // ══════════════════════════════════════════════
    //  DTOs داخلية (تعكس backend PropertiesDto)
    // ══════════════════════════════════════════════

    /**
     * يمثّل مفتاحًا واحدًا من الـ properties
     */
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
    //  Read
    // ══════════════════════════════════════════════

    /**
     * يجلب كل الإعدادات القابلة للتعديل كـ flat list
     * GET /api/settings
     */
    public static ApiResponse<List<PropertyEntry>> getAll()
            throws IOException, InterruptedException {
        return ApiClient.getWithTypeRef(BASE,
                new TypeReference<List<PropertyEntry>>() {
                });
    }

    /**
     * يجلب الإعدادات مجمّعة بالتصنيف — للـ Accordion
     * GET /api/settings/grouped
     * Response: Map<categoryName, List<PropertyEntry>>
     */
    public static ApiResponse<Map<String, List<PropertyEntry>>> getGrouped()
            throws IOException, InterruptedException {
        return ApiClient.getWithTypeRef(BASE + "/grouped",
                new TypeReference<Map<String, List<PropertyEntry>>>() {
                });
    }

    /**
     * مفتاح واحد بالاسم الكامل
     * GET /api/settings/{key}
     */
    public static ApiResponse<PropertyEntry> getOne(String key)
            throws IOException, InterruptedException {
        return ApiClient.get(BASE + "/" + key, PropertyEntry.class);
    }

    // ══════════════════════════════════════════════
    //  Write
    // ══════════════════════════════════════════════

    /**
     * تعديل مفتاح واحد
     * PUT /api/settings/{key}
     */
    public static ApiResponse<PropertyEntry> update(String key, String newValue)
            throws IOException, InterruptedException {
        return ApiClient.put(BASE + "/" + key, new UpdateRequest(newValue), PropertyEntry.class);
    }

    /**
     * تعديل عدة مفاتيح دفعة واحدة (زر "حفظ الكل")
     * PUT /api/settings/batch
     */
    public static ApiResponse<List<PropertyEntry>> batchUpdate(Map<String, String> updates)
            throws IOException, InterruptedException {
        return ApiClient.put(BASE + "/batch", new BatchUpdateRequest(updates),
                (Class<List<PropertyEntry>>) (Class<?>) List.class);
    }

    /**
     * إضافة مفتاح جديد
     * POST /api/settings
     */
    public static ApiResponse<PropertyEntry> add(String key, String value,
                                                 String category, String comment)
            throws IOException, InterruptedException {
        return ApiClient.post(BASE, new AddRequest(key, value, category, comment),
                PropertyEntry.class);
    }

    /**
     * حذف مفتاح
     * DELETE /api/settings/{key}
     */
    public static ApiResponse<Void> delete(String key)
            throws IOException, InterruptedException {
        return ApiClient.delete(BASE + "/" + key, Void.class);
    }

    // ══════════════════════════════════════════════
    //  Backup & Restore
    // ══════════════════════════════════════════════

    /**
     * عمل backup يدوي
     * POST /api/settings/backup
     */
    public static ApiResponse<String> backup()
            throws IOException, InterruptedException {
        return ApiClient.post(BASE + "/backup", null, String.class);
    }

    /**
     * قائمة الـ backups المتاحة
     * GET /api/settings/backups
     */
    public static ApiResponse<List<String>> listBackups()
            throws IOException, InterruptedException {
        return ApiClient.getWithTypeRef(BASE + "/backups",
                new TypeReference<List<String>>() {
                });
    }

    /**
     * استعادة backup بالاسم
     * POST /api/settings/restore
     */
    public static ApiResponse<Void> restore(String backupFileName)
            throws IOException, InterruptedException {
        return ApiClient.post(BASE + "/restore",
                new RestoreRequest(backupFileName), Void.class);
    }

    // ══════════════════════════════════════════════
    //  Async variants (تُستخدم من الـ Controller مباشرةً)
    // ══════════════════════════════════════════════

    public static java.util.concurrent.CompletableFuture<ApiResponse<Map<String, List<PropertyEntry>>>> getGroupedAsync() {
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try {
                return getGrouped();
            } catch (Exception e) {
                ApiResponse<Map<String, List<PropertyEntry>>> err = new ApiResponse<>();
                err.setSuccess(false);
                err.setMessage(e.getMessage());
                return err;
            }
        });
    }

    public static java.util.concurrent.CompletableFuture<ApiResponse<PropertyEntry>> updateAsync(
            String key, String value) {
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try {
                return update(key, value);
            } catch (Exception e) {
                ApiResponse<PropertyEntry> err = new ApiResponse<>();
                err.setSuccess(false);
                err.setMessage(e.getMessage());
                return err;
            }
        });
    }

    public static java.util.concurrent.CompletableFuture<ApiResponse<List<PropertyEntry>>> batchUpdateAsync(
            Map<String, String> updates) {
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try {
                return batchUpdate(updates);
            } catch (Exception e) {
                ApiResponse<List<PropertyEntry>> err = new ApiResponse<>();
                err.setSuccess(false);
                err.setMessage(e.getMessage());
                return err;
            }
        });
    }

    public static java.util.concurrent.CompletableFuture<ApiResponse<PropertyEntry>> addAsync(
            String key, String value, String category, String comment) {
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try {
                return add(key, value, category, comment);
            } catch (Exception e) {
                ApiResponse<PropertyEntry> err = new ApiResponse<>();
                err.setSuccess(false);
                err.setMessage(e.getMessage());
                return err;
            }
        });
    }

    public static java.util.concurrent.CompletableFuture<ApiResponse<Void>> deleteAsync(String key) {
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try {
                return delete(key);
            } catch (Exception e) {
                ApiResponse<Void> err = new ApiResponse<>();
                err.setSuccess(false);
                err.setMessage(e.getMessage());
                return err;
            }
        });
    }

    public static java.util.concurrent.CompletableFuture<ApiResponse<String>> backupAsync() {
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try {
                return backup();
            } catch (Exception e) {
                ApiResponse<String> err = new ApiResponse<>();
                err.setSuccess(false);
                err.setMessage(e.getMessage());
                return err;
            }
        });
    }

    public static java.util.concurrent.CompletableFuture<ApiResponse<List<String>>> listBackupsAsync() {
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try {
                return listBackups();
            } catch (Exception e) {
                ApiResponse<List<String>> err = new ApiResponse<>();
                err.setSuccess(false);
                err.setMessage(e.getMessage());
                return err;
            }
        });
    }

    public static java.util.concurrent.CompletableFuture<ApiResponse<Void>> restoreAsync(
            String backupFileName) {
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try {
                return restore(backupFileName);
            } catch (Exception e) {
                ApiResponse<Void> err = new ApiResponse<>();
                err.setSuccess(false);
                err.setMessage(e.getMessage());
                return err;
            }
        });
    }
}