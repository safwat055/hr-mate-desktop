package com.safwat.hr.main;

import com.safwat.hr.controller.message.controller.MessageInboxController;
import com.safwat.hr.controller.user.AdminUsersController;
import com.safwat.hr.controller.user.ChangePasswordController;
import com.safwat.hr.network.ApiClient;
import com.safwat.hr.notification.ui.HRNotificationBell;
import com.safwat.hr.shared.FXMLPaths;
import com.safwat.hr.system.AppLogBus;
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
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MainViewController implements Initializable {

    @FXML
    private Button btn_payments, btn_changeCard, btn_PayrollVocab, btn_mail, btn_chat, btn_report, btn_payManager, btn_records, btn_tableView;
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

    // ✅ جديد — نحتفظ بـ reference للـ Inbox Controller
    private MessageInboxController inboxController;
    private Tab messagesTab;
    private Icons icons;

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
                event.consume(); // منع الإغلاق الفوري
                boolean confirm = AlertUtil.showConfirmation("إغلاق البرنامج",
                        "هل أنت متأكد من إغلاق البرنامج؟");
                if (confirm) {
                    AppLifecycle.shutdown();
                }
            });

            HRNotificationBell bell = new HRNotificationBell(stage, bellIcon, badge);

            // ✅ جديد — ربط الـ Bell بالـ Inbox
            bell.setOnMouseClicked(e -> {

                com.safwat.hr.notification.ui.HRNotificationPanel panel =
                        new com.safwat.hr.notification.ui.HRNotificationPanel(stage);

                panel.setOnOpenMessage(notification -> {
                    // 1. افتح تاب الرسائل
                    openMessagesTab();
                    // 2. مرر الرسالة
                    if (inboxController != null) {
                        inboxController.openMessage(notification);
                    }
                });

                // Show popup (مش هنستخدم الـ Bell's default panel)
                javafx.stage.Popup popup = new javafx.stage.Popup();
                popup.setAutoHide(true);
                popup.getContent().add(panel);

                double x = bell.localToScreen(bell.getBoundsInLocal()).getMaxX() - 440;
                double y = bell.localToScreen(bell.getBoundsInLocal()).getMaxY() + 8;
                popup.show(stage, x, y);

                // ✅ تسجيل Scene الـ Popup في ThemeEventBus لتطبيق الثيم عليها فورًا
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

        leftLable.setText(ApiClient.getUserName());
    }

    private Stage getStageFromNode(Node node) {
        return (Stage) node.getScene().getWindow();
    }

    void setMainViewIcon() {
        // SAFButton.flat(false, btn_payments, btn_changeCard, btn_PayrollVocab, btn_payManager,
        //   btn_scaleView, btn_records, btn_tableView);
    }

    void setButtonsAction() {
        btn_payments.setOnAction(_ -> openPaymentsView());
        btn_changeCard.setOnAction(_ -> openChangeCard());
        btn_PayrollVocab.setOnAction(_ -> openPayVocab());
        btn_report.setOnAction(_ -> openPayrollReport());
        btn_payManager.setOnAction(_ -> openPayManager());
        // ✅ جديد — فتح التاب مع الـ Controller
        btn_mail.setOnAction(_ -> openMessagesTab());

        btn_scaleView.setOnAction(_ -> openScaleView());
        btn_records.setOnAction(_ -> openRecordsView());
        btn_tableView.setOnAction(_ -> openTableView());
    }

    /**
     * ✅ جديد — فتح/إنشاء تاب الرسائل مع Controller
     */
    private void openMessagesTab() {
        // لو التاب موجود → فعله
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

            // ✅ لما يتقفل → نمسح الـ reference
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
        ViewManager.openIndependentView(new FXMLPaths().getPayrollReport());
    }

    @FXML
    void openChatView() {

        TabManager.loadFXMLInTab(tab, "/com/safwat/hr/view/chat/ChatView.fxml", "محادثات", true);
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


    /**
     * تسجيل الخروج: مسح الجلسة + إيقاف الخدمات المباشرة + إغلاق البرنامج.
     */
    @FXML
    private void logout() {
        boolean confirm = AlertUtil.showConfirmation("تسجيل الخروج",
                "هل أنت متأكد من تسجيل الخروج وإغلاق البرنامج؟");
        if (!confirm) return;

        AppLogBus.getInstance().log("🔴 تسجيل الخروج بواسطة المستخدم");
        AppLifecycle.shutdown();
    }

    /**
     * إعادة تسجيل الدخول: مسح الجلسة + الرجوع لشاشة Login من غير إغلاق البرنامج.
     * الخدمات (Backend / PostgreSQL) تفضل شغّالة.
     */
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

    @FXML
    void openBasicSetting() {
        ViewManager.openIndependentView("/com/safwat/hr/view/system/main.fxml");
    }

    @FXML
    void openChangePasswordView() {
        ChangePasswordController.open(getStageFromNode(btn_chat));
    }

    @FXML
    void openAdminUserView() {
        AdminUsersController.open(getStageFromNode(btn_chat));
    }

    /**
     * الانتقال لشاشة تسجيل الدخول في نفس الـ Stage (بدون إغلاق البرنامج).
     */
    @FXML
    private void navigateToLogin() {
        try {
            // ✅ المسار الصحيح
            String fxmlPath = "/com/safwat/hr/view/Login.fxml";
            URL resource = getClass().getResource(fxmlPath);

            if (resource == null) {
                throw new IOException("FXML file not found: " + fxmlPath);
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Parent loginView = loader.load();

            Stage stage = (Stage) btn_report.getScene().getWindow();
            // ✅ إزالة الـ CloseRequest القديم قبل تبديل الشاشة
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