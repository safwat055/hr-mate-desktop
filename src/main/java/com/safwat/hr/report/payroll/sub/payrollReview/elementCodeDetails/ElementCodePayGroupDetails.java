package com.safwat.hr.report.payroll.sub.payrollReview.elementCodeDetails;

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

public class ElementCodePayGroupDetails implements ReportStrategy {
    @Override
    public String getCode() {
        return "DETAILS_ELEMENT_CODE_PAY_GROUP";
    }

    @Override
    public String getDisplayName() {
        return "عنصر بالكود الاقتصادي لمجموعة تعيين محددة";
    }

    @Override
    public String getCategory() {
        return "DETAILS_ELEMENT_CODE";
    }

    @Override
    public String getMainReport() {
        return "DETAILS_ELEMENT_CODE";
    }

    @Override
    public UiConfiguration getUiConfig() {
        return UiConfiguration.builder()
                .requiredFields(List.of(UiField.H_START_DATE, UiField.H_END_DATE, UiField.H_SEARCH, UiField.H_PAY_GROUP))
                .visibleFields(List.of(UiField.H_START_DATE, UiField.H_END_DATE, UiField.H_SEARCH, UiField.H_PAY_GROUP))
                .searchField(SearchFieldConfig.of(UiField.H_SEARCH, "اختر كود اقتصدي", "elementsCodes"))
                .searchField(SearchFieldConfig.of(UiField.H_PAY_GROUP, "اختر إدارة", "payGroup"))
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
                .payGroup(context.getPayGroup())
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
        if (context.getPayGroup() == null || context.getPayGroup().isBlank()) {
            throw new ValidationException("يجب اختيار مجموعة تعيين أولا");
        }
        if (context.getSearchValue() == null || context.getSearchValue().isBlank()) {
            throw new ValidationException("يجب اختيار عنصر أولا");
        }

    }
}
