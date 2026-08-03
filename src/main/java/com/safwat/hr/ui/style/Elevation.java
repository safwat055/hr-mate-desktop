package com.safwat.hr.ui.style;

/**
 * Material elevation shadow tokens.
 * Used as -fx-effect values in inline CSS.
 */
public final class Elevation {

    /**
     * No shadow
     */
    public static final String NONE = "none";

    /**
     * Subtle lift — cards, panels
     */
    public static final String E1 =
            "dropshadow(gaussian, rgba(0,0,0,0.12), 4, 0, 0, 1)";

    /**
     * Standard card shadow
     */
    public static final String E2 =
            "dropshadow(gaussian, rgba(0,0,0,0.16), 8, 0, 0, 2)";

    /**
     * Dialogs, popovers
     */
    public static final String E3 =
            "dropshadow(gaussian, rgba(0,0,0,0.20), 16, 0, 0, 4)";

    /**
     * Notifications, floating elements
     */
    public static final String E4 =
            "dropshadow(gaussian, rgba(0,0,0,0.24), 24, 0, 0, 6)";

    private Elevation() {
    }
}
