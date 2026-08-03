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

public class PayrollChangeCardAll implements ReportStrategy {
    @Override
    public String getCode() {
        return "CHANGE_CARD_ALL";
    }

    @Override
    public String getDisplayName() {
        return "بطاقات اجر الاشتراك لكل الموظفين";
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
                .requiredFields(List.of(UiField.H_START_DATE, UiField.H_END_DATE))
                .visibleFields(List.of(UiField.H_START_DATE, UiField.H_END_DATE))
                .build();
    }

    @Override
    public void onApply(PayrollReportController c) {

        c.setStartAndEndActions();

    }


    @Override
    public PayrollRequest buildRequest(ReportContext ctx) {
        return PayrollRequest.builder()
                .user(ApiClient.getUserName())
                .startDate(DateUtils.getFirstDayOfMonth(ctx.getStartDate()))
                .endDate(DateUtils.getFirstDayOfMonth(ctx.getEndDate()))
                .report(getCode())
                .reportName(ctx.getReportName())
                .format(ctx.getFormat())
                .build();
    }

    @Override
    public void validate(ReportContext context) {
        if (context.getStartDate() == null || context.getStartDate().isBlank()) {
            throw new ValidationException("يجب اختيار بداية التقرير أولاً!");
        }
    }
}
