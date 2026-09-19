package com.safwat.hr.controller.backup.dto;

/**
 * وضع تصدير الجدول.
 */
public enum ExportMode {
    /**
     * TRUNCATE ثم إدخال البيانات — استبدال كامل.
     */
    REPLACE,

    /**
     * إدخال مع ON CONFLICT UPDATE — دمج.
     */
    MERGE
}