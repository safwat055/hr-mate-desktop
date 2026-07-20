package com.safwat.hr.model.report.payroll;

import com.safwat.hr.controller.report.payroll.PayrollReportController;
import javafx.scene.Node;

import static com.safwat.hr.model.report.payroll.PayrollReportLists.*;

/**
 *
 */
public class PayrollReportUI {

    private final PayrollReportController controller;
    private final PayrollReportLists reportLists;

    public PayrollReportUI(PayrollReportController controller) {
        this.controller = controller;
        this.reportLists = PayrollReportLists.getInstance();
    }

    static void showNode(Node node) {
        node.setManaged(true);
        node.setVisible(true);
    }

    public void uiReportSeter(String selectedReport) {
        switch (selectedReport) {
            case payReport_1 -> {
                showNode(controller.getH_report());
                controller.getCombo_report().getItems().addAll(reportLists.payrollYearlyList_Ar);
                uiYearlySetter();

            }
            case payReport_2 -> {
                System.out.println(selectedReport);
            }
            default -> throw new IllegalStateException("Unexpected value: " + selectedReport);
        }

    }

    void uiYearlySetter() {

        controller.getCombo_report().getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            String subReport = controller.getCombo_report().getSelectionModel().getSelectedItem();
            controller.hideHBoxBar();
            if (yearlReportMap.get(subReport).equals(payrollYearly_1)) {
                showNode(controller.getH_report());
                showNode(controller.getH_startDate());
            } else if (yearlReportMap.get(subReport).equals(payrollYearly_2)) {
                showNode(controller.getH_report());

                showNode(controller.getH_startDate());
            } else if (yearlReportMap.get(subReport).equals(payrollYearly_3)) {
                showNode(controller.getH_report());
                showNode(controller.getH_startDate());
            } else if (yearlReportMap.get(subReport).equals(payrollYearly_6)) {
                showNode(controller.getH_report());
                showNode(controller.getH_startDate());
                showNode(controller.getH_management());
            } else if (yearlReportMap.get(subReport).equals(payrollYearly_7)) {
                showNode(controller.getH_report());
                showNode(controller.getH_startDate());
                showNode(controller.getH_management());
            } else if (yearlReportMap.get(subReport).equals(payrollYearly_8)) {
                showNode(controller.getH_report());
                showNode(controller.getH_startDate());
                showNode(controller.getH_management());
            } else if (yearlReportMap.get(subReport).equals(payrollYearly_9)) {
                showNode(controller.getH_report());
                showNode(controller.getH_startDate());
                showNode(controller.getH_payGroup());
            }
        });
    }


}
