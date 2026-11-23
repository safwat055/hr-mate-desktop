package com.safwat.hr.controller.hr.employee.ui;

import com.safwat.hr.ui.table.TableSetupHelper;
import com.safwat.hr.ui.table.TableSetupHelper.ColumnConfig;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * مكوّن مساعد لبناء جدول قابل للتعديل + زر إضافة + زر حفظ + (اختياري) زر حذف لكل صف.
 *
 * @param <R> نوع صف الجدول — لازم يكون mutable (مش record)
 */
public class EmployeeHistoryTable<R> {

    @FunctionalInterface
    public interface SaveHandler<R> {
        void save(List<R> rows) throws Exception;
    }

    private final String sectionTitle;
    private final Supplier<R> rowFactory;
    private final List<ColumnConfig<R>> columns;
    private final List<R> initialData;
    private final SaveHandler<R> saveHandler;
    private final boolean withDeleteColumn;

    @Getter
    private TableView<R> table;

    public EmployeeHistoryTable(String sectionTitle,
                                Supplier<R> rowFactory,
                                List<ColumnConfig<R>> columns,
                                List<R> initialData,
                                SaveHandler<R> saveHandler) {
        this(sectionTitle, rowFactory, columns, initialData, saveHandler, false);
    }

    public EmployeeHistoryTable(String sectionTitle,
                                Supplier<R> rowFactory,
                                List<ColumnConfig<R>> columns,
                                List<R> initialData,
                                SaveHandler<R> saveHandler,
                                boolean withDeleteColumn) {
        this.sectionTitle = sectionTitle;
        this.rowFactory = rowFactory;
        this.columns = columns;
        this.initialData = initialData;
        this.saveHandler = saveHandler;
        this.withDeleteColumn = withDeleteColumn;
    }

    public VBox build() {
        Label title = new Label(sectionTitle);
        title.getStyleClass().add("section-title");

        table = new TableView<>();
        table.setPrefHeight(200);
        VBox.setVgrow(table, Priority.ALWAYS);

        List<ColumnConfig<R>> effectiveCols = new ArrayList<>(columns);
        if (withDeleteColumn) {
            effectiveCols.add(buildDeleteColumn());
        }

        TableSetupHelper.setupGenericTable(
                table,
                effectiveCols,
                0,
                rowFactory
        );

        table.getItems().clear();
        if (initialData != null) {
            table.getItems().addAll(initialData);
        }
        table.getItems().add(rowFactory.get());

        Button addBtn = new Button("➕ إضافة سطر");
        Button saveBtn = new Button("💾 حفظ " + sectionTitle);

        addBtn.getStyleClass().add("btn-secondary");
        saveBtn.getStyleClass().add("btn-primary");

        addBtn.setOnAction(e -> addRow());
        saveBtn.setOnAction(e -> onSave());

        HBox buttons = new HBox(8, addBtn, saveBtn);
        buttons.setPadding(new Insets(6, 0, 0, 0));

        VBox box = new VBox(6, title, table, buttons);
        box.setPadding(new Insets(10, 0, 10, 0));
        return box;
    }

    /**
     * عمود حذف — زر 🗑 لكل صف.
     * الحذف فرونت فقط: الصف يتشال من items، والـ bulk replace على السيرفر
     * هيستبدل السجل بالكامل فالصف المحذوف مش هيروح.
     */
    private ColumnConfig<R> buildDeleteColumn() {
        return new ColumnConfig<>(
                "", 60,
                tc -> new TableCell<>() {
                    private final Button del = new Button("🗑");
                    {
                        del.getStyleClass().add("btn-icon-danger");
                        del.setTooltip(new Tooltip("حذف السطر"));
                        del.setOnAction(ev -> {
                            int idx = getIndex();
                            if (idx < 0 || idx >= getTableView().getItems().size()) return;
                            R item = getTableView().getItems().get(idx);
                            getTableView().getItems().remove(item);

                            // لو مبقاش فيه صفوف غير فاضية، أضف صف فارغ
                            boolean hasNonEmpty = getTableView().getItems().stream()
                                    .anyMatch(EmployeeHistoryTable.this::isNotEmpty);
                            if (!hasNonEmpty) {
                                getTableView().getItems().add(rowFactory.get());
                            }
                        });
                    }

                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        setGraphic(empty ? null : del);
                        setAlignment(Pos.CENTER);
                    }
                }
        );
    }

    private void addRow() {
        // احذف الصف الفارغ الأخير الأول لو موجود
        int lastIdx = table.getItems().size() - 1;
        if (lastIdx >= 0 && !isNotEmpty(table.getItems().get(lastIdx))) {
            table.getItems().remove(lastIdx);
        }
        R fresh = rowFactory.get();
        table.getItems().add(fresh);
        Platform.runLater(() -> {
            table.getSelectionModel().select(fresh);
            table.scrollTo(fresh);
            table.getColumns().stream()
                    .filter(TableColumn::isEditable)
                    .findFirst()
                    .ifPresent(col -> table.edit(table.getItems().size() - 1, col));
        });
    }

    private void onSave() {
        List<R> toSave = table.getItems().stream()
                .filter(this::isNotEmpty)
                .toList();
        try {
            saveHandler.save(toSave);
            showInfo("تم حفظ " + sectionTitle + " بنجاح");
        } catch (Exception ex) {
            showError("تعذر حفظ " + sectionTitle, ex);
        }
    }

    protected boolean isNotEmpty(R row) {
        return row != null && !row.toString().isBlank();
    }

    public void reload(List<R> newData) {
        table.getItems().clear();
        if (newData != null) table.getItems().addAll(newData);
        table.getItems().add(rowFactory.get());
    }

    private void showInfo(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }

    private void showError(String msg, Exception ex) {
        new Alert(Alert.AlertType.ERROR, msg + "\n" + ex.getMessage(), ButtonType.OK).showAndWait();
    }
}