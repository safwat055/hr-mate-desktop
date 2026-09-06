package com.safwat.hr.payroll.table.engine.data;

import com.safwat.hr.payroll.table.engine.TableSchema;
import com.safwat.hr.payroll.table.engine.header.ColumnWidthAdjuster;
import com.safwat.hr.payroll.table.engine.rows.RowManager;
import javafx.collections.ObservableList;
import javafx.scene.control.TableView;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * =====================================================
 * TableDataMapper — التحويل بين شكل الجدول وبيانات الباك إند
 * =====================================================
 * <p>مسؤولية وحيدة: تحويل {@code Map<Integer, Object[]>} (شكل
 * البيانات القادم/الذاهب للباك إند) من/إلى صفوف الجدول. أي تغيير
 * مستقبلي في شكل عقد البيانات مع الباك إند (API جديد، حقول إضافية)
 * بيتم هنا بس، بعيد عن منطق العرض والتنقل.</p>
 */
public class TableDataMapper {

    private final TableView<ObservableList<String>> tableView;
    private final RowManager rowManager;
    private final ColumnWidthAdjuster columnWidthAdjuster;

    public TableDataMapper(TableView<ObservableList<String>> tableView,
                            RowManager rowManager,
                            ColumnWidthAdjuster columnWidthAdjuster) {
        this.tableView = tableView;
        this.rowManager = rowManager;
        this.columnWidthAdjuster = columnWidthAdjuster;
    }

    public void populateFromMap(Map<Integer, Object[]> tableData) {
        tableView.getItems().clear();
        if (tableData == null || tableData.isEmpty()) {
            rowManager.addMultipleRows(TableSchema.DEFAULT_ROWS);
            return;
        }
        tableData.entrySet().stream()
                .sorted(Comparator.comparingInt(Map.Entry::getKey))
                .forEach(entry -> {
                    ObservableList<String> row = rowManager.newEmptyRow();
                    Object[] values = entry.getValue();
                    for (int i = 0; i < values.length && (TableSchema.SERIAL_COL + i) < TableSchema.COLUMN_COUNT; i++) {
                        row.set(TableSchema.SERIAL_COL + i, values[i] == null ? "" : String.valueOf(values[i]));
                    }
                    tableView.getItems().add(row);
                });
        if (tableView.getItems().size() < TableSchema.DEFAULT_ROWS) {
            rowManager.addMultipleRows(TableSchema.DEFAULT_ROWS - tableView.getItems().size());
        }
        rowManager.updateSerialNumbers();
        rowManager.sortBySerial();
        columnWidthAdjuster.adjustColumnWidths();
    }

    public Map<Integer, Object[]> getDataAsMap() {
        Map<Integer, Object[]> data = new LinkedHashMap<>();
        for (int r = 0; r < tableView.getItems().size(); r++) {
            ObservableList<String> row = tableView.getItems().get(r);
            boolean hasData = false;
            for (int c = TableSchema.SERIAL_COL; c < row.size(); c++) {
                String v = row.get(c);
                if (v != null && !v.isBlank()) {
                    hasData = true;
                    break;
                }
            }
            if (!hasData) continue;

            Object[] values = new Object[TableSchema.COLUMN_COUNT - 1];
            for (int c = TableSchema.SERIAL_COL; c < TableSchema.COLUMN_COUNT; c++) {
                values[c - TableSchema.SERIAL_COL] = row.size() > c ? row.get(c) : "";
            }
            int serial = rowManager.parseInt(row.size() > TableSchema.SERIAL_COL ? row.get(TableSchema.SERIAL_COL) : "");
            data.put(serial == Integer.MAX_VALUE ? r + 1 : serial, values);
        }
        return data;
    }
}
