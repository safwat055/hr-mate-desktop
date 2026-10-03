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
import java.net.ConnectException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ══════════════════════════════════════════════════════════════════
 * HttpCore — نواة شبكة التطبيق (Singleton)
 * ══════════════════════════════════════════════════════════════════
 * <p>
 * ⭐ Timeouts:
 * <ul>
 *   <li>{@link #TIMEOUT} (45s): الافتراضي لكل النداءات العادية.</li>
 *   <li>{@link #SEARCH_TIMEOUT} (15s): للبحث والـ lookups السريعة —
 *       المستخدم مش المفروض يستنى 45 ثانية.</li>
 *   <li>{@link #CONNECT_TIMEOUT} (5s): فتح اتصال TCP. لو أخد أكتر من كده
 *       فالسيرفر غالبًا مش متاح أصلًا.</li>
 * </ul>
 * <p>
 * ⭐ Thread Pools (فصل مهم):
 * <ul>
 *   <li>{@link #HTTP_EXECUTOR}: للـ internals بتاعة الـ HttpClient بس
 *       (callbacks / تجميع الـ response). متستخدمهوش لتشغيل نداءات blocking.</li>
 *   <li>{@link #ASYNC_EXECUTOR}: لتشغيل أي نداء blocking
 *       ({@code ApiClient.*Async} / {@code UiAsync} / SmartSearchHelper).</li>
 * </ul>
 * ليه الفصل؟ {@code HttpClient.send()} بيستنى callbacks بتتنفذ على الـ
 * executor بتاع الـ client. لو شغّلنا نداءات blocking على نفس الـ pool
 * وكلها استنت، الـ callbacks مش هتلاقي thread فاضي → الـ pool يتسد
 * لحد ما الـ timeout يخلص.
 */
public final class HttpCore {

    static {
        // الـ JDK HttpClient بيحتفظ بالاتصالات الخاملة 20 دقيقة افتراضيًا.
        // أي firewall / router / سيرفر بيقفل الاتصال الخامل بدون ما يبلّغ
        // بيخلي أول طلب بعد فترة سكون يعلّق لحد الـ timeout (شائع على ويندوز).
        // لازم تتحط قبل أول استخدام للـ HttpClient.
        if (System.getProperty("jdk.httpclient.keepalive.timeout") == null) {
            System.setProperty("jdk.httpclient.keepalive.timeout", "30");
        }
    }

    // ─────────────────────────────────────────────
    //  Constants
    // ─────────────────────────────────────────────

    private static final DateTimeFormatter SERVER_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** الـ timeout الافتراضي لكل نداءات التطبيق العادية. */
    public static final Duration TIMEOUT = Duration.ofSeconds(45);

    /** timeout للبحث والـ lookups السريعة. */
    public static final Duration SEARCH_TIMEOUT = Duration.ofSeconds(45);

    /** timeout فتح الاتصال (TCP). */
    public static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);

    private static final int HTTP_THREADS = 6;
    private static final int ASYNC_THREADS = 12;

    private static ThreadFactory daemonFactory(String prefix) {
        AtomicInteger counter = new AtomicInteger(1);
        return r -> {
            Thread t = new Thread(r, prefix + counter.getAndIncrement());
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
        };
    }

    /** pool الـ internals بتاعة HttpClient فقط. */
    public static final ExecutorService HTTP_EXECUTOR =
            Executors.newFixedThreadPool(HTTP_THREADS, daemonFactory("hr-http-"));

    /** pool تشغيل النداءات الـ blocking بعيدًا عن الـ UI thread. */
    public static final ExecutorService ASYNC_EXECUTOR =
            Executors.newFixedThreadPool(ASYNC_THREADS, daemonFactory("hr-async-"));

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
    //  Shared Infrastructure
    // ─────────────────────────────────────────────

    public final ObjectMapper mapper;
    public final HttpClient httpClient;

    private final String baseUrl;    // http://host:port/api
    private final String baseWsUrl;  // ws://host:port/ws

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

        // HTTP/1.1: الاتصال هنا cleartext (http://) فمفيش فايدة من HTTP/2،
        // ومحاولة upgrade لـ h2c ممكن تعمل تأخير مع بعض السيرفرات/البروكسيات.
        // لو الباك إند ورا proxy بيدعم h2 على https رجّعها HTTP_2.
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(CONNECT_TIMEOUT)
                .executor(HTTP_EXECUTOR)
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

    public HttpRequest.Builder baseBuilder(String path) {
        return baseBuilder(path, TIMEOUT);
    }

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
    //  Lifecycle
    // ─────────────────────────────────────────────

    /**
     * يُغلق الـ pools بشكل نظيف. استدعيه مرة واحدة عند إغلاق التطبيق
     * (Application.stop() أو shutdown hook).
     */
    public static void shutdown() {
        ASYNC_EXECUTOR.shutdown();
        HTTP_EXECUTOR.shutdown();
        try {
            if (!ASYNC_EXECUTOR.awaitTermination(3, TimeUnit.SECONDS))
                ASYNC_EXECUTOR.shutdownNow();
            if (!HTTP_EXECUTOR.awaitTermination(3, TimeUnit.SECONDS))
                HTTP_EXECUTOR.shutdownNow();
        } catch (InterruptedException ex) {
            ASYNC_EXECUTOR.shutdownNow();
            HTTP_EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // ─────────────────────────────────────────────
    //  Error Factory
    // ─────────────────────────────────────────────

    public <T> ApiResponse<T> createErrorResponse(Exception e) {
        Throwable root = (e instanceof CompletionException && e.getCause() != null)
                ? e.getCause() : e;

        String message;
        if (root instanceof HttpTimeoutException) {
            message = "انتهت مهلة الاتصال بالسيرفر";
        } else if (root instanceof ConnectException) {
            message = "تعذر الاتصال بالسيرفر";
        } else if (root.getMessage() != null) {
            message = root.getMessage();
        } else {
            message = root.getClass().getSimpleName();
        }

        ApiResponse<T> error = new ApiResponse<>();
        error.setSuccess(false);
        error.setMessage(message);
        error.setTimestamp(LocalDateTime.now().toString());
        return error;
    }
}