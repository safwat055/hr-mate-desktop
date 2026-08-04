package com.safwat.hr.report.core.strategies;

import java.util.*;
import java.util.stream.Collectors;

/**
 * سجل مركزي لجميع استراتيجيات التقارير المسجَّلة في النظام.
 *
 * <p>يُخزِّن الاستراتيجيات في ثلاث خرائط متوازية لدعم البحث بطرق مختلفة:
 * <ul>
 *   <li>بالكود ({@code byCode}) — للتواصل مع الـ Backend</li>
 *   <li>بالاسم ({@code byDisplayName}) — للبحث من القوائم المنسدلة</li>
 *   <li>بالفئة ({@code byCategory}) — لجلب التقارير الفرعية التابعة لتقرير رئيسي</li>
 * </ul>
 *
 * <p>يُستخدَم {@link LinkedHashMap} للحفاظ على ترتيب التسجيل في العرض.
 *
 * <p><b>دورة الحياة:</b>
 * يُنشأ الـ Registry مرة واحدة في {@link ReportRegistryFactory#create()}
 * ثم يُحقَن في الـ Controller.
 *
 * @see ReportRegistryFactory
 * @see ReportStrategy
 */
public class ReportStrategyRegistry {

    /**
     * فهرس الاستراتيجيات بكودها الفريد
     */
    private final Map<String, ReportStrategy> byCode = new LinkedHashMap<>();

    /**
     * فهرس الاستراتيجيات باسمها العربي
     */
    private final Map<String, ReportStrategy> byDisplayName = new LinkedHashMap<>();

    /**
     * فهرس الاستراتيجيات بفئتها
     */
    private final Map<String, List<ReportStrategy>> byCategory = new LinkedHashMap<>();

    /**
     * يُسجِّل استراتيجية جديدة في السجل.
     *
     * <p>يُضيف الاستراتيجية في الفهارس الثلاثة تلقائيًا.
     * يُستدعى حصريًا من {@link ReportRegistryFactory}.
     *
     * @param strategy الاستراتيجية المُراد تسجيلها
     */
    public void register(ReportStrategy strategy) {
        byCode.put(strategy.getCode(), strategy);
        byDisplayName.put(strategy.getDisplayName(), strategy);
        byCategory
                .computeIfAbsent(strategy.getCategory(), k -> new ArrayList<>())
                .add(strategy);
    }

    /**
     * يجلب استراتيجية بكودها.
     *
     * @param code كود التقرير (مثال: {@code "payrollYearly_1"})
     * @return الاستراتيجية المقابلة
     * @throws IllegalArgumentException إذا لم يُوجَد كود مطابق
     */
    public ReportStrategy getByCode(String code) {
        return Optional.ofNullable(byCode.get(code))
                .orElseThrow(() -> new IllegalArgumentException("كود تقرير غير معروف: " + code));
    }

    /**
     * يجلب استراتيجية باسمها العربي.
     *
     * <p>يُستخدَم عند تغيير اختيار المستخدم في ComboBox.
     *
     * @param displayName الاسم كما يظهر في القائمة المنسدلة
     * @return الاستراتيجية المقابلة
     * @throws IllegalArgumentException إذا لم يُوجَد اسم مطابق
     */
    public ReportStrategy getByDisplayName(String displayName) {
        return Optional.ofNullable(byDisplayName.get(displayName))
                .orElseThrow(() -> new IllegalArgumentException("تقرير غير معروف: " + displayName));
    }

    /**
     * يجلب أسماء جميع التقارير المنتمية لفئة معينة.
     *
     * <p>يُستخدَم لملء ComboBox الفرعي عند اختيار تقرير حاوٍ.
     *
     * @param category الفئة المطلوبة (مثال: {@code "yearly_payroll"})
     * @return قائمة بالأسماء العربية بترتيب التسجيل، أو قائمة فارغة
     */
    public List<String> getDisplayNamesByCategory(String category) {
        return byCategory.getOrDefault(category, List.of()).stream()
                .map(ReportStrategy::getDisplayName)
                .collect(Collectors.toList());
    }

    /**
     * يجلب أسماء جميع الاستراتيجيات المسجَّلة.
     *
     * @return قائمة بجميع الأسماء بترتيب التسجيل
     */
    public List<String> getAllDisplayNames() {
        return List.copyOf(byDisplayName.keySet());
    }

    /**
     * يجلب جميع الاستراتيجيات المسجَّلة.
     *
     * <p>يُستخدَم في الـ Controller لتصفية التقارير الرئيسية.
     *
     * @return قائمة غير قابلة للتعديل بجميع الاستراتيجيات
     */
    public List<ReportStrategy> getAll() {
        return List.copyOf(byCode.values());
    }
}