package com.safwat.hr.payroll.payrollManager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.safwat.hr.network.ApiClient;
import com.safwat.hr.network.ApiResponse;
import com.safwat.hr.payroll.dto.SearchEmp;
import com.safwat.hr.shared.PayrollRequest;
import com.safwat.hr.shared.ui.DangerConfirmDialog;
import com.safwat.hr.shared.util.DateUtils;
import com.safwat.hr.ui.controls.SAFNotification;

import java.time.LocalDate;
import java.util.*;

import static com.safwat.hr.shared.util.DateUtils.getFirstDayOfMonth;

public class PayrollManagerService {
    private final PayrollManagerApiService apiService;
    private final PayrollManagerController managerController;
    public List<String> allMonthsYearly = new ArrayList<>();
    public List<String> allMonthsReview = new ArrayList<>();
    public List<String> allMonthsChangeCard = new ArrayList<>();

    public List<String> customList = new ArrayList<>();

    public PayrollManagerService(PayrollManagerController payrollManagerController) {
        this.managerController = payrollManagerController;
        apiService = PayrollManagerApiService.getInstance();

        setAllMonthsList();
    }


    public void setAllMonthsList() {
        allMonthsYearly = (apiService.getAllMonthsForYearly());
        allMonthsReview = apiService.getAllMonthForReview();
        allMonthsChangeCard = apiService.getAllMonthForChangeCard();
    }

    // ===============================================================================
    // القسم الخاص بتقرير الصرفيات السنوى
    // ===============================================================================
    public void deleteOneMonthYearly() {
        boolean ok = DangerConfirmDialog.show("تأكيد الحذف", "سيتم حذف بيانات الشهر كاملة من تقرير الصرفيات السنوى", "حذف شهر " + managerController.getTxtAllMonthsYearly().getText());
        if (ok) {
            LocalDate date = getFirstDayOfMonth(managerController.getTxtAllMonthsYearly().getText());
            Integer deletedRows = apiService.deleteFullMonthYearly(date);

            managerController.getTxtAllMonthsYearly().clear();
            SAFNotification.info("تم حذف عدد " + deletedRows + " صف ");
        } else {
            SAFNotification.info("تم إلغاء عملية الحذف");
        }
    }

    public void deleteTargetPayGroup() {
        boolean ok = DangerConfirmDialog.show("تأكيد الحذف", "سيتم حذف بيانات المجموعة كاملة من تقرير الصرفيات السنوى" + managerController.getTxtGroupAnnual().getText(), "حذف شهر " + managerController.getTxtMonthGroupY().getText());
        if (ok) {
            LocalDate date = getFirstDayOfMonth(managerController.getTxtMonthGroupY().getText());
            String payGroup = managerController.getTxtGroupAnnual().getText();
            Integer deletedRows = apiService.deleteTargetGroupByMonth(date, payGroup);
            managerController.getTxtGroupAnnual().clear();
            managerController.getTxtMonthGroupY().clear();

            SAFNotification.info("تم حذف عدد " + deletedRows + " صف ");
        } else {
            SAFNotification.info("تم إلغاء عملية الحذف");
        }
    }

    public List<String> getAvailablePayGroupForMonth() {
        customList.clear();
        String monthText = managerController.getTxtMonthGroupY().getText();
        if (monthText == null || monthText.isBlank()) {
            return Collections.emptyList();
        }
        LocalDate date = getFirstDayOfMonth(monthText);
        if (date == null) {
            return Collections.emptyList();
        }
        customList.addAll(apiService.getAvailablePayGroupForMonth(date));
        return customList;
    }

    public List<SearchEmp> getEmployeeInYearly() {
        LocalDate date = getFirstDayOfMonth(managerController.getTxtMonthForEmpAnnual().getText());
        PayrollRequest request = PayrollRequest.builder()
                .startDate(date)
                .build();

        return apiService.getEmployeeInYearly(request);
    }

