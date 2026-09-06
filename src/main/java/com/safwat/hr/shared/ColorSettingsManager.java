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

/**
 * كلاس عام لتخصيص ألوان الثيم النشط بدون تعديل ملف الثيم الأصلي.
 * <p>
 * الفكرة: بيقرأ ملف الثيم الحالي (اسمه محفوظ في AppConfig تحت ui.theme) من classpath،
 * يستخرج منه بس المتغيرات اللي قيمتها لون حقيقي (#hex أو rgba(...))، بيديها
 * اسم عربي مفهوم، ويسيب المستخدم يعدلها. عند الحفظ، بيكتب ملف overrides
 * منفصل ({@value #}) بيتحمّل بعد ملف الثيم في كل Scene، فقيمه
 * بتغلب قيم الثيم الأصلية — من غير ما نلمس ملفات الثيمات نفسها خالص.
 * <p>
 * ⚠️ ملف overrides بيتم حفظه في مجلد config داخل home المستخدم
 * (مثل ~/hr-mate/config/user-theme-overrides.css).
 * <p>
 * طريقة الاستخدام:
 * <ol>
 *   <li>وقت إنشاء أي Scene (بدل الاستدعاء العادي لإضافة ملف الثيم):
 *       {@code ColorSettingsManager.attachTheme(scene, AppConfig.getString("ui","theme","theme-blue.css")); }</li>
 *   <li>لفتح شاشة التعديل: {@code ColorSettingsManager.buildPanel()} (ينفع تركبها
 *       جوه أي حاوية، زي ما بيحصل في AppearanceSettingsController).</li>
 * </ol>
 */
public class ColorSettingsManager {

    /**
     * المجلد اللي فيه ملفات الثيمات (common.css, theme-blue.css...) داخل classpath.
     */
    private static final String THEMES_DIR = "/com/safwat/hr/css/";

    /**
     * الملف اللي بيتحفظ فيه تخصيصات المستخدم، وبيتحمّل بعد ملف الثيم مباشرة.
     * موجود في مجلد config داخل home المستخدم.
     */
    private static final String OVERRIDES_FILE = System.getProperty("user.dir")
            + "/app/config/user-theme-overrides.css";

    // ---- ألوان واجهة التخصيص نفسها (ثابتة، مش من الثيم) ----
    private static final String C_BG = "#1a1d2e";
    private static final String C_CARD = "#242740";
    private static final String C_ACCENT = "#4f8ef7";
    private static final String C_GREEN = "#43c59e";
    private static final String C_WARN = "#f5a623";
    private static final String C_TEXT = "#e8eaf6";
    private static final String C_MUTED = "#8b90b8";
    private static final String C_BORDER = "#333659";

    /**
     * قاموس الأسماء العربية لأشهر متغيرات الألوان. أي متغير مش موجود هنا بيتعرض باسمه التقني.
     */
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

    /**
     * بيلقط أي سطر شكله: -اسم-المتغير: قيمة-لون؛  (hex أو rgb/rgba)
     */
    private static final Pattern COLOR_VAR_PATTERN =
            Pattern.compile("(-[a-zA-Z][a-zA-Z0-9-]*)\\s*:\\s*(#[0-9A-Fa-f]{3,8}|rgba?\\([^)]*\\))\\s*;");

    /**
     * تسجيل كل الـ Scenes المفتوحة عشان نقدر نطبق عليها فورًا عند الحفظ.
     */
    private static final List<WeakReference<Scene>> REGISTERED_SCENES =
            Collections.synchronizedList(new ArrayList<>());

    private ColorSettingsManager() {
        // كلاس أدوات ثابت بالكامل - مفيش داعي لإنشاء نسخة منه
    }

    // ==================== نموذج بيانات المتغير اللوني ====================

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

    // ==================== قراءة الملفات من classpath ونظام الملفات ====================

    /**
     * تقرأ ملف من الـ classpath (داخل الـ JAR أو مجلد resources).
     */
    private static String readResourceQuietly(String resourcePath) {
        try (InputStream is = ColorSettingsManager.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                System.out.println("Resource not found: " + resourcePath);
                return "";
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * تقرأ ملف من نظام الملفات (خارج الـ JAR).
     */
    private static String readFileQuietly(Path filePath) {
        try {
            if (!Files.exists(filePath)) {
                return "";
            }
            return Files.readString(filePath);
        } catch (IOException e) {
            return "";
        }
    }

    /**
     * تضمن وجود مجلد config وملف overrides فارغ إن لزم الأمر.
     */
    private static void ensureOverridesFileExists() {
        Path file = Paths.get(OVERRIDES_FILE);
        if (Files.exists(file)) {
            return;
        }
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, "/* ملف تخصيص الألوان — هيتملى تلقائياً أول ما تحفظ من شاشة الألوان */\n.root {\n}\n");
        } catch (IOException ignored) {
        }
    }

    // ==================== استخراج المتغيرات من محتوى CSS ====================

    private static Map<String, String> parseColorVars(String css) {
        Map<String, String> result = new LinkedHashMap<>();
        if (css == null || css.isEmpty()) {
            return result;
        }
        Matcher m = COLOR_VAR_PATTERN.matcher(css);
        while (m.find()) {
            String key = m.group(1);
            if (key.startsWith("-fx-")) {
                continue; // دي تعريفات محرك Modena، مش متغيرات تخصيص
            }
            result.putIfAbsent(key, m.group(2));
        }
        return result;
    }

