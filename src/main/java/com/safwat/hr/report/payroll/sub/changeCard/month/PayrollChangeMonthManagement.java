package com.safwat.hr.report.payroll.sub.changeCard.month;

import com.safwat.hr.network.ApiClient;
import com.safwat.hr.report.controller.PayrollReportController;
import com.safwat.hr.report.core.PayrollReport;
import com.safwat.hr.report.core.ReportContext;
import com.safwat.hr.report.core.ValidationException;
import com.safwat.hr.report.core.strategies.ReportStrategy;
import com.safwat.hr.report.core.ui.SearchFieldConfig;
import com.safwat.hr.report.core.ui.UiConfiguration;
import com.safwat.hr.report.core.ui.UiField;
import com.safwat.hr.shared.PayrollRequest;
import com.safwat.hr.shared.util.DateUtils;

import java.util.List;

@PayrollReport(
        code = "CHANGE_MONTH_MANAGEMENT",
        displayName = "اجر اشتراك شهر لادارة",
        category = "CHANGE_MONTH",
        mainReport = "CHANGE_MONTH"
)
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
                .format(context.getFormat())
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
