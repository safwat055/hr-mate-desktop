package com.safwat.hr.controller.employee.enums;

public enum TerminationReason {
    RETIREMENT("بلوغ سن التقاعد"),
    DEATH("الوفاة"),
    DISMISSAL("الفصل"),
    RESIGNATION("الاستقالة");

    private final String labelAr;

    TerminationReason(String labelAr) {
        this.labelAr = labelAr;
    }

    public String getLabelAr() {
        return labelAr;
    }
}