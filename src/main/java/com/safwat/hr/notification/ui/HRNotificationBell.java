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
 * زر الجرس في الـ Toolbar.
 * <p>
 * تغيير مهم: يبني Popup و Panel جديدين في كل مرة يُفتح فيها
 * لتجنب bug الـ scene graph في JavaFX 25.
 */
public class HRNotificationBell extends StackPane {
    private final NotificationService service = NotificationService.getInstance();
    private Stage ownerStage;
    private Popup panelPopup = null;
    private boolean panelVisible = false;

    public HRNotificationBell(Label bellLabel, Label badge) {
        buildBell(bellLabel, badge);
    }

    private void buildBell(Label bellIcon, Label badge) {
        // bellIcon = new Label("[B]");
        bellIcon.setStyle("-fx-font-size:20px;-fx-font-weight:700;" +
                "-fx-text-fill:#555555;-fx-cursor:hand;");

        //  Label badge = new Label();
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

        // هزة عند إشعار جديد
        service.unreadCountProperty().addListener((obs, old, nw) -> {
            if (nw.intValue() > old.intValue()) {
                shake(bellIcon);
                animateBadge(badge);
            }
        });

        setOnMouseClicked(e -> {
            if (ownerStage == null)
                ownerStage = (Stage) getScene().getWindow();
            togglePanel();
        });
    }

    private void togglePanel() {
        if (panelVisible && panelPopup != null) {
            panelPopup.hide();
            panelPopup = null;
            panelVisible = false;
            return;
        }

        // بناء جديد في كل مرة — يحل bug الـ scene graph
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

    private void shake(Label bell) {
        RotateTransition r = new RotateTransition(Duration.millis(80), bell);
        r.setByAngle(15);
        r.setCycleCount(4);
        r.setAutoReverse(true);
        r.play();
    }

    private void animateBadge(Label badge) {
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
