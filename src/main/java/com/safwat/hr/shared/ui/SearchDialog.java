package com.safwat.hr.shared.ui;

import io.github.palexdev.materialfx.controls.MFXButton;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * =====================================================
 * SearchDialog — واجهة بحث عامة قابلة لإعادة الاستخدام
 * =====================================================
 * <p>
 * أصبح الكلاس الآن generic (SearchDialog&lt;T&gt;) بحيث يقبل أي نوع بيانات:
 * <p>
 * 1) الطريقة القديمة بدون أي تعديل (صفوف Object[]):
 * <pre>
 * Optional<Object[]> result = SearchDialog.builder()
 *      .title("بحث عن موظف")
 *      .headers(new String[]{"الكود", "الاسم", "القسم", "الوظيفة"})
 *      .data(employeeList) // List<Object[]>
 *      .searchPlaceholder("اكتب اسم أو كود الموظف...")
 *      .owner(primaryStage)
 *      .show();
 * </pre>
 * <p>
 * 2) قائمة نصوص بسيطة (List&lt;String&gt;):
 * <pre>
 * Optional<String> result = SearchDialog.forStrings()
 *      .title("اختر قسم")
 *      .data(departmentNames) // List<String>
 *      .owner(primaryStage)
 *      .show();
 * </pre>
 * <p>
 * 3) قائمة كائنات من أي كلاس (Generic):
 * <pre>
 * Optional<Employee> result = SearchDialog.builder(Employee.class)
 *      .title("بحث عن موظف")
 *      .column("الكود", Employee::getCode)
 *      .column("الاسم", Employee::getName)
 *      .column("القسم", Employee::getDepartment)
 *      .data(employeeObjectsList) // List<Employee>
 *      .owner(primaryStage)
 *      .show();
 * </pre>
 */
public class SearchDialog<T> {

    private final List<Column<T>> columns = new ArrayList<>();
    // ===================== Builder =====================
    private String title = "بحث";
    private List<T> data = List.of();
    private String searchPlaceholder = "ابحث هنا...";
    private Stage owner = null;
    private double width = 750;
    private double height = 520;

    private SearchDialog() {
    }

    /**
     * الاستخدام القديم — متوافق تمامًا مع الكود الحالي (صفوف Object[])
     */
    public static SearchDialog<Object[]> builder() {
        return new SearchDialog<>();
    }

    /**
     * استخدام عام جديد لأي كلاس: SearchDialog.builder(Employee.class)
     */
    public static <T> SearchDialog<T> builder(Class<T> type) {
        return new SearchDialog<>();
    }

    // ---------- نقاط الدخول ----------

    /**
     * اختصار جاهز لقوائم النصوص البسيطة List&lt;String&gt;
     */
    public static SearchDialog<String> forStrings() {
        SearchDialog<String> d = new SearchDialog<>();
        d.column("قيم البحث", s -> s == null ? "" : s);
        return d;
    }

    // ---------- إعدادات عامة ----------
    public SearchDialog<T> title(String v) {
        this.title = v;
        return this;
    }

    public SearchDialog<T> data(List<T> v) {
        this.data = v;
        return this;
    }

    public SearchDialog<T> searchPlaceholder(String v) {
        this.searchPlaceholder = v;
        return this;
    }

    public SearchDialog<T> owner(Stage v) {
        this.owner = v;
        return this;
    }

    public SearchDialog<T> size(double w, double h) {
        this.width = w;
        this.height = h;
        return this;
    }

    /**
     * الطريقة العامة الجديدة لتعريف عمود بأي منطق استخراج
     */
    public SearchDialog<T> column(String header, ColumnValueExtractor<T> extractor) {
        columns.add(new Column<>(header, extractor));
        return this;
    }

    /**
     * توافق مع الطريقة القديمة headers(String[]) — تفترض أن الصف Object[]
     * تعمل فقط مع SearchDialog.builder() (بدون Class) حيث T = Object[]
     */
    @SuppressWarnings("unchecked")
    public SearchDialog<T> headers(String[] v) {
        columns.clear();
        for (int i = 0; i < v.length; i++) {
            final int idx = i;
            columns.add(new Column<>(v[i], (T item) -> {
                Object[] row = (Object[]) item;
                if (row == null || idx >= row.length) return "";
                Object val = row[idx];
                return val == null ? "" : val.toString();
            }));
        }
        return this;
    }

