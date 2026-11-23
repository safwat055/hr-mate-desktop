package com.safwat.hr.shared;

import lombok.Getter;

@Getter
public class FXMLPaths {
    private final String paymentsView = "/com/safwat/hr/controller/payroll/payments/Payments.fxml";
    private final String changeCardView = "/com/safwat/hr/controller/payroll/changeCard/ChangeCard.fxml";
    private final String payrollVocab = "/com/safwat/hr/controller/payroll/vocab/PayrollVocab.fxml";
    private final String payrollManager = "/com/safwat/hr/controller/payroll/payrollManager/PayrollManagerView.fxml";
    private final String payrollRecords = "/com/safwat/hr/controller/payroll/records/PayrollRecords.fxml";
    private final String payrollTableView = "/com/safwat/hr/controller/payroll/table/ExcelTableView.fxml";
    private final String payrollDateView = "/com/safwat/hr/controller/payroll/payrollData/PayrollData.fxml";

    private final String reportManager = "/com/safwat/hr/controller/report/user_reports.fxml";
    private final String payrollReport = "/com/safwat/hr/controller/report/PayrollReport.fxml";


    private final String chat = "/com/safwat/hr/controller/_public/chat/ChatView.fxml";
    private final String MessageInboxView = "/com/safwat/hr/controller/_public/message/MessageInboxView.fxml";


    private final String backendSetting = "/com/safwat/hr/view/Settings.fxml";

    // scale views
    private final String salaryScale = "/com/safwat/hr/controller/hr/scale/ScaleView.fxml";
    private final String allowanceView = "/com/safwat/hr/controller/hr/entitlements/allowance/AllowanceView.fxml";

    private final String backupView = "/com/safwat/hr/controller/_public/backup/BackupView.fxml";

    private final String employeeForm = "/com/safwat/hr/controller/hr/employee/EmployeeForm.fxml";
    private final String historyDialog = "/com/safwat/hr/controller/HistoryDialog.fxml";
    private final String wagesView = "/com/safwat/hr/controller/hr/wages/EmployeeWagesScreen.fxml";
    private final String form5View = "/com/safwat/hr/controller/hr/form5/Form5.fxml";
    private final String sectorView = "/com/safwat/hr/controller/hr/entitlements/allowance/SectorJobTitleDialog.fxml";
    private final String OverrideDialogView = "/com/safwat/hr/controller/hr/entitlements/allowance/AllowanceOverrideDialog.fxml";
    private final String statutoryDialogView = "/com/safwat/hr/controller/hr/entitlements/allowance/StatutoryDialog.fxml";
    private final String allowanceDefinitionDialog = "/com/safwat/hr/controller/hr/entitlements/allowance/AllowanceDefinitionDialog.fxml";
    private final String tutorialsView = "/com/safwat/hr/controller/tutorials/TutorialsScreen.fxml";


    public String getLeavesView() {
        return "/com/safwat/hr/controller/hr/leave/LeaveManagementView.fxml";
    }public String getLeavesAdminView() {
        return "/com/safwat/hr/controller/hr/leave/admin/LeaveAdminView.fxml";
    }
}
