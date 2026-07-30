package com.safwat.hr.report.payroll.strategies.mainContainer;

import com.safwat.hr.report.payroll.PayrollReport;
import com.safwat.hr.report.payroll.ReportContext;
import com.safwat.hr.report.payroll.strategies.ReportStrategy;
import com.safwat.hr.report.payroll.ui.UiConfiguration;
import com.safwat.hr.service.payroll.dto.PayrollRequest;

import java.util.List;

@PayrollReport(
        code = "payReport_1",
        displayName = "تقرير الصرفيات الشهري",
        category = "main_container",
        mainReport = "yearly_payroll"
)
public class MonthlyExpensesContainerStrategy implements ReportStrategy {

    @Override
    public String getCode() {
        return "payReport_1";
    }

    @Override
    public String getDisplayName() {
        return "تقرير الصرفيات الشهري";
    }

    @Override
    public String getCategory() {
        return "main_container";
    }

    /**
     * عنده تقارير فرعية
     */
    @Override
    public boolean hasSubReports() {
        return true;
    }

    @Override
    public UiConfiguration getUiConfig() {
        // الحاوي نفسه مبيظهرش حقول — الفرعي هو اللي بيتحكم
        return UiConfiguration.builder()
                .title("اختر التقرير الفرعي")
                .visibleFields(List.of())
                .build();
    }

    @Override
    public PayrollRequest buildRequest(ReportContext ctx) {
        // الحاوي مبيبنيش Request — الفرعي هو اللي يبني
        throw new UnsupportedOperationException("Container report — use sub-report instead");
    }

    @Override
    public String getMainReport() {
        return "yearly_payroll";
    }
}