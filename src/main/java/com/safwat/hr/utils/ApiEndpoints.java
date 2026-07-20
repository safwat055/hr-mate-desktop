package com.safwat.hr.utils;


public final class ApiEndpoints {

    public static final String AUTH = "/auth";
    public static final String PAYROLL_CHANGE = "/payrollChange";
    public static final String PAYROLL_HISTORY = "/payrollHistory";
    public static final String PAYROLL_INDEX = "/payrollIndex";
    public static final String PAYROLL_YEARLY = "/payrollYearly";

    private ApiEndpoints() {
    }

    public static class PayrollChange {
        //
        public static final String BASE = "/payrollChange";
        // for search in all employees that in payroll change table
        public static final String SEARCH = "/payrollChange/get/searchEmployee";
        // for get change record in view
        public static final String EMPLOYEE_RECORD = "/payrollChange/get/employeeRecord";
        // for download change file in change view
        public static final String DOWNLOAD_CARD = "/payrollChange/download-changeCard";
        public static final String UPDATE_NOTE = "/payrollChange/update-note";
        public static final String DELETE_RECORD = "/payrollChange/delete-record";

    }

    public static class PayrollYearly {
        public static final String BASE = "/payrollYearly";
        public static final String yearlyExpenses = "/payrollYearly/get/yearlyExpenses";
        public static final String SEARCH = "/payrollYearly/get/searchEmployee";
        public static final String EMPLOYEE_RECORD = "/payrollYearly/get/employeeYearly";
        public static final String DOWNLOAD_PAYMENTS = "/payrollYearly/get/download-payments";
        public static final String UPDATE_EMPLOYEE_NOTE = "/payrollYearly/update-employee-note";
        public static final String DELETE_ONE_EMPLOYEE_RECORD = "/payrollYearly/delete-employee-record";

        public static final String PAY_GROUP_LIST = "/payrollYearly/allPayGroup";
        public static final String PAY_MANAGEMENT_LIST = "/payrollIndex/get-indexInfo";
        public static final String PAY_MONTHS_List = "/payrollYearly/allMonths";

    }


    public static class PayrollHistory {
        public static final String BASE = "/payrollHistory";
        public static final String SEARCH = "/payrollHistory/get/searchEmployee";
        public static final String downloadReview = "/payrollHistory/downloade-review";
    }
}