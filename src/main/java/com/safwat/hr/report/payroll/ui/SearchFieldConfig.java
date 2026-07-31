package com.safwat.hr.report.payroll.ui;

/**
 * إعدادات نافذة البحث المرتبطة بحقل إدخال معين.
 *
 * <p>يربط هذا الكائن بين {@link UiField} (الحقل في النموذج)
 * وإعدادات نافذة البحث الخاصة به (العنوان + مصدر البيانات).
 *
 * <p>يُستخدَم في قائمة {@link UiConfiguration#getSearchFields()}
 * بدلاً من الثلاثة fields المنفصلة السابقة التي كانت تدعم بحثًا واحدًا فقط.
 *
 * <p><b>مثال — تقرير بحقلَي بحث:</b>
 * <pre>{@code
 * List.of(
 *     SearchFieldConfig.of(UiField.MANAGEMENT, "اختر إدارة",        "management"),
 *     SearchFieldConfig.of(UiField.PAY_GROUP,  "اختر مجموعة تعيين", "payGroup")
 * )
 * }</pre>
 *
 * <p><b>مثال — تقرير بدون بحث:</b>
 * <pre>{@code
 * List.of()   // أو null — PayrollUIManager يتعامل مع الحالتين
 * }</pre>
 *
 * @see UiConfiguration
 * @see UiField
 * @see DataSourceResolver
 */
public class SearchFieldConfig {

    /**
     * الحقل الذي يرتبط به هذا الإعداد.
     * يحدد أي TextField وأي زر بحث سيُفعَّلان في الواجهة.
     */
    private final UiField field;

    /**
     * عنوان نافذة البحث — يظهر في الـ title bar عند فتح النافذة.
     * مثال: {@code "اختر إدارة"}
     */
    private final String dialogTitle;

    /**
     * مفتاح مصدر البيانات في {@link DataSourceResolver}.
     * القيم المتاحة: {@code "management"}, {@code "payGroup"}, {@code "monthsYearly"}.
     */
    private final String dataSource;

    private SearchFieldConfig(UiField field, String dialogTitle, String dataSource) {
        this.field = field;
        this.dialogTitle = dialogTitle;
        this.dataSource = dataSource;
    }

    /**
     * Factory method لإنشاء {@code SearchFieldConfig} بصيغة مختصرة.
     *
     * <p><b>الاستخدام:</b>
     * <pre>{@code
     * SearchFieldConfig.of(UiField.MANAGEMENT, "اختر إدارة", "management")
     * }</pre>
     *
     * @param field       الحقل المرتبط بهذا البحث
     * @param dialogTitle عنوان نافذة البحث
     * @param dataSource  مفتاح المصدر في {@link DataSourceResolver}
     * @return كائن {@code SearchFieldConfig} جاهز
     */
    public static SearchFieldConfig of(UiField field, String dialogTitle, String dataSource) {
        return new SearchFieldConfig(field, dialogTitle, dataSource);
    }

    public UiField getField() {
        return field;
    }

    public String getDialogTitle() {
        return dialogTitle;
    }

    public String getDataSource() {
        return dataSource;
    }
}