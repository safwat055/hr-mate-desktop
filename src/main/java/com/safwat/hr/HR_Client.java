package com.safwat.hr;

import com.safwat.hr.service.auth.dto.LoginRequest;
import com.safwat.hr.service.auth.dto.LoginResponse;
import com.safwat.hr.service.auth.service.AuthService;
import com.safwat.hr.utils.ApiResponse;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HR_Client extends Application {
    @Override
    public void start(Stage stage) throws IOException {


        FXMLLoader fxmlLoader = new FXMLLoader(HR_Client.class.getResource("/com/safwat/hr/controller/MainView.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("HR_Management");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void init() throws Exception {

        super.init();

        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("admin");

        ApiResponse<LoginResponse> response =
                AuthService.login(request);
        System.out.println("Success : " + response.isSuccess());
        System.out.println("Message : " + response.getMessage());
        System.out.println("Data    : " + response.getData());

        if (response.getData() != null) {
            System.out.println("Token : " + response.getData().getToken());
        }
        if (!response.isSuccess()) {
            throw new RuntimeException(response.getMessage());
        }
    }
}
