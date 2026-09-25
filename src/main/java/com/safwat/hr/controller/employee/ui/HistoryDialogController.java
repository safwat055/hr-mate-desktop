package com.safwat.hr.controller.employee.ui;

import com.safwat.hr.controller.employee.dto.EmployeeProfileDto;
import com.safwat.hr.controller.employee.dto.JobTitleEntryDto;
import com.safwat.hr.controller.employee.dto.JobTitleEntryRequest;
import com.safwat.hr.controller.employee.dto.SocialStatusEntryDto;
import com.safwat.hr.controller.employee.dto.SocialStatusEntryRequest;
import com.safwat.hr.controller.employee.enums.SocialStatus;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

/**
 * كنترولر عام لبوباب سجل التغييرات، يستخدم لكل من الحالة الاجتماعية والوظيفة.
 * كل جانب بيمرر Strategy صغيرة تحدد: عنوان البوباب، تحميل الصفوف، بناء ComboBox
 * اختيار القيمة، واستدعاءات الـ API المناسبة (إضافة/تعديل/حذف).
 */
public class HistoryDialogController {

    public interface Strategy<V> {
        String title();

        List<Row<V>> loadRows();

        ComboBox<V> buildValueCombo();

        EmployeeProfileDto add(LocalDate date, V value) throws Exception;

        EmployeeProfileDto update(LocalDate oldDate, LocalDate newDate, V value) throws Exception;

        EmployeeProfileDto delete(LocalDate date) throws Exception;
    }

    public record Row<V>(LocalDate date, V value, String label) {
    }

    @FXML private Label titleLabel;
    @FXML private TableView<Row<Object>> table;
    @FXML private TableColumn<Row<Object>, String> dateColumn;
    @FXML private TableColumn<Row<Object>, String> valueColumn;
    @FXML private TableColumn<Row<Object>, Void> actionsColumn;
    @FXML private DatePicker newDatePicker;
    @FXML private HBox newValueContainer;
    @FXML private Button addButton;

    private Strategy<Object> strategy;
    private Consumer<EmployeeProfileDto> onChanged;
    private ComboBox<Object> newValueCombo;

