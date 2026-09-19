package com.safwat.hr.controller.entitlements.allowance.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SectorDto {
    private Long id;
    private String code;
    private String nameAr;
    private String nameEn;
    private Integer displayOrder;
    private Boolean active;
    private String notes;

    @Override
    public String toString() {
        return nameAr != null ? nameAr : code;
    }
}