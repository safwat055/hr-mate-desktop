package com.safwat.hr;

import com.safwat.hr.shared.AppConfig;
import javafx.application.Application;

public class Launcher {
    public static void main(String[] args) {
        AppConfig.ensureInitialized();
        Application.launch(HR_Client.class, args);
    }
}
