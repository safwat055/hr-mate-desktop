package com.safwat.hr.payroll.payrollManager;

import com.safwat.hr.shared.util.DateUtils;
import com.safwat.hr.ui.controls.SAFNotification;
import com.safwat.hr.ui.util.SearchDialog;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import lombok.Getter;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Getter
public class PayrollManagerController implements Initializable {
    private PayrollManagerService managerService;
    @FXML
// ── Header ──
    private Label lblStatus;

    // ── Annual Report ──
    @FXML
    private Button btnRefreshAnnual;
    // Deletion
    @FXML
    private Button btnDeleteAllAnnual;
    @FXML
    private TextField txtAllMonthsYearly, txtMonthGroupY;
    @FXML
    private Label lblAllMonthsYearly, lblMonthGroupY;
    @FXML
    private Button btnDeleteMonthAnnual;

    @FXML
    private TextField txtGroupAnnual;
    @FXML
    private Button btnDeleteGroupAnnual;
    @FXML
    private ComboBox<String> cmbMonthForEmpAnnual;
    @FXML
    private TextField txtEmpIdAnnual;
    @FXML
    private Button btnDeleteEmpMonthAnnual;
    @FXML
    private ComboBox<String> cmbMonthForPaymentAnnual;
    @FXML
    private TextField txtEmpIdPaymentAnnual;
    @FXML
    private TextField txtPaymentNameAnnual;
    @FXML
    private Button btnDeletePaymentAnnual;
    // Edit
    @FXML
    private TextField txtOldPaymentName;
    @FXML
    private TextField txtNewPaymentName;
    @FXML
    private Button btnUpdatePaymentName;
    @FXML
    private ComboBox<String> cmbDescMonthAnnual;
    @FXML
    private Button btnLoadGroupsForDesc;
    @FXML
    private TableView<GroupDescription> tableGroupDescriptions;
    @FXML
    private TableColumn<GroupDescription, String> colGroupName;
    @FXML
    private TableColumn<GroupDescription, String> colGroupDesc;
    @FXML
    private Button btnSaveDescriptions;

    // ── Review Report ──
    @FXML
    private Button btnRefreshReview;
    // Deletion (same pattern as annual)
    @FXML
    private Button btnDeleteAllReview;
    @FXML
    private ComboBox<String> cmbMonthReview;
    @FXML
    private Button btnDeleteMonthReview;
    @FXML
    private ComboBox<String> cmbMonthForGroupReview;
    @FXML
    private TextField txtGroupReview;
    @FXML
    private Button btnDeleteGroupReview;
    @FXML
    private ComboBox<String> cmbMonthForEmpReview;
    @FXML
    private TextField txtEmpIdReview;
    @FXML
    private Button btnDeleteEmpMonthReview;
    @FXML
    private ComboBox<String> cmbMonthForPaymentReview;
    @FXML
    private TextField txtEmpIdPaymentReview;
    @FXML
    private TextField txtPaymentNameReview;
    @FXML
    private Button btnDeletePaymentReview;
    // Key Update
    @FXML
    private ComboBox<String> cmbKeyMonthReview;
    @FXML
    private CheckBox chkAllMonthsReview;
    @FXML
    private TextField txtOldKeyReview;
    @FXML
    private TextField txtNewKeyReview;
    @FXML
    private Button btnUpdateKeysReview;

    // ── Subscription Report ──
    @FXML
    private Button btnRefreshSub;
    @FXML
    private Button btnDeleteAllSub;
    @FXML
    private ComboBox<String> cmbMonthSub;
    @FXML
    private Button btnDeleteMonthSub;
    @FXML
    private ComboBox<String> cmbMonthForEmpSub;
    @FXML
    private TextField txtEmpIdSub;
    @FXML
    private Button btnDeleteEmpSub;

    /**
     * Called to initialize a controller after its root element has been
     * completely processed.
     *
     * @param location  The location used to resolve relative paths for the root object, or
     *                  {@code null} if the location is not known.
     * @param resources The resources used to localize the root object, or {@code null} if
     *                  the root object was not localized.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        managerService = new PayrollManagerService(this);

        fillMainLists();

        setTxtMonthSearch();
        setButtonsActions();
    }

    void fillMainLists() {
        managerService.setAllMonthsList();
    }

    void setButtonsActions() {
        btnDeleteMonthAnnual.setOnAction(_ -> managerService.deleteOneMonthYearly());
        btnDeleteGroupAnnual.setOnAction(_ -> managerService.deleteTargetPayGroup());
    }

    void setTxtMonthSearch() {
        setupSmartSearch(txtAllMonthsYearly, () ->
                        managerService.allMonthsYearly
                ,
                val -> lblAllMonthsYearly.setText(
                        DateUtils.toArabicMonthYear(DateUtils.getFirstDayOfMonth(val))
                )
        );

        setupSmartSearch(txtMonthGroupY, () -> managerService.allMonthsYearly,
                val -> lblMonthGroupY.setText(
                        DateUtils.toArabicMonthYear(DateUtils.getFirstDayOfMonth(val))
                )
        );
        setupSmartSearch(txtGroupAnnual, () -> managerService.getAvailablePayGroupForMonth(),
                val -> txtGroupAnnual.setText(val)
        );
    }

    /**
     * يربط حقل نصي بـ SearchDialog ذكي مع دعم قوائم ديناميكية.
     * القائمة تُجلب في لحظة الاستخدام (lazy) عبر Supplier.
     */
    private void setupSmartSearch(
            TextField textField,
            Supplier<List<String>> dataSupplier,
            Consumer<String> onSelect) {

        Runnable openSearch = () -> {
            List<String> dataList = dataSupplier.get();
            if (dataList == null || dataList.isEmpty()) {
                SAFNotification.warning("لا توجد بيانات متاحة للشهر المختار");
                return;
            }
            SearchDialog.forStrings()
                    .title("اختر")
                    .data(dataList)
                    .show()
                    .ifPresent(val -> {
                        textField.setText(val);
                        if (onSelect != null) onSelect.accept(val);
                    });
        };

        // ── Enter ──
        textField.setOnAction(_ -> {
            if (textField.getText() == null || textField.getText().isBlank()) {
                openSearch.run();
                return;
            }
            handleInput(textField, dataSupplier, onSelect, openSearch);
        });

        // ── Focus Lost ──
        textField.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {  // double click
                String raw = textField.getText();
                if (raw == null || raw.isBlank()) {
                    openSearch.run();          // فاضي + دبل كليك → بحث
                    return;
                }
                handleInput(textField, dataSupplier, onSelect, openSearch);
            }
        });
    }

    private void handleInput(
            TextField textField,
            Supplier<List<String>> dataSupplier,
            Consumer<String> onSelect,
            Runnable openSearch) {

        List<String> dataList = dataSupplier.get();  // ← تجلب هنا في اللحظة
        if (dataList == null || dataList.isEmpty()) {
            openSearch.run();
            return;
        }

        String input = DateUtils.normalizeArabicText(textField.getText());
        List<String> matches = dataList.stream()
                .filter(s -> DateUtils.normalizeArabicText(s).contains(input))
                .toList();

        if (matches.size() == 1) {
            String val = matches.get(0);
            textField.setText(val);
            if (onSelect != null) onSelect.accept(val);
        } else {
            openSearch.run();
        }
    }

    private class GroupDescription {
        private String groupName;
        private String groupDisc;
    }
}
