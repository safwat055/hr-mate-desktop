package com.safwat.hr.system.setup;

import java.net.ServerSocket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * فحص جاهزية شامل لكل مكونات النظام.
 * التسميات عربية — تُعرض في Dialog التقرير.
 */
public final class HealthCheckService {

    public record CheckItem(
            String id,
            String labelAr,     // التسمية العربية
            boolean ok,
            String detail,      // القيمة الحالية
            String suggestion   // إجراء مقترح لو فشل
    ) {
    }

    public record Report(List<CheckItem> items, boolean ready) {
        public long failCount() {
            return items.stream().filter(i -> !i.ok()).count();
        }
    }

    public static Report check(PathResolver.Distribution d, int pgPort, int bePort) {
        List<CheckItem> items = new ArrayList<>();

        // 1) PostgreSQL binary
        boolean pgBin = Files.exists(d.pgInitDb()) && Files.exists(d.pgCtl());
        items.add(new CheckItem(
                "pg.bin", "ملفات PostgreSQL التنفيذية",
                pgBin,
                pgBin ? d.pgBin().toString() : "غير موجودة",
                pgBin ? null : "تأكد من وجود فولدر pgsql/bin"));

        // 2) PostgreSQL data init
        boolean pgInit = Files.exists(d.pgConf())
                && Files.exists(d.pgData().resolve("pg_hba.conf"));
        items.add(new CheckItem(
                "pg.data", "تهيئة قاعدة البيانات (data)",
                pgInit,
                pgInit ? "جاهزة" : "غير مهيأة",
                pgInit ? null : "سيتم تهيئتها تلقائياً عند التشغيل السريع"));

        // 3) Backend executable
        boolean be = Files.exists(d.backendExe());
        items.add(new CheckItem(
                "backend.exe", "ملف Backend التنفيذي",
                be,
                be ? d.backendExe().toString() : "غير موجود",
                be ? null : "تأكد من فولدر hr-mate-system"));

        // 4) Backend config
        boolean beConf = Files.exists(d.backendConfig());
        items.add(new CheckItem(
                "backend.conf", "ملف إعدادات الباك إند",
                beConf,
                beConf ? "موجود" : "سيُنشأ تلقائياً عند الحفظ",
                null));

        // 5) Frontend config
        boolean feConf = Files.exists(d.frontendConfig());
        items.add(new CheckItem(
                "frontend.conf", "ملف إعدادات الفرونت",
                feConf,
                feConf ? "موجود" : "سيُنشأ تلقائياً",
                null));

        // 6) المنافذ
        items.add(portItem("pg.port", "منفذ PostgreSQL (" + pgPort + ")", pgPort));
        items.add(portItem("be.port", "منفذ Backend (" + bePort + ")", bePort));

        // 7) صلاحيات الكتابة
        boolean wr = Files.isWritable(d.root);
        items.add(new CheckItem(
                "writable", "صلاحيات الكتابة على مجلد التوزيع",
                wr,
                wr ? "متاحة" : "ممنوعة",
                wr ? null : "شغّل التطبيق كمسؤول أو انقل التوزيع لمكان آخر"));

        boolean ready = items.stream().allMatch(CheckItem::ok);
        return new Report(items, ready);
    }

    private static CheckItem portItem(String id, String label, int port) {
        boolean free = isPortFree(port);
        return new CheckItem(id, label, free,
                free ? "متاح" : "محجوز",
                free ? null : "أغلق العملية التي تستخدم المنفذ " + port);
    }

    public static boolean isPortFree(int port) {
        try (ServerSocket s = new ServerSocket(port)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}