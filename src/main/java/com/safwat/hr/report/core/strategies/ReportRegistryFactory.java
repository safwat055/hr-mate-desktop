package com.safwat.hr.report.core.strategies;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;

/**
 * مصنع إنشاء السجل الرئيسي لاستراتيجيات التقارير.
 *
 * <p><b>Singleton — التسجيل يتم مرة واحدة فقط طول دورة البرنامج.</b>
 * حتى لو استُدعي {@link #create()} مليون مرة، يُرجع نفس الـ instance.
 */

@Slf4j
public class ReportRegistryFactory {

    // private static final Logger LOG = Logger.getLogger(ReportRegistryFactory.class.getName());
    private static final List<String> SCAN_PACKAGES = List.of(
            "com.safwat.hr.report.payroll.mainContainer",
            "com.safwat.hr.report.payroll.direct",
            "com.safwat.hr.report.payroll.sub.changeCard",
            "com.safwat.hr.report.payroll.sub.payrollReview",
            "com.safwat.hr.report.payroll.sub.payrollSummary",
            "com.safwat.hr.report.payroll.sub.payrollYearly",
            "com.safwat.hr.report.payroll.sub.records",
            "com.safwat.hr.report.payroll.sub.upload",
            "com.safwat.hr.report.payroll.sub.update",
            "com.safwat.hr.report.public_"
    );
    /**
     * الـ instance الوحيد — يُبنى مرة واحدة عند أول استدعاء للـ class
     */
    private static final ReportStrategyRegistry INSTANCE = buildRegistry();

    /**
     * لا يُنشأ من الخارج
     */
    private ReportRegistryFactory() {
    }

    /**
     * يُرجع السجل المُسجَّل — نفس الـ instance دائمًا.
     */
    public static ReportStrategyRegistry create() {
        return INSTANCE;
    }

    // ═══════════════════════════════════════════════════════════════
    //  بناء السجل (مرة واحدة فقط)
    // ═══════════════════════════════════════════════════════════════

    private static ReportStrategyRegistry buildRegistry() {
        ReportStrategyRegistry registry = new ReportStrategyRegistry();
        List<ReportStrategy> strategies = new ArrayList<>();

        for (String pkg : SCAN_PACKAGES) {
            for (Class<?> clazz : findClassesInPackage(pkg)) {
                if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
                    continue;
                }
                if (!ReportStrategy.class.isAssignableFrom(clazz)) {
                    continue;
                }
                try {
                    ReportStrategy strategy = (ReportStrategy) clazz
                            .getDeclaredConstructor()
                            .newInstance();
                    strategies.add(strategy);
                } catch (Exception e) {
                    throw new RuntimeException(
                            "فشل إنشاء instance للتقرير: " + clazz.getName(), e);
                }
            }
        }

        strategies.sort(Comparator
                .comparingInt((ReportStrategy s) -> isMain(s) ? 0 : 1)
                .thenComparing(ReportStrategy::getMainReport)
                .thenComparing(ReportStrategy::getDisplayName)
        );

        log.info("═══ تم تسجيل {} تقرير تلقائيًا ═══", strategies.size());
        for (ReportStrategy s : strategies) {
            String type = isMain(s) ? "رئيسي" : "فرعي";
            log.info(String.format("  [%s] %-45s | cat=%-20s | main=%s | class=%s",
                    type, s.getDisplayName(), s.getCategory(), s.getMainReport(), s.getClass().getSimpleName()));
        }

        strategies.forEach(registry::register);
        return registry;
    }

    /**
     * الرئيسي = حاوي (hasSubReports) أو مباشر (category يبدأ بـ main_)
     */
    private static boolean isMain(ReportStrategy strategy) {
        return strategy.hasSubReports()
                || (strategy.getCategory() != null && strategy.getCategory().startsWith("main_"));
    }

    // ═══════════════════════════════════════════════════════════════
    //  فحص يدوي للباكيج
    // ═══════════════════════════════════════════════════════════════

    private static List<Class<?>> findClassesInPackage(String packageName) {
        List<Class<?>> classes = new ArrayList<>();
        String path = packageName.replace('.', '/');
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        try {
            Enumeration<URL> resources = classLoader.getResources(path);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                String decodedPath = URLDecoder.decode(resource.getFile(), StandardCharsets.UTF_8);
                File directory = new File(decodedPath);

                if (directory.exists() && directory.isDirectory()) {
                    scanDirectory(directory, packageName, classes);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("فشل فحص الباكيج: " + packageName, e);
        }
        return classes;
    }

    private static void scanDirectory(File directory, String packageName, List<Class<?>> classes) {
        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file, packageName + "." + file.getName(), classes);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + "." +
                        file.getName().substring(0, file.getName().length() - 6);
                try {
                    classes.add(Class.forName(className));
                } catch (ClassNotFoundException | NoClassDefFoundError e) {
                    // تجاوز
                }
            }
        }
    }
}