package com.safwat.hr.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.safwat.hr.shared.AppConfig;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/**
 * عميل متكامل للتواصل مع backend (Spring Boot) من تطبيقات JavaFX.
 * يدعم:
 * - طرق HTTP (GET, POST, PUT, DELETE) مع أو بدون متغيرات.
 * - إرسال JSON (RequestBody).
 * - إرسال بيانات نموذج (application/x-www-form-urlencoded).
 * - رفع ملفات (multipart/form-data) مع بيانات إضافية.
 * - WebSocket.
 * - إدارة التوكن (للصلاحيات).
 */
public class ApiClient {

    // ── Jackson ObjectMapper (مكان Gson) ─────────────────────────────
    public static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())                        // دعم Java 8 Date/Time
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)    // ISO-8601 بدل epoch
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); // تجاهل حقول غير معروفة
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    public static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(TIMEOUT)
            .build();
    public static String url = AppConfig.getString("connection", "url", "http://");
    public static String url2 = AppConfig.getString("connection", "url2", "ws://");
    public static String masterPC = AppConfig.getString("connection", "masterPC", "localhost");
    public static String port = AppConfig.getString("connection", "port", "8080");
    public static final String BASE_URL = url + masterPC + ":" + port + "/api";
    public static final String BASE_URL2 = url2 + masterPC + ":" + port + "/ws";
    // ── إدارة التوكن ─────────────────────────────────────────────────
    @Getter
    @Setter
    private static String authToken = null;
    @Getter
    @Setter
    private static String userName = null;

    public static void clearAuthToken() {
        authToken = null;
    }

    public static <T> ApiResponse<T> get(String path, Class<T> responseType)
            throws IOException, InterruptedException {
        return sendRequest(path, "GET", null, null, responseType);
    }

    // ═════════════════════════════════════════════════════════════════
    //  GET
    // ═════════════════════════════════════════════════════════════════
    public static <T> ApiResponse<T> getWithTypeRef(String path, TypeReference<T> responseType)
            throws IOException, InterruptedException {
        return sendRequestWithTypeRef(path, "GET", null, null, responseType);
    }

    private static <T> ApiResponse<T> sendRequestWithTypeRef(String path,
                                                             String method,
                                                             Object body,
                                                             Map<String, String> headers,
                                                             TypeReference<T> responseType)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder = baseBuilder(path, headers);
        applyMethod(builder, method, body);
        HttpResponse<String> response = httpClient.send(
                addAuthHeader(builder).build(),
                HttpResponse.BodyHandlers.ofString());
        return parseResponseWithTypeRef(response, responseType);
    }

    private static <T> ApiResponse<T> parseResponseWithTypeRef(HttpResponse<String> response,
                                                               TypeReference<T> responseType) {
        String body = response.body();
        int statusCode = response.statusCode();

        try {
            JavaType innerType = mapper.getTypeFactory().constructType(responseType);
            JavaType apiType = mapper.getTypeFactory()
                    .constructParametricType(ApiResponse.class, innerType);
            ApiResponse<T> parsed = mapper.readValue(body, apiType);
            if (parsed != null) return parsed;
        } catch (Exception ignored) {
        }

        // fallback
        ApiResponse<T> apiResponse = new ApiResponse<>();
        boolean success = statusCode >= 200 && statusCode < 300;
        apiResponse.setSuccess(success);
        apiResponse.setTimestamp(java.time.LocalDateTime.now().toString());

        if (success) {
            try {
                apiResponse.setData(mapper.readValue(body, responseType));
                apiResponse.setMessage("Success");
            } catch (Exception e) {
                apiResponse.setSuccess(false);
                apiResponse.setMessage("Failed to parse response: " + e.getMessage());
            }
        } else {
            apiResponse.setMessage(body);
        }
        return apiResponse;
    }

    public static <T> ApiResponse<T> get(String path,
                                         Map<String, String> queryParams,
                                         Class<T> responseType)
            throws IOException, InterruptedException {
        return sendRequest(appendQueryParams(path, queryParams), "GET", null, null, responseType);
    }

    public static <T> ApiResponse<T> post(String path, Object body, Class<T> responseType)
            throws IOException, InterruptedException {
        return sendRequest(path, "POST", body, null, responseType);
    }

    // ═════════════════════════════════════════════════════════════════
    //  POST
    // ═════════════════════════════════════════════════════════════════

    public static <T> ApiResponse<T> post(String path,
                                          Object body,
                                          Map<String, String> queryParams,
                                          Class<T> responseType)
            throws IOException, InterruptedException {
        return sendRequest(appendQueryParams(path, queryParams), "POST", body, null, responseType);
    }

    /**
     * POST مع TypeReference لدعم الأنواع المعقدة مثل List&lt;Employee&gt;.
     * <pre>
     *   ApiResponse&lt;List&lt;Employee&gt;&gt; res =
     *       ApiClient.post("/employees", body, new TypeReference&lt;&gt;() {});
     * </pre>
     */
    public static <T> ApiResponse<T> post(String path,
                                          Object body,
                                          TypeReference<T> responseType)
            throws IOException, InterruptedException {
        return sendRequest(path, "POST", body, null, responseType);
    }

    public static <T> ApiResponse<T> put(String path, Object body, Class<T> responseType)
            throws IOException, InterruptedException {
        return sendRequest(path, "PUT", body, null, responseType);
    }

    // ═════════════════════════════════════════════════════════════════
    //  PUT
    // ═════════════════════════════════════════════════════════════════

    public static <T> ApiResponse<T> delete(String path, Class<T> responseType)
            throws IOException, InterruptedException {
        return sendRequest(path, "DELETE", null, null, responseType);
    }

    // ═════════════════════════════════════════════════════════════════
    //  DELETE
    // ═════════════════════════════════════════════════════════════════

    public static <T> ApiResponse<T> delete(String path,
                                            Map<String, String> queryParams,
                                            Class<T> responseType)
            throws IOException, InterruptedException {
        return sendRequest(appendQueryParams(path, queryParams), "DELETE", null, null, responseType);
    }

    public static <T> ApiResponse<T> postForm(String path,
                                              Map<String, String> formData,
                                              Class<T> responseType)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .timeout(TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(
                        buildFormData(formData), StandardCharsets.UTF_8));

        HttpRequest request = addAuthHeader(builder).build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return parseResponse(response, responseType);
    }

    // ═════════════════════════════════════════════════════════════════
    //  Form & File Upload
    // ═════════════════════════════════════════════════════════════════

    public static <T> ApiResponse<T> uploadFile(String path,
                                                Path file,
                                                String fileParamName,
                                                Map<String, String> additionalData,
                                                Class<T> responseType)
            throws IOException, InterruptedException {
        Map<String, Object> formData = new HashMap<>();
        if (additionalData != null) formData.putAll(additionalData);
        formData.put(fileParamName, file);
        return uploadFile(path, formData, responseType);
    }

    public static <T> ApiResponse<T> uploadFile(String path,
                                                Map<String, Object> formData,
                                                Class<T> responseType)
            throws IOException, InterruptedException {
        String boundary = "---Boundary" + System.currentTimeMillis();
        var parts = new ArrayList<byte[]>();

        for (Map.Entry<String, Object> entry : formData.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Path filePath) {
                String mimeType = Files.probeContentType(filePath);
                if (mimeType == null) mimeType = "application/octet-stream";

                parts.add(("--" + boundary + "\r\n" +
                        "Content-Disposition: form-data; name=\"" + name +
                        "\"; filename=\"" + filePath.getFileName() + "\"\r\n" +
                        "Content-Type: " + mimeType + "\r\n\r\n")
                        .getBytes(StandardCharsets.UTF_8));
                parts.add(Files.readAllBytes(filePath));
                parts.add("\r\n".getBytes(StandardCharsets.UTF_8));

            } else {
                String content;
                String contentType;
                if (value instanceof String s) {
                    content = s;
                    contentType = "text/plain";
                } else {
                    // Jackson بدل Gson
                    content = mapper.writeValueAsString(value);
                    contentType = "application/json";
                }
                parts.add(("--" + boundary + "\r\n" +
                        "Content-Disposition: form-data; name=\"" + name + "\"\r\n" +
                        "Content-Type: " + contentType + "\r\n\r\n")
                        .getBytes(StandardCharsets.UTF_8));
                parts.add(content.getBytes(StandardCharsets.UTF_8));
                parts.add("\r\n".getBytes(StandardCharsets.UTF_8));
            }
        }
        parts.add(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        HttpRequest request = addAuthHeader(
                HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + path))
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .header("Accept", "application/json")
                        .timeout(TIMEOUT)
                        .POST(HttpRequest.BodyPublishers.ofByteArrays(parts)))
                .build();

        return parseResponse(
                httpClient.send(request, HttpResponse.BodyHandlers.ofString()),
                responseType);
    }

    public static boolean downloadFile(String path,
                                       Map<String, String> queryParams,
                                       Path targetPath)
            throws IOException, InterruptedException {
        HttpRequest request = addAuthHeader(
                HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + appendQueryParams(path, queryParams)))
                        .timeout(TIMEOUT)
                        .GET())
                .build();
        HttpResponse<Path> response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(targetPath));
        return response.statusCode() >= 200 && response.statusCode() < 300;
    }

    // ═════════════════════════════════════════════════════════════════
    //  File Download
    // ═════════════════════════════════════════════════════════════════

    public static boolean downloadFileViaPost(String path, Object body, Path targetPath)
            throws IOException, InterruptedException {
        HttpRequest request = addAuthHeader(
                HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + path))
                        .header("Content-Type", "application/json")
                        .timeout(TIMEOUT)
                        .POST(HttpRequest.BodyPublishers.ofString(
                                mapper.writeValueAsString(body), StandardCharsets.UTF_8)))
                .build();
        HttpResponse<Path> response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(targetPath));
        return response.statusCode() >= 200 && response.statusCode() < 300;
    }

    public static <T> CompletableFuture<ApiResponse<T>> getAsync(String path, Class<T> responseType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return get(path, responseType);
            } catch (Exception e) {
                return createErrorResponse(e);
            }
        });
    }

    // ═════════════════════════════════════════════════════════════════
    //  Async variants
    // ═════════════════════════════════════════════════════════════════

    public static <T> CompletableFuture<ApiResponse<T>> getAsync(String path,
                                                                 Map<String, String> queryParams,
                                                                 Class<T> responseType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return get(path, queryParams, responseType);
            } catch (Exception e) {
                return createErrorResponse(e);
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
                return createErrorResponse(e);
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
                return createErrorResponse(e);
            }
        });
    }

    public static <T> CompletableFuture<ApiResponse<T>> uploadFileAsync(String path,
                                                                        Path file,
                                                                        String fileParamName,
                                                                        Map<String, String> additionalData,
                                                                        Class<T> responseType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return uploadFile(path, file, fileParamName, additionalData, responseType);
            } catch (Exception e) {
                return createErrorResponse(e);
            }
        });
    }

    public static CompletableFuture<Boolean> downloadFileAsync(String path,
                                                               Map<String, String> queryParams,
                                                               Path targetPath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return downloadFile(path, queryParams, targetPath);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static <T> ApiResponse<T> sendRequest(String path,
                                                  String method,
                                                  Object body,
                                                  Map<String, String> headers,
                                                  Class<T> responseType)
            throws IOException, InterruptedException {

        HttpRequest.Builder builder = baseBuilder(path, headers);
        applyMethod(builder, method, body);
        HttpResponse<String> response = httpClient.send(
                addAuthHeader(builder).build(),
                HttpResponse.BodyHandlers.ofString());
        return parseResponse(response, responseType);
    }

    // ═════════════════════════════════════════════════════════════════
    //  Core sendRequest — Class<T>
    // ═════════════════════════════════════════════════════════════════

    private static <T> ApiResponse<T> sendRequest(String path,
                                                  String method,
                                                  Object body,
                                                  Map<String, String> headers,
                                                  TypeReference<T> responseType)
            throws IOException, InterruptedException {

        HttpRequest.Builder builder = baseBuilder(path, headers);
        applyMethod(builder, method, body);
        HttpResponse<String> response = httpClient.send(
                addAuthHeader(builder).build(),
                HttpResponse.BodyHandlers.ofString());
        return parseResponse(response, responseType);
    }

    // ═════════════════════════════════════════════════════════════════
    //  Core sendRequest — TypeReference<T>  (مكان Type من Gson)
    // ═════════════════════════════════════════════════════════════════

    private static <T> ApiResponse<T> parseResponse(HttpResponse<String> response,
                                                    Class<T> responseType) {
        String body = response.body();
        int statusCode = response.statusCode();

        try {
            // نحاول تحليل الاستجابة كـ ApiResponse<T> مباشرة
            JavaType apiType = mapper.getTypeFactory()
                    .constructParametricType(ApiResponse.class, responseType);
            ApiResponse<T> parsed = mapper.readValue(body, apiType);
            if (parsed != null) return parsed;

        } catch (Exception ignored) {
        }

        // fallback يدوي
        ApiResponse<T> apiResponse = new ApiResponse<>();
        boolean success = statusCode >= 200 && statusCode < 300;
        apiResponse.setSuccess(success);
        apiResponse.setTimestamp(java.time.LocalDateTime.now().toString());

        if (success && responseType != null) {
            try {
                apiResponse.setData(mapper.readValue(body, responseType));
                apiResponse.setMessage("Success");
            } catch (Exception e) {
                apiResponse.setSuccess(false);
                apiResponse.setMessage("Failed to parse response: " + e.getMessage());
            }
        } else {
            apiResponse.setMessage(success ? "Success" : body);
        }
        return apiResponse;
    }

    // ═════════════════════════════════════════════════════════════════
    //  parseResponse — Class<T>
    // ═════════════════════════════════════════════════════════════════

    private static <T> ApiResponse<T> parseResponse(HttpResponse<String> response,
                                                    TypeReference<T> responseType) {
        String body = response.body();
        int statusCode = response.statusCode();

        try {
            // نحاول تحليل الاستجابة كـ ApiResponse<T>
            // نستخرج JavaType من TypeReference
            JavaType innerType = mapper.getTypeFactory().constructType(responseType);
            JavaType apiType = mapper.getTypeFactory()
                    .constructParametricType(ApiResponse.class, innerType);
            ApiResponse<T> parsed = mapper.readValue(body, apiType);
            if (parsed != null) return parsed;

        } catch (Exception ignored) {
        }

        // fallback يدوي
        ApiResponse<T> apiResponse = new ApiResponse<>();
        boolean success = statusCode >= 200 && statusCode < 300;
        apiResponse.setSuccess(success);
        apiResponse.setTimestamp(java.time.LocalDateTime.now().toString());

        if (success) {
            try {
                apiResponse.setData(mapper.readValue(body, responseType));
                apiResponse.setMessage("Success");
            } catch (Exception e) {
                apiResponse.setSuccess(false);
                apiResponse.setMessage("Failed to parse response: " + e.getMessage());
            }
        } else {
            apiResponse.setMessage(body);
        }
        return apiResponse;
    }

    // ═════════════════════════════════════════════════════════════════
    //  parseResponse — TypeReference<T>
    // ═════════════════════════════════════════════════════════════════

    private static HttpRequest.Builder baseBuilder(String path, Map<String, String> headers) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");
        if (headers != null) headers.forEach(builder::header);
        return builder;
    }

    // ═════════════════════════════════════════════════════════════════
    //  Helpers
    // ═════════════════════════════════════════════════════════════════

    private static void applyMethod(HttpRequest.Builder builder,
                                    String method,
                                    Object body) throws IOException {
        // Jackson بدل Gson لتحويل الكائن إلى JSON
        String json = body != null ? mapper.writeValueAsString(body) : "";
        switch (method) {
            case "GET" -> builder.GET();
            case "POST" -> builder.POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
            case "PUT" -> builder.PUT(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
            case "DELETE" -> builder.DELETE();
            default -> throw new IllegalArgumentException("Unsupported method: " + method);
        }
    }

    private static String appendQueryParams(String path, Map<String, String> params) {
        if (params == null || params.isEmpty()) return path;
        StringBuilder sb = new StringBuilder(path);
        sb.append(path.contains("?") ? '&' : '?');
        params.forEach((k, v) -> {
            if (sb.charAt(sb.length() - 1) != '?' && sb.charAt(sb.length() - 1) != '&')
                sb.append('&');
            sb.append(URLEncoder.encode(k, StandardCharsets.UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(v, StandardCharsets.UTF_8));
        });
        return sb.toString();
    }

    private static String buildFormData(Map<String, String> formData) {
        StringBuilder sb = new StringBuilder();
        formData.forEach((k, v) -> {
            if (!sb.isEmpty()) sb.append('&');
            sb.append(URLEncoder.encode(k, StandardCharsets.UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(v, StandardCharsets.UTF_8));
        });
        return sb.toString();
    }

    private static HttpRequest.Builder addAuthHeader(HttpRequest.Builder builder) {
        if (authToken != null && !authToken.isEmpty())
            builder.header("Authorization", "Bearer " + authToken);
        return builder;
    }

    /**
     * مستخدمة فقط في uploadFile (يستقبل HttpRequest بدل Builder)
     */
    private static HttpRequest addAuthHeader(HttpRequest request) {
        if (authToken != null && !authToken.isEmpty())
            return HttpRequest.newBuilder(request, (n, v) -> true)
                    .header("Authorization", "Bearer " + authToken)
                    .build();
        return request;
    }

    private static <T> ApiResponse<T> createErrorResponse(Exception e) {
        ApiResponse<T> error = new ApiResponse<>();
        error.setSuccess(false);
        error.setMessage(e.getMessage());
        error.setTimestamp(java.time.LocalDateTime.now().toString());
        return error;
    }

    public static TaskStatus startAsyncDownload(String path, Object body)
            throws IOException, InterruptedException {
        ApiResponse<TaskStatus> response = post(path, body, TaskStatus.class);
        if (response.isSuccess() && response.getData() != null) return response.getData();
        throw new IOException("Failed to start async download: " + response.getMessage());
    }

    // ═════════════════════════════════════════════════════════════════
    //  WebSocket
    // ═════════════════════════════════════════════════════════════════

    public static TaskStatus getDownloadStatus(String taskId)
            throws IOException, InterruptedException {
        ApiResponse<TaskStatus> response = get("/tasks/" + taskId + "/status", TaskStatus.class);
        if (response.isSuccess() && response.getData() != null) return response.getData();
        throw new IOException("Failed to get task status: " + response.getMessage());
    }

    // ═════════════════════════════════════════════════════════════════
    //  Async Download with Polling
    // ═════════════════════════════════════════════════════════════════

    public static boolean downloadCompletedFile(String taskId, Path targetPath)
            throws IOException, InterruptedException {
        return downloadFile("/tasks/" + taskId + "/download", null, targetPath);
    }

    public static CompletableFuture<Path> pollAsyncDownload(String path,
                                                            Object body,
                                                            Path targetDir,
                                                            int pollIntervalSeconds) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                TaskStatus startStatus = startAsyncDownload(path, body);
                String taskId = startStatus.getTaskId();
                while (true) {
                    Thread.sleep(pollIntervalSeconds * 1000L);
                    TaskStatus status = getDownloadStatus(taskId);
                    if ("COMPLETED".equals(status.getStatus())) {
                        Path filePath = targetDir.resolve("report_" + taskId + ".xlsx");
                        if (downloadCompletedFile(taskId, filePath)) return filePath;
                        throw new IOException("Download failed after task completion");
                    } else if ("FAILED".equals(status.getStatus())) {
                        throw new IOException("Task failed: " + status.getMessage());
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * تحميل ملف باستخدام POST مع RequestBody
     * مفيد للـ endpoints اللي بتستخدم @PostMapping وترجع Resource
     *
     * @param path       المسار النسبي (مثل: "/payroll/download-changeMonth")
     * @param body       كائن الطلب (مثل: PayrollReportRequest)
     * @param targetPath المسار اللي هنحفظ فيه الملف
     * @return true لو نجح التحميل
     */
    public static boolean downloadFileViaPostWithBody(String path,
                                                      Object body,
                                                      Path targetPath)
            throws IOException, InterruptedException {

        // تحويل body لـ JSON
        String jsonBody = mapper.writeValueAsString(body);

        HttpRequest request = addAuthHeader(
                HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + path))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/octet-stream, application/pdf, application/*")
                        .timeout(TIMEOUT)
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8)))
                .build();

        // التحميل كـ byte array عشان نقدر نتعامل مع الـ Resource
        HttpResponse<byte[]> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofByteArray());

        // التحقق من نجاح التحميل
        int statusCode = response.statusCode();
        if (statusCode >= 200 && statusCode < 300) {
            byte[] fileBytes = response.body();
            if (fileBytes != null && fileBytes.length > 0) {
                // حفظ الملف
                Files.write(targetPath, fileBytes);
                return true;
            }
            return false;
        }

        throw new IOException("Download failed with status: " + statusCode +
                ", body: " + new String(response.body(), StandardCharsets.UTF_8));
    }

    /**
     * نفس الميثود بس Async
     */
    public static CompletableFuture<Boolean> downloadFileViaPostWithBodyAsync(
            String path,
            Object body,
            Path targetPath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return downloadFileViaPostWithBody(path, body, targetPath);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static String BASE_URL2() {
        return BASE_URL2;
    }

    public static String getBaseUrl() {
        return BASE_URL;
    }


    public static class WebSocketClient {
        private final URI serverUri;
        private final Consumer<String> onMessage;
        private final Consumer<Throwable> onError;
        private final Runnable onClose;
        private final String authToken;
        private WebSocket webSocket;

        public WebSocketClient(String path,
                               Consumer<String> onMessage,
                               Consumer<Throwable> onError,
                               Runnable onClose) {
            this(path, onMessage, onError, onClose, null);
        }

        public WebSocketClient(String path,
                               Consumer<String> onMessage,
                               Consumer<Throwable> onError,
                               Runnable onClose,
                               String token) {
            this.authToken = token != null ? token : ApiClient.getAuthToken();

            // ✅ استخدم BASE_URL2 بدلاً من BASE_URL
            this.serverUri = URI.create(BASE_URL2);

            this.onMessage = onMessage;
            this.onError = onError;
            this.onClose = onClose;

            System.out.println("[WebSocketClient] Connecting to: " + serverUri);
            System.out.println("[WebSocketClient] Token present: " + (authToken != null && !authToken.isEmpty()));
        }

        public CompletableFuture<Void> connect() {
            // إنشاء Builder وإضافة Headers
            WebSocket.Builder builder = httpClient.newWebSocketBuilder();

            // إضافة Authorization Header
            if (authToken != null && !authToken.isEmpty()) {
                builder.header("Authorization", "Bearer " + authToken);
                System.out.println("[WebSocketClient] ✅ Authorization header added");
            } else {
                System.out.println("[WebSocketClient] ⚠️ No token available");
            }

            // إضافة Headers إضافية
            builder.header("Origin", "http://localhost:8080");
            builder.header("User-Agent", "JavaFX-Client");

            return builder
                    .buildAsync(serverUri, new WebSocket.Listener() {
                        @Override
                        public void onOpen(WebSocket ws) {
                            webSocket = ws;
                            System.out.println("[WebSocketClient] ✅ Connection opened");
                            ws.request(1);
                        }

                        @Override
                        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
                            if (onMessage != null) {
                                System.out.println("[WebSocketClient] 📩 Received: " + data);
                                onMessage.accept(data.toString());
                            }
                            ws.request(1);
                            return null;
                        }

                        @Override
                        public void onError(WebSocket ws, Throwable error) {
                            System.err.println("[WebSocketClient] ❌ Error: " + error.getMessage());
                            if (onError != null) onError.accept(error);
                        }

                        @Override
                        public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
                            System.out.println("[WebSocketClient] 🔌 Closed: " + reason + " (code: " + statusCode + ")");
                            if (onClose != null) onClose.run();
                            return null;
                        }
                    })
                    .thenAccept(ws -> webSocket = ws)
                    .exceptionally(e -> {
                        System.err.println("[WebSocketClient] ❌ Connection failed: " + e.getMessage());
                        if (onError != null) onError.accept(e);
                        return null;
                    });
        }

        public void sendMessage(String message) {
            if (webSocket != null) {
                webSocket.sendText(message, true);
                System.out.println("[WebSocketClient] 📤 Sent: " + message);
            } else {
                System.err.println("[WebSocketClient] ⚠️ Cannot send, WebSocket is null");
            }
        }

        public void close() {
            if (webSocket != null) {
                webSocket.sendClose(1000, "Closing");
                System.out.println("[WebSocketClient] 🔌 Closing connection");
            }
        }
    }

    public static class TaskStatus {
        private String taskId;
        private String status;     // IN_PROGRESS | COMPLETED | FAILED
        private int progress;   // 0-100
        private String message;
        private String downloadUrl;

        public String getTaskId() {
            return taskId;
        }

        public void setTaskId(String v) {
            taskId = v;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String v) {
            status = v;
        }

        public int getProgress() {
            return progress;
        }

        public void setProgress(int v) {
            progress = v;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String v) {
            message = v;
        }

        public String getDownloadUrl() {
            return downloadUrl;
        }

        public void setDownloadUrl(String v) {
            downloadUrl = v;
        }
    }
}