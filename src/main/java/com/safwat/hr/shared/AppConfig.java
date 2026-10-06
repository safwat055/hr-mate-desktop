package com.safwat.hr.shared;

import com.safwat.hr.ui.theme.ThemeEventBus;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.Properties;

/**
 * ══════════════════════════════════════════════════════════════════
 * AppConfig — إعدادات الفرونت (shared)
 * ══════════════════════════════════════════════════════════════════
 * <p>
 * <b>مصدر الحقيقة الوحيد</b> لإعدادات الفرونت. يحلّ محل {@code Config} القديم
 * (config.properties).
 * <p>
 * <b>الموقع الجديد</b>: {@code <user.dir>/config/app_config.json}
 * <p>
 * <b>الترحيل التلقائي</b>: إذا وُجد {@code config/config.properties} قديم ولم
 * تكن قيم paths قد كُتبت في الـ JSON بعد، يتم استيرادها مرة واحدة ثم يُهمَل
 * الملف القديم.
 * <p>
 * الحفظ يحدث فوراً عند كل استدعاء لـ {@link #setValue}.
 */
public class AppConfig {

    // ══════════════════════════ ثوابت المسار ══════════════════════════
    private static final String CONFIG_DIR = "config";
    private static final String CONFIG_NAME = "app_config.json";
    private static final String LEGACY_FILE = "config.properties";

    // ══════════════════════════ الحالة ══════════════════════════
    private static JSONObject config;
    private static File configFile;

    static {
        initializeConfig();
    }

    /**
     * يُستدعى من الـ Application عند البدء للتأكد من التهيئة.
     */
    public static void ensureInitialized() {
        if (config == null) initializeConfig();
    }

    // ══════════════════════════ التهيئة ══════════════════════════

    private static void initializeConfig() {
        try {
            configFile = new File(CONFIG_DIR, CONFIG_NAME);
            File parent = configFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            if (configFile.exists()) {
                String content = new String(Files.readAllBytes(configFile.toPath()));
                config = new JSONObject(content);
            } else {
                config = createDefaultConfig();
                saveConfigToFile();
                // بعد الإنشاء الأول فقط → نحاول الترحيل من القديم
                migrateLegacyIfNeeded();
            }
        } catch (Exception e) {
            System.err.println("[AppConfig] فشل التحميل: " + e.getMessage());
            config = createDefaultConfig();
        }
    }

    /**
     * الإعدادات الافتراضية.
     * <p>
     * <b>الوضع الافتراضي: فردي (alone=true)</b> — المستخدم الفردي يشغّل فوراً.
     */
    private static JSONObject createDefaultConfig() {
        JSONObject root = new JSONObject();

        // ── الاتصال ──
        JSONObject connection = new JSONObject();
        connection.put("url", "http://");
        connection.put("url2", "ws://");
        connection.put("port", "8080");
        connection.put("masterPC", "localhost");
        connection.put("pgPort", "5432");
        connection.put("user", "admin");
        connection.put("alone", true);              // ← الوضع الفردي افتراضي
        root.put("connection", connection);

        // ── المسارات (تُملأ بواسطة Restore Defaults / Detect) ──
        JSONObject paths = new JSONObject();
        paths.put("pgRoot", "");
        paths.put("pgBin", "");
        paths.put("pgData", "");
        paths.put("backend", "");
        paths.put("backendDir", "");
        paths.put("frontend", "");
        root.put("paths", paths);

        // ── واجهة المستخدم ──
        JSONObject ui = new JSONObject();
        ui.put("theme", ThemeEventBus.LIGHT);
        root.put("ui", ui);

        // ── الإشعارات ──
        JSONObject notifications = new JSONObject();
        notifications.put("reportsEnabled", true);
        root.put("notifications", notifications);

        return root;
    }

    private static void saveConfigToFile() {
        try (FileWriter f = new FileWriter(configFile)) {
            f.write(config.toString(2));
        } catch (Exception e) {
            System.err.println("[AppConfig] فشل الحفظ: " + e.getMessage());
        }
    }

    // ══════════════════════════ الترحيل من config.properties ══════════════════════════

