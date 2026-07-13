package com.safwat.hr.service.auth.service;

import com.safwat.hr.service.auth.dto.LoginRequest;
import com.safwat.hr.service.auth.dto.LoginResponse;
import com.safwat.hr.service.auth.session.SessionManager;

import com.safwat.hr.shared.ApiEndpoints;
import com.safwat.hr.utils.ApiClient;
import com.safwat.hr.utils.ApiResponse;

import java.io.IOException;

public final class AuthService {

    private static final String AUTH_URL = "/auth";

    private AuthService() {
    }

    /**
     * تسجيل الدخول.
     */
    public static ApiResponse<LoginResponse> login(LoginRequest request) {

        try {

            ApiResponse<LoginResponse> response =
                    ApiClient.post(
                            ApiEndpoints.AUTH + "/login",
                            request,
                            LoginResponse.class
                    );

            if (response.isSuccess() && response.getData() != null) {
                SessionManager.login(response.getData());
            }

            return response;

        } catch (IOException | InterruptedException ex) {

            ApiResponse<LoginResponse> response = new ApiResponse<>();
            response.setSuccess(false);
            response.setMessage(ex.getMessage());

            return response;
        }
    }

    /**
     * إنهاء الجلسة.
     */
    public static void logout() {
        SessionManager.logout();
    }

    /**
     * هل يوجد مستخدم مسجل دخول؟
     */
    public static boolean isLoggedIn() {
        return SessionManager.isLoggedIn();
    }

    /**
     * المستخدم الحالي.
     */
    public static LoginResponse currentUser() {
        return SessionManager.getCurrentUser();
    }

}