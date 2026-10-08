package com.safwat.hr.controller.admin.system;

import com.safwat.hr.network.ApiClient;
import com.safwat.hr.network.ApiResponse;
import com.safwat.hr.network.FileTransferClient;
import com.safwat.hr.shared.AppConfig;
import com.safwat.hr.system.setup.PathResolver;
import com.safwat.hr.system.setup.PgCredentialsDialog;
import com.safwat.hr.ui.controls.SAFNotification;
import com.safwat.hr.ui.util.AlertUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.util.Pair;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * شاشة PostgreSQL — ويندوز ولينكس.
 * <ul>
 *   <li>التشغيل المباشر (pg_ctl) متاح على الاتنين.</li>
 *   <li>خدمة النظام (Windows Service) على ويندوز بس، والأزرار بتتخفي على لينكس.</li>
 *   <li>بيانات المستخدم وقت التهيئة من شاشة منبثقة؛ الفاضي بياخد admin / admin.</li>
 *   <li>كل العمليات اللي ممكن تاخد وقت بتتعمل على threads، والـ FX thread بيعرض النتيجة بس.</li>
 * </ul>
 */
public class PostgreSQLController implements Initializable {

    private static final String DEFAULT_DB_NAME = PostgreSQLService.DEFAULT_DB;

    @FXML
    private Label lblPgStatus;
    @FXML
    private Label lblPgPath;
    @FXML
    private Label lblPgData;
    @FXML
    private Label lblPgRunningStatus;
    @FXML
    private Label lblPgPort;
    @FXML
    private Label lblPgUser;
    @FXML
    private Label lblPgDatabase;
    @FXML
    private Label lblInitStatus;
    @FXML
    private TextArea txtPgLogs;

    @FXML
    private Button btnInit;
    @FXML
    private Button btnStart;
    @FXML
    private Button btnStop;
    @FXML
    private Button btnRestart;
    @FXML
    private Button btnInstallService;
    @FXML
    private Button btnDeleteService;
    @FXML
    private Button btnCreateDb;
    @FXML
    private Button btnDropDb;
    @FXML
    private Button btnListDb;
    @FXML
    private CheckBox chkServiceMode;

    // ── عناصر الباك أب ──
    @FXML
    private Button btnBackupNow;
    @FXML
    private Button btnRestoreFile;
    @FXML
    private Label lblBackupStatus;

    private PostgreSQLService pgService;
    private final AtomicBoolean refreshing = new AtomicBoolean(false);

    // ══════════════════ Config Helpers ══════════════════

    private String pgPort() {
        return AppConfig.getString("connection", "pgPort", "5432");
    }

    private String appUser() {
        return AppConfig.getString("connection", "pgUser", PgCredentialsDialog.DEFAULT_USER);
    }

    private String serviceName() {
        return AppConfig.getString("connection", "pgServiceName", "PostgreSQL");
    }

    private String pgBinPath() {
        String v = AppConfig.getString("paths", "pgBin", "");
        if (v != null && !v.isEmpty()) return v;
        return PathResolver.detect().map(d -> d.pgBin().toString()).orElse("");
    }

    private String pgDataPath() {
        String v = AppConfig.getString("paths", "pgData", "");
        if (v != null && !v.isEmpty()) return v;
        return PathResolver.detect().map(d -> d.pgData().toString()).orElse("");
    }

    /** وضع الخدمة مسموح بس على ويندوز. */
    private boolean serviceMode() {
        return OsSupport.WINDOWS && chkServiceMode.isSelected();
    }

    // ══════════════════ Init ══════════════════

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        pgService = PostgreSQLService.getInstance();

