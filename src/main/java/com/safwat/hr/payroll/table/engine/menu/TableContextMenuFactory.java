package com.safwat.hr.payroll.table.engine.menu;

import com.safwat.hr.payroll.table.engine.clipboard.ClipboardHandler;
import com.safwat.hr.payroll.table.engine.rows.RowManager;
import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableView;

/**
 * =====================================================
 * TableContextMenuFactory — القائمة السياقية لجدول الإدخال
 * =====================================================
 * <p>مسؤولية وحيدة: بناء القائمة السياقية وربطها بالأفعال الموجودة
 * فعليًا في RowManager/ClipboardHandler. إضافة عنصر قائمة جديد
 * مستقبلًا (مثلاً "تكرار الصف") بيبقى سطر واحد هنا.</p>
 */
public class TableContextMenuFactory {

    public void install(TableView<ObservableList<String>> tableView,
                         RowManager rowManager,
                         ClipboardHandler clipboardHandler) {
        ContextMenu menu = new ContextMenu();

        MenuItem addRow = new MenuItem("إضافة صف في الأسفل");
        addRow.setOnAction(e -> rowManager.insertRowBelow());

        MenuItem copy = new MenuItem("نسخ");
        copy.setOnAction(e -> clipboardHandler.copySelectedCells());

        MenuItem paste = new MenuItem("لصق");
        paste.setOnAction(e -> clipboardHandler.pasteFromClipboard());

        MenuItem deleteRow = new MenuItem("حذف الصف الحالي");
        deleteRow.setOnAction(e -> rowManager.deleteCurrentRow());

        menu.getItems().addAll(addRow, copy, paste, new SeparatorMenuItem(), deleteRow);
        tableView.setContextMenu(menu);
    }
}
