package com.safwat.hr.payroll.payrollManager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.safwat.hr.network.ApiClient;
import com.safwat.hr.network.ApiResponse;
import com.safwat.hr.payroll.payrollApi.PayrollChangeCardApi;
import com.safwat.hr.payroll.payrollApi.PayrollReviewApi;
import com.safwat.hr.payroll.payrollApi.PayrollYearlyApi;
import com.safwat.hr.payroll.payrollApi.dto.SearchEmp;
import com.safwat.hr.report.core.ReportContext;
import com.safwat.hr.report.core.strategies.ReportExternalSubmitter;
import com.safwat.hr.shared.PayrollRequest;
import com.safwat.hr.shared.ui.DangerConfirmDialog;
import com.safwat.hr.shared.util.DateUtils;
import com.safwat.hr.ui.controls.SAFNotification;
import lombok.SneakyThrows;

import java.time.LocalDate;
import java.util.*;

import static com.safwat.hr.shared.util.DateUtils.getFirstDayOfMonth;

public class PayrollManagerService {

    private final PayrollManagerController managerController;
    private final PayrollYearlyApi payrollYearlyApi = PayrollYearlyApi.getInstance();
    private final PayrollChangeCardApi payrollChangeCardApi = PayrollChangeCardApi.getInstance();
    private final PayrollReviewApi payrollReviewApi = PayrollReviewApi.getInstance();

    public List<String> customList = new ArrayList<>();

    public PayrollManagerService(PayrollManagerController payrollManagerController) {
        this.managerController = payrollManagerController;


        setAllMonthsList();
    }


    public void setAllMonthsList() {


    }

    @SneakyThrows
    public List<String> getAllMonthsYearly() {
        return payrollYearlyApi.getAllMonthsForYearly();
    }

    public List<String> getAllMonthsReview() {
        return payrollReviewApi.getAllMonthForReview();
    }

    public List<String> getAllMonthsChangeCard() {
        return payrollChangeCardApi.getAllMonthForChangeCard();
    }

