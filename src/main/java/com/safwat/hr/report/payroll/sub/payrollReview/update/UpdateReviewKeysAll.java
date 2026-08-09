package com.safwat.hr.report.payroll.sub.payrollReview.update;

import com.safwat.hr.network.ApiClient;
import com.safwat.hr.report.core.ReportContext;
import com.safwat.hr.report.core.strategies.ReportStrategy;
import com.safwat.hr.report.core.ui.UiConfiguration;
import com.safwat.hr.shared.PayrollRequest;

public class UpdateReviewKeysAll implements ReportStrategy {
    @Override
    public String getCode() {
        return "UPDATE_REVIEW_KEYS_ALL";
    }

    @Override
    public String getDisplayName() {
        return "كل الشهور";
    }

    @Override
    public String getCategory() {
        return "UPDATE_REVIEW_ALL";
    }

    @Override
    public String getMainReport() {
        return "UPDATE_REVIEW_ALL";
    }

    @Override
    public UiConfiguration getUiConfig() {
        return UiConfiguration.builder().build();
    }

    @Override
    public PayrollRequest buildRequest(ReportContext context) {
        return PayrollRequest.builder()
                .user(ApiClient.getUserName())
                .report(getCode())
                .reportName(context.getReportName())
                .build();
    }
}
