package com.safwat.hr.system;

import com.safwat.hr.main.Config;
import com.safwat.hr.shared.AppConfig;
import com.safwat.hr.ui.util.TabManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

public class MainController implements Initializable {

    // ===== Path Controls =====
    @FXML
    private TextField txtPgFolder, txtAdminPort, txtAdminPC;
    @FXML
    private TextField txtPgBinPath;
    @FXML
    private TextField txtPgDataPath;
    @FXML
    private TextField txtBackendPath;
    @FXML
    private Button btnBrowsePgFolder, btnSaveAdminSet;
    @FXML
    private Button btnBrowseBackend;
    @FXML
    private Button btnSavePaths;
    @FXML
    private Button btnOpenPgTab;
    @FXML
    private Button btnOpenBackendTab;
    @FXML
    private Button btnOpenLogsTab;
    @FXML
    private Label lblPgBinStatus;
    @FXML
    private Label lblPgDataStatus;
    @FXML
    private Label lblPgInfo;
    @FXML
    private Label lblBackendInfo;
    @FXML
    private Label lblStatusInfo;
    @FXML
    private Label lblStatus;
    @FXML
    private Label lblLastUpdate;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private TabPane mainTabPane;
    @FXML
    private Button btnFixServices;
    private Config config;
    private Preferences prefs;
    private Stage stage;
    @FXML
    private TextArea txtPgLogs;
    @FXML
    private CheckBox chk_alone;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        config = Config.getInstance();
        prefs = Preferences.userNodeForPackage(MainController.class);
        txtAdminPC.setText(AppConfig.getString("connection", "masterPC", "localhost"));
        txtAdminPort.setText(AppConfig.getString("connection", "port", "8080"));
        chk_alone.setSelected((AppConfig.getBoolean("connection", "alone", false)));
        loadSavedPaths();
        setupButtons();
        setupBrowseButtons();
        updateInfo();

        // تحديث الحالة كل 5 ثواني
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

    private void loadSavedPaths() {
        String pgFolder = prefs.get("pgFolder", "");
        txtPgFolder.setText(pgFolder);
        if (!pgFolder.isEmpty()) {
            updatePgPaths(pgFolder);
        }
        txtBackendPath.setText(prefs.get("backend", ""));
    }

    private void saveAdminSetting() {
        AppConfig.setValue("connection", "masterPC", txtAdminPC.getText().isEmpty() ? "localhost" : txtAdminPC.getText());
        AppConfig.setValue("connection", "port", txtAdminPort.getText().isEmpty() ? "8080" : txtAdminPort.getText());
        AppConfig.setValue("connection", "alone", String.valueOf(chk_alone.isSelected()));
    }

    private void savePaths() {
        String pgFolder = txtPgFolder.getText();
        if (!pgFolder.isEmpty()) {
            prefs.put("pgFolder", pgFolder);
            updatePgPaths(pgFolder);
            config.setPgFolder(pgFolder);
        }

        String backend = txtBackendPath.getText();
        if (!backend.isEmpty()) {
            prefs.put("backend", backend);
            config.setBackendPath(backend);
        }

        config.save();
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

        lblPgBinStatus.setText(binDir.exists() ? "✅" : "❌");
        lblPgBinStatus.getStyleClass().setAll(binDir.exists() ? "text-success" : "text-danger");

        lblPgDataStatus.setText(dataDir.exists() ? "✅" : "❌");
        lblPgDataStatus.getStyleClass().setAll(dataDir.exists() ? "text-success" : "text-danger");

        config.setPgBinPath(binPath);
        config.setPgDataPath(dataPath);
    }

    private void updateInfo() {
        String pgPath = config.getPgFolder();
        String backendPath = config.getBackendPath();

        lblPgInfo.setText("PostgreSQL: " + (pgPath.isEmpty() ? "غير محدد" : pgPath));
        lblBackendInfo.setText("Backend: " + (backendPath.isEmpty() ? "غير محدد" : backendPath));
        lblStatusInfo.setText("الحالة: جاهز");

        String time = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
        lblLastUpdate.setText("آخر تحديث: " + time);
    }

    private void setupButtons() {
        btnFixServices.setOnAction(e -> fixBackendServices());
        btnSavePaths.setOnAction(e -> savePaths());
        btnSaveAdminSet.setOnAction(e -> saveAdminSetting());
        // فتح التابات باستخدام TabManager
        btnOpenPgTab.setOnAction(e -> {
            TabManager.loadFXMLInTab(mainTabPane,
                    "/com/safwat/hr/view/system/postgresql.fxml",
                    "PostgreSQL", true);
        });

        btnOpenBackendTab.setOnAction(e -> {
            TabManager.loadFXMLInTab(mainTabPane,
                    "/com/safwat/hr/view/system/backend.fxml",
                    "Backend", true);
        });

        btnOpenLogsTab.setOnAction(e -> {
            TabManager.loadFXMLInTab(mainTabPane,
                    "/com/safwat/hr/view/system/logs.fxml",
                    "📊 السجلات", true);
        });
    }

    private void addLog(String message) {
        // ✅ إرسال للـ Bus الموحّد بدل StringBuilder محلي
        AppLogBus.getInstance().log("[Main] " + message);
        // عرض أيضًا في الـ TextArea المحلي (لو موجود في الشاشة)
        Platform.runLater(() -> {
            if (txtPgLogs != null) {
                txtPgLogs.appendText(message + "\n");
            }
        });
    }

    private void updateLogs() {
        // مش محتاجة بعد الآن — AppLogBus هو المصدر
    }

    private void fixBackendServices() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("إصلاح خدمات Backend");
        confirm.setHeaderText("سيتم حذف جميع خدمات Backend المثبتة");
        confirm.setContentText("هل أنت متأكد؟");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
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
    }

    private void setupBrowseButtons() {
        btnBrowsePgFolder.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("اختر المجلد الرئيسي لـ PostgreSQL");
            if (!txtPgFolder.getText().isEmpty()) {
                chooser.setInitialDirectory(new File(txtPgFolder.getText()));
            }
            File dir = chooser.showDialog(stage);
            if (dir != null) {
                txtPgFolder.setText(dir.getAbsolutePath());
                updatePgPaths(dir.getAbsolutePath());
            }
        });

        btnBrowseBackend.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("اختر ملف Backend");
           
            File file = chooser.showOpenDialog(stage);
            if (file != null) {
                txtBackendPath.setText(file.getAbsolutePath());
                config.setBackendPath(file.getAbsolutePath());
                updateInfo();
            }
        });
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }
}