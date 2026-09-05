package com.safwat.hr.shared;


import com.safwat.hr.ui.theme.ThemeEventBus;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;

public class AppConfig {

    private static final String CONFIG_FILE = "app_config.json";
    private static JSONObject config;

    static {
        initializeConfig();
    }

    public static void ensureInitialized() {
        // الكود هيشتغل مرة واحدة فقط بسبب الـ static block

    }

    /**
     * تهيئة ملف الإعدادات - يتأكد من وجود الملف والإعدادات الافتراضية
     */
    private static void initializeConfig() {
        try {
            File configFile = new File(CONFIG_FILE);

            if (configFile.exists()) {
                // الملف موجود - نقوم بتحميله
                String content = new String(Files.readAllBytes(configFile.toPath()));
                config = new JSONObject(content);
            } else {
                // الملف غير موجود - ننشئ إعدادات افتراضية
                config = createDefaultConfig();
                saveConfigToFile();
            }
        } catch (Exception e) {

            config = createDefaultConfig();
        }
    }

    /**
     * إنشاء الإعدادات الافتراضية
     */
    private static JSONObject createDefaultConfig() {
        JSONObject defaultConfig = new JSONObject();

        // إعدادات قاعدة البيانات
        JSONObject connection = new JSONObject();
        connection.put("url", "http://");
        connection.put("url2", "ws://");
        connection.put("port", "8080");
        connection.put("masterPC", "localhost");
        connection.put("user", "admin");
        connection.put("alone", false);

        defaultConfig.put("connection", connection);

        JSONObject ui = new JSONObject();
        ui.put("theme", ThemeEventBus.LIGHT);
        defaultConfig.put("ui", ui);
        return defaultConfig;
    }

    /**
     * حفظ الإعدادات في الملف
     */
    private static void saveConfigToFile() {
        try (FileWriter file = new FileWriter(CONFIG_FILE)) {
            file.write(config.toString(2));
        } catch (Exception e) {

        }
    }

    /**
     * الحصول على قيمة نصية من الإعدادات
     *
     * @param mainKey      المفتاح الأساسي (القسم)
     * @param subKey       المفتاح الثانوي
     * @param defaultValue القيمة الافتراضية إذا لم يوجد
     * @return القيمة المخزنة أو الافتراضية
     */
    public static String getString(String mainKey, String subKey, String defaultValue) {
        try {
            if (config.has(mainKey) && config.getJSONObject(mainKey).has(subKey)) {
                return config.getJSONObject(mainKey).getString(subKey);
            }
        } catch (Exception e) {

        }
        return defaultValue;
    }

    /**
     * الحصول على قيمة رقمية من الإعدادات
     */
    public static int getInt(String mainKey, String subKey, int defaultValue) {
        try {
            if (config.has(mainKey) && config.getJSONObject(mainKey).has(subKey)) {
                return config.getJSONObject(mainKey).getInt(subKey);
            }
        } catch (Exception e) {

        }
        return defaultValue;
    }

    /**
     * الحصول على قيمة منطقية من الإعدادات
     */
    public static boolean getBoolean(String mainKey, String subKey, boolean defaultValue) {
        try {
            if (config.has(mainKey) && config.getJSONObject(mainKey).has(subKey)) {
                return config.getJSONObject(mainKey).getBoolean(subKey);
            }
        } catch (Exception e) {

        }
        return defaultValue;
    }

    /**
     * الحصول على قيمة مصفوفة من الإعدادات
     */
    public static JSONArray getArray(String mainKey, String subKey) {
        try {
            if (config.has(mainKey) && config.getJSONObject(mainKey).has(subKey)) {
                return config.getJSONObject(mainKey).getJSONArray(subKey);
            }
        } catch (Exception e) {

        }
        return new JSONArray(); // مصفوفة فارغة افتراضياً
    }

    /**
     * حفظ/تعديل إعداد في الملف
     *
     * @param mainKey المفتاح الأساسي (القسم)
     * @param subKey  المفتاح الثانوي
     * @param value   القيمة المراد حفظها
     */
    public static void setValue(String mainKey, String subKey, Object value) {
        try {
            // إذا لم يكن القسم موجوداً، ننشئه
            if (!config.has(mainKey)) {
                config.put(mainKey, new JSONObject());
            }

            // إضافة/تعديل القيمة
            config.getJSONObject(mainKey).put(subKey, value);

            // حفظ التغييرات في الملف
            saveConfigToFile();



        } catch (Exception e) {

        }
    }

    /**
     * حذف إعداد معين
     */
    public static void removeValue(String mainKey, String subKey) {
        try {
            if (config.has(mainKey)) {
                config.getJSONObject(mainKey).remove(subKey);
                saveConfigToFile();

            }
        } catch (Exception e) {

        }
    }

    /**
     * الحصول على جميع إعدادات قسم معين
     */
    public static JSONObject getSection(String mainKey) {
        try {
            if (config.has(mainKey)) {
                return config.getJSONObject(mainKey);
            }
        } catch (Exception e) {

        }
        return new JSONObject();
    }
}