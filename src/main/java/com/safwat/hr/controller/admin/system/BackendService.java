package com.safwat.hr.controller.admin.system;

import com.safwat.hr.shared.AppConfig;
import com.safwat.hr.system.setup.PathResolver;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * تشغيل وإيقاف الباك إند (jpackage app-image launcher) على ويندوز ولينكس.
 *
 * <ul>
 *   <li><b>ويندوز:</b> تشغيل مباشر للـ exe من فولدره.</li>
 *   <li><b>لينكس:</b> نولّد {@code run.sh} في جذر الباكيج يحاكي التشغيل اليدوي
 *       ({@code cd bin && exec ./hr-mate-system}) + ينظف env + يعمل symlink
 *       {@code bin/app → ../app}. الـ Java بتنفذ السكريبت بس.</li>
 *   <li>الـ PID بيتحفظ في logs/backend.pid.</li>
 *   <li>الإيقاف: بالـ PID + كل العمليات اللي شغالة من نفس الـ launcher، مع أولادها.</li>
 *   <li>خدمة ويندوز: NSSM + sc (ويندوز بس).</li>
 * </ul>
 */
@Slf4j
public class BackendService {

    static final String SERVICE_NAME = "HR_MATE_Service";

    private static final String DISPLAY_NAME = "HR_MATE_Service";
    private static final String NSSM_EXE = "nssm.exe";
    private static final String PID_FILE = "backend.pid";
    private static final Pattern SERVICE_LINE = Pattern.compile("SERVICE_NAME\\s*:\\s*(\\S+)");
    private static final Pattern SERVICE_NAME_RE = Pattern.compile("[A-Za-z0-9_.\\-]{1,128}");

    /** أسماء الـ launcher المعروفة (بدون .exe). */
    private static final List<String> LAUNCHER_NAMES = List.of("hr-mate-system", "HR_MATE", "hr-mate");
    /** فولدرات التوزيع الداخلية — بنتخطاها وإحنا بندور على الجذر. */
    private static final List<String> LAYOUT_DIRS = List.of("bin", "lib", "runtime", "app");

    /**
     * أقصى مدة نستنى فيها الـ launcher قبل ما نحكم إنه بدأ بنجاح.
     * على لينكس الـ jpackage launcher بياخد وقت لرفع الـ JVM.
     */
    private static final int STARTUP_GRACE_SECONDS = 10;

    private static final String WINDOWS_SERVICE_MSG =
            "ℹ️ خدمة الباك إند مدعومة على ويندوز فقط — على لينكس استخدم الوضع العادي";

    private static volatile BackendService instance;

    private volatile Path lastExe;

    private BackendService() {
    }

    public static BackendService getInstance() {
        if (instance == null) {
            synchronized (BackendService.class) {
                if (instance == null) instance = new BackendService();
            }
        }
        return instance;
    }

    // ══════════════════════════════════════════════════════════════
    //  Launcher resolution (jpackage layout)
    // ══════════════════════════════════════════════════════════════

    /**
     * يحوّل أي مسار (جذر التوزيع، الـ launcher، java، أو jar) لمسار الـ launcher الفعلي.
     * يرجع null لو ملقاش حاجة صالحة.
     */
    static Path resolveLauncher(String raw) {
        if (raw == null || raw.isBlank()) return null;
        Path p = Paths.get(raw.trim().replace("\"", "")).toAbsolutePath().normalize();

        // 1) ملف launcher مباشرة بالاسم المعروف
        if (Files.isRegularFile(p) && !isJavaOrJar(p) && isLauncherName(p)) return p;

        // 2) مسار جوه التوزيع: نطلع للجذر ونلاقي الـ launcher
        Path root = appRootOf(p);
        if (root != null) {
            Path launcher = findLauncher(root);
            if (launcher != null) return launcher;
        }

        // 3) ملف تنفيذي مختار يدوياً بإسم مختلف
        if (Files.isRegularFile(p) && !isJavaOrJar(p)) return p;

        // 4) ويندوز: المسار من غير .exe
        if (OsSupport.WINDOWS && !Files.exists(p)) {
            Path withExe = Paths.get(p + ".exe");
            if (Files.isRegularFile(withExe)) return withExe;
        }
        return null;
    }

