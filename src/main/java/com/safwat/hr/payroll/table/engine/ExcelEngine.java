package com.safwat.hr.payroll.table.engine;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Callback;
import javafx.util.Duration;
import javafx.util.converter.DefaultStringConverter;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * =====================================================
 * ExcelEngine — محرك جدول يحاكي سلوك الإكسيل
 * =====================================================
 * <p>المزايا:</p>
 * <ul>
 *   <li>27 عموداً: بحث + مسلسل + 5 أعمدة ثابتة (قومي/كود/اسم/حالة/فئة) + أعمدة ديناميكية</li>
 *   <li>تنقل Enter/Tab/أسهم مع تمرير سكرول تلقائي واتجاه (أسفل/يمين) — يفتح التحرير فورًا</li>
 *   <li>عمود البحث: عند الإدخال يستدعي searchHandler ثم يتحرك حسب الاتجاه</li>
 *   <li>نسخ Ctrl+C / لصق Ctrl+V (يدعم TSV من إكسيل حقيقي) / Delete للمسح</li>
 *   <li>تحديد خلايا حقيقي متعدد (Cell Selection)</li>
 *   <li>قائمة سياقية: إضافة/إدراج/حذف صف + نسخ/لصق</li>
 *   <li>ترقيم تسلسلي تلقائي + ترتيب رقمي</li>
 *   <li>ضبط عرض الأعمدة تلقائياً</li>
 *   <li>تمييز التكرارات ديناميكيًا + تنسيقات الحالة والفئة</li>
 *   <li>تولتيب إحصائي على هيدر أي عمود متغير (اسم/إجمالي/متوسط/عدد الصفوف)</li>
 *   <li>تولتيب بالبيرول إندكس الكامل عند تمرير الماوس على رقم قومي غير فارغ</li>
 * </ul>
 */
public class ExcelEngine {

    /**
     * إجمالي عدد الأعمدة: بحث + مسلسل + 5 أعمدة ثابتة + 20 عمود ديناميكي
     */
    public static final int COLUMN_COUNT = 27;
    private static final int SEARCH_COL = 0;
    private static final int SERIAL_COL = 1;
    private static final int NATIONAL_ID_COL = 2;
    private static final int CATEGORY_COL = 6;
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
    private final Map<Integer, Label> dynamicHeaderLabels = new HashMap<>();
    /**
     * يوفّرها الكونترولر: رقم قومي -> نص التولتيب (نداء شبكة في الخلفية)
     */
    private Function<String, CompletableFuture<String>> nationalIdTooltipProvider;
    private final Map<String, String> nationalIdTooltipCache = new ConcurrentHashMap<>();

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

        // ✅ زيادة ارتفاع هيدر الجدول
        tableView.setStyle(
                tableView.getStyle() +
                        "; -fx-font-size: 12px;"
        );
        Platform.runLater(() -> {
            javafx.scene.Node headerBg = tableView.lookup(".column-header-background");
            if (headerBg != null) {
                headerBg.setStyle("-fx-pref-height: 40px; -fx-min-height: 40px;");
            }
            // كل هيدر عمود لوحده كمان محتاج نفس الحد الأدنى للارتفاع
            tableView.lookupAll(".column-header").forEach(header ->
                    header.setStyle("-fx-pref-height: 40px; -fx-min-height: 40px;"));
        });
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tableView.getSelectionModel().setCellSelectionEnabled(true);

        for (int i = 0; i < COLUMN_COUNT; i++) {
            if (i == SEARCH_COL) {
                tableView.getColumns().add(createSearchColumn());
            } else if (i == SERIAL_COL) {
                tableView.getColumns().add(createSerialColumn());
            } else if (i < FIRST_DYNAMIC_COL) {
                boolean editable = (i != CATEGORY_COL);
                TableColumn<ObservableList<String>, String> col = createStaticColumn(STATIC_TITLES[i], i, editable);
                if (i == NATIONAL_ID_COL) {
                    col.setCellFactory(c -> nationalIdCell());
                }
                tableView.getColumns().add(col);
            } else {
                tableView.getColumns().add(createDynamicColumn(i));
            }
        }

        setupNavigation();
        setupCopyPasteKeys();
        setupContextMenu();
        initializeDefaultRows();
        formatStateColumn();
        formatCategoryColumn();

