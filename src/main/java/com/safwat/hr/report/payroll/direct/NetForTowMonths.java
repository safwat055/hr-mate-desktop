package com.safwat.hr.report.payroll.direct;

import com.safwat.hr.report.controller.PayrollReportController;
import com.safwat.hr.report.core.PayrollReport;
import com.safwat.hr.report.core.ReportContext;
import com.safwat.hr.report.core.ValidationException;
import com.safwat.hr.report.core.strategies.ReportStrategy;
import com.safwat.hr.report.core.ui.UiConfiguration;
import com.safwat.hr.report.core.ui.UiField;
import com.safwat.hr.shared.PayrollRequest;
import com.safwat.hr.shared.util.DateUtils;

import java.util.List;

@PayrollReport(
        code = "payrollYearly_5",
        displayName = "تقرير صافى شهرين",
        category = "main_direct",
        mainReport = "main_direct"
)
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
