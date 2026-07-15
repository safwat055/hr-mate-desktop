package com.safwat.hr.shared.util;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;

import static com.safwat.hr.shared.util.StringUtil.convertArabicToEnglishNumbers;

public class DateUtils {
    private static final List<String> ARABIC_MONTH_NAMES = Arrays.asList(
            "يناير", "فبراير", "مارس", "ابريل", "مايو", "يونيو",
            "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    );

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


    /**
     * Converts a string in the format "Arabic Month Year"
     * to a LocalDate representing the first day of that month.
     * <p>
     * Examples:
     * "يونيو 2024" -> 2024-06-01
     * "يناير 2025" -> 2025-01-01
     *
     * @param value Arabic month and year.
     * @return LocalDate representing the first day of the month.
     * @throws IllegalArgumentException if the input format is invalid.
     */
    public static LocalDate fromArabicMonthYear(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        String[] parts = value.trim().split("\\s+");

        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid month/year format: " + value);
        }

        String monthName = parts[0];
        int year = Integer.parseInt(parts[1]);

        int month = ARABIC_MONTH_NAMES.indexOf(monthName) + 1;

        if (month == 0) {
            throw new IllegalArgumentException("Unknown Arabic month: " + monthName);
        }

        return LocalDate.of(year, month, 1);
    }

    /**
     * Converts a string containing an Arabic month and a year
     * regardless of their order.
     * <p>
     * Supported examples:
     * "يونيو 2024"
     * "2024 يونيو"
     * "٢٠٢٤ يونيو"
     * "يونيو ٢٠٢٤"
     *
     * @param value Arabic month and year in any order.
     * @return LocalDate representing the first day of the month.
     * @throws IllegalArgumentException if the input format is invalid.
     */
    public static LocalDate fromArabicMonthYearFlexible(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        // تحويل الأرقام العربية إلى إنجليزية
        value = convertArabicToEnglishNumbers(value.trim());

        // ✅ استخراج السنة (أرقام)
        Integer year = null;
        java.util.regex.Matcher yearMatcher = java.util.regex.Pattern.compile("\\d+").matcher(value);
        if (yearMatcher.find()) {
            year = Integer.parseInt(yearMatcher.group());
        }

        // ✅ استخراج اسم الشهر (أي حروف)
        String monthName = null;

        // الحالة 1: اسم الشهر منفصل (مثلاً "ديسمبر 2022" أو "2022 ديسمبر")
        // الحالة 2: اسم الشهر متصل بالسنة (مثلاً "2022ديسمبر")
        // الحالة 3: اسم الشهر فقط (مثلاً "ديسمبر")

        // نزيل كل الأرقام ونأخذ المتبقي
        String cleaned = value.replaceAll("\\d+", "").trim();
        if (!cleaned.isEmpty()) {
            monthName = cleaned;
        }

        if (monthName == null || year == null) {
            throw new IllegalArgumentException("Invalid month/year format: " + value);
        }

        // البحث عن الشهر في القائمة (مع تجاهل حالة الأحرف)
        int month = -1;
        for (int i = 0; i < ARABIC_MONTH_NAMES.size(); i++) {
            if (ARABIC_MONTH_NAMES.get(i).equalsIgnoreCase(monthName)) {
                month = i + 1;
                break;
            }
        }

        if (month == -1) {
            throw new IllegalArgumentException("Unknown Arabic month: " + monthName);
        }

        return LocalDate.of(year, month, 1);
    }
}