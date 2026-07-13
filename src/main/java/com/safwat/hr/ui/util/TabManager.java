package com.safwat.hr.ui.util;

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
     * @param tabPane .
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
            // تحميل FXML
            FXMLLoader loader = new FXMLLoader(TabManager.class.getResource(fxmlPath));
            Node content = loader.load();

            // إنشاء تبويبة جديدة
            Tab tab = new Tab(tabTitle, content);
            tab.setClosable(closAble); // زر الإغلاق ظاهر

            // عند الإغلاق، نحذفه من الماب
            tab.setOnClosed(e
                    -> loadedTabs.remove(fxmlPath)
            );

            // إضافة التبويبة وتفعيلها
            tabPane.getTabs().add(tab);

            tabPane.getSelectionModel().select(tab);

            // حفظها في الماب
            loadedTabs.put(fxmlPath, tab);

        } catch (IOException e) {
            e.printStackTrace();
            // يمكن عرض Alert للمستخدم هنا
        }
    }
}
