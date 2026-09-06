package com.safwat.hr.controller.payroll.table.engine.header;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

/**
 * =====================================================
 * ColumnWidthAdjuster — ضبط عرض أعمدة جدول الإدخال تلقائيًا
 * =====================================================
 * <p>مسؤولية وحيدة: حساب أفضل عرض لكل عمود بناءً على محتوى الهيدر
 * والخلايا، وتطبيقه. منفصل عن باقي المنطق عشان لو حبينا نغيّر
 * سياسة العرض (مثلاً نضيف عمود صور أو نعمل auto-fit مختلف) نلمس
 * ملف واحد بس.</p>
 */
public class ColumnWidthAdjuster {

    private final TableView<ObservableList<String>> tableView;

    public ColumnWidthAdjuster(TableView<ObservableList<String>> tableView) {
        this.tableView = tableView;
    }

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
}
