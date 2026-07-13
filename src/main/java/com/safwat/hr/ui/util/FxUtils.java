package com.safwat.hr.ui.util;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * FxUtils — Common JavaFX utilities.
 *
 * Usage:
 * <pre>
 *   // Run on FX thread safely
 *   FxUtils.runOnFx(() -> table.refresh());
 *
 *   // Run heavy task in background, then update UI
 *   FxUtils.runAsync(
 *       () -> service.fetchAll(),       // background
 *       data -> table.setItems(data)    // FX thread
 *   );
 * </pre>
 */
public final class FxUtils {

    /** Run action on JavaFX Application Thread safely */
    public static void runOnFx(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }

    /**
     * Run supplier in background thread, deliver result to FX thread.
     * @param background supplier executed off FX thread
     * @param onResult   consumer executed on FX thread with the result
     */
    public static <T> void runAsync(Supplier<T> background,
                                    java.util.function.Consumer<T> onResult) {
        CompletableFuture
            .supplyAsync(background)
            .thenAcceptAsync(onResult, Platform::runLater);
    }

    /** Lookup a node by ID within a parent */
    @SuppressWarnings("unchecked")
    public static <T extends Node> T lookup(Parent root, String id) {
        return (T) root.lookup("#" + id);
    }

    private FxUtils() {}
}
