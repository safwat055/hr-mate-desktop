package com.safwat.hr.controller.payroll.table.engine.column;

import com.safwat.hr.controller.payroll.table.engine.TableSchema;
import javafx.collections.ObservableList;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.function.Function;

/**
 * =====================================================
 * CellStyleFormatter — تنسيقات لون/خط الخلايا
 * =====================================================
 * <p>مسؤولية وحيدة: تلوين خلايا عمود بعينه بناءً على قيمته (حالة
 * الموظف، الفئة) أو بناءً على تكراره في العمود. إضافة تنسيق جديد
 * (لون لعمود تاني مثلاً) بيبقى سطر جديد هنا بدل ما يتوه جوه كلاس
 * بناء الأعمدة.</p>
 */
public class CellStyleFormatter {

    private final TableView<ObservableList<String>> tableView;

    public CellStyleFormatter(TableView<ObservableList<String>> tableView) {
        this.tableView = tableView;
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
        setCellStyleFactory(TableSchema.CATEGORY_COL, value -> {
            if (value == null || value.isBlank()) return "";
            if (value.contains("موظف معين على درجة")) return "";
            if (value.contains("تعاقد تحت السن")) return "-fx-text-fill: #0984e3; -fx-font-weight: bold;";
            if (value.contains("مكافات لبعض العاملين من جهات خارجية"))
                return "-fx-text-fill: #e17055; -fx-font-weight: bold;";
            if (value.contains("منتدب من جهات خارجية")) return "-fx-font-weight: bold;";
            if (value.contains("فوق السن")) return "-fx-text-fill: #74b9ff;";
            if (value.contains("منتدب الى جهات خارجية")) return "-fx-text-fill: #d63031; -fx-font-weight: bold;";
            if (value.contains("ندب جزئى")) return "-fx-text-fill: #fdcb6e;";
            return "";
        });
    }

    public void highlightDuplicates(int colIndex) {
        setCellStyleFactory(colIndex, value -> {
            if (value == null || value.isBlank()) return "";
            return isDuplicateInColumn(colIndex, value)
                    ? "-fx-text-fill: red; -fx-font-weight: bold;"
                    : "";
        });
    }

    /**
     * بيستخدمها كمان TableColumnFactory (خلية الرقم القومي) لتلوين التكرار وهو بيبني الخلية.
     */
    public boolean isDuplicateInColumn(int colIndex, String value) {
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
}
