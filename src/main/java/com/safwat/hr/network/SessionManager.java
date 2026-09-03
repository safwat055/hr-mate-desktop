package com.safwat.hr.network;


import com.safwat.hr.controller.message.dto.UserInfo;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SessionManager {
    private static SessionManager instance;
    private String token;
    private String username;
    private String userRole;
    private String fullName;

    private SessionManager() {
    }

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    // ==================== Getters ====================

    // ==================== Setters ====================

    // ==================== Login/Logout ====================

    /**
     * ✅ Logs in a user and stores session data
     */
    public void login(String token, UserInfo user) {
        this.token = token;
        this.username = user.getUsername();
        this.fullName = user.getDisplayName();

    }

    /**
     * ✅ Overloaded login method with separate parameters
     */
    public void login(String token, String username, String role) {
        this.token = token;
        this.username = username;
        this.userRole = role;
        this.fullName = null;

    }

    /**
     * ✅ مسح الجلسة (تسجيل الخروج)
     */
    public void clear() {
        String oldUser = username;
        this.token = null;
        this.username = null;
        this.userRole = null;
        this.fullName = null;

    }

    // ==================== Check Methods ====================

    public boolean isLoggedIn() {
        return token != null && !token.isEmpty() && username != null;
    }

    public boolean isAdmin() {
        return userRole != null && userRole.equals("ADMIN");
    }

    public boolean isViewer() {
        return userRole != null && userRole.equals("VIEWER");
    }

    public boolean isUser() {
        return userRole != null && userRole.equals("USER");
    }

    // ==================== Helper ====================

    public void setSession(String token, String username, String userRole, String fullName) {
        this.token = token;
        this.username = username;
        this.userRole = userRole;
        this.fullName = fullName;

    }

    @Override
    public String toString() {
        return "SessionManager{" +
                "username='" + username + '\'' +
                ", userRole='" + userRole + '\'' +
                ", isLoggedIn=" + isLoggedIn() +
                '}';
    }
}