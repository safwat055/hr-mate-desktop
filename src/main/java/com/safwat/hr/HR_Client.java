package com.safwat.hr;

import com.safwat.hr.auth.dto.LoginRequest;
import com.safwat.hr.auth.dto.LoginResponse;
import com.safwat.hr.auth.service.AuthService;
import com.safwat.hr.message.service.MessageClientService;
import com.safwat.hr.network.ApiClient;
import com.safwat.hr.network.ApiResponse;
import com.safwat.hr.notification.model.HRNotification;
import com.safwat.hr.notification.service.NotificationService;
import com.safwat.hr.notification.service.ReportWebSocketService;
import com.safwat.hr.notification.ui.HRToast;

import com.safwat.hr.shared.AppConfig;
import com.safwat.hr.ui.theme.AppTheme;
import com.safwat.hr.ui.theme.ThemeManager;
import com.safwat.hr.ui.util.FontLoader;   // ← إضافة
import javafx.application.Application;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import javafx.scene.image.Image;
public class HR_Client extends Application {

    private final NotificationService notifService = NotificationService.getInstance();
    private Stage primaryStage;

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void init() throws Exception {
        super.init();

        // ── تحميل الخطوط قبل أي شيء ──────────────────────────────
        // لازم يكون هنا في init() عشان يكون جاهز قبل start()
        FontLoader.load();

        // ── تسجيل الدخول ──────────────────────────────────────────
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("admin");

        ApiResponse<LoginResponse> response = AuthService.login(request);
        if (!response.isSuccess()) {
            throw new RuntimeException(response.getMessage());
        }

        ApiClient.setAuthToken(response.getData().getToken());
        ApiClient.setUserName(response.getData().getUsername());

        // ── الاتصال بـ WebSocket الرسائل (مش محتاج Stage) ──────────
        MessageClientService.getInstance().connect();
    }

    @Override
    public void start(Stage stage) throws IOException {
        this.primaryStage = stage;
        stage.getIcons().add(
                new Image(getClass().getResourceAsStream("/com/safwat/hr/icons/logo.png"))
        );
        FXMLLoader fxmlLoader = new FXMLLoader(
                HR_Client.class.getResource("/com/safwat/hr/view/MainView.fxml"));

        Scene scene = new Scene(fxmlLoader.load());
        AppTheme.apply(scene, AppConfig.getString("ui", "theme", ThemeManager.LIGHT));


        stage.setTitle("HR_Management");
        stage.setScene(scene);
        stage.show();

        // ── الاتصال بـ WebSocket التقارير (بعد ما الـ Stage يبقى موجود) ──
        ReportWebSocketService reportWs = new ReportWebSocketService(stage);
        reportWs.connect();

        stage.setOnCloseRequest(e -> reportWs.disconnect());

        // ── مستمع Toast للإشعارات الجديدة ──────────────────────────
        notifService.getAll().addListener(
                (ListChangeListener<HRNotification>) change -> {
                    while (change.next()) {
                        if (change.wasAdded()) {
                            change.getAddedSubList().forEach(notification -> {
                                if (stage.isShowing()) {
                                    HRToast.show(stage, notification);
                                }
                            });
                        }
                    }
                }
        );
    }
}