    // ===================== العرض =====================
    public Optional<T> show() {
        SearchDialogController<T> controller =
                new SearchDialogController<>(title, columns, data, searchPlaceholder, width, height);
        return controller.showAndWait(owner);
    }

    /**
     * دالة استخراج قيمة نصية من الصف لعمود معين
     */
    @FunctionalInterface
    public interface ColumnValueExtractor<T> {
        String extract(T item);
    }

    /**
     * تعريف عمود: عنوان + طريقة استخراج القيمة
     */
    public static class Column<T> {
        final String header;
        final ColumnValueExtractor<T> extractor;

        Column(String header, ColumnValueExtractor<T> extractor) {
            this.header = header;
            this.extractor = extractor;
        }
    }
}


// =====================================================
//  Controller داخلي — منفصل عن الـ Builder
// =====================================================
class SearchDialogController<T> {

    private final String title;
    private final List<SearchDialog.Column<T>> columns;
    private final List<T> data;
    private final String placeholder;
    private final double width, height;

    // الصف المحدد
    private T selectedRow = null;

    // الجدول
    private TableView<T> table;

    // مربع البحث
    private TextField searchField;

    SearchDialogController(String title, List<SearchDialog.Column<T>> columns,
                           List<T> data, String placeholder,
                           double width, double height) {
        this.title = title;
        this.columns = columns;
        this.data = data;
        this.placeholder = placeholder;
        this.width = width;
        this.height = height;
    }

    Optional<T> showAndWait(Stage owner) {
        Stage stage = buildStage(owner);
        stage.showAndWait();
        return Optional.ofNullable(selectedRow);
    }

    // ===================== بناء الـ Stage =====================
    private Stage buildStage(Stage owner) {
        Stage stage = new Stage();
        stage.initStyle(StageStyle.UNDECORATED);
        stage.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) stage.initOwner(owner);

        VBox root = new VBox(0);
        root.setStyle(
                "-fx-background-color: #FFFFFF;" +
                        "-fx-background-radius: 12px;" +
                        "-fx-border-color: #E0E0E0;" +
                        "-fx-border-width: 0.5px;" +
                        "-fx-border-radius: 12px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 24, 0, 0, 8);"
        );

        root.getChildren().addAll(
                buildTitleBar(stage),
                buildSearchBar(),
                buildTable(stage),
                buildFooter(stage)
        );
        root.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        Scene scene = new Scene(root, width, height);
        scene.setFill(Color.TRANSPARENT);

