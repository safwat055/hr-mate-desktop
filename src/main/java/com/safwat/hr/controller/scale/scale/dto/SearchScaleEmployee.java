package com.safwat.hr.controller.scale.scale.dto;

import java.math.BigDecimal;

public record SearchScaleEmployee(
        String nationalId,
        String codeId,
        String empName,
        Integer law,
        BigDecimal lawCode,
        String qualitativeGroup

) {
    public String castToString() {
        if (this.law == null) {
            return "";

        } else {
            return this.law.toString();
        }
    }
}
