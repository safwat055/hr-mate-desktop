package com.safwat.hr.report.payroll;

import com.safwat.hr.service.payroll.dto.PayrollRequest;
import com.safwat.hr.utils.ApiClient;

import java.io.IOException;

/**
 * طبقة HTTP المسؤولة عن إرسال طلبات التقارير إلى الـ Backend.
 *
 * <p>كلاس utility يحتوي على طرق ثابتة ({@code static}) فقط —
 * لا يحتاج إلى إنشاء instance منه.
 *
 * <p>يُوفِّر فصلاً نظيفًا بين منطق الإرسال ({@link ReportSubmissionService})
 * وتفاصيل HTTP (بناء الطلب، الـ endpoint، تحليل الاستجابة).
 *
 * <p><b>تسلسل الاستدعاء:</b>
 * <pre>
 * Controller
 *   → ReportSubmissionService.submit()   [Thread خلفي]
 *     → ReportApiService.sendPayrollReport()
 *       → ApiClient.post()
 * </pre>
 *
 * <p><b>ملاحظة للتوسع:</b> لو احتجت نقاط HTTP إضافية (تحميل تقرير، معاينة، ...)
 * أضفها هنا كطرق {@code static} جديدة بنفس النمط.
 */
public class ReportApiService {

    private ReportApiService() {
        // utility class — لا يُنشأ منه instance
    }

    /**
     * يُرسِل طلب التقرير إلى الـ Backend ويُعيد معرّف التقرير عند النجاح.
     *
     * <p>يستخدم {@code request.getEndPoint()} كـ URL نسبي،
     * مما يجعل كل استراتيجية قادرة على تحديد الـ endpoint الخاص بها.
     *
     * @param request الطلب المبني من الاستراتيجية عبر {@code buildRequest()}
     * @return معرّف التقرير ({@code reportId}) الذي يُعيده الـ Backend
     * @throws IOException          عند فشل الاتصال بالشبكة
     * @throws InterruptedException إذا انقطع الخيط أثناء الانتظار
     */
    public static Long sendPayrollReport(PayrollRequest request)
            throws IOException, InterruptedException {
        return ApiClient.post(request.getEndPoint(), request, Long.class).getData();
    }
}