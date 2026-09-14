package com.safwat.hr.controller.appearance;

import com.safwat.hr.shared.AppConfig;
import com.safwat.hr.shared.ViewRegistry;
import com.safwat.hr.ui.theme.SettingsThemeLoader;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FontSettingsManager {

    private static final String CONFIG_SECTION_PREFIX = "fonts_";
    private static final String CSS_PATH = "/com/safwat/hr/css/settings.css";

    private static final Map<String, List<WeakReference<Parent>>> REGISTERED_ROOTS = new ConcurrentHashMap<>();

    public enum ComponentType {
        LABEL("Label / نص عادي", "🏷", Label.class),
        BUTTON("Button / زر", "🔘", Button.class),
        TEXT_FIELD("TextField / حقل إدخال", "✏️", TextField.class),
        TEXT_AREA("TextArea / حقل نص متعدد الأسطر", "📝", TextArea.class),
        TABLE_VIEW("TableView / جدول", "📊", TableView.class),
        LIST_VIEW("ListView / ليست", "📋", ListView.class),
        COMBO_BOX("ComboBox / قائمة منسدلة", "🔽", ComboBox.class),
        CHECK_BOX("CheckBox", "☑", CheckBox.class),
        RADIO_BUTTON("RadioButton", "🔘", RadioButton.class),
        TAB_PANE("TabPane / تبويبات", "🗂", TabPane.class),
        MENU_BAR("MenuBar / قوائم", "📜", MenuBar.class);

        private final String displayName;
        private final String icon;
        private final Class<? extends Node> targetClass;

        ComponentType(String displayName, String icon, Class<? extends Node> targetClass) {
            this.displayName = displayName;
            this.icon = icon;
            this.targetClass = targetClass;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getIcon() {
            return icon;
        }

        public Class<? extends Node> getTargetClass() {
            return targetClass;
        }
    }

    private final String viewId;

    public FontSettingsManager(String viewId) {
        if (viewId == null || viewId.trim().isEmpty())
            throw new IllegalArgumentException("viewId لازم يكون له قيمة");
        this.viewId = viewId;
    }

    public void openSettingsWindow() {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("إعدادات الخطوط - " + viewId);

        Scene scene = new Scene(buildPanel(), 620, 640);

        try {
            String css = Objects.requireNonNull(
                    FontSettingsManager.class.getResource(CSS_PATH),
                    "ملف CSS مش موجود: " + CSS_PATH
            ).toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception e) {
            System.err.println("⚠ تعذر تحميل " + CSS_PATH + ": " + e.getMessage());
        }

        stage.setScene(scene);
        stage.showAndWait();
    }

    private static class FontCard {
        final ComponentType type;
        final VBox node;
        final ComboBox<String> familyCombo;
        final Spinner<Integer> sizeSpinner;
        final CheckBox boldCheck;
        final CheckBox italicCheck;
        final Label preview;

        FontCard(ComponentType type, VBox node, ComboBox<String> familyCombo,
                 Spinner<Integer> sizeSpinner, CheckBox boldCheck, CheckBox italicCheck, Label preview) {
            this.type = type;
            this.node = node;
            this.familyCombo = familyCombo;
            this.sizeSpinner = sizeSpinner;
            this.boldCheck = boldCheck;
            this.italicCheck = italicCheck;
            this.preview = preview;
        }
    }

    public Parent buildPanel() {
        List<FontCard> cards = new ArrayList<>();
        Set<ComponentType> dirty = new LinkedHashSet<>();

        // ─── Header ───
        Label headerIcon = new Label("🔤");
        headerIcon.getStyleClass().add("stg-header-icon");

        Label headerTitle = new Label("إعدادات الخطوط");
        headerTitle.getStyleClass().add("stg-header-title");

        Label headerSubtitle = new Label("تخصيص خطوط واجهة: " + viewId);
        headerSubtitle.getStyleClass().add("stg-header-subtitle");

        VBox titleBox = new VBox(2, headerTitle, headerSubtitle);

        TextField searchField = new TextField();
        searchField.setPromptText("🔍 ابحث عن كومبوننت...");
        searchField.setPrefWidth(220);
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
        VBox cardsContainer = new VBox(10);
        cardsContainer.setPadding(new Insets(14));
        cardsContainer.getStyleClass().add("stg-main-content");

        for (ComponentType type : ComponentType.values()) {
            JSONObject saved = loadComponentSettings(type);
            FontCard card = buildCard(type, saved, dirty, updateFooter);
            cards.add(card);
            cardsContainer.getChildren().add(card.node);
        }

        ScrollPane scroll = new ScrollPane(cardsContainer);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("stg-main-scroll");

        searchField.textProperty().addListener((obs, o, n) -> {
            String q = n == null ? "" : n.trim().toLowerCase();
            for (FontCard c : cards) {
                boolean match = q.isEmpty() || c.type.getDisplayName().toLowerCase().contains(q);
                c.node.setVisible(match);
                c.node.setManaged(match);
            }
        });

        discardBtn.setOnAction(e -> {
            for (FontCard c : cards) {
                applySavedToCard(c, loadComponentSettings(c.type));
                c.node.getStyleClass().remove("stg-card-changed");
            }
            dirty.clear();
            updateFooter.run();
            statusLabel.setText("تم تجاهل التغييرات");
            statusLabel.getStyleClass().removeAll("stg-status-msg-ok", "stg-status-msg-error", "stg-status-msg-warn");
            statusLabel.getStyleClass().add("stg-status-msg-warn");
        });

        resetAllBtn.setOnAction(e -> showConfirm(
                resetAllBtn,
                "تأكيد استعادة الافتراضي",
                "هتمسح كل إعدادات الخطوط لواجهة \"" + viewId + "\" ويرجع للثيم الافتراضي فوراً.",
                () -> {
                    resetToDefaults(viewId);
                    for (FontCard c : cards) {
                        applySavedToCard(c, new JSONObject());
                        c.node.getStyleClass().remove("stg-card-changed");
                    }
                    dirty.clear();
                    updateFooter.run();
                    statusLabel.setText("✓ تم الرجوع للخطوط الافتراضية");
                    statusLabel.getStyleClass().removeAll("stg-status-msg-ok", "stg-status-msg-error", "stg-status-msg-warn");
                    statusLabel.getStyleClass().add("stg-status-msg-ok");
                }
        ));

        saveAllBtn.setOnAction(e -> {
            for (FontCard c : cards) {
                saveComponentSettings(c.type, c.familyCombo.getValue(), c.sizeSpinner.getValue(),
                        c.boldCheck.isSelected(), c.italicCheck.isSelected());
                c.node.getStyleClass().remove("stg-card-changed");
            }
            dirty.clear();
            updateFooter.run();
            reapply(viewId);
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

    private static void showConfirm(Node anchor, String title, String message, Runnable onConfirm) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setTitle(title);
        popup.setResizable(false);

        Label msg = new Label(message);
        msg.setWrapText(true);
        msg.setMaxWidth(340);
        msg.getStyleClass().add("stg-dialog-title");

        Button cancelBtn = new Button("إلغاء");
        cancelBtn.getStyleClass().add("stg-btn-dialog-secondary");
        cancelBtn.setOnAction(ev -> popup.close());

        Button confirmBtn = new Button("تأكيد");
        confirmBtn.getStyleClass().add("stg-btn-dialog-primary");
        confirmBtn.setOnAction(ev -> {
            popup.close();
            onConfirm.run();
        });

        HBox buttons = new HBox(10, cancelBtn, confirmBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(16, msg, buttons);
        root.setPadding(new Insets(24));
        root.getStyleClass().add("stg-dialog-content");
        root.setMinWidth(380);

        if (anchor != null && anchor.getScene() != null
                && anchor.getScene().getWindow() instanceof Stage owner) {
            popup.initOwner(owner);
        }

        Scene scene = new Scene(root);
        SettingsThemeLoader.apply(root);
        popup.setScene(scene);
        popup.show();
    }

    private FontCard buildCard(ComponentType type, JSONObject saved,
                               Set<ComponentType> dirty, Runnable updateFooter) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(12));
        card.getStyleClass().add("stg-card");

        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label icon = new Label(type.getIcon());
        icon.getStyleClass().add("stg-card-icon");
        Label name = new Label(type.getDisplayName());
        name.getStyleClass().add("stg-card-title");
        Label preview = new Label("نموذج معاينة Aa 123");
        preview.getStyleClass().add("stg-field-hint");
        titleRow.getChildren().addAll(icon, name, spacer(), preview);
        card.getChildren().add(titleRow);

        HBox controlsRow = new HBox(10);
        controlsRow.setAlignment(Pos.CENTER_LEFT);

        Label familyLbl = new Label("الخط:");
        familyLbl.getStyleClass().add("stg-field-label");

        ComboBox<String> familyCombo = new ComboBox<>(
                FXCollections.observableArrayList(Font.getFamilies()));
        familyCombo.getStyleClass().add("stg-combo-light");
        familyCombo.setPrefWidth(190);

        Label sizeLbl = new Label("الحجم:");
        sizeLbl.getStyleClass().add("stg-field-label");

        Spinner<Integer> sizeSpinner = new Spinner<>(6, 72, 14);
        sizeSpinner.setEditable(true);
        sizeSpinner.setPrefWidth(80);
        sizeSpinner.getStyleClass().add("stg-spinner-light");

        CheckBox boldCheck = new CheckBox("Bold");
        boldCheck.getStyleClass().add("stg-checkbox-dark");
        CheckBox italicCheck = new CheckBox("Italic");
        italicCheck.getStyleClass().add("stg-checkbox-dark");

        controlsRow.getChildren().addAll(familyLbl, familyCombo, sizeLbl, sizeSpinner, boldCheck, italicCheck);
        card.getChildren().add(controlsRow);

        FontCard fc = new FontCard(type, card, familyCombo, sizeSpinner, boldCheck, italicCheck, preview);
        applySavedToCard(fc, saved);

        Runnable markDirty = () -> {
            dirty.add(type);
            if (!card.getStyleClass().contains("stg-card-changed")) {
                card.getStyleClass().add("stg-card-changed");
            }
            updateFooter.run();
        };

        Runnable refreshPreview = () -> {
            String family = familyCombo.getValue() != null ? familyCombo.getValue() : Font.getDefault().getFamily();
            int size = sizeSpinner.getValue();
            FontWeight weight = boldCheck.isSelected() ? FontWeight.BOLD : FontWeight.NORMAL;
            FontPosture posture = italicCheck.isSelected() ? FontPosture.ITALIC : FontPosture.REGULAR;
            preview.setFont(Font.font(family, weight, posture, size));
        };

        familyCombo.setOnAction(e -> {
            markDirty.run();
            refreshPreview.run();
        });
        sizeSpinner.valueProperty().addListener((obs, o, n) -> {
            markDirty.run();
            refreshPreview.run();
        });
        boldCheck.setOnAction(e -> {
            markDirty.run();
            refreshPreview.run();
        });
        italicCheck.setOnAction(e -> {
            markDirty.run();
            refreshPreview.run();
        });

        refreshPreview.run();
        return fc;
    }

    private void applySavedToCard(FontCard c, JSONObject saved) {
        c.familyCombo.setValue(saved.optString("family", Font.getDefault().getFamily()));
        c.sizeSpinner.getValueFactory().setValue(saved.optInt("size", (int) Font.getDefault().getSize()));
        c.boldCheck.setSelected(saved.optBoolean("bold", false));
        c.italicCheck.setSelected(saved.optBoolean("italic", false));
    }

    private JSONObject loadComponentSettings(ComponentType type) {
        JSONObject section = AppConfig.getSection(sectionKey());
        return section.has(type.name()) ? section.getJSONObject(type.name()) : new JSONObject();
    }

    private void saveComponentSettings(ComponentType type, String family, int size, boolean bold, boolean italic) {
        JSONObject value = new JSONObject();
        value.put("family", family);
        value.put("size", size);
        value.put("bold", bold);
        value.put("italic", italic);
        AppConfig.setValue(sectionKey(), type.name(), value);
    }

    private String sectionKey() {
        return CONFIG_SECTION_PREFIX + viewId;
    }

    public static void applySettings(String viewId, Parent root) {
        if (root == null || viewId == null) return;
        ViewRegistry.register(viewId);
        registerRoot(viewId, root);
        JSONObject section = AppConfig.getSection(CONFIG_SECTION_PREFIX + viewId);
        if (section.length() > 0) applyRecursive(root, section);
    }

    public static void resetToDefaults(String viewId) {
        AppConfig.removeValue(CONFIG_SECTION_PREFIX + viewId, null);
        List<WeakReference<Parent>> list = REGISTERED_ROOTS.get(viewId);
        if (list == null) return;

        javafx.application.Platform.runLater(() -> {
            synchronized (list) {
                Iterator<WeakReference<Parent>> it = list.iterator();
                while (it.hasNext()) {
                    Parent root = it.next().get();
                    if (root == null) {
                        it.remove();
                        continue;
                    }
                    clearFontRecursive(root);
                    root.applyCss();
                    root.layout();
                }
            }
        });
    }

    public static void reapply(String viewId) {
        List<WeakReference<Parent>> list = REGISTERED_ROOTS.get(viewId);
        if (list == null) return;
        JSONObject section = AppConfig.getSection(CONFIG_SECTION_PREFIX + viewId);
        synchronized (list) {
            Iterator<WeakReference<Parent>> it = list.iterator();
            while (it.hasNext()) {
                Parent root = it.next().get();
                if (root == null) {
                    it.remove();
                    continue;
                }
                if (section.length() > 0) applyRecursive(root, section);
            }
        }
    }

    private static void registerRoot(String viewId, Parent root) {
        List<WeakReference<Parent>> list =
                REGISTERED_ROOTS.computeIfAbsent(viewId, k -> Collections.synchronizedList(new ArrayList<>()));
        synchronized (list) {
            list.removeIf(ref -> ref.get() == null || ref.get() == root);
            list.add(new WeakReference<>(root));
        }
    }

    private static void applyRecursive(Node node, JSONObject section) {
        for (ComponentType type : ComponentType.values()) {
            if (type.getTargetClass().isInstance(node) && section.has(type.name())) {
                applyFontToNode(node, section.getJSONObject(type.name()));
                break;
            }
        }
        if (node instanceof ScrollPane sp) {
            Node c = sp.getContent();
            if (c != null) applyRecursive(c, section);
        } else if (node instanceof TitledPane tp) {
            Node c = tp.getContent();
            if (c != null) applyRecursive(c, section);
        } else if (node instanceof TabPane tbp) {
            for (Tab t : tbp.getTabs()) if (t.getContent() != null) applyRecursive(t.getContent(), section);
        } else if (node instanceof SplitPane spp) {
            for (Node item : spp.getItems()) applyRecursive(item, section);
        } else if (node instanceof Accordion acc) {
            for (TitledPane pane : acc.getPanes()) {
                applyRecursive(pane, section);
                if (pane.getContent() != null) applyRecursive(pane.getContent(), section);
            }
        }
        if (node instanceof Parent p) for (Node child : p.getChildrenUnmodifiable()) applyRecursive(child, section);
    }

    private static void clearFontRecursive(Node node) {
        if (node instanceof Labeled l) {
            l.setFont(null);
        } else if (node instanceof TextInputControl tic) {
            tic.setStyle(removeFontCss(tic.getStyle()));
        } else {
            node.setStyle(removeFontCss(node.getStyle()));
        }
        if (node instanceof ScrollPane sp) {
            Node c = sp.getContent();
            if (c != null) clearFontRecursive(c);
        } else if (node instanceof TitledPane tp) {
            Node c = tp.getContent();
            if (c != null) clearFontRecursive(c);
        } else if (node instanceof TabPane tbp) {
            for (Tab t : tbp.getTabs()) if (t.getContent() != null) clearFontRecursive(t.getContent());
        } else if (node instanceof SplitPane spp) {
            for (Node item : spp.getItems()) clearFontRecursive(item);
        } else if (node instanceof Accordion acc) {
            for (TitledPane pane : acc.getPanes()) {
                clearFontRecursive(pane);
                if (pane.getContent() != null) clearFontRecursive(pane.getContent());
            }
        }
        if (node instanceof Parent p) for (Node child : p.getChildrenUnmodifiable()) clearFontRecursive(child);
    }

    private static String removeFontCss(String style) {
        if (style == null) return "";
        return style.replaceAll("-fx-font[^;]*;", "").trim();
    }

    private static void applyFontToNode(Node node, JSONObject settings) {
        String family = settings.optString("family", null);
        if (family == null || family.isEmpty()) return;
        int size = settings.optInt("size", 14);
        boolean bold = settings.optBoolean("bold", false);
        boolean italic = settings.optBoolean("italic", false);
        FontWeight weight = bold ? FontWeight.BOLD : FontWeight.NORMAL;
        FontPosture posture = italic ? FontPosture.ITALIC : FontPosture.REGULAR;
        Font font = Font.font(family, weight, posture, size);
        if (node instanceof Labeled l) {
            l.setFont(font);
        } else if (node instanceof TextInputControl tic) {
            tic.setStyle(buildFontCss(family, size, bold, italic));
        } else {
            node.setStyle(buildFontCss(family, size, bold, italic));
        }
    }

    private static String buildFontCss(String family, int size, boolean bold, boolean italic) {
        return String.format(
                "-fx-font-family: '%s'; -fx-font-size: %dpx; -fx-font-weight: %s; -fx-font-style: %s;",
                family, size, bold ? "bold" : "normal", italic ? "italic" : "normal");
    }

    private static Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }
}