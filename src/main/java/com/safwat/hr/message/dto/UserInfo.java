package com.safwat.hr.message.dto;

/**
 * =====================================================================
 * UserInfo
 * =====================================================================
 * يمثل بيانات مستخدم للعرض في واجهة المستخدم.
 * يحتوي على اسم المستخدم (username) والاسم المعروض (displayName).
 * يستخدم في البحث عن المستلمين وعرض الـ chips في محرر الرسائل.
 * يدعم المقارنة بناءً على username فقط.
 */
public class UserInfo {
    private final String username;
    private final String displayName;

    /**
     * إنشاء كائن UserInfo جديد.
     *
     * @param username    اسم المستخدم الفريد
     * @param displayName الاسم الحقيقي المعروض
     */
    public UserInfo(String username, String displayName) {
        this.username = username;
        this.displayName = displayName;
    }

    public String getUsername() {
        return username;
    }

    /**
     * ترجع الاسم المعروض إذا كان موجوداً، وإلا ترجع username.
     *
     * @return الاسم المعروض أو username
     */
    public String getDisplayName() {
        return displayName != null && !displayName.isBlank() ? displayName : username;
    }

    /**
     * النص المعروض داخل chip المستلم.
     *
     * @return الاسم الحقيقي فقط
     */
    public String getChipText() {
        return getDisplayName();
    }

    /**
     * النص المعروض في جدول البحث.
     *
     * @return "الاسم الحقيقي (username)"
     */
    public String getSearchDisplay() {
        return getDisplayName() + " (" + username + ")";
    }

    @Override
    public String toString() {
        return getSearchDisplay();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserInfo userInfo = (UserInfo) o;
        return username.equals(userInfo.username);
    }

    @Override
    public int hashCode() {
        return username.hashCode();
    }
}