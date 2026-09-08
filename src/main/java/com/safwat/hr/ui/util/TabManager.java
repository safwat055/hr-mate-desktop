package com.safwat.hr.ui.util;

import com.safwat.hr.shared.AppConfig;
import com.safwat.hr.shared.ColorSettingsManager;
import com.safwat.hr.shared.FontSettingsManager;
import com.safwat.hr.shared.ViewRegistry;
import com.safwat.hr.shared.ZoomManager;
import com.safwat.hr.ui.controls.SAFNotification;
import com.safwat.hr.ui.theme.ThemeEventBus;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.HashMap;

@Slf4j
public class TabManager {

    private static final HashMap<String, Tab> loadedTabs = new HashMap<>();

    /**
     * بيحمّل FXML في تاب جديد ويطبق تلقائياً:
     * - تسجيل الواجهة في ViewRegistry (تظهر في كومبو الإعدادات)
     * - إعدادات الخطوط المحفوظة
     * - إعدادات الزوم المحفوظة
     * - ألوان الثيم + overrides المحفوظة
     * مفيش أي سطر محتاج يتكتب في الكنترولر.
     *
     * @param tabPane   حاوية التبويبات
     * @param fxmlPath  مسار ملف الـ FXML
     * @param tabTitle  عنوان التاب — بيُستخدم كـ viewId للخطوط والزوم والتسجيل
     * @param closAble  هل التاب قابل للإغلاق
     */
    public static void loadFXMLInTab(TabPane tabPane, String fxmlPath, String tabTitle, boolean closAble) {

        // لو التاب اتحمّل قبل كده — بس نعرضه
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
            Parent content = loader.load();

            // ✅ تطبيق كل الإعدادات تلقائياً
            applyViewSettings(tabTitle, content);

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

    /**
     * Helper مشترك — بيطبق كل إعدادات الواجهة بعد التحميل.
     * الألوان بتتطبق على Scene مستقلة عشان التاب يرث من الـ Scene الرئيسية
     * تلقائياً — فبنكتفي بتطبيق الخطوط والزوم على الـ root مباشرة.
     */
    static void applyViewSettings(String viewId, Parent root) {
        ViewRegistry.register(viewId);
        FontSettingsManager.applySettings(viewId, root);
        ZoomManager.applyZoom(viewId, root);
        // الألوان: التاب بيرث الـ Scene الرئيسية تلقائياً من ThemeEventBus
        // ColorSettingsManager.attachTheme مش محتاجه هنا لأن مفيش Scene منفصلة للتاب
    }
}