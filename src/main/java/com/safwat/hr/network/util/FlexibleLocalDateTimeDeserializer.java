package com.safwat.hr.network.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Deserializer مرن لـ LocalDateTime يقبل عدة صيغ — يحل مشكلة اختلاف
 * الصيغة بين الخدمات (بعضها ISO وبعضها yyyy-MM-dd HH:mm:ss).
 *
 * <p>الصيغ المدعومة:
 * <ul>
 *   <li>{@code 2026-09-12T23:11:15.296397} — ISO-8601 مع كسور ثانية</li>
 *   <li>{@code 2026-09-12T23:11:15} — ISO-8601 بدون كسور</li>
 *   <li>{@code 2026-09-12 23:11:15.296397} — بمسافة + كسور</li>
 *   <li>{@code 2026-09-12 23:11:15.296} — بمسافة + 3 كسور</li>
 *   <li>{@code 2026-09-12 23:11:15} — بمسافة بدون كسور (الأكثر استخدامًا)</li>
 *   <li>{@code 2026-09-12 23:11} — بدون ثواني</li>
 *   <li>{@code 2026-09-12} — تاريخ فقط</li>
 * </ul>
 */
public class FlexibleLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    private static final List<DateTimeFormatter> FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd")
    );

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
        if (p == null) return null;
        String text = p.getText();
        if (text == null || text.isBlank()) return null;

        String trimmed = text.trim();

        for (DateTimeFormatter fmt : FORMATTERS) {
            try {
                return LocalDateTime.parse(trimmed, fmt);
            } catch (DateTimeParseException ignored) {
                // جرّب الصيغة اللي بعدها
            }
        }

        throw new IOException("Cannot parse LocalDateTime: '" + text
                + "'. Supported: ISO-8601 or 'yyyy-MM-dd HH:mm:ss[.SSS]'");
    }
}