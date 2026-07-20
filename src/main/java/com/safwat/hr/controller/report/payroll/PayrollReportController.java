package com.safwat.hr.controller.report.payroll;

import com.safwat.hr.model.report.payroll.PayrollReportLists;
import com.safwat.hr.model.report.payroll.PayrollReportUI;
import com.safwat.hr.shared.util.DateUtils;
import com.safwat.hr.ui.util.SearchDialog;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.Getter;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

@Getter
public class PayrollReportController implements Initializable {
    private final PayrollReportLists reportLists = PayrollReportLists.getInstance();
    private PayrollReportUI ui;
    @FXML
    private HBox H_1, H_2, H_3, H_4, H_5, H_6;
    @FXML
    private HBox H_element, H_employee, H_endDate, H_management, H_payGroup, H_report, H_startDate;
    @FXML
    private ComboBox<String> combo_Format, combo_report, combo_reportName;
    @FXML
    private Label lbl_elementName, lbl_endDate, lbl_name, lbl_nationalId, lbl_payId, lbl_statDate;
    @FXML
    private TextField txt_startDate, txt_endDate, txt_element, txt_search, txt_payGroup, txt_management;
    @FXML
    private VBox mainCont;
    @FXML
    private Button btn_PayGroupSearch, btn_managementSearch, btn_searchMonth;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        ui = new PayrollReportUI(this);
        fillMainCombo();
        fillFormatCombo();
        setButtonActions();
        setSearchActions();
        hideHBoxBar();
        mainComboListener();
    }

    void fillMainCombo() {

        List<String> data = reportLists.getReportList();

        combo_reportName.getItems().addAll(data);
    }

    void fillFormatCombo() {
        combo_Format.getItems().addAll("PDF", "EXCEL");
    }

    void setSearchActions() {
        txt_payGroup.setOnAction(_ -> {
            List<String> result = reportLists.payGroupList.stream()
                    .filter(s -> s.contains(txt_payGroup.getText()))
                    .toList();

            if (result.size() == 1) {
                txt_payGroup.setText(result.getFirst());

            } else if (result.size() > 1) {
                Optional<String> result2 = SearchDialog.forStrings().title("اختر مجموعة تعيين")
                        .data(result)
                        .owner(null)
                        .show();
                result2.ifPresent(_ -> txt_payGroup.setText(result2.get()));
            } else {
                Optional<String> result2 = SearchDialog.forStrings().title("اختر مجموعة تعيين")
                        .data(reportLists.payGroupList)
                        .owner(null)
                        .show();
                result2.ifPresent(_ -> txt_payGroup.setText(result2.get()));
            }
        });

        txt_management.setOnAction(_ -> {
            List<String> filterList = reportLists.payManagement.stream()
                    .filter(s -> s.contains(txt_management.getText()))
                    .toList();
            if (filterList.size() == 1) {
                txt_management.setText(filterList.getFirst());

            } else if (filterList.size() > 1) {
                Optional<String> result = SearchDialog.forStrings()
                        .owner(null)
                        .title("اختر إدارة")
                        .data(filterList)
                        .show();
                result.ifPresent(_ -> txt_management.setText(result.get()));
            } else {
                Optional<String> result = SearchDialog.forStrings()
                        .owner(null)
                        .title("اختر إدارة")
                        .data(reportLists.payManagement)
                        .show();
                result.ifPresent(_ -> txt_management.setText(result.get()));
            }
        });
    }

    void setButtonActions() {
        // زر المجموعات
        btn_PayGroupSearch.setOnAction(_ -> {
            Optional<String> payGroup = SearchDialog.forStrings().title("اختر مجموعة تعيين")
                    .data(reportLists.payGroupList)
                    .owner(null)
                    .show();
            payGroup.ifPresent(_ -> txt_payGroup.setText(payGroup.get()));
        });
        // زر الإدارات
        btn_managementSearch.setOnAction(_ -> {
            Optional<String> management = SearchDialog.forStrings().title("اختر مجموعة تعيين")
                    .data(reportLists.payManagement)
                    .owner(null)
                    .show();
            management.ifPresent(_ -> txt_management.setText(management.get()));
        });

        btn_searchMonth.setOnAction(_ -> {
            Optional<String> month = SearchDialog.forStrings().title("اختر شهر").data(reportLists.payMonthsYearly).show();
            month.ifPresent(_ -> {
                txt_startDate.setText(month.get());
                lbl_statDate.setText(DateUtils.toArabicMonthYear(DateUtils.getFirstDayOfMonth(txt_startDate.getText())));
            });
        });
    }

    void mainComboListener() {
        combo_reportName.getSelectionModel().selectedIndexProperty().addListener((_, _, _) -> {
            String selectedReport = combo_reportName.getSelectionModel().getSelectedItem();
            showComponent(selectedReport);
        });
    }

    public void hideHBoxBar() {
        for (Node node : mainCont.getChildren()) {
            if (node instanceof HBox hBox) {
                hBox.setManaged(false);
                hBox.setVisible(false);
            }
        }
    }

    void showComponent(String selectedReport) {
        ui.uiReportSeter(selectedReport);
    }


}
