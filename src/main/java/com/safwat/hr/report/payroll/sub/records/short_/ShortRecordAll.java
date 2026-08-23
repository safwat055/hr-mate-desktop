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
        code = "SHORT_RECORD_ALL",
        displayName = "سجل 129 مختصر لكل الموظفين",
        category = "SHORT_RECORD",
        mainReport = "SHORT_RECORD"
)
public class ShortRecordAll implements ReportStrategy {
    @Override
    public String getCode() {
        return "SHORT_RECORD_ALL";

    }

    @Override
    public String getDisplayName() {
        return "سجل 129 مختصر لكل الموظفين";
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
                .requiredFields(List.of(UiField.H_START_DATE, UiField.H_END_DATE))
                .visibleFields(List.of(UiField.H_START_DATE, UiField.H_END_DATE))

                .build();
    }

    @Override
    public void onApply(PayrollReportController controller) {
        controller.setStartAndEndActions();
    }

    @Override
    public PayrollRequest buildRequest(ReportContext context) {
        return PayrollRequest.builder()
                .reportName(context.getReportName())
                .user(ApiClient.getUserName())
                .report(getCode())
                .startDate(DateUtils.getFirstDayOfMonth(context.getStartDate()))
                .endDate(DateUtils.getFirstDayOfMonth(context.getEndDate()))
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
    }
}
