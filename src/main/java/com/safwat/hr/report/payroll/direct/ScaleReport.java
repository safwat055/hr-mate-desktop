package com.safwat.hr.report.payroll.direct;

import com.safwat.hr.controller.report.PayrollReportController;
import com.safwat.hr.report.core.ReportContext;
import com.safwat.hr.report.core.strategies.ReportStrategy;
import com.safwat.hr.report.core.ui.UiConfiguration;
import com.safwat.hr.report.core.ui.UiField;
import com.safwat.hr.shared.PayrollRequest;

import java.util.List;

public class ScaleReport implements ReportStrategy {
    @Override
    public String getCode() {
        return "SALARY_SCALE_REPORT_PDF";
    }

    @Override
    public String getDisplayName() {
        return "استخراج تدرج راتب";
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
                .requiredFields(List.of(UiField.H_EMPLOYEE, UiField.H_REPORT_TYPE))
                .visibleFields(List.of(UiField.H_EMPLOYEE, UiField.H_REPORT_TYPE))

                .build();
    }

    @Override
    public void onApply(PayrollReportController controller) {
        controller.getCombo_reportType().getItems().clear();
        controller.getCombo_reportType().getItems().addAll("موظف واحد", "الكل");
        controller.setSearchEmployeeActions();
    }

    @Override
    public PayrollRequest buildRequest(ReportContext context) {
        return PayrollRequest.builder()

                .report(getCode())
                .reportType(context.getReportType())
                .reportName(context.getReportName())
                .nationalId(context.getNationalId())
                .build();
    }
}
