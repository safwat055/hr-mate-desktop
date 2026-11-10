package com.safwat.hr;

import com.safwat.hr.controller.appearance.ColorSettingsManager;
import com.safwat.hr.notification.service.NotificationService;
import com.safwat.hr.shared.AppConfig;
import com.safwat.hr.ui.theme.ThemeEventBus;
import com.safwat.hr.ui.util.FontLoader;   // ← إضافة
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.Objects;

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

    }

    @Override
    public void start(Stage stage) throws IOException {
        this.primaryStage = stage;
        stage.initStyle(StageStyle.UNDECORATED);

        stage.getIcons().add(
                new Image(Objects.requireNonNull(getClass().getResourceAsStream("/com/safwat/hr/icons/logo.png")))
        );
        FXMLLoader fxmlLoader = new FXMLLoader(
                HR_Client.class.getResource("/com/safwat/hr/controller/login/Login.fxml"));

        Scene scene = new Scene(fxmlLoader.load());
        ThemeEventBus.applyTheme(scene, AppConfig.getString("ui", "theme", ThemeEventBus.LIGHT));
        ColorSettingsManager.attachTheme(scene, AppConfig.getString("ui", "theme", ThemeEventBus.LIGHT));

        stage.setTitle("HR_MATE");
        stage.setScene(scene);
        stage.show();

    }
}