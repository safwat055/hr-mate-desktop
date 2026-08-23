package com.safwat.hr.report.payroll.sub.payrollReview.update;

import com.safwat.hr.network.ApiClient;
import com.safwat.hr.report.controller.PayrollReportController;
import com.safwat.hr.report.core.PayrollReport;
import com.safwat.hr.report.core.ReportContext;
import com.safwat.hr.report.core.ValidationException;
import com.safwat.hr.report.core.strategies.ReportStrategy;
import com.safwat.hr.report.core.ui.UiConfiguration;
import com.safwat.hr.report.core.ui.UiField;
import com.safwat.hr.shared.PayrollRequest;
import com.safwat.hr.shared.util.DateUtils;

@PayrollReport(
        code = "UPDATE_REVIEW_KEYS_MONTH",
        displayName = "شهر محدد",
        category = "UPDATE_REVIEW_ALL",
        mainReport = "UPDATE_REVIEW_ALL"
)
public class UpdateReviewKeysMonth implements ReportStrategy {
    @Override
    public String getCode() {
        return "UPDATE_REVIEW_KEYS_MONTH";
    }

    @Override
    public String getDisplayName() {
        return "شهر محدد";
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
                .startDate(DateUtils.getFirstDayOfMonth(context.getStartDate()))
                .report(getCode())
                .reportName(context.getReportName())
                .build();
    }

    @Override
    public void validate(ReportContext context) {

        if (context.getStartDate() == null || context.getStartDate().isBlank()) {
            throw new ValidationException("يجب اختيار شهر");
        }

    }
}
