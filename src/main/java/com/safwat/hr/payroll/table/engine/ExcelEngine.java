package com.safwat.hr.payroll.table.engine;

import com.safwat.hr.payroll.table.engine.clipboard.ClipboardHandler;
import com.safwat.hr.payroll.table.engine.column.CellStyleFormatter;
import com.safwat.hr.payroll.table.engine.column.TableColumnFactory;
import com.safwat.hr.payroll.table.engine.data.TableDataMapper;
import com.safwat.hr.payroll.table.engine.header.ColumnWidthAdjuster;
import com.safwat.hr.payroll.table.engine.header.DynamicHeaderManager;
import com.safwat.hr.payroll.table.engine.menu.TableContextMenuFactory;
import com.safwat.hr.payroll.table.engine.navigation.TableNavigationHandler;
import com.safwat.hr.payroll.table.engine.rows.RowManager;
import com.safwat.hr.payroll.table.engine.tooltip.HeaderStatisticsTooltip;
import com.safwat.hr.payroll.table.engine.tooltip.NationalIdTooltipService;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * =====================================================
 * ExcelEngine — واجهة/منسّق (Facade) لمحرك جدول يحاكي سلوك الإكسيل
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
 * <p>
 * <b>ده الكلاس الوحيد اللي المفروض حد بره الـ package يعرفه أو
 * يستخدمه مباشرة.</b> كل المنطق الفعلي موزّع على كلاسات متخصصة
 * (كل واحدة في package فرعي حسب مسؤوليتها: column / navigation /
 * clipboard / rows / header / tooltip / data / menu) وExcelEngine
 * بس بيوصّل بينها ويعرض نفس الـ API القديم بالظبط، فمفيش أي تعديل
 * مطلوب في الكود اللي بينادي عليه (TableController وغيره).
 * </p>
 */
public class ExcelEngine {

    public static final int COLUMN_COUNT = TableSchema.COLUMN_COUNT;
    public static final int FIRST_DYNAMIC_COL = TableSchema.FIRST_DYNAMIC_COL;

    private final TableView<ObservableList<String>> tableView;

    private final RowManager rowManager;
    private final ColumnWidthAdjuster columnWidthAdjuster;
    private final DynamicHeaderManager dynamicHeaderManager;
    private final CellStyleFormatter cellStyleFormatter;
    private final NationalIdTooltipService nationalIdTooltipService;
    private final ClipboardHandler clipboardHandler;
    private final TableNavigationHandler navigationHandler;
    private final TableColumnFactory columnFactory;
    private final HeaderStatisticsTooltip headerStatisticsTooltip;
    private final TableDataMapper dataMapper;
    private final TableContextMenuFactory contextMenuFactory;

    public ExcelEngine(TableView<ObservableList<String>> tableView) {
        this.tableView = tableView;

        // ── تركيب شجرة الاعتماديات (Dependency wiring) ──
        this.rowManager = new RowManager(tableView);
        this.columnWidthAdjuster = new ColumnWidthAdjuster(tableView);
        this.dynamicHeaderManager = new DynamicHeaderManager(tableView, columnWidthAdjuster);
        this.cellStyleFormatter = new CellStyleFormatter(tableView);
        this.nationalIdTooltipService = new NationalIdTooltipService();
        this.clipboardHandler = new ClipboardHandler(tableView, rowManager, columnWidthAdjuster);
        this.navigationHandler = new TableNavigationHandler(tableView, rowManager, clipboardHandler);
        this.columnFactory = new TableColumnFactory(
                navigationHandler, dynamicHeaderManager, columnWidthAdjuster,
                cellStyleFormatter, nationalIdTooltipService, rowManager);
        this.headerStatisticsTooltip = new HeaderStatisticsTooltip();
        this.dataMapper = new TableDataMapper(tableView, rowManager, columnWidthAdjuster);
        this.contextMenuFactory = new TableContextMenuFactory();
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
        tableView.setStyle(tableView.getStyle() + "; -fx-font-size: 12px;");
        Platform.runLater(() -> {
            javafx.scene.Node headerBg = tableView.lookup(".column-header-background");
            if (headerBg != null) {
                headerBg.setStyle("-fx-pref-height: 40px; -fx-min-height: 40px;");
            }
            tableView.lookupAll(".column-header").forEach(header ->
                    header.setStyle("-fx-pref-height: 40px; -fx-min-height: 40px;"));
        });
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tableView.getSelectionModel().setCellSelectionEnabled(true);

        for (int i = 0; i < TableSchema.COLUMN_COUNT; i++) {
            if (i == TableSchema.SEARCH_COL) {
                tableView.getColumns().add(columnFactory.createSearchColumn());
            } else if (i == TableSchema.SERIAL_COL) {
                tableView.getColumns().add(columnFactory.createSerialColumn());
            } else if (i < TableSchema.FIRST_DYNAMIC_COL) {
                boolean editable = (i != TableSchema.CATEGORY_COL);
                TableColumn<ObservableList<String>, String> col =
                        columnFactory.createStaticColumn(TableSchema.STATIC_TITLES[i], i, editable);
                if (i == TableSchema.NATIONAL_ID_COL) {
                    col.setCellFactory(c -> columnFactory.nationalIdCell());
                }
                tableView.getColumns().add(col);
            } else {
                tableView.getColumns().add(columnFactory.createDynamicColumn(i));
            }
        }

        navigationHandler.setupNavigation();
        contextMenuFactory.install(tableView, rowManager, clipboardHandler);
        rowManager.initializeDefaultRows();
        formatStateColumn();
        formatCategoryColumn();

        Platform.runLater(() -> {
            adjustColumnWidths();
            javafx.scene.Node headerBg = tableView.lookup(".column-header-background");
            if (headerBg != null) headerBg.setStyle("-fx-pref-height: 42px;");
            tableView.lookupAll(".column-header").forEach(h -> h.setStyle("-fx-pref-height: 42px;"));
        });
    }

