package com.safwat.hr.report.payroll.strategies.mainContainer;


import com.safwat.hr.report.payroll.ReportContext;
import com.safwat.hr.report.payroll.strategies.ReportStrategy;
import com.safwat.hr.report.payroll.ui.UiConfiguration;
import com.safwat.hr.service.payroll.dto.PayrollRequest;


public class PayrollChangeCard implements ReportStrategy {

    @Override
    public String getCode() {
        return "PAYROLL_CHANGE_CARD";
    }

    @Override
    public String getDisplayName() {
        return "بطاقة اجر الاشتراك Payroll";
    }

    @Override
    public String getCategory() {
        return "main_container";
    }


    @Override
    public String getMainReport() {
        return "PAYROLL_CHANGE_CARD";
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