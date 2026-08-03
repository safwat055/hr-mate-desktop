package com.safwat.hr.report.payroll;

import com.safwat.hr.service.payroll.dto.PayrollRequest;
import com.safwat.hr.utils.dto.ReportSubmissionResult;
import javafx.application.Platform;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * خدمة إرسال طلبات التقارير.
 *
 * <p>تفصل بين منطق الإرسال وطبقة الـ HTTP ({@link ReportApiService}).
 * تُنفِّذ كل طلب على خيط (Thread) خلفي مستقل حتى لا تُجمِّد واجهة المستخدم أثناء الانتظار.
 *
 * <p><b>إصلاح:</b> كانت استدعاءات HTTP تنفَّذ على UI Thread مباشرةً،
 * مما قد يتسبب في تجميد الواجهة. الإرسال الآن على خيط خلفي.
 *
 * <p><b>ملاحظة:</b> الـ callbacks ({@code onSuccess}, {@code onError}) تُستدعى
 * من الخيط الخلفي — المسؤولية على المُستدعي (Controller) أن يُعيد التنفيذ
 * إلى UI Thread عبر {@code Platform.runLater} إذا أراد تحديث الواجهة.
 *
 * <p><b>مثال من الـ Controller:</b>
 * <pre>{@code
 * submissionService.submit(request,
 *     reportId -> Platform.runLater(() -> SAFNotification.success("رقم الطلب: " + reportId)),
 *     error    -> Platform.runLater(() -> SAFNotification.error(error.getMessage()))
 * );
 * }</pre>
 */
public class ReportSubmissionService {

    /**
     * Executor مخصص لطلبات التقارير.
     * newCachedThreadPool: يُنشئ خيطًا عند الحاجة ويُعيد استخدامه إذا كان متاحًا.
     */
    private static final ExecutorService executor = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "payroll-report-thread");
        thread.setDaemon(true); // لا يمنع إغلاق التطبيق
        return thread;
    });

    /**
     * يُرسِل طلب التقرير على خيط خلفي.
     *
     * @param request   الطلب المبني من الاستراتيجية
     * @param onSuccess يُستدعى بمعرّف التقرير عند النجاح (على الخيط الخلفي)
     * @param onError   يُستدعى بالاستثناء عند الفشل (على الخيط الخلفي)
     */
    public void submit(PayrollRequest request,
                       List<Path> files,  // ⬅️ جديد
                       Consumer<Long> onSuccess,
                       Consumer<Exception> onError) {

        executor.execute(() -> {
            try {
                ReportSubmissionResult result = ReportApiService.sendPayrollReport(request, files);

                if (result == null) {
                    Platform.runLater(() -> onError.accept(
                            new RuntimeException("فشل الاتصال بالخادم")
                    ));
                    return;
                }
                Long reportId = result.getReportId();
                if (reportId == null) {
                    Platform.runLater(() -> onError.accept(
                            new RuntimeException("الخادم لم يرجع رقم التقرير")
                    ));
                    return;
                }
                Platform.runLater(() -> onSuccess.accept(reportId));
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> onError.accept(e));
            }
        });
    }
}