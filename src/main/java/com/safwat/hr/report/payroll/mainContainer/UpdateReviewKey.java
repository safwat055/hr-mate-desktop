package com.safwat.hr.report.payroll.mainContainer;

import com.safwat.hr.report.core.ReportContext;
import com.safwat.hr.report.core.strategies.ReportStrategy;
import com.safwat.hr.report.core.ui.UiConfiguration;
import com.safwat.hr.shared.PayrollRequest;


public class UpdateReviewKey implements ReportStrategy {
    @Override
    public String getCode() {
        return "UPDATE_REVIEW_ALL";
    }

    @Override
    public String getDisplayName() {
        return "تحديث مفاتيح تقارير المراجعة";
    }

    @Override
    public String getCategory() {
        return "main_container";
    }

    @Override
    public String getMainReport() {
        return "UPDATE_REVIEW_ALL";
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
