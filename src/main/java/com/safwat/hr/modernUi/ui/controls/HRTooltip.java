package com.safwat.hr.modernUi.ui.controls;


import com.safwat.hr.modernUi.ui.style.Theme;
import io.github.palexdev.materialfx.controls.MFXTooltip;
import javafx.scene.Node;

/**
 * HRTooltip — Facade فوق MFXTooltip.
 *
 * <pre>
 *   HRTooltip.install(saveBtn, "حفظ البيانات");
 *   HRTooltip.install(deleteBtn, "حذف السجل نهائياً");
 *   HRTooltip.install(refreshBtn, "تحديث القائمة");
 * </pre>
 */
public final class HRTooltip {

    private HRTooltip() {
    }

    public static MFXTooltip install(Node node, String text) {
        MFXTooltip tooltip = new MFXTooltip(node);
        tooltip.setText(text);
        tooltip.setStyle(
                "-fx-background-color: " + Theme.ON_SURFACE + ";" +
                        "-fx-background-radius: 4px;" +
                        "-fx-text-fill: " + Theme.SURFACE + ";" +
                        "-fx-font-family: '" + Theme.FONT_FAMILY + "';" +
                        "-fx-font-size: " + Theme.FONT_SM + "px;" +
                        "-fx-padding: 6 10 6 10;"
        );
        tooltip.install();
        return tooltip;
    }
}
