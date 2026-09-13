package com.safwat.hr.network;

import com.fasterxml.jackson.core.type.TypeReference;
import com.safwat.hr.network.dto.*;
import com.safwat.hr.shared.PayrollRequest;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * ══════════════════════════════════════════════════════════════════
 * ReportApiClient — منطق الـ Reports (Business Layer)
 * ══════════════════════════════════════════════════════════════════
 * <p>
 * مسؤول عن كل endpoints الـ /api/reports:
 * — إرسال طلب تقرير (submitReport)
 * — الاستعلام عن الحالة (getReportStatus)
 * — Polling حتى الانتهاء (pollReportUntilDone)
 * — تحميل ملف التقرير (downloadReportFile)
 * — Async polling كامل (pollAsync)
 * <p>
 * ملاحظة: يستخدم ApiClient و FileTransferClient داخليًا.
 * لا يحتوي على أي منطق HTTP مباشر.
 */
public final class ReportApiClient {

    private ReportApiClient() {
    }

    private static final String BASE = "/reports";

    // ══════════════════════════════════════════════
    //  Queries
    // ══════════════════════════════════════════════

    /**
     * يجلب كل التقارير الخاصة بالمستخدم الحالي.
     */
    public static ApiResponse<List<ReportStatusResponse>> getMyReports()
            throws IOException, InterruptedException {
        return ApiClient.getWithTypeRef(
                BASE + "/my",
                new TypeReference<List<ReportStatusResponse>>() {
                });
    }

    /**
     * يجلب حالة تقرير معين.
     */
    public static ApiResponse<ReportStatusResponse> getReportStatus(Long reportId)
            throws IOException, InterruptedException {
        return ApiClient.get(BASE + "/" + reportId, ReportStatusResponse.class);
    }

    /**
     * يجلب قائمة التقارير المتاحة (الأنواع).
     */
    public static ApiResponse<List<AvailableReportInfo>> getAvailableReports()
            throws IOException, InterruptedException {
        return ApiClient.getWithTypeRef(
                BASE + "/available",
                new TypeReference<List<AvailableReportInfo>>() {
                });
    }

    /**
     * يجلب payload تقرير معين.
     */
    public static ApiResponse<ReportPayloadResponse> getReportPayload(Long reportId)
            throws IOException, InterruptedException {
        return ApiClient.getWithTypeRef(
                BASE + "/" + reportId + "/payload",
                new TypeReference<ReportPayloadResponse>() {
                });
    }

    /**
     * يجلب payload آخر تقرير.
     */
    public static ApiResponse<ReportPayloadResponse> getLastReportPayload()
            throws IOException, InterruptedException {
        return ApiClient.getWithTypeRef(
                BASE + "/lastReport",
                new TypeReference<ReportPayloadResponse>() {
                });
    }

    // ══════════════════════════════════════════════
    //  Submit
    // ══════════════════════════════════════════════

    /**
     * يرسل طلب تقرير بسيط (بدون ملفات مرفقة).
     */
    public static ApiResponse<ReportSubmissionResult> submitReport(Object requestBody)
            throws IOException, InterruptedException {
        return ApiClient.post(BASE, requestBody, ReportSubmissionResult.class);
    }

