package com.safwat.hr.modernUi.ui.animation;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * Shake — اهتزاز للحقول الخاطئة عند التحقق.
 *
 * <pre>
 *   Shake.play(nameField);
 * </pre>
 */
public final class Shake {

    private Shake() {
    }

    public static void play(Node node) {
        double x = node.getTranslateX();
        new Timeline(
                new KeyFrame(Duration.millis(0), new KeyValue(node.translateXProperty(), x)),
                new KeyFrame(Duration.millis(60), new KeyValue(node.translateXProperty(), x - 8)),
                new KeyFrame(Duration.millis(120), new KeyValue(node.translateXProperty(), x + 8)),
                new KeyFrame(Duration.millis(180), new KeyValue(node.translateXProperty(), x - 6)),
                new KeyFrame(Duration.millis(240), new KeyValue(node.translateXProperty(), x + 6)),
                new KeyFrame(Duration.millis(300), new KeyValue(node.translateXProperty(), x - 4)),
                new KeyFrame(Duration.millis(360), new KeyValue(node.translateXProperty(), x + 4)),
                new KeyFrame(Duration.millis(420), new KeyValue(node.translateXProperty(), x))
        ).play();
    }
}
