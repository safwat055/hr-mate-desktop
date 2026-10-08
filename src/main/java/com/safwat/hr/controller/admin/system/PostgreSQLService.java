package com.safwat.hr.controller.admin.system;

import com.safwat.hr.shared.AppConfig;
import com.safwat.hr.system.setup.PathResolver;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * إدارة PostgreSQL المحلي (ويندوز ولينكس — وضع عادي/محمول، وخدمة ويندوز).
 *
 * <p>قرارات التصميم:
 * <ul>
 *   <li>المستخدم الإداري الداخلي اسمه {@value #BOOTSTRAP_USER} (initdb بينشئه)،
 *       ومستخدم التطبيق اسمه من AppConfig (افتراضي admin).</li>
 *   <li>كل أوامر SQL بتتبعت على stdin (psql -f -)، فالباسورد مابيظهرش في سطر الأوامر.</li>
 *   <li>الاتصال من الجهاز نفسه فقط (localhost). مفيش وصول من الشبكة.</li>
 *   <li>إيقاف السيرفر بـ pg_ctl، والـ PID من postmaster.pid كـ fallback، بس لعملية الـ data dir دي.</li>
 * </ul>
 */
@Slf4j
public class PostgreSQLService {

    static final String BOOTSTRAP_USER = "postgres";
    static final String DEFAULT_DB = "hr_db";

    private static final String DEFAULT_APP_USER = "admin";
    private static final String DEFAULT_SERVICE = "PostgreSQL";
    private static final String DEFAULT_PORT = "5432";
    private static final Pattern IDENT = Pattern.compile("[A-Za-z_][A-Za-z0-9_]{0,62}");
    private static final Pattern SERVICE_NAME_RE = Pattern.compile("[A-Za-z0-9_.\\-]{1,128}");
    private static final Pattern PORT_RE = Pattern.compile("\\d{1,5}");
    private static final int LOG_MAX_LINES = 2000;
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static volatile PostgreSQLService instance;

    private final Deque<String> logs = new ArrayDeque<>();

    private PostgreSQLService() {
    }

    public static PostgreSQLService getInstance() {
        if (instance == null) {
            synchronized (PostgreSQLService.class) {
                if (instance == null) instance = new PostgreSQLService();
            }
        }
        return instance;
    }

    // ══════════════════════════════════════════════════════════════
    //  Config
    // ══════════════════════════════════════════════════════════════

    private String pgPort() {
        String v = AppConfig.getString("connection", "pgPort", DEFAULT_PORT);
        return (v != null && PORT_RE.matcher(v.trim()).matches()) ? v.trim() : DEFAULT_PORT;
    }

    private String appUser() {
        return AppConfig.getString("connection", "pgUser", DEFAULT_APP_USER);
    }

    private String serviceName() {
        String v = AppConfig.getString("connection", "pgServiceName", DEFAULT_SERVICE);
        return (v != null && SERVICE_NAME_RE.matcher(v).matches()) ? v : DEFAULT_SERVICE;
    }

    private String pgBinPath() {
        String v = AppConfig.getString("paths", "pgBin", "");
        if (v != null && !v.isEmpty()) return v;
        return PathResolver.detect().map(d -> d.pgBin().toString()).orElse("");
    }

    private String pgDataPath() {
        String v = AppConfig.getString("paths", "pgData", "");
        if (v != null && !v.isEmpty()) return v;
        return PathResolver.detect().map(d -> d.pgData().toString()).orElse("");
    }

    // ══════════════════════════════════════════════════════════════
    //  Logging
    // ══════════════════════════════════════════════════════════════

    private synchronized void say(String msg) {
        logs.addLast("[" + LocalDateTime.now().format(TS) + "] " + msg);
        while (logs.size() > LOG_MAX_LINES) logs.removeFirst();
        log.info(msg);
    }

    private void logResult(ProcessRunner.Result r) {
        say("   رمز الخروج: " + r.exitCode());
        if (!r.output().isBlank()) say("   " + r.output().replace("\n", "\n   "));
    }

    public synchronized String getLogs() {
        return String.join("\n", logs) + "\n";
    }

    public synchronized void clearLogs() {
        logs.clear();
    }

    // ══════════════════════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════════════════════

    private static Path tool(String binPath, String name) {
        return Paths.get(binPath, OsSupport.exe(name));
    }

    /** PATH و LD_LIBRARY_PATH عشان الـ DLLs/الـ .so تتلاقي من فولدر البرنامج المحمول. */
    private static Map<String, String> pgEnv(String binPath) {
        Map<String, String> env = new HashMap<>();
        String path = System.getenv("PATH");
        env.put("PATH", binPath + File.pathSeparator + (path == null ? "" : path));
        if (OsSupport.LINUX) {
            Path lib = Paths.get(binPath).toAbsolutePath().getParent().resolve("lib");
            String old = System.getenv("LD_LIBRARY_PATH");
            env.put("LD_LIBRARY_PATH", lib + (old == null || old.isEmpty() ? "" : File.pathSeparator + old));
        }
        return env;
    }

    private static boolean validIdent(String s) {
        return s != null && IDENT.matcher(s).matches();
    }

    private static String quoteIdent(String s) {
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    private boolean binariesExist(String binPath, String... names) {
        if (binPath == null || binPath.isBlank()) {
            say("❌ مسار bin غير محدد");
            return false;
        }
        boolean ok = true;
        for (String n : names) {
            Path t = tool(binPath, n);
            if (!Files.isRegularFile(t)) {
                say("❌ الأداة مش موجودة: " + t);
                ok = false;
            }
        }
        return ok;
    }

    private static boolean hasEntries(Path dir) throws IOException {
        try (Stream<Path> s = Files.list(dir)) {
            return s.findAny().isPresent();
        }
    }

    /** psql بيقرا الأوامر من stdin. الباسورد مابيتبعتش على سطر الأوامر. */
    private ProcessRunner.Result runSql(String binPath, String sql, boolean tuplesOnly)
            throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>(List.of(
                tool(binPath, "psql").toString(),
                "-X", "-q", "-v", "ON_ERROR_STOP=1",
                "-h", "localhost", "-p", pgPort(),
                "-U", BOOTSTRAP_USER, "-d", "postgres", "-f", "-"));
        if (tuplesOnly) {
            cmd.add(2, "-t");
            cmd.add(3, "-A");
        }
        return ProcessRunner.run(60, cmd, sql, pgEnv(binPath));
    }

    private boolean checkUserExists(String binPath, String username)
            throws IOException, InterruptedException {
        ProcessRunner.Result r = runSql(binPath,
                "SELECT 1 FROM pg_roles WHERE rolname = '" + username + "';", true);
        return r.ok() && "1".equals(r.output().trim());
    }

    // ══════════════════════════════════════════════════════════════
    //  Initialize
    // ══════════════════════════════════════════════════════════════

    /**
     * تهيئة cluster جديد. لازم يتنفذ على thread مش على FX thread.
     * مابيمسحش مجلد البيانات: لو المجلد مش فاضي بيفشل ويقول يعمل إيه.
     */
    public synchronized boolean initialize(String binPath, String dataPath, String username, String password) {
        say("🔄 بدء تهيئة PostgreSQL");

        if (!validIdent(username) || BOOTSTRAP_USER.equals(username)) {
            say("❌ اسم المستخدم غير صالح: حروف إنجليزية وأرقام و_ فقط، ويبدأ بحرف، ومينفعش يكون postgres");
            return false;
        }
        if (OsSupport.isRootUser()) {
            say("❌ PostgreSQL مابيشتغلش بصلاحيات root على لينكس. شغّل البرنامج بمستخدم عادي.");
            return false;
        }
        if (!binariesExist(binPath, "initdb", "pg_ctl", "psql")) return false;
        if (dataPath == null || dataPath.isBlank()) {
            say("❌ مسار البيانات غير محدد");
            return false;
        }

        boolean started = false;
        try {
            OsSupport.makeExecutable(Paths.get(binPath));

            Path data = Paths.get(dataPath).toAbsolutePath().normalize();
            if (Files.isDirectory(data) && hasEntries(data)) {
                say("❌ مجلد البيانات مش فاضي: " + data + " — احذفه يدوياً أو اختر مسار تاني");
                return false;
            }
            if (data.getParent() != null) Files.createDirectories(data.getParent());

            say("🔄 تنفيذ initdb...");
            ProcessRunner.Result init = ProcessRunner.run(180, List.of(
                            tool(binPath, "initdb").toString(),
                            "-U", BOOTSTRAP_USER, "-A", "trust", "-E", "UTF8", "-D", data.toString()),
                    null, pgEnv(binPath));
            logResult(init);
            if (!init.ok()) {
                say("❌ فشل initdb");
                return false;
            }

            writeConf(data, pgPort());
            writeHba(data);

            started = startNormal(binPath, data.toString());
            if (!started) return false;

            say("🔄 إنشاء المستخدم " + username + " (SUPERUSER)...");
            String tag = "pw" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            String pwLiteral = (password == null || password.isEmpty())
                    ? "NULL"
                    : "$" + tag + "$" + password + "$" + tag + "$";
            ProcessRunner.Result role = runSql(binPath,
                    "CREATE ROLE " + quoteIdent(username) + " WITH LOGIN SUPERUSER PASSWORD " + pwLiteral + ";",
                    false);
            logResult(role);
            if (!role.ok()) {
                say("❌ فشل إنشاء المستخدم " + username);
                return false;
            }

            say("🔄 إنشاء قاعدة البيانات " + DEFAULT_DB + "...");
            ProcessRunner.Result db = runSql(binPath,
                    "CREATE DATABASE " + quoteIdent(DEFAULT_DB) + " OWNER " + quoteIdent(username) + ";",
                    false);
            logResult(db);
            if (!db.ok()) {
                say("❌ فشل إنشاء قاعدة البيانات " + DEFAULT_DB);
                return false;
            }

            if (!checkUserExists(binPath, username)) {
                say("❌ المستخدم غير موجود بعد الإنشاء");
                return false;
            }

            say("✅ تم تهيئة PostgreSQL بنجاح");
            say("   URL: jdbc:postgresql://localhost:" + pgPort() + "/" + DEFAULT_DB);
            say("   Username: " + username);
            return true;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            say("❌ تم إلغاء التهيئة");
            return false;
        } catch (Exception e) {
            say("❌ خطأ في التهيئة: " + e.getMessage());
            log.error("PostgreSQL initialization failed", e);
            return false;
        } finally {
            if (started) stopNormal(binPath, dataPath);
        }
    }

    private void writeConf(Path data, String port) throws IOException {
        Path conf = data.resolve("postgresql.conf");
        List<String> lines = new ArrayList<>(Files.readAllLines(conf, StandardCharsets.UTF_8));
        setConf(lines, "listen_addresses", "'localhost'");
        setConf(lines, "port", port);
        setConf(lines, "max_connections", "100");
        Files.write(conf, lines, StandardCharsets.UTF_8);
        say("✅ تم تكوين postgresql.conf (port=" + port + ", listen=localhost)");
    }

    /** يغيّر كل سطر للمفتاح (سواء معلّق أو لأ)، ولو مش موجود بيضيفه في الآخر. */
    private static void setConf(List<String> lines, String key, String value) {
        Pattern p = Pattern.compile("^\\s*#?\\s*" + Pattern.quote(key) + "\\s*=.*$");
        boolean found = false;
        for (int i = 0; i < lines.size(); i++) {
            if (p.matcher(lines.get(i)).matches()) {
                lines.set(i, key + " = " + value);
                found = true;
            }
        }
        if (!found) lines.add(key + " = " + value);
    }

    /** اتصال من الجهاز نفسه فقط. */
    private void writeHba(Path data) throws IOException {
        String content = """
                # Generated by HR-MATE: local connections only.
                # TYPE  DATABASE  USER  ADDRESS        METHOD
                local   all       all                  trust
                host    all       all   127.0.0.1/32   trust
                host    all       all   ::1/128        trust
                """;
        Files.writeString(data.resolve("pg_hba.conf"), content, StandardCharsets.UTF_8);
        say("✅ تم تكوين pg_hba.conf (اتصال من الجهاز نفسه فقط)");
    }

    // ══════════════════════════════════════════════════════════════
    //  Start / Stop / Restart
    // ══════════════════════════════════════════════════════════════

    public synchronized boolean start(String binPath, String dataPath, boolean asService) {
        return asService ? startService() : startNormal(binPath, dataPath);
    }

    public synchronized boolean startNormal(String binPath, String dataPath) {
        try {
            if (!binariesExist(binPath, "pg_ctl")) return false;
            if (dataPath == null || !Files.isDirectory(Paths.get(dataPath))) {
                say("❌ مجلد البيانات غير موجود: " + dataPath);
                return false;
            }
            if (isRunning(binPath, dataPath)) {
                say("ℹ️ PostgreSQL شغال بالفعل");
                return true;
            }

            Path logFile = OsSupport.logsDir().resolve("postgresql.log");
            Files.createDirectories(logFile.getParent());

            say("🔄 تشغيل PostgreSQL (وضع عادي)...");
            ProcessRunner.Result r = ProcessRunner.run(90, List.of(
                            tool(binPath, "pg_ctl").toString(),
                            "start", "-D", dataPath, "-l", logFile.toString(), "-w", "-t", "60"),
                    null, pgEnv(binPath));
            logResult(r);
            say(r.ok() ? "✅ PostgreSQL يعمل" : "❌ فشل تشغيل PostgreSQL");
            return r.ok();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            say("❌ تم إلغاء التشغيل");
            return false;
        } catch (Exception e) {
            say("❌ خطأ في التشغيل: " + e.getMessage());
            return false;
        }
    }

    public synchronized boolean stop(boolean asService) {
        return asService ? stopService() : stopNormal(pgBinPath(), pgDataPath());
    }

    private boolean stopNormal(String binPath, String dataPath) {
        try {
            if (dataPath == null || dataPath.isBlank()) {
                say("❌ مسار البيانات غير محدد");
                return false;
            }
            if (binPath != null && Files.isRegularFile(tool(binPath, "pg_ctl"))) {
                say("🔄 إيقاف PostgreSQL (وضع عادي)...");
                logResult(ProcessRunner.run(60, List.of(
                                tool(binPath, "pg_ctl").toString(),
                                "stop", "-D", dataPath, "-m", "fast", "-w", "-t", "30"),
                        null, pgEnv(binPath)));
            }

            if (isRunning(binPath, dataPath)) {
                // fallback: PID من postmaster.pid، بس لو العملية postgres فعلاً
                readPostmasterPid(dataPath)
                        .flatMap(ProcessRunner::handle)
                        .filter(PostgreSQLService::looksLikePostgres)
                        .ifPresent(h -> {
                            say("⚠️ pg_ctl ما وقفش السيرفر — إيقاف PID " + h.pid() + " مباشرة");
                            ProcessRunner.killTree(h);
                        });
            }

            boolean stopped = !isRunning(binPath, dataPath);
            say(stopped ? "✅ PostgreSQL متوقف" : "❌ PostgreSQL لسه شغال");
            return stopped;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            say("❌ فشل الإيقاف: " + e.getMessage());
            return false;
        }
    }

    public synchronized boolean restart(String binPath, String dataPath, boolean asService) {
        say("🔄 إعادة تشغيل PostgreSQL...");
        try {
            boolean stopped = asService ? stopService() : stopNormal(binPath, dataPath);
            if (!stopped) return false;
            if (!ProcessRunner.waitUntil(() -> !isRunning(binPath, dataPath), 30)) {
                say("❌ PostgreSQL لسه شغال بعد الإيقاف");
                return false;
            }
            return asService ? startService() : startNormal(binPath, dataPath);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static boolean looksLikePostgres(ProcessHandle h) {
        return h.info().command()
                .map(c -> Paths.get(c).getFileName().toString().toLowerCase().startsWith("postgres"))
                .orElse(false);
    }

    private static Optional<Long> readPostmasterPid(String dataPath) {
        Path f = Paths.get(dataPath, "postmaster.pid");
        try {
            if (!Files.isRegularFile(f)) return Optional.empty();
            return Optional.of(Long.parseLong(Files.readAllLines(f).get(0).trim()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /** pg_ctl status: 0 = شغال، 3 = مش شغال. */
    private boolean isRunning(String binPath, String dataPath) {
        try {
            if (binPath == null || binPath.isBlank() || dataPath == null || dataPath.isBlank()) return false;
            if (!Files.isRegularFile(tool(binPath, "pg_ctl"))) return false;
            return ProcessRunner.run(15,
                    List.of(tool(binPath, "pg_ctl").toString(), "status", "-D", dataPath),
                    null, pgEnv(binPath)).ok();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isRunning() {
        return isRunning(pgBinPath(), pgDataPath());
    }

    // ══════════════════════════════════════════════════════════════
    //  Windows service (ويندوز بس)
    // ══════════════════════════════════════════════════════════════

    private static final String LINUX_SERVICE_MSG =
            "ℹ️ خدمات النظام مدعومة على ويندوز فقط — على لينكس استخدم الوضع العادي";

    private boolean startService() {
        if (!OsSupport.WINDOWS) {
            say(LINUX_SERVICE_MSG);
            return false;
        }
        try {
            String svc = serviceName();
            if (!ProcessRunner.serviceInstalled(svc)) {
                say("❌ الخدمة " + svc + " مش مثبتة");
                return false;
            }
            ProcessRunner.Result r = ProcessRunner.run(60, List.of("sc", "start", svc));
            logResult(r);
            boolean ok = r.ok() || r.output().contains("1056"); // 1056 = already running
            boolean running = ok && ProcessRunner.waitUntil(() -> ProcessRunner.serviceRunning(svc), 60);
            say(running ? "✅ خدمة PostgreSQL تعمل" : "❌ فشل تشغيل خدمة PostgreSQL");
            return running;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            say("❌ خطأ: " + e.getMessage());
            return false;
        }
    }

    private boolean stopService() {
        if (!OsSupport.WINDOWS) {
            say(LINUX_SERVICE_MSG);
            return false;
        }
        try {
            String svc = serviceName();
            if (!ProcessRunner.serviceInstalled(svc)) return true;
            ProcessRunner.Result r = ProcessRunner.run(60, List.of("sc", "stop", svc));
            logResult(r);
            boolean stopped = ProcessRunner.waitUntil(() -> !ProcessRunner.serviceRunning(svc), 60);
            say(stopped ? "✅ خدمة PostgreSQL متوقفة" : "❌ خدمة PostgreSQL لسه شغالة");
            return stopped;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            say("❌ خطأ: " + e.getMessage());
            return false;
        }
    }

    public synchronized boolean installService(String binPath, String dataPath, String serviceName) {
        if (!OsSupport.WINDOWS) {
            say(LINUX_SERVICE_MSG);
            return false;
        }
        if (serviceName == null || !SERVICE_NAME_RE.matcher(serviceName).matches()) {
            say("❌ اسم الخدمة غير صالح");
            return false;
        }
        if (!binariesExist(binPath, "pg_ctl")) return false;
        if (dataPath == null || !Files.isDirectory(Paths.get(dataPath))) {
            say("❌ مجلد البيانات غير موجود");
            return false;
        }
        try {
            if (ProcessRunner.serviceInstalled(serviceName) && !deleteService(serviceName)) return false;

            say("🔄 تسجيل الخدمة " + serviceName + " (يحتاج صلاحيات Administrator)...");
            ProcessRunner.Result r = ProcessRunner.run(60, List.of(
                    tool(binPath, "pg_ctl").toString(),
                    "register", "-N", serviceName, "-D", dataPath));
            logResult(r);
            say(r.ok() ? "✅ تم تسجيل الخدمة" : "❌ فشل تسجيل الخدمة");
            return r.ok();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            say("❌ خطأ: " + e.getMessage());
            return false;
        }
    }

    public synchronized boolean deleteService(String serviceName) {
        if (!OsSupport.WINDOWS) {
            say(LINUX_SERVICE_MSG);
            return false;
        }
        try {
            if (!ProcessRunner.serviceInstalled(serviceName)) return true;
            ProcessRunner.run(60, List.of("sc", "stop", serviceName));
            ProcessRunner.waitUntil(() -> !ProcessRunner.serviceRunning(serviceName), 60);

            ProcessRunner.Result r = ProcessRunner.run(30, List.of("sc", "delete", serviceName));
            logResult(r);
            boolean gone = !ProcessRunner.serviceInstalled(serviceName);
            say(gone ? "✅ تم حذف الخدمة" : "❌ فشل حذف الخدمة");
            return gone;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            say("❌ خطأ: " + e.getMessage());
            return false;
        }
    }

    public boolean isServiceInstalled(String serviceName) {
        return ProcessRunner.serviceInstalled(serviceName);
    }

    // ══════════════════════════════════════════════════════════════
    //  Databases
    // ══════════════════════════════════════════════════════════════

    public synchronized boolean createDatabase(String dbName) {
        if (!validIdent(dbName)) {
            say("❌ اسم قاعدة البيانات غير صالح");
            return false;
        }
        try {
            ProcessRunner.Result r = runSql(pgBinPath(),
                    "CREATE DATABASE " + quoteIdent(dbName) + " OWNER " + quoteIdent(appUser()) + ";",
                    false);
            logResult(r);
            if (r.ok()) say("✅ تم إنشاء قاعدة البيانات: " + dbName);
            return r.ok();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            say("❌ خطأ: " + e.getMessage());
            return false;
        }
    }

    public synchronized boolean dropDatabase(String dbName) {
        if (!validIdent(dbName)) {
            say("❌ اسم قاعدة البيانات غير صالح");
            return false;
        }
        try {
            ProcessRunner.Result r = runSql(pgBinPath(),
                    "DROP DATABASE IF EXISTS " + quoteIdent(dbName) + ";", false);
            logResult(r);
            if (r.ok()) say("✅ تم حذف قاعدة البيانات: " + dbName);
            return r.ok();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            say("❌ خطأ: " + e.getMessage());
            return false;
        }
    }

    public String listDatabases() {
        try {
            return runSql(pgBinPath(), "\\l\n", false).output();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "خطأ: تم إلغاء العملية";
        } catch (Exception e) {
            return "خطأ: " + e.getMessage();
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  State
    // ══════════════════════════════════════════════════════════════

    /** الملفات الأساسية موجودة = المجلد اتهيّأ قبل كده. */
    public boolean isInitialized(String dataPath) {
        if (dataPath == null || dataPath.isEmpty()) return false;
        Path d = Paths.get(dataPath);
        return Files.isRegularFile(d.resolve("PG_VERSION"))
                && Files.isRegularFile(d.resolve("postgresql.conf"))
                && Files.isRegularFile(d.resolve("pg_hba.conf"));
    }
}