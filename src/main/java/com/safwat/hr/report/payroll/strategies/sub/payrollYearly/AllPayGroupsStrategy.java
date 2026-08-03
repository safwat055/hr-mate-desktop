package com.safwat.hr.report.payroll.strategies.sub.payrollYearly;

import com.safwat.hr.network.ApiClient;
import com.safwat.hr.network.ApiEndpoints;
import com.safwat.hr.report.controller.PayrollReportController;
import com.safwat.hr.report.payroll.PayrollReport;
import com.safwat.hr.report.payroll.ReportContext;
import com.safwat.hr.report.payroll.ValidationException;
import com.safwat.hr.report.payroll.strategies.ReportStrategy;
import com.safwat.hr.report.payroll.ui.UiConfiguration;
import com.safwat.hr.report.payroll.ui.UiField;
import com.safwat.hr.shared.PayrollRequest;
import com.safwat.hr.shared.util.DateUtils;

/**
 * استراتيجية تقرير "كل مجموعات التعيين".
 *
 * <p>أبسط تقارير الصرفيات — يعرض صرفيات جميع مجموعات التعيين
 * لشهر محدد دون أي تصفية إضافية.
 *
 * <p>الحقول المطلوبة: <b>الشهر فقط</b>.
 *
 * <ul>
 *   <li>الكود: {@code payrollYearly_1}</li>
 *   <li>الفئة: {@code yearly_payroll}</li>
 *   <li>الـ Endpoint: {@link ApiEndpoints.PayrollYearly#YEARLY_EXPENSES}</li>
 * </ul>
 */
@PayrollReport(
        code = "payrollYearly_1",
        displayName = "كل مجموعات التعيين",
        category = "yearly_payroll",
        mainReport = "yearly_payroll"
)
public class AllPayGroupsStrategy implements ReportStrategy {

    @Override
    public String getCode() {
        return "payrollYearly_1";
    }

    @Override
    public String getDisplayName() {
        return "تقرير كل المجموعات";
    }

    @Override
    public String getCategory() {
        return "yearly_payroll";
    }

    @Override
    public String getMainReport() {
        return "yearly_payroll";
    }

    @Override
    public UiConfiguration getUiConfig() {
        return UiConfiguration.builder()
                //    .title("كل مجموعات التعيين")
                .visibleField(UiField.H_START_DATE)
                .requiredField(UiField.H_START_DATE)
                .build();
    }

    @Override
    public void onApply(PayrollReportController c) {

        c.setChoseMonth();

    }

    /**
     * يتحقق من اختيار الشهر.
     *
     * @throws ValidationException إذا كان الشهر فارغًا
     */
    @Override
    public void validate(ReportContext context) {
        if (context.getStartDate() == null || context.getStartDate().isBlank()) {
            throw new ValidationException("يجب اختيار الشهر أولاً!");
        }
    }

    @Override
    public PayrollRequest buildRequest(ReportContext ctx) {
        return PayrollRequest.builder()
                .user(ApiClient.getUserName())
                .startDate(DateUtils.getFirstDayOfMonth(ctx.getStartDate()))
                .report(getCode())
                .reportName(ctx.getReportName())
                .format(ctx.getFormat())
                .endPoint(ApiEndpoints.PayrollYearly.YEARLY_EXPENSES)
                .build();
    }
}