package com.safwat.hr.payroll.table.engine.column;

import com.safwat.hr.payroll.table.engine.TableSchema;
import com.safwat.hr.payroll.table.engine.header.ColumnWidthAdjuster;
import com.safwat.hr.payroll.table.engine.header.DynamicHeaderManager;
import com.safwat.hr.payroll.table.engine.navigation.TableNavigationHandler;
import com.safwat.hr.payroll.table.engine.rows.RowManager;
import com.safwat.hr.payroll.table.engine.tooltip.NationalIdTooltipService;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.Callback;
import javafx.util.converter.DefaultStringConverter;

import java.util.function.BiConsumer;

/**
 * =====================================================
 * TableColumnFactory — بناء كل أعمدة جدول الإدخال
 * =====================================================
 * <p>مسؤولية وحيدة: تعريف شكل كل نوع عمود (بحث/مسلسل/ثابت/ديناميكي)
 * وسلوك خلاياه أثناء التحرير (Enter/Tab بيعمل commit فوري وينقل
 * للخلية التالية). أي عمود جديد مستقبلًا (مثلاً عمود قائمة منسدلة)
 * بيتضاف كـ method جديدة هنا بدل ما يتوه وسط منطق تنقل أو نسخ/لصق.</p>
 */
public class TableColumnFactory {

    private final TableNavigationHandler navigationHandler;
    private final DynamicHeaderManager dynamicHeaderManager;
    private final ColumnWidthAdjuster columnWidthAdjuster;
    private final CellStyleFormatter cellStyleFormatter;
    private final NationalIdTooltipService nationalIdTooltipService;
    private final RowManager rowManager;

    private BiConsumer<Integer, String> searchHandler;

    public TableColumnFactory(TableNavigationHandler navigationHandler,
                              DynamicHeaderManager dynamicHeaderManager,
                              ColumnWidthAdjuster columnWidthAdjuster,
                              CellStyleFormatter cellStyleFormatter,
                              NationalIdTooltipService nationalIdTooltipService,
                              RowManager rowManager) {
        this.navigationHandler = navigationHandler;
        this.dynamicHeaderManager = dynamicHeaderManager;
        this.columnWidthAdjuster = columnWidthAdjuster;
        this.cellStyleFormatter = cellStyleFormatter;
        this.nationalIdTooltipService = nationalIdTooltipService;
        this.rowManager = rowManager;
    }

    public void setSearchHandler(BiConsumer<Integer, String> searchHandler) {
        this.searchHandler = searchHandler;
    }

