package com.safwat.hr.controller.payroll.vocab.service;

import com.safwat.hr.controller.payroll.payrollApi.PayrollReviewApi;
import com.safwat.hr.controller.payroll.payrollApi.dto.EmployeeSearchResult;
import com.safwat.hr.network.HttpCore;
import com.safwat.hr.notification.model.HRNotification;
import com.safwat.hr.notification.service.NotificationService;
import com.safwat.hr.shared.PayrollRequest;
import com.safwat.hr.shared.util.DateUtils;
import com.safwat.hr.ui.controls.SAFNotification;
import javafx.application.Platform;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

/**
 * ⭐ threading:
 * <ul>
 *   <li>دوال الـ lookup ({@code searchEmployee} / {@code getEmployeeMonthsReview} /
 *       {@code getMonthRecordsName}) blocking — بتتنادّى من SmartSearchHelper
 *       على ASYNC_EXECUTOR.</li>
 *   <li>دوال التحميل ({@code download*Report}) بترجع {@link CompletableFuture}<Path>
 *       وبتشتغل على ASYNC_EXECUTOR. الإشعارات بترجع على الـ FX thread داخل الخدمة،
 *       والـ Controller هو اللي بيعرض المعاينة.</li>
 * </ul>
 */
public class PayrollVocabService {

    private final PayrollReviewApi payrollReviewApi = PayrollReviewApi.getInstance();

    /**
     * يمنع تشغيل أكتر من تحميل في نفس الوقت (ضغطات متكررة).
     */
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
    //  Downloads (non-blocking — بترجع Future بالمسار)
    // ─────────────────────────────────────────────

    /**
     * تقرير المراجعة الرئيسي للشهر (زرار PDF).
     */
    public CompletableFuture<Path> downloadMainReviewReport(String nationalId, String targetMonth, String name) {
        PayrollRequest request;
        try {
            request = PayrollRequest.builder().build();
            request.setNationalId(nationalId);
            request.setStartDate(DateUtils.getFirstDayOfMonth(targetMonth));
            request.setFormat("PDF");
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }

        return runDownload(target -> payrollReviewApi.downloadMainReviewReport(request, target), name);
    }

    /**
     * تقرير مراجعة لصرفية معينة داخل الشهر.
     */
    public CompletableFuture<Path> downloadCustomReviewReport(String nationalId, String payGroup,
                                                              String targetMonth, String name) {
        PayrollRequest request;
        try {
            request = PayrollRequest.builder()
                    .nationalId(nationalId)
                    .startDate(DateUtils.getFirstDayOfMonth(targetMonth))
                    .payGroup(payGroup)
                    .build();
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }

        return runDownload(target -> payrollReviewApi.downloadCustomReviewPDF(request, target), name);
    }

    /**
     * ينفّذ التحميل على ASYNC_EXECUTOR، ويرجع المسار بعد النجاح.
     * الإشعار بيتعرض على الـ FX thread.
     *
     * @param job بياخد المسار الهدف ويرجع true لو التحميل نجح
     */
    private CompletableFuture<Path> runDownload(Predicate<Path> job, String name) {
        if (!downloading.compareAndSet(false, true)) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("جاري تحميل تقرير آخر، برجاء الانتظار..."));
        }

        Platform.runLater(() -> SAFNotification.success("جاري تحميل الملف..."));

        return CompletableFuture
                .supplyAsync(() -> {
                    try {
                        Path dir = Paths.get(System.getProperty("user.dir"), "temp_downloads");
                        Files.createDirectories(dir);
                        Path target = dir.resolve("REVIEW_REPORT" + System.currentTimeMillis() + ".pdf");

                        if (!job.test(target)) {
                            throw new RuntimeException("فشل تحميل الملف");
                        }
                        return target;
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                }, HttpCore.ASYNC_EXECUTOR)
                .whenComplete((target, ex) -> Platform.runLater(() -> {
                    downloading.set(false);
                    if (ex == null) {
                        SAFNotification.withAction("هل تريد فتح التقرير الان ؟", target.toFile());
                        notifyPdfReady(target, name);
                    }
                }));
    }

    private void notifyPdfReady(Path target, String name) {
        NotificationService.getInstance().send(
                HRNotification.builder()
                        .type(HRNotification.NotificationType.SYSTEM)
                        .priority(HRNotification.Priority.HIGH)
                        .title("تقرير مراجعه")
                        .message(name)
                        .file(target.toString())
                        .sender("system")
                        .build()
        );
    }
}