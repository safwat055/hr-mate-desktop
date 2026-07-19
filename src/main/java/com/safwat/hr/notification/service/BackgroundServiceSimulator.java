package com.safwat.hr.notification.service;


import com.safwat.hr.notification.model.HRNotification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * محاكي للخدمات الخلفية.
 * يُرسل إشعارات عشوائية كل بضع ثوانٍ لاختبار النظام.
 * في الإنتاج: استبدل هذا بالاتصالات الحقيقية (REST, WebSocket, DB triggers).
 */
public class BackgroundServiceSimulator {

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1, r -> {
                Thread t = new Thread(r, "bg-simulator");
                t.setDaemon(true);
                return t;
            });

    private final NotificationService notifService =
            NotificationService.getInstance();

    private final Random rnd = new Random();

    // قائمة إشعارات نموذجية لكل خدمة
    private final List<HRNotification> sampleNotifications = List.of(

            // ===== موظفون =====
            HRNotification.builder()
                    .type(HRNotification.NotificationType.EMPLOYEE).priority(HRNotification.Priority.HIGH)
                    .title("تعيين موظف جديد")
                    .message("تم تعيين سارة خالد - مهندسة برمجيات - قسم التقنية")
                    .action("عرض الملف", "employee/profile/sarah")
                    .sender("قسم الموارد البشرية")
                    .build(),

            HRNotification.builder()
                    .type(HRNotification.NotificationType.EMPLOYEE).priority(HRNotification.Priority.URGENT)
                    .title("طلب ترقية معلق")
                    .message("محمد أحمد يطلب ترقية من مهندس إلى مهندس أول - 3 سنوات خبرة")
                    .action("مراجعة الطلب", "employee/promotion/123")
                    .sender("مدير القسم")
                    .build(),

            HRNotification.builder()
                    .type(HRNotification.NotificationType.EMPLOYEE).priority(HRNotification.Priority.NORMAL)
                    .title("تجديد عقد موظف")
                    .message("ينتهي عقد خالد عمر في 15 فبراير - يرجى اتخاذ إجراء")
                    .action("تجديد العقد", "employee/contract/456")
                    .build(),

            // ===== رواتب =====
            HRNotification.builder()
                    .type(HRNotification.NotificationType.SALARY).priority(HRNotification.Priority.HIGH)
                    .title("صرف رواتب يناير 2026")
                    .message("تم تحويل رواتب 142 موظف بنجاح - إجمالي: 2,840,000 ريال")
                    .action("فتح التقرير", "salary/report/jan2026")
                    .file("/reports/salary_jan2026.pdf")
                    .sender("نظام الرواتب")
                    .build(),

            HRNotification.builder()
                    .type(HRNotification.NotificationType.SALARY).priority(HRNotification.Priority.URGENT)
                    .title("خطأ في صرف راتب")
                    .message("فشل تحويل راتب علي حسن - الحساب البنكي غير صحيح")
                    .action("تصحيح البيانات", "salary/fix/ali")
                    .sender("نظام البنك")
                    .build(),

            HRNotification.builder()
                    .type(HRNotification.NotificationType.SALARY).priority(HRNotification.Priority.NORMAL)
                    .title("تقرير المستحقات جاهز")
                    .message("تم إنشاء تقرير مستحقات ديسمبر 2025 - 89 بند")
                    .file("/reports/entitlements_dec2025.xlsx")
                    .sender("نظام المحاسبة")
                    .build(),

            // ===== إجازات =====
            HRNotification.builder()
                    .type(HRNotification.NotificationType.LEAVE).priority(HRNotification.Priority.HIGH)
                    .title("طلب إجازة يحتاج موافقة")
                    .message("أحمد محمد - 3 أيام سنوية - 20 يناير إلى 23 يناير")
                    .action("مراجعة الطلب", "leave/request/789")
                    .sender("أحمد محمد")
                    .build(),

            HRNotification.builder()
                    .type(HRNotification.NotificationType.LEAVE).priority(HRNotification.Priority.NORMAL)
                    .title("تمت الموافقة على الإجازة")
                    .message("تمت الموافقة على إجازتك من 25 يناير - 3 أيام سنوية")
                    .action("عرض التفاصيل", "leave/approved/555")
                    .sender("المدير المباشر")
                    .build(),

            HRNotification.builder()
                    .type(HRNotification.NotificationType.LEAVE).priority(HRNotification.Priority.LOW)
                    .title("رصيد إجازات منخفض")
                    .message("تبقى لديك 2 يوم فقط من رصيد الإجازات السنوية")
                    .sender("نظام الإجازات")
                    .build(),

            // ===== تدريب =====
            HRNotification.builder()
                    .type(HRNotification.NotificationType.TRAINING).priority(HRNotification.Priority.NORMAL)
                    .title("اكتمال دورة تدريبية")
                    .message("أكمل 12 موظفاً دورة إدارة الوقت - الشهادات جاهزة")
                    .file("/certificates/time_mgmt_batch3.zip")
                    .action("تحميل الشهادات", "training/certs/batch3")
                    .sender("نظام التدريب")
                    .build(),

            HRNotification.builder()
                    .type(HRNotification.NotificationType.TRAINING).priority(HRNotification.Priority.HIGH)
                    .title("دورة إلزامية قادمة")
                    .message("دورة السلامة والصحة المهنية - إلزامية لجميع الموظفين - 28 يناير")
                    .action("التسجيل الآن", "training/register/safety2026")
                    .sender("قسم السلامة")
                    .build(),

            // ===== مهام =====
            HRNotification.builder()
                    .type(HRNotification.NotificationType.TASK).priority(HRNotification.Priority.URGENT)
                    .title("مهمة متأخرة - مراجعة عقود")
                    .message("يجب مراجعة وتجديد 8 عقود قبل نهاية الشهر - متأخرة 3 أيام")
                    .action("مراجعة العقود", "task/contracts/review")
                    .sender("نظام المهام")
                    .build(),

            HRNotification.builder()
                    .type(HRNotification.NotificationType.TASK).priority(HRNotification.Priority.HIGH)
                    .title("اكتملت مهمة في الخلفية")
                    .message("تم إنشاء تقرير الأداء السنوي لجميع الموظفين - 156 تقرير")
                    .file("/reports/performance_annual_2025.pdf")
                    .action("عرض التقرير", "task/report/performance2025")
                    .sender("نظام الأداء")
                    .build(),

            HRNotification.builder()
                    .type(HRNotification.NotificationType.TASK).priority(HRNotification.Priority.NORMAL)
                    .title("تذكير - التقييم الربعي")
                    .message("موعد تقييم الربع الأول Q1 2026 في 31 مارس - تبقى 75 يوم")
                    .action("فتح التقييمات", "task/evaluation/q1-2026")
                    .sender("نظام الأداء")
                    .build(),

            // ===== نظام =====
            HRNotification.builder()
                    .type(HRNotification.NotificationType.SYSTEM).priority(HRNotification.Priority.LOW)
                    .title("نسخة احتياطية مكتملة")
                    .message("تم إنشاء نسخة احتياطية كاملة من قاعدة البيانات بنجاح")
                    .sender("النظام")
                    .build()
    );

    // ===================== التشغيل =====================
    public void start() {
        // إرسال 3 إشعارات أولية فوراً
        sendInitialNotifications();

        // ثم إرسال إشعار عشوائي كل 8-15 ثانية
        scheduler.scheduleAtFixedRate(
                this::sendRandom, 8, 12, TimeUnit.SECONDS
        );
    }

    private void sendInitialNotifications() {
        notifService.send(sampleNotifications.get(0));  // تعيين موظف
        notifService.send(sampleNotifications.get(3));  // صرف رواتب
        notifService.send(sampleNotifications.get(6));  // طلب إجازة
        notifService.send(sampleNotifications.get(9));  // اكتمال تدريب
        notifService.send(sampleNotifications.get(11)); // مهمة متأخرة
    }

    private void sendRandom() {
        int idx = rnd.nextInt(sampleNotifications.size());
        // نُنشئ نسخة جديدة (timestamp جديد) من نفس القالب
        HRNotification original = sampleNotifications.get(idx);
        HRNotification fresh = HRNotification.builder()
                .type(original.getType())
                .priority(original.getPriority())
                .title(original.getTitle())
                .message(original.getMessage())
                .sender(original.getSenderName() != null ? original.getSenderName() : "")
                .timestamp(LocalDateTime.now())
                .build();

        if (original.hasFile()) fresh = addFile(original, fresh);
        if (original.getActionLabel() != null)
            // بسيط - أعد البناء مع الـ action
            notifService.send(rebuildWithAction(original));
        else
            notifService.send(fresh);
    }

    private HRNotification addFile(HRNotification src, HRNotification n) {
        return HRNotification.builder()
                .type(src.getType()).priority(src.getPriority())
                .title(src.getTitle()).message(src.getMessage())
                .file(src.getFilePath())
                .timestamp(LocalDateTime.now())
                .build();
    }

    private HRNotification rebuildWithAction(HRNotification src) {
        return HRNotification.builder()
                .type(src.getType()).priority(src.getPriority())
                .title(src.getTitle()).message(src.getMessage())
                .action(src.getActionLabel(), src.getActionTarget())
                .sender(src.getSenderName() != null ? src.getSenderName() : "")
                .timestamp(LocalDateTime.now())
                .build();
    }

    public void stop() {
        scheduler.shutdownNow();
    }
}
