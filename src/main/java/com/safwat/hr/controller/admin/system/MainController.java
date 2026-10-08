package com.safwat.hr.controller.admin.system;

import com.safwat.hr.shared.AppConfig;
import com.safwat.hr.system.setup.NavigationBus;
import com.safwat.hr.system.setup.PathResolver;
import com.safwat.hr.ui.util.AlertUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import lombok.SneakyThrows;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.prefs.Preferences;

/**
 * ══════════════════════════════════════════════════════════════════
 * MainController — شاشة الرئيسية داخل لوحة التحكم الموحّدة
 * ══════════════════════════════════════════════════════════════════
 * <ul>
 *   <li>عرض/تعديل بيانات الماستر (PC + Port + alone).</li>
 *   <li>عرض/تعديل مسارات PostgreSQL و Backend.</li>
 *   <li>مسار الباك إند بيتحفظ دايماً كـ launcher فعلي (مش java ولا jar).</li>
 *   <li>زر "كشف تلقائي" بيملأ الحقول من بنية التوزيع (ويندوز ولينكس).</li>
 *   <li>حالة التشغيل بتتحسب على thread منفصل، وخدمات Windows بتتخفي على لينكس.</li>
 * </ul>
 * مصدر الحقيقة: {@link AppConfig}.
 */
public class MainController implements Initializable {

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

    private final Preferences prefs = Preferences.userNodeForPackage(MainController.class);
    private final AtomicBoolean refreshing = new AtomicBoolean(false);

    // ══════════════════════════ Init ══════════════════════════

    @SneakyThrows
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        txtAdminPC.setText(AppConfig.getString("connection", "masterPC", "localhost"));
        txtAdminPort.setText(AppConfig.getString("connection", "port", "8080"));
        chk_alone.setSelected(AppConfig.getBoolean("connection", "alone", true));
        btnOpenBackendJsonTab.setOnAction(e -> NavigationBus.navigate(NavigationBus.BACKEND_JSON));

        loadSavedPaths();
        setupButtons();
        setupBrowseButtons();
        applyPlatformUi();
        updateInfo();

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

    /**
     * ويندوز: كل الأزرار متاحة.
     * لينكس: خدمات Windows بتتخفي، ولو التطبيق شغال بـ root بنحذر إن PostgreSQL مش هيشتغل.
     */
    private void applyPlatformUi() {
        if (OsSupport.WINDOWS) return;
        hide(btnFixServices);
        addLog("ℹ️ لينكس: إصلاح خدمات Backend غير متاح (خدمات Windows بس)");
        if (OsSupport.isRootUser()) {
            addLog("⚠️ التطبيق شغال بصلاحيات root — PostgreSQL مش هيشتغل. شغّله بمستخدم عادي.");
        }
    }

    private static void hide(Node n) {
        n.setVisible(false);
        n.setManaged(false);
    }

    // ══════════════════════════ Path Helpers ══════════════════════════

    /**
     * يحوّل أي مسار (جذر التوزيع، الـ launcher، java، jar) للـ launcher الفعلي.
     * لو ملقيناش، بيرجع المسار زي ما هو.
     */
    private static String normalizeBackend(String raw) {
        if (raw == null || raw.isBlank()) return "";
        Path launcher = BackendService.resolveLauncher(raw.trim());
        return launcher != null ? launcher.toString() : raw.trim();
    }

    /** الـ launcher من بنية التوزيع المكتشفة (ويندوز ولينكس). */
    private static String launcherOf(PathResolver.Distribution d) {
        Path launcher = BackendService.resolveLauncher(d.backend.toString());
        return launcher != null ? launcher.toString() : d.backendExe().toString();
    }

    // ══════════════════════════ Path Loading ══════════════════════════

    /**
     * الأولوية: 1) AppConfig.paths  2) Preferences (توافق خلفي)  3) الكشف التلقائي.
     * لو المسار المحفوظ قديم (java أو jar أو جذر)، بيتصحح للـ launcher ويتحفظ.
     */
    private void loadSavedPaths() throws IOException {
        String pgRoot = AppConfig.getString("paths", "pgRoot", "");
        String backendRaw = AppConfig.getString("paths", "backend", "");

        if (pgRoot == null || pgRoot.isEmpty()) pgRoot = prefs.get("pgFolder", "");
        if (backendRaw == null || backendRaw.isEmpty()) backendRaw = prefs.get("backend", "");

        if (pgRoot.isEmpty() || backendRaw.isEmpty()) {
            Optional<PathResolver.Distribution> det = PathResolver.detect();
            if (det.isPresent()) {
                PathResolver.Distribution d = det.get();
                if (pgRoot.isEmpty()) pgRoot = d.pgRoot.toString();
                if (backendRaw.isEmpty()) backendRaw = launcherOf(d);
            }
        }

        if (!pgRoot.isEmpty()) {
            txtPgFolder.setText(pgRoot);
            updatePgPaths(pgRoot);
        }

        if (!backendRaw.isEmpty()) {
            String backend = normalizeBackend(backendRaw);
            txtBackendPath.setText(backend);
            if (!backend.equals(backendRaw.trim())) {
                AppConfig.setValue("paths", "backend", backend);
                addLog("🔧 تم تصحيح مسار Backend للـ launcher: " + backend);
            }
        }
    }

