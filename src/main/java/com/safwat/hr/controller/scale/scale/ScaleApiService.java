package com.safwat.hr.controller.scale.scale;

import com.fasterxml.jackson.core.type.TypeReference;
import com.safwat.hr.controller.scale.scale.dto.ScaleDto;
import com.safwat.hr.controller.scale.scale.dto.SearchScaleEmployee;
import com.safwat.hr.controller.scale.scale.dto.UpdateEmployeeIdentityRequest;
import com.safwat.hr.network.ApiClient;
import com.safwat.hr.network.ApiResponse;
import com.safwat.hr.network.FileTransferClient;
import com.safwat.hr.notification.model.HRNotification;
import com.safwat.hr.notification.service.NotificationService;
import com.safwat.hr.ui.controls.SAFNotification;
import lombok.SneakyThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ScaleApiService {

    private static final String API_BASE = "/salary-scale";

    private static ScaleApiService instance;

    private ScaleApiService() {
    }

    public static synchronized ScaleApiService getInstance() {
        if (instance == null) instance = new ScaleApiService();
        return instance;
    }

    // ═════════════════════════════════════════════════════════════
    //  بحث
    // ═════════════════════════════════════════════════════════════

    public List<SearchScaleEmployee> search(String searchValue, String searchType)
            throws IOException, InterruptedException {

        Map<String, String> body = Map.of(
                "searchValue", searchValue,
                "searchType", searchType);

        ApiResponse<List<SearchScaleEmployee>> response = ApiClient.post(
                API_BASE + "/search",
                body,
                new TypeReference<List<SearchScaleEmployee>>() {
                });

        if (!response.isSuccess()) {
            throw new IOException(response.getMessage() != null
                    ? response.getMessage()
                    : "فشل البحث في السيرفر");
        }
        return response.getData() != null ? response.getData() : List.of();
    }

    // ═════════════════════════════════════════════════════════════
    //  CRUD
    // ═════════════════════════════════════════════════════════════

    public CompletableFuture<ApiResponse<ScaleDto>> findById(String nationalId) {
        return ApiClient.getAsync(API_BASE + "/" + nationalId, ScaleDto.class);
    }

    public CompletableFuture<ApiResponse<ScaleDto>> calculate(ScaleDto dto) {
        return ApiClient.postAsync(API_BASE + "/calculate", dto, ScaleDto.class);
    }

    public CompletableFuture<ApiResponse<ScaleDto>> save(ScaleDto dto) {
        return ApiClient.postAsync(API_BASE + "/save", dto, ScaleDto.class);
    }

    /**
     * حذف سجل السلم الوظيفي (async).
     */
    public CompletableFuture<ApiResponse<Boolean>> delete(String nationalId) {
        return ApiClient.deleteAsync(
                API_BASE + "/" + nationalId,
                Boolean.class);
    }

    /**
     * تعديل الرقم القومي ورقم الموظف والاسم (async).
     */
    public CompletableFuture<ApiResponse<ScaleDto>> updateIdentity(
            String oldNationalId,
            UpdateEmployeeIdentityRequest req) {

        return ApiClient.putAsync(
                API_BASE + "/" + oldNationalId + "/identity",
                req,
                ScaleDto.class);
    }

    // ═════════════════════════════════════════════════════════════
    //  PDF
    // ═════════════════════════════════════════════════════════════

    @SneakyThrows
    public void downloadScale(ScaleDto dto) {

        String fileName = "ScaleReport" + System.currentTimeMillis() + ".pdf";
        String workingDir = System.getProperty("user.dir");

        Path tempDownloadsDir = Paths.get(workingDir, "temp_downloads");
        if (!Files.exists(tempDownloadsDir)) {
            Files.createDirectories(tempDownloadsDir);
        }

        Path targetPath = tempDownloadsDir.resolve(fileName);
        boolean success = FileTransferClient.downloadFileViaPost(
                API_BASE + "/download",
                dto,
                targetPath
        );

        if (success) {
            openPDF(targetPath, "تدرج راتب", dto.getEmpName());
        } else {
            SAFNotification.error("فشل تحميل الملف");
        }
    }

    private void openPDF(Path targetPath, String reportName, String empName) {
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
}