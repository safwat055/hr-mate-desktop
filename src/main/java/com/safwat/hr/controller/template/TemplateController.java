package com.safwat.hr.controller.template;

import com.safwat.hr.shared.ui.SearchDialog;
import com.safwat.hr.shared.ui.SmartSearchHelper;
import com.safwat.hr.ui.TextFieldSetupHelper;
import com.safwat.hr.ui.controls.SAFNotification;
import com.safwat.hr.ui.util.ViewManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ResourceBundle;

public class TemplateController implements Initializable {
    @FXML
    private Button btn_all;

    @FXML
    private Button btn_download;

    @FXML
    private TextField txt_rowsCount;

    @FXML
    private TextField txt_type;
    private final TemplateService templateService = TemplateService.getInstance();

    /**
     * Called to initialize a controller after its root element has been
     * completely processed.
     *
     * @param location  The location used to resolve relative paths for the root object, or
     *                  {@code null} if the location is not known.
     * @param resources The resources used to localize the root object, or {@code null} if
     *                  the root object was not localized.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setActions();
        setTextFields();
        TextFieldSetupHelper.setupIntegerFields(txt_rowsCount);
    }

    void setActions() {
        btn_download.setOnAction(event -> {
            if (txt_type.getText().isBlank()) {
                SAFNotification.error("يجب تحديد اسم النموذج اولا");
                return;
            }
            int rowsCount = 100;
            if (!txt_type.getText().isEmpty()) {
                try {
                    rowsCount = Integer.parseInt(txt_rowsCount.getText());
                } catch (NumberFormatException e) {

                }

            }

            templateService.downloadTemplate(txt_type.getText(), rowsCount);
            ViewManager.closeWindow(txt_rowsCount);

        });
    }


    void setTextFields() {
        SmartSearchHelper.bind(
                txt_type, btn_all,
                templateService::fetchTemplateTypeNames,
                SearchDialog.builder(String.class)
                        .title("").column("اسم النموذج", s -> s == null ? "" : s), _ -> templateService.fetchTemplateTypeNames(),
                SmartSearchHelper.FieldBind.of(txt_type, value -> value)

        );
    }

}
