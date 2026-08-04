package com.safwat.hr.report.core.strategies;

import com.safwat.hr.report.controller.PayrollReportController;
import com.safwat.hr.report.payroll.direct.EmployeePayments;
import com.safwat.hr.report.payroll.direct.NetDifferenceBetweenTowMonths;
import com.safwat.hr.report.payroll.direct.NetForTowMonths;
import com.safwat.hr.report.payroll.direct.PayrollIndex;
import com.safwat.hr.report.payroll.mainContainer.*;
import com.safwat.hr.report.payroll.sub.PayrollHistory.element.ElementEmployee;
import com.safwat.hr.report.payroll.sub.PayrollHistory.element.ElementEmployees;
import com.safwat.hr.report.payroll.sub.PayrollHistory.element.ElementManagement;
import com.safwat.hr.report.payroll.sub.PayrollHistory.element.ElementPayGroup;
import com.safwat.hr.report.payroll.sub.PayrollHistory.reviewReport.ReviewReportAll;
import com.safwat.hr.report.payroll.sub.PayrollHistory.reviewReport.ReviewReportEmployee;
import com.safwat.hr.report.payroll.sub.PayrollHistory.reviewReport.ReviewReportManagement;
import com.safwat.hr.report.payroll.sub.PayrollHistory.reviewReport.ReviewReportPayGroup;
import com.safwat.hr.report.payroll.sub.UploadReviewReport;
import com.safwat.hr.report.payroll.sub.changeCard.card.PayrollChangeCardAll;
import com.safwat.hr.report.payroll.sub.changeCard.card.PayrollChangeCardEmployee;
import com.safwat.hr.report.payroll.sub.changeCard.card.PayrollChangeCardManagement;
import com.safwat.hr.report.payroll.sub.changeCard.card.PayrollChangeCardPayGroup;
import com.safwat.hr.report.payroll.sub.changeCard.month.PayrollChangeMonthAll;
import com.safwat.hr.report.payroll.sub.changeCard.month.PayrollChangeMonthManagement;
import com.safwat.hr.report.payroll.sub.changeCard.month.PayrollChangeMonthPayGroup;
import com.safwat.hr.report.payroll.sub.payrollSummary.*;
import com.safwat.hr.report.payroll.sub.payrollYearly.*;

/**
 * مصنع إنشاء السجل الرئيسي لاستراتيجيات التقارير.
 *
 * <p>هذا الملف هو <b>النقطة الوحيدة</b> التي يُسجَّل فيها أي تقرير جديد.
 * لا حاجة لتعديل أي ملف آخر لإضافة تقرير.
 *
 * <hr>
 *
 * <h2>ترتيب التسجيل مهم</h2>
 * <p>ترتيب التسجيل هو ترتيب الظهور في القوائم المنسدلة للمستخدم.
 * سجِّل التقارير الرئيسية أولاً، ثم الفرعية.
 *
 * <hr>
 *
 * <h2>لإضافة تقرير جديد</h2>
 * <ol>
 *   <li>أنشئ class جديدًا في المجلد المناسب ({@code sub/} أو {@code mainContainer/})</li>
 *   <li>أضف سطر {@code registry.register(new MyNewStrategy());} هنا</li>
 *   <li>انتهى ✓</li>
 * </ol>
 *
 * <hr>
 *
 * <h2>هيكل الفئات المعتمدة</h2>
 * <pre>
 * main_container               ← تقارير رئيسية (تظهر في ComboBox الأول)
 * │
 * ├── yearly_payroll           ← فرعي لـ "تقرير الصرفيات الشهري"
 * │   ├── payrollYearly_1  كل مجموعات التعيين
 * │   ├── payrollYearly_2  مجموعات التعيين الرئيسية
 * │   ├── payrollYearly_3  مجموعات التعيين المنفصلة
 * │   ├── payrollYearly_6  إدارة محددة
 * │   ├── payrollYearly_7  مجموعات التعيين الرئيسية لإدارة محددة
 * │   ├── payrollYearly_8  مجموعات التعيين المنفصلة لإدارة محددة
 * │   └── payrollYearly_9  مجموعة تعيين محددة
 * │
 * └── payroll_summary          ← فرعي لـ "تقرير إجمالي التكاليف الشهري"
 *     └── (قيد الإضافة)
 * </pre>
 */
