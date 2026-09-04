package com.safwat.hr.payroll.table.engine;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.Callback;
import javafx.util.converter.DefaultStringConverter;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * =====================================================
 * ExcelEngine — محرك جدول يحاكي سلوك الإكسيل
 * =====================================================
 * <p>نقل/تطوير من ExcelLikeTableView القديم بنفس المزايا:</p>
 * <ul>
 *   <li>26 عموداً: بحث + مسلسل (رقمي بتحقق) + 5 أعمدة ثابتة (قومي/كود/اسم/حالة/فئة) + أعمدة ديناميكية</li>
 *   <li>تنقل Enter/Tab/أسهم مع تمرير السكرول تلقائياً واتجاه (أسفل/يمين) — يفتح التحرير فورًا في كل مرة</li>
 *   <li>عمود البحث: عند الإدخال يستدعي searchHandler ثم يتحرك حسب الاتجاه</li>
 *   <li>نسخ Ctrl+C / لصق Ctrl+V (يدعم لصق بلوك من إكسيل حقيقي TSV) / Delete للمسح</li>
 *   <li>تحديد خلايا حقيقي (Cell Selection) متعدد — مش تحديد صف بس</li>
 *   <li>قائمة سياقية: إضافة/إدراج/حذف صف + نسخ/لصق</li>
 *   <li>إدارة الصفوف والترقيم التسلسلي التلقائي (ترتيب رقمي)</li>
 *   <li>ضبط عرض الأعمدة تلقائياً من المحتوى (90 – 350)</li>
 *   <li>تمييز التكرارات (رقم قومي/كود/اسم) ديناميكيًا مع كل تعديل + تمييز حالة التعيين</li>
 *   <li>تعبئة الجدول من Map (قادم من الباك) وتصديره بنفس التنسيق</li>
 * </ul>
 */
public class ExcelEngine {

    /**
     * إجمالي عدد الأعمدة: بحث + مسلسل + 5 أعمدة ثابتة + 19 عمود ديناميكي
     */
    public static final int COLUMN_COUNT = 27;
    private static final int SEARCH_COL = 0;
    private static final int SERIAL_COL = 1;
    /**
     * أول عمود ديناميكي — بعد إضافة عمود "الفئة" الثابت
     */
    public static final int FIRST_DYNAMIC_COL = 7;
    private static final int DEFAULT_ROWS = 20;

    private static final String[] STATIC_TITLES =
            {"البحث", "المسلسل", "الرقم القومى", "رقم الموظف", "الاسم", "الحالة", "الفئة"};

    private final TableView<ObservableList<String>> tableView;

    private BiConsumer<Integer, String> searchHandler;
    private boolean moveDown = true;

    public ExcelEngine(TableView<ObservableList<String>> tableView) {
        this.tableView = tableView;
    }

    // ══════════════════════════════════════════════════════════
    //  التهيئة
    // ══════════════════════════════════════════════════════════

    public void initializeExcelFeatures() {
        tableView.setEditable(true);
        tableView.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        tableView.getColumns().clear();
        tableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        // ✅ تحديد خلايا حقيقي متعدد — بدونه التحديد بالماوس/كيبورد يتصرف كتحديد صف فقط
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tableView.getSelectionModel().setCellSelectionEnabled(true);

        for (int i = 0; i < COLUMN_COUNT; i++) {
            if (i == SEARCH_COL) {
                tableView.getColumns().add(createSearchColumn());
            } else if (i == SERIAL_COL) {
                tableView.getColumns().add(createSerialColumn());
            } else if (i < FIRST_DYNAMIC_COL) {
                tableView.getColumns().add(createStaticColumn(STATIC_TITLES[i], i));
            } else {
                tableView.getColumns().add(createDynamicColumn(i));
            }
        }

        setupNavigation();
        setupCopyPasteKeys();
        setupContextMenu();
        initializeDefaultRows();

        Platform.runLater(this::adjustColumnWidths);
    }

