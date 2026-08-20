package com.safwat.hr.report.controller;

import com.safwat.hr.network.ApiClient;
import com.safwat.hr.network.ApiResponse;
import com.safwat.hr.network.dto.ReportPayloadResponse;
import com.safwat.hr.network.dto.ReportStatusResponse;
import com.safwat.hr.shared.FXMLPaths;
import com.safwat.hr.ui.controls.SAFNotification;
import com.safwat.hr.ui.util.ViewManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

/**
 * واجهة عرض تقارير المستخدم.
 *
 * <p>تعرض جدولاً بكل التقارير المقدمة من المستخدم مرتبة من الأحدث.
 * لكل تقرير: أزرار (عرض مخرجات / نسخ).
 *
 * <p><b>عرض المخرجات:</b>
 * <ul>
 *   <li>ملف (output != null) → FileChooser للتحميل</li>
 *   <li>نص/خطأ → Alert مع TextArea للقراءة</li>
 * </ul>
 */
@Slf4j
public class UserReportsController implements Initializable {

    @FXML
    private TableView<ReportStatusResponse> reportsTable;
    @FXML
    private TableColumn<ReportStatusResponse, Long> colReportId;
    @FXML
    private TableColumn<ReportStatusResponse, String> colReportName;
    @FXML
    private TableColumn<ReportStatusResponse, String> colStatus;
    @FXML
    private TableColumn<ReportStatusResponse, String> colSubmittedTime;
    @FXML
    private TableColumn<ReportStatusResponse, String> colFinishedTime;
    @FXML
    private TableColumn<ReportStatusResponse, String> colMessage;

    @FXML
    private TableColumn<ReportStatusResponse, Void> colActions;

