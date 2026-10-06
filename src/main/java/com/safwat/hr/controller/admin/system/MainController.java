package com.safwat.hr.controller.admin.system;

import com.safwat.hr.shared.AppConfig;
import com.safwat.hr.system.setup.NavigationBus;
import com.safwat.hr.system.setup.PathResolver;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

/**
 * ══════════════════════════════════════════════════════════════════
 * MainController — شاشة الرئيسية داخل لوحة التحكم الموحّدة
 * ══════════════════════════════════════════════════════════════════
 * <p>
 * المسؤوليات:
 * <ul>
 *   <li>عرض/تعديل بيانات جهاز الماستر (PC + Port + alone)</li>
 *   <li>عرض/تعديل مسارات PostgreSQL و Backend</li>
 *   <li>زر "كشف تلقائي" يملأ الحقول من بنية التوزيع</li>
 *   <li>أزرار تنقّل سريع عبر {@link NavigationBus}</li>
 * </ul>
 * <p>
 * <b>مصدر الحقيقة الوحيد</b>: {@link AppConfig} (ملف config/app_config.json).
 * لا يوجد اعتماد على Config القديم.
 */
public class MainController implements Initializable {

    // ══════════════════════════ FXML Fields ══════════════════════════
    @FXML
    private TextField txtAdminPC, txtAdminPort;
    @FXML
    private TextField txtPgFolder;
    @FXML
    private TextField txtPgBinPath;
    @FXML
    private TextField txtPgDataPath;
    @FXML
    private TextField txtBackendPath;

    @FXML
    private Button btnSaveAdminSet, btnSavePaths;
    @FXML
    private Button btnBrowsePgFolder, btnBrowseBackend;
    @FXML
    private Button btnDetectPaths;

    @FXML
    private Button btnOpenPgTab, btnOpenBackendTab;
    @FXML
    private Button btnOpenBackendPropsTab, btnOpenLogsTab;
    @FXML
    private Button btnFixServices;

    @FXML
    private Label lblPgBinStatus, lblPgDataStatus;
    @FXML
    private Label lblPgInfo, lblBackendInfo, lblStatusInfo;
    @FXML
    private Label lblStatus, lblLastUpdate;
    @FXML
    private ProgressIndicator progressIndicator;

    @FXML
    private TextArea txtPgLogs;
    @FXML
    private CheckBox chk_alone;
    @FXML
    private Button btnOpenBackendJsonTab;
    // ══════════════════════════ State ══════════════════════════
    private final Preferences prefs = Preferences.userNodeForPackage(MainController.class);

