package com.safwat.hr.scale;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Controller شاشة احتساب السلم الوظيفي.
 *
 * <p><b>التغيير الجوهري عن النسخة القديمة:</b>
 * بدل 24+ حقل يدوي للإضافة والخصم ({@code mogardAddDate1..6}, ...)،
 * أصبح عندنا 4 {@link TableView} قابلة للتعديل:
 * <ul>
 *   <li>{@link #table_mogardAdd}  — إضافة للمجرد</li>
 *   <li>{@link #table_mogardRival} — خصم من المجرد</li>
 *   <li>{@link #table_bounsAdd}   — إضافة للعلاوات</li>
 *   <li>{@link #table_bounsRival} — خصم من العلاوات</li>
 * </ul>
 *
 * <p>المستخدم يضيف صفوفاً بزر "+" ويحذفها بزر "-" بدون حد أقصى.
 */
public class ScaleController implements Initializable {

    // ─────────────────────────────────────────────
    //  FXML — شريط البحث
    // ─────────────────────────────────────────────

    private final ObservableList<DateValueRow> mogardAddData = FXCollections.observableArrayList();
    private final ObservableList<DateValueRow> mogardRivalData = FXCollections.observableArrayList();
    private final ObservableList<DateValueRow> bounsAddData = FXCollections.observableArrayList();
    private final ObservableList<DateValueRow> bounsRivalData = FXCollections.observableArrayList();
    @FXML
    private TextField txt_Management;

    // ─────────────────────────────────────────────
    //  FXML — شريط الأدوات
    // ─────────────────────────────────────────────
    @FXML
    private TextField txt_empName;
    @FXML
    private TextField txt_empCode;
    @FXML
    private TextField txt_nationalId;
    @FXML
    private Button btn_search;
    @FXML
    private TextField txt_group;
    @FXML
    private TextField txt_law;
    @FXML
    private TextField txt_code;
    @FXML
    private TextField txt_startDegree;

    // ─────────────────────────────────────────────
    //  FXML — بيانات المؤهلات
    // ─────────────────────────────────────────────
    @FXML
    private Button btn_calculate;
    @FXML
    private Button btn_pdf;
    @FXML
    private Button btn_save;
    @FXML
    private Button btn_clear;
    @FXML
    private TextField txt_startDate;
    @FXML
    private TextField txt_backStart;
    @FXML
    private TextField txt_debloma;
    @FXML
    private TextField txt_magester;
    @FXML
    private TextField txt_doctoraa;
    @FXML
    private TextField txt_ectra;

    // ─────────────────────────────────────────────
    //  FXML — نتائج التسكين
    // ─────────────────────────────────────────────
    @FXML
    private TextField txt_regrade3;
    @FXML
    private TextField txt_regrade4;
    @FXML
    private TextField txt_regrade5;
    @FXML
    private TextField txt_backRegrade;
    @FXML
    private TextField yearUp;
    @FXML
    private TextField yearNoUp;
    @FXML
    private TextField gpUp;
    @FXML
    private TextField gpNoUp;
    @FXML
    private TextField yearsBack;

    // ─────────────────────────────────────────────
    //  FXML — جداول الترقيات والتشجيعيات
    // ─────────────────────────────────────────────
    @FXML
    private TextField date_kader;
    @FXML
    private TextField end_day;
    @FXML
    private TextField txt_startCut;

    // ─────────────────────────────────────────────
    //  FXML — جداول الإضافة والخصم (بديل الحقول المكررة)
    // ─────────────────────────────────────────────
    @FXML
    private TextField txt_endCut;
    @FXML
    private TableView<String[]> table_upgrade;
    @FXML
    private TableView<String[]> table_encourge;
    @FXML
    private TableView<String[]> table_promotion;
    @FXML
    private TableView<DateValueRow> table_mogardAdd;
    @FXML
    private TableView<DateValueRow> table_mogardRival;
    @FXML
    private TableView<DateValueRow> table_bounsAdd;
    @FXML
    private TableView<DateValueRow> table_bounsRival;
    // أزرار إضافة/حذف الصفوف
    @FXML
    private Button btn_mogardAddRow;
    @FXML
    private Button btn_mogardDelRow;
    @FXML
    private Button btn_mogardRivalAddRow;
    @FXML
    private Button btn_mogardRivalDelRow;

    // ─────────────────────────────────────────────
    //  FXML — نتائج البحث
    // ─────────────────────────────────────────────
    @FXML
    private Button btn_bounsAddRow;

    // ─────────────────────────────────────────────
    //  Data
    // ─────────────────────────────────────────────
    @FXML
    private Button btn_bounsDelRow;
    @FXML
    private Button btn_bounsRivalAddRow;
    @FXML
    private Button btn_bounsRivalDelRow;
    @FXML
    private TableView<?> table_result;

    // ─────────────────────────────────────────────
    //  Initialize
    // ─────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupEditableTable(table_mogardAdd, mogardAddData);
        setupEditableTable(table_mogardRival, mogardRivalData);
        setupEditableTable(table_bounsAdd, bounsAddData);
        setupEditableTable(table_bounsRival, bounsRivalData);

        setupRowButtons(btn_mogardAddRow, btn_mogardDelRow, table_mogardAdd, mogardAddData);
        setupRowButtons(btn_mogardRivalAddRow, btn_mogardRivalDelRow, table_mogardRival, mogardRivalData);
        setupRowButtons(btn_bounsAddRow, btn_bounsDelRow, table_bounsAdd, bounsAddData);
        setupRowButtons(btn_bounsRivalAddRow, btn_bounsRivalDelRow, table_bounsRival, bounsRivalData);

        btn_search.setOnAction(e -> doSearch());
        btn_calculate.setOnAction(e -> doCalculate());
        btn_save.setOnAction(e -> doSave());
        btn_pdf.setOnAction(e -> doPdf());
        btn_clear.setOnAction(e -> doClear());
    }

    // ─────────────────────────────────────────────
    //  Table Setup
    // ─────────────────────────────────────────────

    /**
     * يُهيِّئ TableView قابل للتعديل بعمودَي (تاريخ + قيمة).
     *
     * <p>الخلايا قابلة للتعديل المباشر بالنقر المزدوج —
     * المستخدم يكتب مباشرةً في الجدول بدون حوار منفصل.
     */
    @SuppressWarnings("unchecked")
    private void setupEditableTable(TableView<DateValueRow> table,
                                    ObservableList<DateValueRow> data) {
        table.setEditable(true);
        table.setItems(data);

        // عمود التاريخ
        TableColumn<DateValueRow, String> dateCol =
                (TableColumn<DateValueRow, String>) table.getColumns().get(0);
        dateCol.setCellValueFactory(cell -> cell.getValue().dateProperty());
        dateCol.setCellFactory(TextFieldTableCell.forTableColumn());
        dateCol.setOnEditCommit(e -> e.getRowValue().setDate(e.getNewValue()));

        // عمود القيمة
        TableColumn<DateValueRow, String> valueCol =
                (TableColumn<DateValueRow, String>) table.getColumns().get(1);
        valueCol.setCellValueFactory(cell -> cell.getValue().valueProperty());
        valueCol.setCellFactory(TextFieldTableCell.forTableColumn());
        valueCol.setOnEditCommit(e -> e.getRowValue().setValue(e.getNewValue()));

        // تلوين الصفوف تناوباً لراحة القراءة
        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(DateValueRow item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else {
                    setStyle(getIndex() % 2 == 0
                            ? "-fx-background-color: #F9FAFB;"
                            : "-fx-background-color: white;");
                }
            }
        });
    }

    /**
     * يربط زرَّي الإضافة والحذف بالجدول المقابل.
     *
     * <p>الإضافة: تضيف صفاً فارغاً ثم تنقل التركيز إليه.
     * الحذف: يحذف الصف المحدد — لو مفيش تحديد لا يفعل شيئاً.
     */
    private void setupRowButtons(Button addBtn, Button delBtn,
                                 TableView<DateValueRow> table,
                                 ObservableList<DateValueRow> data) {
        addBtn.setOnAction(e -> {
            DateValueRow newRow = new DateValueRow("", "");
            data.add(newRow);
            table.getSelectionModel().select(newRow);
            table.scrollTo(newRow);
            // تفعيل تعديل عمود التاريخ مباشرةً
            table.edit(data.size() - 1, table.getColumns().get(0));
        });

        delBtn.setOnAction(e -> {
            DateValueRow selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) data.remove(selected);
        });
    }

    // ─────────────────────────────────────────────
    //  Actions
    // ─────────────────────────────────────────────

    private void doSearch() {
        // TODO: استدعاء الـ Service بـ txt_nationalId أو txt_empCode
    }

    private void doCalculate() {
        ScaleInput input = buildInput();
        // TODO: تمرير الـ input للـ ScaleEngine
    }

    private void doSave() {
        ScaleInput input = buildInput();
        // TODO: حفظ النتائج
    }

    private void doPdf() {
        // TODO: توليد PDF
    }

    private void doClear() {
        // مسح حقول البحث والمؤهلات
        List.of(txt_nationalId, txt_empCode, txt_empName, txt_Management,
                        txt_startDate, txt_backStart, txt_debloma, txt_magester,
                        txt_doctoraa, txt_ectra, txt_regrade3, txt_regrade4,
                        txt_regrade5, txt_backRegrade, txt_group, txt_law,
                        txt_code, txt_startDegree, yearUp, yearNoUp, gpUp,
                        gpNoUp, yearsBack, date_kader, end_day,
                        txt_startCut, txt_endCut)
                .forEach(f -> f.clear());

        // مسح الجداول
        mogardAddData.clear();
        mogardRivalData.clear();
        bounsAddData.clear();
        bounsRivalData.clear();
    }

    // ─────────────────────────────────────────────
    //  Data Extraction
    // ─────────────────────────────────────────────

    /**
     * يجمع كل بيانات النموذج في كائن {@link ScaleInput} واحد.
     *
     * <p>الجداول تُحوَّل لـ {@code List<DateValue>} —
     * بديل واضح لقراءة {@code mogardAddDate1.getText()} حتى 6.
     */
    private ScaleInput buildInput() {
        return new ScaleInput(
                txt_nationalId.getText(),
                txt_empCode.getText(),
                txt_startDate.getText(),
                txt_backStart.getText(),
                txt_debloma.getText(),
                txt_magester.getText(),
                txt_doctoraa.getText(),
                txt_ectra.getText(),
                txt_regrade3.getText(),
                txt_regrade4.getText(),
                txt_regrade5.getText(),
                txt_backRegrade.getText(),
                txt_law.getText(),
                txt_code.getText(),
                txt_startDegree.getText(),
                txt_startCut.getText(),
                txt_endCut.getText(),
                toDateValueList(mogardAddData),
                toDateValueList(mogardRivalData),
                toDateValueList(bounsAddData),
                toDateValueList(bounsRivalData)
        );
    }

    /**
     * يحول {@link ObservableList} من {@link DateValueRow} إلى List عادية
     * مع تصفية الصفوف الفارغة.
     */
    private List<DateValue> toDateValueList(ObservableList<DateValueRow> rows) {
        return rows.stream()
                .filter(r -> !r.getDate().isBlank() || !r.getValue().isBlank())
                .map(r -> new DateValue(r.getDate(), r.getValue()))
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    //  Inner Classes
    // ─────────────────────────────────────────────

    /**
     * نموذج صف الجدول — تاريخ + قيمة.
     * يستخدم {@link SimpleStringProperty} عشان الـ TableView يتحدث تلقائياً.
     */
    public static class DateValueRow {
        private final SimpleStringProperty date;
        private final SimpleStringProperty value;

        public DateValueRow(String date, String value) {
            this.date = new SimpleStringProperty(date);
            this.value = new SimpleStringProperty(value);
        }

        public SimpleStringProperty dateProperty() {
            return date;
        }

        public SimpleStringProperty valueProperty() {
            return value;
        }

        public String getDate() {
            return date.get();
        }

        public void setDate(String v) {
            date.set(v);
        }

        public String getValue() {
            return value.get();
        }

        public void setValue(String v) {
            value.set(v);
        }
    }

    /**
     * DTO نظيف لنقل البيانات للـ Service
     */
    public record DateValue(String date, String value) {
    }

    /**
     * كائن يجمع كل مدخلات الشاشة
     */
    public record ScaleInput(
            String nationalId,
            String empCode,
            String startDate,
            String backStart,
            String debloma,
            String magester,
            String doctoraa,
            String ectra,
            String regrade3,
            String regrade4,
            String regrade5,
            String backRegrade,
            String law,
            String code,
            String startDegree,
            String startCut,
            String endCut,
            List<DateValue> mogardAdd,
            List<DateValue> mogardRival,
            List<DateValue> bounsAdd,
            List<DateValue> bounsRival
    ) {
    }
}