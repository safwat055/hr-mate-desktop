package com.safwat.hr.report.payroll;

import com.safwat.hr.service.payroll.dto.PayrollRequest;
import com.safwat.hr.utils.ApiClient;
import com.safwat.hr.utils.ApiResponse;
import com.safwat.hr.utils.dto.ReportSubmissionResult;

import java.nio.file.Path;
import java.util.List;

public class ReportApiService {

    public static ReportSubmissionResult sendPayrollReport(
            PayrollRequest request,
            List<Path> files) {

        try {
            ApiResponse<ReportSubmissionResult> response;

            response = ApiClient.submitReport(request, files);

            if (response == null) {
                System.err.println("ReportApiService: response is null");
                return null;
            }
            if (!response.isSuccess()) {
                System.err.println("ReportApiService: success=false, msg=" + response.getMessage());
                return null;
            }
            if (response.getData() == null) {
                System.err.println("ReportApiService: data is null");
                return null;
            }
            return response.getData();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}