package com.safwat.hr.controller;


import com.safwat.hr.notification.model.HRNotification;
import com.safwat.hr.notification.service.NotificationService;
import com.safwat.hr.notification.ui.HRNotificationBell;
import com.safwat.hr.shared.FXMLPaths;
import com.safwat.hr.ui.controls.SAFButton;
import com.safwat.hr.ui.util.TabManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

public class MainViewController implements Initializable {

    @FXML
    private Button btn_payments, btn_changeCard, btn_PayrollVocab;
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
    private ToolBar toolbar;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        setMainViewIcon();
        setButtonsAction();
        HRNotificationBell bell = new HRNotificationBell();
        toolbar.getItems().add(bell);


    }

    /**
     *
     */
    void setMainViewIcon() {
        SAFButton.flat(false, btn_payments, btn_changeCard, btn_PayrollVocab);


    }

    /**
     *
     */
    void setButtonsAction() {
        btn_payments.setOnAction(_ -> openPaymentsView());
        btn_changeCard.setOnAction(_ -> openChangeCard());
        btn_PayrollVocab.setOnAction(_ -> openPayVocab());

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

        NotificationService.getInstance().send(
                HRNotification.builder()
                        .type(HRNotification.NotificationType.SALARY)
                        .priority(HRNotification.Priority.HIGH)
                        .title("صرف رواتب يناير 2026")
                        .message("تم تحويل رواتب 142 موظف")
                        .file("/temp_downloads/بطاقة اجر الاشتراك_1783928578489.pdf")
                        .sender("نظام الرواتب")
                        .build()
        );
    }

    /**
     *
     */
    private void openPayVocab() {
        TabManager.loadFXMLInTab(tab, new FXMLPaths().getPayrollVocab(), "مفردات مرتب", true);
    }
}
