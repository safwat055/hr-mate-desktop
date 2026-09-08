package com.safwat.hr.shared;

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

    private static final String C_BG     = "#1a1d2e";
    private static final String C_CARD   = "#242740";
    private static final String C_ACCENT = "#4f8ef7";
    private static final String C_GREEN  = "#43c59e";
    private static final String C_WARN   = "#f5a623";
    private static final String C_TEXT   = "#e8eaf6";
    private static final String C_MUTED  = "#8b90b8";
    private static final String C_BORDER = "#333659";

    private static final Map<String, String> ARABIC_LABELS = new LinkedHashMap<>();
    static {
        ARABIC_LABELS.put("-app-bg",               "خلفية التطبيق");
        ARABIC_LABELS.put("-card-bg",               "خلفية الكروت");
        ARABIC_LABELS.put("-card-border",           "حدود الكروت");
        ARABIC_LABELS.put("-card-shadow",           "ظل الكروت");
        ARABIC_LABELS.put("-text-primary",          "لون النص الأساسي");
        ARABIC_LABELS.put("-text-secondary",        "لون النص الثانوي");
        ARABIC_LABELS.put("-text-muted",            "لون النص الباهت");
        ARABIC_LABELS.put("-on-brand",              "لون النص فوق لون العلامة");
        ARABIC_LABELS.put("-brand-navy",            "اللون الكحلي (العلامة)");
        ARABIC_LABELS.put("-header-alt-bg",         "خلفية الهيدر البديلة");
        ARABIC_LABELS.put("-color-primary",         "اللون الأساسي (Primary)");
        ARABIC_LABELS.put("-color-danger",          "لون الخطر / الحذف");
        ARABIC_LABELS.put("-color-success",         "لون النجاح");
        ARABIC_LABELS.put("-color-purple",          "اللون البنفسجي");
        ARABIC_LABELS.put("-color-secondary-btn",   "لون الأزرار الثانوية");
        ARABIC_LABELS.put("-color-warning",         "لون التحذير");
        ARABIC_LABELS.put("-border-color",          "لون الحدود");
        ARABIC_LABELS.put("-border-gray",           "لون الحدود الرمادي");
        ARABIC_LABELS.put("-divider-color",         "لون الفواصل");
        ARABIC_LABELS.put("-soft-box-bg",           "خلفية الصناديق الناعمة");
        ARABIC_LABELS.put("-soft-box-border",       "حدود الصناديق الناعمة");
        ARABIC_LABELS.put("-control-bg",            "خلفية عناصر التحكم");
        ARABIC_LABELS.put("-table-header-bg",       "خلفية رأس الجدول");
        ARABIC_LABELS.put("-row-alt-bg",            "خلفية الصف البديل");
        ARABIC_LABELS.put("-selection-bg",          "لون التحديد");
    }

    private static final Pattern COLOR_VAR_PATTERN =
            Pattern.compile("(-[a-zA-Z][a-zA-Z0-9-]*)\\s*:\\s*(#[0-9A-Fa-f]{3,8}|rgba?\\([^)]*\\))\\s*;");

    private static final List<WeakReference<Scene>> REGISTERED_SCENES =
            Collections.synchronizedList(new ArrayList<>());

    private ColorSettingsManager() {}

    // ==================== نموذج بيانات المتغير اللوني ====================

    private static class ColorVar {
        final String key;
        final String arabicLabel;
        final String themeDefault;
        String currentValue;

        ColorVar(String key, String arabicLabel, String themeDefault, String currentValue) {
            this.key          = key;
            this.arabicLabel  = arabicLabel;
            this.themeDefault = themeDefault;
            this.currentValue = currentValue;
        }
    }

    // ==================== قراءة الملفات ====================

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
        } catch (IOException ignored) {}
    }

    // ==================== استخراج المتغيرات ====================

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
        String themeName    = AppConfig.getString("ui", "theme", "theme-blue.css");
        String themeContent = readResourceQuietly(THEMES_DIR + themeName);
        Map<String, String> themeDefaults  = parseColorVars(themeContent);
        Map<String, String> savedOverrides = parseColorVars(readFileQuietly(Paths.get(OVERRIDES_FILE)));

        List<ColorVar> result = new ArrayList<>();
        for (Map.Entry<String, String> e : themeDefaults.entrySet()) {
            String key          = e.getKey();
            String defaultValue = e.getValue();
            String current      = savedOverrides.getOrDefault(key, defaultValue);
            String label        = ARABIC_LABELS.getOrDefault(key, key);
            result.add(new ColorVar(key, label, defaultValue, current));
        }
        return result;
    }

    // ==================== الحفظ وإعادة التطبيق ====================

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
        } catch (IOException ignored) {}

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
        String base     = overridesUrlBase();
        String freshUrl = overridesUrlVersioned();
        synchronized (REGISTERED_SCENES) {
            Iterator<WeakReference<Scene>> it = REGISTERED_SCENES.iterator();
            while (it.hasNext()) {
                Scene scene = it.next().get();
                if (scene == null) { it.remove(); continue; }
                scene.getStylesheets().removeIf(s -> s.startsWith(base));
                scene.getStylesheets().add(freshUrl);
                scene.getRoot().applyCss();
                scene.getRoot().layout();
            }
        }
    }

    /**
     * ✅ استعادة كل ألوان الثيم للقيم الافتراضية — يمسح ملف overrides ويطبق فوراً على الكل.
     */
    public static void resetAllToDefaults() {
        try {
            Path file = Paths.get(OVERRIDES_FILE);
            Files.createDirectories(file.getParent());
            // نكتب ملف فارغ (مش نحذف) عشان الـ stylesheet URL يفضل صالح
            Files.writeString(file, "/* تم إعادة الضبط للإعدادات الافتراضية */\n.root {\n}\n");
        } catch (IOException ignored) {}
        AppConfig.setValue("ui", "colorOverridesVersion", String.valueOf(System.currentTimeMillis()));
        reapplyToRegisteredScenes();
    }

    // ==================== واجهة التخصيص ====================

    /**
     * بيبني panel الألوان جاهز للتركيب جوه AppearanceSettingsController.
     * ✅ يضم زر "🔄 استعادة الافتراضي" بيرجع الكل للثيم الأصلي فوراً.
     */
    public static Parent buildPanel() {
        List<ColorVar> vars     = loadColorVars();
        Map<ColorVar, ColorPicker> pickers = new LinkedHashMap<>();
        Set<ColorVar> dirty     = new LinkedHashSet<>();
        List<VBox> cardNodes    = new ArrayList<>();

        // ---------- Header ----------
        Label headerIcon = new Label("🎨");
        headerIcon.setStyle("-fx-font-size:20px;");

        Label headerTitle = new Label("ألوان الثيم");
        headerTitle.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:" + C_TEXT + ";");

        Label headerSubtitle = new Label("تخصيص ألوان الثيم النشط: "
                + AppConfig.getString("ui", "theme", "theme-blue.css"));
        headerSubtitle.setStyle("-fx-font-size:11px; -fx-text-fill:" + C_MUTED + ";");

        VBox titleBox = new VBox(2, headerTitle, headerSubtitle);

        TextField searchField = new TextField();
        searchField.setPromptText("🔍 ابحث عن لون...");
        searchField.setPrefWidth(200);
        searchField.setStyle(inputStyle());

        HBox header = new HBox(12, headerIcon, titleBox, spacer(), searchField);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 20, 14, 20));
        header.setStyle("-fx-background-color:" + C_CARD + "; -fx-border-color:" + C_BORDER
                + "; -fx-border-width:0 0 1 0;");

        // ---------- Footer ----------
        Label statusLabel = new Label("جاهز");
        statusLabel.setStyle("-fx-text-fill:" + C_MUTED + "; -fx-font-size:12px;");

        Label pendingBadge = new Label("");
        pendingBadge.setVisible(false);
        pendingBadge.setStyle("-fx-text-fill:" + C_WARN + "; -fx-font-size:11px; -fx-font-weight:bold;");

        Button discardBtn = new Button("↩ تجاهل");
        discardBtn.setVisible(false);
        discardBtn.setStyle("-fx-background-color:transparent; -fx-text-fill:" + C_WARN
                + "; -fx-border-color:" + C_WARN + "; -fx-border-radius:6; -fx-padding:6 14 6 14;");

        // ✅ زر استعادة الافتراضي العام
        Button resetAllBtn = new Button("🔄 استعادة الافتراضي");
        resetAllBtn.setStyle("-fx-background-color:transparent; -fx-text-fill:" + C_MUTED
                + "; -fx-border-color:" + C_BORDER + "; -fx-border-radius:6; -fx-padding:6 14 6 14; -fx-cursor:hand;");
        Tooltip.install(resetAllBtn, new Tooltip(
                "يمسح كل تخصيصات الألوان ويرجع للثيم الأصلي على الكل فوراً"));

        Button saveAllBtn = new Button("💾 حفظ وتطبيق الكل");
        saveAllBtn.setDisable(true);
        saveAllBtn.setStyle("-fx-background-color:" + C_ACCENT + "; -fx-text-fill:white;"
                + "-fx-font-weight:bold; -fx-background-radius:6; -fx-padding:6 16 6 16;");

        Runnable updateFooter = () -> {
            int n = dirty.size();
            saveAllBtn.setDisable(n == 0);
            saveAllBtn.setText(n > 0 ? "💾 حفظ وتطبيق الكل (" + n + ")" : "💾 حفظ وتطبيق الكل");
            discardBtn.setVisible(n > 0);
            pendingBadge.setVisible(n > 0);
            pendingBadge.setText(n + " تغيير غير محفوظ");
            if (n == 0) {
                statusLabel.setText("جاهز");
                statusLabel.setStyle("-fx-text-fill:" + C_MUTED + "; -fx-font-size:12px;");
            }
        };

        // ---------- Body ----------
        VBox cardsContainer = new VBox(8);
        cardsContainer.setPadding(new Insets(14));
        cardsContainer.setStyle("-fx-background-color:" + C_BG + ";");

        for (ColorVar v : vars) {
            VBox card = buildColorRow(v, pickers, dirty, updateFooter);
            cardNodes.add(card);
            cardsContainer.getChildren().add(card);
        }

        ScrollPane scroll = new ScrollPane(cardsContainer);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background:" + C_BG + "; -fx-background-color:" + C_BG + ";");

        // ---------- بحث ----------
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

        // ---------- تجاهل التغييرات ----------
        discardBtn.setOnAction(e -> {
            for (ColorVar v : vars) {
                v.currentValue = v.themeDefault;
                pickers.get(v).setValue(safeWebColor(v.themeDefault));
            }
            dirty.clear();
            cardNodes.forEach(c -> c.setStyle(cardStyle(false)));
            updateFooter.run();
            statusLabel.setText("تم تجاهل التغييرات");
            statusLabel.setStyle("-fx-text-fill:" + C_WARN + "; -fx-font-size:12px;");
        });

        // ✅ استعادة الكل للافتراضي مع تأكيد
        resetAllBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("استعادة الافتراضي");
            confirm.setHeaderText("هتمسح كل تخصيصات الألوان اللي حفظتها");
            confirm.setContentText("هيرجع للثيم الأصلي فورًا على كل الشاشات المفتوحة.\nمتقدرش ترجع لتخصيصاتك القديمة بعد كده. كمّل؟");
            confirm.showAndWait().ifPresent(btn -> {
                if (btn == ButtonType.OK) {
                    resetAllToDefaults();
                    // تحديث الـ pickers بقيم الثيم الأصلية
                    for (ColorVar v : vars) {
                        v.currentValue = v.themeDefault;
                        pickers.get(v).setValue(safeWebColor(v.themeDefault));
                    }
                    dirty.clear();
                    cardNodes.forEach(c -> c.setStyle(cardStyle(false)));
                    updateFooter.run();
                    statusLabel.setText("✓ تم الرجوع للثيم الافتراضي على الكل");
                    statusLabel.setStyle("-fx-text-fill:" + C_GREEN + "; -fx-font-size:12px;");
                }
            });
        });

        // ---------- حفظ وتطبيق ----------
        saveAllBtn.setOnAction(e -> {
            for (ColorVar v : vars) {
                Color c = pickers.get(v).getValue();
                v.currentValue = toCssHex(c);
            }
            saveColorVars(vars);
            dirty.clear();
            cardNodes.forEach(c -> c.setStyle(cardStyle(false)));
            updateFooter.run();
            reapplyToRegisteredScenes();
            statusLabel.setText("✓ تم الحفظ والتطبيق الفوري");
            statusLabel.setStyle("-fx-text-fill:" + C_GREEN + "; -fx-font-size:12px;");
        });

        HBox footer = new HBox(10, statusLabel, spacer(), pendingBadge, discardBtn, resetAllBtn, saveAllBtn);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(10, 20, 10, 20));
        footer.setStyle("-fx-background-color:" + C_CARD + "; -fx-border-color:" + C_BORDER
                + "; -fx-border-width:1 0 0 0;");

        BorderPane root = new BorderPane();
        root.setTop(header);
        root.setCenter(scroll);
        root.setBottom(footer);
        root.setStyle("-fx-background-color:" + C_BG + ";");
        return root;
    }

    // ==================== بناء كارت لون واحد ====================

    private static VBox buildColorRow(ColorVar v, Map<ColorVar, ColorPicker> pickers,
                                      Set<ColorVar> dirty, Runnable updateFooter) {
        VBox card = new VBox(4);
        card.setPadding(new Insets(10, 12, 10, 12));
        card.setStyle(cardStyle(false));

        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        VBox labelsBox = new VBox(1);
        Label name = new Label(v.arabicLabel);
        name.setStyle("-fx-font-size:12.5px; -fx-font-weight:bold; -fx-text-fill:" + C_TEXT + ";");
        Label keyLbl = new Label(v.key);
        keyLbl.setStyle("-fx-font-size:10px; -fx-font-family:monospace; -fx-text-fill:" + C_MUTED + ";");
        labelsBox.getChildren().addAll(name, keyLbl);

        ColorPicker picker = new ColorPicker(safeWebColor(v.currentValue));
        picker.setStyle(inputStyle());
        pickers.put(v, picker);

        Button resetBtn = new Button("↺");
        resetBtn.setTooltip(new Tooltip("استعادة لون الثيم الافتراضي لهذا اللون فقط"));
        resetBtn.setStyle("-fx-background-color:transparent; -fx-text-fill:" + C_MUTED
                + "; -fx-cursor:hand; -fx-font-size:13px;");
        resetBtn.setOnAction(e -> {
            picker.setValue(safeWebColor(v.themeDefault));
            dirty.add(v);
            card.setStyle(cardStyle(true));
            updateFooter.run();
        });

        row.getChildren().addAll(labelsBox, spacer(), picker, resetBtn);
        card.getChildren().add(row);

        picker.valueProperty().addListener((obs, o, n) -> {
            dirty.add(v);
            card.setStyle(cardStyle(true));
            updateFooter.run();
        });

        return card;
    }

    // ==================== helpers ====================

    private static Color safeWebColor(String cssValue) {
        try { return Color.web(cssValue); }
        catch (Exception e) { return Color.GRAY; }
    }

    private static String toCssHex(Color c) {
        int r = (int) Math.round(c.getRed()   * 255);
        int g = (int) Math.round(c.getGreen() * 255);
        int b = (int) Math.round(c.getBlue()  * 255);
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

    private static String cardStyle(boolean changed) {
        return "-fx-background-color:" + C_CARD + "; -fx-background-radius:8;"
                + "-fx-border-color:" + (changed ? C_ACCENT : C_BORDER)
                + "; -fx-border-radius:8; -fx-border-width:1;";
    }

    private static String inputStyle() {
        return "-fx-background-color:" + C_BG + "; -fx-text-fill:" + C_TEXT + ";"
                + "-fx-prompt-text-fill:" + C_MUTED + "; -fx-border-color:" + C_BORDER + ";"
                + "-fx-border-radius:6; -fx-background-radius:6;";
    }
}