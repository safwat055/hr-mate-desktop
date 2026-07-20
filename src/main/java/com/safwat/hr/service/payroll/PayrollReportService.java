package com.safwat.hr.service.payroll;

import com.safwat.hr.service.payroll.dto.PayrollRequest;
import com.safwat.hr.utils.ApiClient;
import com.safwat.hr.utils.ApiEndpoints;

import java.io.IOException;

public class PayrollReportService {

    
    public Long doYearlyReport(PayrollRequest request) {

        try {
            return ApiClient.post(
                    ApiEndpoints.PayrollYearly.yearlyExpenses,
                    request,
                    Long.class
            ).getData();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
