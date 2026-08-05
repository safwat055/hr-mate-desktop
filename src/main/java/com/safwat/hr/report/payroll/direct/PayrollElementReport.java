package com.safwat.hr.report.payroll.direct;

import com.safwat.hr.network.ApiClient;
import com.safwat.hr.report.controller.PayrollReportController;
import com.safwat.hr.report.core.ReportContext;
import com.safwat.hr.report.core.strategies.ReportStrategy;
import com.safwat.hr.report.core.ui.UiConfiguration;
import com.safwat.hr.shared.PayrollRequest;

public class PayrollElementReport implements ReportStrategy {
    @Override
    public String getCode() {
        return "ELEMENT_REPORT";
    }

    @Override
    public String getDisplayName() {
        return "تقرير العناصر الاقتصادية واكوادها";
    }

    @Override
    public String getCategory() {
        return "main_direct";
    }

    @Override
    public String getMainReport() {
        return "ELEMENT_REPORT";
    }

    @Override
    public UiConfiguration getUiConfig() {
        return UiConfiguration.builder().build();
    }

    @Override
    public void onApply(PayrollReportController controller) {
        ReportStrategy.super.onApply(controller);
    }

    @Override
    public PayrollRequest buildRequest(ReportContext context) {
        return PayrollRequest.builder()
                .reportName(context.getReportName())
                .report(getCode())
                .user(ApiClient.getUserName())
                .format(context.getFormat())
                .build();
    }

    @Override
    public void validate(ReportContext context) {
        ReportStrategy.super.validate(context);
    }

}
