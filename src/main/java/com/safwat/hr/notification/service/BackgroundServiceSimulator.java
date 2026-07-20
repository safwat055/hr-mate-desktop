package com.safwat.hr.notification.service;

import com.safwat.hr.notification.model.HRNotification;
import com.safwat.hr.notification.model.HRNotification.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * محاكي الخدمات الخلفية.
 * يرسل إشعارات نظام ورسائل مستخدمين بشكل دوري.
 * <p>
 * في الإنتاج: استبدل بـ WebSocket Client أو REST Polling.
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
                    .type(NotificationType.SALARY).priority(Priority.HIGH)
                    .title("صرف رواتب يناير 2026")
                    .message("تم تحويل رواتب 142 موظف - إجمالي: 2,840,000 ريال")
                    .action("فتح التقرير", "salary/report/jan2026")
                    .file("/reports/salary_jan2026.pdf")
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
                    .type(NotificationType.LEAVE).priority(Priority.HIGH)
                    .title("طلب إجازة يحتاج موافقة")
                    .message("أحمد محمد - 3 أيام سنوية - 20 يناير إلى 23 يناير")
                    .action("مراجعة الطلب", "leave/request/789")
                    .sender("أحمد محمد")
                    .build(),

            HRNotification.builder()
                    .category(NotificationCategory.SYSTEM)
                    .type(NotificationType.TRAINING).priority(Priority.NORMAL)
                    .title("اكتمال دورة تدريبية")
                    .message("أكمل 12 موظفاً دورة إدارة الوقت - الشهادات جاهزة")
                    .action("تحميل الشهادات", "training/certs/batch3")
                    .file("/certificates/time_mgmt_batch3.zip")
                    .sender("نظام التدريب")
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
                    .attachment("performance_annual_2025.pdf",
                            "/reports/performance_annual_2025.pdf",
                            "application/pdf", 2_400_000)
                    .sender("نظام الأداء")
                    .build(),

            HRNotification.builder()
                    .category(NotificationCategory.SYSTEM)
                    .type(NotificationType.SYSTEM).priority(Priority.LOW)
                    .title("نسخة احتياطية مكتملة")
                    .message("تم حفظ نسخة احتياطية كاملة من قاعدة البيانات")
                    .sender("النظام")
                    .build()
    );
    String workingDir = System.getProperty("user.dir");
    Path tempDownloadsDir = Paths.get(workingDir, "temp_downloads");
    Path targetPath = tempDownloadsDir.resolve("PAYMENTS_REPORT1784512824309.pdf");
    // ===================== رسائل المستخدمين =====================
    private final List<HRNotification> messageSamples = List.of(

            HRNotification.builder()
                    .category(NotificationCategory.MESSAGE)
                    .priority(Priority.NORMAL)
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
                    .attachment("عقد_موظف_جديد.pdf",
                            targetPath.toString(),
                            "application/pdf", 1_200_000)
                    .build());


    // ===================== التشغيل =====================
    public void start() {
        sendInitialBatch();
        // إشعار نظام كل 12 ثانية
        scheduler.scheduleAtFixedRate(this::sendRandomSystem, 8, 12, TimeUnit.SECONDS);
        // رسالة مستخدم كل 20 ثانية
        scheduler.scheduleAtFixedRate(this::sendRandomMessage, 15, 20, TimeUnit.SECONDS);
    }

    private void sendInitialBatch() {
        // إشعارات نظام أولية
        service.send(systemSamples.get(0));  // موظف جديد
        service.send(systemSamples.get(1));  // صرف رواتب
        service.send(systemSamples.get(3));  // طلب إجازة
        service.send(systemSamples.get(5));  // مهمة متأخرة
        // رسالة مستخدم أولية
        service.send(messageSamples.get(0)); // عقد موظف
    }

    private void sendRandomSystem() {
        HRNotification original = systemSamples.get(rnd.nextInt(systemSamples.size()));
        service.send(rebuildWith(original, LocalDateTime.now()));
    }

    private void sendRandomMessage() {
        HRNotification original = messageSamples.get(rnd.nextInt(messageSamples.size()));
        service.send(rebuildWith(original, LocalDateTime.now()));
    }

    /**
     * يعيد بناء إشعار بـ timestamp جديد مع الحفاظ على كل خصائص الأصل
     */
    private HRNotification rebuildWith(HRNotification src, LocalDateTime ts) {
        Builder builder = HRNotification.builder()
                .category(src.getCategory())
                .priority(src.getPriority())
                .title(src.getTitle())
                .message(src.getMessage())
                .timestamp(ts);

        if (src.getType() != null)
            builder.type(src.getType());
        if (src.getMessageBody() != null && !src.getMessageBody().isBlank())
            builder.messageBody(src.getMessageBody());
        if (src.getSenderName() != null && !src.getSenderName().isBlank())
            builder.sender(src.getSenderName());
        if (src.getSenderAvatar() != null && !src.getSenderAvatar().isBlank())
            builder.senderAvatar(src.getSenderAvatar());
        if (src.getActionLabel() != null && !src.getActionLabel().isBlank())
            builder.action(src.getActionLabel(), src.getActionTarget());

        // إعادة بناء المرفقات كلها
        for (Attachment att : src.getAttachments()) {
            builder.attachment(
                    att.getFileName(), att.getFilePath(),
                    att.getMimeType(), att.getFileSize()
            );
        }

        return builder.build();
    }

    public void stop() {
        scheduler.shutdownNow();
    }
}
