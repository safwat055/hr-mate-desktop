package com.safwat.hr.controller.leave;

import com.safwat.hr.controller.employee.dto.EmployeeSearchResult;
import com.safwat.hr.controller.leave.dto.LatePermissionDto;
import com.safwat.hr.controller.leave.dto.LatePermissionRequest;
import com.safwat.hr.controller.leave.service.LeaveApiClient;
import com.safwat.hr.ui.TextFieldSetupHelper;
import com.safwat.hr.ui.UiAsync;
import com.safwat.hr.ui.controls.SAFNotification;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class LatePermissionDialogController implements Initializable {

    @FXML
    private Label employeeLabel;
    @FXML
    private TextField dateField;
    @FXML
    private TextField hoursField;
    @FXML
    private TextArea notesArea;
    @FXML
    private Button cancelButton;
    @FXML
    private Button saveButton;

    private final LeaveApiClient api = new LeaveApiClient();
    private EmployeeSearchResult employee;
    private LatePermissionDto existing;
    private boolean saved = false;

    public boolean isSaved() {
        return saved;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        TextFieldSetupHelper.setupDateFields(dateField);
        TextFieldSetupHelper.setupDecimalFields(2, hoursField);
        cancelButton.setOnAction(e -> close());
        saveButton.setOnAction(e -> onSave());
    }

    public void init(EmployeeSearchResult emp, LatePermissionDto existing) {
        this.employee = emp;
        this.existing = existing;
        employeeLabel.setText(emp.fullName() + " — " + emp.nationalId());

        if (existing != null) {
            dateField.setText(existing.getDate() == null ? "" : existing.getDate().toString());
            hoursField.setText(existing.getHours() == null ? "" : existing.getHours().toPlainString());
            notesArea.setText(existing.getNotes() == null ? "" : existing.getNotes());
        }
    }

    private void onSave() {
        LocalDate date = TextFieldSetupHelper.parseDateInput(dateField.getText());
        if (date == null) {
            SAFNotification.error("تاريخ غير صالح");
            return;
        }
        BigDecimal hours;
        try {
            hours = new BigDecimal(hoursField.getText().trim());
        } catch (Exception ex) {
            SAFNotification.error("عدد الساعات غير صالح");
            return;
        }
        if (hours.compareTo(BigDecimal.ZERO) <= 0 || hours.compareTo(BigDecimal.valueOf(24)) > 0) {
            SAFNotification.error("عدد الساعات يجب أن يكون بين 0 و 24");
            return;
        }

        LatePermissionRequest req = new LatePermissionRequest();
        req.setNationalId(employee.nationalId());
        req.setDate(date);
        req.setHours(hours);
        req.setNotes(notesArea.getText());

        saveButton.setDisable(true);

        UiAsync.runVoid(() -> {
            try {
                if (existing == null) {
                    api.createLate(req);
                } else {
                    api.updateLate(existing.getId(), req);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, () -> {
            saved = true;
            SAFNotification.success(existing == null ? "تم تسجيل الإذن" : "تم تعديل الإذن");
            close();
        }, ex -> {
            saveButton.setDisable(false);
            SAFNotification.error(UiAsync.friendly(ex));
        });
    }

    private void close() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
}