package com.safwat.hr.model.report.payroll;

import com.safwat.hr.service.payroll.PayrollService;
import lombok.Getter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
public class PayrollReportLists {
    public final static String payReport_1 = "تقرير الصرفيات الشهري";
    public final static String payReport_2 = "تقرير إجمالي التكاليف الشهرى";
    public static final String payrollYearly_1 = "payrollYearly_1";
    public static final String payrollYearly_2 = "payrollYearly_2";
    public static final String payrollYearly_3 = "payrollYearly_3";
    public static final String payrollYearly_4 = "payrollYearly_4";
    public static final String payrollYearly_5 = "payrollYearly_5";
    public static final String payrollYearly_6 = "payrollYearly_6";
    public static final String payrollYearly_7 = "payrollYearly_7";
    public static final String payrollYearly_8 = "payrollYearly_8";
    public static final String payrollYearly_9 = "payrollYearly_9";
    public static Map<String, String> yearlReportMap = new LinkedHashMap<>();
    private static PayrollReportLists instance;
    public final String payReport_3 = "";
    public final String payReport_4 = "";
    public final String payReport_5 = "";
    public final String payReport_6 = "";
    public final String payReport_7 = "";
    public final String payReport_8 = "";
    public final String payReport_9 = "";
    public final String payReport_10 = "";
    public final String payReport_11 = "";
    public final String payReport_12 = "";
    public final String payReport_13 = "";
    public final String payReport_14 = "";
    public final String payReport_15 = "";
    public final String payReport_16 = "";
    public final String payReport_17 = "";
    public final String payReport_18 = "";
    public final String payReport_19 = "";
    public final String payReport_20 = "";
    public final String payReport_21 = "";
    public final String payReport_22 = "";
    public final String payReport_23 = "";
    public final String payReport_24 = "";
    public final String payReport_25 = "";
    public final String payReport_26 = "";
    public final List<String> payrollYearlyList_Ar = new ArrayList<>();
    public final List<String> payGroupList = new ArrayList<>();
    public final List<String> payManagement = new ArrayList<>();
    public final List<String> payMonthsYearly = new ArrayList<>();
    private final PayrollService service;
    private final List<String> reportList = List.of(payReport_1, payReport_2);
    private final List<String> payrollYearlyList = List.of(
            payrollYearly_1, payrollYearly_2, payrollYearly_3, payrollYearly_6, payrollYearly_7, payrollYearly_8
    );

    private PayrollReportLists() {
        service = PayrollService.getInstance();
        yearlReportMap.put("كل مجموعات التعيين", payrollYearly_1);
        yearlReportMap.put("مجموعات التعيين الرئيسية", payrollYearly_2);
        yearlReportMap.put("مجموعات التعيين المنفصلة", payrollYearly_3);
        yearlReportMap.put("تقرير إدارة محددة", payrollYearly_6);
        yearlReportMap.put("تقرير مجموعات التعيين الرئيسية لإدارة محددة", payrollYearly_7);
        yearlReportMap.put("تقرير مجموعات التعيين المنفصلة لإدارة محددة", payrollYearly_8);
        yearlReportMap.put("تقرير مجموعة تعيين محددة", payrollYearly_9);

        payrollYearlyList_Ar.addAll(yearlReportMap.keySet().stream().toList());
        payGroupList.addAll(service.getPayGroup());
        payManagement.addAll(service.getManagement());
        payMonthsYearly.addAll(service.getAllMonthsYearly());
    }

    public static PayrollReportLists getInstance() {
        if (instance == null) {
            instance = new PayrollReportLists();
        }
        return instance;
    }


}


