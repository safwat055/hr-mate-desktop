package com.safwat.hr.controller.allowance;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * فترة يدوية واحدة لبدل معين — نسخة الفرونت (client-side).
 *
 * <p>يعكس:
 * <ul>
 *   <li>{@code EmployeeAllowanceConfig.OverrideEntry} (entity في الباك)</li>
 *   <li>{@code OverrideEntry} record المستقل في الباك</li>
 * </ul>
 *
 * <p>بيجي في:
 * <ul>
 *   <li>{@code AllowanceResultDto.AllowanceLineDto#overrides()} — قائمة الفترات اليدوية
 *       لكل بدل في response البدلات</li>
 *   <li>{@code AllowanceResultDto.OverrideRequest#entries()} — الفترات المُرسلة
 *       في PUT /api/allowances/employee/{id}/override</li>
 * </ul>
 *
 * <p>الـ {@code table_entries} في {@code AllowanceOverrideDialogController}
 * بيعمل {@code TableView<OverrideEntry>} باستخدام هذا الـ record.
 */
public record OverrideEntry(
        LocalDate from,
        LocalDate to,       // null = مفتوح لحد دلوقتي
        BigDecimal value
) {
    /**
     * هل التاريخ المطلوب يقع داخل هذه الفترة؟
     */
    public boolean covers(LocalDate targetDate) {
        if (targetDate.isBefore(from)) return false;
        if (to == null) return true;
        return !targetDate.isAfter(to);
    }
}