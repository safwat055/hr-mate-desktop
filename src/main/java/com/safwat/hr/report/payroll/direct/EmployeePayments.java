package com.safwat.hr.report.payroll.direct;

import com.safwat.hr.network.ApiEndpoints;
import com.safwat.hr.report.controller.PayrollReportController;
import com.safwat.hr.report.core.ReportContext;
import com.safwat.hr.report.core.ValidationException;
import com.safwat.hr.report.core.strategies.ReportStrategy;
import com.safwat.hr.report.core.ui.UiConfiguration;
import com.safwat.hr.report.core.ui.UiField;
import com.safwat.hr.shared.PayrollRequest;
import com.safwat.hr.shared.util.DateUtils;


public class EmployeePayments implements ReportStrategy {

    @Override
    public String getCode() {
        return "PAYMENTS_REPORT";
    }

    @Override
    public String getDisplayName() {
        return "تقرير صرفيات الموظف";
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
                .visibleField(UiField.H_START_DATE)
                .visibleField(UiField.H_END_DATE)
                .visibleField(UiField.H_EMPLOYEE)
                .requiredField(UiField.H_START_DATE)
                .requiredField(UiField.H_END_DATE)
                .requiredField(UiField.H_EMPLOYEE)
                .build();
    }

    @Override
    public void onApply(PayrollReportController c) {
        c.setSearchEmployeeActions();

        c.setStartAndEndActions();
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