        Platform.runLater(() -> {
            adjustColumnWidths();
            javafx.scene.Node headerBg = tableView.lookup(".column-header-background");
            if (headerBg != null) headerBg.setStyle("-fx-pref-height: 42px;");
            tableView.lookupAll(".column-header").forEach(h -> h.setStyle("-fx-pref-height: 42px;"));
        });
    }

    private void setCellStyleFactory(int colIndex, Function<String, String> styleFor) {
        TableColumn<ObservableList<String>, ?> col = tableView.getColumns().get(colIndex);
        @SuppressWarnings("unchecked")
        TableColumn<ObservableList<String>, String> strCol = (TableColumn<ObservableList<String>, String>) col;

        strCol.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
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

    public void formatStateColumn() {
        setCellStyleFactory(5, value -> {
            if (value == null || value.isBlank()) return "";
            if (value.contains("تعيين نشط")) return "";
            if (value.contains("إنهاء التعيين")) return "-fx-text-fill: #ff6b6b;";
            if (value.contains("إيقاف التعيين")) return "-fx-text-fill: #d63031; -fx-font-weight: bold;";
            return "";
        });
    }

    public void formatCategoryColumn() {
        setCellStyleFactory(CATEGORY_COL, value -> {
            if (value == null || value.isBlank()) return "";
            if (value.contains("موظف معين على درجة")) return "";
            if (value.contains("تعاقد تحت السن")) return "-fx-text-fill: #0984e3; -fx-font-weight: bold;";
            if (value.contains("مكافات لبعض العاملين من جهات خارجية"))
                return "-fx-text-fill: #e17055; -fx-font-weight: bold;";
            if (value.contains("منتدب من جهات خارجية")) return "-fx-text-fill: #2d3436; -fx-font-weight: bold;";
            if (value.contains("فوق السن")) return "-fx-text-fill: #74b9ff;";
            if (value.contains("منتدب الى جهات خارجية")) return "-fx-text-fill: #d63031; -fx-font-weight: bold;";
            if (value.contains("ندب جزئى")) return "-fx-text-fill: #fdcb6e;";
            return "";
        });
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
        col.setSortable(true);
        col.setReorderable(false);
        return col;
    }

    /**
     * خلية نصية "ذكية" — Enter/Tab أثناء التحرير يعمل commit فورًا وينتقل يفتح
     * تحرير الخلية التالية مباشرة. Tab يتحرك يمين (يسار مع Shift) ويلف للصف
     * التالي عند نهاية الأعمدة.
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
                                if (ke.getCode() == KeyCode.ENTER) {
                                    if (moveDown) moveTo(row + 1, col);
                                    else moveTo(row, col + 1);
                                } else {
                                    int nextCol = col + (ke.isShiftDown() ? -1 : 1);
                                    int nextRow = row;
                                    if (nextCol < 0) {
                                        nextCol = COLUMN_COUNT - 1;
                                        nextRow = row - 1;
                                    } else if (nextCol >= COLUMN_COUNT) {
                                        nextCol = 0;
                                        nextRow = row + 1;
                                    }
                                    if (nextRow < 0) nextRow = 0;
                                    moveTo(nextRow, nextCol);
                                }
                            });
                        }
                    });
                    Platform.runLater(tf::requestFocus);
                }
            }
        };
    }

    /**
     * نفس navigableTextCell بالظبط، لكن بدون تنقل Enter/Tab الذاتي — عمود
     * البحث بيتنقل أصلاً بعد وصول نتيجة البحث (moveAfterSearch من الكونترولر).
     * <p>
     * ✅ الإصلاح المهم هنا: بدون {@code Platform.runLater(tf::requestFocus)}
     * كان التحرير "بيفتح" بصريًا (تظهر خانة الكتابة) لكن الفوكس الفعلي كان
     * بيفضل على TableView مش على TextField الداخلي، فالكتابة ما كانتش بتشتغل.
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
     * عمود المسلسل — أرقام فقط + ترتيب رقمي تلقائي
     */
    private TableColumn<ObservableList<String>, String> createSerialColumn() {
        TableColumn<ObservableList<String>, String> col = baseColumn(STATIC_TITLES[SERIAL_COL], SERIAL_COL);
        col.setPrefWidth(70);
        col.setComparator((a, b) -> Integer.compare(parseInt(a), parseInt(b))); // ← ترتيب رقمي حقيقي
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

    private TableColumn<ObservableList<String>, String> createStaticColumn(String title, int index, boolean editable) {
        TableColumn<ObservableList<String>, String> col = baseColumn(title, index);
        col.setPrefWidth(120);
        col.setEditable(editable);
        if (editable) {
            col.setCellFactory(c -> navigableTextCell());
            col.setOnEditCommit(event -> {
                event.getRowValue().set(index, event.getNewValue());
                Platform.runLater(this::adjustColumnWidths);
            });
        }
        return col;
    }

    /**
     * خلية عمود الرقم القومي — نفس navigableTextCell (تنقل Enter/Tab فوري)
     * + تولتيب عند التمرير على قيمة غير فارغة يعرض السجل الكامل من البيرول
     * إندكس (يُجلب من الباك في الخلفية عبر nationalIdTooltipProvider).
     */
    private TableCell<ObservableList<String>, String> nationalIdCell() {
        return new TextFieldTableCell<>(new DefaultStringConverter()) {
            private final Tooltip tooltip = new Tooltip();

            {
                tooltip.setShowDelay(Duration.millis(350));
                tooltip.setWrapText(false);
                tooltip.setStyle(
                        "-fx-font-size: 16px;"
                                + "-fx-padding: 10px;"
                );
            }

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
                                if (ke.getCode() == KeyCode.ENTER) {
                                    if (moveDown) moveTo(row + 1, col);
                                    else moveTo(row, col + 1);
                                } else {
                                    int nextCol = col + (ke.isShiftDown() ? -1 : 1);
                                    int nextRow = row;
                                    if (nextCol < 0) {
                                        nextCol = COLUMN_COUNT - 1;
                                        nextRow = row - 1;
                                    } else if (nextCol >= COLUMN_COUNT) {
                                        nextCol = 0;
                                        nextRow = row + 1;
                                    }
                                    if (nextRow < 0) nextRow = 0;
                                    moveTo(nextRow, nextCol);
                                }
                            });
                        }
                    });
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

                // ✅ التمييز اللوني للتكرار — بقى جوه نفس الخلية
                setStyle(isDuplicateInColumn(NATIONAL_ID_COL, item)
                        ? "-fx-text-fill: red; -fx-font-weight: bold;"
                        : "");

                // التولتيب
                if (nationalIdTooltipProvider != null) {
                    tooltip.setOnShowing(e -> loadNationalIdTooltip(item, tooltip));
                    setTooltip(tooltip);
                } else {
                    setTooltip(null);
                }
            }
        };
    }

    private void loadNationalIdTooltip(String nationalId, Tooltip tooltip) {

        String cached = nationalIdTooltipCache.get(nationalId);
        if (cached != null) {

            tooltip.setText(cached);
            return;
        }
        tooltip.setText("جارٍ التحميل...");

        nationalIdTooltipProvider.apply(nationalId).whenComplete((text, err) -> {

            String finalText = err != null
                    ? "تعذر جلب بيانات البيرول إندكس"
                    : (text == null || text.isBlank() ? "لا يوجد سجل بالبيرول إندكس لهذا الرقم القومي" : text);
            nationalIdTooltipCache.put(nationalId, finalText);
            Platform.runLater(() -> tooltip.setText(finalText));
        });
    }

    /**
     * عمود ديناميكي — رأسه نص عادي (col.setText) + تولتيب إحصائي على الهيدر
     * (اسم العمود / إجمالي الأرقام / متوسطها / إجمالي الصفوف / الفارغة / غير الفارغة).
     * <p>
     * ⚠️ ملاحظة: كان هنا سابقاً هيدر مبني من Label(wrapText=true) جوه StackPane
     * كـ Graphic، مع إعادة إنشاء العمود بالكامل عند تحديث الهيدرز. هذا التركيب
     * هو سبب كراش JavaFX الداخلي (ArrayIndexOutOfBoundsException في
     * PrismTextLayout). الحل هنا: هيدر نصي بسيط + Tooltip.install على Label
     * شفاف صغير الحجم بدل ما يبقى graphic العمود نفسه — بدون wrapText وبدون
     * أي إعادة إنشاء متكررة، فمفيش مخاطرة تعطل.
     */
    private TableColumn<ObservableList<String>, String> createDynamicColumn(int index) {
        TableColumn<ObservableList<String>, String> col =
                baseColumn("عنصر " + (index - FIRST_DYNAMIC_COL + 1), index);
        col.setPrefWidth(100);
        col.setMinWidth(70);

        Label headerLabel = new Label(col.getText());
        headerLabel.setWrapText(false);
        headerLabel.setMaxWidth(150);
        headerLabel.setStyle("-fx-font-weight: bold; -fx-text-overrun: ellipsis;");
        col.setGraphic(headerLabel);
        col.setText(null);              // ← الإضافة المهمة: امنع تكرار النص
        dynamicHeaderLabels.put(index, headerLabel);

        col.setCellFactory(c -> navigableTextCell());
        col.setOnEditCommit(event -> {
            event.getRowValue().set(index, event.getNewValue());
            Platform.runLater(this::adjustColumnWidths);
        });

        return col;
    }


    public void addStatisticalTooltips(TableView<ObservableList<String>> targetTable) {
        if (targetTable == null) {
            return;
        }

        Platform.runLater(() -> {
            try {
                System.out.println("🔄 إضافة تولتيب إحصائي للجدول...");

                // إضافة تأثيرات التولتيب لجميع الأعمدة
                for (TableColumn<ObservableList<String>, ?> column : targetTable.getColumns()) {
                    addTooltipToColumn(column, targetTable);
                }

                // إضافة مستمع لأي أعمدة جديدة تضاف مستقبلاً
                targetTable.getColumns().addListener((ListChangeListener<TableColumn<ObservableList<String>, ?>>) change -> {
                    while (change.next()) {
                        if (change.wasAdded()) {
                            for (TableColumn<ObservableList<String>, ?> newColumn : change.getAddedSubList()) {
                                addTooltipToColumn(newColumn, targetTable);
                            }
                        }
                    }
                });

                System.out.println("✅ تم إضافة التولتيب الإحصائي لجميع الأعمدة");

            } catch (Exception e) {
                System.err.println("❌ خطأ في إضافة التولتيب الإحصائي: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * تضيف تولتيب إحصائي لعمود معين
     */
    private void addTooltipToColumn(TableColumn<ObservableList<String>, ?> column, TableView<ObservableList<String>> table) {
        if (column == null) {
            return;
        }

        // إنشاء التولتيب الأساسي
        Tooltip tooltip = new Tooltip();
        tooltip.setShowDelay(javafx.util.Duration.seconds(0.5));
        tooltip.setHideDelay(javafx.util.Duration.seconds(0.2));

        tooltip.setStyle(
                "-fx-font-size: 12px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-text-fill: #E4E5E6;"
                        + "-fx-background-color: #000000;"
                        + "-fx-border-color: #6D6E70;"
                        + "-fx-border-width: 1px;"
                        + "-fx-border-radius: 5px;"
                        + "-fx-background-radius: 5px;"
                        + "-fx-padding: 10px;"
                        + // هامش 10 بكسل من جميع الجهات
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 3);"
        );

        tooltip.setMinWidth(150);
        tooltip.setMaxWidth(400);

        // إضافة مستمع لحركة الماوس على رأس العمود
        if (column.getGraphic() != null) {
            // إذا كان للعمود graphic (مثل Label في StackPane)
            column.getGraphic().setOnMouseEntered(event -> showStatisticalTooltip(column, table, tooltip));
            column.getGraphic().setOnMouseExited(event -> tooltip.hide());
        } else {
            // للرأس العادي - نحتاج لطريقة بديلة
            setupHeaderMouseListeners(column, table, tooltip);
        }

        // إضافة التولتيب للعمود
        Tooltip.install(column.getGraphic() != null ? column.getGraphic() : getColumnHeader(column), tooltip);
    }

    /**
     * الحصول على رأس العمود من المشهد
     */
    private javafx.scene.Node getColumnHeader(TableColumn<ObservableList<String>, ?> column) {
        try {
            // البحث في مشهد الجدول عن رأس العمود
            return column.getTableView().lookup(".column-header[data-column=\"" + column.getId() + "\"]");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * إعداد مستمعي الماوس لرأس العمود
     */
    private void setupHeaderMouseListeners(TableColumn<ObservableList<String>, ?> column,
                                           TableView<ObservableList<String>> table,
                                           Tooltip tooltip) {
        // البحث عن رأس العمود في المشهد
        Platform.runLater(() -> {
            try {
                javafx.scene.Node header = getColumnHeader(column);
                if (header != null) {
                    header.setOnMouseEntered(event -> showStatisticalTooltip(column, table, tooltip));
                    header.setOnMouseExited(event -> tooltip.hide());
                }
            } catch (Exception e) {
                System.err.println("❌ خطأ في إعداد مستمعي الماوس للعمود: " + e.getMessage());
            }
        });
    }

    /**
     * بناء نص التولتيب
     */
    private String buildTooltipText(StatisticalInfo stats, String columnName) {
        StringBuilder tooltipText = new StringBuilder();

        tooltipText.append("إحصائيات العمود: ").append(columnName).append("\n\n");
        tooltipText.append(" إجمالي الصفوف: ").append(stats.totalCount).append("\n");
        tooltipText.append(" محتويات: ").append(stats.nonEmptyCount).append("\n");
        tooltipText.append(" أرقام: ").append(stats.numericCount).append("\n");
        tooltipText.append(" نصوص: ").append(stats.nonNumericCount).append("\n");
        tooltipText.append(" فارغ: ").append(stats.emptyCount).append("\n");

        if (stats.missingCount > 0) {
            tooltipText.append("❌ مفقود: ").append(stats.missingCount).append("\n");
        }

        if (stats.numericCount > 0) {
            tooltipText.append("\n المجموع: ").append(String.format("%,.2f", stats.sum)).append("\n");
            tooltipText.append(" المتوسط: ").append(String.format("%,.2f", stats.sum / stats.numericCount));
        }

        return tooltipText.toString();
    }

    /**
     * عرض التولتيب الإحصائي للعمود
     */
    private void showStatisticalTooltip(TableColumn<ObservableList<String>, ?> column,
                                        TableView<ObservableList<String>> table,
                                        Tooltip tooltip) {
        try {
            StatisticalInfo stats = calculateColumnStatistics(column, table);
            String columnName = (column.getGraphic() instanceof Label lbl)
                    ? lbl.getText()
                    : column.getText();
            String tooltipText = buildTooltipText(stats, columnName);
            tooltip.setText(tooltipText);
        } catch (Exception e) {
            tooltip.setText("❌ خطأ في حساب الإحصائيات");
            System.err.println("❌ خطأ في عرض التولتيب: " + e.getMessage());
        }
    }

    /**
     * كلاس لحفظ الإحصائيات
     */
    private static class StatisticalInfo {

        int totalCount = 0;
        int nonEmptyCount = 0;
        int numericCount = 0;
        int nonNumericCount = 0;
        int emptyCount = 0;
        int missingCount = 0;
        double sum = 0.0;
    }

    /**
     * حساب إحصائيات العمود
     */
    private StatisticalInfo calculateColumnStatistics(TableColumn<ObservableList<String>, ?> column,
                                                      TableView<ObservableList<String>> table) {
        StatisticalInfo stats = new StatisticalInfo();

        if (column == null || table == null || table.getItems().isEmpty()) {
            return stats;
        }

        int columnIndex = table.getColumns().indexOf(column);
        if (columnIndex < 0) {
            return stats;
        }

        for (ObservableList<String> row : table.getItems()) {
            if (row.size() > columnIndex) {
                String cellValue = row.get(columnIndex);

                if (cellValue != null && !cellValue.trim().isEmpty()) {
                    // زيادة عداد المحتويات غير الفارغة
                    stats.nonEmptyCount++;

                    // محاولة حساب المجموع إذا كانت القيمة رقمية
                    try {
                        double numericValue = Double.parseDouble(cellValue.trim());
                        stats.sum += numericValue;
                        stats.numericCount++;
                    } catch (NumberFormatException e) {
                        // القيمة ليست رقمية - لا مشكلة
                        stats.nonNumericCount++;
                    }
                } else {
                    // خلية فارغة
                    stats.emptyCount++;
                }
            } else {
                // خلية غير موجودة في الصف
                stats.missingCount++;
            }
        }

        stats.totalCount = table.getItems().size();
        return stats;
    }


    private String formatNumber(double n) {
        if (Double.isNaN(n) || Double.isInfinite(n)) return "0";
        if (n == Math.floor(n)) return String.valueOf((long) n);
        return String.format("%.2f", n);
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
     * انتقال مع تمرير السكرول والدخول في وضع التحرير — مؤجّل بـ
     * Platform.runLater عشان يفتح التحرير فورًا وبثبات بدل ما يحتاج
     * المستخدم يدوس بالماوس.
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
            if (column.isEditable()) {
                tableView.edit(r, column);
            } else {
                tableView.requestFocus();
            }
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
        tableView.refresh();   // ← إضافة
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
        addRow.setOnAction(e -> insertRowBelow());

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

    public void updateSerialNumbers() {
        for (int i = 0; i < tableView.getItems().size(); i++) {
            ObservableList<String> row = tableView.getItems().get(i);
            if (row.size() > SERIAL_COL && row.get(SERIAL_COL).isBlank()) {
                row.set(SERIAL_COL, String.valueOf(i + 1));
            }
        }
    }

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


    // ============ تحديث رؤوس الأعمدة ============
    public void updateColumnHeaders(String[] headers) {
        if (headers == null || ObjectUtils.isEmpty(headers)) return;

        Platform.runLater(() -> {
            for (int i = FIRST_DYNAMIC_COL; i < COLUMN_COUNT; i++) {
                String headerValue = (i < headers.length) ? headers[i] : null;
                if (headerValue == null || headerValue.isBlank() || headerValue.equals("ملاحظات")) {
                    headerValue = "عمود " + (i + 1);
                }
                Label label = dynamicHeaderLabels.get(i);
                if (label != null) label.setText(headerValue);
                // ملاحظة: مفيش col.setText(...) هنا خالص — العمود متعمد يفضل بدون نص
            }
            adjustColumnWidths();
            tableView.refresh();
        });
    }

    private void updateDynamicColumnHeader(int columnIndex, String headerText) {
        if (columnIndex < tableView.getColumns().size()) {
            TableColumn<ObservableList<String>, String> newColumn = createTextColumn(columnIndex, headerText);

            // حفظ الخصائص الحالية للعمود
            double currentWidth = tableView.getColumns().get(columnIndex).getWidth();
            boolean isVisible = tableView.getColumns().get(columnIndex).isVisible();

            // استبدال العمود
            tableView.getColumns().set(columnIndex, newColumn);

            // استعادة الخصائص
            newColumn.setPrefWidth(currentWidth);
            newColumn.setVisible(isVisible);

            System.out.println("📊 تم تحديث عمود " + columnIndex + " إلى: " + headerText);
        }
    }

    private TableColumn<ObservableList<String>, String> createTextColumn(int columnIndex, String headerText) {
        TableColumn<ObservableList<String>, String> column = new TableColumn<>();

        // ✅ إنشاء Label مع إعدادات التمدد في الارتفاع
        Label headerLabel = new Label(headerText);
        headerLabel.setWrapText(true);
        headerLabel.setMaxWidth(150);
        headerLabel.setMaxHeight(90); // ✅ حد أقصى للارتفاع 90
        headerLabel.setStyle("-fx-alignment: center; -fx-font-weight: bold; -fx-text-alignment: center;");

        // ✅ حساب الارتفاع المطلوب بناءً على النص
        Text text = new Text(headerText);
        text.setFont(Font.font("System", 12));
        double textHeight = text.getLayoutBounds().getHeight();
        int lineCount = (int) Math.ceil(text.getLayoutBounds().getWidth() / 120); // تقدير عدد الأسطر

        double requiredHeight = Math.min(25 + (lineCount * 18), 90); // ✅ حد أقصى 90

        headerLabel.setPrefHeight(requiredHeight);
        headerLabel.setMinHeight(25); // ✅ حد أدنى للارتفاع

        StackPane headerContainer = new StackPane(headerLabel);
        headerContainer.setPrefHeight(requiredHeight);
        headerContainer.setMaxHeight(90); // ✅ حد أقصى للحاوية
        column.setGraphic(headerContainer);

        column.setCellValueFactory(data -> {
            if (data.getValue().size() > columnIndex) {
                return new javafx.beans.property.SimpleStringProperty(data.getValue().get(columnIndex));
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });

        column.setCellFactory(TextFieldTableCell.forTableColumn(new DefaultStringConverter()));

        column.setOnEditCommit(event -> {
            if (event.getRowValue().size() > columnIndex) {
                event.getRowValue().set(columnIndex, event.getNewValue());
            }

            Platform.runLater(() -> {
                setupNavigation();
                adjustColumnWidths(); // تحديث العرض بعد التعديل
            });
        });

        column.setStyle("-fx-alignment: center;");
        return column;
    }

    public List<String> getCurrentHeaders() {
        List<String> headers = new ArrayList<>();
        for (int i = 0; i < tableView.getColumns().size(); i++) {
            TableColumn<ObservableList<String>, ?> c = tableView.getColumns().get(i);
            Label label = dynamicHeaderLabels.get(i);
            headers.add(label != null ? label.getText() : c.getText());
        }
        return headers;
    }
    // ══════════════════════════════════════════════════════════
    //  ضبط العرض
    // ══════════════════════════════════════════════════════════


    // ============ إدارة الأعمدة والعرض ============
    public void adjustColumnWidths() {
        Platform.runLater(() -> {
            try {
                for (int i = 0; i < tableView.getColumns().size(); i++) {
                    TableColumn<ObservableList<String>, ?> column = tableView.getColumns().get(i);

                    double calculatedWidth = computeColumnWidth(column);

                    if (i < 6) {
                        // الأعمدة الثابتة - عرض أكثر مرونة
                        column.setPrefWidth(Math.max(100, Math.min(calculatedWidth, 200)));
                        column.setMinWidth(80);
                    } else {
                        // الأعمدة الديناميكية - عرض متوازن
                        column.setPrefWidth(Math.max(90, Math.min(calculatedWidth, 150)));
                        column.setMinWidth(70);
                    }

                    column.setMaxWidth(200); // حد أقصى لجميع الأعمدة
                }

                // إجبار الجدول على إعادة حساب الأبعاد
                tableView.layout();

                System.out.println("✅ تم ضبط عرض الأعمدة بنجاح");

            } catch (Exception e) {
                System.err.println("❌ خطأ في ضبط عرض الأعمدة: " + e.getMessage());
            }
        });
    }

    private double computeColumnWidth(TableColumn<ObservableList<String>, ?> column) {
        double maxWidth = 90;

        String headerText;
        if (column.getGraphic() instanceof Label lbl) {
            headerText = lbl.getText();
        } else if (column.getGraphic() instanceof StackPane sp && !sp.getChildren().isEmpty()
                && sp.getChildren().get(0) instanceof Label lbl2) {
            headerText = lbl2.getText();
        } else {
            headerText = column.getText();
        }

        if (headerText != null && !headerText.isEmpty()) {
            Text text = new Text(headerText);
            text.setFont(Font.font("System", 12));
            double headerWidth = text.getLayoutBounds().getWidth() + 25;
            maxWidth = Math.max(maxWidth, headerWidth);
        }

        for (int i = 0; i < Math.min(tableView.getItems().size(), 100); i++) {
            Object cellData = column.getCellData(i);
            if (cellData != null) {
                String cellText = cellData.toString();
                if (!cellText.isEmpty()) {
                    Text text = new Text(cellText);
                    text.setFont(Font.font("System", 12));
                    double cellWidth = text.getLayoutBounds().getWidth() + 20;
                    if (cellWidth > maxWidth) {
                        maxWidth = cellWidth;
                    }
                }
            }
        }

        return Math.max(90, Math.min(maxWidth, 200));
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

    public void deleteEmptyRows() {
        tableView.getItems().removeIf(row -> {
            for (int i = 0; i < row.size(); i++) {
                if (i == SERIAL_COL) continue;
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

    public void highlightDuplicates(int colIndex) {
        setCellFactoryHighlight(colIndex, value -> {
            if (value == null || value.isBlank()) return "";
            return isDuplicateInColumn(colIndex, value)
                    ? "-fx-text-fill: red; -fx-font-weight: bold;"
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


    private void setCellFactoryHighlight(int colIndex, Function<String, String> styleFor) {
        TableColumn<ObservableList<String>, ?> col = tableView.getColumns().get(colIndex);
        @SuppressWarnings("unchecked")
        TableColumn<ObservableList<String>, String> strCol = (TableColumn<ObservableList<String>, String>) col;

        strCol.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
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


    public void setNationalIdTooltipProvider(Function<String, CompletableFuture<String>> provider) {
        this.nationalIdTooltipProvider = provider;
        // إعادة تعيين CellFactory للعمود الوطني
        TableColumn<ObservableList<String>, ?> col = tableView.getColumns().get(NATIONAL_ID_COL);
        if (col != null) {
            @SuppressWarnings("unchecked")
            TableColumn<ObservableList<String>, String> strCol = (TableColumn<ObservableList<String>, String>) col;
            strCol.setCellFactory(c -> nationalIdCell());
            tableView.refresh(); // تحديث العرض
        }
    }

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

    public void editCell(int row, int col) {
        moveTo(row, col);
    }

    @SuppressWarnings("unused")
    private void consumeEvent(Event e) {
        e.consume();
    }
}