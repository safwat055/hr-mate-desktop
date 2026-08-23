package com.safwat.hr.report.payroll.sub.payrollReview.reviewReport;

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

@PayrollReport(
        code = "MAIN_REVIEW_REPORT",
        displayName = "تقرير مراجعة للصرفيات الرئيسية",
        category = "REVIEW_REPORT",
        mainReport = "REVIEW_REPORT"
)
public class MainReviewReport implements ReportStrategy {
    @Override
    public String getCode() {
        return "MAIN_REVIEW_REPORT";
    }

    @Override
    public String getDisplayName() {
        return "تقرير مراجعة للصرفيات الرئيسية";
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
                .requiredField(UiField.H_START_DATE)
                .requiredField(UiField.H_EMPLOYEE)
                .requiredField(UiField.H_MANAGEMENT)
                .requiredField(UiField.H_PAY_GROUP)
                // .requiredField(UiField.H_REPORT_TYPE)

                .visibleField(UiField.H_START_DATE)
                .visibleField(UiField.H_EMPLOYEE)
                .visibleField(UiField.H_MANAGEMENT)
                .visibleField(UiField.H_PAY_GROUP)
                //.visibleField(UiField.H_REPORT_TYPE)

                .searchField(SearchFieldConfig.of(UiField.H_MANAGEMENT, "اختر إدارة", "management"))
                .searchField(SearchFieldConfig.of(UiField.H_PAY_GROUP, "اختر مجموعة", "payGroup"))

                .build();
    }

    @Override
    public void onApply(PayrollReportController controller) {

        controller.setChoseMonth();

        controller.setSearchEmployeeActions();

    }

    @Override
    public PayrollRequest buildRequest(ReportContext context) {
        String reportType;
        if (context.getManagement() != null && !context.getManagement().isBlank()) {
            reportType = "MANAGEMENT";
        } else if (context.getPayGroup() != null && !context.getPayGroup().isBlank()) {
            reportType = "PAY_GROUP";
        } else if (context.getNationalId() != null && !context.getNationalId().isBlank()) {
            reportType = "EMPLOYEE";
        } else {
            reportType = "ALL";
        }

        return PayrollRequest.builder()
                .user(ApiClient.getUserName())
                .reportName(context.getReportName())
                .report(getCode())
                .startDate(DateUtils.getFirstDayOfMonth(context.getStartDate()))
                .reportType(reportType)
                .management(context.getManagement())
                .payGroup(context.getPayGroup())
                .nationalId(context.getNationalId())
                .build();
    }

    @Override
    public void validate(ReportContext context) {
        if (context.getStartDate() == null || context.getStartDate().isBlank()) {
            throw new ValidationException("الشهر مطلوب");

        }
    }
}
