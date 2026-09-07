package com.safwat.hr.shared;

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

    private static final Map<String, List<WeakReference<Node>>> REGISTERED_TARGETS = new HashMap<>();

    private ZoomManager() {
    }

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

    private static double clamp(double v) {
        return Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, v));
    }

    public static void applyZoom(String viewId, Parent root) {
        if (root == null || viewId == null) return;
        ViewRegistry.register(viewId);
        Node target = resolveScalableTarget(root);
        registerTarget(viewId, target);
        setScale(target, loadZoom(viewId));
    }

    private static Node resolveScalableTarget(Parent root) {
        if (root instanceof ScrollPane) {
            ScrollPane sp = (ScrollPane) root;
            if (sp.getContent() != null) return sp.getContent();
        }
        return root;
    }

    private static void registerTarget(String viewId, Node target) {
        List<WeakReference<Node>> list = REGISTERED_TARGETS.computeIfAbsent(viewId, k -> Collections.synchronizedList(new ArrayList<>()));
        synchronized (list) {
            list.removeIf(ref -> ref.get() == null || ref.get() == target);
            list.add(new WeakReference<>(target));
        }
    }

    private static void setScale(Node node, double factor) {
        if (node == null) return;
        node.setScaleX(factor);
        node.setScaleY(factor);
    }

    private static void applyLive(String viewId, double factor) {
        List<WeakReference<Node>> list = REGISTERED_TARGETS.get(viewId);
        if (list == null) return;
        synchronized (list) {
            Iterator<WeakReference<Node>> it = list.iterator();
            while (it.hasNext()) {
                Node n = it.next().get();
                if (n == null) {
                    it.remove();
                    continue;
                }
                setScale(n, factor);
            }
        }
    }

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
        header.setStyle("-fx-background-color:" + C_CARD + "; -fx-border-color:" + C_BORDER + "; -fx-border-width:0 0 1 0;");

        VBox card = new VBox(16);
        card.setPadding(new Insets(22));
        card.setStyle("-fx-background-color:" + C_CARD + "; -fx-background-radius:10; -fx-border-color:" + C_BORDER + "; -fx-border-radius:10; -fx-border-width:1;");
        card.setMaxWidth(420);

        Label percentLbl = new Label(Math.round(saved * 100) + "%");
        percentLbl.setStyle("-fx-font-size:28px; -fx-font-weight:bold; -fx-text-fill:" + C_ACCENT + ";");
        percentLbl.setMaxWidth(Double.MAX_VALUE);
        percentLbl.setAlignment(Pos.CENTER);

        Slider slider = new Slider(MIN_ZOOM, MAX_ZOOM, saved);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(0.25);
        slider.setBlockIncrement(STEP);

        Button minusBtn = smallBtn("-");
        Button plusBtn = smallBtn("+");
        HBox sliderRow = new HBox(10, minusBtn, slider, plusBtn);
        sliderRow.setAlignment(Pos.CENTER);

        Label statusLabel = new Label("جاهز");
        statusLabel.setStyle("-fx-text-fill:" + C_MUTED + "; -fx-font-size:12px;");

        Button resetBtn = new Button("↺ 100%");
        resetBtn.setStyle("-fx-background-color:transparent; -fx-text-fill:" + C_MUTED + "; -fx-border-color:" + C_BORDER + "; -fx-border-radius:6; -fx-padding:6 12 6 12;");

        Button saveBtn = new Button("💾 حفظ");
        saveBtn.setDisable(true);
        saveBtn.setStyle("-fx-background-color:" + C_ACCENT + "; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:6; -fx-padding:6 16 6 16;");

        // ✅ استخدم Label بدون wrapText + قص + Tooltip
        Label hintLabel = new Label("الزوم بيتطبق فورًا وانت بتسحب الشريط على أي شاشة مفتوحة بنفس الاسم — اضغط \"حفظ\" عشان يفضل متذكر لما تفتح الشاشة تاني بعد كده.");
        hintLabel.setWrapText(false);
        hintLabel.setMaxWidth(380);
        hintLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        hintLabel.setStyle("-fx-font-size:10.5px; -fx-text-fill:" + C_MUTED + ";");
        Tooltip.install(hintLabel, new Tooltip("الزوم بيتطبق فورًا وانت بتسحب الشريط على أي شاشة مفتوحة بنفس الاسم — اضغط \"حفظ\" عشان يفضل متذكر لما تفتح الشاشة تاني بعد كده."));

        Runnable onValueChanged = () -> {
            double v = clamp(current[0]);
            current[0] = v;
            slider.setValue(v);
            percentLbl.setText(Math.round(v * 100) + "%");
            applyLive(viewId, v);
            saveBtn.setDisable(false);
            statusLabel.setText("تغيير غير محفوظ — الزوم اتطبق على الشاشة المفتوحة");
            statusLabel.setStyle("-fx-text-fill:" + C_WARN + "; -fx-font-size:11px;");
        };

        slider.valueProperty().addListener((obs, o, n) -> {
            current[0] = n.doubleValue();
            onValueChanged.run();
        });
        minusBtn.setOnAction(e -> {
            current[0] = current[0] - STEP;
            onValueChanged.run();
        });
        plusBtn.setOnAction(e -> {
            current[0] = current[0] + STEP;
            onValueChanged.run();
        });
        resetBtn.setOnAction(e -> {
            current[0] = DEFAULT_ZOOM;
            onValueChanged.run();
        });

        saveBtn.setOnAction(e -> {
            saveZoom(viewId, current[0]);
            saveBtn.setDisable(true);
            statusLabel.setText("✓ تم الحفظ");
            statusLabel.setStyle("-fx-text-fill:" + C_GREEN + "; -fx-font-size:12px;");
        });

        HBox footer = new HBox(10, statusLabel, spacer(), resetBtn, saveBtn);
        footer.setAlignment(Pos.CENTER_LEFT);


        card.getChildren().addAll(percentLbl, sliderRow, hintLabel, footer);

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

    private static Button smallBtn(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:" + C_CARD + "; -fx-text-fill:" + C_TEXT + "; -fx-border-color:" + C_BORDER + "; -fx-border-radius:6; -fx-background-radius:6; -fx-min-width:34; -fx-min-height:30; -fx-font-weight:bold; -fx-cursor:hand;");
        return b;
    }

    private static Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }
}