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

/**
 * ══════════════════════════════════════════════════════════════════════════════
 * DownloadWithNotification — أداة موحّدة لتنزيل الملفات وإشعار المستخدم
 * ══════════════════════════════════════════════════════════════════════════════
 *
 * <h2>الفكرة</h2>
 * <p>بدل ما كل كنترولر يكرّر نفس منطق التنزيل (تحديد مسار، حفظ، إشعار)،
 * بنستخدم الأداة دي من أي مكان بنداء واحد.</p>
 *
 * <h2>السلوك</h2>
 * <ol>
 *   <li>الملف يُحفظ في {@code {user.dir}/temp_downloads/} بمسار ثابت — بدون فتح نافذة اختيار.</li>
 *   <li>الاسم: {@code <fileNamePrefix>_<timestamp>.<extension>}.</li>
 *   <li>التنزيل async (CompletableFuture) لتفادي تجميد الـ JavaFX UI.</li>
 *   <li>عند النجاح: إرسال {@link HRNotification} للمستخدم + تنفيذ {@code onDone}.</li>
 *   <li>عند الفشل: عرض Alert خطأ عبر {@link AlertUtil} + تنفيذ {@code onDone}.</li>
 * </ol>
 *
 * <h2>الامتدادات المدعومة</h2>
 * <p>الميثود العامة بتقبل أي امتداد ({@code "pdf"}, {@code "xlsx"}, {@code "csv"}، ...).</p>
 * <p>وفي wrappers جاهزة للاستخدام السريع:</p>
 * <ul>
 *   <li>{@link #downloadPdfToTempAndNotify} → PDF</li>
 *   <li>{@link #downloadExcelToTempAndNotify} → XLSX</li>
 * </ul>
 *
 * <h2>مثال استخدام</h2>
 * <pre>{@code
 * // PDF
 * DownloadWithNotification.downloadPdfToTempAndNotify(
 *         "/entitlements/employee/" + nationalId + "/payslip/pdf?date=2025-04-01",
 *         "PAYSLIP_" + nationalId,
 *         "أحمد محمد — أبريل 2025",
 *         "مفردات المرتب",
 *         () -> btn_exportPDF.setDisable(false)
 * );
 *
 * // Excel
 * DownloadWithNotification.downloadExcelToTempAndNotify(
 *         "/entitlements/supplementary-bonus/excel",
 *         "SUPPLEMENTARY_ALL",
 *         "الحافز التكميلي — كل الموظفين",
 *         "تقرير الحافز التكميلي",
 *         () -> btn.setDisable(false)
 * );
 *
 * // أي امتداد آخر
 * DownloadWithNotification.downloadToTempAndNotify(
 *         "/api/xxx/report",
 *         "REPORT_" + id,
 *         "csv",
 *         "وصف الملف",
 *         "عنوان الإشعار",
 *         () -> btn.setDisable(false)
 * );
 * }</pre>
 *
 * @author Safwat
 * @see ApiClient#downloadBinaryAsync(String)
 * @see NotificationService
 */
public final class DownloadWithNotification {

    /** مجلد التنزيلات الموحّد (يُنشأ تلقائيًا لو مش موجود). */
    private static final String TEMP_DOWNLOADS_DIR = "temp_downloads";

    /** امتدادات جاهزة للاستخدام في الـ wrappers. */
    private static final String EXT_PDF = "pdf";
    private static final String EXT_XLSX = "xlsx";

