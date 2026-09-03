package com.safwat.hr.system;

import javafx.application.Platform;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

/**
 * مصدر السجلات الموحّد للتطبيق.
 * <p>
 * كل الـ Controllers (MainController, BackendController, PostgreSQLController)
 * ترسل رسائلها هنا بدل الـ StringBuilder المحلي.
 * LogsController يسجّل نفسه Listener ويعرض كل حدث فورًا.
 * بيكتب كمان على ملف logs/app.log على القرص.
 */
public class AppLogBus {

    private static final String LOG_FILE = "logs/app.log";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static AppLogBus instance;

    // قائمة كل السجلات (للعرض عند التسجيل الجديد)
    private final List<String> allLogs = new ArrayList<>();

    // الـ Listeners — كلهم بيتنادوا على الـ FX Thread
    private final List<Consumer<String>> listeners = new ArrayList<>();

    // كتابة على القرص
    private PrintWriter fileWriter;

    private AppLogBus() {
        initFileWriter();
    }

    public static AppLogBus getInstance() {
        if (instance == null) {
            instance = new AppLogBus();
        }
        return instance;
    }

    /**
     * تسجيل رسالة جديدة.
     * يمكن الاستدعاء من أي Thread (FX أو Background).
     */
    public void log(String message) {
        String timestamp = DATE_FORMAT.format(new Date());
        String formatted = "[" + timestamp + "] " + message;

        // حفظ في الذاكرة
        allLogs.add(formatted);

        // كتابة على القرص
        if (fileWriter != null) {
            fileWriter.println(formatted);
            fileWriter.flush();
        }

        // إبلاغ الـ Listeners على الـ FX Thread
        if (Platform.isFxApplicationThread()) {
            notifyListeners(formatted);
        } else {
            Platform.runLater(() -> notifyListeners(formatted));
        }
    }

    private void notifyListeners(String formatted) {
        for (Consumer<String> listener : listeners) {
            try {
                listener.accept(formatted);
            } catch (Exception e) {
                System.err.println("[AppLogBus] خطأ في Listener: " + e.getMessage());
            }
        }
    }

    /**
     * إضافة Listener جديد — يُنادى عند كل رسالة جديدة.
     * عند التسجيل، يستقبل كل السجلات السابقة فورًا.
     */
    public void addListener(Consumer<String> listener) {
        listeners.add(listener);
        // إرسال كل السجلات السابقة للـ Listener الجديد
        for (String log : allLogs) {
            try {
                listener.accept(log);
            } catch (Exception ignored) {}
        }
    }

    /**
     * إزالة Listener (عند إغلاق تبويب السجلات مثلًا).
     */
    public void removeListener(Consumer<String> listener) {
        listeners.remove(listener);
    }

    /**
     * إرجاع كل السجلات كنص كامل (للعرض الأوّلي).
     */
    public String getAllLogsAsText() {
        return String.join("\n", allLogs);
    }

    // ── تهيئة الكتابة على القرص ──

    private void initFileWriter() {
        try {
            File logDir = new File("logs");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            fileWriter = new PrintWriter(new FileWriter(LOG_FILE, true));
            fileWriter.println("\n" + "=".repeat(60));
            fileWriter.println("[" + DATE_FORMAT.format(new Date()) + "] ===== بدء جلسة جديدة =====");
            fileWriter.flush();
        } catch (IOException e) {
            System.err.println("[AppLogBus] فشل فتح ملف السجلات: " + e.getMessage());
        }
    }
}
