package com.safwat.hr.notification.ui;


import com.safwat.hr.notification.service.NotificationService;
import javafx.animation.*;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import javafx.util.Duration;

/**
 * زر الجرس في شريط التطبيق.
 * يعرض باج بعدد الإشعارات غير المقروءة وعند الضغط يُظهر HRNotificationPanel.
 */
public class HRNotificationBell extends StackPane {

    private final NotificationService service = NotificationService.getInstance();

    private Popup panelPopup;
    private boolean panelVisible = false;

    public HRNotificationBell() {

        buildBell();
    }

    private void buildBell() {
        // أيقونة الجرس
        Label bellIcon = new Label("🔔");
        bellIcon.setStyle("-fx-font-size:22px;-fx-cursor:hand;");

        // باج العداد
        Label badge = new Label();
        badge.textProperty().bind(
                Bindings.when(service.unreadCountProperty().greaterThan(9))
                        .then("9+")
                        .otherwise(service.unreadCountProperty().asString())
        );
        badge.visibleProperty().bind(service.unreadCountProperty().greaterThan(0));
        badge.setStyle(
                "-fx-background-color:#A32D2D;-fx-text-fill:white;" +
                        "-fx-font-size:9px;-fx-font-weight:700;" +
                        "-fx-min-width:16px;-fx-min-height:16px;" +
                        "-fx-background-radius:8px;-fx-padding:1 3 1 3;"
        );
        StackPane.setAlignment(badge, Pos.TOP_RIGHT);
        StackPane.setMargin(badge, new Insets(0, 0, 0, 0));

        getChildren().addAll(bellIcon, badge);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(6));
        setStyle("-fx-cursor:hand;");

        // تأثير الهزة عند وصول إشعار جديد
        service.unreadCountProperty().addListener((obs, old, nw) -> {
            if (nw.intValue() > old.intValue()) {
                shake(bellIcon);
                animateBadge(badge);
            }
        });

        // فتح/إغلاق اللوحة عند الضغط
        setOnMouseClicked(e -> togglePanel());
    }

    // ===================== اللوحة المنبثقة =====================
    private void togglePanel() {
        if (panelVisible && panelPopup != null) {
            panelPopup.hide();
            panelPopup = null;   // احذف القديم خالص
            panelVisible = false;
            return;
        }

        // ابني جديد في كل مرة
        HRNotificationPanel freshPanel = new HRNotificationPanel();
        panelPopup = new Popup();
        panelPopup.setAutoHide(true);
        panelPopup.getContent().add(freshPanel);
        panelPopup.setOnHidden(e -> {
            panelVisible = false;
            panelPopup = null;
        });

        double x = localToScreen(getBoundsInLocal()).getMaxX() - 420;
        double y = localToScreen(getBoundsInLocal()).getMaxY() + 8;
        panelPopup.show(getScene().getWindow(), x, y);
        panelVisible = true;
        animatePanelIn(freshPanel);
    }

    private void animatePanelIn(HRNotificationPanel panel) {
        panel.setOpacity(0);
        panel.setTranslateY(-10);

        FadeTransition fade = new FadeTransition(Duration.millis(180), panel);
        fade.setToValue(1.0);

        TranslateTransition slide = new TranslateTransition(Duration.millis(180), panel);
        slide.setToY(0);

        new ParallelTransition(fade, slide).play();
    }

   
    // ===================== الحركات =====================
    private void shake(Label bell) {
        RotateTransition rotate = new RotateTransition(Duration.millis(80), bell);
        rotate.setByAngle(15);
        rotate.setCycleCount(4);
        rotate.setAutoReverse(true);
        rotate.play();
    }

    private void animateBadge(Label badge) {
        ScaleTransition bounce = new ScaleTransition(Duration.millis(150), badge);
        bounce.setFromX(1.0);
        bounce.setFromY(1.0);
        bounce.setToX(1.4);
        bounce.setToY(1.4);
        bounce.setAutoReverse(true);
        bounce.setCycleCount(2);
        bounce.play();
    }
}
