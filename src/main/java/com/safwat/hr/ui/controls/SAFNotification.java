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
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * HRNotification — Material toast notifications.
 * <p>
 * Usage:
 * <pre>
 *   SAFNotification.success("تم الحفظ بنجاح");
 *   SAFNotification.error("حدث خطأ، يرجى المحاولة");
 *   SAFNotification.warning("تحقق من البيانات");
 *   SAFNotification.info("جاري التحميل...");
 *
 *   // With custom duration (ms):
 *   SAFNotification.success("تم الحفظ", 4000);
 *
 *   // With action buttons (open file / folder):
 *   SAFNotification.withAction("تم تحميل التقرير", file);
 * </pre>
 */
public final class SAFNotification {

    private static final int DEFAULT_DURATION_MS = 3000;
    private static final int ACTION_DURATION_MS = 10_000;   // ⏱ للـ popup بالأزرار
    private static final double POPUP_WIDTH = 420;

    /**
     * الـ Popup الحالي — عشان ما يتراكموش فوق بعض
     */
    private static Popup currentToast;
    private static Popup currentAction;

    private SAFNotification() {
    }

    // ══════════════════════════════════════════════════════════════
    //  Public API
    // ══════════════════════════════════════════════════════════════

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

    public static void info(String message, int durationMs) {
        show(message, "ℹ", Theme.INFO, durationMs);
    }

    /**
     * إشعار مع أزرار (فتح الملف / فتح المجلد / إغلاق).
     */
    public static void withAction(String message, File file) {
        showWithAction(message, "📄", Theme.SUCCESS, file);
    }

    public static void withAction(String message, String icon, String color, File file) {
        showWithAction(message, icon, color, file);
    }

    // ══════════════════════════════════════════════════════════════
    //  Core — Toast بدون أزرار
    // ══════════════════════════════════════════════════════════════

    private static void show(String message, String icon, String color, int durationMs) {

        // ① اقفل أي toast قديم
        if (currentToast != null && currentToast.isShowing()) {
            currentToast.hide();
        }

        Window owner = resolveOwner();
        if (owner == null) return;

        Label iconLabel = buildIconLabel(icon, 14);
        Label msgLabel = buildMessageLabel(message, false);
        msgLabel.setMaxWidth(320);

        HBox box = new HBox(10, iconLabel, msgLabel);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(12, 20, 12, 16));
        box.setStyle(
                "-fx-background-color: " + color + ";" +
                        "-fx-background-radius: " + Radius.LG + ";" +
                        "-fx-effect: " + Elevation.E3 + ";");

        StackPane root = new StackPane(box);
        root.setStyle("-fx-background-color: transparent;");

        Popup popup = new Popup();
        popup.getContent().add(root);
        popup.setAutoHide(false);
        popup.setAutoFix(true);

