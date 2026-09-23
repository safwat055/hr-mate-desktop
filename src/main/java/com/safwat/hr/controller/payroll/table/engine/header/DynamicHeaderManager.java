package com.safwat.hr.controller.payroll.table.engine.header;

import com.safwat.hr.controller.payroll.table.engine.TableSchema;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.ContentDisplay;
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
 *
 * <p>سبب تكرار النص: JavaFX بيحط الـ graphic بتاع العمود جوه Label
 * داخلي في الـ TableColumnHeader — اللي بيرسم الـ text والـ graphic
 * مع بعض. الحل: بعد ما الـ scene graph يتبنى، بنعمل setContentDisplay
 * على الـ parent Label الداخلي ده عشان يرسم الـ graphic بس.</p>
 */
public class DynamicHeaderManager {

    private final TableView<ObservableList<String>> tableView;
    private final ColumnWidthAdjuster columnWidthAdjuster;
    private final Map<Integer, Label> dynamicHeaderLabels = new HashMap<>();

    /**
     * flag عشان نعمل fixParentLabels مرة واحدة بس بعد أول تحديث
     */
    private boolean parentLabelsFixed = false;

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
            // 1. حدّث نص كل Label
            for (int i = TableSchema.FIRST_DYNAMIC_COL; i < TableSchema.COLUMN_COUNT; i++) {
                String headerValue = (i < headers.length) ? headers[i] : null;
                if (headerValue == null || headerValue.isBlank() || headerValue.equals("ملاحظات")) {
                    headerValue = "عمود " + (i + 1);
                }
                Label label = dynamicHeaderLabels.get(i);
                if (label != null) label.setText(headerValue);
            }


            // 2. إصلاح الـ parent Labels الداخلية بتاعة JavaFX
            //    (بيتعمل مرة واحدة بس — بعدها الـ parent مش بيتغير)
            if (!parentLabelsFixed) {
                fixParentLabels();
                parentLabelsFixed = true;
            }

            columnWidthAdjuster.adjustColumnWidths();
            tableView.refresh();
        });
    }

    /**
     * الـ TableColumnHeader في JavaFX بيحط الـ graphic جوه Label داخلي
     * بيرسم الـ text والـ graphic مع بعض — ده سبب تكرار النص.
     * الحل: نعمل GRAPHIC_ONLY على الـ parent Label ده.
     */
    private void fixParentLabels() {
        for (int i = TableSchema.FIRST_DYNAMIC_COL; i < TableSchema.COLUMN_COUNT; i++) {
            TableColumn<?, ?> col = tableView.getColumns().get(i);
            javafx.scene.Node graphic = col.getGraphic();
            if (graphic != null && graphic.getParent() instanceof Label parentLabel) {
                parentLabel.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }
        }
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

        // إصلاح الـ parent Label بعد ما يتضاف للـ scene
        column.graphicProperty().addListener((obs, oldG, newG) -> {
            if (newG != null) {
                Platform.runLater(() -> {
                    if (newG.getParent() instanceof Label parentLabel) {
                        parentLabel.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    }
                });
            }
        });

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