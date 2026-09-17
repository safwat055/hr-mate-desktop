package com.safwat.hr.controller.entitlements.allowance;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.ResourceBundle;

/**
 * Controller لـ Dialog تعديل فترات البدل اليدوية.
 *
 * <p>نداءات الشبكة كلها عن طريق {@link FxApiSupport}.
 */
public class AllowanceOverrideDialogController implements Initializable {

    // ── Labels ──────────────────────────────────────────────────
    @FXML
    private Label lbl_dialog_title;
    @FXML
    private Label lbl_allowance_name;
    @FXML
    private Label lbl_current_source;
    @FXML
    private Button btn_revert_auto;

    // ── جدول الفترات ─────────────────────────────────────────────
    @FXML
    private TableView<OverrideEntry> table_entries;
    @FXML
    private TableColumn<OverrideEntry, LocalDate> col_entry_from;
    @FXML
    private TableColumn<OverrideEntry, LocalDate> col_entry_to;
    @FXML
    private TableColumn<OverrideEntry, BigDecimal> col_entry_value;
    @FXML
    private TableColumn<OverrideEntry, Void> col_entry_actions;

    // ── فورم الفترة ──────────────────────────────────────────────
    @FXML
    private VBox pane_entry_form;
    @FXML
    private Label lbl_form_title;
    @FXML
    private DatePicker date_entry_from;
    @FXML
    private DatePicker date_entry_to;
    @FXML
    private TextField txt_entry_value;
    @FXML
    private Label lbl_form_error;
    @FXML
    private Button btn_save_entry;
    @FXML
    private Button btn_cancel_entry;
    @FXML
    private Button btn_add_entry;

    // ── أزرار رئيسية ─────────────────────────────────────────────
    @FXML
    private Button btn_save_all;
    @FXML
    private Button btn_close;

