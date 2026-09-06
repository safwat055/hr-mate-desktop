package com.safwat.hr.controller.payroll.records;

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

    private static final double MAX_HEADER_HEIGHT = 50;

    // ===================== الإصدارات القديمة (للتوافق) =====================

    public static void fillTable(TableView<ObservableList<String>> tableView,
                                 List<String> headers,
                                 List<Object[]> rows,
                                 boolean editable) {
        fillTable(tableView, headers, rows, editable, null);
    }

    public static void fillTable(TableView<ObservableList<String>> tableView,
                                 List<String> headers,
                                 List<Object[]> rows,
                                 boolean editable,
                                 Double maxColumnWidth) {
        fillTable(tableView, headers, rows, editable, maxColumnWidth, true);
    }

    // ===================== الإصدار الجديد مع التحكم بالعمود الأول =====================

    /**
     * ملء الجدول ببيانات متعددة الصفوف.
     *
     * @param maxColumnWidth             الحد الأقصى لعرض كل عمود (بالبكسل)، أو null لعدم فرض حد أقصى.
     * @param applyMaxWidthToFirstColumn إذا كان true، يطبق maxColumnWidth على العمود الأول أيضاً؛
     *                                   إذا false، يتجاوز العمود الأول الحد الأقصى ويكون عرضه حسب المحتوى.
     */
    public static void fillTable(TableView<ObservableList<String>> tableView,
                                 List<String> headers,
                                 List<Object[]> rows,
                                 boolean editable,
                                 Double maxColumnWidth,
                                 boolean applyMaxWidthToFirstColumn) {
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

            // تطبيق الحد الأقصى للعرض على العمود الأول فقط إذا كان مسموحاً
            boolean applyMax = (maxColumnWidth != null) &&
                    (i != 0 || applyMaxWidthToFirstColumn);  // أول عمود و applyMaxWidthToFirstColumn = true

            if (applyMax) {
                column.setMaxWidth(maxColumnWidth);
                if (column.getPrefWidth() > maxColumnWidth) {
                    column.setPrefWidth(maxColumnWidth);
                }
            }

            configureHeader(column, headerText, applyMax ? maxColumnWidth : null);

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

    // ===================== دوال `fillTableWithSingleRow` =====================

    public static void fillTableWithSingleRow(TableView<ObservableList<String>> tableView,
                                              List<String> headers,
                                              List<String> rowData,
                                              boolean editable) {
        fillTableWithSingleRow(tableView, headers, rowData, editable, null);
    }

    public static void fillTableWithSingleRow(TableView<ObservableList<String>> tableView,
                                              List<String> headers,
                                              List<String> rowData,
                                              boolean editable,
                                              Double maxColumnWidth) {
        Object[] rowArray = rowData.toArray(new Object[0]);
        List<Object[]> rows = Collections.singletonList(rowArray);
        fillTable(tableView, headers, rows, editable, maxColumnWidth);
    }

    public static void fillTableWithSingleRow(TableView<ObservableList<String>> tableView,
                                              List<String> headers,
                                              Object[] rowData,
                                              boolean editable) {
        fillTableWithSingleRow(tableView, headers, rowData, editable, null);
    }

    public static void fillTableWithSingleRow(TableView<ObservableList<String>> tableView,
                                              List<String> headers,
                                              Object[] rowData,
                                              boolean editable,
                                              Double maxColumnWidth) {
        List<Object[]> rows = Collections.singletonList(rowData);
        fillTable(tableView, headers, rows, editable, maxColumnWidth);
    }

    // ===================== المساعدات (Helpers) =====================

    private static Callback<TableColumn<ObservableList<String>, String>,
            TableCell<ObservableList<String>, String>> centeredCellFactory(boolean editable) {

        if (editable) {
            return col -> {
                TextFieldTableCell<ObservableList<String>, String> cell =
                        new TextFieldTableCell<>(new DefaultStringConverter()) {
                            @Override
                            public void updateItem(String item, boolean empty) {
                                super.updateItem(item, empty);
                                applyTooltip(this, item, empty);
                                setStyle("-fx-alignment: CENTER;");
                            }
                        };
                return cell;
            };
        }

        return col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                applyTooltip(this, item, empty);
                setStyle("-fx-alignment: CENTER;");
            }
        };
    }

    private static void applyTooltip(TableCell<?, ?> cell, String item, boolean empty) {
        if (empty || item == null || item.isBlank()) {
            cell.setTooltip(null);
        } else {
            Tooltip tooltip = new Tooltip(item);
            tooltip.setWrapText(true);
            tooltip.setMaxWidth(300);
            cell.setTooltip(tooltip);
        }
    }

    private static final double DEFAULT_HEADER_WIDTH = 100;

    private static void configureHeader(TableColumn<ObservableList<String>, String> column,
                                        String headerText,
                                        Double maxColumnWidth) {
        double headerWidth = (maxColumnWidth != null) ? maxColumnWidth : DEFAULT_HEADER_WIDTH;

        Label headerLabel = new Label(headerText);
        headerLabel.setWrapText(false);
        headerLabel.setAlignment(Pos.CENTER);
        headerLabel.setMaxWidth(headerWidth);
        headerLabel.setPrefWidth(headerWidth);
        headerLabel.setPrefHeight(MAX_HEADER_HEIGHT);
        headerLabel.setMinHeight(MAX_HEADER_HEIGHT);
        headerLabel.setMaxHeight(MAX_HEADER_HEIGHT);

        headerLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        headerLabel.setTooltip(new Tooltip(headerText));

        StackPane headerPane = new StackPane(headerLabel);
        headerPane.setPrefWidth(headerWidth);
        headerPane.setMinHeight(MAX_HEADER_HEIGHT);
        headerPane.setPrefHeight(MAX_HEADER_HEIGHT);
        headerPane.setMaxHeight(MAX_HEADER_HEIGHT);
        headerPane.setAlignment(Pos.CENTER);

        column.setText("");
        column.setGraphic(headerPane);
    }

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