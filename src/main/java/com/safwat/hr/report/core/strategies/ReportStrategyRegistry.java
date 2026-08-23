package com.safwat.hr.report.core.strategies;

import com.safwat.hr.report.core.PayrollReport;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * سجل مركزي — Hybrid:
 * • Containers: eager (دايماً في الذاكرة)
 * • Leaves (Direct + Sub): lazy by @PayrollReport annotation + hot cache
 *
 * <p><b>إصلاح — Race Condition في getByCode:</b>
 * كان الكود بيعمل:
 * <pre>
 *   cached = hotCache.get(code);      // thread A: not found
 *   cached = hotCache.get(code);      // thread B: not found
 *   instance = new Strategy();        // thread A: creates instance
 *   instance = new Strategy();        // thread B: creates ANOTHER instance!
 *   hotCache.put(code, instance);     // A and B put different instances
 * </pre>
 * الحل: {@code computeIfAbsent} اللي هو atomic في {@link ConcurrentHashMap} —
 * مستحيل يتعمل instance اتنين لنفس الكود حتى في بيئة multi-threaded.
 *
 * <p><b>إصلاح — System.err → log.warn:</b>
 * الـ fallback warning بقى يمشي مع باقي الـ logging في النظام.
 */
@Slf4j
public class ReportStrategyRegistry {

    // ── Metadata لـ ALL strategies (خفيف جداً — strings بس) ──
    private final Map<String, StrategyMeta> metaByCode = new LinkedHashMap<>();
    private final Map<String, String> codeByDisplayName = new LinkedHashMap<>();
    private final Map<String, List<String>> codesByCategory = new LinkedHashMap<>();

    // ── Hot Cache: instances (eager + lazily created) ──
    private final ConcurrentHashMap<String, ReportStrategy> hotCache = new ConcurrentHashMap<>();

    // ═════════════════════════════════════════════════════════════
    //  1️⃣ Eager — للحاويات (Containers) بس
    // ═════════════════════════════════════════════════════════════

    public void register(ReportStrategy strategy) {
        String code = strategy.getCode();
        metaByCode.put(code, new StrategyMeta(
                code,
                strategy.getDisplayName(),
                strategy.getCategory(),
                strategy.getMainReport(),
                strategy.hasSubReports(),
                null));
        codeByDisplayName.put(strategy.getDisplayName(), code);
        codesByCategory.computeIfAbsent(strategy.getCategory(), k -> new ArrayList<>()).add(code);
        hotCache.put(code, strategy); // ← eager instance
    }

    // ═════════════════════════════════════════════════════════════
    //  2️⃣ Lazy — بيقرأ من @PayrollReport annotation
    // ═════════════════════════════════════════════════════════════

    /**
     * Lazy — بيقرأ الـ metadata من {@link PayrollReport} annotation.
     *
     * <p>لو مفيش annotation → fallback لـ eager instantiation مع تحذير.
     * ده للـ migration التدريجي — الهدف إن كل الـ leaves يكون عليها {@link PayrollReport}.
     */
    public void registerLazy(Class<? extends ReportStrategy> clazz) {
        PayrollReport ann = clazz.getAnnotation(PayrollReport.class);

        if (ann == null) {
            // ── Fallback: مفيهوش annotation — eager بدل lazy ──
            try {
                ReportStrategy instance = clazz.getDeclaredConstructor().newInstance();
                register(instance);
                // إصلاح: log.warn بدل System.err
                log.warn("⚠️ [Fallback] {} محمل eager — ضيف @PayrollReport عشان يبقى lazy",
                        clazz.getSimpleName());
            } catch (Exception e) {
                throw new RuntimeException(
                        "فشل تسجيل " + clazz.getName()
                                + " (مفيهوش @PayrollReport ومعرفش أعمله instantiate)", e);
            }
            return;
        }

        // ── Lazy path: metadata من الـ Annotation فقط ──
        String code = ann.code();
        metaByCode.put(code, new StrategyMeta(
                code,
                ann.displayName(),
                ann.category(),
                ann.mainReport(),
                false,
                clazz));
        codeByDisplayName.put(ann.displayName(), code);
        codesByCategory.computeIfAbsent(ann.category(), k -> new ArrayList<>()).add(code);
        // ← مش بنضيف في hotCache هنا — بيتضاف أول ما يتطلب
    }

    // ═════════════════════════════════════════════════════════════
    //  3️⃣ Retrieval — مع Hot Cache
    // ═════════════════════════════════════════════════════════════

