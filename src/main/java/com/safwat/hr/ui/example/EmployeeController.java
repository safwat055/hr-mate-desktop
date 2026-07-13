package com.safwat.hr.ui.example;

import com.safwat.hr.ui.controls.*;
import com.safwat.hr.ui.util.FxUtils;
import com.safwat.hr.ui.util.Validation;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Example controller showing HRButton, HRTextField, HRTable, etc. in action.
 * ─────────────────────────────────────────────────────────────────────────────
 * Notice: zero MaterialFX imports here.
 * Switching the underlying library = change only inside the HR* classes.
 */
public class EmployeeController implements Initializable {

    // ─── FXML bindings ───────────────────────────────────────────────
    @FXML private Button        saveBtn;
    @FXML private Button        deleteBtn;
    @FXML private Button        printBtn;
    @FXML private Button        cancelBtn;
    @FXML private Button        searchBtn;

    @FXML private TextField     nameField;
    @FXML private TextField     emailField;
    @FXML private TextField     phoneField;

    @FXML private ComboBox<String> departmentCombo;
    @FXML private ComboBox<String> jobTitleCombo;

    @FXML private TableView<Object> employeeTable;
    @FXML private ProgressBar   uploadProgress;
    @FXML private StackPane     rootPane;

    // ─── Initialize ──────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        // ── Buttons ──────────────────────────────────────────────────
        HRButton.primary(saveBtn);
        HRButton.danger(deleteBtn);
        HRButton.flat(false,cancelBtn, printBtn);
        HRButton.outlined(searchBtn);

        // ── Text fields ──────────────────────────────────────────────
        HRTextField.apply(nameField, emailField, phoneField);

        // ── Combo boxes ──────────────────────────────────────────────
        HRComboBox.apply(departmentCombo, jobTitleCombo);

        // ── Table ─────────────────────────────────────────────────────
        HRTable.striped(employeeTable);

        // ── Progress ─────────────────────────────────────────────────
        HRProgress.bar(uploadProgress);
    }

    // ─── Actions ─────────────────────────────────────────────────────

    @FXML
    private void onSave() {
        // Validate
        boolean nameOk  = Validation.require(nameField);
        boolean emailOk = Validation.email(emailField);
        if (!nameOk || !emailOk) return;

        // Show loading overlay
        HRLoading.show(rootPane, "جاري الحفظ...");

        // Simulate async save
        FxUtils.runAsync(
            () -> { sleep(1500); return true; },
            success -> {
                HRLoading.hide(rootPane);
                if (success) {
                    HRNotification.success("تم حفظ بيانات الموظف بنجاح");
                } else {
                    HRNotification.error("فشل الحفظ، يرجى المحاولة مجدداً");
                }
            }
        );
    }

    @FXML
    private void onDelete() {
        boolean confirmed = HRDialog.confirm(
            "حذف الموظف",
            "هل أنت متأكد من حذف هذا الموظف؟ لا يمكن التراجع عن هذا الإجراء."
        );
        if (confirmed) {
            HRNotification.warning("تم حذف الموظف");
        }
    }

    @FXML
    private void onCancel() {
        Validation.clearAll(nameField, emailField, phoneField);
        nameField.clear();
        emailField.clear();
        phoneField.clear();
    }

    @FXML
    private void onPrint() {
        HRNotification.info("جاري تجهيز التقرير للطباعة...");
        // print logic here
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
