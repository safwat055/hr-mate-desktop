package com.safwat.hr.ui.controls;

import com.safwat.hr.ui.style.Elevation;
import com.safwat.hr.ui.style.Radius;
import com.safwat.hr.ui.style.Theme;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * HRLoading — Overlay loading spinner for any Pane.
 * <p>
 * Usage:
 * <pre>
 *   // Show spinner over your pane
 *   HRLoading.show(rootPane);
 *   HRLoading.show(rootPane, "جاري التحميل...");
 *
 *   // Hide when done
 *   HRLoading.hide(rootPane);
 * </pre>
 */
public final class SAFLoading {

    private static final String OVERLAY_KEY = "hr-loading-overlay";

    private SAFLoading() {
    }

    public static void show(Pane pane) {
        show(pane, null);
    }

    public static void show(Pane pane, String message) {
        if (pane.getProperties().containsKey(OVERLAY_KEY)) return;

        // Semi-transparent backdrop
        StackPane backdrop = new StackPane();
        backdrop.setStyle("-fx-background-color: rgba(255,255,255,0.75);");
        backdrop.prefWidthProperty().bind(pane.widthProperty());
        backdrop.prefHeightProperty().bind(pane.heightProperty());

        // Spinner card
        ProgressIndicator spinner = new ProgressIndicator(-1);
        spinner.setStyle(
                "-fx-progress-color: " + Theme.PRIMARY + ";" +
                        "-fx-max-width: 48;" +
                        "-fx-max-height: 48;"
        );

        VBox card = new VBox(12, spinner);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(24));
        card.setStyle(
                "-fx-background-color: " + Theme.SURFACE + ";" +
                        "-fx-background-radius: " + Radius.XL + ";" +
                        "-fx-effect: " + Elevation.E3 + ";"
        );

        if (message != null && !message.isBlank()) {
            Label label = new Label(message);
            label.setStyle(
                    "-fx-font-size: " + Theme.FONT_SIZE_MD + "px;" +
                            "-fx-font-family: '" + Theme.FONT_FAMILY + "';" +
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
        if (overlay instanceof StackPane) {
            pane.getChildren().remove(overlay);
        }
    }
}
