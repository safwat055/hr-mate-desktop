package com.safwat.hr.theme;

import javafx.scene.Parent;
import javafx.scene.Scene;

import java.util.Objects;

/**
 * مساعد تبديل الثيمات (Themes) في HR MATE.
 *
 * <p>الاستخدام:</p>
 * <pre>{@code
 * // عند فتح أي شاشة
 * ThemeManager.applyTheme(scene, ThemeManager.DEFAULT);
 *
 * // أو من الإعدادات
 * ThemeManager.applyTheme(scene, userPref.getTheme());
 * }</pre>
 */
public class ThemeManager {

    public static final String DEFAULT = "theme-light.css";
    public static final String DARK = "theme-dark.css";
    public static final String BLUE = "theme-blue.css";
    public static final String GRAY = "theme-gray.css";
    public static final String GREEN = "theme-green.css";
    private static final String BASE_PATH = "/com/safwat/hr/css/";

    /**
     * يطبّق ثيم على Scene.
     */
    public static void applyTheme(Scene scene, String themeFile) {
        scene.getStylesheets().clear();
        String url = Objects.requireNonNull(
                ThemeManager.class.getResource(BASE_PATH + themeFile),
                "Theme not found: " + themeFile
        ).toExternalForm();
        scene.getStylesheets().add(url);
    }

    /**
     * يطبّق ثيم على Parent (FXML root).
     */
    public static void applyTheme(Parent root, String themeFile) {
        root.getStylesheets().clear();
        String url = Objects.requireNonNull(
                ThemeManager.class.getResource(BASE_PATH + themeFile),
                "Theme not found: " + themeFile
        ).toExternalForm();
        root.getStylesheets().add(url);
    }

    /**
     * يطبّق الثيم الافتراضي.
     */
    public static void applyDefault(Scene scene) {
        applyTheme(scene, DEFAULT);
    }
}