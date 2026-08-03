package com.safwat.hr.utils;

import lombok.Getter;
import lombok.Setter;

/**
 * يمثل حالة مهمة غير متزامنة (مثل: تحميل تقرير أو معالجة دفعة).
 * <p>
 * كان كلاس داخلي في {@code ApiClient} — تم فصله لـ DTO مستقل.
 */
@Setter
@Getter
public class TaskStatus {
    private String taskId;
    private String status;     // IN_PROGRESS | COMPLETED | FAILED
    private int progress;      // 0-100
    private String message;
    private String downloadUrl;
}