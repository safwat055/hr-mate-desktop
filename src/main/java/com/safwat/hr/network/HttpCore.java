package com.safwat.hr.network;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.safwat.hr.shared.AppConfig;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * ══════════════════════════════════════════════════════════════════
 * HttpCore — نواة شبكة التطبيق (Singleton)
 * ══════════════════════════════════════════════════════════════════
 * <p>
 * ⭐ ملاحظة عن الـ Timeout:
 * {@link #TIMEOUT} هو الـ timeout الافتراضي المستخدم في كل نداءات
 * التطبيق العادية (45 ثانية) — ولسه هو نفسه المستخدم في كل مكان في
 * البرنامج ما عدا الباك أب.
 * <p>
 * العمليات الطويلة (نسخ احتياطي / استعادة / تنفيذ سكريبتات SQL) بتحتاج
 * timeout أطول بكتير، فبدل ما نغيّر القيمة العامة (وده هيأثر على كل
 * endpoint في التطبيق)، أضفنا overloads في {@code baseBuilder} تقبل
 * {@link Duration} مخصص. كل الاستخدامات القديمة فضلت شغالة زي ما هي
 * بالـ 45 ثانية الافتراضية — التغيير ده backward-compatible 100%.
 */
public final class HttpCore {

    // ─────────────────────────────────────────────
    //  Constants
    // ─────────────────────────────────────────────

    private static final DateTimeFormatter SERVER_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * الـ timeout الافتراضي لكل نداءات التطبيق العادية.
     */
    public static final Duration TIMEOUT = Duration.ofSeconds(45);

    // ─────────────────────────────────────────────
    //  Singleton
    // ─────────────────────────────────────────────

    private static volatile HttpCore instance;

    public static HttpCore getInstance() {
        if (instance == null) {
            synchronized (HttpCore.class) {
                if (instance == null) instance = new HttpCore();
            }
        }
        return instance;
    }

    // ─────────────────────────────────────────────
    //  Shared Infrastructure (بنيّة تحتية مشتركة)
    // ─────────────────────────────────────────────

    public final ObjectMapper mapper;
    public final HttpClient httpClient;

    // ─────────────────────────────────────────────
    //  Connection Config (مقروءة من AppConfig مرة واحدة)
    // ─────────────────────────────────────────────

    private final String baseUrl;    // http://host:port/api
    private final String baseWsUrl;  // ws://host:port/ws

    // ─────────────────────────────────────────────
    //  Auth State
    // ─────────────────────────────────────────────

    private volatile String authToken;

    // ─────────────────────────────────────────────
    //  Constructor (private)
    // ─────────────────────────────────────────────

    private HttpCore() {
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule()
                        .addDeserializer(java.time.LocalDateTime.class,
                                new LocalDateTimeDeserializer(SERVER_DATE_FORMAT)))
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // ⭐ ملاحظة: الـ connectTimeout بتاع الـ HttpClient نفسه (زمن فتح
        // الاتصال TCP) بيفضل ثابت على TIMEOUT العادي — ده منطقي لأن فتح
        // الاتصال المفروض يكون سريع دايمًا حتى لو الطلب نفسه هياخد وقت
        // طويل بعد كده. الـ per-request timeout (اللي بيتغير) هو اللي بيتحكم
        // في مدة انتظار الاستجابة الكاملة.
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(TIMEOUT)
                .build();

        String url = AppConfig.getString("connection", "url", "http://");
        String wsUrl = AppConfig.getString("connection", "url2", "ws://");
        String masterPC = AppConfig.getString("connection", "masterPC", "localhost");
        String port = AppConfig.getString("connection", "port", "8080");

        this.baseUrl = url + masterPC + ":" + port + "/api";
        this.baseWsUrl = wsUrl + masterPC + ":" + port + "/ws";
    }

    // ─────────────────────────────────────────────
    //  URL Accessors
    // ─────────────────────────────────────────────

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getBaseWsUrl() {
        return baseWsUrl;
    }

    // ─────────────────────────────────────────────
    //  Auth Token Management
    // ─────────────────────────────────────────────

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String tok) {
        this.authToken = tok;
    }

    public void clearAuthToken() {
        this.authToken = null;
    }

    public boolean isAuthenticated() {
        return authToken != null && !authToken.isBlank();
    }

    // ─────────────────────────────────────────────
    //  Request Building
    // ─────────────────────────────────────────────

    /**
     * يبني Builder أساسي بـ Content-Type و Accept و Timeout الافتراضي.
     */
    public HttpRequest.Builder baseBuilder(String path) {
        return baseBuilder(path, TIMEOUT);
    }

    /**
     * ⭐ نفس الأساسي، لكن بـ timeout مخصص — مستخدم في العمليات الطويلة
     * زي الباك أب والاستعادة وتنفيذ سكريبتات SQL.
     */
    public HttpRequest.Builder baseBuilder(String path, Duration timeout) {
        return HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");
    }

    public HttpRequest.Builder addAuthHeader(HttpRequest.Builder builder) {
        if (isAuthenticated())
            builder.header("Authorization", "Bearer " + authToken);
        return builder;
    }

    public HttpRequest addAuthHeader(HttpRequest request) {
        if (isAuthenticated())
            return HttpRequest.newBuilder(request, (n, v) -> true)
                    .header("Authorization", "Bearer " + authToken)
                    .build();
        return request;
    }

    public void applyMethod(HttpRequest.Builder builder,
                            String method,
                            Object body) throws IOException {
        String json = body != null ? mapper.writeValueAsString(body) : "";
        switch (method) {
            case "GET" -> builder.GET();
            case "POST" -> builder.POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
            case "PUT" -> builder.PUT(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
            case "DELETE" -> {
                if (body != null)
                    builder.method("DELETE", HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
                else
                    builder.DELETE();
            }
            default -> throw new IllegalArgumentException("Unsupported method: " + method);
        }
    }

    // ─────────────────────────────────────────────
    //  Response Parsing
    // ─────────────────────────────────────────────

    public <T> ApiResponse<T> parseResponse(HttpResponse<String> response,
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
        apiResponse.setTimestamp(LocalDateTime.now().toString());

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

    public <T> ApiResponse<T> parseResponse(HttpResponse<String> response,
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
        apiResponse.setTimestamp(LocalDateTime.now().toString());

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

    // ─────────────────────────────────────────────
    //  URL Helpers
    // ─────────────────────────────────────────────

    public String appendQueryParams(String path, Map<String, String> params) {
        if (params == null || params.isEmpty()) return path;
        StringBuilder sb = new StringBuilder(path);
        sb.append(path.contains("?") ? '&' : '?');
        params.forEach((k, v) -> {
            char last = sb.charAt(sb.length() - 1);
            if (last != '?' && last != '&') sb.append('&');
            sb.append(URLEncoder.encode(k, StandardCharsets.UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(v, StandardCharsets.UTF_8));
        });
        return sb.toString();
    }

    public String buildFormData(Map<String, String> formData) {
        StringBuilder sb = new StringBuilder();
        formData.forEach((k, v) -> {
            if (!sb.isEmpty()) sb.append('&');
            sb.append(URLEncoder.encode(k, StandardCharsets.UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(v, StandardCharsets.UTF_8));
        });
        return sb.toString();
    }

    // ─────────────────────────────────────────────
    //  Error Factory
    // ─────────────────────────────────────────────

    public <T> ApiResponse<T> createErrorResponse(Exception e) {
        ApiResponse<T> error = new ApiResponse<>();
        error.setSuccess(false);
        error.setMessage(e.getMessage());
        error.setTimestamp(LocalDateTime.now().toString());
        return error;
    }
}