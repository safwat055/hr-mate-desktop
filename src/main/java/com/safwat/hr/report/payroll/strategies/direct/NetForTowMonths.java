package com.safwat.hr.report.payroll.strategies.direct;

import com.safwat.hr.controller.report.payroll.PayrollReportController;
import com.safwat.hr.report.payroll.ReportContext;
import com.safwat.hr.report.payroll.ValidationException;
import com.safwat.hr.report.payroll.strategies.ReportStrategy;
import com.safwat.hr.report.payroll.ui.UiConfiguration;
import com.safwat.hr.report.payroll.ui.UiField;
import com.safwat.hr.service.payroll.dto.PayrollRequest;
import com.safwat.hr.shared.util.DateUtils;

import java.util.List;

public class NetForTowMonths implements ReportStrategy {
    @Override
    public String getCode() {
        return "payrollYearly_5";
    }

    @Override
    public String getDisplayName() {
        return "تقرير صافى شهرين";
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
                .requiredFields(List.of(UiField.H_START_DATE, UiField.H_END_DATE))
                .visibleFields(List.of(UiField.H_START_DATE, UiField.H_END_DATE))

                .build();
    }


    @Override
    public void onApply(PayrollReportController c) {

        c.setupMonthButton(c.getBtn_searchMonth(), c.getTxt_startDate(), c.getLbl_startDate());
        c.setupMonthButton(c.getBtn_searchMonthEnd(), c.getTxt_endDate(), c.getLbl_endDate());
        c.getLbl_start().setText("شهر حالي");
        c.getLbl_end().setText("شهر سابق");

    }

    @Override
    public PayrollRequest buildRequest(ReportContext context) {
        return PayrollRequest.builder()
                .user(context.getUser())
                .reportName(context.getReportName())
                .report(getCode())
                .startDate(DateUtils.getFirstDayOfMonth(context.getStartDate()))
                .endDate(DateUtils.getFirstDayOfMonth(context.getEndDate()))
                .build();
    }

    @Override
    public void validate(ReportContext context) {
        if (context.getStartDate() == null || context.getStartDate().isBlank()) {
            throw new ValidationException("يجب اختيار فترة من أولا.");
        }
    }
}
