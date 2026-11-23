package com.safwat.hr.controller.hr.leave.admin.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class LeaveAdminDtos {

    private LeaveAdminDtos() {
    }

    // ═══════════ الأنواع ═══════════

    @Getter
    @Setter
    public static class LeaveTypeAdminRequest {
        private String code;
        private String nameAr;
        private String category;
        private String genderRestriction;
        private Boolean openEnded;
        private Boolean overlapAllowed;
        private Boolean active;
    }

    @Getter
    @Setter
    public static class LeaveTypeRuleRequest {
        private String lawCode;
        private LocalDate effectiveFrom;
        private String payMode;
        private String poolCode;
        private boolean countsAgainstAccrual;
        private Map<String, Object> limits;
    }

    @Getter
    @Setter
    public static class LeaveTypeRuleDto {
        private Long id;
        private String lawCode;
        private LocalDate effectiveFrom;
        private LocalDate effectiveTo;
        private String payMode;
        private String poolCode;
        private boolean countsAgainstAccrual;
        private Map<String, Object> limits;
        private long usageCount;
        private boolean inUse;
    }

    // ═══════════ القوانين ═══════════

    @Getter
    @Setter
    public static class LeaveLawRequest {
        private String code;
        private String nameAr;
        private LocalDate effectiveFrom;
        private Integer yearStartMonth;
        private Integer yearStartDay;
    }

    @Getter
    @Setter
    public static class LeaveLawDto {
        private String code;
        private String nameAr;
        private LocalDate effectiveFrom;
        private int yearStartMonth;
        private int yearStartDay;
        private boolean active;
        private long recordsCount;

        @Override
        public String toString() {
            return code + " — " + nameAr;
        }
    }

    // ═══════════ جداول الاستحقاق ═══════════

    @Getter
    @Setter
    public static class AccrualRuleItem {
        private Integer priority;
        private Map<String, Object> conditions;
        private Integer days;
    }

    @Getter
    @Setter
    public static class AccrualRuleDto {
        private Long id;
        private int priority;
        private Map<String, Object> conditions;
        private int days;
    }

    @Getter
    @Setter
    public static class AccrualScheduleRequest {
        private LocalDate effectiveFrom;
        private String poolCode;
        private List<AccrualRuleItem> rules;
    }

    @Getter
    @Setter
    public static class AccrualScheduleDto {
        private LocalDate effectiveFrom;
        private LocalDate effectiveTo;
        private String poolCode;
        private List<AccrualRuleDto> rules;
    }

    // ═══════════ النائية ═══════════

    @Getter
    @Setter
    public static class RemoteBonusRequest {
        private LocalDate effectiveFrom;
        private Integer days;
    }

    @Getter
    @Setter
    public static class RemoteBonusDto {
        private Long id;
        private LocalDate effectiveFrom;
        private int days;
    }

    // ═══════════ بيانات الموظف ═══════════

    @Getter
    @Setter
    public static class EmployeeLawProfileDto {
        private String nationalId;
        private LocalDate hireDate;
        private LocalDate workReceiptDate;
        private Map<LocalDate, String> lawHistory = new TreeMap<>();
        private Map<LocalDate, Boolean> remoteHistory = new TreeMap<>();
    }

    @Getter
    @Setter
    public static class LawChangeRequest {
        private String lawCode;
        private LocalDate effectiveFrom;
    }

    @Getter
    @Setter
    public static class RemoteChangeRequest {
        private Boolean value;
        private LocalDate effectiveFrom;
    }

    @Getter
    @Setter
    public static class OpeningBalanceRequest {
        private Integer leaveYear;
        private String poolCode;
        private Integer remainingAtTransfer;
        private Integer ownUsedBeforeTransfer;
        private String sourceUnit;
        private String statementRef;
    }

    @Getter
    @Setter
    public static class OpeningBalanceDto {
        private Long id;
        private int leaveYear;
        private String poolCode;
        private int remainingAtTransfer;
        private int ownUsedBeforeTransfer;
        private String sourceUnit;
        private String statementRef;
        private String recordedBy;
        private LocalDateTime recordedAt;
    }
}