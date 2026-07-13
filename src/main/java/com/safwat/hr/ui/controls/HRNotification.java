package com.safwat.hr.ui.controls;

import com.safwat.hr.ui.animation.Fade;
import com.safwat.hr.ui.style.Elevation;
import com.safwat.hr.ui.style.Radius;
import com.safwat.hr.ui.style.Theme;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * HRNotification — Material toast notifications.
 * <p>
 * Usage:
 * <pre>
 *   HRNotification.success("تم الحفظ بنجاح");
 *   HRNotification.error("حدث خطأ، يرجى المحاولة");
 *   HRNotification.warning("تحقق من البيانات");
 *   HRNotification.info("جاري التحميل...");
 *
 *   // With custom duration (ms):
 *   HRNotification.success("تم الحفظ", 4000);
 * </pre>
 */
public final class HRNotification {

    private static final int DEFAULT_DURATION_MS = 3000;

    private HRNotification() {
    }

    public static void success(String message) {
        show(message, "✔", Theme.SUCCESS, DEFAULT_DURATION_MS);
    }

    public static void success(String message, int durationMs) {
        show(message, "✔", Theme.SUCCESS, durationMs);
    }

    public static void error(String message) {
        show(message, "✖", Theme.ERROR, DEFAULT_DURATION_MS);
    }

    public static void error(String message, int durationMs) {
        show(message, "✖", Theme.ERROR, durationMs);
    }

    public static void warning(String message) {
        show(message, "⚠", Theme.WARNING, DEFAULT_DURATION_MS);
    }

    public static void warning(String message, int durationMs) {
        show(message, "⚠", Theme.WARNING, durationMs);
    }

    public static void info(String message) {
        show(message, "ℹ", Theme.INFO, DEFAULT_DURATION_MS);
    }

    // ─── Core renderer ───────────────────────────────────────────────

    public static void info(String message, int durationMs) {
        show(message, "ℹ", Theme.INFO, durationMs);
    }

    private static void show(String message, String icon, String color, int durationMs) {
        Window owner = Stage.getWindows().stream()
                .filter(Window::isShowing).findFirst().orElse(null);
        if (owner == null) return;

        // Icon label
        Label iconLabel = new Label(icon);
        iconLabel.setStyle(
                "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;"
        );

        // Message label
        Label msgLabel = new Label(message);
        msgLabel.setStyle(
                "-fx-text-fill: white;" +
                        "-fx-font-size: " + Theme.FONT_SIZE_MD + "px;" +
                        "-fx-font-family: '" + Theme.FONT_FAMILY + "';"
        );
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(320);

        // Container
        HBox box = new HBox(10, iconLabel, msgLabel);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(12, 20, 12, 16));
        box.setStyle(
                "-fx-background-color: " + color + ";" +
                        "-fx-background-radius: " + Radius.LG + ";" +
                        "-fx-effect: " + Elevation.E3 + ";"
        );

        StackPane root = new StackPane(box);
        root.setStyle("-fx-background-color: transparent;");

        // Popup
        Popup popup = new Popup();
        popup.getContent().add(root);
        popup.setAutoHide(true);

        // ====== الحل الأسهل: استخدم عرض ثابت ======
        double popupWidth = 380; // نفس القيمة القديمة
        double x = owner.getX() + (owner.getWidth() - popupWidth) / 2;
        double y = owner.getY() + 20;
        popup.show(owner, x, y);
        // Fade in
        Fade.in(root, 250);

