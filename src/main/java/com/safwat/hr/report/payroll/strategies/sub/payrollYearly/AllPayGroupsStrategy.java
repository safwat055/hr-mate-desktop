package com.safwat.hr.report.payroll.strategies.sub.payrollYearly;

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

@PayrollReport(code = "payrollYearly_1",
        displayName = "كل مجموعات التعيين",
        category = "yearly_payroll",
        mainReport = "yearly_payroll")
public class AllPayGroupsStrategy implements ReportStrategy {
    @Override
    public String getMainReport() {
        return "yearly_payroll";
    }

    @Override
    public String getCode() {
        return "payrollYearly_1";
    }

    @Override
    public String getDisplayName() {
        return "كل مجموعات التعيين";
    }

    @Override
    public String getCategory() {
        return "yearly_payroll";
    }

    @Override
    public void validate(ReportContext context) {
        if (context.getStartDate() == null || context.getStartDate().isBlank()) {
            throw new ValidationException("يجب اختيار فترة اولا!");
        }
    }

    @Override
    public UiConfiguration getUiConfig() {
        return UiConfiguration.builder()
                .title("تقرير كل مجموعات التعيين")
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