        // ESC يغلق
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) stage.close();
        });

        stage.setScene(scene);
        if (owner != null) centerOnOwner(stage, owner);
        return stage;
    }

    // ===================== شريط العنوان =====================
    private HBox buildTitleBar(Stage stage) {
        HBox bar = new HBox();
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(14, 16, 12, 16));
        bar.setStyle(
                "-fx-background-color: #F8F8F8;" +
                        "-fx-background-radius: 12px 12px 0 0;" +
                        "-fx-border-color: transparent transparent #EBEBEB transparent;" +
                        "-fx-border-width: 0 0 0.5 0;"
        );

        Label icon = new Label("🔍");
        icon.setStyle("-fx-font-size:16px;");

        Label titleLbl = new Label(title);
        titleLbl.setStyle(
                "-fx-font-size:15px;-fx-font-weight:700;" +
                        "-fx-text-fill:#1A1A1A;-fx-padding:0 0 0 8;"
        );

        Label countLbl = new Label(data.size() + " نتيجة");
        countLbl.setStyle(
                "-fx-font-size:12px;-fx-text-fill:#888888;" +
                        "-fx-background-color:#F0F0F0;-fx-background-radius:10px;" +
                        "-fx-padding:2 8 2 8;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label closeBtn = new Label("✕");
        closeBtn.setStyle(
                "-fx-font-size:16px;-fx-text-fill:#AAAAAA;-fx-cursor:hand;" +
                        "-fx-padding:4 8 4 8;-fx-background-radius:6px;"
        );
        closeBtn.setOnMouseEntered(e ->
                closeBtn.setStyle(closeBtn.getStyle() +
                        "-fx-background-color:#FFE8E8;-fx-text-fill:#CC3333;"));
        closeBtn.setOnMouseExited(e ->
                closeBtn.setStyle(
                        "-fx-font-size:16px;-fx-text-fill:#AAAAAA;-fx-cursor:hand;" +
                                "-fx-padding:4 8 4 8;-fx-background-radius:6px;"));
        closeBtn.setOnMouseClicked(e -> stage.close());

        final double[] dragDelta = new double[2];
        bar.setOnMousePressed(e -> {
            dragDelta[0] = stage.getX() - e.getScreenX();
            dragDelta[1] = stage.getY() - e.getScreenY();
        });
        bar.setOnMouseDragged(e -> {
            stage.setX(e.getScreenX() + dragDelta[0]);
            stage.setY(e.getScreenY() + dragDelta[1]);
        });

        bar.getChildren().addAll(icon, titleLbl, countLbl, spacer, closeBtn);
        return bar;
    }

    private HBox buildSearchBar() {
        HBox bar = new HBox(8);
        bar.setPadding(new Insets(12, 16, 10, 16));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        bar.setStyle(
                "-fx-border-color: transparent transparent #EBEBEB transparent;" +
                        "-fx-border-width: 0 0 0.5 0;"
        );

        searchField = new TextField();
        searchField.setPromptText(placeholder);
        searchField.setStyle("-fx-font-size:13px;");
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchField.setMaxWidth(Double.MAX_VALUE);

        Button clearBtn = new Button("مسح");
        clearBtn.setStyle(
                "-fx-background-color:#F0F0F0;-fx-text-fill:#666666;" +
                        "-fx-font-size:12px;-fx-background-radius:6px;-fx-cursor:hand;"
        );
        clearBtn.setPrefWidth(60);
        clearBtn.setMinWidth(60);
        clearBtn.setMaxWidth(60);

        clearBtn.managedProperty().bind(clearBtn.visibleProperty());
        clearBtn.visibleProperty().bind(searchField.textProperty().isNotEmpty());
        clearBtn.setOnAction(e -> searchField.clear());

        bar.getChildren().addAll(searchField, clearBtn);
        return bar;
    }

    // ===================== الجدول =====================
    private TableView<T> buildTable(Stage stage) {
        table = new TableView<>();
        table.setStyle("""
                    -fx-background-color: transparent;
                    -fx-font-family: "DejaVu Sans";
                    -fx-font-size: 13px;
                """);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(buildEmptyState());
        VBox.setVgrow(table, Priority.ALWAYS);

        // بناء الأعمدة ديناميكياً من تعريفات columns
        for (int i = 0; i < columns.size(); i++) {
            SearchDialog.Column<T> colDef = columns.get(i);
            final int colIndex = i;
            TableColumn<T, String> col = new TableColumn<>(colDef.header);
            col.setCellValueFactory(param -> {
                T row = param.getValue();
                String val = row == null ? "" : colDef.extractor.extract(row);
                return new SimpleStringProperty(val == null ? "" : val);
            });

            if (i == 0) col.setPrefWidth(80);

            col.setCellFactory(tc -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);
                        if (colIndex == 0) {
                            setStyle("-fx-font-weight:600;-fx-text-fill:#185FA5;");
                        } else {
                            setStyle("-fx-text-fill:#333333;");
                        }
                    }
                }
            });

            table.getColumns().add(col);
        }

        // البيانات مع الفلترة
        ObservableList<T> observableData =
                FXCollections.observableArrayList(data);
        FilteredList<T> filteredData =
                new FilteredList<>(observableData, p -> true);

        // ربط مربع البحث بالفلتر — يبحث في كل الأعمدة المعرّفة عبر extractor
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String query = newVal == null ? "" : newVal.trim().toLowerCase();
            filteredData.setPredicate(row -> {
                if (query.isEmpty()) return true;
                for (SearchDialog.Column<T> colDef : columns) {
                    String cell = colDef.extractor.extract(row);
                    if (cell != null && cell.toLowerCase().contains(query))
                        return true;
                }
                return false;
            });
            updateResultCount(filteredData.size());
        });

        table.setItems(filteredData);

        table.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, row) -> selectedRow = row
        );

        table.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && selectedRow != null) {
                stage.close();
            }
        });

        table.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER && selectedRow != null) {
                stage.close();
            }
        });

        table.setRowFactory(tv -> {
            TableRow<T> row = new TableRow<>();
            row.itemProperty().addListener((obs, old, item) -> {
                if (item == null) {
                    row.setStyle("");
                } else {
                    row.setStyle(row.getIndex() % 2 == 0
                            ? "-fx-background-color:#FFFFFF;"
                            : "-fx-background-color:#F9F9FB;"
                    );
                }
            });

            row.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                if (isSelected) {
                    row.setStyle(
                            "-fx-background-color:#E6F1FB;" +
                                    "-fx-border-color:#185FA5;" +
                                    "-fx-border-width:0 0 0 3;"
                    );
                } else if (row.getItem() != null) {
                    row.setStyle(row.getIndex() % 2 == 0
                            ? "-fx-background-color:#FFFFFF;"
                            : "-fx-background-color:#F9F9FB;"
                    );
                }
            });

            row.setOnMouseEntered(e -> {
                if (!row.isSelected() && row.getItem() != null)
                    row.setStyle("-fx-background-color:#EEF4FF;");
            });
            row.setOnMouseExited(e -> {
                if (!row.isSelected() && row.getItem() != null) {
                    row.setStyle(row.getIndex() % 2 == 0
                            ? "-fx-background-color:#FFFFFF;"
                            : "-fx-background-color:#F9F9FB;"
                    );
                }
            });

            return row;
        });

        searchField.sceneProperty().addListener((obs, old, scene) -> {
            if (scene != null) searchField.requestFocus();
        });

        return table;
    }

    // ===================== حالة البيانات الفارغة =====================
    private VBox buildEmptyState() {
        Label icon = new Label("🔍");
        icon.setStyle("-fx-font-size:36px;-fx-opacity:0.3;");
        Label msg = new Label("لا توجد نتائج مطابقة");
        msg.setStyle("-fx-font-size:14px;-fx-text-fill:#AAAAAA;");
        Label sub = new Label("جرب كلمات بحث مختلفة");
        sub.setStyle("-fx-font-size:12px;-fx-text-fill:#CCCCCC;");
        VBox box = new VBox(6, icon, msg, sub);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    // ===================== التذييل =====================
    private HBox buildFooter(Stage stage) {
        HBox footer = new HBox(10);
        footer.setPadding(new Insets(10, 16, 12, 16));
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setStyle(
                "-fx-background-color:#F8F8F8;" +
                        "-fx-background-radius:0 0 12px 12px;" +
                        "-fx-border-color:#EBEBEB transparent transparent transparent;" +
                        "-fx-border-width:0.5 0 0 0;"
        );

        Label hint = new Label("💡  دبل كليك أو Enter للاختيار  ·  ESC للإغلاق");
        hint.setStyle("-fx-font-size:11px;-fx-text-fill:#AAAAAA;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        MFXButton cancelBtn = new MFXButton("إلغاء");
        cancelBtn.setStyle(
                "-fx-background-color:#F0F0F0;-fx-text-fill:#666666;" +
                        "-fx-font-size:13px;-fx-background-radius:8px;" +
                        "-fx-padding:8 20 8 20;-fx-cursor:hand;"
        );
        cancelBtn.setOnAction(e -> {
            selectedRow = null;
            stage.close();
        });

        MFXButton selectBtn = new MFXButton("اختيار ✓");
        selectBtn.setStyle(
                "-fx-background-color:#185FA5;-fx-text-fill:white;" +
                        "-fx-font-size:13px;-fx-font-weight:600;" +
                        "-fx-background-radius:8px;-fx-padding:8 20 8 20;-fx-cursor:hand;"
        );
        selectBtn.disableProperty().bind(
                table.getSelectionModel().selectedItemProperty().isNull()
        );
        selectBtn.setOnAction(e -> {
            if (selectedRow != null) stage.close();
        });

        footer.getChildren().addAll(hint, spacer, cancelBtn, selectBtn);
        return footer;
    }

    // ===================== مساعدات =====================
    private void updateResultCount(int count) {
        // تحديث عداد النتائج في شريط العنوان
        // (يمكن ربطه بـ Label إذا احتجت)
    }

    private void centerOnOwner(Stage stage, Stage owner) {
        stage.setX(owner.getX() + (owner.getWidth() - width) / 2);
        stage.setY(owner.getY() + (owner.getHeight() - height) / 2);
    }
}