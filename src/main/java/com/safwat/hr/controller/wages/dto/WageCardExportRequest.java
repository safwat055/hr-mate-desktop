package com.safwat.hr.controller.wages.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * مدخلات طلب تصدير بطاقة الأجور المتغيرة.
 *
 * @param nationalId         الرقم القومي للموظف
 * @param from               أول شهر في النطاق (null = من تاريخ التعيين)
 * @param to                 آخر شهر في النطاق  (null = اليوم)
 * @param precomputedMonths  نتيجة المحرك المحتسبة مسبقًا في الفرونت (null = يحتسب الباك من جديد)
 */
public record WageCardExportRequest(
        String nationalId,
        LocalDate from,
        LocalDate to,
        List<WageMonthResultDto> precomputedMonths
) {}
