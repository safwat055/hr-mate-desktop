package com.safwat.hr.controller.login;

import com.safwat.hr.controller.admin.system.AdminConsoleController;
import com.safwat.hr.controller.admin.system.AppLogBus;
import com.safwat.hr.controller.admin.system.BackendService;
import com.safwat.hr.controller.admin.system.PostgreSQLService;
import com.safwat.hr.controller.appearance.ColorSettingsManager;
import com.safwat.hr.network.ApiClient;
import com.safwat.hr.network.ApiResponse;
import com.safwat.hr.network.SessionManager;
import com.safwat.hr.network.auth.dto.LoginRequest;
import com.safwat.hr.network.auth.dto.LoginResponse;
import com.safwat.hr.network.auth.service.AuthService;
import com.safwat.hr.shared.AppConfig;
import com.safwat.hr.system.setup.PathResolver;
import com.safwat.hr.ui.controls.SAFNotification;
import com.safwat.hr.ui.theme.SettingsThemeLoader;
import com.safwat.hr.ui.theme.ThemeEventBus;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    private String clientVersion = "v1.2.1";

    @FXML
    private Button btn_cancel;
    @FXML
    private Button btn_login;
    @FXML
    private Button btn_togglePassword;

    @FXML
    private Label lbl_info;
    @FXML
    private Label lbl_version;

    @FXML
    private PasswordField txt_password;
    @FXML
    private TextField txt_password_visible;
    @FXML
    private TextField txt_userName;

    @FXML
    private ProgressBar progressIndicator;

    private LoginRequest request = new LoginRequest();

    /**
     * هل كلمة المرور ظاهرة الآن؟
     */
    private boolean passwordVisible = false;

    // ── ثوابت وضع Standalone ──
    private static final int HEALTH_POLL_INTERVAL_MS = 1000;
    private static final int HEALTH_TIMEOUT_ATTEMPTS = 30;

    // ── كلاسات الحالة لـ lbl_info ──
    private static final String STYLE_INFO = "stg-login-info";
    private static final String STYLE_ERROR = "stg-login-info-error";
    private static final String STYLE_SUCCESS = "stg-login-info-success";

    // ══════════════════ Config Helpers ══════════════════

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

    private String backendPath() {
        String v = AppConfig.getString("paths", "backend", "");
        if (v != null && !v.isEmpty()) return v;
        return PathResolver.detect().map(d -> d.backendExe().toString()).orElse("");
    }

    // ══════════════════ Init ══════════════════

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        SettingsThemeLoader.apply(btn_cancel);
        lbl_version.setText(clientVersion);

        // حد أقصى لطول اسم المستخدم (بديل textLimit في MaterialFX)
        txt_userName.setTextFormatter(new TextFormatter<>(change ->
                change.getControlNewText().length() <= 50 ? change : null));

        String lastUser = AppConfig.getString("connection", "user", "");
        if (!lastUser.isEmpty()) {
            txt_userName.setText(lastUser);
            txt_password.requestFocus();
        }

        btn_login.setOnAction(e -> handleLogin());
        btn_cancel.setOnAction(e -> confirmAndExit());

        // ── مزامنة الحقلين (مخفي / مرئي) ──
        txt_password.textProperty().addListener((obs, old, nv) -> {
            if (!passwordVisible) txt_password_visible.setText(nv);
            validateFields();
        });
        txt_password_visible.textProperty().addListener((obs, old, nv) -> {
            if (passwordVisible) txt_password.setText(nv);
            validateFields();
        });

        txt_userName.textProperty().addListener((obs, old, nv) -> validateFields());

        validateFields();

        // ── زر إظهار/إخفاء كلمة المرور ──
        btn_togglePassword.setOnAction(e -> togglePasswordVisibility());

        // ── Enter navigation ──
        txt_password.setOnAction(e -> handleLogin());
        txt_password_visible.setOnAction(e -> handleLogin());
        txt_userName.setOnAction(e -> txt_password.requestFocus());

        Platform.runLater(this::setupKeyboardShortcut);
    }

    // ── اختصارات لوحة المفاتيح ──

    private void setupKeyboardShortcut() {
        Scene scene = txt_userName.getScene();
        if (scene == null) scene = btn_login.getScene();
        if (scene == null) return;

        scene.setOnKeyPressed(event -> {
            if (event.isControlDown() && event.isShiftDown() && event.getCode() == KeyCode.S) {
                openServiceManager();
                event.consume();
            }
        });
    }

    private void openServiceManager() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/safwat/hr/controller/admin/system/AdminConsole.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.initStyle(StageStyle.DECORATED);
            stage.setTitle("🔧 إدارة خدمات التطبيق");
            stage.setScene(new Scene(root));
            stage.setMinWidth(800);
            stage.setMinHeight(650);

            AdminConsoleController controller = loader.getController();
            controller.setStage(stage);

            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            SAFNotification.error("فشل فتح إدارة الخدمات: " + e.getMessage());
        }
    }

    // ── التحقق من الحقول ──

    private void validateFields() {
        boolean isValid = !txt_userName.getText().trim().isEmpty()
                && !getPassword().isEmpty();
        btn_login.setDisable(!isValid);
    }

    /**
     * القيمة الحالية لكلمة المرور (سواء كانت مخفية أو مرئية)
     */
    private String getPassword() {
        return passwordVisible ? txt_password_visible.getText() : txt_password.getText();
    }

    // ── التبديل بين المخفي / المرئي ──

    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;

        if (passwordVisible) {
            txt_password_visible.setText(txt_password.getText());
            txt_password_visible.setVisible(true);
            txt_password_visible.setManaged(true);

            txt_password.setVisible(false);
            txt_password.setManaged(false);

            btn_togglePassword.setText("🙈");
            txt_password_visible.requestFocus();
            txt_password_visible.positionCaret(txt_password_visible.getText().length());
        } else {
            txt_password.setText(txt_password_visible.getText());
            txt_password.setVisible(true);
            txt_password.setManaged(true);

            txt_password_visible.setVisible(false);
            txt_password_visible.setManaged(false);

            btn_togglePassword.setText("👁");
            txt_password.requestFocus();
            txt_password.positionCaret(txt_password.getText().length());
        }
    }

    // ════════════════════════════════════════════════════════════
    //  handleLogin — المدخل الرئيسي
    // ════════════════════════════════════════════════════════════

    private void handleLogin() {
        String username = txt_userName.getText().trim();
        String password = getPassword();

        if (username.isEmpty() || password.isEmpty()) {
            showError("❌ الرجاء إدخال اسم المستخدم وكلمة المرور");
            return;
        }

        clearInfo();
        setUiEnabled(false);
        request.setUsername(username);
        request.setPassword(password);

        boolean standaloneMode = AppConfig.getBoolean("connection", "alone", false);

        if (standaloneMode) {
            new Thread(() -> doStandaloneLogin(username)).start();
        } else {
            new Thread(() -> doDirectLogin(username)).start();
        }
    }

    // ════════════════════════════════════════════════════════════
    //  Standalone Login Flow
    // ════════════════════════════════════════════════════════════

    private void doStandaloneLogin(String username) {
        updateInfo("⏳ فحص PostgreSQL...");
        PostgreSQLService pgService = PostgreSQLService.getInstance();

        if (!pgService.isRunning()) {
            String pgBin = pgBinPath();
            String pgData = pgDataPath();
            boolean pgAsService = AppConfig.getBoolean("connection", "pgAsService", false);

            if (!pgAsService && (pgBin.isEmpty() || pgData.isEmpty())) {
                showError("❌ مسار PostgreSQL غير محدد — افتح إدارة الخدمات (Ctrl+Shift+S) واضغط 'إعادة الافتراضي'");
                setUiEnabled(true);
                return;
            }

            updateInfo("⏳ جاري تشغيل PostgreSQL...");
            AppLogBus.getInstance().log("[Login] تشغيل PostgreSQL في وضع Standalone");

            boolean pgOk = pgService.start(pgBin, pgData, pgAsService);
            if (!pgOk) {
                showError("❌ فشل تشغيل PostgreSQL — تحقق من الإعدادات (Ctrl+Shift+S)");
                setUiEnabled(true);
                return;
            }
            sleep(2000);
        }

        updateInfo("⏳ فحص Backend...");
        BackendService backendService = BackendService.getInstance();

        if (!backendService.isRunning()) {
            String bePath = backendPath();
            boolean backendAsService = AppConfig.getBoolean("connection", "backendAsService", false);

            if (!backendAsService && bePath.isEmpty()) {
                showError("❌ مسار Backend غير محدد — افتح إدارة الخدمات (Ctrl+Shift+S) واضغط 'إعادة الافتراضي'");
                setUiEnabled(true);
                return;
            }

            updateInfo("⏳ جاري تشغيل Backend...");
            AppLogBus.getInstance().log("[Login] تشغيل Backend في وضع Standalone");

            boolean bkOk = backendService.start(bePath, backendAsService);
            if (!bkOk) {
                showError("❌ فشل تشغيل Backend — تحقق من الإعدادات (Ctrl+Shift+S)");
                setUiEnabled(true);
                return;
            }
        }

        updateInfo("⏳ انتظار جاهزية Backend...");
        boolean backendReady = waitForHealth();
        if (!backendReady) {
            showError("⏰ انتهى وقت الانتظار — لم يستجب Backend خلال 30 ثانية");
            setUiEnabled(true);
            return;
        }

        updateInfo("⏳ جاري تسجيل الدخول...");
        doDirectLogin(username);
    }

    // ════════════════════════════════════════════════════════════
    //  Direct Login
    // ════════════════════════════════════════════════════════════

    private void doDirectLogin(String username) {
        try {
            ApiResponse<LoginResponse> response = AuthService.login(request);

            if (!response.isSuccess()) {
                String msg = response.getMessage() != null
                        ? response.getMessage()
                        : "فشل تسجيل الدخول — تحقق من البيانات والاتصال";
                showError("❌ " + msg);
                setUiEnabled(true);
                return;
            }

            AppConfig.setValue("connection", "user", username);
            ApiClient.setAuthToken(response.getData().getToken());
            SessionManager.getInstance().setUsername(response.getData().getUsername());
            SessionManager.getInstance().setDisplayName(response.getData().getDisplayName());

            AppLogBus.getInstance().log("[Login] ✅ تسجيل دخول ناجح: " + username);

            showSuccess("✅ تسجيل الدخول ناجح — جاري الفتح...");

            Platform.runLater(() -> {
                openMainWindow();
                closeLoginWindow();
            });

        } catch (Exception e) {
            AppLogBus.getInstance().log("[Login] ❌ خطأ في تسجيل الدخول: " + e.getMessage());
            showError("❌ خطأ في الاتصال: " + e.getMessage());
            setUiEnabled(true);
        }
    }

    // ════════════════════════════════════════════════════════════
    //  Health Polling
    // ════════════════════════════════════════════════════════════

    private boolean waitForHealth() {
        String healthUrl = ApiClient.getBaseUrl() + "/actuator/health";
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();

        for (int i = 0; i < HEALTH_TIMEOUT_ATTEMPTS; i++) {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(healthUrl))
                        .GET()
                        .timeout(Duration.ofSeconds(3))
                        .build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) {
                    AppLogBus.getInstance().log("[Login] ✅ Backend جاهز بعد " + (i + 1) + " ثانية");
                    return true;
                }
            } catch (Exception ignored) {
            }
            int attempt = i + 1;
            updateInfo("⏳ انتظار Backend... (" + attempt + "/" + HEALTH_TIMEOUT_ATTEMPTS + ")");
            sleep(HEALTH_POLL_INTERVAL_MS);
        }
        return false;
    }

    // ════════════════════════════════════════════════════════════
    //  Helpers
    // ════════════════════════════════════════════════════════════

    private void setUiEnabled(boolean enabled) {
        Platform.runLater(() -> {
            btn_login.setDisable(!enabled);
            progressIndicator.setVisible(!enabled);
            if (enabled) {
                progressIndicator.setProgress(0);
            } else {
                progressIndicator.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
            }
        });
    }

    // ── Info / Error / Success — المساحة محجوزة في الـ FXML ──

    private void updateInfo(String message) {
        Platform.runLater(() -> {
            lbl_info.setText(message);
            lbl_info.setVisible(true);
            applyInfoStyle(STYLE_INFO);
        });
    }

    private void showInfo(String message) {
        Platform.runLater(() -> {
            lbl_info.setText(message);
            lbl_info.setVisible(true);
            applyInfoStyle(STYLE_INFO);
        });
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            lbl_info.setText(message);
            lbl_info.setVisible(true);
            applyInfoStyle(STYLE_ERROR);
        });
    }

    private void showSuccess(String message) {
        Platform.runLater(() -> {
            lbl_info.setText(message);
            lbl_info.setVisible(true);
            applyInfoStyle(STYLE_SUCCESS);
        });
    }

    private void clearInfo() {
        Platform.runLater(() -> {
            lbl_info.setText("");
            lbl_info.setVisible(false);
        });
    }

    private void applyInfoStyle(String activeClass) {
        lbl_info.getStyleClass().removeAll(STYLE_INFO, STYLE_ERROR, STYLE_SUCCESS);
        lbl_info.getStyleClass().add(STYLE_INFO);
        lbl_info.getStyleClass().add(activeClass);
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }

    private void openMainWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/safwat/hr/controller/login/MainView.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("HR MATE");
            Scene scene = new Scene(root, 1200, 800);
            stage.setScene(scene);
            stage.setMaximized(true);

            ThemeEventBus.initFromConfig();
            ThemeEventBus.register(scene);
            ThemeEventBus.applyTheme(scene, ThemeEventBus.getCurrentTheme());
            ColorSettingsManager.attachTheme(scene,
                    AppConfig.getString("ui", "theme", ThemeEventBus.LIGHT));
            stage.show();
            AppLifecycle.startNotificationServices(stage);

        } catch (Exception e) {
            e.printStackTrace();
            SAFNotification.error(e.getMessage());
        }
    }

    private void closeLoginWindow() {
        Stage stage = (Stage) btn_login.getScene().getWindow();
        if (stage != null) stage.close();
    }

    private void confirmAndExit() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("إغلاق البرنامج");
        alert.setHeaderText("إغلاق HR MATE");
        alert.setContentText("هل أنت متأكد من الخروج من البرنامج؟");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            AppLifecycle.shutdown();
        }
    }
}