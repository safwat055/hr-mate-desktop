package com.safwat.hr.controller.admin.system;

import com.safwat.hr.shared.AppConfig;
import com.safwat.hr.system.setup.PathResolver;
import com.safwat.hr.ui.controls.SAFNotification;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * شاشة الباك إند — ويندوز ولينكس.
 * <ul>
 *   <li>التشغيل المباشر، الوضع المحمول، والتشغيل التلقائي: متاحين على الاتنين.</li>
 *   <li>خدمة النظام (NSSM / sc) ودمج الخدمات: ويندوز بس، والأزرار بتتخفي على لينكس.</li>
 *   <li>أي عملية ممكن تاخد وقت بتتعمل على thread، والـ FX thread بيعرض النتيجة بس.</li>
 * </ul>
 */
public class BackendController implements Initializable {

    @FXML
    private Button btnFixServices;

    @FXML
    private Label lblBackendStatus;
    @FXML
    private Label lblBackendPath;
    @FXML
    private Label lblBackendRunningStatus;
    @FXML
    private Label lblBackendPort;
    @FXML
    private Label lblBackendPid;
    @FXML
    private TextArea txtBackendLogs;

    @FXML
    private Button btnStart;
    @FXML
    private Button btnAutoStart;
    @FXML
    private Button btnStop;
    @FXML
    private Button btnRestart;
    @FXML
    private Button btnInstallService;
    @FXML
    private Button btnDeleteService;
    @FXML
    private Button btnPortable;
    @FXML
    private CheckBox chkServiceMode;

    private BackendService backendService;
    private final AtomicBoolean refreshing = new AtomicBoolean(false);

    private enum FixResult {FIX_FAILED, DELETED_ONLY, INSTALLED, INSTALL_FAILED}

    // ══════════════════ Config Helpers ══════════════════

    private String backendPath() {
        String v = AppConfig.getString("paths", "backend", "");
        if (v != null && !v.isEmpty()) return v;
        return PathResolver.detect().map(d -> d.backendExe().toString()).orElse("");
    }

    private String backendPort() {
        return AppConfig.getString("connection", "port", "8080");
    }

    /** وضع الخدمة مسموح بس على ويندوز. */
    private boolean serviceMode() {
        return OsSupport.WINDOWS && chkServiceMode.isSelected();
    }

    // ══════════════════ Init ══════════════════

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        backendService = BackendService.getInstance();

