package com.safwat.hr.ui.table;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.StringConverter;
import lombok.Getter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * مساعد عام لإعداد الجداول القابلة للتحرير في JavaFX.
 *
 * <p>يدعم 3 أنواع من الـ editors:
 * <ul>
 *   <li>{@link EditorType#TEXT} — TextFieldTableCell</li>
 *   <li>{@link EditorType#DATE} — TextFieldTableCell مع parse/format للتواريخ</li>
 *   <li>{@link EditorType#COMBO} — ComboBoxTableCell مع قائمة خيارات ثابتة</li>
 * </ul>
 */
public class TableSetupHelper {

    private static final DateTimeFormatter[] INPUT_DATE_FORMATS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd-MM-yy"),
            DateTimeFormatter.ofPattern("yy-MM-dd"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("d-M-yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
    };

    private static final DateTimeFormatter OUTPUT_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ═══════════════════════════════════════════════════════════════
    //  parseDate / formatDate
    // ═══════════════════════════════════════════════════════════════

    public static LocalDate parseDateInput(String text) {
        if (text == null || text.isBlank()) return null;
        text = text.trim();
        for (DateTimeFormatter fmt : INPUT_DATE_FORMATS) {
            try {
                return LocalDate.parse(text, fmt);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    public static String formatDateOutput(LocalDate date) {
        return date != null ? date.format(OUTPUT_DATE_FORMAT) : "";
    }

    // ═══════════════════════════════════════════════════════════════
    //  EditorType + ComboOption — جديد
    // ═══════════════════════════════════════════════════════════════

    public enum EditorType { TEXT, DATE, COMBO }

    /**
     * خيار في ComboBox داخل خلية جدول.
     * <ul>
     *   <li>{@link #value()} — القيمة اللي تُخزَّن في الصف (String)</li>
     *   <li>{@link #display()} — النص اللي يظهر للمستخدم في القائمة</li>
     * </ul>
     */
    public interface ComboOption {
        String value();
        String display();

        static ComboOption of(String value, String display) {
            return new ComboOption() {
                @Override public String value()   { return value; }
                @Override public String display() { return display; }
            };
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Cell factories
    // ═══════════════════════════════════════════════════════════════

    private static <T> TableCell<T, String> createDateCell(
            TableColumn<T, String> column,
            Function<T, String> getter,
            BiConsumer<T, String> setter,
            ColumnAlign align) {

        return new TextFieldTableCell<>(new StringConverter<>() {
            @Override
            public String toString(String object) {
                return object != null ? object : "";
            }

            @Override
            public String fromString(String string) {
                return string;
            }
        }) {
            {
                setPadding(new Insets(5));
                setAlignment(align.getPos());
            }

            @Override
            public void commitEdit(String newValue) {
                if (isEditing()) {
                    if (newValue == null || newValue.isBlank()) {
                        super.commitEdit("");
                        return;
                    }
                    LocalDate parsed = parseDateInput(newValue);
                    if (parsed != null) {
                        super.commitEdit(formatDateOutput(parsed));
                    } else {
                        cancelEdit();
                        Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.WARNING);
                            alert.setTitle("تاريخ غير صالح");
                            alert.setHeaderText(null);
                            alert.setContentText(
                                    "الصيغة المقبولة: yyyy-MM-dd أو dd/MM/yyyy\n" +
                                            "مثال: 2024-03-15 أو 15/03/2024");
                            alert.show();
                        });
                    }
                }
            }

            @Override
            public void startEdit() {
                super.startEdit();
                if (getGraphic() instanceof TextField tf) {
                    Platform.runLater(() -> {
                        tf.selectAll();
                        tf.requestFocus();
                    });
                }
            }
        };
    }

    /**
     * خلية ComboBox — القيمة المخزّنة String، والعرض عبر ComboOption.display().
     */
    private static <T> TableCell<T, String> createComboCell(
            List<ComboOption> options,
            ColumnAlign align) {

        ObservableList<String> values = FXCollections.observableArrayList(
                options.stream().map(ComboOption::value).toList());

        StringConverter<String> converter = new StringConverter<>() {
            @Override
            public String toString(String v) {
                if (v == null) return "";
                return options.stream()
                        .filter(o -> o.value().equals(v))
                        .map(ComboOption::display)
                        .findFirst()
                        .orElse(v);
            }

            @Override
            public String fromString(String s) {
                if (s == null) return null;
                return options.stream()
                        .filter(o -> o.display().equals(s))
                        .map(ComboOption::value)
                        .findFirst()
                        .orElse(s);
            }
        };

        ComboBoxTableCell<T, String> cell = new ComboBoxTableCell<>(converter, values);
        cell.setPadding(new Insets(5));
        cell.setAlignment(align.getPos());
        return cell;
    }

    private static <T> TableCell<T, String> createAlignedCell(ColumnAlign align) {
        return new TableCell<>() {
            {
                setPadding(new Insets(5));
                setAlignment(align.getPos());
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : item);
            }
        };
    }

    // ═══════════════════════════════════════════════════════════════
    //  setupGenericTable
    // ═══════════════════════════════════════════════════════════════

    public static <T> void setupGenericTable(TableView<T> table,
                                             List<ColumnConfig<T>> columns,
                                             int defaultRows,
                                             Supplier<T> rowFactory) {

        table.getColumns().clear();
        table.getItems().clear();
        table.setEditable(true);
        table.getSelectionModel().setCellSelectionEnabled(true);

        TableColumn<T, String> fillColumn = null;
        double fixedColumnsWidth = 0;

        for (ColumnConfig<T> cfg : columns) {
            TableColumn<T, String> col = new TableColumn<>(cfg.title());
            col.setPrefWidth(cfg.width());
            col.setEditable(cfg.editable());
            col.setSortable(false);
            col.setResizable(true);

            // ── عرض القيمة ──
            col.setCellValueFactory(cellData ->
                    new javafx.beans.property.SimpleStringProperty(
                            cfg.getter().apply(cellData.getValue())));

            // ── resolved combo options مرة واحدة قبل بناء الـ cell factory ──
            final List<ComboOption> comboOptions =
                    cfg.comboOptions() != null ? cfg.comboOptions().get() : null;

            // ── CellFactory ──
            if (cfg.editable()) {
                switch (cfg.editorType()) {
                    case DATE -> col.setCellFactory(c ->
                            createDateCell(c, cfg.getter(), cfg.setter(), cfg.alignment()));

                    case COMBO -> {
                        if (comboOptions == null) {
                            throw new IllegalStateException(
                                    "العمود \"" + cfg.title() + "\" من نوع COMBO لازم يكون فيه comboOptions supplier");
                        }
                        col.setCellFactory(c -> createComboCell(comboOptions, cfg.alignment()));
                    }

                    default -> col.setCellFactory(c -> new TextFieldTableCell<>(new StringConverter<>() {
                        @Override
                        public String toString(String object) {
                            return object != null ? object : "";
                        }

                        @Override
                        public String fromString(String string) {
                            return string;
                        }
                    }) {
                        {
                            setPadding(new Insets(5));
                            setAlignment(cfg.alignment().getPos());
                        }
                    });
                }

                col.setOnEditCommit(event -> {
                    T row = event.getRowValue();
                    cfg.setter().accept(row, event.getNewValue());

                    int rowIdx = table.getItems().indexOf(row);
                    if (rowIdx == table.getItems().size() - 1) {
                        T fresh = rowFactory.get();
                        table.getItems().add(fresh);
                        Platform.runLater(() -> {
                            table.getSelectionModel().select(fresh);
                            table.scrollTo(fresh);
                            for (TableColumn<T, ?> c : table.getColumns()) {
                                if (c.isEditable()) {
                                    table.edit(table.getItems().size() - 1, c);
                                    break;
                                }
                            }
                        });
                    }
                });
            } else {
                col.setCellFactory(c -> createAlignedCell(cfg.alignment()));
            }

            table.getColumns().add(col);

            if (cfg.fillRemaining()) {
                fillColumn = col;
            } else {
                fixedColumnsWidth += cfg.width();
            }
        }

        if (fillColumn != null) {
            fillColumn.prefWidthProperty().bind(
                    table.widthProperty()
                            .subtract(fixedColumnsWidth)
                            .subtract(11));
        }

        for (int i = 0; i < defaultRows; i++) {
            table.getItems().add(rowFactory.get());
        }

        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════
    //  ColumnAlign
    // ═══════════════════════════════════════════════════════════════

    @Getter
    public enum ColumnAlign {
        LEFT(Pos.CENTER_LEFT),
        CENTER(Pos.CENTER),
        RIGHT(Pos.CENTER_RIGHT);

        private final Pos pos;

        ColumnAlign(Pos pos) {
            this.pos = pos;
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  ColumnConfig
    // ═══════════════════════════════════════════════════════════════

    public record ColumnConfig<T>(
            String title,
            double width,
            Function<T, String> getter,
            BiConsumer<T, String> setter,
            boolean editable,
            EditorType editorType,
            ColumnAlign alignment,
            boolean fillRemaining,
            Supplier<List<ComboOption>> comboOptions) {

        // ── 4 params ──
        public ColumnConfig(String title, double width,
                            Function<T, String> getter,
                            BiConsumer<T, String> setter) {
            this(title, width, getter, setter, true, EditorType.TEXT,
                    ColumnAlign.CENTER, false, null);
        }

        // ── 5 params ──
        public ColumnConfig(String title, double width,
                            Function<T, String> getter,
                            BiConsumer<T, String> setter,
                            boolean editable) {
            this(title, width, getter, setter, editable, EditorType.TEXT,
                    ColumnAlign.CENTER, false, null);
        }

        // ── 6 params (editable + isDateColumn — backward compat) ──
        public ColumnConfig(String title, double width,
                            Function<T, String> getter,
                            BiConsumer<T, String> setter,
                            boolean editable,
                            boolean isDateColumn) {
            this(title, width, getter, setter, editable,
                    isDateColumn ? EditorType.DATE : EditorType.TEXT,
                    ColumnAlign.CENTER, false, null);
        }

        // ── 7 params (editable + isDateColumn + align — backward compat) ──
        public ColumnConfig(String title, double width,
                            Function<T, String> getter,
                            BiConsumer<T, String> setter,
                            boolean editable,
                            boolean isDateColumn,
                            ColumnAlign alignment) {
            this(title, width, getter, setter, editable,
                    isDateColumn ? EditorType.DATE : EditorType.TEXT,
                    alignment, false, null);
        }

        // ── 8 params (editable + isDateColumn + align + fillRemaining — backward compat) ──
        public ColumnConfig(String title, double width,
                            Function<T, String> getter,
                            BiConsumer<T, String> setter,
                            boolean editable,
                            boolean isDateColumn,
                            ColumnAlign alignment,
                            boolean fillRemaining) {
            this(title, width, getter, setter, editable,
                    isDateColumn ? EditorType.DATE : EditorType.TEXT,
                    alignment, fillRemaining, null);
        }

        // ── 8 params (EditorType + align + fillRemaining) ──
        public ColumnConfig(String title, double width,
                            Function<T, String> getter,
                            BiConsumer<T, String> setter,
                            boolean editable,
                            EditorType editorType,
                            ColumnAlign alignment,
                            boolean fillRemaining) {
            this(title, width, getter, setter, editable, editorType,
                    alignment, fillRemaining, null);
        }

        // ── 9 params (COMBO كامل) ──
        public ColumnConfig(String title, double width,
                            Function<T, String> getter,
                            BiConsumer<T, String> setter,
                            boolean editable,
                            EditorType editorType,
                            ColumnAlign alignment,
                            boolean fillRemaining,
                            Supplier<List<ComboOption>> comboOptions) {
            this.title = title;
            this.width = width;
            this.getter = getter;
            this.setter = setter;
            this.editable = editable;
            this.editorType = editorType;
            this.alignment = alignment;
            this.fillRemaining = fillRemaining;
            this.comboOptions = comboOptions;
        }
    }
}