package com.safwat.hr.controller.payroll.changeCard.service;

import com.safwat.hr.notification.model.HRNotification;
import com.safwat.hr.notification.service.NotificationService;
import com.safwat.hr.controller.payroll.payrollApi.PayrollChangeCardApi;
import com.safwat.hr.controller.payroll.payrollApi.dto.ChangeCardView;
import com.safwat.hr.controller.payroll.payrollApi.dto.SearchEmp;
import com.safwat.hr.shared.PayrollRequest;
import com.safwat.hr.shared.util.DateUtils;
import com.safwat.hr.ui.controls.SAFNotification;
import com.safwat.hr.ui.util.PDFView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.web.WebView;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class PayrollChangeService {
    private final PayrollChangeCardApi payrollChangeCardApi = PayrollChangeCardApi.getInstance();

    public List<SearchEmp> searchInEmployee(String searchValue) {
        PayrollRequest request = PayrollRequest.builder()
                .searchValue(searchValue)
                .build();

        return payrollChangeCardApi.searchInEmployees(request);
    }

    public List<String> getEmployeeAvailableMonths(String nationalId) {
        PayrollRequest request = PayrollRequest.builder()
                .nationalId(nationalId)
                .build();
        return payrollChangeCardApi.getEmployeeMonthsChangeCard(request);
    }


    public ObservableList<ChangeCardResult> getChangeCardData(String nationalId, String startMonth, String endMonth) {
        PayrollRequest request = PayrollRequest.builder()
                .nationalId(nationalId)
                .startDate(DateUtils.getFirstDayOfMonth(startMonth))
                .endDate(DateUtils.getLastDayOfMonth(endMonth))
                .build();
        ChangeCardView data = payrollChangeCardApi.getChangeCardDataView(request);
        ObservableList<ChangeCardResult> resultList = FXCollections.observableArrayList();
        List<Object[]> subData = data.rows();
        for (Object[] row : subData) {
            ChangeCardResult result = new ChangeCardResult(
                    (String) row[0],
                    (String) row[1],
                    row[2] != null ? row[2].toString() : ""
            );
            resultList.add(result);

        }

        return resultList;
    }

    /**
     *
     * @param nationalId
     * @param startMonth
     * @param endMonth
     * @param empName
     * @param webView
     */
    public void downloadChangeCardPDF(String nationalId, String startMonth, String endMonth, String empName, WebView webView) {
        // 4. تحميل الملف
        try {
            PayrollRequest request = PayrollRequest.builder()
                    .nationalId(nationalId)
                    .startDate(DateUtils.getFirstDayOfMonth(startMonth))
                    .endDate(DateUtils.getLastDayOfMonth(endMonth))
                    .build();
            String fileName = "CHANGE_CARD_" + System.currentTimeMillis() + ".pdf";

            String workingDir = System.getProperty("user.dir");

            Path tempDownloadsDir = Paths.get(workingDir, "temp_downloads");
            if (!Files.exists(tempDownloadsDir)) {
                Files.createDirectories(tempDownloadsDir);
            }

            Path targetPath = tempDownloadsDir.resolve(fileName);
            boolean success = payrollChangeCardApi.downloadChangeCardPDF(request, targetPath);
            if (success) {
                openPDF(targetPath, webView, empName);
            } else {
                SAFNotification.error("حدث خطا اثناء التحميل");
            }

        } catch (IOException e) {
            SAFNotification.error(e.getMessage());

        }

    }

    public void updateNote(String nationalId, String month) {
        PayrollRequest request = PayrollRequest.builder()
                .nationalId(nationalId)
                .startDate(DateUtils.getFirstDayOfMonth(month))
                .build();
        int count = payrollChangeCardApi.updateEmployeeMonthNote(request);
        SAFNotification.info("تم تحديث عدد : " + count + " بنجاح");
    }

    public void deleteOneRecord(String nationalId, String month) {
        PayrollRequest request = PayrollRequest.builder()
                .nationalId(nationalId)
                .startDate(DateUtils.getFirstDayOfMonth(month))
                .build();
        Integer count = payrollChangeCardApi.deleteEmployeeMonthChangeCard(request);
        SAFNotification.success("تم حذف " + count + " قيد");
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
