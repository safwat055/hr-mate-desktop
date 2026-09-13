package com.safwat.hr.notification.util;

import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class FileOpener {

    private static volatile Boolean desktopSupported = null;

    /**
     * نسخة async — آمنة من أي thread
     */
    public static void openAsync(String target) {
        if (target == null || target.isBlank()) return;

        CompletableFuture.runAsync(() -> {
            try {
                if (isFilePath(target)) {
                    openFileInternal(target);
                } else {
                    navigateTo(target);
                }
            } catch (Exception e) {
                Platform.runLater(() -> showError("خطأ", e.getMessage()));
            }
        });
    }

    /**
     * نسخة متزامنة — للاستخدام من threads مش FX فقط
     */
    public static void open(String target) {
        if (target == null || target.isBlank()) return;
        if (Platform.isFxApplicationThread()) {
            // احتياطي: لو حد نادى عليها من FX thread، حوّلها لـ async
            openAsync(target);
            return;
        }
        try {
            if (isFilePath(target)) openFileInternal(target);
            else navigateTo(target);
        } catch (Exception e) {
            showError("خطأ", e.getMessage());
        }
    }

    private static void openFileInternal(String path) throws IOException {
        File file = new File(path);
        if (!file.isAbsolute()) {
            file = new File(System.getProperty("user.dir"), path);
        }

        if (!file.exists()) {
            final String fullPath = file.getAbsolutePath();
            Platform.runLater(() -> showError("الملف غير موجود",
                    "المسار: " + fullPath + "\nتأكد من وجود الملف."));
            return;
        }

        if (!isDesktopSupported()) {
            Platform.runLater(() -> showError("غير مدعوم",
                    "لا يدعم النظام فتح الملفات تلقائيًا.\nالمسار: " + path));
            return;
        }

        try {
            Desktop.getDesktop().open(file);
        } catch (IOException e) {
            Platform.runLater(() -> showError("فشل فتح الملف", e.getMessage()));
        } catch (UnsupportedOperationException e) {
            Platform.runLater(() -> showError("غير مدعوم", "النظام لا يدعم هذه العملية"));
        }
    }

    public static boolean isDesktopSupported() {
        if (desktopSupported == null) {
            synchronized (FileOpener.class) {
                if (desktopSupported == null) {
                    try {
                        desktopSupported = Desktop.isDesktopSupported()
                                && Desktop.getDesktop().isSupported(Desktop.Action.OPEN);
                    } catch (Throwable t) {
                        desktopSupported = false;
                    }
                }
            }
        }
        return desktopSupported;
    }

    private static boolean isFilePath(String target) {
        if (target == null || target.isBlank()) return false;
        if (target.startsWith("/api/") || target.startsWith("/messages/")) return false;
        return target.startsWith("/")
                || target.startsWith("./")
                || target.contains(":\\")
                || target.matches(".*\\.(pdf|xlsx|xls|zip|jpg|jpeg|png|doc|docx)$");
    }

    private static void navigateTo(String route) {
        // TODO: AppRouter.getInstance().navigate(route)
    }

    private static void showError(String title, String msg) {
        Runnable show = () -> {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle(title);
            a.setHeaderText(null);
            a.setContentText(msg);
            a.show();
        };
        if (Platform.isFxApplicationThread()) show.run();
        else Platform.runLater(show);
    }
}