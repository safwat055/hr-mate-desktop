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
 */
@Slf4j
public class ReportStrategyRegistry {

    // ── Metadata لـ ALL strategies (خفيف جداً — strings بس) ──
    private final Map<String, StrategyMeta> metaByCode = new LinkedHashMap<>();
    private final Map<String, String> codeByDisplayName = new LinkedHashMap<>();
    private final Map<String, List<String>> codesByCategory = new LinkedHashMap<>();

    // ── Hot Cache: instances (eager + lazily created) ──
    private final Map<String, ReportStrategy> hotCache = new ConcurrentHashMap<>();

    // ═════════════════════════════════════════════════════════════
    //  1️⃣ Eager — للحاويات (Containers) بس
    // ═════════════════════════════════════════════════════════════
    public void register(ReportStrategy strategy) {
        String code = strategy.getCode();
        metaByCode.put(code, new StrategyMeta(code, strategy.getDisplayName(),
                strategy.getCategory(), strategy.getMainReport(),
                strategy.hasSubReports(), null));
        codeByDisplayName.put(strategy.getDisplayName(), code);
        codesByCategory.computeIfAbsent(strategy.getCategory(), k -> new ArrayList<>()).add(code);
        hotCache.put(code, strategy); // ← eager instance
    }

    /**
     * Lazy — بيقرأ من @PayrollReport annotation.
     * لو مفيش annotation، بيعمل fallback لـ eager instantiation (للـ migration التدريجي).
     */
    public void registerLazy(Class<? extends ReportStrategy> clazz) {
        PayrollReport ann = clazz.getAnnotation(PayrollReport.class);

        // ═══════════════════════════════════════════════════════
        //  Fallback: التقرير لسه معدلهوش — نستخدم السلوك القديم
        // ═══════════════════════════════════════════════════════
        if (ann == null) {
            try {
                ReportStrategy instance = clazz.getDeclaredConstructor().newInstance();
                register(instance); // ← eager (زي الأول)
                System.err.println("⚠️ [Fallback] " + clazz.getSimpleName()
                        + " محمل eager — ضيف @PayrollReport عشان يبقى lazy");
            } catch (Exception e) {
                throw new RuntimeException("فشل تسجيل " + clazz.getName()
                        + " (مفيهوش @PayrollReport ومعرفش أعمله instantiate)", e);
            }
            return;
        }

        // ═══════════════════════════════════════════════════════
        //  Lazy path: metadata من الـ Annotation
        // ═══════════════════════════════════════════════════════
        String code = ann.code();
        metaByCode.put(code, new StrategyMeta(code, ann.displayName(), ann.category(),
                ann.mainReport(), false, clazz));
        codeByDisplayName.put(ann.displayName(), code);
        codesByCategory.computeIfAbsent(ann.category(), k -> new ArrayList<>()).add(code);
    }

    // ═════════════════════════════════════════════════════════════
    //  3️⃣ Retrieval — مع Hot Cache
    // ═════════════════════════════════════════════════════════════

    public ReportStrategy getByCode(String code) {
        // أولاً: Hot Cache
        ReportStrategy cached = hotCache.get(code);
        if (cached != null) return cached;

        // ثانياً: Lazy instantiate
        StrategyMeta meta = metaByCode.get(code);
        if (meta == null) {
            throw new IllegalArgumentException("كود تقرير غير معروف: " + code);
        }
        if (meta.clazz == null) {
            throw new IllegalStateException("التقرير " + code + " مسجل eager — لكن مش موجود في cache!");
        }

        try {
            ReportStrategy instance = meta.clazz.getDeclaredConstructor().newInstance();
            hotCache.put(code, instance);
            return instance;
        } catch (Exception e) {
            throw new RuntimeException("فشل إنشاء التقرير: " + code, e);
        }
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
     * بيرجع الـ instances اللي في الـ Cache (eager + مستخدمة قبل كده).
     */
    public List<ReportStrategy> getAllCached() {
        return List.copyOf(hotCache.values());
    }

    /**
     * بيرجع كل الـ instances — force instantiation على اللي لسه.
     * استخدمه بحذر.
     */
    public List<ReportStrategy> getAll() {
        return metaByCode.keySet().stream()
                .map(this::getByCode)
                .collect(Collectors.toList());
    }

    /**
     * بترجع قائمة items (code + name) لفئة معينة — للـ ComboBox الفرعي
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
     * item بسيط للـ ComboBox — بيحمل الكود والاسم
     */
    public record ReportItem(String code, String displayName) {
        @Override
        public String toString() {
            return displayName;
        }
    }

    // ═════════════════════════════════════════════════════════════
    //  Inner: Metadata holder (خفيف جداً)
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