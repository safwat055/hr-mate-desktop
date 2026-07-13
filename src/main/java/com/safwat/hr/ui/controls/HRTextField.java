package com.safwat.hr.ui.controls;

import com.safwat.hr.ui.style.Radius;
import com.safwat.hr.ui.style.Theme;
import javafx.scene.control.TextField;

/**
 * HRTextField — Material outlined text field style.
 *
 * Usage:
 * <pre>
 *   HRTextField.apply(nameField, emailField);
 *   HRTextField.error(nameField);      // mark as invalid
 *   HRTextField.clearError(nameField); // remove error state
 * </pre>
 */
public final class HRTextField {

    private static final String BASE_STYLE =
        "-fx-background-color: " + Theme.SURFACE + ";" +
        "-fx-border-color: " + Theme.DIVIDER + ";" +
        "-fx-border-radius: " + Radius.MD + ";" +
        "-fx-background-radius: " + Radius.MD + ";" +
        "-fx-border-width: 1.5;" +
        "-fx-padding: 8 12 8 12;" +
        "-fx-font-size: " + Theme.FONT_SIZE_MD + "px;" +
        "-fx-font-family: '" + Theme.FONT_FAMILY + "';" +
        "-fx-text-fill: " + Theme.ON_SURFACE + ";";

    private static final String FOCUS_STYLE =
        "-fx-background-color: " + Theme.SURFACE + ";" +
        "-fx-border-color: " + Theme.PRIMARY + ";" +
        "-fx-border-radius: " + Radius.MD + ";" +
        "-fx-background-radius: " + Radius.MD + ";" +
        "-fx-border-width: 2;" +
        "-fx-padding: 8 12 8 12;" +
        "-fx-font-size: " + Theme.FONT_SIZE_MD + "px;" +
        "-fx-font-family: '" + Theme.FONT_FAMILY + "';" +
        "-fx-text-fill: " + Theme.ON_SURFACE + ";";

    private static final String ERROR_STYLE =
        "-fx-border-color: " + Theme.ERROR + ";" +
        "-fx-border-width: 1.5;";

    /** Apply material style to one or more fields */
    public static void apply(TextField... fields) {
        for (TextField field : fields) {
            field.setStyle(BASE_STYLE);
            field.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                // Don't override error state if present
                if (!Boolean.TRUE.equals(field.getProperties().get("hr-error"))) {
                    field.setStyle(isFocused ? FOCUS_STYLE : BASE_STYLE);
                }
            });
        }
    }

    /** Mark field as invalid (red border) */
    public static void error(TextField field) {
        field.setStyle(BASE_STYLE + ERROR_STYLE);
        field.getProperties().put("hr-error", true);
    }

    /** Remove error state */
    public static void clearError(TextField field) {
        field.getProperties().remove("hr-error");
        field.setStyle(BASE_STYLE);
    }

    private HRTextField() {}
}
