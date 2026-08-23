package com.safwat.hr.report.core.strategies;

/**
 * حامل Singleton للـ ReportStrategyRegistry.
 * يضمن إنشاء الاستراتيجيات مرة واحدة فقط في عمر التطبيق.
 * آمن للاستخدام من أي Thread خارج واجهة التقارير.
 */
public class ReportRegistryHolder {

    private static final Object LOCK = new Object();
    private static volatile ReportStrategyRegistry INSTANCE;

    private ReportRegistryHolder() {
    }

    /**
     * يجلب الـ Registry (ينشئه في أول استدعاء فقط)
     */
    public static ReportStrategyRegistry getInstance() {
        if (INSTANCE == null) {
            synchronized (LOCK) {
                if (INSTANCE == null) {
                    INSTANCE = ReportRegistryFactory.create();
                }
            }
            
        }
        return INSTANCE;
    }
}