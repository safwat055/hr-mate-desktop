package com.safwat.hr.report.core;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * ناقل أحداث بسيط لتقارير الرواتب.
 *
 * <p>يُستخدم لإبلاغ الشاشات المهتمة (مثل قائمة تقارير المستخدم) بأن تقريرًا جديدًا
 * تم تقديمه، فتحدّث نفسها دون أن تعرف الشاشة المُرسِلة.</p>
 *
 * <p>القائمة من نوع {@link CopyOnWriteArrayList} لأن المستمعين يُضافون ويُحذفون
 * أثناء تنفيذ الإشعار، وهذا آمن معها.</p>
 */
public final class ReportEventBus {

    private static final List<Consumer<Long>> SUBMITTED_LISTENERS = new CopyOnWriteArrayList<>();

    private ReportEventBus() {
    }

    /**
     * يسجّل مستمعًا لحدث تقديم تقرير.
     *
     * @param listener المستمع، ويستقبل رقم التقرير المُقدَّم
     */
    public static void addSubmittedListener(Consumer<Long> listener) {
        SUBMITTED_LISTENERS.add(listener);
    }

    /**
     * يحذف مستمعًا سبق تسجيله.
     *
     * @param listener المستمع المراد حذفه (نفس المرجع المُسجَّل)
     */
    public static void removeSubmittedListener(Consumer<Long> listener) {
        SUBMITTED_LISTENERS.remove(listener);
    }

    /**
     * يُبلغ كل المستمعين بتقديم تقرير جديد.
     *
     * <p>يجب استدعاؤها من خيط الواجهة (FX Thread).</p>
     *
     * @param reportId رقم التقرير المُقدَّم
     */
    public static void fireSubmitted(Long reportId) {
        for (Consumer<Long> listener : SUBMITTED_LISTENERS) {
            listener.accept(reportId);
        }
    }
}