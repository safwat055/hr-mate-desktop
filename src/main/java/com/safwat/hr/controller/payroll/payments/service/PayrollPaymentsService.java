package com.safwat.hr.controller.payroll.payments.service;

import com.safwat.hr.controller.payroll.payrollApi.PayrollReviewApi;
import com.safwat.hr.controller.payroll.payrollApi.PayrollYearlyApi;
import com.safwat.hr.controller.payroll.payrollApi.dto.EmployeeSearchResult;
import com.safwat.hr.controller.payroll.payrollApi.dto.PaymentsView;
import com.safwat.hr.notification.model.HRNotification;
import com.safwat.hr.notification.service.NotificationService;
import com.safwat.hr.shared.PayrollRequest;
import com.safwat.hr.shared.util.DateUtils;
import com.safwat.hr.ui.controls.SAFNotification;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static com.safwat.hr.shared.util.StringUtil.convertArabicToEnglishNumbers;

@Slf4j
public class PayrollPaymentsService {

    private static final String NATIONAL_ID_REQUIRED = "يجب ادخال الرقم القومى او البحث عن قيمة اولا";

    private final PayrollReviewApi payrollReviewApi = PayrollReviewApi.getInstance();
    private final PayrollYearlyApi payrollYearlyApi = PayrollYearlyApi.getInstance();
    private static PayrollPaymentsService instance;

    private PayrollPaymentsService() {
    }

    public static PayrollPaymentsService getInstance() {
        if (instance == null) {
            instance = new PayrollPaymentsService();
        }
        return instance;
    }

    // ═════════════════════════════════════════════════════════════
    //  البحث والبيانات
    // ═════════════════════════════════════════════════════════════

    public List<EmployeeSearchResult> searchEmployee(String searchValue) {
        PayrollRequest request = PayrollRequest.builder()
                .searchValue(searchValue).build();
        return payrollYearlyApi.searchInEmployee(request);
    }

    public List<String> getEmployeeMonths(String nationalId) {
        PayrollRequest request = PayrollRequest.builder()
                .nationalId(nationalId)
                .build();
        return payrollYearlyApi.getEmployeeMonths(request);
    }

    public List<PaymentsResult> getPaymentsData(String nationalId, String startMonth, String endDate) {
        if (nationalId.length() != 14) {
            SAFNotification.warning(NATIONAL_ID_REQUIRED);
            throw new IllegalArgumentException(NATIONAL_ID_REQUIRED);
        }
        PayrollRequest request = PayrollRequest.builder().build();
        request.setNationalId(nationalId);
        request.setStartDate(DateUtils.getFirstDayOfMonth(startMonth));
        request.setEndDate(DateUtils.getLastDayOfMonth(endDate));
        PaymentsView data = payrollYearlyApi.getPaymentsData(request);

        return createResultList(data);
    }

    @NotNull
    private static List<PaymentsResult> createResultList(PaymentsView data) {
        List<PaymentsResult> resultList = new ArrayList<>();
        List<Object[]> subData = data.rows();
        for (Object[] row : subData) {
            PaymentsResult result = new PaymentsResult(
                    (String) row[0],
                    (String) row[1],
                    (String) row[2],
                    (String) row[3],
                    (String) row[4],
                    (String) row[5],
                    (String) row[6],
                    (String) row[7]
            );
            resultList.add(result);
        }
        return resultList;
    }

    // ═════════════════════════════════════════════════════════════
    //  عمليات الصفوف (تعديل / حذف)
    // ═════════════════════════════════════════════════════════════

    public Integer updateEmployeeNote(PayrollRequest request) {
        return payrollYearlyApi.updateEmployeeNote(request);
    }

    public Integer deleteOneEmployeeRecord(PayrollRequest request) {
        return payrollYearlyApi.deleteOneEmployeeRecord(request);
    }

    // ═════════════════════════════════════════════════════════════
    //  PDF — بترجع مسار الملف المحمّل للعرض في الـ WebView
    // ═════════════════════════════════════════════════════════════

    /**
     * صرفيات الموظف للفترة المختارة (زرار PDF الرئيسي).
     */
    public CompletableFuture<Path> downloadPaymentsPDFToFile(String nationalId, String startMonth,
                                                             String endMonth, String empName) {
        if (nationalId == null || nationalId.length() != 14) {
            return failedValidation();
        }

        PayrollRequest request = PayrollRequest.builder().build();
        request.setNationalId(nationalId);
        request.setStartDate(DateUtils.getFirstDayOfMonth(startMonth));
        request.setEndDate(DateUtils.getLastDayOfMonth(endMonth));
        request.setFormat("PDF");

        return downloadToTemp("PAYMENTS_REPORT", "تقرير صرفيات موظف", empName,
                target -> payrollYearlyApi.downloadPaymentsPDF(request, target));
    }

    /**
     * تقرير مراجعة لصرفية معينة (زرار PDF في الصف).
     */
    public CompletableFuture<Path> downloadCustomReviewToFile(String nationalId, String month,
                                                              String payGroup, String empName) {
        if (nationalId == null || nationalId.length() != 14) {
            return failedValidation();
        }

        PayrollRequest request = PayrollRequest.builder().build();
        request.setNationalId(nationalId);
        request.setStartDate(DateUtils.getFirstDayOfMonth(convertArabicToEnglishNumbers(month)));
        request.setPayGroup(convertArabicToEnglishNumbers(payGroup));
        request.setFormat("PDF");

        return downloadToTemp("REVIEW_REPORT", "تقرير مراجعة", empName,
                target -> payrollReviewApi.downloadCustomReviewPDF(request, target));
    }

    /**
     * تقرير تعديلات الراتب (زرار ملاحظات في الصف).
     */
    public CompletableFuture<Path> downloadComparePDFToFile(String month, String nationalId, String empName) {
        if (nationalId == null || nationalId.length() != 14) {
            return failedValidation();
        }

        PayrollRequest request = PayrollRequest.builder().build();
        request.setNationalId(nationalId);
        request.setStartDate(DateUtils.getFirstDayOfMonth(convertArabicToEnglishNumbers(month)));
        request.setFormat("PDF");

        return downloadToTemp("COMPARE_REPORT", "تقرير تعديلات الراتب", empName,
                target -> payrollReviewApi.downloadComparePDF(request, target));
    }

    // ═════════════════════════════════════════════════════════════
    //  Helpers
    // ═════════════════════════════════════════════════════════════

    private interface PdfDownloader {
        boolean download(Path target) throws Exception;
    }

    private CompletableFuture<Path> downloadToTemp(String prefix, String reportName, String empName,
                                                   PdfDownloader downloader) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path dir = Paths.get(System.getProperty("user.dir"), "temp_downloads");
                Files.createDirectories(dir);
                Path target = dir.resolve(prefix + System.currentTimeMillis() + ".pdf");

                boolean ok = downloader.download(target);
                if (!ok) {
                    throw new RuntimeException("فشل تحميل الملف");
                }

                Platform.runLater(() -> notifyPdfReady(target, reportName, empName));
                return target;
            } catch (Exception e) {
                log.error("PDF download failed: {}", prefix, e);
                throw new CompletionException(e);
            }
        });
    }

    private static CompletableFuture<Path> failedValidation() {
        SAFNotification.warning(NATIONAL_ID_REQUIRED);
        return CompletableFuture.failedFuture(new IllegalArgumentException(NATIONAL_ID_REQUIRED));
    }

    private void notifyPdfReady(Path target, String reportName, String empName) {
        SAFNotification.withAction(target.toString(), target.toFile());
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
}