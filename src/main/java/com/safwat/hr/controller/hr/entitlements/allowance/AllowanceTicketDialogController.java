package com.safwat.hr.controller.hr.entitlements.allowance;

import com.fasterxml.jackson.core.type.TypeReference;
import com.safwat.hr.ui.TextFieldSetupHelper;
import com.safwat.hr.ui.util.AlertUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller لـ Dialog فترات تذاكر السفر (TICKET_CASH).
 *
 * <p>الحفظ بيستبدل كل الفترات دفعة واحدة (PUT)، والتحقق بيتعمل في الفرونت
 * عشان الرسالة تظهر فوراً، والباك إند بيتحقق تاني كضمان.
 */
public class AllowanceTicketDialogController implements Initializable {

    private static final int MAX_PERSONS = 3;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ── Labels ──────────────────────────────────────────────────
    @FXML
    private Label lbl_dialog_title;
    @FXML
    private Label lbl_allowance_name;

    // ── جدول الفترات ─────────────────────────────────────────────
    @FXML
    private TableView<TicketPeriod> table_periods;
    @FXML
    private TableColumn<TicketPeriod, LocalDate> col_from;
    @FXML
    private TableColumn<TicketPeriod, LocalDate> col_to;
    @FXML
    private TableColumn<TicketPeriod, Integer> col_full;
    @FXML
    private TableColumn<TicketPeriod, Integer> col_half;
    @FXML
    private TableColumn<TicketPeriod, Integer> col_persons;
    @FXML
    private TableColumn<TicketPeriod, Void> col_actions;
    @FXML
    private Button btn_add_period;

    // ── فورم الفترة ──────────────────────────────────────────────
    @FXML
    private VBox pane_period_form;
    @FXML
    private Label lbl_form_title;
    @FXML
    private TextField txt_from;
    @FXML
    private TextField txt_to;
    @FXML
    private ComboBox<Integer> combo_full;
    @FXML
    private ComboBox<Integer> combo_half;
    @FXML
    private Label lbl_persons;
    @FXML
    private Label lbl_form_error;
    @FXML
    private Button btn_save_period;
    @FXML
    private Button btn_cancel_period;

    // ── أزرار رئيسية ─────────────────────────────────────────────
    @FXML
    private Button btn_save_all;
    @FXML
    private Button btn_close;

    // ── State ────────────────────────────────────────────────────
    private String allowanceCode;
    private String nationalId;
    private Runnable onSaved;
    private final ObservableList<TicketPeriod> periods = FXCollections.observableArrayList();
    private TicketPeriod editingPeriod = null;

    // ════════════════════════════════════════════════════════════
    //  Initialize
    // ════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        TextFieldSetupHelper.setupDateFields(txt_from, txt_to);

        List<Integer> counts = List.of(0, 1, 2, 3);
        combo_full.setItems(FXCollections.observableArrayList(counts));
        combo_half.setItems(FXCollections.observableArrayList(counts));
        combo_full.setValue(0);
        combo_half.setValue(0);
        combo_full.valueProperty().addListener((o, a, b) -> updatePersonsLabel());
        combo_half.valueProperty().addListener((o, a, b) -> updatePersonsLabel());
        updatePersonsLabel();

        setupTable();
        table_periods.setItems(periods);

