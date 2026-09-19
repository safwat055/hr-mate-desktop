package com.safwat.hr.ui.util;

import com.safwat.hr.controller.appearance.FontSettingsManager;
import com.safwat.hr.controller.appearance.ZoomManager;
import com.safwat.hr.shared.ViewRegistry;
import com.safwat.hr.ui.controls.SAFNotification;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
public class TabManager {

    /**
     * نخزّن التاب + الـ Controller مع بعض عشان نقدر نعيد التهيئة لو التاب موجود.
     */
    private static final Map<String, TabInfo> loadedTabs = new HashMap<>();

    private record TabInfo(Tab tab, Object controller) {
    }

    // ══════════════════════════════════════════════════════════════
    //  Public API
    // ══════════════════════════════════════════════════════════════

    /**
     * النسخة القديمة — بدون initializer.
     */
    public static void loadFXMLInTab(TabPane tabPane, String fxmlPath,
                                     String tabTitle, boolean closAble) {
        loadFXMLInTab(tabPane, fxmlPath, tabTitle, closAble, null);
    }

    /**
     * نسخة جديدة — تقبل {@code controllerInitializer} يتنفّذ على الـ Controller
     * بعد التحميل. بيُستخدم لتمرير بيانات للتاب (زي الرقم القومي).
     *
     * <p><b>مهم:</b> لو التاب مفتوح بالفعل، الـ initializer هيتنفّذ برضه
     * على الـ Controller المخزّن — عشان البيانات تتحدّث.
     */
    public static void loadFXMLInTab(TabPane tabPane, String fxmlPath,
                                     String tabTitle, boolean closAble,
                                     Consumer<Object> controllerInitializer) {

        // ── التاب موجود بالفعل ──
        if (loadedTabs.containsKey(fxmlPath)) {
            TabInfo info = loadedTabs.get(fxmlPath);
            if (!tabPane.getTabs().contains(info.tab())) {
                tabPane.getTabs().add(info.tab());
            }
            tabPane.getSelectionModel().select(info.tab());

            if (controllerInitializer != null && info.controller() != null) {
                controllerInitializer.accept(info.controller());
            }
            return;
        }

        // ── التاب جديد ──
        try {
            FXMLLoader loader = new FXMLLoader(TabManager.class.getResource(fxmlPath));
            Parent content = loader.load();
            Object controller = loader.getController();

            applyViewSettings(tabTitle, content);

            if (controllerInitializer != null && controller != null) {
                controllerInitializer.accept(controller);
            }

            Tab tab = new Tab(tabTitle, content);
            tab.setClosable(closAble);
            tab.setOnClosed(_ -> loadedTabs.remove(fxmlPath));

            tabPane.getTabs().add(tab);
            tabPane.getSelectionModel().select(tab);

            loadedTabs.put(fxmlPath, new TabInfo(tab, controller));

        } catch (IOException e) {
            log.error("Failed to load FXML: {}", fxmlPath, e);
            SAFNotification.error(e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════════════════════

    static void applyViewSettings(String viewId, Parent root) {
        ViewRegistry.register(viewId);
        FontSettingsManager.applySettings(viewId, root);
        ZoomManager.applyZoom(viewId, root);
    }
}