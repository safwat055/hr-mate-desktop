package com.safwat.hr.service.auth.session;

import com.safwat.hr.service.auth.dto.LoginResponse;
import com.safwat.hr.utils.ApiClient;
import lombok.Getter;

public final class SessionManager {

    /**
     * -- GETTER --
     *  الحصول على المستخدم الحالي.
     */
    @Getter
    private static LoginResponse currentUser;

    private SessionManager() {
    }

    /**
     * حفظ بيانات المستخدم الحالية.
     */
    public static void login(LoginResponse response) {

        currentUser = response;

        if (response != null) {
            ApiClient.setAuthToken(response.getToken());
        }
    }

    /**
     * إنهاء الجلسة.
     */
    public static void logout() {

        currentUser = null;
        ApiClient.clearAuthToken();

    }

    /**
     * هل يوجد مستخدم قام بتسجيل الدخول؟
     */
    public static boolean isLoggedIn() {

        return currentUser != null
                && currentUser.getToken() != null
                && !currentUser.getToken().isBlank();

    }

    /**
     * الحصول على الـ JWT.
     */
    public static String getToken() {

        return currentUser == null
                ? null
                : currentUser.getToken();

    }

    /**
     * اسم المستخدم الحالي.
     */
    public static String getUsername() {

        return currentUser == null
                ? null
                : currentUser.getUsername();

    }

}