package com.safwat.hr.report.core;

import com.safwat.hr.payroll.PayrollService;

import java.util.List;

/**
 * مُحلِّل مصادر بيانات القوائم المنسدلة في نماذج التقارير.
 *
 * <p><b>جديد:</b> دعم Dynamic Data Sources بتاخد parameters من الواجهة.
 */
public class DataSourceResolver {

    private static final PayrollService payrollService = PayrollService.getInstance();

    /**
     * الطريقة القديمة — بدون parameters
     */
    public static List<String> get(String key) {
        return get(key, new String[0]);
    }

    /**
     * جديد — بياخد parameters اختيارية (مثلاً الشهر)
     *
     * @param key    مفتاح المصدر
     * @param params parameters اختيارية (مثلاً قيمة الشهر)
     */
    public static List<String> get(String key, String... params) {
        return switch (key) {
            case "payGroup" -> payrollService.getPayGroup();
            case "management" -> payrollService.getManagement();
            case "monthsYearly" -> payrollService.getAllMonthsYearly();
            case "elements" -> payrollService.getAllElementNames();
            case "elementsCodes" -> payrollService.getAllElementCodes();

            // ═══════════════════════════════════════════════════════
            //  جديد — Dynamic Sources بتعتمد على parameters
            // ═══════════════════════════════════════════════════════
            case "elementsByMonth" -> {
                if (params.length > 0 && params[0] != null && !params[0].isBlank()) {
                    yield payrollService.getElementNamesByMonth(params[0]);
                }
                yield List.of();
            }
            case "payGroupsByMonth" -> {
                if (params.length > 0 && params[0] != null && !params[0].isBlank()) {
                    yield payrollService.getPayGroupsByMonth(params[0]);
                }
                yield List.of();
            }

            default -> List.of();
        };
    }
}