package com.safwat.hr.report.payroll.sub.records.full;

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

public class FullRecordPayGroup implements ReportStrategy {
    @Override
    public String getCode() {
        return "FULL_RECORD_PAY_GROUP";
    }

    @Override
    public String getDisplayName() {
        return "سجل 129 كامل لكل لمجموعة تعيين محددة";
    }

    @Override
    public String getCategory() {
        return "FULL_RECORD";
    }

    @Override
    public String getMainReport() {
        return "FULL_RECORD";
    }

    @Override
    public UiConfiguration getUiConfig() {
        return UiConfiguration.builder()
                .requiredFields(List.of(UiField.H_START_DATE, UiField.H_END_DATE, UiField.H_PAY_GROUP))
                .visibleFields(List.of(UiField.H_START_DATE, UiField.H_END_DATE, UiField.H_PAY_GROUP))
                .searchField(SearchFieldConfig.of(UiField.H_PAY_GROUP, "اختر ادارة", "payGroup"))

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
                .payGroup(context.getPayGroup())
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
        if (context.getPayGroup() == null || context.getPayGroup().isBlank()) {
            throw new ValidationException("مجموعة التعيين مطلوبة");
        }
    }
}
