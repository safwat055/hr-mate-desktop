package com.safwat.hr.network.dto;

import java.util.Set;

/**
 * DTOs لإدارة المستخدمين — مطابقة لـ AdminDtos في الباك ايند (com.safwat.hr.hrPublic.user.dto)
 * Jackson بيقرا بالأسماء مباشرة (records-style getters مش هتنفع هنا، عشان كده POJO عادي).
 */
public final class AdminUserDtos {

    private AdminUserDtos() {
    }

    /**
     * GET /api/admin/permissions → List<PermissionDto>
     */
    public static class PermissionDto {
        private Long id;
        private String name;   // "ADMIN"
        private String label;  // "مدير النظام"

        public PermissionDto() {
        }

        public PermissionDto(Long id, String name, String label) {
            this.id = id;
            this.name = name;
            this.label = label;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
    }

    /**
     * GET /api/admin/users → List<UserResponse>
     */
    public static class UserResponse {
        private Long id;
        private String username;
        private String displayName;
        private String jobTitle;
        private boolean active;
        private Set<PermissionDto> permissions;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getJobTitle() {
            return jobTitle;
        }

        public void setJobTitle(String jobTitle) {
            this.jobTitle = jobTitle;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public Set<PermissionDto> getPermissions() {
            return permissions;
        }

        public void setPermissions(Set<PermissionDto> permissions) {
            this.permissions = permissions;
        }
    }

    /**
     * POST /api/admin/users
     */
    public static class CreateUserRequest {
        private String username;
        private String password;
        private String displayName;
        private String jobTitle;
        private Set<Long> permissionIds;

        public CreateUserRequest() {
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getJobTitle() {
            return jobTitle;
        }

        public void setJobTitle(String jobTitle) {
            this.jobTitle = jobTitle;
        }

        public Set<Long> getPermissionIds() {
            return permissionIds;
        }

        public void setPermissionIds(Set<Long> permissionIds) {
            this.permissionIds = permissionIds;
        }
    }

    /**
     * PUT /api/admin/users/{id}/permissions
     */
    public static class UpdatePermissionsRequest {
        private Set<Long> permissionIds;

        public UpdatePermissionsRequest() {
        }

        public UpdatePermissionsRequest(Set<Long> permissionIds) {
            this.permissionIds = permissionIds;
        }

        public Set<Long> getPermissionIds() {
            return permissionIds;
        }

        public void setPermissionIds(Set<Long> permissionIds) {
            this.permissionIds = permissionIds;
        }
    }

    /**
     * POST /api/admin/users/{id}/reset-password
     */
    public static class ResetPasswordRequest {
        private String newPassword;

        public ResetPasswordRequest() {
        }

        public ResetPasswordRequest(String newPassword) {
            this.newPassword = newPassword;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }

    /**
     * POST /api/auth/change-password
     */
    public static class ChangePasswordRequest {
        private String currentPassword;
        private String newPassword;

        public ChangePasswordRequest() {
        }

        public ChangePasswordRequest(String currentPassword, String newPassword) {
            this.currentPassword = currentPassword;
            this.newPassword = newPassword;
        }

        public String getCurrentPassword() {
            return currentPassword;
        }

        public void setCurrentPassword(String currentPassword) {
            this.currentPassword = currentPassword;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }
}