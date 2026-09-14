package com.safwat.hr.ui.theme;

import javafx.scene.Node;
import javafx.scene.Scene;

import java.net.URL;

/**
 * مسؤول عن تحميل settings.css على الـ Scene بتاعة أي شاشة إعدادات.
 * <p>
 * Idempotent — ما يضيفهاش مرتين لو نُدت أكثر من مرة على نفس الـ Scene.
 * <p>
 * لو الـ Scene لسه ما اتحملتش (initialize في الكنترولر بتحصل قبل attach)،
 * بيستنى event الـ sceneProperty ويضيفها أول ما الـ Scene تكون جاهزة.
 */
public final class SettingsThemeLoader {

    private static final String SETTINGS_CSS =
            "/com/safwat/hr/css/settings.css";

    private SettingsThemeLoader() {
    }

    /**
     * @param anyNodeInScene أي Node من الـ FXML بتاع الشاشة —
     *                       بنستنبط منها الـ Scene تلقائيًا.
     */
    public static void apply(Node anyNodeInScene) {
        if (anyNodeInScene == null) return;

        Scene scene = anyNodeInScene.getScene();
        if (scene != null) {
            addTo(scene);
        } else {
            // لسه ما اتولدتش — نستنى إشارة الـ sceneProperty
            anyNodeInScene.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) addTo(newScene);
            });
        }
    }

    private static void addTo(Scene scene) {
        URL css = SettingsThemeLoader.class.getResource(SETTINGS_CSS);
        if (css == null) return;

        String url = css.toExternalForm();
        if (!scene.getStylesheets().contains(url)) {
            scene.getStylesheets().add(url);
        }
    }
}