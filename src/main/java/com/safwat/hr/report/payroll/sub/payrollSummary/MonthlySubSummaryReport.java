package com.safwat.hr.report.payroll.sub.payrollSummary;

import com.safwat.hr.network.ApiClient;
import com.safwat.hr.network.ApiEndpoints;
import com.safwat.hr.report.controller.PayrollReportController;
import com.safwat.hr.report.core.ReportContext;
import com.safwat.hr.report.core.ValidationException;
import com.safwat.hr.report.core.strategies.ReportStrategy;
import com.safwat.hr.report.core.ui.UiConfiguration;
import com.safwat.hr.report.core.ui.UiField;
import com.safwat.hr.shared.PayrollRequest;
import com.safwat.hr.shared.util.DateUtils;

public class MonthlySubSummaryReport implements ReportStrategy {
    @Override
    public String getCode() {
        return "summaryReport_5";
    }

    @Override
    public String getDisplayName() {
        return "إجمالي تكاليف الصرفيات المنفصلة لشهر محدد";
    }

    @Override
    public String getCategory() {
        return "payroll_summary";
    }

    @Override
    public String getMainReport() {
        return "payroll_summary";
    }

    @Override
    public UiConfiguration getUiConfig() {
        return UiConfiguration.builder()
//.title("إجمالي تكاليف الصرفيات المنفصلة لشهر محدد")
                .visibleField(UiField.H_START_DATE)
                .requiredField(UiField.H_START_DATE)
                .build();
    }

    @Override
    public void onApply(PayrollReportController c) {
        c.setChoseMonth();

    }

    @Override
    public PayrollRequest buildRequest(ReportContext context) {
        return PayrollRequest.builder()
                .user(ApiClient.getUserName())
                .reportName(context.getReportName())
                .report(getCode())
                .endPoint(ApiEndpoints.PayrollYearly.PAYROLL_SUMMARY)
                .startDate(DateUtils.getFirstDayOfMonth(context.getStartDate()))
                .format(context.getFormat())
                .build();
    }

    /**
     * يتحقق من اختيار الشهر.
     *
     * @throws ValidationException إذا كان الشهر فارغًا
     */
    @Override
    public void validate(ReportContext context) {
        if (context.getStartDate() == null || context.getStartDate().isBlank()) {
            throw new ValidationException("يجب اختيار الشهر أولاً!");
        }
    }
}
