package com.safwat.hr.ui.util;

import com.safwat.hr.shared.AppConfig;
import com.safwat.hr.shared.ColorSettingsManager;
import com.safwat.hr.shared.FontSettingsManager;
import com.safwat.hr.shared.ViewRegistry;
import com.safwat.hr.shared.ZoomManager;
import com.safwat.hr.ui.controls.SAFNotification;
import com.safwat.hr.ui.theme.ThemeEventBus;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

/**
 * مدير تحميل الواجهات المستقلة (نوافذ + StackPane).
 * كل method بتطبق تلقائياً بعد التحميل:
 *  - تسجيل الواجهة في ViewRegistry
 *  - الخطوط المحفوظة  (FontSettingsManager)
 *  - الزوم المحفوظ    (ZoomManager)
 *  - الثيم + الألوان  (ColorSettingsManager)
 * مفيش أي سطر محتاج يتكتب في الكنترولرات.
 */
@Slf4j
public class ViewManager {

    private static final String ICON_PATH = "/com/safwat/hr/icons/logo.png";

    // ════════════════════════════════════════════════
    //  Helpers مشتركة
    // ════════════════════════════════════════════════

    /**
     * بيطبق كل إعدادات الواجهة على الـ root والـ Scene معاً.
     * بيتنادى من كل method فتح نافذة مستقلة.
     */
    private static void applyViewSettings(String viewId, Parent root, Scene scene) {
        ViewRegistry.register(viewId);
        FontSettingsManager.applySettings(viewId, root);
        ZoomManager.applyZoom(viewId, root);
        // ✅ الألوان: بتطبق على الـ Scene عشان تشمل كل محتواها
        String theme = AppConfig.getString("ui", "theme", ThemeEventBus.LIGHT);
        ColorSettingsManager.attachTheme(scene, theme);
    }

    /**
     * نسخة بدون Scene — للـ StackPane اللي بترث الـ Scene الرئيسية تلقائياً.
     * الألوان مش محتاجة هنا لأن الـ root جزء من Scene موجودة.
     */
    private static void applyViewSettings(String viewId, Parent root) {
        ViewRegistry.register(viewId);
        FontSettingsManager.applySettings(viewId, root);
        ZoomManager.applyZoom(viewId, root);
    }

    private static String iconUrl() {
        return Objects.requireNonNull(ViewManager.class.getResource(ICON_PATH)).toExternalForm();
    }

    // ════════════════════════════════════════════════
    //  فتح نافذة مستقلة (Modal)
    // ════════════════════════════════════════════════

