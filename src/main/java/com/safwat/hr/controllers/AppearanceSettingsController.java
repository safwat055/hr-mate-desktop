package com.safwat.hr.controllers;

import com.safwat.hr.shared.ColorSettingsManager;
import com.safwat.hr.shared.FontSettingsManager;
import com.safwat.hr.shared.ViewRegistry;
import com.safwat.hr.shared.ZoomManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class AppearanceSettingsController implements Initializable {

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
     * اللي بيسبب ArrayIndexOutOfBoundsException في PrismTextLayout.
     */
    public static void open(String viewId) {
        final String FXML_PATH = "/com/safwat/hr/controller/appearance_settings.fxml";
        try {
            FXMLLoader loader = new FXMLLoader(
                    AppearanceSettingsController.class.getResource(FXML_PATH));
            Parent root = loader.load();

            AppearanceSettingsController controller = loader.getController();

            Stage stage = new Stage();
            // ✅ بدون initModality — بيتجنب nested event loop على GTK
            stage.setTitle(viewId != null ? "تخصيص الواجهة - " + viewId : "تخصيص الواجهة");
            stage.setScene(new Scene(root));
            stage.show(); // ← show() مش showAndWait()

            // setViewId بعد show() عشان الـ Scene تكون جاهزة
            controller.setViewId(viewId);

        } catch (IOException e) {
            throw new RuntimeException(
                    "تعذر تحميل appearance_settings.fxml — تأكد من المسار: " + FXML_PATH, e);
        }
    }

    public static void openGeneral() {
        List<String> views = ViewRegistry.getAll();
        open(views.isEmpty() ? null : views.get(0));
    }
}