package com.safwat.hr.ui.theme;

import com.safwat.hr.shared.AppConfig;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ThemeEventBus {

    // ══ ثوابت ملفات الثيمات ═══════════════════════════════════
    public static final String BLACK = "theme-black.css";
    public static final String BLUE = "theme-blue.css";
    public static final String DARK_1 = "theme-dark-1.css";
    public static final String DARK_2 = "theme-dark-2.css";
    public static final String GRAY = "theme-gray.css";
    public static final String GREEN = "theme-green.css";
    public static final String INDIGO = "theme-indigo.css";
    public static final String LIGHT = "theme-light.css";
    public static final String LIGHT_BLUE = "theme-lightblue.css";
    public static final String OLIVE = "theme-olive.css";
    public static final String PASTEL = "theme-pastel.css";
    public static final String TEAL = "theme-teal.css";
    public static final String WARM = "theme-warm.css";
    public static final String PEPSI = "theme-pepsi.css";
    public static final String CUSTOM = "custom-theme.css";

    private static final String BASE_PATH = "/com/safwat/hr/css/";

    // ══ مسارات الخطوط ════════════════════════════════════════
    private static final String FONTS_BASE = "/com/safwat/hr/fonts/";
    private static final String NOTO_BASE = FONTS_BASE + "noto/";

    // Cairo fonts (قائمة مخففة، لكن يمكنك الاحتفاظ بها كخيار ثانوي)
    private static final String[] CAIRO_FILES = {
            "Cairo-Regular.ttf",
            "Cairo-Medium.ttf",
            "Cairo-SemiBold.ttf",
            "Cairo-Bold.ttf",
            "Cairo-ExtraBold.ttf"
    };


    // ══ الحالة الداخلية ════════════════════════════════════════
    private static final List<WeakReference<Scene>> registeredScenes = new ArrayList<>();
    private static String currentTheme = LIGHT;
    private static boolean fontsLoaded = false;

    // اسم العائلة الذي سيُستخدم في CSS كأولوية أولى
    public static final String DEFAULT_FONT_FAMILY = "Cairo-Regular.ttf";

    private ThemeEventBus() {
    }

    // ══ تحميل جميع الخطوط ══════════════════════════════════════

    public static synchronized void loadFonts() {
        if (fontsLoaded) return;

        // تحميل Cairo (اختياري، يمكنك إزالتها إذا استغنيت عنها)
        for (String file : CAIRO_FILES) {
            loadFont(FONTS_BASE + file);
        }

        // تحميل Noto Sans Arabic
        // for (String file : NOTO_FILES) {
        // loadFont(NOTO_BASE + file);
        //  }

        fontsLoaded = true;

    }

    private static void loadFont(String path) {
        try {
            Font font = Font.loadFont(
                    ThemeEventBus.class.getResourceAsStream(path), 13);
            if (font == null) {
                System.err.println("⚠ تعذّر تحميل الخط: " + path);
            }
        } catch (Exception e) {
            System.err.println("⚠ خطأ في تحميل الخط " + path + ": " + e.getMessage());
        }
    }

    // ══ تطبيق الثيم على Scene / Parent ═════════════════════════

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

    public static void applyTheme(Parent root, String themeFile) {
        loadFonts();
        root.getStylesheets().clear();
        String url = Objects.requireNonNull(
                ThemeEventBus.class.getResource(BASE_PATH + themeFile),
                "Theme not found: " + themeFile
        ).toExternalForm();
        root.getStylesheets().add(url);
        root.setStyle("-fx-font-family: \"" + DEFAULT_FONT_FAMILY + "\", Cairo, sans-serif;");
        AppConfig.setValue("ui", "theme", themeFile);
    }

    // ══ تسجيل Scene ════════════════════════════════════════════

    public static synchronized void register(Scene scene) {
        if (scene == null) return;
        cleanDead();
        for (WeakReference<Scene> ref : registeredScenes) {
            if (ref.get() == scene) return;
        }
        registeredScenes.add(new WeakReference<>(scene));
        applySafely(scene, currentTheme);
    }

    public static synchronized void unregister(Scene scene) {
        registeredScenes.removeIf(ref -> ref.get() == null || ref.get() == scene);
    }

    // ══ تبديل الثيم على كل الشاشات ═════════════════════════════

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

    public static String getCurrentTheme() {
        return currentTheme;
    }

    public static void initFromConfig() {
        currentTheme = AppConfig.getString("ui", "theme", LIGHT);
        // loadFonts(); // تحميل الخطوط مبكراً
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