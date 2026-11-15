package com.safwat.hr.controller.payroll.records;

import com.safwat.hr.controller.payroll.payrollApi.PayrollReviewApi;
import com.safwat.hr.controller.payroll.payrollApi.dto.EmployeeSearchResult;
import com.safwat.hr.controller.payroll.payrollApi.dto.ViewMainRecordForRangeDate;
import com.safwat.hr.controller.payroll.payrollApi.dto.ViewNonPrimaryRangeDate;
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

/**
 * ⭐ threading:
 * <ul>
 *   <li>دوال الـ lookup والـ records بتفضل blocking، وبتتنادّى من SmartSearchHelper.</li>
 *   <li>دوال الـ PDF بترجع {@link CompletableFuture}<Path> وبتشتغل على ASYNC_EXECUTOR.
 *       الإشعار بيتعرض على الـ FX thread، والكنترولر هو اللي بيعرض المعاينة.</li>
 * </ul>
 */
public class PayrollRecordsService {

    private static final String TEMP_DIR = "temp_downloads";

    private final PayrollReviewApi payrollReviewApi = PayrollReviewApi.getInstance();

    /**
     * تنفيذ تحميل PDF على المسار المحدد — بيرجع true لو نجح.
     */
    @FunctionalInterface
    private interface PdfJob {
        boolean run(Path target) throws Exception;
    }

    // ─────────────────────────────────────────────
    //  Lookups (blocking)
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

    public List<String> getMonthReviewPayments(String nationalId, String month) {
        PayrollRequest request = PayrollRequest.builder()
                .nationalId(nationalId)
                .startDate(DateUtils.getFirstDayOfMonth(month))
                .build();
        return payrollReviewApi.getEmployeeMonthKeys(request);
    }

    public ViewMainRecordForRangeDate getMainMonthRecords(String nationalId, String startMonth, String endMonth) {
        PayrollRequest request = PayrollRequest.builder()
                .nationalId(nationalId)
                .startDate(DateUtils.getFirstDayOfMonth(startMonth))
                .endDate(DateUtils.getLastDayOfMonth(endMonth))
                .build();
        return payrollReviewApi.getMainMonthRecords(request);
    }

    public ViewNonPrimaryRangeDate getNonPrimaryMonthRecords(String nationalId, String startMonth, String endMonth) {
        if (nationalId == null || startMonth == null || endMonth == null) {
            SAFNotification.error("Invalid parameters");
            throw new IllegalArgumentException("Invalid parameters");
        }
        PayrollRequest request = PayrollRequest.builder()
                .nationalId(nationalId)
                .startDate(DateUtils.getFirstDayOfMonth(startMonth))
                .endDate(DateUtils.getFirstDayOfMonth(endMonth))
                .build();
        return payrollReviewApi.getNonPrimaryMonthRecords(request);
    }

    // ─────────────────────────────────────────────
    //  PDF (non-blocking — بترجع Path)
    // ─────────────────────────────────────────────

    /**
     * تقرير المراجعة الرئيسي للشهر (زرار PDF في الصرفيات الرئيسية).
     */
    public CompletableFuture<Path> downloadMainReviewReport(String nationalId, String targetMonth, String empName) {
        PayrollRequest request;
        try {
            request = PayrollRequest.builder().build();
            request.setNationalId(nationalId);
            request.setStartDate(DateUtils.getFirstDayOfMonth(targetMonth));
            request.setFormat("PDF");
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }

        return downloadPdf("REVIEW_REPORT", "تقرير مراجعة", empName,
                target -> payrollReviewApi.downloadMainReviewReport(request, target));
    }

    /**
     * تقرير مراجعة لصرفية معينة داخل الشهر (زرار PDF في الصرفيات المنفصلة).
     */
    public CompletableFuture<Path> downloadCustomReviewToPDF(String month, String payGroup,
                                                             String nationalId, String empName) {
        PayrollRequest request;
        try {
            request = PayrollRequest.builder()
                    .nationalId(nationalId)
                    .startDate(DateUtils.getFirstDayOfMonth(month))
                    .payGroup(payGroup)
                    .build();
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }

        return downloadPdf("REVIEW_REPORT", "تقرير مراجعة", empName,
                target -> payrollReviewApi.downloadCustomReviewPDF(request, target));
    }

    /**
     * تقرير تعديلات الراتب (زرار ملاحظات).
     */
    public CompletableFuture<Path> compareExportToPDF(String month, String nationalId, String empName) {
        PayrollRequest request;
        try {
            request = PayrollRequest.builder()
                    .nationalId(nationalId)
                    .startDate(DateUtils.getFirstDayOfMonth(month))
                    .build();
            request.setFormat("PDF");
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }

        return downloadPdf("COMPARE_REPORT", "تقرير تعديلات الراتب", empName,
                target -> payrollReviewApi.downloadComparePDF(request, target));
    }

    /**
     * الفلو المشترك: تحميل على ASYNC_EXECUTOR، وبعدين إشعار على الـ FX thread.
     */
    private CompletableFuture<Path> downloadPdf(String prefix, String reportName, String empName, PdfJob job) {
        return CompletableFuture
                .supplyAsync(() -> {
                    try {
                        Path dir = Paths.get(System.getProperty("user.dir"), TEMP_DIR);
                        Files.createDirectories(dir);
                        Path target = dir.resolve(prefix + System.currentTimeMillis() + ".pdf");

                        if (!job.run(target)) {
                            throw new RuntimeException("فشل تحميل الملف");
                        }
                        return target;
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                }, HttpCore.ASYNC_EXECUTOR)
                .whenComplete((target, ex) -> {
                    if (ex == null) {
                        Platform.runLater(() -> notifyPdfReady(target, reportName, empName));
                    }
                });
    }

    private void notifyPdfReady(Path target, String reportName, String empName) {
        SAFNotification.withAction("هل تريد فتح التقرير الان ؟", target.toFile());
        NotificationService.getInstance().send(
                HRNotification.builder()
                        .type(HRNotification.NotificationType.SYSTEM)
                        .priority(HRNotification.Priority.HIGH)
                        .title(reportName)
                        .message(empName)
                        .file(target.toString())
                        .sender("system")
                        .build()
        );
    }

    // ─────────────────────────────────────────────
    //  طلب تقرير السجل (129) — مش PDF، بيرجع رقم الطلب
    // ─────────────────────────────────────────────

    public void downloadRecord_129(String nationalId, String startDate, String endDate) {
        if (nationalId == null || nationalId.isEmpty()) {
            SAFNotification.warning("يجب اختيار موظف اولا");
            return;
        }
        if (startDate == null || endDate == null || startDate.isBlank() || endDate.isBlank()) {
            SAFNotification.warning("يجب تحديد فترة بداية و نهاية");
            return;
        }
        PayrollRequest request = PayrollRequest.builder()
                .report("full_record")
                .nationalId(nationalId)
                .startDate(DateUtils.getFirstDayOfMonth(startDate))
                .endDate(DateUtils.getFirstDayOfMonth(endDate))
                .build();
        Long reportId = payrollReviewApi.downloadRecord_129(request);
        SAFNotification.success("تم تقديم الطلب بنجاح رقم الطلب : " + reportId);
    }
}