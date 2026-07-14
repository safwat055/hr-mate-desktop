package com.safwat.hr.modernUi.ui.controls;


import com.safwat.hr.modernUi.ui.style.Theme;
import io.github.palexdev.materialfx.controls.MFXProgressBar;
import io.github.palexdev.materialfx.controls.MFXProgressSpinner;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.Pane;

/**
 * HRProgress — Facade فوق MFXProgressBar و MFXProgressSpinner.
 *
 * <pre>
 *   // شريط تقدم
 *   MFXProgressBar mfxBar = HRProgress.bar(uploadProgressBar);
 *   MFXProgressBar mfxBar = HRProgress.bar(uploadProgressBar, HRProgress.Color.SUCCESS);
 *
 *   // spinner دائري
 *   MFXProgressSpinner mfxSpinner = HRProgress.spinner(loadingIndicator);
 *
 *   // تحديث القيمة لاحقاً
 *   mfxBar.setProgress(0.75);
 * </pre>
 */
public final class HRProgress {

    private HRProgress() {
    }

    // ── ProgressBar ──────────────────────────────────────────

    public static MFXProgressBar bar(ProgressBar original) {
        return bar(original, Color.PRIMARY);
    }

    public static MFXProgressBar bar(ProgressBar original, Color color) {
        MFXProgressBar mfx = new MFXProgressBar(original.getProgress());
        mfx.setId(original.getId());
        mfx.setVisible(original.isVisible());

        if (original.getPrefWidth() > 0) mfx.setPrefWidth(original.getPrefWidth());
        if (original.getPrefHeight() > 0) mfx.setPrefHeight(original.getPrefHeight());
        if (original.getMaxWidth() > 0) mfx.setMaxWidth(original.getMaxWidth());

        mfx.setStyle(
                "-mfx-progress-color: " + color.hex + ";" +
                        "-mfx-track-color: " + Theme.DIVIDER + ";" +
                        "-fx-pref-height: 6px;"
        );

        // ربط الـ progress من الأصل إن كان mutable
        original.progressProperty().addListener((obs, o, n) ->
                mfx.setProgress(n.doubleValue()));

        replaceInParent(original, mfx);
        return mfx;
    }

    // ── ProgressSpinner ──────────────────────────────────────

    public static MFXProgressSpinner spinner(ProgressIndicator original) {
        MFXProgressSpinner mfx = new MFXProgressSpinner();
        mfx.setId(original.getId());
        mfx.setVisible(original.isVisible());
        mfx.setRadius(20);

        mfx.setStyle(
                "-mfx-progress-color: " + Theme.PRIMARY + ";" +
                        "-mfx-track-color: " + Theme.DIVIDER + ";"
        );

        if (original.getParent() instanceof Pane parent) {
            int index = parent.getChildren().indexOf(original);
            parent.getChildren().set(index, mfx);
        }

        return mfx;
    }

    private static void replaceInParent(ProgressBar original, MFXProgressBar mfx) {
        if (original.getParent() instanceof Pane parent) {
            int index = parent.getChildren().indexOf(original);
            parent.getChildren().set(index, mfx);
        }
    }

    public enum Color {
        PRIMARY(Theme.PRIMARY),
        SUCCESS(Theme.SUCCESS),
        WARNING(Theme.WARNING),
        ERROR(Theme.ERROR),
        INFO(Theme.INFO);

        final String hex;

        Color(String hex) {
            this.hex = hex;
        }
    }
}
