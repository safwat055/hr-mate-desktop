package com.safwat.hr.controller.appearance;

import com.safwat.hr.shared.AppConfig;
import com.safwat.hr.ui.theme.SettingsThemeLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorSettingsManager {

    private static final String THEMES_DIR = "/com/safwat/hr/css/";
    private static final String OVERRIDES_FILE = System.getProperty("user.dir")
            + "/app/config/user-theme-overrides.css";

    private static final Map<String, String> ARABIC_LABELS = new LinkedHashMap<>();

    static {
        ARABIC_LABELS.put("-app-bg", "خلفية التطبيق");
        ARABIC_LABELS.put("-card-bg", "خلفية الكروت");
        ARABIC_LABELS.put("-card-border", "حدود الكروت");
        ARABIC_LABELS.put("-card-shadow", "ظل الكروت");
        ARABIC_LABELS.put("-text-primary", "لون النص الأساسي");
        ARABIC_LABELS.put("-text-secondary", "لون النص الثانوي");
        ARABIC_LABELS.put("-text-muted", "لون النص الباهت");
        ARABIC_LABELS.put("-on-brand", "لون النص فوق لون العلامة");
        ARABIC_LABELS.put("-brand-navy", "اللون الكحلي (العلامة)");
        ARABIC_LABELS.put("-header-alt-bg", "خلفية الهيدر البديلة");
        ARABIC_LABELS.put("-color-primary", "اللون الأساسي (Primary)");
        ARABIC_LABELS.put("-color-danger", "لون الخطر / الحذف");
        ARABIC_LABELS.put("-color-success", "لون النجاح");
        ARABIC_LABELS.put("-color-purple", "اللون البنفسجي");
        ARABIC_LABELS.put("-color-secondary-btn", "لون الأزرار الثانوية");
        ARABIC_LABELS.put("-color-warning", "لون التحذير");
        ARABIC_LABELS.put("-border-color", "لون الحدود");
        ARABIC_LABELS.put("-border-gray", "لون الحدود الرمادي");
        ARABIC_LABELS.put("-divider-color", "لون الفواصل");
        ARABIC_LABELS.put("-soft-box-bg", "خلفية الصناديق الناعمة");
        ARABIC_LABELS.put("-soft-box-border", "حدود الصناديق الناعمة");
        ARABIC_LABELS.put("-control-bg", "خلفية عناصر التحكم");
        ARABIC_LABELS.put("-table-header-bg", "خلفية رأس الجدول");
        ARABIC_LABELS.put("-row-alt-bg", "خلفية الصف البديل");
        ARABIC_LABELS.put("-selection-bg", "لون التحديد");
    }

    private static final Pattern COLOR_VAR_PATTERN =
            Pattern.compile("(-[a-zA-Z][a-zA-Z0-9-]*)\\s*:\\s*(#[0-9A-Fa-f]{3,8}|rgba?\\([^)]*\\))\\s*;");

    private static final List<WeakReference<Scene>> REGISTERED_SCENES =
            Collections.synchronizedList(new ArrayList<>());

    private ColorSettingsManager() {
    }

    private static class ColorVar {
        final String key;
        final String arabicLabel;
        final String themeDefault;
        String currentValue;

        ColorVar(String key, String arabicLabel, String themeDefault, String currentValue) {
            this.key = key;
            this.arabicLabel = arabicLabel;
            this.themeDefault = themeDefault;
            this.currentValue = currentValue;
        }
    }

    private static String readResourceQuietly(String resourcePath) {
        try (InputStream is = ColorSettingsManager.class.getResourceAsStream(resourcePath)) {
            if (is == null) return "";
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    private static String readFileQuietly(Path filePath) {
        try {
            if (!Files.exists(filePath)) return "";
            return Files.readString(filePath);
        } catch (IOException e) {
            return "";
        }
    }

    private static void ensureOverridesFileExists() {
        Path file = Paths.get(OVERRIDES_FILE);
        if (Files.exists(file)) return;
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, "/* ملف تخصيص الألوان */\n.root {\n}\n");
        } catch (IOException ignored) {
        }
    }

    private static Map<String, String> parseColorVars(String css) {
        Map<String, String> result = new LinkedHashMap<>();
        if (css == null || css.isEmpty()) return result;
        Matcher m = COLOR_VAR_PATTERN.matcher(css);
        while (m.find()) {
            String key = m.group(1);
            if (key.startsWith("-fx-")) continue;
            result.putIfAbsent(key, m.group(2));
        }
        return result;
    }

    private static List<ColorVar> loadColorVars() {
        String themeName = AppConfig.getString("ui", "theme", "theme-blue.css");
        String themeContent = readResourceQuietly(THEMES_DIR + themeName);
        Map<String, String> themeDefaults = parseColorVars(themeContent);
        Map<String, String> savedOverrides = parseColorVars(readFileQuietly(Paths.get(OVERRIDES_FILE)));

        List<ColorVar> result = new ArrayList<>();
        for (Map.Entry<String, String> e : themeDefaults.entrySet()) {
            String key = e.getKey();
            String defaultValue = e.getValue();
            String current = savedOverrides.getOrDefault(key, defaultValue);
            String label = ARABIC_LABELS.getOrDefault(key, key);
            result.add(new ColorVar(key, label, defaultValue, current));
        }
        return result;
    }

    private static void saveColorVars(List<ColorVar> vars) {
        StringBuilder sb = new StringBuilder();
        sb.append("/* تم إنشاء هذا الملف تلقائياً من شاشة تخصيص الألوان — لا تعدله يدويًا */\n");
        sb.append(".root {\n");
        for (ColorVar v : vars) {
            sb.append("    ").append(v.key).append(": ").append(v.currentValue).append(";\n");
        }
        sb.append("}\n");

        try {
            Path file = Paths.get(OVERRIDES_FILE);
            Files.createDirectories(file.getParent());
            Files.writeString(file, sb.toString());
        } catch (IOException ignored) {
        }

        AppConfig.setValue("ui", "colorOverridesVersion", String.valueOf(System.currentTimeMillis()));
    }

    private static String overridesUrlBase() {
        return Paths.get(OVERRIDES_FILE).toUri().toString();
    }

    private static String overridesUrlVersioned() {
        String version = AppConfig.getString("ui", "colorOverridesVersion", "0");
        return overridesUrlBase() + "?v=" + version;
    }

    public static void attachTheme(Scene scene, String themeFileName) {
        if (scene == null) return;
        ensureOverridesFileExists();
        String themeResource = THEMES_DIR + themeFileName;
        try {
            String themeUrl = Objects.requireNonNull(
                    ColorSettingsManager.class.getResource(themeResource)).toExternalForm();
            scene.getStylesheets().add(themeUrl);
        } catch (Exception e) {
            System.err.println("تعذر تحميل ملف الثيم: " + themeResource);
        }
        scene.getStylesheets().add(overridesUrlVersioned());
        registerScene(scene);
    }

    private static void registerScene(Scene scene) {
        synchronized (REGISTERED_SCENES) {
            REGISTERED_SCENES.removeIf(ref -> ref.get() == null || ref.get() == scene);
            REGISTERED_SCENES.add(new WeakReference<>(scene));
        }
    }

    private static void reapplyToRegisteredScenes() {
        String base = overridesUrlBase();
        String freshUrl = overridesUrlVersioned();
        synchronized (REGISTERED_SCENES) {
            Iterator<WeakReference<Scene>> it = REGISTERED_SCENES.iterator();
            while (it.hasNext()) {
                Scene scene = it.next().get();
                if (scene == null) {
                    it.remove();
                    continue;
                }
                scene.getStylesheets().removeIf(s -> s.startsWith(base));
                scene.getStylesheets().add(freshUrl);
                scene.getRoot().applyCss();
                scene.getRoot().layout();
            }
        }
    }

    public static void resetAllToDefaults() {
        try {
            Path file = Paths.get(OVERRIDES_FILE);
            Files.createDirectories(file.getParent());
            Files.writeString(file, "/* تم إعادة الضبط للإعدادات الافتراضية */\n.root {\n}\n");
        } catch (IOException ignored) {
        }
        AppConfig.setValue("ui", "colorOverridesVersion", String.valueOf(System.currentTimeMillis()));
        reapplyToRegisteredScenes();
    }

    public static Parent buildPanel() {
        List<ColorVar> vars = loadColorVars();
        Map<ColorVar, ColorPicker> pickers = new LinkedHashMap<>();
        Set<ColorVar> dirty = new LinkedHashSet<>();
        List<VBox> cardNodes = new ArrayList<>();

        // ─── Header ───
        Label headerIcon = new Label("🎨");
        headerIcon.getStyleClass().add("stg-header-icon");

        Label headerTitle = new Label("ألوان الثيم");
        headerTitle.getStyleClass().add("stg-header-title");

        Label headerSubtitle = new Label("تخصيص ألوان الثيم النشط: "
                + AppConfig.getString("ui", "theme", "theme-blue.css"));
        headerSubtitle.getStyleClass().add("stg-header-subtitle");

        VBox titleBox = new VBox(2, headerTitle, headerSubtitle);

        TextField searchField = new TextField();
        searchField.setPromptText("🔍 ابحث عن لون...");
        searchField.setPrefWidth(200);
        searchField.getStyleClass().add("stg-search-field");

        HBox header = new HBox(12, headerIcon, titleBox, spacer(), searchField);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 20, 14, 20));
        header.getStyleClass().add("stg-page-header");

        // ─── Footer ───
        Label statusLabel = new Label("جاهز");
        statusLabel.getStyleClass().add("stg-status-msg");

        Label pendingBadge = new Label("");
        pendingBadge.setVisible(false);
        pendingBadge.getStyleClass().add("stg-pending-badge");

        Button discardBtn = new Button("↩ تجاهل");
        discardBtn.setVisible(false);
        discardBtn.getStyleClass().add("stg-btn-danger-outline");

        Button resetAllBtn = new Button("🔄 استعادة الافتراضي");
        resetAllBtn.getStyleClass().add("stg-btn-secondary");
        Tooltip.install(resetAllBtn, new Tooltip(
                "يمسح كل تخصيصات الألوان ويرجع للثيم الأصلي على الكل فوراً"));

        Button saveAllBtn = new Button("💾 حفظ وتطبيق الكل");
        saveAllBtn.setDisable(true);
        saveAllBtn.getStyleClass().add("stg-btn-primary");

        Runnable updateFooter = () -> {
            int n = dirty.size();
            saveAllBtn.setDisable(n == 0);
            saveAllBtn.setText(n > 0 ? "💾 حفظ وتطبيق الكل (" + n + ")" : "💾 حفظ وتطبيق الكل");
            discardBtn.setVisible(n > 0);
            pendingBadge.setVisible(n > 0);
            pendingBadge.setText(n + " تغيير غير محفوظ");
            if (n == 0) {
                statusLabel.setText("جاهز");
                statusLabel.getStyleClass().removeAll(
                        "stg-status-msg-ok", "stg-status-msg-error", "stg-status-msg-warn");
                statusLabel.getStyleClass().add("stg-status-msg");
            }
        };

        // ─── Body ───
        VBox cardsContainer = new VBox(8);
        cardsContainer.setPadding(new Insets(14));
        cardsContainer.getStyleClass().add("stg-main-content");

        for (ColorVar v : vars) {
            VBox card = buildColorRow(v, pickers, dirty, updateFooter);
            cardNodes.add(card);
            cardsContainer.getChildren().add(card);
        }

        ScrollPane scroll = new ScrollPane(cardsContainer);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("stg-main-scroll");

        searchField.textProperty().addListener((obs, o, n) -> {
            String q = n == null ? "" : n.trim().toLowerCase();
            for (int i = 0; i < vars.size(); i++) {
                ColorVar v = vars.get(i);
                boolean match = q.isEmpty()
                        || v.arabicLabel.toLowerCase().contains(q)
                        || v.key.toLowerCase().contains(q);
                cardNodes.get(i).setVisible(match);
                cardNodes.get(i).setManaged(match);
            }
        });

        discardBtn.setOnAction(e -> {
            for (ColorVar v : vars) {
                v.currentValue = v.themeDefault;
                pickers.get(v).setValue(safeWebColor(v.themeDefault));
            }
            dirty.clear();
            cardNodes.forEach(c -> c.getStyleClass().remove("stg-card-changed"));
            updateFooter.run();
            statusLabel.setText("تم تجاهل التغييرات");
            statusLabel.getStyleClass().removeAll("stg-status-msg-ok", "stg-status-msg-error", "stg-status-msg-warn");
            statusLabel.getStyleClass().add("stg-status-msg-warn");
        });

        resetAllBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("استعادة الافتراضي");
            confirm.setHeaderText("هتمسح كل تخصيصات الألوان اللي حفظتها");
            confirm.setContentText("هيرجع للثيم الأصلي فورًا على كل الشاشات المفتوحة.\nمتقدرش ترجع لتخصيصاتك القديمة بعد كده. كمّل؟");
            confirm.getDialogPane().getStyleClass().add("stg-dialog");
            SettingsThemeLoader.apply(confirm.getDialogPane());
            confirm.showAndWait().ifPresent(btn -> {
                if (btn == ButtonType.OK) {
                    resetAllToDefaults();
                    for (ColorVar v : vars) {
                        v.currentValue = v.themeDefault;
                        pickers.get(v).setValue(safeWebColor(v.themeDefault));
                    }
                    dirty.clear();
                    cardNodes.forEach(c -> c.getStyleClass().remove("stg-card-changed"));
                    updateFooter.run();
                    statusLabel.setText("✓ تم الرجوع للثيم الافتراضي على الكل");
                    statusLabel.getStyleClass().removeAll("stg-status-msg-ok", "stg-status-msg-error", "stg-status-msg-warn");
                    statusLabel.getStyleClass().add("stg-status-msg-ok");
                }
            });
        });

        saveAllBtn.setOnAction(e -> {
            for (ColorVar v : vars) {
                Color c = pickers.get(v).getValue();
                v.currentValue = toCssHex(c);
            }
            saveColorVars(vars);
            dirty.clear();
            cardNodes.forEach(c -> c.getStyleClass().remove("stg-card-changed"));
            updateFooter.run();
            reapplyToRegisteredScenes();
            statusLabel.setText("✓ تم الحفظ والتطبيق الفوري");
            statusLabel.getStyleClass().removeAll("stg-status-msg-ok", "stg-status-msg-error", "stg-status-msg-warn");
            statusLabel.getStyleClass().add("stg-status-msg-ok");
        });

        HBox footer = new HBox(10, statusLabel, spacer(), pendingBadge, discardBtn, resetAllBtn, saveAllBtn);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(10, 20, 10, 20));
        footer.getStyleClass().add("stg-footer");

        BorderPane root = new BorderPane();
        root.setTop(header);
        root.setCenter(scroll);
        root.setBottom(footer);
        root.getStyleClass().add("stg-root");

        SettingsThemeLoader.apply(searchField);
        return root;
    }

    private static VBox buildColorRow(ColorVar v, Map<ColorVar, ColorPicker> pickers,
                                      Set<ColorVar> dirty, Runnable updateFooter) {
        VBox card = new VBox(4);
        card.setPadding(new Insets(10, 12, 10, 12));
        card.getStyleClass().add("stg-card");

        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        VBox labelsBox = new VBox(1);
        Label name = new Label(v.arabicLabel);
        name.getStyleClass().add("stg-card-title");
        Label keyLbl = new Label(v.key);
        keyLbl.getStyleClass().add("stg-field-value-muted");
        labelsBox.getChildren().addAll(name, keyLbl);

        ColorPicker picker = new ColorPicker(safeWebColor(v.currentValue));
        picker.getStyleClass().add("stg-color-picker-light");
        pickers.put(v, picker);

        Button resetBtn = new Button("↺");
        resetBtn.setTooltip(new Tooltip("استعادة لون الثيم الافتراضي لهذا اللون فقط"));
        resetBtn.getStyleClass().add("stg-icon-btn");
        resetBtn.setOnAction(e -> {
            picker.setValue(safeWebColor(v.themeDefault));
            dirty.add(v);
            if (!card.getStyleClass().contains("stg-card-changed")) {
                card.getStyleClass().add("stg-card-changed");
            }
            updateFooter.run();
        });

        row.getChildren().addAll(labelsBox, spacer(), picker, resetBtn);
        card.getChildren().add(row);

        picker.valueProperty().addListener((obs, o, n) -> {
            dirty.add(v);
            if (!card.getStyleClass().contains("stg-card-changed")) {
                card.getStyleClass().add("stg-card-changed");
            }
            updateFooter.run();
        });

        return card;
    }

    private static Color safeWebColor(String cssValue) {
        try {
            return Color.web(cssValue);
        } catch (Exception e) {
            return Color.GRAY;
        }
    }

    private static String toCssHex(Color c) {
        int r = (int) Math.round(c.getRed() * 255);
        int g = (int) Math.round(c.getGreen() * 255);
        int b = (int) Math.round(c.getBlue() * 255);
        if (c.getOpacity() >= 1.0)
            return String.format("#%02X%02X%02X", r, g, b);
        int a = (int) Math.round(c.getOpacity() * 255);
        return String.format("#%02X%02X%02X%02X", r, g, b, a);
    }

    private static Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }
}