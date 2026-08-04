package com.safwat.hr.report.core;

import com.safwat.hr.report.core.strategies.ReportRegistryFactory;
import com.safwat.hr.report.core.strategies.ReportStrategy;
import com.safwat.hr.report.core.strategies.ReportStrategyRegistry;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation تعريفية تُوسِم كل class يُمثِّل استراتيجية تقرير.
 *
 * <p>تُوفِّر metadata سريعة عن التقرير مباشرةً على الـ class
 * دون الحاجة لفتح الكود الداخلي.
 *
 * <p><b>الحالة الحالية:</b> تعريفية فقط — القيم الفعلية تُقرأ
 * من الطرق ({@code getCode()}, {@code getDisplayName()}, ...)
 * عبر {@link ReportStrategyRegistry}.
 *
 * <p><b>التطور المستقبلي المقترح:</b> يمكن استخدامها مع Reflection
 * لتسجيل الاستراتيجيات تلقائيًا في الـ Registry دون الحاجة
 * لإضافة سطر يدوي في {@link ReportRegistryFactory}.
 * مثال:
 * <pre>{@code
 * // مسح الباكيج واستخراج كل class عليه @PayrollReport
 * Reflections reflections = new Reflections("com.safwat.hr.report.payroll.strategies");
 * Set<Class<?>> strategies = reflections.getTypesAnnotatedWith(PayrollReport.class);
 * strategies.forEach(cls -> registry.register((ReportStrategy) cls.getDeclaredConstructor().newInstance()));
 * }</pre>
 *
 * <p><b>ملاحظة:</b> لو قررت تفعيل هذا النهج، احذف الـ Factory
 * واجعل الـ Registry يقرأ البيانات من الـ Annotation مباشرةً
 * بدلاً من استدعاء الطرق.
 *
 * @see ReportStrategy
 * @see ReportRegistryFactory
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PayrollReport {

    /**
     * الكود الفريد للتقرير — يُرسَل للـ Backend في حقل {@code report}.
     * مثال: {@code "payrollYearly_1"}
     */
    String code();

    /**
     * الاسم العربي الظاهر في القوائم المنسدلة.
     * مثال: {@code "كل مجموعات التعيين"}
     */
    String displayName();

    /**
     * فئة التقرير.
     * القيم المعتمدة: {@code "main_container"}, {@code "yearly_payroll"}, {@code "payroll_summary"}.
     */
    String category();

    /**
     * الفئة الأم — لربط التقرير الفرعي بتقريره الرئيسي.
     * للتقارير الرئيسية: يُساوي {@code category}.
     */
    String mainReport();
}