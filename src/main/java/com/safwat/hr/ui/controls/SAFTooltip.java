package com.safwat.hr.ui.controls;


import com.safwat.hr.ui.style.Theme;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;

/**
 * HRTooltip — Facade فوق MFXTooltip.
 *
 * <pre>
 *   HRTooltip.install(saveBtn, "حفظ البيانات");
 *   HRTooltip.install(deleteBtn, "حذف السجل نهائياً");
 *   HRTooltip.install(refreshBtn, "تحديث القائمة");
 * </pre>
 */
public final class SAFTooltip {

    private SAFTooltip() {
    }

    public static Tooltip install(Node node, String text) {
        Tooltip tooltip = new Tooltip();
        tooltip.setText(text);
        tooltip.setStyle(
                "-fx-background-color: " + Theme.ON_SURFACE + ";" +
                        "-fx-background-radius: 4px;" +
                        "-fx-text-fill: " + Theme.SURFACE + ";" +
                        "-fx-font-family: '" + Theme.FONT_FAMILY + "';" +
                        "-fx-font-size: " + Theme.FONT_FAMILY + "px;" +
                        "-fx-padding: 6 10 6 10;"
        );
        Tooltip.install(node, tooltip);
        return tooltip;
    }
}