    /**
     * ترحيل شفاف من الملف القديم {@code config/config.properties}.
     * يُنفَّذ مرة واحدة فقط — لو مسار paths.backend فاضي.
     */
    private static void migrateLegacyIfNeeded() {
        File legacy = new File(CONFIG_DIR, LEGACY_FILE);
        if (!legacy.exists()) return;

        // لو الـ JSON لسه فاضي من المسارات، نستورد من properties
        if (!getString("paths", "backend", "").isEmpty()) return;

        try (FileInputStream fis = new FileInputStream(legacy)) {
            Properties p = new Properties();
            p.load(fis);

            setValue("connection", "pgPort", p.getProperty("pg.port", "5432"));
            setValue("connection", "port", p.getProperty("backend.port", "8080"));

            setValue("paths", "pgRoot", p.getProperty("pg.folder", ""));
            setValue("paths", "pgBin", p.getProperty("pg.bin", ""));
            setValue("paths", "pgData", p.getProperty("pg.data", ""));
            setValue("paths", "backend", p.getProperty("backend.path", ""));

            System.out.println("[AppConfig] ✅ تم ترحيل الإعدادات من config.properties");
        } catch (Exception e) {
            System.err.println("[AppConfig] فشل الترحيل: " + e.getMessage());
        }
    }

    // ══════════════════════════ Getters ══════════════════════════

    public static String getString(String mainKey, String subKey, String defaultValue) {
        try {
            if (config.has(mainKey) && config.getJSONObject(mainKey).has(subKey)) {
                return config.getJSONObject(mainKey).getString(subKey);
            }
        } catch (Exception ignored) {
        }
        return defaultValue;
    }

    public static int getInt(String mainKey, String subKey, int defaultValue) {
        try {
            if (config.has(mainKey) && config.getJSONObject(mainKey).has(subKey)) {
                return config.getJSONObject(mainKey).getInt(subKey);
            }
        } catch (Exception ignored) {
        }
        return defaultValue;
    }

    public static double getDouble(String mainKey, String subKey, double defaultValue) {
        try {
            if (config.has(mainKey) && config.getJSONObject(mainKey).has(subKey)) {
                return config.getJSONObject(mainKey).getDouble(subKey);
            }
        } catch (Exception ignored) {
        }
        return defaultValue;
    }

    public static boolean getBoolean(String mainKey, String subKey, boolean defaultValue) {
        try {
            if (config.has(mainKey) && config.getJSONObject(mainKey).has(subKey)) {
                return config.getJSONObject(mainKey).getBoolean(subKey);
            }
        } catch (Exception ignored) {
        }
        return defaultValue;
    }

    public static JSONArray getArray(String mainKey, String subKey) {
        try {
            if (config.has(mainKey) && config.getJSONObject(mainKey).has(subKey)) {
                return config.getJSONObject(mainKey).getJSONArray(subKey);
            }
        } catch (Exception ignored) {
        }
        return new JSONArray();
    }

    public static JSONObject getSection(String mainKey) {
        try {
            if (config.has(mainKey)) return config.getJSONObject(mainKey);
        } catch (Exception ignored) {
        }
        return new JSONObject();
    }

    // ══════════════════════════ Setters ══════════════════════════

    /**
     * يحفظ القيمة فوراً على القرص.
     * <p>
     * ملاحظة أداء: كل استدعاء = كتابة ملف. لو هتكتب قيم كتير مع بعض،
     * استخدم {@link #setValues} لكتابة واحدة.
     */
    public static void setValue(String mainKey, String subKey, Object value) {
        try {
            if (!config.has(mainKey)) {
                config.put(mainKey, new JSONObject());
            }
            config.getJSONObject(mainKey).put(subKey, value);
            saveConfigToFile();
        } catch (Exception e) {
            System.err.println("[AppConfig] فشل حفظ " + mainKey + "." + subKey + ": " + e.getMessage());
        }
    }

    /**
     * كتابة عدة قيم دفعة واحدة — ملف واحد فقط.
     *
     * @param values Map من "section.key" → value
     */
    public static void setValues(java.util.Map<String, Object> values) {
        try {
            for (var e : values.entrySet()) {
                String[] parts = e.getKey().split("\\.", 2);
                if (parts.length < 2) continue;
                String mainKey = parts[0];
                String subKey = parts[1];
                if (!config.has(mainKey)) config.put(mainKey, new JSONObject());
                config.getJSONObject(mainKey).put(subKey, e.getValue());
            }
            saveConfigToFile();
        } catch (Exception e) {
            System.err.println("[AppConfig] فشل الحفظ الجماعي: " + e.getMessage());
        }
    }

    public static void removeValue(String mainKey, String subKey) {
        try {
            if (config.has(mainKey)) {
                config.getJSONObject(mainKey).remove(subKey);
                saveConfigToFile();
            }
        } catch (Exception ignored) {
        }
    }

    // ══════════════════════════ Utilities ══════════════════════════

    /**
     * إعادة تحميل الملف من القرص.
     */
    public static synchronized void reload() {
        initializeConfig();
        System.out.println("[AppConfig] 🔄 تم إعادة التحميل من: "
                + (configFile != null ? configFile.getAbsolutePath() : "?"));
    }

    public static File getConfigFile() {
        return configFile;
    }
}