package com.safwat.hr.ui.print;

import java.util.ArrayList;
import java.util.List;

/**
 * تحليل نطاق الصفحات مثل "1-3,5".
 */
public final class PrintRange {

    private PrintRange() {
    }

    /**
     * يحوّل النص لقائمة أرقام صفحات (من 1).
     * النص الفاضي أو "ALL" بيرجع null يعني كل الصفحات.
     */
    public static List<Integer> parse(String text) {
        if (text == null) return null;
        String t = text.trim();
        if (t.isEmpty() || t.equalsIgnoreCase("ALL")) return null;

        List<Integer> pages = new ArrayList<>();
        for (String part : t.split(",")) {
            part = part.trim();
            if (part.isEmpty()) continue;
            try {
                if (part.contains("-")) {
                    String[] bounds = part.split("-", 2);
                    int from = Integer.parseInt(bounds[0].trim());
                    int to = Integer.parseInt(bounds[1].trim());
                    if (from < 1 || to < from) throw new IllegalArgumentException();
                    for (int i = from; i <= to; i++) pages.add(i);
                } else {
                    int p = Integer.parseInt(part);
                    if (p < 1) throw new IllegalArgumentException();
                    pages.add(p);
                }
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("نطاق الصفحات غير صحيح: " + part);
            }
        }
        return pages.isEmpty() ? null : pages;
    }

    /**
     * يحوّل أرقام الصفحات (من 1) لفهارس (من 0) ويتأكد إنها موجودة في الملف.
     */
    public static List<Integer> resolve(List<Integer> onebased, int total) {
        List<Integer> out = new ArrayList<>();
        if (onebased == null) {
            for (int i = 0; i < total; i++) out.add(i);
            return out;
        }
        for (int p : onebased) {
            if (p > total) {
                throw new IllegalArgumentException("الصفحة " + p + " أكبر من عدد صفحات الملف (" + total + ")");
            }
            out.add(p - 1);
        }
        return out;
    }
}