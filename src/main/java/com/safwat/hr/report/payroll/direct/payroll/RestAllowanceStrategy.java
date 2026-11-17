package com.safwat.hr.report.payroll.direct.payroll;

import com.safwat.hr.controller.report.PayrollReportController;
import com.safwat.hr.report.core.ReportContext;
import com.safwat.hr.report.core.strategies.ReportStrategy;
import com.safwat.hr.report.core.ui.UiConfiguration;
import com.safwat.hr.report.core.ui.UiField;
import com.safwat.hr.shared.PayrollRequest;
import com.safwat.hr.shared.util.DateUtils;
import com.safwat.hr.ui.TextFieldSetupHelper;

import java.time.LocalDate;
import java.util.List;

/**
 * استراتيجية واجهة تقرير بدل الراحات لشهر معين.
 *
 * <p>الشاشة تعرض حقل الشهر ({@code H_START_DATE})، ويُستخدم كتاريخ بداية الشهر المطلوب
 * لحساب بدل الراحات لكل موظف في جدول "بدل راحات".</p>
 *
 * <p><b>الحقول المُرسلة إلى الخادم:</b></p>
 * <ul>
 *   <li>{@code report}: كود المهمة، وهو {@code REST_ALLOWANCE}.</li>
 *   <li>{@code startDate}: الشهر المطلوب بعد تحويله إلى {@link LocalDate}.</li>
 *   <li>{@code format}: صيغة الملف المختارة من الكومبو.</li>
 * </ul>
 */
public class RestAllowanceStrategy implements ReportStrategy {

    /** كود المهمة، ويجب أن يطابق {@code RestAllowanceReport.getCode()} في الخادم تماماً. */
    private static final String REPORT_CODE = "REST_ALLOWANCE";

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
        return "بدل الراحات";
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
     * <p>حقل الشهر ({@code H_START_DATE}) ظاهر وإلزامي.</p>
     *
     * @return إعدادات الواجهة للتقرير
     */
    @Override
    public UiConfiguration getUiConfig() {
        return UiConfiguration.builder()
                .requiredFields(List.of(UiField.H_START_DATE))
                .visibleFields(List.of(UiField.H_START_DATE))
                .build();
    }

    /**
     * يُنفَّذ عند تطبيق الاستراتيجية على الشاشة.
     *
     * <p>يفرّغ حقل الشهر، حتى لا تبقى قيمة من تقرير سابق عند التبديل.</p>
     *
     * @param controller متحكم شاشة التقارير
     */
    @Override
    public void onApply(PayrollReportController controller) {
        controller.setChoseMonth();
    }

    /**
     * يبني طلب التقرير من بيانات الشاشة.
     *
     * <p>يحوّل النص إلى تاريخ عبر {@link TextFieldSetupHelper#parseDateInput(String)}،
     * ويرفض الطلب إذا كان الحقل فارغًا أو التاريخ غير صالح.</p>
     *
     * @param context بيانات الشاشة الحالية
     * @return طلب التقرير الجاهز للإرسال
     * @throws IllegalStateException إذا كان الشهر فارغًا أو غير صالح
     */
    @Override
    public PayrollRequest buildRequest(ReportContext context) {



        if (context.getStartDate() == null) {
            throw new IllegalStateException("الشهر غير صالح، استخدم الصيغة yyyy MM");
        }

        return PayrollRequest.builder()
                .report(getCode())
                .startDate(DateUtils.getFirstDayOfMonth(context.getStartDate()))
                .format("EXCEL")
                .build();
    }
}