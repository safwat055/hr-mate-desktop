package com.safwat.hr.report.core;

import com.safwat.hr.report.core.strategies.ReportStrategy;

/**
 * استثناء مخصص لأخطاء التحقق من مدخلات نموذج التقارير.
 *
 * <p>يُرمى من داخل
 * {@link ReportStrategy#validate(ReportContext)}
 * عند فشل التحقق (حقل إلزامي فارغ، قيمة غير صحيحة، ...إلخ).
 *
 * <p>يُصطاد في الـ Controller لعرض رسالة الخطأ للمستخدم:
 * <pre>{@code
 * try {
 *     strategy.validate(context);
 * } catch (ValidationException e) {
 *     SAFNotification.warning(e.getMessage());
 *     return;
 * }
 * }</pre>
 *
 * <p><b>قواعد رسائل الخطأ:</b>
 * <ul>
 *   <li>تُكتَب بالعربية وتُخاطِب المستخدم مباشرةً.</li>
 *   <li>تُحدِّد الحقل الفارغ بالاسم. مثال: {@code "يجب اختيار الشهر أولاً!"}</li>
 *   <li>لا تكشف تفاصيل تقنية.</li>
 * </ul>
 *
 * @see ReportStrategy#validate(ReportContext)
 */
public class ValidationException extends RuntimeException {

    /**
     * @param message رسالة الخطأ العربية التي ستُعرَض للمستخدم
     */
    public ValidationException(String message) {
        super(message);
    }
}