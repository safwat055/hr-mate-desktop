package com.safwat.hr.controller.wages.ui;

import com.safwat.hr.controller.wages.dto.WageMonthResultDto;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.util.Map;

/**
 * بوباب تفاصيل شهر واحد من نتيجة محرك الأجور المتغيرة.
 *
 * <p>بيشتغل بمودين مختلفين حسب الزرار اللي فتحه:
 * <ul>
 *   <li>{@link DetailsMode#ALLOWANCES} — بيعرض {@code allowanceBreakdown}
 *       بس (البدلات كما حسبها AllowanceEngine)، من غير قسم الملاحظات.</li>
 *   <li>{@link DetailsMode#DOCUMENTS} — بيعرض {@code breakdown} (الأساس
 *       الفعلي المبني من المستندات) + {@code documentNotes}.</li>
 * </ul>
 */
public class CycleMonthDetailsController {

    public enum DetailsMode { ALLOWANCES, DOCUMENTS }

    @FXML private Label monthLabel;
    @FXML private Label sourceLabel;
    @FXML private Label rawTotalLabel;
    @FXML private Label ceilingLabel;
    @FXML private Label pensionableLabel;

    @FXML private Label breakdownTitleLabel;
    @FXML private TableView<Map.Entry<String, BigDecimal>> breakdownTable;
    @FXML private TableColumn<Map.Entry<String, BigDecimal>, String> itemNameColumn;
    @FXML private TableColumn<Map.Entry<String, BigDecimal>, String> itemValueColumn;

    @FXML private VBox notesBox;
    @FXML private Label notesLabel;

    @FXML
    public void initialize() {
        itemNameColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getKey()));
        itemValueColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getValue().toPlainString()));
    }

    private void bind(WageMonthResultDto row, DetailsMode mode) {
        monthLabel.setText(row.monthLabel());
        sourceLabel.setText(row.sourceLabel());
        rawTotalLabel.setText(row.rawTotal().toPlainString());
        ceilingLabel.setText(row.pensionCeiling() == null ? "—" : row.pensionCeiling().toPlainString());
        pensionableLabel.setText(row.totalPensionableWage().toPlainString()
                + (row.ceilingApplied() ? "  (مقطوع بالحد الأقصى)" : ""));

        if (mode == DetailsMode.ALLOWANCES) {
            breakdownTitleLabel.setText("تفاصيل البدلات (AllowanceEngine)");
            breakdownTable.setItems(FXCollections.observableArrayList(row.allowanceBreakdown().entrySet()));
            notesBox.setVisible(false);
            notesBox.setManaged(false);
        } else {
            breakdownTitleLabel.setText("تفاصيل المستندات");
            breakdownTable.setItems(FXCollections.observableArrayList(row.breakdown().entrySet()));
            notesBox.setVisible(true);
            notesBox.setManaged(true);
            notesLabel.setText(row.documentNotes().isEmpty() ? "—" : String.join("، ", row.documentNotes()));
        }
    }

    /** يفتح بوباب تفاصيل البدلات فقط (متاح دايمًا). */
    public static void openAllowances(WageMonthResultDto row) {
        open(row, DetailsMode.ALLOWANCES);
    }

    /** يفتح بوباب تفاصيل المستندات (بيتفتح بس لما documentNotes مش فاضية). */
    public static void openDocuments(WageMonthResultDto row) {
        open(row, DetailsMode.DOCUMENTS);
    }

    private static void open(WageMonthResultDto row, DetailsMode mode) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    CycleMonthDetailsController.class.getResource("/com/safwat/hr/controller/CycleMonthDetails.fxml"));
            Parent root = loader.load();
            CycleMonthDetailsController controller = loader.getController();
            controller.bind(row, mode);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle((mode == DetailsMode.ALLOWANCES ? "تفاصيل البدلات — " : "تفاصيل المستندات — ")
                    + row.monthLabel());
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (Exception e) {
            throw new RuntimeException("تعذر فتح نافذة التفاصيل", e);
        }
    }
}
