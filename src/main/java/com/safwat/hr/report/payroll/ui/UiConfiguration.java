package com.safwat.hr.report.payroll.ui;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.List;

/**
 * إعدادات واجهة المستخدم الخاصة بكل استراتيجية تقرير.
 *
 * <p>تُحدِّد هذه الكلاس أي الحقول تظهر، وأيها إلزامي،
 * وما حقول البحث المرتبطة بالنموذج.
 *
 * <p>يُنشئ {@link PayrollUIManager} الواجهة بناءً على هذه الإعدادات
 * عند اختيار المستخدم لاستراتيجية معينة.
 *
 * <hr>
 *
 * <p><b>تطور التصميم:</b>
 * <ul>
 *   <li>الإصدار القديم: ثلاثة fields منفصلة ({@code needsSearchDialog},
 *       {@code searchDialogTitle}, {@code searchDataSource}) تدعم بحثًا واحدًا فقط.</li>
 *   <li>الإصدار الحالي: قائمة {@link #searchFields} من {@link SearchFieldConfig}
 *       تدعم صفرًا أو أكثر من حقول البحث في نفس النموذج.</li>
 * </ul>
 *
 * <hr>
 *
 * <p><b>مثال — تقرير بحقل واحد بدون بحث:</b>
 * <pre>{@code
 * UiConfiguration.builder()
 *         .title("تقرير كل مجموعات التعيين")
 *         .visibleField(UiField.START_DATE)
 *         .requiredField(UiField.START_DATE)
 *         .build();
 * }</pre>
 *
 * <p><b>مثال — تقرير بحقل بحث واحد:</b>
 * <pre>{@code
 * UiConfiguration.builder()
 *         .title("تقرير إدارة محددة")
 *         .visibleField(UiField.START_DATE)
 *         .visibleField(UiField.MANAGEMENT)
 *         .requiredField(UiField.START_DATE)
 *         .requiredField(UiField.MANAGEMENT)
 *         .searchField(SearchFieldConfig.of(UiField.MANAGEMENT, "اختر إدارة", "management"))
 *         .build();
 * }</pre>
 *
 * <p><b>مثال — تقرير بحقلَي بحث:</b>
 * <pre>{@code
 * UiConfiguration.builder()
 *         .title("تقرير إدارة ومجموعة تعيين")
 *         .visibleField(UiField.START_DATE)
 *         .visibleField(UiField.MANAGEMENT)
 *         .visibleField(UiField.PAY_GROUP)
 *         .requiredField(UiField.START_DATE)
 *         .requiredField(UiField.MANAGEMENT)
 *         .requiredField(UiField.PAY_GROUP)
 *         .searchField(SearchFieldConfig.of(UiField.MANAGEMENT, "اختر إدارة",        "management"))
 *         .searchField(SearchFieldConfig.of(UiField.PAY_GROUP,  "اختر مجموعة تعيين", "payGroup"))
 *         .build();
 * }</pre>
 */
@Getter
@Builder
public class UiConfiguration {

    /**
     * عنوان النموذج — يظهر في Label أعلى الحقول.
     */
    private final String title;

    /**
     * قائمة الحقول التي تُعرَض للمستخدم.
     * الحقول غير المذكورة تُخفى تلقائيًا بواسطة {@link PayrollUIManager}.
     *
     * <p>يُستخدَم {@code @Singular} ليسمح بإضافة عناصر واحدًا واحدًا
     * في الـ Builder عبر {@code .visibleField(UiField.X)}.
     */
    @Singular("visibleField")
    private final List<UiField> visibleFields;

    /**
     * قائمة الحقول الإلزامية.
     * مرجع لمنطق التحقق في {@code validate()} داخل الاستراتيجية.
     * مستقبلاً يمكن استخدامها لإضافة علامة (*) بجانب الحقل في الواجهة.
     *
     * <p>يُستخدَم {@code @Singular} ليسمح بإضافة عناصر واحدًا واحدًا
     * في الـ Builder عبر {@code .requiredField(UiField.X)}.
     */
    @Singular("requiredField")
    private final List<UiField> requiredFields;

    /**
     * قائمة إعدادات حقول البحث في النموذج.
     *
     * <p>كل عنصر ({@link SearchFieldConfig}) يربط حقل إدخال بعيّنه
     * ({@link UiField}) بنافذة بحث خاصة به (عنوان + مصدر بيانات).
     *
     * <p>قائمة فارغة تعني: لا يوجد بحث في هذا النموذج.
     *
     * <p>يُستخدَم {@code @Singular} ليسمح بإضافة عناصر واحدًا واحدًا
     * في الـ Builder عبر {@code .searchField(SearchFieldConfig.of(...))}.
     */
    @Singular("searchField")
    private final List<SearchFieldConfig> searchFields;

    /**
     * يبحث عن إعداد البحث المرتبط بحقل معين.
     *
     * <p>يُستخدَم في {@link PayrollUIManager} لتفعيل زر البحث
     * وربط الـ TextField بالمصدر الصحيح.
     *
     * @param field الحقل المطلوب
     * @return {@link SearchFieldConfig} المقابل، أو {@code null} لو الحقل بدون بحث
     */
    public SearchFieldConfig getSearchConfigFor(UiField field) {
        if (searchFields == null) return null;
        return searchFields.stream()
                .filter(cfg -> cfg.getField() == field)
                .findFirst()
                .orElse(null);
    }

    /**
     * هل هذا الحقل مرتبط بنافذة بحث؟
     *
     * <p>مختصر للتحقق دون الحاجة لفحص {@code null}.
     *
     * @param field الحقل المطلوب التحقق منه
     * @return {@code true} إذا كان للحقل إعداد بحث مرتبط به
     */
    public boolean hasSearchFor(UiField field) {
        return getSearchConfigFor(field) != null;
    }
}