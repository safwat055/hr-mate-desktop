package com.safwat.hr.controller.leave.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.safwat.hr.controller.employee.dto.EmployeeSearchResult;
import com.safwat.hr.controller.leave.dto.*;
import com.safwat.hr.network.ApiClient;
import com.safwat.hr.network.ApiResponse;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * غلاف HTTP لشاشة الإجازات.
 * <p>
 * قاعدة أساسية: كل method بترمي RuntimeException (ApiCallException)
 * بدل checked exceptions — عشان تشتغل مباشرة مع UiAsync.run بدون try/catch.
 */
public class LeaveApiClient {

    // ═══════════════════════════════════════════
    //  Employees
    // ═══════════════════════════════════════════

    public List<EmployeeSearchResult> searchEmployees(String q) {
        return wrap(() -> {
            String path = "/employees/search?q=" + enc(q);
            ApiResponse<List<EmployeeSearchResult>> resp = ApiClient.getWithTypeRef(
                    path, new TypeReference<List<EmployeeSearchResult>>() {
                    });
            return unwrap(resp);
        });
    }

    // ═══════════════════════════════════════════
    //  Leave Types
    // ═══════════════════════════════════════════

    public List<LeaveTypeDto> getLeaveTypes() {
        return wrap(() -> {
            ApiResponse<List<LeaveTypeDto>> resp = ApiClient.getWithTypeRef(
                    "/leaves/types", new TypeReference<List<LeaveTypeDto>>() {
                    });
            return unwrap(resp);
        });
    }

    // ═══════════════════════════════════════════
    //  Summary
    // ═══════════════════════════════════════════

    public LeaveSummaryDto getSummary(String nationalId, int year) {
        return wrap(() -> {
            String path = "/leaves/summary?nationalId=" + enc(nationalId) + "&year=" + year;
            ApiResponse<LeaveSummaryDto> resp = ApiClient.get(path, LeaveSummaryDto.class);
            return unwrap(resp);
        });
    }

    // ═══════════════════════════════════════════
    //  Records
    // ═══════════════════════════════════════════

    public PagedResult<LeaveRecordDto> getRecords(String nationalId, String typeCode,
                                                  Integer year, int page, int size) {
        return wrap(() -> {
            StringBuilder sb = new StringBuilder("/leaves/records?nationalId=")
                    .append(enc(nationalId))
                    .append("&page=").append(page)
                    .append("&size=").append(size);
            if (typeCode != null && !typeCode.isBlank())
                sb.append("&typeCode=").append(enc(typeCode));
            if (year != null) sb.append("&year=").append(year);

            ApiResponse<List<LeaveRecordDto>> resp = ApiClient.getWithTypeRef(
                    sb.toString(), new TypeReference<List<LeaveRecordDto>>() {
                    });
            List<LeaveRecordDto> data = unwrap(resp);
            return new PagedResult<>(data, resp.getPagination());
        });
    }

    public LeaveRecordDto createRecord(LeaveRecordRequest req) {
        return wrap(() -> {
            ApiResponse<LeaveRecordDto> resp = ApiClient.post(
                    "/leaves/records", req, LeaveRecordDto.class);
            return unwrap(resp);
        });
    }

    public LeaveRecordDto updateRecord(Long id, LeaveRecordRequest req) {
        return wrap(() -> {
            ApiResponse<LeaveRecordDto> resp = ApiClient.put(
                    "/leaves/records/" + id, req, LeaveRecordDto.class);
            return unwrap(resp);
        });
    }

    public void deleteRecord(Long id) {
        wrap(() -> {
            ApiClient.delete("/leaves/records/" + id);
            return null;
        });
    }

    public LeaveRecordDto closeRecord(Long id, String toDate) {
        return wrap(() -> {
            ApiResponse<LeaveRecordDto> resp = ApiClient.put(
                    "/leaves/records/" + id + "/close",
                    Map.of("toDate", toDate), LeaveRecordDto.class);
            return unwrap(resp);
        });
    }

    // ═══════════════════════════════════════════
    //  Late Permissions
    // ═══════════════════════════════════════════

