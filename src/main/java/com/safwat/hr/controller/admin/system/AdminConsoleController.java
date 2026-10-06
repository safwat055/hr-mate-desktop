package com.safwat.hr.controller.admin.system;

import com.safwat.hr.system.setup.HealthCheckService;
import com.safwat.hr.system.setup.NavigationBus;
import com.safwat.hr.system.setup.PathResolver;
import com.safwat.hr.system.setup.SetupWizardService;
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
import lombok.Setter;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * AdminConsoleController — الواجهة الموحّدة لكل شاشات إدارة النظام.
 * <p>
 * تحتوي على ListView جانبي بمفاتيح: الرئيسية / PostgreSQL / Backend /
 * السجلات / إعدادات الباك إند.
 * كل شاشة تُحمّل مرة واحدة وتُخزّن في StackPane — التبديل = visible فقط.
 */
public class AdminConsoleController implements Initializable {

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

    private static final int PG_PORT = 5432;
    private static final int BE_PORT = 8080;
    // لو احتجته لاحقاً لفتح Dialogs
    @Setter
    private Stage stage;

    // ══════════════════ Init ══════════════════
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        SettingsThemeLoader.apply(navList);

        // سجّل الـ NavigationBus عشان MainController يقدر يطلب انتقال
        NavigationBus.register(this::navigateTo);

        // حمّل الشاشات الخمس مرة واحدة
        registerView(NavigationBus.HOME, "🏠  الرئيسية", "/com/safwat/hr/controller/admin/system/main.fxml");
        registerView(NavigationBus.POSTGRESQL, "🐘  PostgreSQL", "/com/safwat/hr/controller/admin/system/postgresql.fxml");
        registerView(NavigationBus.BACKEND, "🚀  Backend", "/com/safwat/hr/controller/admin/system/backend.fxml");
        registerView(NavigationBus.LOGS, "📊  السجلات", "/com/safwat/hr/controller/admin/system/logs.fxml");
        registerView(NavigationBus.BACKEND_PROPERTIES, "⚙  إعدادات الباك إند", "/com/safwat/hr/controller/backendSetting/backend-properties.fxml");
        registerView(NavigationBus.BACKEND_JSON, "⚙  إعدادات الباك إند", "/com/safwat/hr/controller/backendSetting/backend-json.fxml");

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
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
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
        var det = PathResolver.detect();
        if (det.isEmpty()) {
            showAlert("خطأ", "لم يتم العثور على بنية التوزيع");
            return;
        }
        var rep = HealthCheckService.check(det.get(), PG_PORT, BE_PORT);
        showHealthReport(rep);
    }

    @FXML
    private void onRestoreDefaults() {
        setStatus("⏳ جاري الاكتشاف والإعداد...", "stg-status-msg");
        showProgress(true);

        new Thread(() -> {
            var result = SetupWizardService.restoreDefaults(PG_PORT, BE_PORT);
            Platform.runLater(() -> {
                showProgress(false);
                if (!result.success()) {
                    setStatus("❌ " + result.message(), "stg-status-msg-error");
                    showAlert("خطأ", result.message());
                    return;
                }
                setStatus("✅ " + result.message(), "stg-status-msg-ok");
                showHealthReport(result.report());
                // انتقل للرئيسية عشان المستخدم يشوف الحقول المحدّثة
                navigateTo(NavigationBus.HOME);
            });
        }).start();
    }

    @FXML
    private void onQuickStart() {
        setStatus("⏳ جاري التشغيل...", "stg-status-msg");
        showProgress(true);
        btnQuickStart.setDisable(true);

        SetupWizardService.quickStart(PG_PORT, BE_PORT,
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

    private void showAlert(String title, String msg) {
        Alert a = new Alert("خطأ".equals(title)
                ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setContentText(msg);
        a.getDialogPane().getStyleClass().add("stg-dialog");
        SettingsThemeLoader.apply(a.getDialogPane());
        a.showAndWait();
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