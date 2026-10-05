package com.safwat.hr.network;

import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * ══════════════════════════════════════════════════════════════════
 * ApiClient — بوابة HTTP الرئيسية للتطبيق
 * ══════════════════════════════════════════════════════════════════
 * <p>
 * ⭐ كل الدوال القديمة (بدون Duration) بتستخدم {@link HttpCore#TIMEOUT}
 * الافتراضي (45 ثانية).
 * <p>
 * ⭐ الدوال الـ blocking (get/post/put/delete) لازم ما تتنادّاش من
 * JavaFX Application Thread. من الـ UI استخدم نسخ {@code *Async}
 * (أو {@code UiAsync.run}) — كلها بتشتغل على {@link HttpCore#ASYNC_EXECUTOR}.
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
     * GET بـ timeout مخصص + TypeReference.
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
     * POST بـ timeout مخصص.
     */
    public static <T> ApiResponse<T> post(String path,
                                          Object body,
                                          Duration timeout,
                                          Class<T> responseType)
            throws IOException, InterruptedException {
        return send(path, "POST", body, timeout, responseType);
    }

    /**
     * ⭐ جديد: POST بـ timeout مخصص + TypeReference (للبحث وقوائم النتائج).
     */
    public static <T> ApiResponse<T> post(String path,
                                          Object body,
                                          Duration timeout,
                                          TypeReference<T> responseType)
            throws IOException, InterruptedException {
        return sendRef(path, "POST", body, timeout, responseType);
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

    public static void delete(String path)
            throws IOException, InterruptedException {
        send(path, "DELETE", null, HttpCore.TIMEOUT, (Class<Void>) null);
    }

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
    //  Async Wrappers  (كلها على HttpCore.ASYNC_EXECUTOR)
    // ─────────────────────────────────────────────

    /**
     * ⭐ نقطة التشغيل الوحيدة لكل الـ async wrappers:
     * بتشغّل النداء على ASYNC_EXECUTOR (مش ForkJoinPool.commonPool())
     * وبتحوّل أي exception لـ ApiResponse فاشل.
     */
    private static <R> CompletableFuture<ApiResponse<R>> async(Callable<ApiResponse<R>> call) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return call.call();
            } catch (Exception e) {
                return core().<R>createErrorResponse(e);
            }
        }, HttpCore.ASYNC_EXECUTOR);
    }

    // ── GET ──

    public static <T> CompletableFuture<ApiResponse<T>> getAsync(String path,
                                                                 Class<T> responseType) {
        return async(() -> get(path, responseType));
    }

    public static <T> CompletableFuture<ApiResponse<T>> getAsync(String path,
                                                                 Map<String, String> queryParams,
                                                                 Class<T> responseType) {
        return async(() -> get(path, queryParams, responseType));
    }

    public static <T> CompletableFuture<ApiResponse<T>> getAsync(String path,
                                                                 Map<String, String> queryParams,
                                                                 TypeReference<T> responseType) {
        return async(() -> getWithTypeRef(path, queryParams, responseType));
    }

    public static <T> CompletableFuture<ApiResponse<T>> getAsync(String path,
                                                                 Duration timeout,
                                                                 TypeReference<T> responseType) {
        return async(() -> getWithTypeRef(path, timeout, responseType));
    }

    // ── POST ──

    // ── POST ──
    public static <T> CompletableFuture<ApiResponse<T>> postAsync(String path,
                                                                  Object body,
                                                                  Class<T> responseType) {
        return async(() -> post(path, body, responseType));
    }

    public static <T> CompletableFuture<ApiResponse<T>> postAsync(String path,
                                                                  Object body,
                                                                  Duration timeout,
                                                                  Class<T> responseType) {
        return async(() -> post(path, body, timeout, responseType));
    }

    public static <T> CompletableFuture<ApiResponse<T>> postAsync(String path,
                                                                  Object body,
                                                                  TypeReference<T> responseType) {
        return async(() -> post(path, body, responseType));
    }

    public static <T> CompletableFuture<ApiResponse<T>> postAsync(String path,
                                                                  Object body,
                                                                  Duration timeout,
                                                                  TypeReference<T> responseType) {
        return async(() -> post(path, body, timeout, responseType));
    }

    public static <T> CompletableFuture<ApiResponse<T>> postFormAsync(String path,
                                                                      Map<String, String> formData,
                                                                      Class<T> responseType) {
        return async(() -> postForm(path, formData, responseType));
    }

    // ── PUT ★ جديد ──

    public static <T> CompletableFuture<ApiResponse<T>> putAsync(String path,
                                                                 Object body,
                                                                 Class<T> responseType) {
        return async(() -> put(path, body, responseType));
    }

    // ── DELETE ★ جديد ──

    public static <T> CompletableFuture<ApiResponse<T>> deleteAsync(String path,
                                                                    Class<T> responseType) {
        return async(() -> delete(path, responseType));
    }

    public static <T> CompletableFuture<ApiResponse<T>> deleteAsync(String path,
                                                                    Object body,
                                                                    Class<T> responseType) {
        return async(() -> delete(path, body, responseType));
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

    // ─────────────────────────────────────────────
    //  Binary Download
    // ─────────────────────────────────────────────

    /**
     * ينزّل ملف binary (PDF / XLSX / ...) من الـ endpoint.
     *
     * @param path المسار النسبي (بدون /api)
     * @return محتوى الملف كـ byte[]
     */
    public static byte[] downloadBinary(String path)
            throws IOException, InterruptedException {
        return downloadBinary(path, HttpCore.TIMEOUT);
    }

    /**
     * نفس {@link #downloadBinary(String)} لكن بـ timeout مخصص (تقارير طويلة).
     */
    public static byte[] downloadBinary(String path, Duration timeout)
            throws IOException, InterruptedException {
        HttpCore c = core();

        HttpRequest.Builder builder = c.addAuthHeader(
                HttpRequest.newBuilder()
                        .uri(java.net.URI.create(c.getBaseUrl() + path))
                        .header("Accept", "application/pdf, application/octet-stream, */*")
                        .timeout(timeout)
                        .GET()
        );

        HttpResponse<byte[]> response = c.httpClient.send(
                builder.build(),
                HttpResponse.BodyHandlers.ofByteArray()
        );

        if (response.statusCode() >= 400) {
            throw new IOException("فشل التحميل — HTTP " + response.statusCode());
        }

        byte[] body = response.body();
        if (body == null || body.length == 0) {
            throw new IOException("الملف فارغ");
        }
        return body;
    }

    public static CompletableFuture<byte[]> downloadBinaryAsync(String path) {
        return downloadBinaryAsync(path, HttpCore.TIMEOUT);
    }

    public static CompletableFuture<byte[]> downloadBinaryAsync(String path, Duration timeout) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return downloadBinary(path, timeout);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, HttpCore.ASYNC_EXECUTOR);
    }

    /**
     * POST يرسل body JSON وينزّل binary response (PDF).
     * يُستخدم لتصدير بطاقة الأجور مع إرسال precomputedMonths.
     */
    public static byte[] downloadBinaryPost(String path, Object body, Duration timeout)
            throws IOException, InterruptedException {
        HttpCore c = core();

        HttpRequest request = c.addAuthHeader(
                HttpRequest.newBuilder()
                        .uri(java.net.URI.create(c.getBaseUrl() + path))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/pdf, application/octet-stream, */*")
                        .timeout(timeout)
                        .POST(HttpRequest.BodyPublishers.ofString(
                                c.mapper.writeValueAsString(body),
                                StandardCharsets.UTF_8))
        ).build();

        HttpResponse<byte[]> response = c.httpClient.send(
                request, HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() >= 400)
            throw new IOException("فشل التصدير — HTTP " + response.statusCode());

        byte[] bytes = response.body();
        if (bytes == null || bytes.length == 0) throw new IOException("الملف فارغ");
        return bytes;
    }

    public static CompletableFuture<byte[]> downloadBinaryPostAsync(
            String path, Object body, Duration timeout) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return downloadBinaryPost(path, body, timeout);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, HttpCore.ASYNC_EXECUTOR);
    }

}