    // ── State ────────────────────────────────────────────────────
    private AllowanceResultDto.AllowanceLineDto currentLine;
    private String nationalId;
    private Runnable onSaved;
    private final ObservableList<OverrideEntry> entries = FXCollections.observableArrayList();
    private OverrideEntry editingEntry = null;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ════════════════════════════════════════════════════════════
    //  Initialize
    // ════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        btn_add_entry.setOnAction(e -> showForm(null));
        btn_save_entry.setOnAction(e -> handleSaveEntry());
        btn_cancel_entry.setOnAction(e -> hideForm());
        btn_save_all.setOnAction(e -> handleSaveAll());
        btn_close.setOnAction(e -> closeDialog());
        btn_revert_auto.setOnAction(e -> handleRevertAuto());
    }

    public void init(AllowanceResultDto.AllowanceLineDto line, String nationalId, Runnable onSaved) {
        this.currentLine = line;
        this.nationalId = nationalId;
        this.onSaved = onSaved;

        lbl_dialog_title.setText("تعديل فترات: " + line.nameAr());
        lbl_allowance_name.setText(line.nameAr());
        lbl_current_source.setText(sourceLabel(line.source()));
        lbl_current_source.setStyle(sourceLabelStyle(line.source()));

        btn_revert_auto.setVisible(
                line.source() == AllowanceResultDto.AllowanceLineDto.Source.MANUAL);

        if (line.overrides() != null) {
            entries.setAll(line.overrides());
        }
        table_entries.setItems(entries);
    }

    // ════════════════════════════════════════════════════════════
    //  Setup
    // ════════════════════════════════════════════════════════════

    private void setupTable() {
        col_entry_from.setCellValueFactory(d ->
                new javafx.beans.property.SimpleObjectProperty<>(d.getValue().from()));
        col_entry_from.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty);
                setText(empty || d == null ? null : d.format(DATE_FMT));
                setStyle("-fx-alignment: CENTER;");
            }
        });

        col_entry_to.setCellValueFactory(d ->
                new javafx.beans.property.SimpleObjectProperty<>(d.getValue().to()));
        col_entry_to.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty);
                setText(empty ? null : d == null ? "مفتوح" : d.format(DATE_FMT));
                setStyle("-fx-alignment: CENTER;");
            }
        });

        col_entry_value.setCellValueFactory(d ->
                new javafx.beans.property.SimpleObjectProperty<>(d.getValue().value()));
        col_entry_value.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : v.toPlainString() + " ج");
                setStyle("-fx-alignment: CENTER;");
            }
        });

        col_entry_actions.setCellFactory(tc -> new TableCell<>() {
            private final Button btnEdit = new Button("✏");
            private final Button btnDelete = new Button("🗑");

            {
                btnEdit.getStyleClass().add("btn-purple");
                btnEdit.setPrefHeight(26);
                btnEdit.setPrefWidth(36);
                btnDelete.getStyleClass().add("btn-danger");
                btnDelete.setPrefHeight(26);
                btnDelete.setPrefWidth(36);
                btnEdit.setOnAction(e -> showForm(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> entries.remove(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                HBox box = new HBox(6, btnEdit, btnDelete);
                box.setStyle("-fx-alignment: CENTER;");
                setGraphic(box);
            }
        });
    }

    // ════════════════════════════════════════════════════════════
    //  Form
    // ════════════════════════════════════════════════════════════

    private void showForm(OverrideEntry entry) {
        editingEntry = entry;
        lbl_form_title.setText(entry == null ? "إضافة فترة جديدة" : "تعديل فترة");
        lbl_form_error.setVisible(false);
        if (entry != null) {
            date_entry_from.setValue(entry.from());
            date_entry_to.setValue(entry.to());
            txt_entry_value.setText(entry.value() != null ? entry.value().toPlainString() : "");
        } else {
            date_entry_from.setValue(null);
            date_entry_to.setValue(null);
            txt_entry_value.clear();
        }
        pane_entry_form.setVisible(true);
        pane_entry_form.setManaged(true);
    }

    private void hideForm() {
        pane_entry_form.setVisible(false);
        pane_entry_form.setManaged(false);
        editingEntry = null;
    }

    private void handleSaveEntry() {
        LocalDate from = date_entry_from.getValue();
        LocalDate to = date_entry_to.getValue();
        String valueStr = txt_entry_value.getText().trim();

        if (from == null) {
            showFormError("تاريخ البداية مطلوب");
            return;
        }
        if (valueStr.isBlank()) {
            showFormError("القيمة مطلوبة");
            return;
        }
        if (to != null && !to.isAfter(from)) {
            showFormError("تاريخ النهاية يجب أن يكون بعد تاريخ البداية");
            return;
        }

        BigDecimal value;
        try {
            value = new BigDecimal(valueStr);
            if (value.compareTo(BigDecimal.ZERO) < 0) {
                showFormError("القيمة يجب أن تكون موجبة");
                return;
            }
        } catch (NumberFormatException ex) {
            showFormError("القيمة يجب أن تكون رقماً");
            return;
        }

        OverrideEntry newEntry = new OverrideEntry(from, to, value);
        if (hasOverlap(newEntry, editingEntry)) {
            showFormError("هذه الفترة تتداخل مع فترة موجودة");
            return;
        }

        if (editingEntry != null) {
            int idx = entries.indexOf(editingEntry);
            entries.set(idx, newEntry);
        } else {
            entries.add(newEntry);
        }
        entries.sort((a, b) -> a.from().compareTo(b.from()));
        hideForm();
    }

    /**
     * 🆕 بنستخدم reference equality بدل equals عشان نتجنب مشكلة
     * الفترات المتطابقة تماماً.
     */
    private boolean hasOverlap(OverrideEntry newEntry, OverrideEntry excluding) {
        for (OverrideEntry existing : entries) {
            if (existing == excluding) continue;
            boolean startsBefore = existing.to() == null || !newEntry.from().isAfter(existing.to());
            boolean endsAfter = newEntry.to() == null || !newEntry.to().isBefore(existing.from());
            if (startsBefore && endsAfter) return true;
        }
        return false;
    }

    // ════════════════════════════════════════════════════════════
    //  Save All — 🆕 path محدّث
    // ════════════════════════════════════════════════════════════

    private void handleSaveAll() {
        if (entries.isEmpty()) {
            showFormError("أضف فترة واحدة على الأقل");
            return;
        }

        AllowanceResultDto.OverrideRequest request = new AllowanceResultDto.OverrideRequest(
                currentLine.code(),
                new ArrayList<>(entries)
        );

        btn_save_all.setDisable(true);

        FxApiSupport.put(
                "/entitlements/allowances/employee/" + nationalId + "/override",   // 🆕
                request,
                Boolean.class,
                saved -> {
                    btn_save_all.setDisable(false);
                    if (onSaved != null) onSaved.run();
                    closeDialog();
                },
                err -> {
                    btn_save_all.setDisable(false);
                    showFormError("خطأ في الحفظ: " + err);
                }
        );
    }

    // ════════════════════════════════════════════════════════════
    //  Revert Auto — 🆕 path محدّث
    // ════════════════════════════════════════════════════════════

    private void handleRevertAuto() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "سيتم حذف كل الفترات اليدوية والرجوع للحساب التلقائي.",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("تأكيد");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                FxApiSupport.delete(
                        "/entitlements/allowances/employee/" + nationalId
                                + "/override/" + currentLine.code(),   // 🆕
                        () -> {
                            if (onSaved != null) onSaved.run();
                            closeDialog();
                        },
                        err -> showFormError("خطأ: " + err)
                );
            }
        });
    }

    // ════════════════════════════════════════════════════════════
    //  Helpers
    // ════════════════════════════════════════════════════════════

    private void closeDialog() {
        ((Stage) btn_close.getScene().getWindow()).close();
    }

    private void showFormError(String msg) {
        lbl_form_error.setText(msg);
        lbl_form_error.setVisible(true);
    }

    private String sourceLabel(AllowanceResultDto.AllowanceLineDto.Source src) {
        return switch (src) {
            case AUTO -> "تلقائي";
            case MANUAL -> "يدوي";
            case EXCLUDED -> "مستثنى";
        };
    }

    private String sourceLabelStyle(AllowanceResultDto.AllowanceLineDto.Source src) {
        return switch (src) {
            case AUTO -> "-fx-text-fill:#607d8b; -fx-font-weight:bold;";
            case MANUAL -> "-fx-text-fill:#1976d2; -fx-font-weight:bold;";
            case EXCLUDED -> "-fx-text-fill:#e65100; -fx-font-weight:bold;";
        };
    }
}