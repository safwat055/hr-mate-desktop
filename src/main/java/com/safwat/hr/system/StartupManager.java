package com.safwat.hr.system;

import lombok.extern.slf4j.Slf4j;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@Slf4j
public class StartupManager {

    private static final String REG_KEY = "Software\\Microsoft\\Windows\\CurrentVersion\\Run";
    private static final String APP_NAME = "ArchiveManager";

    /**
     * إضافة التطبيق للتشغيل التلقائي عند تسجيل الدخول
     * @param appPath المسار الكامل لتطبيق الواجهة (Archive_Rest.exe)
     * @return true إذا نجحت العملية
     */
    public static boolean addToStartup(String appPath) {
        try {
            if (appPath == null || appPath.trim().isEmpty()) {
                log.error("❌ مسار التطبيق فارغ");
                return false;
            }

            File appFile = new File(appPath);
            if (!appFile.exists()) {
                log.error("❌ التطبيق غير موجود: " + appPath);
                return false;
            }

            // ✅ 1. إنشاء ملف .bat بجوار التطبيق
            String batPath = appFile.getParent() + File.separator + "Archive_Rest.bat";
            String batContent = createBatContent(appPath);

            try (FileWriter fw = new FileWriter(batPath)) {
                fw.write(batContent);
            }

            // ✅ 2. جعل الـ .bat قابل للتنفيذ (على Linux/Mac)
            if (!System.getProperty("os.name").toLowerCase().contains("win")) {
                File batFile = new File(batPath);
                batFile.setExecutable(true);
            }

            // ✅ 3. إضافة الـ .bat إلى الـ Registry بدلاً من الـ .exe
            String command = "\"" + batPath + "\"";
            ProcessBuilder pb = new ProcessBuilder(
                    "reg", "add",
                    "HKCU\\" + REG_KEY,
                    "/v", APP_NAME,
                    "/t", "REG_SZ",
                    "/d", command,
                    "/f"
            );

            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                log.info("✅ تم إضافة التطبيق للتشغيل التلقائي: " + batPath);
                return true;
            } else {
                log.error("❌ فشل إضافة التطبيق، رمز الخطأ: " + exitCode);
                return false;
            }

        } catch (Exception e) {
            log.error("❌ خطأ أثناء إضافة التطبيق للتشغيل التلقائي", e);
            return false;
        }
    }

    /**
     * إنشاء محتوى ملف .bat مع التحقق من PostgreSQL
     */
    private static String createBatContent(String appPath) {
        String os = System.getProperty("os.name").toLowerCase();
        boolean isWindows = os.contains("win");

        if (isWindows) {
            return """
                @echo off
                echo Checking for PostgreSQL service...
                
                :: انتظار خدمة PostgreSQL حتى تعمل
                set SERVICE_NAME=PostgreSQL
                set TIMEOUT=60
                set ELAPSED=0
                
                :CHECK_SERVICE
                sc query "%SERVICE_NAME%" | find "RUNNING" > nul
                if %errorlevel% equ 0 (
                    echo PostgreSQL is running.
                    goto START_APP
                )
                
                echo Waiting for PostgreSQL to start... (%ELAPSED%/%TIMEOUT% seconds)
                timeout /t 2 /nobreak > nul
                set /a ELAPSED=%ELAPSED%+2
                
                if %ELAPSED% geq %TIMEOUT% (
                    echo Timeout waiting for PostgreSQL. Starting application anyway...
                    goto START_APP
                )
                
                goto CHECK_SERVICE
                
                :START_APP
                cd /d "%~dp0"
                echo Starting ArchiveManager...
                start "" "Archive_Rest.exe"
                """;
        } else {
            // ✅ Linux/Mac
            return """
                #!/bin/bash
                echo "Checking for PostgreSQL service..."
                
                SERVICE_NAME="postgresql"
                TIMEOUT=60
                ELAPSED=0
                
                while [ $ELAPSED -lt $TIMEOUT ]; do
                    if systemctl is-active --quiet $SERVICE_NAME; then
                        echo "PostgreSQL is running."
                        break
                    fi
                    echo "Waiting for PostgreSQL to start... ($ELAPSED/$TIMEOUT seconds)"
                    sleep 2
                    ELAPSED=$((ELAPSED + 2))
                done
                
                if [ $ELAPSED -ge $TIMEOUT ]; then
                    echo "Timeout waiting for PostgreSQL. Starting application anyway..."
                fi
                
                cd "$(dirname "$0")"
                echo "Starting ArchiveManager..."
                ./Archive_Rest
                """;
        }
    }

    /**
     * حذف التطبيق من التشغيل التلقائي
     * @return true إذا نجحت العملية
     */
    public static boolean removeFromStartup() {
        try {
            String cmd = String.format(
                    "reg delete HKCU\\%s /v %s /f",
                    REG_KEY, APP_NAME
            );

            Process process = Runtime.getRuntime().exec(cmd);
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                log.info("✅ تم حذف التطبيق من التشغيل التلقائي");
                return true;
            } else {
                log.error("❌ فشل حذف التطبيق، رمز الخطأ: " + exitCode);
                return false;
            }

        } catch (IOException | InterruptedException e) {
            log.error("❌ خطأ أثناء حذف التطبيق من التشغيل التلقائي", e);
            return false;
        }
    }

    /**
     * التحقق من وجود التطبيق في التشغيل التلقائي
     * @return true إذا كان موجوداً
     */
    public static boolean isInStartup() {
        try {
            String cmd = String.format(
                    "reg query HKCU\\%s /v %s",
                    REG_KEY, APP_NAME
            );

            Process process = Runtime.getRuntime().exec(cmd);
            int exitCode = process.waitFor();

            return exitCode == 0;

        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    /**
     * الحصول على مسار التطبيق المسجل في التشغيل التلقائي
     * @return المسار أو null إذا غير موجود
     */
    public static String getStartupPath() {
        try {
            String cmd = String.format(
                    "reg query HKCU\\%s /v %s",
                    REG_KEY, APP_NAME
            );

            Process process = Runtime.getRuntime().exec(cmd);
            try (java.util.Scanner scanner = new java.util.Scanner(process.getInputStream())) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    // تنسيق الـ Registry: "    ArchiveManager    REG_SZ    C:\path\to\app.exe"
                    if (line.contains("REG_SZ")) {
                        String[] parts = line.split("REG_SZ");
                        if (parts.length > 1) {
                            return parts[1].trim().replace("\"", "");
                        }
                    }
                }
            }

            return null;

        } catch (IOException e) {
            return null;
        }
    }
}