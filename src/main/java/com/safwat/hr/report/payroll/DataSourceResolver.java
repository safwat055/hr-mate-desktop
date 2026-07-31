package com.safwat.hr.report.payroll;

import com.safwat.hr.service.payroll.PayrollService;

import java.util.List;

/**
 * مُحلِّل مصادر بيانات القوائم المنسدلة في نماذج التقارير.
 *
 * <p>يُوفِّر نقطة وصول مركزية لجلب بيانات الإدارات ومجموعات التعيين والشهور،
 * بدلاً من تشتيتها في كل استراتيجية على حدة.
 *
 * <p><b>إصلاح:</b> كانت البيانات تُحمَّل مرة واحدة عند تهيئة الـ class
 * ({@code static final Map}). هذا يعني أن أي إدارة أو مجموعة تعيين تُضاف
 * لاحقًا لن تظهر دون إعادة تشغيل التطبيق.
 * الآن تُجلَب البيانات عند كل استدعاء (Lazy / Live).
 *
 * <p><b>مفاتيح المصادر المتاحة:</b>
 * <ul>
 *   <li>{@code "payGroup"}     — قائمة مجموعات التعيين</li>
 *   <li>{@code "management"}   — قائمة الإدارات</li>
 *   <li>{@code "monthsYearly"} — قائمة الشهور السنوية</li>
 * </ul>
 *
 * <p><b>إضافة مصدر جديد:</b> أضف {@code case} جديدًا في {@link #get(String)}.
 */
public class DataSourceResolver {

    private static final PayrollService payrollService = PayrollService.getInstance();

    /**
     * يجلب قائمة البيانات المرتبطة بالمفتاح المحدد.
     *
     * <p>البيانات تُجلَب في اللحظة ذاتها من الـ Service لضمان حداثتها.
     *
     * @param key مفتاح المصدر المطلوب
     * @return قائمة القيم، أو قائمة فارغة إذا كان المفتاح غير معروف
     */
    public static List<String> get(String key) {
        return switch (key) {
            case "payGroup" -> payrollService.getPayGroup();
            case "management" -> payrollService.getManagement();
            case "monthsYearly" -> payrollService.getAllMonthsYearly();
            //case "employee" -> payrollService.searchInEmployees();
            default -> List.of();
        };
    }
}