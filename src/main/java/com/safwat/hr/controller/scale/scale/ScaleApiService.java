package com.safwat.hr.controller.scale.scale;

import com.fasterxml.jackson.core.type.TypeReference;
import com.safwat.hr.controller.scale.scale.dto.ScaleDto;
import com.safwat.hr.controller.scale.scale.dto.SearchScaleEmployee;
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

/**
 * خدمة الاتصال بالـ backend لشاشة السلم الوظيفي.
 * <p>
 * كل استدعاءات REST بتتجمّع هنا، والكنترولر بيتعامل معاها كـ API نظيف
 * من غير أي تفاصيل HTTP أو TypeReference أو Map بناء على الـ URL.
 *
 * <h3>العمليات:</h3>
 * <ul>
 *   <li>{@link #search(String, String)} — بحث متزامن عن الموظفين</li>
 *   <li>{@link #findById(String)} — جلب بيانات موظف كاملة (async)</li>
 *   <li>{@link #calculate(ScaleDto)} — احتساب بدون حفظ (async)</li>
 *   <li>{@link #save(ScaleDto)} — حفظ + احتساب (async)</li>
 * </ul>
 */
public class ScaleApiService {

    private static final String API_BASE = "/salary-scale";

    private static ScaleApiService instance;

    private ScaleApiService() {
        // Singleton — مفيش إنشاء من بره
    }

    public static synchronized ScaleApiService getInstance() {
        if (instance == null) {
            instance = new ScaleApiService();
        }
        return instance;
    }

    // ═════════════════════════════════════════════════════════════
    //  بحث
    // ═════════════════════════════════════════════════════════════

    /**
     * بحث عن الموظفين (متزامن — الشاشة محتاجة النتيجة فورًا لعرض الـ dialog).
     *
     * @param searchValue القيمة (رقم قومي / كود / اسم)
     * @param searchType  نوع البحث (nationalId / code / empName / all)
     * @return قائمة الموظفين المطابقين (فارغة لو مفيش نتائج)
     * @throws IOException          في حالة فشل الاتصال أو رجوع رد غير ناجح
     * @throws InterruptedException في حالة قطع الـ thread
     */
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

    /**
     * جلب بيانات موظف كاملة بالرقم القومي (async).
     */
    public CompletableFuture<ApiResponse<ScaleDto>> findById(String nationalId) {
        return ApiClient.getAsync(API_BASE + "/" + nationalId, ScaleDto.class);
    }

    // ═════════════════════════════════════════════════════════════
    //  احتساب / حفظ
    // ═════════════════════════════════════════════════════════════

    /**
     * احتساب السلم بدون حفظ (async).
     */
    public CompletableFuture<ApiResponse<ScaleDto>> calculate(ScaleDto dto) {
        return ApiClient.postAsync(API_BASE + "/calculate", dto, ScaleDto.class);
    }

    /**
     * حفظ السلم + الاحتساب (async).
     */
    public CompletableFuture<ApiResponse<ScaleDto>> save(ScaleDto dto) {
        return ApiClient.postAsync(API_BASE + "/save", dto, ScaleDto.class);
    }

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