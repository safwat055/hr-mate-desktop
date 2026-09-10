package com.safwat.hr.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.safwat.hr.backup.BackupFileInfo;
import com.safwat.hr.backup.BackupFormat;
import com.safwat.hr.backup.RestoreMode;
import com.safwat.hr.network.ApiClient;
import com.safwat.hr.network.ApiResponse;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

/**
 * كنترولر شاشة النسخ الاحتياطي.
 */
public class BackupController implements Initializable {

    // ── Header ──
    @FXML
    private Label statusDot;
    @FXML
    private Label statusLabel;

    // ── النسخ الكامل ──
    @FXML
    private RadioButton radioCustomFormat;
    @FXML
    private RadioButton radioPlainFormat;
    @FXML
    private Slider compressionSlider;
    @FXML
    private Label compressionValueLabel;
    @FXML
    private Button btnBackup;
    @FXML
    private ProgressBar backupProgress;
    @FXML
    private Label backupStatusLabel;

    // ── استعادة من ملف مرفوع ──
    @FXML
    private TextField uploadFilePath;
    @FXML
    private RadioButton radioReplace;
    @FXML
    private RadioButton radioMerge;
    @FXML
    private Button btnRestoreFile;
    @FXML
    private ProgressBar restoreFileProgress;
    @FXML
    private Label restoreFileStatusLabel;

    // ── النسخ المحلية ──
    @FXML
    private RadioButton radioLocalReplace;
    @FXML
    private RadioButton radioLocalMerge;
    @FXML
    private Label backupsCountLabel;
    @FXML
    private VBox backupsList;
    @FXML
    private ScrollPane backupsScrollPane;
    @FXML
    private Button btnRestoreSelected;
    @FXML
    private ProgressBar restoreLocalProgress;
    @FXML
    private TextArea restoreLocalStatusArea;

    // ── النسخ التلقائي ──
    @FXML
    private ToggleButton toggleAutoBackup;
    @FXML
    private TextField cronField;
    @FXML
    private Label cronHintLabel;
    @FXML
    private Spinner<Integer> maxFilesSpinner;
    @FXML
    private Label autoBackupStatusLabel;

    // ── State ──
    private final ObservableList<BackupFileInfo> backupsListData = FXCollections.observableArrayList();
    private File selectedUploadFile;
    private BackupFileInfo selectedBackupItem;
    private boolean isFirstLoad = true;  // لتحديد أول مرة يتم تحميل الشاشة

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd  HH:mm:ss");

    // ════════════════════════════════════════════════════
    //  Init
    // ════════════════════════════════════════════════════

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupFormatToggleGroup();
        setupRestoreModeToggleGroup();
        setupLocalRestoreModeToggleGroup();
        setupCompressionSlider();
        setupSpinner();
        cronField.textProperty().addListener((obs, o, n) -> updateCronHint(n));

        // عرض رسالة انتظار بدلاً من تحميل البيانات تلقائياً
        showEmptyState("اضغط على ↻ تحديث لتحميل النسخ المحفوظة");

