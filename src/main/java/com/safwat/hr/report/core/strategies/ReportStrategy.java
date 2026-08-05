package com.safwat.hr.report.core.strategies;

import com.safwat.hr.report.controller.PayrollReportController;
import com.safwat.hr.report.core.ReportContext;
import com.safwat.hr.report.core.ValidationException;
import com.safwat.hr.report.core.ui.PayrollUIManager;
import com.safwat.hr.report.core.ui.UiConfiguration;
import com.safwat.hr.shared.PayrollRequest;
import org.springframework.stereotype.Component;

/**
 * واجهة أساسية لجميع استراتيجيات التقارير.
 *
 * <p>كل تقرير في النظام هو class منفصل يُطبِّق هذه الواجهة.
 * تُوفِّر الواجهة عقدًا موحَّدًا يشمل:
 * <ul>
 *   <li>هوية التقرير (code, displayName, category)</li>
 *   <li>إعدادات الواجهة ({@link #getUiConfig()})</li>
 *   <li>تخصيص الواجهة ({@link #onApply(PayrollReportController)})</li>
 *   <li>بناء الطلب ({@link #buildRequest(ReportContext)})</li>
 *   <li>التحقق من المدخلات ({@link #validate(ReportContext)})</li>
 * </ul>
 *
 * <p><b>تسلسل التنفيذ في الـ Controller:</b>
 * <pre>
 *   1. getUiConfig()                    ← يحدد الحقول الظاهرة وأزرار البحث
 *   2. onApply(controller)              ← تخصيص كامل للمكونات (نصوص، مستمعين، ...)
 *   3. validate(context)                ← للتحقق قبل الإرسال
 *   4. buildRequest(context)            ← لبناء الطلب بعد نجاح التحقق
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
@Component
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
     * إعدادات الواجهة الأساسية لهذا التقرير.
     *
     * <p>يُحدِّد:
     * <ul>
     *   <li>الحقول الظاهرة ({@code visibleFields})</li>
     *   <li>الحقول الإلزامية ({@code requiredFields})</li>
     *   <li>حقول البحث ({@code searchFields})</li>
     * </ul>
     *
     * <p>يُستدعى أولاً قبل {@link #onApply} — يُعِد الهيكل،
     * ثم {@link #onApply} يُخصِّص التفاصيل.
     *
     * @return كائن {@link UiConfiguration} يصف شكل النموذج
     */
    UiConfiguration getUiConfig();

    /**
     * تخصيص كامل لمكونات الواجهة بعد تطبيق {@link #getUiConfig()}.
     *
     * <p>يُستدعى من {@link PayrollUIManager#apply}
     * بعد إظهار الحقول وتفعيل أزرار البحث الأساسية.
     *
     * <p><b>ما يمكن تخصيصه هنا:</b>
     * <ul>
     *   <li>تغيير نصوص الـ Labels</li>
     *   <li>تغيير الـ placeholder للحقول</li>
     *   <li>ربط مستمعين خاصين على الأزرار أو الحقول</li>
     *   <li>تغيير مصادر بيانات أزرار البحث</li>
     *   <li>إخفاء/إظهار أزرار داخل الـ HBoxes الظاهرة</li>
     *   <li>أي منطق UI خاص بهذا التقرير</li>
     * </ul>
     *
     * <p><b>ملاحظة مهمة:</b> لا تحتاج لتنظيف ما تضيفه هنا يدوياً —
     * {@link PayrollUIManager#hideAll()}
     * يمسح جميع الـ handlers والتخصيصات قبل كل تطبيق جديد.
     *
     * <p><b>الافتراضي:</b> لا يفعل شيئًا — التقارير التي لا تحتاج تخصيصًا
     * لا تحتاج لـ override هذه الطريقة.
     *
     * <p><b>مثال:</b>
     * <pre>{@code
     * @Override
     * public void onApply(PayrollReportController controller) {
     *     // تغيير نص Label
     *     controller.getLbl_startDate().setText("من شهر");
     *
     *     // زر بحث بمصدر بيانات مختلف
     *     controller.getBtn_managementSearch().setOnAction(e ->
     *         controller.openSearchDialog("اختر قسم",
     *             DataSourceResolver.get("departments"),
     *             controller.getTxt_management())
     *     );
     *
     *     // مستمع على حقل إدخال
     *     controller.getTxt_payGroup().textProperty()
     *         .addListener((obs, old, val) -> doSomething(val));
     * }
     * }</pre>
     *
     * @param controller الـ Controller المالك لجميع مكونات الواجهة
     */
    default void onApply(PayrollReportController controller) {
        // افتراضي: لا تخصيص — التقارير البسيطة لا تحتاج override
    }

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

    default boolean requiresFiles() {
        return false;
    }
}