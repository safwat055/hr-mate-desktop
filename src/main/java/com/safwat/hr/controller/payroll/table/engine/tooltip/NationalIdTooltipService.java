package com.safwat.hr.controller.payroll.table.engine.tooltip;

import javafx.application.Platform;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * =====================================================
 * NationalIdTooltipService — تولتيب الرقم القومي (بيرول إندكس)
 * =====================================================
 * <p>مسؤولية منفصلة تمامًا عن رسم الأعمدة: جلب نص التولتيب من الباك
 * إند (نداء شبكة غير متزامن يوفّره الكونترولر) مع Cache بسيط عشان
 * منعملش نفس النداء مرتين لنفس الرقم القومي. أي منطق شبكة/تخزين
 * مؤقت مستقبلي (زي انتهاء صلاحية الكاش) بيتغير هنا بس.</p>
 */
public class NationalIdTooltipService {

    private final Map<String, String> cache = new ConcurrentHashMap<>();
    @Setter
    private Function<String, CompletableFuture<String>> provider;
    /**
     * -- SETTER --
     * تفعيل/تعطيل التولتيب — مقصود إنها تتغيّر في أي وقت بعد الإنشاء (من
     * تشيك بوكس مثلاً)، مش بس وقت initializeExcelFeatures. بعد التغيير
     * لازم يتعمل tableView.refresh() (أو ExcelEngine.quickRefresh) عشان
     * الخلايا المعروضة حاليًا تعيد تقييم updateItem وتاخد القرار الجديد.
     */
    @Setter
    @Getter
    private boolean enabled = true;

    public boolean hasProvider() {
        return provider != null;
    }

    /**
     * بيبني Tooltip جاهز يتنسخ لكل خلية، مع تحميل النص عند العرض.
     */
    public Tooltip buildTooltip() {
        Tooltip tooltip = new Tooltip();
        tooltip.setShowDelay(Duration.millis(350));
        tooltip.setWrapText(false);
        tooltip.setStyle("-fx-font-size: 16px;-fx-padding: 10px;");
        return tooltip;
    }

    public void attachAndLoad(Tooltip tooltip, String nationalId) {
        tooltip.setOnShowing(e -> loadTooltip(nationalId, tooltip));
    }

    private void loadTooltip(String nationalId, Tooltip tooltip) {
        String cached = cache.get(nationalId);
        if (cached != null) {
            tooltip.setText(cached);
            return;
        }
        tooltip.setText("جارٍ التحميل...");

        provider.apply(nationalId).whenComplete((text, err) -> {
            String finalText = err != null
                    ? "تعذر جلب بيانات فهرس البيرول"
                    : (text == null || text.isBlank() ? "لا يوجد سجل فهرس بيرول لهذا الرقم القومي" : text);
            cache.put(nationalId, finalText);
            Platform.runLater(() -> tooltip.setText(finalText));
        });
    }
}