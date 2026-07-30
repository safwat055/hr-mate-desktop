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

@PayrollReport(code = "payrollYearly_9", displayName = "تقرير مجموعة تعيين محددة", category = "yearly_payroll", mainReport = "yearly_payroll")
public class SpecificPayGroupStrategy implements ReportStrategy {
    @Override
    public String getMainReport() {
        return "yearly_payroll";
    }

    @Override
    public String getCode() {
        return "payrollYearly_9";
    }

    @Override
    public String getDisplayName() {
        return "تقرير مجموعة تعيين محددة";
    }

    @Override
    public String getCategory() {
        return "yearly_payroll";
    }

    @Override
    public UiConfiguration getUiConfig() {
        return UiConfiguration.builder()
                .title("تقرير مجموعة تعيين محددة")
                .visibleFields(List.of(UiField.START_DATE, UiField.PAY_GROUP))
                .requiredFields(List.of(UiField.START_DATE, UiField.PAY_GROUP))
                .needsSearchDialog(true)
                .searchDialogTitle("اختر مجموعة تعيين")
                .searchDataSource("payGroup")
                .build();
    }

    @Override
    public void validate(ReportContext context) {
        if (context.getPayGroup() == null || context.getPayGroup().isBlank()) {
            throw new ValidationException("مجموعة التعيين مطلوبة");
        }
    }

    @Override
    public PayrollRequest buildRequest(ReportContext ctx) {
        return PayrollRequest.builder()
                .user(ApiClient.getUserName())
                .startDate(DateUtils.getFirstDayOfMonth(ctx.getStartDate()))
                .payGroup(ctx.getPayGroup())
                .report(getCode())
                .reportName(ctx.getReportName())
                .format(ctx.getFormat())
                .endPoint(ApiEndpoints.PayrollYearly.YEARLY_EXPENSES)
                .build();
    }
}