package com.safwat.hr.payroll.table.engine.clipboard;

import com.safwat.hr.payroll.table.engine.TableSchema;
import com.safwat.hr.payroll.table.engine.header.ColumnWidthAdjuster;
import com.safwat.hr.payroll.table.engine.rows.RowManager;
import javafx.collections.ObservableList;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * =====================================================
 * ClipboardHandler — نسخ/لصق/مسح خلايا جدول الإدخال
 * =====================================================
 * <p>مسؤولية وحيدة: تحويل تحديد الخلايا لصيغة TSV (نفس اللي إكسيل
 * بيفهمها) والعكس. منفصل عن التنقل بالكيبورد عشان تقدر تستخدمه من
 * القائمة السياقية أو أي مصدر تاني (زر توولبار مثلاً) من غير ما
 * تلمس منطق الأسهم/Enter/Tab.</p>
 */
public class ClipboardHandler {

    private final TableView<ObservableList<String>> tableView;
    private final RowManager rowManager;
    private final ColumnWidthAdjuster columnWidthAdjuster;

    public ClipboardHandler(TableView<ObservableList<String>> tableView,
                             RowManager rowManager,
                             ColumnWidthAdjuster columnWidthAdjuster) {
        this.tableView = tableView;
        this.rowManager = rowManager;
        this.columnWidthAdjuster = columnWidthAdjuster;
    }

    public void copySelectedCells() {
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

    public void pasteFromClipboard() {
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
            if (row >= tableView.getItems().size()) rowManager.addNewRow();
            String[] cells = lines[r].split("\\t", -1);
            for (int c = 0; c < cells.length; c++) {
                int col = startColumn + c;
                if (col < TableSchema.COLUMN_COUNT) {
                    updateCellValue(row, col, cells[c]);
                }
            }
        }
        rowManager.updateSerialNumbers();
        tableView.refresh();
        columnWidthAdjuster.adjustColumnWidths();
    }

    public void clearSelectedCells() {
        for (TablePosition p : tableView.getSelectionModel().getSelectedCells()) {
            updateCellValue(p.getRow(), p.getColumn(), "");
        }
        columnWidthAdjuster.adjustColumnWidths();
    }

    private String cellValue(int row, int col) {
        if (row < 0 || row >= tableView.getItems().size()) return "";
        ObservableList<String> r = tableView.getItems().get(row);
        return (col < r.size()) ? r.get(col) : "";
    }

    public void updateCellValue(int row, int col, String value) {
        if (row < 0 || row >= tableView.getItems().size()) return;
        ObservableList<String> r = tableView.getItems().get(row);
        while (r.size() < TableSchema.COLUMN_COUNT) r.add("");
        r.set(col, value == null ? "" : value);
    }
}
