package com.safwat.hr.report.public_;

import com.safwat.hr.controller.report.PayrollReportController;
import com.safwat.hr.report.core.PayrollReport;
import com.safwat.hr.report.core.ReportContext;
import com.safwat.hr.report.core.ValidationException;
import com.safwat.hr.report.core.strategies.ReportStrategy;
import com.safwat.hr.report.core.ui.UiConfiguration;
import com.safwat.hr.report.core.ui.UiField;
import com.safwat.hr.shared.PayrollRequest;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * تقرير فرونت لتنفيذ مجموعة سكريبتات SQL.
 *
 * <p>للأدمن فقط — يختار ملفات <b>.sql</b> من الجهاز، والـ framework
 * بيرفعها للسيرفر ويعطيها مسارات، ثم الباك إند بينفذها بالتسلسل.
 */
@PayrollReport(
        code = "SCHEDULED_SCRIPT_EXECUTION",
        displayName = "تنفيذ مجدول لسكريبتات SQL",
        category = "main_direct",
        mainReport = "main_direct"
)
public class ScheduledScriptExecutionReport implements ReportStrategy {

    @Override
    public String getCode() {
        return "SCHEDULED_SCRIPT_EXECUTION";
    }

    @Override
    public String getDisplayName() {
        return "تنفيذ مجدول لسكريبتات SQL";
    }

    @Override
    public String getCategory() {
        return "main_direct";
    }

    @Override
    public String getMainReport() {
        return "main_direct";
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
                .reportName(context.getReportName())
                .report(getCode())
                .build();
    }

    @Override
    public void validate(ReportContext context) {
        if (context.getFiles() == null || context.getFiles().isEmpty()) {
            throw new ValidationException("يجب اختيار ملف SQL واحد على الأقل!");
        }

        // التحقق من وجود الملف على الديسك + امتداد .sql
        for (Path file : context.getFiles()) {
            if (!Files.exists(file)) {
                throw new ValidationException("الملف غير موجود: " + file.getFileName());
            }
            String name = file.getFileName().toString().toLowerCase();
            if (!name.endsWith(".sql")) {
                throw new ValidationException(
                        "الملف لازم يكون بصيغة .sql: " + file.getFileName());
            }
        }
    }

    @Override
    public boolean requiresFiles() {
        return true;
    }
}