    /**
     * يرسل طلب تقرير مع ملفات مرفقة (multipart/form-data).
     *
     * @param request كائن الـ PayrollRequest (يُرسَل كـ JSON في حقل "data")
     * @param files   قائمة الملفات المرفقة (nullable أو فارغة)
     */
    public static ApiResponse<ReportSubmissionResult> submitReport(PayrollRequest request,
                                                                   List<Path> files)
            throws IOException, InterruptedException {
        HttpCore c = HttpCore.getInstance();
        String boundary = "---Boundary" + System.currentTimeMillis();
        var parts = new ArrayList<byte[]>();

        // ── الـ JSON data ──
        String json = c.mapper.writeValueAsString(request);
        parts.add(("--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"data\"\r\n" +
                "Content-Type: application/json\r\n\r\n")
                .getBytes(StandardCharsets.UTF_8));
        parts.add(json.getBytes(StandardCharsets.UTF_8));
        parts.add("\r\n".getBytes(StandardCharsets.UTF_8));

        // ── الملفات المرفقة ──
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

        HttpRequest httpRequest = c.addAuthHeader(
                        HttpRequest.newBuilder()
                                .uri(URI.create(c.getBaseUrl() + BASE))
                                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                                .header("Accept", "application/json")
                                .timeout(HttpCore.TIMEOUT)
                                .POST(HttpRequest.BodyPublishers.ofByteArrays(parts)))
                .build();

        return c.parseResponse(
                c.httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString()),
                ReportSubmissionResult.class);
    }

    // ══════════════════════════════════════════════
    //  Cancel
    // ══════════════════════════════════════════════

    /**
     * يلغي تقريرًا قيد التشغيل.
     */
    public static ApiResponse<Void> cancelReport(Long reportId)
            throws IOException, InterruptedException {
        return ApiClient.post(BASE + "/" + reportId + "/cancel", null, Void.class);
    }

    // ══════════════════════════════════════════════
    //  Download
    // ══════════════════════════════════════════════

    /**
     * يحمّل ملف تقرير مكتمل إلى مسار محدد.
     *
     * @return true إذا نجح التحميل
     */
    public static boolean downloadReportFile(Long reportId, Path targetPath)
            throws IOException, InterruptedException {
        return FileTransferClient.downloadFile(
                "/download/" + reportId + "/file", null, targetPath);
    }

    // ══════════════════════════════════════════════
    //  Polling — Sync
    // ══════════════════════════════════════════════

    /**
     * يستطلع حالة تقرير بشكل دوري حتى ينتهي أو يفشل.
     *
     * @param reportId            معرّف التقرير
     * @param pollIntervalSeconds الفترة بين كل استطلاع (بالثواني)
     * @param maxAttempts         أقصى عدد محاولات قبل رمي IOException
     * @return {@link ReportStatusResponse} بالحالة النهائية
     * @throws IOException إذا انتهى الوقت أو فشل الاتصال
     */
    public static ReportStatusResponse pollReportUntilDone(Long reportId,
                                                           int pollIntervalSeconds,
                                                           int maxAttempts)
            throws IOException, InterruptedException {
        for (int i = 0; i < maxAttempts; i++) {
            ApiResponse<ReportStatusResponse> response = getReportStatus(reportId);

            if (!response.isSuccess() || response.getData() == null)
                throw new IOException("Failed to get report status: " + response.getMessage());

            ReportStatusResponse status = response.getData();
            String currentStatus = status.getStatus();

            if ("COMPLETED".equals(currentStatus)
                    || "FAILED".equals(currentStatus)
                    || "CANCELLED".equals(currentStatus)) {
                return status;
            }
            Thread.sleep(pollIntervalSeconds * 1000L);
        }
        throw new IOException("Report polling timed out after " + maxAttempts + " attempts");
    }

    // ══════════════════════════════════════════════
    //  Polling — Async (submit → poll → download)
    // ══════════════════════════════════════════════

    /**
     * يرسل طلب تقرير ثم يستطلع حتى الانتهاء ثم يحمّل الملف — كله async.
     *
     * @param requestBody         body الطلب
     * @param targetPath          مسار حفظ الملف بعد الانتهاء
     * @param pollIntervalSeconds الفترة بين الاستطلاعات
     * @param maxAttempts         أقصى عدد محاولات
     * @return CompletableFuture<Path> مسار الملف المحمّل
     */
    public static CompletableFuture<Path> submitAndDownloadAsync(Object requestBody,
                                                                 Path targetPath,
                                                                 int pollIntervalSeconds,
                                                                 int maxAttempts) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // ① إرسال الطلب
                ApiResponse<ReportSubmissionResult> submission = submitReport(requestBody);
                if (!submission.isSuccess() || submission.getData() == null)
                    throw new IOException("Failed to submit report: " + submission.getMessage());

                Long reportId = submission.getData().getReportId();

                // ② Polling
                ReportStatusResponse finalStatus =
                        pollReportUntilDone(reportId, pollIntervalSeconds, maxAttempts);

                if (!"COMPLETED".equals(finalStatus.getStatus()))
                    throw new IOException("Report ended with status: " + finalStatus.getStatus());

                // ③ تحميل الملف
                if (downloadReportFile(reportId, targetPath))
                    return targetPath;

                throw new IOException("File download failed after report completion");

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    // ══════════════════════════════════════════════
    //  Legacy: Task-based Async Download
    // ══════════════════════════════════════════════

    /**
     * @deprecated استخدم {@link #submitAndDownloadAsync} بدلًا منه.
     */
    @Deprecated
    public static TaskStatus startAsyncDownload(String path, Object body)
            throws IOException, InterruptedException {
        ApiResponse<TaskStatus> response = ApiClient.post(path, body, TaskStatus.class);
        if (response.isSuccess() && response.getData() != null) return response.getData();
        throw new IOException("Failed to start async download: " + response.getMessage());
    }

    /**
     * @deprecated استخدم {@link #submitAndDownloadAsync} بدلًا منه.
     */
    @Deprecated
    public static TaskStatus getDownloadStatus(String taskId)
            throws IOException, InterruptedException {
        ApiResponse<TaskStatus> response = ApiClient.get("/tasks/" + taskId + "/status", TaskStatus.class);
        if (response.isSuccess() && response.getData() != null) return response.getData();
        throw new IOException("Failed to get task status: " + response.getMessage());
    }

    /**
     * @deprecated استخدم {@link #submitAndDownloadAsync} بدلًا منه.
     */
    @Deprecated
    public static boolean downloadCompletedFile(String taskId, Path targetPath)
            throws IOException, InterruptedException {
        return FileTransferClient.downloadFile("/tasks/" + taskId + "/download", null, targetPath);
    }

    /**
     * @deprecated استخدم {@link #submitAndDownloadAsync} بدلًا منه.
     */
    @Deprecated
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
}