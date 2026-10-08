package com.safwat.hr.controller.admin.system;

import com.safwat.hr.shared.AppConfig;
import com.safwat.hr.system.setup.HealthCheckService;
import com.safwat.hr.system.setup.NavigationBus;
import com.safwat.hr.system.setup.PathResolver;
import com.safwat.hr.system.setup.PgCredentialsDialog;
import com.safwat.hr.system.setup.SetupWizardService;
import com.safwat.hr.ui.util.AlertUtil;
import com.safwat.hr.ui.theme.SettingsThemeLoader;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Pair;
import lombok.Setter;

import java.net.URL;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * AdminConsoleController — الواجهة الموحّدة لكل شاشات إدارة النظام (ويندوز ولينكس).
 * <p>
 * ListView جانبي بمفاتيح الشاشات. كل شاشة تُحمّل مرة واحدة وتُخزّن في StackPane،
 * والتبديل = visible فقط.
 * <p>
 * البورتات بتتقرأ من AppConfig ({@code connection.pgPort} و{@code connection.port})،
 * عشان التوزيع المحمول يشتغل بالإعدادات اللي المستخدم حددها.
 */
public class AdminConsoleController implements Initializable {

    private static final int DEFAULT_PG_PORT = 5432;
    private static final int DEFAULT_BE_PORT = 8080;

    // ══════════════════ FXML ══════════════════
    @FXML
    private Label headerSubtitle;
    @FXML
    private Label globalStatus;
    @FXML
    private ProgressIndicator headerProgress;
    @FXML
    private SplitPane mainSplit;
    @FXML
    private ListView<String> navList;
    @FXML
    private StackPane contentStack;
    @FXML
    private Label statusLabel;
    @FXML
    private ProgressIndicator footerProgress;
    @FXML
    private Button btnHealth, btnRestoreDefaults, btnQuickStart;

    // ══════════════════ State ══════════════════
    private final Map<String, Node> views = new LinkedHashMap<>();
    private final Map<String, String> viewTitles = new LinkedHashMap<>();

    /** لو احتجته لاحقاً لفتح Dialogs */
    @Setter
    private Stage stage;

    // ══════════════════ Config ══════════════════

    private int pgPort() {
        return parsePort(AppConfig.getString("connection", "pgPort", String.valueOf(DEFAULT_PG_PORT)),
                DEFAULT_PG_PORT);
    }

    private int bePort() {
        return parsePort(AppConfig.getString("connection", "port", String.valueOf(DEFAULT_BE_PORT)),
                DEFAULT_BE_PORT);
    }

