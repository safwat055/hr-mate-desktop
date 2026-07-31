package com.safwat.hr.report.payroll.strategies.sub.payrollYearly;

import com.safwat.hr.report.payroll.PayrollReport;
import com.safwat.hr.report.payroll.ReportContext;
import com.safwat.hr.report.payroll.ValidationException;
import com.safwat.hr.report.payroll.strategies.ReportStrategy;
import com.safwat.hr.report.payroll.ui.UiConfiguration;
import com.safwat.hr.report.payroll.ui.UiField;
import com.safwat.hr.service.payroll.dto.PayrollRequest;
import com.safwat.hr.shared.util.DateUtils;
import com.safwat.hr.utils.ApiClient;
import com.safwat.hr.utils.ApiEndpoints;

/**
 * استراتيجية تقرير "مجموعات التعيين المنفصلة".
 *
 * <p>يعرض صرفيات كل مجموعة تعيين على حدة (صفحة أو قسم منفصل لكل مجموعة)
 * لشهر محدد، بعكس {@link MainPayGroupsStrategy} التي تجمعها في عرض واحد.
 *
 * <p>الحقول المطلوبة: <b>الشهر فقط</b>.
 *
 * <ul>
 *   <li>الكود: {@code payrollYearly_3}</li>
 *   <li>الفئة: {@code yearly_payroll}</li>
 *   <li>الـ Endpoint: {@link ApiEndpoints.PayrollYearly#YEARLY_EXPENSES}</li>
 * </ul>
 *
 * @see MainPayGroupsStrategy النظير المجمَّع لنفس البيانات
 */
@PayrollReport(
        code = "payrollYearly_3",
        displayName = "مجموعات التعيين المنفصلة",
        category = "yearly_payroll",
        mainReport = "yearly_payroll"
)
public class SeparatePayGroupsStrategy implements ReportStrategy {

    @Override
    public String getCode() {
        return "payrollYearly_3";
    }

    @Override
    public String getDisplayName() {
        return "مجموعات التعيين المنفصلة";
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
                //     .title("مجموعات التعيين المنفصلة")
                .visibleField(UiField.START_DATE)
                .requiredField(UiField.START_DATE)
                .build();
    }

    /**
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