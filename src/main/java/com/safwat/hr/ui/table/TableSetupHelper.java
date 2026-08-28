package com.safwat.hr.ui.table;


import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.util.StringConverter;

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
 * <p>المميزات:</p>
 * <ul>
 *   <li>أي عدد أعمدة عبر {@link ColumnConfig}</li>
 *   <li>صفوف افتراضية (default rows)</li>
 *   <li>Enter في آخر صف → يضيف صف جديد + يبدأ تحريره</li>
 *   <li>دعم إدخال التاريخ بصيغ مختلفة → يتحول تلقائياً لـ yyyy-MM-dd</li>
 *   <li>تلوين الصفوف بالتناوب</li>
 * </ul>
 */
public class TableSetupHelper {

    // ── صيغ التاريخ المدعومة للإدخال ──
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
    //  ColumnConfig
    // ═══════════════════════════════════════════════════════════════

    /**
     * يحاول تحليل نص التاريخ بكل الصيغ المدعومة.
     *
     * @return LocalDate أو null لو فشل
     */
    public static LocalDate parseDateInput(String text) {
        if (text == null || text.isBlank()) return null;
        text = text.trim();
        for (DateTimeFormatter fmt : INPUT_DATE_FORMATS) {
            try {
                return LocalDate.parse(text, fmt);
            } catch (DateTimeParseException ignored) {
                // جرب الصيغة اللي بعدها
            }
        }
        return null;
    }

    // ═══════════════════════════════════════════════════════════════
    //  parseDate — يقبل صيغ متعددة
    // ═══════════════════════════════════════════════════════════════

    /**
     * يحوّل LocalDate لنص بالصيغة الموحدة yyyy-MM-dd.
     */
    public static String formatDateOutput(LocalDate date) {
        return date != null ? date.format(OUTPUT_DATE_FORMAT) : "";
    }