    private static int parsePort(String v, int fallback) {
        try {
            int p = Integer.parseInt(v == null ? "" : v.trim());
            return (p > 0 && p < 65536) ? p : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // ══════════════════ Init ══════════════════

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        SettingsThemeLoader.apply(navList);

        // سجّل الـ NavigationBus عشان MainController يقدر يطلب انتقال
        NavigationBus.register(this::navigateTo);

        // حمّل الشاشات مرة واحدة
        registerView(NavigationBus.HOME, "🏠  الرئيسية",
                "/com/safwat/hr/controller/admin/system/main.fxml");
        registerView(NavigationBus.POSTGRESQL, "🐘  PostgreSQL",
                "/com/safwat/hr/controller/admin/system/postgresql.fxml");
        registerView(NavigationBus.BACKEND, "🚀  Backend",
                "/com/safwat/hr/controller/admin/system/backend.fxml");
        registerView(NavigationBus.LOGS, "📊  السجلات",
                "/com/safwat/hr/controller/admin/system/logs.fxml");
        registerView(NavigationBus.BACKEND_PROPERTIES, "⚙  إعدادات الباك إند",
                "/com/safwat/hr/controller/backendSetting/backend-properties.fxml");
        registerView(NavigationBus.BACKEND_JSON, "اعدادات اضافية للنظام",
                "/com/safwat/hr/controller/backendSetting/backend-json.fxml");
        registerView(NavigationBus.FRONTEND_JSON, "🔧  إعدادات الفرونت (JSON)",
                "/com/safwat/hr/controller/backendSetting/frontend-json.fxml");

        navList.setItems(FXCollections.observableArrayList(views.keySet()));
        navList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : viewTitles.get(item));
            }
        });

        navList.getSelectionModel().selectedItemProperty()
                .addListener((o, oldV, newV) -> {
                    if (newV != null) showView(newV);
                });

        navList.getSelectionModel().selectFirst();
    }

    // ══════════════════ Loading ══════════════════

    private void registerView(String key, String title, String fxmlPath) {
        URL res = getClass().getResource(fxmlPath);
        if (res == null) {
            System.err.println("[AdminConsole] ملف FXML مش موجود: " + fxmlPath);
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(res);
            Node node = loader.load();
            node.setVisible(false);
            node.setManaged(false);
            views.put(key, node);
            viewTitles.put(key, title);
            contentStack.getChildren().add(node);
        } catch (Exception e) {
            System.err.println("[AdminConsole] فشل تحميل " + fxmlPath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showView(String key) {
        views.forEach((k, node) -> {
            boolean active = k.equals(key);
            node.setVisible(active);
            node.setManaged(active);
        });
        headerSubtitle.setText(viewTitles.getOrDefault(key, ""));
    }

    public void navigateTo(String key) {
        Platform.runLater(() -> {
            if (views.containsKey(key)) {
                navList.getSelectionModel().select(key);
            }
        });
    }

    // ══════════════════ Footer Actions ══════════════════

    @FXML
    private void onHealthCheck() {
        Optional<PathResolver.Distribution> det = PathResolver.detect();
        if (det.isEmpty()) {
            showAlert("خطأ", "لم يتم العثور على بنية التوزيع");
            return;
        }
        HealthCheckService.Report rep = HealthCheckService.check(det.get(), pgPort(), bePort());
        showHealthReport(rep);
    }

    @FXML
    private void onRestoreDefaults() {
        setStatus("⏳ جاري الاكتشاف والإعداد...", "stg-status-msg");
        showProgress(true);

        int pg = pgPort();
        int be = bePort();

        new Thread(() -> {
            SetupWizardService.SetupResult result = SetupWizardService.restoreDefaults(pg, be);
            Platform.runLater(() -> {
                showProgress(false);
                if (!result.success()) {
                    setStatus("❌ " + result.message(), "stg-status-msg-error");
                    showAlert("خطأ", result.message());
                    return;
                }
                setStatus("✅ " + result.message(), "stg-status-msg-ok");
                showHealthReport(result.report());
                navigateTo(NavigationBus.HOME);
            });
        }, "admin-restore-defaults").start();
    }

    @FXML
    private void onQuickStart() {
        // 1) PostgreSQL مابيشتغلش بـ root على لينكس — نوقف قبل أي حاجة
        if (OsSupport.isRootUser()) {
            showAlert("خطأ",
                    "التطبيق شغال بصلاحيات root.\n"
                            + "PostgreSQL مابيشتغلش بصلاحيات root. شغّل البرنامج بمستخدم عادي.");
            return;
        }

        // 2) لو PostgreSQL مش متهيأ نطلب بيانات المستخدم قبل ما نبدأ
        boolean needsInit = PathResolver.detect()
                .map(d -> !Files.isRegularFile(d.pgConf()))
                .orElse(false);

        String user = null;
        String pass = null;
        if (needsInit) {
            Optional<Pair<String, String>> creds = PgCredentialsDialog.ask(
                    AppConfig.getString("connection", "pgUser", PgCredentialsDialog.DEFAULT_USER),
                    "تشغيل سريع — مستخدم PostgreSQL",
                    "PostgreSQL مش متهيأ. أدخل بيانات المستخدم.\n"
                            + "لو سبت أي حقل فاضي هيتستخدم admin / admin");
            if (creds.isEmpty()) return;   // المستخدم ألغى
            user = creds.get().getKey();
            pass = creds.get().getValue();
        }

        // 3) التشغيل
        setStatus("⏳ جاري التشغيل...", "stg-status-msg");
        showProgress(true);
        btnQuickStart.setDisable(true);

        SetupWizardService.quickStart(pgPort(), bePort(), user, pass,
                (msg, pct) -> {
                    setStatus(msg, "stg-status-msg");
                    footerProgress.setProgress(pct / 100.0);
                },
                (ok, msg) -> {
                    showProgress(false);
                    btnQuickStart.setDisable(false);
                    setStatus(msg, ok ? "stg-status-msg-ok" : "stg-status-msg-error");
                    showAlert(ok ? "نجاح" : "خطأ", msg);
                });
    }

    // ══════════════════ Dialogs ══════════════════

    private void showHealthReport(HealthCheckService.Report rep) {
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("تقرير الجاهزية");
        dlg.setHeaderText(rep.ready()
                ? "✅ كل شيء جاهز للتشغيل"
                : "⚠️ " + rep.failCount() + " عناصر محتاجة إصلاح");
        dlg.getDialogPane().getStyleClass().add("stg-dialog");
        SettingsThemeLoader.apply(dlg.getDialogPane());

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(8);
        grid.setPadding(new Insets(15));

        int r = 0;
        for (var item : rep.items()) {
            Label icon = new Label(item.ok() ? "✅" : "❌");
            Label label = new Label(item.labelAr());
            label.setMinWidth(220);
            label.getStyleClass().add("stg-field-label");
            Label detail = new Label(item.detail());
            detail.getStyleClass().add("stg-field-value-muted");

            grid.add(icon, 0, r);
            grid.add(label, 1, r);
            grid.add(detail, 2, r);
            r++;

            if (!item.ok() && item.suggestion() != null) {
                Label sug = new Label("↳ " + item.suggestion());
                sug.getStyleClass().add("stg-status-msg-warn");
                sug.setPadding(new Insets(0, 0, 6, 26));
                grid.add(sug, 1, r, 2, 1);
                r++;
            }
        }

        dlg.getDialogPane().setContent(grid);
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dlg.showAndWait();
    }

    private void showAlert(String title, String message) {
        if (title != null && title.contains("خطأ")) {
            AlertUtil.showError(title, message);
        } else {
            AlertUtil.showInfo(title, message);
        }
    }

    // ══════════════════ Helpers ══════════════════

    private void setStatus(String msg, String cssClass) {
        Platform.runLater(() -> {
            statusLabel.setText(msg);
            statusLabel.getStyleClass().removeAll(
                    "stg-status-label", "stg-status-msg-ok",
                    "stg-status-msg-error", "stg-status-msg-warn");
            statusLabel.getStyleClass().add(cssClass);
        });
    }

    private void showProgress(boolean show) {
        footerProgress.setVisible(show);
        headerProgress.setVisible(show);
    }
}