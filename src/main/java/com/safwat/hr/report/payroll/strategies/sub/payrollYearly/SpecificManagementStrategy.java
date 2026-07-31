package com.safwat.hr.report.payroll.strategies.sub.payrollYearly;

import com.safwat.hr.report.payroll.PayrollReport;
import com.safwat.hr.report.payroll.ReportContext;
import com.safwat.hr.report.payroll.ValidationException;
import com.safwat.hr.report.payroll.strategies.ReportStrategy;
import com.safwat.hr.report.payroll.ui.SearchFieldConfig;
import com.safwat.hr.report.payroll.ui.UiConfiguration;
import com.safwat.hr.report.payroll.ui.UiField;
import com.safwat.hr.service.payroll.dto.PayrollRequest;
import com.safwat.hr.shared.util.DateUtils;
import com.safwat.hr.utils.ApiClient;
import com.safwat.hr.utils.ApiEndpoints;

/**
 * استراتيجية تقرير "كل مجموعات التعيين لإدارة محددة".
 *
 * <p>يعرض صرفيات جميع مجموعات التعيين مُصفَّاةً بإدارة بعينها،
 * يختارها المستخدم من نافذة بحث.
 *
 * <p>الحقول المطلوبة: <b>الشهر + الإدارة</b>.
 *
 * <ul>
 *   <li>الكود: {@code payrollYearly_6}</li>
 *   <li>الفئة: {@code yearly_payroll}</li>
 *   <li>الـ Endpoint: {@link ApiEndpoints.PayrollYearly#YEARLY_EXPENSES}</li>
 * </ul>
 *
 * @see MainForManagementStrategy   للمجموعات الرئيسية فقط لنفس الإدارة
 * @see SeparateForManagementStrategy للمجموعات المنفصلة لنفس الإدارة
 */
@PayrollReport(
        code = "payrollYearly_6",
        displayName = "تقرير كل مجموعات التعيين لإدارة محددة",
        category = "yearly_payroll",
        mainReport = "yearly_payroll"
)
public class SpecificManagementStrategy implements ReportStrategy {

    @Override
    public String getCode() {
        return "payrollYearly_6";
    }

    @Override
    public String getDisplayName() {
        return "تقرير كل مجموعات التعيين لإدارة محددة";
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
                //           .title("تقرير كل مجموعات التعيين لإدارة محددة")
                .visibleField(UiField.START_DATE)
                .visibleField(UiField.MANAGEMENT)
                .requiredField(UiField.START_DATE)
                .requiredField(UiField.MANAGEMENT)
                .searchField(SearchFieldConfig.of(UiField.MANAGEMENT, "اختر إدارة", "management"))
                .build();
    }

    /**
     * @throws ValidationException إذا كان الشهر أو الإدارة فارغًا
     */
    @Override
    public void validate(ReportContext context) {
        if (context.getStartDate() == null || context.getStartDate().isBlank()) {
            throw new ValidationException("يجب اختيار الشهر أولاً!");
        }
        if (context.getManagement() == null || context.getManagement().isBlank()) {
            throw new ValidationException("الإدارة مطلوبة");
        }
    }

    @Override
    public PayrollRequest buildRequest(ReportContext ctx) {
        return PayrollRequest.builder()
                .user(ApiClient.getUserName())
                .startDate(DateUtils.getFirstDayOfMonth(ctx.getStartDate()))
                .management(ctx.getManagement())
                .report(getCode())
                .reportName(ctx.getReportName())
                .format(ctx.getFormat())
                .endPoint(ApiEndpoints.PayrollYearly.YEARLY_EXPENSES)
                .build();
    }
}