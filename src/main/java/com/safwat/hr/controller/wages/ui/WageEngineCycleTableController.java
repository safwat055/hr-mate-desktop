package com.safwat.hr.controller.wages.ui;

import com.safwat.hr.network.ApiClient;
import com.safwat.hr.controller.wages.dto.WageMonthResultDto;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * جدول نتيجة دورة الأجر المتغير (ناتج المحرك) لموظف واحد.
 *
 * <p>يضم الآن شريطَين:
 * <ol>
 *   <li>شريط الاحتساب — زرار «احسب / تحديث» مع جدول النتيجة.</li>
 *   <li>شريط تصدير البطاقة — حقلا نص «من» و«إلى» (yyyy-MM-dd، اختياريان)
 *       + زرار «تصدير PDF» يفتح FileChooser للحفظ.</li>
 * </ol>
 */
public class WageEngineCycleTableController {

    // ── شريط الاحتساب ──
    @FXML private Button    calculateButton;
    @FXML private Label     statusLabel;
    @FXML private TableView<WageMonthResultDto> table;
    @FXML private TableColumn<WageMonthResultDto, String> monthColumn;
    @FXML private TableColumn<WageMonthResultDto, String> rawTotalColumn;
    @FXML private TableColumn<WageMonthResultDto, String> ceilingColumn;
    @FXML private TableColumn<WageMonthResultDto, String> pensionableColumn;
    @FXML private TableColumn<WageMonthResultDto, String> ceilingAppliedColumn;
    @FXML private TableColumn<WageMonthResultDto, String> sourceColumn;
    @FXML private TableColumn<WageMonthResultDto, Void>   allowanceDetailsColumn;
    @FXML private TableColumn<WageMonthResultDto, Void>   documentDetailsColumn;

    // ── شريط التصدير ──
    @FXML private TextField fromField;
    @FXML private TextField toField;
    @FXML private Button    exportButton;
    @FXML private Label     exportStatusLabel;

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

    private String currentNationalId;

