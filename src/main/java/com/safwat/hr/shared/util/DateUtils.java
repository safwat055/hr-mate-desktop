package com.safwat.hr.shared.util;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.safwat.hr.shared.util.StringUtil.convertArabicToEnglishNumbers;

public class DateUtils {

    // ══════════════════════════════════════════════════════════════
    //  الأشهر العربية — كل الاختلافات المدعومة
    // ══════════════════════════════════════════════════════════════
    private static final Map<String, Integer> ARABIC_MONTH_MAP;
    private static final List<String> ARABIC_MONTHS_DISPLAY = Arrays.asList(
            "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
            "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    );

    static {
        Map<String, Integer> map = new LinkedHashMap<>();

        putAll(map, 1, "يناير", "كانون الثاني", "كانون يناير", "كانون2", "كانون 2");
        putAll(map, 2, "فبراير", "شباط");
        putAll(map, 3, "مارس", "آذار", "اذار");
        putAll(map, 4, "أبريل", "ابريل", "نيسان");
        putAll(map, 5, "مايو", "أيار", "ايار");
        putAll(map, 6, "يونيو", "يونيه", "يونية", "حزيران");
        putAll(map, 7, "يوليو", "يوليه", "يولية", "تموز");
        putAll(map, 8, "أغسطس", "اغسطس", "آب", "اب");
        putAll(map, 9, "سبتمبر", "أيلول", "ايلول");
        putAll(map, 10, "أكتوبر", "اكتوبر", "تشرين الأول", "تشرين الاول", "تشرين1", "تشرين 1");
        putAll(map, 11, "نوفمبر", "تشرين الثاني", "تشرين2", "تشرين 2");
        putAll(map, 12, "ديسمبر", "كانون الأول", "كانون الاول", "كانون1", "كانون 1");

        ARABIC_MONTH_MAP = Collections.unmodifiableMap(map);
    }

    // ══════════════════════════════════════════════════════════════
    //  Normalization: إزالة الهمزة والتاء المربوطة والتشكيل
    // ══════════════════════════════════════════════════════════════

    private static void putAll(Map<String, Integer> map, int monthNumber, String... names) {
        for (String name : names) {
            map.put(normalizeArabicText(name), monthNumber);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  getFirstDayOfMonth — الشكل المرن
    // ══════════════════════════════════════════════════════════════

    /**
     * تطبيع النص العربي للمقارنة الآمنة.
     * <ul>
     *   <li>الهمزة → ألف (أ/إ/آ → ا)</li>
     *   <li>التاء المربوطة → هاء (ة → ه)</li>
     *   <li>إزالة التشكيل</li>
     *   <li>إزالة المسافات الزائدة</li>
     *   <li>حروف صغيرة</li>
     * </ul>
     */
    public static String normalizeArabicText(String input) {
        if (input == null) return "";
        String s = input.trim().toLowerCase();
        // إزالة التشكيل
        s = s.replaceAll("[\u064B-\u065F\u0670\u0640]", "");
        // الهمزة والألف المتنوعة → ألف عادية
        s = s.replace('أ', 'ا')
                .replace('إ', 'ا')
                .replace('آ', 'ا')
                .replace('ٱ', 'ا');
        // التاء المربوطة → هاء
        s = s.replace('ة', 'ه');
        // إزالة المسافات الزائدة
        s = s.replaceAll("\s+", "");
        return s;
    }

    /**
     * تحويل أي نص يحتوي على شهر وسنة إلى LocalDate لأول يوم في الشهر.
     * <p>يدعم:
     * <ul>
     *   <li>الأرقام: "2 2025", "02/2025", "2025-02"</li>
     *   <li>العربي: "يونيو 2025", "يوليه 2025", "أغسطس 2025", "اكتوبر 2025"</li>
     *   <li>الإنجليزي: "Jun 2025", "July 2025"</li>
     *   <li>أي ترتيب: "2025 يونيو" أو "يونيو 2025"</li>
     *   <li>الأرقام العربية: "٢٠٢٥ يونيو"</li>
     * </ul>
     *
     * @param monthYear النص المدخل
     * @return LocalDate لأول يوم في الشهر، أو null إذا فشل التحويل
     */
    public static LocalDate getFirstDayOfMonth(String monthYear) {
        if (monthYear == null || monthYear.trim().isEmpty()) {
            return null;
        }

        // 1) تحويل الأرقام العربية → إنجليزية
        String input = convertArabicToEnglishNumbers(monthYear.trim());

        // 2) محاولة الصيغ الرقمية المباشرة (M/yyyy, MM-yyyy, ...)
        LocalDate numeric = tryNumericFormats(input);
        if (numeric != null) return numeric;

        // 3) استخراج السنة والشهر من النص
        return tryExtractMonthAndYear(input);
    }

    // ── محاولة الصيغ الرقمية ──
    private static LocalDate tryNumericFormats(String input) {
        String[][] patterns = {
                {"M yyyy", "yyyy M"},
                {"MM yyyy", "yyyy MM"},
                {"M-yyyy", "yyyy-M"},
                {"MM-yyyy", "yyyy-MM"},
                {"M/yyyy", "yyyy/M"},
                {"MM/yyyy", "yyyy/MM"},
                {"M.yyyy", "yyyy.M"},
                {"MM.yyyy", "yyyy.MM"},
        };

        for (String[] pair : patterns) {
            for (String p : pair) {
                try {
                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern(p);
                    YearMonth ym = YearMonth.parse(input, fmt);
                    return ym.atDay(1);
                } catch (DateTimeParseException ignored) {
                }
            }
        }
        return null;
    }

    // ── استخراج السنة والشهر من نص مختلط ──
    private static LocalDate tryExtractMonthAndYear(String input) {
        try {
            // استخراج السنة (أول رباعي)
            Matcher yearMatcher = Pattern.compile("(\\d{4})").matcher(input);
            Integer year = null;
            if (yearMatcher.find()) {
                year = Integer.parseInt(yearMatcher.group(1));
            }

            // استخراج اسم الشهر (كل ما ليس رقماً أو رمز تاريخ)
            String textOnly = input.replaceAll("[0-9\\-\\/\\.\\s]+", " ").trim();
            if (textOnly.isEmpty()) return null;

            // قد يكون هناك أكثر من كلمة، نجرب كل كلمة
            String[] words = textOnly.split("\\s+");
            Integer month = null;
            for (String word : words) {
                month = resolveMonth(word);
                if (month != null) break;
            }

            if (month != null && year != null) {
                return LocalDate.of(year, month, 1);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    // ── تحويل اسم الشهر إلى رقم (1-12) مع Normalization ──
    private static Integer resolveMonth(String monthName) {
        if (monthName == null || monthName.isBlank()) return null;

        String normalized = normalizeArabicText(monthName);

        // 1) البحث في الأشهر العربية (normalized)
        Integer arabic = ARABIC_MONTH_MAP.get(normalized);
        if (arabic != null) return arabic;

        // 2) البحث في الأشهر الإنجليزية
        return resolveEnglishMonth(normalized);
    }

    // ══════════════════════════════════════════════════════════════
    //  آخر يوم في الشهر
    // ══════════════════════════════════════════════════════════════

    private static Integer resolveEnglishMonth(String normalized) {
        // نحاول DateTimeFormatter أولاً
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);
            YearMonth ym = YearMonth.parse("2000 " + normalized,
                    DateTimeFormatter.ofPattern("yyyy MMM", Locale.ENGLISH));
            return ym.getMonthValue();
        } catch (Exception ignored) {
        }

        // المختصرات اليدوية
        switch (normalized) {
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
            default:
                return null;
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  تحويل LocalDate → نص عربي
    // ══════════════════════════════════════════════════════════════

    public static LocalDate getLastDayOfMonth(String monthYear) {
        LocalDate first = getFirstDayOfMonth(monthYear);
        if (first == null) return null;
        return YearMonth.from(first).atEndOfMonth();
    }

    public static String toArabicMonthYear(LocalDate date) {
        if (date == null) return null;
        return ARABIC_MONTHS_DISPLAY.get(date.getMonthValue() - 1) + " " + date.getYear();
    }

    public static String toMonthYearNumber(LocalDate date) {
        if (date == null) return null;
        return date.getMonthValue() + " " + date.getYear();
    }

    // ══════════════════════════════════════════════════════════════
    //  parseDate — صيغتان فقط
    // ══════════════════════════════════════════════════════════════

    public static LocalDate parseDate(String date) {
        if (date == null || date.isBlank()) return null;
        date = date.trim();
        try {
            if (date.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } else if (date.matches("\\d{2}-\\d{2}-\\d{4}")) {
                return LocalDate.parse(date, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            }
        } catch (DateTimeParseException ignored) {
        }
        return null;
    }

    // ══════════════════════════════════════════════════════════════
    //  fromArabicMonthYear — الصيغة الصارمة "شهر سنة"
    // ══════════════════════════════════════════════════════════════

    public static LocalDate fromArabicMonthYear(String value) {
        if (value == null || value.isBlank()) return null;
        String[] parts = value.trim().split("\\s+");
        if (parts.length != 2) return null;

        Integer month = resolveMonth(parts[0]);
        if (month == null) return null;

        Integer year = extractYear(parts[1]);
        if (year == null) return null;

        return LocalDate.of(year, month, 1);
    }

    public static LocalDate fromArabicMonthYearFlexible(String value) {
        if (value == null || value.isBlank()) return null;
        return getFirstDayOfMonth(value);
    }

    private static Integer extractYear(String text) {
        try {
            String clean = convertArabicToEnglishNumbers(text).replaceAll("[^0-9]", "");
            if (clean.length() == 4) return Integer.parseInt(clean);
        } catch (Exception ignored) {
        }
        return null;
    }
}