package com.safwat.hr.controller.admin.system;

import com.safwat.hr.shared.AppConfig;
import com.safwat.hr.system.setup.PathResolver;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@Slf4j
public class StartupManager {

    private static final String REG_KEY = "Software\\Microsoft\\Windows\\CurrentVersion\\Run";
    private static final String APP_NAME = "hr-mate-system";
    private static final String LEGACY_APP_NAME = "HR_MATE";
    private static final String LEGACY_BAT_NAME = LEGACY_APP_NAME + ".bat";

    /**
     * إضافة التطبيق للتشغيل التلقائي باستخدام المسار المحفوظ في AppConfig
     * (مع fallback للكشف التلقائي من بنية التوزيع).
     *
     * @return true إذا نجحت العملية
     */
    public static boolean addToStartup() {
        String path = AppConfig.getString("paths", "backend", "");

        // Fallback: كشف تلقائي من بنية التوزيع
        if (path == null || path.isBlank()) {
            path = PathResolver.detect()
                    .map(d -> d.backendExe().toString())
                    .orElse("");
        }

        if (path.isBlank()) {
            log.error("❌ مسار التطبيق غير محفوظ في ملف الإعدادات ولم يتم العثور عليه تلقائياً");
            return false;
        }

        String exePath = resolveExePath(path);
        if (exePath == null) {
            log.error("❌ لم يتم العثور على الملف التنفيذي انطلاقاً من المسار المحفوظ: " + path);
            return false;
        }
        return addToStartup(exePath);
    }

    /**
     * تحديد مسار الملف التنفيذي من القيمة المحفوظة في الإعدادات، وتغطي الحالات دي:
     * <ul>
     *   <li>القيمة هي الملف التنفيذي نفسه (بامتداد أو من غيره على ويندوز)</li>
     *   <li>القيمة هي فولدر الملف التنفيذي (جذر الصورة على ويندوز / bin على لينكس)</li>
     *   <li>القيمة هي جذر الصورة على لينكس (الملف داخل bin)</li>
     *   <li>القيمة هي الفولدر الأب اللي جواه فولدر hr-mate-system</li>
     *   <li>الملف التنفيذي لسه بالاسم القديم HR_MATE</li>
     * </ul>
     *
     * @return مسار الملف التنفيذي، أو null لو مش موجود
     */
    private static String resolveExePath(String path) {
        if (path == null) {
            return null;
        }
        // تنظيف: مسافات وعلامات تنصيص
        path = path.trim().replace("\"", "");
        if (path.isEmpty()) {
            return null;
        }

        boolean win = System.getProperty("os.name").toLowerCase().contains("win");
        File f = new File(path);

        // 1) القيمة هي ملف
        if (f.isFile()) {
            return f.getPath();
        }
        // ويندوز: اتحفظ الاسم من غير .exe
        if (win && !f.exists()) {
            File withExt = new File(path + ".exe");
            if (withExt.isFile()) {
                return withExt.getPath();
            }
        }

        // 2) القيمة هي فولدر: نجرب الأماكن المحتملة بالاسم الجديد ثم القديم
        if (f.isDirectory()) {
            String[] names = win
                    ? new String[]{APP_NAME + ".exe", LEGACY_APP_NAME + ".exe"}
                    : new String[]{APP_NAME, LEGACY_APP_NAME};

            File[] bases = {
                    f,                                              // فولدر الملف التنفيذي نفسه
                    new File(f, "bin"),                             // جذر الصورة على لينكس
                    new File(f, APP_NAME),                          // الفولدر الأب
                    new File(new File(f, APP_NAME), "bin")          // الفولدر الأب على لينكس
            };

            for (File base : bases) {
                for (String name : names) {
                    File candidate = new File(base, name);
                    if (candidate.isFile()) {
                        return candidate.getPath();
                    }
                }
            }
        }

        return null;
    }

    /**
     * إضافة التطبيق للتشغيل التلقائي عند تسجيل الدخول
     *
     * @param appPath المسار الكامل لتطبيق الواجهة (hr-mate-system.exe)
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

            // ✅ 0. حذف ملف .bat القديم (HR_MATE.bat) لو موجود من نسخة سابقة
            new File(appFile.getParent(), LEGACY_BAT_NAME).delete();

            // ✅ 1. إنشاء ملف .bat بجوار التطبيق (اسم الملف التنفيذي مأخوذ من المسار نفسه)
            String exeName = appFile.getName();
            String batPath = appFile.getParent() + File.separator + APP_NAME + ".bat";
            String batContent = createBatContent(exeName);

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
     *
     * @param exeName اسم الملف التنفيذي (hr-mate-system.exe أو hr-mate-system)
     */
    private static String createBatContent(String exeName) {
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
                    echo Starting __APP__...
                    start "" "__EXE__"
                    """.replace("__APP__", APP_NAME).replace("__EXE__", exeName);
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
                    echo "Starting __APP__..."
                    ./__EXE__
                    """.replace("__APP__", APP_NAME).replace("__EXE__", exeName);
        }
    }

    /**
     * حذف التطبيق من التشغيل التلقائي
     *
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
     *
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
     *
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
                    // تنسيق الـ Registry: "    hr-mate-system    REG_SZ    C:\path\to\hr-mate-system.bat"
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