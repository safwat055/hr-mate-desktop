package com.safwat.hr.report.payroll.strategies.mainContainer;

import com.safwat.hr.report.payroll.ReportContext;
import com.safwat.hr.report.payroll.strategies.ReportStrategy;
import com.safwat.hr.report.payroll.ui.UiConfiguration;
import com.safwat.hr.service.payroll.dto.PayrollRequest;

public class ReviewReport implements ReportStrategy {
    @Override
    public String getCode() {
        return "REVIEW_REPORT";
    }

    @Override
    public String getDisplayName() {
        return "تقرير المراجعة للصرفيات الرئيسية";
    }

    @Override
    public String getCategory() {
        return "main_container";
    }

    @Override
    public String getMainReport() {
        return "REVIEW_REPORT";
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
