package com.safwat.hr.system;

import com.safwat.hr.main.Config;
import com.safwat.hr.network.ApiClient;
import com.safwat.hr.network.ApiResponse;
import com.safwat.hr.system.AppLogBus;
import com.safwat.hr.ui.util.AlertUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class PostgreSQLController implements Initializable {

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
    @FXML private Button btnBackupNow;
    @FXML private Button btnRestoreFile;
    @FXML private Label  lblBackupStatus;

    private PostgreSQLService pgService;
    private Config config;
    private StringBuilder logs = new StringBuilder();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        config = Config.getInstance();
        pgService = PostgreSQLService.getInstance();

        updateInfo();
        setupButtons();
        disableServiceControlsOnLinux();

        // تحديث كل 5 ثواني
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
        String binPath = config.getPgBinPath();
        String dataPath = config.getPgDataPath();
        boolean running = pgService.isRunning();
        boolean initialized = pgService.isInitialized(dataPath);

        lblPgPath.setText(binPath.isEmpty() ? "غير محدد" : binPath);

        lblPgData.setText(dataPath.isEmpty() ? "غير محدد" : dataPath);
        lblPgRunningStatus.setText(running ? "🟢 يعمل" : (initialized ? "⏹ جاهز" : "❌ غير مهيأ"));
        lblPgStatus.setText(running ? "🟢 يعمل" : (initialized ? "⏹ متوقف" : "❌ غير مهيأ"));
        lblPgPort.setText(config.getPgPort() != null ? config.getPgPort() : "5432");
        lblPgUser.setText("admin");
        lblPgDatabase.setText("hr_db");

        updateLogs();
    }

    private void updateLogs() {
        // مش محتاجة — AppLogBus هو المصدر
    }

    private void addLog(String message) {
        // ✅ AppLogBus الموحّد
        AppLogBus.getInstance().log("[PostgreSQL] " + message);
        // عرض في الـ TextArea المحلي أيضًا
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

    private void initializeDatabase() {
        String binPath = config.getPgBinPath();
        String dataPath = config.getPgDataPath();

        if (binPath.isEmpty() || dataPath.isEmpty()) {
            showAlert("خطأ", "يرجى تحديد مسار PostgreSQL أولاً");
            return;
        }
        if (new File(dataPath).exists()) {
            if (!AlertUtil.showConfirmation("تحذير", "فولدر داتا موجود بالفعل في حال الاستمرار ستفقد كل قواعد البيانات " + "\n" + "للاستمرار اضغط موافق")) {

            }
        }
        btnInit.setDisable(true);
        lblInitStatus.setText("⏳ جاري التهيئة...");
        addLog("🔄 بدء تهيئة PostgreSQL...");

        new Thread(() -> {
            boolean success = pgService.initialize(binPath, dataPath, "admin", "admin");
            Platform.runLater(() -> {
                btnInit.setDisable(false);
                if (success) {
                    lblInitStatus.setText("✅ تم التهيئة بنجاح");
                    addLog("✅ تم تهيئة PostgreSQL بنجاح");
                    showAlert("نجاح", "تم تهيئة PostgreSQL وإنشاء قاعدة البيانات");
                } else {
                    lblInitStatus.setText("❌ فشل التهيئة");
                    addLog("❌ فشل تهيئة PostgreSQL");
                    showAlert("خطأ", "فشل تهيئة PostgreSQL");
                }
                updateInfo();
            });
        }).start();
    }

    private void startPostgreSQL() {
        String binPath = config.getPgBinPath();
        String dataPath = config.getPgDataPath();
        boolean asService = chkServiceMode.isSelected();

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
                updateInfo();
            });
        }).start();
    }

    private void stopPostgreSQL() {
        boolean asService = chkServiceMode.isSelected();
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
                updateInfo();
            });
        }).start();
    }

    private void restartPostgreSQL() {
        String binPath = config.getPgBinPath();
        String dataPath = config.getPgDataPath();
        boolean asService = chkServiceMode.isSelected();

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
                updateInfo();
            });
        }).start();
    }

    private void installService() {
        String binPath = config.getPgBinPath();
        String dataPath = config.getPgDataPath();

        addLog("📦 تثبيت خدمة PostgreSQL...");
        new Thread(() -> {
            boolean success = pgService.installService(binPath, dataPath, "PostgreSQL");
            Platform.runLater(() -> {
                if (success) {
                    addLog("✅ تم تثبيت خدمة PostgreSQL");
                    showAlert("نجاح", "تم تثبيت خدمة PostgreSQL");
                } else {
                    addLog("❌ فشل تثبيت الخدمة");
                    showAlert("خطأ", "فشل تثبيت الخدمة");
                }
                updateInfo();
            });
        }).start();
    }

    private void deleteService() {
        addLog("❌ حذف خدمة PostgreSQL...");
        new Thread(() -> {
            boolean success = pgService.deleteService("PostgreSQL");
            Platform.runLater(() -> {
                if (success) {
                    addLog("✅ تم حذف خدمة PostgreSQL");
                    showAlert("نجاح", "تم حذف خدمة PostgreSQL");
                } else {
                    addLog("❌ فشل حذف الخدمة");
                    showAlert("خطأ", "فشل حذف الخدمة");
                }
                updateInfo();
            });
        }).start();
    }

    private void createDatabase() {
        TextInputDialog dialog = new TextInputDialog("hr_db");
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
                    updateInfo();
                });
            }).start();
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
                        updateInfo();
                    });
                }).start();
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
        }).start();
    }

    // ════════════════════════════════════════════════════════════
    //  النسخ الاحتياطي (2.7) — يستهلك Backend API مباشرة
    // ════════════════════════════════════════════════════════════

    /**
     * نسخة احتياطية فورية — POST /api/payroll/backupFull
     */
    @FXML
    private void handleBackupNow() {
        addLog("💾 طلب نسخة احتياطية...");
        lblBackupStatus.setText("⏳ جاري النسخ الاحتياطي...");
        btnBackupNow.setDisable(true);

        new Thread(() -> {
            try {
                // PayrollRequest فارغ — الباك اند بيتعامل مع القيم الافتراضية
                ApiResponse<Object> response = ApiClient.post(
                        "/payroll/backupFull",
                        new java.util.HashMap<>(),
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
        }).start();
    }

    /**
     * استعادة من ملف — FileChooser + POST /api/payroll/restore (multipart)
     */
    @FXML
    private void handleRestoreFromFile() {
        // تأكيد إضافي — عملية خطيرة
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("تأكيد الاستعادة");
        confirm.setHeaderText("⚠️ تحذير: سيتم مسح قاعدة البيانات الحالية بالكامل!");
        confirm.setContentText(
                "ستُستعاد قاعدة البيانات من الملف المختار.\n" +
                        "هذه العملية لا يمكن التراجع عنها.\n\n" +
                        "هل أنت متأكد من المتابعة؟"
        );

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        // اختيار الملف
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("اختر ملف النسخة الاحتياطية");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Backup Files", "*.sql", "*.dump", "*.backup"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        java.io.File file = fileChooser.showOpenDialog(btnRestoreFile.getScene().getWindow());
        if (file == null) return;

        addLog("📂 بدء الاستعادة من ملف: " + file.getName());
        lblBackupStatus.setText("⏳ جاري الاستعادة...");
        btnRestoreFile.setDisable(true);
        btnBackupNow.setDisable(true);

        java.nio.file.Path filePath = file.toPath();

        new Thread(() -> {
            try {
                // ملاحظة: الـ part "data" بيحتوي على PayrollRequest — نبعته كـ JSON فارغ
                // والباك اند بيستخدم hr_db Hardcoded في PayrollController.restore()
                java.util.Map<String, Object> formData = new java.util.HashMap<>();
                formData.put("data", "{}");   // PayrollRequest فارغ كـ JSON string
                formData.put("file", filePath);

                ApiResponse<Object> response = ApiClient.uploadFile(
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
        }).start();
    }

    /**
     * على Linux: تعطيل أزرار الخدمة (Windows-only).
     * Init/Start/Stop/Restart وقواعد البيانات تفضل متاحة.
     */
    private void disableServiceControlsOnLinux() {
        if (!System.getProperty("os.name", "").toLowerCase().contains("windows")) {
            Platform.runLater(() -> {
                btnInstallService.setDisable(true);
                btnInstallService.setVisible(false);
                btnDeleteService.setDisable(true);
                btnDeleteService.setVisible(false);
                chkServiceMode.setSelected(false);
                chkServiceMode.setDisable(true);
                chkServiceMode.setVisible(false);
                com.safwat.hr.shared.AppConfig.setValue("connection", "pgAsService", "false");
                addLog("ℹ️ Linux: تم تعطيل أزرار خدمة PostgreSQL — التشغيل المباشر فقط");
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