package com.safwat.hr.controller.hr.leave.util;

public final class LeaveLabels {

    private LeaveLabels() {
    }

    public static String payMode(String v) {
        if (v == null) return "—";
        return switch (v) {
            case "PAID" -> "مدفوعة";
            case "UNPAID" -> "بدون أجر";
            default -> v;
        };
    }

    public static String pool(String v) {
        if (v == null) return "—";
        return switch (v) {
            case "ANNUAL" -> "الاعتيادي";
            case "CASUAL" -> "العارضة";
            default -> v;
        };
    }

    public static String category(String v) {
        if (v == null) return "—";
        return switch (v) {
            case "BALANCED" -> "رصيد وحساب";
            case "PERIODIC" -> "دوري";
            case "MILITARY" -> "خدمة عسكرية";
            case "SICK" -> "مرضي";
            case "ABSENCE" -> "غياب";
            case "UNPAID" -> "بدون أجر";
            default -> v;
        };
    }
}