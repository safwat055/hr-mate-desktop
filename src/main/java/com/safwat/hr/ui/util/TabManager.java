package com.safwat.hr.ui.util;

import com.safwat.hr.ui.controls.SAFNotification;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import java.io.IOException;
import java.util.HashMap;

public class TabManager {
    // هذا الماب سيبقى مشترك لكل التابات

    private static final HashMap<String, Tab> loadedTabs = new HashMap<>();

    /**
     *
     * @param tabPane  .
     * @param fxmlPath .
     * @param tabTitle .
     * @param closAble .
     */
    public static void loadFXMLInTab(TabPane tabPane, String fxmlPath, String tabTitle, boolean closAble) {
        // تحقق من وجود التاب مسبقًا
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

            tab.setOnClosed(e
                    -> loadedTabs.remove(fxmlPath)
            );

            tabPane.getTabs().add(tab);

            tabPane.getSelectionModel().select(tab);

            loadedTabs.put(fxmlPath, tab);

        } catch (IOException e) {
            e.printStackTrace();
            SAFNotification.error(e.getMessage());
        }
    }
}