    @FXML
    public void initialize() {
        dateColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().date().toString()));
        valueColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().label()));
        addActionButtons();
    }

    @SuppressWarnings("unchecked")
    private void setup(Strategy<?> strategy, Consumer<EmployeeProfileDto> onChanged) {
        this.strategy = (Strategy<Object>) strategy;
        this.onChanged = onChanged;
        titleLabel.setText(this.strategy.title());
        newValueCombo = this.strategy.buildValueCombo();
        newValueContainer.getChildren().setAll(newValueCombo);
        addButton.setOnAction(e -> onAdd());
        refresh();
    }

    private void refresh() {
        ObservableList<Row<Object>> rows = FXCollections.observableArrayList(strategy.loadRows());
        table.setItems(rows);
    }

    private void onAdd() {
        LocalDate date = newDatePicker.getValue();
        Object value = newValueCombo.getValue();
        if (date == null || value == null) {
            new Alert(Alert.AlertType.WARNING, "اختار التاريخ والقيمة الأول").showAndWait();
            return;
        }
        try {
            EmployeeProfileDto updated = strategy.add(date, value);
            onChanged.accept(updated);
            newDatePicker.setValue(null);
            newValueCombo.setValue(null);
            refresh();
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "تعذرت الإضافة: " + ex.getMessage()).showAndWait();
        }
    }

    private void addActionButtons() {
        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("تعديل");
            private final Button delBtn = new Button("حذف");
            private final HBox box = new HBox(6, editBtn, delBtn);

            {
                editBtn.setOnAction(e -> onEdit(getTableRow().getItem()));
                delBtn.setOnAction(e -> onDelete(getTableRow().getItem()));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void onEdit(Row<Object> row) {
        if (row == null) return;
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("تعديل السجل");
        DatePicker datePicker = new DatePicker(row.date());
        ComboBox<Object> valueCombo = strategy.buildValueCombo();
        valueCombo.setValue(row.value());
        HBox content = new HBox(10, new Label("التاريخ:"), datePicker, new Label("القيمة:"), valueCombo);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().filter(bt -> bt == ButtonType.OK).ifPresent(bt -> {
            try {
                EmployeeProfileDto updated = strategy.update(row.date(), datePicker.getValue(), valueCombo.getValue());
                onChanged.accept(updated);
                refresh();
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "تعذر التعديل: " + ex.getMessage()).showAndWait();
            }
        });
    }

    private void onDelete(Row<Object> row) {
        if (row == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "حذف السجل بتاريخ " + row.date() + "؟ الفترة اللي بعده هترجع تاخد القيمة اللي قبله.",
                ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().filter(bt -> bt == ButtonType.YES).ifPresent(bt -> {
            try {
                EmployeeProfileDto updated = strategy.delete(row.date());
                onChanged.accept(updated);
                refresh();
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "تعذر الحذف: " + ex.getMessage()).showAndWait();
            }
        });
    }

    // ---------- نقاط الدخول العامة ----------

    public static void openSocialStatus(Long employeeId, List<SocialStatusEntryDto> current,
                                        Consumer<EmployeeProfileDto> onChanged) {
        ApiClient api = new ApiClient();
        Strategy<SocialStatus> strategy = new Strategy<>() {
            @Override
            public String title() {
                return "سجل الحالة الاجتماعية";
            }

            @Override
            public List<Row<SocialStatus>> loadRows() {
                return current.stream()
                        .map(e -> new Row<>(e.effectiveFrom(), SocialStatus.valueOf(e.code()), e.labelAr()))
                        .toList();
            }

            @Override
            public ComboBox<SocialStatus> buildValueCombo() {
                ComboBox<SocialStatus> combo = new ComboBox<>(FXCollections.observableArrayList(SocialStatus.values()));
                combo.setCellFactory(cb -> statusCell());
                combo.setButtonCell(statusCell());
                return combo;
            }

            @Override
            public EmployeeProfileDto add(LocalDate date, SocialStatus value) throws Exception {
                return api.post("/employees/" + employeeId + "/social-status",
                        new SocialStatusEntryRequest(date, value), EmployeeProfileDto.class);
            }

            @Override
            public EmployeeProfileDto update(LocalDate oldDate, LocalDate newDate, SocialStatus value) throws Exception {
                return api.put("/employees/" + employeeId + "/social-status/" + oldDate,
                        new SocialStatusEntryRequest(newDate, value), EmployeeProfileDto.class);
            }

            @Override
            public EmployeeProfileDto delete(LocalDate date) throws Exception {
                api.delete("/employees/" + employeeId + "/social-status/" + date);
                return api.get("/employees/" + employeeId, EmployeeProfileDto.class);
            }
        };
        open(strategy, onChanged);
    }

    public static void openJobTitle(Long employeeId, List<JobTitleEntryDto> current, Long sectorId,
                                    Consumer<EmployeeProfileDto> onChanged) {
        ApiClient api = new ApiClient();
        Strategy<JobTitleOption> strategy = new Strategy<>() {
            @Override
            public String title() {
                return "سجل الوظائف";
            }

            @Override
            public List<Row<JobTitleOption>> loadRows() {
                return current.stream()
                        .map(e -> new Row<>(e.effectiveFrom(),
                                new JobTitleOption(e.jobTitleId(), e.jobTitleNameAr()), e.jobTitleNameAr()))
                        .toList();
            }

            @Override
            public ComboBox<JobTitleOption> buildValueCombo() {
                ComboBox<JobTitleOption> combo = new ComboBox<>();
                try {
                    JobTitleOption[] options = api.get(
                            "/job-titles?sectorId=" + sectorId + "&active=true", JobTitleOption[].class);
                    combo.getItems().setAll(options);
                } catch (Exception ex) {
                    new Alert(Alert.AlertType.ERROR, "تعذر تحميل وظائف القطاع: " + ex.getMessage()).showAndWait();
                }
                return combo;
            }

            @Override
            public EmployeeProfileDto add(LocalDate date, JobTitleOption value) throws Exception {
                return api.post("/employees/" + employeeId + "/job-title",
                        new JobTitleEntryRequest(date, value.id()), EmployeeProfileDto.class);
            }

            @Override
            public EmployeeProfileDto update(LocalDate oldDate, LocalDate newDate, JobTitleOption value) throws Exception {
                return api.put("/employees/" + employeeId + "/job-title/" + oldDate,
                        new JobTitleEntryRequest(newDate, value.id()), EmployeeProfileDto.class);
            }

            @Override
            public EmployeeProfileDto delete(LocalDate date) throws Exception {
                api.delete("/employees/" + employeeId + "/job-title/" + date);
                return api.get("/employees/" + employeeId, EmployeeProfileDto.class);
            }
        };
        open(strategy, onChanged);
    }

    /** يطابق JobTitleOptionDto في الباك (id, code, nameAr) - الحقول الزيادة يتجاهلها الـ deserializer */
    public record JobTitleOption(Long id, String nameAr) {
        @Override
        public String toString() {
            return nameAr;
        }
    }

    private static ListCell<SocialStatus> statusCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(SocialStatus item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getLabelAr());
            }
        };
    }

    private static <V> void open(Strategy<V> strategy, Consumer<EmployeeProfileDto> onChanged) {
        try {
            FXMLLoader loader = new FXMLLoader(HistoryDialogController.class.getResource("/fxml/HistoryDialog.fxml"));
            Parent root = loader.load();
            HistoryDialogController controller = loader.getController();
            controller.setup(strategy, onChanged);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(strategy.title());
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            throw new RuntimeException("تعذر فتح نافذة السجل", e);
        }
    }
}