package com.safwat.hr.report.payroll.sub.payrollYearly;

import com.safwat.hr.network.ApiClient;
import com.safwat.hr.network.ApiEndpoints;
import com.safwat.hr.report.controller.PayrollReportController;
import com.safwat.hr.report.core.strategies.ReportStrategy;
import com.safwat.hr.report.core.ui.SearchFieldConfig;
import com.safwat.hr.report.core.ui.UiConfiguration;
import com.safwat.hr.report.core.ui.UiField;
import com.safwat.hr.report.core.DataSourceResolver;
import com.safwat.hr.report.core.PayrollReport;
import com.safwat.hr.report.core.ReportContext;
import com.safwat.hr.report.core.ValidationException;
import com.safwat.hr.shared.PayrollRequest;
import com.safwat.hr.shared.util.DateUtils;

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
                .visibleField(UiField.H_START_DATE)
                .visibleField(UiField.H_MANAGEMENT)
                .requiredField(UiField.H_START_DATE)
                .requiredField(UiField.H_MANAGEMENT)
                .searchField(SearchFieldConfig.of(UiField.H_MANAGEMENT, "اختر إدارة", "management"))
                .build();
    }

    @Override
    public void onApply(PayrollReportController c) {

        c.setChoseMonth();
        c.getBtn_managementSearch().setOnAction(_ -> {
            c.openSearchDialog(
                    "اختر الإدارة",
                    DataSourceResolver.get("management"),
                    c.getTxt_management()
            );
        });

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