package com.safwat.hr.shared;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.lang.ref.WeakReference;
import java.util.*;

/**
 * ZoomManager — زوم حقيقي مثل المتصفح.
 *
 * المنطق:
 * - لو الـ root (أو أحد أبناؤه المباشرين) ScrollPane → نشتغل عليه مباشرة.
 * - لو مفيش ScrollPane → نحط الـ root جوه ScrollPane جديد ونستبدله في أبوه.
 * - الزوم بيشتغل على الـ width والـ height مع بعض (setScaleX + setScaleY).
 * - ScrollBar بتظهر تلقائياً لما المحتوى يكبر، وتختفي عند 100%.
 * - زر "🔄 100%" يمسح المحفوظ ويرجع الزوم للافتراضي فوراً.
 * - ✅ بديل Alert لتجنب GTK nested event loop crash.
 */
public class ZoomManager {

    private static final String SECTION      = "zoom";
    private static final double MIN_ZOOM     = 0.5;
    private static final double MAX_ZOOM     = 2.0;
    private static final double DEFAULT_ZOOM = 1.0;
    private static final double STEP         = 0.1;

    private static final String C_BG     = "#1a1d2e";
    private static final String C_CARD   = "#242740";
    private static final String C_ACCENT = "#4f8ef7";
    private static final String C_GREEN  = "#43c59e";
    private static final String C_WARN   = "#f5a623";
    private static final String C_TEXT   = "#e8eaf6";
    private static final String C_MUTED  = "#8b90b8";
    private static final String C_BORDER = "#333659";

    /** viewId → قائمة ScrollPane المسجّلة (WeakRef عشان GC يشتغل بحرية) */
    private static final Map<String, List<WeakReference<ScrollPane>>> REGISTERED = new HashMap<>();

    private ZoomManager() {}

    // ==================== حفظ / تحميل ====================

    private static double loadZoom(String viewId) {
        String raw = AppConfig.getString(SECTION, viewId, String.valueOf(DEFAULT_ZOOM));
        try { return clamp(Double.parseDouble(raw)); }
        catch (NumberFormatException e) { return DEFAULT_ZOOM; }
    }

    private static void saveZoom(String viewId, double factor) {
        AppConfig.setValue(SECTION, viewId, String.valueOf(clamp(factor)));
    }

    public static void resetToDefault(String viewId) {
        AppConfig.removeValue(SECTION, viewId);
        applyLive(viewId, DEFAULT_ZOOM);
    }

