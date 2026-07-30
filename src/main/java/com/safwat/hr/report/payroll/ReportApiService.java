package com.safwat.hr.report.payroll;

import com.safwat.hr.service.payroll.dto.PayrollRequest;
import com.safwat.hr.utils.ApiClient;

import java.io.IOException;

public class ReportApiService {
    public static Long sendPayrollReport(PayrollRequest request) throws IOException, InterruptedException {
        return ApiClient.post(request.getEndPoint(),
                request,
                Long.class).getData();
    }
}
