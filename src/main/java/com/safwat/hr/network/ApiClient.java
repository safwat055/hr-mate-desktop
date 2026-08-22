package com.safwat.hr.network;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.safwat.hr.network.dto.*;
import com.safwat.hr.shared.AppConfig;
import com.safwat.hr.shared.PayrollRequest;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * عميل HTTP موحد للتواصل مع Backend (Spring Boot) من تطبيقات JavaFX.
 * <p>
 * يدعم: GET, POST, PUT, DELETE, Form POST, Multipart Upload, File Download.
 * <p>
 * <b>ملاحظة:</b> الـ WebSocket تم فصله في {@link WebSocketClient}.
 *
 * @see WebSocketClient
 */
public class ApiClient {

    private static final DateTimeFormatter SERVER_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule()
                    .addDeserializer(java.time.LocalDateTime.class,
                            new LocalDateTimeDeserializer(SERVER_DATE_FORMAT)))
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

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

    @Getter
    @Setter
    private static String authToken = null;

    @Getter
    @Setter
    private static String userName = null;

    public static void clearAuthToken() {
        authToken = null;
    }

    // ═════════════════════════════════════════════════════════════════
    //  HTTP Methods
    // ═════════════════════════════════════════════════════════════════

    public static <T> ApiResponse<T> get(String path, Class<T> responseType)
            throws IOException, InterruptedException {
        return sendRequest(path, "GET", null, null, responseType);
    }

    public static <T> ApiResponse<T> get(String path,
                                         Map<String, String> queryParams,
                                         Class<T> responseType)
            throws IOException, InterruptedException {
        return sendRequest(appendQueryParams(path, queryParams), "GET", null, null, responseType);
    }

    public static <T> ApiResponse<T> getWithTypeRef(String path, TypeReference<T> responseType)
            throws IOException, InterruptedException {
        return sendRequest(path, "GET", null, null, responseType);
    }

    public static <T> ApiResponse<T> getWithTypeRef(String path,
                                                    Map<String, String> queryParams,
                                                    TypeReference<T> responseType)
            throws IOException, InterruptedException {
        return sendRequest(appendQueryParams(path, queryParams), "GET", null, null, responseType);
    }

    public static <T> ApiResponse<T> post(String path, Object body, Class<T> responseType)
            throws IOException, InterruptedException {
        return sendRequest(path, "POST", body, null, responseType);
    }

    public static <T> ApiResponse<T> post(String path,
                                          Object body,
                                          Map<String, String> queryParams,
                                          Class<T> responseType)
            throws IOException, InterruptedException {
        return sendRequest(appendQueryParams(path, queryParams), "POST", body, null, responseType);
    }

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

    public static <T> ApiResponse<T> delete(String path, Class<T> responseType)
            throws IOException, InterruptedException {
        return sendRequest(path, "DELETE", null, null, responseType);
    }

    public static <T> ApiResponse<T> delete(String path,
                                            Map<String, String> queryParams,
                                            Class<T> responseType)
            throws IOException, InterruptedException {
        return sendRequest(appendQueryParams(path, queryParams), "DELETE", null, null, responseType);
    }

    public static <T> ApiResponse<T> delete(String path,
                                            Object body,
                                            Class<T> responseType)
            throws IOException, InterruptedException {
        return sendRequest(path, "DELETE", body, null, responseType);
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
    //  File Upload
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

    // ═════════════════════════════════════════════════════════════════
    //  File Download
    // ═════════════════════════════════════════════════════════════════

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

    public static boolean downloadFileViaPostWithBody(String path,
                                                      Object body,
                                                      Path targetPath)
            throws IOException, InterruptedException {
        String jsonBody = mapper.writeValueAsString(body);

        HttpRequest request = addAuthHeader(
                HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + path))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/octet-stream, application/pdf, application/*")
                        .timeout(TIMEOUT)
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8)))
                .build();

        HttpResponse<byte[]> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofByteArray());

        int statusCode = response.statusCode();
        if (statusCode >= 200 && statusCode < 300) {
            byte[] fileBytes = response.body();
            if (fileBytes != null && fileBytes.length > 0) {
                Files.write(targetPath, fileBytes);
                return true;
            }
            return false;
        }

        throw new IOException("Download failed with status: " + statusCode +
                ", body: " + new String(response.body(), StandardCharsets.UTF_8));
    }

    // ═════════════════════════════════════════════════════════════════
    //  Async Variants
    // ═════════════════════════════════════════════════════════════════

    public static <T> CompletableFuture<ApiResponse<T>> getAsync(String path, Class<T> responseType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return get(path, responseType);
            } catch (Exception e) {
                return createErrorResponse(e);
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

    public static CompletableFuture<Boolean> downloadFileViaPostWithBodyAsync(
            String path, Object body, Path targetPath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return downloadFileViaPostWithBody(path, body, targetPath);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    // ═════════════════════════════════════════════════════════════════
    //  Report Manager Methods
    // ═════════════════════════════════════════════════════════════════

    public static ApiResponse<List<ReportStatusResponse>> getMyReports()
            throws IOException, InterruptedException {
        return getWithTypeRef("/reports/my", new TypeReference<List<ReportStatusResponse>>() {
        });
    }

    public static boolean downloadReportFile(Long reportId, Path targetPath)
            throws IOException, InterruptedException {
        return downloadFile("/download/" + reportId + "/file", null, targetPath);
    }

    public static ApiResponse<ReportSubmissionResult> submitReport(Object requestBody)
            throws IOException, InterruptedException {
        return post("/reports", requestBody, ReportSubmissionResult.class);
    }

    public static ApiResponse<ReportSubmissionResult> submitReport(PayrollRequest request,
                                                                   List<Path> files)
            throws IOException, InterruptedException {
        String boundary = "---Boundary" + System.currentTimeMillis();
        var parts = new ArrayList<byte[]>();

        String json = mapper.writeValueAsString(request);
        parts.add(("--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"data\"\r\n" +
                "Content-Type: application/json\r\n\r\n")
                .getBytes(StandardCharsets.UTF_8));
        parts.add(json.getBytes(StandardCharsets.UTF_8));
        parts.add("\r\n".getBytes(StandardCharsets.UTF_8));

        if (files != null) {
            for (Path file : files) {
                String mime = Files.probeContentType(file);
                if (mime == null) mime = "application/octet-stream";
                String fileName = file.getFileName().toString();

                parts.add(("--" + boundary + "\r\n" +
                        "Content-Disposition: form-data; name=\"files\"; filename=\"" + fileName + "\"\r\n" +
                        "Content-Type: " + mime + "\r\n\r\n")
                        .getBytes(StandardCharsets.UTF_8));
                parts.add(Files.readAllBytes(file));
                parts.add("\r\n".getBytes(StandardCharsets.UTF_8));
            }
        }
        parts.add(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        HttpRequest httpRequest = addAuthHeader(
                HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/reports"))
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .header("Accept", "application/json")
                        .timeout(TIMEOUT)
                        .POST(HttpRequest.BodyPublishers.ofByteArrays(parts)))
                .build();

        return parseResponse(
                httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString()),
                ReportSubmissionResult.class);
    }

    public static ApiResponse<ReportStatusResponse> getReportStatus(Long reportId)
            throws IOException, InterruptedException {
        return get("/reports/" + reportId, ReportStatusResponse.class);
    }

    public static ApiResponse<Void> cancelReport(Long reportId)
            throws IOException, InterruptedException {
        return post("/reports/" + reportId + "/cancel", null, Void.class);
    }

    public static ApiResponse<List<AvailableReportInfo>> getAvailableReports()
            throws IOException, InterruptedException {
        return getWithTypeRef("/reports/available", new TypeReference<List<AvailableReportInfo>>() {
        });
    }

    public static ApiResponse<ReportPayloadResponse> getReportPayload(Long reportId)
            throws IOException, InterruptedException {
        return getWithTypeRef("/reports/" + reportId + "/payload", new TypeReference<ReportPayloadResponse>() {
        });
    }

    public static ApiResponse<ReportPayloadResponse> getReportPayload()
            throws IOException, InterruptedException {
        return getWithTypeRef("/reports/lastReport", new TypeReference<ReportPayloadResponse>() {
        });
    }

    public static ReportStatusResponse pollReportUntilDone(Long reportId,
                                                           int pollIntervalSeconds,
                                                           int maxAttempts)
            throws IOException, InterruptedException {
        for (int i = 0; i < maxAttempts; i++) {
            ApiResponse<ReportStatusResponse> response = getReportStatus(reportId);
            if (!response.isSuccess() || response.getData() == null) {
                throw new IOException("Failed to get report status: " + response.getMessage());
            }
            ReportStatusResponse status = response.getData();
            String currentStatus = status.getStatus();
            if ("COMPLETED".equals(currentStatus) || "FAILED".equals(currentStatus) || "CANCELLED".equals(currentStatus)) {
                return status;
            }
            Thread.sleep(pollIntervalSeconds * 1000L);
        }
        throw new IOException("Report polling timed out after " + maxAttempts + " attempts");
    }

    // ═════════════════════════════════════════════════════════════════
    //  Async Download with Polling (Legacy)
    // ═════════════════════════════════════════════════════════════════

    public static TaskStatus startAsyncDownload(String path, Object body)
            throws IOException, InterruptedException {
        ApiResponse<TaskStatus> response = post(path, body, TaskStatus.class);
        if (response.isSuccess() && response.getData() != null) return response.getData();
        throw new IOException("Failed to start async download: " + response.getMessage());
    }

    public static TaskStatus getDownloadStatus(String taskId)
            throws IOException, InterruptedException {
        ApiResponse<TaskStatus> response = get("/tasks/" + taskId + "/status", TaskStatus.class);
        if (response.isSuccess() && response.getData() != null) return response.getData();
        throw new IOException("Failed to get task status: " + response.getMessage());
    }

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

    // ═════════════════════════════════════════════════════════════════
    //  Core Helpers
    // ═════════════════════════════════════════════════════════════════

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

    private static <T> ApiResponse<T> parseResponse(HttpResponse<String> response,
                                                    Class<T> responseType) {
        String body = response.body();
        int statusCode = response.statusCode();

        try {
            JavaType apiType = mapper.getTypeFactory()
                    .constructParametricType(ApiResponse.class, responseType);
            ApiResponse<T> parsed = mapper.readValue(body, apiType);
            if (parsed != null) return parsed;
        } catch (Exception ignored) {
        }

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

    private static <T> ApiResponse<T> parseResponse(HttpResponse<String> response,
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

    private static HttpRequest.Builder baseBuilder(String path, Map<String, String> headers) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");
        if (headers != null) headers.forEach(builder::header);
        return builder;
    }

    private static void applyMethod(HttpRequest.Builder builder,
                                    String method,
                                    Object body) throws IOException {
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

    public static String BASE_URL2() {
        return BASE_URL2;
    }

    public static String getBaseUrl() {
        return BASE_URL;
    }
}