    private static double clamp(double v) {
        return Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, v));
    }

    // ==================== تطبيق على الواجهة ====================

    /**
     * نقطة الدخول الرئيسية — بيتنادى من ViewManager/TabManager بعد تحميل كل واجهة.
     *
     * الخوارزمية:
     * 1. نبحث عن ScrollPane موجود (الـ root نفسه، أو أول child مباشر).
     * 2. لو مفيش → نلف الـ root في ScrollPane جديد ونستبدله في أبوه.
     * 3. نسجّل الـ ScrollPane ونطبق الزوم المحفوظ.
     */
    public static void applyZoom(String viewId, Parent root) {
        if (root == null || viewId == null) return;
        ViewRegistry.register(viewId);

        ScrollPane sp = findOrWrap(root);
        register(viewId, sp);
        applyFactor(sp, loadZoom(viewId));
    }

    /**
     * بيرجع ScrollPane جاهز:
     * 1. الـ root نفسه ScrollPane → ارجعه.
     * 2. أول child مباشر ScrollPane → استخدمه.
     * 3. لو مفيش → ابني ScrollPane جديد واستبدل الـ root في أبوه.
     */
    private static ScrollPane findOrWrap(Parent root) {

        // الحالة 1
        if (root instanceof ScrollPane sp) return sp;

        // الحالة 2 — أول child مباشر
        for (Node child : root.getChildrenUnmodifiable()) {
            if (child instanceof ScrollPane sp) return sp;
        }

        // الحالة 3 — نلف في ScrollPane جديد
        ScrollPane sp = new ScrollPane(root);
        sp.setFitToWidth(false);
        sp.setFitToHeight(false);
        sp.setPannable(true);
        sp.setStyle(
                "-fx-background:" + C_BG + ";"
                        + "-fx-background-color:" + C_BG + ";"
                        + "-fx-focus-color:transparent;"
                        + "-fx-faint-focus-color:transparent;");

        Parent parent = root.getParent();
        if (parent instanceof Pane pane) {
            int idx = pane.getChildren().indexOf(root);
            if (idx >= 0) {
                pane.getChildren().set(idx, sp);
                HBox.setHgrow(sp, HBox.getHgrow(root));
                VBox.setVgrow(sp, VBox.getVgrow(root));
                BorderPane.setAlignment(sp, BorderPane.getAlignment(root));
                BorderPane.setMargin(sp, BorderPane.getMargin(root));
            }
        } else if (parent instanceof BorderPane bp) {
            if      (bp.getTop()    == root) bp.setTop(sp);
            else if (bp.getCenter() == root) bp.setCenter(sp);
            else if (bp.getBottom() == root) bp.setBottom(sp);
            else if (bp.getLeft()   == root) bp.setLeft(sp);
            else if (bp.getRight()  == root) bp.setRight(sp);
        }

        return sp;
    }

    /**
     * بيطبق الـ factor على الـ ScrollPane:
     * - Scale على الـ content (width + height) → نصوص وعناصر بتتكبر فعلياً
     * - ScrollBar بتظهر AS_NEEDED لما الزوم > 100%
     * - عند 100% → بنلغي الـ scrollbars تماماً
     */
    private static void applyFactor(ScrollPane sp, double factor) {
        Node content = sp.getContent();
        if (content == null) return;

        // نخزّن الأبعاد الأصلية مرة واحدة
        if (!content.getProperties().containsKey("_zoom_origW")) {
            double w = content instanceof Region r ? r.getPrefWidth()  : -1;
            double h = content instanceof Region r2 ? r2.getPrefHeight() : -1;
            if (w <= 0) w = content.getBoundsInLocal().getWidth();
            if (h <= 0) h = content.getBoundsInLocal().getHeight();
            content.getProperties().put("_zoom_origW", w);
            content.getProperties().put("_zoom_origH", h);
        }

        double origW = (double) content.getProperties().get("_zoom_origW");
        double origH = (double) content.getProperties().get("_zoom_origH");

        // تكبير الـ content نفسه (width + height مع بعض)
        content.setScaleX(factor);
        content.setScaleY(factor);

        // تعديل prefSize عشان الـ ScrollPane يحسب الـ scrollbar صح
        if (content instanceof Region r) {
            if (origW > 0) r.setPrefWidth(origW  * factor);
            if (origH > 0) r.setPrefHeight(origH * factor);
        }

        // ScrollBar — تظهر بس لما في حاجة تتسكرول
        boolean needsScroll = Math.abs(factor - DEFAULT_ZOOM) > 0.01;
        sp.setHbarPolicy(needsScroll ? ScrollPane.ScrollBarPolicy.AS_NEEDED : ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(needsScroll ? ScrollPane.ScrollBarPolicy.AS_NEEDED : ScrollPane.ScrollBarPolicy.NEVER);
    }

    // ==================== Live update ====================

    private static void register(String viewId, ScrollPane sp) {
        List<WeakReference<ScrollPane>> list =
                REGISTERED.computeIfAbsent(viewId, k -> Collections.synchronizedList(new ArrayList<>()));
        synchronized (list) {
            list.removeIf(ref -> ref.get() == null || ref.get() == sp);
            list.add(new WeakReference<>(sp));
        }
    }

    private static void applyLive(String viewId, double factor) {
        List<WeakReference<ScrollPane>> list = REGISTERED.get(viewId);
        if (list == null) return;
        synchronized (list) {
            Iterator<WeakReference<ScrollPane>> it = list.iterator();
            while (it.hasNext()) {
                ScrollPane sp = it.next().get();
                if (sp == null) { it.remove(); continue; }
                applyFactor(sp, factor);
            }
        }
    }

    // ==================== Panel ====================

    public static Parent buildPanel(String viewId) {
        double saved     = loadZoom(viewId);
        double[] current = {saved};

        // ---------- Header ----------
        Label headerIcon = new Label("🔍");
        headerIcon.setStyle("-fx-font-size:20px;");
        Label headerTitle = new Label("مستوى التكبير");
        headerTitle.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:" + C_TEXT + ";");
        Label headerSubtitle = new Label("تخصيص حجم واجهة: " + viewId);
        headerSubtitle.setStyle("-fx-font-size:11px; -fx-text-fill:" + C_MUTED + ";");
        VBox titleBox = new VBox(2, headerTitle, headerSubtitle);

        HBox header = new HBox(12, headerIcon, titleBox);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 20, 14, 20));
        header.setStyle("-fx-background-color:" + C_CARD + "; -fx-border-color:" + C_BORDER
                + "; -fx-border-width:0 0 1 0;");

        // ---------- Card ----------
        VBox card = new VBox(16);
        card.setPadding(new Insets(22));
        card.setStyle("-fx-background-color:" + C_CARD + "; -fx-background-radius:10;"
                + "-fx-border-color:" + C_BORDER + "; -fx-border-radius:10; -fx-border-width:1;");
        card.setMaxWidth(440);

        Label percentLbl = new Label(fmt(saved));
        percentLbl.setStyle("-fx-font-size:28px; -fx-font-weight:bold; -fx-text-fill:" + C_ACCENT + ";");
        percentLbl.setMaxWidth(Double.MAX_VALUE);
        percentLbl.setAlignment(Pos.CENTER);

        // معاينة حية
        Label previewLbl = new Label("نموذج معاينة — هكذا يبدو الحجم على الشاشة");
        previewLbl.setStyle(previewStyle(saved));
        previewLbl.setMaxWidth(Double.MAX_VALUE);
        previewLbl.setAlignment(Pos.CENTER);

        Slider slider = new Slider(MIN_ZOOM, MAX_ZOOM, saved);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(0.25);
        slider.setBlockIncrement(STEP);
        HBox.setHgrow(slider, Priority.ALWAYS);

        Button minusBtn = smallBtn("−");
        Button plusBtn  = smallBtn("+");
        HBox sliderRow  = new HBox(10, minusBtn, slider, plusBtn);
        sliderRow.setAlignment(Pos.CENTER);

        Label hintLabel = new Label(
                "الزوم بيكبّر المحتوى فعلياً — scrollbar بتظهر تلقائياً عند الحاجة. "
                        + "اضغط \"حفظ\" عشان يتذكر الإعداد.");
        hintLabel.setWrapText(true);
        hintLabel.setStyle("-fx-font-size:10.5px; -fx-text-fill:" + C_MUTED + ";");

        // ---------- Footer ----------
        Label statusLabel = new Label("جاهز");
        statusLabel.setStyle("-fx-text-fill:" + C_MUTED + "; -fx-font-size:12px;");

        Button resetBtn = new Button("🔄 100%");
        resetBtn.setStyle("-fx-background-color:transparent; -fx-text-fill:" + C_MUTED
                + "; -fx-border-color:" + C_BORDER + "; -fx-border-radius:6; -fx-padding:6 12 6 12; -fx-cursor:hand;");
        Tooltip.install(resetBtn, new Tooltip("يرجع الزوم لـ 100% ويمسح الإعداد المحفوظ"));

        Button saveBtn = new Button("💾 حفظ");
        saveBtn.setDisable(true);
        saveBtn.setStyle("-fx-background-color:" + C_ACCENT + "; -fx-text-fill:white;"
                + "-fx-font-weight:bold; -fx-background-radius:6; -fx-padding:6 16 6 16;");

        // ---------- منطق التغيير ----------
        Runnable onChanged = () -> {
            double v = clamp(current[0]);
            current[0] = v;
            slider.setValue(v);
            percentLbl.setText(fmt(v));
            previewLbl.setStyle(previewStyle(v));
            applyLive(viewId, v);
            saveBtn.setDisable(false);
            statusLabel.setText("تغيير غير محفوظ — الزوم اتطبق على الشاشة المفتوحة");
            statusLabel.setStyle("-fx-text-fill:" + C_WARN + "; -fx-font-size:11px;");
        };

        slider.valueProperty().addListener((obs, o, n) -> {
            current[0] = n.doubleValue(); onChanged.run();
        });
        minusBtn.setOnAction(e -> { current[0] -= STEP; onChanged.run(); });
        plusBtn.setOnAction(e  -> { current[0] += STEP; onChanged.run(); });

        resetBtn.setOnAction(e -> {
            resetToDefault(viewId);
            current[0] = DEFAULT_ZOOM;
            slider.setValue(DEFAULT_ZOOM);
            percentLbl.setText(fmt(DEFAULT_ZOOM));
            previewLbl.setStyle(previewStyle(DEFAULT_ZOOM));
            saveBtn.setDisable(true);
            statusLabel.setText("✓ تم الرجوع لـ 100% ومسح الإعداد المحفوظ");
            statusLabel.setStyle("-fx-text-fill:" + C_GREEN + "; -fx-font-size:12px;");
        });

        saveBtn.setOnAction(e -> {
            saveZoom(viewId, current[0]);
            saveBtn.setDisable(true);
            statusLabel.setText("✓ تم الحفظ");
            statusLabel.setStyle("-fx-text-fill:" + C_GREEN + "; -fx-font-size:12px;");
        });

        HBox footer = new HBox(10, statusLabel, spacer(), resetBtn, saveBtn);
        footer.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(percentLbl, previewLbl, sliderRow, hintLabel, footer);

        VBox body = new VBox(card);
        body.setAlignment(Pos.TOP_CENTER);
        body.setPadding(new Insets(30));
        body.setStyle("-fx-background-color:" + C_BG + ";");

        BorderPane root = new BorderPane();
        root.setTop(header);
        root.setCenter(body);
        root.setStyle("-fx-background-color:" + C_BG + ";");
        return root;
    }

    // ==================== Helpers ====================

    private static String fmt(double factor) {
        return Math.round(factor * 100) + "%";
    }

    private static String previewStyle(double factor) {
        int size = (int) Math.round(13 * factor);
        return "-fx-font-size:" + size + "px; -fx-text-fill:" + C_TEXT
                + "; -fx-background-color:#1a1d2e; -fx-padding:10 14 10 14;"
                + "-fx-background-radius:6; -fx-border-color:" + C_BORDER + "; -fx-border-radius:6;";
    }

    private static Button smallBtn(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:" + C_CARD + "; -fx-text-fill:" + C_TEXT
                + "; -fx-border-color:" + C_BORDER + "; -fx-border-radius:6; -fx-background-radius:6;"
                + "-fx-min-width:34; -fx-min-height:30; -fx-font-weight:bold; -fx-cursor:hand;");
        return b;
    }

    private static Region spacer() {
        Region r = new Region(); HBox.setHgrow(r, Priority.ALWAYS); return r;
    }
}