        applyPlatformUi();
        setupButtons();
        refreshAutoStartLabel();
        refreshStatusAsync();
        startTicker();
    }

    private void startTicker() {
        Thread t = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(5000);
                    refreshStatusAsync();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "backend-status-ticker");
        t.setDaemon(true);
        t.start();
    }

    /**
     * حالة الباك إند بتتحسب على thread منفصل (sc / ProcessHandle ممكن ياخدوا وقت).
     */
    private void refreshStatusAsync() {
        if (!refreshing.compareAndSet(false, true)) return;

        String path = backendPath();
        Thread t = new Thread(() -> {
            try {
                boolean running = backendService.isRunning();
                Long pid = backendService.getPid();
                Platform.runLater(() -> applyStatus(path, running, pid));
            } finally {
                refreshing.set(false);
            }
        }, "backend-status");
        t.setDaemon(true);
        t.start();
    }

    private void applyStatus(String path, boolean running, Long pid) {
        lblBackendPath.setText(path.isEmpty() ? "غير محدد" : path);
        lblBackendRunningStatus.setText(running ? "🟢 يعمل" : "❌ غير مشغول");
        lblBackendStatus.setText(running ? "🟢 يعمل" : "⏹ متوقف");
        lblBackendPort.setText(backendPort());
        lblBackendPid.setText(pid != null ? pid.toString() : "-");
    }

    /**
     * ويندوز: كل الأزرار متاحة.
     * لينكس: خدمات النظام بتتخفي، والتشغيل المباشر + الوضع المحمول + التشغيل التلقائي بيفضلوا.
     */
    private void applyPlatformUi() {
        if (OsSupport.WINDOWS) return;
        hide(btnInstallService);
        hide(btnDeleteService);
        hide(btnFixServices);
        hide(chkServiceMode);
        chkServiceMode.setSelected(false);
        addLog("ℹ️ لينكس: التشغيل المباشر والتشغيل التلقائي متاحين — خدمات النظام غير مدعومة");
    }

    private static void hide(Node n) {
        n.setVisible(false);
        n.setManaged(false);
    }

    private void addLog(String message) {
        AppLogBus.getInstance().log("[Backend] " + message);
        Platform.runLater(() -> {
            if (txtBackendLogs != null) {
                txtBackendLogs.appendText(message + "\n");
            }
        });
    }

    /**
     * تشغيل أي عملية على thread، وعرض النتيجة على FX thread.
     */
    private static <T> void runBg(Supplier<T> work, Consumer<T> onFx) {
        Thread t = new Thread(() -> {
            T result = work.get();
            Platform.runLater(() -> onFx.accept(result));
        }, "backend-bg");
        t.setDaemon(true);
        t.start();
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

    // ══════════════════ Service repair (ويندوز بس) ══════════════════

    private void fixServices() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("إصلاح الخدمات");
        confirm.setHeaderText("سيتم حذف جميع خدمات Backend المثبتة وتثبيت خدمة جديدة");
        confirm.setContentText("هل أنت متأكد؟");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        addLog("🔧 بدء إصلاح خدمات Backend...");

        runBg(() -> {
            if (!backendService.fixAllServices()) return FixResult.FIX_FAILED;

            String p = backendPath();
            if (p.isEmpty() || !new File(p).exists()) return FixResult.DELETED_ONLY;

            return backendService.installService(p, BackendService.SERVICE_NAME)
                    ? FixResult.INSTALLED
                    : FixResult.INSTALL_FAILED;
        }, result -> {
            switch (result) {
                case FIX_FAILED -> {
                    addLog("❌ فشل إصلاح خدمات Backend");
                    showAlert("خطأ", "فشل إصلاح خدمات Backend");
                }
                case DELETED_ONLY -> {
                    addLog("⚠️ مسار Backend غير محدد، تم الحذف فقط");
                    showAlert("نجاح", "تم حذف جميع خدمات Backend");
                }
                case INSTALLED -> {
                    addLog("✅ تم تثبيت خدمة Backend جديدة");
                    showAlert("نجاح", "تم إصلاح خدمات Backend بنجاح");
                }
                case INSTALL_FAILED -> {
                    addLog("❌ فشل تثبيت خدمة Backend");
                    showAlert("خطأ", "فشل تثبيت خدمة Backend");
                }
            }
            refreshStatusAsync();
        });
    }

    // ══════════════════ Start / Stop / Restart ══════════════════

    private void startBackend() {
        String path = backendPath();
        if (path.isEmpty()) {
            showAlert("خطأ", "يرجى تحديد مسار Backend أولاً");
            return;
        }

        boolean asService = serviceMode();
        addLog("▶ تشغيل Backend...");

        runBg(() -> backendService.start(path, asService), success -> {
            if (success) {
                addLog("✅ تم تشغيل Backend");
                showAlert("نجاح", "تم تشغيل Backend");
            } else {
                addLog("❌ فشل تشغيل Backend — راجع logs/backend.log");
                showAlert("خطأ", "فشل تشغيل Backend");
            }
            refreshStatusAsync();
        });
    }

    private void stopBackend() {
        boolean asService = serviceMode();
        addLog("⏹ إيقاف Backend...");

        runBg(() -> backendService.stop(asService), success -> {
            if (success) {
                addLog("✅ تم إيقاف Backend");
                showAlert("نجاح", "تم إيقاف Backend");
            } else {
                addLog("❌ فشل إيقاف Backend");
                showAlert("خطأ", "فشل إيقاف Backend");
            }
            refreshStatusAsync();
        });
    }

    private void restartBackend() {
        String path = backendPath();
        boolean asService = serviceMode();

        addLog("🔄 إعادة تشغيل Backend...");
        runBg(() -> backendService.restart(path, asService), success -> {
            if (success) {
                addLog("✅ تم إعادة تشغيل Backend");
                showAlert("نجاح", "تم إعادة تشغيل Backend");
            } else {
                addLog("❌ فشل إعادة تشغيل Backend");
                showAlert("خطأ", "فشل إعادة تشغيل Backend");
            }
            refreshStatusAsync();
        });
    }

    private void startPortable() {
        String path = backendPath();
        if (path.isEmpty()) {
            showAlert("خطأ", "يرجى تحديد مسار Backend أولاً");
            return;
        }

        addLog("📱 تشغيل Backend (محمول)...");
        runBg(() -> backendService.startPortable(path), success -> {
            if (success) {
                addLog("✅ تم تشغيل Backend (محمول)");
                showAlert("نجاح", "تم تشغيل Backend (محمول)");
            } else {
                addLog("❌ فشل تشغيل Backend (محمول)");
                showAlert("خطأ", "فشل تشغيل Backend (محمول)");
            }
            refreshStatusAsync();
        });
    }

    // ══════════════════ Service install / delete (ويندوز بس) ══════════════════

    private void installService() {
        String path = backendPath();
        if (path.isEmpty()) {
            showAlert("خطأ", "يرجى تحديد مسار Backend أولاً");
            return;
        }

        addLog("📦 تثبيت خدمة Backend...");
        runBg(() -> backendService.installService(path, BackendService.SERVICE_NAME), success -> {
            if (success) {
                addLog("✅ تم تثبيت خدمة Backend");
                showAlert("نجاح", "تم تثبيت خدمة Backend");
            } else {
                addLog("❌ فشل تثبيت الخدمة — راجع السجل");
                showAlert("خطأ", "فشل تثبيت الخدمة");
            }
            refreshStatusAsync();
        });
    }

    private void deleteService() {
        addLog("🗑 حذف خدمة Backend...");
        runBg(() -> backendService.deleteService(BackendService.SERVICE_NAME), success -> {
            if (success) {
                addLog("✅ تم حذف خدمة Backend");
                showAlert("نجاح", "تم حذف خدمة Backend");
            } else {
                addLog("❌ فشل حذف الخدمة");
                showAlert("خطأ", "فشل حذف الخدمة");
            }
            refreshStatusAsync();
        });
    }

    // ══════════════════ Auto start (ويندوز ولينكس) ══════════════════

    @FXML
    private void handleAutoStart() {
        String appPath = backendPath();
        btnAutoStart.setDisable(true);

        runBg(StartupManager::isInStartup, inStartup -> {
            if (inStartup) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("التشغيل التلقائي");
                confirm.setHeaderText("التطبيق مسجل بالفعل للتشغيل التلقائي");
                confirm.setContentText("هل تريد إزالته؟");

                if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                    runBg(StartupManager::removeFromStartup, removed -> {
                        btnAutoStart.setDisable(false);
                        if (removed) {
                            showAlert("نجاح", "تم حذف التطبيق من التشغيل التلقائي");
                        } else {
                            showAlert("خطأ", "فشل حذف التطبيق من التشغيل التلقائي");
                        }
                        refreshAutoStartLabel();
                    });
                } else {
                    btnAutoStart.setDisable(false);
                }
            } else {
                runBg(() -> StartupManager.addToStartup(appPath), added -> {
                    btnAutoStart.setDisable(false);
                    if (added) {
                        showAlert("نجاح", "تم إضافة التطبيق للتشغيل التلقائي");
                    } else {
                        showAlert("خطأ", "فشل إضافة التطبيق للتشغيل التلقائي");
                    }
                    refreshAutoStartLabel();
                });
            }
        });
    }

    private void refreshAutoStartLabel() {
        runBg(StartupManager::isInStartup, on ->
                btnAutoStart.setText(on ? "إلغاء التشغيل التلقائي" : "تفعيل التشغيل التلقائي"));
    }

    // ══════════════════ Helpers ══════════════════

    private void showAlert(String title, String message) {
        if (title != null && title.contains("خطأ")) {
            SAFNotification.error(message);
        } else {
            SAFNotification.success(message);
        }
    }
}