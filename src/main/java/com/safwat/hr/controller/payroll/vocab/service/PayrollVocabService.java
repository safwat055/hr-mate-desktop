package com.safwat.hr.controller.payroll.vocab.service;

import com.safwat.hr.controller.payroll.payrollApi.PayrollReviewApi;
import com.safwat.hr.controller.payroll.payrollApi.dto.SearchEmp;
import com.safwat.hr.notification.model.HRNotification;
import com.safwat.hr.notification.service.NotificationService;
import com.safwat.hr.shared.PayrollRequest;
import com.safwat.hr.shared.util.DateUtils;
import com.safwat.hr.ui.controls.SAFNotification;
import com.safwat.hr.ui.util.PDFView;
import javafx.scene.web.WebView;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class PayrollVocabService {

    private final PayrollReviewApi payrollReviewApi = PayrollReviewApi.getInstance();


    public List<SearchEmp> searchEmployee(String searchValue) {
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

    public void downloadMainReviewReport(String nationalId, String targetMonth, WebView webView, String name) {

        try {
            if (nationalId.length() != 14) {
                SAFNotification.warning("يجب إدخال الرقم القومى او البحث عن قيمة أولا");
                return;
            }

            PayrollRequest request = PayrollRequest.builder().build();

            request.setNationalId(nationalId);
            request.setStartDate(DateUtils.getFirstDayOfMonth(targetMonth));

            request.setFormat("PDF");
            String fileName = "REVIEW_REPORT" + System.currentTimeMillis() + ".pdf";

            String workingDir = System.getProperty("user.dir");

            Path tempDownloadsDir = Paths.get(workingDir, "temp_downloads");
            if (!Files.exists(tempDownloadsDir)) {
                Files.createDirectories(tempDownloadsDir);
            }

            Path targetPath = tempDownloadsDir.resolve(fileName);

            SAFNotification.success("جاري تحميل الملف...");

            boolean success = payrollReviewApi.downloadMainReviewReport(request, targetPath);

            if (success) {
                SAFNotification.withAction("هل تريد فتح التقرير الان ؟", targetPath.toFile());
                openPDF(targetPath, webView, name);

            } else {

                SAFNotification.error("فشل تحميل الملف");


            }

        } catch (Exception e) {
            SAFNotification.error("حدث خطأ أثناء التحميل: " + e.getMessage());

        }
    }

    public void downloadCustomReviewReport(String nationalId, String payGroup, String targetMonth, WebView webView, String name) {

        try {
            if (nationalId.length() != 14) {
                SAFNotification.warning("يجب إدخال الرقم القومى او البحث عن قيمة أولا");
                return;
            }

            PayrollRequest request = PayrollRequest.builder()
                    .nationalId(nationalId)
                    .startDate(DateUtils.getFirstDayOfMonth(targetMonth))
                    .payGroup(payGroup)
                    .build();

            String fileName = "REVIEW_REPORT" + System.currentTimeMillis() + ".pdf";

            String workingDir = System.getProperty("user.dir");

            Path tempDownloadsDir = Paths.get(workingDir, "temp_downloads");
            if (!Files.exists(tempDownloadsDir)) {
                Files.createDirectories(tempDownloadsDir);
            }

            Path targetPath = tempDownloadsDir.resolve(fileName);

            SAFNotification.success("جاري تحميل الملف...");

            boolean success = payrollReviewApi.downloadCustomReviewPDF(request, targetPath);

            if (success) {
                SAFNotification.withAction("هل تريد فتح التقرير الان ؟", targetPath.toFile());
                openPDF(targetPath, webView, name);

            } else {

                SAFNotification.error("فشل تحميل الملف");


            }

        } catch (Exception e) {
            SAFNotification.error("حدث خطأ أثناء التحميل: " + e.getMessage());

        }
    }


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
