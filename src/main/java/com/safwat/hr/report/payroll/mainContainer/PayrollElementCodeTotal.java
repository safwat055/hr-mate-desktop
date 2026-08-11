package com.safwat.hr.report.payroll.mainContainer;


import com.safwat.hr.report.core.ReportContext;
import com.safwat.hr.report.core.strategies.ReportStrategy;
import com.safwat.hr.report.core.ui.UiConfiguration;
import com.safwat.hr.shared.PayrollRequest;


public class PayrollElementCodeTotal implements ReportStrategy {

    @Override
    public String getCode() {
        return "TOTAL_ELEMENT_CODE";
    }

    @Override
    public String getDisplayName() {
        return "تقرير عنصر معين بالكود الاقتصادي إجماليات";
    }

    @Override
    public String getCategory() {
        return "main_container";
    }


    @Override
    public String getMainReport() {
        return "TOTAL_ELEMENT_CODE";
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