package com.safwat.hr.shared;

import lombok.Getter;

@Getter
public class FXMLPaths {
    private final String paymentsView = "/com/safwat/hr/controller/payroll/Payments.fxml";
    private final String changeCardView = "/com/safwat/hr/controller/payroll/ChangeCard.fxml";
    private final String payrollVocab = "/com/safwat/hr/controller/payroll/PayrollVocab.fxml";
    private final String payrollManager = "/com/safwat/hr/controller/payroll/PayrollManagerView.fxml";
    private final String payrollRecords = "/com/safwat/hr/controller/payroll/PayrollRecords.fxml";
    private final String payrollTableView = "/com/safwat/hr/controller/payroll/ExcelTableView.fxml";

    private final String reportManager = "/com/safwat/hr/controller/report/user_reports.fxml";
    private final String payrollReport = "/com/safwat/hr/controller/report/PayrollReport.fxml";


    private final String chat = "/com/safwat/hr/chat/ChatView.fxml";
    private final String MessageInboxView = "/com/safwat/hr/controller/message/MessageInboxView.fxml";


    private final String backendSetting = "/com/safwat/hr/view/Settings.fxml";

    // scale views
    private final String salaryScale = "/com/safwat/hr/controller/scale/scale/ScaleView.fxml";
    private final String allowanceView = "/com/safwat/hr/controller/entitlements/allowance/AllowanceView.fxml";

    private final String backupView = "/com/safwat/hr/controller/BackupView.fxml";

    private final String employeeForm = "/com/safwat/hr/controller/EmployeeForm.fxml";
    private final String historyDialog = "/com/safwat/hr/controller/HistoryDialog.fxml";
    private final String wagesView = "/com/safwat/hr/controller/EmployeeWagesScreen.fxml";
}
