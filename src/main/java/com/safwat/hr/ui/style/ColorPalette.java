package com.safwat.hr.ui.style;

import javafx.scene.paint.Color;

/**
 * Converts Theme hex strings to JavaFX Color objects and CSS-safe strings.
 */
public final class ColorPalette {

    public static Color primary()       { return Color.web(Theme.PRIMARY); }
    public static Color primaryLight()  { return Color.web(Theme.PRIMARY_LIGHT); }
    public static Color success()       { return Color.web(Theme.SUCCESS); }
    public static Color warning()       { return Color.web(Theme.WARNING); }
    public static Color error()         { return Color.web(Theme.ERROR); }
    public static Color info()          { return Color.web(Theme.INFO); }

    /** Convert hex string to rgba() for inline CSS */
    public static String toRgba(String hex, double alpha) {
        Color c = Color.web(hex);
        return String.format("rgba(%d,%d,%d,%.2f)",
            (int)(c.getRed()   * 255),
            (int)(c.getGreen() * 255),
            (int)(c.getBlue()  * 255),
            alpha);
    }

    private ColorPalette() {}
}
