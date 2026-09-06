package com.safwat.hr.payroll.table.engine.navigation;

import com.safwat.hr.payroll.table.engine.TableSchema;
import com.safwat.hr.payroll.table.engine.clipboard.ClipboardHandler;
import com.safwat.hr.payroll.table.engine.rows.RowManager;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 * =====================================================
 * TableNavigationHandler — التنقل بالكيبورد داخل جدول الإدخال
 * =====================================================
 * <p>مسؤولية وحيدة: تحويل ضغطات الكيبورد (Enter/Tab/الأسهم/Delete/
 * Ctrl+C/V) لحركة/فعل داخل الجدول، بما فيها فتح التحرير تلقائيًا
 * وتمرير السكرول. أي خلية تحتاج "تتحرك للخلية اللي بعدها بعد
 * Enter" (زي navigableTextCell) بتستخدم {@link #moveTo(int, int)}
 * من هنا بدل ما تكرر نفس المنطق.</p>
 */
public class TableNavigationHandler {

    private final TableView<ObservableList<String>> tableView;
    private final RowManager rowManager;
    private final ClipboardHandler clipboardHandler;
    private boolean moveDown = true;

    public TableNavigationHandler(TableView<ObservableList<String>> tableView,
                                   RowManager rowManager,
                                   ClipboardHandler clipboardHandler) {
        this.tableView = tableView;
        this.rowManager = rowManager;
        this.clipboardHandler = clipboardHandler;
    }

    public void setupNavigation() {
        tableView.setOnKeyPressed(this::handleKey);
    }

    public void setMoveDown(boolean moveDown) {
        this.moveDown = moveDown;
    }

    public boolean isMoveDown() {
        return moveDown;
    }

    private void handleKey(KeyEvent event) {
        boolean ctrl = event.isControlDown();
        KeyCode code = event.getCode();

        if (ctrl && code == KeyCode.C) {
            clipboardHandler.copySelectedCells();
            event.consume();
            return;
        }
        if (ctrl && code == KeyCode.V) {
            clipboardHandler.pasteFromClipboard();
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
                moveTo(pos.getRow(), Math.min(TableSchema.COLUMN_COUNT - 1, pos.getColumn() + 1));
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
                clipboardHandler.clearSelectedCells();
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
    public void moveTo(int row, int col) {
        if (row >= tableView.getItems().size()) {
            rowManager.addNewRow();
        }
        final int r = Math.min(row, tableView.getItems().size() - 1);
        final int c = Math.min(col, TableSchema.COLUMN_COUNT - 1);
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

    /** الحركة بعد انتهاء البحث حسب الاتجاه المختار */
    public void moveAfterSearch(int currentRow) {
        if (moveDown) {
            moveTo(currentRow + 1, TableSchema.SEARCH_COL);
        } else {
            moveTo(currentRow, TableSchema.FIRST_DYNAMIC_COL);
        }
    }
}
