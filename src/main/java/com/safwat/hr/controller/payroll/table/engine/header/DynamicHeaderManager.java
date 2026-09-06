package com.safwat.hr.controller.payroll.table.engine.header;

import com.safwat.hr.controller.payroll.table.engine.TableSchema;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.converter.DefaultStringConverter;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * =====================================================
 * DynamicHeaderManager — رؤوس الأعمدة الديناميكية
 * =====================================================
 * <p>الأعمدة الديناميكية (بعد {@link TableSchema#FIRST_DYNAMIC_COL})
 * أسماؤها بتتغيّر حسب البيانات القادمة من الباك إند، فمحتاجة إدارة
 * منفصلة عن باقي الأعمدة الثابتة. الكلاس ده بيحتفظ بـ Label كل عمود
 * ديناميكي عشان يقدر يحدّث نصه من غير ما يعيد بناء العمود كله (وده
 * كان سبب كراش JavaFX الأصلي لو اتعمل بطريقة تانية).</p>
 */
public class DynamicHeaderManager {

    private final TableView<ObservableList<String>> tableView;
    private final ColumnWidthAdjuster columnWidthAdjuster;
    private final Map<Integer, Label> dynamicHeaderLabels = new HashMap<>();

    public DynamicHeaderManager(TableView<ObservableList<String>> tableView,
                                ColumnWidthAdjuster columnWidthAdjuster) {
        this.tableView = tableView;
        this.columnWidthAdjuster = columnWidthAdjuster;
    }

    /**
     * يسجّل الـ Label الخاص بهيدر عمود ديناميكي عشان يقدر يحدّثه لاحقًا.
     * بينادى عليه من TableColumnFactory وقت إنشاء العمود.
     */
    public void registerHeaderLabel(int columnIndex, Label label) {
        dynamicHeaderLabels.put(columnIndex, label);
    }

    public void updateColumnHeaders(String[] headers) {
        if (headers == null || ObjectUtils.isEmpty(headers)) return;

        Platform.runLater(() -> {
            for (int i = TableSchema.FIRST_DYNAMIC_COL; i < TableSchema.COLUMN_COUNT; i++) {
                String headerValue = (i < headers.length) ? headers[i] : null;
                if (headerValue == null || headerValue.isBlank() || headerValue.equals("ملاحظات")) {
                    headerValue = "عمود " + (i + 1);
                }
                Label label = dynamicHeaderLabels.get(i);
                if (label != null) label.setText(headerValue);
                // ملاحظة: مفيش col.setText(...) هنا خالص — العمود متعمد يفضل بدون نص
            }
            columnWidthAdjuster.adjustColumnWidths();
            tableView.refresh();
        });
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

    /**
     * منفصلة عن {@link com.safwat.hr.controller.payroll.table.engine.column.TableColumnFactory}
     * لأنها بتبني عمود بديل بهيدر Label قابل للف (wrap) مختلف عن الشكل الافتراضي —
     * مسار استخدام خاص (استبدال عمود كامل)، مش إنشاء أولي.
     */
    public TableColumn<ObservableList<String>, String> createTextColumn(int columnIndex, String headerText) {
        TableColumn<ObservableList<String>, String> column = new TableColumn<>();

        Label headerLabel = new Label(headerText);
        headerLabel.setWrapText(true);
        headerLabel.setMaxWidth(150);
        headerLabel.setMaxHeight(90);
        headerLabel.setStyle("-fx-alignment: center; -fx-font-weight: bold; -fx-text-alignment: center;");

        Text text = new Text(headerText);
        text.setFont(Font.font("System", 12));
        int lineCount = (int) Math.ceil(text.getLayoutBounds().getWidth() / 120);
        double requiredHeight = Math.min(25 + (lineCount * 18), 90);

        headerLabel.setPrefHeight(requiredHeight);
        headerLabel.setMinHeight(25);

        StackPane headerContainer = new StackPane(headerLabel);
        headerContainer.setPrefHeight(requiredHeight);
        headerContainer.setMaxHeight(90);
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
            Platform.runLater(columnWidthAdjuster::adjustColumnWidths);
        });

        column.setStyle("-fx-alignment: center;");
        return column;
    }
}
