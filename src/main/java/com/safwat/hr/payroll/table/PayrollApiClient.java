package com.safwat.hr.payroll.table;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.safwat.hr.network.ApiClient;
import com.safwat.hr.network.ApiResponse;
import lombok.SneakyThrows;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * عميل استدعاء الباك إند الخاص بشيت الإكسيل.
 * <p>
 * يعتمد بالكامل على {@link ApiClient} العام (نفس التوكن / نفس الـ base URL /
 * نفس منطق الأخطاء المستخدم في باقي الشاشات) — بدون أي HttpClient خاص
 * وبدون AuthContext منفصل.
 */
public final class PayrollApiClient {

    private PayrollApiClient() {
    }

    public static Optional<PayrollIndexEntity> getPayrollIndexByNationalId(String nationalId) {
        ApiResponse<PayrollIndexEntity> response = call(() ->
                ApiClient.post("/payrollIndex/nationalId",
                        Map.of("nationalId", nationalId),
                        PayrollIndexEntity.class));
        return response.isSuccess() ? Optional.ofNullable(response.getData()) : Optional.empty();
    }

    public record PayrollIndexEntity(
            Long id,
            String nationalId,
            String payId,
            String empName,
            String empStatus,
            String assignmentClass,
            String payManagement,
            String degree,
            String job,
            String secondaryPayId,
            Double basic306,
            Double salaryBasic
    ) {
    }
    // ==================== DTOs (مطابقة للباك إند) ====================

    /**
     * استجابة جلب صرفية ثابتة — مطابقة لـ PayrollTableDtos.TableResponse
     */
    public record PayrollTableResponse(
            String tableId,
            String tableElement,
            String status,
            Map<Integer, Object[]> tableData
    ) {
    }

    public record TempSheetResponse(
            String user,
            String payrollGroup,
            String elementGroup,
            Map<Integer, Object[]> tableData
    ) {
    }

    /**
     * نتيجة بحث اللوك أب الدقيق — مطابقة لـ PayrollIndex.lookupResult
     */
    public record LookupResult(
            String nationalId,
            String payId,
            String empName,
            String empStatus,
            String payManagement,
            String assignmentClass
    ) {
    }

    /**
     * نتيجة البحث الحر — مطابقة لـ PayrollIndex.searchEmployee بالباك إند.
     * أسماء الحقول في الـ JSON قادمة snake_case زي ما الـ record الأصلي معرِّفها
     * (national_id, pay_id, ...)، فبنربطها بـ JsonProperty بدل ما نغيّر تسمية الجافا.
     */
    public record SearchEmployeeResult(
            @JsonProperty("national_id") String nationalId,
            @JsonProperty("pay_id") String payId,
            @JsonProperty("emp_name") String empName,
            @JsonProperty("emp_status") String empStatus,
            @JsonProperty("assignment_class") String assignmentClass,
            @JsonProperty("pay_management") String payManagement
    ) {
    }

    public static final String SEARCH_MAIN = "MAIN";
    public static final String SEARCH_SECONDARY = "SECONDARY";

    // ==================== القوائم (لواجهات البحث) ====================

    public static List<String> getPayrollGroupNames() {
        return getList("/payrollGroups/payrollGroups");
    }

    public static List<String> getElementGroupNames() {
        return getList("/payrollGroups/elementGroups");
    }

    public static List<String> getVisibleTableIds() {
        return getList("/payrollTables/allIds");
    }

    @SneakyThrows
    public static List<String> getElementsByGroup(String groupName) {
        Map<String, String> payload = Map.of("groupName", groupName);

        return ApiClient.post("/payrollGroups/elements", payload, new TypeReference<List<String>>() {
        }).getData();

    }

    // ==================== الصرفيات الثابتة ====================

    public static Optional<PayrollTableResponse> getPayrollTable(String tableId) {
        ApiResponse<PayrollTableResponse> response = call(() ->
                ApiClient.post("/payrollTables/get", Map.of("tableId", tableId), PayrollTableResponse.class));
        return response.isSuccess() ? Optional.ofNullable(response.getData()) : Optional.empty();
    }

    public static void savePayrollTable(String tableId, String tableElement, String status,
                                        Map<Integer, Object[]> tableData) {
        Map<String, Object> payload = Map.of(
                "tableId", tableId,
                "tableElement", tableElement,
                "status", status,
                "tableData", tableData
        );
        requireSuccess(call(() -> ApiClient.post("/payrollTables/save", payload, Void.class)),
                "فشل حفظ الصرفية");
    }

