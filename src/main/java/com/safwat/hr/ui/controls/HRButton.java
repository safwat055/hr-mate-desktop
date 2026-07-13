package com.safwat.hr.ui.controls;

import com.safwat.hr.ui.style.Elevation;
import com.safwat.hr.ui.style.Radius;
import com.safwat.hr.ui.style.Theme;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;

/**
 * HRButton — Material-style button factory.
 *
 * Usage examples:
 * <pre>
 *   HRButton.primary(saveBtn);
 *   HRButton.flat(cancelBtn, deleteBtn, printBtn);
 *   HRButton.outlined(editBtn);
 *   HRButton.danger(deleteBtn);
 *   HRButton.icon(refreshBtn, "↻");
 * </pre>
 *
 * To switch UI library in the future, only change this class.
 */
public final class HRButton {

    // ─── Variants ────────────────────────────────────────────────────

    /** Filled primary button — main actions (Save, Submit, Confirm) */
    public static void primary(Button... buttons) {
        for (Button btn : buttons) {
            applyBase(btn);
            btn.setStyle(
                "-fx-background-color: " + Theme.PRIMARY + ";" +
                "-fx-text-fill: " + Theme.ON_PRIMARY + ";" +
                "-fx-font-size: " + Theme.FONT_SIZE_MD + "px;" +
                "-fx-font-family: '" + Theme.FONT_FAMILY + "';" +
                "-fx-padding: 8 20 8 20;" +
                "-fx-background-radius: " + Radius.MD + ";" +
                "-fx-effect: " + Elevation.E1 + ";"
            );
            addHoverEffect(btn,
                "-fx-background-color: " + Theme.PRIMARY_LIGHT + ";",
                "-fx-background-color: " + Theme.PRIMARY + ";"
            );
        }
    }

    /** Flat/text button — secondary actions (Cancel, Back) */
    public static void flat(boolean isStyle,Button... buttons) {
        for (Button btn : buttons) {
            applyBase(btn);

                btn.setStyle(
                        "-fx-background-color:  linear-gradient(to bottom, #090909, #191919, #262627, #343434, #424243, #414243, #404142, #404141, #313232, #232323, #151515, #000000);" +
                                "-fx-text-fill:  white;" +
                                "-fx-font-weight: bold;"+
                                "-fx-font-size: " + Theme.FONT_SIZE_MD + "px;" +
                                "-fx-font-family: '" + Theme.FONT_FAMILY + "';" +
                                "-fx-padding: 8 16 8 16;" +
                                "-fx-background-radius:  10;" +
                                "-fx-border-radius: 10;"+
                                "-fx-effect:  dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0, 0, 2);"
                );


            addHoverEffect(btn,
                "-fx-background-color: " + ColorPalette.toRgbaCss(Theme.PRIMARY, 0.08) + ";" + "-fx-text-fill:  black;",
                "-fx-background-color: transparent;"
            );
        }
    }

    /** Outlined button — medium emphasis */
    public static void outlined(Button... buttons) {
        for (Button btn : buttons) {
            applyBase(btn);
            btn.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-text-fill: " + Theme.PRIMARY + ";" +
                "-fx-font-size: " + Theme.FONT_SIZE_MD + "px;" +
                "-fx-font-family: '" + Theme.FONT_FAMILY + "';" +
                "-fx-padding: 7 18 7 18;" +
                "-fx-background-radius: " + Radius.MD + ";" +
                "-fx-border-color: " + Theme.PRIMARY + ";" +
                "-fx-border-radius: " + Radius.MD + ";" +
                "-fx-border-width: 1.5;" +
                "-fx-effect: none;"
            );
            addHoverEffect(btn,
                "-fx-background-color: " + ColorPalette.toRgbaCss(Theme.PRIMARY, 0.08) + ";",
                "-fx-background-color: transparent;"
            );
        }
    }

    /** Danger/destructive button — delete, remove actions */
    public static void danger(Button... buttons) {
        for (Button btn : buttons) {
            applyBase(btn);
            btn.setStyle(
                "-fx-background-color: " + Theme.ERROR + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-size: " + Theme.FONT_SIZE_MD + "px;" +
                "-fx-font-family: '" + Theme.FONT_FAMILY + "';" +
                "-fx-padding: 8 20 8 20;" +
                "-fx-background-radius: " + Radius.MD + ";" +
                "-fx-effect: " + Elevation.E1 + ";"
            );
            addHoverEffect(btn,
                "-fx-background-color: " + Theme.ERROR_LIGHT + ";",
                "-fx-background-color: " + Theme.ERROR + ";"
            );
        }
    }

    /** Success/confirm button — approve, confirm actions */
    public static void success(Button... buttons) {
        for (Button btn : buttons) {
            applyBase(btn);
            btn.setStyle(
                "-fx-background-color: " + Theme.SUCCESS + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-size: " + Theme.FONT_SIZE_MD + "px;" +
                "-fx-font-family: '" + Theme.FONT_FAMILY + "';" +
                "-fx-padding: 8 20 8 20;" +
                "-fx-background-radius: " + Radius.MD + ";" +
                "-fx-effect: " + Elevation.E1 + ";"
            );
            addHoverEffect(btn,
                "-fx-background-color: " + Theme.SUCCESS_LIGHT + ";",
                "-fx-background-color: " + Theme.SUCCESS + ";"
            );
        }
    }

    /** Small icon-only button with optional tooltip */
    public static void icon(Button btn, String tooltipText) {
        applyBase(btn);
        btn.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-text-fill: " + Theme.ON_SURFACE + ";" +
            "-fx-font-size: " + Theme.FONT_SIZE_LG + "px;" +
            "-fx-padding: 6 6 6 6;" +
            "-fx-background-radius: " + Radius.CIRCLE + ";" +
            "-fx-min-width: 32;" +
            "-fx-min-height: 32;"
        );
        if (tooltipText != null && !tooltipText.isBlank()) {
            btn.setTooltip(new Tooltip(tooltipText));
        }
        addHoverEffect(btn,
            "-fx-background-color: " + ColorPalette.toRgbaCss(Theme.ON_SURFACE, 0.08) + ";",
            "-fx-background-color: transparent;"
        );
    }

    // ─── Internal helpers ────────────────────────────────────────────

    private static void applyBase(Button btn) {
        btn.setCursor(Cursor.HAND);
        btn.setMnemonicParsing(false);
    }

    private static void addHoverEffect(Button btn, String hoverStyle, String normalStyle) {
        // Merge hover style on top of existing inline style
        String baseStyle = btn.getStyle();
        btn.setOnMouseEntered(e -> btn.setStyle(baseStyle + hoverStyle));
        btn.setOnMouseExited(e  -> btn.setStyle(baseStyle));
    }

    // ─── Inline helper (avoids importing ColorPalette everywhere) ───
    private static final class ColorPalette {
        static String toRgbaCss(String hex, double alpha) {
            javafx.scene.paint.Color c = javafx.scene.paint.Color.web(hex);
            return String.format("rgba(%d,%d,%d,%.2f)",
                (int)(c.getRed()   * 255),
                (int)(c.getGreen() * 255),
                (int)(c.getBlue()  * 255),
                alpha);
        }
    }

    private HRButton() {}
}
