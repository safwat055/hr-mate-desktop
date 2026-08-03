package com.safwat.hr.report.payroll.strategies.sub;

import com.safwat.hr.controller.report.payroll.PayrollReportController;
import com.safwat.hr.report.payroll.ReportContext;
import com.safwat.hr.report.payroll.strategies.ReportStrategy;
import com.safwat.hr.report.payroll.ui.UiConfiguration;
import com.safwat.hr.report.payroll.ui.UiField;
import com.safwat.hr.service.payroll.dto.PayrollRequest;
import com.safwat.hr.utils.ApiClient;

public class UploadReviewReport implements ReportStrategy {
    @Override
    public String getCode() {
        return "UPLOAD_REVIEW_REPORT";
    }

    @Override
    public String getDisplayName() {
        return "تقرير المراجعة";
    }

    @Override
    public String getCategory() {
        return "UPLOAD_PAYROLL";
    }

    @Override
    public String getMainReport() {
        return "UPLOAD_PAYROLL";
    }

    @Override
    public UiConfiguration getUiConfig() {
        return UiConfiguration.builder()
                .requiredField(UiField.H_FILES)
                .visibleField(UiField.H_FILES)
                .build();
    }

    @Override
    public void onApply(PayrollReportController controller) {
        ReportStrategy.super.onApply(controller);
    }

    @Override
    public PayrollRequest buildRequest(ReportContext context) {
        return PayrollRequest.builder()
                .user(ApiClient.getUserName())
                .reportName(context.getReportName())
                .report(getCode())
                .build();
    }

    @Override
    public void validate(ReportContext context) {
        if (context.getFiles() == null || context.getFiles().isEmpty()) {
            throw new com.safwat.hr.report.payroll.ValidationException("يجب اختيار ملف واحد على الأقل!");
        }

        // التحقق من أن الملفات موجودة فعلاً على الديسك
        for (java.nio.file.Path file : context.getFiles()) {
            if (!java.nio.file.Files.exists(file)) {
                throw new com.safwat.hr.report.payroll.ValidationException(
                        "الملف غير موجود: " + file.getFileName());
            }
        }
    }

    @Override
    public boolean requiresFiles() {
        return true;
    }
}
