package com.safwat.hr.system;

import com.safwat.hr.main.Config;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class PostgreSQLService {

    private static PostgreSQLService instance;
    private StringBuilder logs = new StringBuilder();
    private Config config;
    private Process currentProcess;
    private boolean isRunning = false;

    private static final String PG_USER = "postgres";
    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASSWORD = "admin";
    private static final String DEFAULT_DB = "hr_db";

    private PostgreSQLService() {
        config = Config.getInstance();
    }

    public static PostgreSQLService getInstance() {
        if (instance == null) {
            instance = new PostgreSQLService();
        }
        return instance;
    }

    // ==================== التهيئة ====================

    public boolean initialize(String binPath, String dataPath, String username, String password) {
        log("🔄 بدء تهيئة PostgreSQL...");
        log("   Bin: " + binPath);
        log("   Data: " + dataPath);
        log("   User: " + username);
        log("   Password: " + (password != null && !password.isEmpty() ? "********" : "empty"));

        try {
            String ext = getOsExt();
            File initDb = new File(binPath, "initdb" + ext);
            File pgCtl = new File(binPath, "pg_ctl" + ext);
            File psql = new File(binPath, "psql" + ext);
            File createdb = new File(binPath, "createdb" + ext);

            if (!initDb.exists() || !pgCtl.exists() || !psql.exists() || !createdb.exists()) {
                log("❌ ملفات PostgreSQL غير مكتملة في: " + binPath);
                log("   initdb: " + initDb.exists() + " | pg_ctl: " + pgCtl.exists());
                log("   psql: " + psql.exists() + " | createdb: " + createdb.exists());
                return false;
            }

            File dataDir = new File(dataPath);
            if (dataDir.exists()) {
                log("⚠️ مجلد data موجود، جاري حذفه...");
                deleteDirectory(dataDir);
            }

            if (!dataDir.mkdirs()) {
                log("❌ فشل إنشاء مجلد data");
                return false;
            }
            log("✅ تم إنشاء مجلد data");

            log("🔄 تنفيذ initdb...");
            List<String> initCmd = Arrays.asList(
                    binPath + File.separator + "initdb" + ext,
                    "-U", PG_USER,
                    "-A", "trust",
                    "-D", dataPath,
                    "-E", "UTF8"
            );

            int initResult = executeCommand(initCmd, 60);
            if (initResult != 0) {
                log("❌ فشل تهيئة PostgreSQL (رمز: " + initResult + ")");
                return false;
            }
            log("✅ تم تهيئة PostgreSQL");

            configurePgHba(dataPath, "trust");
            configurePostgresqlConf(dataPath);

            log("🔄 بدء PostgreSQL مؤقتاً...");
            if (!start(binPath, dataPath, false)) {
                log("❌ فشل بدء PostgreSQL");
                return false;
            }

            log("⏳ انتظار PostgreSQL ليصبح جاهزاً...");
            Thread.sleep(5000);

            if (!isRunning()) {
                log("❌ PostgreSQL لم يعمل بشكل صحيح");
                return false;
            }
            log("✅ PostgreSQL يعمل وجاهز");

            if (!createSuperUser(binPath, username, password)) {
                log("❌ فشل إنشاء المستخدم " + username);
                stop(false);
                return false;
            }

            if (!checkUserExists(binPath, username)) {
                log("❌ المستخدم " + username + " غير موجود بعد الإنشاء");
                stop(false);
                return false;
            }

            if (!createDatabaseWithPostgres(binPath, DEFAULT_DB)) {
                log("❌ فشل إنشاء قاعدة البيانات");
                stop(false);
                return false;
            }

            grantPrivileges(binPath, DEFAULT_DB, username);

            if (!testAdminConnection(binPath, DEFAULT_DB, username, password)) {
                log("⚠️ تحذير: فشل اختبار الاتصال باستخدام admin");
            } else {
                log("✅ اختبار الاتصال باستخدام admin ناجح");
            }

            stop(false);
            configurePgHba(dataPath, "md5");

            log("✅ تم تهيئة PostgreSQL بنجاح");
            log("   👤 المستخدم: " + username + " (SUPERUSER)");
            log("   🔑 كلمة المرور: " + (password != null && !password.isEmpty() ? "********" : "بدون"));
            log("   📁 قاعدة البيانات: " + DEFAULT_DB);
            log("   🔑 المنفذ: " + config.getPgPort());
            log("");
            log("📌 معلومات الاتصال للتطبيق:");
            log("   URL: jdbc:postgresql://localhost:" + config.getPgPort() + "/" + DEFAULT_DB);
            log("   Username: " + username);
            log("   Password: " + (password != null && !password.isEmpty() ? "********" : "بدون"));
            return true;

        } catch (Exception e) {
            log("❌ خطأ في التهيئة: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private boolean checkUserExists(String binPath, String username) {
        try {
            String ext = getOsExt();
            String port = config.getPgPort();

            List<String> cmd = Arrays.asList(
                    binPath + File.separator + "psql" + ext,
                    "-U", PG_USER,
                    "-h", "localhost",
                    "-p", port,
                    "-tAc", "SELECT 1 FROM pg_roles WHERE rolname='" + username + "'"
            );

            int result = executeCommand(cmd, 5);
            return result == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean testAdminConnection(String binPath, String dbName, String username, String password) {
        try {
            String ext = getOsExt();
            String port = config.getPgPort();

            ProcessBuilder pb = new ProcessBuilder(
                    binPath + File.separator + "psql" + ext,
                    "-U", username,
                    "-h", "localhost",
                    "-p", port,
                    "-d", dbName,
                    "-c", "SELECT 'Connection successful!' as Status;"
            );

            Map<String, String> env = pb.environment();
            env.put("PGPASSWORD", password);

            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (Scanner scanner = new Scanner(process.getInputStream())) {
                while (scanner.hasNextLine()) {
                    output.append(scanner.nextLine()).append("\n");
                }
            }

            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }

            return process.exitValue() == 0;
        } catch (Exception e) {
            log("⚠️ فشل اختبار الاتصال: " + e.getMessage());
            return false;
        }
    }

    private void configurePostgresqlConf(String dataPath) throws IOException {
        File confFile = new File(dataPath, "postgresql.conf");
        if (confFile.exists()) {
            List<String> lines = Files.readAllLines(confFile.toPath());
            List<String> newLines = new ArrayList<>();

            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.startsWith("listen_addresses") && !trimmed.startsWith("#")) {
                    newLines.add("listen_addresses = '*'");
                } else if (trimmed.startsWith("port") && !trimmed.startsWith("#")) {
                    newLines.add("port = " + config.getPgPort());
                } else if (trimmed.startsWith("max_connections") && !trimmed.startsWith("#")) {
                    newLines.add("max_connections = 100");
                } else if (trimmed.startsWith("shared_buffers") && !trimmed.startsWith("#")) {
                    newLines.add("shared_buffers = 128MB");
                } else if (trimmed.startsWith("password_encryption") && !trimmed.startsWith("#")) {
                    newLines.add("password_encryption = 'scram-sha-256'");
                } else {
                    newLines.add(line);
                }
            }

            Files.write(confFile.toPath(), newLines);
            log("✅ تم تكوين postgresql.conf");
        }
    }

    private void configurePgHba(String dataPath, String method) throws IOException {
        File hbaFile = new File(dataPath, "pg_hba.conf");
        String content =
                "# PostgreSQL Client Authentication Configuration\n" +
                        "# TYPE  DATABASE        USER            ADDRESS                 METHOD\n\n" +
                        "# Local connections\n" +
                        "local   all             all                                     " + method + "\n" +
                        "# IPv4 local connections\n" +
                        "host    all             all             127.0.0.1/32            " + method + "\n" +
                        "# IPv6 local connections\n" +
                        "host    all             all             ::1/128                 " + method + "\n" +
                        "# Allow admin user from any address\n" +
                        "host    all             admin           0.0.0.0/0               " + method + "\n" +
                        "# Allow postgres user from any address\n" +
                        "host    all             postgres        0.0.0.0/0               " + method + "\n";

        try (FileWriter fw = new FileWriter(hbaFile)) {
            fw.write(content);
        }
        log("✅ تم تكوين pg_hba.conf (" + method + ")");
    }

    private boolean createSuperUser(String binPath, String username, String password) {
        log("🔄 إنشاء مستخدم SUPERUSER: " + username + "...");

        try {
            String ext = getOsExt();
            String port = config.getPgPort();

            List<String> dropCmd = Arrays.asList(
                    binPath + File.separator + "psql" + ext,
                    "-U", PG_USER,
                    "-h", "localhost",
                    "-p", port,
                    "-c", "DROP USER IF EXISTS " + username + ";"
            );
            executeCommand(dropCmd, 5);

            String safePassword = password != null ? password.replace("'", "''") : "";
            List<String> createCmd = Arrays.asList(
                    binPath + File.separator + "psql" + ext,
                    "-U", PG_USER,
                    "-h", "localhost",
                    "-p", port,
                    "-c", "CREATE USER " + username + " WITH PASSWORD '" + safePassword + "' SUPERUSER;"
            );

            int result = executeCommand(createCmd, 10);
            if (result != 0) {
                log("❌ فشل إنشاء المستخدم (رمز: " + result + ")");
                return false;
            }

            List<String> checkCmd = Arrays.asList(
                    binPath + File.separator + "psql" + ext,
                    "-U", PG_USER,
                    "-h", "localhost",
                    "-p", port,
                    "-c", "SELECT rolname, rolsuper FROM pg_roles WHERE rolname='" + username + "';"
            );
            executeCommand(checkCmd, 5);

            log("✅ تم إنشاء مستخدم SUPERUSER: " + username);
            log("   كلمة المرور: " + (password != null && !password.isEmpty() ? "********" : "بدون"));
            return true;

        } catch (Exception e) {
            log("❌ فشل إنشاء المستخدم: " + e.getMessage());
            return false;
        }
    }

    private boolean createDatabaseWithPostgres(String binPath, String dbName) {
        log("🔄 إنشاء قاعدة البيانات: " + dbName + " (باستخدام postgres)...");

        try {
            String ext = getOsExt();
            String port = config.getPgPort();

            List<String> dropCmd = Arrays.asList(
                    binPath + File.separator + "dropdb" + ext,
                    "-U", PG_USER,
                    "-h", "localhost",
                    "-p", port,
                    "--if-exists", dbName
            );
            executeCommand(dropCmd, 5);

            List<String> createCmd = Arrays.asList(
                    binPath + File.separator + "createdb" + ext,
                    "-U", PG_USER,
                    "-h", "localhost",
                    "-p", port,
                    "-O", ADMIN_USER,
                    dbName
            );

            int result = executeCommand(createCmd, 10);
            if (result != 0) {
                log("❌ فشل إنشاء قاعدة البيانات (رمز: " + result + ")");
                return false;
            }

            log("✅ تم إنشاء قاعدة البيانات: " + dbName + " (المالك: " + ADMIN_USER + ")");
            return true;

        } catch (Exception e) {
            log("❌ خطأ: " + e.getMessage());
            return false;
        }
    }

    private void grantPrivileges(String binPath, String dbName, String username) {
        log("🔄 منح صلاحيات للمستخدم " + username + " على قاعدة " + dbName);

        try {
            String ext = getOsExt();
            String port = config.getPgPort();

            List<String> grantCmd = Arrays.asList(
                    binPath + File.separator + "psql" + ext,
                    "-U", PG_USER,
                    "-h", "localhost",
                    "-p", port,
                    "-c", "GRANT ALL PRIVILEGES ON DATABASE " + dbName + " TO " + username + ";"
            );
            executeCommand(grantCmd, 5);

            List<String> connectCmd = Arrays.asList(
                    binPath + File.separator + "psql" + ext,
                    "-U", PG_USER,
                    "-h", "localhost",
                    "-p", port,
                    "-c", "GRANT CONNECT ON DATABASE " + dbName + " TO PUBLIC;"
            );
            executeCommand(connectCmd, 5);

            log("✅ تم منح الصلاحيات للمستخدم " + username);

        } catch (Exception e) {
            log("⚠️ فشل منح الصلاحيات: " + e.getMessage());
        }
    }

    // ==================== التشغيل والإيقاف ====================

    public boolean start(String binPath, String dataPath, boolean asService) {
        log("🔄 تشغيل PostgreSQL...");
        log("   وضع: " + (asService ? "خدمة" : "عادي"));

        try {
            if (asService) {
                return startService();
            } else {
                return startNormal(binPath, dataPath);
            }
        } catch (Exception e) {
            log("❌ فشل التشغيل: " + e.getMessage());
            return false;
        }
    }

    public boolean startNormal(String binPath, String dataPath) {
        try {
            String ext = getOsExt();
            String logsDir = System.getProperty("user.dir") + File.separator + "logs";
            new File(logsDir).mkdirs();
            String logFile = logsDir + File.separator + "postgresql.log";

            String pgCtlPath = binPath + File.separator + "pg_ctl" + ext;

            ProcessBuilder pb = new ProcessBuilder(
                    pgCtlPath,
                    "start",
                    "-D", dataPath,
                    "-l", logFile
            );

            Map<String, String> env = pb.environment();
            String currentPath = env.getOrDefault("PATH", "");
            env.put("PATH", binPath + File.pathSeparator + currentPath);

            pb.redirectErrorStream(true);

            Process process = pb.start();
            currentProcess = process;

            boolean finished = process.waitFor(15, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                log("❌ انتهى الوقت أثناء انتظار التشغيل");
                return false;
            }

            int result = process.exitValue();
            if (result == 0) {
                isRunning = true;
                log("✅ PostgreSQL يعمل (وضع عادي)");
                return true;
            } else {
                log("❌ فشل التشغيل (رمز: " + result + ")");
                return false;
            }

        } catch (Exception e) {
            log("❌ خطأ: " + e.getMessage());
            return false;
        }
    }

    private boolean startService() {
        try {
            int result = executeCommand(Arrays.asList("net", "start", "PostgreSQL"), 30);
            if (result == 0) {
                isRunning = true;
                log("✅ خدمة PostgreSQL تعمل");
                return true;
            }
            log("❌ فشل تشغيل الخدمة");
            return false;
        } catch (Exception e) {
            log("❌ خطأ: " + e.getMessage());
            return false;
        }
    }

    public boolean stop(boolean asService) {
        log("🔄 إيقاف PostgreSQL...");
        log("   وضع: " + (asService ? "خدمة" : "عادي"));

        try {
            if (asService) {
                return stopService();
            } else {
                return stopNormal();
            }
        } catch (Exception e) {
            log("❌ فشل الإيقاف: " + e.getMessage());
            return false;
        }
    }

    private boolean stopNormal() {
        try {
            String ext = getOsExt();
            String binPath = config.getPgBinPath();
            String dataPath = config.getPgDataPath();

            if (!binPath.isEmpty() && !dataPath.isEmpty()) {
                List<String> stopCmd = Arrays.asList(
                        binPath + File.separator + "pg_ctl" + ext,
                        "stop",
                        "-D", dataPath,
                        "-m", "fast"
                );
                executeCommand(stopCmd, 10);
            }

            if (isRunning()) {
                if (System.getProperty("os.name").toLowerCase().contains("win")) {
                    executeCommand(Arrays.asList("taskkill", "/F", "/IM", "postgres.exe"), 10);
                } else {
                    executeCommand(Arrays.asList("bash", "-c", "pkill -f postgres || true"), 10);
                }
            }

            isRunning = false;
            log("✅ PostgreSQL متوقف (وضع عادي)");
            return true;
        } catch (Exception e) {
            log("⚠️ فشل الإيقاف: " + e.getMessage());
            return false;
        }
    }

    private boolean stopService() {
        try {
            int result = executeCommand(Arrays.asList("net", "stop", "PostgreSQL"), 30);
            if (result == 0) {
                isRunning = false;
                log("✅ خدمة PostgreSQL متوقفة");
                return true;
            }
            log("❌ فشل إيقاف الخدمة");
            return false;
        } catch (Exception e) {
            log("❌ خطأ: " + e.getMessage());
            return false;
        }
    }

    public boolean restart(String binPath, String dataPath, boolean asService) {
        log("🔄 إعادة تشغيل PostgreSQL...");
        stop(asService);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException ignored) {
        }
        return start(binPath, dataPath, asService);
    }

    // ==================== الخدمة (Service) ====================

    public boolean installService(String binPath, String dataPath, String serviceName) {
        log("🔄 تثبيت خدمة PostgreSQL...");

        try {
            String ext = getOsExt();

            executeCommand(Arrays.asList("sc", "delete", serviceName), 10);
            Thread.sleep(1000);

            List<String> cmd = Arrays.asList(
                    binPath + File.separator + "pg_ctl" + ext,
                    "register",
                    "-N", serviceName,
                    "-D", dataPath
            );

            int result = executeCommand(cmd, 30);
            if (result == 0) {
                log("✅ تم تثبيت خدمة PostgreSQL");
                return true;
            }
            log("❌ فشل تثبيت الخدمة (رمز: " + result + ")");
            return false;
        } catch (Exception e) {
            log("❌ خطأ: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteService(String serviceName) {
        log("🔄 حذف خدمة PostgreSQL...");

        try {
            executeCommand(Arrays.asList("net", "stop", serviceName), 10);
            Thread.sleep(1000);

            int result = executeCommand(Arrays.asList("sc", "delete", serviceName), 10);
            if (result == 0) {
                log("✅ تم حذف خدمة PostgreSQL");
                return true;
            }
            log("❌ فشل حذف الخدمة");
            return false;
        } catch (Exception e) {
            log("❌ خطأ: " + e.getMessage());
            return false;
        }
    }

    // ==================== إدارة قواعد البيانات ====================

    public boolean createDatabase(String dbName) {
        log("🔄 إنشاء قاعدة بيانات: " + dbName);

        try {
            String binPath = config.getPgBinPath();
            String ext = getOsExt();
            String port = config.getPgPort();

            List<String> cmd = Arrays.asList(
                    binPath + File.separator + "createdb" + ext,
                    "-U", PG_USER,
                    "-h", "localhost",
                    "-p", port,
                    dbName
            );

            int result = executeCommand(cmd, 10);
            if (result == 0) {
                log("✅ تم إنشاء قاعدة البيانات: " + dbName);
                grantPrivileges(binPath, dbName, ADMIN_USER);
                return true;
            }

            log("❌ فشل إنشاء قاعدة البيانات (رمز: " + result + ")");
            return false;
        } catch (Exception e) {
            log("❌ خطأ: " + e.getMessage());
            return false;
        }
    }

    public boolean dropDatabase(String dbName) {
        log("🔄 حذف قاعدة بيانات: " + dbName);

        try {
            String binPath = config.getPgBinPath();
            String ext = getOsExt();
            String port = config.getPgPort();

            List<String> cmd = Arrays.asList(
                    binPath + File.separator + "dropdb" + ext,
                    "-U", PG_USER,
                    "-h", "localhost",
                    "-p", port,
                    "--if-exists", dbName
            );

            int result = executeCommand(cmd, 10);
            if (result == 0) {
                log("✅ تم حذف قاعدة البيانات: " + dbName);
                return true;
            }
            log("❌ فشل حذف قاعدة البيانات");
            return false;
        } catch (Exception e) {
            log("❌ خطأ: " + e.getMessage());
            return false;
        }
    }

    public String listDatabases() {
        try {
            String binPath = config.getPgBinPath();
            String ext = getOsExt();
            String port = config.getPgPort();

            ProcessBuilder pb = new ProcessBuilder(
                    binPath + File.separator + "psql" + ext,
                    "-U", PG_USER,
                    "-h", "localhost",
                    "-p", port,
                    "-l"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (Scanner scanner = new Scanner(process.getInputStream())) {
                while (scanner.hasNextLine()) {
                    output.append(scanner.nextLine()).append("\n");
                }
            }
            process.waitFor(10, TimeUnit.SECONDS);
            return output.toString();
        } catch (Exception e) {
            return "خطأ: " + e.getMessage();
        }
    }

    // ==================== فحص الحالة ====================

    public boolean isRunning() {
        if (currentProcess != null && currentProcess.isAlive()) {
            return true;
        }

        try {
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                ProcessBuilder pb = new ProcessBuilder("tasklist", "/FI", "IMAGENAME eq postgres.exe");
                pb.redirectErrorStream(true);
                Process process = pb.start();
                try (Scanner scanner = new Scanner(process.getInputStream())) {
                    while (scanner.hasNextLine()) {
                        if (scanner.nextLine().contains("postgres.exe")) {
                            return true;
                        }
                    }
                }
            } else {
                ProcessBuilder pb = new ProcessBuilder("bash", "-c", "ps aux | grep '[p]ostgres' | grep -v grep");
                pb.redirectErrorStream(true);
                Process process = pb.start();
                try (Scanner scanner = new Scanner(process.getInputStream())) {
                    return scanner.hasNext();
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isInitialized(String dataPath) {
        if (dataPath == null || dataPath.isEmpty()) {
            return false;
        }
        File confFile = new File(dataPath, "postgresql.conf");
        File hbaFile = new File(dataPath, "pg_hba.conf");
        return confFile.exists() && hbaFile.exists();
    }

    public boolean isServiceInstalled(String serviceName) {
        try {
            ProcessBuilder pb = new ProcessBuilder("sc", "query", serviceName);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== أدوات مساعدة ====================

    private String getOsExt() {

        return System.getProperty("os.name").toLowerCase().contains("win") ? ".exe" : "";
    }

    private int executeCommand(List<String> command, int timeoutSeconds) throws IOException, InterruptedException {
        log("🔄 تنفيذ: " + String.join(" ", command));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        Process process = pb.start();
        currentProcess = process;

        StringBuilder output = new StringBuilder();
        Thread reader = new Thread(() -> {
            try (Scanner scanner = new Scanner(process.getInputStream())) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    output.append(line).append("\n");
                }
            } catch (Exception ignored) {
            }
        });
        reader.start();

        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

        if (!finished) {
            process.destroyForcibly();
            log("⏱️ انتهى الوقت (" + timeoutSeconds + " ثانية)");
            return -1;
        }

        reader.join(2000);

        int exitCode = process.exitValue();
        if (output.length() > 0) {
            log("   المخرجات:\n" + output.toString().trim());
        }
        log("   رمز الخروج: " + exitCode);
        return exitCode;
    }

    private void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        dir.delete();
    }

    private void log(String message) {
        String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String logMessage = "[" + timestamp + "] " + message;

        logs.append(logMessage).append("\n");
    }

    public String getLogs() {
        return logs.toString();
    }

    public void clearLogs() {
        logs = new StringBuilder();
    }
}