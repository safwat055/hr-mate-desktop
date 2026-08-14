package com.safwat.hr.network;


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

        public static final String SEARCH = "/payrollYearly/get/searchEmployee";
        public static final String EMPLOYEE_RECORD = "/payrollYearly/get/employeeYearly";
        public static final String DOWNLOAD_PAYMENTS = "/payrollYearly/get/download-payments";
        public static final String UPDATE_EMPLOYEE_NOTE = "/payrollYearly/update-employee-note";
        public static final String DELETE_ONE_EMPLOYEE_RECORD = "/payrollYearly/delete-employee-record";
        //report
        public static final String YEARLY_EXPENSES = "/payrollYearly/get/yearlyExpenses";
        public static final String PAYROLL_SUMMARY = "/payrollYearly/get/monthSummary";
        public static final String PAYROLL_PAYMENTS = "/payrollYearly/get/paymentsReport";

        public static final String PAY_GROUP_LIST = "/payrollYearly/allPayGroup";
        public static final String PAY_GROUP_LIST_MONTH = "/payrollYearly/get/ExchangeNamesWithMonth";
        public static final String PAY_MANAGEMENT_LIST = "/payrollIndex/get-indexInfo";
        public static final String PAY_MONTHS_List = "/payrollYearly/allMonths";

    }


    public static class PayrollReview {
        public static final String BASE = "/payroll-review";
        public static final String SEARCH = "/payroll-review/search-employee";
        public static final String downloadReview = "/payroll-review/download-main-report";
    }

    public static class PayrollElement {
        public static final String BASE = "/element";
        public static final String GET_NAMES = "/element/getElement";
        public static final String GET_CODES = "/element/getAllCodes";
    }
}