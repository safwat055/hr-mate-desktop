package com.safwat.hr.modernUi.ui.animation;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * Slide — تأثير انزلاق من الجوانب.
 *
 * <pre>
 *   Slide.fromRight(panel, 350);
 *   Slide.fromBottom(notification, 250);
 * </pre>
 */
public final class Slide {

    private Slide() {
    }

    public static void fromRight(Node node, int durationMs) {
        double startX = node.getTranslateX();
        node.setTranslateX(300);
        node.setOpacity(0);
        new Timeline(
                new KeyFrame(Duration.millis(0),
                        new KeyValue(node.translateXProperty(), 300),
                        new KeyValue(node.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(durationMs),
                        new KeyValue(node.translateXProperty(), startX),
                        new KeyValue(node.opacityProperty(), 1))
        ).play();
    }

    public static void fromBottom(Node node, int durationMs) {
        double startY = node.getTranslateY();
        node.setTranslateY(60);
        node.setOpacity(0);
        new Timeline(
                new KeyFrame(Duration.millis(0),
                        new KeyValue(node.translateYProperty(), 60),
                        new KeyValue(node.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(durationMs),
                        new KeyValue(node.translateYProperty(), startY),
                        new KeyValue(node.opacityProperty(), 1))
        ).play();
    }

    public static void fromLeft(Node node, int durationMs) {
        double startX = node.getTranslateX();
        node.setTranslateX(-300);
        node.setOpacity(0);
        new Timeline(
                new KeyFrame(Duration.millis(0),
                        new KeyValue(node.translateXProperty(), -300),
                        new KeyValue(node.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(durationMs),
                        new KeyValue(node.translateXProperty(), startX),
                        new KeyValue(node.opacityProperty(), 1))
        ).play();
    }
}
