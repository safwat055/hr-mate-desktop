package com.safwat.hr.report.payroll.strategies.sub.PayrollHistory.element;

import com.safwat.hr.controller.report.payroll.PayrollReportController;
import com.safwat.hr.report.payroll.ReportContext;
import com.safwat.hr.report.payroll.ValidationException;
import com.safwat.hr.report.payroll.strategies.ReportStrategy;
import com.safwat.hr.report.payroll.ui.SearchFieldConfig;
import com.safwat.hr.report.payroll.ui.UiConfiguration;
import com.safwat.hr.report.payroll.ui.UiField;
import com.safwat.hr.service.payroll.dto.PayrollRequest;
import com.safwat.hr.shared.util.DateUtils;
import com.safwat.hr.utils.ApiClient;

import java.util.List;

public class ElementEmployees implements ReportStrategy {
    @Override
    public String getCode() {
        return "ELEMENT_EMPLOYEES";
    }

    @Override
    public String getDisplayName() {
        return "تقرير كل الموظفين";
    }

    @Override
    public String getCategory() {
        return "ELEMENT";
    }

    @Override
    public String getMainReport() {
        return "ELEMENT";
    }

    @Override
    public UiConfiguration getUiConfig() {
        return UiConfiguration.builder()
                .requiredFields(List.of(UiField.H_START_DATE, UiField.H_END_DATE, UiField.H_SEARCH))
                .visibleFields(List.of(UiField.H_START_DATE, UiField.H_END_DATE, UiField.H_SEARCH))
                .searchField(SearchFieldConfig.of(UiField.H_SEARCH, "اختر عنصر", "elements"))
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
                .searchValue(context.getSearchValue())
                .format(context.getFormat())
                .build();
    }

    @Override
    public void validate(ReportContext context) {

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
