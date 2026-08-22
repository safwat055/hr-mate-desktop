package com.safwat.hr.payroll.table;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

public class TableController implements Initializable {
    @FXML
    private ComboBox<?> Combo_elements_1;

    @FXML
    private ComboBox<?> Combo_payrollGroup;

    @FXML
    private Button btn_Excel;

    @FXML
    private Button btn_Pdf;

    @FXML
    private Button btn_Refrech;

    @FXML
    private Button btn_clear;

    @FXML
    private Button btn_clearstatic;

    @FXML
    private Button btn_delete;


    @FXML
    private Button btn_insertRows;

    @FXML
    private Button btn_savestatic;

    @FXML
    private ComboBox<?> combo_static;

    @FXML
    private RadioButton getMainCode;

    @FXML
    private RadioButton getSecondCode;

    @FXML
    private ToggleGroup group;

    @FXML
    private ToggleGroup group1;

    @FXML
    private TableView<?> tableView;

    @FXML
    private RadioButton toDown;

    @FXML
    private RadioButton toRight;

    @FXML
    private CheckBox useVoices;

    @FXML
    void add20Rows(ActionEvent event) {

    }

    @FXML
    void clearTableView(ActionEvent event) {

    }

    @FXML
    void deleteEmptyRows(ActionEvent event) {

    }

    @FXML
    void deleteFromPayrollTable(ActionEvent event) {

    }

    @FXML
    void exportToExcel(ActionEvent event) {

    }

    @FXML
    void exportToPDF(ActionEvent event) {

    }

    @FXML
    void refreshTable(ActionEvent event) {

    }

    @FXML
    void savePayrollTable(ActionEvent event) {

    }

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
}
