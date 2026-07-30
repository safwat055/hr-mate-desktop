package com.safwat.hr;

import com.safwat.hr.model.message.service.MessageClientService;
import com.safwat.hr.notification.model.HRNotification;
import com.safwat.hr.notification.service.NotificationService;
import com.safwat.hr.notification.ui.HRToast;
import com.safwat.hr.service.auth.dto.LoginRequest;
import com.safwat.hr.service.auth.dto.LoginResponse;
import com.safwat.hr.service.auth.service.AuthService;
import com.safwat.hr.ui.util.AppTheme;
import com.safwat.hr.utils.ApiClient;
import com.safwat.hr.utils.ApiResponse;
import javafx.application.Application;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HR_Client extends Application {
    private final NotificationService notifService = NotificationService.getInstance();

    @Override
    public void start(Stage stage) throws IOException {

        FXMLLoader fxmlLoader = new FXMLLoader(HR_Client.class.getResource("/com/safwat/hr/controller/MainView.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        AppTheme.apply(scene);
        stage.setTitle("HR_Management");
        stage.setScene(scene);

        stage.show();
// ✅ إضافة مستمع للإشعارات الجديدة

        NotificationService.getInstance().getAll().addListener(
                (ListChangeListener<HRNotification>) change -> {
                    while (change.next()) {
                        if (change.wasAdded()) {
                            change.getAddedSubList().forEach(notification -> {
                                // عرض Toast
                                HRToast.show(stage, notification);
                            });
                        }
                    }
                }
        );


    }


    @Override
    public void init() throws Exception {

        super.init();

        LoginRequest request = new LoginRequest();
        request.setUsername("safwat055");
        request.setPassword("safwat055");

        ApiResponse<LoginResponse> response =
                AuthService.login(request);

        if (!response.isSuccess()) {
            throw new RuntimeException(response.getMessage());
        }

        ApiClient.setAuthToken(response.getData().getToken());
        ApiClient.setUserName(response.getData().getUsername());
        MessageClientService.getInstance().connect();


    }


}
