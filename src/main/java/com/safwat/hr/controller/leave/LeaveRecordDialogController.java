package com.safwat.hr.controller.leave;

import com.safwat.hr.controller.employee.dto.EmployeeSearchResult;
import com.safwat.hr.controller.leave.dto.LeaveRecordDto;
import com.safwat.hr.controller.leave.dto.LeaveRecordRequest;
import com.safwat.hr.controller.leave.dto.LeaveTypeDto;
import com.safwat.hr.controller.leave.service.LeaveApiClient;
import com.safwat.hr.ui.TextFieldSetupHelper;
import com.safwat.hr.ui.UiAsync;
import com.safwat.hr.ui.controls.SAFNotification;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class LeaveRecordDialogController implements Initializable {

    @FXML
    private Label employeeLabel;
    @FXML
    private ComboBox<LeaveTypeDto> leaveTypeCombo;
    @FXML
    private TextField fromDateField;
    @FXML
    private Label toDateLabel;
    @FXML
    private TextField toDateField;
    @FXML
    private Label openEndedHint;
    @FXML
    private Label daysPreviewLabel;
    @FXML
    private TextArea notesArea;
    @FXML
    private Label warningLabel;
    @FXML
    private Button cancelButton;
    @FXML
    private Button saveButton;

    private final LeaveApiClient api = new LeaveApiClient();
    private EmployeeSearchResult employee;
    private LeaveTypeDto selectedType;
    private LeaveRecordDto existing;
    private boolean saved = false;

    public boolean isSaved() {
        return saved;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        TextFieldSetupHelper.setupDateFields(fromDateField, toDateField);
        leaveTypeCombo.valueProperty().addListener((obs, o, n) -> onTypeChanged(n));
        fromDateField.textProperty().addListener((obs, o, n) -> updatePreview());
        toDateField.textProperty().addListener((obs, o, n) -> updatePreview());

        cancelButton.setOnAction(e -> close());
        saveButton.setOnAction(e -> onSave());
    }

    public void init(EmployeeSearchResult emp, List<LeaveTypeDto> allTypes, LeaveRecordDto existing) {
        this.employee = emp;
        this.existing = existing;

        employeeLabel.setText(emp.fullName() + " — " + emp.nationalId());

        List<LeaveTypeDto> filtered = new ArrayList<>(allTypes);
        leaveTypeCombo.setItems(FXCollections.observableArrayList(filtered));

        if (existing != null) {
            // تعديل: اختر النوع الحالي
            filtered.stream()
                    .filter(t -> t.getCode().equals(existing.getLeaveTypeCode()))
                    .findFirst()
                    .ifPresent(t -> leaveTypeCombo.setValue(t));
            fromDateField.setText(existing.getFromDate() == null ? "" : existing.getFromDate().toString());
            toDateField.setText(existing.getToDate() == null ? "" : existing.getToDate().toString());
            notesArea.setText(existing.getNotes() == null ? "" : existing.getNotes());
        } else {
            leaveTypeCombo.getSelectionModel().selectFirst();
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  عند تغيير النوع
    // ═══════════════════════════════════════════════════════════

    private void onTypeChanged(LeaveTypeDto type) {
        this.selectedType = type;
        if (type == null) return;

        boolean openEnded = type.isOpenEnded();

        toDateLabel.setVisible(true);
        toDateLabel.setManaged(true);
        toDateField.setVisible(true);
        toDateField.setManaged(true);

        toDateLabel.setText(openEnded ? "إلى تاريخ (اختياري):" : "إلى تاريخ:");

        openEndedHint.setVisible(openEnded);
        openEndedHint.setManaged(openEnded);
        openEndedHint.setText(openEnded
                ? "اتركه فارغًا إذا كانت الخدمة مستمرة — يمكن إضافة تاريخ النهاية لاحقًا عند العودة"
                : "");

        updatePreview();
    }

    // ═══════════════════════════════════════════════════════════
    //  معاينة الأيام
    // ═══════════════════════════════════════════════════════════

    private void updatePreview() {
        String fromText = fromDateField.getText();
        String toText = toDateField.getText();

        LocalDate from = TextFieldSetupHelper.parseDateInput(fromText);
        LocalDate to = (toText == null || toText.isBlank())
                ? null
                : TextFieldSetupHelper.parseDateInput(toText);

        // عدد الأيام
        if (from != null && to != null && !to.isBefore(from)) {
            long days = to.toEpochDay() - from.toEpochDay() + 1;
            daysPreviewLabel.setText(days + " يوم");
        } else if (from != null && selectedType != null && selectedType.isOpenEnded()) {
            daysPreviewLabel.setText("مفتوح (بدون تاريخ نهاية)");
        } else {
            daysPreviewLabel.setText("—");
        }

        // تحذير معاينة (غير مانع)
        if (selectedType != null && selectedType.getRules() != null
                && from != null && to != null && !to.isBefore(from)) {
            Integer maxDays = selectedType.getRules().getMaxDaysPerRequest();
            if (maxDays != null) {
                long days = to.toEpochDay() - from.toEpochDay() + 1;
                if (days > maxDays) {
                    showWarning("⚠️ هذا النوع بحد أقصى " + maxDays + " يوم في المرة الواحدة");
                    return;
                }
            }
        }
        hideWarning();
    }

    private void showWarning(String msg) {
        warningLabel.setText(msg);
        warningLabel.setVisible(true);
        warningLabel.setManaged(true);
    }

    private void hideWarning() {
        warningLabel.setVisible(false);
        warningLabel.setManaged(false);
    }

    // ═══════════════════════════════════════════════════════════
    //  الحفظ
    // ═══════════════════════════════════════════════════════════

    private void onSave() {
        if (selectedType == null) {
            SAFNotification.error("اختر نوع الإجازة");
            return;
        }
        LocalDate from = TextFieldSetupHelper.parseDateInput(fromDateField.getText());
        if (from == null) {
            SAFNotification.error("تاريخ البداية غير صالح");
            return;
        }

        LocalDate to = null;
        String toText = toDateField.getText();

        if (selectedType.isOpenEnded()) {
            // ═══ اختياري: لو مليان يبقى مغلق، لو فاضي يبقى مفتوح ═══
            if (toText != null && !toText.isBlank()) {
                to = TextFieldSetupHelper.parseDateInput(toText);
                if (to == null) {
                    SAFNotification.error("تاريخ النهاية غير صالح");
                    return;
                }
                if (to.isBefore(from)) {
                    SAFNotification.error("تاريخ النهاية قبل البداية");
                    return;
                }
            }
        } else {
            // ═══ إلزامي ═══
            to = TextFieldSetupHelper.parseDateInput(toText);
            if (to == null) {
                SAFNotification.error("تاريخ النهاية غير صالح");
                return;
            }
            if (to.isBefore(from)) {
                SAFNotification.error("تاريخ النهاية قبل البداية");
                return;
            }
        }

        LeaveRecordRequest req = new LeaveRecordRequest();
        req.setNationalId(employee.nationalId());
        req.setLeaveTypeCode(selectedType.getCode());
        req.setFromDate(from);
        req.setToDate(to);          // ← null لو مفتوح
        req.setNotes(notesArea.getText());

        saveButton.setDisable(true);

        UiAsync.runVoid(() -> {
            try {
                if (existing == null) {
                    api.createRecord(req);
                } else {
                    api.updateRecord(existing.getId(), req);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, () -> {
            saved = true;
            SAFNotification.success(existing == null ? "تم تسجيل الإجازة" : "تم تعديل الإجازة");
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