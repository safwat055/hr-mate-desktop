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

import java.util.List;

/**
 * استراتيجية تقرير "مجموعات التعيين الرئيسية لإدارة محددة".
 *
 * <p>يُظهر الحقول: الشهر + الإدارة.
 * يتحقق من إدخال كليهما قبل إرسال الطلب.
 *
 * <p>الكود: {@code payrollYearly_7}
 * الفئة: {@code yearly_payroll}
 */
@PayrollReport(
        code = "payrollYearly_7",
        displayName = "تقرير مجموعات التعيين الرئيسية لإدارة محددة",
        category = "yearly_payroll",
        mainReport = "yearly_payroll"
)
public class MainForManagementStrategy implements ReportStrategy {

    @Override
    public String getCode() {
        return "payrollYearly_7";
    }

    @Override
    public String getDisplayName() {
        return "تقرير مجموعات التعيين الرئيسية لإدارة محددة";
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
                .title("تقرير مجموعات التعيين الرئيسية لإدارة محددة")
                .visibleFields(List.of(UiField.START_DATE, UiField.MANAGEMENT))
                .requiredFields(List.of(UiField.START_DATE, UiField.MANAGEMENT))
                .needsSearchDialog(true)
                .searchDialogTitle("اختر إدارة")
                .searchDataSource("management")
                .build();
    }

    /**
     * التحقق من صحة المدخلات.
     *
     * <p><b>إصلاح:</b> كانت رسالة الخطأ الخاصة بالتاريخ تعرض "الإدارة مطلوبة" بالخطأ.
     *
     * @param context بيانات الطلب المدخلة من المستخدم
     * @throws ValidationException إذا كان التاريخ أو الإدارة فارغًا
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