    /** يرجع الـ launcher لو موجود، وإلا المسار الخام (عشان رسالة الخطأ تبقى واضحة). */
    private static Path resolveExe(String path) {
        Path launcher = resolveLauncher(path);
        return launcher != null
                ? launcher
                : Paths.get(path.trim().replace("\"", "")).toAbsolutePath().normalize();
    }

    private static boolean isJavaOrJar(Path p) {
        String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
        return n.equals("java") || n.equals("java.exe") || n.equals("javaw.exe") || n.endsWith(".jar");
    }

    private static boolean isLauncherName(Path p) {
        String n = p.getFileName().toString();
        if (n.toLowerCase(Locale.ROOT).endsWith(".exe")) n = n.substring(0, n.length() - 4);
        return LAUNCHER_NAMES.contains(n);
    }

    /** يطلع من المسار لحد ما يلاقي جذر فيه launcher، ويتخطى فولدرات التوزيع الداخلية. */
    private static Path appRootOf(Path p) {
        Path cur = Files.isDirectory(p) ? p : p.getParent();
        for (int i = 0; cur != null && i < 6; i++, cur = cur.getParent()) {
            Path name = cur.getFileName();
            if (name != null && LAYOUT_DIRS.contains(name.toString().toLowerCase(Locale.ROOT))) continue;
            if (findLauncher(cur) != null) return cur;
        }
        return null;
    }

    /** يدور على الـ launcher في الجذر حسب النظام. */
    private static Path findLauncher(Path root) {
        for (String n : LAUNCHER_NAMES) {
            List<Path> candidates = OsSupport.WINDOWS
                    ? List.of(root.resolve(n + ".exe"))
                    : List.of(root.resolve("bin").resolve(n), root.resolve(n));
            for (Path c : candidates) {
                if (Files.isRegularFile(c)) return c;
            }
        }
        return null;
    }

    // ══════════════════════════════════════════════════════════════
    //  Paths & PID
    // ══════════════════════════════════════════════════════════════

    /** آخر launcher اتشغّل، ولو مفيش نرجع للمسار المحفوظ في الإعدادات. */
    private Path currentExe() {
        if (lastExe != null) return lastExe;
        String cfg = AppConfig.getString("paths", "backend", "");
        return (cfg == null || cfg.isBlank()) ? null : resolveExe(cfg);
    }

    private static void writePid(long pid) throws IOException {
        Path f = OsSupport.logsDir().resolve(PID_FILE);
        Files.createDirectories(f.getParent());
        Files.writeString(f, String.valueOf(pid), StandardCharsets.UTF_8);
    }

