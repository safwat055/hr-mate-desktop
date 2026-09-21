package com.safwat.hr.controller.scale.scale.dto;

public record SearchScaleEmployee(
        String nationalId,
        String codeId,
        String empName,
        Integer law,
        String lawCode,
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
