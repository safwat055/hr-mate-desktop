package com.safwat.hr.controller.payroll.payrollManager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.safwat.hr.controller.payroll.payrollApi.PayrollChangeCardApi;
import com.safwat.hr.controller.payroll.payrollApi.PayrollReviewApi;
import com.safwat.hr.controller.payroll.payrollApi.PayrollYearlyApi;
import com.safwat.hr.controller.payroll.payrollApi.dto.EmployeeSearchResult;
import com.safwat.hr.network.ApiClient;
import com.safwat.hr.network.ApiResponse;
import com.safwat.hr.network.SessionManager;
import com.safwat.hr.report.core.ReportContext;
import com.safwat.hr.report.core.strategies.ReportExternalSubmitter;
import com.safwat.hr.shared.PayrollRequest;
import com.safwat.hr.shared.ui.DangerConfirmDialog;
import com.safwat.hr.shared.util.DateUtils;
import com.safwat.hr.ui.controls.SAFNotification;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.*;

import static com.safwat.hr.shared.util.DateUtils.getFirstDayOfMonth;

@Slf4j
public class PayrollManagerService {

    private final PayrollManagerController managerController;
    private final PayrollYearlyApi payrollYearlyApi = PayrollYearlyApi.getInstance();
    private final PayrollChangeCardApi payrollChangeCardApi = PayrollChangeCardApi.getInstance();
    private final PayrollReviewApi payrollReviewApi = PayrollReviewApi.getInstance();

    private final List<String> customList = new ArrayList<>();

    public PayrollManagerService(PayrollManagerController payrollManagerController) {
        this.managerController = payrollManagerController;
        setAllMonthsList();
    }

    /** يعرض إشعار خطأ على الـ FX thread بغض النظر عن الـ thread المنادي. */
    private static void fxError(String message) {
        if (Platform.isFxApplicationThread()) {
            SAFNotification.error(message);
        } else {
            Platform.runLater(() -> SAFNotification.error(message));
        }
    }

    /** يعرض إشعار info على الـ FX thread بغض النظر عن الـ thread المنادي. */
    private static void fxInfo(String message) {
        if (Platform.isFxApplicationThread()) {
            SAFNotification.info(message);
        } else {
            Platform.runLater(() -> SAFNotification.info(message));
        }
    }

    public void setAllMonthsList() {
        // TODO: املأ القائمة لو محتاج، أو احذف الدالة
    }

    // ═══════════════════════════════════════════════════════════
    //  Read-only helpers (بتتنادى من SmartSearchHelper على ASYNC_EXECUTOR)
    // ═══════════════════════════════════════════════════════════

    public List<String> getAllMonthsYearly() {
        try {
            return payrollYearlyApi.getAllMonthsForYearly();
        } catch (Exception e) {
            fxError(e.getMessage());
            log.error("getAllMonthsYearly failed", e);
            return Collections.emptyList();
        }
    }

    public List<String> getAllMonthsReview() {
        return payrollReviewApi.getAllMonthForReview();
    }

