package com.safwat.hr.controller.backendSetting;

import com.safwat.hr.controller.backendSetting.AppConfigApiClient.JsonEntry;
import com.safwat.hr.controller.backendSetting.SettingsApiClient.PropertyEntry;
import com.safwat.hr.ui.theme.SettingsThemeLoader;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * SettingsController — شاشة إعدادات التطبيق
 * <p>
 * تحتوي على تابين:
 * 1) Application Properties  → /api/settings
 * 2) App Config (JSON)       → /api/app-config
 * <p>
 * كل التنسيقات من settings.css عبر كلاسات stg- (بدون inline styles).
 */
public class SettingsController implements Initializable {

    // ══════════════════════════ FXML Fields — Header ══════════════════════════
    @FXML
    private Label headerSubtitle;
    @FXML
    private TextField searchField;
    @FXML
    private Button btnRefresh, btnBackup, btnAdd, btnBackups;

    // ══════════════════════════ FXML Fields — Tabs ══════════════════════════
    @FXML
    private TabPane mainTabPane;

    // ── Properties tab ──
    @FXML
    private SplitPane propsSplitPane;
    @FXML
    private ListView<String> categoryList;
    @FXML
    private Label currentCategoryLabel;
    @FXML
    private Label entryCountLabel;
    @FXML
    private ScrollPane entriesScroll;
    @FXML
    private VBox entriesContainer;
    @FXML
    private VBox loadingPlaceholder;

    // ── JSON tab ──
    @FXML
    private SplitPane jsonSplitPane;
    @FXML
    private ListView<String> jsonCategoryList;
    @FXML
    private Label jsonCategoryLabel;
    @FXML
    private Label jsonEntryCountLabel;
    @FXML
    private ScrollPane jsonEntriesScroll;
    @FXML
    private VBox jsonEntriesContainer;
    @FXML
    private VBox jsonLoadingPlaceholder;

    // ══════════════════════════ FXML Fields — Footer ══════════════════════════
    @FXML
    private Label statusLabel;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private Label pendingBadge;
    @FXML
    private Button btnDiscard;
    @FXML
    private Button btnSaveAll;

    // ══════════════════════════ State — Properties ══════════════════════════
    private Map<String, List<PropertyEntry>> groupedData = new LinkedHashMap<>();
    private String currentCategory = null;
    private final Map<String, String> pendingChanges = new LinkedHashMap<>();

    // ══════════════════════════ State — JSON ══════════════════════════
    private Map<String, List<JsonEntry>> jsonGroupedData = new LinkedHashMap<>();
    private String currentJsonCategory = null;
    private final Map<String, String> jsonPendingChanges = new LinkedHashMap<>();

    // ══════════════════════════ Init ══════════════════════════
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // ✅ حمّل settings.css
        SettingsThemeLoader.apply(btnAdd);

        setupCategoryList(categoryList);
        setupCategoryList(jsonCategoryList);

        categoryList.getSelectionModel().selectedItemProperty()
                .addListener((obs, o, n) -> {
                    if (n != null) showCategory(n);
                });

        jsonCategoryList.getSelectionModel().selectedItemProperty()
                .addListener((obs, o, n) -> {
                    if (n != null) showJsonCategory(n);
                });

        mainTabPane.getSelectionModel().selectedIndexProperty()
                .addListener((obs, o, n) -> onTabChanged(n.intValue()));

