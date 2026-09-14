package com.safwat.hr.controller.appearance;

import com.safwat.hr.shared.ViewRegistry;
import com.safwat.hr.ui.theme.SettingsThemeLoader;
import com.safwat.hr.ui.util.ViewManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class AppearanceSettingsController implements Initializable {

    private static final String CSS_PATH =
            "/com/safwat/hr/css/settings.css";

    private enum Category {
        FONTS("🔤  خطوط", true),
        COLORS("🎨  ألوان", true),
        ZOOM("🔍  زوم", true);

        final String label;
        final boolean available;

        Category(String label, boolean available) {
            this.label = label;
            this.available = available;
        }
    }

    @FXML
    private ListView<Category> categoryList;
    @FXML
    private StackPane contentArea;
    @FXML
    private Label subtitleLabel;
    @FXML
    private ComboBox<String> viewCombo;

    private String viewId;
    private Category currentCategory = Category.FONTS;
    private boolean syncingCombo = false;

    public void setViewId(String viewId) {
        this.viewId = viewId;
        subtitleLabel.setText(viewId != null
                ? "تخصيص واجهة: " + viewId
                : "اختر واجهة من القائمة على اليمين");

        if (viewId != null && !viewCombo.getItems().contains(viewId)) {
            viewCombo.getItems().add(viewId);
        }
        syncingCombo = true;
        viewCombo.setValue(viewId);
        syncingCombo = false;

        showCategory(currentCategory);
        categoryList.getSelectionModel().select(currentCategory);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // ✅ ضمان إن combo-light موجود على viewCombo
        if (!viewCombo.getStyleClass().contains("combo-light")) {
            viewCombo.getStyleClass().add("combo-light");
        }
        viewCombo.setStyle(""); // نظّف أي style inline قديم

        categoryList.setItems(FXCollections.observableArrayList(Category.values()));
        categoryList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Category item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item.label + (item.available ? "" : "   (قريبًا)"));
                setDisable(!item.available);
                setStyle("-fx-font-size:12.5px; -fx-padding:9 14 9 14;"
                        + "-fx-text-fill:" + (item.available ? "#e8eaf6" : "#5a5f80") + ";"
                        + "-fx-background-color:transparent;");
            }
        });

        categoryList.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null && n.available) {
                currentCategory = n;
                showCategory(n);
            }
        });

        List<String> knownViews = ViewRegistry.getAll();
        viewCombo.setItems(FXCollections.observableArrayList(knownViews));
        viewCombo.valueProperty().addListener((obs, o, n) -> {
            if (syncingCombo || n == null || n.equals(viewId)) return;
            setViewId(n);
        });

        SettingsThemeLoader.apply(subtitleLabel);
    }

    private void showCategory(Category category) {
        if (viewId == null) {
            contentArea.getChildren().clear();
            Label placeholder = new Label("👈 اختر واجهة");
            placeholder.setStyle("-fx-text-fill:#8b90b8; -fx-font-size:13px;");
            StackPane.setAlignment(placeholder, Pos.CENTER);
            contentArea.getChildren().add(placeholder);
            return;
        }
        contentArea.getChildren().clear();
        switch (category) {
            case FONTS -> contentArea.getChildren().add(new FontSettingsManager(viewId).buildPanel());
            case COLORS -> contentArea.getChildren().add(ColorSettingsManager.buildPanel());
            case ZOOM -> contentArea.getChildren().add(ZoomManager.buildPanel(viewId));
        }
    }

    // ══════════════════════════════════════════════════════════
    //  فتح الشاشة
    // ══════════════════════════════════════════════════════════

    /**
     * ✅ show() بدل showAndWait() — يتجنب GTK nested event loop crash
     * ✅ يحمّل settings.css على الـ Scene عشان combo-light/color-picker-light يشتغلوا
     */
    public static void open(String viewId) {
        final String fxmlPath = "/com/safwat/hr/controller/appearance_settings.fxml";
        final String title = "تخصيص الواجهة";

        ViewManager.openIndependentView(
                fxmlPath,
                title,
                null,                          // مفيش owner
                null,                          // مفيش modality (زي الكود الأصلي)
                true,                          // resizable
                (ctrl, stage) -> {
                    AppearanceSettingsController controller =
                            (AppearanceSettingsController) ctrl;

                    // ✅ نأجّل setViewId لبعد show() — بنستخدم Platform.runLater
                    javafx.application.Platform.runLater(
                            () -> controller.setViewId(title));
                }
        );
    }

    public static void openGeneral() {
        List<String> views = ViewRegistry.getAll();
        open(views.isEmpty() ? null : views.get(0));
    }
}