        applyPlatformUi();
        setupButtons();
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
        }, "pg-status-ticker");
        t.setDaemon(true);
        t.start();
    }

    /**
     * يحسب الحالة على thread منفصل (pg_ctl status ممكن ياخد ثواني)،
     * والـ FX thread بيعرض النتيجة بس.
     */
    private void refreshStatusAsync() {
        if (!refreshing.compareAndSet(false, true)) return;

        String binPath = pgBinPath();
        String dataPath = pgDataPath();

        Thread t = new Thread(() -> {
            try {
                boolean running = pgService.isRunning();
                boolean initialized = pgService.isInitialized(dataPath);
                Platform.runLater(() -> applyStatus(binPath, dataPath, running, initialized));
            } finally {
                refreshing.set(false);
            }
        }, "pg-status");
        t.setDaemon(true);
        t.start();
    }

    private void applyStatus(String binPath, String dataPath, boolean running, boolean initialized) {
        lblPgPath.setText(binPath.isEmpty() ? "غير محدد" : binPath);
        lblPgData.setText(dataPath.isEmpty() ? "غير محدد" : dataPath);
        lblPgRunningStatus.setText(running ? "🟢 يعمل" : (initialized ? "⏹ جاهز" : "❌ غير مهيأ"));
        lblPgStatus.setText(running ? "🟢 يعمل" : (initialized ? "⏹ متوقف" : "❌ غير مهيأ"));
        lblPgPort.setText(pgPort());
        lblPgUser.setText(appUser());
        lblPgDatabase.setText(DEFAULT_DB_NAME);
    }

    /**
     * لينكس: إخفاء أزرار خدمة النظام. التشغيل المباشر بيفضل متاح.
     */
    private void applyPlatformUi() {
        if (OsSupport.WINDOWS) return;
        hide(btnInstallService);
        hide(btnDeleteService);
        hide(chkServiceMode);
        chkServiceMode.setSelected(false);
        addLog("ℹ️ لينكس: التشغيل المباشر فقط — خدمات النظام غير مدعومة");
    }

    private static void hide(Node n) {
        n.setVisible(false);
        n.setManaged(false);
    }

    private void addLog(String message) {
        AppLogBus.getInstance().log("[PostgreSQL] " + message);
        Platform.runLater(() -> {
            if (txtPgLogs != null) {
                txtPgLogs.appendText(message + "\n");
            }
        });
    }

    private void setupButtons() {
        btnInit.setOnAction(e -> initializeDatabase());
        btnStart.setOnAction(e -> startPostgreSQL());
        btnStop.setOnAction(e -> stopPostgreSQL());
        btnRestart.setOnAction(e -> restartPostgreSQL());
        btnInstallService.setOnAction(e -> installService());
        btnDeleteService.setOnAction(e -> deleteService());
        btnCreateDb.setOnAction(e -> createDatabase());
        btnDropDb.setOnAction(e -> dropDatabase());
        btnListDb.setOnAction(e -> listDatabases());
    }

    // ══════════════════ Initialize ══════════════════

    private void initializeDatabase() {
        String binPath = pgBinPath();
        String dataPath = pgDataPath();

        if (binPath.isEmpty() || dataPath.isEmpty()) {
            showAlert("خطأ", "يرجى تحديد مسار PostgreSQL أولاً");
            return;
        }

        try {
            Path data = Paths.get(dataPath);
            if (Files.isDirectory(data) && hasEntries(data)) {
                showAlert("خطأ",
                        "مجلد البيانات مش فاضي:\n" + data
                                + "\nاحذفه يدوياً أو اختر مسار تاني. التهيئة مابتمسحش بيانات موجودة.");
                return;
            }
        } catch (IOException e) {
            showAlert("خطأ", "تعذر قراءة مجلد البيانات: " + e.getMessage());
            return;
        }

        Optional<Pair<String, String>> creds = PgCredentialsDialog.ask(
                appUser(),
                "تهيئة PostgreSQL — مستخدم",
                "أدخل بيانات مستخدم قاعدة البيانات.\n"
                        + "لو سبت أي حقل فاضي هيتستخدم admin / admin");
        if (creds.isEmpty()) return;

        String user = creds.get().getKey();
        String pass = creds.get().getValue();

        // اسم المستخدم بس بيتحفظ — الباسورد مابيتخزنش
        AppConfig.setValue("connection", "pgUser", user);

        btnInit.setDisable(true);
        lblInitStatus.setText("⏳ جاري التهيئة...");
        addLog("🔄 بدء تهيئة PostgreSQL (المستخدم: " + user + ")...");

        new Thread(() -> {
            boolean success = pgService.initialize(binPath, dataPath, user, pass);
            Platform.runLater(() -> {
                btnInit.setDisable(false);
                if (success) {
                    lblInitStatus.setText("✅ تم التهيئة بنجاح");
                    addLog("✅ تم تهيئة PostgreSQL بنجاح");
                    showAlert("نجاح", "تم تهيئة PostgreSQL وإنشاء قاعدة البيانات");
                } else {
                    lblInitStatus.setText("❌ فشل التهيئة");
                    addLog("❌ فشل تهيئة PostgreSQL — راجع السجل");
                    showAlert("خطأ", "فشل تهيئة PostgreSQL. راجع السجل للتفاصيل.");
                }
                refreshStatusAsync();
            });
        }, "pg-init").start();
    }

    private static boolean hasEntries(Path dir) throws IOException {
        try (Stream<Path> s = Files.list(dir)) {
            return s.findAny().isPresent();
        }
    }

    // ══════════════════ Start / Stop / Restart ══════════════════

    private void startPostgreSQL() {
        String binPath = pgBinPath();
        String dataPath = pgDataPath();
        boolean asService = serviceMode();

        addLog("▶ تشغيل PostgreSQL...");
        new Thread(() -> {
            boolean success = pgService.start(binPath, dataPath, asService);
            Platform.runLater(() -> {
                if (success) {
                    addLog("✅ تم تشغيل PostgreSQL");
                    showAlert("نجاح", "تم تشغيل PostgreSQL");
                } else {
                    addLog("❌ فشل تشغيل PostgreSQL");
                    showAlert("خطأ", "فشل تشغيل PostgreSQL");
                }
                refreshStatusAsync();
            });
        }, "pg-start").start();
    }

    private void stopPostgreSQL() {
        boolean asService = serviceMode();
        addLog("⏹ إيقاف PostgreSQL...");
        new Thread(() -> {
            boolean success = pgService.stop(asService);
            Platform.runLater(() -> {
                if (success) {
                    addLog("✅ تم إيقاف PostgreSQL");
                    showAlert("نجاح", "تم إيقاف PostgreSQL");
                } else {
                    addLog("❌ فشل إيقاف PostgreSQL");
                    showAlert("خطأ", "فشل إيقاف PostgreSQL");
                }
                refreshStatusAsync();
            });
        }, "pg-stop").start();
    }

    private void restartPostgreSQL() {
        String binPath = pgBinPath();
        String dataPath = pgDataPath();
        boolean asService = serviceMode();

        addLog("🔄 إعادة تشغيل PostgreSQL...");
        new Thread(() -> {
            boolean success = pgService.restart(binPath, dataPath, asService);
            Platform.runLater(() -> {
                if (success) {
                    addLog("✅ تم إعادة تشغيل PostgreSQL");
                    showAlert("نجاح", "تم إعادة تشغيل PostgreSQL");
                } else {
                    addLog("❌ فشل إعادة تشغيل PostgreSQL");
                    showAlert("خطأ", "فشل إعادة تشغيل PostgreSQL");
                }
                refreshStatusAsync();
            });
        }, "pg-restart").start();
    }

    // ══════════════════ Service (ويندوز بس) ══════════════════

    private void installService() {
        String binPath = pgBinPath();
        String dataPath = pgDataPath();
        String svc = serviceName();

        addLog("📦 تثبيت خدمة PostgreSQL (" + svc + ")...");
        new Thread(() -> {
            boolean success = pgService.installService(binPath, dataPath, svc);
            Platform.runLater(() -> {
                if (success) {
                    addLog("✅ تم تثبيت خدمة PostgreSQL");
                    showAlert("نجاح", "تم تثبيت خدمة PostgreSQL");
                } else {
                    addLog("❌ فشل تثبيت الخدمة — راجع السجل");
                    showAlert("خطأ", "فشل تثبيت الخدمة");
                }
                refreshStatusAsync();
            });
        }, "pg-install-svc").start();
    }

    private void deleteService() {
        String svc = serviceName();
        addLog("🗑 حذف خدمة PostgreSQL (" + svc + ")...");
        new Thread(() -> {
            boolean success = pgService.deleteService(svc);
            Platform.runLater(() -> {
                if (success) {
                    addLog("✅ تم حذف خدمة PostgreSQL");
                    showAlert("نجاح", "تم حذف خدمة PostgreSQL");
                } else {
                    addLog("❌ فشل حذف الخدمة");
                    showAlert("خطأ", "فشل حذف الخدمة");
                }
                refreshStatusAsync();
            });
        }, "pg-delete-svc").start();
    }

    // ══════════════════ Databases ══════════════════

    private void createDatabase() {
        TextInputDialog dialog = new TextInputDialog(DEFAULT_DB_NAME);
        dialog.setTitle("إنشاء قاعدة بيانات");
        dialog.setHeaderText("أدخل اسم قاعدة البيانات");
        dialog.setContentText("اسم القاعدة:");

        dialog.showAndWait().ifPresent(dbName -> {
            addLog("➕ إنشاء قاعدة بيانات: " + dbName);
            new Thread(() -> {
                boolean success = pgService.createDatabase(dbName);
                Platform.runLater(() -> {
                    if (success) {
                        addLog("✅ تم إنشاء قاعدة البيانات: " + dbName);
                        showAlert("نجاح", "تم إنشاء قاعدة البيانات: " + dbName);
                    } else {
                        addLog("❌ فشل إنشاء قاعدة البيانات");
                        showAlert("خطأ", "فشل إنشاء قاعدة البيانات");
                    }
                    refreshStatusAsync();
                });
            }, "pg-create-db").start();
        });
    }

    private void dropDatabase() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("حذف قاعدة بيانات");
        dialog.setHeaderText("أدخل اسم قاعدة البيانات للحذف");
        dialog.setContentText("اسم القاعدة:");

        dialog.showAndWait().ifPresent(dbName -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("تأكيد");
            confirm.setHeaderText("هل أنت متأكد من حذف قاعدة البيانات '" + dbName + "'؟");
            confirm.setContentText("هذه العملية لا يمكن التراجع عنها.");

            if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                addLog("🗑 حذف قاعدة بيانات: " + dbName);
                new Thread(() -> {
                    boolean success = pgService.dropDatabase(dbName);
                    Platform.runLater(() -> {
                        if (success) {
                            addLog("✅ تم حذف قاعدة البيانات: " + dbName);
                            showAlert("نجاح", "تم حذف قاعدة البيانات: " + dbName);
                        } else {
                            addLog("❌ فشل حذف قاعدة البيانات");
                            showAlert("خطأ", "فشل حذف قاعدة البيانات");
                        }
                        refreshStatusAsync();
                    });
                }, "pg-drop-db").start();
            }
        });
    }

    private void listDatabases() {
        addLog("📋 عرض قواعد البيانات...");
        new Thread(() -> {
            String list = pgService.listDatabases();
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("قواعد البيانات");
                alert.setHeaderText("قائمة قواعد البيانات");
                alert.setContentText(list);
                alert.showAndWait();
                addLog("✅ تم عرض قواعد البيانات");
            });
        }, "pg-list-db").start();
    }

    // ════════════════════════════════════════════════════════════
    //  النسخ الاحتياطي — يستهلك Backend API مباشرة
    // ════════════════════════════════════════════════════════════

    @FXML
    private void handleBackupNow() {
        addLog("💾 طلب نسخة احتياطية...");
        lblBackupStatus.setText("⏳ جاري النسخ الاحتياطي...");
        btnBackupNow.setDisable(true);

        new Thread(() -> {
            try {
                ApiResponse<Object> response = ApiClient.post(
                        "/payroll/backupFull",
                        new HashMap<>(),
                        Object.class
                );
                Platform.runLater(() -> {
                    btnBackupNow.setDisable(false);
                    if (response.isSuccess()) {
                        lblBackupStatus.setText("✅ تم النسخ الاحتياطي بنجاح");
                        addLog("✅ تمت النسخة الاحتياطية بنجاح");
                        showAlert("نجاح", "تم إنشاء النسخة الاحتياطية بنجاح");
                    } else {
                        String msg = response.getMessage() != null ? response.getMessage() : "فشل غير محدد";
                        lblBackupStatus.setText("❌ فشل: " + msg);
                        addLog("❌ فشل النسخ الاحتياطي: " + msg);
                        showAlert("خطأ", "فشل النسخ الاحتياطي:\n" + msg);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    btnBackupNow.setDisable(false);
                    lblBackupStatus.setText("❌ خطأ في الاتصال");
                    addLog("❌ خطأ في النسخ الاحتياطي: " + e.getMessage());
                    showAlert("خطأ", "خطأ في الاتصال: " + e.getMessage());
                });
            }
        }, "pg-backup").start();
    }

    @FXML
    private void handleRestoreFromFile() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("تأكيد الاستعادة");
        confirm.setHeaderText("⚠️ تحذير: سيتم مسح قاعدة البيانات الحالية بالكامل!");
        confirm.setContentText(
                "ستُستعاد قاعدة البيانات من الملف المختار.\n"
                        + "هذه العملية لا يمكن التراجع عنها.\n\n"
                        + "هل أنت متأكد من المتابعة؟");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("اختر ملف النسخة الاحتياطية");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Backup Files", "*.sql", "*.dump", "*.backup"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));

        File file = fileChooser.showOpenDialog(btnRestoreFile.getScene().getWindow());
        if (file == null) return;

        addLog("📂 بدء الاستعادة من ملف: " + file.getName());
        lblBackupStatus.setText("⏳ جاري الاستعادة...");
        btnRestoreFile.setDisable(true);
        btnBackupNow.setDisable(true);

        Path filePath = file.toPath();

        new Thread(() -> {
            try {
                Map<String, Object> formData = new HashMap<>();
                formData.put("data", "{}");
                formData.put("file", filePath);

                ApiResponse<Object> response = FileTransferClient.uploadFile(
                        "/payroll/restore",
                        formData,
                        Object.class
                );

                Platform.runLater(() -> {
                    btnRestoreFile.setDisable(false);
                    btnBackupNow.setDisable(false);
                    if (response.isSuccess()) {
                        lblBackupStatus.setText("✅ تمت الاستعادة بنجاح");
                        addLog("✅ تمت الاستعادة بنجاح من: " + file.getName());
                        showAlert("نجاح", "تمت استعادة قاعدة البيانات بنجاح!");
                    } else {
                        String msg = response.getMessage() != null ? response.getMessage() : "فشل غير محدد";
                        lblBackupStatus.setText("❌ فشل: " + msg);
                        addLog("❌ فشل الاستعادة: " + msg);
                        showAlert("خطأ", "فشل الاستعادة:\n" + msg);
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    btnRestoreFile.setDisable(false);
                    btnBackupNow.setDisable(false);
                    lblBackupStatus.setText("❌ خطأ في الاتصال");
                    addLog("❌ خطأ في الاستعادة: " + e.getMessage());
                    showAlert("خطأ", "خطأ أثناء الاستعادة:\n" + e.getMessage());
                });
            }
        }, "pg-restore").start();
    }

    // ══════════════════ Helpers ══════════════════

    private void showAlert(String title, String message) {
        if (title != null && title.contains("خطأ")) {
            AlertUtil.showError(title,message);
        } else {
            AlertUtil.showInfo(title,message);
        }
    }
}