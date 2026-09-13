package com.safwat.hr.controller.backup.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BackupFileInfo {
    private String fileName;
    private String fullPath;
    private long sizeBytes;
    private String createdAt;   // String بدل LocalDateTime — تجنباً لمشاكل الـ format
    private BackupFormat format;

    public String getSizeFormatted() {
        if (sizeBytes < 1024) return sizeBytes + " B";
        if (sizeBytes < 1024 * 1024) return String.format("%.1f KB", sizeBytes / 1024.0);
        if (sizeBytes < 1024 * 1024 * 1024) return String.format("%.1f MB", sizeBytes / (1024.0 * 1024));
        return String.format("%.2f GB", sizeBytes / (1024.0 * 1024 * 1024));
    }

    /**
     * يحول createdAt لـ LocalDateTime للعرض في الجدول.
     * يدعم ISO format وformat الخادم.
     */
    public LocalDateTime getCreatedAtAsDateTime() {
        if (createdAt == null || createdAt.isBlank()) return null;
        try {
            // ISO: "2024-01-15T02:00:00"
            return LocalDateTime.parse(createdAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException e1) {
            try {
                // format الخادم: "2024-01-15 02:00:00"
                return LocalDateTime.parse(createdAt,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (DateTimeParseException e2) {
                return null;
            }
        }
    }
}