public class ReportRegistryFactory {

    /**
     * يُنشئ ويُعيد سجلاً جاهزًا بجميع الاستراتيجيات المسجَّلة.
     *
     * <p>يُستدعى مرة واحدة في بناء {@link PayrollReportController}.
     *
     * @return {@link ReportStrategyRegistry} مكتمل وجاهز للاستخدام
     */
    public static ReportStrategyRegistry create() {
        ReportStrategyRegistry registry = new ReportStrategyRegistry();

        // ══════════════════════════════════════════
        //  التقارير الرئيسية (main_container)
        // ══════════════════════════════════════════
        registry.register(new MonthlyExpensesContainerStrategy()); // تقرير الصرفيات الشهري   — حاوٍ
        registry.register(new PayrollCostSummaryStrategy());        // إجمالي التكاليف الشهري  — حاوٍ
        registry.register(new PayrollChangeCard());        // إجمالي التكاليف الشهري  — حاوٍ
        registry.register(new PayrollElement()); // تقرير عنصر معين  - حاو
        registry.register(new PayrollChangeMonth()); //  تقرير اجر الاشتراك تقرير  حاو
        registry.register(new ReviewReport()); // تقرير المراجعة للصرفيات الرئيسية تقرير   حاو
        registry.register(new UploadPayrollReport()); // تحميل التقارير المنظومة  - حاو
        // main_direct
        registry.register(new EmployeePayments());
        registry.register(new NetForTowMonths());  // payrollYearly_5
        registry.register(new NetDifferenceBetweenTowMonths()); // payrollYearly_4

        registry.register(new PayrollIndex());
        // ══════════════════════════════════════════
        //  فرعيات "تقرير الصرفيات الشهري" (yearly_payroll)
        // ══════════════════════════════════════════
        registry.register(new AllPayGroupsStrategy());              // payrollYearly_1
        registry.register(new MainPayGroupsStrategy());             // payrollYearly_2
        registry.register(new SeparatePayGroupsStrategy());         // payrollYearly_3

        registry.register(new SpecificManagementStrategy());        // payrollYearly_6
        registry.register(new MainForManagementStrategy());         // payrollYearly_7
        registry.register(new SeparateForManagementStrategy());     // payrollYearly_8
        registry.register(new SpecificPayGroupStrategy());          // payrollYearly_9

        // ══════════════════════════════════════════
        //  فرعيات "إجمالي التكاليف" (payroll_summary)
        // ══════════════════════════════════════════
        registry.register(new MonthlySummaryReport());
        registry.register(new MonthlySummaryReportInRange());
        registry.register(new MonthlyMainSummaryReport());
        registry.register(new MonthlyMainSummaryReportInRange());
        registry.register(new MonthlySubSummaryReport());

        // ==============================================
        //  تقارير اجر الاشتراك الفرعية PAYROLL_CHANGE_CARD
        //================================================
        registry.register(new PayrollChangeCardAll());
        registry.register(new PayrollChangeCardEmployee());
        registry.register(new PayrollChangeCardManagement());
        registry.register(new PayrollChangeCardPayGroup());

        // ==============================================
        //  تقارير اجر عنصر معين الفرعية ELEMENT
        //================================================
        registry.register(new ElementEmployee());
        registry.register(new ElementEmployees());
        registry.register(new ElementManagement());
        registry.register(new ElementPayGroup());

        // ==============================================
        //  تقارير اجر اشتراك شهر معين الفرعية CHANGE_MONTH
        //================================================
        registry.register(new PayrollChangeMonthAll());
        registry.register(new PayrollChangeMonthManagement());
        registry.register(new PayrollChangeMonthPayGroup());
        // ==============================================
        //  تقارير اجر عنصر معين الفرعية REVIEW_REPORT
        //================================================
        registry.register(new ReviewReportAll());
        registry.register(new ReviewReportEmployee());
        registry.register(new ReviewReportManagement());
        registry.register(new ReviewReportPayGroup());
        // ==============================================
        //  تقارير اجر عنصر معين الفرعية UPLOAD_PAYROLL
        //================================================
        registry.register(new UploadReviewReport());
        return registry;
    }
}