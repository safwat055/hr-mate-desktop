package com.safwat.hr.ui.controls;

import com.safwat.hr.ui.style.Radius;
import com.safwat.hr.ui.style.Theme;
import javafx.scene.control.ComboBox;

/**
 * HRComboBox — Material outlined combo box style.
 * <p>
 * Usage:
 * <pre>
 *   HRComboBox.apply(departmentCombo, jobCombo);
 * </pre>
 */
public final class SAFComboBox {

    private static final String BASE_STYLE =
            "-fx-background-color: " + Theme.SURFACE + ";" +
                    "-fx-border-color: " + Theme.DIVIDER + ";" +
                    "-fx-border-radius: " + Radius.MD + ";" +
                    "-fx-background-radius: " + Radius.MD + ";" +
                    "-fx-border-width: 1.5;" +
                    "-fx-font-size: " + Theme.FONT_SIZE_MD + "px;" +
                    "-fx-font-family: '" + Theme.FONT_FAMILY + "';" +
                    "-fx-padding: 4 8 4 8;";

    private SAFComboBox() {
    }

    public static <T> void apply(ComboBox<T>... combos) {
        for (ComboBox<T> combo : combos) {
            combo.setStyle(BASE_STYLE);
            combo.showingProperty().addListener((obs, wasShowing, isShowing) ->
                    combo.setStyle(isShowing
                            ? BASE_STYLE + "-fx-border-color: " + Theme.PRIMARY + "; -fx-border-width: 2;"
                            : BASE_STYLE)
            );
        }
    }
}
