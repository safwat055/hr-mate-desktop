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
 * ══════════════════════════════════════════════════════════════════
 * SettingsController — شاشة إعدادات التطبيق
 * ══════════════════════════════════════════════════════════════════
 * <p>
 * تحتوي على تابين:
 * 1) Application Properties  → /api/settings
 * 2) App Config (JSON)       → /api/app-config
 */
public class SettingsController implements Initializable {

    // ══════════════════════════════════════════════
    //  FXML Fields — Header
    // ══════════════════════════════════════════════
    @FXML
    private Label headerSubtitle;
    @FXML
    private TextField searchField;
    @FXML
    private Button btnRefresh;
    @FXML
    private Button btnBackup;
    @FXML
    private Button btnAdd;
    @FXML
    private Button btnBackups;

    // ══════════════════════════════════════════════
    //  FXML Fields — Tabs
    // ══════════════════════════════════════════════
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

    // ══════════════════════════════════════════════
    //  FXML Fields — Footer
    // ══════════════════════════════════════════════
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

    // ══════════════════════════════════════════════
    //  State — Properties
    // ══════════════════════════════════════════════
    private Map<String, List<PropertyEntry>> groupedData = new LinkedHashMap<>();
    private String currentCategory = null;
    private final Map<String, String> pendingChanges = new LinkedHashMap<>();

    // ══════════════════════════════════════════════
    //  State — JSON
    // ══════════════════════════════════════════════
    private Map<String, List<JsonEntry>> jsonGroupedData = new LinkedHashMap<>();
    private String currentJsonCategory = null;
    private final Map<String, String> jsonPendingChanges = new LinkedHashMap<>();

    // ══════════════════════════════════════════════
    //  Constants — Colors
    // ══════════════════════════════════════════════
    private static final String C_BG = "#1a1d2e";
    private static final String C_CARD = "#242740";
    private static final String C_ACCENT = "#4f8ef7";
    private static final String C_GREEN = "#43c59e";
    private static final String C_WARN = "#f5a623";
    private static final String C_DANGER = "#e05c5c";
    private static final String C_TEXT = "#e8eaf6";
    private static final String C_MUTED = "#8b90b8";
    private static final String C_BORDER = "#333659";
    private static final String C_ENV = "#1a3a6e";
    private static final String C_NESTED = "#2d1f4a";

    // ══════════════════════════════════════════════
    //  Init
    // ══════════════════════════════════════════════
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupCategoryList(categoryList);
        setupCategoryList(jsonCategoryList);
        SettingsThemeLoader.apply(btnAdd);
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
                String base = "-fx-font-size:12px; -fx-padding:8 12 8 12;"
                        + "-fx-text-fill:" + C_TEXT + "; -fx-background-color:transparent;";
                if (isSelected())
                    setStyle(base + "-fx-background-color:" + C_ACCENT + "22; -fx-font-weight:bold;");
                else setStyle(base);
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
        setStatus("جاري تحميل الإعدادات...", C_MUTED);

