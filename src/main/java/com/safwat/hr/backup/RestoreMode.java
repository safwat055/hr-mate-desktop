package com.safwat.hr.backup;

public enum RestoreMode {
    /**
     * يمسح كل البيانات الموجودة أولاً ثم يستعيد من النسخة.
     * (--clean --if-exists)
     * آمن ونظيف — مفيش تعارض بيانات.
     */
    REPLACE,

    /**
     * يكتب فوق الموجود بدون حذف مسبق.
     * مفيد لو عايز تضيف بيانات ناقصة.
     * ممكن يسبب تعارض في Primary Keys لو البيانات موجودة.
     */
    MERGE
}