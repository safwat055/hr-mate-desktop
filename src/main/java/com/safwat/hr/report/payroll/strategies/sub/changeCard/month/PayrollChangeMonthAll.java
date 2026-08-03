package com.safwat.hr.report.payroll.strategies.sub.changeCard.month;

import com.safwat.hr.controller.report.payroll.PayrollReportController;
import com.safwat.hr.report.payroll.ReportContext;
import com.safwat.hr.report.payroll.ValidationException;
import com.safwat.hr.report.payroll.strategies.ReportStrategy;
import com.safwat.hr.report.payroll.ui.UiConfiguration;
import com.safwat.hr.report.payroll.ui.UiField;
import com.safwat.hr.service.payroll.dto.PayrollRequest;
import com.safwat.hr.shared.util.DateUtils;
import com.safwat.hr.utils.ApiClient;

public class PayrollChangeMonthAll implements ReportStrategy {
    @Override
    public String getCode() {
        return "CHANGE_MONTH_ALL";
    }

    @Override
    public String getDisplayName() {
        return "اجر اشتراك شهر للكل";
    }

    @Override
    public String getCategory() {
        return "CHANGE_MONTH";
    }

    @Override
    public String getMainReport() {
        return "CHANGE_MONTH";
    }

    @Override
    public UiConfiguration getUiConfig() {
        return UiConfiguration.builder()
                .requiredField(UiField.H_START_DATE)
                .visibleField(UiField.H_START_DATE)
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
                .build();
    }

    @Override
    public void validate(ReportContext context) {
        if (context.getStartDate() == null || context.getStartDate().isBlank()) {
            throw new ValidationException("حقل الشهر مطلوب");

        }
    }
}
