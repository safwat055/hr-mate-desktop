package com.safwat.hr.report.payroll.sub.changeCard.card;

import com.safwat.hr.network.ApiClient;
import com.safwat.hr.report.controller.PayrollReportController;
import com.safwat.hr.report.core.ReportContext;
import com.safwat.hr.report.core.ValidationException;
import com.safwat.hr.report.core.strategies.ReportStrategy;
import com.safwat.hr.report.core.ui.SearchFieldConfig;
import com.safwat.hr.report.core.ui.UiConfiguration;
import com.safwat.hr.report.core.ui.UiField;
import com.safwat.hr.shared.PayrollRequest;
import com.safwat.hr.shared.util.DateUtils;

import java.util.List;

public class PayrollChangeCardManagement implements ReportStrategy {
    @Override
    public String getCode() {
        return "CHANGE_CARD_MANAGEMENT";
    }

    @Override
    public String getDisplayName() {
        return "بطاقة اجر الاشتراك لادارة";
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
                .requiredFields(List.of(UiField.H_START_DATE, UiField.H_END_DATE, UiField.H_MANAGEMENT))
                .visibleFields(List.of(UiField.H_START_DATE, UiField.H_END_DATE, UiField.H_MANAGEMENT))
                .searchField(SearchFieldConfig.of(UiField.H_MANAGEMENT, "اختر إدارة", "management"))
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
                .management(ctx.getManagement())
                .format(ctx.getFormat())
                .build();
    }

    @Override
    public void validate(ReportContext context) {
        if (context.getStartDate() == null || context.getStartDate().isBlank()) {
            throw new ValidationException("يجب اختيار بداية التقرير أولاً!");
        }

        if (context.getManagement() == null || context.getManagement().isBlank()) {
            throw new ValidationException("يجب اختيار الإدارة أولاً!");
        }
    }
}
