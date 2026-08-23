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
 *   <li>أضف سطر {@code registry.register(new MyNewStrategy());} في القسم المناسب</li>
 *   <li>انتهى ✓</li>
 * </ol>
 */
public class ReportRegistryFactory {

    public static ReportStrategyRegistry create() {
        ReportStrategyRegistry registry = new ReportStrategyRegistry();

        // ══════════════════════════════════════════
        //  التقارير الرئيسية (mainContainer)
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
        //  Direct (تقارير مباشرة بدون حاوٍ)
        // ══════════════════════════════════════════
        registry.register(new ElementComparisonAddedDeletedReport());
        registry.register(new EmployeePayments());
        registry.register(new NetDifferenceBetweenTowMonths());
        registry.register(new NetForTowMonths());
        registry.register(new PayrollElementReport());
        registry.register(new PayrollIndex());
        registry.register(new PayrollReviewSheet());
        registry.register(new ScaleReport());
        registry.register(new SummaryTotal());
        registry.register(new StartTransferData());

        // ══════════════════════════════════════════
        //  فرعيات payrollYearly
        // ══════════════════════════════════════════
        registry.register(new AllPayGroupsStrategy());
        registry.register(new MainPayGroupsStrategy());
        registry.register(new SeparatePayGroupsStrategy());
        registry.register(new SpecificManagementStrategy());
        registry.register(new MainForManagementStrategy());
        registry.register(new SeparateForManagementStrategy());
        registry.register(new SpecificPayGroupStrategy());

        // ══════════════════════════════════════════
        //  فرعيات payrollSummary
        // ══════════════════════════════════════════
        registry.register(new MonthlySummaryReport());
        registry.register(new MonthlyMainSummaryReport());
        registry.register(new MonthlySubSummaryReport());
        registry.register(new MonthlySummaryReportInRange());
        registry.register(new MonthlyMainSummaryReportInRange());

        // ══════════════════════════════════════════
        //  فرعيات changeCard — card
        // ══════════════════════════════════════════
        registry.register(new PayrollChangeCardAll());
        registry.register(new PayrollChangeCardEmployee());
        registry.register(new PayrollChangeCardManagement());
        registry.register(new PayrollChangeCardPayGroup());

        // ══════════════════════════════════════════
        //  فرعيات changeCard — month
        // ══════════════════════════════════════════
        registry.register(new PayrollChangeMonthAll());
        registry.register(new PayrollChangeMonthManagement());
        registry.register(new PayrollChangeMonthPayGroup());

        // ══════════════════════════════════════════
        //  فرعيات payrollReview — elementCompare
        // ══════════════════════════════════════════
        registry.register(new ElementCompareEmployee());
        registry.register(new ElementCompareEmployees());

        // ══════════════════════════════════════════
        //  فرعيات payrollReview — elementCodeDetails
        // ══════════════════════════════════════════
        registry.register(new ElementCodeEmployeeDetails());
        registry.register(new ElementCodeEmployeesDetails());
        registry.register(new ElementCodeManagementDetails());
        registry.register(new ElementCodePayGroupDetails());

        // ══════════════════════════════════════════
        //  فرعيات payrollReview — elementCodeTotal
        // ══════════════════════════════════════════
        registry.register(new ElementCodeEmployeeTotal());
        registry.register(new ElementCodeEmployeesTotal());
        registry.register(new ElementCodeManagementTotal());
        registry.register(new ElementCodePayGroupTotal());

        // ══════════════════════════════════════════
        //  فرعيات payrollReview — elementDetails
        // ══════════════════════════════════════════
        registry.register(new ElementEmployeeDetails());
        registry.register(new ElementEmployeesDetails());
        registry.register(new ElementManagementDetails());
        registry.register(new ElementPayGroupDetails());

        // ══════════════════════════════════════════
        //  فرعيات payrollReview — elementTotal
        // ══════════════════════════════════════════
        registry.register(new ElementEmployee());
        registry.register(new ElementEmployees());
        registry.register(new ElementManagement());
        registry.register(new ElementPayGroup());

        // ══════════════════════════════════════════
        //  فرعيات payrollReview — reviewReport
        // ══════════════════════════════════════════
        registry.register(new FullReviewReport());
        registry.register(new MainReviewReport());

        // ══════════════════════════════════════════
        //  فرعيات payrollReview — update
        // ══════════════════════════════════════════
        registry.register(new UpdateReviewKeysAll());
        registry.register(new UpdateReviewKeysMonth());

        // ══════════════════════════════════════════
        //  فرعيات records — full
        // ══════════════════════════════════════════
        registry.register(new FullRecordAll());
        registry.register(new FullRecordEmployee());
        registry.register(new FullRecordManagement());
        registry.register(new FullRecordPayGroup());

        // ══════════════════════════════════════════
        //  فرعيات records — short_
        // ══════════════════════════════════════════
        registry.register(new ShortRecordAll());
        registry.register(new ShortRecordEmployee());
        registry.register(new ShortRecordManagement());
        registry.register(new ShortRecordPayGroup());

        // ══════════════════════════════════════════
        //  فرعيات upload
        // ══════════════════════════════════════════
        registry.register(new UploadChangeCardReport());
        registry.register(new UploadIndex_30_06_Report());
        registry.register(new UploadIndexReport());
        registry.register(new UploadReviewReport());
        registry.register(new UploadYearlyReport());

        return registry;
    }
}