        loadData();
        loadJsonData();
        updateTabLabels(0);
    }

    private void setupCategoryList(ListView<String> list) {
        list.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(categoryIcon(item) + "  " + item);
                // ✅ clear style على الخلية لأن stg-category-list بتتكفل بيه
                setStyle("");
            }
        });
    }

    private void onTabChanged(int index) {
        updateTabLabels(index);
        pendingChanges.clear();
        jsonPendingChanges.clear();
        updateFooter();
        if (index == 0) headerSubtitle.setText("إدارة ملف application.properties");
        else headerSubtitle.setText("إدارة ملف app_config.json");
    }

    private void updateTabLabels(int idx) {
        // ممكن نضيف تخصيصات لاحقاً حسب التاب
    }

    // ══════════════════════════════════════════════════════════════
    //  PROPERTIES TAB — Data Loading
    // ══════════════════════════════════════════════════════════════
    private void loadData() {
        showLoading(true);
        setStatus("جاري تحميل الإعدادات...", "stg-status-msg");

        SettingsApiClient.getGroupedAsync().thenAccept(response ->
                Platform.runLater(() -> {
                    showLoading(false);
                    if (response.isSuccess() && response.getData() != null) {
                        groupedData = new LinkedHashMap<>(response.getData());
                        categoryList.setItems(FXCollections.observableArrayList(groupedData.keySet()));
                        int total = groupedData.values().stream().mapToInt(List::size).sum();
                        setStatus("تم تحميل " + total + " إعداد ✓", "stg-status-msg-ok");
                        if (!groupedData.isEmpty())
                            categoryList.getSelectionModel().select(
                                    groupedData.keySet().iterator().next());
                    } else {
                        setStatus("❌ فشل التحميل: " + response.getMessage(), "stg-status-msg-error");
                    }
                })
        );
    }

    private void showCategory(String category) {
        currentCategory = category;
        currentCategoryLabel.setText(categoryIcon(category) + "  " + category);

        List<PropertyEntry> entries = groupedData.getOrDefault(category, List.of());
        entryCountLabel.setText(entries.size() + " إعداد");

        String query = searchField.getText();
        List<PropertyEntry> filtered = (query == null || query.isBlank())
                ? entries
                : entries.stream().filter(e -> matchesSearch(e, query.toLowerCase()))
                .collect(Collectors.toList());

        renderEntries(filtered);
    }

    private void renderEntries(List<PropertyEntry> entries) {
        entriesContainer.getChildren().clear();
        if (entries.isEmpty()) {
            Label empty = new Label("لا توجد إعدادات في هذا التصنيف");
            empty.getStyleClass().add("stg-empty-placeholder");
            VBox.setMargin(empty, new Insets(40, 0, 0, 0));
            entriesContainer.getChildren().add(empty);
            return;
        }
        for (PropertyEntry entry : entries)
            entriesContainer.getChildren().add(buildEntryCard(entry));
    }

    // ══════════════════════════════════════════════════════════════
    //  PROPERTIES TAB — Card Builder
    // ══════════════════════════════════════════════════════════════
    private Node buildEntryCard(PropertyEntry entry) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(10, 14, 10, 14));
        card.getStyleClass().add("stg-card");

        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label keyLbl = new Label(entry.key());
        keyLbl.getStyleClass().add("stg-key-badge");
        topRow.getChildren().add(keyLbl);

        if (entry.envBound() && entry.envVarName() != null)
            topRow.getChildren().add(badge("🐳 ENV: " + entry.envVarName(), "stg-badge-env"));
        if (entry.nested())
            topRow.getChildren().add(badge("🔗 nested", "stg-badge-nested"));
        if (!entry.editable())
            topRow.getChildren().add(badge("🔒 محمي", "stg-badge-muted"));
        if (entry.sensitive())
            topRow.getChildren().add(badge("🔑 حساس", "stg-badge-warn"));

        card.getChildren().add(topRow);

        if (entry.comment() != null && !entry.comment().isBlank()) {
            Label commentLbl = new Label("# " + entry.comment());
            commentLbl.getStyleClass().add("stg-field-hint");
            card.getChildren().add(commentLbl);
        }

        if (entry.envBound()) {
            String activeEnvVal = System.getenv(entry.envVarName());
            VBox envInfo = new VBox(3);
            if (activeEnvVal != null) {
                Label activeLabel = new Label(
                        "🟢 متغير البيئة نشط: " + entry.envVarName() + " = " + activeEnvVal);
                activeLabel.getStyleClass().add("stg-env-active");
                Label infoLabel = new Label("التعديل هنا لن يؤثر — متغير البيئة له الأولوية");
                infoLabel.getStyleClass().add("stg-env-hint");
                envInfo.getChildren().addAll(activeLabel, infoLabel);
            } else {
                Label warnLbl = new Label("⚙ Fallback نشط — تعديل القيمة يغيّر الـ Default في الملف");
                warnLbl.getStyleClass().add("stg-env-warn");
                Label varName = new Label("متغير البيئة: " + entry.envVarName() + " (غير مضبوط)");
                varName.getStyleClass().add("stg-env-hint");
                envInfo.getChildren().addAll(warnLbl, varName);
            }
            card.getChildren().add(envInfo);
        }

        HBox valueRow = new HBox(8);
        valueRow.setAlignment(Pos.CENTER_LEFT);

        String editableValue = entry.envBound()
                ? (entry.defaultValue() != null ? entry.defaultValue() : "")
                : (entry.rawValue() != null ? entry.rawValue() : "");

        Control inputCtrl;
        if (!entry.editable()) {
            TextField tf = new TextField(editableValue);
            tf.setEditable(false);
            tf.getStyleClass().add("stg-field-dark");
            tf.getStyleClass().add("stg-field-disabled");
            HBox.setHgrow(tf, Priority.ALWAYS);
            inputCtrl = tf;
        } else if (entry.sensitive()) {
            PasswordField pf = new PasswordField();
            pf.setText(editableValue);
            pf.getStyleClass().add("stg-field-dark");
            HBox.setHgrow(pf, Priority.ALWAYS);
            pf.textProperty().addListener((obs, o, n) -> trackChange(entry.key(), n, card));
            inputCtrl = pf;
        } else {
            TextField tf = new TextField(editableValue);
            tf.getStyleClass().add("stg-field-dark");
            HBox.setHgrow(tf, Priority.ALWAYS);
            tf.textProperty().addListener((obs, o, n) -> trackChange(entry.key(), n, card));
            inputCtrl = tf;
        }
        valueRow.getChildren().add(inputCtrl);

        if (entry.editable()) {
            Button saveBtn = iconBtn("💾", "stg-icon-btn-success", "حفظ هذا الإعداد");
            saveBtn.setOnAction(e -> {
                String val = inputCtrl instanceof PasswordField
                        ? ((PasswordField) inputCtrl).getText()
                        : ((TextField) inputCtrl).getText();
                saveSingle(entry.key(), val, card);
            });
            Button delBtn = iconBtn("🗑", "stg-icon-btn-danger", "حذف هذا الإعداد");
            delBtn.setOnAction(e -> confirmDelete(entry.key()));
            valueRow.getChildren().addAll(saveBtn, delBtn);
        }
        card.getChildren().add(valueRow);

        if (entry.rawValue() != null && entry.envBound()) {
            Label rawLbl = new Label("في الملف: " + entry.rawValue());
            rawLbl.getStyleClass().add("stg-raw-value");
            card.getChildren().add(rawLbl);
        }

        return card;
    }

    // ══════════════════════════════════════════════════════════════
    //  JSON TAB — Data Loading
    // ══════════════════════════════════════════════════════════════
    private void loadJsonData() {
        showJsonLoading(true);
        SettingsApiClient.getFileInfoAsync();

        AppConfigApiClient.getGroupedAsync().thenAccept(response ->
                Platform.runLater(() -> {
                    showJsonLoading(false);
                    if (response.isSuccess() && response.getData() != null) {
                        jsonGroupedData = new LinkedHashMap<>(response.getData());
                        jsonCategoryList.setItems(
                                FXCollections.observableArrayList(jsonGroupedData.keySet()));
                        int total = jsonGroupedData.values().stream().mapToInt(List::size).sum();
                        setStatus("تم تحميل " + total + " مفتاح JSON ✓", "stg-status-msg-ok");
                        if (!jsonGroupedData.isEmpty())
                            jsonCategoryList.getSelectionModel().select(
                                    jsonGroupedData.keySet().iterator().next());
                    } else {
                        setStatus("❌ فشل تحميل JSON: " + response.getMessage(), "stg-status-msg-error");
                    }
                })
        );
    }

    private void showJsonCategory(String category) {
        currentJsonCategory = category;
        jsonCategoryLabel.setText(categoryIcon(category) + "  " + category);

        List<JsonEntry> entries = jsonGroupedData.getOrDefault(category, List.of());
        jsonEntryCountLabel.setText(entries.size() + " مفتاح");

        String query = searchField.getText();
        List<JsonEntry> filtered = (query == null || query.isBlank())
                ? entries
                : entries.stream().filter(e -> matchesJsonSearch(e, query.toLowerCase()))
                .collect(Collectors.toList());

        renderJsonEntries(filtered);
    }

    private void renderJsonEntries(List<JsonEntry> entries) {
        jsonEntriesContainer.getChildren().clear();
        if (entries.isEmpty()) {
            Label empty = new Label("لا توجد إعدادات في هذا القسم");
            empty.getStyleClass().add("stg-empty-placeholder");
            VBox.setMargin(empty, new Insets(40, 0, 0, 0));
            jsonEntriesContainer.getChildren().add(empty);
            return;
        }
        for (JsonEntry entry : entries)
            jsonEntriesContainer.getChildren().add(buildJsonCard(entry));
    }

    // ══════════════════════════════════════════════════════════════
    //  JSON TAB — Card Builder
    // ══════════════════════════════════════════════════════════════
    private Node buildJsonCard(JsonEntry entry) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(10, 14, 10, 14));
        card.getStyleClass().add("stg-card");

        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label keyLbl = new Label(entry.key());
        keyLbl.getStyleClass().add("stg-key-badge");
        topRow.getChildren().add(keyLbl);

        topRow.getChildren().add(badge(entry.type(), typeClass(entry.type())));

        if (!entry.editable())
            topRow.getChildren().add(badge("🔒 محمي", "stg-badge-muted"));

        card.getChildren().add(topRow);

        Label pathLbl = new Label(entry.path());
        pathLbl.getStyleClass().add("stg-field-value-muted");
        card.getChildren().add(pathLbl);

        if (!entry.editable()) return card;

        HBox valueRow = new HBox(8);
        valueRow.setAlignment(Pos.CENTER_LEFT);

        Control inputCtrl;
        if ("BOOLEAN".equals(entry.type())) {
            ComboBox<String> cb = new ComboBox<>(
                    FXCollections.observableArrayList("true", "false"));
            cb.setValue(entry.value() != null ? entry.value() : "false");
            cb.getStyleClass().add("stg-combo-light");
            cb.setPrefWidth(140);
            cb.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(cb, Priority.ALWAYS);
            cb.valueProperty().addListener((obs, o, n) ->
                    trackJsonChange(entry.path(), n, card));
            inputCtrl = cb;
        } else {
            TextField tf = new TextField(entry.value() != null ? entry.value() : "");
            tf.getStyleClass().add("stg-field-dark");
            HBox.setHgrow(tf, Priority.ALWAYS);
            tf.textProperty().addListener((obs, o, n) ->
                    trackJsonChange(entry.path(), n, card));
            inputCtrl = tf;
        }
        valueRow.getChildren().add(inputCtrl);

        Button saveBtn = iconBtn("💾", "stg-icon-btn-success", "حفظ هذا المفتاح");
        saveBtn.setOnAction(e -> {
            String val = inputCtrl instanceof ComboBox
                    ? String.valueOf(((ComboBox<?>) inputCtrl).getValue())
                    : ((TextField) inputCtrl).getText();
            saveJsonSingle(entry.path(), val, card);
        });

        Button delBtn = iconBtn("🗑", "stg-icon-btn-danger", "حذف هذا المفتاح");
        delBtn.setOnAction(e -> confirmJsonDelete(entry.path()));

        valueRow.getChildren().addAll(saveBtn, delBtn);
        card.getChildren().add(valueRow);

        return card;
    }

    // ══════════════════════════════════════════════════════════════
    //  FXML Actions — Toolbar
    // ══════════════════════════════════════════════════════════════
    @FXML
    private void onSearch() {
        int idx = mainTabPane.getSelectionModel().getSelectedIndex();
        if (idx == 0 && currentCategory != null) showCategory(currentCategory);
        if (idx == 1 && currentJsonCategory != null) showJsonCategory(currentJsonCategory);
    }

    @FXML
    private void onCategorySelected() {
        String sel = categoryList.getSelectionModel().getSelectedItem();
        if (sel != null) showCategory(sel);
    }

    @FXML
    private void onJsonCategorySelected() {
        String sel = jsonCategoryList.getSelectionModel().getSelectedItem();
        if (sel != null) showJsonCategory(sel);
    }

    @FXML
    private void onRefresh() {
        int idx = mainTabPane.getSelectionModel().getSelectedIndex();
        if (idx == 0) {
            pendingChanges.clear();
            updateFooter();
            loadData();
        } else {
            jsonPendingChanges.clear();
            updateFooter();
            loadJsonData();
        }
    }

    @FXML
    private void onAdd() {
        int idx = mainTabPane.getSelectionModel().getSelectedIndex();
        if (idx == 0) showAddDialog();
        else showJsonAddDialog();
    }

    @FXML
    private void onShowBackups() {
        int idx = mainTabPane.getSelectionModel().getSelectedIndex();
        if (idx == 0) showBackupsDialog();
        else showJsonBackupsDialog();
    }

    @FXML
    private void onBackup() {
        int idx = mainTabPane.getSelectionModel().getSelectedIndex();
        setStatus("جاري عمل نسخة احتياطية...", "stg-status-msg");
        showProgress(true);

        if (idx == 0) {
            SettingsApiClient.backupAsync().thenAccept(r -> Platform.runLater(() -> {
                showProgress(false);
                setStatus(r.isSuccess() ? "✓ تم عمل نسخة احتياطية" : "❌ " + r.getMessage(),
                        r.isSuccess() ? "stg-status-msg-ok" : "stg-status-msg-error");
            }));
        } else {
            AppConfigApiClient.backupAsync().thenAccept(r -> Platform.runLater(() -> {
                showProgress(false);
                setStatus(r.isSuccess() ? "✓ تم عمل نسخة احتياطية (JSON)" : "❌ " + r.getMessage(),
                        r.isSuccess() ? "stg-status-msg-ok" : "stg-status-msg-error");
            }));
        }
    }

    @FXML
    private void onDiscard() {
        int idx = mainTabPane.getSelectionModel().getSelectedIndex();
        if (idx == 0) {
            pendingChanges.clear();
            updateFooter();
            if (currentCategory != null) showCategory(currentCategory);
        } else {
            jsonPendingChanges.clear();
            updateFooter();
            if (currentJsonCategory != null) showJsonCategory(currentJsonCategory);
        }
        setStatus("تم تجاهل التغييرات", "stg-status-msg-warn");
    }

    @FXML
    private void onSaveAll() {
        int idx = mainTabPane.getSelectionModel().getSelectedIndex();
        if (idx == 0) saveAllProperties();
        else saveAllJson();
    }

    private void saveAllProperties() {
        if (pendingChanges.isEmpty()) return;
        showProgress(true);
        btnSaveAll.setDisable(true);
        setStatus("جاري حفظ " + pendingChanges.size() + " إعداد...", "stg-status-msg");

        SettingsApiClient.batchUpdateAsync(new LinkedHashMap<>(pendingChanges))
                .thenAccept(r -> Platform.runLater(() -> {
                    showProgress(false);
                    if (r.isSuccess()) {
                        pendingChanges.clear();
                        updateFooter();
                        loadData();
                        setStatus("✓ تم حفظ كل التغييرات", "stg-status-msg-ok");
                    } else {
                        btnSaveAll.setDisable(false);
                        setStatus("❌ فشل الحفظ: " + r.getMessage(), "stg-status-msg-error");
                    }
                }));
    }

    private void saveAllJson() {
        if (jsonPendingChanges.isEmpty()) return;
        showProgress(true);
        btnSaveAll.setDisable(true);
        setStatus("جاري حفظ " + jsonPendingChanges.size() + " مفتاح...", "stg-status-msg");

        List<Map.Entry<String, String>> entries = List.copyOf(jsonPendingChanges.entrySet());
        saveJsonSequentially(entries, 0, new AtomicInteger(0));
    }

    private void saveJsonSequentially(List<Map.Entry<String, String>> entries,
                                      int index,
                                      AtomicInteger okCount) {
        if (index >= entries.size()) {
            Platform.runLater(() -> {
                showProgress(false);
                jsonPendingChanges.clear();
                updateFooter();
                loadJsonData();
                setStatus("✓ تم حفظ " + okCount.get() + " مفتاح", "stg-status-msg-ok");
            });
            return;
        }

        Map.Entry<String, String> e = entries.get(index);
        AppConfigApiClient.updateAsync(e.getKey(), e.getValue())
                .thenAccept(r -> {
                    if (r.isSuccess()) okCount.incrementAndGet();
                    saveJsonSequentially(entries, index + 1, okCount);
                });
    }

    // ══════════════════════════════════════════════════════════════
    //  Single Save / Delete — Properties
    // ══════════════════════════════════════════════════════════════
    private void saveSingle(String key, String value, VBox card) {
        showProgress(true);
        setStatus("جاري حفظ: " + key + "...", "stg-status-msg");
        SettingsApiClient.updateAsync(key, value).thenAccept(r ->
                Platform.runLater(() -> {
                    showProgress(false);
                    if (r.isSuccess()) {
                        pendingChanges.remove(key);
                        updateFooter();
                        flashCard(card, true);
                        setStatus("✓ تم حفظ: " + key, "stg-status-msg-ok");
                    } else {
                        flashCard(card, false);
                        setStatus("❌ " + r.getMessage(), "stg-status-msg-error");
                    }
                })
        );
    }

    private void confirmDelete(String key) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("حذف إعداد");
        alert.setHeaderText("حذف: " + key);
        alert.setContentText("سيتم عمل نسخة احتياطية تلقائياً. هل أنت متأكد؟");
        styleAlert(alert);
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                showProgress(true);
                SettingsApiClient.deleteAsync(key).thenAccept(r ->
                        Platform.runLater(() -> {
                            showProgress(false);
                            if (r.isSuccess()) {
                                loadData();
                                setStatus("✓ تم حذف: " + key, "stg-status-msg-ok");
                            } else setStatus("❌ " + r.getMessage(), "stg-status-msg-error");
                        })
                );
            }
        });
    }

    // ══════════════════════════════════════════════════════════════
    //  Single Save / Delete — JSON
    // ══════════════════════════════════════════════════════════════
    private void saveJsonSingle(String path, String value, VBox card) {
        showProgress(true);
        setStatus("جاري حفظ: " + path + "...", "stg-status-msg");
        AppConfigApiClient.updateAsync(path, value).thenAccept(r ->
                Platform.runLater(() -> {
                    showProgress(false);
                    if (r.isSuccess()) {
                        jsonPendingChanges.remove(path);
                        updateFooter();
                        flashCard(card, true);
                        setStatus("✓ تم حفظ: " + path, "stg-status-msg-ok");
                    } else {
                        flashCard(card, false);
                        setStatus("❌ " + r.getMessage(), "stg-status-msg-error");
                    }
                })
        );
    }

    private void confirmJsonDelete(String path) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("حذف مفتاح");
        alert.setHeaderText("حذف: " + path);
        alert.setContentText("سيتم عمل نسخة احتياطية تلقائياً. هل أنت متأكد؟");
        styleAlert(alert);
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                showProgress(true);
                AppConfigApiClient.deleteAsync(path).thenAccept(r ->
                        Platform.runLater(() -> {
                            showProgress(false);
                            if (r.isSuccess()) {
                                loadJsonData();
                                setStatus("✓ تم حذف: " + path, "stg-status-msg-ok");
                            } else setStatus("❌ " + r.getMessage(), "stg-status-msg-error");
                        })
                );
            }
        });
    }

    // ══════════════════════════════════════════════════════════════
    //  Dialogs — Properties
    // ══════════════════════════════════════════════════════════════
    private void showAddDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("إضافة إعداد جديد");
        dialog.setHeaderText(null);
        styleDialog(dialog);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.getStyleClass().add("stg-dialog-content");

        TextField keyField = styledField("مثال: app.my.new.setting");
        TextField valueField = styledField("مثال: ${MY_ENV:defaultValue}");
        TextField commentField = styledField("وصف اختياري");

        ComboBox<String> catBox = new ComboBox<>(
                FXCollections.observableArrayList(groupedData.keySet()));
        catBox.getStyleClass().add("stg-combo-light");
        catBox.setPrefWidth(280);
        if (!groupedData.isEmpty()) catBox.setValue(groupedData.keySet().iterator().next());

        Label hintLbl = new Label(
                "نمط Docker:  ${ENV_VAR:default}   أو   ${ENV_VAR:${other.key}/suffix}");
        hintLbl.getStyleClass().add("stg-dialog-hint");

        addGridRow(grid, "المفتاح *", keyField, 0);
        addGridRow(grid, "القيمة *", valueField, 1);
        addGridRow(grid, "التصنيف", catBox, 2);
        addGridRow(grid, "تعليق", commentField, 3);
        grid.add(hintLbl, 0, 4, 2, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setText("إضافة");
        okBtn.getStyleClass().add("stg-btn-dialog-primary");
        okBtn.disableProperty().bind(keyField.textProperty().isEmpty()
                .or(valueField.textProperty().isEmpty()));

        dialog.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                showProgress(true);
                SettingsApiClient.addAsync(keyField.getText().trim(),
                        valueField.getText().trim(), catBox.getValue(),
                        commentField.getText().trim()
                ).thenAccept(r -> Platform.runLater(() -> {
                    showProgress(false);
                    if (r.isSuccess()) {
                        loadData();
                        setStatus("✓ تمت إضافة: " + keyField.getText().trim(), "stg-status-msg-ok");
                    } else setStatus("❌ " + r.getMessage(), "stg-status-msg-error");
                }));
            }
        });
    }

    private void showBackupsDialog() {
        showProgress(true);
        SettingsApiClient.listBackupsAsync().thenAccept(r -> Platform.runLater(() -> {
            showProgress(false);
            if (!r.isSuccess()) {
                setStatus("❌ " + r.getMessage(), "stg-status-msg-error");
                return;
            }

            List<String> backups = r.getData() != null ? r.getData() : List.of();
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("النسخ الاحتياطية — Properties");
            dialog.setHeaderText(null);
            styleDialog(dialog);

            ListView<String> list = new ListView<>(FXCollections.observableArrayList(backups));
            list.setPrefHeight(280);
            list.setPrefWidth(420);
            list.getStyleClass().add("stg-backups-list-view");

            Button restoreBtn = new Button("↩  استعادة المحدد");
            restoreBtn.getStyleClass().add("stg-btn-dialog-primary");
            restoreBtn.disableProperty().bind(
                    list.getSelectionModel().selectedItemProperty().isNull());

            Label titleLbl = new Label("النسخ المتاحة (" + backups.size() + "):");
            titleLbl.getStyleClass().add("stg-dialog-title");

            VBox content = new VBox(10, titleLbl, list, restoreBtn);
            content.setPadding(new Insets(10));
            content.getStyleClass().add("stg-dialog-content");

            restoreBtn.setOnAction(e -> {
                String sel = list.getSelectionModel().getSelectedItem();
                if (sel != null) {
                    dialog.close();
                    confirmRestore(sel);
                }
            });

            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialog.showAndWait();
        }));
    }

    private void confirmRestore(String backupFileName) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("استعادة نسخة احتياطية");
        alert.setHeaderText("استعادة: " + backupFileName);
        alert.setContentText("ستُستبدل الإعدادات الحالية بالكامل. هل أنت متأكد؟");
        styleAlert(alert);
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                showProgress(true);
                SettingsApiClient.restoreAsync(backupFileName).thenAccept(r ->
                        Platform.runLater(() -> {
                            showProgress(false);
                            if (r.isSuccess()) {
                                loadData();
                                setStatus("✓ تمت الاستعادة من: " + backupFileName, "stg-status-msg-ok");
                            } else setStatus("❌ " + r.getMessage(), "stg-status-msg-error");
                        })
                );
            }
        });
    }

    // ══════════════════════════════════════════════════════════════
    //  Dialogs — JSON
    // ══════════════════════════════════════════════════════════════
    private void showJsonAddDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("إضافة مفتاح JSON جديد");
        dialog.setHeaderText(null);
        styleDialog(dialog);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.getStyleClass().add("stg-dialog-content");

        TextField pathField = styledField("setting.myNewKey");
        TextField valueField = styledField("القيمة");

        ComboBox<String> typeBox = new ComboBox<>(
                FXCollections.observableArrayList("STRING", "INT", "DOUBLE", "BOOLEAN"));
        typeBox.setValue("STRING");
        typeBox.getStyleClass().add("stg-combo-light");
        typeBox.setPrefWidth(280);

        Label hintLbl = new Label(
                "المسار: section.subSection.key — الأقسام المتداخلة تُنشأ تلقائياً");
        hintLbl.getStyleClass().add("stg-dialog-hint");

        addGridRow(grid, "المسار *", pathField, 0);
        addGridRow(grid, "النوع *", typeBox, 1);
        addGridRow(grid, "القيمة *", valueField, 2);
        grid.add(hintLbl, 0, 3, 2, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setText("إضافة");
        okBtn.getStyleClass().add("stg-btn-dialog-primary");
        okBtn.disableProperty().bind(pathField.textProperty().isEmpty()
                .or(valueField.textProperty().isEmpty()));

        dialog.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                showProgress(true);
                AppConfigApiClient.addAsync(
                        pathField.getText().trim(),
                        valueField.getText().trim(),
                        typeBox.getValue()
                ).thenAccept(r -> Platform.runLater(() -> {
                    showProgress(false);
                    if (r.isSuccess()) {
                        loadJsonData();
                        setStatus("✓ تمت إضافة: " + pathField.getText().trim(), "stg-status-msg-ok");
                    } else setStatus("❌ " + r.getMessage(), "stg-status-msg-error");
                }));
            }
        });
    }

    private void showJsonBackupsDialog() {
        showProgress(true);
        AppConfigApiClient.listBackupsAsync().thenAccept(r -> Platform.runLater(() -> {
            showProgress(false);
            if (!r.isSuccess()) {
                setStatus("❌ " + r.getMessage(), "stg-status-msg-error");
                return;
            }

            List<String> backups = r.getData() != null ? r.getData() : List.of();
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("النسخ الاحتياطية — App Config");
            dialog.setHeaderText(null);
            styleDialog(dialog);

            ListView<String> list = new ListView<>(FXCollections.observableArrayList(backups));
            list.setPrefHeight(280);
            list.setPrefWidth(420);
            list.getStyleClass().add("stg-backups-list-view");

            Button restoreBtn = new Button("↩  استعادة المحدد");
            restoreBtn.getStyleClass().add("stg-btn-dialog-primary");
            restoreBtn.disableProperty().bind(
                    list.getSelectionModel().selectedItemProperty().isNull());

            Label titleLbl = new Label("النسخ المتاحة (" + backups.size() + "):");
            titleLbl.getStyleClass().add("stg-dialog-title");

            VBox content = new VBox(10, titleLbl, list, restoreBtn);
            content.setPadding(new Insets(10));
            content.getStyleClass().add("stg-dialog-content");

            restoreBtn.setOnAction(e -> {
                String sel = list.getSelectionModel().getSelectedItem();
                if (sel != null) {
                    dialog.close();
                    confirmJsonRestore(sel);
                }
            });

            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialog.showAndWait();
        }));
    }

    private void confirmJsonRestore(String backupFileName) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("استعادة نسخة احتياطية (JSON)");
        alert.setHeaderText("استعادة: " + backupFileName);
        alert.setContentText("ستُستبدل الإعدادات الحالية بالكامل. هل أنت متأكد؟");
        styleAlert(alert);
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                showProgress(true);
                AppConfigApiClient.restoreAsync(backupFileName).thenAccept(r ->
                        Platform.runLater(() -> {
                            showProgress(false);
                            if (r.isSuccess()) {
                                loadJsonData();
                                setStatus("✓ تمت الاستعادة من: " + backupFileName, "stg-status-msg-ok");
                            } else setStatus("❌ " + r.getMessage(), "stg-status-msg-error");
                        })
                );
            }
        });
    }

    // ══════════════════════════════════════════════════════════════
    //  State Helpers
    // ══════════════════════════════════════════════════════════════
    private void trackChange(String key, String newValue, VBox card) {
        pendingChanges.put(key, newValue);
        markCardChanged(card);
        updateFooter();
    }

    private void trackJsonChange(String path, String newValue, VBox card) {
        jsonPendingChanges.put(path, newValue);
        markCardChanged(card);
        updateFooter();
    }

    private void markCardChanged(VBox card) {
        if (!card.getStyleClass().contains("stg-card-changed")) {
            card.getStyleClass().add("stg-card-changed");
        }
    }

    private void updateFooter() {
        int idx = mainTabPane.getSelectionModel().getSelectedIndex();
        int n = (idx == 0) ? pendingChanges.size() : jsonPendingChanges.size();

        btnSaveAll.setDisable(n == 0);
        btnSaveAll.setText(n > 0 ? "💾  حفظ الكل (" + n + ")" : "💾  حفظ الكل");
        btnDiscard.setVisible(n > 0);
        pendingBadge.setVisible(n > 0);
        pendingBadge.setText(n + " تغيير غير محفوظ");
        if (n == 0) setStatus("جاهز", "stg-status-label");
    }

    private void showLoading(boolean show) {
        loadingPlaceholder.setVisible(show);
        loadingPlaceholder.setManaged(show);
        showProgress(show);
    }

    private void showJsonLoading(boolean show) {
        jsonLoadingPlaceholder.setVisible(show);
        jsonLoadingPlaceholder.setManaged(show);
        showProgress(show);
    }

    private void showProgress(boolean show) {
        Platform.runLater(() -> progressIndicator.setVisible(show));
    }

    /**
     * يحدّث نص statusLabel + يغيّر الـ class حسب الحالة.
     *
     * @param cssClass واحد من: stg-status-label / stg-status-msg-ok / stg-status-msg-error / stg-status-msg-warn
     */
    private void setStatus(String msg, String cssClass) {
        Platform.runLater(() -> {
            statusLabel.setText(msg);
            statusLabel.getStyleClass().removeAll(
                    "stg-status-label", "stg-status-msg-ok",
                    "stg-status-msg-error", "stg-status-msg-warn");
            statusLabel.getStyleClass().add(cssClass);
        });
    }

    private void flashCard(VBox card, boolean success) {
        String flashClass = success ? "stg-card-flash-ok" : "stg-card-flash-error";
        card.getStyleClass().add(flashClass);
        new Thread(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignored) {
            }
            Platform.runLater(() -> {
                card.getStyleClass().remove(flashClass);
                card.getStyleClass().remove("stg-card-changed");
            });
        }).start();
    }

    private boolean matchesSearch(PropertyEntry e, String query) {
        return (e.key() != null && e.key().toLowerCase().contains(query))
                || (e.rawValue() != null && e.rawValue().toLowerCase().contains(query))
                || (e.envVarName() != null && e.envVarName().toLowerCase().contains(query))
                || (e.comment() != null && e.comment().toLowerCase().contains(query))
                || (e.defaultValue() != null && e.defaultValue().toLowerCase().contains(query));
    }

    private boolean matchesJsonSearch(JsonEntry e, String query) {
        return (e.key() != null && e.key().toLowerCase().contains(query))
                || (e.path() != null && e.path().toLowerCase().contains(query))
                || (e.value() != null && e.value().toLowerCase().contains(query))
                || (e.type() != null && e.type().toLowerCase().contains(query));
    }

    // ══════════════════════════════════════════════════════════════
    //  UI Helpers
    // ══════════════════════════════════════════════════════════════

    /**
     * Badge منسّق بالكامل عبر stg-badge-* classes.
     */
    private Label badge(String text, String cssClass) {
        Label lbl = new Label(text);
        lbl.getStyleClass().add("stg-badge");
        lbl.getStyleClass().add(cssClass);
        return lbl;
    }

    /**
     * زر أيقوني — يستخدم stg-icon-btn-base + class إضافي حسب النوع.
     */
    private Button iconBtn(String icon, String cssClass, String tip) {
        Button btn = new Button(icon);
        btn.setTooltip(new Tooltip(tip));
        btn.getStyleClass().add("stg-icon-btn-base");
        btn.getStyleClass().add(cssClass);
        return btn;
    }

    private TextField styledField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setPrefWidth(280);
        tf.getStyleClass().add("stg-field-dark");
        return tf;
    }

    private void addGridRow(GridPane grid, String labelText, Node field, int row) {
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("stg-field-label");
        lbl.setMinWidth(70);
        grid.add(lbl, 0, row);
        grid.add(field, 1, row);
    }

    /**
     * ينسّق Alert داكن + يحمّل settings.css.
     */
    private void styleAlert(Dialog<?> d) {
        d.getDialogPane().getStyleClass().add("stg-dialog");
        SettingsThemeLoader.apply(d.getDialogPane());
    }

    /**
     * نفس styleAlert لكن يضيف headerText = null في الـ dialog.
     */
    private void styleDialog(Dialog<?> d) {
        styleAlert(d);
    }

    private String categoryIcon(String cat) {
        if (cat == null) return "⚙️";
        if (cat.contains("بيانات") || cat.contains("Data")) return "🗄️";
        if (cat.contains("JPA") || cat.contains("Hibernate")) return "🔗";
        if (cat.contains("Tomcat") || cat.contains("خادم")) return "🌐";
        if (cat.contains("JWT") || cat.contains("أمان")) return "🔐";
        if (cat.contains("تخزين") || cat.contains("Storage")) return "📁";
        if (cat.contains("Log") || cat.contains("سجل")) return "📋";
        if (cat.contains("Actuator")) return "📊";
        if (cat.contains("setting")) return "🧩";
        return "⚙️";
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
}