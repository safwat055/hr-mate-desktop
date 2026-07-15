package com.safwat.hr.shared.util;

import java.time.LocalDate;
import java.time.YearMonth;

public class DateUtils {

    /**
     * تحويل string بصيغة "شهر سنة" إلى LocalDate لأول يوم في الشهر
     * مثال: "2 2025" -> 2025-02-01
     */
    public static LocalDate getFirstDayOfMonth(String monthYear) {

        String[] parts = monthYear.trim().split(" ");
        if (parts.length != 2) {
            return null;
        }

        int month = Integer.parseInt(parts[0]);
        int year = Integer.parseInt(parts[1]);
       
        return LocalDate.of(year, month, 1);
    }

    /**
     * تحويل string بصيغة "شهر سنة" إلى LocalDate لآخر يوم في الشهر
     * مثال: "2 2025" -> 2025-02-28 (أو 29 في سنة كبيسة)
     */
    public static LocalDate getLastDayOfMonth(String monthYear) {
        String[] parts = monthYear.trim().split(" ");
        if (parts.length != 2) {
            return null;
        }

        int month = Integer.parseInt(parts[0]);
        int year = Integer.parseInt(parts[1]);

        YearMonth yearMonth = YearMonth.of(year, month);
        return yearMonth.atEndOfMonth();
    }

    // ====== طرق استخدام إضافية ======

    /**
     * ترجع أول يوم في الشهر كـ String
     */
    public static String getFirstDayAsString(String monthYear) {
        return getFirstDayOfMonth(monthYear).toString();
    }

    /**
     * ترجع آخر يوم في الشهر كـ String
     */
    public static String getLastDayAsString(String monthYear) {
        return getLastDayOfMonth(monthYear).toString();
    }

    /**
     * ترجع أول وآخر يوم في الشهر كـ Array
     */
    public static LocalDate[] getMonthRange(String monthYear) {
        return new LocalDate[]{
                getFirstDayOfMonth(monthYear),
                getLastDayOfMonth(monthYear)
        };
    }
}