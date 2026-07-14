package com.safwat.hr.modernUi.ui.style;

import javafx.scene.Scene;

/**
 * ─────────────────────────────────────────────────────────────
 * Theme — نقطة تحكم واحدة في كل الألوان والخطوط والـ theme.
 * <p>
 * لتغيير المظهر الكامل للتطبيق: غيّر هنا فقط.
 * ─────────────────────────────────────────────────────────────
 */
public final class Theme {

    // ── Brand ────────────────────────────────────────────────
    public static final String PRIMARY = "#1565C0";
    public static final String PRIMARY_LIGHT = "#1E88E5";
    public static final String PRIMARY_DARK = "#0D47A1";

    public static final String SECONDARY = "#00897B";

    // ── Semantic ─────────────────────────────────────────────
    public static final String SUCCESS = "#2E7D32";
    public static final String SUCCESS_LIGHT = "#43A047";
    public static final String WARNING = "#E65100";
    public static final String WARNING_LIGHT = "#FB8C00";
    public static final String ERROR = "#C62828";
    public static final String ERROR_LIGHT = "#E53935";
    public static final String INFO = "#01579B";

    // ── Neutrals ─────────────────────────────────────────────
    public static final String SURFACE = "#FFFFFF";
    public static final String BACKGROUND = "#F5F5F5";
    public static final String ON_SURFACE = "#212121";
    public static final String HINT = "#9E9E9E";
    public static final String DIVIDER = "#E0E0E0";
    public static final String ON_PRIMARY = "#FFFFFF";

    // ── Typography ───────────────────────────────────────────
    public static final String FONT_FAMILY = "Segoe UI";
    public static final double FONT_SM = 11.0;
    public static final double FONT_MD = 13.0;
    public static final double FONT_LG = 15.0;
    public static final double FONT_XL = 18.0;

    private Theme() {
    }

    /**
     * استدعِ هذا من Main.start() مرة واحدة بعد إنشاء الـ Scene.
     *
     * <pre>
     *   Scene scene = new Scene(root);
     *   Theme.applyOn(scene);
     * </pre>
     */
    public static void applyOn(Scene scene) {
        // يطبق الـ DEFAULT theme (يشمل كل controls MaterialFX)
        // + LEGACY لدعم الكنترولز العادية JavaFX داخل نفس الـ Scene
        //  MFXThemeManager.addOn(scene, Themes.DEFAULT, Themes.LEGACY);
    }
}