    /**
     * يجلب استراتيجية بكودها — lazy instantiation مع hot cache.
     *
     * <p><b>إصلاح — Race Condition:</b>
     * استُبدِل:
     * <pre>
     *   cached = hotCache.get(code);
     *   if (cached == null) { instance = new ...; hotCache.put(...); }
     * </pre>
     * بـ:
     * <pre>
     *   hotCache.computeIfAbsent(code, k -> new ...)
     * </pre>
     * {@code computeIfAbsent} في {@link ConcurrentHashMap} atomic —
     * الـ factory function بتتنفذ مرة واحدة بس حتى لو اتنين threads طلبوا نفس الكود.
     *
     * @param code كود التقرير
     * @return instance الاستراتيجية
     * @throws IllegalArgumentException لو الكود مش مسجل
     */
    public ReportStrategy getByCode(String code) {
        StrategyMeta meta = metaByCode.get(code);
        if (meta == null) {
            throw new IllegalArgumentException("كود تقرير غير معروف: " + code);
        }

        // computeIfAbsent: atomic — مستحيل instance اتنين لنفس الكود
        return hotCache.computeIfAbsent(code, k -> {
            if (meta.clazz == null) {
                throw new IllegalStateException(
                        "التقرير " + code + " مسجل eager لكن مش موجود في cache!");
            }
            try {
                log.debug("🔧 Lazy loading: {}", code);
                return meta.clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("فشل إنشاء التقرير: " + code, e);
            }
        });
    }

    public ReportStrategy getByDisplayName(String displayName) {
        String code = codeByDisplayName.get(displayName);
        if (code == null) throw new IllegalArgumentException("تقرير غير معروف: " + displayName);
        return getByCode(code);
    }

    public List<String> getDisplayNamesByCategory(String category) {
        return codesByCategory.getOrDefault(category, List.of()).stream()
                .map(code -> metaByCode.get(code).displayName)
                .collect(Collectors.toList());
    }

    public List<String> getAllDisplayNames() {
        return List.copyOf(codeByDisplayName.keySet());
    }

    /**
     * بيرجع الـ instances اللي في الـ Cache فقط (eager + مستخدمة قبل كده).
     *
     * <p>آمن للاستخدام في أي وقت — مش بيعمل instantiation إضافي.
     */
    public List<ReportStrategy> getAllCached() {
        return List.copyOf(hotCache.values());
    }

    /**
     * بيرجع كل الـ instances — force instantiation على اللي لسه lazy.
     *
     * <p><b>⚠️ تحذير:</b> بيكسر الـ lazy loading ويحمل كل التقارير في الذاكرة.
     * استخدمه فقط في الـ admin diagnostics أو الاختبارات — لا تستدعيه تلقائياً.
     */
    public List<ReportStrategy> getAll() {
        return metaByCode.keySet().stream()
                .map(this::getByCode)
                .collect(Collectors.toList());
    }

    /**
     * بترجع قائمة items (code + name) لفئة معينة — للـ ComboBox الفرعي.
     *
     * <p>لا تعمل instantiation — بتقرأ من الـ metadata بس.
     */
    public List<ReportItem> getItemsByCategory(String category) {
        return codesByCategory.getOrDefault(category, List.of()).stream()
                .map(code -> {
                    StrategyMeta meta = metaByCode.get(code);
                    return new ReportItem(code, meta.displayName);
                })
                .collect(Collectors.toList());
    }

    /**
     * item بسيط للـ ComboBox — بيحمل الكود والاسم.
     */
    public record ReportItem(String code, String displayName) {
        @Override
        public String toString() {
            return displayName;
        }
    }

    // ═════════════════════════════════════════════════════════════
    //  Inner: Metadata holder (خفيف جداً — strings + class ref بس)
    // ═════════════════════════════════════════════════════════════

    private static class StrategyMeta {
        final String code;
        final String displayName;
        final String category;
        final String mainReport;
        final boolean hasSubReports;
        final Class<? extends ReportStrategy> clazz; // null = eager-only

        StrategyMeta(String code, String displayName, String category,
                     String mainReport, boolean hasSubReports,
                     Class<? extends ReportStrategy> clazz) {
            this.code = code;
            this.displayName = displayName;
            this.category = category;
            this.mainReport = mainReport;
            this.hasSubReports = hasSubReports;
            this.clazz = clazz;
        }
    }
}