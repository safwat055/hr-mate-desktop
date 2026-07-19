package com.safwat.hr.notification.ui;


import com.safwat.hr.notification.model.HRNotification;
import io.github.palexdev.materialfx.controls.MFXButton;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Toast إشعار فوري يظهر في زاوية الشاشة ويختفي تلقائياً.
 * يدعم تراكم عدة Toasts في وقت واحد.
 */
public class HRToast {

    private static final int TOAST_WIDTH = 360;
    private static final int DISPLAY_SECS = 5;
    private static final int MAX_VISIBLE = 4;
    private static final double BOTTOM_MARGIN = 24;
    private static final double RIGHT_MARGIN = 24;
    private static final double TOAST_HEIGHT = 76;
    private static final double GAP = 10;

    // قائمة الـ Toasts الظاهرة حالياً
    private static final Deque<Popup> activeToasts = new ArrayDeque<>();

    /**
     * يُظهر Toast جديداً في الزاوية السفلية اليمنى.
     */
    public static void show(Stage owner, HRNotification notification) {
        if (activeToasts.size() >= MAX_VISIBLE) {
            // أزل الأقدم
            Popup oldest = activeToasts.pollFirst();
            if (oldest != null) oldest.hide();
        }

        Popup popup = buildPopup(notification);
        popup.show(owner);
        activeToasts.addLast(popup);
        repositionAll(owner);
        animateIn(popup, owner, notification);
    }

    // ===================== بناء الـ Toast =====================
    private static Popup buildPopup(HRNotification n) {
        Popup popup = new Popup();
        popup.setAutoHide(false);

        // الحاوية الرئيسية
        HBox root = new HBox(12);
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPrefWidth(TOAST_WIDTH);
        root.setMinHeight(TOAST_HEIGHT);
        root.setPadding(new Insets(12, 16, 12, 14));
        root.setStyle(
                "-fx-background-color: #FFFFFF;" +
                        "-fx-background-radius: 10px;" +
                        "-fx-border-color: " + n.getType().color + ";" +
                        "-fx-border-width: 0 0 0 4px;" +
                        "-fx-border-radius: 10px 0 0 10px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 12, 0, 0, 4);"
        );

        // نقطة الأهمية
        Circle dot = new Circle(5);
        dot.setFill(Color.web(n.getType().color));

        // الأيقونة
        StackPane iconBox = buildIcon(n);

        // النصوص
        VBox texts = new VBox(3);
        texts.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(texts, Priority.ALWAYS);

        Label titleLbl = new Label(n.getTitle());
        titleLbl.setStyle("-fx-font-size:13px;-fx-font-weight:600;-fx-text-fill:#1a1a1a;");
        titleLbl.setWrapText(false);
        titleLbl.setMaxWidth(220);
        titleLbl.setEllipsisString("...");

        Label msgLbl = new Label(n.getMessage());
        msgLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#555555;");
        msgLbl.setWrapText(false);
        msgLbl.setMaxWidth(220);
        msgLbl.setEllipsisString("...");

        Label timeLbl = new Label(n.getFormattedTime());
        timeLbl.setStyle("-fx-font-size:11px;-fx-text-fill:#888888;");

        texts.getChildren().addAll(titleLbl, msgLbl, timeLbl);

        // زر إغلاق
        MFXButton closeBtn = new MFXButton("✕");
        closeBtn.setStyle("-fx-background-color:transparent;-fx-text-fill:#aaa;-fx-font-size:14px;-fx-cursor:hand;");
        closeBtn.setPrefSize(24, 24);
        closeBtn.setOnAction(e -> dismissToast(popup));

        root.getChildren().addAll(dot, iconBox, texts, closeBtn);

        // نوار تقدم الوقت
        VBox wrapper = new VBox(0);
        wrapper.getChildren().addAll(root, buildProgressBar(n));

        popup.getContent().add(wrapper);
        return popup;
    }

    private static StackPane buildIcon(HRNotification n) {
        Rectangle bg = new Rectangle(36, 36);
        bg.setArcWidth(8);
        bg.setArcHeight(8);
        bg.setFill(Color.web(n.getType().bgColor));

        Label icon = new Label(getIcon(n.getType()));
        icon.setStyle("-fx-font-size:18px;");

        StackPane box = new StackPane(bg, icon);
        box.setMinSize(36, 36);
        return box;
    }

    private static Rectangle buildProgressBar(HRNotification n) {
        Rectangle bar = new Rectangle(TOAST_WIDTH, 3);
        bar.setFill(Color.web(n.getType().color));
        bar.setArcWidth(3);
        bar.setArcHeight(3);

        // يتقلص من اليمين إلى الشمال خلال مدة العرض
        ScaleTransition scale = new ScaleTransition(
                Duration.seconds(DISPLAY_SECS), bar);
        scale.setFromX(1.0);
        scale.setToX(0.0);
        scale.play();

        return bar;
    }

    // ===================== الحركة =====================
    private static void animateIn(Popup popup, Stage owner, HRNotification n) {
        // Translate in من اليمين
        javafx.scene.Node root = popup.getContent().get(0);
        root.setTranslateX(TOAST_WIDTH + 30);
        root.setOpacity(0);

        TranslateTransition slide = new TranslateTransition(
                Duration.millis(280), root);
        slide.setToX(0);

        FadeTransition fade = new FadeTransition(Duration.millis(280), root);
        fade.setToValue(1.0);

        ParallelTransition intro = new ParallelTransition(slide, fade);
        intro.play();

        // اختفاء تلقائي
        intro.setOnFinished(e -> {
            PauseTransition pause = new PauseTransition(Duration.seconds(DISPLAY_SECS));
            pause.setOnFinished(pe -> dismissToast(popup));
            pause.play();
        });
    }

    private static void dismissToast(Popup popup) {
        javafx.scene.Node root = popup.getContent().isEmpty()
                ? null : popup.getContent().get(0);
        if (root == null) {
            popup.hide();
            return;
        }

        FadeTransition fade = new FadeTransition(Duration.millis(200), root);
        fade.setToValue(0);
        TranslateTransition slide = new TranslateTransition(Duration.millis(200), root);
        slide.setToX(TOAST_WIDTH + 30);

        ParallelTransition out = new ParallelTransition(fade, slide);
        out.setOnFinished(e -> {
            popup.hide();
            activeToasts.remove(popup);
        });
        out.play();
    }

    // ===================== إعادة تموضع الكل =====================
    private static void repositionAll(Stage owner) {
        double screenX = owner.getX() + owner.getWidth() - TOAST_WIDTH - RIGHT_MARGIN;
        double screenY = owner.getY() + owner.getHeight();

        int i = 0;
        for (Popup p : activeToasts) {
            double y = screenY - BOTTOM_MARGIN - (TOAST_HEIGHT + GAP) * (i + 1);
            p.setX(screenX);
            p.setY(y);
            i++;
        }
    }

    // ===================== مساعد الأيقونات =====================
    private static String getIcon(HRNotification.NotificationType type) {
        return switch (type) {
            case EMPLOYEE -> "👤";
            case SALARY -> "💰";
            case LEAVE -> "📅";
            case TRAINING -> "🎓";
            case TASK -> "✅";
            case SYSTEM -> "⚙️";
        };
    }
}