    private static Optional<Long> readPid() {
        Path f = OsSupport.logsDir().resolve(PID_FILE);
        try {
            if (!Files.isRegularFile(f)) return Optional.empty();
            return Optional.of(Long.parseLong(Files.readString(f, StandardCharsets.UTF_8).trim()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static void clearPid() {
        try {
            Files.deleteIfExists(OsSupport.logsDir().resolve(PID_FILE));
        } catch (IOException ignored) {
        }
    }

    private static void say(String msg) {
        log.info(msg);
    }

    /** آخر n سطر من ملف (للتشخيص). */
    private static String tailOf(Path file, int n) {
        try {
            if (file == null || !Files.isRegularFile(file)) return "(لا يوجد سجل)";
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            int from = Math.max(0, lines.size() - n);
            return lines.subList(from, lines.size()).stream()
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            return "(تعذر قراءة السجل: " + e.getMessage() + ")";
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  Linux: run.sh generation
    // ══════════════════════════════════════════════════════════════

    /**
     * يكتب {@code run.sh} في جذر الباكيج (بجانب bin/) ويخليه قابل للتنفيذ.
     *
     * <p>السكريبت بيحاكي التشغيل اليدوي 100%:
     * <ol>
     *   <li>ينظف متغيرات jpackage اللي بتتسرب من الأدمن كونسول
     *       (JAVA_TOOL_OPTIONS, LD_LIBRARY_PATH, ...).</li>
     *   <li>يعمل symlink {@code bin/app → ../app} عشان المسارات النسبية في cfg
     *       (spring.config.location=file:app/config/) تقع على جذر الباكيج
     *       بدل ما تروح جوه bin/.</li>
     *   <li>{@code cd bin && exec ./hr-mate-system} — نفس اللي بتعمله يدوي.</li>
     * </ol>
     * idempotent: بيتكتب كل مرة (خفيف) والـ chmod بيتطبق.
     */
    private static Path ensureLinuxRunScript(Path launcher) throws IOException {
        Path home = PathResolver.homeOfLauncher(launcher);
        Path script = home.resolve("run.sh");

        String content = """
                #!/bin/bash
                # Auto-generated by HR MATE Admin Console.
                # Simulates: cd bin && ./hr-mate-system
                set -e

                PKG_ROOT="$(cd "$(dirname "$(readlink -f "$0")")" && pwd)"
                LAUNCHER_DIR="$PKG_ROOT/bin"

                # 🧹 نظّف متغيرات jpackage اللي بتتسرب من الأدمن كونسول
                unset JAVA_TOOL_OPTIONS _JAVA_OPTIONS JDK_JAVA_OPTIONS
                unset JAVA_HOME JRE_HOME CLASSPATH
                unset LD_PRELOAD LD_LIBRARY_PATH APP_STORAGE_ROOT

                # 🔗 symlink: bin/app → ../app
                # CWD=bin/ (الـ launcher سعيد) + المسارات النسبية تقع على الجذر
                if [ ! -e "$LAUNCHER_DIR/app" ] && [ ! -L "$LAUNCHER_DIR/app" ]; then
                    mkdir -p "$PKG_ROOT/app"
                    ln -s ../app "$LAUNCHER_DIR/app"
                fi

                export APP_STORAGE_ROOT="$PKG_ROOT/app"

                # سجل تشخيصي بسيط
                mkdir -p "$PKG_ROOT/logs"
                {
                    echo "=== $(date) ==="
                    echo "PKG_ROOT=$PKG_ROOT"
                    echo "CWD_AFTER_CD=$LAUNCHER_DIR"
                    env | grep -iE 'java|jre|ld_|class' || true
                } >> "$PKG_ROOT/logs/backend-script.log"

                cd "$LAUNCHER_DIR"
                exec ./hr-mate-system "$@"
                """;

        Files.writeString(script, content, StandardCharsets.UTF_8);
        script.toFile().setExecutable(true, false);
        return script;
    }

    // ══════════════════════════════════════════════════════════════
    //  NSSM (ويندوز بس)
    // ══════════════════════════════════════════════════════════════

    private Path findNssm() {
        if (!OsSupport.WINDOWS) return null;
        Path appDir = OsSupport.appDir();
        List<Path> candidates = List.of(
                appDir.resolve(NSSM_EXE),
                appDir.resolve("services").resolve("windows").resolve(NSSM_EXE),
                appDir.resolve("..").resolve(NSSM_EXE),
                appDir.resolve("lib").resolve(NSSM_EXE),
                Paths.get("C:\\nssm\\nssm.exe"),
                Paths.get("C:\\nssm-2.24\\win64\\nssm.exe"),
                Paths.get("C:\\Program Files\\nssm\\nssm.exe"));
        for (Path c : candidates) {
            Path n = c.toAbsolutePath().normalize();
            if (Files.isRegularFile(n)) return n;
        }
        return null;
    }

    public boolean isNssmAvailable() {
        return findNssm() != null;
    }

    private boolean nssm(Path nssm, String... args) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>();
        cmd.add(nssm.toString());
        cmd.addAll(List.of(args));
        ProcessRunner.Result r = ProcessRunner.run(60, cmd);
        if (!r.ok()) say("❌ nssm " + String.join(" ", args) + " فشل: " + r.output());
        return r.ok();
    }

    // ══════════════════════════════════════════════════════════════
    //  Start / Stop
    // ══════════════════════════════════════════════════════════════

    public synchronized boolean start(String backendPath, boolean asService) {
        if (backendPath == null || backendPath.isBlank()) {
            say("❌ مسار الباك إند فاضي");
            return false;
        }
        if (isRunning()) {
            say("ℹ️ الباك إند شغال بالفعل");
            return true;
        }
        return asService ? startService() : startNormal(backendPath);
    }

    /**
     * تشغيل الباك إند:
     * <ul>
     *   <li>ويندوز: تنفيذ مباشر للـ exe من فولدره.</li>
     *   <li>لينكس: توليد {@code run.sh} وتنفيذه — السكريبت بيحاكي التشغيل اليدوي.</li>
     * </ul>
     */
    public synchronized boolean startNormal(String backendPath) {
        try {
            if (backendPath == null || backendPath.isBlank()) return false;

            Path exe = resolveExe(backendPath);
            if (!Files.isRegularFile(exe)) {
                say("❌ الـ launcher غير موجود: " + exe);
                return false;
            }
            if (isRunning(exe)) {
                lastExe = exe;
                say("ℹ️ الباك إند شغال بالفعل");
                return true;
            }

            Path logFile = OsSupport.logsDir().resolve("backend.log");
            Files.createDirectories(logFile.getParent());

            ProcessBuilder pb;

            if (OsSupport.WINDOWS) {
                // ويندوز: تشغيل مباشر من فولدر الـ exe
                Path workDir = exe.getParent();
                exe.toFile().setExecutable(true, false);
                say("🔄 تشغيل: " + exe + " | CWD: " + workDir);
                pb = new ProcessBuilder(exe.toString())
                        .directory(workDir.toFile());
            } else {
                // لينكس: نولّد run.sh في جذر الباكيج ونشغّله
                Path script = ensureLinuxRunScript(exe);
                say("🔄 تشغيل عبر run.sh: " + script);
                pb = new ProcessBuilder("/bin/bash", script.toString());
                // مش بنحدد directory — السكريبت بيعمل cd بنفسه
            }

            pb.redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.appendTo(logFile.toFile()));

            Process p = pb.start();

            if (p.waitFor(STARTUP_GRACE_SECONDS, TimeUnit.SECONDS)) {
                say("❌ الباك إند خرج مباشرة (رمز " + p.exitValue() + ") — آخر السجل:\n"
                        + tailOf(logFile, 25));
                return false;
            }

            lastExe = exe;
            writePid(p.pid());
            say("✅ تم تشغيل الباك إند (PID " + p.pid() + ")");
            return true;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            say("❌ فشل تشغيل الباك إند: " + e.getMessage());
            return false;
        }
    }

    public synchronized boolean startPortable(String backendPath) {
        return startNormal(backendPath);
    }

    private boolean startService() {
        if (!OsSupport.WINDOWS) {
            say(WINDOWS_SERVICE_MSG);
            return false;
        }
        try {
            if (!ProcessRunner.serviceInstalled(SERVICE_NAME)) {
                say("❌ الخدمة " + SERVICE_NAME + " مش مثبتة");
                return false;
            }
            ProcessRunner.Result r = ProcessRunner.run(60, List.of("sc", "start", SERVICE_NAME));
            boolean ok = r.ok() || r.output().contains("1056");
            boolean running = ok && ProcessRunner.waitUntil(() -> ProcessRunner.serviceRunning(SERVICE_NAME), 60);
            say(running ? "✅ خدمة الباك إند شغالة" : "❌ فشل تشغيل الخدمة: " + r.output());
            return running;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            say("❌ خطأ: " + e.getMessage());
            return false;
        }
    }

    public synchronized boolean stop(boolean asService) {
        return asService ? stopService() : stopNormal();
    }

    private boolean stopNormal() {
        try {
            Path exe = currentExe();

            Set<ProcessHandle> targets = new LinkedHashSet<>();
            readPid().flatMap(ProcessRunner::handle)
                    .filter(h -> exe == null || ProcessRunner.matchesExe(h, exe))
                    .ifPresent(targets::add);
            if (exe != null) targets.addAll(ProcessRunner.findByExecutable(exe));

            if (targets.isEmpty()) {
                clearPid();
                say("ℹ️ الباك إند مش شغال");
                return true;
            }

            for (ProcessHandle h : targets) {
                say("🔄 إيقاف العملية PID " + h.pid());
                ProcessRunner.killTree(h);
            }
            clearPid();

            boolean stopped = targets.stream().noneMatch(ProcessHandle::isAlive)
                    && (exe == null || ProcessRunner.findByExecutable(exe).isEmpty());
            say(stopped ? "✅ تم إيقاف الباك إند" : "❌ الباك إند لسه شغال");
            return stopped;

        } catch (Exception e) {
            say("⚠️ خطأ في إيقاف الباك إند: " + e.getMessage());
            return false;
        }
    }

    private boolean stopService() {
        if (!OsSupport.WINDOWS) {
            say(WINDOWS_SERVICE_MSG);
            return false;
        }
        try {
            if (!ProcessRunner.serviceInstalled(SERVICE_NAME)) return true;
            ProcessRunner.run(60, List.of("sc", "stop", SERVICE_NAME));
            boolean stopped = ProcessRunner.waitUntil(() -> !ProcessRunner.serviceRunning(SERVICE_NAME), 60);
            say(stopped ? "✅ الخدمة متوقفة" : "❌ الخدمة لسه شغالة");
            return stopped;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            say("❌ خطأ: " + e.getMessage());
            return false;
        }
    }

    public synchronized boolean restart(String backendPath, boolean asService) {
        if (!stop(asService)) return false;
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        return start(backendPath, asService);
    }

    // ══════════════════════════════════════════════════════════════
    //  Status
    // ══════════════════════════════════════════════════════════════

    private boolean isRunning(Path exe) {
        boolean byPid = readPid().flatMap(ProcessRunner::handle)
                .filter(h -> ProcessRunner.matchesExe(h, exe))
                .isPresent();
        return byPid || !ProcessRunner.findByExecutable(exe).isEmpty();
    }

    public synchronized boolean isRunning() {
        if (OsSupport.WINDOWS && ProcessRunner.serviceRunning(SERVICE_NAME)) return true;
        Path exe = currentExe();
        return exe != null && isRunning(exe);
    }

    public Long getPid() {
        return readPid().flatMap(ProcessRunner::handle).map(ProcessHandle::pid).orElse(null);
    }

    public boolean isServiceInstalled(String serviceName) {
        return ProcessRunner.serviceInstalled(serviceName);
    }

    public boolean isServiceRunning(String serviceName) {
        return ProcessRunner.serviceRunning(serviceName);
    }

    // ══════════════════════════════════════════════════════════════
    //  Service install / uninstall (ويندوز بس — NSSM)
    // ══════════════════════════════════════════════════════════════

    public synchronized boolean installService(String backendPath, String serviceName) {
        if (!OsSupport.WINDOWS) {
            say(WINDOWS_SERVICE_MSG);
            return false;
        }
        if (serviceName == null || !SERVICE_NAME_RE.matcher(serviceName).matches()) {
            say("❌ اسم الخدمة غير صالح");
            return false;
        }
        Path nssm = findNssm();
        if (nssm == null) {
            say("❌ nssm.exe مش موجود (حطه جنب البرنامج أو في C:\\nssm)");
            return false;
        }
        if (backendPath == null || backendPath.isBlank()) return false;
        Path exe = resolveExe(backendPath);
        if (!Files.isRegularFile(exe)) {
            say("❌ الـ launcher غير موجود: " + exe);
            return false;
        }

        try {
            if (ProcessRunner.serviceInstalled(serviceName) && !forceUninstallService(serviceName)) return false;

            Path logs = OsSupport.logsDir();
            Files.createDirectories(logs);

            if (!nssm(nssm, "install", serviceName, exe.toString())) return false;

            boolean ok = true;
            ok &= nssm(nssm, "set", serviceName, "AppDirectory", exe.getParent().toString());
            ok &= nssm(nssm, "set", serviceName, "AppStdout", logs.resolve("backend_stdout.log").toString());
            ok &= nssm(nssm, "set", serviceName, "AppStderr", logs.resolve("backend_stderr.log").toString());
            ok &= nssm(nssm, "set", serviceName, "AppRotateFiles", "1");
            ok &= nssm(nssm, "set", serviceName, "AppRotateOnline", "1");
            ok &= nssm(nssm, "set", serviceName, "AppRotateSeconds", "86400");
            ok &= nssm(nssm, "set", serviceName, "AppRotateBytes", "10485760");
            ok &= nssm(nssm, "set", serviceName, "DisplayName", DISPLAY_NAME);
            ok &= nssm(nssm, "set", serviceName, "Description", "HR_MATE backend service");
            ok &= nssm(nssm, "set", serviceName, "Start", "SERVICE_DELAYED_AUTO_START");
            ok &= nssm(nssm, "set", serviceName, "AppExit", "Default", "Restart");
            ok &= nssm(nssm, "set", serviceName, "AppThrottle", "10000");
            ok &= nssm(nssm, "set", serviceName, "AppRestartDelay", "15000");
            if (!ok) {
                say("❌ فشل ضبط بعض إعدادات الخدمة");
                return false;
            }

            String pgSvc = AppConfig.getString("connection", "pgServiceName", "PostgreSQL");
            ProcessRunner.Result dep = ProcessRunner.run(30, List.of("sc", "config", serviceName, "depend=", pgSvc));
            if (!dep.ok()) say("⚠️ تعذر ضبط التبعية على " + pgSvc + ": " + dep.output());

            say("✅ تم تثبيت الخدمة " + serviceName);
            return true;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            say("❌ خطأ في تثبيت الخدمة: " + e.getMessage());
            return false;
        }
    }

    /** إيقاف + حذف الخدمة (NSSM remove، وبعدين sc delete كـ fallback). */
    public synchronized boolean forceUninstallService(String serviceName) {
        if (!OsSupport.WINDOWS) {
            say(WINDOWS_SERVICE_MSG);
            return false;
        }
        try {
            if (!ProcessRunner.serviceInstalled(serviceName)) return true;

            ProcessRunner.run(60, List.of("sc", "stop", serviceName));
            ProcessRunner.waitUntil(() -> !ProcessRunner.serviceRunning(serviceName), 30);

            Path nssm = findNssm();
            if (nssm != null) nssm(nssm, "remove", serviceName, "confirm");

            for (int i = 0; i < 5 && ProcessRunner.serviceInstalled(serviceName); i++) {
                ProcessRunner.Result r = ProcessRunner.run(30, List.of("sc", "delete", serviceName));
                if (r.ok() || r.output().contains("1060")) break;
                Thread.sleep(2000);
            }

            boolean gone = !ProcessRunner.serviceInstalled(serviceName);
            say(gone
                    ? "✅ تم حذف الخدمة " + serviceName
                    : "❌ الخدمة " + serviceName + " لسه موجودة (ممكن تحتاج إعادة تشغيل)");
            return gone;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            say("❌ خطأ في حذف الخدمة: " + e.getMessage());
            return false;
        }
    }

    public boolean uninstallService(String serviceName) {
        return forceUninstallService(serviceName);
    }

    public boolean deleteService(String serviceName) {
        return forceUninstallService(serviceName);
    }

    /** يحذف كل الخدمات اللي اسمها HR_MATE* (ويندوز بس). */
    public synchronized boolean fixAllServices() {
        if (!OsSupport.WINDOWS) {
            say(WINDOWS_SERVICE_MSG);
            return false;
        }
        try {
            ProcessRunner.Result list = ProcessRunner.run(30, List.of("sc", "query", "state=", "all"));
            if (!list.ok()) return false;

            Matcher m = SERVICE_LINE.matcher(list.output());
            List<String> targets = new ArrayList<>();
            while (m.find()) {
                String name = m.group(1);
                if (name.equalsIgnoreCase(SERVICE_NAME)
                        || name.toUpperCase(Locale.ROOT).startsWith("HR_MATE")) {
                    targets.add(name);
                }
            }

            boolean all = true;
            for (String name : targets) {
                all &= forceUninstallService(name);
            }
            return all;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            say("❌ خطأ: " + e.getMessage());
            return false;
        }
    }
}