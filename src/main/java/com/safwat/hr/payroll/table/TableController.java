package com.safwat.hr.payroll.table;

import com.safwat.hr.payroll.table.PayrollApiClient.LookupResult;
import com.safwat.hr.payroll.table.PayrollApiClient.PayrollTableResponse;
import com.safwat.hr.payroll.table.PayrollApiClient.SearchEmployeeResult;
import com.safwat.hr.payroll.table.engine.ExcelEngine;
import com.safwat.hr.shared.ui.SearchDialog;
import com.safwat.hr.shared.ui.TextToSpeech;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * =====================================================
 * TableController — شيت إدخال البيرول (النسخة الجديدة)
 * =====================================================
 * <p>
 * حقول البحث النصية (TextField + 🔍 يفتح SearchDialog) + محرك الإكسيل (ExcelEngine)
 * + بحث اللوك أب من الباك + واجهة الاختيار عند تعدد النتائج + TTS.
 * </p>
 */
public class TableController implements Initializable {

    // ---- حقول البحث النصية + أزرار البحث ----
    @FXML
    private TextField fieldPayrollGroup;
    @FXML
    private TextField fieldElementGroup;
    @FXML
    private TextField fieldStatic;
    @FXML
    private Button btnSearchPayrollGroup;
    @FXML
    private Button btnSearchElementGroup;
    @FXML
    private Button btnSearchStatic;

    // ---- خيارات ----
    @FXML
    private RadioButton getMainCode;
    @FXML
    private RadioButton getSecondCode;
    @FXML
    private RadioButton toDown;
    @FXML
    private RadioButton toRight;
    @FXML
    private ToggleGroup group;
    @FXML
    private ToggleGroup group1;
    @FXML
    private CheckBox useVoices;
    @FXML
    private CheckBox privateCheck;

    // ---- أزرار ----
    @FXML
    private Button btn_Excel;
    @FXML
    private Button btn_Pdf;
    @FXML
    private Button btn_Refrech;
    @FXML
    private Button btn_clear;
    @FXML
    private Button btn_clearstatic;
    @FXML
    private Button btn_delete;
    @FXML
    private Button btn_insertRows;
    @FXML
    private Button btn_savestatic;
    @FXML
    private Button btn_tempSave;
    @FXML
    private Button btn_tempLoad;

    // ---- الجدول ----
    @FXML
    @SuppressWarnings("rawtypes")
    private TableView rawTableView;

    @SuppressWarnings("unchecked")
    private TableView<ObservableList<String>> tableView() {
        return (TableView<ObservableList<String>>) rawTableView;
    }

    private ExcelEngine engine;

    // ══════════════════════════════════════════════════════════
    //  التهيئة
    // ══════════════════════════════════════════════════════════

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        engine = new ExcelEngine(tableView());
        engine.initializeExcelFeatures();
        engine.setSearchHandler(this::handleSearch);

