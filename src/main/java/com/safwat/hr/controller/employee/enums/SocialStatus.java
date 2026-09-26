package com.safwat.hr.controller.employee.enums;

/**
 * القائمة ثابتة قانونيًا ومحدودة، فهي enum في الكود وليست جدول DB منفصل.
 * الـ code (اسم الـ enum) هو مصدر الحقيقة اللي بيستخدمه محرك الرواتب،
 * والـ labelAr للعرض في الواجهة بس.
 */
public enum SocialStatus {
    SINGLE("أعزب"),
    MARRIED("متزوج"),
    MARRIED_1_CHILD("متزوج + طفل"),
    MARRIED_2_CHILDREN("متزوج + طفلين أو أكثر"),
    WIDOWED("أرمل"),
    WIDOWED_1_CHILD("أرمل + طفل"),
    WIDOWED_2_CHILDREN("أرمل + طفلين أو أكثر"),
    DIVORCED("مطلق"),
    DIVORCED_1_CHILD("مطلق + طفل"),
    DIVORCED_2_CHILDREN("مطلق + طفلين أو أكثر");

    private final String labelAr;

    SocialStatus(String labelAr) {
        this.labelAr = labelAr;
    }

    public String getLabelAr() {
        return labelAr;
    }
}