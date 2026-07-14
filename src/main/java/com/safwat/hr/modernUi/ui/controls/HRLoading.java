package com.safwat.hr.modernUi.ui.controls;

import com.safwat.hr.modernUi.ui.style.Theme;
import io.github.palexdev.materialfx.controls.MFXProgressSpinner;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * ─────────────────────────────────────────────────────────────
 * HRLoading — Overlay تحميل فوق أي Pane.
 * يستخدم MFXProgressSpinner من MaterialFX.
 * <p>
 * الاستخدام:
 * <pre>
 *    HRLoading.show(rootPane);
 *    HRLoading.show(rootPane, "جاري الحفظ...");
 *    HRLoading.hide(rootPane);
 *  </pre>
 * ─────────────────────────────────────────────────────────────
 */
public final class HRLoading {

    private static final String OVERLAY_KEY = "hr-loading-overlay";

    private HRLoading() {
    }

    public static void show(Pane pane) {
        show(pane, null);
    }

    public static void show(Pane pane, String message) {
        if (pane.getProperties().containsKey(OVERLAY_KEY)) return;

        // خلفية شفافة
        StackPane backdrop = new StackPane();
        backdrop.setStyle("-fx-background-color: rgba(255,255,255,0.72);");
        backdrop.prefWidthProperty().bind(pane.widthProperty());
        backdrop.prefHeightProperty().bind(pane.heightProperty());

        // MaterialFX spinner
        MFXProgressSpinner spinner = new MFXProgressSpinner();
        spinner.setRadius(24);
        spinner.setStyle("-mfx-track-color: " + Theme.DIVIDER + ";" +
                "-mfx-progress-color: " + Theme.PRIMARY + ";");

        VBox card = new VBox(12, spinner);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(28));
        card.setStyle(
                "-fx-background-color: " + Theme.SURFACE + ";" +
                        "-fx-background-radius: 12px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.20), 16, 0, 0, 4);"
        );

        if (message != null && !message.isBlank()) {
            Label label = new Label(message);
            label.setStyle(
                    "-fx-font-family: '" + Theme.FONT_FAMILY + "';" +
                            "-fx-font-size: " + Theme.FONT_MD + "px;" +
                            "-fx-text-fill: " + Theme.HINT + ";"
            );
            card.getChildren().add(label);
        }

        backdrop.getChildren().add(card);
        pane.getChildren().add(backdrop);
        pane.getProperties().put(OVERLAY_KEY, backdrop);
    }

    public static void hide(Pane pane) {
        Object overlay = pane.getProperties().remove(OVERLAY_KEY);
        if (overlay instanceof StackPane sp) {
            pane.getChildren().remove(sp);
        }
    }
}
