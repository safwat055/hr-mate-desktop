package com.safwat.hr.ui.animation;

import javafx.animation.FadeTransition;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * Fade — تأثيرات ظهور واختفاء.
 *
 * <pre>
 *   Fade.in(node, 300);
 *   Fade.out(node, 300, () -> pane.getChildren().remove(node));
 * </pre>
 */
public final class Fade {

    private Fade() {
    }

    public static void in(Node node, int durationMs) {
        node.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(durationMs), node);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    public static void out(Node node, int durationMs, Runnable onFinished) {
        FadeTransition ft = new FadeTransition(Duration.millis(durationMs), node);
        ft.setFromValue(node.getOpacity());
        ft.setToValue(0);
        if (onFinished != null) ft.setOnFinished(e -> onFinished.run());
        ft.play();
    }

    public static void out(Node node, int durationMs) {
        out(node, durationMs, null);
    }
}
