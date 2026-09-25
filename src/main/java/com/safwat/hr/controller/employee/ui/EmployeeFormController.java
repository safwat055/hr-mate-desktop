package com.safwat.hr.controller.employee.ui;

import com.safwat.hr.controller.employee.dto.*;
import com.safwat.hr.controller.employee.enums.TerminationReason;
import com.safwat.hr.network.ApiClient;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;

public class EmployeeFormController {

    @FXML private TextField employeeNumberField;
    @FXML private TextField fullNameField;
    @FXML private TextField nationalIdField;
    @FXML private DatePicker hireDatePicker;
    @FXML private DatePicker terminationDatePicker;
    @FXML private ComboBox<TerminationReason> terminationReasonCombo;
    @FXML private ComboBox<SectorOption> sectorCombo; // املأها من غير هذا الملف: GET /api/sectors

    @FXML private Label currentSocialStatusLabel;
    @FXML private Label currentJobTitleLabel;

    @FXML private Button saveButton;
    @FXML private Button deleteButton;
    @FXML private Button socialStatusHistoryButton;
    @FXML private Button jobTitleHistoryButton;


    private Long employeeId; // null يعني موظف جديد لسه مش متحفظ
    private EmployeeProfileDto current;

    @FXML
    public void initialize() {
        terminationReasonCombo.getItems().setAll(TerminationReason.values());
        terminationReasonCombo.setCellFactory(cb -> reasonCell());
        terminationReasonCombo.setButtonCell(reasonCell());

        saveButton.setOnAction(e -> onSave());
        deleteButton.setOnAction(e -> onDelete());
        socialStatusHistoryButton.setOnAction(e -> openSocialStatusHistory());
        jobTitleHistoryButton.setOnAction(e -> openJobTitleHistory());
    }

    /** يتنادى من شاشة القائمة لما المستخدم يفتح موظف موجود */
    public void loadEmployee(Long id) {
        this.employeeId = id;
        try {
            current = ApiClient.get("/employees/" + id, EmployeeProfileDto.class).getData();
            bindToForm(current);
        } catch (Exception ex) {
            showError("تعذر تحميل بيانات الموظف", ex);
        }
    }

    /** يتنادى لما المستخدم يفتح فورم "موظف جديد" */
    public void newEmployee() {
        this.employeeId = null;
        this.current = null;
        clearForm();
        deleteButton.setDisable(true);
        socialStatusHistoryButton.setDisable(true);
        jobTitleHistoryButton.setDisable(true);
    }

    private void bindToForm(EmployeeProfileDto dto) {
        employeeNumberField.setText(dto.employeeNumber());
        employeeNumberField.setDisable(true); // رقم الموظف لا يتعدل بعد الإنشاء
        fullNameField.setText(dto.fullName());
        nationalIdField.setText(dto.nationalId());
        hireDatePicker.setValue(dto.hireDate());
        terminationDatePicker.setValue(dto.terminationDate());
        terminationReasonCombo.setValue(dto.terminationReason());

        currentSocialStatusLabel.setText(dto.currentSocialStatusLabelAr() != null ? dto.currentSocialStatusLabelAr() : "—");
        currentJobTitleLabel.setText(dto.currentJobTitleNameAr() != null ? dto.currentJobTitleNameAr() : "—");

        deleteButton.setDisable(false);
        socialStatusHistoryButton.setDisable(false);
        jobTitleHistoryButton.setDisable(false);
    }

    private void clearForm() {
        employeeNumberField.clear();
        employeeNumberField.setDisable(false);
        fullNameField.clear();
        nationalIdField.clear();
        hireDatePicker.setValue(null);
        terminationDatePicker.setValue(null);
        terminationReasonCombo.setValue(null);
        currentSocialStatusLabel.setText("—");
        currentJobTitleLabel.setText("—");
    }

    private void onSave() {
        try {
            if (employeeId == null) {
                EmployeeCreateRequest req = new EmployeeCreateRequest(
                        employeeNumberField.getText(),
                        fullNameField.getText(),
                        nationalIdField.getText(),
                        hireDatePicker.getValue(),
                        sectorCombo.getValue() != null ? sectorCombo.getValue().id() : null
                );
                current = ApiClient.post("/employees", req, EmployeeProfileDto.class).getData();
                employeeId = current.id();
            } else {
                EmployeeUpdateRequest req = new EmployeeUpdateRequest(
                        fullNameField.getText(),
                        nationalIdField.getText(),
                        hireDatePicker.getValue(),
                        terminationDatePicker.getValue(),
                        terminationReasonCombo.getValue()
                );
                current = ApiClient.put("/employees/" + employeeId, req, EmployeeProfileDto.class).getData();
            }
            bindToForm(current);
            showInfo("تم الحفظ بنجاح");
        } catch (Exception ex) {
            showError("تعذر حفظ بيانات الموظف", ex);
        }
    }

    private void onDelete() {
        if (employeeId == null) return;
        Alert confirm = new Alert(AlertType.CONFIRMATION, "متأكد من حذف الموظف؟", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    ApiClient.delete("/employees/" + employeeId);
                    newEmployee();
                    showInfo("تم الحذف");
                } catch (Exception ex) {
                    showError("تعذر حذف الموظف", ex);
                }
            }
        });
    }

    private void openSocialStatusHistory() {
        HistoryDialogController.openSocialStatus(employeeId, current.socialStatusHistory(), updated -> {
            currentSocialStatusLabel.setText(updated.currentSocialStatusLabelAr() != null ? updated.currentSocialStatusLabelAr() : "—");
            current = updated;
        });
    }

    private void openJobTitleHistory() {
        HistoryDialogController.openJobTitle(employeeId, current.jobTitleHistory(), current.sectorId(), updated -> {
            currentJobTitleLabel.setText(updated.currentJobTitleNameAr() != null ? updated.currentJobTitleNameAr() : "—");
            current = updated;
        });
    }

    private ListCell<TerminationReason> reasonCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(TerminationReason item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getLabelAr());
            }
        };
    }

    private void showInfo(String msg) {
        new Alert(AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }

    private void showError(String msg, Exception ex) {
        new Alert(AlertType.ERROR, msg + "\n" + ex.getMessage(), ButtonType.OK).showAndWait();
    }

    /** عنصر بسيط لعرض القطاع في ComboBox - املأه من GET /api/sectors (مش موجود في هذا التسليم) */
    public record SectorOption(Long id, String nameAr) {
        @Override
        public String toString() {
            return nameAr;
        }
    }
}