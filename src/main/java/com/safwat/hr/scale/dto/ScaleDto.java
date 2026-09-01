package com.safwat.hr.scale.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * الكلاس المجمع لشاشة السلم الوظيفي.
 * يُستخدم في الاتجاهين بين الواجهة والخلفية.
 *
 * <p>الواجهة ترسله بدون result → الخلفية ترجعه مع result مملوء.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScaleDto {

    // ══════════════════════════════════════════
    //  بيانات الموظف الأساسية
    // ══════════════════════════════════════════

    private String nationalId;
    private String empName;
    private String codeId;
    private Integer law;
    private BigDecimal lawCode;
    private String qualitativeGroup;
    private Integer startDegree;
    private LocalDate startDate;
    private LocalDate restartDate;
    private LocalDate basic30Date;
    private LocalDate basic30From;
    private BigDecimal startTied;
    // ══════════════════════════════════════════
    //  فترة القطع
    // ══════════════════════════════════════════

    private LocalDate cutStart;
    private LocalDate cutEnd;


    private ScaleExtraInfo extraInfo;
    // ══════════════════════════════════════════
    //  الأحداث
    // ══════════════════════════════════════════

    private List<UpgradeRecord> upgrades;
    private List<EncouragementRecord> encouragements;
    private List<PromotionIncentiveRecord> promotionIncentives;
    private List<GroupChangeRecord> groupChanges;

    // ══════════════════════════════════════════
    //  الإضافات والخصومات
    // ══════════════════════════════════════════

    private List<AdjustmentRecord> mogardAdditions;
    private List<AdjustmentRecord> mogardRemovals;
    private List<AdjustmentRecord> bonusAdditions;
    private List<AdjustmentRecord> bonusRemovals;

    // ══════════════════════════════════════════
    //  نتيجة الاحتساب — null لو مش محسوب
    // ══════════════════════════════════════════

    private List<ScaleTimelinePoint> timeline;

    // ══════════════════════════════════════════
    //  Inner DTO — النتيجة
    // ══════════════════════════════════════════



}