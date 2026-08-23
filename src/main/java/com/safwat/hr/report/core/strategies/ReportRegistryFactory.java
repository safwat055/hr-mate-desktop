package com.safwat.hr.report.core.strategies;

import com.safwat.hr.report.payroll.direct.*;
import com.safwat.hr.report.payroll.mainContainer.*;
import com.safwat.hr.report.payroll.sub.changeCard.card.PayrollChangeCardAll;
import com.safwat.hr.report.payroll.sub.changeCard.card.PayrollChangeCardEmployee;
import com.safwat.hr.report.payroll.sub.changeCard.card.PayrollChangeCardManagement;
import com.safwat.hr.report.payroll.sub.changeCard.card.PayrollChangeCardPayGroup;
import com.safwat.hr.report.payroll.sub.changeCard.month.PayrollChangeMonthAll;
import com.safwat.hr.report.payroll.sub.changeCard.month.PayrollChangeMonthManagement;
import com.safwat.hr.report.payroll.sub.changeCard.month.PayrollChangeMonthPayGroup;
import com.safwat.hr.report.payroll.sub.payrollReview.elementCodeDetails.ElementCodeEmployeeDetails;
import com.safwat.hr.report.payroll.sub.payrollReview.elementCodeDetails.ElementCodeEmployeesDetails;
import com.safwat.hr.report.payroll.sub.payrollReview.elementCodeDetails.ElementCodeManagementDetails;
import com.safwat.hr.report.payroll.sub.payrollReview.elementCodeDetails.ElementCodePayGroupDetails;
import com.safwat.hr.report.payroll.sub.payrollReview.elementCodeTotal.ElementCodeEmployeeTotal;
import com.safwat.hr.report.payroll.sub.payrollReview.elementCodeTotal.ElementCodeEmployeesTotal;
import com.safwat.hr.report.payroll.sub.payrollReview.elementCodeTotal.ElementCodeManagementTotal;
import com.safwat.hr.report.payroll.sub.payrollReview.elementCodeTotal.ElementCodePayGroupTotal;
import com.safwat.hr.report.payroll.sub.payrollReview.elementCompare.ElementCompareEmployee;
import com.safwat.hr.report.payroll.sub.payrollReview.elementCompare.ElementCompareEmployees;
import com.safwat.hr.report.payroll.sub.payrollReview.elementDetails.ElementEmployeeDetails;
import com.safwat.hr.report.payroll.sub.payrollReview.elementDetails.ElementEmployeesDetails;
import com.safwat.hr.report.payroll.sub.payrollReview.elementDetails.ElementManagementDetails;
import com.safwat.hr.report.payroll.sub.payrollReview.elementDetails.ElementPayGroupDetails;
import com.safwat.hr.report.payroll.sub.payrollReview.elementTotal.ElementEmployee;
import com.safwat.hr.report.payroll.sub.payrollReview.elementTotal.ElementEmployees;
import com.safwat.hr.report.payroll.sub.payrollReview.elementTotal.ElementManagement;
import com.safwat.hr.report.payroll.sub.payrollReview.elementTotal.ElementPayGroup;
import com.safwat.hr.report.payroll.sub.payrollReview.reviewReport.FullReviewReport;
import com.safwat.hr.report.payroll.sub.payrollReview.reviewReport.MainReviewReport;
import com.safwat.hr.report.payroll.sub.payrollReview.update.UpdateReviewKeysAll;
import com.safwat.hr.report.payroll.sub.payrollReview.update.UpdateReviewKeysMonth;
import com.safwat.hr.report.payroll.sub.payrollSummary.*;
import com.safwat.hr.report.payroll.sub.payrollYearly.*;
import com.safwat.hr.report.payroll.sub.records.full.FullRecordAll;
import com.safwat.hr.report.payroll.sub.records.full.FullRecordEmployee;
import com.safwat.hr.report.payroll.sub.records.full.FullRecordManagement;
import com.safwat.hr.report.payroll.sub.records.full.FullRecordPayGroup;
import com.safwat.hr.report.payroll.sub.records.short_.ShortRecordAll;
import com.safwat.hr.report.payroll.sub.records.short_.ShortRecordEmployee;
import com.safwat.hr.report.payroll.sub.records.short_.ShortRecordManagement;
import com.safwat.hr.report.payroll.sub.records.short_.ShortRecordPayGroup;
import com.safwat.hr.report.payroll.sub.upload.*;
import com.safwat.hr.report.public_.StartTransferData;

