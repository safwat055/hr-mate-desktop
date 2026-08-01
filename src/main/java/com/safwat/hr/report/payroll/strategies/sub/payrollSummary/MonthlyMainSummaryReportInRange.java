package com.safwat.hr.report.payroll.strategies.sub.payrollSummary;

import com.safwat.hr.controller.report.payroll.PayrollReportController;
import com.safwat.hr.report.payroll.ReportContext;
import com.safwat.hr.report.payroll.ValidationException;
import com.safwat.hr.report.payroll.strategies.ReportStrategy;
import com.safwat.hr.report.payroll.ui.UiConfiguration;
import com.safwat.hr.report.payroll.ui.UiField;
import com.safwat.hr.service.payroll.dto.PayrollRequest;
import com.safwat.hr.shared.util.DateUtils;
import com.safwat.hr.utils.ApiClient;
import com.safwat.hr.utils.ApiEndpoints;

import java.util.List;


public class MonthlyMainSummaryReportInRange implements ReportStrategy {
    @Override
    public String getCode() {
        return "summaryReport_4";
    }

    @Override
    public String getDisplayName() {
        return "إجمالي تكاليف الصرفيات الرئيسية لمدة محدد";
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
                // .title("إجمالي تكاليف الصرفيات الرئيسية لمدة محدد")
                .visibleFields(List.of(UiField.H_START_DATE, UiField.H_END_DATE))
                .requiredFields(List.of(UiField.H_START_DATE, UiField.H_END_DATE))
                .build();
    }

    @Override
    public void onApply(PayrollReportController c) {

        c.setStartAndEndActions();
    }

    @Override
    public PayrollRequest buildRequest(ReportContext context) {
        return PayrollRequest.builder()
                .user(ApiClient.getUserName())
                .reportName(context.getReportName())
                .report(getCode())
                .endPoint(ApiEndpoints.PayrollYearly.PAYROLL_SUMMARY)
                .startDate(DateUtils.getFirstDayOfMonth(context.getStartDate()))
                .endDate(DateUtils.getFirstDayOfMonth(context.getEndDate()))
                .format(context.getFormat())
                .build();
    }

    @Override
    public void validate(ReportContext context) {
        if (context.getStartDate() == null || context.getStartDate().isBlank()) {
            throw new ValidationException("يجب اختيار فترة البداية أولا.");
        }
        if (context.getEndDate() == null || context.getEndDate().isBlank()) {
            throw new ValidationException("يجب اختيار فترة النهاية أولا.");
        }
    }
}