    /** منع الاستخدام كـ instance — Utility class. */
    private DownloadWithNotification() {
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Public API — Wrappers (الاستخدام السريع)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * ينزّل ملف PDF من الـ endpoint المحدّد، يحفظه في temp_downloads،
     * ويرسل إشعار للمستخدم عند النجاح.
     *
     * @param path              مسار الـ API النسبي.
     * @param fileNamePrefix    بادئة اسم الملف (يُضاف لها timestamp تلقائيًا).
     * @param employeeName      رسالة الإشعار (عادةً اسم الموظف).
     * @param notificationTitle عنوان الإشعار.
     * @param onDone            Callback بعد انتهاء العملية (نجاح أو فشل) — يقبل {@code null}.
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
     * ينزّل ملف Excel (xlsx) من الـ endpoint المحدّد، يحفظه في temp_downloads،
     * ويرسل إشعار للمستخدم عند النجاح.
     *
     * @param path              مسار الـ API النسبي.
     * @param fileNamePrefix    بادئة اسم الملف (يُضاف لها timestamp تلقائيًا).
     * @param employeeName      رسالة الإشعار (عادةً اسم الموظف أو وصف مختصر).
     * @param notificationTitle عنوان الإشعار.
     * @param onDone            Callback بعد انتهاء العملية — يقبل {@code null}.
     */
    public static void downloadExcelToTempAndNotify(String path,
                                                    String fileNamePrefix,
                                                    String employeeName,
                                                    String notificationTitle,
                                                    Runnable onDone) {
        downloadToTempAndNotify(path, fileNamePrefix, EXT_XLSX,
                employeeName, notificationTitle, onDone);
    }
// ══════════════════════════════════════════════════════════════════════
//  Public API — POST (للتقارير اللي محتاجة body)
// ══════════════════════════════════════════════════════════════════════

    /**
     * ينزّل ملف PDF عبر طلب <b>POST</b> (يرسل {@code body} كـ JSON)،
     * يحفظه في temp_downloads، ويرسل إشعار للمستخدم.
     *
     * <p><b>متى تستخدمها؟</b> لما الـ endpoint يستقبل بيانات إضافية في
     * الـ body (زي: قائمة شهور محسوبة مسبقًا، فلاتر، ...) — بدل ما يعيد
     * الحساب من الصفر.</p>
     *
     * @param path              مسار الـ API.
     * @param body              الـ request body (يُحوَّل لـ JSON تلقائيًا).
     *                          يقبل {@code null} لو مفيش body.
     * @param fileNamePrefix    بادئة اسم الملف.
     * @param message           رسالة الإشعار.
     * @param notificationTitle عنوان الإشعار.
     * @param onDone            Callback بعد انتهاء العملية.
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
     * النسخة العامة — تقبل أي امتداد.
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
    //  Public API — General (أي امتداد)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * ينزّل ملف بالامتداد المحدّد من الـ endpoint، يحفظه في temp_downloads،
     * ويرسل إشعار للمستخدم عند النجاح.
     *
     * @param path              مسار الـ API النسبي
     *                          (مثال: {@code "/entitlements/employee/.../payslip/pdf"}).
     * @param fileNamePrefix    بادئة اسم الملف — يُضاف لها timestamp تلقائيًا
     *                          (مثال: {@code "PAYSLIP_29303062600511"}).
     * @param extension         امتداد الملف بدون نقطة
     *                          (مثال: {@code "pdf"}, {@code "xlsx"}, {@code "csv"}).
     *                          يقبل {@code null} → الافتراضي {@code "pdf"}.
     * @param message           رسالة الإشعار — عادةً اسم الموظف أو وصف مختصر.
     * @param notificationTitle عنوان الإشعار.
     * @param onDone            Callback بعد انتهاء العملية (نجاح أو فشل) — يقبل {@code null}.
     */
    public static void downloadToTempAndNotify(String path,
                                               String fileNamePrefix,
                                               String extension,
                                               String message,
                                               String notificationTitle,
                                               Runnable onDone) {
        try {
            // ── 1. تجهيز مسار الحفظ في temp_downloads ──
            Path targetPath = prepareTargetPath(fileNamePrefix, extension);

            // ── 2. تنزيل الملف (async لتجنب تجميد الواجهة) ──
            ApiClient.downloadBinaryAsync(path)
                    .thenAccept(bytes -> handleSuccess(bytes, targetPath,
                            message, notificationTitle, onDone))
                    .exceptionally(ex -> {
                        handleFailure("خطأ في التحميل", ex, onDone);
                        return null;
                    });

        } catch (Exception ex) {
            // أي خطأ قبل بدء التنزيل (مثلًا: مشكلة في إنشاء المجلد)
            handleFailure("خطأ في التحميل", ex, onDone);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Private Helpers
    // ══════════════════════════════════════════════════════════════════════

    /**
     * يجهّز مسار الحفظ داخل {@code temp_downloads} — ينشئ المجلد لو مش موجود.
     *
     * @param fileNamePrefix البادئة (يُضاف لها timestamp + امتداد).
     * @param extension      امتداد الملف (بدون نقطة) — يقبل {@code null} → {@code "pdf"}.
     * @return المسار الكامل للملف.
     */
    private static Path prepareTargetPath(String fileNamePrefix, String extension)
            throws IOException {

        String workingDir = System.getProperty("user.dir");
        Path tempDownloadsDir = Paths.get(workingDir, TEMP_DOWNLOADS_DIR);

        if (!Files.exists(tempDownloadsDir)) {
            Files.createDirectories(tempDownloadsDir);
        }

        // تطبيع الامتداد: null/فاضي → "pdf" + إزالة النقطة البادئة لو موجودة
        String ext = (extension == null || extension.isBlank())
                ? EXT_PDF
                : extension.trim().replaceFirst("^\\.", "");

        String fileName = fileNamePrefix + "_" + System.currentTimeMillis() + "." + ext;
        return tempDownloadsDir.resolve(fileName);
    }

    /**
     * نجاح التنزيل:
     * <ol>
     *   <li>كتابة الملف على الديسك.</li>
     *   <li>إرسال {@link HRNotification} للمستخدم.</li>
     *   <li>تنفيذ {@code onDone} على الـ UI thread.</li>
     * </ol>
     *
     * <p>لو فشلت الكتابة (مثلًا: مساحة غير كافية أو صلاحيات) → يُعرض خطأ.</p>
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
     * إرسال الإشعار للمستخدم مع رابط الملف المحفوظ.
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
     *
     * <p><b>ملاحظة:</b> {@link AlertUtil#showError(String, String)} بياخد
     * {@code (title, message)} — الرسالة الحقيقية بتتحدد من الـ exception.</p>
     *
     * @param title  عنوان الخطأ (مثلًا: "خطأ في التحميل").
     * @param ex     الـ exception (لو null → message = "حدث خطأ غير معروف").
     * @param onDone callback لتفعيل الزر.
     */
    private static void handleFailure(String title, Throwable ex, Runnable onDone) {
        String message = (ex != null && ex.getMessage() != null)
                ? ex.getMessage()
                : "حدث خطأ غير معروف";

        Platform.runLater(() -> {
            AlertUtil.showError(title, message);
            if (onDone != null) onDone.run();
        });
    }
}