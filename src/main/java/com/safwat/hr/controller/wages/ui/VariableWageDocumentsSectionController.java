package com.safwat.hr.controller.wages.ui;

import com.safwat.hr.network.ApiClient;
import com.safwat.hr.controller.wages.dto.EmployeeProfileDto;
import com.safwat.hr.controller.wages.dto.VariableWageDocumentDto;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.util.List;

public class VariableWageDocumentsSectionController {

    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private Label searchStatusLabel;

    @FXML private Label employeeNameLabel;
    @FXML private Label employeeNumberLabel;
    @FXML private Label employeeNationalIdLabel;

    @FXML private Button addButton;
    @FXML private TableView<VariableWageDocumentDto> table;
    @FXML private TableColumn<VariableWageDocumentDto, String> monthColumn;
    @FXML private TableColumn<VariableWageDocumentDto, String> nameColumn;
    @FXML private TableColumn<VariableWageDocumentDto, String> numberColumn;
    @FXML private TableColumn<VariableWageDocumentDto, String> totalColumn;
    @FXML private TableColumn<VariableWageDocumentDto, String> pensionColumn;
    @FXML private TableColumn<VariableWageDocumentDto, String> taxColumn;
    @FXML private TableColumn<VariableWageDocumentDto, Void> actionsColumn;

    private String currentNationalId;

    @FXML
    public void initialize() {
        monthColumn.setCellValueFactory(c  -> new SimpleStringProperty(c.getValue().periodMonth().toString()));
        nameColumn.setCellValueFactory(c   -> new SimpleStringProperty(c.getValue().documentName()));
        numberColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().documentNumber()));
        totalColumn.setCellValueFactory(c  -> new SimpleStringProperty(c.getValue().totalAmount().toPlainString()));
        pensionColumn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().subjectToPension() ? "خاضع" : "غير خاضع"));
        taxColumn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().subjectToTax() ? "خاضع" : "غير خاضع"));

        addActionButtons();
        searchButton.setOnAction(e -> onSearch());
        searchField.setOnAction(e -> onSearch());
        addButton.setOnAction(e -> onAdd());
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
        currentNationalId = profile.nationalId();
        employeeNameLabel.setText(profile.fullName());
        employeeNumberLabel.setText(profile.employeeNumber());
        employeeNationalIdLabel.setText(profile.nationalId());
        addButton.setDisable(false);
        refreshDocuments();
    }

    private void clearEmployee() {
        currentNationalId = null;
        employeeNameLabel.setText("—");
        employeeNumberLabel.setText("—");
        employeeNationalIdLabel.setText("—");
        addButton.setDisable(true);
        table.setItems(FXCollections.observableArrayList());
    }

    private void refreshDocuments() {
        if (currentNationalId == null) {
            table.setItems(FXCollections.observableArrayList());
            return;
        }
        try {
            var response = ApiClient.get(
                    "/variable-wage-documents?nationalId=" + currentNationalId,
                    VariableWageDocumentDto[].class);
            if (!response.isSuccess()) {
                new Alert(Alert.AlertType.ERROR, "تعذر تحميل المستندات: " + response.getMessage()).showAndWait();
                return;
            }
            table.setItems(FXCollections.observableArrayList(List.of(response.getData())));
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "تعذر تحميل مستندات الأجور المتغيرة: " + ex.getMessage()).showAndWait();
        }
    }

    private void onAdd() {
        if (currentNationalId == null) {
            new Alert(Alert.AlertType.WARNING, "ابحث عن الموظف الأول").showAndWait();
            return;
        }
        AddVariableWageDocumentController.open(currentNationalId)
                .ifPresent(created -> refreshDocuments());
    }

    private void addActionButtons() {
        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button deleteBtn = new Button("حذف");

            {
                deleteBtn.setOnAction(e -> onDelete(getTableRow().getItem()));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : new HBox(deleteBtn));
            }
        });
    }

    private void onDelete(VariableWageDocumentDto row) {
        if (row == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "حذف مستند \"" + row.documentName() + "\" برقم شطب " + row.documentNumber() + "؟",
                ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().filter(bt -> bt == ButtonType.YES).ifPresent(bt -> {
            try {
                ApiClient.delete("/variable-wage-documents/" + row.id());
                refreshDocuments();
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "تعذر الحذف: " + ex.getMessage()).showAndWait();
            }
        });
    }
}