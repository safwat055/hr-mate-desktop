package com.safwat.hr.controller.report.payroll;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import lombok.Getter;

import java.net.URL;
import java.util.ResourceBundle;

@Getter
public class PayrollReportController implements Initializable {

    @FXML
    private HBox H_1, H_2, H_3, H_4, H_5, H_6;


    @FXML
    private HBox H_element, H_employee, H_endDate, H_management, H_payGroup, H_report, H_startDate;


    @FXML
    private ComboBox<?> combo_Format, combo_management, combo_payGroup, combo_report, combo_reportName;


    @FXML
    private Label lbl_elementName, lbl_endDate, lbl_name, lbl_nationalId, lbl_payId, lbl_statDate;

    @FXML
    private TextField txt_startDate, txt_endDate, txt_element, txt_search;

   
    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }
}
