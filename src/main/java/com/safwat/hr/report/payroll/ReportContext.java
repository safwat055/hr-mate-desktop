package com.safwat.hr.report.payroll;

import com.safwat.hr.shared.PayrollRequest;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;
import java.util.List;

/**
 * نموذج بيانات طلب التقرير.
 *
 * <p>يجمع كل ما يُدخله المستخدم في نموذج التقارير في كائن واحد،
 * يُمرَّر إلى:
 * <ul>
 *   <li>{@link com.safwat.hr.report.payroll.strategies.ReportStrategy#validate(ReportContext)}
 *       — للتحقق من اكتمال البيانات.</li>
 *   <li>{@link com.safwat.hr.report.payroll.strategies.ReportStrategy#buildRequest(ReportContext)}
 *       — لبناء {@link PayrollRequest} الجاهز للإرسال.</li>
 * </ul>
 *
 * <p>يُنشأ في الـ Controller ويُملأ من حقول الـ UI قبيل الإرسال.
 *
 * <p><b>ملاحظة على الحقول المحجوزة:</b>
 * {@code customGroup}, {@code description}, {@code note}, {@code searchValue}
 * لم تُستخدَم في الاستراتيجيات الحالية، لكنها محجوزة للتقارير القادمة.
 * لا تحذفها.
 */
@Getter
@Setter
@Builder
public class ReportContext {

    /**
     * تاريخ البداية بصيغة {@code yyyy-MM} أو {@code yyyy-MM-dd} حسب التقرير.
     * يُحوَّل إلى أول يوم في الشهر عبر {@code DateUtils.getFirstDayOfMonth()}
     * عند بناء الطلب.
     */
    private String startDate;

    /**
     * تاريخ النهاية — يُستخدَم في التقارير ذات النطاق الزمني.
     */
    private String endDate;

    /**
     * اسم الإدارة المختارة من نافذة البحث.
     * مثال: {@code "إدارة الموارد البشرية"}
     */
    private String management;

    /**
     * كود مجموعة التعيين المختارة.
     * مثال: {@code "PG001"}
     */
    private String payGroup;

    /**
     * الرقم القومي للموظف — للبحث عن موظف بعينه.
     */
    private String nationalId;

    /**
     * صيغة التقرير المطلوبة.
     * القيم المتاحة: {@code "PDF"}, {@code "EXCEL"}.
     */
    private String format;

    /**
     * اسم التقرير كما يظهر في الـ UI — يُرسَل للـ Backend كـ metadata.
     */
    private String reportName;

    /**
     * اسم المستخدم الحالي — يُضاف تلقائيًا عبر {@code ApiClient.getUserName()}.
     */
    private String user;

    // ────────────────────────────────────────────────────
    //  حقول محجوزة للتوسع المستقبلي
    // ────────────────────────────────────────────────────

    /**
     * مجموعة مخصصة أو عنصر — محجوز للتقارير القادمة
     */
    private String customGroup;

    /**
     * وصف إضافي — محجوز للتقارير القادمة
     */
    private String description;

    /**
     * ملاحظة — محجوزة للتقارير القادمة
     */
    private String note;

    /**
     * قيمة بحث حر — محجوزة للتقارير القادمة
     */
    private String searchValue;

    @Builder.Default
    private List<Path> files = java.util.Collections.emptyList();
}