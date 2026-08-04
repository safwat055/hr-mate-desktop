package com.safwat.hr.report.payroll.mainContainer;

import com.safwat.hr.report.core.PayrollReport;
import com.safwat.hr.report.core.ReportContext;
import com.safwat.hr.report.core.strategies.ReportRegistryFactory;
import com.safwat.hr.report.core.strategies.ReportStrategy;
import com.safwat.hr.report.core.strategies.ReportStrategyRegistry;
import com.safwat.hr.report.core.ui.UiConfiguration;
import com.safwat.hr.shared.PayrollRequest;

/**
 * تقرير رئيسي حاوٍ — "تقرير الصرفيات الشهري".
 *
 * <p>لا يُرسِل بيانات بنفسه، بل يعمل كحاوٍ (Container)
 * يُظهِر قائمة منسدلة فرعية بالتقارير التابعة له في الـ Controller.
 *
 * <p>التقارير الفرعية التابعة له (الفئة {@code yearly_payroll}):
 * <ul>
 *   <li>{@code payrollYearly_1} — كل مجموعات التعيين</li>
 *   <li>{@code payrollYearly_2} — مجموعات التعيين الرئيسية</li>
 *   <li>{@code payrollYearly_3} — مجموعات التعيين المنفصلة</li>
 *   <li>{@code payrollYearly_6} — كل مجموعات التعيين لإدارة محددة</li>
 *   <li>{@code payrollYearly_7} — مجموعات التعيين الرئيسية لإدارة محددة</li>
 *   <li>{@code payrollYearly_8} — مجموعات التعيين المنفصلة لإدارة محددة</li>
 *   <li>{@code payrollYearly_9} — مجموعة تعيين محددة</li>
 * </ul>
 *
 * <p><b>لإضافة تقرير فرعي جديد لهذا الحاوي:</b>
 * <ol>
 *   <li>أنشئ class في {@code strategies/sub/} بالفئة {@code "yearly_payroll"}</li>
 *   <li>سجِّله في {@link ReportRegistryFactory}</li>
 * </ol>
 *
 * <ul>
 *   <li>الكود: {@code payReport_1}</li>
 *   <li>الفئة: {@code main_container}</li>
 *   <li>الفئة الفرعية: {@code yearly_payroll}</li>
 * </ul>
 */
@PayrollReport(
        code = "payReport_1",
        displayName = "تقرير الصرفيات الشهري",
        category = "main_container",
        mainReport = "yearly_payroll"
)
public class MonthlyExpensesContainerStrategy implements ReportStrategy {

    @Override
    public String getCode() {
        return "payReport_1";
    }

    @Override
    public String getDisplayName() {
        return "تقرير الصرفيات الشهري";
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
        return "yearly_payroll";
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