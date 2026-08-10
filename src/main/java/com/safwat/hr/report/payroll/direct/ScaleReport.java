package com.safwat.hr.report.payroll.direct;

import com.safwat.hr.network.ApiClient;
import com.safwat.hr.report.controller.PayrollReportController;
import com.safwat.hr.report.core.ReportContext;
import com.safwat.hr.report.core.strategies.ReportStrategy;
import com.safwat.hr.report.core.ui.UiConfiguration;
import com.safwat.hr.report.core.ui.UiField;
import com.safwat.hr.shared.PayrollRequest;

public class ScaleReport implements ReportStrategy {
    @Override
    public String getCode() {
        return "123";
    }

    @Override
    public String getDisplayName() {
        return "a";
    }

    @Override
    public String getCategory() {
        return "main_direct";
    }

    @Override
    public String getMainReport() {
        return "main_direct";
    }

    @Override
    public UiConfiguration getUiConfig() {
        return UiConfiguration.builder()
                .requiredField(UiField.H_EMPLOYEE)
                .visibleField(UiField.H_EMPLOYEE)
                .build();
    }

    @Override
    public void onApply(PayrollReportController controller) {
        controller.setSearchEmployeeActions();
    }

    @Override
    public PayrollRequest buildRequest(ReportContext context) {
        return PayrollRequest.builder()
                .user(ApiClient.getUserName())
                .report(getCode())
                .reportName(context.getReportName())
                .nationalId(context.getNationalId())
                .build();
    }
}
