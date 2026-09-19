package com.safwat.hr.controller.backup.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TableSummary {
    private String schema;
    private String tableName;

    @Override
    public String toString() {
        return tableName;
    }
}