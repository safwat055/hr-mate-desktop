package com.safwat.hr.report.payroll.sub.payrollReview.elementCompare;

import com.safwat.hr.network.ApiClient;
import com.safwat.hr.report.controller.PayrollReportController;
import com.safwat.hr.report.core.ReportContext;
import com.safwat.hr.report.core.ValidationException;
import com.safwat.hr.report.core.strategies.ReportStrategy;
import com.safwat.hr.report.core.ui.UiConfiguration;
import com.safwat.hr.report.core.ui.UiField;
import com.safwat.hr.shared.PayrollRequest;
import com.safwat.hr.shared.util.DateUtils;

import java.util.List;

public class ElementCompareEmployees implements ReportStrategy {
    @Override
    public String getCode() {
        return "ELEMENT_COMPARE_EMPLOYEES";
    }

    @Override
    public String getDisplayName() {
        return "تقرير تعديلات عناصر الصرفية الرئيسية لكل الموظفين";
    }

    @Override
    public String getCategory() {
        return "ELEMENT_COMPARE";
    }

    @Override
    public String getMainReport(
            
    ) {
        return "ELEMENT_COMPARE";
    }

    @Override
    public UiConfiguration getUiConfig() {
        return UiConfiguration.builder()
                .requiredFields(List.of(UiField.H_START_DATE))
                .visibleFields(List.of(UiField.H_START_DATE))

                .build();
    }

    @Override
    public void onApply(PayrollReportController controller) {
        controller.setChoseMonth();

    }

    @Override
    public PayrollRequest buildRequest(ReportContext context) {
        return PayrollRequest.builder()
                .user(ApiClient.getUserName())
                .reportName(context.getReportName())
                .report(getCode())
                .startDate(DateUtils.getFirstDayOfMonth(context.getStartDate()))
                .format(context.getFormat())
                .build();
    }

    @Override
    public void validate(ReportContext context) {

        if (context.getStartDate() == null || context.getStartDate().isBlank()) {
            throw new ValidationException("يجب اختيار  شهر");
        }

    }
}
