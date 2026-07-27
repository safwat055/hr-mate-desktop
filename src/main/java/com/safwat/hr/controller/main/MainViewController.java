package com.safwat.hr.controller.main;

import com.safwat.hr.notification.ui.HRNotificationBell;
import com.safwat.hr.shared.FXMLPaths;
import com.safwat.hr.ui.controls.SAFButton;
import com.safwat.hr.ui.icons.Icons;
import com.safwat.hr.ui.util.TabManager;
import com.safwat.hr.ui.util.ViewManager;
import com.safwat.hr.utils.ApiClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class MainViewController implements Initializable {

    @FXML
    private Button btn_payments, btn_changeCard, btn_PayrollVocab, btn_payReport, btn_mail, btn_chat;
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

            HRNotificationBell bell = new HRNotificationBell(stage, bellIcon, badge);

            // ✅ جديد — ربط الـ Bell بالـ Inbox
            bell.setOnMouseClicked(e -> {
                // لما يدوس على الجرس → يفتح الـ Panel
                // الـ Panel هيتبنى جوه الـ HRNotificationBell
                // بس احنا محتاجين نربط الـ Panel بـ onOpenMessage
                // الحل: ن override الـ togglePanel في الـ Bell أو نبني Panel منفصل

                // أسهل حاجة: نبني Panel منفصل هنا ونربطه
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
            });

            toolbar.getChildren().add(bell);
        });
        icons.getBellmage(bellIcon);
        icons.getChatImage(btn_chat);
        icons.getMailImage(btn_mail);

        leftLable.setText(ApiClient.getUserName());
    }

    private Stage getStageFromNode(Node node) {
        return (Stage) node.getScene().getWindow();
    }

    void setMainViewIcon() {
        SAFButton.flat(false, btn_payments, btn_changeCard, btn_PayrollVocab, btn_payReport);
    }

    void setButtonsAction() {
        btn_payments.setOnAction(_ -> openPaymentsView());
        btn_changeCard.setOnAction(_ -> openChangeCard());
        btn_PayrollVocab.setOnAction(_ -> openPayVocab());

        // ✅ جديد — فتح التاب مع الـ Controller
        btn_mail.setOnAction(e -> openMessagesTab());
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
        ViewManager.openIndependentView(new FXMLPaths().getPayrollReport(), null);
    }

    @FXML
    void openChatView() {

        TabManager.loadFXMLInTab(tab, "/com/safwat/hr/chat/ChatView.fxml", "محادثات", true);
    }
}