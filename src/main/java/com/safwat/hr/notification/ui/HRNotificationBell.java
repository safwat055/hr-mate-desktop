package com.safwat.hr.notification.ui;

import com.safwat.hr.notification.service.NotificationService;
import javafx.animation.*;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * =====================================================================
 * HRNotificationBell
 * =====================================================================
 * زر الجرس في شريط الأدوات العلوي.
 * يعرض عدد الإشعارات غير المقروءة داخل شارة (badge).
 * يهتز عند وصول إشعار جديد.
 * يفتح لوحة الإشعارات (HRNotificationPanel) عند الضغط.
 * يتم بناء اللوحة من جديد في كل مرة لتجنب مشاكل scene graph.
 * <p>
 * الاستخدام:
 * toolbar.getChildren().add(new HRNotificationBell(primaryStage, bellIcon, badge));
 */
public class HRNotificationBell extends StackPane {

    private final NotificationService service = NotificationService.getInstance();
    private final Stage ownerStage;
    private Popup panelPopup = null;
    private boolean panelVisible = false;

    /**
     * إنشاء زر الجرس.
     *
     * @param ownerStage النافذة الرئيسية
     * @param bellIcon   عنصر Label يمثل أيقونة الجرس
     * @param badge      عنصر Label يمثل شارة العدد
     */
    public HRNotificationBell(Stage ownerStage, Label bellIcon, Label badge) {
        this.ownerStage = ownerStage;
        buildBell(bellIcon, badge);
    }

    /**
     * بناء واجهة زر الجرس وربطها بخدمة الإشعارات.
     * تضيف مستمع لتغير عدد الإشعارات لتحريك الجرس عند وصول إشعار جديد.
     *
     * @param bellIcon عنصر أيقونة الجرس
     * @param badge    عنصر شارة العدد
     */
    private void buildBell(Label bellIcon, Label badge) {
        bellIcon.setStyle(
                "-fx-font-size:20px;-fx-font-weight:700;" +
                        "-fx-text-fill:#555555;-fx-cursor:hand;"
        );

        badge.textProperty().bind(
                Bindings.when(service.unreadCountProperty().greaterThan(9))
                        .then("9+")
                        .otherwise(service.unreadCountProperty().asString())
        );
        badge.visibleProperty().bind(service.unreadCountProperty().greaterThan(0));
        badge.managedProperty().bind(badge.visibleProperty());
        badge.setStyle(
                "-fx-background-color:#A32D2D;-fx-text-fill:white;" +
                        "-fx-font-size:9px;-fx-font-weight:700;" +
                        "-fx-min-width:16px;-fx-min-height:16px;" +
                        "-fx-background-radius:8px;-fx-padding:1 3 1 3;"
        );
        StackPane.setAlignment(badge, Pos.TOP_RIGHT);

        getChildren().addAll(bellIcon, badge);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(6));
        setStyle("-fx-cursor:hand;");

        service.unreadCountProperty().addListener((obs, old, nw) -> {
            if (nw.intValue() > old.intValue()) {
                shake(bellIcon);
                bounceBadge(badge);
            }
        });

        setOnMouseClicked(e -> togglePanel());
    }

    /**
     * فتح أو إغلاق لوحة الإشعارات.
     * إذا كانت مفتوحة تغلقها، وإذا كانت مغلقة تنشئ لوحة جديدة وتعرضها.
     */
    private void togglePanel() {
        if (panelVisible && panelPopup != null) {
            panelPopup.hide();
            panelPopup = null;
            panelVisible = false;
            return;
        }

        HRNotificationPanel freshPanel = new HRNotificationPanel(ownerStage);
        panelPopup = new Popup();
        panelPopup.setAutoHide(true);
        panelPopup.getContent().add(freshPanel);
        panelPopup.setOnHidden(e -> {
            panelVisible = false;
            panelPopup = null;
        });

        double x = localToScreen(getBoundsInLocal()).getMaxX() - 440;
        double y = localToScreen(getBoundsInLocal()).getMaxY() + 8;
        panelPopup.show(ownerStage, x, y);
        panelVisible = true;
        animatePanelIn(freshPanel);
    }

    /**
     * تشغيل حركة ظهور اللوحة (تلاشي + انزلاق من الأعلى).
     *
     * @param panel لوحة الإشعارات المراد تحريكها
     */
    private void animatePanelIn(HRNotificationPanel panel) {
        panel.setOpacity(0);
        panel.setTranslateY(-10);

        FadeTransition fade = new FadeTransition(Duration.millis(180), panel);
        fade.setToValue(1.0);
        TranslateTransition slide = new TranslateTransition(Duration.millis(180), panel);
        slide.setToY(0);

        new ParallelTransition(fade, slide).play();
    }

    /**
     * تشغيل حركة اهتزاز أيقونة الجرس.
     *
     * @param bell عنصر أيقونة الجرس
     */
    private void shake(Label bell) {
        RotateTransition r = new RotateTransition(Duration.millis(80), bell);
        r.setByAngle(15);
        r.setCycleCount(4);
        r.setAutoReverse(true);
        r.play();
    }

    /**
     * تشغيل حركة تكبير شارة العدد مؤقتاً.
     *
     * @param badge عنصر شارة العدد
     */
    private void bounceBadge(Label badge) {
        ScaleTransition s = new ScaleTransition(Duration.millis(150), badge);
        s.setFromX(1.0);
        s.setFromY(1.0);
        s.setToX(1.4);
        s.setToY(1.4);
        s.setAutoReverse(true);
        s.setCycleCount(2);
        s.play();
    }
}