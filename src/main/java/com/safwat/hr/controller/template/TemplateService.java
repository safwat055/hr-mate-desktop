package com.safwat.hr.controller.template;

import com.fasterxml.jackson.core.type.TypeReference;
import com.safwat.hr.network.ApiClient;
import com.safwat.hr.network.ApiResponse;
import com.safwat.hr.network.FileTransferClient;
import com.safwat.hr.notification.model.HRNotification;
import com.safwat.hr.notification.service.NotificationService;
import com.safwat.hr.ui.controls.SAFNotification;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Slf4j
public class TemplateService {

    private static final String BASE_API = "/template-download";

    private TemplateService() {
    }

    private static class Holder {
        private static final TemplateService INSTANCE = new TemplateService();
    }

    public static TemplateService getInstance() {
        return Holder.INSTANCE;
    }

    // ══════════════════════════════════════════════
    //  تحميل القالب
    // ══════════════════════════════════════════════

    public void downloadTemplate(String templateType, int rowsCount) {
        TemplateInfo body = TemplateInfo.builder()
                .reportTyp(templateType)
                .rowsCount(rowsCount)
                .build();

        try {
            FileTransferClient.DownloadedFile df =
                    FileTransferClient.downloadInTempDir(BASE_API + "/template", body, "xlsx");
            log.info("تم تحميل القالب: {}", df.fileName());
            openFile(df.path(), df.fileName(), templateType);


        } catch (IOException e) {
            log.error("فشل تحميل القالب [{}]: {}", templateType, e.getMessage(), e);
            throw new TemplateDownloadException("تعذر تحميل القالب: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TemplateDownloadException("تم إلغاء تحميل القالب", e);
        }
    }


    private void openFile(Path targetPath, String reportName, String empName) {

        SAFNotification.withAction(targetPath.toString(), targetPath.toFile());

        NotificationService.getInstance().send(
                HRNotification.builder()
                        .type(HRNotification.NotificationType.SYSTEM)
                        .priority(HRNotification.Priority.HIGH)
                        .title(reportName)
                        .message(empName)
                        .file(targetPath.toString())
                        .sender("system")
                        .build()
        );
    }

    // ══════════════════════════════════════════════
    //  قائمة أنواع القوالب (عربي)
    // ══════════════════════════════════════════════

    /**
     * يرجّع قائمة بأسماء القوالب بالعربي — لعرضها في ComboBox مثلاً.
     */
    public List<String> fetchTemplateTypeNames() {
        try {
            ApiResponse<List<String>> response = ApiClient.getWithTypeRef(
                    BASE_API + "/types",
                    new TypeReference<List<String>>() {
                    });

            if (!response.isSuccess() || response.getData() == null) {
                log.warn("فشل جلب قائمة القوالب: {}", response.getMessage());
                return List.of();
            }
            return response.getData();

        } catch (Exception e) {
            log.error("خطأ في جلب قائمة القوالب: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * (اختياري) يرجّع الخريطة الكاملة (عربي → إنجليزي).
     */
    public Map<String, String> fetchTemplateTypeMap() {
        try {
            ApiResponse<Map<String, String>> response = ApiClient.getWithTypeRef(
                    BASE_API + "/types/map",
                    new TypeReference<Map<String, String>>() {
                    });

            if (!response.isSuccess() || response.getData() == null) {
                return Map.of();
            }
            return response.getData();

        } catch (Exception e) {
            log.error("خطأ في جلب خريطة القوالب: {}", e.getMessage(), e);
            return Map.of();
        }
    }

    // ══════════════════════════════════════════════
    //  Exception
    // ══════════════════════════════════════════════

    public static class TemplateDownloadException extends RuntimeException {
        public TemplateDownloadException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}