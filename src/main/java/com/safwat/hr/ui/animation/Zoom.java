package com.safwat.hr.ui.animation;


import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * Zoom — تأثير تكبير/تصغير عند الظهور.
 *
 * <pre>
 *   Zoom.in(dialogPane, 200);
 * </pre>
 */
public final class Zoom {

    private Zoom() {
    }

    public static void in(Node node, int durationMs) {
        node.setScaleX(0.8);
        node.setScaleY(0.8);
        node.setOpacity(0);
        new Timeline(
                new KeyFrame(Duration.millis(0),
                        new KeyValue(node.scaleXProperty(), 0.8),
                        new KeyValue(node.scaleYProperty(), 0.8),
                        new KeyValue(node.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(durationMs),
                        new KeyValue(node.scaleXProperty(), 1.0),
                        new KeyValue(node.scaleYProperty(), 1.0),
                        new KeyValue(node.opacityProperty(), 1))
        ).play();
    }

    public static void out(Node node, int durationMs, Runnable onFinished) {
        new Timeline(
                new KeyFrame(Duration.millis(0),
                        new KeyValue(node.scaleXProperty(), 1.0),
                        new KeyValue(node.scaleYProperty(), 1.0),
                        new KeyValue(node.opacityProperty(), 1)),
                new KeyFrame(Duration.millis(durationMs),
                        new KeyValue(node.scaleXProperty(), 0.8),
                        new KeyValue(node.scaleYProperty(), 0.8),
                        new KeyValue(node.opacityProperty(), 0))
        ).play();
    }
}
