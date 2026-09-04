package com.safwat.hr.controller.user;

import com.safwat.hr.network.ApiClient;
import com.safwat.hr.network.ApiResponse;
import com.safwat.hr.network.SessionManager;
import com.safwat.hr.network.dto.AdminUserDtos.ChangePasswordRequest;
import com.safwat.hr.system.AppLogBus;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * تغيير كلمة المرور الشخصية — POST /api/auth/change-password
 * اليوزر بيتاخد من الـ JWT تلقائيًا (مفيش حاجة بتتبعت من الواجهة).
 * أي يوزر مسجّل دخول يقدر يستخدمها.
 * كل العناصر JavaFX عادية.
 */
public class ChangePasswordController implements Initializable {

    @FXML
    private Label lblUser;
    @FXML
    private PasswordField txtCurrent, txtNew, txtConfirm;
    @FXML
    private Label lblError;
    @FXML
    private Button btnSave, btnCancel;

    private Stage stage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        SessionManager session = SessionManager.getInstance();
        String display = session.getDisplayName() != null && !session.getDisplayName().isBlank()
                ? session.getDisplayName() : session.getUsername();
        lblUser.setText("المستخدم: " + display);
    }

    void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    private void changePassword() {
        clearError();

        String current = safe(txtCurrent.getText());
        String newPw = safe(txtNew.getText());
        String confirm = safe(txtConfirm.getText());

        if (current.isEmpty() || newPw.isEmpty()) {
            showError("كل الحقول مطلوبة");
            return;
        }
        if (newPw.length() < 6) {
            showError("كلمة المرور الجديدة 6 أحرف على الأقل");
            return;
        }
        if (!newPw.equals(confirm)) {
            showError("تأكيد كلمة المرور مش مطابق");
            return;
        }

        setBusy(true);
        new Thread(() -> {
            try {
                ApiResponse<Void> resp = ApiClient.post(
                        "/auth/change-password",
                        new ChangePasswordRequest(current, newPw),
                        Void.class);

                Platform.runLater(() -> {
                    setBusy(false);
                    if (resp.isSuccess()) {
                        AppLogBus.getInstance().log("[Auth] ✅ تم تغيير كلمة المرور");
                        close();
                        com.safwat.hr.ui.util.AlertUtil.showConfirmation("تم",
                                "تم تغيير كلمة المرور بنجاح");
                    } else {
                        // الباك ايند بيرجّع رسائل عربية واضحة (400/401) بعد التعديل
                        showError(resp.getMessage() != null
                                ? resp.getMessage() : "فشل تغيير كلمة المرور");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setBusy(false);
                    showError("خطأ في الاتصال: " + e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void close() {
        if (stage != null) stage.close();
    }

    // ── helpers ──

    private void setBusy(boolean busy) {
        btnSave.setDisable(busy);
        btnCancel.setDisable(busy);
        txtCurrent.setDisable(busy);
        txtNew.setDisable(busy);
        txtConfirm.setDisable(busy);
    }

    private void showError(String msg) {
        lblError.setText(msg);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void clearError() {
        lblError.setVisible(false);
        lblError.setManaged(false);
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    /**
     * فتح النافذة كـ Dialog modal
     */
    public static void open(Stage owner) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    ChangePasswordController.class.getResource("/com/safwat/hr/view/user/ChangePasswordDialog.fxml"));
            javafx.scene.Parent root = loader.load();
            ChangePasswordController controller = loader.getController();

            Stage stage = new Stage();
            stage.initModality(Modality.WINDOW_MODAL);
            if (owner != null) stage.initOwner(owner);
            stage.setTitle("🔑 تغيير كلمة المرور");
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            controller.setStage(stage);
            stage.show();
        } catch (Exception e) {
            AppLogBus.getInstance().log("[Auth] ❌ فشل فتح نافذة تغيير كلمة المرور: " + e.getMessage());
        }
    }
}