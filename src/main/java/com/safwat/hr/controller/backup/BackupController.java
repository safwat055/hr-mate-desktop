package com.safwat.hr.controller.backup;

import com.safwat.hr.controller.backup.dto.BackupFileInfo;
import com.safwat.hr.controller.backup.dto.BackupFormat;
import com.safwat.hr.controller.backup.dto.RestoreMode;
import com.safwat.hr.ui.theme.SettingsThemeLoader;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class BackupController implements Initializable {

    private final BackupService service = new BackupService();

    @FXML
    private Label statusDot;
    @FXML
    private Label statusLabel;

    @FXML
    private RadioButton radioCustomFormat, radioPlainFormat;
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

    @FXML
    private TextField uploadFilePath;
    @FXML
    private RadioButton radioReplace, radioMerge;
    @FXML
    private Button btnRestoreFile;
    @FXML
    private ProgressBar restoreFileProgress;
    @FXML
    private Label restoreFileStatusLabel;

    @FXML
    private RadioButton radioLocalReplace, radioLocalMerge;
    @FXML
    private Label backupsCountLabel;
    @FXML
    private ListView<BackupFileInfo> backupsListView;
    @FXML
    private Button btnRestoreSelected;
    @FXML
    private ProgressBar restoreLocalProgress;
    @FXML
    private Label restoreLocalStatusArea;

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

    @FXML
    private TextField sqlFilePath;
    @FXML
    private CheckBox chkStopOnError;
    @FXML
    private Button btnValidateScript, btnExecuteScript;
    @FXML
    private ProgressBar scriptProgress;
    @FXML
    private Label scriptStatusLabel;
    @FXML
    private Label scriptResultArea;

    private final ObservableList<BackupFileInfo> backupsListData = FXCollections.observableArrayList();
    private File selectedUploadFile, selectedSqlFile;
    private BackupFileInfo selectedBackupItem;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd  HH:mm:ss");

    private static final String EMPTY_PLACEHOLDER = "— لا توجد بيانات —";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupFormatToggleGroup();
        setupRestoreModeToggleGroup();
        setupLocalRestoreModeToggleGroup();
        setupCompressionSlider();
        setupSpinner();
        setupBackupsListView();
        initStatusLabels();
        cronField.textProperty().addListener((obs, o, n) -> updateCronHint(n));

        SettingsThemeLoader.apply(autoBackupStatusLabel);
    }

    private void initStatusLabels() {
        clearStatusInArea(restoreLocalStatusArea);
        clearStatusInArea(scriptResultArea);
    }

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
            compressionValueLabel.getStyleClass().removeAll(
                    "stg-slider-value-ok", "stg-slider-value-warn", "stg-slider-value-error");
            if (val >= 7) {
                compressionValueLabel.setText(val + " → ضغط عالي - ملف صغير");
                compressionValueLabel.getStyleClass().add("stg-slider-value-ok");
            } else if (val <= 3) {
                compressionValueLabel.setText(val + " → ضغط منخفض - ملف كبير");
                compressionValueLabel.getStyleClass().add("stg-slider-value-error");
            } else {
                compressionValueLabel.setText(val + " → ضغط متوسط");
                compressionValueLabel.getStyleClass().add("stg-slider-value-warn");
            }
        });
    }

    private void setupSpinner() {
        SpinnerValueFactory<Integer> factory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 30, 7);
        maxFilesSpinner.setValueFactory(factory);
    }

    private void setupBackupsListView() {
        backupsListView.setItems(backupsListData);
        backupsListView.setCellFactory(lv -> new BackupItemCell());

        Label emptyLabel = new Label("📭 لا توجد نسخ احتياطية محفوظة");
        emptyLabel.getStyleClass().add("stg-empty-placeholder");
        backupsListView.setPlaceholder(emptyLabel);

        backupsListView.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, sel) -> {
                    selectedBackupItem = sel;
                    btnRestoreSelected.setDisable(sel == null);
                });
    }

    private class BackupItemCell extends ListCell<BackupFileInfo> {

        private final HBox row = new HBox();
        private final Label fileNameLabel = new Label();
        private final Label formatTag = new Label();
        private final Label sizeLabel = new Label();
        private final Label dateLabel = new Label();
        private final Button restoreBtn = new Button("⬇ استعادة");

        BackupItemCell() {
            row.setAlignment(Pos.CENTER_LEFT);
            row.setSpacing(10);
            row.setPadding(new Insets(6, 10, 6, 10));
            row.getStyleClass().add("stg-backup-item");

            fileNameLabel.getStyleClass().add("stg-backup-item-name");
            fileNameLabel.setMinWidth(180);
            HBox.setHgrow(fileNameLabel, Priority.ALWAYS);

            sizeLabel.getStyleClass().add("stg-backup-item-size");
            sizeLabel.setMinWidth(70);

            dateLabel.getStyleClass().add("stg-backup-item-date");
            dateLabel.setMinWidth(150);

            restoreBtn.getStyleClass().add("stg-table-action-btn");

            row.getChildren().setAll(fileNameLabel, formatTag, sizeLabel, dateLabel, restoreBtn);
        }

        @Override
        protected void updateItem(BackupFileInfo item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                restoreBtn.setOnAction(null);
                return;
            }

            fileNameLabel.setText(item.getFileName());

            boolean isCustom = item.getFormat() == BackupFormat.CUSTOM;
            formatTag.setText(isCustom ? "Custom" : "Plain SQL");
            formatTag.getStyleClass().removeAll("stg-table-tag-dump", "stg-table-tag-sql");
            formatTag.getStyleClass().add(isCustom ? "stg-table-tag-dump" : "stg-table-tag-sql");

            sizeLabel.setText(item.getSizeFormatted());

            LocalDateTime dt = item.getCreatedAtAsDateTime();
            dateLabel.setText(dt != null ? dt.format(DATE_FMT) : "—");

            restoreBtn.setOnAction(e -> handleRestoreLocalItem(item));

            setGraphic(row);
        }
    }

    // ─── Handlers ───

    @FXML
    private void handleFullBackup() {
        boolean useCustom = radioCustomFormat.isSelected();
        int compression = (int) compressionSlider.getValue();

        setBackupBusy(true);
        showStatus(backupStatusLabel, "⏳ جاري إنشاء النسخة الاحتياطية...", "");

        service.backupFull(useCustom, compression)
                .thenAccept(response -> Platform.runLater(() -> {
                    setBackupBusy(false);
                    if (response.isSuccess()) {
                        showStatus(backupStatusLabel,
                                "✅ " + service.nullSafe(response.getMessage(),
                                        "تم إنشاء النسخة بنجاح"), "ok");
                        handleRefreshBackups();
                    } else {
                        showStatus(backupStatusLabel,
                                "❌ " + service.nullSafe(response.getMessage(),
                                        "فشل إنشاء النسخة"), "error");
                    }
                }))
                .exceptionally(ex ->
                        handleConnectionError(backupStatusLabel, this::setBackupBusy, ex));
    }

    @FXML
    private void handleChooseFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("اختر ملف النسخة الاحتياطية");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("ملفات النسخ الاحتياطي",
                        "*.dump", "*.sql", "*.backup"),
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

        service.restoreFromFile(selectedUploadFile.toPath(), mode)
                .thenAccept(response -> Platform.runLater(() -> {
                    setRestoreFileBusy(false);
                    showStatus(restoreFileStatusLabel,
                            response.isSuccess()
                                    ? "✅ " + service.nullSafe(response.getMessage(), "تمت الاستعادة بنجاح")
                                    : "❌ " + service.nullSafe(response.getMessage(), "فشلت الاستعادة"),
                            response.isSuccess() ? "ok" : "error");
                }))
                .exceptionally(ex ->
                        handleConnectionError(restoreFileStatusLabel, this::setRestoreFileBusy, ex));
    }

    @FXML
    private void handleRefreshBackups() {
        backupsCountLabel.setText("⏳ جاري التحميل...");

        service.listBackups()
                .thenAccept(response -> Platform.runLater(() -> {
                    if (response != null && response.isSuccess() && response.getData() != null) {
                        List<BackupFileInfo> data = response.getData();
                        backupsListData.setAll(data);
                        backupsCountLabel.setText(data.size() + " نسخة");
                        selectedBackupItem = null;
                        btnRestoreSelected.setDisable(true);
                    } else {
                        backupsListData.clear();
                        backupsCountLabel.setText("0 نسخة");
                        selectedBackupItem = null;
                        btnRestoreSelected.setDisable(true);
                        if (response != null && response.getMessage() != null) {
                            showStatusInArea(restoreLocalStatusArea,
                                    "⚠ " + response.getMessage(), "warn");
                        }
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        backupsListData.clear();
                        backupsCountLabel.setText("0 نسخة");
                        selectedBackupItem = null;
                        btnRestoreSelected.setDisable(true);
                        showStatusInArea(restoreLocalStatusArea,
                                "❌ فشل تحميل قائمة النسخ: " + ex.getMessage(), "error");
                    });
                    return null;
                });
    }

    @FXML
    private void handleRestoreSelected() {
        if (selectedBackupItem == null) {
            showStatusInArea(restoreLocalStatusArea,
                    "⚠ يرجى اختيار نسخة من القائمة أولاً", "warn");
            return;
        }
        handleRestoreLocalItem(selectedBackupItem);
    }

    private void handleRestoreLocalItem(BackupFileInfo item) {
        RestoreMode mode = radioLocalReplace.isSelected()
                ? RestoreMode.REPLACE : RestoreMode.MERGE;
        if (mode == RestoreMode.REPLACE && !confirmReplace(item.getFileName())) return;

        setRestoreLocalBusy(true);
        showStatusInArea(restoreLocalStatusArea,
                "⏳ جاري استعادة «" + item.getFileName() + "»...", "");

        service.restoreLocal(item.getFileName(), mode)
                .thenAccept(response -> Platform.runLater(() -> {
                    setRestoreLocalBusy(false);
                    showStatusInArea(restoreLocalStatusArea,
                            response.isSuccess()
                                    ? "✅ " + service.nullSafe(response.getMessage(), "تمت الاستعادة بنجاح")
                                    : "❌ " + service.nullSafe(response.getMessage(), "فشلت الاستعادة"),
                            response.isSuccess() ? "ok" : "error");
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

    @FXML
    private void handleCronPreset(javafx.event.ActionEvent event) {
        if (event.getSource() instanceof Button btn
                && btn.getUserData() instanceof String preset) {
            cronField.setText(preset);
            updateCronHint(preset);
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

        showStatus(autoBackupStatusLabel,
                service.buildAutoBackupHint(enabled, cron, maxFiles),
                enabled ? "ok" : "warn");
    }

    @FXML
    private void handleChooseSqlFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("اختر ملف SQL");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("ملفات SQL", "*.sql"),
                new FileChooser.ExtensionFilter("كل الملفات", "*.*"));

        File file = chooser.showOpenDialog(sqlFilePath.getScene().getWindow());
        if (file != null) {
            selectedSqlFile = file;
            sqlFilePath.setText(file.getAbsolutePath());
            btnValidateScript.setDisable(false);
            btnExecuteScript.setDisable(false);
            clearStatusInArea(scriptResultArea);
            showStatus(scriptStatusLabel, "", "");
        }
    }

    @FXML
    private void handleValidateScript() {
        if (selectedSqlFile == null) return;

        setScriptBusy(true);
        showStatus(scriptStatusLabel, "⏳ جاري فحص توقيع الملف...", "");

        service.validateScript(selectedSqlFile.toPath())
                .thenAccept(response -> Platform.runLater(() -> {
                    setScriptBusy(false);
                    showStatus(scriptStatusLabel,
                            response.isSuccess()
                                    ? "✅ " + service.nullSafe(response.getMessage(), "التوقيع صحيح")
                                    : "❌ " + service.nullSafe(response.getMessage(), "التوقيع غير صحيح"),
                            response.isSuccess() ? "ok" : "error");
                }))
                .exceptionally(ex ->
                        handleConnectionError(scriptStatusLabel, this::setScriptBusy, ex));
    }

    @FXML
    private void handleExecuteScript() {
        if (selectedSqlFile == null) return;
        if (!confirmExecuteScript(selectedSqlFile.getName())) return;

        boolean stopOnError = chkStopOnError.isSelected();

        setScriptBusy(true);
        showStatus(scriptStatusLabel, "⏳ جاري تنفيذ السكريبت على قاعدة البيانات...", "");
        showStatusInArea(scriptResultArea, "جاري التنفيذ...\n\nلا تغلق البرنامج.", "warn");

        service.executeScript(selectedSqlFile.toPath(), stopOnError)
                .thenAccept(response -> Platform.runLater(() -> {
                    setScriptBusy(false);
                    ScriptExecuteResult result = response.getData();
                    if (response.isSuccess() && result != null) {
                        showStatus(scriptStatusLabel,
                                "✅ " + service.nullSafe(response.getMessage(), "تم التنفيذ بنجاح"), "ok");
                        showStatusInArea(scriptResultArea,
                                service.buildScriptReport(result), "ok");
                    } else {
                        showStatus(scriptStatusLabel,
                                "❌ " + service.nullSafe(response.getMessage(), "فشل التنفيذ"), "error");
                        showStatusInArea(scriptResultArea,
                                result != null
                                        ? service.buildScriptReport(result)
                                        : service.nullSafe(response.getMessage(), "فشل التنفيذ بدون تفاصيل"),
                                "error");
                    }
                }))
                .exceptionally(ex ->
                        handleConnectionError(scriptStatusLabel, this::setScriptBusy, ex));
    }

    // ─── UI Helpers ───

    private boolean confirmExecuteScript(String fileName) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("تأكيد تنفيذ السكريبت");
        confirm.setHeaderText("⚠ سيتم تنفيذ الأوامر على قاعدة البيانات مباشرة");
        confirm.setContentText("الملف: " + fileName + "\n\nهل أنت متأكد؟");
        confirm.getDialogPane().getStyleClass().add("stg-dialog");
        SettingsThemeLoader.apply(confirm.getDialogPane());

        ButtonType btnYes = new ButtonType("تنفيذ", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnNo = new ButtonType("إلغاء", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(btnYes, btnNo);

        return confirm.showAndWait().map(b -> b == btnYes).orElse(false);
    }

    private Void handleConnectionError(Label label, Consumer<Boolean> busySetter, Throwable ex) {
        Platform.runLater(() -> {
            busySetter.accept(false);
            showStatus(label, "❌ خطأ في الاتصال: " + ex.getMessage(), "error");
        });
        return null;
    }

    private void setBackupBusy(boolean busy) {
        btnBackup.setDisable(busy);
        toggleProgress(backupProgress, busy);
    }

    private void setRestoreFileBusy(boolean busy) {
        btnRestoreFile.setDisable(busy);
        toggleProgress(restoreFileProgress, busy);
    }

    private void setRestoreLocalBusy(boolean busy) {
        btnRestoreSelected.setDisable(busy
                || backupsListView.getSelectionModel().getSelectedItem() == null);
        toggleProgress(restoreLocalProgress, busy);
    }

    private void setScriptBusy(boolean busy) {
        btnValidateScript.setDisable(busy || selectedSqlFile == null);
        btnExecuteScript.setDisable(busy || selectedSqlFile == null);
        toggleProgress(scriptProgress, busy);
    }

    private void toggleProgress(ProgressBar bar, boolean busy) {
        bar.getStyleClass().removeAll("stg-progress-bar-hidden", "stg-progress-bar-visible");
        bar.getStyleClass().add(busy ? "stg-progress-bar-visible" : "stg-progress-bar-hidden");
    }

    private void showStatus(Label label, String message, String type) {
        label.setText(message);
        label.getStyleClass().removeAll(
                "stg-status-msg-ok", "stg-status-msg-error", "stg-status-msg-warn", "stg-status-msg-empty");
        switch (type) {
            case "ok" -> label.getStyleClass().add("stg-status-msg-ok");
            case "error" -> label.getStyleClass().add("stg-status-msg-error");
            case "warn" -> label.getStyleClass().add("stg-status-msg-warn");
            default -> {
            }
        }
    }

    private void showStatusInArea(Label area, String message, String type) {
        area.setText(message);
        area.setStyle("");
        area.getStyleClass().removeAll(
                "stg-status-msg-ok", "stg-status-msg-error", "stg-status-msg-warn", "stg-status-msg-empty");
        switch (type) {
            case "ok" -> area.getStyleClass().add("stg-status-msg-ok");
            case "error" -> area.getStyleClass().add("stg-status-msg-error");
            case "warn" -> area.getStyleClass().add("stg-status-msg-warn");
            default -> area.getStyleClass().add("stg-status-msg-empty");
        }
    }

    private void clearStatusInArea(Label area) {
        area.setText(EMPTY_PLACEHOLDER);
        area.setStyle("");
        area.getStyleClass().removeAll(
                "stg-status-msg-ok", "stg-status-msg-error", "stg-status-msg-warn");
        area.getStyleClass().add("stg-status-msg-empty");
    }

    private boolean confirmReplace(String target) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("تأكيد الاستعادة");
        alert.setHeaderText("⚠ سيتم مسح البيانات الحالية");
        alert.setContentText(
                "هذه العملية ستحذف جميع بيانات قاعدة البيانات الحالية\n" +
                        "وتستعيد من: " + target + "\n\n" +
                        "هل أنت متأكد؟");
        alert.getDialogPane().getStyleClass().add("stg-dialog");
        SettingsThemeLoader.apply(alert.getDialogPane());

        ButtonType btnConfirm = new ButtonType("نعم، استعادة", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel = new ButtonType("إلغاء", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnConfirm, btnCancel);

        return alert.showAndWait().map(b -> b == btnConfirm).orElse(false);
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
}