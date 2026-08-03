package com.safwat.hr.chat.ui;

import com.safwat.hr.chat.dto.ChatDTOs.DepartmentDTO;
import com.safwat.hr.chat.service.ChatApiService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * Dialog إرسال رسالة broadcast:
 * - إما لكل الموظفين
 * - أو لقسم معين
 * <p>
 * النتيجة: BroadcastRequest(name, targetDepartmentId nullable)
 */
public class NewBroadcastDialog extends Dialog<NewBroadcastDialog.BroadcastRequest> {

    private final TextField messageNameField = new TextField();
    private final RadioButton rbAll = new RadioButton("كل الموظفين");
    private final RadioButton rbDept = new RadioButton("قسم معين");
    private final ComboBox<DepartmentDTO> deptCombo = new ComboBox<>();
    private final ButtonType sendBtn = new ButtonType("إرسال", ButtonBar.ButtonData.OK_DONE);

    public NewBroadcastDialog(Window owner) {
        initOwner(owner);
        setTitle("رسالة عامة");
        setHeaderText(null);
        getDialogPane().setPrefWidth(400);

        buildContent();
        loadDepartments();
        setupResultConverter();

        updateSendButton();
        messageNameField.textProperty().addListener((o, ov, nv) -> updateSendButton());
        deptCombo.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> updateSendButton());
    }

    private void buildContent() {
        getDialogPane().getButtonTypes().addAll(sendBtn, ButtonType.CANCEL);

        messageNameField.setPromptText("عنوان الرسالة...");

        ToggleGroup tg = new ToggleGroup();
        rbAll.setToggleGroup(tg);
        rbDept.setToggleGroup(tg);
        rbAll.setSelected(true);

        deptCombo.setPromptText("اختر القسم...");
        deptCombo.setMaxWidth(Double.MAX_VALUE);
        deptCombo.setDisable(true);
        deptCombo.setCellFactory(lv -> new DeptCell());
        deptCombo.setButtonCell(new DeptCell());

        rbDept.selectedProperty().addListener((o, ov, nv) -> {
            deptCombo.setDisable(!nv);
            updateSendButton();
        });
        rbAll.selectedProperty().addListener((o, ov, nv) -> updateSendButton());

        HBox radioBox = new HBox(16, rbAll, rbDept);
        radioBox.setAlignment(Pos.CENTER_LEFT);

        VBox content = new VBox(12);
        content.setPadding(new Insets(16));
        content.getChildren().addAll(
                new Label("عنوان الرسالة:"), messageNameField,
                new Separator(),
                new Label("إرسال إلى:"), radioBox, deptCombo
        );

        getDialogPane().setContent(content);
        Platform.runLater(messageNameField::requestFocus);
    }

    private void loadDepartments() {
        ChatApiService.getDepartments().thenAccept(res ->
                Platform.runLater(() -> {
                    if (res.isSuccess() && res.getData() != null) {
                        deptCombo.getItems().setAll(res.getData());
                    }
                })
        );
    }

    private void setupResultConverter() {
        setResultConverter(btn -> {
            if (btn == sendBtn) {
                Long deptId = rbDept.isSelected() && deptCombo.getValue() != null
                        ? deptCombo.getValue().getId() : null;
                return new BroadcastRequest(messageNameField.getText().trim(), deptId);
            }
            return null;
        });
    }

    private void updateSendButton() {
        boolean nameOk = !messageNameField.getText().trim().isEmpty();
        boolean targetOk = rbAll.isSelected()
                || (rbDept.isSelected() && deptCombo.getValue() != null);
        getDialogPane().lookupButton(sendBtn).setDisable(!(nameOk && targetOk));
    }

    public record BroadcastRequest(String name, Long targetDepartmentId) {
    }

    private static class DeptCell extends ListCell<DepartmentDTO> {
        @Override
        protected void updateItem(DepartmentDTO d, boolean empty) {
            super.updateItem(d, empty);
            setText(empty || d == null ? null : d.getName());
        }
    }
}