    // ══════════════════════════════════════════════════════════
    //  إنشاء الأعمدة
    // ══════════════════════════════════════════════════════════

    private TableColumn<ObservableList<String>, String> baseColumn(String title, int index) {
        TableColumn<ObservableList<String>, String> col = new TableColumn<>(title);
        col.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().size() > index ? data.getValue().get(index) : ""));
        col.setEditable(true);
        col.setSortable(false);
        col.setReorderable(false);
        return col;
    }

    /**
     * خلية نصية "ذكية" — Enter أو Tab أثناء التحرير يعمل commit للقيمة الحالية
     * فورًا وينتقل يفتح تحرير الخلية التالية مباشرة (بدون الحاجة لضغط Enter مرتين).
     */
    private TableCell<ObservableList<String>, String> navigableTextCell() {
        return new TextFieldTableCell<>(new DefaultStringConverter()) {
            @Override
            public void startEdit() {
                super.startEdit();
                if (getGraphic() instanceof TextField tf) {
                    tf.addEventFilter(KeyEvent.KEY_PRESSED, ke -> {
                        if (ke.getCode() == KeyCode.ENTER || ke.getCode() == KeyCode.TAB) {
                            ke.consume();
                            int row = getIndex();
                            int col = getTableView().getColumns().indexOf(getTableColumn());
                            commitEdit(tf.getText());
                            Platform.runLater(() -> {
                                if (moveDown) moveTo(row + 1, col);
                                else moveTo(row, col + 1);
                            });
                        }
                    });
                }
            }
        };
    }

    /**
     * عمود البحث — الإدخال هنا يشغّل اللوك أب
     */
    private TableColumn<ObservableList<String>, String> createSearchColumn() {
        TableColumn<ObservableList<String>, String> col = baseColumn(STATIC_TITLES[SEARCH_COL], SEARCH_COL);
        col.setPrefWidth(110);
        col.setCellFactory(c -> navigableSearchCell());
        col.setOnEditCommit(event -> {
            int row = event.getTablePosition().getRow();
            String value = event.getNewValue() == null ? "" : event.getNewValue().trim();
            event.getRowValue().set(SEARCH_COL, value);
            if (!value.isEmpty() && searchHandler != null) {
                searchHandler.accept(row, value);
            } else {
                moveAfterSearch(row);
            }
        });
        return col;
    }

    /**
     * نفس فكرة navigableTextCell لكن بدون تنقل ذاتي على Enter/Tab —
     * عمود البحث بيتنقل أصلاً بعد استلام نتيجة البحث (moveAfterSearch)،
     * فبنكتفي هنا بضمان إن commitEdit يحصل فورًا على أول Enter (سلوك افتراضي already).
     */
    private TableCell<ObservableList<String>, String> navigableSearchCell() {
        return new TextFieldTableCell<>(new DefaultStringConverter());
    }

    /**
     * عمود المسلسل — أرقام فقط + ترتيب رقمي تلقائي
     */
    private TableColumn<ObservableList<String>, String> createSerialColumn() {
        TableColumn<ObservableList<String>, String> col = baseColumn(STATIC_TITLES[SERIAL_COL], SERIAL_COL);
        col.setPrefWidth(70);

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
                tableView.refresh();
                return;
            }
            event.getRowValue().set(SERIAL_COL, v);
            sortBySerial();
        });
        return col;
    }

    private TableColumn<ObservableList<String>, String> createStaticColumn(String title, int index) {
        TableColumn<ObservableList<String>, String> col = baseColumn(title, index);
        col.setPrefWidth(120);
        col.setCellFactory(c -> navigableTextCell());
        col.setOnEditCommit(event -> {
            event.getRowValue().set(index, event.getNewValue());
            Platform.runLater(this::adjustColumnWidths);
        });
        return col;
    }

    /**
     * عمود ديناميكي — رأسه يتغير حسب مجموعة العناصر
     */
    private TableColumn<ObservableList<String>, String> createDynamicColumn(int index) {
        TableColumn<ObservableList<String>, String> col = baseColumn("عنصر " + (index - FIRST_DYNAMIC_COL + 1), index);
        col.setPrefWidth(100);
        col.setCellFactory(c -> navigableTextCell());
        col.setOnEditCommit(event -> {
            event.getRowValue().set(index, event.getNewValue());
            Platform.runLater(this::adjustColumnWidths);
        });
        return col;
    }

    // ══════════════════════════════════════════════════════════
    //  التنقل
    // ══════════════════════════════════════════════════════════

    private void setupNavigation() {
        tableView.setOnKeyPressed(this::handleKey);
    }

    private void handleKey(KeyEvent event) {
        boolean ctrl = event.isControlDown();
        KeyCode code = event.getCode();

        if (ctrl && code == KeyCode.C) {
            copySelectedCells();
            event.consume();
            return;
        }
        if (ctrl && code == KeyCode.V) {
            pasteFromClipboard();
            event.consume();
            return;
        }

        TablePosition<ObservableList<String>, ?> pos = tableView.getFocusModel().getFocusedCell();
        if (pos == null) return;

        switch (code) {
            case ENTER -> {
                event.consume();
                if (moveDown) moveTo(pos.getRow() + 1, pos.getColumn());
                else moveTo(pos.getRow(), pos.getColumn() + 1);
            }
            case TAB -> {
                event.consume();
                if (moveDown) moveTo(pos.getRow() + 1, pos.getColumn());
                else moveTo(pos.getRow(), pos.getColumn() + 1);
            }
            case LEFT -> {
                event.consume();
                moveTo(pos.getRow(), Math.max(0, pos.getColumn() - 1));
            }
            case RIGHT -> {
                event.consume();
                moveTo(pos.getRow(), Math.min(COLUMN_COUNT - 1, pos.getColumn() + 1));
            }
            case UP -> {
                event.consume();
                moveTo(Math.max(0, pos.getRow() - 1), pos.getColumn());
            }
            case DOWN -> {
                event.consume();
                moveTo(pos.getRow() + 1, pos.getColumn());
            }
            case DELETE -> {
                event.consume();
                clearSelectedCells();
            }
            default -> {
            }
        }
    }

    /**
     * انتقال مع تمرير السكرول والدخول في وضع التحرير.
     * <p>
     * ✅ فتح التحرير (edit) بيتأجل لآخر نبضة UI (Platform.runLater) بعد ما السكرول
     * والتحديد يستقرّوا — تنفيذه فورًا زي الأول كان بيفشل بصمت أحيانًا (خصوصًا
     * على صف جديد لسه بيتضاف أو عمود بره النطاق المرئي)، فيحتاج المستخدم يدوس
     * بالماوس عشان يبدأ التحرير فعليًا.
     */
    private void moveTo(int row, int col) {
        if (row >= tableView.getItems().size()) {
            addNewRow();
        }
        final int r = Math.min(row, tableView.getItems().size() - 1);
        final int c = Math.min(col, COLUMN_COUNT - 1);
        if (r < 0) return;

        tableView.scrollTo(Math.max(0, r - 8));
        scrollToColumn(c);

        Platform.runLater(() -> {
            TableColumn<ObservableList<String>, ?> column = tableView.getColumns().get(c);
            tableView.getFocusModel().focus(r, column);
            tableView.getSelectionModel().clearSelection();
            tableView.getSelectionModel().select(r, column);
            tableView.edit(r, column);
        });
    }

    private void scrollToColumn(int col) {
        TableColumn<ObservableList<String>, ?> column = tableView.getColumns().get(col);
        tableView.scrollToColumnIndex(Math.max(0, col - 3));
        tableView.scrollToColumn(column);
    }

    /**
     * الحركة بعد انتهاء البحث حسب الاتجاه المختار
     */
    public void moveAfterSearch(int currentRow) {
        if (moveDown) {
            moveTo(currentRow + 1, SEARCH_COL);
        } else {
            moveTo(currentRow, FIRST_DYNAMIC_COL);
        }
    }

    // ══════════════════════════════════════════════════════════
    //  النسخ واللصق
    // ══════════════════════════════════════════════════════════

    private void setupCopyPasteKeys() { /* مُعالج ضمن handleKey */ }

    private void copySelectedCells() {
        ObservableList<TablePosition> cells = tableView.getSelectionModel().getSelectedCells();
        if (cells.isEmpty()) return;

        Map<Integer, List<TablePosition>> byRow = new TreeMap<>();
        for (TablePosition p : cells) {
            byRow.computeIfAbsent(p.getRow(), r -> new ArrayList<>()).add(p);
        }

        StringBuilder sb = new StringBuilder();
        for (var entry : byRow.entrySet()) {
            List<TablePosition> rowCells = entry.getValue();
            rowCells.sort(Comparator.comparingInt(TablePosition::getColumn));
            for (int i = 0; i < rowCells.size(); i++) {
                TablePosition p = rowCells.get(i);
                String v = cellValue(p.getRow(), p.getColumn());
                sb.append(v == null ? "" : v);
                if (i < rowCells.size() - 1) sb.append('\t');
            }
            sb.append('\n');
        }

        ClipboardContent content = new ClipboardContent();
        content.putString(sb.toString());
        Clipboard.getSystemClipboard().setContent(content);
    }

    private void pasteFromClipboard() {
        String data = Clipboard.getSystemClipboard().getString();
        if (data == null || data.isEmpty()) return;

        TablePosition<ObservableList<String>, ?> pos = tableView.getFocusModel().getFocusedCell();
        int startRow = pos != null ? pos.getRow() : 0;
        int startCol = pos != null ? pos.getColumn() : 0;
        pasteDataFromString(data, startRow, startCol);
    }

    /**
     * لصق بلوك كامل (يدعم TSV القادم من إكسيل حقيقي)
     */
    public void pasteDataFromString(String data, int startRow, int startColumn) {
        String[] lines = data.split("\\R");
        for (int r = 0; r < lines.length; r++) {
            int row = startRow + r;
            if (row >= tableView.getItems().size()) addNewRow();
            String[] cells = lines[r].split("\\t", -1);
            for (int c = 0; c < cells.length; c++) {
                int col = startColumn + c;
                if (col < COLUMN_COUNT) {
                    updateCellValue(row, col, cells[c]);
                }
            }
        }
        updateSerialNumbers();
        adjustColumnWidths();
    }

    private void clearSelectedCells() {
        for (TablePosition p : tableView.getSelectionModel().getSelectedCells()) {
            updateCellValue(p.getRow(), p.getColumn(), "");
        }
        adjustColumnWidths();
    }

    private String cellValue(int row, int col) {
        if (row < 0 || row >= tableView.getItems().size()) return "";
        ObservableList<String> r = tableView.getItems().get(row);
        return (col < r.size()) ? r.get(col) : "";
    }

    public void updateCellValue(int row, int col, String value) {
        if (row < 0 || row >= tableView.getItems().size()) return;
        ObservableList<String> r = tableView.getItems().get(row);
        while (r.size() < COLUMN_COUNT) r.add("");
        r.set(col, value == null ? "" : value);
    }

    // ══════════════════════════════════════════════════════════
    //  القائمة السياقية
    // ══════════════════════════════════════════════════════════

    private void setupContextMenu() {
        ContextMenu menu = new ContextMenu();

        MenuItem addRow = new MenuItem("إضافة صف في الأسفل");
        addRow.setOnAction(e -> {
            insertRowBelow();
        });

        MenuItem copy = new MenuItem("نسخ");
        copy.setOnAction(e -> copySelectedCells());

        MenuItem paste = new MenuItem("لصق");
        paste.setOnAction(e -> pasteFromClipboard());

        MenuItem deleteRow = new MenuItem("حذف الصف الحالي");
        deleteRow.setOnAction(e -> deleteCurrentRow());

        menu.getItems().addAll(addRow, copy, paste, new SeparatorMenuItem(), deleteRow);
        tableView.setContextMenu(menu);
    }

    // ══════════════════════════════════════════════════════════
    //  إدارة الصفوف
    // ══════════════════════════════════════════════════════════

    private void initializeDefaultRows() {
        tableView.setItems(FXCollections.observableArrayList());
        addMultipleRows(DEFAULT_ROWS);
    }

    private ObservableList<String> newEmptyRow() {
        ObservableList<String> row = FXCollections.observableArrayList();
        for (int i = 0; i < COLUMN_COUNT; i++) row.add("");
        return row;
    }

    public void addNewRow() {
        tableView.getItems().add(newEmptyRow());
        updateSerialNumbers();
    }

    public void addMultipleRows(int count) {
        for (int i = 0; i < count; i++) {
            tableView.getItems().add(newEmptyRow());
        }
        updateSerialNumbers();
    }

    public void insertRowBelow() {
        int row = currentRow();
        if (row < 0) return;
        tableView.getItems().add(row + 1, newEmptyRow());
        updateSerialNumbers();
    }

    public void deleteCurrentRow() {
        int row = currentRow();
        if (row < 0) return;
        tableView.getItems().remove(row);
        updateSerialNumbers();
    }

    public void deleteRowAt(int row) {
        if (row >= 0 && row < tableView.getItems().size()) {
            tableView.getItems().remove(row);
            updateSerialNumbers();
        }
    }

    /**
     * تحديث ترقيم المسلسل حسب موضع الصف (فقط للخانات الفارغة منه)
     */
    public void updateSerialNumbers() {
        for (int i = 0; i < tableView.getItems().size(); i++) {
            ObservableList<String> row = tableView.getItems().get(i);
            if (row.size() > SERIAL_COL && row.get(SERIAL_COL).isBlank()) {
                row.set(SERIAL_COL, String.valueOf(i + 1));
            }
        }
    }

    /**
     * ترتيب الجدول رقمياً حسب عمود المسلسل
     */
    private void sortBySerial() {
        FXCollections.sort(tableView.getItems(), (a, b) -> {
            int x = parseInt(a.size() > SERIAL_COL ? a.get(SERIAL_COL) : "");
            int y = parseInt(b.size() > SERIAL_COL ? b.get(SERIAL_COL) : "");
            return Integer.compare(x, y);
        });
    }

    private int parseInt(String v) {
        try {
            return Integer.parseInt(v.trim());
        } catch (Exception e) {
            return Integer.MAX_VALUE;
        }
    }

    // ══════════════════════════════════════════════════════════
    //  رؤوس الأعمدة الديناميكية
    // ══════════════════════════════════════════════════════════

    /**
     * تحديث رؤوس الأعمدة الديناميكية (بعد عمود "الفئة" فصاعداً).
     * الأعمدة الثابتة لا تتغير أبداً.
     *
     * @param headers مصفوفة بحجم COLUMN_COUNT — الفهرس FIRST_DYNAMIC_COL+ هي الرؤوس الديناميكية
     */
    public void updateColumnHeaders(String[] headers) {
        for (int i = FIRST_DYNAMIC_COL; i < COLUMN_COUNT && i < headers.length; i++) {
            String h = headers[i];
            if (h != null && !h.isBlank()) {
                tableView.getColumns().get(i).setText(h);
            } else if (tableView.getColumns().get(i).getText().isBlank()
                    || tableView.getColumns().get(i).getText().startsWith("عنصر")) {
                tableView.getColumns().get(i).setText("عنصر " + (i - FIRST_DYNAMIC_COL + 1));
            }
        }
        quickRefresh();
    }

    /**
     * الرؤوس الحالية كاملة (للتصدير)
     */
    public List<String> getCurrentHeaders() {
        List<String> headers = new ArrayList<>();
        for (TableColumn<ObservableList<String>, ?> c : tableView.getColumns()) {
            headers.add(c.getText());
        }
        return headers;
    }

    // ══════════════════════════════════════════════════════════
    //  ضبط العرض
    // ══════════════════════════════════════════════════════════

    public void adjustColumnWidths() {
        for (int i = 0; i < tableView.getColumns().size(); i++) {
            TableColumn<ObservableList<String>, ?> col = tableView.getColumns().get(i);
            double width = computeColumnWidth(col, i);
            col.setPrefWidth(width);
            col.setMinWidth(width);
        }
        tableView.refresh();
    }

    private double computeColumnWidth(TableColumn<ObservableList<String>, ?> column, int colIndex) {
        double max = 60;
        String header = column.getText() == null ? "" : column.getText();
        max = Math.max(max, header.length() * 9.0 + 30);

        int limit = Math.min(tableView.getItems().size(), 100);
        for (int r = 0; r < limit; r++) {
            String v = cellValue(r, colIndex);
            if (v != null) {
                max = Math.max(max, v.length() * 8.5 + 30);
            }
        }
        return Math.min(Math.max(max, 90), 350);
    }

    // ══════════════════════════════════════════════════════════
    //  التحديث والمسح
    // ══════════════════════════════════════════════════════════

    public void quickRefresh() {
        tableView.refresh();
        Platform.runLater(this::adjustColumnWidths);
    }

    public void clearTable() {
        tableView.getItems().clear();
        addMultipleRows(DEFAULT_ROWS);
    }

    /**
     * حذف الصفوف الفارغة (كل خلاياها فارغة)
     */
    public void deleteEmptyRows() {
        tableView.getItems().removeIf(row -> {
            for (int i = 0; i < row.size(); i++) {
                if (i == SERIAL_COL) continue; // المسلسل لا يُعتبر بيانات
                String v = row.get(i);
                if (v != null && !v.isBlank()) return false;
            }
            return true;
        });
        if (tableView.getItems().size() < DEFAULT_ROWS) {
            addMultipleRows(DEFAULT_ROWS - tableView.getItems().size());
        }
        updateSerialNumbers();
    }

    // ══════════════════════════════════════════════════════════
    //  التمييز اللوني
    // ══════════════════════════════════════════════════════════

    /**
     * تمييز القيم المكررة في عمود (رقم قومي/كود/اسم) — ✅ يُعاد حساب المكرر
     * من بيانات الجدول الحالية في كل مرة تُرسم فيها الخلية (updateItem)، مش
     * مرة واحدة وقت التهيئة. قبل كده كان التمييز بيتجمّد على حالة الجدول
     * الفارغة الأولى ولا يتحدث أبداً بعد تحميل/كتابة بيانات جديدة.
     */
    public void highlightDuplicates(int colIndex) {
        setCellFactoryHighlight(colIndex, value -> {
            if (value == null || value.isBlank()) return "";
            return isDuplicateInColumn(colIndex, value)
                    ? " -fx-text-fill:red; -fx-font-weight:bold;"
                    : "";
        });
    }

    private boolean isDuplicateInColumn(int colIndex, String value) {
        int count = 0;
        for (ObservableList<String> row : tableView.getItems()) {
            String v = (colIndex < row.size()) ? row.get(colIndex) : "";
            if (value.equals(v)) {
                count++;
                if (count > 1) return true;
            }
        }
        return false;
    }

    /**
     * تمييز حالة التعيين (على رأس العمل = أخضر، غير ذلك = أصفر)
     */
    public void highlightState(int colIndex) {
        setCellFactoryHighlight(colIndex, v -> {
            if (v == null || v.isBlank()) return "";
            if (v.contains("على رأس العمل") || v.contains("مثبت")) {
                return "-fx-background-color: #c8f7c5;";
            }
            return "-fx-background-color: #fff3b3;";
        });
    }

    private void setCellFactoryHighlight(int colIndex, java.util.function.Function<String, String> styleFor) {
        TableColumn<ObservableList<String>, ?> col = tableView.getColumns().get(colIndex);
        @SuppressWarnings("unchecked")
        TableColumn<ObservableList<String>, String> strCol = (TableColumn<ObservableList<String>, String>) col;

        strCol.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);   // ✅ call super, not another cell
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle(styleFor.apply(item));
                }
            }
        });
    }

    // ══════════════════════════════════════════════════════════
    //  تبادل البيانات مع الباك إند
    // ══════════════════════════════════════════════════════════

    /**
     * تعبئة الجدول من بيانات قادمة من الباك (Map: رقم الصف -> قيم الأعمدة من العمود 1)
     * يحافظ على عمود البحث فارغاً ويحافظ على الترتيب الرقمي للمفاتيح.
     */
    public void populateFromMap(Map<Integer, Object[]> tableData) {
        tableView.getItems().clear();
        if (tableData == null || tableData.isEmpty()) {
            addMultipleRows(DEFAULT_ROWS);
            return;
        }
        tableData.entrySet().stream()
                .sorted(Comparator.comparingInt(Map.Entry::getKey))
                .forEach(entry -> {
                    ObservableList<String> row = newEmptyRow();
                    Object[] values = entry.getValue();
                    for (int i = 0; i < values.length && (SERIAL_COL + i) < COLUMN_COUNT; i++) {
                        row.set(SERIAL_COL + i, values[i] == null ? "" : String.valueOf(values[i]));
                    }
                    tableView.getItems().add(row);
                });
        if (tableView.getItems().size() < DEFAULT_ROWS) {
            addMultipleRows(DEFAULT_ROWS - tableView.getItems().size());
        }
        updateSerialNumbers();
        sortBySerial();
        adjustColumnWidths();
    }

    /**
     * بيانات الجدول للحفظ/التصدير: Map (رقم الصف -> قيم الأعمدة من العمود 1 فصاعداً)
     * يتخطى الصفوف الفارغة تماماً — نفس تنسيق ConvertToJSON القديم.
     */
    public Map<Integer, Object[]> getDataAsMap() {
        Map<Integer, Object[]> data = new LinkedHashMap<>();
        for (int r = 0; r < tableView.getItems().size(); r++) {
            ObservableList<String> row = tableView.getItems().get(r);
            boolean hasData = false;
            for (int c = SERIAL_COL; c < row.size(); c++) {
                String v = row.get(c);
                if (v != null && !v.isBlank()) {
                    hasData = true;
                    break;
                }
            }
            if (!hasData) continue;

            Object[] values = new Object[COLUMN_COUNT - 1];
            for (int c = SERIAL_COL; c < COLUMN_COUNT; c++) {
                values[c - SERIAL_COL] = row.size() > c ? row.get(c) : "";
            }
            int serial = parseInt(row.size() > SERIAL_COL ? row.get(SERIAL_COL) : "");
            data.put(serial == Integer.MAX_VALUE ? r + 1 : serial, values);
        }
        return data;
    }

    // ══════════════════════════════════════════════════════════
    //  Getters / Setters
    // ══════════════════════════════════════════════════════════

    public void setSearchHandler(BiConsumer<Integer, String> searchHandler) {
        this.searchHandler = searchHandler;
    }

    /**
     * true = الانتقال لأسفل (Enter/Tab يفرغان للأسفل)، false = لليمين
     */
    public void setNavigationDirection(boolean moveDown) {
        this.moveDown = moveDown;
    }

    public boolean isMoveDown() {
        return moveDown;
    }

    private int currentRow() {
        return tableView.getFocusModel().getFocusedCell() != null
                ? tableView.getFocusModel().getFocusedCell().getRow()
                : tableView.getSelectionModel().getSelectedIndex();
    }

    /**
     * فتح تحرير خلية محددة (يستخدمها الكونترولر عند الحاجة)
     */
    public void editCell(int row, int col) {
        moveTo(row, col);
    }

    @SuppressWarnings("unused")
    private void consumeEvent(Event e) {
        e.consume();
    }
}