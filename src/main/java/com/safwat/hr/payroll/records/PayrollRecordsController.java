package com.safwat.hr.payroll.records;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.controlsfx.control.SearchableComboBox;

import java.net.URL;
import java.util.ResourceBundle;

public class PayrollRecordsController implements Initializable {
    @FXML
    private Button btn_pdf;

    @FXML
    private Button btn_pdf2;

    @FXML
    private Button btn_pdf3;

    @FXML
    private Button btn_pdf4;

    @FXML
    private Button btn_search;

    @FXML
    private Button btn_viewRecord;

    @FXML
    private Button btn_view_Period;

    @FXML
    private SearchableComboBox<?> combo_dates;

    @FXML
    private SearchableComboBox<?> combo_dates2;

    @FXML
    private SearchableComboBox<?> combo_end;

    @FXML
    private SearchableComboBox<?> combo_payment;

    @FXML
    private SearchableComboBox<?> combo_start;

    @FXML
    private RadioButton rb_emp_code;

    @FXML
    private RadioButton rb_emp_name;

    @FXML
    private RadioButton rb_national_id;

    @FXML
    private ToggleGroup searchGroup;

    @FXML
    private TableView<?> t_allownces;

    @FXML
    private TableView<?> t_allownces2;

    @FXML
    private TableView<?> t_deductions;

    @FXML
    private TableView<?> t_deductions2;

    @FXML
    private TextField txt_bank;

    @FXML
    private TextField txt_basic_30_6;

    @FXML
    private TextField txt_branch;

    @FXML
    private TextField txt_code;

    @FXML
    private TextField txt_degree;

    @FXML
    private TextField txt_id;

    @FXML
    private TextField txt_management;

    @FXML
    private TextField txt_name;

    @FXML
    private TextField txt_search, txt_month;


    /**
     * Called to initialize a controller after its root element has been
     * completely processed.
     *
     * @param location  The location used to resolve relative paths for the root object, or
     *                  {@code null} if the location is not known.
     * @param resources The resources used to localize the root object, or {@code null} if
     *                  the root object was not localized.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }


    @FXML
    void singleRecord(ActionEvent event) {

    }
}
