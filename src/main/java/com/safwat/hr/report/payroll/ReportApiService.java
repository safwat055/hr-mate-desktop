package com.safwat.hr.report.payroll;

import com.safwat.hr.service.payroll.dto.PayrollRequest;
import com.safwat.hr.utils.ApiClient;
import com.safwat.hr.utils.ApiResponse;
import com.safwat.hr.utils.dto.ReportSubmissionResult;

public class ReportApiService {

    public static ReportSubmissionResult sendPayrollReport(PayrollRequest request) {
        try {
            ApiResponse<ReportSubmissionResult> response = ApiClient.submitReport(request);

            // ⬅️ المشكلة هنا: لو response = null أو isSuccess = false
            if (response == null) {
                System.err.println("ReportApiService: response is null");
                return null;
            }

            // ⬅️ لو isSuccess() = false رغم إن الـ data موجودة
            if (!response.isSuccess()) {
                System.err.println("ReportApiService: success=false, msg=" + response.getMessage());
                return null;
            }

            if (response.getData() == null) {
                System.err.println("ReportApiService: data is null");
                return null;
            }

            System.out.println("ReportApiService: تم استلام الطلب بمعرف رقم " + response.getData().getReportId());
            return response.getData();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}