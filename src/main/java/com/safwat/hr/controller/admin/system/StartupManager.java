package com.safwat.hr.controller.admin.system;

import com.safwat.hr.shared.AppConfig;
import com.safwat.hr.system.setup.PathResolver;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * التشغيل التلقائي عند تسجيل الدخول.
 * <ul>
 *   <li><b>ويندوز:</b> مفتاح Run في الـ Registry (HKCU) → bat بينتظر PostgreSQL ويشغّل الـ launcher.</li>
 *   <li><b>لينكس:</b> ملف .desktop في ~/.config/autostart → سكريبت بينتظر بورت PostgreSQL ويشغّل الـ launcher.</li>
 * </ul>
 * المسار المدخل ممكن يكون جذر التوزيع أو الـ launcher أو java أو jar،
 * والتحويل للـ launcher الفعلي بيتم بـ {@link BackendService#resolveLauncher(String)}.
 */
@Slf4j
public class StartupManager {

    private static final String APP_NAME = "hr-mate-system";
    private static final String LEGACY_APP_NAME = "HR_MATE";
    private static final String REG_KEY = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run";
    private static final String DESKTOP_FILE = APP_NAME + ".desktop";
    private static final String WRAPPER_FILE = "autostart.sh";
    private static final Pattern REG_SZ = Pattern.compile("REG_SZ\\s+(.+)$", Pattern.MULTILINE);

    // ══════════════════════════════════════════════════════════════
    //  Public API
    // ══════════════════════════════════════════════════════════════

    /** يضيف التطبيق للتشغيل التلقائي باستخدام المسار المحفوظ (مع fallback للكشف التلقائي). */
    public static boolean addToStartup() {
        String path = AppConfig.getString("paths", "backend", "");
        if (path == null || path.isBlank()) {
            path = PathResolver.detect().map(d -> d.backendExe().toString()).orElse("");
        }
        if (path.isBlank()) {
            log.error("❌ مسار التطبيق غير محفوظ ولم يتم العثور عليه تلقائياً");
            return false;
        }
        return addToStartup(path);
    }

    public static boolean addToStartup(String appPath) {
        try {
            if (appPath == null || appPath.isBlank()) {
                log.error("❌ مسار التطبيق فارغ");
                return false;
            }
            Path launcher = BackendService.resolveLauncher(appPath);
            if (launcher == null || !Files.isRegularFile(launcher)) {
                log.error("❌ لم يتم العثور على الـ launcher انطلاقاً من: " + appPath);
                return false;
            }
            return OsSupport.WINDOWS ? addWindows(launcher) : addLinux(launcher);
        } catch (Exception e) {
            log.error("❌ خطأ أثناء إضافة التطبيق للتشغيل التلقائي", e);
            return false;
        }
    }

    public static boolean removeFromStartup() {
        try {
            if (OsSupport.WINDOWS) {
                Path bat = windowsBatFromRegistry();
                ProcessRunner.Result r = ProcessRunner.run(30,
                        List.of("reg", "delete", REG_KEY, "/v", APP_NAME, "/f"));
                if (bat != null && bat.getFileName().toString().equals(APP_NAME + ".bat")) {
                    Files.deleteIfExists(bat);
                }
                boolean ok = r.ok() || !isInStartup();
                log.info(ok ? "✅ تم حذف التطبيق من التشغيل التلقائي" : "❌ فشل حذف التشغيل التلقائي");
                return ok;
            }
            Files.deleteIfExists(desktopPath());
            Files.deleteIfExists(wrapperPath());
            boolean ok = !isInStartup();
            log.info(ok ? "✅ تم حذف التطبيق من التشغيل التلقائي" : "❌ فشل حذف التشغيل التلقائي");
            return ok;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (IOException e) {
            log.error("❌ خطأ أثناء حذف التشغيل التلقائي", e);
            return false;
        }
    }

    public static boolean isInStartup() {
        try {
            if (OsSupport.WINDOWS) {
                return ProcessRunner.run(15, List.of("reg", "query", REG_KEY, "/v", APP_NAME)).ok();
            }
            return Files.isRegularFile(desktopPath());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    /** المسار المسجّل للتشغيل التلقائي، أو null. */
    public static String getStartupPath() {
        try {
            if (OsSupport.WINDOWS) {
                ProcessRunner.Result r = ProcessRunner.run(15,
                        List.of("reg", "query", REG_KEY, "/v", APP_NAME));
                if (!r.ok()) return null;
                Matcher m = REG_SZ.matcher(r.output());
                return m.find() ? m.group(1).trim().replace("\"", "") : null;
            }
            Path d = desktopPath();
            if (!Files.isRegularFile(d)) return null;
            for (String line : Files.readAllLines(d, StandardCharsets.UTF_8)) {
                if (line.startsWith("Exec=")) {
                    return line.substring(5).trim().replace("\"", "");
                }
            }
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  Windows
    // ══════════════════════════════════════════════════════════════

    private static boolean addWindows(Path launcher) throws IOException, InterruptedException {
        Files.deleteIfExists(launcher.getParent().resolve(LEGACY_APP_NAME + ".bat"));

        Path bat = launcher.getParent().resolve(APP_NAME + ".bat");
        Files.writeString(bat, windowsBat(launcher.getFileName().toString()), StandardCharsets.US_ASCII);

        ProcessRunner.Result r = ProcessRunner.run(30, List.of(
                "reg", "add", REG_KEY, "/v", APP_NAME, "/t", "REG_SZ",
                "/d", "\"" + bat + "\"", "/f"));
        if (r.ok()) {
            log.info("✅ تم إضافة التطبيق للتشغيل التلقائي: " + bat);
            return true;
        }
        log.error("❌ فشل reg add: " + r.output());
        return false;
    }

    /** bat بينتظر PostgreSQL (بالخدمة أو بالبورت) ثم يشغّل الـ launcher. CRLF عشان cmd يتعامل معاه صح. */
    private static String windowsBat(String exeName) {
        String bat = """
                @echo off
                setlocal
                set PG_SERVICE=__PG_SERVICE__
                set PG_PORT=__PG_PORT__
                set TIMEOUT=60
                set ELAPSED=0
                :CHECK_PG
                sc query "%PG_SERVICE%" | find ": 4" > nul
                if not errorlevel 1 goto START_APP
                netstat -an | find ":%PG_PORT% " | find "LISTENING" > nul
                if not errorlevel 1 goto START_APP
                if %ELAPSED% geq %TIMEOUT% goto START_APP
                timeout /t 2 /nobreak > nul
                set /a ELAPSED=%ELAPSED%+2  
                goto CHECK_PG
                :START_APP
                cd /d "%~dp0"
                start "" "__EXE__"
                """;
        return bat.replace("__PG_SERVICE__", pgService())
                .replace("__PG_PORT__", pgPort())
                .replace("__EXE__", exeName)
                .replace("\n", "\r\n");
    }

    private static Path windowsBatFromRegistry() {
        try {
            ProcessRunner.Result r = ProcessRunner.run(15,
                    List.of("reg", "query", REG_KEY, "/v", APP_NAME));
            if (!r.ok()) return null;
            Matcher m = REG_SZ.matcher(r.output());
            return m.find() ? Paths.get(m.group(1).trim().replace("\"", "")) : null;
        } catch (Exception e) {
            return null;
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  Linux
    // ══════════════════════════════════════════════════════════════

    private static boolean addLinux(Path launcher) throws IOException {
        launcher.toFile().setExecutable(true, false);

        Path dataDir = xdgHome("XDG_DATA_HOME", ".local/share").resolve(APP_NAME);
        Files.createDirectories(dataDir);
        Path wrapper = dataDir.resolve(WRAPPER_FILE);
        Files.writeString(wrapper, linuxWrapper(launcher), StandardCharsets.UTF_8);
        wrapper.toFile().setExecutable(true, false);

        Path autostartDir = xdgHome("XDG_CONFIG_HOME", ".config").resolve("autostart");
        Files.createDirectories(autostartDir);
        String desktop = """
                [Desktop Entry]
                Type=Application
                Name=HR MATE System
                Comment=Start HR MATE after login
                Exec="__EXEC__"
                Terminal=false
                X-GNOME-Autostart-enabled=true
                """.replace("__EXEC__", escapeDesktop(wrapper.toString()));
        Files.writeString(autostartDir.resolve(DESKTOP_FILE), desktop, StandardCharsets.UTF_8);

        log.info("✅ تم إضافة التطبيق للتشغيل التلقائي: " + autostartDir.resolve(DESKTOP_FILE));
        return true;
    }

    /** سكريبت بينتظر بورت PostgreSQL (بدون pg_isready) ثم يشغّل الـ launcher. */
    private static String linuxWrapper(Path launcher) {
        Path home = PathResolver.homeOfLauncher(launcher);
        Path launcherDir = launcher.getParent();           // bin/
        Path storage = home.resolve("app");
        return "#!/bin/bash\n"
                + "# Generated by hr-mate-system: waits for PostgreSQL then starts the app.\n"
                + "PORT=" + pgPort() + "\n"
                + "for i in $(seq 1 60); do\n"
                + "  if (exec 3<>/dev/tcp/127.0.0.1/$PORT) 2>/dev/null; then break; fi\n"
                + "  sleep 2\n"
                + "done\n"
                + "export APP_STORAGE_ROOT=" + shQuote(storage.toString()) + "\n"
                + "cd " + shQuote(launcherDir.toString()) + " || exit 1\n"   // ← bin/
                + "exec " + shQuote(launcher.toString()) + "\n";
    }

    private static Path desktopPath() {
        return xdgHome("XDG_CONFIG_HOME", ".config").resolve("autostart").resolve(DESKTOP_FILE);
    }

    private static Path wrapperPath() {
        return xdgHome("XDG_DATA_HOME", ".local/share").resolve(APP_NAME).resolve(WRAPPER_FILE);
    }

    private static Path xdgHome(String envVar, String fallback) {
        String v = System.getenv(envVar);
        return (v != null && !v.isBlank())
                ? Paths.get(v)
                : Paths.get(System.getProperty("user.home"), fallback);
    }

    private static String shQuote(String s) {
        return "'" + s.replace("'", "'\\''") + "'";
    }

    private static String escapeDesktop(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("`", "\\`")
                .replace("$", "\\$");
    }

    // ══════════════════════════════════════════════════════════════
    //  Shared
    // ══════════════════════════════════════════════════════════════

    private static String pgService() {
        String v = AppConfig.getString("connection", "pgServiceName", "PostgreSQL");
        return (v == null || v.isBlank()) ? "PostgreSQL" : v;
    }

    private static String pgPort() {
        String v = AppConfig.getString("connection", "pgPort", "5432");
        return (v != null && v.trim().matches("\\d{1,5}")) ? v.trim() : "5432";
    }
}