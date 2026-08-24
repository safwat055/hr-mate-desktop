package com.safwat.hr.chat.cach;

import com.safwat.hr.chat.dto.ChatDTOs;
import com.safwat.hr.chat.service.ChatApiService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

/**
 * ✅ جديد: كاش محلي مؤقت للمرفقات (خصوصاً الصور).
 * <p>
 * السبب: تحميل صورة مباشرة عن طريق رابط نسبي (زي /api/chat/attachments/{token})
 * من غير توثيق (Authorization header) كان بيفشل دايماً. الحل: نحمّل الملف مرة
 * <p>
 * واحدة بشكل آمن عن طريق {@link ChatApiService#downloadAttachment}
 * (اللي بالفعل بيبعت التوثيق صح)، ونحفظه محلياً، وبعدين أي عرض تاني للصورة
 * (المعاينة داخل الفقاعة أو عارض الصور الداخلي) بيستخدم النسخة المحلية.
 */
public final class AttachmentCache {

    private AttachmentCache() {
    }

    /**
     * المسار المحلي المتوقع لمرفق معين (سواء موجود بالفعل أو لسه هيتحمل).
     */
    public static Path cachePath(ChatDTOs.ChatAttachmentDTO att) {
        Path dir = Paths.get(System.getProperty("java.io.tmpdir"), "hr_chat_cache");
        try {
            Files.createDirectories(dir);
        } catch (Exception ignored) {
        }
        String safeName = att.getFileName() != null
                ? att.getFileName().replaceAll("[\\\\/:*?\"<>|]", "_")
                : "file";
        return dir.resolve(att.getDownloadToken() + "_" + safeName);
    }

    /**
     * بيرجع مسار الملف محلياً — يحمّله أولاً لو مش موجود بالفعل في الكاش.
     */
    public static CompletableFuture<Path> ensureDownloaded(ChatDTOs.ChatAttachmentDTO att) {
        Path target = cachePath(att);

        if (Files.exists(target)) {
            return CompletableFuture.completedFuture(target);
        }
        if (att.getDownloadToken() == null) {
            return CompletableFuture.completedFuture(null);
        }

        return ChatApiService.downloadAttachment(att.getDownloadToken(), target)
                .thenApply(success -> success ? target : null);
    }
}