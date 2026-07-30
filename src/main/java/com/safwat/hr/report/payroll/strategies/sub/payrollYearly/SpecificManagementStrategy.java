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

@PayrollReport(code = "payrollYearly_6", displayName = "تقرير إدارة محددة", category = "yearly_payroll", mainReport = "yearly_payroll")
public class SpecificManagementStrategy implements ReportStrategy {
    @Override
    public String getMainReport() {
        return "yearly_payroll";
    }

    @Override
    public String getCode() {
        return "payrollYearly_6";
    }

    @Override
    public String getDisplayName() {
        return "تقرير إدارة محددة";
    }

    @Override
    public String getCategory() {
        return "yearly_payroll";
    }

    @Override
    public UiConfiguration getUiConfig() {
        return UiConfiguration.builder()
                .title("تقرير إدارة محددة")
                .visibleFields(List.of(UiField.START_DATE, UiField.MANAGEMENT))
                .requiredFields(List.of(UiField.START_DATE, UiField.MANAGEMENT))
                .needsSearchDialog(true)
                .searchDialogTitle("اختر إدارة")
                .searchDataSource("management")
                .build();
    }

    @Override
    public void validate(ReportContext context) {
        if (context.getManagement() == null || context.getManagement().isBlank()) {
            throw new ValidationException("الإدارة مطلوبة");
        }
    }

    @Override
    public PayrollRequest buildRequest(ReportContext ctx) {
        return PayrollRequest.builder()
                .user(ApiClient.getUserName())
                .startDate(DateUtils.getFirstDayOfMonth(ctx.getStartDate()))
                .management(ctx.getManagement())
                .report(getCode())
                .reportName(ctx.getReportName())
                .format(ctx.getFormat())
                .endPoint(ApiEndpoints.PayrollYearly.YEARLY_EXPENSES)
                .build();
    }
}