        setupSearchFields();
        setupDirectionOptions();
        setupHighlights();
    }

    private void setupDirectionOptions() {
        toDown.setSelected(true);
        getMainCode.setSelected(true);
        engine.setNavigationDirection(true);

        toDown.selectedProperty().addListener((obs, o, isDown) -> {
            if (isDown) engine.setNavigationDirection(true);
        });
        toRight.selectedProperty().addListener((obs, o, isRight) -> {
            if (isRight) engine.setNavigationDirection(false);
        });
    }

    private void setupHighlights() {
        // نفس القديم: تمييز التكرارات في القومي/الكود/الاسم + تمييز حالة التعيين
        engine.highlightDuplicates(2); // الرقم القومي
        engine.highlightDuplicates(3); // رقم الموظف
        engine.highlightDuplicates(4); // الاسم
        engine.highlightState(5);      // الحالة
    }

    // ══════════════════════════════════════════════════════════
    //  حقول البحث
    // ══════════════════════════════════════════════════════════

    private void setupSearchFields() {
        wireSearch(fieldPayrollGroup, btnSearchPayrollGroup,
                PayrollApiClient::getPayrollGroupNames,
                name -> updateTableViewHeaders(),
                "اختر مجموعة التعيين");

        wireSearch(fieldElementGroup, btnSearchElementGroup,
                PayrollApiClient::getElementGroupNames,
                name -> updateTableViewHeaders(),
                "اختر مجموعة العناصر");

        wireSearch(fieldStatic, btnSearchStatic,
                PayrollApiClient::getVisibleTableIds,
                this::loadPayrollTable,
                "اختر من الصرفيات الثابتة");
    }

    /**
     * ربط حقل نصي + زر 🔍 بواجهة البحث العامة (SearchDialog).
     * Enter في الحقل أو الضغط على الزر يفتح البحث، وبعد الاختيار
     * يُضبط النص ويُنفَّذ onSelect.
     */
    private void wireSearch(TextField field, Button searchButton,
                            Supplier<List<String>> fetcher,
                            Consumer<String> onSelect, String dialogTitle) {
        Runnable openSearch = () -> {
            List<String> items;
            try {
                items = fetcher.get();
            } catch (Exception ex) {
                items = List.of();
            }
            SearchDialog.forStrings()
                    .title(dialogTitle)
                    .data(items == null ? List.of() : items)
                    .searchPlaceholder("اكتب للتصفية...")
                    .owner(field.getScene() != null
                            ? (javafx.stage.Stage) field.getScene().getWindow() : null)
                    .show()
                    .ifPresent(value -> {
                        field.setText(value);
                        if (onSelect != null) {
                            onSelect.accept(value);
                        }
                    });
        };
        searchButton.setOnAction(e -> openSearch.run());
        field.setOnAction(e -> openSearch.run()); // Enter يفتح البحث
    }

    // ══════════════════════════════════════════════════════════
    //  رؤوس الأعمدة
    // ══════════════════════════════════════════════════════════

    private void updateTableViewHeaders() {
        String elementGroup = fieldElementGroup.getText();
        if (elementGroup == null || elementGroup.isBlank()) return;

        runBackground(() -> {
            List<String> elements = PayrollApiClient.getElementsByGroup(elementGroup);
            Platform.runLater(() -> {
                String[] allColumns = buildHeaders(elements);
                engine.updateColumnHeaders(allColumns);
            });
        });
    }

    private String[] buildHeaders(List<String> elements) {
        String[] staticTitles = {"البحث", "المسلسل", "الرقم القومى", "رقم الموظف", "الاسم", "الحالة"};
        String[] allColumns = new String[ExcelEngine.COLUMN_COUNT];
        System.arraycopy(staticTitles, 0, allColumns, 0, staticTitles.length);
        if (elements != null) {
            for (int i = 0; i < elements.size() && (6 + i) < allColumns.length; i++) {
                allColumns[6 + i] = elements.get(i);
            }
        }
        return allColumns;
    }

    // ══════════════════════════════════════════════════════════
    //  استدعاء صرفية ثابتة
    // ══════════════════════════════════════════════════════════

    private void loadPayrollTable(String tableId) {
        runBackground(() -> {
            var response = PayrollApiClient.getPayrollTable(tableId);
            Platform.runLater(() -> response.ifPresentOrElse(
                    this::applyLoadedTable,
                    () -> showError("لا توجد مجموعة بهذا الاسم أو لا تملك صلاحية رؤيتها")));
        });
    }

    private void applyLoadedTable(PayrollTableResponse r) {
        fieldPayrollGroup.setText(r.tableId());
        fieldElementGroup.setText(r.tableElement());
        privateCheck.setSelected("PRIVATE".equalsIgnoreCase(r.status()));
        engine.populateFromMap(r.tableData());
        updateTableViewHeaders();
    }

    // ══════════════════════════════════════════════════════════
    //  بحث اللوك أب (عمود البحث)
    // ══════════════════════════════════════════════════════════

    private void handleSearch(int rowIndex, String searchTerm) {
        // "الكود الثانوي" لسه بحث دقيق (تطابق تام) — نفس السلوك القديم المقصود
        if (getSecondCode.isSelected()) {
            runBackground(() -> {
                try {
                    List<LookupResult> results =
                            PayrollApiClient.lookup(searchTerm, PayrollApiClient.SEARCH_SECONDARY);
                    Platform.runLater(() -> routeSearchResults(results, rowIndex, searchTerm));
                } catch (Exception e) {
                    Platform.runLater(() -> showError(e.getMessage()));
                }
            });
            return;
        }

        // البحث الرئيسي: حر — يطابق أي جزء من الرقم القومي أو رقم الصرف أو الاسم معًا
        runBackground(() -> {
            try {
                List<SearchEmployeeResult> results = PayrollApiClient.searchEmployees(searchTerm);
                Platform.runLater(() -> routeSearchEmployeeResults(results, rowIndex, searchTerm));
            } catch (Exception e) {
                Platform.runLater(() -> showError(e.getMessage()));
            }
        });
    }

    // ---- مسار البحث الدقيق بالكود الثانوي (كما كان) ----

    private void routeSearchResults(List<LookupResult> results, int rowIndex, String searchTerm) {
        if (results == null || results.isEmpty()) {
            showError("لا توجد نتائج مطابقة للبحث");
            return;
        }
        if (results.size() == 1) {
            updateRowWithResult(rowIndex, results.get(0), searchTerm);
        } else {
            showSelectionDialog(results, rowIndex, searchTerm);
        }
    }

    private void updateRowWithResult(int rowIndex, LookupResult r, String searchTerm) {
        applyRowValues(rowIndex, searchTerm, r.nationalId(), r.payId(), r.empName(), r.empStatus(), r.assignmentClass());
    }

    /**
     * نافذة الاختيار عند تعدد نتائج البحث الدقيق (الكود الثانوي)
     */
    private void showSelectionDialog(List<LookupResult> results, int rowIndex, String searchTerm) {
        List<Object[]> rows = results.stream()
                .map(r -> new Object[]{r.nationalId(), r.payId(), r.empName(),
                        r.empStatus(), r.payManagement()})
                .toList();

        SearchDialog.builder()
                .title("اختر الموظف الصحيح — نتائج: " + searchTerm)
                .headers(new String[]{"الرقم القومي", "رقم الموظف", "الاسم", "الحالة", "الإدارة"})
                .data(rows)
                .searchPlaceholder("تصفية النتائج...")
                .show()
                .ifPresent(selected -> {
                    LookupResult chosen = results.get(rows.indexOf(selected));
                    updateRowWithResult(rowIndex, chosen, searchTerm);
                });
    }

    // ---- مسار البحث الحر (الرقم القومي / رقم الصرف / الاسم — أي جزء منهم) ----

    private void routeSearchEmployeeResults(List<SearchEmployeeResult> results, int rowIndex, String searchTerm) {
        if (results == null || results.isEmpty()) {
            showError("لا توجد نتائج مطابقة للبحث");
            return;
        }
        if (results.size() == 1) {
            updateRowWithSearchResult(rowIndex, results.get(0), searchTerm);
        } else {
            showSearchSelectionDialog(results, rowIndex, searchTerm);
        }
    }

    private void updateRowWithSearchResult(int rowIndex, SearchEmployeeResult r, String searchTerm) {
        applyRowValues(rowIndex, searchTerm, r.nationalId(), r.payId(), r.empName(), r.empStatus(), r.assignmentClass());
    }

    /**
     * نافذة الاختيار عند تعدد نتائج البحث الحر — بتعرض الرقم القومي، رقم الصرف،
     * الاسم، الحالة، الفئة، والإدارة عشان المستخدم يقدر يميّز بينهم بسهولة.
     */
    private void showSearchSelectionDialog(List<SearchEmployeeResult> results, int rowIndex, String searchTerm) {
        List<Object[]> rows = results.stream()
                .map(r -> new Object[]{r.nationalId(), r.payId(), r.empName(),
                        r.empStatus(), r.assignmentClass(), r.payManagement()})
                .toList();

        SearchDialog.builder()
                .title("اختر الموظف الصحيح — نتائج: " + searchTerm)
                .headers(new String[]{"الرقم القومي", "رقم الصرف", "الاسم", "الحالة", "الفئة", "الإدارة"})
                .data(rows)
                .searchPlaceholder("تصفية النتائج...")
                .show()
                .ifPresent(selected -> {
                    SearchEmployeeResult chosen = results.get(rows.indexOf(selected));
                    updateRowWithSearchResult(rowIndex, chosen, searchTerm);
                });
    }

    // ---- مشترك بين المسارين ----

    private void applyRowValues(int rowIndex, String searchTerm,
                                String nationalId, String payId, String empName, String empStatus, String assignmentClass) {
        engine.updateCellValue(rowIndex, 0, searchTerm);
        engine.updateCellValue(rowIndex, 2, nationalId);
        engine.updateCellValue(rowIndex, 3, payId);
        engine.updateCellValue(rowIndex, 4, empName);
        engine.updateCellValue(rowIndex, 5, empStatus);
        engine.updateCellValue(rowIndex, 6, assignmentClass);


        if (useVoices.isSelected()) {
            TextToSpeech.speak(empName);
        }

        engine.adjustColumnWidths();
        engine.moveAfterSearch(rowIndex);
    }

    // ══════════════════════════════════════════════════════════
    //  الحفظ والحذف
    // ══════════════════════════════════════════════════════════

    @FXML
    void savePayrollTable(ActionEvent event) {
        String tableId = fieldPayrollGroup.getText();
        if (tableId == null || tableId.isBlank()) {
            showError("اكتب اسم الصرفية أولاً في حقل الصرفيات الثابتة");
            return;
        }
        runBackground(() -> {
            try {
                String status = privateCheck.isSelected() ? "PRIVATE" : "PUBLIC";
                PayrollApiClient.savePayrollTable(tableId,
                        fieldElementGroup.getText(), status, engine.getDataAsMap());
                Platform.runLater(() -> showInfo("تم الحفظ بنجاح"));
            } catch (Exception e) {
                Platform.runLater(() -> showError(e.getMessage()));
            }
        });
    }

    @FXML
    void deleteFromPayrollTable(ActionEvent event) {
        String tableId = fieldStatic.getText();
        if (tableId == null || tableId.isBlank()) {
            showError("اختر اسم الصرفية أولاً");
            return;
        }
        if (!confirm("سيتم حذف بيانات المجموعة المحفوظة هل تريد الاستمرار؟")) return;

        runBackground(() -> {
            try {
                PayrollApiClient.deletePayrollTable(tableId);
                Platform.runLater(() -> {
                    showInfo("تم الحذف بنجاح");
                    fieldStatic.clear();
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError(e.getMessage()));
            }
        });
    }

    // ══════════════════════════════════════════════════════════
    //  الحفظ المؤقت
    // ══════════════════════════════════════════════════════════

    @FXML
    void tempSave(ActionEvent event) {
        runBackground(() -> {
            try {
                PayrollApiClient.tempSave(fieldPayrollGroup.getText(),
                        fieldElementGroup.getText(), engine.getDataAsMap());
                Platform.runLater(() -> showInfo(
                        "تم الحفظ المؤقت للبيانات يمكنك إغلاق التطبيق والعودة لاستئناف إدخال البيانات من خلال زر استدعاء البيانات"));
            } catch (Exception e) {
                Platform.runLater(() -> showError(e.getMessage()));
            }
        });
    }

    @FXML
    void tempLoad(ActionEvent event) {
        if (!confirm("سيتم استدعاء البيانات المحفوظة — أي بيانات أدخلتها بعد آخر حفظ ستفقدها\nاضغط نعم للاستمرار")) {
            return;
        }
        runBackground(() -> {
            try {
                var temp = PayrollApiClient.tempLoad();
                Platform.runLater(() -> temp.ifPresentOrElse(
                        t -> {
                            fieldPayrollGroup.setText(t.payrollGroup());
                            fieldElementGroup.setText(t.elementGroup());
                            engine.populateFromMap(t.tableData());
                            updateTableViewHeaders();
                        },
                        () -> showError("لا توجد بيانات محفوظة مؤقتاً")));
            } catch (Exception e) {
                Platform.runLater(() -> showError(e.getMessage()));
            }
        });
    }

    // ══════════════════════════════════════════════════════════
    //  التصدير
    // ══════════════════════════════════════════════════════════

    @FXML
    void exportToExcel(ActionEvent event) {
        exportSheet("excel");
    }

    @FXML
    void exportToPDF(ActionEvent event) {
        exportSheet("pdf");
    }

    private void exportSheet(String format) {
        String firstTitle = fieldPayrollGroup.getText().isBlank()
                ? "صرفية" : fieldPayrollGroup.getText();
        List<String> headers = engine.getCurrentHeaders();
        var tableData = engine.getDataAsMap();

        runBackground(() -> {
            try {
                long reportId = PayrollApiClient.exportSheet(format, firstTitle,
                        fieldElementGroup.getText(), headers, tableData);
                Platform.runLater(() ->
                        showInfo("تم تقديم الطلب بنجاح رقم الطلب " + reportId));
            } catch (Exception e) {
                Platform.runLater(() -> showError(e.getMessage()));
            }
        });
    }

    // ══════════════════════════════════════════════════════════
    //  أدوات الجدول
    // ══════════════════════════════════════════════════════════

    @FXML
    void add20Rows(ActionEvent event) {
        engine.addMultipleRows(20);
    }

    @FXML
    void clearTableView(ActionEvent event) {
        if (confirm("تأكيد مسح بيانات الجدول وتفريغه")) {
            engine.clearTable();
        }
    }

    @FXML
    void deleteEmptyRows(ActionEvent event) {
        engine.deleteEmptyRows();
    }

    @FXML
    void refreshTable(ActionEvent event) {
        engine.quickRefresh();
    }

    // ══════════════════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════════════════

    private void runBackground(Runnable task) {
        new Thread(task, "payroll-api").start();
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).show();
    }

    private void showInfo(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).show();
    }

    private boolean confirm(String msg) {
        return new Alert(Alert.AlertType.CONFIRMATION, msg,
                ButtonType.YES, ButtonType.NO)
                .showAndWait()
                .filter(b -> b == ButtonType.YES)
                .isPresent();
    }

    @SuppressWarnings("unused")
    private static ObservableList<String> emptyRow() {
        return FXCollections.observableArrayList();
    }
}