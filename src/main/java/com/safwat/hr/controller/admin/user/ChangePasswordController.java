package com.safwat.hr.controller.admin.user;

import com.safwat.hr.controller.admin.system.AppLogBus;
import com.safwat.hr.network.ApiClient;
import com.safwat.hr.network.ApiResponse;
import com.safwat.hr.network.SessionManager;
import com.safwat.hr.network.dto.AdminUserDtos.ChangePasswordRequest;
import com.safwat.hr.ui.theme.SettingsThemeLoader;
import com.safwat.hr.ui.util.ViewManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * تغيير كلمة المرور الشخصية — POST /api/auth/change-password
 * <p>
 * اليوزر بيتاخد من الـ JWT تلقائيًا (مفيش حاجة بتتبعت من الواجهة).
 * أي يوزر مسجّل دخول يقدر يستخدمها.
 * <p>
 * كل التنسيقات من settings.css — لا يوجد inline styles.
 */
public class ChangePasswordController implements Initializable {

    private static final String CSS_PATH = "/com/safwat/hr/css/settings.css";
    private static final String FXML_PATH =
            "/com/safwat/hr/controller/admin/user/ChangePasswordDialog.fxml";

    // ══════════════ FXML ══════════════

    @FXML
    private Label lblUser;
    @FXML
    private PasswordField txtCurrent, txtNew, txtConfirm;
    @FXML
    private Label lblError;
    @FXML
    private Button btnSave, btnCancel;

    private Stage stage;

    // ══════════════ Init ══════════════

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        SessionManager session = SessionManager.getInstance();
        String display = session.getDisplayName() != null && !session.getDisplayName().isBlank()
                ? session.getDisplayName()
                : session.getUsername();
        lblUser.setText("المستخدم: " + display);
        SettingsThemeLoader.apply(btnCancel);
    }

    void setStage(Stage stage) {
        this.stage = stage;
    }

    // ══════════════ Main Action ══════════════

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
                        // الباك ايند بيرجّع رسائل عربية واضحة (400/401)
                        showError(resp.getMessage() != null
                                ? resp.getMessage()
                                : "فشل تغيير كلمة المرور");
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

    // ══════════════ Helpers ══════════════

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

    // ══════════════ Static Open ══════════════

    /**
     * فتح النافذة كـ Dialog modal.
     */
    public static void open(Stage owner) {
        ViewManager.openIndependentView(
                FXML_PATH,
                "🔑 تغيير كلمة المرور",
                owner,
                Modality.WINDOW_MODAL,
                false, // resizable
                (ctrl, stage) -> ((ChangePasswordController) ctrl).setStage(stage)
        );
    }
}