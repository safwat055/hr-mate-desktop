package com.safwat.hr.payroll.records;

import com.safwat.hr.payroll.payrollApi.PayrollReviewApi;
import com.safwat.hr.payroll.payrollApi.dto.SearchEmp;
import com.safwat.hr.payroll.payrollApi.dto.ViewMainRecordForRangeDate;
import com.safwat.hr.shared.PayrollRequest;
import com.safwat.hr.shared.util.DateUtils;
import com.safwat.hr.ui.controls.SAFNotification;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static com.safwat.hr.shared.util.StringUtil.convertArabicToEnglishNumbers;

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

    public void downloadMainReviewReport(String nationalId, String targetMonth) {

        try {
            if (nationalId.isEmpty() || nationalId.length() != 14) {
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

            } else {

                SAFNotification.error("فشل تحميل الملف");
            }

        } catch (Exception e) {
            SAFNotification.error("حدث خطأ أثناء التحميل: " + e.getMessage());

        }
    }

    public void downloadCustomReviewToPDF(String month, String payGroup, String nationalId) {

        try {

            PayrollRequest request = PayrollRequest.builder().build();

            request.setNationalId(nationalId);
            request.setStartDate(DateUtils.getFirstDayOfMonth(convertArabicToEnglishNumbers(month)));
            request.setPayGroup(convertArabicToEnglishNumbers(payGroup));

            request.setFormat("PDF");
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

            } else {

                SAFNotification.error("فشل تحميل الملف");
            }

        } catch (Exception e) {
            SAFNotification.error("حدث خطأ أثناء التحميل: " + e.getMessage());

        }
    }

    public void compareExportToPDF(String month, String nationalId) {

        try {

            PayrollRequest request = PayrollRequest.builder().build();

            request.setNationalId(nationalId);
            request.setStartDate(DateUtils.getFirstDayOfMonth(convertArabicToEnglishNumbers(month)));

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
                SAFNotification.withAction("هل تريد فتح التقرير الان ؟", targetPath.toFile());

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
}