    // ══════════════════════════ Buttons Wiring ══════════════════════════

    private void setupButtons() throws IOException{
        btnFixServices.setOnAction(e -> fixBackendServices());
        btnSavePaths.setOnAction(e -> savePaths());
        btnSaveAdminSet.setOnAction(e -> saveAdminSetting());
        btnDetectPaths.setOnAction(e -> onDetectPaths());

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
            chooser.setTitle("اختر launcher الباك إند (hr-mate-system)");

            if (!txtBackendPath.getText().isEmpty()) {
                File init = new File(txtBackendPath.getText());
                File parent = init.getParentFile();
                if (parent != null && parent.isDirectory()) {
                    chooser.setInitialDirectory(parent);
                }
            }

            File file = chooser.showOpenDialog(ownerWindow(btnBrowseBackend));
            if (file == null) return;

            Path launcher = BackendService.resolveLauncher(file.getAbsolutePath());
            if (launcher == null) {
                showAlert("خطأ",
                        "الملف ده مش launcher الباك إند ولا جزء من توزيعه.\n"
                                + "اختار hr-mate-system (على ويندوز hr-mate-system.exe).");
                return;
            }

            txtBackendPath.setText(launcher.toString());
            AppConfig.setValue("paths", "backend", launcher.toString());
            updateInfo();
        });
    }

    private Window ownerWindow(Node node) {
        if (node == null || node.getScene() == null) return null;
        return node.getScene().getWindow();
    }

    // ══════════════════════════ Actions ══════════════════════════

    private void onDetectPaths() {
        Optional<PathResolver.Distribution> det = PathResolver.detect();
        if (det.isEmpty()) {
            showAlert("خطأ",
                    "لم يتم العثور على بنية التوزيع.\n"
                            + "تأكد من وجود فولدرات pgsql و hr-mate-system بجوار التطبيق.");
            return;
        }
        PathResolver.Distribution d = det.get();

        String pg = d.pgRoot.toString();
        String backend = launcherOf(d);

        txtPgFolder.setText(pg);
        updatePgPaths(pg);
        txtBackendPath.setText(backend);

        addLog("🔍 تم الكشف التلقائي: " + d.root);
        setStatus("✓ تم كشف المسارات", "ok");
        showAlert("نجاح",
                "تم كشف المسارات:\n"
                        + "• PostgreSQL: " + pg + "\n"
                        + "• Backend: " + backend);
    }

    private void saveAdminSetting() {
        AppConfig.setValue("connection", "masterPC",
                txtAdminPC.getText().isEmpty() ? "localhost" : txtAdminPC.getText());
        AppConfig.setValue("connection", "port",
                txtAdminPort.getText().isEmpty() ? "8080" : txtAdminPort.getText());
        AppConfig.setValue("connection", "alone", String.valueOf(chk_alone.isSelected()));

        setStatus("✓ تم حفظ إعدادات الماستر", "ok");
        addLog("💾 تم حفظ إعدادات الماستر");
    }
    @SneakyThrows
    private void savePaths() {
        String pgFolder = txtPgFolder.getText();
        if (pgFolder != null && !pgFolder.isEmpty()) {
            prefs.put("pgFolder", pgFolder);
            updatePgPaths(pgFolder);

            AppConfig.setValue("paths", "pgRoot", pgFolder);
            AppConfig.setValue("paths", "pgBin", txtPgBinPath.getText());
            AppConfig.setValue("paths", "pgData", txtPgDataPath.getText());
        }

        String backend = normalizeBackend(txtBackendPath.getText());
        if (!backend.isEmpty()) {
            txtBackendPath.setText(backend);
            prefs.put("backend", backend);
            AppConfig.setValue("paths", "backend", backend);

            if (!Files.isRegularFile(Paths.get(backend))) {
                addLog("⚠️ الـ launcher المحفوظ مش موجود: " + backend);
            } else {
                // ⚠️ إصلاح: نضمن وجود فولدرات التخزين على طول (لينكس بالذات)
                try {
                    Path launcher = Paths.get(backend);
                    Path home = com.safwat.hr.system.setup.PathResolver.homeOfLauncher(launcher);
                    Path storage = home.resolve("app");
                    Files.createDirectories(storage.resolve("config"));
                    Files.createDirectories(storage.resolve("logs"));
                    Files.createDirectories(storage.resolve("data"));

                    // لينكس: صلاحيات التنفيذ للـ launcher وللـ runtime
                    if (!OsSupport.WINDOWS) {
                        launcher.toFile().setExecutable(true, false);
                        setExecRecursive(home.resolve("bin"));
                        setExecRecursive(home.resolve("lib").resolve("runtime").resolve("bin"));
                    }
                } catch (Exception ex) {
                    addLog("⚠️ تعذر تجهيز فولدرات التخزين: " + ex.getMessage());
                }
            }
        }

        setStatus("✓ تم حفظ المسارات", "ok");
        addLog("💾 تم حفظ المسارات");
        showAlert("نجاح", "تم حفظ المسارات بنجاح");
        updateInfo();
    }

    /** صلاحيات تنفيذ متكررة لملفات فولدر (لينكس بس). */
    private static void setExecRecursive(Path dir) {
        if (dir == null || !Files.isDirectory(dir)) return;
        try (var walk = Files.walk(dir, 4)) {
            walk.filter(Files::isRegularFile)
                    .forEach(p -> p.toFile().setExecutable(true, false));
        } catch (Exception ignored) {
        }
    }

    /**
     * يحدد مسارات bin و data من مجلد PostgreSQL، ويعرض حالة وجودها.
     * على لينكس: الملفات بتتعمل لها صلاحية تنفيذ (الـ zip بيضيعها غالباً).
     */
    @SneakyThrows
    private void updatePgPaths(String pgFolder)  {
        String binPath = pgFolder + File.separator + "bin";
        String dataPath = pgFolder + File.separator + "data";

        txtPgBinPath.setText(binPath);
        txtPgDataPath.setText(dataPath);

        boolean binOk = new File(binPath, OsSupport.exe("pg_ctl")).isFile();
        boolean dataOk = new File(dataPath).isDirectory();

        if (binOk && !OsSupport.WINDOWS) {
            OsSupport.makeExecutable(Paths.get(binPath));
        }

        lblPgBinStatus.setText(binOk ? "✅" : "❌");
        lblPgBinStatus.getStyleClass().setAll(
                binOk ? "stg-status-msg-ok" : "stg-status-msg-error");

        lblPgDataStatus.setText(dataOk ? "✅" : "❌");
        lblPgDataStatus.getStyleClass().setAll(
                dataOk ? "stg-status-msg-ok" : "stg-status-msg-error");
    }

    // ══════════════════════════ Info Update ══════════════════════════

    /**
     * المعلومات الثابتة بتتحدث فوراً. حالة التشغيل بتتحسب على thread منفصل.
     */
    private void updateInfo() {
        String pgPath = AppConfig.getString("paths", "pgRoot", "");
        String backendPath = AppConfig.getString("paths", "backend", "");

        lblPgInfo.setText("PostgreSQL: " + (pgPath.isEmpty() ? "غير محدد" : pgPath));
        lblBackendInfo.setText("Backend: " + (backendPath.isEmpty() ? "غير محدد" : backendPath));
        lblLastUpdate.setText("آخر تحديث: " + new SimpleDateFormat("HH:mm:ss").format(new Date()));

        refreshRunningStatusAsync();
    }

    private void refreshRunningStatusAsync() {
        if (!refreshing.compareAndSet(false, true)) return;

        Thread t = new Thread(() -> {
            try {
                boolean running = BackendService.getInstance().isRunning();
                Platform.runLater(() -> applyRunningStatus(running));
            } finally {
                refreshing.set(false);
            }
        }, "main-running-status");
        t.setDaemon(true);
        t.start();
    }

    /**
     * حالة التشغيل بتتعرض في lblStatusInfo بس، و lblStatus بيفضل مخصص لرسائل العملية.
     */
    private void applyRunningStatus(boolean running) {
        lblStatusInfo.setText("الحالة: " + (running ? "🟢 النظام يعمل" : "⏹ جاهز للتشغيل"));
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

    // ══════════════════════════ Fix Services (ويندوز بس) ══════════════════════════

    private void fixBackendServices() {
        if (!OsSupport.WINDOWS) {
            showAlert("معلومات", "إصلاح الخدمات متاح على ويندوز فقط");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("إصلاح خدمات Backend");
        confirm.setHeaderText("سيتم حذف جميع خدمات Backend المثبتة");
        confirm.setContentText("هل أنت متأكد؟");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        addLog("🔧 حذف جميع خدمات Backend...");
        btnFixServices.setDisable(true);

        Thread t = new Thread(() -> {
            boolean fixed = BackendService.getInstance().fixAllServices();
            Platform.runLater(() -> {
                btnFixServices.setDisable(false);
                if (fixed) {
                    addLog("✅ تم حذف جميع خدمات Backend");
                    showAlert("نجاح", "تم حذف جميع خدمات Backend");
                } else {
                    addLog("⚠️ تم حذف بعض الخدمات أو لا توجد خدمات");
                    showAlert("معلومات", "تم حذف الخدمات الموجودة");
                }
                updateInfo();
            });
        }, "main-fix-services");
        t.setDaemon(true);
        t.start();
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
        if (title != null && title.contains("خطأ")) {
            AlertUtil.showError(title, message);
        } else {
            AlertUtil.showInfo(title, message);
        }
    }
}