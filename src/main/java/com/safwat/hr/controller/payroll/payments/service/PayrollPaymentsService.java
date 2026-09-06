package com.safwat.hr.controller.payroll.payments.service;

import com.safwat.hr.controller.payroll.payrollApi.PayrollReviewApi;
import com.safwat.hr.controller.payroll.payrollApi.PayrollYearlyApi;
import com.safwat.hr.controller.payroll.payrollApi.dto.PaymentsView;
import com.safwat.hr.controller.payroll.payrollApi.dto.SearchEmp;
import com.safwat.hr.notification.model.HRNotification;
import com.safwat.hr.notification.service.NotificationService;
import com.safwat.hr.shared.PayrollRequest;
import com.safwat.hr.shared.util.DateUtils;
import com.safwat.hr.ui.controls.SAFNotification;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static com.safwat.hr.shared.util.StringUtil.convertArabicToEnglishNumbers;

@Slf4j
public class PayrollPaymentsService {
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

    public List<SearchEmp> searchEmployee(String searchValue) {
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
            SAFNotification.warning("يجب ادخال الرقم القومى او البحث عن قيمة اولا");
            throw new IllegalArgumentException();
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

    @SneakyThrows
    public void downloadPaymentsPDF(String nationalId, String startMonth, String endMonth, String empName) {
        if (nationalId.length() != 14) {
            SAFNotification.warning("يجب ادخال الرقم القومى او البحث عن قيمة اولا");
            throw new IllegalArgumentException();
        }

        PayrollRequest request = PayrollRequest.builder().build();

        request.setNationalId(nationalId);
        request.setStartDate(DateUtils.getFirstDayOfMonth(startMonth));
        request.setEndDate(DateUtils.getLastDayOfMonth(endMonth));
        request.setFormat("PDF");

        String fileName = "PAYMENTS_REPORT" + System.currentTimeMillis() + ".pdf";

        String workingDir = System.getProperty("user.dir");

        Path tempDownloadsDir = Paths.get(workingDir, "temp_downloads");
        if (!Files.exists(tempDownloadsDir)) {
            Files.createDirectories(tempDownloadsDir);
        }

        Path targetPath = tempDownloadsDir.resolve(fileName);
        boolean success = payrollYearlyApi.downloadPaymentsPDF(request, targetPath);
        if (success) {

            openPDF(targetPath, "تقرير صرفيات موظف", empName);

        } else {

            SAFNotification.error("فشل تحميل الملف");
        }
    }


    public Integer updateEmployeeNote(PayrollRequest request) {
        return payrollYearlyApi.updateEmployeeNote(request);
    }

    public Integer deleteOneEmployeeRecord(PayrollRequest request) {
        return payrollYearlyApi.deleteOneEmployeeRecord(request);
    }

    public void downLoadCustomReview(String nationalId, String month, String payGroup, String empName) {

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

    public void downloadComparePDF(String month, String nationalId, String empName) {

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


    // helper methods
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
