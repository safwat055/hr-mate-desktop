package com.safwat.hr.controller.admin.system;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * أدوات مشتركة: اكتشاف نظام التشغيل، والمسارات، وصلاحيات التنفيذ.
 */
final class OsSupport {

    static final boolean WINDOWS = System.getProperty("os.name", "")
            .toLowerCase(Locale.ROOT).contains("win");
    static final boolean LINUX = System.getProperty("os.name", "")
            .toLowerCase(Locale.ROOT).contains("linux");

    private OsSupport() {
    }

    /** يضيف .exe على ويندوز بس. */
    static String exe(String base) {
        return WINDOWS ? base + ".exe" : base;
    }

    /** PostgreSQL مابيشتغلش بصلاحيات root على لينكس. */
    static boolean isRootUser() {
        return LINUX && "root".equals(System.getProperty("user.name"));
    }

    static Path appDir() {
        return Paths.get(System.getProperty("user.dir"));
    }

    static Path logsDir() {
        return appDir().resolve("logs");
    }

    /**
     * يدي صلاحية التنفيذ لكل ملفات الفولدر.
     * لو النسخة اتنقلت من ZIP أو فلاشة على لينكس، صلاحيات التنفيذ بتضيع.
     */
    static void makeExecutable(Path dir) throws IOException {
        if (WINDOWS || dir == null || !Files.isDirectory(dir)) return;
        try (Stream<Path> files = Files.walk(dir)) {
            files.filter(Files::isRegularFile)
                    .forEach(p -> p.toFile().setExecutable(true, false));
        }
    }
}