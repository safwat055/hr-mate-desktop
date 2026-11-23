package com.safwat.hr.controller.hr.leave.admin.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.safwat.hr.controller.hr.leave.admin.dto.LeaveAdminDtos.*;

import com.safwat.hr.controller.hr.leave.dto.LeaveTypeDto;
import com.safwat.hr.controller.hr.leave.service.LeaveApiClient.ApiCallException;
import com.safwat.hr.network.ApiClient;
import com.safwat.hr.network.ApiResponse;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class LeaveAdminApiClient {

    private static final String BASE = "/leaves/admin";

    // ═══════════ الأنواع ═══════════

    public List<LeaveTypeDto> listTypes() {
        return wrap(() -> unwrap(ApiClient.getWithTypeRef(BASE + "/types",
                new TypeReference<List<LeaveTypeDto>>() {
                })));
    }

    public LeaveTypeDto createType(LeaveTypeAdminRequest req) {
        return wrap(() -> unwrap(ApiClient.post(BASE + "/types", req, LeaveTypeDto.class)));
    }

    public LeaveTypeDto updateType(Long id, LeaveTypeAdminRequest req) {
        return wrap(() -> unwrap(ApiClient.put(BASE + "/types/" + id, req, LeaveTypeDto.class)));
    }

    public LeaveTypeDto setTypeActive(Long id, boolean value) {
        return wrap(() -> unwrap(ApiClient.put(
                BASE + "/types/" + id + "/active?value=" + value, Map.of(), LeaveTypeDto.class)));
    }

    // ═══════════ قواعد النوع ═══════════

    public List<LeaveTypeRuleDto> listRules(Long typeId) {
        return wrap(() -> unwrap(ApiClient.getWithTypeRef(
                BASE + "/types/" + typeId + "/rules",
                new TypeReference<List<LeaveTypeRuleDto>>() {
                })));
    }

    public LeaveTypeRuleDto createRule(Long typeId, LeaveTypeRuleRequest req) {
        return wrap(() -> unwrap(ApiClient.post(
                BASE + "/types/" + typeId + "/rules", req, LeaveTypeRuleDto.class)));
    }

    public void deleteRule(Long typeId, Long ruleId) {
        wrap(() -> {
            ApiClient.delete(BASE + "/types/" + typeId + "/rules/" + ruleId);
            return null;
        });
    }

    // ═══════════ القوانين ═══════════

    public List<LeaveLawDto> listLaws() {
        return wrap(() -> unwrap(ApiClient.getWithTypeRef(BASE + "/laws",
                new TypeReference<List<LeaveLawDto>>() {
                })));
    }

    public LeaveLawDto createLaw(LeaveLawRequest req) {
        return wrap(() -> unwrap(ApiClient.post(BASE + "/laws", req, LeaveLawDto.class)));
    }

    public LeaveLawDto updateLaw(String code, LeaveLawRequest req) {
        return wrap(() -> unwrap(ApiClient.put(BASE + "/laws/" + enc(code), req, LeaveLawDto.class)));
    }

    public LeaveLawDto setLawActive(String code, boolean value) {
        return wrap(() -> unwrap(ApiClient.put(
                BASE + "/laws/" + enc(code) + "/active?value=" + value, Map.of(), LeaveLawDto.class)));
    }

    // ═══════════ جداول الاستحقاق ═══════════

    public List<AccrualScheduleDto> listSchedules(String lawCode, String poolCode) {
        return wrap(() -> unwrap(ApiClient.getWithTypeRef(
                BASE + "/laws/" + enc(lawCode) + "/accrual-schedules?pool=" + enc(poolCode),
                new TypeReference<List<AccrualScheduleDto>>() {
                })));
    }

    public AccrualScheduleDto publishSchedule(String lawCode, AccrualScheduleRequest req) {
        return wrap(() -> unwrap(ApiClient.post(
                BASE + "/laws/" + enc(lawCode) + "/accrual-schedules", req, AccrualScheduleDto.class)));
    }

    public void deleteSchedule(String lawCode, String poolCode, LocalDate effectiveFrom) {
        wrap(() -> {
            ApiClient.delete(BASE + "/laws/" + enc(lawCode) + "/accrual-schedules"
                    + "?pool=" + enc(poolCode) + "&effectiveFrom=" + effectiveFrom);
            return null;
        });
    }

    // ═══════════ النائية ═══════════

    public List<RemoteBonusDto> listRemote() {
        return wrap(() -> unwrap(ApiClient.getWithTypeRef(BASE + "/remote-bonus",
                new TypeReference<List<RemoteBonusDto>>() {
                })));
    }

    public RemoteBonusDto createRemote(RemoteBonusRequest req) {
        return wrap(() -> unwrap(ApiClient.post(BASE + "/remote-bonus", req, RemoteBonusDto.class)));
    }

    public void deleteRemote(Long id) {
        wrap(() -> {
            ApiClient.delete(BASE + "/remote-bonus/" + id);
            return null;
        });
    }

    // ═══════════ بيانات الموظف ═══════════

    public EmployeeLawProfileDto getEmployeeLaw(String nationalId) {
        return wrap(() -> unwrap(ApiClient.get(
                BASE + "/employees/" + enc(nationalId), EmployeeLawProfileDto.class)));
    }

    public EmployeeLawProfileDto setLaw(String nationalId, LawChangeRequest req) {
        return wrap(() -> unwrap(ApiClient.put(
                BASE + "/employees/" + enc(nationalId) + "/law", req, EmployeeLawProfileDto.class)));
    }

    public EmployeeLawProfileDto setRemote(String nationalId, RemoteChangeRequest req) {
        return wrap(() -> unwrap(ApiClient.put(
                BASE + "/employees/" + enc(nationalId) + "/remote", req, EmployeeLawProfileDto.class)));
    }

    public List<OpeningBalanceDto> listOpenings(String nationalId) {
        return wrap(() -> unwrap(ApiClient.getWithTypeRef(
                BASE + "/employees/" + enc(nationalId) + "/opening-balances",
                new TypeReference<List<OpeningBalanceDto>>() {
                })));
    }

    public OpeningBalanceDto saveOpening(String nationalId, OpeningBalanceRequest req) {
        return wrap(() -> unwrap(ApiClient.put(
                BASE + "/employees/" + enc(nationalId) + "/opening-balances", req, OpeningBalanceDto.class)));
    }

    public void deleteOpening(String nationalId, int leaveYear) {
        wrap(() -> {
            ApiClient.delete(BASE + "/employees/" + enc(nationalId)
                    + "/opening-balances/" + leaveYear);
            return null;
        });
    }

    // ═══════════ Helpers ═══════════

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    private static <T> T wrap(ThrowingSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (ApiCallException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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
}