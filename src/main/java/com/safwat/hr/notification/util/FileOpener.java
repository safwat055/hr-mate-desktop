package com.safwat.hr.notification.util;

import javafx.scene.control.Alert;

import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * =====================================================================
 * FileOpener
 * =====================================================================
 * أداة لفتح الملفات والروابط الداخلية.
 * تميز بين مسار ملف حقيقي (تفتحه Desktop) ومسار تنقل داخلي في التطبيق.
 * تستثني مسارات API من اعتبارها ملفات.
 * <p>
 * الاستخدام:
 * FileOpener.open("/reports/salary.xlsx");
 * FileOpener.open("employees/123");
 */
public class FileOpener {

    /**
     * فتح هدف (ملف أو مسار داخلي).
     * إذا كان مسار ملف يتم فتحه بالبرنامج الافتراضي.
     * إذا كان مسار داخلي يتم تمريره لمنظومة التنقل.
     *
     * @param target المسار أو الهدف
     */
    public static void open(String target) {
        if (target == null || target.isBlank()) return;

        if (isFilePath(target)) openFile(target);
        else navigateTo(target);
    }

    /**
     * التحقق مما إذا كان النص يمثل مسار ملف.
     * تستثني مسارات API مثل /api/ و /messages/.
     *
     * @param target النص المراد فحصه
     * @return true إذا كان مسار ملف
     */
    private static boolean isFilePath(String target) {
        if (target == null || target.isBlank()) return false;

        if (target.startsWith("/api/") || target.startsWith("/messages/"))
            return false;

        return target.startsWith("/")
                || target.startsWith("./")
                || target.contains(":\\")
                || target.matches(".*\\.(pdf|xlsx|xls|zip|jpg|jpeg|png|doc|docx)$");
    }

    /**
     * فتح ملف باستخدام Desktop API.
     * إذا كان المسار نسبياً يتم تحويله لمسار مطلق.
     *
     * @param path مسار الملف
     */
    private static void openFile(String path) {
        File file = new File(path);

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
            try {
                Desktop.getDesktop().open(finalFile);
            } catch (IOException e) {
                javafx.application.Platform.runLater(() ->
                        showError("فشل فتح الملف", e.getMessage()));
            }
        }, "file-opener").start();
    }

    /**
     * التنقل إلى مسار داخلي في التطبيق.
     *
     * @param route المسار الداخلي
     */
    private static void navigateTo(String route) {
        System.out.println("[FileOpener] التنقل إلى: " + route);
        // TODO: AppRouter.getInstance().navigate(route)
    }

    private static void showError(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.show();
    }
}