        // Auto dismiss
        PauseTransition pause = new PauseTransition(Duration.millis(durationMs));
        pause.setOnFinished(e ->
                Fade.out(root, 300, () -> popup.hide())
        );
        pause.play();
    }


    // ====== NEW: Notification with action buttons ======

    /**
     * عرض إشعار مع زر لفتح الملف وزر للإغلاق
     *
     * @param message نص الإشعار
     * @param file    الملف المراد فتحه
     */
    public static void withAction(String message, File file) {
        showWithAction(message, "📄", Theme.SUCCESS, file);
    }

    /**
     * عرض إشعار مع زر لفتح الملف وزر للإغلاق (لون مخصص)
     */
    public static void withAction(String message, String icon, String color, File file) {
        showWithAction(message, icon, color, file);
    }

    private static void showWithAction(String message, String icon, String color, File file) {
        Window owner = Stage.getWindows().stream()
                .filter(Window::isShowing).findFirst().orElse(null);
        if (owner == null) return;

        // Icon label
        Label iconLabel = new Label(icon);
        iconLabel.setStyle(
                "-fx-text-fill: white;" +
                        "-fx-font-size: 18px;" +
                        "-fx-font-weight: bold;"
        );

        // Message label
        Label msgLabel = new Label(message);
        msgLabel.setStyle(
                "-fx-text-fill: white;" +
                        "-fx-font-size: " + Theme.FONT_SIZE_MD + "px;" +
                        "-fx-font-family: '" + Theme.FONT_FAMILY + "';" +
                        "-fx-font-weight: bold;"
        );
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(300);

        // ====== Container للرسالة والأزرار ======
        HBox contentBox = new HBox(12, iconLabel, msgLabel);
        contentBox.setAlignment(Pos.CENTER_LEFT);

        // ====== التنسيق العام ======
        VBox mainBox = new VBox(10);
        mainBox.setPadding(new Insets(14, 20, 14, 16));
        mainBox.setStyle(
                "-fx-background-color: " + color + ";" +
                        "-fx-background-radius: " + Radius.LG + ";" +
                        "-fx-effect: " + Elevation.E3 + ";"
        );
        mainBox.getChildren().add(contentBox);

        StackPane root = new StackPane(mainBox);
        root.setStyle("-fx-background-color: transparent;");

        // Popup
        Popup popup = new Popup();
        popup.getContent().add(root);
        popup.setAutoHide(false);

        // ====== Method للإغلاق ======
        Runnable closePopup = () -> {
            if (popup.isShowing()) {
                Fade.out(root, 300, popup::hide);
            }
        };

        // ====== زر فتح الملف ======
        Button openButton = new Button("📂 فتح");
        openButton.setStyle(
                "-fx-background-color: rgba(255,255,255,0.2);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 6 16 6 16;" +
                        "-fx-background-radius: 20;" +
                        "-fx-cursor: hand;"
        );
        openButton.setOnAction(e -> {
            CompletableFuture.runAsync(() -> {
                try {
                    if (file != null && file.exists()) {
                        Desktop.getDesktop().open(file);
                        // ====== إغلاق الـ Popup بعد فتح الملف ======
                        Platform.runLater(closePopup);
                    } else {
                        Platform.runLater(() ->
                                HRNotification.error("الملف غير موجود"));
                    }
                } catch (IOException ex) {
                    Platform.runLater(() ->
                            HRNotification.error("لا يمكن فتح الملف: " + ex.getMessage()));
                }
            });
        });

        // ====== زر فتح المجلد ======
        Button openFolderButton = new Button("📁 فتح المجلد");
        openFolderButton.setStyle(
                "-fx-background-color: rgba(255,255,255,0.2);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 6 16 6 16;" +
                        "-fx-background-radius: 20;" +
                        "-fx-cursor: hand;"
        );
        openFolderButton.setOnAction(e -> {
            if (file != null && file.exists()) {
                CompletableFuture.runAsync(() -> {
                    try {
                        Desktop.getDesktop().open(file.getParentFile());
                        // ====== إغلاق الـ Popup بعد فتح المجلد ======
                        Platform.runLater(closePopup);
                    } catch (IOException ex) {
                        Platform.runLater(() ->
                                HRNotification.error("لا يمكن فتح المجلد: " + ex.getMessage()));
                    }
                });
            } else {
                HRNotification.error("المجلد غير موجود");
            }
        });

        // ====== زر الإغلاق ======
        Button closeButton = new Button("اغلاق");
        closeButton.setStyle(
                "-fx-background-color: rgba(255,255,255,0.2);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 4 10 4 10;" +
                        "-fx-background-radius: 20;" +
                        "-fx-cursor: hand;"
        );
        closeButton.setOnAction(e -> closePopup.run());

        // ====== إضافة الأزرار ======
        HBox buttonBox = new HBox(10, openButton, openFolderButton, closeButton);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        mainBox.getChildren().add(buttonBox);

        // Position: top-center
        double popupWidth = 420;
        double x = owner.getX() + (owner.getWidth() - popupWidth) / 2;
        double y = owner.getY() + 20;
        popup.show(owner, x, y);

        // Fade in
        Fade.in(root, 250);
    }
}
