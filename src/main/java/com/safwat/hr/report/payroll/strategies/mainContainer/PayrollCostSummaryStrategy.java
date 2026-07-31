package com.safwat.hr.report.payroll.strategies.mainContainer;

import com.safwat.hr.report.payroll.PayrollReport;
import com.safwat.hr.report.payroll.ReportContext;
import com.safwat.hr.report.payroll.strategies.ReportStrategy;
import com.safwat.hr.report.payroll.ui.UiConfiguration;
import com.safwat.hr.service.payroll.dto.PayrollRequest;

/**
 * تقرير رئيسي حاوٍ — "تقرير إجمالي التكاليف الشهري".
 *
 * <p>لا يُرسِل بيانات بنفسه، بل يعمل كحاوٍ (Container)
 * يُظهِر قائمة منسدلة فرعية بالتقارير التابعة له في الـ Controller.
 *
 * <p>التقارير الفرعية التابعة له (الفئة {@code payroll_summary})
 * قيد الإضافة — سجِّلها في
 * {@link com.safwat.hr.report.payroll.strategies.ReportRegistryFactory}
 * بالفئة {@code "payroll_summary"} عند إنشائها.
 *
 * <p><b>ملاحظة — Bug مُصلَح:</b>
 * كان التعليق يقول "مباشر" بينما {@code hasSubReports()} تُعيد {@code true}.
 * الصواب: هذا تقرير حاوٍ فعلاً، والتعليق القديم كان خاطئًا.
 *
 * <ul>
 *   <li>الكود: {@code payReport_2}</li>
 *   <li>الفئة: {@code main_container}</li>
 *   <li>الفئة الفرعية: {@code payroll_summary}</li>
 * </ul>
 */
@PayrollReport(
        code = "payReport_2",
        displayName = "تقرير إجمالي التكاليف الشهري",
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
        return "تقرير إجمالي التكاليف الشهري";
    }

    @Override
    public String getCategory() {
        return "main_container";
    }

    /**
     * يُعيد فئة التقارير الفرعية التابعة لهذا الحاوي.
     * يُستخدَم في {@link com.safwat.hr.report.payroll.strategies.ReportStrategyRegistry#getDisplayNamesByCategory(String)}
     * لجلب قائمة الفرعيات عند اختيار هذا التقرير.
     */
    @Override
    public String getMainReport() {
        return "payroll_summary";
    }

    /**
     * هذا تقرير حاوٍ — يُظهر ComboBox فرعي ولا يُرسِل مباشرةً.
     *
     * <p><b>إصلاح:</b> حُذف التعليق القديم الخاطئ "مفيش تقارير فرعية — مباشر"
     * الذي كان متعارضًا مع إعادة {@code true}.
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