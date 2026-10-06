package com.safwat.hr.controller.backendSetting;

import com.safwat.hr.controller.admin.system.BackendService;
import com.safwat.hr.controller.backendSetting.BackendPropertiesStore.Entry;
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
 * BackendPropertiesController — تحرير application.properties للباك إند
 * من داخل الفرونت (وضع فردي).
 * <p>
 * يُحمّل الملف من المسار المكتشف تلقائياً، يعرض كل مفتاح في كارت
 * مع تسمية عربية مقابلة، ويحفظ في الملف مباشرة.
 */
public class BackendPropertiesController implements Initializable {

    // ══════════════════ FXML ══════════════════
    @FXML
    private Label currentCategoryLabel;
    @FXML
    private TextField searchField;
    @FXML
    private Label entryCountLabel;
    @FXML
    private SplitPane mainSplit;
    @FXML
    private ListView<String> categoryList;
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
    private Button btnDiscard, btnSaveAll, btnRestartBackend;

    // ══════════════════ State ══════════════════
    private BackendPropertiesStore store;
    private Map<String, List<Entry>> grouped = new LinkedHashMap<>();
    private String currentCategory;
    private final Map<String, String> pendingChanges = new LinkedHashMap<>();

    // ══════════════════ Init ══════════════════
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        SettingsThemeLoader.apply(categoryList);

        categoryList.getSelectionModel().selectedItemProperty()
                .addListener((o, oldV, newV) -> {
                    if (newV != null) showCategory(newV);
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
        store = new BackendPropertiesStore(det.get().backendConfig());

        if (!store.exists()) {
            setStatus("⚠️ ملف application.properties غير موجود — سيُنشأ عند الحفظ",
                    "stg-status-msg-warn");
        }

        try {
            List<Entry> all = store.read();
            grouped = all.stream().collect(Collectors.groupingBy(
                    e -> BackendPropertiesLabels.groupOf(e.key()),
                    LinkedHashMap::new,
                    Collectors.toList()));

            categoryList.setItems(FXCollections.observableArrayList(grouped.keySet()));
            int total = all.size();
            setStatus("تم تحميل " + total + " مفتاح", "stg-status-msg-ok");

            if (!grouped.isEmpty()) {
                categoryList.getSelectionModel().selectFirst();
            }
        } catch (Exception e) {
            setStatus("❌ فشل القراءة: " + e.getMessage(), "stg-status-msg-error");
        }
    }

    private void showCategory(String category) {
        currentCategory = category;
        currentCategoryLabel.setText(category);

        List<Entry> entries = grouped.getOrDefault(category, List.of());
        entryCountLabel.setText(entries.size() + " مفتاح");

        String q = searchField.getText();
        List<Entry> filtered = (q == null || q.isBlank())
                ? entries
                : entries.stream().filter(e -> matches(e, q.toLowerCase()))
                .collect(Collectors.toList());

        renderEntries(filtered);
    }

    private boolean matches(Entry e, String q) {
        return e.key().toLowerCase().contains(q)
                || (e.value() != null && e.value().toLowerCase().contains(q))
                || BackendPropertiesLabels.label(e.key()).toLowerCase().contains(q);
    }

    private void renderEntries(List<Entry> entries) {
        entriesContainer.getChildren().clear();
        if (entries.isEmpty()) {
            Label empty = new Label("لا توجد مفاتيح في هذه المجموعة");
            empty.getStyleClass().add("stg-empty-placeholder");
            VBox.setMargin(empty, new Insets(40, 0, 0, 0));
            entriesContainer.getChildren().add(empty);
            return;
        }
        for (Entry e : entries) {
            entriesContainer.getChildren().add(buildCard(e));
        }
    }

    // ══════════════════ Card Builder ══════════════════
    private Node buildCard(Entry entry) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(10, 14, 10, 14));
        card.getStyleClass().add("stg-card");

        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label keyLbl = new Label(entry.key());
        keyLbl.getStyleClass().add("stg-key-badge");
        topRow.getChildren().add(keyLbl);

        Label groupLbl = new Label(BackendPropertiesLabels.groupOf(entry.key()));
        groupLbl.getStyleClass().addAll("stg-badge", "stg-badge-muted");
        topRow.getChildren().add(groupLbl);

        card.getChildren().add(topRow);

        // التسمية العربية
        String arabic = BackendPropertiesLabels.hasLabel(entry.key());
        Label arLbl = new Label(arabic != null ? arabic : "(لا توجد تسمية عربية)");
        arLbl.getStyleClass().add("stg-field-hint");
        if (arabic != null) arLbl.getStyleClass().add("stg-field-label");
        card.getChildren().add(arLbl);