/**
 * مصنع إنشاء السجل الرئيسي لاستراتيجيات التقارير.
 *
 * <p>هذا الملف هو <b>النقطة الوحيدة</b> التي يُسجَّل فيها أي تقرير جديد.
 *
 * <h2>لإضافة تقرير جديد</h2>
 * <ol>
 *   <li>أنشئ class في المجلد المناسب</li>
 *   <li>ضع عليه {@code @PayrollReport} annotation</li>
 *   <li>أضف سطر {@code registry.registerLazy(MyNewStrategy.class);} في القسم المناسب</li>
 *   <li>انتهى ✓</li>
 * </ol>
 */
public class ReportRegistryFactory {

    public static ReportStrategyRegistry create() {
        ReportStrategyRegistry registry = new ReportStrategyRegistry();

        // ══════════════════════════════════════════
        //  التقارير الرئيسية (mainContainer) — eager
        // ══════════════════════════════════════════
        registry.register(new MonthlyExpensesContainerStrategy());
        registry.register(new PayrollCostSummaryStrategy());
        registry.register(new ElementCompare());
        registry.register(new FullRecords());
        registry.register(new ShortRecords());
        registry.register(new PayrollChangeCard());
        registry.register(new PayrollChangeMonth());
        registry.register(new PayrollElementCodeDetails());
        registry.register(new PayrollElementCodeTotal());
        registry.register(new PayrollElementDetails());
        registry.register(new PayrollElementTotal());
        registry.register(new ReviewReport());
        registry.register(new UpdateReviewKey());
        registry.register(new UploadPayrollReport());

        // ══════════════════════════════════════════
        //  Direct — lazy
        // ══════════════════════════════════════════
        registry.registerLazy(ElementComparisonAddedDeletedReport.class);
        registry.registerLazy(EmployeePayments.class);
        registry.registerLazy(NetDifferenceBetweenTowMonths.class);
        registry.registerLazy(NetForTowMonths.class);
        registry.registerLazy(PayrollElementReport.class);
        registry.registerLazy(PayrollIndex.class);
        registry.registerLazy(PayrollReviewSheet.class);
        registry.registerLazy(ScaleReport.class);
        registry.registerLazy(SummaryTotal.class);
        registry.registerLazy(StartTransferData.class);

        // ══════════════════════════════════════════
        //  فرعيات payrollYearly — lazy
        // ══════════════════════════════════════════
        registry.registerLazy(AllPayGroupsStrategy.class);
        registry.registerLazy(MainPayGroupsStrategy.class);
        registry.registerLazy(SeparatePayGroupsStrategy.class);
        registry.registerLazy(SpecificManagementStrategy.class);
        registry.registerLazy(MainForManagementStrategy.class);
        registry.registerLazy(SeparateForManagementStrategy.class);
        registry.registerLazy(SpecificPayGroupStrategy.class);

        // ══════════════════════════════════════════
        //  فرعيات payrollSummary — lazy
        // ══════════════════════════════════════════
        registry.registerLazy(MonthlySummaryReport.class);
        registry.registerLazy(MonthlyMainSummaryReport.class);
        registry.registerLazy(MonthlySubSummaryReport.class);
        registry.registerLazy(MonthlySummaryReportInRange.class);
        registry.registerLazy(MonthlyMainSummaryReportInRange.class);

        // ══════════════════════════════════════════
        //  فرعيات changeCard — card — lazy
        // ══════════════════════════════════════════
        registry.registerLazy(PayrollChangeCardAll.class);
        registry.registerLazy(PayrollChangeCardEmployee.class);
        registry.registerLazy(PayrollChangeCardManagement.class);
        registry.registerLazy(PayrollChangeCardPayGroup.class);

        // ══════════════════════════════════════════
        //  فرعيات changeCard — month — lazy
        // ══════════════════════════════════════════
        registry.registerLazy(PayrollChangeMonthAll.class);
        registry.registerLazy(PayrollChangeMonthManagement.class);
        registry.registerLazy(PayrollChangeMonthPayGroup.class);

        // ══════════════════════════════════════════
        //  فرعيات payrollReview — elementCompare — lazy
        // ══════════════════════════════════════════
        registry.registerLazy(ElementCompareEmployee.class);
        registry.registerLazy(ElementCompareEmployees.class);

        // ══════════════════════════════════════════
        //  فرعيات payrollReview — elementCodeDetails — lazy
        // ══════════════════════════════════════════
        registry.registerLazy(ElementCodeEmployeeDetails.class);
        registry.registerLazy(ElementCodeEmployeesDetails.class);
        registry.registerLazy(ElementCodeManagementDetails.class);
        registry.registerLazy(ElementCodePayGroupDetails.class);

        // ══════════════════════════════════════════
        //  فرعيات payrollReview — elementCodeTotal — lazy
        // ══════════════════════════════════════════
        registry.registerLazy(ElementCodeEmployeeTotal.class);
        registry.registerLazy(ElementCodeEmployeesTotal.class);
        registry.registerLazy(ElementCodeManagementTotal.class);
        registry.registerLazy(ElementCodePayGroupTotal.class);

        // ══════════════════════════════════════════
        //  فرعيات payrollReview — elementDetails — lazy
        // ══════════════════════════════════════════
        registry.registerLazy(ElementEmployeeDetails.class);
        registry.registerLazy(ElementEmployeesDetails.class);
        registry.registerLazy(ElementManagementDetails.class);
        registry.registerLazy(ElementPayGroupDetails.class);

        // ══════════════════════════════════════════
        //  فرعيات payrollReview — elementTotal — lazy
        // ══════════════════════════════════════════
        registry.registerLazy(ElementEmployee.class);
        registry.registerLazy(ElementEmployees.class);
        registry.registerLazy(ElementManagement.class);
        registry.registerLazy(ElementPayGroup.class);

        // ══════════════════════════════════════════
        //  فرعيات payrollReview — reviewReport — lazy
        // ══════════════════════════════════════════
        registry.registerLazy(FullReviewReport.class);
        registry.registerLazy(MainReviewReport.class);

        // ══════════════════════════════════════════
        //  فرعيات payrollReview — update — lazy
        // ══════════════════════════════════════════
        registry.registerLazy(UpdateReviewKeysAll.class);
        registry.registerLazy(UpdateReviewKeysMonth.class);

        // ══════════════════════════════════════════
        //  فرعيات records — full — lazy
        // ══════════════════════════════════════════
        registry.registerLazy(FullRecordAll.class);
        registry.registerLazy(FullRecordEmployee.class);
        registry.registerLazy(FullRecordManagement.class);
        registry.registerLazy(FullRecordPayGroup.class);

        // ══════════════════════════════════════════
        //  فرعيات records — short_ — lazy
        // ══════════════════════════════════════════
        registry.registerLazy(ShortRecordAll.class);
        registry.registerLazy(ShortRecordEmployee.class);
        registry.registerLazy(ShortRecordManagement.class);
        
        registry.registerLazy(ShortRecordPayGroup.class);

        // ══════════════════════════════════════════
        //  فرعيات upload — lazy
        // ══════════════════════════════════════════
        registry.registerLazy(UploadChangeCardReport.class);
        registry.registerLazy(UploadIndex_30_06_Report.class);
        registry.registerLazy(UploadIndexReport.class);
        registry.registerLazy(UploadReviewReport.class);
        registry.registerLazy(UploadYearlyReport.class);

        return registry;
    }
}