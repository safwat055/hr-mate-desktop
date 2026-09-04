package com.safwat.hr.ui.theme;

import com.safwat.hr.shared.AppConfig;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * =====================================================
 * ThemeEventBus — إدارة الثيمات بالكامل في HR MATE
 * =====================================================
 * <p>
 * كلاس موحّد يجمع:
 *  1) ثوابت ملفات الثيمات (بدل ThemeManager)
 *  2) تحميل خطوط Cairo مرة واحدة طوال عمر التطبيق (بدل AppTheme)
 *  3) Bus لتطبيق الثيم فورًا على كل الـ Scenes المفتوحة (ThemeEventBus)
 * <p>
 * الاستخدام:
 * <pre>
 *   // عند بدء التطبيق (في Main أو App)
 *   ThemeEventBus.initFromConfig();
 *
 *   // عند فتح أي شاشة / Dialog
 *   Scene scene = new Scene(root);
 *   ThemeEventBus.register(scene);   // بتطبّق الثيم الحالي فورًا
 *
 *   // من شاشة الإعدادات — تبديل فوري على كل الشاشات + حفظ
 *   ThemeEventBus.applyTheme(ThemeEventBus.DARK_1);
 * </pre>
 * <p>
 * ملاحظات:
 * - يستخدم WeakReference حتى لا يمنع الـ GC من تنظيف الـ Scenes المغلقة.
 * - يحفظ الثيم الحالي في AppConfig تلقائيًا.
 * - آمن للاستدعاء من أي Thread.
 * - لا تستخدم @font-face في CSS مع JavaFX — تحميل الخطوط هنا بـ Font.loadFont() فقط.
 */
public final class ThemeEventBus {

    // ══ ثوابت ملفات الثيمات ═══════════════════════════════════
    public static final String BLACK      = "theme-black.css";
    public static final String BLUE       = "theme-blue.css";
    public static final String DARK_1     = "theme-dark-1.css";
    public static final String DARK_2     = "theme-dark-2.css";
    public static final String GRAY       = "theme-gray.css";
    public static final String GREEN      = "theme-green.css";
    public static final String INDIGO     = "theme-indigo.css";
    public static final String LIGHT      = "theme-light.css";
    public static final String LIGHT_BLUE = "theme-lightblue.css";
    public static final String OLIVE      = "theme-olive.css";
    public static final String PASTEL     = "theme-pastel.css";
    public static final String TEAL       = "theme-teal.css";
    public static final String WARM       = "theme-warm.css";
    public static final String PEPSI      = "theme-pepsi.css";

    private static final String BASE_PATH = "/com/safwat/hr/css/";

    // ══ مسارات خطوط Cairo ══════════════════════════════════════
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

    // ══ الحالة الداخلية ════════════════════════════════════════
    private static final List<WeakReference<Scene>> registeredScenes = new ArrayList<>();
    private static String currentTheme = LIGHT;
    private static boolean fontsLoaded = false;

    private ThemeEventBus() {}

    // ══ تحميل الخطوط ═══════════════════════════════════════════

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
                        ThemeEventBus.class.getResourceAsStream(path), 13);
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

    // ══ تطبيق الثيم على Scene / Parent مباشرة ══════════════════

    /**
     * يطبّق ثيم على Scene محددة (بدون تسجيلها في الـ Bus).
     * بيحمّل الخطوط تلقائيًا لو لسه ما اتحملوش.
     */
    public static void applyTheme(Scene scene, String themeFile) {
        loadFonts();
        scene.getStylesheets().clear();
        String url = Objects.requireNonNull(
                ThemeEventBus.class.getResource(BASE_PATH + themeFile),
                "Theme not found: " + themeFile
        ).toExternalForm();
        scene.getStylesheets().add(url);
        AppConfig.setValue("ui", "theme", themeFile);
    }

    /**
     * يطبّق ثيم على Parent (FXML root) — للاستخدام في Dialogs السريعة.
     */
    public static void applyTheme(Parent root, String themeFile) {
        loadFonts();
        root.getStylesheets().clear();
        String url = Objects.requireNonNull(
                ThemeEventBus.class.getResource(BASE_PATH + themeFile),
                "Theme not found: " + themeFile
        ).toExternalForm();
        root.getStylesheets().add(url);
        AppConfig.setValue("ui", "theme", themeFile);
    }

    // ══ تسجيل Scene في الـ Bus ═════════════════════════════════

    /**
     * تسجيل Scene جديدة.
     * بيطبّق عليها الثيم الحالي فورًا — استخدمها عند فتح أي شاشة أو Dialog.
     */
    public static synchronized void register(Scene scene) {
        if (scene == null) return;

        cleanDead();

        // التحقق من عدم التسجيل المزدوج
        for (WeakReference<Scene> ref : registeredScenes) {
            if (ref.get() == scene) return;
        }

        registeredScenes.add(new WeakReference<>(scene));

        // تطبيق الثيم الحالي على الـ Scene الجديدة فورًا
        applySafely(scene, currentTheme);
    }

    /**
     * إلغاء تسجيل Scene (اختياري — بيحصل تلقائيًا عند GC).
     */
    public static synchronized void unregister(Scene scene) {
        registeredScenes.removeIf(ref -> ref.get() == null || ref.get() == scene);
    }

    // ══ تبديل الثيم على كل الشاشات ═════════════════════════════

    /**
     * تطبيق ثيم جديد على كل الـ Scenes المسجّلة فورًا + حفظه في AppConfig.
     * آمن للاستدعاء من أي Thread.
     */
    public static synchronized void applyTheme(String theme) {
        if (theme == null || theme.equals(currentTheme)) return;

        currentTheme = theme;
        AppConfig.setValue("ui", "theme", theme);

        cleanDead();

        List<Scene> alive = getAliveScenes();
        if (Platform.isFxApplicationThread()) {
            alive.forEach(s -> applySafely(s, theme));
        } else {
            Platform.runLater(() -> alive.forEach(s -> applySafely(s, theme)));
        }
    }

    /** الثيم الحالي المطبّق. */
    public static String getCurrentTheme() {
        return currentTheme;
    }

    /** تهيئة الثيم الحالي من AppConfig (يُستدعى عند بدء التطبيق). */
    public static void initFromConfig() {
        currentTheme = AppConfig.getString("ui", "theme", LIGHT);
    }

    // ══ helpers ════════════════════════════════════════════════

    private static void applySafely(Scene scene, String theme) {
        try {
            applyTheme(scene, theme);
        } catch (Exception e) {
            System.err.println("[ThemeEventBus] خطأ في تطبيق الثيم: " + e.getMessage());
        }
    }

    private static void cleanDead() {
        registeredScenes.removeIf(ref -> ref.get() == null);
    }

    private static List<Scene> getAliveScenes() {
        List<Scene> result = new ArrayList<>();
        for (WeakReference<Scene> ref : registeredScenes) {
            Scene s = ref.get();
            if (s != null) result.add(s);
        }
        return result;
    }
}