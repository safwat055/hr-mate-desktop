package com.safwat.hr.main;

import com.safwat.hr.network.ApiClient;
import com.safwat.hr.network.SessionManager;
import com.safwat.hr.system.AppLogBus;
import com.safwat.hr.system.BackendService;
import com.safwat.hr.system.PostgreSQLService;
import javafx.application.Platform;

/**
 * منطق الإغلاق الموحّد للتطبيق.
 * <p>
 * يُستدعى من:
 * - MainViewController.logout()
 * - stage.setOnCloseRequest() على نافذة MainView (زرار X)
 * - LoginController.confirmAndExit()
 *
 * <p>
 * ملاحظة: reLogin() لا يستدعي shutdown() — هو بيمسح الجلسة بس
 * ويرجّع لـ Login.fxml من غير إيقاف أي خدمة.
 */
public class AppLifecycle {

    private AppLifecycle() {}

    /**
     * إغلاق التطبيق بالكامل مع إيقاف أي خدمة مباشرة (non-service) شغّالة.
     * الخدمات المسجّلة كـ Windows Service تفضل شغّالة — دي مسؤولية الـ OS.
     */
    public static void shutdown() {
        AppLogBus.getInstance().log("🔴 بدء إجراء الإغلاق...");

        // 1. مسح الجلسة والتوكن
        clearSession();

        // 2. إيقاف الـ Backend لو شغّال مباشر (مش خدمة)
        stopBackendIfDirect();

        // 3. إيقاف PostgreSQL لو شغّال مباشر (مش خدمة)
        stopPostgresIfDirect();

        // 4. إغلاق فعلي
        AppLogBus.getInstance().log("👋 إغلاق التطبيق");
        Platform.exit();
        System.exit(0);
    }

    /**
     * مسح الجلسة فقط (يُستخدم في reLogin أيضًا).
     * لا يُوقف أي خدمة ولا يُغلق التطبيق.
     */
    public static void clearSession() {
        ApiClient.clearAuthToken();
        ApiClient.setUserName(null);
        SessionManager.getInstance().clear();
        AppLogBus.getInstance().log("🔓 تم مسح الجلسة والتوكن");
    }

    // ── helpers ──

    private static void stopBackendIfDirect() {
        try {
            BackendService backend = BackendService.getInstance();
            // نوقف فقط لو مش خدمة (backendAsService = false)
            boolean asService = com.safwat.hr.shared.AppConfig
                    .getBoolean("connection", "backendAsService", false);
            if (!asService && backend.isRunning()) {
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
            boolean asService = com.safwat.hr.shared.AppConfig
                    .getBoolean("connection", "pgAsService", false);
            if (!asService && pg.isRunning()) {
                AppLogBus.getInstance().log("⏹ إيقاف PostgreSQL...");
                pg.stop(false);
                AppLogBus.getInstance().log("✅ تم إيقاف PostgreSQL");
            }
        } catch (Exception e) {
            AppLogBus.getInstance().log("⚠️ خطأ أثناء إيقاف PostgreSQL: " + e.getMessage());
        }
    }
}