    public static void deletePayrollTable(String tableId) {
        Map<String, String> payload = Map.of("tableId", tableId);
        requireSuccess(call(() -> ApiClient.post("/payrollTables/delete", payload, Void.class)),
                "فشل حذف الصرفية");
    }

    // ==================== الحفظ المؤقت ====================

    public static void tempSave(String payrollGroup, String elementGroup, Map<Integer, Object[]> tableData) {
        Map<String, Object> payload = Map.of(
                "payrollGroup", payrollGroup == null ? "" : payrollGroup,
                "elementGroup", elementGroup == null ? "" : elementGroup,
                "tableData", tableData
        );
        requireSuccess(call(() -> ApiClient.post("/tempSheet/save", payload, Void.class)),
                "فشل الحفظ المؤقت");
    }

    public static Optional<TempSheetResponse> tempLoad() {
        ApiResponse<TempSheetResponse> response = call(() -> ApiClient.get("/tempSheet/load", TempSheetResponse.class));
        return response.isSuccess() ? Optional.ofNullable(response.getData()) : Optional.empty();
    }

    // ==================== التصدير ====================

    public static long exportSheet(String format, String firstTitle, String secondTitle,
                                   List<String> headers, Map<Integer, Object[]> tableData) {
        String currentUser = ApiClient.getUserName();
        Map<String, Object> payload = Map.of(
                "reportName", "صرفية " + firstTitle,
                "format", format,
                "user", currentUser == null ? "" : currentUser,
                "firstTitle", firstTitle,
                "secondTitle", secondTitle,
                "headers", headers,
                "tableData", tableData
        );
        ApiResponse<Long> response = call(() -> ApiClient.post("/payrollSheet/export", payload, Long.class));
        if (!response.isSuccess() || response.getData() == null) {
            throw new RuntimeException(errorMessage(response, "فشل تقديم طلب التصدير"));
        }
        return response.getData();
    }

    // ==================== البحث ====================

    /**
     * بحث حر — يبحث بأي جزء (ولو حرف واحد) من الرقم القومي، أو رقم الصرف،
     * أو اسم الموظف معًا، ويرجع كل الصفوف المطابقة.
     * POST /api/payrollIndex/get/searchEmployee
     */
    public static List<SearchEmployeeResult> searchEmployees(String searchValue) {
        ApiResponse<List<SearchEmployeeResult>> response = call(() ->
                ApiClient.post("/payrollIndex/get/searchEmployee",
                        Map.of("searchValue", searchValue),
                        new TypeReference<List<SearchEmployeeResult>>() {
                        }));
        if (!response.isSuccess()) {
            throw new RuntimeException(errorMessage(response, "لا توجد نتائج مطابقة للبحث"));
        }
        return response.getData() != null ? response.getData() : List.of();
    }

    /**
     * بحث دقيق (تطابق تام) بالكود الرئيسي أو الثانوي — يُستخدم فقط عند اختيار
     * "الكود الثانوي" في شاشة الإدخال. POST /api/payrollIndex/get/lookup
     */
    public static List<LookupResult> lookup(String searchValue, String searchType) {
        ApiResponse<List<LookupResult>> response = call(() ->
                ApiClient.post("/payrollIndex/get/lookup",
                        Map.of("searchValue", searchValue, "searchType", searchType),
                        new TypeReference<List<LookupResult>>() {
                        }));
        if (!response.isSuccess()) {
            throw new RuntimeException(errorMessage(response, "لا توجد نتائج مطابقة للبحث"));
        }
        return response.getData() != null ? response.getData() : List.of();
    }

    // ==================== Helpers ====================

    private static List<String> getList(String path) {
        ApiResponse<List<String>> response = call(() ->
                ApiClient.getWithTypeRef(path, new TypeReference<List<String>>() {
                }));
        return response.isSuccess() && response.getData() != null ? response.getData() : List.of();
    }

    private static void requireSuccess(ApiResponse<?> response, String fallbackMessage) {
        if (!response.isSuccess()) {
            throw new RuntimeException(errorMessage(response, fallbackMessage));
        }
    }

    private static String errorMessage(ApiResponse<?> response, String fallback) {
        return response.getMessage() != null && !response.getMessage().isBlank()
                ? response.getMessage() : fallback;
    }

    private static String urlEncode(String v) {
        return URLEncoder.encode(v, StandardCharsets.UTF_8);
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws IOException, InterruptedException;
    }

    private static <T> T call(ThrowingSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("تعذر الاتصال بالسيرفر", e);
        }
    }
}
