package com.safwat.hr.report.payroll.strategies.mainContainer;

import com.safwat.hr.report.payroll.ReportContext;
import com.safwat.hr.report.payroll.strategies.ReportStrategy;
import com.safwat.hr.report.payroll.ui.UiConfiguration;
import com.safwat.hr.service.payroll.dto.PayrollRequest;

public class UploadPayrollReport implements ReportStrategy {
    @Override
    public String getCode() {
        return "UPLOAD_PAYROLL";
    }

    @Override
    public String getDisplayName() {
        return "رفع تقارير المنظومة";
    }

    @Override
    public String getCategory() {
        return "main_container";
    }

    @Override
    public String getMainReport() {
        return "UPLOAD_PAYROLL";
    }

    @Override
    public UiConfiguration getUiConfig() {
        return UiConfiguration.builder().build();
    }

    @Override
    public PayrollRequest buildRequest(ReportContext context) {
        throw new UnsupportedOperationException(
                "'" + getDisplayName() + "' تقرير حاوٍ — استخدم التقرير الفرعي للإرسال"
        );
    }

    @Override
    public boolean hasSubReports() {
        return true;
    }
}
