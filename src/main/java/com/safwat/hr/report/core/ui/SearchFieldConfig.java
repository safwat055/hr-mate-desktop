package com.safwat.hr.report.core.ui;

import java.util.function.Function;

/**
 * إعدادات نافذة البحث المرتبطة بحقل إدخال معين.
 *
 * <p><b>جديد:</b> دعم تحويل قيمة الحقل المعتمد (Value Converter) قبل الإرسال للـ DataSource.
 */
public class SearchFieldConfig {

    private final UiField field;
    private final String dialogTitle;
    private final String dataSource;
    private final boolean multiSelect;
    private final UiField dependentField;
    private final String dependentParamKey;

    // ═══════════════════════════════════════════════════════
    //  جديد — محول قيمة الحقل المعتمد (اختياري)
    // ═══════════════════════════════════════════════════════
    /**
     * دالة تحويل قيمة الحقل المعتمد قبل الإرسال للـ DataSource.
     * مثال: تحويل "01/2026" → "2026-01-01"
     */
    private final Function<String, String> dependentValueConverter;

    private SearchFieldConfig(UiField field, String dialogTitle, String dataSource,
                              boolean multiSelect, UiField dependentField,
                              String dependentParamKey, Function<String, String> dependentValueConverter) {
        this.field = field;
        this.dialogTitle = dialogTitle;
        this.dataSource = dataSource;
        this.multiSelect = multiSelect;
        this.dependentField = dependentField;
        this.dependentParamKey = dependentParamKey;
        this.dependentValueConverter = dependentValueConverter;
    }

    // ─── Factory Methods (القديمة — بدون تغيير) ───

    public static SearchFieldConfig of(UiField field, String dialogTitle, String dataSource) {
        return new SearchFieldConfig(field, dialogTitle, dataSource, false, null, null, null);
    }

    public static SearchFieldConfig multiSelectOf(UiField field, String dialogTitle, String dataSource) {
        return new SearchFieldConfig(field, dialogTitle, dataSource, true, null, null, null);
    }

    public static SearchFieldConfig multiSelectOf(UiField field, String dialogTitle, String dataSource,
                                                  UiField dependentField, String dependentParamKey) {
        return new SearchFieldConfig(field, dialogTitle, dataSource, true, dependentField, dependentParamKey, null);
    }

    // ═══════════════════════════════════════════════════════
    //  جديد — مع محول قيمة
    // ═══════════════════════════════════════════════════════

    /**
     * بحث متعدد معتمد على حلف تاني + محول قيمة
     *
     * @param dependentValueConverter دالة تحويل القيمة (مثلاً: v -> "2026-" + v.substring(0,2) + "-01")
     */
    public static SearchFieldConfig multiSelectOf(UiField field, String dialogTitle, String dataSource,
                                                  UiField dependentField, String dependentParamKey,
                                                  Function<String, String> dependentValueConverter) {
        return new SearchFieldConfig(field, dialogTitle, dataSource, true,
                dependentField, dependentParamKey, dependentValueConverter);
    }

    // ─── Getters ───

    public UiField getField() {
        return field;
    }

    public String getDialogTitle() {
        return dialogTitle;
    }

    public String getDataSource() {
        return dataSource;
    }

    public boolean isMultiSelect() {
        return multiSelect;
    }

    public UiField getDependentField() {
        return dependentField;
    }

    public String getDependentParamKey() {
        return dependentParamKey;
    }

    public Function<String, String> getDependentValueConverter() {
        return dependentValueConverter;
    }
}