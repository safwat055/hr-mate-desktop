package com.safwat.hr.report.payroll.sub.payrollReview.elementCodeTotal;

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
        code = "TOTAL_ELEMENT_CODE_EMPLOYEE",
        displayName = "عنصر بالكود الاقتصادي لموظف إجمالي",
        category = "TOTAL_ELEMENT_CODE",
        mainReport = "TOTAL_ELEMENT_CODE"
)
public class ElementCodeEmployeeTotal implements ReportStrategy {
    @Override
    public String getCode() {
        return "TOTAL_ELEMENT_CODE_EMPLOYEE";
    }

    @Override
    public String getDisplayName() {
        return "عنصر بالكود الاقتصادي لموظف إجمالي";
    }

    @Override
    public String getCategory() {
        return "TOTAL_ELEMENT_CODE";
    }

    @Override
    public String getMainReport() {
        return "TOTAL_ELEMENT_CODE";
    }

    @Override
    public UiConfiguration getUiConfig() {
        return UiConfiguration.builder()
                .requiredFields(List.of(UiField.H_START_DATE, UiField.H_END_DATE, UiField.H_EMPLOYEE, UiField.H_SEARCH))
                .visibleFields(List.of(UiField.H_START_DATE, UiField.H_END_DATE, UiField.H_EMPLOYEE, UiField.H_SEARCH))
                .searchField(SearchFieldConfig.of(UiField.H_SEARCH, "اختر كود اقتصادي", "elementsCodes"))
                .build();
    }

    @Override
    public void onApply(PayrollReportController controller) {
        controller.setStartAndEndActions();
        controller.setSearchEmployeeActions();
        controller.getLbl_search().setText("اسم العنصر");

    }

    @Override
    public PayrollRequest buildRequest(ReportContext context) {
        return PayrollRequest.builder()
                .user(ApiClient.getUserName())
                .reportName(context.getReportName())
                .report(getCode())
                .startDate(DateUtils.getFirstDayOfMonth(context.getStartDate()))
                .endDate(DateUtils.getFirstDayOfMonth(context.getEndDate()))
                .nationalId(context.getNationalId())
                .searchValue(context.getSearchValue())
                .format(context.getFormat())
                .build();
    }

    @Override
    public void validate(ReportContext context) {
        if (context.getNationalId() == null || context.getNationalId().isBlank()) {
            throw new ValidationException("يجب اختيار موظف");
        }
        if (context.getStartDate() == null || context.getStartDate().isBlank()) {
            throw new ValidationException("يجب اختيار بداية التقرير");
        }
        if (context.getEndDate() == null || context.getEndDate().isBlank()) {
            throw new ValidationException("يجب اختيار نهاية التقرير");
        }
        if (context.getSearchValue() == null || context.getSearchValue().isBlank()) {
            throw new ValidationException("يجب اختيار عنصر أولا");
        }
    }
}
