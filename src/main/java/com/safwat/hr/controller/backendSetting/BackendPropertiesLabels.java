package com.safwat.hr.controller.backendSetting;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * خريطة المفاتيح الإنجليزية → العربية لملف application.properties.
 * أي مفتاح غير موجود هنا يُعرض بالإنجليزية كما هو.
 */
public final class BackendPropertiesLabels {

    private static final Map<String, String> LABELS = new LinkedHashMap<>();
    private static final Map<String, String> GROUPS = new LinkedHashMap<>();

    static {
        // ── الخادم ──
        LABELS.put("server.port", "منفذ الخادم");
        LABELS.put("server.address", "عنوان الاستماع");
        LABELS.put("server.servlet.context-path", "المسار الأساسي للتطبيق");
        LABELS.put("spring.application.name", "اسم التطبيق");

        // ── قاعدة البيانات ──
        LABELS.put("spring.datasource.url", "رابط قاعدة البيانات");
        LABELS.put("spring.datasource.username", "اسم مستخدم القاعدة");
        LABELS.put("spring.datasource.password", "كلمة مرور القاعدة");
        LABELS.put("spring.datasource.driver-class-name", "مشغل قاعدة البيانات");

        // ── JPA / Hibernate ──
        LABELS.put("spring.jpa.hibernate.ddl-auto", "طريقة تحديث الجداول");
        LABELS.put("spring.jpa.show-sql", "إظهار استعلامات SQL");
        LABELS.put("spring.jpa.properties.hibernate.format_sql", "تنسيق SQL");
        LABELS.put("spring.jpa.properties.hibernate.dialect", "لهجة قاعدة البيانات");

        // ── السجلات ──
        LABELS.put("logging.level.root", "مستوى السجلات العام");
        LABELS.put("logging.level.com.safwat", "مستوى سجلات التطبيق");
        LABELS.put("logging.file.name", "ملف السجلات");

        // ── الأمان ──
        LABELS.put("jwt.secret", "المفتاح السري لـ JWT");
        LABELS.put("jwt.expiration", "مدة صلاحية الرمز (ms)");
        LABELS.put("app.security.api-key", "مفتاح API");

        // ── الملفات ──
        LABELS.put("spring.servlet.multipart.max-file-size", "أقصى حجم ملف");
        LABELS.put("spring.servlet.multipart.max-request-size", "أقصى حجم طلب");
        LABELS.put("app.storage.root", "جذر التخزين");

        // ── المجموعات (Sidebar) ──
        GROUPS.put("server.", "🌐 الخادم");
        GROUPS.put("spring.datasource.", "🗄️ قاعدة البيانات");
        GROUPS.put("spring.jpa.", "🔗 JPA / Hibernate");
        GROUPS.put("logging.", "📋 السجلات");
        GROUPS.put("jwt.", "🔐 الأمان");
        GROUPS.put("app.security.", "🔐 الأمان");
        GROUPS.put("spring.servlet.", "📁 الملفات");
        GROUPS.put("app.storage.", "📁 التخزين");
    }

    private BackendPropertiesLabels() {
    }

    public static String label(String englishKey) {
        return LABELS.getOrDefault(englishKey, englishKey);
    }

    public static String hasLabel(String englishKey) {
        return LABELS.containsKey(englishKey) ? label(englishKey) : null;
    }

    public static String groupOf(String key) {
        for (var e : GROUPS.entrySet()) {
            if (key.startsWith(e.getKey())) return e.getValue();
        }
        return "⚙️ عام";
    }
}