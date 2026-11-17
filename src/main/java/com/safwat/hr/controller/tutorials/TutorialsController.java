package com.safwat.hr.controller.tutorials;

import com.safwat.hr.shared.tutorials.TutorialsLoader;
import com.safwat.hr.shared.tutorials.TutorialsLoader.Source;
import com.safwat.hr.shared.tutorials.TutorialsLoader.Tutorial;
import com.safwat.hr.shared.tutorials.TutorialsLoader.TutorialsFile;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * متحكم شاشة الشروحات.
 * كل قسم بيتعرض ككارت، وتحت الشاشة شريط روابط التطبيق والتواصل.
 */
public class TutorialsController {

    private static final double PREVIEW_W = 240;
    private static final double PREVIEW_H = 135;
    private static final double ICON_SIZE = 18;

    @FXML private Label versionLabel;
    @FXML private Label sourceLabel;
    @FXML private VBox sectionsBox;
    @FXML private HBox linksBox;

    /** كاش للصور عشان ما نحملش نفس الصورة مرتين في نفس الجلسة */
    private final Map<String, Image> imageCache = new HashMap<>();

    @FXML
    public void initialize() {
        render();
        buildLinksBar();
    }

    // ══════════════════════════════════════════════════════════════
    //  Actions
    // ══════════════════════════════════════════════════════════════

    @FXML
    private void onRewriteClicked() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("إعادة كتابة الملف");
        confirm.setHeaderText("إعادة كتابة ملف الشروحات من الصفر؟");
        confirm.setContentText("أي تعديل عملته على ملف " + TutorialsLoader.FILE_NAME
                + " هيتمسح ويرجع للنسخة الافتراضية.");

        boolean ok = confirm.showAndWait()
                .filter(b -> b == ButtonType.OK)
                .isPresent();
        if (!ok) return;

