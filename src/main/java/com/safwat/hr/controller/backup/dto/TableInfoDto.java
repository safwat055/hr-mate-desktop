package com.safwat.hr.controller.backup.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TableInfoDto {

    private String schema;
    private String tableName;
    private long rowCount;
    private int columnCount;
    private List<String> primaryKeyColumns;
    private List<ColumnInfo> columns;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ColumnInfo {
        private String name;
        private String sqlType;
        private boolean nullable;
        private boolean primaryKey;
        private boolean autoIncrement;
    }
}