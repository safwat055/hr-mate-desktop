package com.safwat.hr.main;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safwat.hr.auth.dto.LoginRequest;
import com.safwat.hr.auth.dto.LoginResponse;
import com.safwat.hr.auth.service.AuthService;
import com.safwat.hr.auth.session.SessionManager;
import com.safwat.hr.main.AppLifecycle;
import com.safwat.hr.network.ApiClient;
import com.safwat.hr.network.ApiResponse;
import com.safwat.hr.shared.AppConfig;
import com.safwat.hr.system.AppLogBus;
import com.safwat.hr.system.BackendService;
import com.safwat.hr.system.MainController;
import com.safwat.hr.system.PostgreSQLService;
import com.safwat.hr.ui.controls.SAFNotification;
import com.safwat.hr.ui.theme.AppTheme;
import com.safwat.hr.ui.theme.ThemeManager;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXPasswordField;
import io.github.palexdev.materialfx.controls.MFXProgressBar;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
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

    @FXML private MFXButton btn_cancel;
    @FXML private MFXButton btn_login;
    @FXML private Label lbl_info;
    @FXML private MFXPasswordField txt_password;
    @FXML private MFXTextField txt_userName;
    @FXML private MFXProgressBar progressIndicator;

    LoginRequest request = new LoginRequest();

    // ── ثوابت وضع Standalone ──
    private static final int  HEALTH_POLL_INTERVAL_MS = 1000; // كل ثانية
    private static final int  HEALTH_TIMEOUT_ATTEMPTS  = 30;  // 30 ثانية timeout

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // ✅ تحميل آخر مستخدم
        String lastUser = AppConfig.getString("connection", "user", "");
        String lastUser2 = AppConfig.getString("connection", "user", "admin");
        if (!lastUser.isEmpty()) {
            txt_userName.setText(lastUser);
            txt_password.requestFocus();
            txt_password.setText(lastUser2);
        }

        btn_login.setOnAction(e -> handleLogin());
        btn_cancel.setOnAction(e -> confirmAndExit());

        txt_userName.textProperty().addListener((obs, old, nv) -> validateFields());
        txt_password.textProperty().addListener((obs, old, nv) -> validateFields());
        validateFields();

        txt_password.setOnAction(e -> handleLogin());
        txt_userName.setOnAction(e -> txt_password.requestFocus());

        progressIndicator.setVisible(false);

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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/safwat/hr/view/system/main.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.initStyle(StageStyle.DECORATED);
            stage.setTitle("🔧 إدارة خدمات التطبيق");
            stage.setScene(new Scene(root));
            stage.setMinWidth(800);
            stage.setMinHeight(650);

            MainController controller = loader.getController();
            controller.setStage(stage);

            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
            AppTheme.apply(root, AppConfig.getString("ui", "theme", ThemeManager.LIGHT));
        } catch (IOException e) {
            e.printStackTrace();
            SAFNotification.error("فشل فتح إدارة الخدمات: " + e.getMessage());
        }
    }

    private void validateFields() {
        boolean isValid = !txt_userName.getText().trim().isEmpty()
                && !txt_password.getText().isEmpty();
        btn_login.setDisable(!isValid);
    }

    // ════════════════════════════════════════════════════════════
    //  handleLogin — المدخل الرئيسي
    // ════════════════════════════════════════════════════════════

    private void handleLogin() {
        String username = txt_userName.getText().trim();
        String password = txt_password.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showInfo("الرجاء إدخال اسم المستخدم وكلمة المرور");
            return;
        }

        // تعطيل الواجهة
        setUiEnabled(false);
        request.setUsername(username);
        request.setPassword(password);

        boolean standaloneMode = AppConfig.getBoolean("connection", "alone", false);

        if (standaloneMode) {
            // ── وضع Standalone: تشغيل الخدمات أولاً ──
            new Thread(() -> doStandaloneLogin(username)).start();
        } else {
            // ── وضع Client/Server العادي ──
            new Thread(() -> doDirectLogin(username)).start();
        }
    }

    // ════════════════════════════════════════════════════════════
    //  Standalone Login Flow
    // ════════════════════════════════════════════════════════════

    private void doStandaloneLogin(String username) {
        // 1. تشغيل PostgreSQL لو مش شغّال
        updateInfo("⏳ فحص PostgreSQL...");
        PostgreSQLService pgService = PostgreSQLService.getInstance();
        if (!pgService.isRunning()) {
            updateInfo("⏳ جاري تشغيل PostgreSQL...");
            AppLogBus.getInstance().log("[Login] تشغيل PostgreSQL في وضع Standalone");

            boolean pgAsService = AppConfig.getBoolean("connection", "pgAsService", false);
            String pgBin  = AppConfig.getString("connection", "pgBinPath", "");
            String pgData = AppConfig.getString("connection", "pgDataPath", "");

            boolean pgOk = pgService.start(pgBin, pgData, pgAsService);
            if (!pgOk) {
                showError("❌ فشل تشغيل PostgreSQL — تحقق من الإعدادات (Ctrl+Shift+S)");
                setUiEnabled(true);
                return;
            }
            // انتظر قليلاً للـ PostgreSQL يستقر
            sleep(2000);
        }

        // 2. تشغيل Backend لو مش شغّال
        updateInfo("⏳ فحص Backend...");
        BackendService backendService = BackendService.getInstance();
        if (!backendService.isRunning()) {
            updateInfo("⏳ جاري تشغيل Backend...");
            AppLogBus.getInstance().log("[Login] تشغيل Backend في وضع Standalone");

            boolean backendAsService = AppConfig.getBoolean("connection", "backendAsService", false);
            String backendPath = AppConfig.getString("connection", "backendPath", "");

            boolean bkOk = backendService.start(backendPath, backendAsService);
            if (!bkOk) {
                showError("❌ فشل تشغيل Backend — تحقق من الإعدادات (Ctrl+Shift+S)");
                setUiEnabled(true);
                return;
            }
        }

        // 3. Polling على /actuator/health لحد ما يرد أو Timeout
        updateInfo("⏳ انتظار جاهزية Backend...");
        boolean backendReady = waitForHealth();
        if (!backendReady) {
            showError("⏰ انتهى وقت الانتظار — لم يستجب Backend خلال 30 ثانية");
            setUiEnabled(true);
            return;
        }

        // 4. Backend جاهز → تسجيل الدخول الفعلي
        updateInfo("⏳ جاري تسجيل الدخول...");
        doDirectLogin(username);
    }

    // ════════════════════════════════════════════════════════════
    //  Direct Login (الـ API call الفعلي)
    // ════════════════════════════════════════════════════════════

    private void doDirectLogin(String username) {
        try {
            ApiResponse<LoginResponse> response = AuthService.login(request);

            if (!response.isSuccess()) {
                // ✅ رسالة خطأ واضحة — لا RuntimeException
                String msg = response.getMessage() != null
                        ? response.getMessage()
                        : "فشل تسجيل الدخول — تحقق من البيانات والاتصال";
                showError(msg);
                setUiEnabled(true);
                return;
            }

            // ✅ نجاح — حفظ التوكن ثم فتح النافذة الرئيسية
            AppConfig.setValue("connection", "user", username);
            ApiClient.setAuthToken(response.getData().getToken());
            ApiClient.setUserName(response.getData().getUsername());
            AppLogBus.getInstance().log("[Login] ✅ تسجيل دخول ناجح: " + username);

            Platform.runLater(() -> {
                openMainWindow();
                closeLoginWindow();
            });

        } catch (Exception e) {
            AppLogBus.getInstance().log("[Login] ❌ خطأ في تسجيل الدخول: " + e.getMessage());
            showError("خطأ في الاتصال: " + e.getMessage());
            setUiEnabled(true);
        }
    }

    // ════════════════════════════════════════════════════════════
    //  Health Polling
    // ════════════════════════════════════════════════════════════

    /**
     * Polling على GET /actuator/health كل ثانية، حتى 30 محاولة.
     * يعيد true لما يرجع 200.
     */
    private boolean waitForHealth() {
        String healthUrl = ApiClient.url + ApiClient.masterPC + ":" + ApiClient.port + "/actuator/health";
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
                // Backend لسه بيشتغل — استمر في الانتظار
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
                lbl_info.setVisible(false);
            } else {
                progressIndicator.setProgress(-1); // indeterminate
            }
        });
    }

    private void updateInfo(String message) {
        Platform.runLater(() -> {
            lbl_info.setText(message);
            lbl_info.setVisible(true);
        });
    }

    private void showInfo(String message) {
        Platform.runLater(() -> {
            lbl_info.setText(message);
            lbl_info.setVisible(true);
        });
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            lbl_info.setText(message);
            lbl_info.setVisible(true);
        });
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    private void openMainWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/safwat/hr/view/MainView.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("HR MATE");
            stage.setScene(new Scene(root, 1200, 800));
            stage.setMaximized(true);
            AppTheme.apply(root, AppConfig.getString("ui", "theme", ThemeManager.LIGHT));
            stage.show();

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
            // ✅ AppLifecycle بدل Platform.exit() المباشر
            AppLifecycle.shutdown();
        }
    }
}