        // ② ESC للإغلاق
        root.setFocusTraversable(true);
        root.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) hidePopup(popup, root);
        });

        // ③ اعرض + موقع
        centerTop(popup, owner, POPUP_WIDTH);
        Fade.in(root, 250);
        root.requestFocus();

        // ④ إغلاق تلقائي
        PauseTransition pause = new PauseTransition(Duration.millis(durationMs));
        pause.setOnFinished(e -> hidePopup(popup, root));
        pause.play();

        // ⑤ لو المستخدم عمل hover — أوقف الإغلاق
        root.setOnMouseEntered(e -> pause.pause());
        root.setOnMouseExited(e -> pause.playFromStart());

        // ⑥ تتبّع الحالي
        currentToast = popup;
        popup.setOnHidden(e -> {
            if (currentToast == popup) currentToast = null;
        });
    }

    // ══════════════════════════════════════════════════════════════
    //  Core — Popup مع أزرار
    // ══════════════════════════════════════════════════════════════

    private static void showWithAction(String message, String icon, String color, File file) {

        // ① اقفل أي popup قديم
        if (currentAction != null && currentAction.isShowing()) {
            currentAction.hide();
        }

        Window owner = resolveOwner();
        if (owner == null) return;

        Label iconLabel = buildIconLabel(icon, 18);
        Label msgLabel = buildMessageLabel(message, true);
        msgLabel.setMaxWidth(300);

        HBox contentBox = new HBox(12, iconLabel, msgLabel);
        contentBox.setAlignment(Pos.CENTER_LEFT);

        VBox mainBox = new VBox(10);
        mainBox.setPadding(new Insets(14, 20, 14, 16));
        mainBox.setPrefWidth(POPUP_WIDTH);
        mainBox.setMaxWidth(POPUP_WIDTH);
        mainBox.setStyle(
                "-fx-background-color: " + color + ";" +
                        "-fx-background-radius: " + Radius.LG + ";" +
                        "-fx-effect: " + Elevation.E3 + ";");
        mainBox.getChildren().add(contentBox);

        StackPane root = new StackPane(mainBox);
        root.setStyle("-fx-background-color: transparent;");

        Popup popup = new Popup();
        popup.getContent().add(root);
        popup.setAutoHide(false);
        popup.setAutoFix(true);

        // ② closePopup مع flag لمنع النداء المزدوج
        AtomicBoolean closing = new AtomicBoolean(false);
        Runnable closePopup = () -> {
            if (closing.compareAndSet(false, true) && popup.isShowing()) {
                Fade.out(root, 300, () -> {
                    popup.hide();
                    if (currentAction == popup) currentAction = null;
                });
            }
        };

        // ③ الأزرار
        Button openButton = buildPopupButton("📂 فتح");
        openButton.setOnAction(e -> openInBackground(file, closePopup, false));

        Button openFolderButton = buildPopupButton("📁 فتح المجلد");
        openFolderButton.setOnAction(e -> openInBackground(file, closePopup, true));

        Button closeButton = buildPopupButton("اغلاق");
        closeButton.setOnAction(e -> closePopup.run());

        HBox buttonBox = new HBox(10, openButton, openFolderButton, closeButton);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        mainBox.getChildren().add(buttonBox);

        // ④ ESC + auto-dismiss مع pause on hover
        root.setFocusTraversable(true);
        root.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) closePopup.run();
        });

        PauseTransition autoClose = new PauseTransition(Duration.millis(ACTION_DURATION_MS));
        autoClose.setOnFinished(e -> closePopup.run());
        root.setOnMouseEntered(e -> autoClose.pause());
        root.setOnMouseExited(e -> autoClose.playFromStart());

        // ⑤ اعرض + موقع
        centerTop(popup, owner, POPUP_WIDTH);
        Fade.in(root, 250);
        root.requestFocus();
        autoClose.play();

        // ⑥ تتبّع الحالي
        currentAction = popup;
        popup.setOnHidden(e -> {
            if (currentAction == popup) currentAction = null;
        });
    }

    // ══════════════════════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════════════════════

    /**
     * يحدد الـ owner بأمان — Primary Stage فقط (مش أي Window عارض).
     */
    private static Window resolveOwner() {
        return Stage.getWindows().stream()
                .filter(w -> w instanceof Stage s
                        && s.getModality() == Modality.NONE
                        && w.isShowing())
                .findFirst()
                .orElse(null);
    }

    /**
     * يوسّط الـ popup أعلى النافذة المالكة.
     */
    private static void centerTop(Popup popup, Window owner, double popupWidth) {
        double x = owner.getX() + (owner.getWidth() - popupWidth) / 2;
        double y = owner.getY() + 20;
        popup.show(owner, x, y);
    }

    /**
     * إغلاق الـ popup مع animation.
     */
    private static void hidePopup(Popup popup, StackPane root) {
        if (popup.isShowing()) {
            Fade.out(root, 300, popup::hide);
        }
    }

    /**
     * بناء Label للأيقونة.
     */
    private static Label buildIconLabel(String icon, int fontSize) {
        Label l = new Label(icon);
        l.setStyle(
                "-fx-text-fill: white;" +
                        "-fx-font-size: " + fontSize + "px;" +
                        "-fx-font-weight: bold;");
        return l;
    }

    /**
     * بناء Label للرسالة.
     */
    private static Label buildMessageLabel(String message, boolean bold) {
        Label l = new Label(message);
        l.setStyle(
                "-fx-text-fill: white;" +
                        "-fx-font-size: " + Theme.FONT_SIZE_MD + "px;" +
                        "-fx-font-family: '" + Theme.FONT_FAMILY + "';" +
                        (bold ? "-fx-font-weight: bold;" : ""));
        l.setWrapText(true);
        return l;
    }

    /**
     * زر موحّد الستايل للـ popup بالأزرار.
     */
    private static Button buildPopupButton(String text) {
        Button b = new Button(text);
        b.setStyle(
                "-fx-background-color: rgba(255,255,255,0.2);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 6 16 6 16;" +
                        "-fx-background-radius: 20;" +
                        "-fx-cursor: hand;");
        return b;
    }

    /**
     * يفتح الملف أو المجلد في thread منفصل + يقفل الـ popup بعد النجاح.
     */
    private static void openInBackground(File file, Runnable closePopup, boolean openFolder) {

        if (file == null || !file.exists()) {
            Platform.runLater(() -> error("الملف غير موجود"));
            return;
        }

        if (!Desktop.isDesktopSupported()) {
            Platform.runLater(() -> error("فتح الملفات غير مدعوم على هذا النظام"));
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                File target = openFolder ? file.getParentFile() : file;
                if (target == null || !target.exists()) {
                    Platform.runLater(() -> error("المجلد غير موجود"));
                    return;
                }
                Desktop.getDesktop().open(target);
                Platform.runLater(closePopup);

            } catch (IOException ex) {
                Platform.runLater(() -> error("لا يمكن فتح الملف: " + ex.getMessage()));
            } catch (UnsupportedOperationException ex) {
                Platform.runLater(() -> error("النظام لا يدعم هذه العملية"));
            }
        });
    }
}