    /**
     * @param fxmlFile مسار الـ FXML
     * @param viewId   اسم الواجهة (يظهر في كومبو الإعدادات + يُستخدم للخطوط والزوم والألوان)
     */
    public static void openIndependentView(String fxmlFile, String viewId) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    Objects.requireNonNull(ViewManager.class.getResource(fxmlFile)));
            Parent view = loader.load();

            Scene scene = new Scene(view);

            // ✅ تطبيق كل الإعدادات تلقائياً
            applyViewSettings(viewId, view, scene);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.setTitle(viewId);
            stage.getIcons().add(new Image(iconUrl()));
            stage.setScene(scene);
            stage.show();

        } catch (IOException ex) {
            ex.printStackTrace();
            SAFNotification.error(ex.getMessage());
            log.error(ex.getMessage(), ex);
        }
    }

    /** للتوافق مع الكود القديم */
    public static void openIndependentView(String fxmlFile) {
        openIndependentView(fxmlFile, fxmlFile);
    }

    // ════════════════════════════════════════════════
    //  فتح نافذة غير Modal
    // ════════════════════════════════════════════════

    /**
     * @param fxmlFile مسار الـ FXML
     * @param viewId   اسم الواجهة
     */
    public static void openNoIndependentView(String fxmlFile, String viewId) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    Objects.requireNonNull(ViewManager.class.getResource(fxmlFile)));
            Parent view = loader.load();

            Scene scene = new Scene(view);

            // ✅ تطبيق كل الإعدادات تلقائياً
            applyViewSettings(viewId, view, scene);

            Stage stage = new Stage();
            stage.setTitle(viewId);
            stage.setResizable(false);
            stage.getIcons().add(new Image(iconUrl()));
            stage.setScene(scene);
            stage.show();

        } catch (IOException ex) {
            SAFNotification.error(ex.getMessage());
            log.error(ex.getMessage());
        }
    }

    /** للتوافق مع الكود القديم */
    public static void openNoIndependentView(String fxmlFile) {
        openNoIndependentView(fxmlFile, fxmlFile);
    }

    public static void openNoIndependentView(String fxmlFile, boolean isResizeAble) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    Objects.requireNonNull(ViewManager.class.getResource(fxmlFile)));
            Parent view = loader.load();

            Scene scene = new Scene(view);

            // ✅ تطبيق كل الإعدادات تلقائياً
            applyViewSettings(fxmlFile, view, scene);

            Stage stage = new Stage();
            stage.setTitle("HR MATE");
            stage.setResizable(isResizeAble);
            stage.getIcons().add(new Image(iconUrl()));
            stage.setScene(scene);
            stage.show();

        } catch (IOException ex) {
            SAFNotification.error(ex.getMessage());
            log.error(ex.getMessage());
        }
    }

    // ════════════════════════════════════════════════
    //  تحميل في StackPane رئيسية
    // ════════════════════════════════════════════════

    /**
     * بيحمّل الواجهة في StackPane — الألوان بترث من الـ Scene الرئيسية تلقائياً.
     * الخطوط والزوم بيتطبقوا مرة واحدة عند أول تحميل.
     *
     * @param fxmFile       مسار الـ FXML
     * @param viewId        اسم الواجهة
     * @param mainStackPane الحاوية الرئيسية
     */
    public static void LoadViewOnMainView(String fxmFile, String viewId, StackPane mainStackPane) {

        Optional<Node> existingNode = mainStackPane.getChildren().stream()
                .filter(node -> node.getId() != null && node.getId().equals(fxmFile))
                .findFirst();

        if (existingNode.isPresent()) {
            // الواجهة موجودة — بس نعرضها
            mainStackPane.getChildren().forEach(node -> node.setVisible(false));
            existingNode.get().setVisible(true);
        } else {
            try {
                FXMLLoader loader = new FXMLLoader(ViewManager.class.getResource(fxmFile));
                Parent view = loader.load();
                view.setId(fxmFile);

                // ✅ خطوط + زوم (الألوان من الـ Scene الرئيسية)
                applyViewSettings(viewId, view);

                mainStackPane.getChildren().forEach(node -> node.setVisible(false));
                mainStackPane.getChildren().add(view);

            } catch (IOException ex) {
                SAFNotification.error(ex.getMessage());
                log.error(ex.getMessage());
            }
        }
    }

    /** للتوافق مع الكود القديم */
    public static void LoadViewOnMainView(String fxmFile, StackPane mainStackPane) {
        LoadViewOnMainView(fxmFile, fxmFile, mainStackPane);
    }

    public static void LoadViewOnReportView(String fxmFile, StackPane mainStackPane) {
        LoadViewOnMainView(fxmFile, mainStackPane);
    }

    // ════════════════════════════════════════════════
    //  تحميل FXML بكنترولر مخصص
    // ════════════════════════════════════════════════

    public static Parent loadFXML(String relativePath, Object controller) {
        try {
            FXMLLoader loader = new FXMLLoader(controller.getClass().getResource(relativePath));
            loader.setController(controller);
            return loader.load();
        } catch (IOException e) {
            SAFNotification.error(e.getMessage());
            log.error(e.getMessage());
            return null;
        }
    }
}