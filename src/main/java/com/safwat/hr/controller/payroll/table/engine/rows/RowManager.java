package com.safwat.hr.controller.payroll.table.engine.rows;

import com.safwat.hr.controller.payroll.table.engine.TableSchema;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableView;

/**
 * =====================================================
 * RowManager — كل ما يخص إدارة صفوف جدول الإدخال
 * =====================================================
 * <p>المسؤولية الوحيدة هنا: إضافة/إدراج/حذف الصفوف، الترقيم
 * التسلسلي التلقائي، والترتيب الرقمي حسب المسلسل. أي كلاس تاني
 * محتاج "يضيف صف جديد" أو "يرقّم الصفوف" بيستخدم الكلاس ده بدل
 * ما يعيد كتابة نفس المنطق.</p>
 */
public class RowManager {

    private final TableView<ObservableList<String>> tableView;

    public RowManager(TableView<ObservableList<String>> tableView) {
        this.tableView = tableView;
    }

    public void initializeDefaultRows() {
        tableView.setItems(FXCollections.observableArrayList());
        addMultipleRows(TableSchema.DEFAULT_ROWS);
    }

    public ObservableList<String> newEmptyRow() {
        ObservableList<String> row = FXCollections.observableArrayList();
        for (int i = 0; i < TableSchema.COLUMN_COUNT; i++) row.add("");
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

    public void clearTable() {
        tableView.getItems().clear();
        addMultipleRows(TableSchema.DEFAULT_ROWS);
    }

    public void deleteEmptyRows() {
        tableView.getItems().removeIf(row -> {
            for (int i = 0; i < row.size(); i++) {
                if (i == TableSchema.SERIAL_COL) continue;
                String v = row.get(i);
                if (v != null && !v.isBlank()) return false;
            }
            return true;
        });
        if (tableView.getItems().size() < TableSchema.DEFAULT_ROWS) {
            addMultipleRows(TableSchema.DEFAULT_ROWS - tableView.getItems().size());
        }
        updateSerialNumbers();
    }

    public void updateSerialNumbers() {
        for (int i = 0; i < tableView.getItems().size(); i++) {
            ObservableList<String> row = tableView.getItems().get(i);
            if (row.size() > TableSchema.SERIAL_COL && row.get(TableSchema.SERIAL_COL).isBlank()) {
                row.set(TableSchema.SERIAL_COL, String.valueOf(i + 1));
            }
        }
    }

    public void sortBySerial() {
        FXCollections.sort(tableView.getItems(), (a, b) -> {
            int x = parseInt(a.size() > TableSchema.SERIAL_COL ? a.get(TableSchema.SERIAL_COL) : "");
            int y = parseInt(b.size() > TableSchema.SERIAL_COL ? b.get(TableSchema.SERIAL_COL) : "");
            return Integer.compare(x, y);
        });
    }

    public int parseInt(String v) {
        try {
            return Integer.parseInt(v.trim());
        } catch (Exception e) {
            return Integer.MAX_VALUE;
        }
    }

    private int currentRow() {
        return tableView.getFocusModel().getFocusedCell() != null
                ? tableView.getFocusModel().getFocusedCell().getRow()
                : tableView.getSelectionModel().getSelectedIndex();
    }
}