    public List<LatePermissionDto> getLatePermissions(String nationalId, Integer year) {
        return wrap(() -> {
            StringBuilder sb = new StringBuilder("/leaves/late-permissions?nationalId=")
                    .append(enc(nationalId));
            if (year != null) sb.append("&year=").append(year);

            ApiResponse<List<LatePermissionDto>> resp = ApiClient.getWithTypeRef(
                    sb.toString(), new TypeReference<List<LatePermissionDto>>() {
                    });
            return unwrap(resp);
        });
    }

    public LatePermissionDto createLate(LatePermissionRequest req) {
        return wrap(() -> {
            ApiResponse<LatePermissionDto> resp = ApiClient.post(
                    "/leaves/late-permissions", req, LatePermissionDto.class);
            return unwrap(resp);
        });
    }

    public LatePermissionDto updateLate(Long id, LatePermissionRequest req) {
        return wrap(() -> {
            ApiResponse<LatePermissionDto> resp = ApiClient.put(
                    "/leaves/late-permissions/" + id, req, LatePermissionDto.class);
            return unwrap(resp);
        });
    }

    public void deleteLate(Long id) {
        wrap(() -> {
            ApiClient.delete("/leaves/late-permissions/" + id);
            return null;
        });
    }

    // ═══════════════════════════════════════════
    //  Recalculate / Reset
    // ═══════════════════════════════════════════

    /**
     * إعادة حساب رصيد سنة واحدة فقط.
     */
    public void recalculate(String nationalId, int year) {
        wrap(() -> {
            String path = "/leaves/balance/recalculate?nationalId="
                    + enc(nationalId) + "&year=" + year;
            ApiClient.post(path, null, Void.class);
            return null;
        });
    }

    /**
     * ★ إعادة بناء كامل — يحذف كل أرصدة الموظف (leave_balance)
     * ويعيد حسابها من سنة التعيين حتى السنة الحالية.
     * <p>
     * لا يحذف أي سجل إجازة (leave_record) — الإجازات المسجلة تبقى كما هي.
     */
    public void resetBalance(String nationalId) {
        wrap(() -> {
            String path = "/leaves/balance/reset?nationalId=" + enc(nationalId);
            ApiClient.post(path, null, Void.class);
            return null;
        });
    }

    // ═══════════════════════════════════════════
    //  Helpers — قلب الحل
    // ═══════════════════════════════════════════

    /**
     * Functional interface بيسمح بـ checked exceptions جواه.
     */
    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    /**
     * بيشغّل supplier ويغلّف أي exception (بما فيها IOException/InterruptedException)
     * في ApiCallException (RuntimeException).
     * <p>
     * ده اللي بيخلي الـ Controller يستخدم api.X() مباشرة بدون try/catch.
     */
    private static <T> T wrap(ThrowingSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (ApiCallException e) {
            throw e;                       // سبق ولُفّ
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();  // رجّع الـ flag
            throw new ApiCallException("تم إلغاء العملية");
        } catch (Exception e) {
            throw new ApiCallException(messageOf(e));
        }
    }

    private static String messageOf(Throwable ex) {
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) root = root.getCause();
        return root.getMessage() != null ? root.getMessage() : root.getClass().getSimpleName();
    }

    private static <T> T unwrap(ApiResponse<T> resp) {
        if (resp == null || !resp.isSuccess()) {
            throw new ApiCallException(resp == null
                    ? "لا يوجد رد من السيرفر"
                    : resp.getMessage());
        }
        return resp.getData();
    }

    private static String enc(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    /**
     * استثناء موحّد — الـ Controller يعرض رسالته مباشرة.
     */
    public static class ApiCallException extends RuntimeException {
        public ApiCallException(String message) {
            super(message);
        }
    }

    /**
     * نتيجة paginated — بتغلّف data + pagination.
     */
    public static class PagedResult<T> {
        private final List<T> items;
        private final ApiResponse.PaginationInfo pagination;

        public PagedResult(List<T> items, ApiResponse.PaginationInfo pagination) {
            this.items = items;
            this.pagination = pagination;
        }

        public List<T> items() {
            return items;
        }

        public ApiResponse.PaginationInfo pagination() {
            return pagination;
        }
    }
}