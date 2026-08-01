package com.safwat.hr.report.payroll.strategies.mainContainer;

import com.safwat.hr.report.payroll.ReportContext;
import com.safwat.hr.report.payroll.strategies.ReportStrategy;
import com.safwat.hr.report.payroll.ui.UiConfiguration;
import com.safwat.hr.service.payroll.dto.PayrollRequest;

public class PayrollChangeMonth implements ReportStrategy {
    @Override
    public String getCode() {
        return "CHANGE_MONTH";
    }

    @Override
    public String getDisplayName() {
        return "اجر الاشتراك لشهر";
    }

    @Override
    public String getCategory() {
        return "main_container";
    }

    @Override
    public String getMainReport() {
        return "CHANGE_MONTH";
    }

    @Override
    public boolean hasSubReports() {
        return true;
    }

    /**
     * التقارير الحاوية لا تملك نموذجًا خاصًا بها —
     * الـ Controller يُظهر ComboBox الفرعيات مباشرةً.
     */
    @Override
    public UiConfiguration getUiConfig() {
        return UiConfiguration.builder()
                .title("اختر التقرير الفرعي")
                .build();
    }


    @Override
    public PayrollRequest buildRequest(ReportContext context) {
        throw new UnsupportedOperationException(
                "'" + getDisplayName() + "' تقرير حاوٍ — استخدم التقرير الفرعي للإرسال"
        );
    }
}
