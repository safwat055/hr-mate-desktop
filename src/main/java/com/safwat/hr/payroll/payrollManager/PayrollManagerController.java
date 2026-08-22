package com.safwat.hr.payroll.payrollManager;

import com.safwat.hr.payroll.dto.SearchEmp;
import com.safwat.hr.shared.SmartSearchHelper;
import com.safwat.hr.shared.ui.SearchDialog;
import com.safwat.hr.shared.util.DateUtils;
import com.safwat.hr.ui.controls.SAFNotification;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.DefaultStringConverter;
import lombok.Getter;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

@Getter
public class PayrollManagerController implements Initializable {

    private final ObservableList<GroupDescription> groupDescriptionList = FXCollections.observableArrayList();
    private PayrollManagerService managerService;

    // ── Header ──
    @FXML
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
    private Button btnDeleteMonthAnnual;
    @FXML
    private TextField txtGroupAnnual;
    @FXML
    private Button btnDeleteGroupAnnual;
    @FXML
    private TextField txtEmpIdAnnual;
    @FXML
    private TextField txtEmpNameAnnual, txtEmpCodeAnnual;
    @FXML
    private Button btnDeleteEmpMonthAnnual;
    @FXML
    private TextField txtEmpIdPaymentAnnual, txtMonthForPaymentAnnual, txtPaymentNameAnnual, txtMonthForEmpAnnual;
    @FXML
    private TextField txtEmpNameAnnual2, txtEmpCodeAnnual2;
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
    private TextField txtDescMonthAnnual;
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

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        managerService = new PayrollManagerService(this);

