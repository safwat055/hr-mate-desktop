package com.safwat.hr.shared.file;


import javafx.application.Platform;
import javafx.scene.control.Label;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * أداة تنظيف الملفات المؤقتة (Excel, PDF, وغيرها)
 * يتم تفعيلها تلقائياً عند بدء التشغيل
 */
@Slf4j
public class TempFileCleaner {

    // ════════════════════════════════════════════════════════════
    //  إعدادات التنظيف (Hard Coded - يمكن تغييرها لاحقاً)
    // ════════════════════════════════════════════════════════════

    /**
     * عدد الأيام التي بعدها يتم حذف الملفات المؤقتة
     */
    private static final int RETENTION_DAYS = 7;

    /**
     * مسار مجلد الملفات المؤقتة (نسبة إلى مجلد المشروع)
     */
    private static final String TEMP_DOWNLOADS_DIR = "temp_downloads";

    /**
     * قائمة الامتدادات التي سيتم تنظيفها
     * (Excel, PDF, Word, PowerPoint, images, ...)
     */
    private static final List<String> FILE_EXTENSIONS = List.of(
            // Excel
            ".xlsx", ".xls", ".xlsm", ".csv",
            // PDF
            ".pdf",
            // Word
            ".docx", ".doc",
            // PowerPoint
            ".pptx", ".ppt",
            // Images
            ".png", ".jpg", ".jpeg", ".gif", ".bmp", ".svg",
            // Text
            ".txt", ".rtf",
            // Archives
            ".zip", ".rar", ".7z",
            // Other
            ".tmp", ".temp", ".download", ".part"
    );

    // ════════════════════════════════════════════════════════════
    //  التنظيف التلقائي (يتم استدعاؤها عند بدء التشغيل)
    // ════════════════════════════════════════════════════════════

