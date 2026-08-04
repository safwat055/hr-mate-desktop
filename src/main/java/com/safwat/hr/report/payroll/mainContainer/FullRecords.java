package com.safwat.hr.report.payroll.mainContainer;

import com.safwat.hr.report.core.ReportContext;
import com.safwat.hr.report.core.strategies.ReportStrategy;
import com.safwat.hr.report.core.strategies.ReportStrategyRegistry;
import com.safwat.hr.report.core.ui.UiConfiguration;
import com.safwat.hr.shared.PayrollRequest;


public class FullRecords implements ReportStrategy {

    @Override
    public String getCode() {
        return "FULL_RECORD";
    }

    @Override
    public String getDisplayName() {
        return "استخراج سجلات 129 سايرة";
    }

    @Override
    public String getCategory() {
        return "main_container";
    }

    /**
     * يُعيد فئة التقارير الفرعية التابعة لهذا الحاوي.
     * يُستخدَم في {@link ReportStrategyRegistry#getDisplayNamesByCategory(String)}
     * لجلب قائمة الفرعيات.
     */
    @Override
    public String getMainReport() {
        return "FULL_RECORD";
    }

    /**
     * هذا تقرير حاوٍ — يُظهر ComboBox فرعي ولا يُرسِل مباشرةً
     */
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

    /**
     * التقارير الحاوية لا تُرسِل بيانات مباشرةً.
     * الإرسال يتم من خلال الاستراتيجية الفرعية المختارة.
     *
     * @throws UnsupportedOperationException دائمًا
     */
    @Override
    public PayrollRequest buildRequest(ReportContext context) {
        throw new UnsupportedOperationException(
                "'" + getDisplayName() + "' تقرير حاوٍ — استخدم التقرير الفرعي للإرسال"
        );
    }
}