    // ==================== تحميل المتغيرات الحالية ====================

    private static List<ColorVar> loadColorVars() {
        String themeName = AppConfig.getString("ui", "theme", "theme-blue.css");
        String themePath = THEMES_DIR + themeName;
        String themeContent = readResourceQuietly(themePath);
        Map<String, String> themeDefaults = parseColorVars(themeContent);

        // قراءة التخصيصات المحفوظة من ملف overrides
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
        } catch (IOException ignored) {
            // تجاهل بصمت
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

    /**
     * بتضيف ملف الثيم + ملف تخصيص الألوان لأي Scene، وتسجّلها عشان تتحدث
     * فورًا لو المستخدم عدّل الألوان وهي لسه مفتوحة. استخدمها بدل الإضافة
     * العادية لملف الثيم وقت إنشاء أي Scene جديدة في التطبيق.
     */
    public static void attachTheme(Scene scene, String themeFileName) {
        if (scene == null) {
            return;
        }
        ensureOverridesFileExists();

        // إضافة ملف الثيم من الـ classpath
        String themeResource = THEMES_DIR + themeFileName;
        try {
            String themeUrl = Objects.requireNonNull(ColorSettingsManager.class.getResource(themeResource)).toExternalForm();
            System.out.println(themeUrl);
            scene.getStylesheets().add(themeUrl);
        } catch (Exception e) {
            System.err.println("تعذر تحميل ملف الثيم: " + themeResource);
            e.printStackTrace();
        }

        // إضافة ملف التخصيص من نظام الملفات
        scene.getStylesheets().add(overridesUrlVersioned());
        System.out.println(overridesUrlVersioned());
        registerScene(scene);
    }

    private static void registerScene(Scene scene) {
        synchronized (REGISTERED_SCENES) {
            REGISTERED_SCENES.removeIf(ref -> ref.get() == null || ref.get() == scene);
            REGISTERED_SCENES.add(new WeakReference<>(scene));
        }
    }

    /**
     * بتعيد تطبيق ملف الألوان (بعد تعديله) على كل الـ Scenes المسجّلة والمفتوحة حاليًا.
     */
    private static void reapplyToRegisteredScenes() {
        String base = overridesUrlBase();
        String freshUrl = overridesUrlVersioned();
        System.out.println("[ColorSettingsManager] إعادة تطبيق التخصيصات...");
        System.out.println("  base = " + base);
        System.out.println("  fresh = " + freshUrl);
        synchronized (REGISTERED_SCENES) {
            Iterator<WeakReference<Scene>> it = REGISTERED_SCENES.iterator();
            while (it.hasNext()) {
                Scene scene = it.next().get();
                if (scene == null) {
                    it.remove();
                    continue;
                }
                boolean removed = scene.getStylesheets().removeIf(s -> s.startsWith(base));
                System.out.println("  Scene: removed " + (removed ? "yes" : "no") + ", added fresh");

                scene.getStylesheets().add(freshUrl);
// فرض إعادة تحميل الأنماط
                scene.getRoot().setStyle("-fx-something: 1;");
                scene.getRoot().applyCss();
                scene.getRoot().layout();
            }
        }
    }

    // ==================== واجهة التخصيص ====================

    /**
     * بيبني واجهة تخصيص الألوان (كارت لكل متغير لون) جاهزة للتركيب جوه أي
     * حاوية (زي منطقة المحتوى في AppearanceSettingsController).
     */
    public static Parent buildPanel() {
        List<ColorVar> vars = loadColorVars();
        Map<ColorVar, ColorPicker> pickers = new LinkedHashMap<>();
        Set<ColorVar> dirty = new LinkedHashSet<>();
        List<VBox> cardNodes = new ArrayList<>();

        // ---------- Header ----------
        Label headerIcon = new Label("🎨");
        headerIcon.setStyle("-fx-font-size:20px;");
        Label headerTitle = new Label("ألوان الثيم");
        headerTitle.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:" + C_TEXT + ";");
        Label headerSubtitle = new Label("تخصيص ألوان الثيم النشط حاليًا: "
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

        // ---------- Body: كارت لكل متغير لون ----------
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
                v.currentValue = v.themeDefault; // نرجع للقيمة الافتراضية
                ColorPicker picker = pickers.get(v);
                picker.setValue(safeWebColor(v.themeDefault));
            }
            dirty.clear();
            cardNodes.forEach(c -> c.setStyle(cardStyle(false)));
            updateFooter.run();
            statusLabel.setText("تم تجاهل التغييرات");
            statusLabel.setStyle("-fx-text-fill:" + C_WARN + "; -fx-font-size:12px;");
        });

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

        HBox footer = new HBox(10, statusLabel, spacer(), pendingBadge, discardBtn, saveAllBtn);
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
        resetBtn.setTooltip(new Tooltip("استعادة لون الثيم الافتراضي"));
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
        if (c.getOpacity() >= 1.0) {
            return String.format("#%02X%02X%02X", r, g, b);
        }
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
                + "-fx-border-color:" + (changed ? C_ACCENT : C_BORDER) + "; -fx-border-radius:8; -fx-border-width:1;";
    }

    private static String inputStyle() {
        return "-fx-background-color:" + C_BG + "; -fx-text-fill:" + C_TEXT + ";"
                + "-fx-prompt-text-fill:" + C_MUTED + "; -fx-border-color:" + C_BORDER + ";"
                + "-fx-border-radius:6; -fx-background-radius:6;";
    }
}