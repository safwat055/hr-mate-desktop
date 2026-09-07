package com.safwat.hr.shared;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * سجل دائم بكل أسماء الواجهات (viewId) اللي سبق واستخدمت مع أي كلاس تخصيص
 * (FontSettingsManager, ZoomManager...). بيتحفظ في نفس app_config.json عشان
 * يفضل موجود حتى بعد إغلاق البرنامج، على عكس التسجيل المؤقت في الذاكرة
 * (WeakReference) اللي بيستخدموه FontSettingsManager/ZoomManager للتطبيق الفوري.
 * <p>
 * الفايدة: تقدر تفتح شاشة "تخصيص الواجهة" وتختار أي شاشة اتفتحت قبل كده
 * ولو مرة واحدة، من غير ما تكون مفتوحة فعليًا دلوقتي.
 */
public class ViewRegistry {

    private static final String SECTION = "meta";
    private static final String KEY = "registeredViews";

    private ViewRegistry() {
        // كلاس أدوات ثابت
    }

    /** بتسجل اسم واجهة جديد لو مش مسجل قبل كده. آمنة تتنادى كل مرة من غير أي فحص مسبق. */
    public static void register(String viewId) {
        if (viewId == null || viewId.isBlank()) {
            return;
        }
        Set<String> current = new LinkedHashSet<>(getAll());
        if (current.add(viewId)) {
            JSONArray arr = new JSONArray();
            current.forEach(arr::put);
            AppConfig.setValue(SECTION, KEY, arr);
        }
    }

    /** بترجع كل أسماء الواجهات المسجلة، مرتبة أبجديًا. */
    public static List<String> getAll() {
        JSONArray arr = AppConfig.getArray(SECTION, KEY);
        List<String> result = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            result.add(arr.getString(i));
        }
        Collections.sort(result);
        return result;
    }
}
