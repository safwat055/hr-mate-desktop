package com.safwat.hr.ui.theme;

import com.safwat.hr.shared.AppConfig;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;

/**
 * =====================================================
 * AppTheme — تحميل الخطوط وتطبيق الستايل على كل واجهات التطبيق
 * =====================================================
 * <p>
 * بيحمّل كل أوزان خط Cairo مرة واحدة طوال عمر التطبيق،
 * ثم يطبّق common.css على أي Scene (الرئيسية + أي Dialog).
 * <p>
 * الاستخدام:
 * <pre>
 * Scene scene = new Scene(root);
 * AppTheme.apply(scene);
 * </pre>
 * ملاحظة: لا تستخدم @font-face في CSS مع JavaFX — مش مدعوم.
 * تحميل الخطوط لازم يكون هنا بـ Font.loadFont() فقط.
 */
public final class AppTheme {

    // ── مسارات خطوط Cairo ────────────────────────────────────────
    private static final String FONTS_BASE = "/com/safwat/hr/fonts/";
    private static final String[] CAIRO_FILES = {
            "Cairo-ExtraLight.ttf",
            "Cairo-Light.ttf",
            "Cairo-Regular.ttf",
            "Cairo-Medium.ttf",
            "Cairo-SemiBold.ttf",
            "Cairo-Bold.ttf",
            "Cairo-ExtraBold.ttf",
            "Cairo-Black.ttf",
    };

    private static boolean fontsLoaded = false;

    private AppTheme() {}

    // ── تحميل الخطوط ─────────────────────────────────────────────

    /**
     * بيحمّل كل أوزان Cairo مرة واحدة فقط طوال عمر التطبيق.
     * آمن يتنادى أكتر من مرة.
     */
    public static synchronized void loadFonts() {
        if (fontsLoaded) return;

        for (String file : CAIRO_FILES) {
            String path = FONTS_BASE + file;
            try {
                Font font = Font.loadFont(
                        AppTheme.class.getResourceAsStream(path), 13);
                if (font == null) {
                    System.err.println("⚠ تعذّر تحميل الخط: " + path);
                }
            } catch (Exception e) {
                System.err.println("⚠ خطأ أثناء تحميل الخط " + path
                        + ": " + e.getMessage());
            }
        }

        fontsLoaded = true;
    }

    // ── تطبيق الثيم على Scene ────────────────────────────────────

    /**
     * لازم تتنادى على كل Scene بتتعمل في التطبيق (الرئيسية + الـ Dialogs).
     * بتحمّل الخطوط تلقائيًا لو لسه ما اتحملوش.
     */
    public static void apply(Scene scene, String themeFile) {
        loadFonts();

      ThemeManager.applyTheme(scene, themeFile);

    }
    public static void apply(Parent scene, String themeFile) {
        loadFonts();

      ThemeManager.applyTheme(scene, themeFile);

    }
}