    public List<String> getEmployeeMonths(String nationalId) {
        PayrollRequest request = PayrollRequest.builder()
                .nationalId(nationalId)
                .build();
        return apiService.getEmployeeMonths(request);
    }

    public void deleteEmployeeMonth() {

        boolean ok = DangerConfirmDialog.show("تاكيد الحذف", "سيتم حذف شهر " + managerController.getTxtMonthForEmpAnnual().getText() + " للموظف " + managerController.getTxtEmpNameAnnual().getText(), "");
        if (ok) {
            PayrollRequest request = PayrollRequest.builder()
                    .nationalId(managerController.getTxtEmpIdAnnual().getText())
                    .startDate(getFirstDayOfMonth(managerController.getTxtMonthForEmpAnnual().getText()))
                    .build();
            Integer deletedRows = apiService.deleteMonthForEmployee(request);

            SAFNotification.info("تم حذف عدد " + deletedRows + " صف ");
        } else {
            SAFNotification.info("تم إلغاء عملية الحذف");
        }
    }

    public List<String> getPayGroupForEmployeeInMonth(String nationalId, String strDate) {
        PayrollRequest request = PayrollRequest.builder()
                .nationalId(nationalId)
                .startDate(getFirstDayOfMonth(strDate))
                .build();
        return apiService.getPayGroupForEmployeeInMonth(request);
    }

    public void deletePayGroupInTargetMonthAndEmployee(String nationalId, String strDate, String payGroup) {
        boolean ok = DangerConfirmDialog.show("تاكيد الحذف", "سيتم حذف شهر " + managerController.getTxtMonthForPaymentAnnual().getText() + " للموظف " + managerController.getTxtEmpNameAnnual2().getText(), "");
        if (ok) {
            PayrollRequest request = PayrollRequest.builder()
                    .nationalId(nationalId)
                    .startDate(getFirstDayOfMonth(strDate))
                    .payGroup(payGroup)
                    .build();
            Integer deletedRows = apiService.deletePayGroupInTargetMonthAndEmployee(request);

            SAFNotification.info("تم حذف عدد " + deletedRows + " صف ");
        } else {
            SAFNotification.info("تم إلغاء عملية الحذف");
        }
    }

    public List<String> getPayGroup() {
        return apiService.getPayGroup();
    }

    public void updatePayGroupName(String oldName, String newName) {

        PayrollRequest request = PayrollRequest.builder()
                .payGroup(oldName)
                .description(newName)
                .build();
        try {
            Integer updatedRows = apiService.updatePayGroupName(request);
            managerController.getTxtOldPaymentName().clear();
            managerController.getTxtNewPaymentName().clear();
            SAFNotification.info("تم تحديث عدد " + updatedRows + " صف");
        } catch (Exception e) {
            SAFNotification.error(e.getMessage());
        }

    }

    public List<PayrollManagerController.GroupDescription> getDescriptions(String strDate) {
        PayrollRequest request = PayrollRequest.builder()
                .startDate(getFirstDayOfMonth(strDate))
                .build();
        return apiService.getDescriptions(request);

    }

    boolean saveDescriptions(String month,
                             List<PayrollManagerController.GroupDescription> descriptions) {

        // 1. تحويل الـ DTO لـ List<Map> (الـ payload العام)
        List<Map<String, String>> payload = descriptions.stream()
                .map(d -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("payGroup", d.getPayGroup());
                    map.put("description", d.getDescription());
                    return map;
                })
                .toList();

        // 2. بناء الـ Request مع الـ payload
        PayrollRequest request = PayrollRequest.builder()
                .startDate(DateUtils.getFirstDayOfMonth(month))
                .payload(payload)   // ← الحقل العام
                .build();

        // 3. إرسال للـ Backend
        try {
            ApiResponse<Integer> response = ApiClient.post(
                    "/payrollYearly/update-descriptions-list",
                    request,
                    new TypeReference<>() {
                    }
            );
            SAFNotification.info("تم تحديث الوصف لعدد " + response.getData() + " صف");
            return response != null
                    && response.getData() != null
                    && response.getData() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
