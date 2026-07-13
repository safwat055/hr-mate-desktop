package com.safwat.hr.controller.payroll;

import com.safwat.hr.ui.controls.HRButton;
import com.safwat.hr.ui.controls.HRTextField;
import javafx.fxml.Initializable;
import io.github.palexdev.materialfx.controls.MFXFilterComboBox;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * this is controller to payments view
 */
public class PaymentsController implements Initializable {
    @FXML
    private Button btn_clear;

    @FXML
    private Button btn_pdf;

    @FXML
    private Button btn_search;

    @FXML
    private Button btn_view;

    @FXML
    private MFXFilterComboBox<?> combo_PayEnd;

    @FXML
    private MFXFilterComboBox<?> combo_PayStart;

    @FXML
    private TextField txt_empCode;

    @FXML
    private TextField txt_empName;

    @FXML
    private TextField txt_nationalID;

    @FXML
    private TextField txt_searchValue;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setView();
    }

    void setView(){
        HRTextField.apply(txt_empCode, txt_empName, txt_nationalID, txt_searchValue);
        HRButton.flat(true,btn_clear, btn_pdf, btn_search, btn_view);
    }
}
