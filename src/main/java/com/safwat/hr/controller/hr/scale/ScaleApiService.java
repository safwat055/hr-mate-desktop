package com.safwat.hr.controller.hr.scale;

import com.fasterxml.jackson.core.type.TypeReference;
import com.safwat.hr.controller.hr.scale.dto.ScaleDto;
import com.safwat.hr.controller.hr.scale.dto.SearchScaleEmployee;
import com.safwat.hr.controller.hr.scale.dto.UpdateEmployeeIdentityRequest;
import com.safwat.hr.network.ApiClient;
import com.safwat.hr.network.ApiResponse;
import com.safwat.hr.network.FileTransferClient;
import com.safwat.hr.notification.model.HRNotification;
import com.safwat.hr.notification.service.NotificationService;
import com.safwat.hr.ui.controls.SAFNotification;
import javafx.application.Platform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

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
    //  PDF — بترجع مسار الملف المحمّل للعرض في الـ WebView
    // ═════════════════════════════════════════════════════════════

    /**
     * تدرج كامل لحد الإحالة (زرار PDF).
     */
    public CompletableFuture<Path> downloadScaleToFile(ScaleDto dto) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return downloadToTemp(API_BASE + "/download", dto, "ScaleReport", "تدرج الراتب");
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    /**
     * تفاصيل الراتب ابتداءً من تاريخ معين (زرار تدرج مختصر).
     * لو fromDate فاضي، الـ PDF بيبدأ من أول التعيين.
     */
    public CompletableFuture<Path> downloadSalaryDetailsToFile(ScaleDto dto, LocalDate fromDate) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String endpoint = API_BASE + "/" + dto.getNationalId() + "/details/pdf";
                if (fromDate != null) endpoint += "?fromDate=" + fromDate;
                return downloadToTemp(endpoint, dto, "SalaryDetails", "تفاصيل الراتب");
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    private Path downloadToTemp(String endpoint, ScaleDto dto, String prefix, String reportName) throws Exception {
        Path dir = Paths.get(System.getProperty("user.dir"), "temp_downloads");
        Files.createDirectories(dir);
        Path target = dir.resolve(prefix + System.currentTimeMillis() + ".pdf");

        boolean ok = FileTransferClient.downloadFileViaPost(endpoint, dto, target);
        if (!ok) throw new RuntimeException("فشل تحميل الملف");

        String empName = dto.getEmpName();
        Platform.runLater(() -> notifyPdfReady(target, reportName, empName));
        return target;
    }

    private void notifyPdfReady(Path target, String reportName, String empName) {
        SAFNotification.withAction(target.toString(), target.toFile());
        NotificationService.getInstance().send(
                HRNotification.builder()
                        .type(HRNotification.NotificationType.SYSTEM)
                        .priority(HRNotification.Priority.HIGH)
                        .title(reportName)
                        .message(empName)
                        .file(target.toString())
                        .sender("system")
                        .build()
        );
    }
}