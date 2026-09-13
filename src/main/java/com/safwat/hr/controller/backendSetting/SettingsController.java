package com.safwat.hr.controller.backendSetting;

import com.safwat.hr.controller.backendSetting.SettingsApiClient.PropertyEntry;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class SettingsController implements Initializable {

    @FXML
    private TextField searchField;
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
    @FXML
    private Button btnRefresh;
    @FXML
    private Button btnBackup;
    @FXML
    private Button btnAdd;
    @FXML
    private Button btnBackups;
    @FXML
    private Label filePathLabel;   // اختياري — يعرض مسار الملف

    private Map<String, List<PropertyEntry>> groupedData = new LinkedHashMap<>();
    private String currentCategory = null;
    private final Map<String, String> pendingChanges = new LinkedHashMap<>();

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

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupCategoryList();
        loadData();
        loadFileInfo();
    }

    // ══════════════════════════════════════════════
    //  Setup
    // ══════════════════════════════════════════════

    private void setupCategoryList() {
        categoryList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);

                    setStyle("");
                    return;
                }
                setText(categoryIcon(item) + "  " + item);
                setStyle("-fx-font-size:12px; -fx-padding:8 12 8 12;"
                        + "-fx-text-fill:" + C_TEXT + "; -fx-background-color:transparent;");
                if (isSelected())
                    setStyle(getStyle() + "-fx-background-color:" + C_ACCENT + "22;"
                            + "-fx-font-weight:bold;");
            }
        });
        categoryList.getSelectionModel().selectedItemProperty()
                .addListener((obs, o, n) -> {
                    if (n != null) showCategory(n);
                });
    }

    // ══════════════════════════════════════════════
    //  Data Loading
    // ══════════════════════════════════════════════

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

    private void loadFileInfo() {
        SettingsApiClient.getFileInfoAsync().thenAccept(r -> Platform.runLater(() -> {
            if (r.isSuccess() && r.getData() != null && filePathLabel != null) {
                Object path = r.getData().get("currentPath");
                filePathLabel.setText(path != null ? path.toString() : "");
                filePathLabel.setStyle("-fx-font-size:10px; -fx-text-fill:" + C_MUTED
                        + "; -fx-font-family:monospace;");
            }
        }));
    }

    // ══════════════════════════════════════════════
    //  Category Display
    // ══════════════════════════════════════════════

    private void showCategory(String category) {
        currentCategory = category;
        currentCategoryLabel.setText(categoryIcon(category) + "  " + category);

        List<PropertyEntry> entries = groupedData.getOrDefault(category, List.of());
        entryCountLabel.setText(entries.size() + " إعداد");

        String query = searchField.getText();
        List<PropertyEntry> filtered = query == null || query.isBlank()
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

    // ══════════════════════════════════════════════
    //  Entry Card
    // ══════════════════════════════════════════════

    private Node buildEntryCard(PropertyEntry entry) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(10, 14, 10, 14));
        card.setStyle(cardStyle(false));
        card.setId("card-" + safeId(entry.key()));

        // ─── Key + badges ───
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

        // ─── Comment ───
        if (entry.comment() != null && !entry.comment().isBlank()) {
            Label commentLbl = new Label("# " + entry.comment());
            commentLbl.setStyle("-fx-font-size:11px; -fx-text-fill:" + C_MUTED + ";");
            card.getChildren().add(commentLbl);
        }

        // ─── ENV info + input ───
        if (entry.envBound()) {
            // الـ ENV الفعلي اللي شغال دلوقتي
            String activeEnvVal = System.getenv(entry.envVarName());

            VBox envInfo = new VBox(3);

            if (activeEnvVal != null) {
                // متغير البيئة موجود وشغال — الـ default مش بيتأثر
                Label activeLabel = new Label(
                        "🟢 متغير البيئة نشط: " + entry.envVarName() + " = " + activeEnvVal);
                activeLabel.setStyle("-fx-font-size:10px; -fx-text-fill:#43c59e;");
                Label infoLabel = new Label("التعديل هنا لن يؤثر — متغير البيئة له الأولوية");
                infoLabel.setStyle("-fx-font-size:10px; -fx-text-fill:" + C_MUTED + ";");
                envInfo.getChildren().addAll(activeLabel, infoLabel);
            } else {
                // متغير البيئة غير موجود — الـ fallback هو اللي بيشتغل
                Label warnLbl = new Label(
                        "⚙ Fallback نشط — تعديل القيمة يغيّر الـ Default في الملف");
                warnLbl.setStyle("-fx-font-size:10px; -fx-text-fill:" + C_WARN + ";");
                Label varName = new Label(
                        "متغير البيئة: " + entry.envVarName() + " (غير مضبوط)");
                varName.setStyle("-fx-font-size:10px; -fx-text-fill:" + C_MUTED + ";");
                envInfo.getChildren().addAll(warnLbl, varName);
            }

            card.getChildren().add(envInfo);
        }

        // ─── Value input ───
        HBox valueRow = new HBox(8);
        valueRow.setAlignment(Pos.CENTER_LEFT);

        /*
         * ✅ المنطق الصح:
         *  - envBound → يعرض الـ defaultValue (اللي المستخدم يعدله)
         *               الـ Backend يعيد بناء ${ENV:newDefault} تلقائياً
         *  - مش envBound → يعرض الـ rawValue مباشرةً
         */
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

        // ─── Raw value hint ───
        if (entry.rawValue() != null && entry.envBound()) {
            Label rawLbl = new Label("في الملف: " + entry.rawValue());
            rawLbl.setStyle("-fx-font-size:10px; -fx-font-family:monospace;"
                    + "-fx-text-fill:#444870; -fx-padding:0 0 0 2;");
            card.getChildren().add(rawLbl);
        }

        return card;
    }

    // ══════════════════════════════════════════════
    //  FXML Actions
    // ══════════════════════════════════════════════

    @FXML
    private void onSearch() {
        if (currentCategory != null) showCategory(currentCategory);
    }

    @FXML
    private void onCategorySelected() {
        String sel = categoryList.getSelectionModel().getSelectedItem();
        if (sel != null) showCategory(sel);
    }

    @FXML
    private void onRefresh() {
        pendingChanges.clear();
        updateFooter();
        loadData();
    }

    @FXML
    private void onAdd() {
        showAddDialog();
    }

    @FXML
    private void onShowBackups() {
        showBackupsDialog();
    }

    @FXML
    private void onBackup() {
        setStatus("جاري عمل نسخة احتياطية...", C_MUTED);
        showProgress(true);
        SettingsApiClient.backupAsync().thenAccept(r -> Platform.runLater(() -> {
            showProgress(false);
            setStatus(r.isSuccess() ? "✓ تم عمل نسخة احتياطية" : "❌ " + r.getMessage(),
                    r.isSuccess() ? C_GREEN : C_DANGER);
        }));
    }

    @FXML
    private void onDiscard() {
        pendingChanges.clear();
        updateFooter();
        if (currentCategory != null) showCategory(currentCategory);
        setStatus("تم تجاهل التغييرات", C_WARN);
    }

    @FXML
    private void onSaveAll() {
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

    // ══════════════════════════════════════════════
    //  Single Save / Delete
    // ══════════════════════════════════════════════

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

    // ══════════════════════════════════════════════
    //  Dialogs
    // ══════════════════════════════════════════════

    private void showAddDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("إضافة إعداد جديد");
        dialog.setHeaderText("أدخل بيانات الإعداد الجديد");
        styleAlert(dialog);

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.setStyle("-fx-background-color:" + C_CARD + ";");

        TextField keyField = styledField("مثال: app.my.new.setting");
        TextField valueField = styledField("مثال: ${MY_ENV:defaultValue}");
        TextField commentField = styledField("وصف اختياري");
        ComboBox<String> catBox = new ComboBox<>(
                FXCollections.observableArrayList(groupedData.keySet()));
        catBox.setStyle(inputStyle() + "-fx-pref-width:260;");
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
            dialog.setTitle("النسخ الاحتياطية");
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

    // ══════════════════════════════════════════════
    //  State Helpers
    // ══════════════════════════════════════════════

    private void trackChange(String key, String newValue, VBox card) {
        pendingChanges.put(key, newValue);
        card.setStyle(cardStyle(true));
        updateFooter();
    }

    private void updateFooter() {
        int n = pendingChanges.size();
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

    // ══════════════════════════════════════════════
    //  UI Helpers
    // ══════════════════════════════════════════════

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
        btn.setStyle("-fx-background-color:transparent; -fx-text-fill:" + color
                + "; -fx-font-size:14; -fx-cursor:hand; -fx-padding:4 8 4 8; -fx-background-radius:6;");
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color:" + color + "22; -fx-text-fill:" + color
                        + "; -fx-font-size:14; -fx-cursor:hand; -fx-padding:4 8 4 8; -fx-background-radius:6;"));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color:transparent; -fx-text-fill:" + color
                        + "; -fx-font-size:14; -fx-cursor:hand; -fx-padding:4 8 4 8; -fx-background-radius:6;"));
        return btn;
    }

    private TextField styledField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setPrefWidth(280);
        tf.setStyle(inputStyle());
        return tf;
    }

    private void addGridRow(javafx.scene.layout.GridPane grid, String labelText, Node field, int row) {
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
        return "⚙️";
    }

    private String safeId(String key) {
        return key == null ? "unknown" : key.replace(".", "-");
    }
}