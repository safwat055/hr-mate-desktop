package com.safwat.hr.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Paths;
import java.util.Properties;

public class Config {

    private static Config instance;
    private Properties properties;
    private String configPath;

    // إعدادات المسارات
    private String pgFolder = "";
    private String pgBinPath = "";
    private String pgDataPath = "";
    private String backendPath = "";
    private String pgPort = "5432";
    private String backendPort = "8080";

    private Config() {
        load();
    }

    public static Config getInstance() {
        if (instance == null) {
            instance = new Config();
        }
        return instance;
    }

    private void load() {
        properties = new Properties();

        // ===== مسار الإعدادات في مجلد التطبيق =====
        String appDir = Paths.get("").toAbsolutePath().toString();
        configPath = Paths.get(appDir, "config", "config.properties").toString();


        try {
            // إنشاء المجلد إذا لم يكن موجوداً
            File configFile = new File(configPath);
            if (!configFile.exists()) {
                configFile.getParentFile().mkdirs();
                configFile.createNewFile();
                saveDefaultConfig();

            }

            // تحميل الإعدادات
            try (FileInputStream fis = new FileInputStream(configFile)) {
                properties.load(fis);
            }

            // قراءة القيم
            pgFolder = properties.getProperty("pg.folder", "");
            pgBinPath = properties.getProperty("pg.bin", "");
            pgDataPath = properties.getProperty("pg.data", "");
            backendPath = properties.getProperty("backend.path", "");
            pgPort = properties.getProperty("pg.port", "5432");
            backendPort = properties.getProperty("backend.port", "8080");


        } catch (Exception e) {

            saveDefaultConfig();
        }
    }

    private void saveDefaultConfig() {
        pgFolder = "";
        pgBinPath = "";
        pgDataPath = "";
        backendPath = "";
        pgPort = "5432";
        backendPort = "8080";
        save();
    }

    public void save() {
        try {
            properties.setProperty("pg.folder", pgFolder != null ? pgFolder : "");
            properties.setProperty("pg.bin", pgBinPath != null ? pgBinPath : "");
            properties.setProperty("pg.data", pgDataPath != null ? pgDataPath : "");
            properties.setProperty("backend.path", backendPath != null ? backendPath : "");
            properties.setProperty("pg.port", pgPort != null ? pgPort : "5432");
            properties.setProperty("backend.port", backendPort != null ? backendPort : "8080");

            File configFile = new File(configPath);
            configFile.getParentFile().mkdirs();

            try (FileOutputStream fos = new FileOutputStream(configFile)) {
                properties.store(fos, "ArchiveManager Configuration");
            }


        } catch (Exception e) {

        }
    }

    // ===== Getters and Setters =====

    public String getPgFolder() {
        return pgFolder != null ? pgFolder : "";
    }

    public void setPgFolder(String pgFolder) {
        this.pgFolder = pgFolder;
        // تحديث المسارات المشتقة
        if (pgFolder != null && !pgFolder.isEmpty()) {
            this.pgBinPath = pgFolder + File.separator + "bin";
            this.pgDataPath = pgFolder + File.separator + "data";
        }
    }

    public String getPgBinPath() {
        return pgBinPath != null ? pgBinPath : "";
    }

    public void setPgBinPath(String pgBinPath) {
        this.pgBinPath = pgBinPath;
    }

    public String getPgDataPath() {
        return pgDataPath != null ? pgDataPath : "";
    }

    public void setPgDataPath(String pgDataPath) {
        this.pgDataPath = pgDataPath;
    }

    public String getBackendPath() {
        return backendPath != null ? backendPath : "";
    }

    public void setBackendPath(String backendPath) {
        this.backendPath = backendPath;
    }

    public String getPgPort() {
        return pgPort != null ? pgPort : "5432";
    }

    public void setPgPort(String pgPort) {
        this.pgPort = pgPort;
    }

    public String getBackendPort() {
        return backendPort != null ? backendPort : "8080";
    }

    public void setBackendPort(String backendPort) {
        this.backendPort = backendPort;
    }

    public String getConfigPath() {
        return configPath;
    }
}