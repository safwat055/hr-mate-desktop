package com.safwat.hr.network;

import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * ══════════════════════════════════════════════════════════════════
 * ApiClient — بوابة HTTP الرئيسية للتطبيق
 * ══════════════════════════════════════════════════════════════════
 * <p>
 * ⭐ كل الدوال القديمة (بدون Duration) فضلت زي ما هي بالظبط، وبتستخدم
 * {@link HttpCore#TIMEOUT} الافتراضي (45 ثانية) — مفيش أي تغيير في سلوك
 * أي endpoint تاني في التطبيق.
 * <p>
 * أُضيفت overloads جديدة بتقبل {@link Duration} مخصص لاستخدامها في
 * العمليات الطويلة (زي {@code BackupService}).
 */
public final class ApiClient {

    private ApiClient() {
    }

    private static HttpCore core() {
        return HttpCore.getInstance();
    }

    // ─────────────────────────────────────────────
    //  Auth Token — Forwarded to HttpCore
    // ─────────────────────────────────────────────

    public static String getAuthToken() {
        return core().getAuthToken();
    }

    public static void setAuthToken(String tok) {
        core().setAuthToken(tok);
    }

    public static void clearAuthToken() {
        core().clearAuthToken();
    }

    // ─────────────────────────────────────────────
    //  URL Accessors
    // ─────────────────────────────────────────────

    public static String getBaseUrl() {
        return core().getBaseUrl();
    }

    public static String getBaseWsUrl() {
        return core().getBaseWsUrl();
    }

    // ─────────────────────────────────────────────
    //  GET
    // ─────────────────────────────────────────────

    public static <T> ApiResponse<T> get(String path, Class<T> responseType)
            throws IOException, InterruptedException {
        return send(path, "GET", null, responseType);
    }

    public static <T> ApiResponse<T> get(String path,
                                         Map<String, String> queryParams,
                                         Class<T> responseType)
            throws IOException, InterruptedException {
        return send(core().appendQueryParams(path, queryParams), "GET", null, responseType);
    }

    public static <T> ApiResponse<T> getWithTypeRef(String path,
                                                    TypeReference<T> responseType)
            throws IOException, InterruptedException {
        return sendRef(path, "GET", null, responseType);
    }

    public static <T> ApiResponse<T> getWithTypeRef(String path,
                                                    Map<String, String> queryParams,
                                                    TypeReference<T> responseType)
            throws IOException, InterruptedException {
        return sendRef(core().appendQueryParams(path, queryParams), "GET", null, responseType);
    }

    /**
     * ⭐ جديد: GET بـ timeout مخصص + TypeReference.
     */
    public static <T> ApiResponse<T> getWithTypeRef(String path,
                                                    Duration timeout,
                                                    TypeReference<T> responseType)
            throws IOException, InterruptedException {
        return sendRef(path, "GET", null, timeout, responseType);
    }

    // ─────────────────────────────────────────────
    //  POST
    // ─────────────────────────────────────────────

    public static <T> ApiResponse<T> post(String path,
                                          Object body,
                                          Class<T> responseType)
            throws IOException, InterruptedException {
        return send(path, "POST", body, responseType);
    }

    public static <T> ApiResponse<T> post(String path,
                                          Object body,
                                          Map<String, String> queryParams,
                                          Class<T> responseType)
            throws IOException, InterruptedException {
        return send(core().appendQueryParams(path, queryParams), "POST", body, responseType);
    }

    public static <T> ApiResponse<T> post(String path,
                                          Object body,
                                          TypeReference<T> responseType)
            throws IOException, InterruptedException {
        return sendRef(path, "POST", body, responseType);
    }

    /**
     * ⭐ جديد: POST بـ timeout مخصص.
     */
    public static <T> ApiResponse<T> post(String path,
                                          Object body,
                                          Duration timeout,
                                          Class<T> responseType)
            throws IOException, InterruptedException {
        return send(path, "POST", body, timeout, responseType);
    }

    // ─────────────────────────────────────────────
    //  PUT
    // ─────────────────────────────────────────────

    public static <T> ApiResponse<T> put(String path,
                                         Object body,
                                         Class<T> responseType)
            throws IOException, InterruptedException {
        return send(path, "PUT", body, responseType);
    }

    // ─────────────────────────────────────────────
    //  DELETE
    // ─────────────────────────────────────────────

    public static <T> ApiResponse<T> delete(String path,
                                            Class<T> responseType)
            throws IOException, InterruptedException {
        return send(path, "DELETE", null, responseType);
    }

    public static <T> ApiResponse<T> delete(String path,
                                            Map<String, String> queryParams,
                                            Class<T> responseType)
            throws IOException, InterruptedException {
        return send(core().appendQueryParams(path, queryParams), "DELETE", null, responseType);
    }

    public static <T> ApiResponse<T> delete(String path,
                                            Object body,
                                            Class<T> responseType)
            throws IOException, InterruptedException {
        return send(path, "DELETE", body, responseType);
    }

    // ─────────────────────────────────────────────
    //  Form POST
    // ─────────────────────────────────────────────

    public static <T> ApiResponse<T> postForm(String path,
                                              Map<String, String> formData,
                                              Class<T> responseType)
            throws IOException, InterruptedException {
        HttpCore c = core();
        HttpRequest request = c.addAuthHeader(
                        HttpRequest.newBuilder()
                                .uri(java.net.URI.create(c.getBaseUrl() + path))
                                .header("Content-Type", "application/x-www-form-urlencoded")
                                .header("Accept", "application/json")
                                .timeout(HttpCore.TIMEOUT)
                                .POST(HttpRequest.BodyPublishers.ofString(
                                        c.buildFormData(formData), StandardCharsets.UTF_8)))
                .build();

        HttpResponse<String> response = c.httpClient.send(
                request, HttpResponse.BodyHandlers.ofString());
        return c.parseResponse(response, responseType);
    }

    // ─────────────────────────────────────────────
    //  Async Wrappers
    // ─────────────────────────────────────────────

    public static <T> CompletableFuture<ApiResponse<T>> getAsync(String path,
                                                                 Class<T> responseType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return get(path, responseType);
            } catch (Exception e) {
                return core().createErrorResponse(e);
            }
        });
    }

    public static <T> CompletableFuture<ApiResponse<T>> getAsync(String path,
                                                                 Map<String, String> queryParams,
                                                                 Class<T> responseType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return get(path, queryParams, responseType);
            } catch (Exception e) {
                return core().createErrorResponse(e);
            }
        });
    }

    public static <T> CompletableFuture<ApiResponse<T>> getAsync(String path,
                                                                 Map<String, String> queryParams,
                                                                 TypeReference<T> responseType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getWithTypeRef(path, queryParams, responseType);
            } catch (Exception e) {
                return core().createErrorResponse(e);
            }
        });
    }

    /**
     * ⭐ جديد: نسخة async من GET+TypeReference بـ timeout مخصص.
     */
    public static <T> CompletableFuture<ApiResponse<T>> getAsync(String path,
                                                                 Duration timeout,
                                                                 TypeReference<T> responseType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getWithTypeRef(path, timeout, responseType);
            } catch (Exception e) {
                return core().createErrorResponse(e);
            }
        });
    }

    public static <T> CompletableFuture<ApiResponse<T>> postAsync(String path,
                                                                  Object body,
                                                                  Class<T> responseType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return post(path, body, responseType);
            } catch (Exception e) {
                return core().createErrorResponse(e);
            }
        });
    }

    /**
     * ⭐ جديد: نسخة async من POST بـ timeout مخصص.
     */
    public static <T> CompletableFuture<ApiResponse<T>> postAsync(String path,
                                                                  Object body,
                                                                  Duration timeout,
                                                                  Class<T> responseType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return post(path, body, timeout, responseType);
            } catch (Exception e) {
                return core().createErrorResponse(e);
            }
        });
    }

    public static <T> CompletableFuture<ApiResponse<T>> postFormAsync(String path,
                                                                      Map<String, String> formData,
                                                                      Class<T> responseType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return postForm(path, formData, responseType);
            } catch (Exception e) {
                return core().createErrorResponse(e);
            }
        });
    }

    // ─────────────────────────────────────────────
    //  Core Send Helpers (private)
    // ─────────────────────────────────────────────

    private static <T> ApiResponse<T> send(String path,
                                           String method,
                                           Object body,
                                           Class<T> responseType)
            throws IOException, InterruptedException {
        return send(path, method, body, HttpCore.TIMEOUT, responseType);
    }

    /**
     * ⭐ جديد: نفس المنطق، بـ timeout قابل للتخصيص.
     */
    private static <T> ApiResponse<T> send(String path,
                                           String method,
                                           Object body,
                                           Duration timeout,
                                           Class<T> responseType)
            throws IOException, InterruptedException {
        HttpCore c = core();
        HttpRequest.Builder builder = c.baseBuilder(path, timeout);
        c.applyMethod(builder, method, body);
        HttpResponse<String> response = c.httpClient.send(
                c.addAuthHeader(builder).build(),
                HttpResponse.BodyHandlers.ofString());
        return c.parseResponse(response, responseType);
    }

    private static <T> ApiResponse<T> sendRef(String path,
                                              String method,
                                              Object body,
                                              TypeReference<T> responseType)
            throws IOException, InterruptedException {
        return sendRef(path, method, body, HttpCore.TIMEOUT, responseType);
    }

    /**
     * ⭐ جديد: نفس المنطق، بـ timeout قابل للتخصيص.
     */
    private static <T> ApiResponse<T> sendRef(String path,
                                              String method,
                                              Object body,
                                              Duration timeout,
                                              TypeReference<T> responseType)
            throws IOException, InterruptedException {
        HttpCore c = core();
        HttpRequest.Builder builder = c.baseBuilder(path, timeout);
        c.applyMethod(builder, method, body);
        HttpResponse<String> response = c.httpClient.send(
                c.addAuthHeader(builder).build(),
                HttpResponse.BodyHandlers.ofString());
        return c.parseResponse(response, responseType);
    }
}