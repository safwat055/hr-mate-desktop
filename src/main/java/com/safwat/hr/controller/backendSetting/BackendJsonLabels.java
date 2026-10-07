package com.safwat.hr.controller.backendSetting;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * خريطة المفاتيح الإنجليزية → العربية لملف app_config.json للباك إند.
 * البنية: 2 مستويات كحد أقصى (section.key).
 */
public final class BackendJsonLabels {

    /**
     * التسميات العربية للمفاتيح (section.key)
     */
    private static final Map<String, String> LABELS = new LinkedHashMap<>();

    /**
     * أسماء الأقسام بالعربية (بأيقونة)
     */
    private static final Map<String, String> SECTION_TITLES = new LinkedHashMap<>();

    static {
        // ── الأقسام ──
        SECTION_TITLES.put("setting", "⚙  الإعدادات العامة");
        SECTION_TITLES.put("reward", "🎁  المكافآت");
        SECTION_TITLES.put("basic", "🏛  البيانات الأساسية");
        SECTION_TITLES.put("startupScripts", "تحميل سكريبتات");

        // ── setting ──
        LABELS.put("setting.scaleUp", "التقريب لاعلى");
        LABELS.put("setting.upgradeFirst", "الترقية أولاً");

        // ── reward ──
        LABELS.put("reward.dayCount_1", "عدد أيام المكافأة (1)");
        LABELS.put("reward.dayPercent_1", "نسبة المكافأة % (1)");
        LABELS.put("reward.dayCount_2", "عدد أيام المكافأة (2)");
        LABELS.put("reward.dayPercent_2", "نسبة المكافأة % (2)");

        // ── basic ──
        LABELS.put("basic.government", "المحافظة");
        LABELS.put("basic.organize", "المديرية / الهيئة");
        LABELS.put("basic.sectorName", "اسم القطاع");
        LABELS.put("basic.prefix", "بادئة الترقيم");

        // ── startupScripts ──
        LABELS.put("startupScripts.enabled", "مُفعّل");
        LABELS.put("startupScripts.folder", "مجلد السكريبتات");
    }

    private BackendJsonLabels() {
    }

    public static String label(String path) {
        return LABELS.getOrDefault(path, path);
    }

    public static String sectionTitle(String section) {
        return SECTION_TITLES.getOrDefault(section, "⚙  " + section);
    }

    public static boolean hasLabel(String path) {
        return LABELS.containsKey(path);
    }
}