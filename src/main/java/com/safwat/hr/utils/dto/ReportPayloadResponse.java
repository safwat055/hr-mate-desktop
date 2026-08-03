package com.safwat.hr.utils.dto;

import java.util.Map;

/**
 * نسخة Frontend من استجابة payload التقرير.
 * يطابق الـ Backend record بالظبط.
 */
public record ReportPayloadResponse(
        String reportName,   // الاسم اللي دخله المستخدم (مخصص)
        String reportCode,   // الكود الثابت — ده اللي نستخدمه للبحث
        String payloadType,
        Map<String, Object> payload
) {
}