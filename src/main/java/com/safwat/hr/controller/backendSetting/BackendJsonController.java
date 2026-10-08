package com.safwat.hr.controller.backendSetting;

import com.safwat.hr.controller.backendSetting.BackendJsonStore.Entry;
import com.safwat.hr.system.setup.PathResolver;
import com.safwat.hr.ui.theme.SettingsThemeLoader;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * BackendJsonController — تحرير app_config.json للباك إند من الفرونت.
 * <p>
 * التعليقات (_comment_*) مابتظهرش كمفاتيح، وبتتعرض كنص توضيحي:
 * تعليق القسم جنب اسمه، وتعليق المفتاح تحت مساره.
 */
public class BackendJsonController implements Initializable {

    // ══════════════════ FXML ══════════════════
    @FXML
    private Label currentCategoryLabel;
    @FXML
    private TextField searchField;
    @FXML
    private Label entryCountLabel;
    @FXML
    private ListView<String> sectionList;
    @FXML
    private ScrollPane entriesScroll;
    @FXML
    private VBox entriesContainer;
    @FXML
    private Label statusLabel;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private Label pendingBadge;
    @FXML
    private Button btnDiscard, btnSaveAll, btnCreateDefaults;

    // ══════════════════ State ══════════════════
    private BackendJsonStore store;
    private Map<String, List<Entry>> grouped = new LinkedHashMap<>();
    /** التعليقات: "setting" → تعليق القسم، "setting.scaleUp" → تعليق المفتاح. */
    private Map<String, String> comments = new HashMap<>();
    private String currentSection;
    private final Map<String, String> pendingChanges = new LinkedHashMap<>();

    // ══════════════════ Init ══════════════════
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        SettingsThemeLoader.apply(sectionList);

        sectionList.getSelectionModel().selectedItemProperty()
                .addListener((o, oldV, newV) -> {
                    if (newV != null) showSection(newV);
                });

