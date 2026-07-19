package com.safwat.hr.notification.util;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;

import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * أداة لفتح الملفات والروابط من الإشعارات.
 * تدعم PDF, Excel, ZIP وروابط التطبيق الداخلية.
 */
public class FileOpener {

    /**
     * فتح ملف أو رابط.
     *
     * @param target مسار ملف أو رابط داخلي (مثل: "employee/profile/123")
     */
    public static void open(String target) {
        if (target == null || target.isBlank()) return;

        if (target.startsWith("/") || target.contains(":\\")) {
            // مسار ملف حقيقي
            openFile(target);
        } else {
            // رابط تنقل داخلي
            navigateTo(target);
        }
    }

    private static void openFile(String path) {
        File file = new File(path);
        if (!file.exists()) {
            showError("الملف غير موجود", "المسار: " + path +
                    "\nتأكد من وجود الملف أو أعد التنزيل.");
            return;
        }

        if (!Desktop.isDesktopSupported()) {
            showError("غير مدعوم",
                    "لا يدعم النظام فتح الملفات تلقائياً.\nالمسار: " + path);
            return;
        }

        new Thread(() -> {
            try {
                Desktop.getDesktop().open(file);
            } catch (IOException e) {
                Platform.runLater(() ->
                        showError("فشل فتح الملف", e.getMessage()));
            }
        }, "file-opener").start();
    }

    private static void navigateTo(String route) {
        // في التطبيق الحقيقي: ربط بـ Router أو NavigationService
        System.out.println("[FileOpener] التنقل إلى: " + route);
        // مثال: AppRouter.getInstance().navigate(route);
    }

    /**
     * فتح ملف باختيار المستخدم إذا لم يكن المسار محدداً.
     */
    public static void openWithDialog(String extension, String description) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("فتح ملف");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(description, "*." + extension)
        );
        File file = chooser.showOpenDialog(null);
        if (file != null) openFile(file.getAbsolutePath());
    }

    private static void showError(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.show();
    }
}
