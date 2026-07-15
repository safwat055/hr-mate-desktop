package com.safwat.hr.ui.controls;

import com.safwat.hr.ui.style.Radius;
import com.safwat.hr.ui.style.Theme;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;

/**
 * HRProgress — Material-style progress components.
 * <p>
 * Usage:
 * <pre>
 *   HRProgress.bar(uploadProgressBar);
 *   HRProgress.spinner(loadingIndicator);
 *
 *   // Semantic colors
 *   HRProgress.bar(progressBar, HRProgress.Color.SUCCESS);
 * </pre>
 */
public final class SAFProgress {

    private SAFProgress() {
    }

    public static void bar(ProgressBar bar) {
        bar(bar, Color.PRIMARY);
    }

    public static void bar(ProgressBar bar, Color color) {
        bar.setStyle(
                "-fx-accent: " + color.hex + ";" +
                        "-fx-background-color: " + Theme.DIVIDER + ";" +
                        "-fx-background-radius: " + Radius.PILL + ";" +
                        "-fx-pref-height: 6;"
        );
    }

    public static void spinner(ProgressIndicator indicator) {
        indicator.setStyle(
                "-fx-progress-color: " + Theme.PRIMARY + ";"
        );
    }

    public enum Color {
        PRIMARY(Theme.PRIMARY),
        SUCCESS(Theme.SUCCESS),
        WARNING(Theme.WARNING),
        ERROR(Theme.ERROR);

        final String hex;

        Color(String hex) {
            this.hex = hex;
        }
    }
}