    /**
     * تنفيذ التنظيف التلقائي عند بدء التشغيل
     * يمكن استدعاؤها من main أو من Application.start()
     */
    public static void cleanOnStartup() {
        log.info("=== Starting Temp Files Cleanup on Startup ===");
        log.info("Retention days: {}", RETENTION_DAYS);
        log.info("Temp directory: {}", getTempPath());
        log.info("File extensions to clean: {}", FILE_EXTENSIONS);

        // تنفيذ التنظيف في Thread منفصل
        CompletableFuture.runAsync(() -> {
            try {
                CleanupResult result = performCleanup();

                // عرض النتيجة في الـ UI إذا كان التطبيق قيد التشغيل
                Platform.runLater(() -> {
                    // يمكن إرسال النتيجة إلى Notification أو Log
                    log.info("Cleanup completed: {}", result);
                });

            } catch (Exception e) {
                log.error("Error during temp files cleanup: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * التنظيف مع إمكانية عرض النتيجة في Label
     */
    public static void cleanWithStatus(Label statusLabel) {
        log.info("=== Starting Temp Files Cleanup with Status ===");

        CompletableFuture.runAsync(() -> {
            try {
                CleanupResult result = performCleanup();

                Platform.runLater(() -> {
                    if (statusLabel != null) {
                        if (result.deletedCount > 0) {
                            statusLabel.setText("🧹 تم حذف " + result.deletedCount +
                                    " ملف مؤقت (توفير " + result.freedSpaceFormatted + ")");
                            statusLabel.getStyleClass().removeAll("status-msg-ok", "status-msg-error");
                            statusLabel.getStyleClass().add("status-msg-ok");
                        } else {
                            statusLabel.setText("✅ لا توجد ملفات مؤقتة للتنظيف");
                            statusLabel.getStyleClass().removeAll("status-msg-ok", "status-msg-error");
                            statusLabel.getStyleClass().add("status-msg-ok");
                        }
                    }
                });

            } catch (Exception e) {
                log.error("Error during temp files cleanup: {}", e.getMessage(), e);
                Platform.runLater(() -> {
                    if (statusLabel != null) {
                        statusLabel.setText("❌ فشل تنظيف الملفات المؤقتة: " + e.getMessage());
                        statusLabel.getStyleClass().removeAll("status-msg-ok", "status-msg-error");
                        statusLabel.getStyleClass().add("status-msg-error");
                    }
                });
            }
        });
    }

    // ════════════════════════════════════════════════════════════
    //  منطق التنظيف الأساسي
    // ════════════════════════════════════════════════════════════

    /**
     * الحصول على مسار مجلد الملفات المؤقتة
     */
    private static Path getTempPath() {
        return Path.of(System.getProperty("user.dir"), TEMP_DOWNLOADS_DIR);
    }

    /**
     * تنفيذ عملية التنظيف وإرجاع النتيجة
     */
    private static CleanupResult performCleanup() throws IOException {
        Path tempDir = getTempPath();
        CleanupResult result = new CleanupResult();

        // إذا كان المجلد غير موجود، لا داعي للتنظيف
        if (!Files.exists(tempDir)) {
            log.debug("Temp directory does not exist: {}", tempDir);
            return result;
        }

        // حساب التاريخ المحدد (الأقدم من عدد الأيام)
        Instant cutoffTime = Instant.now().minus(RETENTION_DAYS, ChronoUnit.DAYS);

        log.debug("Cleaning temp files older than: {}", cutoffTime);

        // تجول في المجلد وحذف الملفات القديمة
        try (var stream = Files.walk(tempDir, 1)) {
            for (Path file : (Iterable<Path>) stream::iterator) {
                // تخطي المجلدات الفرعية (نحذف الملفات فقط)
                if (Files.isDirectory(file)) {
                    continue;
                }

                String fileName = file.getFileName().toString().toLowerCase();

                // التحقق من الامتداد
                boolean isTargetFile = FILE_EXTENSIONS.stream()
                        .anyMatch(fileName::endsWith);

                if (!isTargetFile) {
                    log.trace("Skipping non-target file: {}", fileName);
                    continue;
                }

                try {
                    // التحقق من وقت الإنشاء
                    BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
                    Instant fileTime = attrs.creationTime().toInstant();

                    // إذا كان الملف أقدم من التاريخ المحدد
                    if (fileTime.isBefore(cutoffTime)) {
                        long fileSize = Files.size(file);
                        Files.deleteIfExists(file);
                        result.deletedCount++;
                        result.totalSizeFreed += fileSize;

                        log.debug("Deleted temp file: {} (created: {}, size: {} bytes)",
                                fileName, fileTime, fileSize);
                    } else {
                        log.trace("Keeping file (too recent): {}", fileName);
                    }

                } catch (IOException e) {
                    log.warn("Failed to process file: {} - {}", fileName, e.getMessage());
                }
            }
        }

        // تنسيق الحجم المحرر
        result.freedSpaceFormatted = formatSize(result.totalSizeFreed);

        log.info("Cleanup result: deleted {} files, freed {}",
                result.deletedCount, result.freedSpaceFormatted);

        return result;
    }

    // ════════════════════════════════════════════════════════════
    //  دوال مساعدة
    // ════════════════════════════════════════════════════════════

    /**
     * تنسيق حجم الملف بشكل مقروء
     */
    private static String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * الحصول على إحصائيات المجلد المؤقت (للأغراض الإحصائية)
     */
    public static TempStats getStats() {
        TempStats stats = new TempStats();
        try {
            Path tempDir = getTempPath();
            if (!Files.exists(tempDir)) {
                return stats;
            }

            try (var stream = Files.walk(tempDir, 1)) {
                for (Path file : (Iterable<Path>) stream::iterator) {
                    if (Files.isDirectory(file)) continue;
                    stats.totalFiles++;
                    stats.totalSize += Files.size(file);
                }
            }
            stats.totalSizeFormatted = formatSize(stats.totalSize);

        } catch (IOException e) {
            log.error("Failed to get temp stats: {}", e.getMessage());
        }
        return stats;
    }

    // ════════════════════════════════════════════════════════════
    //  كلاسات داخلية (Data Classes)
    // ════════════════════════════════════════════════════════════

    /**
     * نتيجة عملية التنظيف
     */
    public static class CleanupResult {
        public int deletedCount = 0;
        public long totalSizeFreed = 0;
        public String freedSpaceFormatted = "0 B";

        @Override
        public String toString() {
            return "CleanupResult{deletedCount=" + deletedCount +
                    ", freedSpace=" + freedSpaceFormatted + "}";
        }
    }

    /**
     * إحصائيات المجلد المؤقت
     */
    public static class TempStats {
        public int totalFiles = 0;
        public long totalSize = 0;
        public String totalSizeFormatted = "0 B";

        @Override
        public String toString() {
            return "TempStats{totalFiles=" + totalFiles +
                    ", totalSize=" + totalSizeFormatted + "}";
        }
    }

    // ════════════════════════════════════════════════════════════
    //  طرق استدعاء يدوي (للاستخدام من الـ Controller)
    // ════════════════════════════════════════════════════════════

    /**
     * تنظيف فوري (بدون عرض في الـ UI)
     */
    public static void cleanNow() {
        log.info("Manual temp files cleanup requested");
        CompletableFuture.runAsync(() -> {
            try {
                CleanupResult result = performCleanup();
                log.info("Manual cleanup completed: {}", result);
            } catch (Exception e) {
                log.error("Manual cleanup failed: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * تنظيف فوري مع عرض النتيجة في Label
     */
    public static void cleanNowWithStatus(Label statusLabel) {
        cleanWithStatus(statusLabel);
    }

    /**
     * الحصول على إحصائيات (للعرض في الـ UI)
     */
    public static TempStats getCurrentStats() {
        return getStats();
    }
}