        try {
            TutorialsLoader.resetExternalFile();
            render();
        } catch (IOException e) {
            showError("تعذر إعادة كتابة الملف", e.getMessage());
        }
    }

    /** يفتح رابط في الخلفية، ولو فشل يعرض رسالة فيها الرابط عشان المستخدم ينسخه */
    private void runLink(CompletableFuture<Void> future, String label, String url) {
        future.exceptionally(ex -> {
            Platform.runLater(() -> showError("تعذر فتح " + label,
                    "انسخ الرابط وافتحه يدوياً:\n" + url));
            return null;
        });
    }

    // ══════════════════════════════════════════════════════════════
    //  Rendering: الشروحات
    // ══════════════════════════════════════════════════════════════

    private void render() {
        TutorialsFile file = TutorialsLoader.load();

        String updated = file.updatedAt().isEmpty() ? "" : " - آخر تحديث: " + file.updatedAt();
        versionLabel.setText("الإصدار " + file.version() + updated);
        sourceLabel.setText(sourceText(file.source()));

        sectionsBox.getChildren().clear();
        if (file.sections().isEmpty()) {
            sectionsBox.getChildren().add(new Label("لا توجد شروحات متاحة حالياً"));
            return;
        }
        for (Tutorial t : file.sections()) {
            sectionsBox.getChildren().add(buildCard(t));
        }
    }

    private String sourceText(Source source) {
        return switch (source) {
            case EXTERNAL -> "المصدر: ملف " + TutorialsLoader.FILE_NAME + " في مسار التشغيل";
            case BUNDLED -> "المصدر: النسخة الافتراضية";
            case BUNDLED_FALLBACK -> "ملف " + TutorialsLoader.FILE_NAME
                    + " في مسار التشغيل بايظ، والعرض بالنسخة الافتراضية. اضغط إعادة كتابة الملف لإصلاحه";
        };
    }

    private Node buildCard(Tutorial t) {
        StackPane preview = buildPreview(t);

        Label title = new Label(t.title());
        title.getStyleClass().add("tut-title");
        title.setWrapText(true);

        Label desc = new Label(t.description());
        desc.getStyleClass().add("tut-desc");
        desc.setWrapText(true);

        VBox text = new VBox(6, title, desc);
        HBox.setHgrow(text, Priority.ALWAYS);

        HBox top = new HBox(16, preview, text);
        top.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(12, top);
        card.getStyleClass().add("tut-card");

        if (t.hasVideo()) {
            Button open = new Button("فتح الفيديو على يوتيوب");
            open.getStyleClass().add("tut-open-btn");
            open.setOnAction(e -> runLink(
                    TutorialsLoader.openInBrowser(t.youtubeUrl()), "الفيديو", t.youtubeUrl()));
            card.getChildren().add(open);

            preview.setCursor(Cursor.HAND);
            preview.setOnMouseClicked(e -> runLink(
                    TutorialsLoader.openInBrowser(t.youtubeUrl()), "الفيديو", t.youtubeUrl()));
        }
        return card;
    }

    private StackPane buildPreview(Tutorial t) {
        StackPane box = new StackPane();
        box.setMinSize(PREVIEW_W, PREVIEW_H);
        box.setMaxSize(PREVIEW_W, PREVIEW_H);
        box.getStyleClass().add("tut-preview");

        if (!t.hasVideo()) {
            box.getChildren().add(new Label("لا يوجد فيديو"));
            return box;
        }

        Image image = imageCache.computeIfAbsent(t.thumbnailUrl(),
                url -> new Image(url, PREVIEW_W, PREVIEW_H, true, true, true));

        ImageView view = new ImageView(image);
        view.setFitWidth(PREVIEW_W);
        view.setFitHeight(PREVIEW_H);
        view.setPreserveRatio(true);

        Label offline = new Label("المعاينة تحتاج إنترنت");
        offline.getStyleClass().add("tut-offline");
        offline.setWrapText(true);

        view.visibleProperty().bind(Bindings.not(image.errorProperty()));
        offline.visibleProperty().bind(image.errorProperty());

        box.getChildren().addAll(view, offline);
        return box;
    }

    // ══════════════════════════════════════════════════════════════
    //  Rendering: شريط الروابط
    // ══════════════════════════════════════════════════════════════

    private void buildLinksBar() {
        linksBox.getChildren().addAll(
                linkButton("فيسبوك التطبيق", "facebook",
                        TutorialsLoader::openAppFacebookPage, "صفحة التطبيق", "https://www.facebook.com/"),
                linkButton("قائمة الفيديوهات", "youtube",
                        TutorialsLoader::openYouTubeVideos, "قائمة الفيديوهات", "https://www.youtube.com/"),
                linkButton("واتساب", "whatsapp",
                        TutorialsLoader::openWhatsApp, "واتساب", "https://wa.me/201021816864"),
                linkButton("facebook", "facebook",
                        TutorialsLoader::openPersonalFacebook, "فيسبوك", "https://www.facebook.com/"),
                linkButton("GitHub", "github",
                        TutorialsLoader::openGitHubProfile, "GitHub", "https://github.com/safwat055"),
                linkButton("GitHub Pages", "github-pages",
                        TutorialsLoader::openGitHubPages, "GitHub Pages", "https://safwat055.github.io/")
        );
    }

    private Button linkButton(String text, String iconName, Supplier<CompletableFuture<Void>> action,
                              String label, String fallbackUrl) {
        Button b = new Button(text, loadIcon(iconName));
        b.getStyleClass().add("tut-link-btn");
        b.setOnAction(e -> runLink(action.get(), label, fallbackUrl));
        return b;
    }

    /** يحمّل الأيقونة من resources/icons/<name>.png. لو مش موجودة الزرار بيظهر بالنص بس */
    private ImageView loadIcon(String name) {
        try (InputStream in = getClass().getResourceAsStream("/icons/" + name + ".png")) {
            if (in == null) return null;
            return new ImageView(new Image(in, ICON_SIZE, ICON_SIZE, true, true));
        } catch (IOException e) {
            return null;
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════════════════════

    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(header);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}