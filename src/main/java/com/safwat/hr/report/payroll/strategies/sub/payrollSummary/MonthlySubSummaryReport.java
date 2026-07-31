package com.safwat.hr.report.payroll.strategies.sub.payrollSummary;

import com.safwat.hr.report.payroll.ReportContext;
import com.safwat.hr.report.payroll.ValidationException;
import com.safwat.hr.report.payroll.strategies.ReportStrategy;
import com.safwat.hr.report.payroll.ui.UiConfiguration;
import com.safwat.hr.report.payroll.ui.UiField;
import com.safwat.hr.service.payroll.dto.PayrollRequest;
import com.safwat.hr.shared.util.DateUtils;
import com.safwat.hr.utils.ApiClient;
import com.safwat.hr.utils.ApiEndpoints;

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
                .visibleField(UiField.START_DATE)
                .requiredField(UiField.START_DATE)
                .build();
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