    // ══════════════════════════════════════════════════════
    //  Initialize
    // ══════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        // جدول الاحتساب
        monthColumn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().monthLabel()));
        rawTotalColumn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().rawTotal().toPlainString()));
        ceilingColumn.setCellValueFactory(c ->
                new SimpleStringProperty(
                        c.getValue().pensionCeiling() == null ? "—"
                                : c.getValue().pensionCeiling().toPlainString()));
        pensionableColumn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().totalPensionableWage().toPlainString()));
        ceilingAppliedColumn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().ceilingApplied() ? "مقطوع" : "—"));
        sourceColumn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().sourceLabel()));

        addAllowanceDetailsButton();
        addDocumentDetailsButton();

        calculateButton.setOnAction(e -> onCalculate());
        calculateButton.setDisable(true);

        // شريط التصدير
        exportButton.setOnAction(e -> onExport());
        exportButton.setDisable(true);
    }

    // ══════════════════════════════════════════════════════
    //  Public API — يُستدعى من الشاشة الأم
    // ══════════════════════════════════════════════════════

    public void setNationalId(String nationalId) {
        this.currentNationalId = nationalId;
        boolean hasId = nationalId != null;
        calculateButton.setDisable(!hasId);
        exportButton.setDisable(!hasId);
        table.setItems(FXCollections.observableArrayList());
        statusLabel.setText("");
        exportStatusLabel.setText("");
    }

    // ══════════════════════════════════════════════════════
    //  احتساب الدورة
    // ══════════════════════════════════════════════════════

    private void onCalculate() {
        if (currentNationalId == null) return;
        try {
            var response = ApiClient.get(
                    "/wages/engine/cycle?nationalId=" + currentNationalId,
                    WageMonthResultDto[].class);
            if (!response.isSuccess()) {
                statusLabel.setText("تعذر حساب الدورة: " + response.getMessage());
                return;
            }
            statusLabel.setText("");
            table.setItems(FXCollections.observableArrayList(List.of(response.getData())));
        } catch (Exception ex) {
            statusLabel.setText("تعذر حساب الدورة: " + ex.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════
    //  تصدير البطاقة
    // ══════════════════════════════════════════════════════

    private void onExport() {
        if (currentNationalId == null) return;

        // بناء الـ URL
        StringBuilder url = new StringBuilder("/wages/card/export?nationalId=")
                .append(currentNationalId);

        String fromText = fromField.getText() == null ? "" : fromField.getText().trim();
        String toText   = toField.getText()   == null ? "" : toField.getText().trim();

        if (!fromText.isEmpty()) {
            if (!isValidDate(fromText)) {
                setExportError("صيغة تاريخ «من» غير صحيحة (yyyy-MM-dd)");
                return;
            }
            url.append("&from=").append(fromText);
        }

        if (!toText.isEmpty()) {
            if (!isValidDate(toText)) {
                setExportError("صيغة تاريخ «إلى» غير صحيحة (yyyy-MM-dd)");
                return;
            }
            url.append("&to=").append(toText);
        }

        // اختيار مكان الحفظ
        FileChooser chooser = new FileChooser();
        chooser.setTitle("حفظ بطاقة الأجور المتغيرة");
        chooser.setInitialFileName("wage_card_" + currentNationalId + ".pdf");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File dest = chooser.showSaveDialog(exportButton.getScene().getWindow());
        if (dest == null) return;

        setExportInfo("جاري التصدير...");
        exportButton.setDisable(true);

        String finalUrl = url.toString();
        new Thread(() -> {
            try {
                // استخدام downloadBinary من ApiClient
                byte[] pdfBytes = ApiClient.downloadBinary(
                        finalUrl, Duration.ofMinutes(3));
                Files.write(dest.toPath(), pdfBytes);
                javafx.application.Platform.runLater(() -> {
                    setExportSuccess("تم الحفظ: " + dest.getName());
                    exportButton.setDisable(false);
                });
            } catch (Exception ex) {
                javafx.application.Platform.runLater(() -> {
                    setExportError("فشل التصدير: " + ex.getMessage());
                    exportButton.setDisable(false);
                });
            }
        }, "WageCardExport").start();
    }

    // ══════════════════════════════════════════════════════
    //  أزرار التفاصيل في الجدول
    // ══════════════════════════════════════════════════════

    private void addAllowanceDetailsButton() {
        allowanceDetailsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("تفاصيل البدلات");
            { btn.setOnAction(e -> {
                WageMonthResultDto row = getTableRow().getItem();
                if (row != null) CycleMonthDetailsController.openAllowances(row);
            }); }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : new HBox(btn));
            }
        });
    }

    private void addDocumentDetailsButton() {
        documentDetailsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("تفاصيل المستندات");
            { btn.setOnAction(e -> {
                WageMonthResultDto row = getTableRow().getItem();
                if (row != null) CycleMonthDetailsController.openDocuments(row);
            }); }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                WageMonthResultDto row = empty ? null : getTableRow().getItem();
                boolean hasDocs = row != null && !row.documentNotes().isEmpty();
                setGraphic(hasDocs ? new HBox(btn) : null);
            }
        });
    }

    // ══════════════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════════════

    private boolean isValidDate(String s) {
        try { LocalDate.parse(s, ISO); return true; }
        catch (DateTimeParseException e) { return false; }
    }

    private void setExportInfo(String msg) {
        exportStatusLabel.setStyle("-fx-text-fill: #555555;");
        exportStatusLabel.setText(msg);
    }

    private void setExportSuccess(String msg) {
        exportStatusLabel.setStyle("-fx-text-fill: #1a7a1a;");
        exportStatusLabel.setText(msg);
    }

    private void setExportError(String msg) {
        exportStatusLabel.setStyle("-fx-text-fill: #b00020;");
        exportStatusLabel.setText(msg);
    }
}
