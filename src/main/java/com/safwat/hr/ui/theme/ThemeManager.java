package com.safwat.hr.ui.theme;

import com.safwat.hr.shared.AppConfig;
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



    public static final String Black = "theme-black.css";
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
        AppConfig.setValue("ui", "theme",themeFile);
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
        AppConfig.setValue("ui", "theme",themeFile);
    }

    /**
     *
     * @param scene .
     */
    public static void applyDefault(Scene scene) {
        applyTheme(scene, LIGHT);
    }
}