        btn_add_period.setOnAction(e -> showForm(null));
        btn_save_period.setOnAction(e -> handleSavePeriod());
        btn_cancel_period.setOnAction(e -> hideForm());
        btn_save_all.setOnAction(e -> handleSaveAll());
        btn_close.setOnAction(e -> closeDialog());
    }

    /**
     * يُستدعى من AllowanceFxController بعد فتح الـ dialog.
     */
    public void init(String allowanceCode, String nameAr, String nationalId, Runnable onSaved) {
        this.allowanceCode = allowanceCode;
        this.nationalId = nationalId;
        this.onSaved = onSaved;

        lbl_dialog_title.setText("فترات التذاكر: " + nameAr);
        lbl_allowance_name.setText(nameAr);

        loadPeriods();
    }

    private void loadPeriods() {
        FxApiSupport.getList(
                "/entitlements/allowances/employee/" + nationalId + "/tickets/" + allowanceCode,
                new TypeReference<List<TicketPeriod>>() {
                },
                list -> {
                    periods.setAll(list);
                    sortPeriods();
                },
                err -> AlertUtil.showError("خطأ", "تعذر تحميل فترات التذاكر: " + err)
        );
    }
    /**
     * نفس منطق الباك إند: كل فترة مفتوحة أو متداخلة بتتقفل على يوم قبل اللي بعدها.
     */
    private List<TicketPeriod> autoCloseOverlaps(List<TicketPeriod> input) {
        List<TicketPeriod> sorted = new ArrayList<>(input);
        sorted.sort(Comparator.comparing(TicketPeriod::from));

        List<TicketPeriod> result = new ArrayList<>(sorted.size());
        for (int i = 0; i < sorted.size(); i++) {
            TicketPeriod curr = sorted.get(i);

            if (i + 1 < sorted.size()) {
                TicketPeriod next = sorted.get(i + 1);
                boolean needsClose = curr.to() == null || !curr.to().isBefore(next.from());

                if (needsClose) {
                    LocalDate autoTo = next.from().minusDays(1);
                    if (autoTo.isBefore(curr.from()))
                        throw new IllegalArgumentException(
                                "فترتين بيبدأوا في نفس التاريخ: " + curr.from().format(DATE_FMT));
                    curr = new TicketPeriod(curr.from(), autoTo,
                            curr.fullTickets(), curr.halfTickets());
                }
            }
            result.add(curr);
        }
        return result;
    }
    // ════════════════════════════════════════════════════════════
    //  Table
    // ════════════════════════════════════════════════════════════

    private void setupTable() {
        col_from.setCellValueFactory(d ->
                new javafx.beans.property.SimpleObjectProperty<>(d.getValue().from()));
        col_from.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty);
                setText(empty || d == null ? null : d.format(DATE_FMT));
                setStyle("-fx-alignment: CENTER;");
            }
        });

        col_to.setCellValueFactory(d ->
                new javafx.beans.property.SimpleObjectProperty<>(d.getValue().to()));
        col_to.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty);
                setText(empty ? null : d == null ? "مفتوح" : d.format(DATE_FMT));
                setStyle("-fx-alignment: CENTER;");
            }
        });

        col_full.setCellValueFactory(d ->
                new javafx.beans.property.SimpleObjectProperty<>(d.getValue().fullTickets()));
        col_full.setCellFactory(tc -> centeredIntCell());

        col_half.setCellValueFactory(d ->
                new javafx.beans.property.SimpleObjectProperty<>(d.getValue().halfTickets()));
        col_half.setCellFactory(tc -> centeredIntCell());

        col_persons.setCellValueFactory(d ->
                new javafx.beans.property.SimpleObjectProperty<>(
                        d.getValue().fullTickets() + d.getValue().halfTickets()));
        col_persons.setCellFactory(tc -> centeredIntCell());

        col_actions.setCellFactory(tc -> new TableCell<>() {
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
                btnDelete.setOnAction(e -> periods.remove(getTableView().getItems().get(getIndex())));
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

    private TableCell<TicketPeriod, Integer> centeredIntCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Integer v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.valueOf(v));
                setStyle("-fx-alignment: CENTER;");
            }
        };
    }

    // ════════════════════════════════════════════════════════════
    //  Form
    // ════════════════════════════════════════════════════════════

    private void showForm(TicketPeriod period) {
        editingPeriod = period;
        lbl_form_title.setText(period == null ? "إضافة فترة جديدة" : "تعديل فترة");
        lbl_form_error.setVisible(false);

        if (period != null) {
            txt_from.setText(period.from() != null ? period.from().format(DATE_FMT) : "");
            txt_to.setText(period.to() != null ? period.to().format(DATE_FMT) : "");
            combo_full.setValue(period.fullTickets());
            combo_half.setValue(period.halfTickets());
        } else {
            txt_from.clear();
            txt_to.clear();
            combo_full.setValue(0);
            combo_half.setValue(0);
        }
        updatePersonsLabel();

        pane_period_form.setVisible(true);
        pane_period_form.setManaged(true);
    }

    private void hideForm() {
        pane_period_form.setVisible(false);
        pane_period_form.setManaged(false);
        editingPeriod = null;
    }

    private void handleSavePeriod() {
        String fromText = txt_from.getText() == null ? "" : txt_from.getText().trim();
        String toText = txt_to.getText() == null ? "" : txt_to.getText().trim();

        if (fromText.isEmpty()) {
            showFormError("تاريخ البداية مطلوب");
            return;
        }
        LocalDate from = TextFieldSetupHelper.parseDateInput(fromText);
        if (from == null) {
            showFormError("صيغة تاريخ البداية غير صحيحة (yyyy-MM-dd)");
            return;
        }

        LocalDate to = null;
        if (!toText.isEmpty()) {
            to = TextFieldSetupHelper.parseDateInput(toText);
            if (to == null) {
                showFormError("صيغة تاريخ النهاية غير صحيحة (yyyy-MM-dd)");
                return;
            }
            if (to.isBefore(from)) {
                showFormError("تاريخ النهاية لازم يكون بعد البداية أو يساويها");
                return;
            }
        }

        int full = combo_full.getValue();
        int half = combo_half.getValue();
        if (full + half == 0) {
            showFormError("أدخل تذكرة واحدة على الأقل");
            return;
        }
        if (full + half > MAX_PERSONS) {
            showFormError("الحد الأقصى 3 أفراد (نصف التذكرة يحسب فرد)");
            return;
        }

        TicketPeriod newPeriod = new TicketPeriod(from, to, full, half);

        // مسودة: الفترات الحالية بدون الفترة اللي بنعدلها + الفترة الجديدة
        List<TicketPeriod> draft = new ArrayList<>(periods);
        if (editingPeriod != null) {
            draft.removeIf(p -> p == editingPeriod);
        }
        draft.add(newPeriod);

        // القفل التلقائي للفترات المتداخلة (نفس منطق الباك إند)
        List<TicketPeriod> normalized;
        try {
            normalized = autoCloseOverlaps(draft);
        } catch (IllegalArgumentException ex) {
            showFormError(ex.getMessage());
            return;
        }

        periods.setAll(normalized);
        hideForm();
    }

    /**
     * التداخل بالمقارنة بالمرجع (==) عشان الفترة نفسها ما تتحسبش تداخل مع نفسها.
     */
    private boolean hasOverlap(TicketPeriod newPeriod, TicketPeriod excluding) {
        for (TicketPeriod existing : periods) {
            if (existing == excluding) continue;
            boolean startsBeforeExistingEnds = existing.to() == null
                    || !newPeriod.from().isAfter(existing.to());
            boolean endsAfterExistingStarts = newPeriod.to() == null
                    || !newPeriod.to().isBefore(existing.from());
            if (startsBeforeExistingEnds && endsAfterExistingStarts) return true;
        }
        return false;
    }

    private void sortPeriods() {
        periods.sort((a, b) -> a.from().compareTo(b.from()));
    }

    private void updatePersonsLabel() {
        Integer full = combo_full.getValue();
        Integer half = combo_half.getValue();
        int total = (full == null ? 0 : full) + (half == null ? 0 : half);
        lbl_persons.setText(total + " / " + MAX_PERSONS);
        lbl_persons.setStyle(total > MAX_PERSONS
                ? "-fx-text-fill:#c62828; -fx-font-weight:bold;"
                : "-fx-text-fill:#1b5e20; -fx-font-weight:bold;");
    }

    // ════════════════════════════════════════════════════════════
    //  Save All
    // ════════════════════════════════════════════════════════════

    private void handleSaveAll() {
        if (periods.isEmpty()) {
            boolean confirmed = AlertUtil.showConfirmation("تأكيد",
                    "الحفظ بدون فترات هيصفّر تذاكر الموظف لهذا البدل. متأكد؟");
            if (!confirmed) return;
        }

        btn_save_all.setDisable(true);

        FxApiSupport.put(
                "/entitlements/allowances/employee/" + nationalId + "/tickets/" + allowanceCode,
                new TicketPeriodsRequest(new ArrayList<>(periods)),
                Boolean.class,
                saved -> {
                    btn_save_all.setDisable(false);
                    AlertUtil.showInfo("تم الحفظ", "تم حفظ فترات التذاكر بنجاح");
                    if (onSaved != null) onSaved.run();
                    closeDialog();
                },
                err -> {
                    btn_save_all.setDisable(false);
                    showFormError("خطأ في الحفظ: " + err);
                    AlertUtil.showError("فشل الحفظ", "لم يتم حفظ فترات التذاكر.\n" + err);
                }
        );
    }

    // ════════════════════════════════════════════════════════════
    //  Helpers
    // ════════════════════════════════════════════════════════════

    private void showFormError(String msg) {
        lbl_form_error.setText(msg);
        lbl_form_error.setVisible(true);
    }

    private void closeDialog() {
        ((Stage) btn_close.getScene().getWindow()).close();
    }
}