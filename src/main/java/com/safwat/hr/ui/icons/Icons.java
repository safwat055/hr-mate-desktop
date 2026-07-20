package com.safwat.hr.ui.icons;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.Objects;

public class Icons {

    public static Icons instance;

    private Icons() {

    }

    public static Icons getInstance() {
        if (instance == null) {
            instance = new Icons();
        }
        return instance;

    }

    /**
     *
     * @param button
     */
    public void getPDFImage(Button button) {
        ImageView Pdf = new ImageView();
        Pdf.setImage(new Image(Objects.requireNonNull(getClass().getResource("/com/safwat/hr/icons/pdf2.png")).toExternalForm()));

        Pdf.setFitHeight(30);
        Pdf.setFitHeight(30);
        // Pdf.setPreserveRatio(true);
        // button.getStyleClass().clear();
        button.setGraphic(Pdf);

    }

    public void getBellmage(Label bellLabel) {
        ImageView Pdf = new ImageView();
        Pdf.setImage(new Image(Objects.requireNonNull(getClass().getResource("/com/safwat/hr/icons/bell50.png")).toExternalForm()));

        Pdf.setFitHeight(40);
        Pdf.setFitHeight(40);
        Pdf.setPreserveRatio(true);
        // button.getStyleClass().clear();
        bellLabel.setGraphic(Pdf);

    }

    /**
     *
     * @param button
     */
    public void getSaveImage(Button button) {
        ImageView Pdf = new ImageView();
        Pdf.setImage(new Image(Objects.requireNonNull(getClass().getResource("/com/safwat/hr/icons/save2.png")).toExternalForm()));

        Pdf.setFitHeight(30);
        Pdf.setFitHeight(30);
        Pdf.setPreserveRatio(true);
        button.getStyleClass().clear();
        button.setGraphic(Pdf);

    }

}
