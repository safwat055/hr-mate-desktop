package com.safwat.hr.report.payroll.ui;

import com.safwat.hr.report.payroll.DataSourceResolver;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * إعدادات واجهة المستخدم الخاصة بكل استراتيجية تقرير.
 *
 * <p>تُحدِّد هذه الكلاس أي الحقول تظهر، وأيها إلزامي،
 * وهل يحتاج التقرير إلى نافذة بحث، ومن أين تأتي بياناتها.
 *
 * <p>يُنشئ {@link PayrollUIManager} الواجهة بناءً على هذه الإعدادات،
 * وذلك عند اختيار المستخدم لاستراتيجية معينة.
 *
 * <p><b>دمج:</b> {@code requiredFields} الآن من نوع {@link UiField}
 * بدلاً من {@code RequiredField} المنفصل السابق الذي كان مكررًا.
 *
 * <p><b>مثال الاستخدام في الاستراتيجية:</b>
 * <pre>{@code
 * return UiConfiguration.builder()
 *         .title("تقرير إدارة محددة")
 *         .visibleFields(List.of(UiField.START_DATE, UiField.MANAGEMENT))
 *         .requiredFields(List.of(UiField.START_DATE, UiField.MANAGEMENT))
 *         .needsSearchDialog(true)
 *         .searchDialogTitle("اختر إدارة")
 *         .searchDataSource("management")
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
     */
    private final List<UiField> visibleFields;

    /**
     * قائمة الحقول الإلزامية.
     * تُستخدَم حاليًا كمرجع لمنطق التحقق في {@code validate()} داخل الاستراتيجية.
     * مستقبلًا يمكن استخدامها لإضافة علامة (*) بجانب الحقل في الواجهة.
     */
    private final List<UiField> requiredFields;

    /**
     * هل يحتاج هذا التقرير إلى نافذة بحث لاختيار قيمة؟
     * (مثال: اختيار إدارة أو مجموعة تعيين من قائمة)
     */
    private final boolean needsSearchDialog;

    /**
     * عنوان نافذة البحث (يظهر في الـ title bar).
     * يُستخدَم فقط عندما {@code needsSearchDialog = true}.
     */
    private final String searchDialogTitle;

    /**
     * مفتاح مصدر البيانات في {@link DataSourceResolver}.
     * القيم المعتمدة: {@code "management"}, {@code "payGroup"}, {@code "monthsYearly"}.
     * يُستخدَم فقط عندما {@code needsSearchDialog = true}.
     */
    private final String searchDataSource;
}