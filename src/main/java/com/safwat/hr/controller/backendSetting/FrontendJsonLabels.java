package com.safwat.hr.controller.backendSetting;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * ══════════════════════════════════════════════════════════════════
 * FrontendJsonLabels — تسميات + أقسام مستثناة لـ app_config.json (الفرونت)
 * ══════════════════════════════════════════════════════════════════
 * <p>
 * أقسام مثل {@code paths} لا تظهر في الواجهة لأنها تُدار تلقائياً
 * بواسطة {@code PathResolver} و {@code SetupWizardService}.
 */
public final class FrontendJsonLabels {

    /**
     * الأقسام المستثناة من الظهور في الواجهة.
     * <p>
     * <b>لا تُحذف من الملف</b> — فقط لا تُعرَض ولا تُحرَّر من قِبَل المستخدم.
     */
    public static final Set<String> HIDDEN_SECTIONS = Set.of(
            "paths", "ui", "meta"
    );

    private static final Map<String, String> SECTION_TITLES = new LinkedHashMap<>();
    private static final Map<String, String> LABELS = new LinkedHashMap<>();

    static {
        // ── الأقسام الظاهرة ──
        SECTION_TITLES.put("connection", "🔌  الاتصال");
        SECTION_TITLES.put("ui", "🎨  الواجهة");
        SECTION_TITLES.put("notifications", "🔔  الإشعارات");

        // ── connection ──
        LABELS.put("connection.url", "بروتوكول الاتصال");
        LABELS.put("connection.url2", "بروتوكول WebSocket");
        LABELS.put("connection.port", "منفذ الباك إند");
        LABELS.put("connection.masterPC", "عنوان جهاز الماستر");
        LABELS.put("connection.pgPort", "منفذ PostgreSQL");
        LABELS.put("connection.user", "آخر مستخدم مسجَّل");
        LABELS.put("connection.alone", "وضع التشغيل الفردي");

        // ── ui ──
        LABELS.put("ui.theme", "الثيم الحالي");

        // ── notifications ──
        LABELS.put("notifications.reportsEnabled", "تفعيل إشعارات التقارير");
    }

    private FrontendJsonLabels() {
    }

    public static String sectionTitle(String section) {
        return SECTION_TITLES.getOrDefault(section, "⚙  " + section);
    }

    public static String label(String path) {
        return LABELS.getOrDefault(path, path);
    }

    public static boolean hasLabel(String path) {
        return LABELS.containsKey(path);
    }

    public static boolean isHidden(String section) {
        return HIDDEN_SECTIONS.contains(section);
    }
}