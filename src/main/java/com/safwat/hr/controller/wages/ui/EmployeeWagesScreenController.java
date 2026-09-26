package com.safwat.hr.controller.wages.ui;

import com.safwat.hr.network.ApiClient;
import com.safwat.hr.controller.wages.dto.EmployeeProfileDto;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * الشاشة الأم لكل ما يخص الأجور المتغيرة لموظف واحد.
 *
 * <p>بتملك شريط البحث + بيانات الموظف مرة واحدة، وبتوزّع الرقم القومي
 * على تبويبين مستقلين بعد كل بحث ناجح:
 * <ul>
 *   <li>{@code documentsTabController} — سجلات مستندات الأجور المتغيرة (CRUD)</li>
 *   <li>{@code engineTabController} — نتيجة محرك الأجور (عرض فقط)</li>
 * </ul>
 */
public class EmployeeWagesScreenController {

    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private Label searchStatusLabel;

    @FXML private Label employeeNameLabel;
    @FXML private Label employeeNumberLabel;
    @FXML private Label employeeNationalIdLabel;

    // أسماء الحقول دي لازم تطابق fx:id + "Controller" بتاع كل fx:include في الـ FXML
    @FXML private VariableWageDocumentsTableController documentsTabController;
    @FXML private WageEngineCycleTableController engineTabController;

    @FXML
    public void initialize() {
        searchButton.setOnAction(e -> onSearch());
        searchField.setOnAction(e -> onSearch());
    }

    private void onSearch() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim();
        if (query.isEmpty()) {
            searchStatusLabel.setText("اكتب الرقم القومي أو رقم الموظف الأول");
            return;
        }
        try {
            var response = ApiClient.get("/wages/employees/search?query=" + query, EmployeeProfileDto.class);
            if (!response.isSuccess()) {
                searchStatusLabel.setText("مفيش موظف بهذا الرقم");
                clearEmployee();
                return;
            }
            searchStatusLabel.setText("");
            bindEmployee(response.getData());
        } catch (Exception ex) {
            searchStatusLabel.setText("مفيش موظف بهذا الرقم");
            clearEmployee();
        }
    }

    private void bindEmployee(EmployeeProfileDto profile) {
        employeeNameLabel.setText(profile.fullName());
        employeeNumberLabel.setText(profile.employeeNumber());
        employeeNationalIdLabel.setText(profile.nationalId());
        documentsTabController.setNationalId(profile.nationalId());
        engineTabController.setNationalId(profile.nationalId());
    }

    private void clearEmployee() {
        employeeNameLabel.setText("—");
        employeeNumberLabel.setText("—");
        employeeNationalIdLabel.setText("—");
        documentsTabController.setNationalId(null);
        engineTabController.setNationalId(null);
    }
}
