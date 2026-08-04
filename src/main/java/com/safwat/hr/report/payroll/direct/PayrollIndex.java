package com.safwat.hr.report.payroll.direct;

import com.safwat.hr.network.ApiClient;
import com.safwat.hr.report.core.ReportContext;
import com.safwat.hr.report.core.strategies.ReportStrategy;
import com.safwat.hr.report.core.ui.UiConfiguration;
import com.safwat.hr.shared.PayrollRequest;

public class PayrollIndex implements ReportStrategy {
    @Override
    public String getCode() {
        return "PAYROLL_INDEX";
    }

    @Override
    public String getDisplayName() {
        return "تقرير الفهرس الكامل";
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
        return UiConfiguration.builder().build();
    }

    @Override
    public PayrollRequest buildRequest(ReportContext context) {
        return PayrollRequest.builder()
                .user(ApiClient.getUserName())
                .reportName(context.getReportName())
                .report(getCode())
                .format(context.getFormat())
                .build();
    }
}
