package com.safwat.hr.controller.backup.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SchemaSummary {
    private String name;
    private long tableCount;

    @Override
    public String toString() {
        return name + " (" + tableCount + " جدول)";
    }
}