package com.safwat.hr.report.payroll.strategies.sub.payrollSummary;

import com.safwat.hr.report.payroll.PayrollReport;
import com.safwat.hr.report.payroll.ReportContext;
import com.safwat.hr.report.payroll.ValidationException;
import com.safwat.hr.report.payroll.strategies.ReportStrategy;
import com.safwat.hr.report.payroll.ui.UiConfiguration;
import com.safwat.hr.report.payroll.ui.UiField;
import com.safwat.hr.service.payroll.dto.PayrollRequest;
import com.safwat.hr.shared.util.DateUtils;
import com.safwat.hr.utils.ApiClient;
import com.safwat.hr.utils.ApiEndpoints;

import java.util.List;


@PayrollReport(code = "summaryReport_1",
        displayName = "اجمالى التكاليف لشهر محدد",
        category = "payroll_summary",
        mainReport = "payroll_summary")
public class MonthlySummaryReport implements ReportStrategy {
    @Override
    public String getCode() {
        return "summaryReport_1";
    }

    @Override
    public String getDisplayName() {
        return "اجمالى التكاليف لشهر محدد";
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
                .title("تقرير شهر محدد")
                .visibleFields(List.of(UiField.START_DATE))
                .requiredFields(List.of(UiField.START_DATE))
                .build();
    }

    @Override
    public PayrollRequest buildRequest(ReportContext context) {
        return PayrollRequest.builder()
                .user(ApiClient.getUserName())
                .startDate(DateUtils.getFirstDayOfMonth(context.getStartDate()))
                .report(getCode())
                .reportName(context.getReportName())
                .format(context.getFormat())
                .endPoint(ApiEndpoints.PayrollYearly.PAYROLL_SUMMARY)
                .build();
    }

    @Override
    public void validate(ReportContext context) {
        if (context.getStartDate() == null || context.getStartDate().isBlank()) {
            throw new ValidationException("يجب اختيار فترة اولا!");
        }
    }


}