        // ❌ لا نقوم بتحميل البيانات تلقائياً عند فتح الواجهة
        // handleRefreshBackups();  // تم إلغاؤها
    }

    // ── Toggle Groups ──

    private void setupFormatToggleGroup() {
        ToggleGroup group = new ToggleGroup();
        radioCustomFormat.setToggleGroup(group);
        radioPlainFormat.setToggleGroup(group);
        radioCustomFormat.selectedProperty().addListener((obs, o, sel) ->
                compressionSlider.setDisable(!sel));
    }

    private void setupRestoreModeToggleGroup() {
        ToggleGroup group = new ToggleGroup();
        radioReplace.setToggleGroup(group);
        radioMerge.setToggleGroup(group);
    }

    private void setupLocalRestoreModeToggleGroup() {
        ToggleGroup group = new ToggleGroup();
        radioLocalReplace.setToggleGroup(group);
        radioLocalMerge.setToggleGroup(group);
    }

    private void setupCompressionSlider() {
        compressionSlider.valueProperty().addListener((obs, o, n) -> {
            int val = n.intValue();
            if (val >= 7) {
                compressionValueLabel.setStyle("-fx-text-fill: #2b7a4b;");
                compressionValueLabel.setText(val + " → ضغط عالي - ملف صغير");
            } else if (val <= 3) {
                compressionValueLabel.setStyle("-fx-text-fill: #c0392b;");
                compressionValueLabel.setText(val + " → ضغط منخفض - ملف كبير");
            } else {
                compressionValueLabel.setStyle("-fx-text-fill: #f39c12;");
                compressionValueLabel.setText(val + " → ضغط متوسط");
            }
        });
    }

    private void setupSpinner() {
        SpinnerValueFactory<Integer> factory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 30, 7);
        maxFilesSpinner.setValueFactory(factory);
    }

    // ════════════════════════════════════════════════════
    //  بناء القائمة
    // ════════════════════════════════════════════════════

    /**
     * عرض حالة انتظار (القائمة فارغة)
     */
    private void showEmptyState(String message) {
        Platform.runLater(() -> {
            backupsList.getChildren().clear();
            Label emptyLabel = new Label("📭 " + message);
            emptyLabel.getStyleClass().add("empty-label");
            emptyLabel.setAlignment(Pos.CENTER);
            emptyLabel.setMaxWidth(Double.MAX_VALUE);
            emptyLabel.setPadding(new Insets(20, 0, 20, 0));
            backupsList.getChildren().add(emptyLabel);
            backupsCountLabel.setText("0 نسخة");
            btnRestoreSelected.setDisable(true);
        });
    }

    /**
     * يعيد بناء قائمة النسخ المحلية
     */
    private void rebuildBackupsList() {
        Platform.runLater(() -> {
            backupsList.getChildren().clear();

            if (backupsListData.isEmpty()) {
                Label emptyLabel = new Label("📭 لا توجد نسخ احتياطية محفوظة");
                emptyLabel.getStyleClass().add("empty-label");
                emptyLabel.setAlignment(Pos.CENTER);
                emptyLabel.setMaxWidth(Double.MAX_VALUE);
                emptyLabel.setPadding(new Insets(20, 0, 20, 0));
                backupsList.getChildren().add(emptyLabel);
                return;
            }

            for (BackupFileInfo item : backupsListData) {
                Node itemNode = createBackupItemNode(item);
                backupsList.getChildren().add(itemNode);
            }
        });
    }

    /**
     * ينشئ عنصر (HBox) يمثل نسخة احتياطية واحدة.
     */
    private Node createBackupItemNode(BackupFileInfo item) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setSpacing(10);
        row.setPadding(new Insets(8, 12, 8, 12));
        row.getStyleClass().add("backup-item");
        row.setMaxWidth(Double.MAX_VALUE);

        if (selectedBackupItem != null && selectedBackupItem.getFileName().equals(item.getFileName())) {
            row.getStyleClass().add("backup-item-selected");
        }

        // ── اسم الملف ──
        Label fileNameLabel = new Label(item.getFileName());
        fileNameLabel.getStyleClass().add("backup-item-name");
        fileNameLabel.setMinWidth(200);
        HBox.setHgrow(fileNameLabel, Priority.ALWAYS);

        // ── Tag ──
        Label formatTag = new Label(
                item.getFormat() == BackupFormat.CUSTOM ? "Custom" : "Plain SQL"
        );
        formatTag.getStyleClass().add(
                item.getFormat() == BackupFormat.CUSTOM ? "table-tag-dump" : "table-tag-sql"
        );

        // ── الحجم ──
        Label sizeLabel = new Label(item.getSizeFormatted());
        sizeLabel.getStyleClass().add("backup-item-size");
        sizeLabel.setMinWidth(80);

        // ── التاريخ ──
        LocalDateTime dt = item.getCreatedAtAsDateTime();
        Label dateLabel = new Label(dt != null ? dt.format(DATE_FMT) : "—");
        dateLabel.getStyleClass().add("backup-item-date");
        dateLabel.setMinWidth(160);

        // ── زر الاستعادة ──
        Button restoreBtn = new Button("⬇ استعادة");
        restoreBtn.getStyleClass().add("table-action-btn");
        restoreBtn.setOnAction(e -> {
            selectedBackupItem = item;
            handleRestoreLocalItem(item);
        });

        row.getChildren().addAll(
                fileNameLabel,
                formatTag,
                sizeLabel,
                dateLabel,
                restoreBtn
        );

        row.setOnMouseClicked(e -> {
            selectedBackupItem = item;
            updateBackupItemSelection();
            btnRestoreSelected.setDisable(false);
        });

        return row;
    }

    private void updateBackupItemSelection() {
        Platform.runLater(() -> {
            for (Node child : backupsList.getChildren()) {
                child.getStyleClass().remove("backup-item-selected");
                if (child instanceof HBox row && selectedBackupItem != null) {
                    if (!row.getChildren().isEmpty()) {
                        Node firstChild = row.getChildren().get(0);
                        if (firstChild instanceof Label nameLabel) {
                            if (nameLabel.getText().equals(selectedBackupItem.getFileName())) {
                                row.getStyleClass().add("backup-item-selected");
                            }
                        }
                    }
                }
            }
        });
    }

    // ════════════════════════════════════════════════════
    //  Handlers
    // ════════════════════════════════════════════════════

    @FXML
    private void handleFullBackup() {
        boolean useCustom = radioCustomFormat.isSelected();
        int compression = (int) compressionSlider.getValue();

        setBackupBusy(true);
        showStatus(backupStatusLabel, "⏳ جاري إنشاء النسخة الاحتياطية...", "");

        String endpoint = "/payroll/backupFull?useCustomFormat=" + useCustom
                + "&compressionLevel=" + compression;

        ApiClient.postAsync(endpoint, null, Object.class)
                .thenAccept(response -> Platform.runLater(() -> {
                    setBackupBusy(false);
                    if (response.isSuccess()) {
                        showStatus(backupStatusLabel,
                                "✅ " + nullSafe(response.getMessage(), "تم إنشاء النسخة بنجاح"),
                                "ok");
                        // ✅ بعد إنشاء النسخة، نقوم بتحديث القائمة تلقائياً
                        handleRefreshBackups();
                    } else {
                        showStatus(backupStatusLabel,
                                "❌ " + nullSafe(response.getMessage(), "فشل إنشاء النسخة"),
                                "error");
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        setBackupBusy(false);
                        showStatus(backupStatusLabel,
                                "❌ خطأ في الاتصال: " + ex.getMessage(), "error");
                    });
                    return null;
                });
    }

    @FXML
    private void handleChooseFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("اختر ملف النسخة الاحتياطية");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("ملفات النسخ الاحتياطي", "*.dump", "*.sql", "*.backup"),
                new FileChooser.ExtensionFilter("كل الملفات", "*.*"));

        File file = chooser.showOpenDialog(uploadFilePath.getScene().getWindow());
        if (file != null) {
            selectedUploadFile = file;
            uploadFilePath.setText(file.getAbsolutePath());
            btnRestoreFile.setDisable(false);
        }
    }

    @FXML
    private void handleRestoreFromFile() {
        if (selectedUploadFile == null) return;

        RestoreMode mode = radioReplace.isSelected() ? RestoreMode.REPLACE : RestoreMode.MERGE;

        if (mode == RestoreMode.REPLACE && !confirmReplace("الملف المحدد")) return;

        setRestoreFileBusy(true);
        showStatus(restoreFileStatusLabel, "⏳ جاري الاستعادة...", "");

        Path filePath = selectedUploadFile.toPath();
        ApiClient.uploadFileAsync(
                        "/payroll/restore?mode=" + mode.name(),
                        filePath, "file", null, Object.class)
                .thenAccept(response -> Platform.runLater(() -> {
                    setRestoreFileBusy(false);
                    if (response.isSuccess()) {
                        showStatus(restoreFileStatusLabel,
                                "✅ " + nullSafe(response.getMessage(), "تمت الاستعادة بنجاح"),
                                "ok");
                    } else {
                        showStatus(restoreFileStatusLabel,
                                "❌ " + nullSafe(response.getMessage(), "فشلت الاستعادة"),
                                "error");
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        setRestoreFileBusy(false);
                        showStatus(restoreFileStatusLabel,
                                "❌ خطأ في الاتصال: " + ex.getMessage(), "error");
                    });
                    return null;
                });
    }

    // ════════════════════════════════════════════════════
    //  Handlers — النسخ المحلية (تحديث يدوي فقط)
    // ════════════════════════════════════════════════════

    /**
     * زر التحديث - يتم استدعاؤه فقط عند الضغط على الزر
     */
    @FXML
    private void handleRefreshBackups() {
        // إظهار حالة التحميل
        showEmptyState("جاري تحميل النسخ الاحتياطية...");
        backupsCountLabel.setText("⏳ جاري التحميل...");

        CompletableFuture
                .<ApiResponse<List<BackupFileInfo>>>supplyAsync(() -> {
                    try {
                        return ApiClient.getWithTypeRef(
                                "/payroll/backups",
                                new TypeReference<List<BackupFileInfo>>() {
                                });
                    } catch (Exception e) {
                        ApiResponse<List<BackupFileInfo>> err = new ApiResponse<>();
                        err.setSuccess(false);
                        err.setMessage(e.getMessage());
                        return err;
                    }
                })
                .thenAccept(response -> {
                    if (response != null && response.isSuccess() && response.getData() != null) {
                        List<BackupFileInfo> data = response.getData();
                        // تحديث البيانات
                        backupsListData.setAll(data);
                        Platform.runLater(() -> {
                            backupsCountLabel.setText(data.size() + " نسخة");
                            selectedBackupItem = null;
                            btnRestoreSelected.setDisable(true);
                            rebuildBackupsList();
                        });
                    } else {
                        Platform.runLater(() -> {
                            backupsListData.clear();
                            backupsCountLabel.setText("0 نسخة");
                            selectedBackupItem = null;
                            btnRestoreSelected.setDisable(true);
                            rebuildBackupsList();
                            if (response != null && response.getMessage() != null) {
                                showStatusInArea(restoreLocalStatusArea,
                                        "⚠ " + response.getMessage(), "warn");
                            }
                        });
                    }
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        backupsListData.clear();
                        backupsCountLabel.setText("⚠ فشل التحميل");
                        selectedBackupItem = null;
                        btnRestoreSelected.setDisable(true);
                        rebuildBackupsList();
                        showStatusInArea(restoreLocalStatusArea,
                                "❌ فشل تحميل قائمة النسخ: " + ex.getMessage(), "error");
                    });
                    return null;
                });
    }

    @FXML
    private void handleRestoreSelected() {
        if (selectedBackupItem == null) {
            BackupFileInfo item = getSelectedItemFromList();
            if (item != null) {
                selectedBackupItem = item;
            } else {
                showStatusInArea(restoreLocalStatusArea,
                        "⚠ يرجى اختيار نسخة من القائمة أولاً", "warn");
                return;
            }
        }
        handleRestoreLocalItem(selectedBackupItem);
    }

    private BackupFileInfo getSelectedItemFromList() {
        for (Node child : backupsList.getChildren()) {
            if (child.getStyleClass().contains("backup-item-selected") && child instanceof HBox row) {
                if (!row.getChildren().isEmpty() && row.getChildren().get(0) instanceof Label nameLabel) {
                    String fileName = nameLabel.getText();
                    for (BackupFileInfo item : backupsListData) {
                        if (item.getFileName().equals(fileName)) {
                            return item;
                        }
                    }
                }
            }
        }
        return null;
    }

    private void handleRestoreLocalItem(BackupFileInfo item) {
        RestoreMode mode = radioLocalReplace.isSelected() ? RestoreMode.REPLACE : RestoreMode.MERGE;

        if (mode == RestoreMode.REPLACE && !confirmReplace(item.getFileName())) return;

        setRestoreLocalBusy(true);
        showStatusInArea(restoreLocalStatusArea,
                "⏳ جاري استعادة «" + item.getFileName() + "»...", "");

        restoreLocalStatusArea.setManaged(true);
        restoreLocalStatusArea.setVisible(true);

        ApiClient.postAsync("/payroll/restore-local?" +
                                "fileName=" + urlEncode(item.getFileName()) +
                                "&mode=" + mode.name(),
                        null, Object.class)
                .thenAccept(response -> Platform.runLater(() -> {
                    setRestoreLocalBusy(false);
                    if (response.isSuccess()) {
                        showStatusInArea(restoreLocalStatusArea,
                                "✅ " + nullSafe(response.getMessage(), "تمت الاستعادة بنجاح"),
                                "ok");
                    } else {
                        showStatusInArea(restoreLocalStatusArea,
                                "❌ " + nullSafe(response.getMessage(), "فشلت الاستعادة"),
                                "error");
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        setRestoreLocalBusy(false);
                        showStatusInArea(restoreLocalStatusArea,
                                "❌ خطأ: " + ex.getMessage(), "error");
                    });
                    return null;
                });
    }

    // ════════════════════════════════════════════════════
    //  Handlers — النسخ التلقائي
    // ════════════════════════════════════════════════════

    @FXML
    private void handleCronPreset(javafx.event.ActionEvent event) {
        if (event.getSource() instanceof Button btn) {
            String preset = (String) btn.getUserData();
            if (preset != null) {
                cronField.setText(preset);
                updateCronHint(preset);
            }
        }
    }

    @FXML
    private void handleToggleAutoBackup() {
        boolean enabled = toggleAutoBackup.isSelected();
        toggleAutoBackup.setText(enabled ? "مفعّل ✓" : "غير مفعّل");
        cronField.setDisable(!enabled);
        maxFilesSpinner.setDisable(!enabled);
    }

    @FXML
    private void handleSaveAutoBackupSettings() {
        boolean enabled = toggleAutoBackup.isSelected();
        String cron = cronField.getText().trim();
        int maxFiles = maxFilesSpinner.getValue();

        if (enabled && cron.isBlank()) {
            showStatus(autoBackupStatusLabel, "⚠ أدخل Cron expression أولاً", "warn");
            return;
        }

        String message = buildPropertiesHint(enabled, cron, maxFiles);
        showStatus(autoBackupStatusLabel, message, enabled ? "ok" : "warn");
    }

    private String buildPropertiesHint(boolean enabled, String cron, int maxFiles) {
        if (!enabled) {
            return "ℹ النسخ التلقائي معطّل — لتفعيله عيّن:\nBACKUP_AUTO_ENABLED=true";
        }
        return "✅ لتطبيق هذه الإعدادات عيّن متغيرات البيئة:\n" +
                "BACKUP_AUTO_ENABLED=true\n" +
                "BACKUP_AUTO_CRON=" + cron + "\n" +
                "BACKUP_MAX_FILES=" + maxFiles + "\n" +
                "ثم أعد تشغيل الخادم.";
    }

    // ════════════════════════════════════════════════════
    //  UI Helpers
    // ════════════════════════════════════════════════════

    private void setBackupBusy(boolean busy) {
        btnBackup.setDisable(busy);
        backupProgress.getStyleClass().removeAll("progress-bar-hidden", "progress-bar-visible");
        backupProgress.getStyleClass().add(busy ? "progress-bar-visible" : "progress-bar-hidden");
    }

    private void setRestoreFileBusy(boolean busy) {
        btnRestoreFile.setDisable(busy);
        restoreFileProgress.getStyleClass().removeAll("progress-bar-hidden", "progress-bar-visible");
        restoreFileProgress.getStyleClass().add(busy ? "progress-bar-visible" : "progress-bar-hidden");
    }

    private void setRestoreLocalBusy(boolean busy) {
        btnRestoreSelected.setDisable(busy);
        restoreLocalProgress.getStyleClass().removeAll("progress-bar-hidden", "progress-bar-visible");
        restoreLocalProgress.getStyleClass().add(busy ? "progress-bar-visible" : "progress-bar-hidden");
    }

    private void showStatus(Label label, String message, String type) {
        label.setText(message);
        label.getStyleClass().removeAll("status-msg-ok", "status-msg-error", "status-msg-warn");
        switch (type) {
            case "ok" -> label.getStyleClass().add("status-msg-ok");
            case "error" -> label.getStyleClass().add("status-msg-error");
            case "warn" -> label.getStyleClass().add("status-msg-warn");
        }
    }

    private void showStatusInArea(TextArea area, String message, String type) {
        area.setText(message);
        area.getStyleClass().removeAll("status-msg-ok", "status-msg-error", "status-msg-warn");
        switch (type) {
            case "ok" -> area.getStyleClass().add("status-msg-ok");
            case "error" -> area.getStyleClass().add("status-msg-error");
            case "warn" -> area.getStyleClass().add("status-msg-warn");
        }
        area.setManaged(true);
        area.setVisible(true);
    }

    private boolean confirmReplace(String target) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("تأكيد الاستعادة");
        alert.setHeaderText("⚠ سيتم مسح البيانات الحالية");
        alert.setContentText(
                "هذه العملية ستحذف جميع بيانات قاعدة البيانات الحالية\n" +
                        "وتستعيد من: " + target + "\n\n" +
                        "هل أنت متأكد؟");

        ButtonType btnConfirm = new ButtonType("نعم، استعادة", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel = new ButtonType("إلغاء", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnConfirm, btnCancel);

        return alert.showAndWait()
                .map(b -> b == btnConfirm)
                .orElse(false);
    }

    private void updateCronHint(String cron) {
        if (cron == null || cron.isBlank()) {
            cronHintLabel.setText("");
            return;
        }
        cronHintLabel.setText(switch (cron.trim()) {
            case "0 0 2 * * ?" -> "كل يوم الساعة 2:00 صباحاً";
            case "0 0 0 * * ?" -> "كل يوم منتصف الليل";
            case "0 0 12 * * ?" -> "كل يوم الظهر";
            case "0 0 2 */2 * ?" -> "كل يومين الساعة 2:00 صباحاً";
            case "0 0 2 */3 * ?" -> "كل 3 أيام الساعة 2:00 صباحاً";
            case "0 0 2 */5 * ?" -> "كل 5 أيام الساعة 2:00 صباحاً";
            case "0 0 2 */7 * ?" -> "كل 7 أيام الساعة 2:00 صباحاً";
            case "0 0 2 * * 0" -> "كل أحد الساعة 2:00 صباحاً";
            case "0 0 2 1 * ?" -> "أول كل شهر الساعة 2:00 صباحاً";
            case "0 0 */6 * * ?" -> "كل 6 ساعات";
            case "0 0 */12 * * ?" -> "كل 12 ساعة";
            default -> "Cron مخصص";
        });
    }

    private String nullSafe(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value : fallback;
    }

    private String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

}