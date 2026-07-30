package com.safwat.hr.report.payroll.strategies.sub.payrollYearly;

import com.safwat.hr.report.payroll.PayrollReport;
import com.safwat.hr.report.payroll.ReportContext;
import com.safwat.hr.report.payroll.strategies.ReportStrategy;
import com.safwat.hr.report.payroll.ui.UiConfiguration;
import com.safwat.hr.report.payroll.ui.UiField;
import com.safwat.hr.service.payroll.dto.PayrollRequest;
import com.safwat.hr.shared.util.DateUtils;
import com.safwat.hr.utils.ApiClient;
import com.safwat.hr.utils.ApiEndpoints;

import java.util.List;

@PayrollReport(code = "payrollYearly_3", displayName = "مجموعات التعيين المنفصلة", category = "yearly_payroll", mainReport = "yearly_payroll")
public class SeparatePayGroupsStrategy implements ReportStrategy {
    @Override
    public String getMainReport() {
        return "yearly_payroll";
    }

    @Override
    public String getCode() {
        return "payrollYearly_3";
    }

    @Override
    public String getDisplayName() {
        return "مجموعات التعيين المنفصلة";
    }

    @Override
    public String getCategory() {
        return "yearly_payroll";
    }

    @Override
    public UiConfiguration getUiConfig() {
        return UiConfiguration.builder()
                .title("تقرير مجموعات التعيين المنفصلة")
                .visibleFields(List.of(UiField.START_DATE))
                .requiredFields(List.of(UiField.START_DATE))
                .build();
    }

    @Override
    public PayrollRequest buildRequest(ReportContext ctx) {
        return PayrollRequest.builder()
                .user(ApiClient.getUserName())
                .startDate(DateUtils.getFirstDayOfMonth(ctx.getStartDate()))
                .report(getCode())
                .reportName(ctx.getReportName())
                .format(ctx.getFormat())
                .endPoint(ApiEndpoints.PayrollYearly.YEARLY_EXPENSES)
                .build();
    }
}