        // تعليق الملف الأصلي لو موجود
        if (entry.comment() != null && !entry.comment().isBlank()) {
            Label commentLbl = new Label("# " + entry.comment());
            commentLbl.getStyleClass().add("stg-field-value-muted");
            card.getChildren().add(commentLbl);
        }

        // حقل القيمة + زر حفظ
        HBox valueRow = new HBox(8);
        valueRow.setAlignment(Pos.CENTER_LEFT);

        TextField tf = new TextField(entry.value() != null ? entry.value() : "");
        tf.getStyleClass().add("stg-field-dark");
        HBox.setHgrow(tf, Priority.ALWAYS);
        tf.textProperty().addListener((o, oldV, newV) ->
                trackChange(entry.key(), newV, card));
        valueRow.getChildren().add(tf);

        Button saveBtn = iconBtn("💾", "stg-icon-btn-success", "حفظ هذا المفتاح");
        saveBtn.setOnAction(ev -> saveSingle(entry.key(), tf.getText(), card));
        valueRow.getChildren().add(saveBtn);

        card.getChildren().add(valueRow);
        return card;
    }

    // ══════════════════ Actions ══════════════════

    @FXML
    private void onSearch() {
        if (currentCategory != null) showCategory(currentCategory);
    }

    @FXML
    private void onDiscard() {
        pendingChanges.clear();
        updateFooter();
        if (currentCategory != null) showCategory(currentCategory);
        setStatus("تم تجاهل التغييرات", "stg-status-msg-warn");
    }

    @FXML
    private void onSaveAll() {
        if (pendingChanges.isEmpty() || store == null) return;
        showProgress(true);
        btnSaveAll.setDisable(true);
        setStatus("جاري حفظ " + pendingChanges.size() + " مفتاح...", "stg-status-msg");

        new Thread(() -> {
            try {
                store.updateBatch(new LinkedHashMap<>(pendingChanges));
                Platform.runLater(() -> {
                    showProgress(false);
                    pendingChanges.clear();
                    updateFooter();
                    loadData();
                    setStatus("✓ تم حفظ التغييرات — أعد تشغيل الباك إند للتطبيق",
                            "stg-status-msg-ok");
                    askRestartBackend();
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

    @FXML
    private void onRestartBackend() {
        var det = PathResolver.detect();
        if (det.isEmpty()) {
            setStatus("❌ لا يمكن تحديد مسار الباك إند", "stg-status-msg-error");
            return;
        }
        showProgress(true);
        setStatus("🔄 إعادة تشغيل الباك إند...", "stg-status-msg");

        new Thread(() -> {
            boolean ok = BackendService.getInstance()
                    .restart(det.get().backendExe().toString(), false);
            Platform.runLater(() -> {
                showProgress(false);
                setStatus(ok ? "✓ تم إعادة التشغيل" : "❌ فشل إعادة التشغيل",
                        ok ? "stg-status-msg-ok" : "stg-status-msg-error");
            });
        }).start();
    }

    private void saveSingle(String key, String value, VBox card) {
        if (store == null) return;
        showProgress(true);
        setStatus("جاري حفظ: " + key + "...", "stg-status-msg");

        new Thread(() -> {
            try {
                store.update(key, value);
                Platform.runLater(() -> {
                    showProgress(false);
                    pendingChanges.remove(key);
                    updateFooter();
                    flashCard(card, true);
                    setStatus("✓ تم حفظ: " + key, "stg-status-msg-ok");
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

    private void trackChange(String key, String value, VBox card) {
        String original = findOriginal(key);
        if (Objects.equals(original, value)) {
            pendingChanges.remove(key);
            card.getStyleClass().remove("stg-card-changed");
        } else {
            pendingChanges.put(key, value);
            if (!card.getStyleClass().contains("stg-card-changed"))
                card.getStyleClass().add("stg-card-changed");
        }
        updateFooter();
    }

    private String findOriginal(String key) {
        for (var list : grouped.values()) {
            for (var e : list) {
                if (e.key().equals(key)) return e.value();
            }
        }
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

    private void askRestartBackend() {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("إعادة تشغيل الباك إند");
        a.setHeaderText(null);
        a.setContentText("تم حفظ التغييرات. هل تريد إعادة تشغيل الباك إند الآن لتطبيقها؟");
        a.getDialogPane().getStyleClass().add("stg-dialog");
        SettingsThemeLoader.apply(a.getDialogPane());
        a.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) onRestartBackend();
        });
    }

    // ══════════════════ UI Helpers ══════════════════

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
}