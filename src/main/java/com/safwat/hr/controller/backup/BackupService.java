package com.safwat.hr.controller.backup;

import com.fasterxml.jackson.core.type.TypeReference;
import com.safwat.hr.controller.backup.dto.BackupFileInfo;
import com.safwat.hr.controller.backup.dto.RestoreMode;
import com.safwat.hr.network.ApiClient;
import com.safwat.hr.network.ApiResponse;
import com.safwat.hr.network.FileTransferClient;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * طبقة الخدمة لشاشة النسخ الاحتياطي واستعادة السكريبتات.
 * كل منطق النداءات على الـ API والتقارير موجود هنا،
 * والكنترولر مسؤول فقط عن ربط الـ FXML وتحديث الواجهة.
 * <p>
 * ⭐ الـ Timeout: عمليات الباك أب والاستعادة وتنفيذ سكريبتات SQL ممكن
 * تاخد وقت طويل جدًا (خصوصًا مع قواعد بيانات كبيرة)، أطول بكتير من
 * الـ 45 ثانية الافتراضية المستخدمة في باقي endpoints التطبيق. عشان كده
 * كل النداءات هنا بتستخدم {@link #BACKUP_TIMEOUT} (15 دقيقة) بدل
 * الافتراضي، من غير ما يأثر ده على أي جزء تاني من البرنامج.
 * <p>
 * لو محتاج وقت أطول من كده (نسخة كبيرة جدًا)، غيّر القيمة هنا بس.
 */
public class BackupService {

    /**
     * timeout مخصص لعمليات الباك أب/الاستعادة/تنفيذ السكريبتات.
     */
    private static final Duration BACKUP_TIMEOUT = Duration.ofMinutes(15);

    // ════════════════════════════════════════════════════
    //  النسخ الاحتياطي
    // ════════════════════════════════════════════════════

    /**
     * إنشاء نسخة احتياطية كاملة.
     *
     * @param useCustomFormat  صيغة Custom (.dump) أو Plain SQL
     * @param compressionLevel مستوى الضغط 0-9
     */
    public CompletableFuture<ApiResponse<Object>> backupFull(boolean useCustomFormat, int compressionLevel) {
        String endpoint = "/payroll/backupFull?useCustomFormat=" + useCustomFormat
                + "&compressionLevel=" + compressionLevel;
        return ApiClient.postAsync(endpoint, null, BACKUP_TIMEOUT, Object.class);
    }

    /**
     * جلب قائمة النسخ المحفوظة على الخادم.
     */
    public CompletableFuture<ApiResponse<List<BackupFileInfo>>> listBackups() {
        return ApiClient.getAsync(
                "/payroll/backups",
                BACKUP_TIMEOUT,
                new TypeReference<List<BackupFileInfo>>() {
                });
    }

    /**
     * استعادة من ملف مرفوع من الجهاز.
     */
    public CompletableFuture<ApiResponse<Object>> restoreFromFile(Path filePath, RestoreMode mode) {
        return FileTransferClient.uploadFileAsync(
                "/payroll/restore?mode=" + mode.name(),
                filePath, "file", null, BACKUP_TIMEOUT, Object.class);
    }

    /**
     * استعادة نسخة محفوظة على الخادم.
     */
    public CompletableFuture<ApiResponse<Object>> restoreLocal(String fileName, RestoreMode mode) {
        return ApiClient.postAsync(
                "/payroll/restore-local?fileName=" + urlEncode(fileName)
                        + "&mode=" + mode.name(),
                null, BACKUP_TIMEOUT, Object.class);
    }

    // ════════════════════════════════════════════════════
    //  أدوات SQL
    // ════════════════════════════════════════════════════

    /**
     * فحص توقيع ملف SQL فقط بدون تنفيذ.
     */
    public CompletableFuture<ApiResponse<Object>> validateScript(Path filePath) {
        return FileTransferClient.uploadFileAsync(
                "/db-tools/scripts/validate",
                filePath, "file", null, BACKUP_TIMEOUT, Object.class);
    }

    /**
     * تنفيذ ملف SQL موقّع على قاعدة البيانات.
     */
    public CompletableFuture<ApiResponse<ScriptExecuteResult>> executeScript(Path filePath, boolean stopOnError) {
        return FileTransferClient.uploadFileAsync(
                "/db-tools/scripts/execute?stopOnError=" + stopOnError,
                filePath, "file", null, BACKUP_TIMEOUT, ScriptExecuteResult.class);
    }

    /**
     * بناء تقرير نصي مفصّل لنتيجة تنفيذ السكريبت.
     */
    public String buildScriptReport(ScriptExecuteResult r) {
        StringBuilder sb = new StringBuilder();
        sb.append("الملف: ").append(nullSafe(r.getFileName(), "—")).append('\n');
        sb.append("إجمالي الأوامر: ").append(r.getTotalStatements()).append('\n');
        sb.append("✔ نُفذ بنجاح: ").append(r.getExecutedCount()).append('\n');
        sb.append("✖ فشل: ").append(r.getFailedCount()).append('\n');
        sb.append("المدة: ").append(r.getDurationMs()).append(" ms\n");
        sb.append("وقت التنفيذ: ").append(r.getExecutedAt()).append('\n');
        if (r.isRolledBack()) {
            sb.append("⚠ تم التراجع عن كل التغييرات (Rollback)\n");
        }
        if (r.getErrors() != null && !r.getErrors().isEmpty()) {
            sb.append("\n─────────── الأخطاء ───────────\n");
            r.getErrors().forEach(e -> sb.append("• ").append(e).append('\n'));
        }
        return sb.toString();
    }

    /**
     * بناء رسالة إرشادية لإعدادات النسخ التلقائي.
     */
    public String buildAutoBackupHint(boolean enabled, String cron, int maxFiles) {
        if (!enabled) {
            return "ℹ النسخ التلقائي معطّل — لتفعيله عيّن:\nBACKUP_AUTO_ENABLED=true";
        }
        return "✅ لتطبيق هذه الإعدادات عيّن متغيرات البيئة:\n" +
                "BACKUP_AUTO_ENABLED=true\n" +
                "BACKUP_AUTO_CRON=" + cron + "\n" +
                "BACKUP_MAX_FILES=" + maxFiles + "\n" +
                "ثم أعد تشغيل الخادم.";
    }

    // ════════════════════════════════════════════════════
    //  Helpers
    // ════════════════════════════════════════════════════

    public String nullSafe(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value : fallback;
    }

    private String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}