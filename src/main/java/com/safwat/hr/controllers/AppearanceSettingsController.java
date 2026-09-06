package com.safwat.hr.controllers;

import com.safwat.hr.shared.ColorSettingsManager;
import com.safwat.hr.shared.FontSettingsManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * كنترولر شاشة "تخصيص الواجهة" الشاملة — بتضم كل أقسام تخصيص شكل الشاشة
 * (خطوط / ألوان / زوم) في سايدبار واحد على الجنب، وكل قسم بيعرض محتواه
 * في نفس منطقة المحتوى (contentArea) من غير فتح نوافذ منفصلة.
 * <p>
 * دلوقتي شغال منها قسم "خطوط" بس. "ألوان" و"زوم" متسجلين في الليست
 * وظاهرين لكن معطلين (قريبًا) لحد ما نفعّلهم لاحقاً.
 */
public class AppearanceSettingsController implements Initializable {

    /**
     * التصنيفات المتاحة في السايدبار. لتفعيل قسم جديد لاحقاً:
     * 1) غيّر available لـ true.
     * 2) ضيف حالة جديدة في switch جوه showCategory().
     */
    private enum Category {
        FONTS("🔤  خطوط", true),
        COLORS("🎨  ألوان", true),
        ZOOM("🔍  زوم", false);

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

    private String viewId;

    /**
     * لازم تتنادى فوراً بعد تحميل الـ FXML وقبل عرض النافذة، عشان الكنترولر
     * يعرف يخصص إعدادات أي "واجهة/شاشة" (نفس فكرة viewId في FontSettingsManager).
     */
    public void setViewId(String viewId) {
        this.viewId = viewId;
        subtitleLabel.setText("تخصيص واجهة: " + viewId);
        showCategory(Category.FONTS);
        showCategory(Category.COLORS);
        categoryList.getSelectionModel().select(Category.FONTS);
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
                showCategory(n);
            }
        });
    }

    private void showCategory(Category category) {
        if (viewId == null) {
            return; // لسه setViewId ما اتنادتش
        }
        contentArea.getChildren().clear();

        switch (category) {
            case FONTS:
                contentArea.getChildren().add(new FontSettingsManager(viewId).buildPanel());
                break;
            case COLORS:
                contentArea.getChildren().add(ColorSettingsManager.buildPanel());
                break;
            case ZOOM:
                Label placeholder = new Label("🚧 القسم ده هيتفعّل قريبًا");
                placeholder.setStyle("-fx-text-fill:#8b90b8; -fx-font-size:13px;");
                StackPane.setAlignment(placeholder, Pos.CENTER);
                contentArea.getChildren().add(placeholder);
                break;
        }
    }

    // ══════════════════════════════════════════════════════════
    //  فتح الشاشة من أي كنترولر تاني بسطر واحد
    // ══════════════════════════════════════════════════════════

    /**
     * بتفتح شاشة "تخصيص الواجهة" الشاملة لواجهة معينة، وتبدأ بقسم "خطوط" مفتوح.
     * <p>
     * ⚠️ عدّل قيمة FXML_PATH تحت لو مسار ملف appearance_settings.fxml مختلف
     * عندك (حالياً بيفترض إنه جوه resources في نفس حزمة الكنترولرات).
     *
     * @param viewId اسم القسم/الواجهة المطلوب تخصيصها (نفس الاسم المستخدم في applySettings)
     */
    public static void open(String viewId) {
        final String FXML_PATH = "/com/safwat/hr/controller/appearance_settings.fxml";
        try {
            FXMLLoader loader = new FXMLLoader(AppearanceSettingsController.class.getResource(FXML_PATH));
            Parent root = loader.load();

            AppearanceSettingsController controller = loader.getController();
            controller.setViewId(viewId);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("تخصيص الواجهة - " + viewId);
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            throw new RuntimeException("تعذر تحميل appearance_settings.fxml — تأكد من المسار: " + FXML_PATH, e);
        }
    }
}
