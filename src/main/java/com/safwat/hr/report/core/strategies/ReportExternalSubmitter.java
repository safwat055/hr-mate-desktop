package com.safwat.hr.report.core.strategies;

import com.safwat.hr.report.core.ReportContext;
import com.safwat.hr.report.core.ReportSubmissionService;
import com.safwat.hr.shared.PayrollRequest;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * خدمة إرسال التقارير برمجياً من خارج واجهة التقارير.
 * Singleton — يُستخدم instance واحد في كل أرجاء التطبيق.
 */
public class ReportExternalSubmitter {

    private static final Object LOCK = new Object();
    private static volatile ReportExternalSubmitter INSTANCE;
    private final ReportSubmissionService submissionService;

    private ReportExternalSubmitter() {
        this.submissionService = new ReportSubmissionService();
    }

    /**
     * يجلب الـ instance الوحيد (Thread-safe)
     */
    public static ReportExternalSubmitter getInstance() {
        if (INSTANCE == null) {
            synchronized (LOCK) {
                if (INSTANCE == null) {
                    INSTANCE = new ReportExternalSubmitter();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * يُرسِل تقريراً برمجياً باستخدام كود التقرير وContext يدوي.
     */
    public void submit(String reportCode,
                       ReportContext context,
                       List<Path> files,
                       Consumer<Long> onSuccess,
                       Consumer<Exception> onError) {

        ReportStrategyRegistry registry = ReportRegistryHolder.getInstance();
        ReportStrategy strategy = registry.getByCode(reportCode);

        String mainReportCode = strategy.getMainReport();
        if (!mainReportCode.equals(strategy.getCode())) {
            String parentDisplayName = registry.getByCode(mainReportCode).getDisplayName();
            context.setReportName(parentDisplayName);
        } else {
            context.setReportName(strategy.getDisplayName());
        }

        strategy.validate(context);
        PayrollRequest request = strategy.buildRequest(context);
        submissionService.submit(request, files, onSuccess, onError);
    }

    /**
     * نسخة مبسطة بدون ملفات
     */
    public void submit(String reportCode,
                       ReportContext context,
                       Consumer<Long> onSuccess,
                       Consumer<Exception> onError) {
        submit(reportCode, context, List.of(), onSuccess, onError);
    }
}