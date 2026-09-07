package com.safwat.hr.ui.util;

import com.safwat.hr.shared.ViewRegistry;
import com.safwat.hr.ui.controls.SAFNotification;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.HashMap;

@Slf4j
public class TabManager {

    private static final HashMap<String, Tab> loadedTabs = new HashMap<>();

    /**
     * @param tabPane  حاوية التبويبات
     * @param fxmlPath مسار ملف الـ FXML
     * @param tabTitle عنوان التاب — بيتسجل تلقائياً في ViewRegistry كـ viewId
     * @param closAble هل التاب قابل للإغلاق
     */
    public static void loadFXMLInTab(TabPane tabPane, String fxmlPath, String tabTitle, boolean closAble) {

        // ✅ تسجيل الواجهة تلقائياً في ViewRegistry باسم التاب
        ViewRegistry.register(tabTitle);

        if (loadedTabs.containsKey(fxmlPath)) {
            Tab existingTab = loadedTabs.get(fxmlPath);
            if (!tabPane.getTabs().contains(existingTab)) {
                tabPane.getTabs().add(existingTab);
            }
            tabPane.getSelectionModel().select(existingTab);
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(TabManager.class.getResource(fxmlPath));
            Node content = loader.load();

            Tab tab = new Tab(tabTitle, content);
            tab.setClosable(closAble);

            tab.setOnClosed(_ -> loadedTabs.remove(fxmlPath));

            tabPane.getTabs().add(tab);
            tabPane.getSelectionModel().select(tab);

            loadedTabs.put(fxmlPath, tab);

        } catch (IOException e) {
            e.printStackTrace();
            log.info(e.getMessage());
            SAFNotification.error(e.getMessage());
        }
    }
}