    @FXML
    private Button btnRefresh;
    @FXML
    private Button btnNewReport;
    @FXML
    private Label lblStatus;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupColumns();
        loadReports();
    }

    /**
     * إعداد أعمدة الجدول + عمود الإجراءات (عرض/نسخ).
     */
    private void setupColumns() {
        // الفورمتر بتاعك — هنستخدمه دلوقتي
        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.of("Africa/Cairo"));

        colReportId.setCellValueFactory(new PropertyValueFactory<>("reportId"));
        colReportName.setCellValueFactory(new PropertyValueFactory<>("reportName"));
        colReportName.setMinWidth(280.00);
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        // ⬅️ تاريخ التقديم — بدون T
        colSubmittedTime.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatInstant(item, formatter));
            }
        });
        colSubmittedTime.setCellValueFactory(new PropertyValueFactory<>("submittedTime"));

        // ⬅️ تاريخ الانتهاء — بدون T
        colFinishedTime.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatInstant(item, formatter));
            }
        });
        colFinishedTime.setCellValueFactory(new PropertyValueFactory<>("finishedTime"));
        colMessage.setCellValueFactory(new PropertyValueFactory<>("message"));
        colMessage.setMaxWidth(90.00);
        // ── عمود الإجراءات (عرض + نسخ) ──
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnView = new Button("عرض");
            private final Button btnCopy = new Button("نسخ");
            private final Button btnCancel = new Button("إلغاء");

            private final HBox pane = new HBox(8, btnView, btnCopy, btnCancel);

            {
                btnView.setStyle("-fx-font-size: 11;");
                btnCopy.setStyle("-fx-font-size: 11;");
                btnCancel.setStyle("-fx-font-size: 11;");

                btnView.setOnAction(e -> handleView(getTableRow().getItem()));
                btnCopy.setOnAction(e -> handleCopy(getTableRow().getItem()));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    /**
     * يفك ISO-8601 ويرجع تاريخ مقروء
     */
    private String formatInstant(String isoString, DateTimeFormatter formatter) {
        try {
            // يقبل: 2026-08-01T23:36:50Z أو 2026-08-01T23:36:50+03:00
            Instant instant = Instant.parse(isoString.replace(" ", "T")); // احتياط
            return formatter.format(instant);
        } catch (Exception e) {
            // لو فشل، ارجع النص بعد إزالة T يدوياً
            return isoString.replace("T", " ");
        }
    }

    /**
     * يحمل/يُعيد تحميل قائمة التقارير من السيرفر.
     */
    @FXML
    void loadReports() {
        lblStatus.setText("جاري التحميل...");
        try {
            var response = ApiClient.getMyReports();
            if (response.isSuccess() && response.getData() != null) {
                reportsTable.getItems().setAll(response.getData());
                lblStatus.setText("عدد التقارير: " + response.getData().size());
            } else {
                lblStatus.setText("فشل تحميل التقارير");
                SAFNotification.warning(response.getMessage());
            }
        } catch (Exception e) {
            lblStatus.setText("خطأ في الاتصال");
            SAFNotification.error("تعذر الاتصال بالخادم: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    //  عرض المخرجات (ملف أو نص)
    // ─────────────────────────────────────────────

    private void handleView(ReportStatusResponse report) {
        String status = report.getStatus();

        // فشل → اعرض الخطأ
        if ("FAILED".equals(status)) {
            showTextDialog("❌ خطأ في التقرير " + report.getReportId(),
                    report.getErrorMessage());
            return;
        }

        // لسه شغال → اعرض حالة فقط
        if ("PENDING".equals(status) || "QUEUED".equals(status) || "RUNNING".equals(status)) {
            showTextDialog("⏳ حالة التقرير " + report.getReportId(),
                    "الحالة: " + status + "\nنسبة الإنجاز: " + report.getProgress() + "%");
            return;
        }

        // اكتمل وفيه ملف → حمّل
        if (report.getOutput() != null && !report.getOutput().isBlank()
                && (report.getOutput().endsWith(".pdf") || report.getOutput().endsWith(".xlsx"))) {
            downloadFile(report);
            return;
        }

        // اكتمل بس مفيش ملف (مثلاً نص) → اعرض الرسالة
        if (report.getMessage() != null && !report.getMessage().isBlank()) {
            showTextDialog("📋 مخرجات التقرير " + report.getReportId(),
                    report.getMessage() + "\n" + report.getOutput() + "\n" + report.getErrorMessage());
            return;
        }

        // أي حالة تانية
        showTextDialog("ℹ️ تقرير " + report.getReportId(), "لا توجد مخرجات متاحة");
    }

    /**
     * يفتح FileChooser ويحمل الملف من السيرفر.
     */
    private void downloadFile(ReportStatusResponse report) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("حفظ ملف التقرير");

        // ── 1. إنشاء/تحديد مجلد temp_downloads ──
        try {
            Path tempDir = Path.of(System.getProperty("user.dir"), "temp_downloads");
            if (!Files.exists(tempDir)) {
                Files.createDirectories(tempDir);
                log.info("تم إنشاء مجلد التحميلات: {}", tempDir);
            }
            chooser.setInitialDirectory(tempDir.toFile());
        } catch (Exception e) {
            log.warn("فشل إنشاء مجلد temp_downloads: {}", e.getMessage());
            // لو فشل، الـ FileChooser هيفتح في المجلد الافتراضي (Home)
        }

        // ── 2. اسم الملف الافتراضي ──
        String output = report.getOutput();
        String defaultName = output != null && output.contains("/")
                ? output.substring(output.lastIndexOf('/') + 1)
                : "report_" + report.getReportId() + ".pdf";
        chooser.setInitialFileName(defaultName);

        File file = chooser.showSaveDialog(reportsTable.getScene().getWindow());
        if (file == null) return;

        // ── 3. التحميل (زي ما هو) ──
        new Thread(() -> {
            Path target = file.toPath();
            boolean downloaded = false;
            try {
                downloaded = ApiClient.downloadReportFile(report.getReportId(), target);
            } catch (Exception e) {
                Platform.runLater(() -> SAFNotification.error("خطأ في التحميل: " + e.getMessage()));
                return;
            }

            boolean finalDownloaded = downloaded;
            Platform.runLater(() -> {
                try {
                    if (!finalDownloaded || !Files.exists(target) || Files.size(target) == 0) {
                        Files.deleteIfExists(target);
                        SAFNotification.error("❌ الملف غير موجود على السير فر أو تم حذفه");
                        return;
                    }
                    SAFNotification.withAction("✅ تم التحميل: " + file.getName(), file);
                } catch (IOException e) {
                    SAFNotification.error("خطأ في التحقق من الملف");
                }
            });
        }).start();
    }

    /**
     * يعرض نص في Alert قابل للتكبير.
     */
    private void showTextDialog(String title, String content) {
        if (content == null || content.isBlank()) {
            content = "لا توجد بيانات";
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);

        TextArea textArea = new TextArea(content);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefRowCount(12);
        textArea.setPrefColumnCount(50);

        alert.getDialogPane().setContent(textArea);
        alert.setResizable(true);
        alert.getDialogPane().setPrefSize(500, 350);
        alert.showAndWait();
    }

    // ─────────────────────────────────────────────
    //  نسخ التقرير
    // ─────────────────────────────────────────────

    private void handleCopy(ReportStatusResponse report) {
        try {
            ApiResponse<ReportPayloadResponse> response =
                    ApiClient.getReportPayload(report.getReportId());

            if (!response.isSuccess() || response.getData() == null) {
                SAFNotification.warning("تعذر جلب بيانات التقرير الأصلي");
                return;
            }

            openPayrollReportWithPayload(response.getData());

        } catch (Exception e) {
            e.printStackTrace();
            SAFNotification.error("فشل النسخ: " + e.getMessage());
        }
    }

    private void openPayrollReportWithPayload(ReportPayloadResponse response) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/safwat/hr/view/report/payroll/PayrollReport.fxml"));
            Parent root = loader.load();

            PayrollReportController controller = loader.getController();
            controller.loadFromPayload(response);  // ⬅️ نمرر الـ DTO كامل

            Stage stage = new Stage();
            stage.setTitle("نسخ تقرير: " + response.reportName());
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            SAFNotification.error("فشل فتح الواجهة: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    //  تقرير جديد
    // ─────────────────────────────────────────────

    @FXML
    void openNewReport() {

        ViewManager.openIndependentView(new FXMLPaths().getPayrollReport(), null);

    }
}