    public List<String> getAllMonthsChangeCard() {
        return payrollChangeCardApi.getAllMonthForChangeCard();
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

    public List<EmployeeSearchResult> getEmployeeInYearly() {
        String searchValue = (managerController.getTxtEmpIdAnnual().getText());
        PayrollRequest request = PayrollRequest.builder()
                .searchValue(searchValue)
                .build();
        return payrollYearlyApi.searchInEmployee(request);
    }

    public List<String> getEmployeeMonths(String nationalId) {
        PayrollRequest request = PayrollRequest.builder()
                .nationalId(nationalId)
                .build();
        return payrollYearlyApi.getEmployeeMonths(request);
    }

    public List<String> getPayGroupForEmployeeInMonth(String nationalId, String strDate) {
        PayrollRequest request = PayrollRequest.builder()
                .nationalId(nationalId)
                .startDate(getFirstDayOfMonth(strDate))
                .build();
        return payrollYearlyApi.getPayGroupForEmployeeInMonth(request);
    }

    public List<String> getPayGroup() {
        return payrollYearlyApi.getPayGroup();
    }

    public List<PayrollManagerController.GroupDescription> getDescriptions(String strDate) {
        PayrollRequest request = PayrollRequest.builder()
                .startDate(getFirstDayOfMonth(strDate))
                .build();
        return payrollYearlyApi.getDescriptions(request);
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

    public List<EmployeeSearchResult> getEmployeeInReview(String searchValue) {
        PayrollRequest request = PayrollRequest.builder()
                .searchValue(searchValue)
                .build();
        return payrollReviewApi.searchInEmployee(request);
    }

    public List<EmployeeSearchResult> getEmployeeInSub(String searchValue) {
        PayrollRequest request = PayrollRequest.builder()
                .searchValue(searchValue)
                .build();
        return payrollChangeCardApi.searchInEmployees(request);
    }

    public List<String> getEmployeeMonthsSub(String nationalId) {
        PayrollRequest request = PayrollRequest.builder()
                .nationalId(nationalId)
                .build();
        return payrollChangeCardApi.getEmployeeMonthsChangeCard(request);
    }

    // ═══════════════════════════════════════════════════════════
    //  Delete operations
    //  ⚠️ كل الدوال دي blocking — لازم تتنادى من UiAsync.run(...)
    //     عشان متجمّدش الشاشة. الـ DangerConfirmDialog بيتنادى
    //     *قبلها* على الـ FX thread (في الـ Controller).
    // ═══════════════════════════════════════════════════════════

    /** تنفيذ الحذف الفعلي بعد موافقة المستخدم — blocking. */
    public void deleteOneMonthYearly() {
        LocalDate date = getFirstDayOfMonth(managerController.getTxtAllMonthsYearly().getText());
        Integer deletedRows = payrollYearlyApi.deleteFullMonthYearly(date);
        managerController.getTxtAllMonthsYearly().clear();
        fxInfo("تم حذف عدد " + deletedRows + " صف");
    }

    public void deleteTargetPayGroup() {
        LocalDate date = getFirstDayOfMonth(managerController.getTxtMonthGroupY().getText());
        String payGroup = managerController.getTxtGroupAnnual().getText();
        Integer deletedRows = payrollYearlyApi.deleteTargetGroupByMonth(date, payGroup);
        managerController.getTxtGroupAnnual().clear();
        managerController.getTxtMonthGroupY().clear();
        fxInfo("تم حذف عدد " + deletedRows + " صف");
    }

    public void deleteEmployeeMonth() {
        PayrollRequest request = PayrollRequest.builder()
                .nationalId(managerController.getTxtEmpIdAnnual().getText())
                .startDate(getFirstDayOfMonth(managerController.getTxtMonthForEmpAnnual().getText()))
                .build();
        Integer deletedRows = payrollYearlyApi.deleteMonthForEmployee(request);
        fxInfo("تم حذف عدد " + deletedRows + " صف");
    }

    public void deletePayGroupInTargetMonthAndEmployee(String nationalId, String strDate, String payGroup) {
        PayrollRequest request = PayrollRequest.builder()
                .nationalId(nationalId)
                .startDate(getFirstDayOfMonth(strDate))
                .payGroup(payGroup)
                .build();
        Integer deletedRows = payrollYearlyApi.deletePayGroupInTargetMonthAndEmployee(request);
        fxInfo("تم حذف عدد " + deletedRows + " صف");
    }

    public void updatePayGroupName(String oldName, String newName) {
        PayrollRequest request = PayrollRequest.builder()
                .payGroup(oldName)
                .description(newName)
                .build();
        Integer updatedRows = payrollYearlyApi.updatePayGroupName(request);
        managerController.getTxtOldPaymentName().clear();
        managerController.getTxtNewPaymentName().clear();
        fxInfo("تم تحديث عدد " + updatedRows + " صف");
    }

    public boolean saveDescriptions(String month,
                                    List<PayrollManagerController.GroupDescription> descriptions) {
        List<Map<String, String>> payload = descriptions.stream()
                .map(d -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("payGroup", d.getPayGroup());
                    map.put("description", d.getDescription());
                    return map;
                })
                .toList();

        PayrollRequest request = PayrollRequest.builder()
                .startDate(DateUtils.getFirstDayOfMonth(month))
                .payload(payload)
                .build();

        try {
            ApiResponse<Integer> response = ApiClient.post(
                    "/payrollYearly/update-descriptions-list",
                    request,
                    new TypeReference<>() {
                    }
            );
            if (response != null && response.isSuccess() && response.getData() != null) {
                fxInfo("تم تحديث الوصف لعدد " + response.getData() + " صف");
                return response.getData() > 0;
            }
            fxError("فشل حفظ الأوصاف: " + (response != null ? response.getMessage() : "لا يوجد رد"));
            return false;
        } catch (Exception e) {
            log.error("saveDescriptions failed", e);
            fxError("فشل حفظ الأوصاف: " + e.getMessage());
            return false;
        }
    }

    public void deleteFullMonthReview(String strDate) {
        PayrollRequest request = PayrollRequest.builder()
                .startDate(getFirstDayOfMonth(strDate))
                .build();
        Integer deletedRows = payrollReviewApi.deleteFullMonthReview(request);
        fxInfo("تم حذف عدد " + deletedRows + " صف");
    }

    public void deletePayGroupReview(String strDate, String payGroup) {
        PayrollRequest request = PayrollRequest.builder()
                .startDate(getFirstDayOfMonth(strDate))
                .payGroup(payGroup)
                .build();
        Integer deletedRows = payrollReviewApi.deletePayGroupReview(request);
        fxInfo("تم حذف عدد " + deletedRows + " صف");
    }

    public void deleteEmployeeMonthReview(String nationalId, String strDate) {
        PayrollRequest request = PayrollRequest.builder()
                .nationalId(nationalId)
                .startDate(getFirstDayOfMonth(strDate))
                .build();
        Integer deletedRows = payrollReviewApi.deleteEmployeeMonthReview(request);
        fxInfo("تم حذف عدد " + deletedRows + " صف");
    }

    public void deleteEmployeePayGroupReview(String nationalId, String strDate, String payGroup) {
        PayrollRequest request = PayrollRequest.builder()
                .nationalId(nationalId)
                .startDate(getFirstDayOfMonth(strDate))
                .payGroup(payGroup)
                .build();
        Integer deletedRows = payrollReviewApi.deleteEmployeePayGroup(request);
        fxInfo("تم حذف عدد " + deletedRows + " صف");
    }

    public void deleteEmployeeMonthSub(String nationalId, String strDate) {
        PayrollRequest request = PayrollRequest.builder()
                .nationalId(nationalId)
                .startDate(getFirstDayOfMonth(strDate))
                .build();
        Integer deletedRows = payrollChangeCardApi.deleteEmployeeMonthChangeCard(request);
        fxInfo("تم حذف عدد " + deletedRows + " صف");
    }

    public void deleteFullMonthSub(String strDate) {
        PayrollRequest request = PayrollRequest.builder()
                .startDate(getFirstDayOfMonth(strDate))
                .build();
        Integer deletedRows = payrollChangeCardApi.deleteFullMonthChangeCard(request);
        fxInfo("تم حذف عدد " + deletedRows + " صف");
    }

    // ═══════════════════════════════════════════════════════════
    //  Key Update — ReportExternalSubmitter async بالفعل
    // ═══════════════════════════════════════════════════════════

    public void updateKeysReviewAllReport() {
        ReportContext ctx = ReportContext.builder()
                .user(SessionManager.getInstance().getUsername())
                .build();

        ReportExternalSubmitter.getInstance().submit("UPDATE_REVIEW_KEYS_ALL", ctx,
                reportId -> SAFNotification.success("تم إرسال الطلب رقم: " + reportId),
                error -> SAFNotification.error("فشل الإرسال: " + error.getMessage())
        );
    }

    public void updateKeysReviewMonth(String strDate) {
        ReportContext ctx = ReportContext.builder()
                .user(SessionManager.getInstance().getUsername())
                .startDate(strDate)
                .build();
        ReportExternalSubmitter.getInstance().submit("UPDATE_REVIEW_KEYS_MONTH", ctx,
                reportId -> SAFNotification.success("تم إرسال الطلب رقم: " + reportId),
                error -> SAFNotification.error("فشل الإرسال: " + error.getMessage())
        );
    }
}