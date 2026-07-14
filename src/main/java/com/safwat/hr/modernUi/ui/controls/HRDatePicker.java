package com.safwat.hr.modernUi.ui.controls;


import com.safwat.hr.modernUi.ui.style.Theme;
import io.github.palexdev.materialfx.controls.MFXDatePicker;
import javafx.scene.control.DatePicker;
import javafx.scene.layout.Pane;

import java.time.LocalDate;

/**
 * HRDatePicker — Facade فوق MFXDatePicker.
 *
 * <pre>
 *   MFXDatePicker mfxBirth = HRDatePicker.apply(birthDatePicker, "تاريخ الميلاد");
 *
 *   // قراءة القيمة
 *   LocalDate date = mfxBirth.getValue();
 * </pre>
 */
public final class HRDatePicker {

    private HRDatePicker() {
    }

    public static MFXDatePicker apply(DatePicker original, String floatLabel) {
        MFXDatePicker mfx = new MFXDatePicker();
        mfx.setFloatingText(floatLabel);
        mfx.setId(original.getId());
        mfx.setDisable(original.isDisable());
        mfx.setVisible(original.isVisible());

        // نقل القيمة إن وجدت
        LocalDate val = original.getValue();
        if (val != null) mfx.setValue(val);

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
