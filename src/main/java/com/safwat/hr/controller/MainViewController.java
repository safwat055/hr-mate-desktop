package com.safwat.hr.controller;


import com.safwat.hr.notification.service.MessageClientService;
import com.safwat.hr.notification.ui.ComposeMessageDialog;
import com.safwat.hr.notification.ui.HRNotificationBell;
import com.safwat.hr.shared.FXMLPaths;
import com.safwat.hr.ui.controls.SAFButton;
import com.safwat.hr.ui.icons.Icons;
import com.safwat.hr.ui.util.TabManager;
import com.safwat.hr.ui.util.ViewManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
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
    private Button btn_payments, btn_changeCard, btn_PayrollVocab, btn_payReport, btn_sendMSG;
    @FXML
    private TabPane tab;
    @FXML
    private Tab mainTab;
    @FXML
    private Label lblParts;

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

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        setMainViewIcon();
        setButtonsAction();
        Platform.runLater(() -> {
            Stage stage = (Stage) toolbar.getScene().getWindow();

            HRNotificationBell bell = new HRNotificationBell(stage, bellIcon, badge);
            toolbar.getChildren().add(bell);
        });
        Icons.getInstance().getBellmage(bellIcon);
        //new BackgroundServiceSimulator().start();

    }

    private Stage getStageFromNode(Node node) {
        return (Stage) node.getScene().getWindow();
    }

    /**
     *
     */
    void setMainViewIcon() {
        SAFButton.flat(false, btn_payments, btn_changeCard, btn_PayrollVocab, btn_payReport);


    }

    /**
     *
     */
    void setButtonsAction() {
        btn_payments.setOnAction(_ -> openPaymentsView());
        btn_changeCard.setOnAction(_ -> openChangeCard());
        btn_PayrollVocab.setOnAction(_ -> openPayVocab());
        btn_sendMSG.setOnAction(e -> {
            // فتح نافذة إرسال رسالة فارغة
            ComposeMessageDialog.show((Stage) toolbar.getScene().getWindow());
        });


        MessageClientService.getInstance().loadUnreadMessagesAndNotify();
    }


    /**
     *
     */
    void openPaymentsView() {

        TabManager.loadFXMLInTab(tab, new FXMLPaths().getPaymentsView(), "تقارير صرف", true);
    }

    /**
     *
     */
    void openChangeCard() {

        TabManager.loadFXMLInTab(tab, new FXMLPaths().getChangeCardView(), "اجر الاشتراك", true);


    }

    /**
     *
     */
    private void openPayVocab() {
        TabManager.loadFXMLInTab(tab, new FXMLPaths().getPayrollVocab(), "مفردات مرتب", true);
    }

    @FXML
    private void openPayrollReport() {
        ViewManager.openIndependentView("/com/safwat/hr/controller/report/payroll/PayrollReport.fxml", null);
    }
}
