package com.safwat.hr.utils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

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
    private static final String BASE_URL = "http://localhost:8080";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(TIMEOUT)
            .build();
    private static final Gson gson = new Gson();

    // ------ إدارة التوكن (للصلاحيات) ------
    private static String authToken = null;

    public static void setAuthToken(String token) {
        authToken = token;
    }

    public static void clearAuthToken() {
        authToken = null;
    }

    // ------ الطلبات المتزامنة (synchronous) ------

    /**
     * GET request بدون query parameters.
     */
    public static <T> ApiResponse<T> get(String path, Class<T> responseType) throws IOException, InterruptedException {
        return sendRequest(path, "GET", null, null, responseType);
    }

    /**
     * GET request مع query parameters.
     */
    public static <T> ApiResponse<T> get(String path, Map<String, String> queryParams, Class<T> responseType) throws IOException, InterruptedException {
        String fullPath = appendQueryParams(path, queryParams);
        return sendRequest(fullPath, "GET", null, null, responseType);
    }

    /**
     * POST request مع JSON body.
     */
    public static <T> ApiResponse<T> post(String path, Object body, Class<T> responseType) throws IOException, InterruptedException {
        return sendRequest(path, "POST", body, null, responseType);
    }

    /**
     * POST request مع JSON body و query parameters.
     */
    public static <T> ApiResponse<T> post(String path, Object body, Map<String, String> queryParams, Class<T> responseType) throws IOException, InterruptedException {
        String fullPath = appendQueryParams(path, queryParams);
        return sendRequest(fullPath, "POST", body, null, responseType);
    }

    /**
     * PUT request مع JSON body.
     */
    public static <T> ApiResponse<T> put(String path, Object body, Class<T> responseType) throws IOException, InterruptedException {
        return sendRequest(path, "PUT", body, null, responseType);
    }

    /**
     * DELETE request.
     */
    public static <T> ApiResponse<T> delete(String path, Class<T> responseType) throws IOException, InterruptedException {
        return sendRequest(path, "DELETE", null, null, responseType);
    }

    /**
     * DELETE request مع query parameters.
     */
    public static <T> ApiResponse<T> delete(String path, Map<String, String> queryParams, Class<T> responseType) throws IOException, InterruptedException {
        String fullPath = appendQueryParams(path, queryParams);
        return sendRequest(fullPath, "DELETE", null, null, responseType);
    }

    /**
     * POST request مع بيانات نموذج (application/x-www-form-urlencoded).
     */
    public static <T> ApiResponse<T> postForm(String path, Map<String, String> formData, Class<T> responseType) throws IOException, InterruptedException {
        String formBody = buildFormData(formData);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .timeout(TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(formBody, StandardCharsets.UTF_8))
                .build();
        request = addAuthHeader(request);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return parseResponse(response, responseType);
    }

    /**
     * رفع ملف (multipart/form-data) مع بيانات إضافية.
     *
     * @param path           مسار الـ endpoint.
     * @param file           الملف المراد رفعه (Path).
     * @param fileParamName  اسم الباراميتر الخاص بالملف (عادة "file").
     * @param additionalData خريطة بالبيانات النصية الإضافية.
     * @param responseType   نوع البيانات المتوقعة في الاستجابة.
     */
    public static <T> ApiResponse<T> uploadFile(String path, Path file, String fileParamName,
                                                Map<String, String> additionalData, Class<T> responseType) throws IOException, InterruptedException {
        Map<String, Object> formData = new HashMap<>();
        if (additionalData != null) {
            formData.putAll(additionalData);
        }
        formData.put(fileParamName, file);
        return uploadFile(path, formData, responseType);
    }

    /**
     * رفع ملف مع بيانات إضافية (قد تحتوي على كائنات JSON).
     *
     * @param path     مسار الـ endpoint.
     * @param formData خريطة تحتوي على بيانات نصية (String) وملفات (Path) وأي كائنات أخرى (ستحول إلى JSON).
     */
    public static <T> ApiResponse<T> uploadFile(String path, Map<String, Object> formData, Class<T> responseType) throws IOException, InterruptedException {
        String boundary = "---Boundary" + System.currentTimeMillis();
        var byteArrays = new java.util.ArrayList<byte[]>();

        for (Map.Entry<String, Object> entry : formData.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Path filePath) {
                // جزء الملف
                String fileName = filePath.getFileName().toString();
                String mimeType = Files.probeContentType(filePath);
                if (mimeType == null) mimeType = "application/octet-stream";

                String header = "--" + boundary + "\r\n" +
                        "Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + fileName + "\"\r\n" +
                        "Content-Type: " + mimeType + "\r\n\r\n";
                byteArrays.add(header.getBytes(StandardCharsets.UTF_8));
                byteArrays.add(Files.readAllBytes(filePath));
                byteArrays.add("\r\n".getBytes(StandardCharsets.UTF_8));
            } else {
                // جزء نصي أو JSON
                String content;
                String contentType;
                if (value instanceof String) {
                    content = (String) value;
                    contentType = "text/plain";
                } else {
                    content = gson.toJson(value);
                    contentType = "application/json";
                }
                String header = "--" + boundary + "\r\n" +
                        "Content-Disposition: form-data; name=\"" + name + "\"\r\n" +
                        "Content-Type: " + contentType + "\r\n\r\n";
                byteArrays.add(header.getBytes(StandardCharsets.UTF_8));
                byteArrays.add(content.getBytes(StandardCharsets.UTF_8));
                byteArrays.add("\r\n".getBytes(StandardCharsets.UTF_8));
            }
        }
        // إنهاء multipart
        byteArrays.add(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        // دمج جميع الأجزاء في BodyPublisher
        var publisher = HttpRequest.BodyPublishers.ofByteArrays(byteArrays);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("Accept", "application/json")
                .timeout(TIMEOUT)
                .POST(publisher);

        HttpRequest request = addAuthHeader(builder).build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return parseResponse(response, responseType);
    }

    // ------ الطلبات غير المتزامنة (async) ------

    public static <T> CompletableFuture<ApiResponse<T>> getAsync(String path, Class<T> responseType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return get(path, responseType);
            } catch (Exception e) {
                return createErrorResponse(e);
            }
        });
    }

    public static <T> CompletableFuture<ApiResponse<T>> getAsync(String path, Map<String, String> queryParams, Class<T> responseType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return get(path, queryParams, responseType);
            } catch (Exception e) {
                return createErrorResponse(e);
            }
        });
    }

    public static <T> CompletableFuture<ApiResponse<T>> postAsync(String path, Object body, Class<T> responseType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return post(path, body, responseType);
            } catch (Exception e) {
                return createErrorResponse(e);
            }
        });
    }

    public static <T> CompletableFuture<ApiResponse<T>> postFormAsync(String path, Map<String, String> formData, Class<T> responseType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return postForm(path, formData, responseType);
            } catch (Exception e) {
                return createErrorResponse(e);
            }
        });
    }

    public static <T> CompletableFuture<ApiResponse<T>> uploadFileAsync(String path, Path file, String fileParamName,
                                                                        Map<String, String> additionalData, Class<T> responseType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return uploadFile(path, file, fileParamName, additionalData, responseType);
            } catch (Exception e) {
                return createErrorResponse(e);
            }
        });
    }

    // ------ الطلب الأساسي (لإرسال JSON) ------
    private static <T> ApiResponse<T> sendRequest(String path, String method, Object body,
                                                  Map<String, String> headers, Class<T> responseType) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .timeout(TIMEOUT);

        // إضافة headers عامة
        builder.header("Content-Type", "application/json")
                .header("Accept", "application/json");

        // إضافة headers مخصصة (إن وجدت)
        if (headers != null) {
            headers.forEach(builder::header);
        }

        // إضافة التوكن إن وجد
        builder = addAuthHeader(builder);

        // تعيين method والـ body
        String jsonBody = body != null ? gson.toJson(body) : "";
        switch (method) {
            case "GET":
                builder.GET();
                break;
            case "POST":
                builder.POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8));
                break;
            case "PUT":
                builder.PUT(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8));
                break;
            case "DELETE":
                builder.DELETE();
                break;
            default:
                throw new IllegalArgumentException("Unsupported method: " + method);
        }

        HttpRequest request = builder.build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return parseResponse(response, responseType);
    }

    /**
     * تحميل ملف فوري (GET) مع إمكانية إضافة query parameters.
     *
     * @param path        مسار endpoint (قد يحتوي PathVariable).
     * @param queryParams باراميترز اختيارية (يمكن null).
     * @param targetPath  المسار الذي سيحفظ فيه الملف.
     * @return true إذا نجح التحميل، false مع رسالة الخطأ في استثناء.
     */
    public static boolean downloadFile(String path, Map<String, String> queryParams, Path targetPath) throws IOException, InterruptedException {
        String fullPath = appendQueryParams(path, queryParams);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + fullPath))
                .timeout(TIMEOUT)
                .GET();

        builder = addAuthHeader(builder);
        HttpRequest request = builder.build();

        HttpResponse<Path> response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(targetPath));
        return response.statusCode() >= 200 && response.statusCode() < 300;
    }

    /**
     * تحميل ملف فوري (POST) مع JSON body.
     *
     * @param path       مسار endpoint.
     * @param body       كائن JSON (سيحول إلى طلب).
     * @param targetPath المسار المحلي للحفظ.
     */
    public static boolean downloadFileViaPost(String path, Object body, Path targetPath) throws IOException, InterruptedException {
        String jsonBody = body != null ? gson.toJson(body) : "";
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json")
                .timeout(TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8));

        builder = addAuthHeader(builder);
        HttpRequest request = builder.build();

        HttpResponse<Path> response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(targetPath));
        return response.statusCode() >= 200 && response.statusCode() < 300;
    }

    // نسخة غير متزامنة من التحميل المباشر (مع إشعار بالتقدم)
    public static CompletableFuture<Boolean> downloadFileAsync(String path, Map<String, String> queryParams, Path targetPath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return downloadFile(path, queryParams, targetPath);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    // ------ تحليل الاستجابة ------
    private static <T> ApiResponse<T> parseResponse(HttpResponse<String> response, Class<T> responseType) {
        ApiResponse<T> apiResponse = new ApiResponse<>();
        String body = response.body();
        int statusCode = response.statusCode();

        try {
            // نحاول تحويل الاستجابة إلى ApiResponse مباشرة (إذا كان backend يعيدها)
            var type = TypeToken.getParameterized(ApiResponse.class, responseType).getType();
            ApiResponse<T> parsed = gson.fromJson(body, type);
            if (parsed != null) {
                // نجاح: نعيدها كما هي
                return parsed;
            }
        } catch (JsonSyntaxException e) {
            // إذا لم تكن الاستجابة بصيغة ApiResponse، نتعامل معها يدوياً
        }

        // إذا وصلنا هنا، فإما أن الاستجابة ليست ApiResponse أو حدث خطأ في التحليل
        apiResponse.setSuccess(statusCode >= 200 && statusCode < 300);
        apiResponse.setMessage(statusCode >= 200 && statusCode < 300 ? "Success" : "HTTP error " + statusCode);
        if (statusCode >= 200 && statusCode < 300 && responseType != null) {
            // محاولة تحويل الجسم مباشرة إلى النوع المطلوب
            try {
                T data = gson.fromJson(body, responseType);
                apiResponse.setData(data);
            } catch (JsonSyntaxException e) {
                apiResponse.setSuccess(false);
                apiResponse.setMessage("Failed to parse response: " + e.getMessage());
            }
        } else {
            // في حالة الخطأ، نضع الجسم كرسالة
            apiResponse.setMessage(body);
        }
        apiResponse.setTimestamp(java.time.LocalDateTime.now().toString());
        return apiResponse;
    }

    // ------ دوال مساعدة ------
    private static String appendQueryParams(String path, Map<String, String> params) {
        if (params == null || params.isEmpty()) return path;
        StringBuilder sb = new StringBuilder(path);
        if (!path.contains("?")) sb.append('?');
        else sb.append('&');
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (sb.charAt(sb.length() - 1) != '?' && sb.charAt(sb.length() - 1) != '&')
                sb.append('&');
            sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    private static String buildFormData(Map<String, String> formData) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : formData.entrySet()) {
            if (sb.length() > 0) sb.append('&');
            sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    private static HttpRequest.Builder addAuthHeader(HttpRequest.Builder builder) {
        if (authToken != null && !authToken.isEmpty()) {
            builder.header("Authorization", "Bearer " + authToken);
        }
        return builder;
    }

    private static HttpRequest addAuthHeader(HttpRequest request) {
        if (authToken != null && !authToken.isEmpty()) {
            return HttpRequest.newBuilder(request, (n, v) -> true)
                    .header("Authorization", "Bearer " + authToken)
                    .build();
        }
        return request;
    }

    private static <T> ApiResponse<T> createErrorResponse(Exception e) {
        ApiResponse<T> error = new ApiResponse<>();
        error.setSuccess(false);
        error.setMessage(e.getMessage());
        error.setTimestamp(java.time.LocalDateTime.now().toString());
        return error;
    }

    // ------ WebSocket ------
    public static class WebSocketClient {
        private WebSocket webSocket;
        private final URI serverUri;
        private final Consumer<String> onMessage;
        private final Consumer<Throwable> onError;
        private final Runnable onClose;

        public WebSocketClient(String path, Consumer<String> onMessage, Consumer<Throwable> onError, Runnable onClose) {
            this.serverUri = URI.create(BASE_URL.replace("http", "ws") + path);
            this.onMessage = onMessage;
            this.onError = onError;
            this.onClose = onClose;
        }

        public CompletableFuture<Void> connect() {
            return httpClient.newWebSocketBuilder()
                    .buildAsync(serverUri, new WebSocket.Listener() {
                        @Override
                        public void onOpen(WebSocket webSocket) {
                            WebSocketClient.this.webSocket = webSocket;
                            webSocket.request(1); // استعداد لاستقبال أول رسالة
                        }

                        @Override
                        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                            if (onMessage != null) {
                                onMessage.accept(data.toString());
                            }
                            webSocket.request(1);
                            return null;
                        }

                        @Override
                        public void onError(WebSocket webSocket, Throwable error) {
                            if (onError != null) {
                                onError.accept(error);
                            }
                        }

                        @Override
                        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                            if (onClose != null) {
                                onClose.run();
                            }
                            return null;
                        }
                    })
                    .thenAccept(ws -> this.webSocket = ws);
        }

        public void sendMessage(String message) {
            if (webSocket != null) {
                webSocket.sendText(message, true);
            }
        }

        public void close() {
            if (webSocket != null) {
                webSocket.sendClose(1000, "Closing");
            }
        }
    }

    // كلاس لتمثيل حالة المهمة
    public static class TaskStatus {
        private String taskId;
        private String status; // "IN_PROGRESS", "COMPLETED", "FAILED"
        private int progress; // 0-100
        private String message;
        private String downloadUrl; // رابط تحميل الملف النهائي (عند COMPLETED)

        // getters/setters
        public String getTaskId() {
            return taskId;
        }

        public void setTaskId(String taskId) {
            this.taskId = taskId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public int getProgress() {
            return progress;
        }

        public void setProgress(int progress) {
            this.progress = progress;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getDownloadUrl() {
            return downloadUrl;
        }

        public void setDownloadUrl(String downloadUrl) {
            this.downloadUrl = downloadUrl;
        }
    }

    /**
     * بدء مهمة تحميل غير متزامنة (POST).
     *
     * @param path endpoint لبدء المهمة.
     * @param body كائن يحتوي على باراميترز الطلب (قد يكون Map أو كيان).
     * @return TaskStatus يحوي taskId على الأقل.
     */
    public static TaskStatus startAsyncDownload(String path, Object body) throws IOException, InterruptedException {
        ApiResponse<TaskStatus> response = post(path, body, TaskStatus.class);
        if (response.isSuccess() && response.getData() != null) {
            return response.getData();
        }
        throw new IOException("Failed to start async download: " + response.getMessage());
    }

    /**
     * الاستعلام عن حالة مهمة.
     *
     * @param taskId معرف المهمة.
     */
    public static TaskStatus getDownloadStatus(String taskId) throws IOException, InterruptedException {
        ApiResponse<TaskStatus> response = get("/tasks/" + taskId + "/status", TaskStatus.class);
        if (response.isSuccess() && response.getData() != null) {
            return response.getData();
        }
        throw new IOException("Failed to get task status: " + response.getMessage());
    }

    /**
     * تحميل الملف الناتج بعد اكتمال المهمة.
     *
     * @param taskId     معرف المهمة.
     * @param targetPath المسار المحلي للحفظ.
     */
    public static boolean downloadCompletedFile(String taskId, Path targetPath) throws IOException, InterruptedException {
        // نفترض أن الرابط هو /tasks/{taskId}/download
        String path = "/tasks/" + taskId + "/download";
        return downloadFile(path, null, targetPath);
    }

    // دوال غير متزامنة مع استقصاء دوري (polling)
    public static CompletableFuture<Path> pollAsyncDownload(String path, Object body, Path targetDir, int pollIntervalSeconds) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // بدء المهمة
                TaskStatus startStatus = startAsyncDownload(path, body);
                String taskId = startStatus.getTaskId();

                // استقصاء الحالة حتى الاكتمال
                while (true) {
                    Thread.sleep(pollIntervalSeconds * 1000L);
                    TaskStatus status = getDownloadStatus(taskId);
                    if ("COMPLETED".equals(status.getStatus())) {
                        // تحميل الملف
                        Path filePath = targetDir.resolve("report_" + taskId + ".xlsx"); // اسم ديناميكي
                        boolean downloaded = downloadCompletedFile(taskId, filePath);
                        if (downloaded) return filePath;
                        else throw new IOException("Download failed after task completion");
                    } else if ("FAILED".equals(status.getStatus())) {
                        throw new IOException("Task failed: " + status.getMessage());
                    }
                    // استمر في الاستقصاء
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}