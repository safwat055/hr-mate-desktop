package com.safwat.hr.ui.util;

import com.safwat.hr.ui.controls.SAFNotification;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class ViewManager {

    public static void openIndependentView(String fxmlFile, String cssPath) {

        try {
            Parent view = FXMLLoader.load(
                    Objects.requireNonNull(ViewManager.class.getResource(fxmlFile)));

            if (cssPath != null && !cssPath.isBlank()) {
                view.getStylesheets().add(
                        Objects.requireNonNull(
                                ViewManager.class.getResource(cssPath)
                        ).toExternalForm()
                );
            }

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(view));
            AppTheme.apply(stage.getScene());
            String iconPath = Objects.requireNonNull(
                            ViewManager.class.getResource("/com/safwat/hr/icons/logo.png"))
                    .toExternalForm();

            stage.getIcons().add(new Image(iconPath));
            stage.setResizable(false);
            stage.setTitle("HR_MANAGEMENT");
            stage.show();

        } catch (IOException ex) {
            ex.printStackTrace();
            SAFNotification.error(ex.getMessage());
            log.error(ex.getMessage(), ex);
        }
    }

    public static void openNoIndependentView(String fxmlFile, String cssPath) {

        try {
            Parent view = FXMLLoader.load(Objects.requireNonNull(ViewManager.class.getResource(fxmlFile)));
            Stage stage = new Stage();
            if (cssPath != null && !cssPath.isBlank()) {
                view.getStylesheets().add(
                        Objects.requireNonNull(
                                ViewManager.class.getResource(cssPath)
                        ).toExternalForm()
                );
            }
            String iconPath = Objects.requireNonNull(ViewManager.class.getResource("/safwat/icons/123.png")).toString();
            stage.getIcons().add(new Image(iconPath));
            stage.setTitle("HR_MANAGEMENT");
            stage.setResizable(false);
            stage.setScene(new Scene(view));
            stage.show();
        } catch (IOException ex) {
            SAFNotification.error(ex.getMessage());
            log.error(ex.getMessage());
        }

    }

    public static void openNoIndependentView(String fxmlFile, String cssPath, boolean isResizeAble) {

        try {
            Parent view = FXMLLoader.load(Objects.requireNonNull(ViewManager.class.getResource(fxmlFile)));
            Stage stage = new Stage();
            if (cssPath != null && !cssPath.isBlank()) {
                view.getStylesheets().add(
                        Objects.requireNonNull(
                                ViewManager.class.getResource(cssPath)
                        ).toExternalForm()
                );
            }
            String iconpath = Objects.requireNonNull(ViewManager.class.getResource("/safwat/icons/123.png")).toString();
            stage.getIcons().add(new Image(iconpath));
            stage.setTitle("HR_MANAGEMENT");
            stage.setResizable(isResizeAble);
            stage.setScene(new Scene(view));
            stage.show();
        } catch (IOException ex) {
            SAFNotification.error(ex.getMessage());
            log.error(ex.getMessage());
        }

    }

    public static void LoadViewOnMainView(String fxmFile, StackPane MainstackPane) {

        Optional<Node> existingNode = MainstackPane.getChildren().stream()
                .filter(node -> node.getId() != null && node.getId().equals(fxmFile))
                .findFirst();

        if (existingNode.isPresent()) {
            MainstackPane.getChildren().forEach(node -> node.setVisible(false));
            existingNode.get().setVisible(true);

        } else {
            try {
                FXMLLoader loader = new FXMLLoader(ViewManager.class.getResource(fxmFile));

                Parent view = loader.load();
                view.setId(fxmFile);

                MainstackPane.getChildren().forEach(node -> node.setVisible(false));
                MainstackPane.getChildren().add(view);

            } catch (IOException ex) {
                SAFNotification.error(ex.getMessage());
                log.error(ex.getMessage());
            }
        }

    }

    public static void LoadViewOnReportView(String fxmFile, StackPane MainstackPane) {

        LoadViewOnMainView(fxmFile, MainstackPane);

    }

    /**
     *
     * @param relativePath
     * @param controller
     * @return
     */

    public static Parent loadFXML(String relativePath, Object controller) {
        try {
            FXMLLoader loader = new FXMLLoader(controller.getClass().getResource(relativePath));
            loader.setController(controller);
            return loader.load();

        } catch (IOException e) {
            SAFNotification.error(e.getMessage());
            log.error(e.getMessage());
            return null;
        }
    }
}
