package com.safwat.hr.notification.util;

import javafx.scene.control.Alert;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

/**
 * =====================================================
 *  FileOpener — فتح الملفات والروابط الداخلية
 * =====================================================
 *
 *  الاستخدام:
 *    FileOpener.open("/reports/salary.pdf");       // ملف
 *    FileOpener.open("employee/profile/123");      // رابط داخلي
 */
public class FileOpener {

    public static void open(String target) {
        if (target == null || target.isBlank()) return;

        if (isFilePath(target)) openFile(target);
        else                    navigateTo(target);
    }

    private static boolean isFilePath(String target) {
        return target.startsWith("/")
            || target.startsWith("./")
            || target.contains(":\\")   // Windows path
            || target.endsWith(".pdf")
            || target.endsWith(".xlsx")
            || target.endsWith(".xls")
            || target.endsWith(".zip")
            || target.endsWith(".jpg")
            || target.endsWith(".png");
    }

    private static void openFile(String path) {
        File file = new File(path);

        // لو مسار نسبي — ابحث من مجلد التشغيل
        if (!file.isAbsolute())
            file = new File(System.getProperty("user.dir"), path);

        if (!file.exists()) {
            showError("الملف غير موجود",
                "المسار: " + file.getAbsolutePath() + "\nتأكد من وجود الملف.");
            return;
        }

        if (!Desktop.isDesktopSupported()) {
            showError("غير مدعوم", "لا يدعم النظام فتح الملفات تلقائياً.\nالمسار: " + path);
            return;
        }

        final File finalFile = file;
        new Thread(() -> {
            try { Desktop.getDesktop().open(finalFile); }
            catch (IOException e) {
                javafx.application.Platform.runLater(() ->
                    showError("فشل فتح الملف", e.getMessage()));
            }
        }, "file-opener").start();
    }

    private static void navigateTo(String route) {
        // في التطبيق الحقيقي: AppRouter.getInstance().navigate(route)
        System.out.println("[FileOpener] التنقل إلى: " + route);
    }

    private static void showError(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.show();
    }
}
