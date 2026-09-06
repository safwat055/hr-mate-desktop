package com.safwat.hr.payroll.records;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Callback;
import javafx.util.converter.DefaultStringConverter;

import java.util.Collections;
import java.util.List;

public class TableUtils {

    /**
     * الحد الأقصى لارتفاع صف رؤوس الأعمدة بالكامل (بالبكسل).
     */
    private static final double MAX_HEADER_HEIGHT = 60;

    /**
     * ملء الجدول ببيانات متعددة الصفوف.
     * (بدون حد أقصى لعرض الأعمدة — النسخة القديمة، للتوافق مع الكود الحالي)
     */
    public static void fillTable(TableView<ObservableList<String>> tableView,
                                 List<String> headers,
                                 List<Object[]> rows,
                                 boolean editable) {
        fillTable(tableView, headers, rows, editable, null);
    }

    /**
     * ملء الجدول ببيانات متعددة الصفوف، مع إمكانية تحديد حد أقصى لعرض الأعمدة.
     *
     * @param maxColumnWidth الحد الأقصى لعرض كل عمود (بالبكسل)، أو null لعدم فرض حد أقصى.
     */
    public static void fillTable(TableView<ObservableList<String>> tableView,
                                 List<String> headers,
                                 List<Object[]> rows,
                                 boolean editable,
                                 Double maxColumnWidth) {
        tableView.getColumns().clear();
        for (int i = 0; i < headers.size(); i++) {
            final int colIndex = i;
            String headerText = headers.get(i);

            TableColumn<ObservableList<String>, String> column = new TableColumn<>(headerText);
            column.setCellValueFactory(data -> {
                ObservableList<String> row = data.getValue();
                if (row.size() > colIndex) {
                    return new javafx.beans.property.SimpleStringProperty(row.get(colIndex));
                }
                return new javafx.beans.property.SimpleStringProperty("");
            });
            column.setEditable(editable);
            column.setCellFactory(centeredCellFactory(editable));

            if (editable) {
                column.setOnEditCommit(event -> {
                    ObservableList<String> row = event.getRowValue();
                    if (row.size() > colIndex) {
                        row.set(colIndex, event.getNewValue());
                    }
                });
            }

            if (maxColumnWidth != null) {
                column.setMaxWidth(maxColumnWidth);
                if (column.getPrefWidth() > maxColumnWidth) {
                    column.setPrefWidth(maxColumnWidth);
                }
            }

            // رأس العمود: تول تيب باسم العمود + دعم الانقسام لأكتر من سطر
            configureHeader(column, headerText, maxColumnWidth);

            tableView.getColumns().add(column);
        }

        limitHeaderRowHeight(tableView);

        ObservableList<ObservableList<String>> tableData = FXCollections.observableArrayList();
        for (Object[] row : rows) {
            ObservableList<String> rowList = FXCollections.observableArrayList();
            for (Object value : row) {
                rowList.add(value == null ? "" : value.toString());
            }
            tableData.add(rowList);
        }
        tableView.setItems(tableData);
    }

    /**
     * ملء الجدول بصف واحد (بيانات من List<String>).
     * (بدون حد أقصى لعرض الأعمدة — النسخة القديمة)
     */
    public static void fillTableWithSingleRow(TableView<ObservableList<String>> tableView,
                                              List<String> headers,
                                              List<String> rowData,
                                              boolean editable) {
        fillTableWithSingleRow(tableView, headers, rowData, editable, null);
    }

    /**
     * ملء الجدول بصف واحد (بيانات من List<String>)، مع إمكانية تحديد حد أقصى لعرض الأعمدة.
     */
    public static void fillTableWithSingleRow(TableView<ObservableList<String>> tableView,
                                              List<String> headers,
                                              List<String> rowData,
                                              boolean editable,
                                              Double maxColumnWidth) {
        Object[] rowArray = rowData.toArray(new Object[0]);
        List<Object[]> rows = Collections.singletonList(rowArray);
        fillTable(tableView, headers, rows, editable, maxColumnWidth);
    }

    /**
     * ملء الجدول بصف واحد (بيانات من Object[]).
     * (بدون حد أقصى لعرض الأعمدة — النسخة القديمة)
     */
    public static void fillTableWithSingleRow(TableView<ObservableList<String>> tableView,
                                              List<String> headers,
                                              Object[] rowData,
                                              boolean editable) {
        fillTableWithSingleRow(tableView, headers, rowData, editable, null);
    }

    /**
     * ملء الجدول بصف واحد (بيانات من Object[])، مع إمكانية تحديد حد أقصى لعرض الأعمدة.
     */
    public static void fillTableWithSingleRow(TableView<ObservableList<String>> tableView,
                                              List<String> headers,
                                              Object[] rowData,
                                              boolean editable,
                                              Double maxColumnWidth) {
        List<Object[]> rows = Collections.singletonList(rowData);
        fillTable(tableView, headers, rows, editable, maxColumnWidth);
    }

