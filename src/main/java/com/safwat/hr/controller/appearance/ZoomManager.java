package com.safwat.hr.controller.appearance;

import com.safwat.hr.shared.AppConfig;
import com.safwat.hr.shared.ViewRegistry;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.lang.ref.WeakReference;
import java.util.*;

public class ZoomManager {

    private static final String SECTION = "zoom";
    private static final double MIN_ZOOM = 0.5;
    private static final double MAX_ZOOM = 2.0;
    private static final double DEFAULT_ZOOM = 1.0;
    private static final double STEP = 0.1;

    private static final String C_BG = "#1a1d2e";
    private static final String C_CARD = "#242740";
    private static final String C_ACCENT = "#4f8ef7";
    private static final String C_GREEN = "#43c59e";
    private static final String C_WARN = "#f5a623";
    private static final String C_TEXT = "#e8eaf6";
    private static final String C_MUTED = "#8b90b8";
    private static final String C_BORDER = "#333659";

    private static final Map<String, List<WeakReference<ZoomTarget>>> REGISTERED = new HashMap<>();

    private ZoomManager() {
    }

    // ── ZoomTarget: بيحفظ كل المعلومات اللازمة للـ live update ──
    private static class ZoomTarget {
        final ScrollPane scrollPane;
        final Node content;
        double origW = -1;
        double origH = -1;

        ZoomTarget(ScrollPane scrollPane, Node content) {
            this.scrollPane = scrollPane;
            this.content = content;
        }

        /**
         * بترجع true لما الأبعاد اتحسبت فعلاً
         */
        boolean hasOrigin() {
            return origW > 0 && origH > 0;
        }

        /**
         * بتحفظ الأبعاد الأصلية من layoutBounds الفعلية
         */
        void captureOrigin() {
            if (content instanceof Region r) {
                double pw = r.getPrefWidth();
                double ph = r.getPrefHeight();
                origW = (pw > 0 && pw != Region.USE_COMPUTED_SIZE)
                        ? pw : content.getBoundsInLocal().getWidth();
                origH = (ph > 0 && ph != Region.USE_COMPUTED_SIZE)
                        ? ph : content.getBoundsInLocal().getHeight();
            } else {
                origW = content.getBoundsInLocal().getWidth();
                origH = content.getBoundsInLocal().getHeight();
            }
        }
    }

    // ==================== حفظ / تحميل ====================

