package com.safwat.hr.system;


import com.safwat.hr.main.Config;
import com.safwat.hr.system.AppLogBus;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class BackendController implements Initializable {
    @FXML private Button btnFixServices;

    @FXML private Label lblBackendStatus;
    @FXML private Label lblBackendPath;
    @FXML private Label lblBackendRunningStatus;
    @FXML private Label lblBackendPort;
    @FXML private Label lblBackendPid;
    @FXML private TextArea txtBackendLogs;

    @FXML private Button btnStart;
    @FXML private Button btnAutoStart;
    @FXML private Button btnStop;
    @FXML private Button btnRestart;
    @FXML private Button btnInstallService;
    @FXML private Button btnDeleteService;
    @FXML private Button btnPortable;
    @FXML private CheckBox chkServiceMode;

    private BackendService backendService;
    private Config config;
    private StringBuilder logs = new StringBuilder();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        config = Config.getInstance();
        backendService = BackendService.getInstance();

        updateInfo();
        setupButtons();
        setBtnAutoStart();
        disableServiceControlsOnLinux();
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5000);
                    Platform.runLater(this::updateInfo);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    private void updateInfo() {
        String backendPath = config.getBackendPath();
        boolean running = backendService.isRunning();
        Long pid = backendService.getPid();

        lblBackendPath.setText(backendPath.isEmpty() ? "غير محدد" : backendPath);
        lblBackendRunningStatus.setText(running ? "🟢 يعمل" : "❌ غير مشغول");
        lblBackendStatus.setText(running ? "🟢 يعمل" : "⏹ متوقف");
        lblBackendPort.setText("8080");
        lblBackendPid.setText(pid != null ? pid.toString() : "-");

        updateLogs();
    }

    private void updateLogs() {
        // مش محتاجة — AppLogBus هو المصدر
    }

    private void addLog(String message) {
        // ✅ AppLogBus الموحّد
        AppLogBus.getInstance().log("[Backend] " + message);
        // عرض في الـ TextArea المحلي أيضًا
        Platform.runLater(() -> {
            if (txtBackendLogs != null) {
                txtBackendLogs.appendText(message + "\n");
            }
        });
    }

    private void setupButtons() {
        btnFixServices.setOnAction(e -> fixServices());
        btnStart.setOnAction(e -> startBackend());
        btnStop.setOnAction(e -> stopBackend());
        btnRestart.setOnAction(e -> restartBackend());
        btnInstallService.setOnAction(e -> installService());
        btnDeleteService.setOnAction(e -> deleteService());
        btnPortable.setOnAction(e -> startPortable());
    }
    private void fixServices() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("إصلاح الخدمات");
        confirm.setHeaderText("سيتم حذف جميع خدمات Backend المثبتة وتثبيت خدمة جديدة");
        confirm.setContentText("هل أنت متأكد؟");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            addLog("🔧 بدء إصلاح خدمات Backend...");

            new Thread(() -> {
                // 1. حذف جميع الخدمات
                boolean fixed = backendService.fixAllServices();

                Platform.runLater(() -> {
                    if (fixed) {
                        addLog("✅ تم حذف جميع خدمات Backend القديمة");

                        // 2. تثبيت خدمة جديدة إذا كان المسار موجوداً
                        String backendPath = config.getBackendPath();
                        if (!backendPath.isEmpty() && new File(backendPath).exists()) {
                            addLog("📦 تثبيت خدمة Backend جديدة...");
                            boolean installed = backendService.installService(backendPath, "ArchiveManager_Backend");
                            if (installed) {
                                addLog("✅ تم تثبيت خدمة Backend جديدة");
                                showAlert("نجاح", "تم إصلاح خدمات Backend بنجاح");
                            } else {
                                addLog("❌ فشل تثبيت خدمة Backend");
                                showAlert("خطأ", "فشل تثبيت خدمة Backend");
                            }
                        } else {
                            addLog("⚠️ مسار Backend غير محدد، تم الحذف فقط");
                            showAlert("نجاح", "تم حذف جميع خدمات Backend");
                        }
                        updateInfo();
                    } else {
                        addLog("❌ فشل إصلاح خدمات Backend");
                        showAlert("خطأ", "فشل إصلاح خدمات Backend");
                    }
                });
            }).start();
        }
    }
    private void startBackend() {
        String backendPath = config.getBackendPath();
        if (backendPath.isEmpty()) {
            showAlert("خطأ", "يرجى تحديد مسار Backend أولاً");
            return;
        }

        boolean asService = chkServiceMode.isSelected();
        addLog("▶ تشغيل Backend...");

        new Thread(() -> {
            boolean success = backendService.start(backendPath, asService);
            Platform.runLater(() -> {
                if (success) {
                    addLog("✅ تم تشغيل Backend");
                    showAlert("نجاح", "تم تشغيل Backend");
                } else {
                    addLog("❌ فشل تشغيل Backend");
                    showAlert("خطأ", "فشل تشغيل Backend");
                }
                updateInfo();
            });
        }).start();
    }

    private void stopBackend() {
        boolean asService = chkServiceMode.isSelected();
        addLog("⏹ إيقاف Backend...");

        new Thread(() -> {
            boolean success = backendService.stop(asService);
            Platform.runLater(() -> {
                if (success) {
                    addLog("✅ تم إيقاف Backend");
                    showAlert("نجاح", "تم إيقاف Backend");
                } else {
                    addLog("❌ فشل إيقاف Backend");
                    showAlert("خطأ", "فشل إيقاف Backend");
                }
                updateInfo();
            });
        }).start();
    }

    private void restartBackend() {
        String backendPath = config.getBackendPath();
        boolean asService = chkServiceMode.isSelected();

        addLog("🔄 إعادة تشغيل Backend...");
        new Thread(() -> {
            boolean success = backendService.restart(backendPath, asService);
            Platform.runLater(() -> {
                if (success) {
                    addLog("✅ تم إعادة تشغيل Backend");
                    showAlert("نجاح", "تم إعادة تشغيل Backend");
                } else {
                    addLog("❌ فشل إعادة تشغيل Backend");
                    showAlert("خطأ", "فشل إعادة تشغيل Backend");
                }
                updateInfo();
            });
        }).start();
    }

    private void installService() {
        String backendPath = config.getBackendPath();
        if (backendPath.isEmpty()) {
            showAlert("خطأ", "يرجى تحديد مسار Backend أولاً");
            return;
        }

        addLog("📦 تثبيت خدمة Backend...");
        new Thread(() -> {
            boolean success = backendService.installService(backendPath, "ArchiveManager_Backend");
            Platform.runLater(() -> {
                if (success) {
                    addLog("✅ تم تثبيت خدمة Backend");
                    showAlert("نجاح", "تم تثبيت خدمة Backend");
                } else {
                    addLog("❌ فشل تثبيت الخدمة");
                    showAlert("خطأ", "فشل تثبيت الخدمة");
                }
                updateInfo();
            });
        }).start();
    }

    private void deleteService() {
        addLog("❌ حذف خدمة Backend...");
        new Thread(() -> {
            boolean success = backendService.deleteService("ArchiveManager_Backend");
            Platform.runLater(() -> {
                if (success) {
                    addLog("✅ تم حذف خدمة Backend");
                    showAlert("نجاح", "تم حذف خدمة Backend");
                } else {
                    addLog("❌ فشل حذف الخدمة");
                    showAlert("خطأ", "فشل حذف الخدمة");
                }
                updateInfo();
            });
        }).start();
    }

    private void startPortable() {
        String backendPath = config.getBackendPath();
        if (backendPath.isEmpty()) {
            showAlert("خطأ", "يرجى تحديد مسار Backend أولاً");
            return;
        }

        addLog("📱 تشغيل Backend (محمول)...");
        new Thread(() -> {
            boolean success = backendService.startPortable(backendPath);
            Platform.runLater(() -> {
                if (success) {
                    addLog("✅ تم تشغيل Backend (محمول)");
                    showAlert("نجاح", "تم تشغيل Backend (محمول)");
                } else {
                    addLog("❌ فشل تشغيل Backend (محمول)");
                    showAlert("خطأ", "فشل تشغيل Backend (محمول)");
                }
                updateInfo();
            });
        }).start();
    }
    @FXML
    private void handleAutoStart() {
        String appPath = config.getBackendPath(); // قراءة المسار من الإعدادات

        if (StartupManager.isInStartup()) {
            // إذا كان موجوداً، اسأل المستخدم إذا كان يريد إزالته
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("التشغيل التلقائي");
            confirm.setHeaderText("التطبيق مسجل بالفعل للتشغيل التلقائي");
            confirm.setContentText("هل تريد إزالته؟");

            if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                boolean removed = StartupManager.removeFromStartup();
                if (removed) {
                    showAlert("نجاح", "تم حذف التطبيق من التشغيل التلقائي");
                    btnAutoStart.setText("تفعيل التشغيل التلقائي");
                } else {
                    showAlert("خطأ", "فشل حذف التطبيق من التشغيل التلقائي");
                }
            }
        } else {
            // إذا لم يكن موجوداً، أضفه
            boolean added = StartupManager.addToStartup(appPath);
            if (added) {
                showAlert("نجاح", "تم إضافة التطبيق للتشغيل التلقائي");
                btnAutoStart.setText("إلغاء التشغيل التلقائي");
            } else {
                showAlert("خطأ", "فشل إضافة التطبيق للتشغيل التلقائي");
            }
        }
    }

    private void setBtnAutoStart() {
        Platform.runLater(()->{
            if (StartupManager.isInStartup()) {
                btnAutoStart.setText("إلغاء التشغيل التلقائي");
            } else {
                btnAutoStart.setText("تفعيل التشغيل التلقائي");
            }
        });

    }
    /**
     * على Linux: تعطيل كل عناصر الخدمة (Windows-only) وإخفاء الـ Checkbox.
     * التشغيل المباشر (btnStart/Stop/Restart) يفضل متاح.
     */
    private void disableServiceControlsOnLinux() {
        if (!System.getProperty("os.name", "").toLowerCase().contains("windows")) {
            Platform.runLater(() -> {
                btnInstallService.setDisable(true);
                btnInstallService.setVisible(false);
                btnDeleteService.setDisable(true);
                btnDeleteService.setVisible(false);
                btnFixServices.setDisable(true);
                btnFixServices.setVisible(false);
                btnAutoStart.setDisable(true);
                btnAutoStart.setVisible(false);
                btnPortable.setDisable(true);
                btnPortable.setVisible(false);
                chkServiceMode.setSelected(false);
                chkServiceMode.setDisable(true);
                chkServiceMode.setVisible(false);
                // حفظ قيمة false في AppConfig
                com.safwat.hr.shared.AppConfig.setValue("connection", "backendAsService", "false");
                addLog("ℹ️ Linux: تم تعطيل أزرار الخدمة — التشغيل المباشر فقط متاح");
            });
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(title.contains("خطأ") ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}