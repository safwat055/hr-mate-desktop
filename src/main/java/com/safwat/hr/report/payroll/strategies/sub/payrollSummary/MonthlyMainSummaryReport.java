package com.safwat.hr.report.payroll.strategies.sub.payrollSummary;

import com.safwat.hr.network.ApiClient;
import com.safwat.hr.network.ApiEndpoints;
import com.safwat.hr.report.controller.PayrollReportController;
import com.safwat.hr.report.payroll.PayrollReport;
import com.safwat.hr.report.payroll.ReportContext;
import com.safwat.hr.report.payroll.strategies.ReportStrategy;
import com.safwat.hr.report.payroll.ui.UiConfiguration;
import com.safwat.hr.report.payroll.ui.UiField;
import com.safwat.hr.shared.PayrollRequest;
import com.safwat.hr.shared.util.DateUtils;

import java.util.List;

@PayrollReport(code = "summaryReport_3",
        displayName = "إجمالي تكاليف الصرفيات الرئيسية لشهر محدد",
        category = "payroll_summary",
        mainReport = "payroll_summary")
public class MonthlyMainSummaryReport implements ReportStrategy {
    @Override
    public String getCode() {
        return "summaryReport_3";
    }

    @Override
    public String getDisplayName() {
        return "إجمالي تكاليف الصرفيات الرئيسية لشهر محدد";
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
                //.title("إجمالي تكاليف الصرفيات الرئيسية لشهر محدد")
                .requiredFields(List.of(UiField.H_START_DATE))
                .visibleFields(List.of(UiField.H_START_DATE))
                .build();

    }

    @Override
    public void onApply(PayrollReportController c) {

        c.setChoseMonth();
    }

    @Override
    public PayrollRequest buildRequest(ReportContext context) {
        return PayrollRequest.builder()
                .user(ApiClient.getUserName())
                .reportName(context.getReportName())
                .report(getCode())
                .format(context.getFormat())
                .endPoint(ApiEndpoints.PayrollYearly.PAYROLL_SUMMARY)
                .startDate(DateUtils.getFirstDayOfMonth(context.getStartDate())).build();
    }

    @Override
    public void validate(ReportContext context) {
        ReportStrategy.super.validate(context);
    }
}
