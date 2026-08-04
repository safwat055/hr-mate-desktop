package com.safwat.hr.report.payroll.sub.payrollHistory.reviewReport;

import com.safwat.hr.network.ApiClient;
import com.safwat.hr.report.controller.PayrollReportController;
import com.safwat.hr.report.core.ReportContext;
import com.safwat.hr.report.core.ValidationException;
import com.safwat.hr.report.core.strategies.ReportStrategy;
import com.safwat.hr.report.core.ui.UiConfiguration;
import com.safwat.hr.report.core.ui.UiField;
import com.safwat.hr.shared.PayrollRequest;
import com.safwat.hr.shared.util.DateUtils;

public class ReviewReportAll implements ReportStrategy {
    @Override
    public String getCode() {
        return "REVIEW_REPORT_ALL";
    }

    @Override
    public String getDisplayName() {
        return "تفرير مراجعة لكل الموظفين";
    }

    @Override
    public String getCategory() {
        return "REVIEW_REPORT";
    }

    @Override
    public String getMainReport() {
        return "REVIEW_REPORT";
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
                .user(ApiClient.getUserName())
                .reportName(context.getReportName())
                .report(getCode())
                .startDate(DateUtils.getFirstDayOfMonth(context.getStartDate()))
                .build();
    }

    @Override
    public void validate(ReportContext context) {
        if (context.getStartDate() == null || context.getStartDate().isBlank()) {
            throw new ValidationException("الشهر مطلوب");

        }
    }
}
