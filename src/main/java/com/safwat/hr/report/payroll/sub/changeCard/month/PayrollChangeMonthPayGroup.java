package com.safwat.hr.report.payroll.sub.changeCard.month;

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

public class PayrollChangeMonthPayGroup implements ReportStrategy {
    @Override
    public String getCode() {
        return "CHANGE_MONTH_PAY_GROUP";
    }

    @Override
    public String getDisplayName() {
        return "اجر اشتراك شهر لمجموعة تعيين";
    }

    @Override
    public String getCategory() {
        return "CHANGE_MONTH";
    }

    @Override
    public String getMainReport() {
        return "CHANGE_MONTH";
    }

    @Override
    public UiConfiguration getUiConfig() {
        return UiConfiguration.builder()
                .requiredFields(List.of(UiField.H_START_DATE, UiField.H_PAY_GROUP))
                .visibleFields(List.of(UiField.H_START_DATE, UiField.H_PAY_GROUP))
                .searchField(SearchFieldConfig.of(UiField.H_PAY_GROUP, "اختر إدارة", "payGroup"))
                /*   .searchField(SearchFieldConfig.multiSelectOf(
                           UiField.H_PAY_GROUP,           // الحقل اللي هيفتح البحث
                           "اختر البنود",              // عنوان النافذة
                           "payGroupsByMonth",          // مفتاح الـ DataSource
                           UiField.TXT_START_DATE,       // الحقل المعتمد عليه (الشهر)
                           "payMonth",                     // اسم الـ param
                           value -> DateUtils.getFirstDayOfMonth((value)).toString()
                   ))*/
                .build();
    }

    @Override
    public void onApply(PayrollReportController controller) {
        controller.setChoseMonth();
    }

    @Override
    public PayrollRequest buildRequest(ReportContext context) {
        return PayrollRequest.builder()
                .user(ApiClient.getUserName())
                .reportName(context.getReportName())
                .report(getCode())
                .startDate(DateUtils.getFirstDayOfMonth(context.getStartDate()))
                .payGroup(context.getPayGroup())
                .format(context.getFormat())
                .build();
    }

    @Override
    public void validate(ReportContext context) {
        if (context.getStartDate() == null || context.getStartDate().isBlank()) {
            throw new ValidationException("حقل الشهر مطلوب");

        }
        if (context.getPayGroup() == null || context.getPayGroup().isBlank()) {
            throw new ValidationException("حقل مجوعة التعيين مطلوب");

        }
    }
}
