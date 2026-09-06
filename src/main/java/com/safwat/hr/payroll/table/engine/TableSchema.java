package com.safwat.hr.payroll.table.engine;

/**
 * =====================================================
 * TableSchema — تعريف شكل جدول الإدخال (أعمدة/ثوابت)
 * =====================================================
 * <p>
 * مكان واحد لكل الثوابت اللي بتوصف بنية الجدول (عدد الأعمدة، مواقع
 * الأعمدة الثابتة، إلخ) بدل ما تتكرر أو تتوزع بين كل الكلاسات.
 * كل الكلاسات التانية في الـ package بتقرأ من هنا بدل ما تعرّف
 * نسختها الخاصة من نفس الأرقام.
 */
public final class TableSchema {

    /** إجمالي عدد الأعمدة: بحث + مسلسل + 5 أعمدة ثابتة + 20 عمود ديناميكي */
    public static final int COLUMN_COUNT = 27;

    public static final int SEARCH_COL = 0;
    public static final int SERIAL_COL = 1;
    public static final int NATIONAL_ID_COL = 2;
    public static final int CATEGORY_COL = 6;

    /** أول عمود ديناميكي — بعد إضافة عمود "الفئة" الثابت */
    public static final int FIRST_DYNAMIC_COL = 7;

    public static final int DEFAULT_ROWS = 20;

    public static final String[] STATIC_TITLES =
            {"البحث", "المسلسل", "الرقم القومى", "رقم الموظف", "الاسم", "الحالة", "الفئة"};

    private TableSchema() {
    }
}
