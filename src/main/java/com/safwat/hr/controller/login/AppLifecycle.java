package com.safwat.hr.controller.login;

import com.safwat.hr.controller.admin.system.AppLogBus;
import com.safwat.hr.controller.admin.system.BackendService;
import com.safwat.hr.controller.admin.system.PostgreSQLService;
import com.safwat.hr.controller.chat.service.ChatService;
import com.safwat.hr.controller.message.service.MessageClientService;
import com.safwat.hr.network.ApiClient;
import com.safwat.hr.network.SessionManager;
import com.safwat.hr.notification.service.ReportWebSocketService;
import com.safwat.hr.shared.AppConfig;
import javafx.application.Platform;
import javafx.stage.Stage;

public class AppLifecycle {

    private AppLifecycle() {
    }

    private static ReportWebSocketService reportWS;

    // في AppLifecycle
    public static ReportWebSocketService getReportWS() {
        return reportWS;
    }

    /**
     * تُستدعى فورًا بعد نجاح تسجيل الدخول (LoginController.openMainWindow()).
     * - رسائل + شات: تشتغل دايمًا، من غير أي شرط.
     * - تقارير: حسب إعداد المستخدم في app_config.json.
     */
    public static void startNotificationServices(Stage primaryStage) {
        // ✅ رسائل — دايمًا
        MessageClientService.getInstance().connect();

        // ✅ شات — دايمًا
        String username = SessionManager.getInstance().getUsername();
        ChatService.getInstance().init(username, err ->
                AppLogBus.getInstance().log("⚠️ خطأ اتصال الشات: " + err)
        );

        // ✅ تقارير — حسب اختيار المستخدم
        if (AppConfig.getBoolean("notifications", "reportsEnabled", true)) {
            reportWS = new ReportWebSocketService(primaryStage);
            reportWS.connect();
        } else {
            AppLogBus.getInstance().log("ℹ️ إشعارات التقارير معطّلة من إعدادات المستخدم");
        }
    }

    /**
     * إيقاف كل اتصالات الإشعارات — تُستدعى من clearSession()
     * (تغطي reLogin و shutdown في نفس الوقت).
     */
    public static void stopNotificationServices() {
        MessageClientService.getInstance().disconnect();
        ChatService.getInstance().shutdown(); // بتقفل الـ ChatStompClient وتنضف المحادثة المفتوحة
        if (reportWS != null) {
            reportWS.disconnect();
            reportWS = null;
        }
    }

    public static void shutdown() {
        AppLogBus.getInstance().log("🔴 بدء إجراء الإغلاق...");
        clearSession();
        stopBackendIfDirect();
        stopPostgresIfDirect();
        AppLogBus.getInstance().log("👋 إغلاق التطبيق");
        Platform.exit();
        System.exit(0);
    }

    public static void clearSession() {
        stopNotificationServices(); // ✅ قفل السوكتات الأول قبل مسح التوكن
        ApiClient.clearAuthToken();
        SessionManager.getInstance().clear();
        AppLogBus.getInstance().log("🔓 تم مسح الجلسة والتوكن");
    }

    private static void stopBackendIfDirect() {
        try {
            BackendService backend = BackendService.getInstance();
            boolean alone = AppConfig.getBoolean("connection", "alone", false);
            if (alone && backend.isRunning()) {
                AppLogBus.getInstance().log("⏹ إيقاف Backend...");
                backend.stop(false);
                AppLogBus.getInstance().log("✅ تم إيقاف Backend");
            }
        } catch (Exception e) {
            AppLogBus.getInstance().log("⚠️ خطأ أثناء إيقاف Backend: " + e.getMessage());
        }
    }

    private static void stopPostgresIfDirect() {
        try {
            PostgreSQLService pg = PostgreSQLService.getInstance();
            boolean alone = AppConfig.getBoolean("connection", "alone", false);
            if (alone && pg.isRunning()) {
                AppLogBus.getInstance().log("⏹ إيقاف PostgreSQL...");
                pg.stop(false);
                AppLogBus.getInstance().log("✅ تم إيقاف PostgreSQL");
            }
        } catch (Exception e) {
            AppLogBus.getInstance().log("⚠️ خطأ أثناء إيقاف PostgreSQL: " + e.getMessage());
        }
    }
}