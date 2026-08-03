package com.safwat.hr.report.payroll.strategies.sub.payrollYearly;

import com.safwat.hr.network.ApiClient;
import com.safwat.hr.network.ApiEndpoints;
import com.safwat.hr.report.controller.PayrollReportController;
import com.safwat.hr.report.payroll.DataSourceResolver;
import com.safwat.hr.report.payroll.PayrollReport;
import com.safwat.hr.report.payroll.ReportContext;
import com.safwat.hr.report.payroll.ValidationException;
import com.safwat.hr.report.payroll.strategies.ReportStrategy;
import com.safwat.hr.report.payroll.ui.SearchFieldConfig;
import com.safwat.hr.report.payroll.ui.UiConfiguration;
import com.safwat.hr.report.payroll.ui.UiField;
import com.safwat.hr.shared.PayrollRequest;
import com.safwat.hr.shared.util.DateUtils;

/**
 * استراتيجية تقرير "مجموعات التعيين المنفصلة لإدارة محددة".
 *
 * <p>يعرض صرفيات كل مجموعة تعيين منفصلةً عن الأخرى،
 * مُصفَّاةً بإدارة بعينها يختارها المستخدم من نافذة بحث.
 *
 * <p>الحقول المطلوبة: <b>الشهر + الإدارة</b>.
 *
 * <ul>
 *   <li>الكود: {@code payrollYearly_8}</li>
 *   <li>الفئة: {@code yearly_payroll}</li>
 *   <li>الـ Endpoint: {@link ApiEndpoints.PayrollYearly#YEARLY_EXPENSES}</li>
 * </ul>
 *
 * @see MainForManagementStrategy   للمجموعات الرئيسية (مجمَّعة) لنفس الإدارة
 * @see SpecificManagementStrategy  لكل المجموعات (مجمَّعة) لنفس الإدارة
 */
@PayrollReport(
        code = "payrollYearly_8",
        displayName = "تقرير مجموعات التعيين المنفصلة لإدارة محددة",
        category = "yearly_payroll",
        mainReport = "yearly_payroll"
)
public class SeparateForManagementStrategy implements ReportStrategy {

    @Override
    public String getCode() {
        return "payrollYearly_8";
    }

    @Override
    public String getDisplayName() {
        return "تقرير مجموعات التعيين المنفصلة لإدارة محددة";
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
                //     .title("تقرير مجموعات التعيين المنفصلة لإدارة محددة")
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