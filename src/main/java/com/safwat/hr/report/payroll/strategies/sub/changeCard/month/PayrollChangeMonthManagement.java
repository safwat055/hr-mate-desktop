package com.safwat.hr.report.payroll.strategies.sub.changeCard.month;

import com.safwat.hr.controller.report.payroll.PayrollReportController;
import com.safwat.hr.report.payroll.ReportContext;
import com.safwat.hr.report.payroll.ValidationException;
import com.safwat.hr.report.payroll.strategies.ReportStrategy;
import com.safwat.hr.report.payroll.ui.SearchFieldConfig;
import com.safwat.hr.report.payroll.ui.UiConfiguration;
import com.safwat.hr.report.payroll.ui.UiField;
import com.safwat.hr.service.payroll.dto.PayrollRequest;
import com.safwat.hr.shared.util.DateUtils;
import com.safwat.hr.utils.ApiClient;

import java.util.List;

public class PayrollChangeMonthManagement implements ReportStrategy {
    @Override
    public String getCode() {
        return "CHANGE_MONTH_MANAGEMENT";
    }

    @Override
    public String getDisplayName() {
        return "اجر اشتراك شهر لادارة";
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
                .requiredFields(List.of(UiField.H_START_DATE, UiField.H_MANAGEMENT))
                .visibleFields(List.of(UiField.H_START_DATE, UiField.H_MANAGEMENT))
                .searchField(SearchFieldConfig.of(UiField.H_MANAGEMENT, "اختر إدارة", "management"))
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
                .management(context.getManagement())
                .build();
    }

    @Override
    public void validate(ReportContext context) {
        if (context.getStartDate() == null || context.getStartDate().isBlank()) {
            throw new ValidationException("حقل الشهر مطلوب");

        }
        if (context.getManagement() == null || context.getManagement().isBlank()) {
            throw new ValidationException("حقل الإدارة مطلوب");

        }
    }
}