    public void formatStateColumn() {
        cellStyleFormatter.formatStateColumn();
    }

    public void formatCategoryColumn() {
        cellStyleFormatter.formatCategoryColumn();
    }

    public void highlightDuplicates(int colIndex) {
        cellStyleFormatter.highlightDuplicates(colIndex);
    }

    // ══════════════════════════════════════════════════════════
    //  التحديث والمسح
    // ══════════════════════════════════════════════════════════

    public void quickRefresh() {
        tableView.refresh();
        Platform.runLater(this::adjustColumnWidths);
    }

    public void clearTable() {
        rowManager.clearTable();
    }

    public void deleteEmptyRows() {
        rowManager.deleteEmptyRows();
    }

    // ══════════════════════════════════════════════════════════
    //  إدارة الصفوف
    // ══════════════════════════════════════════════════════════

    public void addNewRow() {
        rowManager.addNewRow();
    }

    public void addMultipleRows(int count) {
        rowManager.addMultipleRows(count);
    }

    public void insertRowBelow() {
        rowManager.insertRowBelow();
    }

    public void deleteCurrentRow() {
        rowManager.deleteCurrentRow();
    }

    public void deleteRowAt(int row) {
        rowManager.deleteRowAt(row);
    }

    public void updateSerialNumbers() {
        rowManager.updateSerialNumbers();
    }

    // ══════════════════════════════════════════════════════════
    //  النسخ واللصق
    // ══════════════════════════════════════════════════════════

    public void pasteDataFromString(String data, int startRow, int startColumn) {
        clipboardHandler.pasteDataFromString(data, startRow, startColumn);
    }

    public void updateCellValue(int row, int col, String value) {
        clipboardHandler.updateCellValue(row, col, value);
    }

    // ══════════════════════════════════════════════════════════
    //  تبادل البيانات مع الباك إند
    // ══════════════════════════════════════════════════════════

    public void populateFromMap(Map<Integer, Object[]> tableData) {
        dataMapper.populateFromMap(tableData);
    }

    public Map<Integer, Object[]> getDataAsMap() {
        return dataMapper.getDataAsMap();
    }

    // ══════════════════════════════════════════════════════════
    //  رؤوس الأعمدة الديناميكية وضبط العرض
    // ══════════════════════════════════════════════════════════

    public void updateColumnHeaders(String[] headers) {
        dynamicHeaderManager.updateColumnHeaders(headers);
    }

    public List<String> getCurrentHeaders() {
        return dynamicHeaderManager.getCurrentHeaders();
    }

    public void adjustColumnWidths() {
        columnWidthAdjuster.adjustColumnWidths();
    }

    // ══════════════════════════════════════════════════════════
    //  التولتيب الإحصائي
    // ══════════════════════════════════════════════════════════

    public void addStatisticalTooltips(TableView<ObservableList<String>> targetTable) {
        headerStatisticsTooltip.addStatisticalTooltips(targetTable);
    }

    /**
     * تحكم بتشيك بوكس: تفعيل/تعطيل التولتيب الإحصائي على الهيدر فورًا.
     */
    public void setStatisticalTooltipsEnabled(boolean enabled) {
        headerStatisticsTooltip.setEnabled(enabled);
    }

    public boolean isStatisticalTooltipsEnabled() {
        return headerStatisticsTooltip.isEnabled();
    }

    /**
     * تحكم بتشيك بوكس: تفعيل/تعطيل تولتيب البيرول إندكس على عمود الرقم القومي فورًا.
     */
    public void setNationalIdTooltipEnabled(boolean enabled) {
        nationalIdTooltipService.setEnabled(enabled);
        quickRefresh(); // يخلي الخلايا المعروضة تعيد تقييم قرار عرض التولتيب فورًا
    }

    public boolean isNationalIdTooltipEnabled() {
        return nationalIdTooltipService.isEnabled();
    }

    // ══════════════════════════════════════════════════════════
    //  Getters / Setters
    // ══════════════════════════════════════════════════════════

    public void setSearchHandler(BiConsumer<Integer, String> searchHandler) {
        columnFactory.setSearchHandler(searchHandler);
    }

    public void setNationalIdTooltipProvider(Function<String, CompletableFuture<String>> provider) {
        nationalIdTooltipService.setProvider(provider);
        // إعادة تعيين CellFactory للعمود الوطني
        TableColumn<ObservableList<String>, ?> col = tableView.getColumns().get(TableSchema.NATIONAL_ID_COL);
        if (col != null) {
            @SuppressWarnings("unchecked")
            TableColumn<ObservableList<String>, String> strCol = (TableColumn<ObservableList<String>, String>) col;
            strCol.setCellFactory(c -> columnFactory.nationalIdCell());
            tableView.refresh();
        }
    }

    public void setNavigationDirection(boolean moveDown) {
        navigationHandler.setMoveDown(moveDown);
    }

    public boolean isMoveDown() {
        return navigationHandler.isMoveDown();
    }

    public void editCell(int row, int col) {
        navigationHandler.moveTo(row, col);
    }

    public void moveAfterSearch(int currentRow) {
        navigationHandler.moveAfterSearch(currentRow);
    }
}