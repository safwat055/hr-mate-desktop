package com.safwat.hr.report.payroll.sub.payrollSummary;

import com.safwat.hr.network.ApiClient;
import com.safwat.hr.network.ApiEndpoints;
import com.safwat.hr.report.controller.PayrollReportController;
import com.safwat.hr.report.core.PayrollReport;
import com.safwat.hr.report.core.ReportContext;
import com.safwat.hr.report.core.ValidationException;
import com.safwat.hr.report.core.strategies.ReportStrategy;
import com.safwat.hr.report.core.ui.UiConfiguration;
import com.safwat.hr.report.core.ui.UiField;
import com.safwat.hr.shared.PayrollRequest;
import com.safwat.hr.shared.util.DateUtils;

import java.util.List;


@PayrollReport(code = "summaryReport_2",
        displayName = "إجمالي التكاليف لمدة محددة",
        category = "payroll_summary",
        mainReport = "payroll_summary")
public class MonthlySummaryReportInRange implements ReportStrategy {
    @Override
    public String getCode() {
        return "summaryReport_2";
    }

    @Override
    public String getDisplayName() {
        return "إجمالي التكاليف لمدة محددة";
    }

    @Override
    public String getCategory() {
        return "payroll_summary";
    }

    @Override
    public String getMainReport() {
        return "payroll_summary";
    }

    @Override
    public UiConfiguration getUiConfig() {
        return UiConfiguration.builder()
                //  .title("تقرير شهر محدد")
                .visibleFields(List.of(UiField.H_START_DATE, UiField.H_END_DATE))
                .requiredFields(List.of(UiField.H_START_DATE, UiField.H_END_DATE))
                .build();
    }

    @Override
    public void onApply(PayrollReportController c) {

        c.setStartAndEndActions();
    }

    @Override
    public PayrollRequest buildRequest(ReportContext context) {
        return PayrollRequest.builder()
                .user(ApiClient.getUserName())

                .startDate(DateUtils.getFirstDayOfMonth(context.getStartDate()))
                .endDate(DateUtils.getFirstDayOfMonth(context.getEndDate()))
                .report(getCode())
                .reportName(context.getReportName())
                .format(context.getFormat())
                .endPoint(ApiEndpoints.PayrollYearly.PAYROLL_SUMMARY)
                .build();
    }

    @Override
    public void validate(ReportContext context) {
        if (context.getStartDate() == null || context.getStartDate().isBlank()) {
            throw new ValidationException("يجب اختيار فترة بداية!");
        }
        if (context.getEndDate() == null || context.getEndDate().isBlank()) {
            throw new ValidationException("يجب اختيار فترة نهاية!");
        }
    }


}
