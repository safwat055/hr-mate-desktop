package com.safwat.hr.controller.wages.ui;

import com.safwat.hr.network.ApiClient;
import com.safwat.hr.controller.wages.dto.WageMonthResultDto;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.util.List;

/**
 * جدول نتيجة دورة الأجر المتغير (ناتج المحرك) لموظف واحد.
 *
 * <p>عمودين منفصلين للتفاصيل:
 * <ul>
 *   <li>"تفاصيل البدلات" — متاح دايمًا، بيفتح breakdown البدلات
 *       ({@code allowanceBreakdown}) بس.</li>
 *   <li>"تفاصيل المستندات" — بيظهر بس لما الشهر فيه مستندات
 *       ({@code documentNotes} مش فاضية)، بيفتح breakdown المستندات
 *       الفعلي المستخدم في الحساب.</li>
 * </ul>
 */
public class WageEngineCycleTableController {

    @FXML private Button calculateButton;
    @FXML private Label statusLabel;
    @FXML private TableView<WageMonthResultDto> table;
    @FXML private TableColumn<WageMonthResultDto, String> monthColumn;
    @FXML private TableColumn<WageMonthResultDto, String> rawTotalColumn;
    @FXML private TableColumn<WageMonthResultDto, String> ceilingColumn;
    @FXML private TableColumn<WageMonthResultDto, String> pensionableColumn;
    @FXML private TableColumn<WageMonthResultDto, String> ceilingAppliedColumn;
    @FXML private TableColumn<WageMonthResultDto, String> sourceColumn;
    @FXML private TableColumn<WageMonthResultDto, Void> allowanceDetailsColumn;
    @FXML private TableColumn<WageMonthResultDto, Void> documentDetailsColumn;

    private String currentNationalId;

    @FXML
    public void initialize() {
        monthColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().monthLabel()));
        rawTotalColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().rawTotal().toPlainString()));
        ceilingColumn.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().pensionCeiling() == null ? "—" : c.getValue().pensionCeiling().toPlainString()));
        pensionableColumn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().totalPensionableWage().toPlainString()));
        ceilingAppliedColumn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().ceilingApplied() ? "مقطوع" : "—"));
        sourceColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().sourceLabel()));

        addAllowanceDetailsButton();
        addDocumentDetailsButton();
        calculateButton.setOnAction(e -> onCalculate());
        calculateButton.setDisable(true);
    }

    /** يُستدعى من الشاشة الأب بعد نجاح/مسح البحث عن الموظف. */
    public void setNationalId(String nationalId) {
        this.currentNationalId = nationalId;
        calculateButton.setDisable(nationalId == null);
        table.setItems(FXCollections.observableArrayList());
        statusLabel.setText("");
    }

    private void onCalculate() {
        if (currentNationalId == null) return;
        try {
            var response = ApiClient.get(
                    "/wages/engine/cycle?nationalId=" + currentNationalId,
                    WageMonthResultDto[].class);
            if (!response.isSuccess()) {
                statusLabel.setText("تعذر حساب الدورة: " + response.getMessage());
                return;
            }
            statusLabel.setText("");
            table.setItems(FXCollections.observableArrayList(List.of(response.getData())));
        } catch (Exception ex) {
            statusLabel.setText("تعذر حساب الدورة: " + ex.getMessage());
        }
    }

    /** عمود "تفاصيل البدلات" — متاح دايمًا لأن allowanceBreakdown بيترجع في كل شهر. */
    private void addAllowanceDetailsButton() {
        allowanceDetailsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button detailsBtn = new Button("تفاصيل البدلات");

            {
                detailsBtn.setOnAction(e -> {
                    WageMonthResultDto row = getTableRow().getItem();
                    if (row != null) CycleMonthDetailsController.openAllowances(row);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : new HBox(detailsBtn));
            }
        });
    }

    /** عمود "تفاصيل المستندات" — بيظهر الزرار بس لو الشهر ده فيه مستندات فعلاً. */
    private void addDocumentDetailsButton() {
        documentDetailsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button detailsBtn = new Button("تفاصيل المستندات");

            {
                detailsBtn.setOnAction(e -> {
                    WageMonthResultDto row = getTableRow().getItem();
                    if (row != null) CycleMonthDetailsController.openDocuments(row);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                WageMonthResultDto row = empty ? null : getTableRow().getItem();
                boolean hasDocs = row != null && !row.documentNotes().isEmpty();
                setGraphic(hasDocs ? new HBox(detailsBtn) : null);
            }
        });
    }
}
