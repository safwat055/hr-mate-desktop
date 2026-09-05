package com.safwat.hr.system;

import com.safwat.hr.main.Config;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class BackendService {

    private static BackendService instance;
    private Process currentProcess;
    private Long currentPid = null;
    private boolean isRunning = false;
    private Config config;
    private static final String SERVICE_NAME = "HR_MATE_Service";
    private static final String NSSM_EXE = "nssm.exe";

    private BackendService() {
        config = Config.getInstance();
    }

    public static BackendService getInstance() {
        if (instance == null) {
            instance = new BackendService();
        }
        return instance;
    }

    /**
     * البحث عن NSSM في المسارات المختلفة
     */
    private String findNssm() {
        String[] searchPaths = {
                System.getProperty("user.dir") + File.separator + "nssm.exe",
                System.getProperty("user.dir") + File.separator + "services" + File.separator + "windows" + File.separator + "nssm.exe",
                System.getProperty("user.dir") + File.separator + ".." + File.separator + "nssm.exe",
                System.getProperty("user.dir") + File.separator + "lib" + File.separator + "nssm.exe",
                "nssm.exe",
                "C:\\nssm\\nssm.exe",
                "C:\\nssm-2.24\\win64\\nssm.exe",
                "C:\\Program Files\\nssm\\nssm.exe"
        };

        for (String path : searchPaths) {
            File file = new File(path);
            if (file.exists()) {
                return path;
            }
        }
        return null;
    }

    public boolean isNssmAvailable() {
        return findNssm() != null;
    }

    // ==================== التشغيل والإيقاف ====================

    public boolean start(String backendPath, boolean asService) {
        if (backendPath == null || backendPath.isEmpty()) {
            return false;
        }

        if (isRunning()) {
            return true;
        }

        if (asService) {
            return startService();
        } else {
            return startNormal(backendPath);
        }
    }

    public boolean startNormal(String backendPath) {
        try {
            File backendFile = new File(backendPath);
            if (!backendFile.exists()) {
                return false;
            }

            stopNormal();

            ProcessBuilder pb = new ProcessBuilder(backendPath);
            pb.directory(backendFile.getParentFile());
            pb.redirectErrorStream(true);

            String logsDir = System.getProperty("user.dir") + File.separator + "logs";
            new File(logsDir).mkdirs();

            String logFile = logsDir + File.separator + "backend.log";
            pb.redirectOutput(new File(logFile));

            currentProcess = pb.start();
            currentPid = currentProcess.pid();
            isRunning = true;

            new Thread(() -> {
                try {
                    currentProcess.waitFor();
                    isRunning = false;
                    currentPid = null;
                } catch (InterruptedException ignored) {
                }
            }).start();

            return true;
        } catch (Exception e) {
            isRunning = false;
            return false;
        }
    }

    private boolean startService() {
        try {
            if (!isServiceInstalled(SERVICE_NAME)) {
                return false;
            }

            Process process = Runtime.getRuntime().exec("net start " + SERVICE_NAME);
            boolean success = process.waitFor() == 0;
            if (success) {
                isRunning = true;
            }
            return success;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean stop(boolean asService) {
        if (asService) {
            return stopService();
        } else {
            return stopNormal();
        }
    }

    private boolean stopNormal() {
        try {
            if (currentProcess != null && currentProcess.isAlive()) {

                currentProcess.destroy();
                // انتظر ثانية عشان العملية تخلص
                Thread.sleep(1000);

                // لو لسه شغالة، استخدم القتل القسري
                if (currentProcess.isAlive()) {
                    currentProcess.destroyForcibly();
                }

                currentProcess = null;

                currentPid = null;
                isRunning = false;
                return true;
            }

            String os = System.getProperty("os.name").toLowerCase();
            boolean killed = false;

            if (os.contains("win")) {


                // ===== الطريقة الأولى: taskkill (الأفضل) =====

                // HR_MATE.exe
                Process p1 = Runtime.getRuntime().exec("taskkill /F /IM HR_MATE.exe");
                int exitCode1 = p1.waitFor();
                if (exitCode1 == 0) {
                    killed = true;

                }

                // 2. قتل أي Process بـ Window Title يحتوي على "HR_MATE"
                Process p2 = Runtime.getRuntime().exec("taskkill /F /FI \"WINDOWTITLE eq *HR_MATE*\"");
                if (p2.waitFor() == 0) {
                    killed = true;

                }

                // 3. قتل أي javaw.exe بـ Window Title (لأن JavaFX بيشتغل على javaw.exe)
                Process p3 = Runtime.getRuntime().exec("taskkill /F /FI \"IMAGENAME eq javaw.exe\" /FI \"WINDOWTITLE eq *HR_MATE*\"");
                if (p3.waitFor() == 0) {
                    killed = true;

                }

                // 4. قتل أي java.exe بـ Window Title
                Process p4 = Runtime.getRuntime().exec("taskkill /F /FI \"IMAGENAME eq java.exe\" /FI \"WINDOWTITLE eq *HR_MATE*\"");
                if (p4.waitFor() == 0) {
                    killed = true;

                }

                // ===== الطريقة الثانية: PowerShell (بديل wmic) =====
                // قتل أي Process بـ Command Line يحتوي على HR_MATE
                Process p5 = Runtime.getRuntime().exec(
                        "powershell -Command \"Get-Process | Where-Object { $_.CommandLine -like '*HR_MATE*' } | Stop-Process -Force\""
                );
                if (p5.waitFor() == 0) {
                    killed = true;

                }

                // قتل أي Process بـ Command Line يحتوي على "HR_MATE "
                Process p6 = Runtime.getRuntime().exec(
                        "powershell -Command \"Get-Process | Where-Object { $_.CommandLine -like '*HR_MATE*' } | Stop-Process -Force\""
                );
                if (p6.waitFor() == 0) {
                    killed = true;

                }

            } else {
                // ===== Linux / Mac =====


                // 1. قتل أي Process باسم HR_MATE
                Process p1 = Runtime.getRuntime().exec("pkill -f HR_MATE");
                if (p1.waitFor() == 0) {
                    killed = true;

                }

                // 2. قتل أي Process بـ Command Line يحتوي على HR_MATE
                Process p2 = Runtime.getRuntime().exec("pkill -f \"HR_MATE\"");
                if (p2.waitFor() == 0) {
                    killed = true;

                }

                // 3. قتل أي Process بـ Window Title (X11)
                Process p3 = Runtime.getRuntime().exec("wmctrl -c \"HR_MATE\" 2>/dev/null");
                p3.waitFor();

                // 4. قتل أي Process بـ PID (للمزيد من التحكم)
                Process p4 = Runtime.getRuntime().exec("pgrep -f \"HR_MATE\" | xargs kill -9 2>/dev/null");
                if (p4.waitFor() == 0) {
                    killed = true;
                }
            }

            isRunning = false;
            currentPid = null;


            return true;

        } catch (Exception e) {
            System.err.println("⚠️ خطأ في إيقاف البرنامج: " + e.getMessage());
            return false;
        }
    }

    private boolean stopService() {
        try {
            if (!isServiceInstalled(SERVICE_NAME)) {
                return true;
            }

            Process process = Runtime.getRuntime().exec("net stop " + SERVICE_NAME);
            boolean success = process.waitFor() == 0;
            if (success) {
                isRunning = false;
            }
            return success;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean restart(String backendPath, boolean asService) {
        stop(asService);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ignored) {
        }
        return start(backendPath, asService);
    }

    public boolean startPortable(String backendPath) {
        return startNormal(backendPath);
    }

    // ==================== إدارة الخدمة باستخدام NSSM ====================

    /**
     * تثبيت خدمة Backend باستخدام NSSM (مثل الباتش)
     */
    public boolean installService(String backendPath, String serviceName) {
        try {

            if (backendPath == null || backendPath.isEmpty()) {
                return false;
            }

            File backendFile = new File(backendPath);

            if (!backendFile.exists()) {
                return false;
            }

            // ✅ 1. البحث عن NSSM
            String nssmPath = findNssm();

            if (nssmPath == null) {
                return false;
            }

            // ✅ 2. حذف الخدمة القديمة
            forceUninstallService(serviceName);
            Thread.sleep(2000);

            // ✅ 3. تثبيت الخدمة باستخدام NSSM
            String installCmd = String.format(
                    "\"%s\" install %s \"%s\"",
                    nssmPath, serviceName, backendPath
            );

            Process process = Runtime.getRuntime().exec(installCmd);
            int result = process.waitFor();

            if (result != 0) {
                return false;
            }
            String javaArgs = "-Djava.awt.headless=false -Dfile.encoding=UTF-8";
            // ✅ 4. تعيين إعدادات NSSM
            String baseDir = backendFile.getParent();
            String logsDir = System.getProperty("user.dir") + File.separator + "logs";
            new File(logsDir).mkdirs();

            String[] settings = {
                    "AppDirectory", baseDir,
                    "AppStdout", logsDir + File.separator + "backend_stdout.log",
                    "AppStderr", logsDir + File.separator + "backend_stderr.log",
                    "AppRotateFiles", "1",
                    "AppRotateOnline", "1",
                    "AppRotateSeconds", "86400",
                    "AppRotateBytes", "10485760",
                    "DisplayName", "AHR_MATE_Service",
                    "Description", "HR_MATE_Service",
                    "Start", "SERVICE_DELAYED_AUTO_START",  // ✅ بدء متأخر
                    // ✅ إضافة تأخير عند فشل البدء
                    "AppFail", "ignore",                    // تجاهل الفشل
                    "AppFailDelay", "10000",                // انتظار 10 ثواني قبل إعادة المحاولة
                    "AppRestartDelay", "15000",              // تأخير 15 ثانية بين إعادة المحاولات
                    "AppParameters", javaArgs
            };

            for (int i = 0; i < settings.length; i += 2) {
                String setCmd = String.format(
                        "\"%s\" set %s %s \"%s\"",
                        nssmPath, serviceName, settings[i], settings[i + 1]
                );
                Runtime.getRuntime().exec(setCmd).waitFor();
            }

            // ✅ 5. إضافة تبعية على PostgreSQL
            String depCmd = String.format(
                    "sc config %s depend= PostgreSQL",
                    serviceName
            );
            Runtime.getRuntime().exec(depCmd);

            // ✅ 6. إضافة تأخير إضافي عن طريق sc
            // لا يوجد أمر مباشر للتأخير، لكننا نعتمد على SERVICE_DELAYED_AUTO_START

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * حذف الخدمة بالقوة مع محاولات متعددة
     */
    public boolean forceUninstallService(String serviceName) {
        try {
            // ✅ 1. محاولة إيقاف الخدمة عدة مرات
            for (int i = 0; i < 3; i++) {
                try {
                    Process stopProcess = Runtime.getRuntime().exec("net stop \"" + serviceName + "\"");
                    stopProcess.waitFor(5, TimeUnit.SECONDS);
                } catch (Exception ignored) {
                }
                Thread.sleep(1000);
            }

            // ✅ 2. محاولة الحذف باستخدام NSSM أولاً
            String nssmPath = findNssm();
            if (nssmPath != null) {
                String removeCmd = String.format(
                        "\"%s\" remove %s confirm",
                        nssmPath, serviceName
                );
                Process process = Runtime.getRuntime().exec(removeCmd);
                boolean result = process.waitFor(10, TimeUnit.SECONDS);
                if (result) {
                    return true;
                }
            }

            // ✅ 3. محاولة الحذف باستخدام SC مع تأخير
            for (int i = 0; i < 5; i++) {
                Process process = Runtime.getRuntime().exec("sc delete \"" + serviceName + "\"");
                boolean result = process.waitFor(5, TimeUnit.SECONDS);

                if (result) { // 1060 = service does not exist
                    return true;
                }

                if (result) { // marked for deletion
                    Thread.sleep(3000);
                    continue;
                }

                Thread.sleep(2000);
            }

            // ✅ 4. المحاولة الأخيرة - حذف من الريجستري
            String regCmd = String.format(
                    "reg delete HKLM\\SYSTEM\\CurrentControlSet\\Services\\\"%s\" /f",
                    serviceName
            );
            Process regProcess = Runtime.getRuntime().exec(regCmd);
            boolean regResult = regProcess.waitFor(5, TimeUnit.SECONDS);

            if (regResult) {
                return true;
            }

            // ✅ 5. استخدام WMIC كحل أخير
            String wmicCmd = String.format(
                    "wmic service where \"name='%s'\" delete",
                    serviceName
            );
            Process wmicProcess = Runtime.getRuntime().exec(wmicCmd);
            return wmicProcess.waitFor(10, TimeUnit.SECONDS);

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * إلغاء تثبيت الخدمة (نسخة مبسطة)
     */
    public boolean uninstallService(String serviceName) {
        return forceUninstallService(serviceName);
    }

    public boolean deleteService(String serviceName) {
        return forceUninstallService(serviceName);
    }

    /**
     * إصلاح جميع خدمات Backend
     */
    public boolean fixAllServices() {
        try {
            List<String> services = findBackendServices();
            boolean allDeleted = true;

            // ✅ 1. حذف الخدمات الموجودة
            for (String service : services) {
                if (!forceUninstallService(service)) {
                    allDeleted = false;
                }
            }

            // ✅ 2. انتظار للتأكد
            Thread.sleep(3000);

            // ✅ 3. التحقق من وجود خدمات متبقية
            List<String> remaining = findBackendServices();
            for (String service : remaining) {
                // محاولة حذف من الريجستري مباشرة
                String regCmd = String.format(
                        "reg delete HKLM\\SYSTEM\\CurrentControlSet\\Services\\\"%s\" /f",
                        service
                );
                Runtime.getRuntime().exec(regCmd);
            }

            return allDeleted;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * البحث عن جميع خدمات Backend المثبتة
     */
    private List<String> findBackendServices() {
        List<String> services = new ArrayList<>();
        try {
            Process process = Runtime.getRuntime().exec("sc query");
            try (Scanner scanner = new Scanner(process.getInputStream())) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    if (line.contains("SERVICE_NAME:")) {
                        String[] parts = line.split(":");
                        if (parts.length > 1) {
                            String name = parts[1].trim();
                            if (name.toLowerCase().contains("HR_MATE") ||
                                    name.toLowerCase().contains("HR_MATE")) {
                                services.add(name);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // تجاهل
        }
        return services;
    }

    // ==================== فحص الحالة ====================

    public boolean isRunning() {
        if (currentProcess != null && currentProcess.isAlive()) {
            return true;
        }

        if (isServiceRunning(SERVICE_NAME)) {
            return true;
        }

        try {
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                // البحث عن HR_MATE.exe
                Process process = Runtime.getRuntime().exec("tasklist /FI \"IMAGENAME eq HR_MATE.exe\"");
                try (Scanner scanner = new Scanner(process.getInputStream())) {
                    while (scanner.hasNextLine()) {
                        if (scanner.nextLine().contains("HR_MATE.exe")) {
                            return true;
                        }
                    }
                }
            } else {
                Process process = Runtime.getRuntime().exec("ps aux | grep -E 'HR_MATE|HR_MATE' | grep -v grep");
                try (Scanner scanner = new Scanner(process.getInputStream())) {
                    return scanner.hasNext();
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public Long getPid() {
        if (currentPid != null && currentProcess != null && currentProcess.isAlive()) {
            return currentPid;
        }
        return null;
    }

    public boolean isServiceInstalled(String serviceName) {
        try {
            Process process = Runtime.getRuntime().exec("sc query \"" + serviceName + "\"");
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isServiceRunning(String serviceName) {
        try {
            Process process = Runtime.getRuntime().exec("sc query \"" + serviceName + "\"");
            try (Scanner scanner = new Scanner(process.getInputStream())) {

                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    if (line.contains("STATE") && line.contains("RUNNING")) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}