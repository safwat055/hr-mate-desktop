package com.safwat.hr.report.payroll.direct.scale;

import com.safwat.hr.controller.report.PayrollReportController;
import com.safwat.hr.report.core.ReportContext;
import com.safwat.hr.report.core.strategies.ReportStrategy;
import com.safwat.hr.report.core.ui.UiConfiguration;
import com.safwat.hr.report.core.ui.UiField;
import com.safwat.hr.shared.PayrollRequest;
import com.safwat.hr.ui.TextFieldSetupHelper;

import java.time.LocalDate;
import java.util.List;

/**
 * استراتيجية واجهة تقرير قيمة الحزمة الاجتماعية في تاريخ معين.
 *
 * <p>الشاشة تعرض حقلًا نصيًا واحدًا للتاريخ بصيغة {@code yyyy-MM-dd}، وحقل الصيغة موجود دائمًا في الكومبو.
 * يُرسَل التاريخ إلى الخادم ليجمع لكل موظف قيم الحزمة الاجتماعية حتى ذلك التاريخ،
 * والواجهة لا تحتاج معرفة منطق الجمع أو الاستبعاد.</p>
 *
 * <p><b>الحقول المُرسلة إلى الخادم:</b></p>
 * <ul>
 *   <li>{@code report}: كود المهمة، وهو {@code SOCIAL_PACKAGE_AT_DATE}.</li>
 *   <li>{@code startDate}: التاريخ المطلوب بعد تحويله إلى {@link LocalDate}.</li>
 *   <li>{@code format}: صيغة الملف المختارة من الكومبو.</li>
 * </ul>
 */
public class SocialPackageAtDateStrategy implements ReportStrategy {

    /** كود المهمة، ويجب أن يطابق الكود المعرّف في الخادم تماماً. */
    private static final String REPORT_CODE = "SOCIAL_PACKAGE_AT_DATE";

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
        return "قيمة الحزمة الاجتماعية في تاريخ";
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
     * <p>الحاوي {@code H_4} يحتوي على حقل التاريخ النصي وعنوانه، وهو الحقل الوحيد الظاهر والإلزامي.</p>
     *
     * @return إعدادات الواجهة للتقرير
     */
    @Override
    public UiConfiguration getUiConfig() {
        return UiConfiguration.builder()
                .requiredFields(List.of(UiField.H_4))
                .visibleFields(List.of(UiField.H_4))
                .build();
    }

    /**
     * يُنفَّذ عند تطبيق الاستراتيجية على الشاشة.
     *
     * <p>يفرّغ حقل التاريخ، حتى لا تبقى قيمة من تقرير سابق عند التبديل.</p>
     *
     * @param controller متحكم شاشة التقارير
     */
    @Override
    public void onApply(PayrollReportController controller) {
        controller.getTxt_targetDate().setText("");
    }

    /**
     * يبني طلب التقرير من بيانات الشاشة.
     *
     * <p>يحوّل النص إلى تاريخ عبر {@link TextFieldSetupHelper#parseDateInput(String)}،
     * ويرفض الطلب إذا كان الحقل فارغًا أو التاريخ غير صالح.</p>
     *
     * @param context بيانات الشاشة الحالية
     * @return طلب التقرير الجاهز للإرسال
     * @throws IllegalStateException إذا كان التاريخ فارغًا أو غير صالح
     */
    @Override
    public PayrollRequest buildRequest(ReportContext context) {
        String rawDate = context.getTargetDate();

        if (rawDate == null || rawDate.isBlank()) {
            throw new IllegalStateException("يجب إدخال التاريخ المطلوب");
        }

        LocalDate targetDate = TextFieldSetupHelper.parseDateInput(rawDate);
        if (targetDate == null) {
            throw new IllegalStateException("التاريخ غير صالح، استخدم الصيغة yyyy-MM-dd");
        }

        return PayrollRequest.builder()
                .report(getCode())
                .startDate(targetDate)
                .format(context.getFormat())
                .build();
    }
}