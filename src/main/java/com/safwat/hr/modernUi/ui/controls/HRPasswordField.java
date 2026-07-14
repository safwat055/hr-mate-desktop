package com.safwat.hr.modernUi.ui.controls;


import com.safwat.hr.modernUi.ui.style.Theme;
import io.github.palexdev.materialfx.controls.MFXPasswordField;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.Pane;

/**
 * HRPasswordField — Facade فوق MFXPasswordField.
 *
 * <pre>
 *   MFXPasswordField mfxPass = HRPasswordField.apply(passwordField, "كلمة المرور");
 * </pre>
 */
public final class HRPasswordField {

    private HRPasswordField() {
    }

    public static MFXPasswordField apply(PasswordField original, String floatLabel) {
        MFXPasswordField mfx = new MFXPasswordField();
        mfx.setFloatingText(floatLabel);
        mfx.setId(original.getId());
        mfx.setDisable(original.isDisable());
        mfx.setVisible(original.isVisible());

        if (original.getPrefWidth() > 0) mfx.setPrefWidth(original.getPrefWidth());
        if (original.getPrefHeight() > 0) mfx.setPrefHeight(original.getPrefHeight());
        if (original.getMaxWidth() > 0) mfx.setMaxWidth(original.getMaxWidth());

        mfx.setStyle(
                "-mfx-line-color: " + Theme.PRIMARY + ";" +
                        "-mfx-unfocus-line-color: " + Theme.DIVIDER + ";" +
                        "-fx-font-family: '" + Theme.FONT_FAMILY + "';" +
                        "-fx-font-size: " + Theme.FONT_MD + "px;"
        );

        if (original.getParent() instanceof Pane parent) {
            int index = parent.getChildren().indexOf(original);
            parent.getChildren().set(index, mfx);
        }

        return mfx;
    }
}
