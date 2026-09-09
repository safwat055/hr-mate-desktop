package com.safwat.hr.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.safwat.hr.backup.BackupFileInfo;
import com.safwat.hr.backup.BackupFormat;
import com.safwat.hr.backup.RestoreMode;
import com.safwat.hr.network.ApiClient;
import com.safwat.hr.network.ApiResponse;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * كنترولر شاشة النسخ الاحتياطي.
 *
 * <p>يغطي:
 * <ul>
 *   <li>النسخ الكامل (Custom / Plain SQL، مستوى ضغط متغير)</li>
 *   <li>استعادة من ملف مرفوع (REPLACE / MERGE)</li>
 *   <li>استعراض النسخ المحلية واستعادة منها (REPLACE / MERGE)</li>
 *   <li>إعدادات النسخ التلقائي (cron + max files)</li>
 * </ul>
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
    private TableView<BackupFileInfo> backupsTable;
    @FXML
    private TableColumn<BackupFileInfo, String> colFileName;
    @FXML
    private TableColumn<BackupFileInfo, String> colFormat;
    @FXML
    private TableColumn<BackupFileInfo, String> colSize;
    @FXML
    private TableColumn<BackupFileInfo, String> colDate;
    @FXML
    private TableColumn<BackupFileInfo, Void> colActions;
    @FXML
    private Button btnRestoreSelected;
    @FXML
    private ProgressBar restoreLocalProgress;
    @FXML
    private Label restoreLocalStatusLabel;

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
    private final ObservableList<BackupFileInfo> backupsList = FXCollections.observableArrayList();
    private File selectedUploadFile;

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
        setupBackupsTable();
        setupSpinner();
        cronField.textProperty().addListener((obs, o, n) -> updateCronHint(n));

        // جلب النسخ المحلية عند فتح الشاشة
        handleRefreshBackups();
    }

    // ── Toggle Groups ──

    private void setupFormatToggleGroup() {
        ToggleGroup group = new ToggleGroup();
        radioCustomFormat.setToggleGroup(group);
        radioPlainFormat.setToggleGroup(group);
        // مستوى الضغط يظهر فقط مع Custom Format
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
        compressionSlider.valueProperty().addListener((obs, o, n) ->
                compressionValueLabel.setText(String.valueOf(n.intValue())));
    }

    private void setupSpinner() {
        SpinnerValueFactory<Integer> factory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 30, 7);
        maxFilesSpinner.setValueFactory(factory);
    }

    // ── Table ──

    private void setupBackupsTable() {
        colFileName.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getFileName()));

        colFormat.setCellValueFactory(c ->
                new SimpleStringProperty(
                        c.getValue().getFormat() == BackupFormat.CUSTOM ? "Custom" : "Plain SQL"));
        colFormat.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                Label tag = new Label(item);
                tag.getStyleClass().add(
                        item.equals("Custom") ? "table-tag-dump" : "table-tag-sql");
                setGraphic(tag);
                setText(null);
            }
        });

        colSize.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getSizeFormatted()));

        colDate.setCellValueFactory(c ->
                new SimpleStringProperty(
                        c.getValue().getCreatedAt() != null
                                ? c.getValue().getCreatedAt().format(DATE_FMT)
                                : "—"));

        // زر الاستعادة داخل كل صف
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("⬇ استعادة");

            {
                btn.getStyleClass().add("table-action-btn");
                btn.setOnAction(e -> {
                    BackupFileInfo item = getTableView().getItems().get(getIndex());
                    handleRestoreLocalItem(item);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        backupsTable.setItems(backupsList);

        // تفعيل زر الاستعادة عند اختيار صف
        backupsTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) ->
                btnRestoreSelected.setDisable(n == null));
    }

    // ════════════════════════════════════════════════════
    //  Handlers — النسخ الكامل
    // ════════════════════════════════════════════════════

    @FXML
    private void handleFullBackup() {
        boolean useCustom = radioCustomFormat.isSelected();
        int compression = (int) compressionSlider.getValue();

        setBackupBusy(true);
        showStatus(backupStatusLabel, "⏳ جاري إنشاء النسخة الاحتياطية...", "");

        Map<String, String> params = Map.of(
                "useCustomFormat", String.valueOf(useCustom),
                "compressionLevel", String.valueOf(compression));

        ApiClient.postAsync("/payroll/backupFull", null, Object.class)
                .thenAccept(response -> Platform.runLater(() -> {
                    setBackupBusy(false);
                    if (response.isSuccess()) {
                        showStatus(backupStatusLabel,
                                "✅ " + nullSafe(response.getMessage(), "تم إنشاء النسخة بنجاح"),
                                "ok");
                        handleRefreshBackups(); // تحديث القائمة تلقائياً
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

    // ════════════════════════════════════════════════════
    //  Handlers — استعادة من ملف مرفوع
    // ════════════════════════════════════════════════════

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

        // تأكيد للـ REPLACE لأنه عملية مدمرة
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
    //  Handlers — النسخ المحلية
    // ════════════════════════════════════════════════════

    @FXML
    private void handleRefreshBackups() {
        ApiClient.getAsync("/payroll/backups",
                        null,
                        new TypeReference<ApiResponse<List<BackupFileInfo>>>() {
                        })
                .thenAccept(response -> Platform.runLater(() -> {

                    if (response != null && response.isSuccess() && response.getData() != null) {
                        List<BackupFileInfo> data = (List<BackupFileInfo>) response.getData();
                        backupsList.setAll(data);
                        backupsCountLabel.setText(data.size() + " نسخة");
                    } else {
                        backupsList.clear();
                        backupsCountLabel.setText("0 نسخة");
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> backupsCountLabel.setText("⚠ فشل التحديث"));
                    return null;
                });
    }

    @FXML
    private void handleRestoreSelected() {
        BackupFileInfo selected = backupsTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        handleRestoreLocalItem(selected);
    }

    private void handleRestoreLocalItem(BackupFileInfo item) {
        RestoreMode mode = radioLocalReplace.isSelected() ? RestoreMode.REPLACE : RestoreMode.MERGE;

        if (mode == RestoreMode.REPLACE && !confirmReplace(item.getFileName())) return;

        setRestoreLocalBusy(true);
        showStatus(restoreLocalStatusLabel,
                "⏳ جاري استعادة «" + item.getFileName() + "»...", "");

        Map<String, String> params = Map.of(
                "fileName", item.getFileName(),
                "mode", mode.name());

        ApiClient.postAsync("/payroll/restore-local?" +
                                "fileName=" + urlEncode(item.getFileName()) +
                                "&mode=" + mode.name(),
                        null, Object.class)
                .thenAccept(response -> Platform.runLater(() -> {
                    setRestoreLocalBusy(false);
                    if (response.isSuccess()) {
                        showStatus(restoreLocalStatusLabel,
                                "✅ " + nullSafe(response.getMessage(), "تمت الاستعادة بنجاح"),
                                "ok");
                    } else {
                        showStatus(restoreLocalStatusLabel,
                                "❌ " + nullSafe(response.getMessage(), "فشلت الاستعادة"),
                                "error");
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        setRestoreLocalBusy(false);
                        showStatus(restoreLocalStatusLabel,
                                "❌ خطأ: " + ex.getMessage(), "error");
                    });
                    return null;
                });
    }

    // ════════════════════════════════════════════════════
    //  Handlers — النسخ التلقائي
    // ════════════════════════════════════════════════════

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

        // إرسال الإعدادات للـ backend
        Map<String, Object> body = Map.of(
                "enabled", enabled,
                "cron", cron,
                "maxFiles", maxFiles);

        ApiClient.postAsync("/payroll/backup-settings", body, Object.class)
                .thenAccept(response -> Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        showStatus(autoBackupStatusLabel,
                                "✅ تم حفظ الإعدادات — ستُطبَّق عند إعادة تشغيل الخادم", "ok");
                    } else {
                        showStatus(autoBackupStatusLabel,
                                "❌ " + nullSafe(response.getMessage(), "فشل الحفظ"), "error");
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() ->
                            showStatus(autoBackupStatusLabel,
                                    "❌ خطأ في الاتصال: " + ex.getMessage(), "error"));
                    return null;
                });
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
        btnRestoreSelected.setDisable(busy || backupsTable.getSelectionModel().getSelectedItem() == null);
        restoreLocalProgress.getStyleClass().removeAll("progress-bar-hidden", "progress-bar-visible");
        restoreLocalProgress.getStyleClass().add(busy ? "progress-bar-visible" : "progress-bar-hidden");
    }

    /**
     * يعرض رسالة حالة ملونة على الـ Label المحدد.
     *
     * @param label   الـ Label المستهدف
     * @param message النص
     * @param type    "" | "ok" | "error" | "warn"
     */
    private void showStatus(Label label, String message, String type) {
        label.setText(message);
        label.getStyleClass().removeAll("status-msg-ok", "status-msg-error", "status-msg-warn");
        switch (type) {
            case "ok" -> label.getStyleClass().add("status-msg-ok");
            case "error" -> label.getStyleClass().add("status-msg-error");
            case "warn" -> label.getStyleClass().add("status-msg-warn");
        }
    }

    /**
     * تأكيد للعمليات المدمرة (REPLACE).
     */
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

    /**
     * تفسير Cron expression بالعربي (بسيط).
     */
    private void updateCronHint(String cron) {
        if (cron == null || cron.isBlank()) {
            cronHintLabel.setText("");
            return;
        }
        // تفسيرات شائعة
        cronHintLabel.setText(switch (cron.trim()) {
            case "0 0 2 * * ?" -> "كل يوم الساعة 2:00 صباحاً";
            case "0 0 0 * * ?" -> "كل يوم منتصف الليل";
            case "0 0 12 * * ?" -> "كل يوم الظهر";
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