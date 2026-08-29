package com.safwat.hr.scale.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * نتيجة حساب التدرج في نقطة زمنية واحدة.
 * نسخة الواجهة — mirror للـ record في الخلفية.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScaleTimelinePoint {
    private LocalDate date;
    private int degree;
    private String degreeLabel;
    private BigDecimal currentBasic;
    private BigDecimal basic30_6;
    private BigDecimal mogard;
    private BigDecimal periodicBonus;
    private BigDecimal upgradeBonus;
    private BigDecimal encourageBonus;
    private BigDecimal spBonusNotSubject;
    private BigDecimal spBonusSubject;
    private BigDecimal other_sp_subject;
    private BigDecimal otherBonus;
    private BigDecimal extraIncentive;
    private BigDecimal socialPackage;
}