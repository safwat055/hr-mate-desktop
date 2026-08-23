package com.safwat.hr.report.payroll.direct;

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
        code = "ELEMENT_COMPARE_ADDED_DELETED",
        displayName = "تقرير العناصر المضافة والمحذوفة",
        category = "main_direct",
        mainReport = "main_direct"
)
public class ElementComparisonAddedDeletedReport implements ReportStrategy {
    @Override
    public String getCode() {
        return "ELEMENT_COMPARE_ADDED_DELETED";
    }

    @Override
    public String getDisplayName() {
        return "تقرير العناصر المضافة والمحذوفة";
    }

    @Override
    public String getCategory() {
        return "main_direct";
    }

    @Override
    public String getMainReport() {
        return "main_direct";
    }

    @Override
    public UiConfiguration getUiConfig() {
        return UiConfiguration.builder()
                .requiredFields(List.of(UiField.H_REPORT_TYPE, UiField.H_MANAGEMENT, UiField.H_START_DATE, UiField.H_PAY_GROUP))
                .visibleFields(List.of(UiField.H_REPORT_TYPE, UiField.H_MANAGEMENT, UiField.H_START_DATE, UiField.H_PAY_GROUP))
                .searchFields(List.of(
                        SearchFieldConfig.of(UiField.H_MANAGEMENT, "", "management"),
                        SearchFieldConfig.of(UiField.H_PAY_GROUP, "", "payGroup")

                ))

                .build();
    }

    @Override
    public void onApply(PayrollReportController controller) {
        controller.setChoseMonth();

        controller.getCombo_reportType().getItems().clear();
        controller.getCombo_reportType().getItems().addAll("العناصر المضافة", "الاستحقاقات المضافة", "الاستقطاعات المضافة", "العناصر المحذوفة", "الاستحقاقات المحذوفة", "الاستقطاعات المحذوفة");
        //controller.setSearchEmployeeActions();
    }

    @Override
    public PayrollRequest buildRequest(ReportContext context) {

        return PayrollRequest.builder()
                .user(ApiClient.getUserName())
                .reportName(context.getReportName())
                .report(getCode())
                .payGroup(context.getPayGroup() == null ? null : context.getPayGroup())
                .management(context.getManagement() == null ? null : context.getManagement())
                .startDate(DateUtils.getFirstDayOfMonth(context.getStartDate()))
                .reportType(context.getReportType())
                .build();
    }

    @Override
    public void validate(ReportContext context) {
        if (context.getReportType() == null || context.getReportType().isBlank()) {
            throw new ValidationException("يجب تحديد نوع التقرير ");
        }
        if (context.getStartDate() == null || context.getStartDate().isBlank()) {
            throw new ValidationException("يجب تحديد فترة ");
        }
    }
}