    /**
     * CellFactory مخصوص للتواريخ:
     * • يقبل أي صيغة مدخلة
     * • بعد التحرير يحوّلها لـ yyyy-MM-dd
     * • لو التاريخ غلط → يرجع للقيمة القديمة + alert
     */
    private static <T> TableCell<T, String> createDateCell(
            TableColumn<T, String> column,
            Function<T, String> getter,
            BiConsumer<T, String> setter) {

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
            @Override
            public void commitEdit(String newValue) {
                if (isEditing()) {
                    if (newValue == null || newValue.isBlank()) {
                        super.commitEdit("");
                        return;

                    }
                    LocalDate parsed = parseDateInput(newValue);
                    if (parsed != null) {
                        String formatted = formatDateOutput(parsed);
                        super.commitEdit(formatted);
                    } else {
                        // الغي التحرير وارجع للقيمة القديمة
                        cancelEdit();
                        Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.WARNING);
                            alert.setTitle("تاريخ غير صالح");
                            alert.setHeaderText(null);
                            alert.setContentText("الصيغة المقبولة: yyyy-MM-dd أو dd/MM/yyyy\n" +
                                    "مثال: 2024-03-15 أو 15/03/2024");
                            alert.show();
                        });
                    }
                }
            }

            @Override
            public void startEdit() {
                super.startEdit();
                // لما نبدأ التحرير، نخلي النص محدد بالكامل عشان يسهل الكتابة
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
    //  DateCellFactory — خلية تاريخ ذكية
    // ═══════════════════════════════════════════════════════════════

    /**
     * إعداد جدول عام قابل للتحرير.
     *
     * @param table       الـ TableView
     * @param columns     إعدادات الأعمدة
     * @param defaultRows عدد الصفوف الافتراضية الفارغة
     * @param rowFactory  Supplier ينشئ صف جديد فارغ
     */
    public static <T> void setupGenericTable(TableView<T> table,
                                             List<ColumnConfig<T>> columns,
                                             int defaultRows,
                                             Supplier<T> rowFactory) {

        table.getColumns().clear();
        table.getItems().clear();
        table.setEditable(true);
        table.getSelectionModel().setCellSelectionEnabled(true);

        for (ColumnConfig<T> cfg : columns) {
            TableColumn<T, String> col = new TableColumn<>(cfg.getTitle());
            col.setPrefWidth(cfg.getWidth());
            col.setEditable(cfg.isEditable());
            col.setSortable(false);
            col.setResizable(true);

            // ── عرض القيمة ──
            col.setCellValueFactory(cellData ->
                    new javafx.beans.property.SimpleStringProperty(
                            cfg.getGetter().apply(cellData.getValue())));

            // ── CellFactory ──
            if (cfg.isEditable()) {
                if (cfg.isDateColumn()) {
                    // عمود تاريخ → خلية ذكية
                    col.setCellFactory(c -> createDateCell(c, cfg.getGetter(), cfg.getSetter()));
                } else {
                    // عمود عادي → TextField عادي
                    col.setCellFactory(TextFieldTableCell.forTableColumn());
                }

                // ── بعد التحرير (commit) ──
                col.setOnEditCommit(event -> {
                    T row = event.getRowValue();
                    String newVal = event.getNewValue();

                    // حط القيمة في الـ object
                    cfg.getSetter().accept(row, newVal);

                    int rowIdx = table.getItems().indexOf(row);
                    int lastIdx = table.getItems().size() - 1;

                    // ── لو في آخر صف → ضيف صف جديد ──
                    if (rowIdx == lastIdx) {
                        T fresh = rowFactory.get();
                        table.getItems().add(fresh);

                        Platform.runLater(() -> {
                            table.getSelectionModel().select(fresh);
                            table.scrollTo(fresh);
                            // ابدأ تحرير أول عمود editable
                            for (TableColumn<T, ?> c : table.getColumns()) {
                                if (c.isEditable()) {
                                    table.edit(table.getItems().size() - 1, c);
                                    break;
                                }
                            }
                        });
                    }
                });
            }

            table.getColumns().add(col);
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
                if (empty || item == null) {
                    setStyle("");
                } else {
                    setStyle(getIndex() % 2 == 0
                            ? "-fx-background-color: #F9FAFB;"
                            : "-fx-background-color: #FFFFFF;");
                }
            }
        });

        // ── Enter يحرك للخلية اللي تحتها (Tab behavior) ──
        table.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                // سيب الـ default behavior يشتغل (commit + move)
                // لو المستخدم عايز Enter يضيف صف → ده بيحصل في OnEditCommit فوق
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════
    //  setupGenericTable — الميثود العامة
    // ═══════════════════════════════════════════════════════════════

    /**
     * إعدادات عمود واحد في الجدول العام.
     */
    public static class ColumnConfig<T> {
        private final String title;
        private final double width;
        private final Function<T, String> getter;
        private final BiConsumer<T, String> setter;
        private final boolean editable;
        private final boolean isDateColumn; // هل العمود ده تاريخ؟

        public ColumnConfig(String title, double width,
                            Function<T, String> getter,
                            BiConsumer<T, String> setter) {
            this(title, width, getter, setter, true, false);
        }

        public ColumnConfig(String title, double width,
                            Function<T, String> getter,
                            BiConsumer<T, String> setter,
                            boolean editable) {
            this(title, width, getter, setter, editable, false);
        }

        public ColumnConfig(String title, double width,
                            Function<T, String> getter,
                            BiConsumer<T, String> setter,
                            boolean editable,
                            boolean isDateColumn) {
            this.title = title;
            this.width = width;
            this.getter = getter;
            this.setter = setter;
            this.editable = editable;
            this.isDateColumn = isDateColumn;
        }

        public String getTitle() {
            return title;
        }

        public double getWidth() {
            return width;
        }

        public Function<T, String> getGetter() {
            return getter;
        }

        public BiConsumer<T, String> getSetter() {
            return setter;
        }

        public boolean isEditable() {
            return editable;
        }

        public boolean isDateColumn() {
            return isDateColumn;
        }
    }
}