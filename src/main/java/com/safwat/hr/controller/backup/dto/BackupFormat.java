package com.safwat.hr.controller.backup.dto;

public enum BackupFormat {
    CUSTOM(".dump"),    // pg_dump -Fc — مضغوط، يحتاج pg_restore
    PLAIN_SQL(".sql");  // pg_dump -Fp — نص عادي، يحتاج psql

    private final String extension;

    BackupFormat(String extension) {
        this.extension = extension;
    }

    public String getExtension() {
        return extension;
    }

    public static BackupFormat fromFileName(String fileName) {
        if (fileName == null) return PLAIN_SQL;
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".dump") || lower.endsWith(".backup")) return CUSTOM;
        return PLAIN_SQL;
    }
}