    private TableColumn<ObservableList<String>, String> baseColumn(String title, int index) {
        TableColumn<ObservableList<String>, String> col = new TableColumn<>(title);
        col.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().size() > index ? data.getValue().get(index) : ""));
        col.setEditable(true);
        col.setSortable(true);
        col.setReorderable(false);
        return col;
    }

    /**
     * خلية نصية "ذكية" — Enter/Tab أثناء التحرير يعمل commit فورًا
     * وينتقل يفتح تحرير الخلية التالية مباشرة. Tab يتحرك يمين (يسار
     * مع Shift) ويلف للصف التالي عند نهاية الأعمدة.
     */
    private TableCell<ObservableList<String>, String> navigableTextCell() {
        return new TextFieldTableCell<>(new DefaultStringConverter()) {
            @Override
            public void startEdit() {
                super.startEdit();
                if (getGraphic() instanceof TextField tf) {
                    tf.addEventFilter(KeyEvent.KEY_PRESSED, ke -> handleEnterTab(ke, tf, this));
                    Platform.runLater(tf::requestFocus);
                }
            }
        };
    }

    private void handleEnterTab(KeyEvent ke, TextField tf, TableCell<ObservableList<String>, String> cell) {
        if (ke.getCode() != KeyCode.ENTER && ke.getCode() != KeyCode.TAB) return;
        ke.consume();
        int row = cell.getIndex();
        int col = cell.getTableView().getColumns().indexOf(cell.getTableColumn());
        cell.commitEdit(tf.getText());
        Platform.runLater(() -> {
            if (ke.getCode() == KeyCode.ENTER) {
                if (navigationHandler.isMoveDown()) navigationHandler.moveTo(row + 1, col);
                else navigationHandler.moveTo(row, col + 1);
            } else {
                int nextCol = col + (ke.isShiftDown() ? -1 : 1);
                int nextRow = row;
                if (nextCol < 0) {
                    nextCol = TableSchema.COLUMN_COUNT - 1;
                    nextRow = row - 1;
                } else if (nextCol >= TableSchema.COLUMN_COUNT) {
                    nextCol = 0;
                    nextRow = row + 1;
                }
                if (nextRow < 0) nextRow = 0;
                navigationHandler.moveTo(nextRow, nextCol);
            }
        });
    }

    /**
     * نفس navigableTextCell بالظبط، لكن بدون تنقل Enter/Tab الذاتي —
     * عمود البحث بيتنقل أصلاً بعد وصول نتيجة البحث (moveAfterSearch
     * من الكونترولر).
     * <p>
     * ✅ الإصلاح المهم هنا: بدون {@code Platform.runLater(tf::requestFocus)}
     * كان التحرير "بيفتح" بصريًا (تظهر خانة الكتابة) لكن الفوكس الفعلي
     * كان بيفضل على TableView مش على TextField الداخلي، فالكتابة ما
     * كانتش بتشتغل.
     */
    private TableCell<ObservableList<String>, String> navigableSearchCell() {
        return new TextFieldTableCell<>(new DefaultStringConverter()) {
            @Override
            public void startEdit() {
                super.startEdit();
                if (getGraphic() instanceof TextField tf) {
                    Platform.runLater(tf::requestFocus);
                }
            }
        };
    }

    /**
     * عمود البحث — الإدخال هنا يشغّل اللوك أب
     */
    public TableColumn<ObservableList<String>, String> createSearchColumn() {
        TableColumn<ObservableList<String>, String> col = baseColumn(TableSchema.STATIC_TITLES[TableSchema.SEARCH_COL], TableSchema.SEARCH_COL);
        col.setPrefWidth(110);
        col.setCellFactory(c -> navigableSearchCell());
        col.setOnEditCommit(event -> {
            int row = event.getTablePosition().getRow();
            String value = event.getNewValue() == null ? "" : event.getNewValue().trim();
            event.getRowValue().set(TableSchema.SEARCH_COL, value);
            if (!value.isEmpty() && searchHandler != null) {
                searchHandler.accept(row, value);
            } else {
                navigationHandler.moveAfterSearch(row);
            }
        });
        return col;
    }

    /**
     * عمود المسلسل — أرقام فقط + ترتيب رقمي تلقائي
     */
    public TableColumn<ObservableList<String>, String> createSerialColumn() {
        TableColumn<ObservableList<String>, String> col = baseColumn(TableSchema.STATIC_TITLES[TableSchema.SERIAL_COL], TableSchema.SERIAL_COL);
        col.setPrefWidth(70);
        col.setComparator((a, b) -> Integer.compare(parseIntSafe(a), parseIntSafe(b)));
        col.setCellFactory(new Callback<>() {
            @Override
            public TableCell<ObservableList<String>, String> call(TableColumn<ObservableList<String>, String> param) {
                return new TextFieldTableCell<>(new DefaultStringConverter()) {
                    @Override
                    public void startEdit() {
                        super.startEdit();
                        if (getGraphic() instanceof TextField tf) {
                            tf.textProperty().addListener((obs, old, newVal) -> {
                                if (!newVal.matches("\\d*")) {
                                    tf.setText(newVal.replaceAll("[^\\d]", ""));
                                }
                            });
                        }
                    }
                };
            }
        });

        col.setOnEditCommit(event -> {
            String v = event.getNewValue() == null ? "" : event.getNewValue().trim();
            if (!v.isEmpty() && !v.matches("\\d+")) {
                event.getTableView().refresh();
                return;
            }
            event.getRowValue().set(TableSchema.SERIAL_COL, v);
            rowManager.sortBySerial();
        });
        return col;
    }

    private int parseIntSafe(String v) {
        try {
            return Integer.parseInt(v.trim());
        } catch (Exception e) {
            return Integer.MAX_VALUE;
        }
    }

    public TableColumn<ObservableList<String>, String> createStaticColumn(String title, int index, boolean editable) {
        TableColumn<ObservableList<String>, String> col = baseColumn(title, index);
        col.setPrefWidth(120);
        col.setEditable(editable);
        if (editable) {
            col.setCellFactory(c -> navigableTextCell());
            col.setOnEditCommit(event -> {
                event.getRowValue().set(index, event.getNewValue());
                Platform.runLater(columnWidthAdjuster::adjustColumnWidths);
            });
        }
        return col;
    }

    /**
     * خلية عمود الرقم القومي — نفس navigableTextCell (تنقل Enter/Tab
     * فوري) + تولتيب عند التمرير على قيمة غير فارغة يعرض السجل الكامل
     * من البيرول إندكس (عبر NationalIdTooltipService).
     */
    public TableCell<ObservableList<String>, String> nationalIdCell() {
        return new TextFieldTableCell<>(new DefaultStringConverter()) {
            private final Tooltip tooltip = nationalIdTooltipService.buildTooltip();

            @Override
            public void startEdit() {
                super.startEdit();
                if (getGraphic() instanceof TextField tf) {
                    tf.addEventFilter(KeyEvent.KEY_PRESSED, ke -> handleEnterTab(ke, tf, this));
                    Platform.runLater(tf::requestFocus);
                }
            }

            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) {
                    setTooltip(null);
                    setStyle("");
                    return;
                }

                setStyle(cellStyleFormatter.isDuplicateInColumn(TableSchema.NATIONAL_ID_COL, item)
                        ? "-fx-text-fill: red; -fx-font-weight: bold;"
                        : "");

                if (nationalIdTooltipService.isEnabled() && nationalIdTooltipService.hasProvider()) {
                    nationalIdTooltipService.attachAndLoad(tooltip, item);
                    setTooltip(tooltip);
                } else {
                    setTooltip(null);
                }
            }
        };
    }

    /**
     * عمود ديناميكي — رأسه نص عادي (col.setText) + تولتيب إحصائي على
     * الهيدر (اسم العمود / إجمالي الأرقام / متوسطها / إجمالي الصفوف /
     * الفارغة / غير الفارغة).
     * <p>
     * ⚠️ ملاحظة: كان هنا سابقاً هيدر مبني من Label(wrapText=true) جوه
     * StackPane كـ Graphic، مع إعادة إنشاء العمود بالكامل عند تحديث
     * الهيدرز. هذا التركيب هو سبب كراش JavaFX الداخلي
     * (ArrayIndexOutOfBoundsException في PrismTextLayout). الحل هنا:
     * هيدر نصي بسيط + Tooltip.install على Label شفاف صغير الحجم بدل
     * ما يبقى graphic العمود نفسه — بدون wrapText وبدون أي إعادة
     * إنشاء متكررة، فمفيش مخاطرة تعطل.
     */
    public TableColumn<ObservableList<String>, String> createDynamicColumn(int index) {
        TableColumn<ObservableList<String>, String> col =
                baseColumn("عنصر " + (index - TableSchema.FIRST_DYNAMIC_COL + 1), index);
        col.setPrefWidth(100);
        col.setMinWidth(70);

        Label headerLabel = new Label(col.getText());
        headerLabel.setWrapText(false);
        headerLabel.setMaxWidth(150);
        headerLabel.setStyle("-fx-font-weight: bold; -fx-text-overrun: ellipsis;");
        col.setGraphic(headerLabel);
        col.setText(null); // ← امنع تكرار النص
        dynamicHeaderManager.registerHeaderLabel(index, headerLabel);

        col.setCellFactory(c -> navigableTextCell());
        col.setOnEditCommit(event -> {
            event.getRowValue().set(index, event.getNewValue());
            Platform.runLater(columnWidthAdjuster::adjustColumnWidths);
        });

        return col;
    }
}