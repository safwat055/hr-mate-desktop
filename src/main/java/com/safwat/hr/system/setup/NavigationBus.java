package com.safwat.hr.system.setup;

import java.util.function.Consumer;

/**
 * ناقل التنقل الموحّد بين الشاشات.
 * <p>
 * MainController يستخدم {@link #navigate(String)} لطلب الانتقال لتاب معيّن،
 * و AdminConsoleController يسجّل نفسه بـ {@link #register} ويستجيب للطلب.
 */
public final class NavigationBus {

    /**
     * مفاتيح التنقل المعروفة
     */
    public static final String HOME = "home";
    public static final String POSTGRESQL = "pg";
    public static final String BACKEND = "backend";
    public static final String LOGS = "logs";
    public static final String BACKEND_PROPERTIES = "backend-props";
    public static final String BACKEND_JSON = "backend-json";
    private static volatile Consumer<String> handler;

    private NavigationBus() {
    }

    public static void register(Consumer<String> h) {
        handler = h;
    }

    public static void navigate(String key) {
        var h = handler;
        if (h != null) h.accept(key);
    }
}