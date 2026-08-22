package com.safwat.hr.shared.ui;

import io.github.palexdev.materialfx.controls.MFXButton;
import javafx.beans.property.SimpleBooleanProperty;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * =====================================================
 * MultiSelectSearchDialog — بحث متعدد الاختيار (Checkboxes)
 * =====================================================
 * <p>
 * زي SearchDialog بالظبط بس مع:
 * - عمود checkbox في الأول
 * - زرار "اختيار الكل" / "إلغاء الكل"
 * - يرجع List<T> (مش Optional واحد)
 */
public class MultiSelectSearchDialog<T> {

    private final List<Column<T>> columns = new ArrayList<>();
    private String title = "اختيار متعدد";
    private List<T> data = List.of();
    private String searchPlaceholder = "ابحث هنا...";
    private Stage owner = null;
    private double width = 750;
    private double height = 520;

    private MultiSelectSearchDialog() {
    }

    public static <T> MultiSelectSearchDialog<T> builder(Class<T> type) {
        return new MultiSelectSearchDialog<>();
    }

    public static MultiSelectSearchDialog<String> forStrings() {
        MultiSelectSearchDialog<String> d = new MultiSelectSearchDialog<>();
        d.column("القيمة", s -> s == null ? "" : s);
        return d;
    }

    public MultiSelectSearchDialog<T> title(String v) {
        this.title = v;
        return this;
    }

    public MultiSelectSearchDialog<T> data(List<T> v) {
        this.data = v;
        return this;
    }

    public MultiSelectSearchDialog<T> searchPlaceholder(String v) {
        this.searchPlaceholder = v;
        return this;
    }

    public MultiSelectSearchDialog<T> owner(Stage v) {
        this.owner = v;
        return this;
    }

    public MultiSelectSearchDialog<T> size(double w, double h) {
        this.width = w;
        this.height = h;
        return this;
    }

    public MultiSelectSearchDialog<T> column(String header, ColumnValueExtractor<T> extractor) {
        columns.add(new Column<>(header, extractor));
        return this;
    }

    /**
     * بيعرض الـ Dialog ويرجع اللي تم اختيارهم (List)
     * لو المستخدم قفل من غير اختيار بيرجع List فاضية
     */
    public List<T> showAndWait() {
        MultiSelectDialogController<T> controller =
                new MultiSelectDialogController<>(title, columns, data, searchPlaceholder, width, height);
        return controller.showAndWait(owner);
    }

    // ---------- Functional Interface ----------
    @FunctionalInterface
    public interface ColumnValueExtractor<T> {
        String extract(T item);
    }

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
//  Controller داخلي
// =====================================================
class MultiSelectDialogController<T> {

    private final String title;
    private final List<MultiSelectSearchDialog.Column<T>> columns;
    private final List<T> data;
    private final String placeholder;
    private final double width, height;

    private final Set<T> selectedItems = new LinkedHashSet<>();
    private TableView<T> table;
    private TextField searchField;
    // ===================== التذييل =====================
    private Label selectedCountLbl;

    MultiSelectDialogController(String title,
                                List<MultiSelectSearchDialog.Column<T>> columns,
                                List<T> data, String placeholder,
                                double width, double height) {
        this.title = title;
        this.columns = columns;
        this.data = data;
        this.placeholder = placeholder;
        this.width = width;
        this.height = height;
    }

    List<T> showAndWait(Stage owner) {
        Stage stage = buildStage(owner);
        stage.showAndWait();
        return new ArrayList<>(selectedItems);
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
                buildToolbar(stage),
                buildSearchBar(),
                buildTable(stage),
                buildFooter(stage)
        );
        root.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        Scene scene = new Scene(root, width, height);
        scene.setFill(Color.TRANSPARENT);

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

        Label icon = new Label("☑️");
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