        loadData();
    }

    // ══════════════════ Loading ══════════════════
    private void loadData() {
        var det = PathResolver.detect();
        if (det.isEmpty()) {
            setStatus("❌ لم يتم العثور على بنية التوزيع", "stg-status-msg-error");
            return;
        }

        store = new BackendJsonStore(det.get().backendAppConfig(), BackendJsonStore.BACKEND_DOCS);

        if (!store.exists()) {
            setStatus("⚠️ ملف app_config.json غير موجود — اضغط 'إنشاء بالافتراضي'",
                    "stg-status-msg-warn");
            btnCreateDefaults.setVisible(true);
            btnCreateDefaults.setManaged(true);
            grouped.clear();
            comments.clear();
            sectionList.setItems(FXCollections.observableArrayList());
            return;
        }

        btnCreateDefaults.setVisible(false);
        btnCreateDefaults.setManaged(false);

        try {
            List<Entry> all = store.read();
            comments = store.comments();

            grouped = all.stream().collect(Collectors.groupingBy(
                    Entry::section,
                    LinkedHashMap::new,
                    Collectors.toList()));

            sectionList.setItems(FXCollections.observableArrayList(grouped.keySet()));
            setStatus("تم تحميل " + all.size() + " مفتاح", "stg-status-msg-ok");

            if (!grouped.isEmpty()) {
                sectionList.getSelectionModel().selectFirst();
            }
        } catch (Exception e) {
            setStatus("❌ فشل القراءة: " + e.getMessage(), "stg-status-msg-error");
        }
    }

    @FXML
    private void onCreateDefaults() {
        if (store == null) return;
        try {
            store.createDefaults();
            setStatus("✓ تم إنشاء الملف بالافتراضي", "stg-status-msg-ok");
            loadData();
        } catch (Exception e) {
            setStatus("❌ فشل الإنشاء: " + e.getMessage(), "stg-status-msg-error");
        }
    }

    private void showSection(String section) {
        currentSection = section;

        String sectionDoc = comments.get(section);
        currentCategoryLabel.setText(BackendJsonLabels.sectionTitle(section)
                + (isBlank(sectionDoc) ? "" : "  —  " + sectionDoc));

        List<Entry> entries = grouped.getOrDefault(section, List.of());
        entryCountLabel.setText(entries.size() + " مفتاح");

        String q = searchField.getText();
        List<Entry> filtered = (q == null || q.isBlank())
                ? entries
                : entries.stream()
                .filter(e -> matches(e, q.toLowerCase()))
                .collect(Collectors.toList());

        renderEntries(filtered);
    }

    private boolean matches(Entry e, String q) {
        return e.key().toLowerCase().contains(q)
                || BackendJsonLabels.label(e.path()).toLowerCase().contains(q)
                || (e.value() != null && e.value().toLowerCase().contains(q))
                || (comments.get(e.path()) != null
                && comments.get(e.path()).toLowerCase().contains(q));
    }

    private void renderEntries(List<Entry> entries) {
        entriesContainer.getChildren().clear();
        if (entries.isEmpty()) {
            Label empty = new Label("لا توجد مفاتيح في هذا القسم");
            empty.getStyleClass().add("stg-empty-placeholder");
            VBox.setMargin(empty, new Insets(40, 0, 0, 0));
            entriesContainer.getChildren().add(empty);
            return;
        }
        for (Entry e : entries) {
            entriesContainer.getChildren().add(buildCard(e));
        }
    }

    // ══════════════════ Card ══════════════════
    private Node buildCard(Entry entry) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(10, 14, 10, 14));
        card.getStyleClass().add("stg-card");

        // الصف العلوي: key + type badge
        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label keyLbl = new Label(entry.key());
        keyLbl.getStyleClass().add("stg-key-badge");
        topRow.getChildren().add(keyLbl);

        topRow.getChildren().add(badge(entry.type(), typeClass(entry.type())));
        card.getChildren().add(topRow);

        // التسمية العربية
        String arabic = BackendJsonLabels.label(entry.path());
        if (!arabic.equals(entry.path())) {
            Label arLbl = new Label(arabic);
            arLbl.getStyleClass().add("stg-field-label");
            card.getChildren().add(arLbl);
        }

        // المسار الكامل
        Label pathLbl = new Label(entry.path());
        pathLbl.getStyleClass().add("stg-field-value-muted");
        card.getChildren().add(pathLbl);

        // تعليق المفتاح (من _comment_ اللي فوقه في الملف)
        String doc = comments.get(entry.path());
        if (!isBlank(doc)) {
            Label docLbl = new Label("# " + doc);
            docLbl.getStyleClass().add("stg-field-hint");
            card.getChildren().add(docLbl);
        }

        // حقل القيمة
        HBox valueRow = new HBox(8);
        valueRow.setAlignment(Pos.CENTER_LEFT);

        Control inputCtrl;
        if ("BOOLEAN".equals(entry.type())) {
            ComboBox<String> cb = new ComboBox<>(
                    FXCollections.observableArrayList("true", "false"));
            cb.setValue("true".equalsIgnoreCase(entry.value()) ? "true" : "false");
            cb.getStyleClass().add("stg-combo-light");
            cb.setPrefWidth(160);
            cb.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(cb, Priority.ALWAYS);
            cb.valueProperty().addListener((o, oldV, newV) ->
                    trackChange(entry.path(), newV, card));
            inputCtrl = cb;
        } else {
            TextField tf = new TextField(entry.value() != null ? entry.value() : "");
            tf.getStyleClass().add("stg-field-dark");
            HBox.setHgrow(tf, Priority.ALWAYS);
            tf.textProperty().addListener((o, oldV, newV) ->
                    trackChange(entry.path(), newV, card));
            inputCtrl = tf;
        }
        valueRow.getChildren().add(inputCtrl);

        Button saveBtn = iconBtn("💾", "stg-icon-btn-success", "حفظ هذا المفتاح");
        saveBtn.setOnAction(ev -> {
            String val = inputCtrl instanceof ComboBox
                    ? String.valueOf(((ComboBox<?>) inputCtrl).getValue())
                    : ((TextField) inputCtrl).getText();
            saveSingle(entry.path(), val, card);
        });
        valueRow.getChildren().add(saveBtn);

        card.getChildren().add(valueRow);
        return card;
    }

    private String typeClass(String type) {
        return switch (type) {
            case "BOOLEAN" -> "stg-badge-bool";
            case "INT" -> "stg-badge-int";
            case "DOUBLE" -> "stg-badge-double";
            case "STRING" -> "stg-badge-string";
            case "ARRAY" -> "stg-badge-array";
            case "OBJECT" -> "stg-badge-object";
            default -> "stg-badge-muted";
        };
    }

    private Label badge(String text, String cssClass) {
        Label l = new Label(text);
        l.getStyleClass().addAll("stg-badge", cssClass);
        return l;
    }

    // ══════════════════ Actions ══════════════════

    @FXML
    private void onSearch() {
        if (currentSection != null) showSection(currentSection);
    }

    @FXML
    private void onDiscard() {
        pendingChanges.clear();
        updateFooter();
        if (currentSection != null) showSection(currentSection);
        setStatus("تم تجاهل التغييرات", "stg-status-msg-warn");
    }

    @FXML
    private void onSaveAll() {
        if (pendingChanges.isEmpty() || store == null) return;
        showProgress(true);
        btnSaveAll.setDisable(true);
        setStatus("جاري حفظ " + pendingChanges.size() + " مفتاح...", "stg-status-msg");

        Map<String, String> snapshot = new LinkedHashMap<>(pendingChanges);

        new Thread(() -> {
            try {
                store.updateBatch(snapshot);
                Platform.runLater(() -> {
                    showProgress(false);
                    pendingChanges.clear();
                    updateFooter();
                    loadData();
                    setStatus("✓ تم الحفظ — ستُطبَّق عند بدء الباك إند", "stg-status-msg-ok");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showProgress(false);
                    btnSaveAll.setDisable(false);
                    setStatus("❌ فشل الحفظ: " + e.getMessage(), "stg-status-msg-error");
                });
            }
        }).start();
    }

    private void saveSingle(String path, String value, VBox card) {
        if (store == null) return;
        showProgress(true);
        setStatus("جاري حفظ: " + path + "...", "stg-status-msg");

        new Thread(() -> {
            try {
                store.update(path, value);
                Platform.runLater(() -> {
                    showProgress(false);
                    pendingChanges.remove(path);
                    updateFooter();
                    flashCard(card, true);
                    setStatus("✓ تم حفظ: " + path, "stg-status-msg-ok");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showProgress(false);
                    flashCard(card, false);
                    setStatus("❌ " + e.getMessage(), "stg-status-msg-error");
                });
            }
        }).start();
    }

    // ══════════════════ State ══════════════════

    private void trackChange(String path, String value, VBox card) {
        String original = findOriginal(path);
        if (Objects.equals(original, value)) {
            pendingChanges.remove(path);
            card.getStyleClass().remove("stg-card-changed");
        } else {
            pendingChanges.put(path, value);
            if (!card.getStyleClass().contains("stg-card-changed"))
                card.getStyleClass().add("stg-card-changed");
        }
        updateFooter();
    }

    private String findOriginal(String path) {
        for (var list : grouped.values())
            for (var e : list)
                if (e.path().equals(path)) return e.value();
        return null;
    }

    private void updateFooter() {
        int n = pendingChanges.size();
        btnSaveAll.setDisable(n == 0);
        btnSaveAll.setText(n > 0 ? "💾  حفظ الكل (" + n + ")" : "💾  حفظ الكل");
        btnDiscard.setVisible(n > 0);
        pendingBadge.setVisible(n > 0);
        pendingBadge.setText(n + " تغيير غير محفوظ");
    }

    // ══════════════════ Helpers ══════════════════

    private Button iconBtn(String icon, String cssClass, String tip) {
        Button b = new Button(icon);
        b.setTooltip(new Tooltip(tip));
        b.getStyleClass().add("stg-icon-btn-base");
        b.getStyleClass().add(cssClass);
        return b;
    }

    private void flashCard(VBox card, boolean ok) {
        String flashClass = ok ? "stg-card-flash-ok" : "stg-card-flash-error";
        card.getStyleClass().add(flashClass);
        new Thread(() -> {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException ignored) {
            }
            Platform.runLater(() -> {
                card.getStyleClass().remove(flashClass);
                card.getStyleClass().remove("stg-card-changed");
            });
        }).start();
    }

    private void showProgress(boolean show) {
        Platform.runLater(() -> progressIndicator.setVisible(show));
    }

    private void setStatus(String msg, String cssClass) {
        Platform.runLater(() -> {
            statusLabel.setText(msg);
            statusLabel.getStyleClass().removeAll(
                    "stg-status-label", "stg-status-msg-ok",
                    "stg-status-msg-error", "stg-status-msg-warn");
            statusLabel.getStyleClass().add(cssClass);
        });
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}