package com.safwat.hr.backup;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BackupFileInfo {
    private String fileName;
    private String fullPath;
    private long sizeBytes;
    private LocalDateTime createdAt;
    private BackupFormat format; // CUSTOM أو PLAIN_SQL

    public String getSizeFormatted() {
        if (sizeBytes < 1024) return sizeBytes + " B";
        if (sizeBytes < 1024 * 1024) return String.format("%.1f KB", sizeBytes / 1024.0);
        if (sizeBytes < 1024 * 1024 * 1024) return String.format("%.1f MB", sizeBytes / (1024.0 * 1024));
        return String.format("%.2f GB", sizeBytes / (1024.0 * 1024 * 1024));
    }
}