package com.safwat.hr.controller.employee.ui;

import com.safwat.hr.ui.table.TableSetupHelper;
import com.safwat.hr.ui.table.TableSetupHelper.ColumnAlign;
import com.safwat.hr.ui.table.TableSetupHelper.ColumnConfig;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Supplier;

/**
 * مكوّن مساعد لبناء جدول قابل للتعديل + زر إضافة + زر حفظ.
 *
 * الاستخدام:
 * <pre>
 *   EmployeeHistoryTable&lt;SocialStatusRow&gt; tbl = new EmployeeHistoryTable&lt;&gt;(
 *       "الحالة الاجتماعية",
 *       SocialStatusRow::new,         // صف فارغ جديد
 *       columns,                      // List<ColumnConfig<R>>
 *       rows,                         // البيانات الأولية
 *       saveHandler                   // SaveHandler<R>
 *   );
 *   parentVBox.getChildren().add(tbl.build());
 * </pre>
 *
 * @param <R> نوع صف الجدول — لازم يكون mutable (مش record)
 */
public class EmployeeHistoryTable<R> {

    /** callback للحفظ — بيستقبل قائمة الصفوف الحالية */
    @FunctionalInterface
    public interface SaveHandler<R> {
        /**
         * @param rows الصفوف الحالية في الجدول (بدون الصف الفارغ الأخير)
         * @throws Exception لو الـ API رجع خطأ
         */
        void save(List<R> rows) throws Exception;
    }

    private final String            sectionTitle;
    private final Supplier<R>       rowFactory;
    private final List<ColumnConfig<R>> columns;
    private final List<R>           initialData;
    private final SaveHandler<R>    saveHandler;

    private TableView<R> table;

    public EmployeeHistoryTable(String sectionTitle,
                                Supplier<R> rowFactory,
                                List<ColumnConfig<R>> columns,
                                List<R> initialData,
                                SaveHandler<R> saveHandler) {
        this.sectionTitle = sectionTitle;
        this.rowFactory   = rowFactory;
        this.columns      = columns;
        this.initialData  = initialData;
        this.saveHandler  = saveHandler;
    }

    /**
     * يبني الـ VBox الكامل (عنوان + جدول + أزرار).
     * استدعيه مرة واحدة وأضف الناتج للـ parent.
     */
    public VBox build() {
        // ── العنوان ──────────────────────────────────────────────────
        Label title = new Label(sectionTitle);
        title.getStyleClass().add("section-title");

        // ── الجدول ───────────────────────────────────────────────────
        table = new TableView<>();
        table.setPrefHeight(200);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableSetupHelper.setupGenericTable(
                table,
                columns,
                0,          // لا صفوف افتراضية — البيانات من initialData
                rowFactory
        );

        // نملأ بالبيانات الحقيقية بعد الـ setup
        table.getItems().clear();
        if (initialData != null) {
            table.getItems().addAll(initialData);
        }
        // صف فارغ في الآخر للإدخال
        table.getItems().add(rowFactory.get());

        // ── أزرار ────────────────────────────────────────────────────
        Button addBtn  = new Button("➕ إضافة سطر");
        Button saveBtn = new Button("💾 حفظ " + sectionTitle);

        addBtn.getStyleClass().add("btn-secondary");
        saveBtn.getStyleClass().add("btn-primary");

        addBtn.setOnAction(e -> addRow());
        saveBtn.setOnAction(e -> onSave());

        HBox buttons = new HBox(8, addBtn, saveBtn);
        buttons.setPadding(new Insets(6, 0, 0, 0));

        // ── تجميع ────────────────────────────────────────────────────
        VBox box = new VBox(6, title, table, buttons);
        box.setPadding(new Insets(10, 0, 10, 0));
        return box;
    }

    // ─────────────────────────────────────────────────────────────────
    //  عمليات داخلية
    // ─────────────────────────────────────────────────────────────────

    private void addRow() {
        R fresh = rowFactory.get();
        table.getItems().add(fresh);
        Platform.runLater(() -> {
            table.getSelectionModel().select(fresh);
            table.scrollTo(fresh);
            // ابدأ التعديل على أول عمود قابل للتعديل
            table.getColumns().stream()
                 .filter(TableColumn::isEditable)
                 .findFirst()
                 .ifPresent(col -> table.edit(table.getItems().size() - 1, col));
        });
    }

    private void onSave() {
        // نستبعد الصفوف الفارغة تماماً قبل الإرسال
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

    /**
     * يتحقق لو الصف فاضي — يشتغل مع أي Row type طالما
     * toString() بترجع شيء مفيد، أو override للـ subclass.
     * الـ subclass ممكن يعمل override لو محتاج منطق أدق.
     */
    protected boolean isNotEmpty(R row) {
        return row != null && !row.toString().isBlank();
    }

    /**
     * يرجّع الجدول للـ controller لو محتاج يقرأ الـ items بره.
     */
    public TableView<R> getTable() {
        return table;
    }

    /**
     * يحدّث بيانات الجدول بعد reload — مثلاً بعد ما الباك يرجع profile جديد.
     */
    public void reload(List<R> newData) {
        table.getItems().clear();
        if (newData != null) table.getItems().addAll(newData);
        table.getItems().add(rowFactory.get());
    }

    // ─────────────────────────────────────────────────────────────────
    private void showInfo(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }

    private void showError(String msg, Exception ex) {
        new Alert(Alert.AlertType.ERROR, msg + "\n" + ex.getMessage(), ButtonType.OK).showAndWait();
    }
}
