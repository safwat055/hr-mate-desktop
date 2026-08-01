package com.safwat.hr.report.payroll.strategies.sub.changeCard.card;

import com.safwat.hr.controller.report.payroll.PayrollReportController;
import com.safwat.hr.report.payroll.ReportContext;
import com.safwat.hr.report.payroll.ValidationException;
import com.safwat.hr.report.payroll.strategies.ReportStrategy;
import com.safwat.hr.report.payroll.ui.UiConfiguration;
import com.safwat.hr.report.payroll.ui.UiField;
import com.safwat.hr.service.payroll.dto.PayrollRequest;
import com.safwat.hr.shared.util.DateUtils;
import com.safwat.hr.utils.ApiClient;

import java.util.List;

public class PayrollChangeCardEmployee implements ReportStrategy {
    @Override
    public String getCode() {
        return "CHANGE_CARD_EMPLOYEE";
    }

    @Override
    public String getDisplayName() {
        return "موظف محدد";
    }

    @Override
    public String getCategory() {
        return "PAYROLL_CHANGE_CARD";
    }

    @Override
    public String getMainReport() {
        return "PAYROLL_CHANGE_CARD";
    }

    @Override
    public UiConfiguration getUiConfig() {
        return UiConfiguration.builder()
                .requiredFields(List.of(UiField.H_START_DATE, UiField.H_END_DATE, UiField.H_EMPLOYEE))
                .visibleFields(List.of(UiField.H_START_DATE, UiField.H_END_DATE, UiField.H_EMPLOYEE))
                .build();
    }

    @Override
    public void onApply(PayrollReportController c) {
        c.setStartAndEndActions();
        c.setSearchEmployeeActions();
    }


    @Override
    public PayrollRequest buildRequest(ReportContext ctx) {
        return PayrollRequest.builder()
                .user(ApiClient.getUserName())
                .startDate(DateUtils.getFirstDayOfMonth(ctx.getStartDate()))
                .endDate(DateUtils.getFirstDayOfMonth(ctx.getEndDate()))
                .report(getCode())
                .reportName(ctx.getReportName())
                .nationalId(ctx.getNationalId())

                .format(ctx.getFormat())
                .build();
    }

    @Override
    public void validate(ReportContext context) {
        if (context.getStartDate() == null || context.getStartDate().isBlank()) {
            throw new ValidationException("يجب اختيار بداية التقرير أولاً!");
        }
        if (context.getNationalId() == null || context.getNationalId().isBlank()) {
            throw new ValidationException("يجب تحديد موظف أولا");
        }
    }
}
