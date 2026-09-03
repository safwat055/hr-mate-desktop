package com.safwat.hr.system;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class LogsController implements Initializable {

    @FXML private TextArea txtLogs;
    @FXML private Label lblLogCount;
    @FXML private TextField txtFilter;
    @FXML private CheckBox chkAutoScroll;
    @FXML private Button btnRefresh;
    @FXML private Button btnClear;
    @FXML private Button btnExport;
    @FXML private Button btnFilter;
    @FXML private Button btnClearFilter;

    // ✅ Listener مسجّل على AppLogBus — نحتفظ بـ reference عشان نقدر نشيله
    private Consumer<String> busListener;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupButtons();

        // ✅ ربط مع AppLogBus — يستقبل كل السجلات القديمة + كل جديد
        busListener = this::appendLine;
        AppLogBus.getInstance().addListener(busListener);

        updateCount();
    }

    private void setupButtons() {
        btnRefresh.setOnAction(e -> refreshFromBus());
        btnClear.setOnAction(e -> clearDisplay());
        btnExport.setOnAction(e -> exportLogs());
        btnFilter.setOnAction(e -> filterLogs());
        btnClearFilter.setOnAction(e -> clearFilter());
        txtFilter.setOnAction(e -> filterLogs());
    }

    /**
     * يُنادى تلقائيًا من AppLogBus عند كل رسالة جديدة.
     * الرسالة وصلت بالفعل مع timestamp من AppLogBus.
     */
    private void appendLine(String formattedLine) {
        txtLogs.appendText(formattedLine + "\n");
        updateCount();
        if (chkAutoScroll.isSelected()) {
            txtLogs.setScrollTop(Double.MAX_VALUE);
        }
    }

    private void refreshFromBus() {
        // ✅ إعادة تحميل كل السجلات من AppLogBus
        txtLogs.setText(AppLogBus.getInstance().getAllLogsAsText() + "\n");
        updateCount();
    }

    private void updateCount() {
        String text = txtLogs.getText();
        long count = text.isEmpty() ? 0 : text.lines().filter(l -> !l.isBlank()).count();
        lblLogCount.setText("السجلات: " + count);
    }

    private void clearDisplay() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("تأكيد");
        alert.setHeaderText("هل تريد مسح عرض السجلات؟\n(لن يمسح ملف app.log على القرص)");
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            txtLogs.clear();
            updateCount();
        }
    }

    private void exportLogs() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("تصدير السجلات");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Text Files", "*.txt")
        );
        fileChooser.setInitialFileName("logs_" + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt");

        Stage stage = new Stage();
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try (FileWriter fw = new FileWriter(file)) {
                fw.write(txtLogs.getText());
                AppLogBus.getInstance().log("✅ تم تصدير السجلات إلى: " + file.getName());
                showAlert("نجاح", "تم تصدير السجلات بنجاح");
            } catch (Exception e) {
                AppLogBus.getInstance().log("❌ فشل تصدير السجلات: " + e.getMessage());
                showAlert("خطأ", "فشل تصدير السجلات: " + e.getMessage());
            }
        }
    }

    private void filterLogs() {
        String filter = txtFilter.getText().trim().toLowerCase();
        if (filter.isEmpty()) {
            refreshFromBus();
            return;
        }

        String full = AppLogBus.getInstance().getAllLogsAsText();
        StringBuilder filtered = new StringBuilder();
        for (String line : full.split("\n")) {
            if (line.toLowerCase().contains(filter)) {
                filtered.append(line).append("\n");
            }
        }
        txtLogs.setText(filtered.toString());
        updateCount();
    }

    private void clearFilter() {
        txtFilter.clear();
        refreshFromBus();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(title.contains("خطأ") ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