    // ══════════════════════════ Init ══════════════════════════
    @Override
    public void initialize(URL location, ResourceBundle resources) {

        // ── املأ بيانات الماستر من AppConfig ──
        txtAdminPC.setText(AppConfig.getString("connection", "masterPC", "localhost"));
        txtAdminPort.setText(AppConfig.getString("connection", "port", "8080"));
        chk_alone.setSelected(AppConfig.getBoolean("connection", "alone", true));
        btnOpenBackendJsonTab.setOnAction(e -> NavigationBus.navigate(NavigationBus.BACKEND_JSON));
        loadSavedPaths();
        setupButtons();
        setupBrowseButtons();
        updateInfo();

        // ── تحديث دوري كل 5 ثواني ──
        Thread ticker = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(5000);
                    Platform.runLater(this::updateInfo);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "main-status-ticker");
        ticker.setDaemon(true);
        ticker.start();
    }

    // ══════════════════════════ Path Loading ══════════════════════════

    /**
     * الأولوية:
     * 1) AppConfig.paths (يكتبها SetupWizardService.restoreDefaults)
     * 2) Preferences (توافق خلفي مع النسخ القديمة)
     * 3) الكشف التلقائي من بنية التوزيع
     */
    private void loadSavedPaths() {
        // ── الأولوية 1: AppConfig.paths ──
        String pgRoot = AppConfig.getString("paths", "pgRoot", "");
        String backendExe = AppConfig.getString("paths", "backend", "");

        // ── الأولوية 2: Preferences ──
        if (pgRoot == null || pgRoot.isEmpty())
            pgRoot = prefs.get("pgFolder", "");
        if (backendExe == null || backendExe.isEmpty())
            backendExe = prefs.get("backend", "");

        // ── الأولوية 3: الكشف التلقائي ──
        if (pgRoot.isEmpty() || backendExe.isEmpty()) {
            Optional<PathResolver.Distribution> det = PathResolver.detect();
            if (det.isPresent()) {
                PathResolver.Distribution d = det.get();
                if (pgRoot.isEmpty()) pgRoot = d.pgRoot.toString();
                if (backendExe.isEmpty()) backendExe = d.backendExe().toString();
            }
        }

        if (pgRoot != null && !pgRoot.isEmpty()) {
            txtPgFolder.setText(pgRoot);
            updatePgPaths(pgRoot);
        }
        if (backendExe != null && !backendExe.isEmpty()) {
            txtBackendPath.setText(backendExe);
        }
    }

    // ══════════════════════════ Buttons Wiring ══════════════════════════

    private void setupButtons() {
        btnFixServices.setOnAction(e -> fixBackendServices());
        btnSavePaths.setOnAction(e -> savePaths());
        btnSaveAdminSet.setOnAction(e -> saveAdminSetting());
        btnDetectPaths.setOnAction(e -> onDetectPaths());

        // التنقّل عبر NavigationBus
        btnOpenPgTab.setOnAction(e -> NavigationBus.navigate(NavigationBus.POSTGRESQL));
        btnOpenBackendTab.setOnAction(e -> NavigationBus.navigate(NavigationBus.BACKEND));
        btnOpenBackendPropsTab.setOnAction(e -> NavigationBus.navigate(NavigationBus.BACKEND_PROPERTIES));
        btnOpenLogsTab.setOnAction(e -> NavigationBus.navigate(NavigationBus.LOGS));
    }

    private void setupBrowseButtons() {
        btnBrowsePgFolder.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("اختر المجلد الرئيسي لـ PostgreSQL");

            if (!txtPgFolder.getText().isEmpty()) {
                File init = new File(txtPgFolder.getText());
                if (init.isDirectory()) chooser.setInitialDirectory(init);
            }

            File dir = chooser.showDialog(ownerWindow(btnBrowsePgFolder));
            if (dir != null) {
                txtPgFolder.setText(dir.getAbsolutePath());
                updatePgPaths(dir.getAbsolutePath());
            }
        });

        btnBrowseBackend.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("اختر ملف Backend");

            if (!txtBackendPath.getText().isEmpty()) {
                File init = new File(txtBackendPath.getText());
                File parent = init.getParentFile();
                if (parent != null && parent.isDirectory()) {
                    chooser.setInitialDirectory(parent);
                }
            }

            File file = chooser.showOpenDialog(ownerWindow(btnBrowseBackend));
            if (file != null) {
                txtBackendPath.setText(file.getAbsolutePath());
                AppConfig.setValue("paths", "backend", file.getAbsolutePath());
                updateInfo();
            }
        });
    }

    /**
     * الحصول على الـ Window المالك من الـ Scene بدل حقل stage منفصل.
     * لأن MainController بقى يتحمّل داخل AdminConsoleController.
     */
    private Window ownerWindow(javafx.scene.Node node) {
        if (node == null || node.getScene() == null) return null;
        return node.getScene().getWindow();
    }

    // ══════════════════════════ Actions ══════════════════════════

    /**
     * زر "كشف تلقائي" — يملأ المسارات من بنية التوزيع بدون كتابة مباشرة في الإعدادات.
     */
    private void onDetectPaths() {
        Optional<PathResolver.Distribution> det = PathResolver.detect();
        if (det.isEmpty()) {
            showAlert("خطأ",
                    "لم يتم العثور على بنية التوزيع.\n" +
                            "تأكد من وجود فولدرات pgsql و hr-mate-system بجوار التطبيق.");
            return;
        }
        PathResolver.Distribution d = det.get();

        txtPgFolder.setText(d.pgRoot.toString());
        updatePgPaths(d.pgRoot.toString());
        txtBackendPath.setText(d.backendExe().toString());

        addLog("🔍 تم الكشف التلقائي: " + d.root);
        setStatus("✓ تم كشف المسارات", "ok");
        showAlert("نجاح",
                "تم كشف المسارات:\n" +
                        "• PostgreSQL: " + d.pgRoot + "\n" +
                        "• Backend: " + d.backendExe());
    }

    private void saveAdminSetting() {
        AppConfig.setValue("connection", "masterPC",
                txtAdminPC.getText().isEmpty() ? "localhost" : txtAdminPC.getText());
        AppConfig.setValue("connection", "port",
                txtAdminPort.getText().isEmpty() ? "8080" : txtAdminPort.getText());
        AppConfig.setValue("connection", "alone",
                String.valueOf(chk_alone.isSelected()));

        setStatus("✓ تم حفظ إعدادات الماستر", "ok");
        addLog("💾 تم حفظ إعدادات الماستر");
    }

    private void savePaths() {
        String pgFolder = txtPgFolder.getText();
        if (pgFolder != null && !pgFolder.isEmpty()) {
            prefs.put("pgFolder", pgFolder);
            updatePgPaths(pgFolder);

            AppConfig.setValue("paths", "pgRoot", pgFolder);
            AppConfig.setValue("paths", "pgBin", txtPgBinPath.getText());
            AppConfig.setValue("paths", "pgData", txtPgDataPath.getText());
        }

        String backend = txtBackendPath.getText();
        if (backend != null && !backend.isEmpty()) {
            prefs.put("backend", backend);
            AppConfig.setValue("paths", "backend", backend);
        }

        setStatus("✓ تم حفظ المسارات", "ok");
        addLog("💾 تم حفظ المسارات");
        showAlert("نجاح", "تم حفظ المسارات بنجاح");
        updateInfo();
    }

    private void updatePgPaths(String pgFolder) {
        String binPath = pgFolder + File.separator + "bin";
        String dataPath = pgFolder + File.separator + "data";

        txtPgBinPath.setText(binPath);
        txtPgDataPath.setText(dataPath);

        File binDir = new File(binPath);
        File dataDir = new File(dataPath);

        boolean binOk = binDir.exists();
        boolean dataOk = dataDir.exists();

        lblPgBinStatus.setText(binOk ? "✅" : "❌");
        lblPgBinStatus.getStyleClass().setAll(
                binOk ? "stg-status-msg-ok" : "stg-status-msg-error");

        lblPgDataStatus.setText(dataOk ? "✅" : "❌");
        lblPgDataStatus.getStyleClass().setAll(
                dataOk ? "stg-status-msg-ok" : "stg-status-msg-error");
    }

    // ══════════════════════════ Info Update ══════════════════════════

    private void updateInfo() {
        String pgPath = AppConfig.getString("paths", "pgRoot", "");
        String backendPath = AppConfig.getString("paths", "backend", "");

        lblPgInfo.setText("PostgreSQL: " + (pgPath.isEmpty() ? "غير محدد" : pgPath));
        lblBackendInfo.setText("Backend: " + (backendPath.isEmpty() ? "غير محدد" : backendPath));

        boolean running = BackendService.getInstance().isRunning();
        lblStatusInfo.setText("الحالة: " + (running ? "🟢 النظام يعمل" : "⏹ جاهز للتشغيل"));
        lblStatus.setText(running ? "🟢 يعمل" : "✅ جاهز");
        lblStatus.getStyleClass().setAll("stg-status-badge", "stg-status-badge-ok");

        String time = new java.text.SimpleDateFormat("HH:mm:ss")
                .format(new java.util.Date());
        lblLastUpdate.setText("آخر تحديث: " + time);
    }

    // ══════════════════════════ Logging ══════════════════════════

    private void addLog(String message) {
        AppLogBus.getInstance().log("[Main] " + message);
        Platform.runLater(() -> {
            if (txtPgLogs != null) {
                txtPgLogs.appendText(message + "\n");
            }
        });
    }

    // ══════════════════════════ Fix Services ══════════════════════════

    private void fixBackendServices() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("إصلاح خدمات Backend");
        confirm.setHeaderText("سيتم حذف جميع خدمات Backend المثبتة");
        confirm.setContentText("هل أنت متأكد؟");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        addLog("🔧 حذف جميع خدمات Backend...");
        new Thread(() -> {
            boolean fixed = BackendService.getInstance().fixAllServices();
            Platform.runLater(() -> {
                if (fixed) {
                    addLog("✅ تم حذف جميع خدمات Backend");
                    showAlert("نجاح", "تم حذف جميع خدمات Backend");
                } else {
                    addLog("⚠️ تم حذف بعض الخدمات أو لا توجد خدمات");
                    showAlert("معلومات", "تم حذف الخدمات الموجودة");
                }
                updateInfo();
            });
        }).start();
    }

    // ══════════════════════════ Helpers ══════════════════════════

    private void setStatus(String message, String kind) {
        Platform.runLater(() -> {
            if (lblStatus == null) return;

            lblStatus.setText(message);
            lblStatus.getStyleClass().removeAll(
                    "stg-status-badge", "stg-status-badge-ok",
                    "stg-status-msg", "stg-status-msg-ok",
                    "stg-status-msg-error", "stg-status-msg-warn");

            String css = switch (kind == null ? "" : kind) {
                case "ok" -> "stg-status-badge,stg-status-badge-ok";
                case "error" -> "stg-status-msg-error";
                case "warn" -> "stg-status-msg-warn";
                default -> "stg-status-badge";
            };
            for (String c : css.split(",")) lblStatus.getStyleClass().add(c);
        });
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(
                "خطأ".equals(title) ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}