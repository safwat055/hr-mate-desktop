package com.safwat.hr.notification.service;

import com.safwat.hr.notification.model.HRNotification;
import com.safwat.hr.notification.model.HRNotification.Builder;
import com.safwat.hr.notification.model.HRNotification.NotificationCategory;
import com.safwat.hr.notification.model.HRNotification.NotificationType;
import com.safwat.hr.notification.model.HRNotification.Priority;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * =====================================================
 * BackgroundServiceSimulator — محاكي الخدمات الخلفية
 * =====================================================
 * <p>
 * للاختبار فقط — في الإنتاج استبدله بـ:
 * - WebSocket (رسائل المستخدمين)     ← MessageClientService
 * - REST Polling أو SSE (إشعارات النظام)
 * <p>
 * الاستخدام:
 * BackgroundServiceSimulator sim = new BackgroundServiceSimulator();
 * sim.start();   // في start() بتاع Application
 * sim.stop();    // في stop()  بتاع Application
 */
public class BackgroundServiceSimulator {

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1, r -> {
                Thread t = new Thread(r, "bg-simulator");
                t.setDaemon(true);
                return t;
            });

    private final NotificationService service = NotificationService.getInstance();
    private final Random rnd = new Random();

    // ===================== إشعارات النظام =====================
    private final List<HRNotification> systemSamples = List.of(

            HRNotification.builder()
                    .category(NotificationCategory.SYSTEM)
                    .type(NotificationType.EMPLOYEE).priority(Priority.HIGH)
                    .title("تعيين موظف جديد")
                    .message("تم تعيين سارة خالد - مهندسة برمجيات - قسم التقنية")
                    .action("عرض الملف", "employee/profile/sarah")
                    .sender("قسم الموارد البشرية")
                    .build(),

            HRNotification.builder()
                    .category(NotificationCategory.SYSTEM)
                    .type(NotificationType.EMPLOYEE).priority(Priority.URGENT)
                    .title("طلب ترقية معلق")
                    .message("محمد أحمد يطلب ترقية من مهندس إلى مهندس أول")
                    .action("مراجعة الطلب", "employee/promotion/123")
                    .sender("مدير القسم")
                    .build(),

            HRNotification.builder()
                    .category(NotificationCategory.SYSTEM)
                    .type(NotificationType.EMPLOYEE).priority(Priority.NORMAL)
                    .title("تجديد عقد موظف")
                    .message("ينتهي عقد خالد عمر في 15 فبراير - يرجى اتخاذ إجراء")
                    .action("تجديد العقد", "employee/contract/456")
                    .build(),

            HRNotification.builder()
                    .category(NotificationCategory.SYSTEM)
                    .type(NotificationType.SALARY).priority(Priority.HIGH)
                    .title("صرف رواتب يناير 2026")
                    .message("تم تحويل رواتب 142 موظف بنجاح - إجمالي: 2,840,000 ريال")
                    .action("فتح التقرير", "salary/report/jan2026")
                    .attachment("salary_jan2026.pdf", "/reports/salary_jan2026.pdf",
                            "application/pdf", 2_100_000)
                    .sender("نظام الرواتب")
                    .build(),

            HRNotification.builder()
                    .category(NotificationCategory.SYSTEM)
                    .type(NotificationType.SALARY).priority(Priority.URGENT)
                    .title("خطأ في صرف راتب")
                    .message("فشل تحويل راتب علي حسن - الحساب البنكي غير صحيح")
                    .action("تصحيح البيانات", "salary/fix/ali")
                    .sender("نظام البنك")
                    .build(),

            HRNotification.builder()
                    .category(NotificationCategory.SYSTEM)
                    .type(NotificationType.SALARY).priority(Priority.NORMAL)
                    .title("تقرير المستحقات جاهز")
                    .message("تم إنشاء تقرير مستحقات ديسمبر 2025 - 89 بند")
                    .attachment("entitlements_dec2025.xlsx", "/reports/entitlements_dec2025.xlsx",
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 540_000)
                    .sender("نظام المحاسبة")
                    .build(),

            HRNotification.builder()
                    .category(NotificationCategory.SYSTEM)
                    .type(NotificationType.LEAVE).priority(Priority.HIGH)
                    .title("طلب إجازة يحتاج موافقة")
                    .message("أحمد محمد - 3 أيام سنوية - 20 يناير إلى 23 يناير")
                    .action("مراجعة الطلب", "leave/request/789")
                    .sender("أحمد محمد")
                    .build(),

            HRNotification.builder()
                    .category(NotificationCategory.SYSTEM)
                    .type(NotificationType.LEAVE).priority(Priority.NORMAL)
                    .title("تمت الموافقة على الإجازة")
                    .message("تمت الموافقة على إجازتك من 25 يناير - 3 أيام سنوية")
                    .action("عرض التفاصيل", "leave/approved/555")
                    .sender("المدير المباشر")
                    .build(),

            HRNotification.builder()
                    .category(NotificationCategory.SYSTEM)
                    .type(NotificationType.LEAVE).priority(Priority.LOW)
                    .title("رصيد إجازات منخفض")
                    .message("تبقى لديك 2 يوم فقط من رصيد الإجازات السنوية")
                    .sender("نظام الإجازات")
                    .build(),

            HRNotification.builder()
                    .category(NotificationCategory.SYSTEM)
                    .type(NotificationType.TRAINING).priority(Priority.NORMAL)
                    .title("اكتمال دورة تدريبية")
                    .message("أكمل 12 موظفاً دورة إدارة الوقت - الشهادات جاهزة")
                    .action("تحميل الشهادات", "training/certs/batch3")
                    .attachment("time_mgmt_batch3.zip", "/certificates/time_mgmt_batch3.zip",
                            "application/zip", 3_200_000)
                    .sender("نظام التدريب")
                    .build(),

            HRNotification.builder()
                    .category(NotificationCategory.SYSTEM)
                    .type(NotificationType.TRAINING).priority(Priority.HIGH)
                    .title("دورة إلزامية قادمة")
                    .message("دورة السلامة والصحة المهنية - إلزامية لجميع الموظفين - 28 يناير")
                    .action("التسجيل الآن", "training/register/safety2026")
                    .sender("قسم السلامة")
                    .build(),

            HRNotification.builder()
                    .category(NotificationCategory.SYSTEM)
                    .type(NotificationType.TASK).priority(Priority.URGENT)
                    .title("مهمة متأخرة - مراجعة عقود")
                    .message("يجب مراجعة وتجديد 8 عقود قبل نهاية الشهر - متأخرة 3 أيام")
                    .action("مراجعة العقود", "task/contracts/review")
                    .sender("نظام المهام")
                    .build(),

            HRNotification.builder()
                    .category(NotificationCategory.SYSTEM)
                    .type(NotificationType.TASK).priority(Priority.HIGH)
                    .title("اكتمال تقرير الأداء السنوي")
                    .message("تم إنشاء تقارير الأداء لـ 156 موظف - جاهزة للمراجعة")
                    .action("عرض التقارير", "task/report/performance2025")
                    .attachment("performance_annual_2025.pdf", "/reports/performance_annual_2025.pdf",
                            "application/pdf", 4_800_000)
                    .sender("نظام الأداء")
                    .build(),

            HRNotification.builder()
                    .category(NotificationCategory.SYSTEM)
                    .type(NotificationType.SYSTEM).priority(Priority.LOW)
                    .title("نسخة احتياطية مكتملة")
                    .message("تم حفظ نسخة احتياطية كاملة من قاعدة البيانات بنجاح")
                    .sender("النظام")
                    .build()
    );

    // ===================== رسائل المستخدمين =====================
    private final List<HRNotification> messageSamples = List.of(

            HRNotification.builder()
                    .category(NotificationCategory.MESSAGE)
                    .type(NotificationType.MESSAGE).priority(Priority.NORMAL)
                    .title("عقد الموظف الجديد")
                    .message("مرحباً، برجاء مراجعة عقد الموظف الجديد وإرسال ملاحظاتك...")
                    .messageBody(
                            "مرحباً،\n\n" +
                                    "برجاء مراجعة عقد الموظف الجديد المرفق وإرسال ملاحظاتك في أقرب وقت.\n\n" +
                                    "نحتاج الموافقة قبل نهاية الأسبوع لاستكمال إجراءات التعيين.\n\n" +
                                    "شكراً،\nأحمد محمد"
                    )
                    .sender("أحمد محمد")
                    .senderAvatar("أم")
                    .attachment("عقد_موظف_جديد.pdf", "/temp_downloads/عقد_موظف.pdf",
                            "application/pdf", 1_200_000)
                    .build(),

            HRNotification.builder()
                    .category(NotificationCategory.MESSAGE)
                    .type(NotificationType.MESSAGE).priority(Priority.HIGH)
                    .title("تقرير الحضور - أسبوع 3")
                    .message("ترفق تقرير الحضور الأسبوعي للمراجعة والاعتماد...")
                    .messageBody(
                            "السلام عليكم،\n\n" +
                                    "يرجى الاطلاع على تقرير الحضور والانصراف للأسبوع الثالث من يناير.\n\n" +
                                    "ملاحظة: يوجد 3 موظفين بتأخيرات متكررة تحتاج مراجعة.\n\n" +
                                    "مع التحية،\nفاطمة سعيد"
                    )
                    .sender("فاطمة سعيد")
                    .senderAvatar("فس")
                    .attachment("تقرير_حضور_اسبوع3.xlsx", "/temp_downloads/attendance_week3.xlsx",
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 540_000)
                    .build(),

            HRNotification.builder()
                    .category(NotificationCategory.MESSAGE)
                    .type(NotificationType.MESSAGE).priority(Priority.NORMAL)
                    .title("صور وثائق الموظف")
                    .message("مرفق صور وثائق الموظف الجديد للأرشفة...")
                    .messageBody(
                            "مرحباً،\n\n" +
                                    "مرفق صور وثائق الموظف الجديد خالد عمر للأرشفة في الملف.\n\n" +
                                    "الوثائق تشمل: بطاقة الهوية، الشهادة الدراسية، شهادة الخبرة.\n\n" +
                                    "خالد عمر"
                    )
                    .sender("خالد عمر")
                    .senderAvatar("خع")
                    .attachment("بطاقة_هوية.jpg", "/temp_downloads/id_card.jpg",
                            "image/jpeg", 820_000)
                    .attachment("شهادة_دراسية.pdf", "/temp_downloads/certificate.pdf",
                            "application/pdf", 1_500_000)
                    .attachment("شهادة_خبرة.pdf", "/temp_downloads/experience.pdf",
                            "application/pdf", 980_000)
                    .build(),

            HRNotification.builder()
                    .category(NotificationCategory.MESSAGE)
                    .type(NotificationType.MESSAGE).priority(Priority.LOW)
                    .title("دعوة اجتماع - مراجعة الأداء")
                    .message("يسعدنا دعوتكم لاجتماع مراجعة الأداء الربعي...")
                    .messageBody(
                            "السلام عليكم ورحمة الله،\n\n" +
                                    "يسعدنا دعوتكم لاجتماع مراجعة الأداء الربعي Q1 2026.\n\n" +
                                    "الموعد: الأحد 25 يناير 2026 الساعة 10 صباحاً\n" +
                                    "المكان: قاعة الاجتماعات الرئيسية\n\n" +
                                    "يرجى الإبلاغ بالتأكيد أو الاعتذار.\n\n" +
                                    "منى عبدالرحمن\nمدير الموارد البشرية"
                    )
                    .sender("منى عبدالرحمن")
                    .senderAvatar("مع")
                    .build()
    );

    // ===================== التشغيل =====================
    public void start() {
        sendInitialBatch();
        // إشعار نظام كل 12 ثانية
        scheduler.scheduleAtFixedRate(this::sendRandomSystem, 10, 12, TimeUnit.SECONDS);
        // رسالة مستخدم كل 20 ثانية
        scheduler.scheduleAtFixedRate(this::sendRandomMessage, 18, 20, TimeUnit.SECONDS);
    }

    private void sendInitialBatch() {
        service.send(systemSamples.get(0));   // موظف جديد
        service.send(systemSamples.get(3));   // صرف رواتب
        service.send(systemSamples.get(6));   // طلب إجازة
        service.send(systemSamples.get(11));  // مهمة متأخرة
        service.send(messageSamples.get(0));  // رسالة عقد
    }

    private void sendRandomSystem() {
        HRNotification src = systemSamples.get(rnd.nextInt(systemSamples.size()));
        service.send(rebuild(src));
    }

    private void sendRandomMessage() {
        HRNotification src = messageSamples.get(rnd.nextInt(messageSamples.size()));
        service.send(rebuild(src));
    }

    /**
     * يعيد بناء إشعار بـ timestamp جديد مع الحفاظ على كل خصائص الأصل
     */
    private HRNotification rebuild(HRNotification src) {
        Builder builder = HRNotification.builder()
                .category(src.getCategory())
                .priority(src.getPriority())
                .title(src.getTitle())
                .message(src.getMessage())
                .timestamp(LocalDateTime.now());

        if (src.getType() != null) builder.type(src.getType());
        if (src.getMessageBody() != null && !src.getMessageBody().isBlank())
            builder.messageBody(src.getMessageBody());
        if (src.getSenderName() != null && !src.getSenderName().isBlank())
            builder.sender(src.getSenderName());
        if (src.getSenderAvatar() != null && !src.getSenderAvatar().isBlank())
            builder.senderAvatar(src.getSenderAvatar());
        if (src.getActionLabel() != null && !src.getActionLabel().isBlank())
            builder.action(src.getActionLabel(), src.getActionTarget());

        src.getAttachments().forEach(a ->
                builder.attachment(a.getFileName(), a.getFilePath(),
                        a.getMimeType(), a.getFileSize()));

        return builder.build();
    }

    public void stop() {
        scheduler.shutdownNow();
    }
}
