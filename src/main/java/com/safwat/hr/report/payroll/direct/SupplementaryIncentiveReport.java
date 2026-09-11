package com.safwat.hr.report.payroll.direct;

import com.safwat.hr.controller.report.PayrollReportController;
import com.safwat.hr.report.core.ReportContext;
import com.safwat.hr.report.core.ValidationException;
import com.safwat.hr.report.core.strategies.ReportStrategy;
import com.safwat.hr.report.core.ui.UiConfiguration;
import com.safwat.hr.report.core.ui.UiField;
import com.safwat.hr.shared.PayrollRequest;
import com.safwat.hr.shared.util.DateUtils;

import java.util.List;

public class SupplementaryIncentiveReport implements ReportStrategy {
    @Override
    public String getCode() {
        return "SUPPLEMENTARY_INCENTIVE";
    }

    @Override
    public String getDisplayName() {
        return "احتساب الحافز التكميلي للموظفين من تقرير المراجعة";
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
                .requiredFields(List.of(UiField.H_START_DATE, UiField.H_SupplementaryIncentive))
                .visibleFields(List.of(UiField.H_START_DATE, UiField.H_SupplementaryIncentive))
                .build();
    }

    @Override
    public void onApply(PayrollReportController c) {
        c.setupMonthButton(c.getBtn_searchMonth(), c.getTxt_startDate(), c.getLbl_startDate());

    }

    @Override
    public PayrollRequest buildRequest(ReportContext context) {
        return PayrollRequest.builder()
                .reportName(context.getReportName())
                .report(getCode())
                .startDate(DateUtils.getFirstDayOfMonth(context.getStartDate()))
                .intValue(context.getIntValue())
                .doubleValue(context.getDoubleValue())
                .build();
    }

    @Override
    public void validate(ReportContext context) {
        if (context.getStartDate() == null) {
            throw new ValidationException("يجب اختيار شهر اولا");
        }
    }
}
