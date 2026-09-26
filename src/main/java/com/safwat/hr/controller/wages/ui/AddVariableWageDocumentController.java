package com.safwat.hr.controller.wages.ui;

import com.safwat.hr.network.ApiClient;
import com.safwat.hr.controller.wages.dto.VariableWageDocumentCreateRequest;
import com.safwat.hr.controller.wages.dto.VariableWageDocumentDto;
import com.safwat.hr.shared.util.DateUtils;
import com.safwat.hr.ui.TextFieldSetupHelper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.util.Optional;

public class AddVariableWageDocumentController {

    @FXML private TextField periodMonth;
    @FXML private TextField documentNameField;
    @FXML private TextField documentNumberField;
    @FXML private TextField totalAmountField;
    @FXML private CheckBox subjectToPensionCheck;
    @FXML private TextField pensionSubjectAmountField;
    @FXML private CheckBox subjectToTaxCheck;
    @FXML private TextField taxSubjectAmountField;
    @FXML private TextField taxAmountField;
    @FXML private TextField stampDutyAmountField;
    @FXML private TextArea notesArea;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private String nationalId;
    private Stage stage;
    private VariableWageDocumentDto result;

    @FXML
    public void initialize() {
        saveButton.setOnAction(e -> onSave());
        cancelButton.setOnAction(e -> stage.close());
        TextFieldSetupHelper.setupDateFields(periodMonth);
    }

    private void onSave() {
        if (periodMonth.getText() == null
                || documentNameField.getText().isBlank()
                || documentNumberField.getText().isBlank()
                || totalAmountField.getText().isBlank()) {
            new Alert(Alert.AlertType.WARNING,
                    "الشهر واسم المستند ورقم الشطب والإجمالي حقول إجبارية").showAndWait();
            return;
        }

        BigDecimal total, pensionSubject, taxSubject, tax, stampDuty;
        try {
            total         = new BigDecimal(totalAmountField.getText().trim());
            pensionSubject = parseOptional(pensionSubjectAmountField.getText());
            taxSubject    = parseOptional(taxSubjectAmountField.getText());
            tax           = parseOptional(taxAmountField.getText());
            stampDuty     = parseOptional(stampDutyAmountField.getText());
        } catch (NumberFormatException ex) {
            new Alert(Alert.AlertType.WARNING, "تأكد إن كل المبالغ أرقام صحيحة").showAndWait();
            return;
        }

        VariableWageDocumentCreateRequest req = new VariableWageDocumentCreateRequest(
                nationalId,
                DateUtils.parseDate(periodMonth.getText()),
                documentNameField.getText().trim(),
                documentNumberField.getText().trim(),
                total,
                subjectToPensionCheck.isSelected(),
                pensionSubject,
                subjectToTaxCheck.isSelected(),
                taxSubject,
                tax,
                stampDuty,
                notesArea.getText()
        );

        try {
            var response = ApiClient.post("/variable-wage-documents", req, VariableWageDocumentDto.class);
            if (!response.isSuccess()) {

                new Alert(Alert.AlertType.ERROR, "تعذر حفظ المستند: " + response.getMessage()).showAndWait();
                return;
            }
            result = response.getData();
            stage.close();
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "تعذر حفظ المستند: " + ex.getMessage()).showAndWait();
        }
    }

    private BigDecimal parseOptional(String text) {
        if (text == null || text.isBlank()) return null;
        return new BigDecimal(text.trim());
    }

    /** يفتح البوباب ويرجع المستند المتحفظ لو المستخدم حفظ، أو Optional فاضي لو ألغى */
    public static Optional<VariableWageDocumentDto> open(String nationalId) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    AddVariableWageDocumentController.class.getResource("/com/safwat/hr/controller/AddVariableWageDocument.fxml"));
            Parent root = loader.load();
            AddVariableWageDocumentController controller = loader.getController();
            controller.nationalId = nationalId;

            Stage stage = new Stage();
            controller.stage = stage;
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("إضافة مستند أجور متغيرة");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            return Optional.ofNullable(controller.result);
        } catch (Exception e) {
            throw new RuntimeException("تعذر فتح نافذة الإضافة", e);
        }
    }
}