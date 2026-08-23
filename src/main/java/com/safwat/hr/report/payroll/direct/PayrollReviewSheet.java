package com.safwat.hr.report.payroll.direct;

import com.safwat.hr.network.ApiClient;
import com.safwat.hr.report.controller.PayrollReportController;
import com.safwat.hr.report.core.PayrollReport;
import com.safwat.hr.report.core.ReportContext;
import com.safwat.hr.report.core.strategies.ReportStrategy;
import com.safwat.hr.report.core.ui.UiConfiguration;
import com.safwat.hr.report.core.ui.UiField;
import com.safwat.hr.shared.PayrollRequest;
import com.safwat.hr.shared.util.DateUtils;

@PayrollReport(
        code = "PAYROLL_REVIEW_SHEET",
        displayName = "شيت الرواتب الشهري",
        category = "main_direct",
        mainReport = "main_direct"
)
public class PayrollReviewSheet implements ReportStrategy {
    @Override
    public String getCode() {
        return "PAYROLL_REVIEW_SHEET";
    }

    @Override
    public String getDisplayName() {
        return "شيت الرواتب الشهري";
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

                .requiredField(UiField.H_START_DATE)
                .visibleField(UiField.H_START_DATE)
                .build();
    }

    @Override
    public void onApply(PayrollReportController controller) {
        controller.setChoseMonth();
    }

    @Override
    public PayrollRequest buildRequest(ReportContext context) {
        return PayrollRequest.builder()
                .reportName(context.getReportName())
                .report(getCode())
                .user(ApiClient.getUserName())
                .startDate(DateUtils.getFirstDayOfMonth(context.getStartDate()))
                .build();
    }
}
