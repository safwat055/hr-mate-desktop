package com.safwat.hr.report.payroll.direct.scale;

import com.safwat.hr.controller.report.PayrollReportController;
import com.safwat.hr.report.core.ReportContext;
import com.safwat.hr.report.core.strategies.ReportStrategy;
import com.safwat.hr.report.core.ui.UiConfiguration;
import com.safwat.hr.shared.PayrollRequest;

import java.util.List;

/**
 * استراتيجية واجهة تقرير أساسي 30-06 للمكافآت والبدلات.
 *
 * <p><b>لا تعرض الشاشة أي حقل إدخال</b>، لأن التقرير لا يحتاج تاريخًا؛ فالمفتاح يُؤخذ
 * من {@code basic30Date} لكل موظف أو من الافتراضي {@code 2015-06-30}، والصيغة تُختار
 * من الكومبو العام عبر {@link ReportContext#getFormat()}.</p>
 *
 * <p><b>الحقول المُرسلة إلى الخادم:</b></p>
 * <ul>
 *   <li>{@code report}: كود المهمة، وهو {@code BASIC_30_FOR_BONUSES}.</li>
 *   <li>{@code format}: صيغة الملف المختارة من الكومبو.</li>
 * </ul>
 */
public class Basic30ForBonusesStrategy implements ReportStrategy {

    /** كود المهمة، ويجب أن يطابق الكود المعرّف في الخادم تماماً. */
    private static final String REPORT_CODE = "BASIC_30_FOR_BONUSES";

    /**
     * يعيد الكود الفريد للتقرير داخل الواجهة.
     *
     * @return كود التقرير
     */
    @Override
    public String getCode() {
        return REPORT_CODE;
    }

    /**
     * يعيد الاسم الذي يظهر للمستخدم في قائمة التقارير.
     *
     * @return اسم التقرير بالعربية
     */
    @Override
    public String getDisplayName() {
        return "أساسي 30-06 للمكافآت والبدلات";
    }

    /**
     * يعيد فئة التقرير التي ينتمي إليها.
     *
     * @return اسم الفئة
     */
    @Override
    public String getCategory() {
        return "main_direct";
    }

    /**
     * يعيد التقرير الرئيسي الذي يتبع له هذا التقرير.
     *
     * @return اسم التقرير الرئيسي
     */
    @Override
    public String getMainReport() {
        return "main_direct";
    }

    /**
     * يحدد الحقول الظاهرة والإلزامية في شاشة التقرير.
     *
     * <p>لا توجد حقول مطلوبة ولا ظاهرة؛ فالتقرير يعمل بالكامل من بيانات الموظف
     * ومن اختيار صيغة الملف المتاح دائمًا في الكومبو العام.</p>
     *
     * @return إعدادات الواجهة للتقرير
     */
    @Override
    public UiConfiguration getUiConfig() {
        return UiConfiguration.builder()
                .requiredFields(List.of())
                .visibleFields(List.of())
                .build();
    }

    /**
     * يُنفَّذ عند تطبيق الاستراتيجية على الشاشة.
     *
     * <p>لا يوجد حقل تاريخ لتفريغه، فالدالة لا تُجري شيئًا.</p>
     *
     * @param controller متحكم شاشة التقارير
     */
    @Override
    public void onApply(PayrollReportController controller) {
        // لا حقول إدخال لتفريغها
    }

    /**
     * يبني طلب التقرير من بيانات الشاشة.
     *
     * <p>لا يقرأ أي تاريخ؛ يكتفي بكود المهمة والصيغة.</p>
     *
     * @param context بيانات الشاشة الحالية
     * @return طلب التقرير الجاهز للإرسال
     */
    @Override
    public PayrollRequest buildRequest(ReportContext context) {
        return PayrollRequest.builder()
                .report(getCode())
                .format(context.getFormat())
                .build();
    }
}