        SettingsApiClient.getGroupedAsync().thenAccept(response ->
                Platform.runLater(() -> {
                    showLoading(false);
                    if (response.isSuccess() && response.getData() != null) {
                        groupedData = new LinkedHashMap<>(response.getData());
                        categoryList.setItems(FXCollections.observableArrayList(groupedData.keySet()));
                        int total = groupedData.values().stream().mapToInt(List::size).sum();
                        setStatus("تم تحميل " + total + " إعداد ✓", C_GREEN);
                        if (!groupedData.isEmpty())
                            categoryList.getSelectionModel().select(
                                    groupedData.keySet().iterator().next());
                    } else {
                        setStatus("❌ فشل التحميل: " + response.getMessage(), C_DANGER);
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
            empty.setStyle("-fx-text-fill:" + C_MUTED + "; -fx-font-size:13px;");
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
        card.setStyle(cardStyle(false));

        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label keyLbl = new Label(entry.key());
        keyLbl.setStyle("-fx-font-family:monospace; -fx-font-size:12px;"
                + "-fx-font-weight:bold; -fx-text-fill:" + C_ACCENT + ";"
                + "-fx-background-color:#1a2540; -fx-background-radius:4;"
                + "-fx-padding:2 7 2 7;");
        topRow.getChildren().add(keyLbl);

        if (entry.envBound() && entry.envVarName() != null)
            topRow.getChildren().add(badge("🐳 ENV: " + entry.envVarName(), "#7eb3ff", C_ENV));
        if (entry.nested())
            topRow.getChildren().add(badge("🔗 nested", "#c4a6ff", C_NESTED));
        if (!entry.editable())
            topRow.getChildren().add(badge("🔒 محمي", C_MUTED, "#1e2030"));
        if (entry.sensitive())
            topRow.getChildren().add(badge("🔑 حساس", C_WARN, "#2a2000"));

        card.getChildren().add(topRow);

        if (entry.comment() != null && !entry.comment().isBlank()) {
            Label commentLbl = new Label("# " + entry.comment());
            commentLbl.setStyle("-fx-font-size:11px; -fx-text-fill:" + C_MUTED + ";");
            card.getChildren().add(commentLbl);
        }

        if (entry.envBound()) {
            String activeEnvVal = System.getenv(entry.envVarName());
            VBox envInfo = new VBox(3);
            if (activeEnvVal != null) {
                Label activeLabel = new Label(
                        "🟢 متغير البيئة نشط: " + entry.envVarName() + " = " + activeEnvVal);
                activeLabel.setStyle("-fx-font-size:10px; -fx-text-fill:#43c59e;");
                Label infoLabel = new Label("التعديل هنا لن يؤثر — متغير البيئة له الأولوية");
                infoLabel.setStyle("-fx-font-size:10px; -fx-text-fill:" + C_MUTED + ";");
                envInfo.getChildren().addAll(activeLabel, infoLabel);
            } else {
                Label warnLbl = new Label("⚙ Fallback نشط — تعديل القيمة يغيّر الـ Default في الملف");
                warnLbl.setStyle("-fx-font-size:10px; -fx-text-fill:" + C_WARN + ";");
                Label varName = new Label("متغير البيئة: " + entry.envVarName() + " (غير مضبوط)");
                varName.setStyle("-fx-font-size:10px; -fx-text-fill:" + C_MUTED + ";");
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
            tf.setStyle(inputStyle() + "-fx-opacity:0.55;");
            HBox.setHgrow(tf, Priority.ALWAYS);
            inputCtrl = tf;
        } else if (entry.sensitive()) {
            PasswordField pf = new PasswordField();
            pf.setText(editableValue);
            pf.setStyle(inputStyle());
            HBox.setHgrow(pf, Priority.ALWAYS);
            pf.textProperty().addListener((obs, o, n) -> trackChange(entry.key(), n, card));
            inputCtrl = pf;
        } else {
            TextField tf = new TextField(editableValue);
            tf.setStyle(inputStyle());
            HBox.setHgrow(tf, Priority.ALWAYS);
            tf.textProperty().addListener((obs, o, n) -> trackChange(entry.key(), n, card));
            inputCtrl = tf;
        }
        valueRow.getChildren().add(inputCtrl);

        if (entry.editable()) {
            Button saveBtn = iconBtn("💾", C_GREEN, "حفظ هذا الإعداد");
            saveBtn.setOnAction(e -> {
                String val = inputCtrl instanceof PasswordField
                        ? ((PasswordField) inputCtrl).getText()
                        : ((TextField) inputCtrl).getText();
                saveSingle(entry.key(), val, card);
            });
            Button delBtn = iconBtn("🗑", C_DANGER, "حذف هذا الإعداد");
            delBtn.setOnAction(e -> confirmDelete(entry.key()));
            valueRow.getChildren().addAll(saveBtn, delBtn);
        }
        card.getChildren().add(valueRow);

        if (entry.rawValue() != null && entry.envBound()) {
            Label rawLbl = new Label("في الملف: " + entry.rawValue());
            rawLbl.setStyle("-fx-font-size:10px; -fx-font-family:monospace;"
                    + "-fx-text-fill:#444870; -fx-padding:0 0 0 2;");
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
                        setStatus("تم تحميل " + total + " مفتاح JSON ✓", C_GREEN);
                        if (!jsonGroupedData.isEmpty())
                            jsonCategoryList.getSelectionModel().select(
                                    jsonGroupedData.keySet().iterator().next());
                    } else {
                        setStatus("❌ فشل تحميل JSON: " + response.getMessage(), C_DANGER);
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
            empty.setStyle("-fx-text-fill:" + C_MUTED + "; -fx-font-size:13px;");
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
        card.setStyle(cardStyle(false));

        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label keyLbl = new Label(entry.key());
        keyLbl.setStyle("-fx-font-family:monospace; -fx-font-size:12px;"
                + "-fx-font-weight:bold; -fx-text-fill:" + C_ACCENT + ";"
                + "-fx-background-color:#1a2540; -fx-background-radius:4;"
                + "-fx-padding:2 7 2 7;");
        topRow.getChildren().add(keyLbl);

        topRow.getChildren().add(badge(entry.type(),
                typeColor(entry.type()), typeBg(entry.type())));

        if (!entry.editable())
            topRow.getChildren().add(badge("🔒 محمي", C_MUTED, "#1e2030"));

        card.getChildren().add(topRow);

        Label pathLbl = new Label(entry.path());
        pathLbl.setStyle("-fx-font-size:10px; -fx-font-family:monospace;"
                + "-fx-text-fill:" + C_MUTED + ";");
        card.getChildren().add(pathLbl);

        if (!entry.editable()) return card;

        HBox valueRow = new HBox(8);
        valueRow.setAlignment(Pos.CENTER_LEFT);

        Control inputCtrl;
        if ("BOOLEAN".equals(entry.type())) {
            ComboBox<String> cb = new ComboBox<>(
                    FXCollections.observableArrayList("true", "false"));
            cb.setValue(entry.value() != null ? entry.value() : "false");
            cb.getStyleClass().add("combo-light");      // ✅ CSS class
            cb.setPrefWidth(140);
            cb.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(cb, Priority.ALWAYS);
            cb.valueProperty().addListener((obs, o, n) ->
                    trackJsonChange(entry.path(), n, card));
            inputCtrl = cb;
        } else {
            TextField tf = new TextField(entry.value() != null ? entry.value() : "");
            tf.setStyle(inputStyle());
            HBox.setHgrow(tf, Priority.ALWAYS);
            tf.textProperty().addListener((obs, o, n) ->
                    trackJsonChange(entry.path(), n, card));
            inputCtrl = tf;
        }
        valueRow.getChildren().add(inputCtrl);

        Button saveBtn = iconBtn("💾", C_GREEN, "حفظ هذا المفتاح");
        saveBtn.setOnAction(e -> {
            String val = inputCtrl instanceof ComboBox
                    ? String.valueOf(((ComboBox<?>) inputCtrl).getValue())
                    : ((TextField) inputCtrl).getText();
            saveJsonSingle(entry.path(), val, card);
        });

        Button delBtn = iconBtn("🗑", C_DANGER, "حذف هذا المفتاح");
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
        setStatus("جاري عمل نسخة احتياطية...", C_MUTED);
        showProgress(true);

        if (idx == 0) {
            SettingsApiClient.backupAsync().thenAccept(r -> Platform.runLater(() -> {
                showProgress(false);
                setStatus(r.isSuccess() ? "✓ تم عمل نسخة احتياطية" : "❌ " + r.getMessage(),
                        r.isSuccess() ? C_GREEN : C_DANGER);
            }));
        } else {
            AppConfigApiClient.backupAsync().thenAccept(r -> Platform.runLater(() -> {
                showProgress(false);
                setStatus(r.isSuccess() ? "✓ تم عمل نسخة احتياطية (JSON)" : "❌ " + r.getMessage(),
                        r.isSuccess() ? C_GREEN : C_DANGER);
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
        setStatus("تم تجاهل التغييرات", C_WARN);
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
        setStatus("جاري حفظ " + pendingChanges.size() + " إعداد...", C_MUTED);

        SettingsApiClient.batchUpdateAsync(new LinkedHashMap<>(pendingChanges))
                .thenAccept(r -> Platform.runLater(() -> {
                    showProgress(false);
                    if (r.isSuccess()) {
                        pendingChanges.clear();
                        updateFooter();
                        loadData();
                        setStatus("✓ تم حفظ كل التغييرات", C_GREEN);
                    } else {
                        btnSaveAll.setDisable(false);
                        setStatus("❌ فشل الحفظ: " + r.getMessage(), C_DANGER);
                    }
                }));
    }

    private void saveAllJson() {
        if (jsonPendingChanges.isEmpty()) return;
        showProgress(true);
        btnSaveAll.setDisable(true);
        setStatus("جاري حفظ " + jsonPendingChanges.size() + " مفتاح...", C_MUTED);

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
                setStatus("✓ تم حفظ " + okCount.get() + " مفتاح", C_GREEN);
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
        setStatus("جاري حفظ: " + key + "...", C_MUTED);
        SettingsApiClient.updateAsync(key, value).thenAccept(r ->
                Platform.runLater(() -> {
                    showProgress(false);
                    if (r.isSuccess()) {
                        pendingChanges.remove(key);
                        updateFooter();
                        flashCard(card, true);
                        setStatus("✓ تم حفظ: " + key, C_GREEN);
                    } else {
                        flashCard(card, false);
                        setStatus("❌ " + r.getMessage(), C_DANGER);
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
                                setStatus("✓ تم حذف: " + key, C_GREEN);
                            } else setStatus("❌ " + r.getMessage(), C_DANGER);
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
        setStatus("جاري حفظ: " + path + "...", C_MUTED);
        AppConfigApiClient.updateAsync(path, value).thenAccept(r ->
                Platform.runLater(() -> {
                    showProgress(false);
                    if (r.isSuccess()) {
                        jsonPendingChanges.remove(path);
                        updateFooter();
                        flashCard(card, true);
                        setStatus("✓ تم حفظ: " + path, C_GREEN);
                    } else {
                        flashCard(card, false);
                        setStatus("❌ " + r.getMessage(), C_DANGER);
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
                                setStatus("✓ تم حذف: " + path, C_GREEN);
                            } else setStatus("❌ " + r.getMessage(), C_DANGER);
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
        dialog.setHeaderText("أدخل بيانات الإعداد الجديد");
        styleAlert(dialog);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.setStyle("-fx-background-color:" + C_CARD + ";");

        TextField keyField = styledField("مثال: app.my.new.setting");
        TextField valueField = styledField("مثال: ${MY_ENV:defaultValue}");
        TextField commentField = styledField("وصف اختياري");

        ComboBox<String> catBox = new ComboBox<>(
                FXCollections.observableArrayList(groupedData.keySet()));
        catBox.getStyleClass().add("combo-light");     // ✅ CSS class
        catBox.setPrefWidth(280);
        if (!groupedData.isEmpty()) catBox.setValue(groupedData.keySet().iterator().next());

        Label hintLbl = new Label(
                "نمط Docker:  ${ENV_VAR:default}   أو   ${ENV_VAR:${other.key}/suffix}");
        hintLbl.setStyle("-fx-font-size:11px; -fx-font-family:monospace; -fx-text-fill:" + C_WARN + ";");

        addGridRow(grid, "المفتاح *", keyField, 0);
        addGridRow(grid, "القيمة *", valueField, 1);
        addGridRow(grid, "التصنيف", catBox, 2);
        addGridRow(grid, "تعليق", commentField, 3);
        grid.add(hintLbl, 0, 4, 2, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setText("إضافة");
        okBtn.setStyle("-fx-background-color:" + C_GREEN + "; -fx-text-fill:white;"
                + "-fx-font-weight:bold; -fx-background-radius:8;");
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
                        setStatus("✓ تمت إضافة: " + keyField.getText().trim(), C_GREEN);
                    } else setStatus("❌ " + r.getMessage(), C_DANGER);
                }));
            }
        });
    }

    private void showBackupsDialog() {
        showProgress(true);
        SettingsApiClient.listBackupsAsync().thenAccept(r -> Platform.runLater(() -> {
            showProgress(false);
            if (!r.isSuccess()) {
                setStatus("❌ " + r.getMessage(), C_DANGER);
                return;
            }

            List<String> backups = r.getData() != null ? r.getData() : List.of();
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("النسخ الاحتياطية — Properties");
            dialog.setHeaderText("اختر نسخة لاستعادتها");
            styleAlert(dialog);

            ListView<String> list = new ListView<>(FXCollections.observableArrayList(backups));
            list.setPrefHeight(280);
            list.setPrefWidth(420);
            list.setStyle("-fx-background-color:" + C_BG + "; -fx-border-color:" + C_BORDER + ";");

            Button restoreBtn = new Button("↩  استعادة المحدد");
            restoreBtn.setStyle("-fx-background-color:" + C_WARN
                    + "; -fx-text-fill:white; -fx-font-weight:bold;"
                    + "-fx-background-radius:8; -fx-padding:6 14 6 14;");
            restoreBtn.disableProperty().bind(
                    list.getSelectionModel().selectedItemProperty().isNull());

            VBox content = new VBox(10,
                    new Label("النسخ المتاحة (" + backups.size() + "):"), list, restoreBtn);
            content.setPadding(new Insets(10));
            content.setStyle("-fx-background-color:" + C_CARD + ";");
            content.getChildren().get(0).setStyle("-fx-text-fill:" + C_TEXT + "; -fx-font-weight:bold;");

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
                                setStatus("✓ تمت الاستعادة من: " + backupFileName, C_GREEN);
                            } else setStatus("❌ " + r.getMessage(), C_DANGER);
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
        dialog.setHeaderText("أدخل بيانات المفتاح");
        styleAlert(dialog);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.setStyle("-fx-background-color:" + C_CARD + ";");

        TextField pathField = styledField("setting.myNewKey");
        TextField valueField = styledField("القيمة");

        ComboBox<String> typeBox = new ComboBox<>(
                FXCollections.observableArrayList("STRING", "INT", "DOUBLE", "BOOLEAN"));
        typeBox.setValue("STRING");
        typeBox.getStyleClass().add("combo-light");    // ✅ CSS class
        typeBox.setPrefWidth(280);

        Label hintLbl = new Label(
                "المسار: section.subSection.key — الأقسام المتداخلة تُنشأ تلقائياً");
        hintLbl.setStyle("-fx-font-size:11px; -fx-text-fill:" + C_WARN + ";");

        addGridRow(grid, "المسار *", pathField, 0);
        addGridRow(grid, "النوع *", typeBox, 1);
        addGridRow(grid, "القيمة *", valueField, 2);
        grid.add(hintLbl, 0, 3, 2, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setText("إضافة");
        okBtn.setStyle("-fx-background-color:" + C_GREEN + "; -fx-text-fill:white;"
                + "-fx-font-weight:bold; -fx-background-radius:8;");
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
                        setStatus("✓ تمت إضافة: " + pathField.getText().trim(), C_GREEN);
                    } else setStatus("❌ " + r.getMessage(), C_DANGER);
                }));
            }
        });
    }

    private void showJsonBackupsDialog() {

        showProgress(true);
        AppConfigApiClient.listBackupsAsync().thenAccept(r -> Platform.runLater(() -> {
            showProgress(false);
            if (!r.isSuccess()) {
                setStatus("❌ " + r.getMessage(), C_DANGER);
                return;
            }

            List<String> backups = r.getData() != null ? r.getData() : List.of();
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("النسخ الاحتياطية — App Config");
            dialog.setHeaderText("اختر نسخة لاستعادتها");
            styleAlert(dialog);

            ListView<String> list = new ListView<>(FXCollections.observableArrayList(backups));
            list.setPrefHeight(280);
            list.setPrefWidth(420);
            list.setStyle("-fx-background-color:" + C_BG + "; -fx-border-color:" + C_BORDER + ";");

            Button restoreBtn = new Button("↩  استعادة المحدد");
            restoreBtn.setStyle("-fx-background-color:" + C_WARN
                    + "; -fx-text-fill:white; -fx-font-weight:bold;"
                    + "-fx-background-radius:8; -fx-padding:6 14 6 14;");
            restoreBtn.disableProperty().bind(
                    list.getSelectionModel().selectedItemProperty().isNull());

            VBox content = new VBox(10,
                    new Label("النسخ المتاحة (" + backups.size() + "):"), list, restoreBtn);
            content.setPadding(new Insets(10));
            content.setStyle("-fx-background-color:" + C_CARD + ";");
            content.getChildren().get(0).setStyle("-fx-text-fill:" + C_TEXT + "; -fx-font-weight:bold;");

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
                                setStatus("✓ تمت الاستعادة من: " + backupFileName, C_GREEN);
                            } else setStatus("❌ " + r.getMessage(), C_DANGER);
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
        card.setStyle(cardStyle(true));
        updateFooter();
    }

    private void trackJsonChange(String path, String newValue, VBox card) {
        jsonPendingChanges.put(path, newValue);
        card.setStyle(cardStyle(true));
        updateFooter();
    }

    private void updateFooter() {
        int idx = mainTabPane.getSelectionModel().getSelectedIndex();
        int n = (idx == 0) ? pendingChanges.size() : jsonPendingChanges.size();

        btnSaveAll.setDisable(n == 0);
        btnSaveAll.setText(n > 0 ? "💾  حفظ الكل (" + n + ")" : "💾  حفظ الكل");
        btnDiscard.setVisible(n > 0);
        pendingBadge.setVisible(n > 0);
        pendingBadge.setText(n + " تغيير غير محفوظ");
        if (n == 0) setStatus("جاهز", C_MUTED);
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

    private void setStatus(String msg, String color) {
        Platform.runLater(() -> {
            statusLabel.setText(msg);
            statusLabel.setStyle("-fx-text-fill:" + color + "; -fx-font-size:12px;");
        });
    }

    private void flashCard(VBox card, boolean success) {
        String color = success ? C_GREEN : C_DANGER;
        card.setStyle(cardStyle(false) + "-fx-border-color:" + color + "; -fx-border-width:1.5;");
        new Thread(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignored) {
            }
            Platform.runLater(() -> card.setStyle(cardStyle(false)));
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
    private Label badge(String text, String textColor, String bg) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size:10px; -fx-font-weight:bold;"
                + "-fx-text-fill:" + textColor + ";"
                + "-fx-background-color:" + bg + ";"
                + "-fx-background-radius:4; -fx-padding:2 6 2 6;");
        return lbl;
    }

    private Button iconBtn(String icon, String color, String tip) {
        Button btn = new Button(icon);
        btn.setTooltip(new Tooltip(tip));
        String normal = "-fx-background-color:transparent; -fx-text-fill:" + color
                + "; -fx-font-size:14; -fx-cursor:hand; -fx-padding:4 8 4 8; -fx-background-radius:6;";
        String hover = "-fx-background-color:" + color + "22; -fx-text-fill:" + color
                + "; -fx-font-size:14; -fx-cursor:hand; -fx-padding:4 8 4 8; -fx-background-radius:6;";
        btn.setStyle(normal);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(normal));
        return btn;
    }

    private TextField styledField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setPrefWidth(280);
        tf.setStyle(inputStyle());
        return tf;
    }

    private void addGridRow(GridPane grid, String labelText, Node field, int row) {
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-text-fill:" + C_TEXT + "; -fx-font-weight:bold;"
                + "-fx-font-size:12px; -fx-min-width:70;");
        grid.add(lbl, 0, row);
        grid.add(field, 1, row);
    }

    private void styleAlert(Dialog<?> d) {
        d.getDialogPane().setStyle("-fx-background-color:" + C_CARD
                + "; -fx-font-family:System; -fx-font-size:13px;");
    }

    private String cardStyle(boolean changed) {
        return "-fx-background-color:" + (changed ? C_ACCENT + "11" : C_CARD) + ";"
                + "-fx-background-radius:8; -fx-border-color:" + (changed ? C_ACCENT : C_BORDER)
                + "; -fx-border-radius:8; -fx-border-width:1;";
    }

    private String inputStyle() {
        return "-fx-background-color:" + C_BG + "; -fx-text-fill:" + C_TEXT + ";"
                + "-fx-prompt-text-fill:" + C_MUTED + "; -fx-border-color:" + C_BORDER + ";"
                + "-fx-border-radius:6; -fx-background-radius:6; -fx-padding:6 10 6 10;";
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

    private String typeColor(String type) {
        return switch (type) {
            case "BOOLEAN" -> "#43c59e";
            case "INT" -> "#f5a623";
            case "DOUBLE" -> "#9b59b6";
            case "STRING" -> "#7eb3ff";
            case "ARRAY" -> "#e05c5c";
            case "OBJECT" -> "#8b90b8";
            default -> C_TEXT;
        };
    }

    private String typeBg(String type) {
        return switch (type) {
            case "BOOLEAN" -> "#0d2e22";
            case "INT" -> "#2a2000";
            case "DOUBLE" -> "#2d1f4a";
            case "STRING" -> "#1a2540";
            case "ARRAY" -> "#2a1414";
            case "OBJECT" -> "#1e2030";
            default -> C_CARD;
        };
    }

    private String safeId(String key) {
        return key == null ? "unknown" : key.replace(".", "-");
    }
}