    // ===================== Toolbar (اختيار الكل / إلغاء الكل) =====================
    private HBox buildToolbar(Stage stage) {
        HBox toolbar = new HBox(10);
        toolbar.setPadding(new Insets(8, 16, 6, 16));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setStyle(
                "-fx-border-color: transparent transparent #EBEBEB transparent;" +
                        "-fx-border-width: 0 0 0.5 0;"
        );

        MFXButton selectAllBtn = new MFXButton("☑️ اختيار الكل");
        selectAllBtn.setStyle(
                "-fx-background-color:#E6F1FB;-fx-text-fill:#185FA5;" +
                        "-fx-font-size:12px;-fx-background-radius:6px;" +
                        "-fx-padding:6 14 6 14;-fx-cursor:hand;"
        );
        selectAllBtn.setOnAction(e -> {
            selectedItems.addAll(data);
            table.refresh();
            updateSelectedCount();
        });

        MFXButton deselectAllBtn = new MFXButton("⬜ إلغاء الكل");
        deselectAllBtn.setStyle(
                "-fx-background-color:#F0F0F0;-fx-text-fill:#666666;" +
                        "-fx-font-size:12px;-fx-background-radius:6px;" +
                        "-fx-padding:6 14 6 14;-fx-cursor:hand;"
        );
        deselectAllBtn.setOnAction(e -> {
            selectedItems.clear();
            table.refresh();
            updateSelectedCount();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        toolbar.getChildren().addAll(selectAllBtn, deselectAllBtn, spacer);
        return toolbar;
    }

    // ===================== Search Bar =====================
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

    // ===================== الجدول مع Checkboxes =====================
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

        // ✅ عمود Checkbox
        TableColumn<T, Boolean> checkCol = new TableColumn<>("");
        checkCol.setPrefWidth(40);
        checkCol.setMaxWidth(40);
        checkCol.setMinWidth(40);
        checkCol.setCellValueFactory(param -> {
            T item = param.getValue();
            return new SimpleBooleanProperty(selectedItems.contains(item));
        });
        checkCol.setCellFactory(tc -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();

            {
                checkBox.setOnAction(e -> {
                    T item = getTableRow().getItem();
                    if (item == null) return;
                    if (checkBox.isSelected()) {
                        selectedItems.add(item);
                    } else {
                        selectedItems.remove(item);
                    }
                    updateSelectedCount();
                });
            }

            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    T rowItem = getTableRow().getItem();
                    checkBox.setSelected(selectedItems.contains(rowItem));
                    setGraphic(checkBox);
                }
            }
        });
        table.getColumns().add(checkCol);

        // بناء الأعمدة ديناميكياً
        for (int i = 0; i < columns.size(); i++) {
            MultiSelectSearchDialog.Column<T> colDef = columns.get(i);
            final int colIndex = i;
            TableColumn<T, String> col = new TableColumn<>(colDef.header);
            col.setCellValueFactory(param -> {
                T row = param.getValue();
                String val = row == null ? "" : colDef.extractor.extract(row);
                return new SimpleStringProperty(val == null ? "" : val);
            });

            if (i == 0) col.setPrefWidth(180);
            if (i == 1) col.setPrefWidth(120);

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
                            setStyle("-fx-font-weight:600;-fx-text-fill:#1A1A1A;");
                        } else {
                            setStyle("-fx-font-size:12px;-fx-text-fill:#666666;");
                        }
                    }
                }
            });

            table.getColumns().add(col);
        }

        // البيانات مع الفلترة
        ObservableList<T> observableData = FXCollections.observableArrayList(data);
        FilteredList<T> filteredData = new FilteredList<>(observableData, p -> true);

        // ربط مربع البحث بالفلتر — يبحث في كل الأعمدة
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String query = newVal == null ? "" : newVal.trim().toLowerCase();
            filteredData.setPredicate(row -> {
                if (query.isEmpty()) return true;
                for (MultiSelectSearchDialog.Column<T> colDef : columns) {
                    String cell = colDef.extractor.extract(row);
                    if (cell != null && cell.toLowerCase().contains(query))
                        return true;
                }
                return false;
            });
        });

        table.setItems(filteredData);

        // دبل كليك يعمل toggle
        table.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                T selected = table.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    if (selectedItems.contains(selected)) {
                        selectedItems.remove(selected);
                    } else {
                        selectedItems.add(selected);
                    }
                    table.refresh();
                    updateSelectedCount();
                }
            }
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

        selectedCountLbl = new Label("تم اختيار 0");
        selectedCountLbl.setStyle(
                "-fx-font-size:12px;-fx-text-fill:#0F6E56;-fx-font-weight:600;" +
                        "-fx-background-color:#E6F5F1;-fx-background-radius:10px;" +
                        "-fx-padding:3 10 3 10;"
        );

        Label hint = new Label("💡  دبل كليك للاختيار  ·  ESC للإغلاق");
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
            selectedItems.clear();
            stage.close();
        });

        MFXButton selectBtn = new MFXButton("تأكيد الاختيار ✓");
        selectBtn.setStyle(
                "-fx-background-color:#185FA5;-fx-text-fill:white;" +
                        "-fx-font-size:13px;-fx-font-weight:600;" +
                        "-fx-background-radius:8px;-fx-padding:8 20 8 20;-fx-cursor:hand;"
        );
        selectBtn.setOnAction(e -> stage.close());

        footer.getChildren().addAll(selectedCountLbl, hint, spacer, cancelBtn, selectBtn);
        return footer;
    }

    private void updateSelectedCount() {
        selectedCountLbl.setText("تم اختيار " + selectedItems.size());
    }

    // ===================== مساعدات =====================
    private void centerOnOwner(Stage stage, Stage owner) {
        stage.setX(owner.getX() + (owner.getWidth() - width) / 2);
        stage.setY(owner.getY() + (owner.getHeight() - height) / 2);
    }
}