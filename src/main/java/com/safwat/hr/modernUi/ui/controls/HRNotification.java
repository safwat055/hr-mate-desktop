package com.safwat.hr.modernUi.ui.controls;


import com.safwat.hr.modernUi.ui.style.Theme;
import io.github.palexdev.materialfx.controls.MFXIconWrapper;
import io.github.palexdev.materialfx.controls.MFXSimpleNotification;
import io.github.palexdev.materialfx.enums.NotificationPos;
import io.github.palexdev.materialfx.factories.InsetsFactory;

import io.github.palexdev.materialfx.notifications.MFXNotificationSystem;
import io.github.palexdev.mfxresources.fonts.MFXFontIcon;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * ─────────────────────────────────────────────────────────────
 * HRNotification — Facade فوق MFXNotificationSystem.
 * <p>
 * قبل الاستخدام: استدعِ init() مرة واحدة في Main.start()
 *
 * <pre>
 *    // في Main.start():
 *    HRNotification.init(primaryStage);
 *
 *    // في أي controller:
 *    HRNotification.success("تم الحفظ بنجاح");
 *    HRNotification.error("حدث خطأ غير متوقع");
 *    HRNotification.warning("تحقق من البيانات المدخلة");
 *    HRNotification.info("جاري معالجة الطلب...");
 *
 *    // مع عنوان مخصص:
 *    HRNotification.success("عنوان", "تم الحفظ بنجاح");
 *  </pre>
 * ─────────────────────────────────────────────────────────────
 */
public final class HRNotification {

    private static boolean initialized = false;

    private HRNotification() {
    }

    // ── Static API ───────────────────────────────────────────

    /**
     * يجب استدعاؤه مرة واحدة في Main.start() بعد show()
     */
    public static void init(Stage stage) {
        if (initialized) return;
        MFXNotificationSystem.instance()
                .initOwner(stage)
                .setPosition(NotificationPos.BOTTOM_RIGHT);
        // .setSpacing(10);
        initialized = true;
    }

    public static void success(String message) {
        success("نجاح", message);
    }

    public static void success(String title, String message) {
        show(title, message, "mfx-variant7-mark", Theme.SUCCESS);
    }

    public static void error(String message) {
        error("خطأ", message);
    }

    public static void error(String title, String message) {
        show(title, message, "mfx-x-circle", Theme.ERROR);
    }

    public static void warning(String message) {
        warning("تحذير", message);
    }

    public static void warning(String title, String message) {
        show(title, message, "mfx-exclamation-circle", Theme.WARNING);
    }

    public static void info(String message) {
        info("معلومة", message);
    }

    // ── Core ─────────────────────────────────────────────────

    public static void info(String title, String message) {
        show(title, message, "mfx-info-circle", Theme.INFO);
    }

    private static void show(String title, String message, String iconCode, String color) {
        Platform.runLater(() -> {
            // الأيقونة
            MFXFontIcon icon = new MFXFontIcon(iconCode, 20);
            icon.setStyle("-fx-color: " + color + ";");
            MFXIconWrapper iconWrapper = new MFXIconWrapper(icon, 32);

            // العنوان
            Label titleLabel = new Label(title);
            titleLabel.setStyle(
                    "-fx-font-family: '" + Theme.FONT_FAMILY + "';" +
                            "-fx-font-size: " + Theme.FONT_MD + "px;" +
                            "-fx-font-weight: bold;" +
                            "-fx-text-fill: " + color + ";"
            );

            // النص
            Label msgLabel = new Label(message);
            msgLabel.setStyle(
                    "-fx-font-family: '" + Theme.FONT_FAMILY + "';" +
                            "-fx-font-size: " + Theme.FONT_SM + "px;" +
                            "-fx-text-fill: " + Theme.ON_SURFACE + ";" +
                            "-fx-wrap-text: true;"
            );
            msgLabel.setWrapText(true);
            msgLabel.setMaxWidth(260);

            VBox textBox = new VBox(3, titleLabel, msgLabel);
            textBox.setAlignment(Pos.CENTER_LEFT);

            HBox content = new HBox(12, iconWrapper, textBox);
            content.setAlignment(Pos.CENTER_LEFT);
            content.setPadding(InsetsFactory.all(12));
            content.setStyle(
                    "-fx-background-color: " + Theme.SURFACE + ";" +
                            "-fx-background-radius: 8px;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 12, 0, 0, 3);" +
                            "-fx-border-color: " + color + ";" +
                            "-fx-border-width: 0 0 0 4;" +   // حد ملون على اليسار فقط
                            "-fx-border-radius: 8px 0 0 8px;"
            );
            content.setPrefWidth(300);

            MFXSimpleNotification notification = new MFXSimpleNotification(content);

            MFXNotificationSystem.instance()
                    .setCloseAfter(Duration.seconds(3))
                    .publish(notification);
        });
    }
}
