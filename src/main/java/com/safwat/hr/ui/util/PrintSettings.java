package com.safwat.hr.ui.util;

import com.safwat.hr.shared.AppConfig;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * قراءة وحفظ آخر إعدادات الطباعة في قسم "printing" من app_config.json.
 */
public final class PrintSettings {

    private static final String SECTION = "printing";

    private static final String KEY_PRINTER = "printer";
    private static final String KEY_PAPER = "paper";
    private static final String KEY_SIDES = "sides";
    private static final String KEY_COPIES = "copies";
    private static final String KEY_ORIENTATION = "orientation";
    private static final String KEY_PAGE_RANGE = "pageRange";
    private static final String KEY_COLLATE = "collate";
    private static final String KEY_COLOR = "color";

    private PrintSettings() {
    }

    public static String getPrinter() {
        return AppConfig.getString(SECTION, KEY_PRINTER, "");
    }

    public static String getPaper() {
        return AppConfig.getString(SECTION, KEY_PAPER, "A4");
    }

    public static String getSides() {
        return AppConfig.getString(SECTION, KEY_SIDES, "ONE_SIDED");
    }

    public static int getCopies() {
        return AppConfig.getInt(SECTION, KEY_COPIES, 1);
    }

    public static String getOrientation() {
        return AppConfig.getString(SECTION, KEY_ORIENTATION, "PORTRAIT");
    }

    public static String getPageRange() {
        return AppConfig.getString(SECTION, KEY_PAGE_RANGE, "ALL");
    }

    public static boolean isCollate() {
        return AppConfig.getBoolean(SECTION, KEY_COLLATE, true);
    }

    public static String getColor() {
        return AppConfig.getString(SECTION, KEY_COLOR, "COLOR");
    }

    /**
     * يحفظ كل الإعدادات دفعة واحدة في ملف واحد.
     */
    public static void save(String printerName, String paperName, String sidesName, int copies,
                            String orientationName, String pageRange, boolean collate, String colorName) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(SECTION + "." + KEY_PRINTER, printerName);
        values.put(SECTION + "." + KEY_PAPER, paperName);
        values.put(SECTION + "." + KEY_SIDES, sidesName);
        values.put(SECTION + "." + KEY_COPIES, copies);
        values.put(SECTION + "." + KEY_ORIENTATION, orientationName);
        values.put(SECTION + "." + KEY_PAGE_RANGE, pageRange);
        values.put(SECTION + "." + KEY_COLLATE, collate);
        values.put(SECTION + "." + KEY_COLOR, colorName);
        AppConfig.setValues(values);
    }
}