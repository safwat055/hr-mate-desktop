package com.safwat.hr.network;

import com.safwat.hr.controller.message.dto.UserInfo;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Setter
@Getter
public class SessionManager {
    private static SessionManager instance;
    private String token;
    private String username;
    private String userRole;          // legacy — يفضّل الاعتماد على permissions
    private String fullName;
    // ✅ جديد: بيانات العرض والصلاحيات (بتتعبّى من LoginResponse الموسّع)
    private String displayName;
    private String jobTitle;
    private Set<String> permissions = new HashSet<>();

    private SessionManager() {
    }

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    // ==================== Login/Logout ====================

    public void login(String token, UserInfo user) {
        this.token = token;
        this.username = user.getUsername();
        this.fullName = user.getDisplayName();
    }

    public void login(String token, String username, String role) {
        this.token = token;
        this.username = username;
        this.userRole = role;
        this.fullName = null;
    }

    /**
     * ✅ تعبئة الجلسة الكاملة بعد Login ناجح (LoginResponse الموسّع)
     */
    public void login(String token, String username, String displayName,
                      String jobTitle, Set<String> permissions) {
        this.token = token;
        this.username = username;
        this.displayName = displayName;
        this.fullName = displayName;
        this.jobTitle = jobTitle;
        this.permissions = permissions != null ? permissions : new HashSet<>();
    }

    /**
     * مسح الجلسة (تسجيل الخروج / إعادة تسجيل الدخول)
     */
    public void clear() {
        this.token = null;
        this.username = null;
        this.userRole = null;
        this.fullName = null;
        this.displayName = null;
        this.jobTitle = null;
        this.permissions = new HashSet<>();
    }

    // ==================== Check Methods ====================

    public boolean isLoggedIn() {
        return token != null && !token.isEmpty() && username != null;
    }

    /**
     * ✅ فحص الصلاحية بالاسم — "ADMIN" / "HR_MANAGER" / ...
     */
    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }

    public boolean isAdmin() {
        return hasPermission("ADMIN") || "ADMIN".equals(userRole);
    }

    // legacy helpers — ممكن تمسحهم بعد ما الكل ينتقل لـ hasPermission
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
                ", permissions=" + permissions +
                ", isLoggedIn=" + isLoggedIn() +
                '}';
    }
}