package com.safwat.hr.controller.entitlements.allowance.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobTitleDto {
    private Long id;
    private Long sectorId;
    private String sectorCode;
    private String sectorNameAr;
    private String code;
    private String nameAr;      // "تعليم.إداري"
    private String nameEn;
    private Integer displayOrder;
    private Boolean active;
    private Boolean subjectToLaw81;
    private String notes;

    // ══════════════════════════════════════════════════════════════
    //  Smart display helpers — تدعم الصيغتين للمرحلة الانتقالية
    // ══════════════════════════════════════════════════════════════

    /**
     * اسم القطاع من الـ {@code nameAr}.
     * <ul>
     *   <li>{@code "تعليم.إداري"} → {@code "تعليم"} ✅</li>
     *   <li>{@code "إداري.تعليم"} → {@code "تعليم"} (legacy)</li>
     *   <li>{@code "إداري"} → {@code sectorNameAr} (fallback)</li>
     * </ul>
     */
    public String getDisplaySector() {
        String raw = nameAr;
        if (raw == null || raw.isBlank()) return sectorNameAr;
        if (!raw.contains(".")) return sectorNameAr;

        // الصيغة الجديدة: "قطاع.وظيفة" → القطاع أول
        String firstPart = raw.substring(0, raw.indexOf('.'));
        String lastPart = raw.substring(raw.lastIndexOf('.') + 1);

        if (sectorNameAr != null) {
            if (firstPart.equals(sectorNameAr)) return firstPart;  // صيغة جديدة
            if (lastPart.equals(sectorNameAr)) return lastPart;    // صيغة قديمة
        }
        // احتياطي: الجزء الأول
        return firstPart;
    }

    /**
     * اسم الوظيفة المجرّد (بدون القطاع).
     * <ul>
     *   <li>{@code "تعليم.إداري"} → {@code "إداري"} ✅</li>
     *   <li>{@code "إداري.تعليم"} → {@code "إداري"} (legacy)</li>
     *   <li>{@code "إداري"} → {@code "إداري"}</li>
     * </ul>
     */
    public String getDisplayName() {
        String raw = nameAr;
        if (raw == null || raw.isBlank()) return raw;
        if (!raw.contains(".")) return raw;

        String firstPart = raw.substring(0, raw.indexOf('.'));
        String lastPart = raw.substring(raw.lastIndexOf('.') + 1);

        if (sectorNameAr != null) {
            if (firstPart.equals(sectorNameAr)) {
                // صيغة جديدة: القطاع أول → الوظيفة بعد أول نقطة
                return raw.substring(raw.indexOf('.') + 1);
            }
            if (lastPart.equals(sectorNameAr)) {
                // صيغة قديمة: القطاع آخر → الوظيفة قبل آخر نقطة
                return raw.substring(0, raw.lastIndexOf('.'));
            }
        }
        // احتياطي: الجزء بعد أول نقطة
        return raw.substring(raw.indexOf('.') + 1);
    }

    /**
     * عرض موحّد للمستخدم: {@code "تعليم · إداري"}.
     * <p>ملاحظة RTL: "تعليم" تظهر على اليمين، "إداري" على اليسار.
     */
    public String getDisplayLabel() {
        String sector = getDisplaySector();
        String name = getDisplayName();
        if (sector == null || sector.isBlank()) return name != null ? name : "";
        if (name == null || name.isBlank()) return sector;
        return sector + " · " + name;
    }

    @Override
    public String toString() {
        return getDisplayLabel();
    }
}