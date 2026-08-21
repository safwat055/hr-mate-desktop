package com.safwat.hr.payroll.payrollManager;

import com.safwat.hr.shared.ui.DangerConfirmDialog;
import com.safwat.hr.ui.controls.SAFNotification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
        boolean ok = DangerConfirmDialog.show("تأكيد الحذف", "سيتم حذف بيانات الشهر كاملة من تقرير الصرفيات السنوى", "حذف شهر " + managerController.getLblAllMonthsYearly().getText());
        if (ok) {
            LocalDate date = getFirstDayOfMonth(managerController.getLblAllMonthsYearly().getText());
            Integer deletedRows = apiService.deleteFullMonthYearly(date);
            managerController.getLblAllMonthsYearly().setText("");
            managerController.getTxtAllMonthsYearly().clear();
            SAFNotification.info("تم حذف عدد " + deletedRows + " صف ");
        } else {
            SAFNotification.info("تم إلغاء عملية الحذف");
        }
    }

    public void deleteTargetPayGroup() {
        boolean ok = DangerConfirmDialog.show("تأكيد الحذف", "سيتم حذف بيانات المجموعة كاملة من تقرير الصرفيات السنوى" + managerController.getTxtGroupAnnual().getText(), "حذف شهر " + managerController.getLblMonthGroupY().getText());
        if (ok) {
            LocalDate date = getFirstDayOfMonth(managerController.getLblMonthGroupY().getText());
            String payGroup = managerController.getTxtGroupAnnual().getText();
            Integer deletedRows = apiService.deleteTargetGroupByMonth(date, payGroup);
            SAFNotification.info("تم حذف عدد " + deletedRows + " صف ");
        } else {
            SAFNotification.info("تم إلغاء عملية الحذف");
        }
    }

    public List<String> getAvailablePayGroupForMonth() {
        customList.clear();
        String monthText = managerController.getLblMonthGroupY().getText();
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

}
