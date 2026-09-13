package com.safwat.hr.network;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ══════════════════════════════════════════════════════════════════
 * FileTransferClient — رفع وتحميل الملفات
 * ══════════════════════════════════════════════════════════════════
 * <p>
 * ⭐ ملاحظة عن الـ Timeout: تمت إضافة overloads لـ {@code uploadFile}/
 * {@code uploadFileAsync} تقبل {@link Duration} مخصص، لاستخدامها في رفع
 * ملفات كبيرة أو عمليات طويلة (زي استعادة نسخة احتياطية أو تنفيذ سكريبت
 * SQL كبير) بدون التأثير على باقي عمليات رفع الملفات العادية في التطبيق،
 * اللي فضلت شغالة بالـ {@link HttpCore#TIMEOUT} الافتراضي (45 ثانية).
 */
public final class FileTransferClient {

    private FileTransferClient() {
    }

    private static HttpCore core() {
        return HttpCore.getInstance();
    }

    public record DownloadedFile(Path path, String fileName, Path tempDir) {
    }

    // ══════════════════════════════════════════════
    //  Upload — Multipart
    // ══════════════════════════════════════════════

    public static <T> ApiResponse<T> uploadFile(String path,
                                                Path file,
                                                String fileParamName,
                                                Map<String, String> additionalData,
                                                Class<T> responseType)
            throws IOException, InterruptedException {
        return uploadFile(path, file, fileParamName, additionalData, HttpCore.TIMEOUT, responseType);
    }

    /**
     * ⭐ جديد: رفع ملف واحد بـ timeout مخصص.
     */
    public static <T> ApiResponse<T> uploadFile(String path,
                                                Path file,
                                                String fileParamName,
                                                Map<String, String> additionalData,
                                                Duration timeout,
                                                Class<T> responseType)
            throws IOException, InterruptedException {
        Map<String, Object> formData = new HashMap<>();
        if (additionalData != null) formData.putAll(additionalData);
        formData.put(fileParamName, file);
        return uploadFile(path, formData, timeout, responseType);
    }

    public static <T> ApiResponse<T> uploadFile(String path,
                                                Map<String, Object> formData,
                                                Class<T> responseType)
            throws IOException, InterruptedException {
        return uploadFile(path, formData, HttpCore.TIMEOUT, responseType);
    }

    /**
     * ⭐ جديد: نفس منطق uploadFile الأساسي، بـ timeout قابل للتخصيص.
     */
    public static <T> ApiResponse<T> uploadFile(String path,
                                                Map<String, Object> formData,
                                                Duration timeout,
                                                Class<T> responseType)
            throws IOException, InterruptedException {
        HttpCore c = core();
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
                    content = c.mapper.writeValueAsString(value);
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

        HttpRequest request = c.addAuthHeader(
                        HttpRequest.newBuilder()
                                .uri(URI.create(c.getBaseUrl() + path))
                                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                                .header("Accept", "application/json")
                                .timeout(timeout)
                                .POST(HttpRequest.BodyPublishers.ofByteArrays(parts)))
                .build();

        return c.parseResponse(
                c.httpClient.send(request, HttpResponse.BodyHandlers.ofString()),
                responseType);
    }

    // ══════════════════════════════════════════════
    //  Download — GET
    // ══════════════════════════════════════════════

    public static boolean downloadFile(String path,
                                       Map<String, String> queryParams,
                                       Path targetPath)
            throws IOException, InterruptedException {
        HttpCore c = core();
        String fullPath = c.appendQueryParams(path, queryParams);

        HttpRequest request = c.addAuthHeader(
                        HttpRequest.newBuilder()
                                .uri(URI.create(c.getBaseUrl() + fullPath))
                                .timeout(HttpCore.TIMEOUT)
                                .GET())
                .build();

        HttpResponse<Path> response = c.httpClient.send(
                request, HttpResponse.BodyHandlers.ofFile(targetPath));
        return response.statusCode() >= 200 && response.statusCode() < 300;
    }

    // ══════════════════════════════════════════════
    //  Download — POST with body
    // ══════════════════════════════════════════════

    public static boolean downloadFileViaPost(String path,
                                              Object body,
                                              Path targetPath)
            throws IOException, InterruptedException {
        HttpCore c = core();
        String jsonBody = c.mapper.writeValueAsString(body);

        HttpRequest request = c.addAuthHeader(
                        HttpRequest.newBuilder()
                                .uri(URI.create(c.getBaseUrl() + path))
                                .header("Content-Type", "application/json")
                                .header("Accept", "application/octet-stream, application/pdf, application/*")
                                .timeout(HttpCore.TIMEOUT)
                                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8)))
                .build();

        HttpResponse<byte[]> response = c.httpClient.send(
                request, HttpResponse.BodyHandlers.ofByteArray());

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

    // ══════════════════════════════════════════════
    //  downloadInTempDir — التحميل للمجلد المؤقت
    // ══════════════════════════════════════════════

    public static DownloadedFile downloadInTempDir(String path,
                                                   Object body,
                                                   String extension)
            throws IOException, InterruptedException {
        HttpCore c = core();

        Path tempDir = ensureTempDownloadDir();
        String ext = (extension != null && !extension.isBlank()) ? extension : "bin";
        Path targetPath = tempDir.resolve("download_" + System.currentTimeMillis() + "." + ext);

        HttpRequest request = c.addAuthHeader(
                        HttpRequest.newBuilder()
                                .uri(URI.create(c.getBaseUrl() + path))
                                .header("Content-Type", "application/json")
                                .header("Accept", "application/pdf, application/octet-stream, application/*")
                                .timeout(HttpCore.TIMEOUT)
                                .POST(HttpRequest.BodyPublishers.ofString(
                                        c.mapper.writeValueAsString(body), StandardCharsets.UTF_8)))
                .build();

        HttpResponse<Path> response = c.httpClient.send(
                request, HttpResponse.BodyHandlers.ofFile(targetPath));

        int status = response.statusCode();

        if (status < 200 || status >= 300) {
            Files.deleteIfExists(targetPath);
            throw new IOException("Download failed with HTTP " + status);
        }

        if (!Files.exists(targetPath) || Files.size(targetPath) == 0) {
            Files.deleteIfExists(targetPath);
            throw new IOException("Downloaded file is empty or missing");
        }

        String serverName = extractFileName(response.headers(), targetPath);

        Path finalPath = targetPath;
        if (!serverName.equals(targetPath.getFileName().toString())) {
            Path renamed = tempDir.resolve(serverName);
            if (Files.exists(renamed)) {
                String base = serverName;
                String fileExt = "";
                int dot = serverName.lastIndexOf('.');
                if (dot > 0) {
                    base = serverName.substring(0, dot);
                    fileExt = serverName.substring(dot);
                }
                renamed = tempDir.resolve(base + "_" + System.currentTimeMillis() + fileExt);
            }
            try {
                Files.move(targetPath, renamed, StandardCopyOption.REPLACE_EXISTING);
                finalPath = renamed;
            } catch (IOException ignored) { /* نحتفظ بالاسم الأصلي */ }
        }

        return new DownloadedFile(finalPath, serverName, tempDir);
    }

    // ══════════════════════════════════════════════
    //  Async Wrappers
    // ══════════════════════════════════════════════

    public static <T> CompletableFuture<ApiResponse<T>> uploadFileAsync(
            String path,
            Path file,
            String fileParamName,
            Map<String, String> additionalData,
            Class<T> responseType) {
        return uploadFileAsync(path, file, fileParamName, additionalData, HttpCore.TIMEOUT, responseType);
    }

    /**
     * ⭐ جديد: نسخة async من رفع الملف بـ timeout مخصص.
     */
    public static <T> CompletableFuture<ApiResponse<T>> uploadFileAsync(
            String path,
            Path file,
            String fileParamName,
            Map<String, String> additionalData,
            Duration timeout,
            Class<T> responseType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return uploadFile(path, file, fileParamName, additionalData, timeout, responseType);
            } catch (Exception e) {
                return core().createErrorResponse(e);
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

    public static CompletableFuture<Boolean> downloadFileViaPostAsync(String path,
                                                                      Object body,
                                                                      Path targetPath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return downloadFileViaPost(path, body, targetPath);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static CompletableFuture<DownloadedFile> downloadInTempDirAsync(String path,
                                                                           Object body,
                                                                           String extension) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return downloadInTempDir(path, body, extension);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    // ══════════════════════════════════════════════
    //  Private Helpers
    // ══════════════════════════════════════════════

    private static Path ensureTempDownloadDir() throws IOException {
        Path dir = Paths.get(System.getProperty("user.dir"), "temp_downloads")
                .toAbsolutePath().normalize();
        if (!Files.exists(dir)) Files.createDirectories(dir);
        return dir;
    }

    private static String extractFileName(HttpHeaders headers, Path fallback) {
        Optional<String> cd = headers.firstValue("Content-Disposition");
        if (cd.isEmpty()) return fallback.getFileName().toString();

        String value = cd.get();

        Matcher m1 = Pattern.compile(
                "filename\\*\\s*=\\s*[^']*'[^']*'([^;]+)",
                Pattern.CASE_INSENSITIVE).matcher(value);
        if (m1.find()) {
            try {
                return URLDecoder.decode(m1.group(1).trim(), StandardCharsets.UTF_8);
            } catch (Exception ignored) {
            }
        }

        Matcher m2 = Pattern.compile(
                "filename\\s*=\\s*\"?([^\";]+)\"?",
                Pattern.CASE_INSENSITIVE).matcher(value);
        if (m2.find()) {
            String name = m2.group(1).trim();
            try {
                return URLDecoder.decode(name, StandardCharsets.UTF_8);
            } catch (Exception ignored) {
                return name;
            }
        }

        return fallback.getFileName().toString();
    }
}