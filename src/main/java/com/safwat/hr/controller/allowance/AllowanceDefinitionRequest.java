package com.safwat.hr.controller.allowance;

import com.safwat.hr.controller.allowance.AllowanceDefinition.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Request لإنشاء أو تعديل snapshot في allowance_definition — نسخة الفرونت.
 *
 * <p>بيُبنى في {@code AllowanceDefinitionDialogController#handleSave()}
 * ويُرسل في:
 * <ul>
 *   <li>POST /api/allowances/definitions       — إنشاء بدل جديد</li>
 *   <li>PUT  /api/allowances/definitions/{id}  — تعديل snapshot موجود</li>
 * </ul>
 *
 * <p>يعكس بالضبط {@code AllowanceDefinitionRequest} record في الباك —
 * لكن الـ enums هنا من {@link AllowanceDefinition} في الفرونت
 * (مش من entity الباك) عشان مفيش dependency على الباك في الفرونت.
 *
 * <p>{@code valuesMap} عام قصدًا (Map<String, BigDecimal>) عشان يفضل مرن
 * مع أي CalcType — المفتاح ممكن يكون رقم درجة ("6") أو "all" أو "percent"
 * حسب calcType المختار.
 */
public record AllowanceDefinitionRequest(
        String code,
        String nameAr,
        String nameEn,
        LocalDate effectiveFrom,
        Behavior behavior,
        CalcType calcType,
        BaseSource baseSource,
        Scope scope,
        ElementType elementType,
        boolean subjectToInsurance,
        boolean subjectToTaxAndStamp,
        Map<String, BigDecimal> valuesMap,
        List<String> excludedMonths,
        List<String> eligibleLaws,
        List<String> eligibleLawCodes,
        TimelineAnchor timelineAnchor,        // ← لازم يكون موجود
        String notes
) {
}