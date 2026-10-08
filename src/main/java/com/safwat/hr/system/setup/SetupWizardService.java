package com.safwat.hr.system.setup;

import com.safwat.hr.controller.admin.system.BackendService;
import com.safwat.hr.controller.admin.system.PostgreSQLService;
import com.safwat.hr.shared.AppConfig;
import javafx.application.Platform;

import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * سيناريو المستخدم الفردي الكامل (ويندوز ولينكس):
 * 1) اكتشاف تلقائي للمسارات
 * 2) كتابة القيم في app_config.json + alone=true
 * 3) فحص جاهزية شامل
 * 4) تشغيل سريع بضغطة واحدة (بيطلب بيانات PostgreSQL لو مش متهيأ)
 *
 * <p>إعدادات الباك إند (application.properties) مابتتعدلش من هنا.
 */
public final class SetupWizardService {

    static final String DEFAULT_PG_USER = PgCredentialsDialog.DEFAULT_USER;
    static final String DEFAULT_PG_PASSWORD = PgCredentialsDialog.DEFAULT_PASSWORD;

    public record SetupResult(
            boolean success,
            PathResolver.Distribution dist,
            HealthCheckService.Report report,
            String message
    ) {
    }

    private SetupWizardService() {
    }

    /**
     * زر "إعادة الافتراضي" — يظبط كل شيء تلقائياً.
     */
    public static SetupResult restoreDefaults(int pgPort, int bePort) {
        Optional<PathResolver.Distribution> det = PathResolver.detect();
        if (det.isEmpty()) {
            return new SetupResult(false, null, null,
                    "لم يتم العثور على بنية التوزيع. تأكد من وجود فولدرات "
                            + "pgsql و hr-mate-system بجوار التطبيق.");
        }
        PathResolver.Distribution d = det.get();

        AppConfig.setValue("connection", "alone", "true");
        AppConfig.setValue("connection", "masterPC", "localhost");
        AppConfig.setValue("connection", "port", String.valueOf(bePort));
        AppConfig.setValue("connection", "pgPort", String.valueOf(pgPort));
        AppConfig.setValue("connection", "pgServiceName", "PostgreSQL");
        AppConfig.setValue("paths", "pgRoot", d.pgRoot.toString());
        AppConfig.setValue("paths", "pgBin", d.pgBin().toString());
        AppConfig.setValue("paths", "pgData", d.pgData().toString());
        AppConfig.setValue("paths", "backend", d.backendExe().toString());
        AppConfig.setValue("paths", "backendDir", d.backend.toString());
        AppConfig.setValue("paths", "frontend", d.frontend.toString());

        HealthCheckService.Report rep = HealthCheckService.check(d, pgPort, bePort);
        String msg = rep.ready()
                ? "✅ كل الإعدادات جاهزة — اضغط 'تشغيل سريع' للبدء"
                : "⚠️ " + rep.failCount() + " عناصر محتاجة إصلاح — راجع التقرير";
        return new SetupResult(true, d, rep, msg);
    }

    /**
     * زر "تشغيل سريع" — سلسلة خطوات مع تحديث الـ progress.
     *
     * @param pgUser     اسم مستخدم PostgreSQL (بيتستخدم بس لو PostgreSQL مش متهيأ). null = admin
     * @param pgPassword باسورد PostgreSQL (بيتستخدم بس لو PostgreSQL مش متهيأ). null = admin
     */
    public static void quickStart(
            int pgPort, int bePort,
            String pgUser, String pgPassword,
            BiConsumer<String, Integer> onStep,
            BiConsumer<Boolean, String> onDone) {

        new Thread(() -> {
            try {
                // ── Step 1: اكتشاف ──
                step(onStep, "🔍 اكتشاف المسارات...", 5);
                var det = PathResolver.detect();
                if (det.isEmpty()) {
                    finish(onDone, false, "لم يتم العثور على بنية التوزيع");
                    return;
                }
                var d = det.get();

                // ── Step 2: تهيئة PG لو محتاجة ──
                boolean pgInit = Files.isRegularFile(d.pgConf());
                if (!pgInit) {
                    String user = orDefault(pgUser, DEFAULT_PG_USER);
                    String pass = orDefault(pgPassword, DEFAULT_PG_PASSWORD);

                    step(onStep, "🔧 تهيئة PostgreSQL (أول مرّة)...", 15);
                    boolean ok = PostgreSQLService.getInstance()
                            .initialize(d.pgBin().toString(), d.pgData().toString(), user, pass);
                    if (!ok) {
                        finish(onDone, false, "فشل تهيئة PostgreSQL — راجع السجل");
                        return;
                    }
                    // اسم المستخدم بس بيتحفظ — الباسورد مابيتخزنش
                    AppConfig.setValue("connection", "pgUser", user);
                } else {
                    step(onStep, "✅ PostgreSQL مهيأ مسبقاً", 20);
                }

                // ── Step 3: تشغيل PG ──
                step(onStep, "🐘 تشغيل PostgreSQL...", 35);
                boolean pgStarted = PostgreSQLService.getInstance()
                        .start(d.pgBin().toString(), d.pgData().toString(), false);
                if (!pgStarted) {
                    finish(onDone, false, "فشل تشغيل PostgreSQL");
                    return;
                }

                // ── Step 4: انتظار PG (البورت بيتفتح على localhost) ──
                step(onStep, "⏳ انتظار جاهزية PostgreSQL...", 50);
                for (int i = 0; i < 15; i++) {
                    Thread.sleep(1000);
                    if (!HealthCheckService.isPortFree(pgPort)) break;
                }

                // ── Step 5: تشغيل Backend ──
                step(onStep, "🚀 تشغيل الباك إند...", 70);
                boolean beStarted = BackendService.getInstance()
                        .startNormal(d.backendExe().toString());
                if (!beStarted) {
                    finish(onDone, false, "فشل تشغيل الباك إند");
                    return;
                }

                // ── Step 6: انتظار HTTP ──
                step(onStep, "⏳ انتظار جاهزية الخادم...", 90);
                boolean httpReady = waitForHttp(
                        "http://localhost:" + bePort + "/actuator/health", 30);
                if (!httpReady) {
                    httpReady = waitForHttp("http://localhost:" + bePort + "/", 15);
                }
                if (!httpReady) {
                    finish(onDone, false, "الباك إند اتشغّل لكن مش بيرد على المنفذ " + bePort
                            + " — راجع logs/backend.log");
                    return;
                }

                step(onStep, "✅ اكتمل التشغيل", 100);
                finish(onDone, true, "النظام يعمل الآن على المنفذ " + bePort);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                finish(onDone, false, "تم إلغاء التشغيل السريع");
            } catch (Exception e) {
                finish(onDone, false, "خطأ غير متوقع: " + e.getMessage());
            }
        }, "quick-start").start();
    }

    // ───── Helpers ─────

    private static String orDefault(String v, String def) {
        return (v == null || v.isBlank()) ? def : v.trim();
    }

    private static void step(BiConsumer<String, Integer> cb, String msg, int pct) {
        Platform.runLater(() -> cb.accept(msg, pct));
    }

    private static void finish(BiConsumer<Boolean, String> cb, boolean ok, String msg) {
        Platform.runLater(() -> cb.accept(ok, msg));
    }

    private static boolean waitForHttp(String url, int seconds) {
        for (int i = 0; i < seconds; i++) {
            try {
                HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
                conn.setConnectTimeout(800);
                conn.setReadTimeout(800);
                int code = conn.getResponseCode();
                conn.disconnect();
                if (code >= 200 && code < 500) return true;
            } catch (Exception ignored) {
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }
}