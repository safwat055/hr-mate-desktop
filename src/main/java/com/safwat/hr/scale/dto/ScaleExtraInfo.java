package com.safwat.hr.scale.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ScaleExtraInfo {
    private LocalDate regrade3;
    private LocalDate regrade4;
    private LocalDate regrade5;
    private LocalDate reBackDate;
    private LocalDate debloma;
    private LocalDate magester;
    private LocalDate doctoraa;
    private Integer yearBack;
    private Integer yearUp;
    private Integer yearNoUp;
    private BigDecimal gpUp;
    private BigDecimal gpNoUp;
    private String periodicCalcType;

    private static final String DEFAULT_CALC_TYPE = "الكل على 30-6";

    public static ScaleExtraInfo empty() {
        ScaleExtraInfo info = new ScaleExtraInfo();

        info.periodicCalcType = DEFAULT_CALC_TYPE;
        return info;
    }
}