    // ═══════════════════════════════════════════════════════════
    //  Helpers
    // ═══════════════════════════════════════════════════════════

    /**
     * ينشئ Cell Factory توسّط النص في منتصف الخلية، سواء كان العمود
     * قابلاً للتعديل (TextField) أو للعرض فقط (Label).
     */
    private static Callback<TableColumn<ObservableList<String>, String>,
            TableCell<ObservableList<String>, String>> centeredCellFactory(boolean editable) {

        if (editable) {
            return col -> {
                TextFieldTableCell<ObservableList<String>, String> cell =
                        new TextFieldTableCell<>(new DefaultStringConverter());
                cell.setStyle("-fx-alignment: CENTER;");
                return cell;
            };
        }

        return col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setStyle("-fx-alignment: CENTER;");
            }
        };
    }

    /**
     * العرض الافتراضي لرأس العمود لو مفيش maxColumnWidth محدد.
     * (نفس عرض العمود الافتراضي في JavaFX تقريبًا)
     */
    private static final double DEFAULT_HEADER_WIDTH = 100;


    /**
     * يستبدل نص رأس العمود بـ TextArea غير قابل للتعديل (شكله زي Label
     * تمامًا بعد الـ CSS) بدل Label العادية.
     * <p>
     * السبب: خلل معروف في محرك النصوص بتاع JavaFX (PrismTextLayout) بيرمي
     * ArrayIndexOutOfBoundsException عند حساب تخطيط نص ملفوف (wrapText)
     * لبعض تركيبات العرض/طول النص — سواء كان الحساب بييجي من عملية القص
     * (computeClippedWrappedText) أو من حساب الارتفاع المفضّل
     * (computeTextHeight). المسارين بيمرّوا من PrismTextLayout الواحدة
     * المستخدمة في Labeled (Label/Button/...).
     * <p>
     * TextArea بتستخدم مسار تفاف نص مختلف تمامًا (تقسيم فقرات بمنطقها
     * الخاص، مش نفس استدعاء PrismTextLayout.layout المستخدم في الحسابات
     * دي)، فبتتفادى نفس الخلل.
     */
    private static void configureHeader(TableColumn<ObservableList<String>, String> column,
                                        String headerText,
                                        Double maxColumnWidth) {
        double headerWidth = (maxColumnWidth != null) ? maxColumnWidth : DEFAULT_HEADER_WIDTH;

        // ✅ استخدام Label بدون wrapText
        Label headerLabel = new Label(headerText);
        headerLabel.setWrapText(false);
        headerLabel.setAlignment(Pos.CENTER);
        headerLabel.setMaxWidth(headerWidth);
        headerLabel.setPrefWidth(headerWidth);
        headerLabel.setPrefHeight(MAX_HEADER_HEIGHT);
        headerLabel.setMinHeight(MAX_HEADER_HEIGHT);
        headerLabel.setMaxHeight(MAX_HEADER_HEIGHT);

        // قص النص بعلامة ... إذا كان طويلاً
        headerLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        headerLabel.setTooltip(new Tooltip(headerText));

        // وضع Label في StackPane لتوسيطه
        StackPane headerPane = new StackPane(headerLabel);
        headerPane.setPrefWidth(headerWidth);
        headerPane.setMinHeight(MAX_HEADER_HEIGHT);
        headerPane.setPrefHeight(MAX_HEADER_HEIGHT);
        headerPane.setMaxHeight(MAX_HEADER_HEIGHT);
        headerPane.setAlignment(Pos.CENTER);

        column.setText("");
        column.setGraphic(headerPane);
    }

    /**
     * يفرض حداً أقصى لارتفاع صف رؤوس الأعمدة بالكامل (MAX_HEADER_HEIGHT)،
     * بحيث لو رؤوس متعددة السطور، الصف يكبر لحد الحد الأقصى بدل ما
     * يتمدد بلا حدود أو يقص المحتوى فجأة.
     * <p>
     * التنفيذ الفعلي بيتأجل لحد ما الـ skin يبقى جاهز، لأن عنصر
     * "column-header-background" مش موجود قبل عرض الجدول فعليًا.
     */
    private static void limitHeaderRowHeight(TableView<?> tableView) {
        Runnable apply = () -> {
            Region headerBg = (Region) tableView.lookup(".column-header-background");
            if (headerBg != null) {
                headerBg.setMinHeight(MAX_HEADER_HEIGHT);
                headerBg.setPrefHeight(MAX_HEADER_HEIGHT);
                headerBg.setMaxHeight(MAX_HEADER_HEIGHT);
            }
        };

        if (tableView.getSkin() != null) {
            Platform.runLater(apply);
        } else {
            tableView.skinProperty().addListener((obs, oldSkin, newSkin) -> {
                if (newSkin != null) {
                    Platform.runLater(apply);
                }
            });
        }
    }
}