    // ===============================================================================
    // القسم الخاص بتقرير الصرفيات السنوى
    // ===============================================================================
    public void deleteOneMonthYearly() {
        boolean ok = DangerConfirmDialog.show("تأكيد الحذف", "سيتم حذف بيانات الشهر كاملة من تقرير الصرفيات السنوى", "حذف شهر " + managerController.getTxtAllMonthsYearly().getText());
        if (ok) {
            LocalDate date = getFirstDayOfMonth(managerController.getTxtAllMonthsYearly().getText());
            Integer deletedRows = payrollYearlyApi.deleteFullMonthYearly(date);

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
            Integer deletedRows = payrollYearlyApi.deleteTargetGroupByMonth(date, payGroup);
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
        customList.addAll(payrollYearlyApi.getAvailablePayGroupForMonth(date));
        return customList;
    }

    public List<SearchEmp> getEmployeeInYearly() {
        LocalDate date = getFirstDayOfMonth(managerController.getTxtMonthForEmpAnnual().getText());
        PayrollRequest request = PayrollRequest.builder()
                .startDate(date)
                .build();

        return payrollYearlyApi.getEmployeeInYearly(request);
    }

    public List<String> getEmployeeMonths(String nationalId) {
        PayrollRequest request = PayrollRequest.builder()
                .nationalId(nationalId)
                .build();
        return payrollYearlyApi.getEmployeeMonths(request);
    }

    public void deleteEmployeeMonth() {

        boolean ok = DangerConfirmDialog.show("تاكيد الحذف", "سيتم حذف شهر " + managerController.getTxtMonthForEmpAnnual().getText() + " للموظف " + managerController.getTxtEmpNameAnnual().getText(), "");
        if (ok) {
            PayrollRequest request = PayrollRequest.builder()
                    .nationalId(managerController.getTxtEmpIdAnnual().getText())
                    .startDate(getFirstDayOfMonth(managerController.getTxtMonthForEmpAnnual().getText()))
                    .build();
            Integer deletedRows = payrollYearlyApi.deleteMonthForEmployee(request);

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
        return payrollYearlyApi.getPayGroupForEmployeeInMonth(request);
    }

    public void deletePayGroupInTargetMonthAndEmployee(String nationalId, String strDate, String payGroup) {
        boolean ok = DangerConfirmDialog.show("تاكيد الحذف", "سيتم حذف شهر " + managerController.getTxtMonthForPaymentAnnual().getText() + " للموظف " + managerController.getTxtEmpNameAnnual2().getText(), "");
        if (ok) {
            PayrollRequest request = PayrollRequest.builder()
                    .nationalId(nationalId)
                    .startDate(getFirstDayOfMonth(strDate))
                    .payGroup(payGroup)
                    .build();
            Integer deletedRows = payrollYearlyApi.deletePayGroupInTargetMonthAndEmployee(request);

            SAFNotification.info("تم حذف عدد " + deletedRows + " صف ");
        } else {
            SAFNotification.info("تم إلغاء عملية الحذف");
        }
    }

    public List<String> getPayGroup() {
        return payrollYearlyApi.getPayGroup();
    }

    public void updatePayGroupName(String oldName, String newName) {

        PayrollRequest request = PayrollRequest.builder()
                .payGroup(oldName)
                .description(newName)
                .build();
        try {
            Integer updatedRows = payrollYearlyApi.updatePayGroupName(request);
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
        return payrollYearlyApi.getDescriptions(request);

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
    // ===========================================================
    // =============== تقارير المراجعة ===========================
    // ===========================================================

    public List<String> getAllReviewKeys() {
        return payrollReviewApi.getAllKeys();

    }

    public List<String> getAllKeysForMonthReview(String strDate) {
        PayrollRequest request = PayrollRequest.builder()
                .startDate(getFirstDayOfMonth(strDate))
                .build();
        return payrollReviewApi.getAllKeysForMonth(request);
    }

    public List<String> getEmployeeMonthKeys(String nationalId, String strDate) {
        PayrollRequest request = PayrollRequest.builder()
                .nationalId(nationalId)
                .startDate(getFirstDayOfMonth(strDate))
                .build();
        return payrollReviewApi.getEmployeeMonthKeys(request);
    }

    public List<String> getEmployeeMonthsReview(String nationalId) {
        PayrollRequest request = PayrollRequest.builder()
                .nationalId(nationalId)
                .build();
        return payrollReviewApi.getEmployeeMonthsReview(request);
    }

    public List<SearchEmp> getEmployeeInReview(String searchValue) {

        PayrollRequest request = PayrollRequest.builder()
                .searchValue(searchValue)
                .build();

        return payrollReviewApi.searchInEmployee(request);
    }

    public void deleteFullMonthReview(String strDate) {
        boolean ok = DangerConfirmDialog.show("تأكيد حذف", "سيتم حذف الشهر بالكامل في حالة الاستمرار", "شهر " + DateUtils.toArabicMonthYear(getFirstDayOfMonth(strDate)));
        if (ok) {
            PayrollRequest request = PayrollRequest.builder()
                    .startDate(getFirstDayOfMonth(strDate))
                    .build();
            Integer deletedRows = payrollReviewApi.deleteFullMonthReview(request);
            SAFNotification.info("تم حذف عدد" + deletedRows + " صف");

        } else {
            SAFNotification.info("تم إلغاء العملية");
        }
    }

    public void deletePayGroupReview(String strDate, String payGroup) {
        boolean ok = DangerConfirmDialog.show("تأكيد حذف", "سيتم حذف المجموعة بالكامل في حالة الاستمرار  " + payGroup, "شهر " + DateUtils.toArabicMonthYear(getFirstDayOfMonth(strDate)));
        if (ok) {
            PayrollRequest request = PayrollRequest.builder()
                    .startDate(getFirstDayOfMonth(strDate))
                    .payGroup(payGroup)
                    .build();
            Integer deletedRows = payrollReviewApi.deletePayGroupReview(request);
            SAFNotification.info("تم حذف عدد" + deletedRows + " صف");

        } else {
            SAFNotification.info("تم إلغاء العملية");
        }
    }

    public void deleteEployeeMonthReviewُ(String nationalId, String strDate) {
        boolean ok = DangerConfirmDialog.show("تأكيد حذف", "سيتم حذف سجل الموظف للشهر بالكامل في حالة الاستمرار  " + nationalId, "شهر " + DateUtils.toArabicMonthYear(getFirstDayOfMonth(strDate)));
        if (ok) {
            PayrollRequest request = PayrollRequest.builder()
                    .nationalId(nationalId)
                    .startDate(getFirstDayOfMonth(strDate))

                    .build();
            Integer deletedRows = payrollReviewApi.deleteEmployeeMonthReview(request);
            SAFNotification.info("تم حذف عدد" + deletedRows + " صف");

        } else {
            SAFNotification.info("تم إلغاء العملية");
        }
    }

    public void deleteEployeeMonthReviewُ(String nationalId, String strDate, String payGroup) {
        boolean ok = DangerConfirmDialog.show("تأكيد حذف", "سيتم حذف مجوعة من الموظف في حالة الاستمرار  " + nationalId + "\n " + payGroup, "شهر " + DateUtils.toArabicMonthYear(getFirstDayOfMonth(strDate)));
        if (ok) {
            PayrollRequest request = PayrollRequest.builder()
                    .nationalId(nationalId)
                    .startDate(getFirstDayOfMonth(strDate))
                    .payGroup(payGroup)
                    .build();
            Integer deletedRows = payrollReviewApi.deleteEmployeePayGroup(request);
            SAFNotification.info("تم حذف عدد" + deletedRows + " صف");

        } else {
            SAFNotification.info("تم إلغاء العملية");
        }
    }

    public void updateKeysReviewAllReport() {

        ReportContext ctx = ReportContext.builder()

                .user(ApiClient.getUserName())
                .build();

        ReportExternalSubmitter.getInstance().submit("UPDATE_REVIEW_KEYS_ALL", ctx,
                reportId -> {
                    // على UI Thread already (شغال جوه Platform.runLater)
                    SAFNotification.success("تم إرسال الطلب رقم: " + reportId);
                },
                error -> {
                    SAFNotification.error("فشل الإرسال: " + error.getMessage());
                }
        );

    }

    public void updateKeysReviewMonth(String strDate) {

        ReportContext ctx = ReportContext.builder()
                .user(ApiClient.getUserName())
                .startDate(strDate)
                .build();
        ReportExternalSubmitter.getInstance().submit("UPDATE_REVIEW_KEYS_MONTH", ctx,
                reportId -> {
                    // على UI Thread already (شغال جوه Platform.runLater)
                    SAFNotification.success("تم إرسال الطلب رقم: " + reportId);
                },
                error -> {
                    SAFNotification.error("فشل الإرسال: " + error.getMessage());
                }
        );
    }

    public List<SearchEmp> getEmployeeInSub(String searchValue) {
        PayrollRequest request = PayrollRequest.builder()
                .searchValue(searchValue)
                .build();
        return payrollChangeCardApi.getEmployeeInChangeCard(request);
    }

    public List<String> getEmployeeMonthsSub(String nationalId) {
        PayrollRequest request = PayrollRequest.builder()
                .nationalId(nationalId)
                .build();
        return payrollChangeCardApi.getEmployeeMonthsChangeCard(request);
    }

    public void deleteEmployeeMonthSub(String nationalId, String strDate) {
        boolean ok = DangerConfirmDialog.show("تأكيد حذف", "سيتم حذف سجل الموظف للشهر بالكامل في حالة الاستمرار  " + nationalId, "شهر " + DateUtils.toArabicMonthYear(getFirstDayOfMonth(strDate)));
        if (ok) {
            PayrollRequest request = PayrollRequest.builder()
                    .nationalId(nationalId)
                    .startDate(getFirstDayOfMonth(strDate))
                    .build();
            Integer deletedRows = payrollChangeCardApi.deleteEmployeeMonthChangeCard(request);
            SAFNotification.info("تم حذف عدد" + deletedRows + " صف");

        } else {
            SAFNotification.info("تم إلغاء العملية");
        }
    }

    public void deleteFullMonthSub(String strDate) {
        boolean ok = DangerConfirmDialog.show("تأكيد حذف", "سيتم حذف الشهر بالكامل في حالة الاستمرار", "شهر " + DateUtils.toArabicMonthYear(getFirstDayOfMonth(strDate)));
        if (ok) {
            PayrollRequest request = PayrollRequest.builder()
                    .startDate(getFirstDayOfMonth(strDate))
                    .build();
            Integer deletedRows = payrollChangeCardApi.deleteFullMonthChangeCard(request);
            SAFNotification.info("تم حذف عدد" + deletedRows + " صف");

        } else {
            SAFNotification.info("تم إلغاء العملية");
        }
    }
}