    private static double loadZoom(String viewId) {
        String raw = AppConfig.getString(SECTION, viewId, String.valueOf(DEFAULT_ZOOM));
        try {
            return clamp(Double.parseDouble(raw));
        } catch (NumberFormatException e) {
            return DEFAULT_ZOOM;
        }
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

    // ==================== نقطة الدخول ====================

    /**
     * بيتنادى من ViewManager/TabManager بعد تحميل الواجهة.
     * بيستنى الـ layout يخلص الأول (Platform.runLater مزدوج)
     * عشان يقدر يقرأ الأبعاد الحقيقية للـ content.
     */
    public static void applyZoom(String viewId, Parent root) {
        if (root == null || viewId == null) return;
        ViewRegistry.register(viewId);

        double factor = loadZoom(viewId);

        // لو الزوم = 100% مش محتاجين نعمل أي حاجة
        if (Math.abs(factor - DEFAULT_ZOOM) < 0.005) return;

        // نستنى pulse واحد + runLater عشان الـ layout يكمل
        Platform.runLater(() -> Platform.runLater(() -> {
            ScrollPane sp = findOrWrap(root);
            Node content = sp.getContent();
            if (content == null) return;

            ZoomTarget target = new ZoomTarget(sp, content);
            target.captureOrigin();

            // لو الأبعاد لسه مش جاهزة → نستنى الـ layout يكمل
            if (!target.hasOrigin()) {
                content.layoutBoundsProperty().addListener((obs, o, n) -> {
                    if (n.getWidth() > 0 && n.getHeight() > 0 && !target.hasOrigin()) {
                        target.captureOrigin();
                        registerTarget(viewId, target);
                        applyFactor(target, factor);
                    }
                });
                return;
            }

            registerTarget(viewId, target);
            applyFactor(target, factor);
        }));
    }

    // ==================== findOrWrap ====================

    private static ScrollPane findOrWrap(Parent root) {
        if (root instanceof ScrollPane sp) return sp;

        for (Node child : root.getChildrenUnmodifiable()) {
            if (child instanceof ScrollPane sp) return sp;
        }

        // مفيش ScrollPane → نلف الـ root
        ScrollPane sp = new ScrollPane(root);
        sp.setFitToWidth(false);
        sp.setFitToHeight(false);
        sp.setPannable(true);
        sp.setStyle(
                "-fx-background:" + C_BG + ";"
                        + "-fx-background-color:" + C_BG + ";"
                        + "-fx-focus-color:transparent;"
                        + "-fx-faint-focus-color:transparent;");

        replaceInParent(root.getParent(), root, sp);
        return sp;
    }

    private static void replaceInParent(Parent parent, Node oldNode, Node newNode) {
        if (parent == null) return;
        if (parent instanceof Pane pane) {
            int idx = pane.getChildren().indexOf(oldNode);
            if (idx >= 0) {
                HBox.setHgrow(newNode, HBox.getHgrow(oldNode));
                VBox.setVgrow(newNode, VBox.getVgrow(oldNode));
                BorderPane.setAlignment(newNode, BorderPane.getAlignment(oldNode));
                BorderPane.setMargin(newNode, BorderPane.getMargin(oldNode));
                pane.getChildren().set(idx, newNode);
            }
        } else if (parent instanceof BorderPane bp) {
            if (bp.getTop() == oldNode) bp.setTop(newNode);
            else if (bp.getCenter() == oldNode) bp.setCenter(newNode);
            else if (bp.getBottom() == oldNode) bp.setBottom(newNode);
            else if (bp.getLeft() == oldNode) bp.setLeft(newNode);
            else if (bp.getRight() == oldNode) bp.setRight(newNode);
        }
    }

    // ==================== تطبيق الزوم ====================

    /**
     * الزوم الحقيقي:
     * - بيغير prefWidth/prefHeight للـ content بالنسبة للأبعاد الأصلية
     * - الـ ScrollPane يشوف المحتوى كبر ويعرض scrollbar تلقائياً
     */
    private static void applyFactor(ZoomTarget target, double factor) {
        if (!target.hasOrigin()) return;
        Node content = target.content;

        if (content instanceof Region r) {
            if (Math.abs(factor - DEFAULT_ZOOM) < 0.005) {
                // رجوع للأصل
                r.setPrefWidth(target.origW);
                r.setPrefHeight(target.origH);
                r.setMinWidth(Region.USE_COMPUTED_SIZE);
                r.setMinHeight(Region.USE_COMPUTED_SIZE);
            } else {
                r.setPrefWidth(target.origW * factor);
                r.setPrefHeight(target.origH * factor);
                r.setMinWidth(target.origW * factor);
                r.setMinHeight(target.origH * factor);
            }
        }

        boolean needsScroll = Math.abs(factor - DEFAULT_ZOOM) > 0.005;
        target.scrollPane.setHbarPolicy(
                needsScroll ? ScrollPane.ScrollBarPolicy.AS_NEEDED : ScrollPane.ScrollBarPolicy.NEVER);
        target.scrollPane.setVbarPolicy(
                needsScroll ? ScrollPane.ScrollBarPolicy.AS_NEEDED : ScrollPane.ScrollBarPolicy.NEVER);
    }

    // ==================== Live update ====================

    private static void registerTarget(String viewId, ZoomTarget target) {
        List<WeakReference<ZoomTarget>> list =
                REGISTERED.computeIfAbsent(viewId, k -> Collections.synchronizedList(new ArrayList<>()));
        synchronized (list) {
            list.removeIf(ref -> {
                ZoomTarget t = ref.get();
                return t == null || t.scrollPane == target.scrollPane;
            });
            list.add(new WeakReference<>(target));
        }
    }

    static void applyLive(String viewId, double factor) {
        List<WeakReference<ZoomTarget>> list = REGISTERED.get(viewId);
        if (list == null) return;
        synchronized (list) {
            Iterator<WeakReference<ZoomTarget>> it = list.iterator();
            while (it.hasNext()) {
                ZoomTarget t = it.next().get();
                if (t == null) {
                    it.remove();
                    continue;
                }
                applyFactor(t, factor);
            }
        }
    }

    // ==================== Panel ====================

    public static Parent buildPanel(String viewId) {
        double saved = loadZoom(viewId);
        double[] current = {saved};

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

        VBox card = new VBox(16);
        card.setPadding(new Insets(22));
        card.setStyle("-fx-background-color:" + C_CARD + "; -fx-background-radius:10;"
                + "-fx-border-color:" + C_BORDER + "; -fx-border-radius:10; -fx-border-width:1;");
        card.setMaxWidth(440);

        Label percentLbl = new Label(fmt(saved));
        percentLbl.setStyle("-fx-font-size:28px; -fx-font-weight:bold; -fx-text-fill:" + C_ACCENT + ";");
        percentLbl.setMaxWidth(Double.MAX_VALUE);
        percentLbl.setAlignment(Pos.CENTER);

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
        Button plusBtn = smallBtn("+");
        HBox sliderRow = new HBox(10, minusBtn, slider, plusBtn);
        sliderRow.setAlignment(Pos.CENTER);

        Label hintLabel = new Label(
                "الزوم بيكبّر المحتوى فعلياً — scrollbar بتظهر تلقائياً عند الحاجة. "
                        + "اضغط \"حفظ\" عشان يتذكر الإعداد.");
        hintLabel.setWrapText(true);
        hintLabel.setStyle("-fx-font-size:10.5px; -fx-text-fill:" + C_MUTED + ";");

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
            current[0] = n.doubleValue();
            onChanged.run();
        });
        minusBtn.setOnAction(e -> {
            current[0] -= STEP;
            onChanged.run();
        });
        plusBtn.setOnAction(e -> {
            current[0] += STEP;
            onChanged.run();
        });

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

    private static String fmt(double f) {
        return Math.round(f * 100) + "%";
    }

    private static String previewStyle(double f) {
        return "-fx-font-size:" + (int) Math.round(13 * f) + "px; -fx-text-fill:" + C_TEXT
                + "; -fx-background-color:#1a1d2e; -fx-padding:10 14 10 14;"
                + "-fx-background-radius:6; -fx-border-color:" + C_BORDER + "; -fx-border-radius:6;";
    }

    private static Button smallBtn(String t) {
        Button b = new Button(t);
        b.setStyle("-fx-background-color:" + C_CARD + "; -fx-text-fill:" + C_TEXT
                + "; -fx-border-color:" + C_BORDER + "; -fx-border-radius:6; -fx-background-radius:6;"
                + "-fx-min-width:34; -fx-min-height:30; -fx-font-weight:bold; -fx-cursor:hand;");
        return b;
    }

    private static Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }
}