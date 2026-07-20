package com.safwat.hr.shared.util;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static com.safwat.hr.shared.util.StringUtil.convertArabicToEnglishNumbers;

public class DateUtils {
    private static final List<String> ARABIC_MONTH_NAMES = Arrays.asList(
            "يناير", "فبراير", "مارس", "ابريل", "مايو", "يونيو",
            "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    );


    /**
     * تحويل string بصيغة "شهر سنة" أو "سنة شهر" إلى LocalDate لأول يوم في الشهر
     * يدعم الصيغ التالية:
     * - "2 2025" أو "2025 2"
     * - "02 2025" أو "2025 02"
     * - "فبراير 2025" أو "2025 فبراير"
     * - "Feb 2025" أو "2025 Feb"
     * - "February 2025" أو "2025 February"
     * - "2-2025" أو "2025-2"
     * - "02/2025" أو "2025/02"
     * - "2-2025" أو "2025-2"
     *
     * @param monthYear النص المدخل
     * @return LocalDate لأول يوم في الشهر، أو null إذا كان التنسيق غير صحيح
     */
    public static LocalDate getFirstDayOfMonth(String monthYear) {
        if (monthYear == null || monthYear.trim().isEmpty()) {
            return null;
        }

        String input = monthYear.trim();

        // محاولة التحويل المباشر باستخدام YearMonth
        try {
            // محاولة صيغ مختلفة
            DateTimeFormatter[] formatters = {
                    DateTimeFormatter.ofPattern("M yyyy"),      // 2 2025
                    DateTimeFormatter.ofPattern("yyyy M"),      // 2025 2
                    DateTimeFormatter.ofPattern("MM yyyy"),     // 02 2025
                    DateTimeFormatter.ofPattern("yyyy MM"),     // 2025 02
                    DateTimeFormatter.ofPattern("M-yyyy"),      // 2-2025
                    DateTimeFormatter.ofPattern("yyyy-M"),      // 2025-2
                    DateTimeFormatter.ofPattern("MM-yyyy"),     // 02-2025
                    DateTimeFormatter.ofPattern("yyyy-MM"),     // 2025-02
                    DateTimeFormatter.ofPattern("M/yyyy"),      // 2/2025
                    DateTimeFormatter.ofPattern("yyyy/M"),      // 2025/2
                    DateTimeFormatter.ofPattern("MM/yyyy"),     // 02/2025
                    DateTimeFormatter.ofPattern("yyyy/MM"),     // 2025/02
                    DateTimeFormatter.ofPattern("M.yyyy"),      // 2.2025
                    DateTimeFormatter.ofPattern("yyyy.M"),      // 2025.2
                    DateTimeFormatter.ofPattern("MM.yyyy"),     // 02.2025
                    DateTimeFormatter.ofPattern("yyyy.MM"),     // 2025.02
            };

            for (DateTimeFormatter formatter : formatters) {
                try {
                    YearMonth yearMonth = YearMonth.parse(input, formatter);
                    return yearMonth.atDay(1);
                } catch (DateTimeParseException e) {
                    // تجاهل ومحاولة الصيغة التالية
                }
            }
        } catch (Exception e) {
            // تجاهل
        }

        // محاولة استخراج الشهر والسنة من النص (يدعم أسماء الأشهر)
        try {
            // فصل الأرقام عن النص
            String[] parts = input.split("[\\s\\-/.]+");

            if (parts.length == 2) {
                // محاولة تحديد أي جزء هو الشهر وأي جزء هو السنة
                Integer month = null;
                Integer year = null;

                for (String part : parts) {
                    // محاولة تحويل إلى رقم
                    try {
                        int num = Integer.parseInt(part);
                        // إذا كان الرقم بين 1-12 فهو شهر، وإلا فهو سنة
                        if (num >= 1 && num <= 12 && month == null) {
                            month = num;
                        } else if (num >= 1000 && num <= 9999 && year == null) {
                            year = num;
                        } else if (year == null && num > 12) {
                            year = num;
                        } else if (month == null && num <= 12) {
                            month = num;
                        } else {
                            // إذا كان الرقم لا ينطبق على أي منهما، حاول التعامل معه
                            if (year == null) year = num;
                            else if (month == null && num <= 12) month = num;
                        }
                    } catch (NumberFormatException e) {
                        // ليس رقماً، قد يكون اسم شهر
                        Integer monthFromName = getMonthFromName(part);
                        if (monthFromName != null) {
                            month = monthFromName;
                        }
                    }
                }

                if (month != null && year != null) {
                    // التأكد من صحة القيم
                    if (month >= 1 && month <= 12 && year >= 1000 && year <= 9999) {
                        return LocalDate.of(year, month, 1);
                    }
                }
            }
        } catch (Exception e) {
            // تجاهل
        }

        // محاولة استخدام التعبيرات النمطية للتعامل مع الصيغ المعقدة
        try {
            // صيغة: "شهر سنة" مع أسماء الأشهر العربية
            Pattern arabicPattern = Pattern.compile(
                    "(يناير|فبراير|مارس|أبريل|مايو|يونيو|يوليو|أغسطس|سبتمبر|أكتوبر|نوفمبر|ديسمبر)\\s*(\\d{4})",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
            );
            java.util.regex.Matcher matcher = arabicPattern.matcher(input);
            if (matcher.find()) {
                String monthName = matcher.group(1);
                int year = Integer.parseInt(matcher.group(2));
                Integer month = getMonthFromName(monthName);
                if (month != null) {
                    return LocalDate.of(year, month, 1);
                }
            }

            // صيغة: "سنة شهر" مع أسماء الأشهر العربية
            Pattern arabicPatternReversed = Pattern.compile(
                    "(\\d{4})\\s*(يناير|فبراير|مارس|أبريل|مايو|يونيو|يوليو|أغسطس|سبتمبر|أكتوبر|نوفمبر|ديسمبر)",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
            );
            matcher = arabicPatternReversed.matcher(input);
            if (matcher.find()) {
                int year = Integer.parseInt(matcher.group(1));
                String monthName = matcher.group(2);
                Integer month = getMonthFromName(monthName);
                if (month != null) {
                    return LocalDate.of(year, month, 1);
                }
            }

            // صيغة: "شهر سنة" مع أسماء الأشهر الإنجليزية
            Pattern englishPattern = Pattern.compile(
                    "(January|February|March|April|May|June|July|August|September|October|November|December|Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\s*(\\d{4})",
                    Pattern.CASE_INSENSITIVE
            );
            matcher = englishPattern.matcher(input);
            if (matcher.find()) {
                String monthName = matcher.group(1);
                int year = Integer.parseInt(matcher.group(2));
                Integer month = getMonthFromName(monthName);
                if (month != null) {
                    return LocalDate.of(year, month, 1);
                }
            }

            // صيغة: "سنة شهر" مع أسماء الأشهر الإنجليزية
            Pattern englishPatternReversed = Pattern.compile(
                    "(\\d{4})\\s*(January|February|March|April|May|June|July|August|September|October|November|December|Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)",
                    Pattern.CASE_INSENSITIVE
            );
            matcher = englishPatternReversed.matcher(input);
            if (matcher.find()) {
                int year = Integer.parseInt(matcher.group(1));
                String monthName = matcher.group(2);
                Integer month = getMonthFromName(monthName);
                if (month != null) {
                    return LocalDate.of(year, month, 1);
                }
            }
        } catch (Exception e) {
            // تجاهل
        }

        return null;
    }

    /**
     * تحويل اسم الشهر إلى رقم (1-12)
     * يدعم العربية والإنجليزية والمختصرات
     */
    private static Integer getMonthFromName(String monthName) {
        if (monthName == null || monthName.isEmpty()) {
            return null;
        }

        String lower = monthName.trim().toLowerCase();

        // أسماء الأشهر العربية
        switch (lower) {
            case "يناير":
            case "كانون الثاني":
            case "كانون يناير":
                return 1;
            case "فبراير":
            case "شباط":
                return 2;
            case "مارس":
            case "آذار":
                return 3;
            case "أبريل":
            case "نيسان":
                return 4;
            case "مايو":
            case "أيار":
                return 5;
            case "يونيو":
            case "حزيران":
                return 6;
            case "يوليو":
            case "تموز":
                return 7;
            case "أغسطس":
            case "آب":
                return 8;
            case "سبتمبر":
            case "أيلول":
                return 9;
            case "أكتوبر":
            case "تشرين الأول":
            case "تشرين اول":
                return 10;
            case "نوفمبر":
            case "تشرين الثاني":
            case "تشرين ثاني":
                return 11;
            case "ديسمبر":
            case "كانون الأول":
            case "كانون اول":
                return 12;
        }

        // أسماء الأشهر الإنجليزية والمختصرات
        try {
            // استخدام DateTimeFormatter للتعرف على أسماء الأشهر الإنجليزية
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);
            YearMonth yearMonth = YearMonth.parse("2000 " + lower,
                    DateTimeFormatter.ofPattern("yyyy MMM", Locale.ENGLISH));
            return yearMonth.getMonthValue();
        } catch (Exception e) {
            // محاولة المختصرات
            switch (lower) {
                case "jan":
                case "jan.":
                    return 1;
                case "feb":
                case "feb.":
                    return 2;
                case "mar":
                case "mar.":
                    return 3;
                case "apr":
                case "apr.":
                    return 4;
                case "may":
                    return 5;
                case "jun":
                case "jun.":
                    return 6;
                case "jul":
                case "jul.":
                    return 7;
                case "aug":
                case "aug.":
                    return 8;
                case "sep":
                case "sept":
                case "sep.":
                    return 9;
                case "oct":
                case "oct.":
                    return 10;
                case "nov":
                case "nov.":
                    return 11;
                case "dec":
                case "dec.":
                    return 12;
            }
        }

        return null;
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

    public static String toArabicMonthYear(LocalDate date) {
        if (date == null) {
            return null;
        }
        int month = date.getMonthValue(); // 1-12
        int year = date.getYear();
        return ARABIC_MONTH_NAMES.get(month - 1) + " " + year;
    }
}