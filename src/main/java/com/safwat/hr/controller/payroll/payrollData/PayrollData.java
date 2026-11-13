package com.safwat.hr.controller.payroll.payrollData;

import com.safwat.hr.network.ApiResponse;
import com.safwat.hr.shared.ui.SearchDialog;
import com.safwat.hr.shared.ui.SmartSearchHelper;
import com.safwat.hr.ui.controls.SAFNotification;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

public class PayrollData implements Initializable {

    @FXML
    private Button btn_NewPayId, btn_UserManagement, btn_lastPayId, btn_payManagement;

    @FXML
    private TextField txt_NewPayId, txt_Paymanagement, txt_UserManagement, txt_lastPayId;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        txt_lastPayId.setEditable(false);
        txt_NewPayId.setEditable(false);

        btn_lastPayId.setOnAction(e -> loadInto(
                PayrollDataApi.lastPayNumberAsync(), txt_lastPayId, btn_lastPayId));
        btn_NewPayId.setOnAction(e -> loadInto(
                PayrollDataApi.newPayNumberAsync(), txt_NewPayId, btn_NewPayId));

        bindList(txt_Paymanagement, btn_payManagement,
                PayrollDataApi::indexManagements, "إدارات الفهرس");
        bindList(txt_UserManagement, btn_UserManagement,
                PayrollDataApi::userManagements, "إدارات المستخدم");
    }

    /**
     * يجيب قيمة من الباك في الخلفية ويحطها في الحقل على الـ FX thread.
     */
    private void loadInto(CompletableFuture<ApiResponse<String>> future, TextField target, Button source) {
        source.setDisable(true);
        future.whenComplete((resp, ex) -> Platform.runLater(() -> {
            source.setDisable(false);
            if (ex != null || resp == null) {
                SAFNotification.error("تعذر الاتصال بالسيرفر");
                return;
            }
            if (resp.isSuccess() && resp.getData() != null) {
                target.setText(resp.getData());
            } else {
                target.clear();
                SAFNotification.warning(resp.getMessage() != null ? resp.getMessage() : "لا توجد بيانات");
            }
        }));
    }

    /**
     * زر ":" بيفتح قائمة اختيار، واللي يتختار يتكتب في الحقل.
     */
    private void bindList(TextField field, Button button,
                          java.util.function.Supplier<java.util.List<String>> supplier, String title) {
        SmartSearchHelper.bind(
                field, button,
                supplier::get,
                SearchDialog.builder(String.class)
                        .title(title)
                        .column("الإدارة", s -> s == null ? "" : s)
                        .searchPlaceholder("اكتب جزءاً من الاسم..."),
                _ -> {
                },
                SmartSearchHelper.FieldBind.of(field, s -> s)
        );
    }
}