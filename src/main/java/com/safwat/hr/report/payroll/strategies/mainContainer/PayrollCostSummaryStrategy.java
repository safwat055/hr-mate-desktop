package com.safwat.hr.report.payroll.strategies.mainContainer;

import com.safwat.hr.report.payroll.PayrollReport;
import com.safwat.hr.report.payroll.ReportContext;
import com.safwat.hr.report.payroll.strategies.ReportStrategy;
import com.safwat.hr.report.payroll.ui.UiConfiguration;
import com.safwat.hr.service.payroll.dto.PayrollRequest;

import java.util.List;

@PayrollReport(
        code = "payReport_2",
        displayName = "تقرير إجمالي التكاليف الشهرى",
        category = "main_container",
        mainReport = "payroll_summary"
)
public class PayrollCostSummaryStrategy implements ReportStrategy {

    @Override
    public String getCode() {
        return "payReport_2";
    }

    @Override
    public String getDisplayName() {
        return "تقرير إجمالي التكاليف الشهرى";
    }


    @Override
    public String getCategory() {
        return "main_container";
    }

    
    @Override
    public boolean hasSubReports() {
        return true;
    }

    @Override
    public UiConfiguration getUiConfig() {
        return UiConfiguration.builder()
                .title("اختر التقرير الفرعى")
                .visibleFields(List.of())
                .build();
    }

    @Override
    public PayrollRequest buildRequest(ReportContext ctx) {
        throw new UnsupportedOperationException("Container report — use sub-report instead");
    }

    @Override
    public String getMainReport() {
        return "payroll_summary";
    }
}