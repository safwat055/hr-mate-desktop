package com.safwat.hr.report.public_;

import com.safwat.hr.report.core.PayrollReport;
import com.safwat.hr.report.core.ReportContext;
import com.safwat.hr.report.core.strategies.ReportStrategy;
import com.safwat.hr.report.core.strategies.ReportStrategyRegistry;
import com.safwat.hr.report.core.ui.UiConfiguration;
import com.safwat.hr.shared.PayrollRequest;

@PayrollReport(
        code = "FULL_BACKUP_REPORT",
        displayName = "انشاء وتحميل نسخة احتياطية",
        category = "main_direct",
        mainReport = "main_direct"
)
public class FullBackupReport implements ReportStrategy {
    /**
     * الكود الفريد للتقرير.
     * يُرسَل إلى الـ Backend في حقل {@code report}.
     * مثال: {@code "payrollYearly_1"}
     */
    @Override
    public String getCode() {
        return "FULL_BACKUP_REPORT";

    }

    /**
     * الاسم العربي الذي يظهر في القوائم المنسدلة للمستخدم.
     * مثال: {@code "كل مجموعات التعيين"}
     */
    @Override
    public String getDisplayName() {
        return "انشاء وتحميل نسخة احتياطية";
    }

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
    @Override
    public String getCategory() {
        return "main_direct";
    }

    /**
     * الفئة الأم لهذا التقرير الفرعي.
     *
     * <p>يُستخدَم في {@link ReportStrategyRegistry#getDisplayNamesByCategory(String)}
     * لجلب التقارير الفرعية التابعة لتقرير رئيسي معين.
     *
     * <p>للتقارير الرئيسية ({@code main_container}): أعِد نفس الفئة.
     */
    @Override
    public String getMainReport() {
        return "main_direct";
    }

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
    @Override
    public UiConfiguration getUiConfig() {
         return UiConfiguration.builder().build();
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
    @Override
    public PayrollRequest buildRequest(ReportContext context) {
        return PayrollRequest.builder()
                .reportName(context.getReportName())
                .report(getCode())
                .build();
    }
}
