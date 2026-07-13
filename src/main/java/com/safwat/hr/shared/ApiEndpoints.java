package com.safwat.hr.shared;


public final class ApiEndpoints {

    public static final String AUTH = "/auth";
    public static final String PAYROLL_CHANGE = "/payrollChange";
    public static final String PAYROLL_HISTORY = "/payrollHistory";
    public static final String PAYROLL_INDEX = "/payrollIndex";
    public static final String PAYROLL_YEARLY = "/payrollYearly";

    private ApiEndpoints() {
    }

    public static class PayrollChange {
        public static final String BASE = "/payrollChange";
        public static final String SEARCH = "/payrollChange/get/searchEmployee";
        public static final String EMPLOYEE_RECORD = "/payrollChange/get/EMPLOYEE_RECORD";
        public static final String DOWNLOAD_CARD = "/payrollChange/download-changeCard";

    }

    public static class PayrollYearly {
        public static final String BASE = "/payrollYearly";
        public static final String SEARCH = "/payrollYearly/get/searchEmployee";
        public static final String EMPLOYEE_RECORD = "/payrollYearly/get/employeeYearly";
        public static final String DOWNLOAD_PAYMENTS = "/payrollYearly/get/download-payments";

    }
}