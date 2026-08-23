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
    @FXML
    TextField txtMonthReview;
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
    private Button btnDeleteMonthReview;
    @FXML
    private TextField txtMonthForGroupReview, txtGroupReview;

    @FXML
    private Button btnDeleteGroupReview;
    @FXML
    private TextField txtMonthForEmpReview, txtEmpIdReview, txtEmpNameReview, txtEmpCodeReview;

    @FXML
    private Button btnDeleteEmpMonthReview;

    @FXML
    private TextField txtMonthForPaymentReview, txtEmpIdPaymentReview, txtPaymentNameReview, txtEmpNamePaymentReview, txtEmpCodePaymentReview;

    @FXML
    private Button btnDeletePaymentReview;

    // Key Update


    @FXML
    private TextField txtKeyMonthReview;
    @FXML
    private Button btnUpdateKeysReview, btnUpdateAllKeysReview;

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
        setTxtMonthSearchYearly();
        setTxtMonthSearchReview();
        setButtonsActionsYearly();
        setButtonsActionsReview();
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

    void setButtonsActionsYearly() {
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

    void setTxtMonthSearchYearly() {
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

    void setTxtMonthSearchReview() {
        SmartSearchHelper.bind(txtMonthReview,
                () -> managerService.allMonthsReview,
                val -> txtMonthReview.setText(
                        DateUtils.toArabicMonthYear(DateUtils.getFirstDayOfMonth(val))
                )
        );
        SmartSearchHelper.bind(txtMonthForGroupReview,
                () -> managerService.allMonthsReview,
                val -> txtMonthForGroupReview.setText(
                        DateUtils.toArabicMonthYear(DateUtils.getFirstDayOfMonth(val))
                )
        );
        SmartSearchHelper.bind(txtGroupReview,
                () -> managerService.getAllKeysForMonthReview(txtMonthForGroupReview.getText()),
                val -> txtGroupReview.setText(
                        val
                )
        );

        SmartSearchHelper.bind(
                txtEmpIdReview,
                () -> managerService.getEmployeeInReview(txtEmpIdReview.getText()),
                SearchDialog.builder(SearchEmp.class)
                        .title("بحث عن موظف")
                        .column("رقم قومى", SearchEmp::getNational_id)
                        .column("رقم موظف", SearchEmp::getPay_id)
                        .column("الاسم", SearchEmp::getEmp_name),
                emp -> {
                },
                SmartSearchHelper.FieldBind.of(txtEmpIdReview, SearchEmp::getNational_id),
                SmartSearchHelper.FieldBind.of(txtEmpNameReview, SearchEmp::getEmp_name),
                SmartSearchHelper.FieldBind.of(txtEmpCodeReview, SearchEmp::getPay_id)
        );
        SmartSearchHelper.bind(txtMonthForEmpReview,
                () -> managerService.getEmployeeMonthsReview(txtEmpIdReview.getText()),
                val -> txtMonthForEmpReview.setText(
                        DateUtils.toArabicMonthYear(DateUtils.getFirstDayOfMonth(val))
                )
        );
        SmartSearchHelper.bind(
                txtEmpIdPaymentReview,
                () -> managerService.getEmployeeInReview(txtEmpIdPaymentReview.getText()),
                SearchDialog.builder(SearchEmp.class)
                        .title("بحث عن موظف")
                        .column("رقم قومى", SearchEmp::getNational_id)
                        .column("رقم موظف", SearchEmp::getPay_id)
                        .column("الاسم", SearchEmp::getEmp_name),
                emp -> {
                },
                SmartSearchHelper.FieldBind.of(txtEmpIdPaymentReview, SearchEmp::getNational_id),
                SmartSearchHelper.FieldBind.of(txtEmpNamePaymentReview, SearchEmp::getEmp_name),
                SmartSearchHelper.FieldBind.of(txtEmpCodePaymentReview, SearchEmp::getPay_id)
        );
        SmartSearchHelper.bind(txtMonthForPaymentReview,
                () -> managerService.getEmployeeMonthsReview(txtEmpIdPaymentReview.getText()),
                val -> txtMonthForPaymentReview.setText(
                        DateUtils.toArabicMonthYear(DateUtils.getFirstDayOfMonth(val))
                )
        );
        SmartSearchHelper.bind(txtPaymentNameReview,
                () -> managerService.getEmployeeMonthKeys(txtEmpIdPaymentReview.getText(), txtMonthForPaymentReview.getText()),
                val -> txtPaymentNameReview.setText(
                        val
                )
        );

        SmartSearchHelper.bind(txtKeyMonthReview,
                () -> managerService.allMonthsReview,
                val -> txtKeyMonthReview.setText(
                        DateUtils.toArabicMonthYear(DateUtils.getFirstDayOfMonth(val))
                )
        );
    }

    void setButtonsActionsReview() {
        btnDeleteMonthReview.setOnAction(_ -> managerService.deleteFullMonthReview(txtMonthReview.getText()));
        btnDeleteGroupReview.setOnAction(_ -> managerService.deletePayGroupReview(txtMonthForGroupReview.getText(), txtGroupReview.getText()));
        btnDeleteEmpMonthReview.setOnAction(_ -> managerService.deleteEployeeMonthReviewُ(txtEmpIdReview.getText(), txtMonthForEmpReview.getText()));
        btnDeletePaymentReview.setOnAction(_ -> managerService.deleteEployeeMonthReviewُ(txtEmpIdPaymentReview.getText(), txtMonthForPaymentReview.getText(), txtPaymentNameReview.getText()));

        btnUpdateAllKeysReview.setOnAction(_ -> managerService.updateKeysReviewAllReport());
        btnUpdateKeysReview.setOnAction(_ -> managerService.updateKeysReviewMonth(txtKeyMonthReview.getText()));
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