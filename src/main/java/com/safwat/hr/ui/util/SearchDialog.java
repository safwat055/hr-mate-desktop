package com.safwat.hr.ui.util;

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

import java.util.List;
import java.util.Optional;

/**
 * =====================================================
 * SearchDialog — واجهة بحث عامة قابلة لإعادة الاستخدام
 * =====================================================
 * <p>
 * طريقة الاستخدام:
 * <p>
 * Optional<Object[]> result = SearchDialog.builder()
 * .title("بحث عن موظف")
 * .headers(new String[]{"الكود", "الاسم", "القسم", "الوظيفة"})
 * .data(employeeList)
 * .searchPlaceholder("اكتب اسم أو كود الموظف...")
 * .owner(primaryStage)
 * .show();
 * <p>
 * result.ifPresent(row -> {
 * String id   = row[0].toString();
 * String name = row[1].toString();
 * });
 */
public class SearchDialog {

    // ===================== Builder =====================
    private String title = "بحث";
    private String[] headers = {};
    private List<Object[]> data = List.of();
    private String searchPlaceholder = "ابحث هنا...";
    private Stage owner = null;
    private double width = 750;
    private double height = 520;

    private SearchDialog() {
    }

    public static SearchDialog builder() {
        return new SearchDialog();
    }

    public SearchDialog title(String v) {
        this.title = v;
        return this;
    }

    public SearchDialog headers(String[] v) {
        this.headers = v;
        return this;
    }

    public SearchDialog data(List<Object[]> v) {
        this.data = v;
        return this;
    }

    public SearchDialog searchPlaceholder(String v) {
        this.searchPlaceholder = v;
        return this;
    }

    public SearchDialog owner(Stage v) {
        this.owner = v;
        return this;
    }

    public SearchDialog size(double w, double h) {
        this.width = w;
        this.height = h;
        return this;
    }

    // ===================== العرض =====================
    public Optional<Object[]> show() {
        SearchDialogController controller =
                new SearchDialogController(title, headers, data, searchPlaceholder, width, height);
        return controller.showAndWait(owner);
    }
}


// =====================================================
//  Controller داخلي — منفصل عن الـ Builder
// =====================================================
class SearchDialogController {

    private final String title;
    private final String[] headers;
    private final List<Object[]> data;
    private final String placeholder;
    private final double width, height;

    // الصف المحدد
    private Object[] selectedRow = null;

    // الجدول
    private TableView<Object[]> table;

    // مربع البحث
    private TextField searchField;

    SearchDialogController(String title, String[] headers,
                           List<Object[]> data, String placeholder,
                           double width, double height) {
        this.title = title;
        this.headers = headers;
        this.data = data;
        this.placeholder = placeholder;
        this.width = width;
        this.height = height;
    }

    Optional<Object[]> showAndWait(Stage owner) {
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

        // أيقونة البحث
        Label icon = new Label("🔍");
        icon.setStyle("-fx-font-size:16px;");

        Label titleLbl = new Label(title);
        titleLbl.setStyle(
                "-fx-font-size:15px;-fx-font-weight:700;" +
                        "-fx-text-fill:#1A1A1A;-fx-padding:0 0 0 8;"
        );

        // عداد النتائج
        Label countLbl = new Label(data.size() + " نتيجة");
        countLbl.setStyle(
                "-fx-font-size:12px;-fx-text-fill:#888888;" +
                        "-fx-background-color:#F0F0F0;-fx-background-radius:10px;" +
                        "-fx-padding:2 8 2 8;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // زر الإغلاق
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

        // سحب النافذة
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
        // عرض مرن يملأ المساحة المتبقية
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchField.setMaxWidth(Double.MAX_VALUE);

        Button clearBtn = new Button("مسح");
        clearBtn.setStyle(
                "-fx-background-color:#F0F0F0;-fx-text-fill:#666666;" +
                        "-fx-font-size:12px;-fx-background-radius:6px;-fx-cursor:hand;"
        );
        // عرض ثابت للزر
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
    private TableView<Object[]> buildTable(Stage stage) {
        table = new TableView<>();
        table.setStyle("-fx-background-color: transparent;-fx-font-size:13px;");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(buildEmptyState());
        VBox.setVgrow(table, Priority.ALWAYS);

        // بناء الأعمدة ديناميكياً
        for (int i = 0; i < headers.length; i++) {
            final int colIndex = i;
            TableColumn<Object[], String> col = new TableColumn<>(headers[i]);
            col.setCellValueFactory(param -> {
                Object[] row = param.getValue();
                if (row == null || colIndex >= row.length) return new SimpleStringProperty("");
                Object val = row[colIndex];
                return new SimpleStringProperty(val == null ? "" : val.toString());
            });

            // العمود الأول (ID) أضيق
            if (i == 0) col.setPrefWidth(80);

            // تنسيق الخلية
            col.setCellFactory(tc -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);
                        // العمود الأول بولد
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
        ObservableList<Object[]> observableData =
                FXCollections.observableArrayList(data);
        FilteredList<Object[]> filteredData =
                new FilteredList<>(observableData, p -> true);

        // ربط مربع البحث بالفلتر
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String query = newVal == null ? "" : newVal.trim().toLowerCase();
            filteredData.setPredicate(row -> {
                if (query.isEmpty()) return true;
                for (Object cell : row) {
                    if (cell != null && cell.toString().toLowerCase().contains(query))
                        return true;
                }
                return false;
            });
            updateResultCount(filteredData.size());
        });

        table.setItems(filteredData);

        // تمييز الصف عند التحديد
        table.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, row) -> selectedRow = row
        );

        // دبل كليك يختار ويغلق
        table.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && selectedRow != null) {
                stage.close();
            }
        });

        // Enter يغلق لو في صف محدد
        table.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER && selectedRow != null) {
                stage.close();
            }
        });

        // تلوين الصفوف بالتناوب
        table.setRowFactory(tv -> {
            TableRow<Object[]> row = new TableRow<>();
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

            // hover
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

        // فوكس على البحث عند الفتح
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
