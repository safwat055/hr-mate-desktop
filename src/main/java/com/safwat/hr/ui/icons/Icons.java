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
        ImageView imag = new ImageView();
        imag.setImage(new Image(Objects.requireNonNull(getClass().getResource("/com/safwat/hr/icons/pdf2.png")).toExternalForm()));

        imag.setFitHeight(30);
        imag.setFitHeight(30);
        // Pdf.setPreserveRatio(true);
        // button.getStyleClass().clear();
        button.setGraphic(imag);

    }


    /**
     *
     * @param button
     */
    public void getSaveImage(Button button) {
        ImageView imag = new ImageView();
        imag.setImage(new Image(Objects.requireNonNull(getClass().getResource("/com/safwat/hr/icons/save2.png")).toExternalForm()));

        imag.setFitHeight(30);
        imag.setFitHeight(30);
        imag.setPreserveRatio(true);
        button.getStyleClass().clear();
        button.setGraphic(imag);

    }

    public void getMailImage(Button button) {
        ImageView imageView = new ImageView(
                new Image(Objects.requireNonNull(
                        getClass().getResource("/com/safwat/hr/icons/mail.png")
                ).toExternalForm())
        );

        imageView.setFitWidth(55);
        imageView.setFitHeight(55);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        button.getStyleClass().clear();
        button.setGraphic(imageView);
    }


    public void getChatImage(Button button) {
        ImageView imageView = new ImageView(
                new Image(Objects.requireNonNull(
                        getClass().getResource("/com/safwat/hr/icons/chat.png")
                ).toExternalForm())
        );

        imageView.setFitWidth(55);
        imageView.setFitHeight(55);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        button.getStyleClass().clear();
        button.setGraphic(imageView);
    }

    public void getReportImage(Button button) {
        ImageView imageView = new ImageView(
                new Image(Objects.requireNonNull(
                        getClass().getResource("/com/safwat/hr/icons/task_report.png")
                ).toExternalForm())
        );

        imageView.setFitWidth(55);
        imageView.setFitHeight(55);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        button.getStyleClass().clear();
        button.setGraphic(imageView);
    }

    public void getBellImage(Label bellLabel) {
        ImageView imageView = new ImageView(
                new Image(Objects.requireNonNull(
                        getClass().getResource("/com/safwat/hr/icons/bell50.png")
                ).toExternalForm())
        );

        imageView.setFitWidth(45);
        imageView.setFitHeight(45);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        bellLabel.setGraphic(imageView);
    }
}
