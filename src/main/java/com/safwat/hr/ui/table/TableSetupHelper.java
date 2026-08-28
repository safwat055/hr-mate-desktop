package com.safwat.hr.ui.table;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
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
    //  parseDate
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
    //  ColumnAlign
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
                            alert.setContentText("الصيغة المقبولة: yyyy-MM-dd أو dd/MM/yyyy\nمثال: 2024-03-15 أو 15/03/2024");
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

    // ═══════════════════════════════════════════════════════════════
    //  Cell Factories
    // ═══════════════════════════════════════════════════════════════

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

            // ── CellFactory ──
            if (cfg.editable()) {
                if (cfg.isDateColumn()) {
                    col.setCellFactory(c -> createDateCell(c, cfg.getter(), cfg.setter(), cfg.alignment()));
                } else {
                    col.setCellFactory(c -> new TextFieldTableCell<>(new StringConverter<>() {
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

            // ── تتبع العمود اللي هيملا المساحة ──
            if (cfg.fillRemaining()) {
                fillColumn = col;
            } else {
                fixedColumnsWidth += cfg.width();
            }
        }

        // ── ربط آخر عمود (أو العمود المحدد) بباقي عرض الجدول ──
        if (fillColumn != null) {
            fillColumn.prefWidthProperty().bind(
                    table.widthProperty()
                            .subtract(fixedColumnsWidth)
                            .subtract(11
                            ) // حدود الجدول الداخلية
            );
        }

        // ── ملء الصفوف الافتراضية ──
        for (int i = 0; i < defaultRows; i++) {
            table.getItems().add(rowFactory.get());
        }

        // ── تلوين الصفوف بالتناوب ──
        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                // setStyle((empty || item == null) ? "" :
                //   (getIndex() % 2 == 0 ? "-fx-background-color: #F9FAFB;" : "-fx-background-color: #FFFFFF;"));
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════
    //  setupGenericTable
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
            boolean isDateColumn,
            ColumnAlign alignment,
            boolean fillRemaining) {

        // 4 params
        public ColumnConfig(String title, double width,
                            Function<T, String> getter,
                            BiConsumer<T, String> setter) {
            this(title, width, getter, setter, true, false, ColumnAlign.CENTER, false);
        }

        // 5 params
        public ColumnConfig(String title, double width,
                            Function<T, String> getter,
                            BiConsumer<T, String> setter,
                            boolean editable) {
            this(title, width, getter, setter, editable, false, ColumnAlign.CENTER, false);
        }

        // 6 params
        public ColumnConfig(String title, double width,
                            Function<T, String> getter,
                            BiConsumer<T, String> setter,
                            boolean editable,
                            boolean isDateColumn) {
            this(title, width, getter, setter, editable, isDateColumn, ColumnAlign.CENTER, false);
        }

        // 7 params
        public ColumnConfig(String title, double width,
                            Function<T, String> getter,
                            BiConsumer<T, String> setter,
                            boolean editable,
                            boolean isDateColumn,
                            ColumnAlign alignment) {
            this(title, width, getter, setter, editable, isDateColumn, alignment, false);
        }
    }
}