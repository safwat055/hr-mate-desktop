package com.safwat.hr.report.payroll;

import com.safwat.hr.service.payroll.dto.PayrollRequest;

import java.io.IOException;
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
                       Consumer<Long> onSuccess,
                       Consumer<Exception> onError) {
        executor.execute(() -> {
            try {
                Long reportId = ReportApiService.sendPayrollReport(request);
                onSuccess.accept(reportId);
            } catch (IOException | InterruptedException e) {
                Thread.currentThread().interrupt(); // استعادة حالة الإيقاف لو كانت InterruptedException
                onError.accept(e);
            }
        });
    }
}