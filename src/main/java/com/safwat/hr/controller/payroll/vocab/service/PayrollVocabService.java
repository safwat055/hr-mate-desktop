package com.safwat.hr.controller.payroll.vocab.service;

import com.safwat.hr.controller.payroll.payrollApi.PayrollReviewApi;
import com.safwat.hr.controller.payroll.payrollApi.dto.EmployeeSearchResult;
import com.safwat.hr.network.HttpCore;
import com.safwat.hr.notification.model.HRNotification;
import com.safwat.hr.notification.service.NotificationService;
import com.safwat.hr.shared.PayrollRequest;
import com.safwat.hr.shared.util.DateUtils;
import com.safwat.hr.ui.controls.SAFNotification;
import com.safwat.hr.ui.util.PDFView;
import javafx.application.Platform;
import javafx.scene.web.WebView;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.http.HttpTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

/**
 * ⭐ threading:
 * <ul>
 *   <li>دوال الـ lookup ({@code searchEmployee} / {@code getEmployeeMonthsReview} /
 *       {@code getMonthRecordsName}) blocking — بتتنادّى من SmartSearchHelper
 *       على ASYNC_EXECUTOR.</li>
 *   <li>دوال التحميل ({@code download*Report}) بتتنادّى من الـ FX thread
 *       وبترجع فورًا: التحميل على ASYNC_EXECUTOR، وكل ما يخص الـ UI
 *       (إشعارات / WebView) بيرجع على الـ FX thread.</li>
 * </ul>
 */
public class PayrollVocabService {

    private final PayrollReviewApi payrollReviewApi = PayrollReviewApi.getInstance();

    /** يمنع تشغيل أكتر من تحميل في نفس الوقت (ضغطات متكررة). */
    private final AtomicBoolean downloading = new AtomicBoolean(false);

    // ─────────────────────────────────────────────
    //  Lookups (blocking — لا تنادِها من FX thread)
    // ─────────────────────────────────────────────

    public List<EmployeeSearchResult> searchEmployee(String searchValue) {
        PayrollRequest request = PayrollRequest.builder()
                .searchValue(searchValue).build();
        return payrollReviewApi.searchInEmployee(request);
    }

    public List<String> getEmployeeMonthsReview(String nationalId) {
        PayrollRequest request = PayrollRequest.builder()
                .nationalId(nationalId)
                .build();
        return payrollReviewApi.getEmployeeMonthsReview(request);
    }

    public List<String> getMonthRecordsName(String nationalId, String month) {
        PayrollRequest request = PayrollRequest.builder()
                .nationalId(nationalId)
                .startDate(DateUtils.getFirstDayOfMonth(month))
                .build();
        return payrollReviewApi.getEmployeeMonthKeys(request);
    }

    // ─────────────────────────────────────────────
    //  Downloads (non-blocking — آمنة من FX thread)
    // ─────────────────────────────────────────────

    public void downloadMainReviewReport(String nationalId, String targetMonth, WebView webView, String name) {
        if (nationalId == null || nationalId.length() != 14) {
            SAFNotification.warning("يجب إدخال الرقم القومى او البحث عن قيمة أولا");
            return;
        }

        PayrollRequest request;
        try {
            request = PayrollRequest.builder().build();
            request.setNationalId(nationalId);
            request.setStartDate(DateUtils.getFirstDayOfMonth(targetMonth));
            request.setFormat("PDF");
        } catch (Exception e) {
            SAFNotification.error("حدث خطأ أثناء التحميل: " + e.getMessage());
            return;
        }

        runDownload(target -> payrollReviewApi.downloadMainReviewReport(request, target), webView, name);
    }

    public void downloadCustomReviewReport(String nationalId, String payGroup, String targetMonth, WebView webView, String name) {
        if (nationalId == null || nationalId.length() != 14) {
            SAFNotification.warning("يجب إدخال الرقم القومى او البحث عن قيمة أولا");
            return;
        }

        PayrollRequest request;
        try {
            request = PayrollRequest.builder()
                    .nationalId(nationalId)
                    .startDate(DateUtils.getFirstDayOfMonth(targetMonth))
                    .payGroup(payGroup)
                    .build();
        } catch (Exception e) {
            SAFNotification.error("حدث خطأ أثناء التحميل: " + e.getMessage());
            return;
        }

        runDownload(target -> payrollReviewApi.downloadCustomReviewPDF(request, target), webView, name);
    }

    /**
     * ينفّذ التحميل على ASYNC_EXECUTOR ثم يرجع للـ FX thread لعرض النتيجة.
     *
     * @param job بياخد المسار الهدف ويرجع true لو التحميل نجح
     */
    private void runDownload(Predicate<Path> job, WebView webView, String name) {
        if (!downloading.compareAndSet(false, true)) {
            SAFNotification.warning("جاري تحميل تقرير آخر، برجاء الانتظار...");
            return;
        }

        SAFNotification.success("جاري تحميل الملف...");

        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        Path dir = Paths.get(System.getProperty("user.dir"), "temp_downloads");
                        Files.createDirectories(dir);
                        Path target = dir.resolve("REVIEW_REPORT" + System.currentTimeMillis() + ".pdf");
                        return job.test(target) ? target : null;
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }, HttpCore.ASYNC_EXECUTOR)
                .whenComplete((target, ex) -> Platform.runLater(() -> {
                    downloading.set(false);

                    if (ex != null) {
                        SAFNotification.error("حدث خطأ أثناء التحميل: " + friendly(ex));
                        return;
                    }
                    if (target == null) {
                        SAFNotification.error("فشل تحميل الملف");
                        return;
                    }

                    SAFNotification.withAction("هل تريد فتح التقرير الان ؟", target.toFile());
                    openPDF(target, webView, name);
                }));
    }

    private static String friendly(Throwable ex) {
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) root = root.getCause();
        if (root instanceof HttpTimeoutException) return "انتهت مهلة الاتصال بالسيرفر";
        return root.getMessage() != null ? root.getMessage() : root.getClass().getSimpleName();
    }

    /** لازم تتنادّى على الـ FX thread (WebView). */
    private void openPDF(Path targetPath, WebView webView, String name) {
        PDFView.showIN(targetPath.toString(), webView);
        NotificationService.getInstance().send(
                HRNotification.builder()
                        .type(HRNotification.NotificationType.SYSTEM)
                        .priority(HRNotification.Priority.HIGH)
                        .title("تقرير مراجعه")
                        .message(name)
                        .file(targetPath.toString())
                        .sender("system")
                        .build()
        );
    }
}