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

import java.util.List;

public class ReviewReportEmployee implements ReportStrategy {
    @Override
    public String getCode() {
        return "REVIEW_REPORT_EMPLOYEE";
    }

    @Override
    public String getDisplayName() {
        return "تفرير مراجعة لموظف";
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
                .requiredFields(List.of(UiField.H_START_DATE, UiField.H_EMPLOYEE))
                .visibleFields(List.of(UiField.H_START_DATE, UiField.H_EMPLOYEE))

                .build();
    }

    @Override
    public void onApply(PayrollReportController controller) {
        controller.setChoseMonth();
        controller.setSearchEmployeeActions();
    }

    @Override
    public PayrollRequest buildRequest(ReportContext context) {
        return PayrollRequest.builder()
                .reportName(context.getReportName())
                .user(ApiClient.getUserName())
                .report(getCode())
                .startDate(DateUtils.getFirstDayOfMonth(context.getStartDate()))
                .nationalId(context.getNationalId())
                .build();
    }

    @Override
    public void validate(ReportContext context) {
        if (context.getStartDate() == null || context.getStartDate().isEmpty()) {
            throw new ValidationException("الشهر مطلوب");

        }
        if (context.getNationalId() == null || context.getNationalId().isBlank()) {
            throw new ValidationException("الرقم القومي مطلوب");
        }
    }
}
