package com.safwat.hr.report.public_;

import com.safwat.hr.network.ApiClient;
import com.safwat.hr.report.core.ReportContext;
import com.safwat.hr.report.core.strategies.ReportStrategy;
import com.safwat.hr.report.core.ui.UiConfiguration;
import com.safwat.hr.shared.PayrollRequest;

public class StartTransferData implements ReportStrategy {
    @Override
    public String getCode() {
        return "TRANSFER";
    }


    @Override
    public String getDisplayName() {
        return "نقل البيانات من HR SALARY SCALE";
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
                .build();


    }
}
