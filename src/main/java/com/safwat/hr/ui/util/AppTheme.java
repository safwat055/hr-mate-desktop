package com.safwat.hr.ui.util;

import javafx.scene.Scene;
import javafx.scene.text.Font;

/**
 * =====================================================
 * AppTheme — تطبيق خط وستايل موحّد على كل واجهات التطبيق
 * =====================================================
 * <p>
 * بيحل مشكلة تشوه الحروف الخاصة (مثل - / ,) وسط النص العربي
 * عن طريق تحميل خط واحد يدعم العربي والرموز بشكل صحيح،
 * وتطبيقه على أي Scene في التطبيق (الشاشة الرئيسية + أي Dialog).
 * <p>
 * الاستخدام:
 * <pre>
 * Scene scene = new Scene(root);
 * AppTheme.apply(scene);
 * </pre>
 */
public final class AppTheme {

    // ضع ملفات الخط هنا: src/main/resources/com/safwat/hr/fonts/
    private static final String FONT_REGULAR = "/com/safwat/hr/fonts/Cairo-Regular.ttf";
    private static final String FONT_MEDIUM = "/com/safwat/hr/fonts/Cairo-Medium.ttf";
    private static final String FONT_BOLD = "/com/safwat/hr/fonts/Cairo-Bold.ttf";

    // ملف الستايل الموحّد: src/main/resources/com/safwat/hr/css/global.css
    private static final String GLOBAL_CSS = "/com/safwat/hr/css/global.css";

    private static boolean fontsLoaded = false;

    private AppTheme() {
    }

    /**
     * تحميل الخطوط مرة واحدة فقط طوال عمر التطبيق
     */
    public static synchronized void loadFonts() {
        if (fontsLoaded) return;

        loadFontSafely(FONT_REGULAR);
        loadFontSafely(FONT_MEDIUM);
        loadFontSafely(FONT_BOLD);

        fontsLoaded = true;
    }

    private static void loadFontSafely(String path) {
        try {
            Font font = Font.loadFont(AppTheme.class.getResourceAsStream(path), 13);
            if (font == null) {
                System.err.println("⚠ تعذّر تحميل الخط: " + path + " (تأكد من مكان الملف)");
            }
        } catch (Exception e) {
            System.err.println("⚠ خطأ أثناء تحميل الخط " + path + ": " + e.getMessage());
        }
    }

    /**
     * يطبّق الخط والستايل الموحّد على أي Scene.
     * لازم تتنادى على كل Scene بتتعمل في التطبيق (الرئيسية + الـ Dialogs).
     */
    public static void apply(Scene scene) {
        loadFonts();
        try {
            scene.getStylesheets().add(
                    AppTheme.class.getResource(GLOBAL_CSS).toExternalForm()
            );
        } catch (Exception e) {
            System.err.println("⚠ تعذّر تحميل global.css: " + e.getMessage());
        }
    }
}