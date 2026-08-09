package com.safwat.hr.report.payroll.sub.payrollReview.reviewReport;

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

public class ReviewReportPayGroup implements ReportStrategy {
    @Override
    public String getCode() {
        return "REVIEW_REPORT_PAY_GROUP";
    }

    @Override
    public String getDisplayName() {
        return "تفرير مراجعة لمجموعة تعيين";
    }

    @Override
    public String getCategory() {
        return "REVIEW_REPORT";
    }

    @Override
    public String getMainReport() {
        return "REVIEW_REPORT";
    }

    @Override
    public UiConfiguration getUiConfig() {
        return UiConfiguration.builder()
                .requiredFields(List.of(UiField.H_START_DATE, UiField.H_PAY_GROUP))
                .visibleFields(List.of(UiField.H_START_DATE, UiField.H_PAY_GROUP))
                .searchField(SearchFieldConfig.of(UiField.H_PAY_GROUP, "اختر إدارة", "payGroup"))
                .build();
    }

    @Override
    public void onApply(PayrollReportController controller) {
        controller.setChoseMonth();

    }

    @Override
    public PayrollRequest buildRequest(ReportContext context) {
        return PayrollRequest.builder()
                .reportName(context.getReportName())
                .user(ApiClient.getUserName())
                .report(getCode())
                .startDate(DateUtils.getFirstDayOfMonth(context.getStartDate()))
                .payGroup(context.getPayGroup())
                .build();
    }

    @Override
    public void validate(ReportContext context) {
        if (context.getStartDate() == null || context.getStartDate().isEmpty()) {
            throw new ValidationException("الشهر مطلوب");

        }
        if (context.getPayGroup() == null || context.getPayGroup().isBlank()) {
            throw new ValidationException("مجموعة التعيين مطلوبة");
        }
    }
}
