package com.safwat.hr.controller;


import com.safwat.hr.ui.controls.HRButton;
import com.safwat.hr.ui.controls.HRDialog;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

public class MainViewController implements Initializable {

    @FXML
    private Button btn_payments, btn_changeCard;
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

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        setMainViewIcon();
        setButtonsAction();
    }

    /**
     *
     */
    void setMainViewIcon() {
        HRButton.flat(false, btn_payments, btn_changeCard);


    }

    /**
     *
     */
    void setButtonsAction() {
        btn_payments.setOnAction((e) -> openPayView());
        btn_changeCard.setOnAction((e) -> openChangeCard());

    }

    /**
     *
     */
    void openPayView() {
        HRDialog.confirm("", "aaaaaaaaaaaaaaaaa");

        //TabManager.loadFXMLInTab(tab, new FXMLPathes().getPaymentsView(), "تقارير صرف", true);
    }

    void openChangeCard() {
        HRDialog.input("", "aaaaaaaaaaaa");
        //TabManager.loadFXMLInTab(tab, new FXMLPathes().getChangeCardView(), "اجر الاشتراك", true);
    }
}
