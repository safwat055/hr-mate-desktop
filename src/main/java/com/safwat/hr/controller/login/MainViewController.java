package com.safwat.hr.controller.login;

import com.safwat.hr.controller.admin.system.AppLogBus;
import com.safwat.hr.controller.admin.user.AdminUsersController;
import com.safwat.hr.controller.admin.user.ChangePasswordController;
import com.safwat.hr.controller.appearance.AppearanceSettingsController;
import com.safwat.hr.controller.message.controller.MessageInboxController;
import com.safwat.hr.network.SessionManager;
import com.safwat.hr.notification.ui.HRNotificationBell;
import com.safwat.hr.notification.ui.HRNotificationPanel;
import com.safwat.hr.shared.FXMLPaths;
import com.safwat.hr.shared.file.TempFileCleaner;
import com.safwat.hr.ui.icons.Icons;
import com.safwat.hr.ui.theme.ThemeEventBus;
import com.safwat.hr.ui.util.AlertUtil;
import com.safwat.hr.ui.util.TabManager;
import com.safwat.hr.ui.util.ViewManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MainViewController implements Initializable {

    @FXML
    private Button btn_payments, btn_changeCard, btn_PayrollVocab, btn_mail, btn_chat,
            btn_report, btn_payManager, btn_records, btn_tableView;
    @FXML
    private Button btn_scaleView;
    @FXML
    private TabPane tab;
    @FXML
    private Tab mainTab;
    @FXML
    private Label lblParts, leftLable;

    @FXML
    private AnchorPane leftPane;

    @FXML
    private AnchorPane rightPane;

    @FXML
    private VBox rightPanelContent;
    @FXML
    private VBox toolbar;
    @FXML
    private Label bellIcon, badge;

    // ══════════════════════════════════════════════════════════════
    //  Refs — نحتفظ بها عشان نقدر نمرر النتوفيكشن للتاب
    // ══════════════════════════════════════════════════════════════

    private MessageInboxController inboxController;
    private Tab messagesTab;

    private com.safwat.hr.controller.chat.controller.ChatViewController chatController;
    private Tab chatTab;

    private Icons icons;

    private static final String CHAT_FXML = "/com/safwat/hr/controller/chat/ChatView.fxml";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        icons = Icons.getInstance();
        setMainViewIcon();
        setButtonsAction();
        Platform.runLater(() -> {
            Stage stage = (Stage) toolbar.getScene().getWindow();

            // ✅ تسجيل الـ Scene الرئيسية في ThemeEventBus
            ThemeEventBus.register(toolbar.getScene());

            // ✅ ربط زرار X بـ AppLifecycle.shutdown()
            stage.setOnCloseRequest((WindowEvent event) -> {
                event.consume();
                boolean confirm = AlertUtil.showConfirmation("إغلاق البرنامج",
                        "هل أنت متأكد من إغلاق البرنامج؟");
                if (confirm) {
                    AppLifecycle.shutdown();
                }
            });

            HRNotificationBell bell = new HRNotificationBell(stage, bellIcon, badge);

            // ✅ ربط الـ Bell بـ Panel + routing للإشعارات
            bell.setOnMouseClicked(e -> {
                HRNotificationPanel panel = new HRNotificationPanel(stage);

                // ═══════════════════════════════════════════════════
                //  رسائل النظام (MESSAGE) → نافذة Messages
                // ═══════════════════════════════════════════════════
                panel.setOnOpenMessage(notification -> {
                    openMessagesTab();
                    if (inboxController != null) {
                        inboxController.openMessage(notification);
                    }
                });

                // ═══════════════════════════════════════════════════
                //  محادثات (CHAT) → نافذة الشات على المحادثة المحددة
                // ═══════════════════════════════════════════════════
                panel.setOnOpenChat(notification -> {
                    Long conversationId = extractId(notification.getActionTarget(), "chat/");
                    openChatTab();
                    if (chatController != null && conversationId != null) {
                        chatController.openConversation(conversationId);
                    }
                });

                // ═══════════════════════════════════════════════════
                //  fallback لأي نوع تاني (يفتح actionTarget كملف)
                // ═══════════════════════════════════════════════════
                panel.setOnOpenNotification(notification -> {
                    String target = notification.getActionTarget();
                    if (target != null && !target.isBlank()) {
                        com.safwat.hr.notification.util.FileOpener.openAsync(target);
                    }
                });

                // ── عرض الـ Popup ──
                Popup popup = new Popup();
                popup.setAutoHide(true);
                popup.getContent().add(panel);

                double x = bell.localToScreen(bell.getBoundsInLocal()).getMaxX() - 440;
                double y = bell.localToScreen(bell.getBoundsInLocal()).getMaxY() + 8;
                popup.show(stage, x, y);

                // ✅ تسجيل Scene الـ Popup في ThemeEventBus
                if (popup.getScene() != null) {
                    ThemeEventBus.register(popup.getScene());
                }
            });

            toolbar.getChildren().add(bell);
        });

        icons.getBellImage(bellIcon);
        icons.getChatImage(btn_chat);
        icons.getMailImage(btn_mail);
        icons.getReportImage(btn_report);

        leftLable.setText(SessionManager.getInstance().getDisplayName());
        TempFileCleaner.cleanOnStartup();
    }

    private Stage getStageFromNode(Node node) {
        return (Stage) node.getScene().getWindow();
    }

    void setMainViewIcon() {
    }

    void setButtonsAction() {
        btn_payments.setOnAction(_ -> openPaymentsView());
        btn_changeCard.setOnAction(_ -> openChangeCard());
        btn_PayrollVocab.setOnAction(_ -> openPayVocab());
        btn_report.setOnAction(_ -> openPayrollReport());
        btn_payManager.setOnAction(_ -> openPayManager());

        // ✅ فتح تاب الرسائل مع الـ Controller
        btn_mail.setOnAction(_ -> openMessagesTab());

        // ✅ فتح تاب الشات مع الـ Controller
        btn_chat.setOnAction(_ -> openChatTab());

        btn_scaleView.setOnAction(_ -> openScaleView());
        btn_records.setOnAction(_ -> openRecordsView());
        btn_tableView.setOnAction(_ -> openTableView());
    }

    // ══════════════════════════════════════════════════════════════
    //  Tabs with Controller Refs
    // ══════════════════════════════════════════════════════════════

    /**
     * فتح/إنشاء تاب الرسائل مع Controller. لو موجود → نفعّله.
     */
    private void openMessagesTab() {
        if (messagesTab != null) {
            tab.getSelectionModel().select(messagesTab);
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    new FXMLPaths().getMessageInboxView()));
            Parent inboxRoot = loader.load();
            inboxController = loader.getController();

            messagesTab = new Tab("📧 البريد", inboxRoot);
            messagesTab.setClosable(true);

            // لما يتقفل → نمسح الـ reference
            messagesTab.setOnClosed(e -> {
                inboxController = null;
                messagesTab = null;
            });

            tab.getTabs().add(messagesTab);
            tab.getSelectionModel().select(messagesTab);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * فتح/إنشاء تاب الشات مع Controller. لو موجود → نفعّله.
     */
    private void openChatTab() {
        if (chatTab != null) {
            tab.getSelectionModel().select(chatTab);
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(CHAT_FXML));
            Parent chatRoot = loader.load();
            chatController = loader.getController();

            chatTab = new Tab("💬 محادثات", chatRoot);
            chatTab.setClosable(true);

            // لما يتقفل → نمسح الـ reference
            chatTab.setOnClosed(e -> {
                chatController = null;
                chatTab = null;
            });

            tab.getTabs().add(chatTab);
            tab.getSelectionModel().select(chatTab);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  Helper — extract ID from actionTarget
    // ══════════════════════════════════════════════════════════════

    /**
     * يستخرج الـ id من actionTarget (مثل "chat/45" → 45).
     *
     * @param target النص
     * @param prefix "chat/" أو "messages/"
     * @return الـ id أو null
     */
    private Long extractId(String target, String prefix) {
        if (target == null || !target.startsWith(prefix)) return null;
        try {
            return Long.parseLong(target.substring(prefix.length()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  Tab Loaders العادية
    // ══════════════════════════════════════════════════════════════

    void openPaymentsView() {
        TabManager.loadFXMLInTab(tab, new FXMLPaths().getPaymentsView(), "تقارير صرف", true);
    }

    void openChangeCard() {
        TabManager.loadFXMLInTab(tab, new FXMLPaths().getChangeCardView(), "اجر الاشتراك", true);
    }

    private void openPayVocab() {
        TabManager.loadFXMLInTab(tab, new FXMLPaths().getPayrollVocab(), "مفردات مرتب", true);
    }

    @FXML
    private void openPayrollReport() {
        TabManager.loadFXMLInTab(tab, new FXMLPaths().getReportManager(), "مدير التقارير", false);
    }

    /**
     * @deprecated استخدم {@link #openChatTab()} بدل دي.
     * موجودة للتوافق مع FXML القديم.
     */
    @Deprecated
    @FXML
    void openChatView() {
        openChatTab();
    }

    @FXML
    void openPayManager() {
        TabManager.loadFXMLInTab(tab, new FXMLPaths().getPayrollManager(), "مدير استحقاقات", true);
    }

    @FXML
    void openScaleView() {
        TabManager.loadFXMLInTab(tab, new FXMLPaths().getSalaryScale(), "تدرج راتب", true);
    }

    @FXML
    void openRecordsView() {
        TabManager.loadFXMLInTab(tab, new FXMLPaths().getPayrollRecords(), "سجلات", true);
    }

    @FXML
    void openTableView() {
        TabManager.loadFXMLInTab(tab, new FXMLPaths().getPayrollTableView(), "واجهة الإدخال", true);
    }

    @FXML
    void openServerSetting() {
        TabManager.loadFXMLInTab(tab, new FXMLPaths().getBackendSetting(), "server setting", true);
    }

    @FXML
    void openBackupView() {
        TabManager.loadFXMLInTab(tab, new FXMLPaths().getBackupView(), "نسخ احتياطي", true);
    }

    // ══════════════════════════════════════════════════════════════
    //  Themes
    // ══════════════════════════════════════════════════════════════

    @FXML
    void applyThemeBlack() {
        ThemeEventBus.applyTheme(ThemeEventBus.BLACK);
    }

    @FXML
    void applyThemeBlue() {
        ThemeEventBus.applyTheme(ThemeEventBus.BLUE);
    }

    @FXML
    void applyThemeDark1() {
        ThemeEventBus.applyTheme(ThemeEventBus.DARK_1);
    }

    @FXML
    void applyThemeDark2() {
        ThemeEventBus.applyTheme(ThemeEventBus.DARK_2);
    }

    @FXML
    void applyThemeGray() {
        ThemeEventBus.applyTheme(ThemeEventBus.GRAY);
    }

    @FXML
    void applyThemeGreen() {
        ThemeEventBus.applyTheme(ThemeEventBus.GREEN);
    }

    @FXML
    void applyThemeIndigo() {
        ThemeEventBus.applyTheme(ThemeEventBus.INDIGO);
    }

    @FXML
    void applyThemeLightBlue() {
        ThemeEventBus.applyTheme(ThemeEventBus.LIGHT_BLUE);
    }

    @FXML
    void applyThemeBluePepsi() {
        ThemeEventBus.applyTheme(ThemeEventBus.PEPSI);
    }

    @FXML
    void applyThemeOlive() {
        ThemeEventBus.applyTheme(ThemeEventBus.OLIVE);
    }

    @FXML
    void applyThemePastel() {
        ThemeEventBus.applyTheme(ThemeEventBus.PASTEL);
    }

    @FXML
    void applyThemeTeal() {
        ThemeEventBus.applyTheme(ThemeEventBus.TEAL);
    }

    @FXML
    void applyThemeWarm() {
        ThemeEventBus.applyTheme(ThemeEventBus.WARM);
    }

    @FXML
    void applyThemeLight() {
        ThemeEventBus.applyTheme(ThemeEventBus.LIGHT);
    }

    @FXML
    void applyThemeCustom() {
        ThemeEventBus.applyTheme(ThemeEventBus.CUSTOM);
    }

    // ══════════════════════════════════════════════════════════════
    //  Session
    // ══════════════════════════════════════════════════════════════

    @FXML
    private void logout() {
        boolean confirm = AlertUtil.showConfirmation("تسجيل الخروج",
                "هل أنت متأكد من تسجيل الخروج وإغلاق البرنامج؟");
        if (!confirm) return;

        AppLogBus.getInstance().log("🔴 تسجيل الخروج بواسطة المستخدم");
        AppLifecycle.shutdown();
    }

    @FXML
    private void reLogin() {
        boolean confirm = AlertUtil.showConfirmation("إعادة تسجيل الدخول",
                "هل تريد تسجيل الخروج والرجوع لشاشة تسجيل الدخول؟\n" +
                        "ستظل الخدمات تعمل في الخلفية.");
        if (!confirm) return;

        AppLogBus.getInstance().log("🔄 إعادة تسجيل الدخول — مسح الجلسة فقط");
        AppLifecycle.clearSession();
        navigateToLogin();
    }

    // ══════════════════════════════════════════════════════════════
    //  Dialogs / Windows
    // ══════════════════════════════════════════════════════════════

    @FXML
    void openBasicSetting() {
        ViewManager.openIndependentView(
                "/com/safwat/hr/controller/admin/system/main.fxml",
                "اعدادات التشغيل");
    }

    @FXML
    void openChangePasswordView() {
        ChangePasswordController.open(getStageFromNode(btn_chat));
    }

    @FXML
    void openAdminUserView() {
        AdminUsersController.open(getStageFromNode(btn_chat));
    }

    @FXML
    void openViewSetting() {
        AppearanceSettingsController.openGeneral();
    }

    @FXML
    void openTemplateView() {
        ViewManager.openIndependentView(
                "/com/safwat/hr/controller/template/Template.fxml",
                "نماذج التحميل");
    }

    // ══════════════════════════════════════════════════════════════
    //  Navigate to Login
    // ══════════════════════════════════════════════════════════════

    @FXML
    private void navigateToLogin() {
        try {
            String fxmlPath = "/com/safwat/hr/controller/login/Login.fxml";
            URL resource = getClass().getResource(fxmlPath);

            if (resource == null) {
                throw new IOException("FXML file not found: " + fxmlPath);
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Parent loginView = loader.load();

            Stage stage = (Stage) btn_report.getScene().getWindow();
            stage.setOnCloseRequest(null);
            stage.setScene(new Scene(loginView));
            stage.setTitle("HR MATE - تسجيل الدخول");
            stage.setMaximized(false);
            stage.show();

        } catch (IOException e) {
            AlertUtil.showError("خطأ", "فشل تحميل شاشة تسجيل الدخول: " + e.getMessage());
            Platform.exit();
        }
    }
}