package com.safwat.hr.network;

import com.safwat.hr.notification.model.HRNotification;
import com.safwat.hr.notification.service.NotificationService;
import com.safwat.hr.ui.controls.SAFNotification;
import com.safwat.hr.ui.util.AlertUtil;
import javafx.application.Platform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * ══════════════════════════════════════════════════════════════════════════════
 * DownloadWithNotification — أداة موحّدة لتنزيل الملفات وإشعار المستخدم
 * ══════════════════════════════════════════════════════════════════════════════
 *
 * <h2>الفكرة</h2>
 * <p>بدل ما كل كنترولر يكرّر منطق التنزيل (تحديد المسار، الحفظ، الإشعار)،
 * بنستخدم الأداة دي بنداء واحد.</p>
 *
 * <h2>الميثودات</h2>
 * <ul>
 *   <li>{@link #downloadPdfToTempAsync} — ترجع {@link CompletableFuture} بمسار الملف.
 *       الكنترولر بيستخدمها لعرض الـ PDF في الـ WebView.</li>
 *   <li>{@link #downloadPdfToTempAndNotify}, {@link #downloadExcelToTempAndNotify},
 *       {@link #downloadToTempAndNotify} — بتحفظ وتعرض إشعار، وبتنفذ {@code onDone}.</li>
 *   <li>{@link #downloadPdfToTempAndNotifyPost}, {@link #downloadToTempAndNotifyPost} — نفس الفكرة بطلب POST.</li>
 * </ul>
 *
 * @author Safwat
 * @see ApiClient
 * @see NotificationService
 */
public final class DownloadWithNotification {

    /**
     * مجلد التنزيلات الموحّد (يُنشأ تلقائيًا لو مش موجود).
     */
    private static final String TEMP_DOWNLOADS_DIR = "temp_downloads";

    /**
     * امتدادات جاهزة للاستخدام.
     */
    private static final String EXT_PDF = "pdf";
    private static final String EXT_XLSX = "xlsx";

    /**
     * منع الاستخدام كـ instance — Utility class.
     */
    private DownloadWithNotification() {
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Public API — Future (بترجع المسار، والكنترولر يقرر يعرضه إزاي)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * ينزّل ملف PDF من الـ endpoint المحدّد، ويحفظه في temp_downloads،
     * ويعرض إشعار للمستخدم عند النجاح. بترجع المسار بعد الحفظ.
     *
     * <p>الإشعار بيتعرض على الـ FX thread. الكنترولر بيستقبل المسار
     * ويعرضه في الـ WebView بنفسه.</p>
     *
     * @param path              مسار الـ API النسبي.
     * @param fileNamePrefix    بادئة اسم الملف (يُضاف لها timestamp تلقائيًا).
     * @param name              نص الإشعار (عادةً اسم الموظف أو وصف مختصر).
     * @param notificationTitle عنوان الإشعار.
     * @return future بمسار الملف المحفوظ، أو فشل بالـ exception.
     */
    public static CompletableFuture<Path> downloadPdfToTempAsync(String path,
                                                                 String fileNamePrefix,
                                                                 String name,
                                                                 String notificationTitle) {
        Path targetPath;
        try {
            targetPath = prepareTargetPath(fileNamePrefix, EXT_PDF);
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }

        return ApiClient.downloadBinaryAsync(path)
                .thenApply(bytes -> {
                    try {
                        Files.write(targetPath, bytes);
                        return targetPath;
                    } catch (IOException e) {
                        throw new CompletionException(e);
                    }
                })
                .thenApply(saved -> {
                    Platform.runLater(() -> sendNotification(name, notificationTitle, saved));
                    return saved;
                });
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Public API — Wrappers (بتحفظ وتعرض إشعار وتنفذ onDone)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * ينزّل ملف PDF، يحفظه في temp_downloads، ويرسل إشعار عند النجاح.
     *
     * @param onDone Callback بعد انتهاء العملية (نجاح أو فشل) — يقبل {@code null}.
     */
    public static void downloadPdfToTempAndNotify(String path,
                                                  String fileNamePrefix,
                                                  String employeeName,
                                                  String notificationTitle,
                                                  Runnable onDone) {
        downloadToTempAndNotify(path, fileNamePrefix, EXT_PDF,
                employeeName, notificationTitle, onDone);
    }

    /**
     * ينزّل ملف Excel (xlsx)، يحفظه في temp_downloads، ويرسل إشعار عند النجاح.
     */
    public static void downloadExcelToTempAndNotify(String path,
                                                    String fileNamePrefix,
                                                    String employeeName,
                                                    String notificationTitle,
                                                    Runnable onDone) {
        downloadToTempAndNotify(path, fileNamePrefix, EXT_XLSX,
                employeeName, notificationTitle, onDone);
    }

    /**
     * النسخة العامة — تقبل أي امتداد.
     *
     * @param extension امتداد الملف بدون نقطة. يقبل {@code null} → الافتراضي {@code "pdf"}.
     */
    public static void downloadToTempAndNotify(String path,
                                               String fileNamePrefix,
                                               String extension,
                                               String message,
                                               String notificationTitle,
                                               Runnable onDone) {
        try {
            Path targetPath = prepareTargetPath(fileNamePrefix, extension);

            ApiClient.downloadBinaryAsync(path)
                    .thenAccept(bytes -> handleSuccess(bytes, targetPath,
                            message, notificationTitle, onDone))
                    .exceptionally(ex -> {
                        handleFailure("خطأ في التحميل", ex, onDone);
                        return null;
                    });

        } catch (Exception ex) {
            handleFailure("خطأ في التحميل", ex, onDone);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Public API — POST (للتقارير اللي محتاجة body)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * ينزّل ملف PDF عبر طلب POST (يرسل {@code body} كـ JSON)، ويحفظه ويرسل إشعار.
     *
     * @param body الـ request body. يقبل {@code null} لو مفيش body.
     */
    public static void downloadPdfToTempAndNotifyPost(String path,
                                                      Object body,
                                                      String fileNamePrefix,
                                                      String message,
                                                      String notificationTitle,
                                                      Runnable onDone) {
        downloadToTempAndNotifyPost(path, body, fileNamePrefix, EXT_PDF,
                message, notificationTitle, onDone);
    }

    /**
     * النسخة العامة لطلب POST — تقبل أي امتداد.
     */
    public static void downloadToTempAndNotifyPost(String path,
                                                   Object body,
                                                   String fileNamePrefix,
                                                   String extension,
                                                   String message,
                                                   String notificationTitle,
                                                   Runnable onDone) {
        try {
            Path targetPath = prepareTargetPath(fileNamePrefix, extension);

            ApiClient.downloadBinaryPostAsync(path, body, java.time.Duration.ofMinutes(3))
                    .thenAccept(bytes -> handleSuccess(bytes, targetPath,
                            message, notificationTitle, onDone))
                    .exceptionally(ex -> {
                        handleFailure("خطأ في التحميل", ex, onDone);
                        return null;
                    });

        } catch (Exception ex) {
            handleFailure("خطأ في التحميل", ex, onDone);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Private Helpers
    // ══════════════════════════════════════════════════════════════════════

    /**
     * يجهّز مسار الحفظ داخل {@code temp_downloads} — ينشئ المجلد لو مش موجود.
     */
    private static Path prepareTargetPath(String fileNamePrefix, String extension)
            throws IOException {

        String workingDir = System.getProperty("user.dir");
        Path tempDownloadsDir = Paths.get(workingDir, TEMP_DOWNLOADS_DIR);

        if (!Files.exists(tempDownloadsDir)) {
            Files.createDirectories(tempDownloadsDir);
        }

        String ext = (extension == null || extension.isBlank())
                ? EXT_PDF
                : extension.trim().replaceFirst("^\\.", "");

        String fileName = fileNamePrefix + "_" + System.currentTimeMillis() + "." + ext;
        return tempDownloadsDir.resolve(fileName);
    }

    /**
     * نجاح التنزيل: كتابة الملف، ثم إشعار وتنفيذ {@code onDone} على الـ FX thread.
     */
    private static void handleSuccess(byte[] bytes,
                                      Path targetPath,
                                      String message,
                                      String notificationTitle,
                                      Runnable onDone) {
        try {
            Files.write(targetPath, bytes);
            Platform.runLater(() -> {
                sendNotification(message, notificationTitle, targetPath);
                if (onDone != null) onDone.run();
            });
        } catch (Exception ex) {
            handleFailure("خطأ في حفظ الملف", ex, onDone);
        }
    }

    /**
     * إرسال الإشعار للمستخدم مع رابط الملف المحفوظ. لازم يتنادى على الـ FX thread.
     */
    private static void sendNotification(String message,
                                         String notificationTitle,
                                         Path targetPath) {
        SAFNotification.withAction("تم التحميل بنجاح", targetPath.toFile());

        NotificationService.getInstance().send(
                HRNotification.builder()
                        .type(HRNotification.NotificationType.SYSTEM)
                        .priority(HRNotification.Priority.HIGH)
                        .title(notificationTitle)
                        .message(message)
                        .file(targetPath.toString())
                        .sender("system")
                        .build()
        );
    }

    /**
     * عند الفشل: عرض Alert خطأ + تنفيذ {@code onDone} على الـ UI thread.
     */
    private static void handleFailure(String title, Throwable ex, Runnable onDone) {
        Throwable root = ex;
        if (root instanceof CompletionException && root.getCause() != null) {
            root = root.getCause();
        }
        String message = (root != null && root.getMessage() != null)
                ? root.getMessage()
                : "حدث خطأ غير معروف";

        Platform.runLater(() -> {
            AlertUtil.showError(title, message);
            if (onDone != null) onDone.run();
        });
    }

    public static CompletableFuture<Path> downloadPdfToTempAndNotifyPostAsync(String path,
                                                                              Object body,
                                                                              String fileNamePrefix,
                                                                              String name,
                                                                              String notificationTitle) {
        Path targetPath;
        try {
            targetPath = prepareTargetPath(fileNamePrefix, EXT_PDF);
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }

        return ApiClient.downloadBinaryPostAsync(path, body, java.time.Duration.ofMinutes(3))
                .thenApply(bytes -> {
                    try {
                        Files.write(targetPath, bytes);
                        return targetPath;
                    } catch (IOException e) {
                        throw new CompletionException(e);
                    }
                })
                .thenApply(saved -> {
                    Platform.runLater(() -> sendNotification(name, notificationTitle, saved));
                    return saved;
                });
    }
}