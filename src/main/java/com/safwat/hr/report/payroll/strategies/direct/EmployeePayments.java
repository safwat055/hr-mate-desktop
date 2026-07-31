package com.safwat.hr.report.payroll.strategies.direct;

import com.safwat.hr.report.payroll.PayrollReport;
import com.safwat.hr.report.payroll.ReportContext;
import com.safwat.hr.report.payroll.ValidationException;
import com.safwat.hr.report.payroll.strategies.ReportStrategy;
import com.safwat.hr.report.payroll.ui.UiConfiguration;
import com.safwat.hr.report.payroll.ui.UiField;
import com.safwat.hr.service.payroll.dto.PayrollRequest;
import com.safwat.hr.shared.util.DateUtils;
import com.safwat.hr.utils.ApiEndpoints;

@PayrollReport(
        code = "employeePayments",
        displayName = "تقرير صرفيات موظف",
        category = "main_direct",
        mainReport = "main_direct"
)
public class EmployeePayments implements ReportStrategy {

    @Override
    public String getCode() {
        return "employeePayments";
    }

    @Override
    public String getDisplayName() {
        return "تقرير صرفيات موظف";
    }

    @Override
    public String getCategory() {
        return "main_direct";
    }

    @Override
    public String getMainReport() {
        return "main_direct";
    }


    @Override
    public UiConfiguration getUiConfig() {
        return UiConfiguration.builder()
                //    .title("تقرير صرفيات موظف")
                .visibleField(UiField.START_DATE)
                .visibleField(UiField.END_DATE)
                .visibleField(UiField.NATIONAL_ID)
                .requiredField(UiField.START_DATE)
                .requiredField(UiField.END_DATE)
                .requiredField(UiField.NATIONAL_ID)
                .build();
    }

    /**
     * @throws ValidationException
     */
    @Override
    public void validate(ReportContext context) {

        if (context.getNationalId() == null || context.getNationalId().isBlank()) {
            throw new ValidationException("الرقم القومي مطلوب");
        }

    }

    @Override
    public PayrollRequest buildRequest(ReportContext ctx) {
        return PayrollRequest.builder()
                .user(ctx.getUser())
                .reportName(ctx.getReportName())
                .report(getCode())
                .format(ctx.getFormat())
                .nationalId(ctx.getNationalId())
                .startDate(DateUtils.getFirstDayOfMonth(ctx.getStartDate()))
                .endDate(DateUtils.getFirstDayOfMonth(ctx.getEndDate()))
                .endPoint(ApiEndpoints.PayrollYearly.PAYROLL_PAYMENTS)
                .build();
    }
}