        setupTableColumns();
        fillMainLists();
        setTxtMonthSearch();
        setButtonsActions();
    }

    /**
     * ── ربط أعمدة الجدول + تفعيل التعديل على عمود الوصف ──
     */
    private void setupTableColumns() {
        // عمود اسم المجموعة — للعرض فقط
        colGroupName.setCellValueFactory(cell ->
                cell.getValue().payGroupProperty());
        colGroupName.setEditable(false);

        // عمود الوصف — قابل للتعديل
        colGroupDesc.setCellValueFactory(cell ->
                cell.getValue().descriptionProperty());
        colGroupDesc.setCellFactory(TextFieldTableCell.forTableColumn(new DefaultStringConverter()));
        colGroupDesc.setOnEditCommit(event -> {
            GroupDescription row = event.getRowValue();
            row.setDescription(event.getNewValue());
        });

        tableGroupDescriptions.setEditable(true);
        tableGroupDescriptions.setItems(groupDescriptionList);
    }

    void fillMainLists() {
        managerService.setAllMonthsList();
    }

    void setButtonsActions() {
        btnDeleteMonthAnnual.setOnAction(_ -> managerService.deleteOneMonthYearly());
        btnDeleteGroupAnnual.setOnAction(_ -> managerService.deleteTargetPayGroup());
        btnDeleteEmpMonthAnnual.setOnAction(_ -> managerService.deleteEmployeeMonth());
        btnDeletePaymentAnnual.setOnAction(_ -> managerService.deletePayGroupInTargetMonthAndEmployee(
                txtEmpIdPaymentAnnual.getText(), txtMonthForPaymentAnnual.getText(), txtPaymentNameAnnual.getText()
        ));
        btnUpdatePaymentName.setOnAction(_ -> managerService.updatePayGroupName(
                txtOldPaymentName.getText(), txtNewPaymentName.getText()));
        btnLoadGroupsForDesc.setOnAction(_ -> updateDescriptionList());

        // ── حفظ الأوصاف المعدلة ──
        btnSaveDescriptions.setOnAction(_ -> saveDescriptions());
    }

    /**
     * ── تحميل الأوصاف من الـ Backend وملء الجدول ──
     */
    void updateDescriptionList() {
        groupDescriptionList.clear();
        List<GroupDescription> fetched = managerService.getDescriptions(txtDescMonthAnnual.getText());
        groupDescriptionList.addAll(fetched);
    }

    /**
     * ── حفظ الأوصاف المعدلة ──
     */
    private void saveDescriptions() {
        String month = txtDescMonthAnnual.getText();
        if (month == null || month.isBlank()) {
            // SAFNotification.warning("اختر الشهر أولاً");
            return;
        }

        List<GroupDescription> modified = new ArrayList<>(groupDescriptionList);
        boolean success = managerService.saveDescriptions(month, modified);

        if (success) {
            //SAFNotification.success("تم حفظ الأوصاف بنجاح");
        } else {
            SAFNotification.error("فشل حفظ الأوصاف");
        }
    }

    void setTxtMonthSearch() {
        // ── String simple binds ──
        SmartSearchHelper.bind(txtAllMonthsYearly, () -> managerService.allMonthsYearly,
                val -> txtAllMonthsYearly.setText(
                        DateUtils.toArabicMonthYear(DateUtils.getFirstDayOfMonth(val))
                ));

        SmartSearchHelper.bind(txtMonthGroupY, () -> managerService.allMonthsYearly,
                val -> txtMonthGroupY.setText(
                        DateUtils.toArabicMonthYear(DateUtils.getFirstDayOfMonth(val))
                ));

        SmartSearchHelper.bind(txtGroupAnnual, () -> managerService.getAvailablePayGroupForMonth(),
                val -> txtGroupAnnual.setText(val)
        );

        // ── Multi-field: Employee (Annual deletion) ──
        SmartSearchHelper.bind(
                txtEmpIdAnnual,
                () -> managerService.getEmployeeInYearly(),
                SearchDialog.builder(SearchEmp.class)
                        .title("بحث عن موظف")
                        .column("رقم قومى", SearchEmp::getNational_id)
                        .column("رقم موظف", SearchEmp::getPay_id)
                        .column("الاسم", SearchEmp::getEmp_name),
                emp -> {
                },
                SmartSearchHelper.FieldBind.of(txtEmpIdAnnual, SearchEmp::getNational_id),
                SmartSearchHelper.FieldBind.of(txtEmpNameAnnual, SearchEmp::getEmp_name),
                SmartSearchHelper.FieldBind.of(txtEmpCodeAnnual, SearchEmp::getPay_id)
        );

        // ── Multi-field: Employee (Payment deletion) ──
        SmartSearchHelper.bind(
                txtEmpIdPaymentAnnual,
                () -> managerService.getEmployeeInYearly(),
                SearchDialog.builder(SearchEmp.class)
                        .title("بحث عن موظف")
                        .column("رقم قومى", SearchEmp::getNational_id)
                        .column("رقم موظف", SearchEmp::getPay_id)
                        .column("الاسم", SearchEmp::getEmp_name),
                emp -> {
                },
                SmartSearchHelper.FieldBind.of(txtEmpIdPaymentAnnual, SearchEmp::getNational_id),
                SmartSearchHelper.FieldBind.of(txtEmpNameAnnual2, SearchEmp::getEmp_name),
                SmartSearchHelper.FieldBind.of(txtEmpCodeAnnual2, SearchEmp::getPay_id)
        );

        // ── Month binds ──
        SmartSearchHelper.bind(txtMonthForEmpAnnual,
                () -> managerService.getEmployeeMonths(txtEmpIdAnnual.getText()),
                val -> txtMonthForEmpAnnual.setText(
                        DateUtils.toArabicMonthYear(DateUtils.getFirstDayOfMonth(val))
                )
        );

        SmartSearchHelper.bind(txtMonthForPaymentAnnual,
                () -> managerService.getEmployeeMonths(txtEmpIdPaymentAnnual.getText()),
                val -> txtMonthForPaymentAnnual.setText(
                        DateUtils.toArabicMonthYear(DateUtils.getFirstDayOfMonth(val))
                )
        );

        SmartSearchHelper.bind(txtPaymentNameAnnual,
                () -> managerService.getPayGroupForEmployeeInMonth(
                        txtEmpIdPaymentAnnual.getText(), txtMonthForPaymentAnnual.getText()),
                val -> txtPaymentNameAnnual.setText(val)
        );

        SmartSearchHelper.bind(txtOldPaymentName,
                () -> managerService.getPayGroup(),
                val -> txtOldPaymentName.setText(val)
        );

        SmartSearchHelper.bind(txtDescMonthAnnual,
                () -> managerService.allMonthsYearly,
                val -> txtDescMonthAnnual.setText(
                        DateUtils.toArabicMonthYear(DateUtils.getFirstDayOfMonth(val))
                )
        );
    }

    // ═══════════════════════════════════════════════════════════
    //  DTO — Class مع Property (عشان التعديل في الجدول)
    // ═══════════════════════════════════════════════════════════
    public static class GroupDescription {
        private final StringProperty payGroup = new SimpleStringProperty();
        private final StringProperty description = new SimpleStringProperty();

        public GroupDescription() {
        }

        public GroupDescription(String payGroup, String description) {
            this.payGroup.set(payGroup);
            this.description.set(description);
        }

        public String getPayGroup() {
            return payGroup.get();
        }

        public void setPayGroup(String value) {
            payGroup.set(value);
        }

        public StringProperty payGroupProperty() {
            return payGroup;
        }

        public String getDescription() {
            return description.get();
        }

        public void setDescription(String value) {
            description.set(value);
        }

        public StringProperty descriptionProperty() {
            return description;
        }
    }
}