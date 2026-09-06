package com.safwat.hr.controller.payroll.records;

import com.safwat.hr.controller.payroll.payrollApi.PayrollReviewApi;
import com.safwat.hr.controller.payroll.payrollApi.dto.SearchEmp;
import com.safwat.hr.controller.payroll.payrollApi.dto.ViewMainRecordForRangeDate;
import com.safwat.hr.controller.payroll.payrollApi.dto.ViewNonPrimaryRangeDate;
import com.safwat.hr.notification.model.HRNotification;
import com.safwat.hr.notification.service.NotificationService;
import com.safwat.hr.shared.PayrollRequest;
import com.safwat.hr.shared.util.DateUtils;
import com.safwat.hr.ui.controls.SAFNotification;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class PayrollRecordsService {
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
            throw new IllegalArgumentException();
        }
        PayrollRequest request = PayrollRequest.builder()
                .nationalId(nationalId)
                .startDate(DateUtils.getFirstDayOfMonth(startMonth))
                .endDate(DateUtils.getFirstDayOfMonth(endMonth))
                .build();
        return payrollReviewApi.getNonPrimaryMonthRecords(request);
    }

    public void downloadMainReviewReport(String nationalId, String targetMonth, String empName) {

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
                openPDF(targetPath, "تقرير مراجعة", empName);

            } else {

                SAFNotification.error("فشل تحميل الملف");
            }

        } catch (Exception e) {
            SAFNotification.error("حدث خطأ أثناء التحميل: " + e.getMessage());

        }
    }

    public void downloadCustomReviewToPDF(String month, String payGroup, String nationalId, String empName) {

        try {

            PayrollRequest request = PayrollRequest.builder()
                    .nationalId(nationalId)
                    .startDate(DateUtils.getFirstDayOfMonth(month))
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
                openPDF(targetPath, "تقرير مراجعة", empName);

            } else {

                SAFNotification.error("فشل تحميل الملف");
            }

        } catch (Exception e) {
            SAFNotification.error("حدث خطأ أثناء التحميل: " + e.getMessage());

        }
    }

    public void compareExportToPDF(String month, String nationalId, String empName) {

        try {

            PayrollRequest request = PayrollRequest.builder()
                    .nationalId(nationalId)
                    .startDate(DateUtils.getFirstDayOfMonth(month))
                    .build();

            request.setFormat("PDF");
            String fileName = "COMPARE_REPORT" + System.currentTimeMillis() + ".pdf";

            String workingDir = System.getProperty("user.dir");

            Path tempDownloadsDir = Paths.get(workingDir, "temp_downloads");
            if (!Files.exists(tempDownloadsDir)) {
                Files.createDirectories(tempDownloadsDir);
            }

            Path targetPath = tempDownloadsDir.resolve(fileName);

            SAFNotification.success("جاري تحميل الملف...");

            boolean success = payrollReviewApi.downloadComparePDF(request, targetPath);

            if (success) {
                openPDF(targetPath, "تقرير تعديلات الراتب", empName);

            } else {

                SAFNotification.error("فشل تحميل الملف");
            }

        } catch (Exception e) {
            SAFNotification.error("حدث خطأ أثناء التحميل: " + e.getMessage());

        }
    }

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
                .startDate(DateUtils.getFirstDayOfMonth((startDate)))
                .endDate(DateUtils.getFirstDayOfMonth((endDate)))
                .build();
        Long reportId = payrollReviewApi.downloadRecord_129(request);
        SAFNotification.success("تم تقديم الطلب بنجاح رقم الطلب : " + reportId);
    }


    private void openPDF(Path targetPath, String reportName, String empName) {

        SAFNotification.withAction(targetPath.toString(), targetPath.toFile());

        NotificationService.getInstance().send(
                HRNotification.builder()
                        .type(HRNotification.NotificationType.SYSTEM)
                        .priority(HRNotification.Priority.HIGH)
                        .title(reportName)
                        .message(empName)
                        .file(targetPath.toString())
                        .sender("system")
                        .build()
        );
    }
}
