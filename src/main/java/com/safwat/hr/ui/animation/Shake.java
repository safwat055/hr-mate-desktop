package com.safwat.hr.ui.animation;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * Shake animation — use on invalid form fields.
 *
 * Usage:
 * <pre>
 *   Shake.play(nameField);
 * </pre>
 */
public final class Shake {

    public static void play(Node node) {
        double originalX = node.getTranslateX();
        Timeline shake = new Timeline(
            new KeyFrame(Duration.millis(0),   new KeyValue(node.translateXProperty(), originalX)),
            new KeyFrame(Duration.millis(60),  new KeyValue(node.translateXProperty(), originalX - 8)),
            new KeyFrame(Duration.millis(120), new KeyValue(node.translateXProperty(), originalX + 8)),
            new KeyFrame(Duration.millis(180), new KeyValue(node.translateXProperty(), originalX - 6)),
            new KeyFrame(Duration.millis(240), new KeyValue(node.translateXProperty(), originalX + 6)),
            new KeyFrame(Duration.millis(300), new KeyValue(node.translateXProperty(), originalX - 4)),
            new KeyFrame(Duration.millis(360), new KeyValue(node.translateXProperty(), originalX + 4)),
            new KeyFrame(Duration.millis(420), new KeyValue(node.translateXProperty(), originalX))
        );
        shake.play();
    }

    private Shake() {}
}
