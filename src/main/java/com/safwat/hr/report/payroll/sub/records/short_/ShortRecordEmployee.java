package com.safwat.hr.report.payroll.sub.records.short_;

import com.safwat.hr.network.ApiClient;
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
        code = "SHORT_RECORD_EMPLOYEE",
        displayName = "سجل 129 مختصر لكل لموظف محددة",
        category = "SHORT_RECORD",
        mainReport = "SHORT_RECORD"
)
public class ShortRecordEmployee implements ReportStrategy {
    @Override
    public String getCode() {
        return "SHORT_RECORD_EMPLOYEE";
    }

    @Override
    public String getDisplayName() {
        return "سجل 129 مختصر لكل لموظف محددة";
    }

    @Override
    public String getCategory() {
        return "SHORT_RECORD";
    }

    @Override
    public String getMainReport() {
        return "SHORT_RECORD";
    }

    @Override
    public UiConfiguration getUiConfig() {
        return UiConfiguration.builder()
                .requiredFields(List.of(UiField.H_START_DATE, UiField.H_END_DATE, UiField.H_EMPLOYEE))
                .visibleFields(List.of(UiField.H_START_DATE, UiField.H_END_DATE, UiField.H_EMPLOYEE))
                //.searchField(SearchFieldConfig.of(UiField.H_PAY_GROUP, "اختر ادارة", "payGroup"))

                .build();
    }

    @Override
    public void onApply(PayrollReportController controller) {
        controller.setStartAndEndActions();
        controller.setSearchEmployeeActions();
    }

    @Override
    public PayrollRequest buildRequest(ReportContext context) {
        return PayrollRequest.builder()
                .reportName(context.getReportName())
                .user(ApiClient.getUserName())
                .report(getCode())
                .startDate(DateUtils.getFirstDayOfMonth(context.getStartDate()))
                .endDate(DateUtils.getFirstDayOfMonth(context.getEndDate()))
                .nationalId(context.getNationalId())
                .build();
    }

    @Override
    public void validate(ReportContext context) {
        if (context.getStartDate() == null || context.getStartDate().isBlank()) {
            throw new ValidationException("فترة البداية مطلوب");
        }
        if (context.getEndDate() == null || context.getEndDate().isBlank()) {
            throw new ValidationException("فترة النهاية مطلوب");
        }
        if (context.getNationalId() == null || context.getNationalId().isBlank()) {
            throw new ValidationException("يجب تحديد موظف اولا");
        }
    }
}
