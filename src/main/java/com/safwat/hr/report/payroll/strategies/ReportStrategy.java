package com.safwat.hr.report.payroll.strategies;

import com.safwat.hr.report.payroll.ReportContext;
import com.safwat.hr.report.payroll.ValidationException;
import com.safwat.hr.report.payroll.ui.UiConfiguration;
import com.safwat.hr.service.payroll.dto.PayrollRequest;

/**
 * واجهة أساسية لجميع استراتيجيات التقارير.
 *
 * <p>كل تقرير في النظام هو class منفصل يُطبِّق هذه الواجهة.
 * تُوفِّر الواجهة عقدًا موحَّدًا يشمل:
 * <ul>
 *   <li>هوية التقرير (code, displayName, category)</li>
 *   <li>إعدادات الواجهة ({@link #getUiConfig()})</li>
 *   <li>بناء الطلب ({@link #buildRequest(ReportContext)})</li>
 *   <li>التحقق من المدخلات ({@link #validate(ReportContext)})</li>
 * </ul>
 *
 * <p><b>تسلسل التنفيذ في الـ Controller:</b>
 * <pre>
 *   1. getUiConfig()   ← لإعداد الواجهة عند اختيار التقرير
 *   2. validate()      ← للتحقق قبل الإرسال
 *   3. buildRequest()  ← لبناء الطلب بعد نجاح التحقق
 * </pre>
 *
 * <p><b>لإضافة تقرير جديد:</b>
 * <ol>
 *   <li>أنشئ class جديدًا يُطبِّق هذه الواجهة</li>
 *   <li>أضفه في {@link ReportRegistryFactory#create()}</li>
 *   <li>انتهى — لا تعديل في أي مكان آخر</li>
 * </ol>
 *
 * @see ReportStrategyRegistry
 * @see ReportRegistryFactory
 */
public interface ReportStrategy {

    /**
     * الكود الفريد للتقرير.
     * يُرسَل إلى الـ Backend في حقل {@code report}.
     * مثال: {@code "payrollYearly_1"}
     */
    String getCode();

    /**
     * الاسم العربي الذي يظهر في القوائم المنسدلة للمستخدم.
     * مثال: {@code "كل مجموعات التعيين"}
     */
    String getDisplayName();

    /**
     * فئة التقرير — تُحدِّد في أي قائمة رئيسية يندرج هذا التقرير.
     *
     * <p>الفئات المعتمدة حاليًا:
     * <ul>
     *   <li>{@code "main_container"} — تقرير رئيسي يحوي تقارير فرعية</li>
     *   <li>{@code "yearly_payroll"} — تقارير الصرفيات الشهرية (فرعية)</li>
     *   <li>{@code "payroll_summary"} — تقارير إجمالي التكاليف (فرعية)</li>
     * </ul>
     */
    String getCategory();

    /**
     * الفئة الأم لهذا التقرير الفرعي.
     *
     * <p>يُستخدَم في {@link ReportStrategyRegistry#getDisplayNamesByCategory(String)}
     * لجلب التقارير الفرعية التابعة لتقرير رئيسي معين.
     *
     * <p>للتقارير الرئيسية ({@code main_container}): أعِد نفس الفئة.
     */
    String getMainReport();

    /**
     * إعدادات الواجهة الخاصة بهذا التقرير.
     *
     * <p>يُستدعى عند اختيار التقرير لإعداد الحقول الظاهرة وإعدادات البحث.
     *
     * @return كائن {@link UiConfiguration} يصف شكل النموذج
     */
    UiConfiguration getUiConfig();

    /**
     * يبني كائن الطلب المُرسَل إلى الـ Backend.
     *
     * <p>يُستدعى بعد نجاح {@link #validate(ReportContext)}.
     *
     * @param context بيانات المستخدم المُدخَلة في النموذج
     * @return كائن {@link PayrollRequest} جاهز للإرسال
     * @throws UnsupportedOperationException للتقارير الحاوية التي لا تُرسِل مباشرةً
     */
    PayrollRequest buildRequest(ReportContext context);

    /**
     * يتحقق من صحة مدخلات المستخدم قبل الإرسال.
     *
     * <p>الإمبلمنتيشن الافتراضية لا تفعل شيئًا (تقارير بلا حقول إلزامية).
     * كل استراتيجية تتجاوز هذه الطريقة حسب حقولها الإلزامية.
     *
     * @param context بيانات المستخدم المُدخَلة
     * @throws ValidationException إذا كان هناك حقل إلزامي فارغ أو قيمة غير صحيحة
     */
    default void validate(ReportContext context) {
        // لا تحقق افتراضي — الاستراتيجيات التي لها حقول إلزامية تتجاوز هذه الطريقة
    }

    /**
     * هل هذا التقرير حاوٍ (container) يملك تقارير فرعية؟
     *
     * <p>التقارير الحاوية تُظهر ComboBox فرعي عند اختيارها،
     * ولا تُرسِل طلبات مباشرة (يُرسِل التقرير الفرعي).
     *
     * <p>الافتراضي: {@code false} (تقرير مباشر).
     *
     * @return {@code true} إذا كان هذا التقرير حاوٍ لتقارير فرعية
     */
    default boolean hasSubReports() {
        return false;
    }
}