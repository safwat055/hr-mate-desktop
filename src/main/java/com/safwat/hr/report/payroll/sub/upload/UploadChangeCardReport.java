package com.safwat.hr.report.payroll.sub.upload;

import com.safwat.hr.network.ApiClient;
import com.safwat.hr.report.controller.PayrollReportController;
import com.safwat.hr.report.core.ReportContext;
import com.safwat.hr.report.core.ValidationException;
import com.safwat.hr.report.core.strategies.ReportStrategy;
import com.safwat.hr.report.core.ui.UiConfiguration;
import com.safwat.hr.report.core.ui.UiField;
import com.safwat.hr.shared.PayrollRequest;


public class UploadChangeCardReport implements ReportStrategy {
    @Override
    public String getCode() {
        return "UPLOAD_CHANGE_CARD_REPORT";
    }

    @Override
    public String getDisplayName() {
        return "تقرير اجر الاشتراك";
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
            throw new ValidationException("يجب اختيار ملف واحد على الأقل!");
        }

        // التحقق من أن الملفات موجودة فعلاً على الديسك
        for (java.nio.file.Path file : context.getFiles()) {
            if (!java.nio.file.Files.exists(file)) {
                throw new ValidationException(
                        "الملف غير موجود: " + file.getFileName());
            }
        }
    }

    @Override
    public boolean requiresFiles() {
        return true;
    }
}
