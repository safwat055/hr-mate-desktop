package com.safwat.hr.controller.payroll.payrollManager;

import com.safwat.hr.controller.payroll.payrollApi.dto.EmployeeSearchResult;
import com.safwat.hr.shared.ui.DangerConfirmDialog;
import com.safwat.hr.shared.ui.SearchDialog;
import com.safwat.hr.shared.ui.SmartSearchHelper;
import com.safwat.hr.shared.util.DateUtils;
import com.safwat.hr.ui.UiAsync;
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
    private TextField txtMonthSub;
    @FXML
    private Button btnDeleteMonthSub;
    @FXML
    private TextField txtMonthForEmpSub, txtEmpIdSub, txtEmpNameSub, txtEmpCodeSub;
    @FXML
    private Button btnDeleteEmpSub;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        managerService = new PayrollManagerService(this);

        setupTableColumns();
        fillMainLists();
        setTxtMonthSearchYearly();
        setTxtMonthSearchReview();
        setTxtMonthSearchSub();
        setButtonsActionsYearly();
        setButtonsActionsReview();
        setButtonsActionsSub();
    }

    private void setupTableColumns() {
        colGroupName.setCellValueFactory(cell -> cell.getValue().payGroupProperty());
        colGroupName.setEditable(false);

        colGroupDesc.setCellValueFactory(cell -> cell.getValue().descriptionProperty());
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

    // ═══════════════════════════════════════════════════════════
    //  Buttons — Annual Report
    // ═══════════════════════════════════════════════════════════

    void setButtonsActionsYearly() {

        // ── حذف شهر كامل ──
        btnDeleteMonthAnnual.setOnAction(_ -> {
            String monthText = txtAllMonthsYearly.getText();
            boolean ok = DangerConfirmDialog.show(
                    "تأكيد الحذف",
                    "سيتم حذف بيانات الشهر كاملة من تقرير الصرفيات السنوى",
                    "حذف شهر " + monthText);
            if (!ok) {
                SAFNotification.info("تم إلغاء عملية الحذف");
                return;
            }
            UiAsync.runVoid(() -> managerService.deleteOneMonthYearly());
        });

        // ── حذف مجموعة كاملة ──
        btnDeleteGroupAnnual.setOnAction(_ -> {
            boolean ok = DangerConfirmDialog.show(
                    "تأكيد الحذف",
                    "سيتم حذف بيانات المجموعة كاملة من تقرير الصرفيات السنوى " + txtGroupAnnual.getText(),
                    "حذف شهر " + txtMonthGroupY.getText());
            if (!ok) {
                SAFNotification.info("تم إلغاء عملية الحذف");
                return;
            }
            UiAsync.runVoid(() -> managerService.deleteTargetPayGroup());
        });

        // ── حذف شهر موظف ──
        btnDeleteEmpMonthAnnual.setOnAction(_ -> {
            boolean ok = DangerConfirmDialog.show(
                    "تاكيد الحذف",
                    "سيتم حذف شهر " + txtMonthForEmpAnnual.getText() + " للموظف " + txtEmpNameAnnual.getText(),
                    "");
            if (!ok) {
                SAFNotification.info("تم إلغاء عملية الحذف");
                return;
            }
            UiAsync.runVoid(() -> managerService.deleteEmployeeMonth());
        });

        // ── حذف مجموعة من موظف ──
        btnDeletePaymentAnnual.setOnAction(_ -> {
            boolean ok = DangerConfirmDialog.show(
                    "تاكيد الحذف",
                    "سيتم حذف شهر " + txtMonthForPaymentAnnual.getText() + " للموظف " + txtEmpNameAnnual2.getText(),
                    "");
            if (!ok) {
                SAFNotification.info("تم إلغاء عملية الحذف");
                return;
            }
            UiAsync.runVoid(() -> managerService.deletePayGroupInTargetMonthAndEmployee(
                    txtEmpIdPaymentAnnual.getText(),
                    txtMonthForPaymentAnnual.getText(),
                    txtPaymentNameAnnual.getText()));
        });

        // ── تحديث اسم المجموعة ──
        btnUpdatePaymentName.setOnAction(_ ->
                UiAsync.runVoid(() -> managerService.updatePayGroupName(
                        txtOldPaymentName.getText(),
                        txtNewPaymentName.getText())));

        // ── تحميل الأوصاف ──
        btnLoadGroupsForDesc.setOnAction(_ -> loadDescriptionsAsync());

        // ── حفظ الأوصاف ──
        btnSaveDescriptions.setOnAction(_ -> saveDescriptionsAsync());
    }

    /** تحميل الأوصاف من الـ Backend — async. */
    private void loadDescriptionsAsync() {
        String month = txtDescMonthAnnual.getText();
        if (month == null || month.isBlank()) {
            SAFNotification.warning("اختر الشهر أولاً");
            return;
        }
        groupDescriptionList.clear();
        UiAsync.run(
                () -> managerService.getDescriptions(month),
                fetched -> {
                    if (fetched != null) groupDescriptionList.addAll(fetched);
                }
        );
    }

    /** حفظ الأوصاف المعدلة — async. */
    /** حفظ الأوصاف المعدلة — async. */
    private void saveDescriptionsAsync() {
        String month = txtDescMonthAnnual.getText();
        if (month == null || month.isBlank()) {
            SAFNotification.warning("اختر الشهر أولاً");
            return;
        }
        List<GroupDescription> modified = new ArrayList<>(groupDescriptionList);
        UiAsync.runVoid(() -> managerService.saveDescriptions(month, modified));
    }

    // ═══════════════════════════════════════════════════════════
    //  SmartSearch bindings — Annual
    // ═══════════════════════════════════════════════════════════

    void setTxtMonthSearchYearly() {
        SmartSearchHelper.bind(txtAllMonthsYearly, () -> managerService.getAllMonthsYearly(),
                val -> txtAllMonthsYearly.setText(
                        DateUtils.toArabicMonthYear(DateUtils.getFirstDayOfMonth(val))));

        SmartSearchHelper.bind(txtMonthGroupY, () -> managerService.getAllMonthsYearly(),
                val -> txtMonthGroupY.setText(
                        DateUtils.toArabicMonthYear(DateUtils.getFirstDayOfMonth(val))));

        SmartSearchHelper.bind(txtGroupAnnual, () -> managerService.getAvailablePayGroupForMonth(),
                txtGroupAnnual::setText);

        SmartSearchHelper.bind(
                txtEmpIdAnnual,
                () -> managerService.getEmployeeInYearly(),
                SearchDialog.builder(EmployeeSearchResult.class)
                        .title("بحث عن موظف")
                        .column("رقم قومى", EmployeeSearchResult::getNational_id)
                        .column("رقم موظف", EmployeeSearchResult::getPay_id)
                        .column("الوظيفة", EmployeeSearchResult::getJob)
                        .column("الادارة", EmployeeSearchResult::getPay_management)
                        .column("فئة التعيين", EmployeeSearchResult::getAssignment_class),
                _ -> {
                },
                SmartSearchHelper.FieldBind.of(txtEmpIdAnnual, EmployeeSearchResult::getNational_id),
                SmartSearchHelper.FieldBind.of(txtEmpNameAnnual, EmployeeSearchResult::getEmp_name),
                SmartSearchHelper.FieldBind.of(txtEmpCodeAnnual, EmployeeSearchResult::getPay_id)
        );

        SmartSearchHelper.bind(
                txtEmpIdPaymentAnnual,
                () -> managerService.getEmployeeInYearly(),
                SearchDialog.builder(EmployeeSearchResult.class)
                        .title("بحث عن موظف")
                        .column("رقم قومى", EmployeeSearchResult::getNational_id)
                        .column("رقم موظف", EmployeeSearchResult::getPay_id)
                        .column("الاسم", EmployeeSearchResult::getEmp_name)
                        .column("الوظيفة", EmployeeSearchResult::getJob)
                        .column("الادارة", EmployeeSearchResult::getPay_management)
                        .column("فئة التعيين", EmployeeSearchResult::getAssignment_class),
                _ -> {
                },
                SmartSearchHelper.FieldBind.of(txtEmpIdPaymentAnnual, EmployeeSearchResult::getNational_id),
                SmartSearchHelper.FieldBind.of(txtEmpNameAnnual2, EmployeeSearchResult::getEmp_name),
                SmartSearchHelper.FieldBind.of(txtEmpCodeAnnual2, EmployeeSearchResult::getPay_id)
        );

        SmartSearchHelper.bind(txtMonthForEmpAnnual,
                () -> managerService.getEmployeeMonths(txtEmpIdAnnual.getText()),
                val -> txtMonthForEmpAnnual.setText(
                        DateUtils.toArabicMonthYear(DateUtils.getFirstDayOfMonth(val))));

        SmartSearchHelper.bind(txtMonthForPaymentAnnual,
                () -> managerService.getEmployeeMonths(txtEmpIdPaymentAnnual.getText()),
                val -> txtMonthForPaymentAnnual.setText(
                        DateUtils.toArabicMonthYear(DateUtils.getFirstDayOfMonth(val))));

        SmartSearchHelper.bind(txtPaymentNameAnnual,
                () -> managerService.getPayGroupForEmployeeInMonth(
                        txtEmpIdPaymentAnnual.getText(), txtMonthForPaymentAnnual.getText()),
                txtPaymentNameAnnual::setText);

        SmartSearchHelper.bind(txtOldPaymentName,
                () -> managerService.getPayGroup(),
                txtOldPaymentName::setText);

        SmartSearchHelper.bind(txtDescMonthAnnual,
                () -> managerService.getAllMonthsYearly(),
                val -> txtDescMonthAnnual.setText(
                        DateUtils.toArabicMonthYear(DateUtils.getFirstDayOfMonth(val))));
    }

    // ═══════════════════════════════════════════════════════════
    //  SmartSearch bindings — Review
    // ═══════════════════════════════════════════════════════════

    void setTxtMonthSearchReview() {
        SmartSearchHelper.bind(txtMonthReview,
                () -> managerService.getAllMonthsReview(),
                val -> txtMonthReview.setText(
                        DateUtils.toArabicMonthYear(DateUtils.getFirstDayOfMonth(val))));

        SmartSearchHelper.bind(txtMonthForGroupReview,
                () -> managerService.getAllMonthsReview(),
                val -> txtMonthForGroupReview.setText(
                        DateUtils.toArabicMonthYear(DateUtils.getFirstDayOfMonth(val))));

        SmartSearchHelper.bind(txtGroupReview,
                () -> managerService.getAllKeysForMonthReview(txtMonthForGroupReview.getText()),
                txtGroupReview::setText);

        SmartSearchHelper.bind(
                txtEmpIdReview,
                () -> managerService.getEmployeeInReview(txtEmpIdReview.getText()),
                SearchDialog.builder(EmployeeSearchResult.class)
                        .title("بحث عن موظف")
                        .column("رقم قومى", EmployeeSearchResult::getNational_id)
                        .column("رقم موظف", EmployeeSearchResult::getPay_id)
                        .column("الاسم", EmployeeSearchResult::getEmp_name)
                        .column("الوظيفة", EmployeeSearchResult::getJob)
                        .column("الادارة", EmployeeSearchResult::getPay_management)
                        .column("فئة التعيين", EmployeeSearchResult::getAssignment_class),
                _ -> {
                },
                SmartSearchHelper.FieldBind.of(txtEmpIdReview, EmployeeSearchResult::getNational_id),
                SmartSearchHelper.FieldBind.of(txtEmpNameReview, EmployeeSearchResult::getEmp_name),
                SmartSearchHelper.FieldBind.of(txtEmpCodeReview, EmployeeSearchResult::getPay_id)
        );

        SmartSearchHelper.bind(txtMonthForEmpReview,
                () -> managerService.getEmployeeMonthsReview(txtEmpIdReview.getText()),
                val -> txtMonthForEmpReview.setText(
                        DateUtils.toArabicMonthYear(DateUtils.getFirstDayOfMonth(val))));

        SmartSearchHelper.bind(
                txtEmpIdPaymentReview,
                () -> managerService.getEmployeeInReview(txtEmpIdPaymentReview.getText()),
                SearchDialog.builder(EmployeeSearchResult.class)
                        .title("بحث عن موظف")
                        .column("رقم قومى", EmployeeSearchResult::getNational_id)
                        .column("رقم موظف", EmployeeSearchResult::getPay_id)
                        .column("الاسم", EmployeeSearchResult::getEmp_name)
                        .column("الوظيفة", EmployeeSearchResult::getJob)
                        .column("الادارة", EmployeeSearchResult::getPay_management)
                        .column("فئة التعيين", EmployeeSearchResult::getAssignment_class),
                _ -> {
                },
                SmartSearchHelper.FieldBind.of(txtEmpIdPaymentReview, EmployeeSearchResult::getNational_id),
                SmartSearchHelper.FieldBind.of(txtEmpNamePaymentReview, EmployeeSearchResult::getEmp_name),
                SmartSearchHelper.FieldBind.of(txtEmpCodePaymentReview, EmployeeSearchResult::getPay_id)
        );

        SmartSearchHelper.bind(txtMonthForPaymentReview,
                () -> managerService.getEmployeeMonthsReview(txtEmpIdPaymentReview.getText()),
                val -> txtMonthForPaymentReview.setText(
                        DateUtils.toArabicMonthYear(DateUtils.getFirstDayOfMonth(val))));

        SmartSearchHelper.bind(txtPaymentNameReview,
                () -> managerService.getEmployeeMonthKeys(txtEmpIdPaymentReview.getText(), txtMonthForPaymentReview.getText()),
                txtPaymentNameReview::setText);

        SmartSearchHelper.bind(txtKeyMonthReview,
                () -> managerService.getAllMonthsReview(),
                val -> txtKeyMonthReview.setText(
                        DateUtils.toArabicMonthYear(DateUtils.getFirstDayOfMonth(val))));
    }

    // ═══════════════════════════════════════════════════════════
    //  Buttons — Review
    // ═══════════════════════════════════════════════════════════

    void setButtonsActionsReview() {

        btnDeleteMonthReview.setOnAction(_ -> {
            String monthText = txtMonthReview.getText();
            boolean ok = DangerConfirmDialog.show(
                    "تأكيد حذف",
                    "سيتم حذف الشهر بالكامل في حالة الاستمرار",
                    "شهر " + DateUtils.toArabicMonthYear(DateUtils.getFirstDayOfMonth(monthText)));
            if (!ok) {
                SAFNotification.info("تم إلغاء العملية");
                return;
            }
            UiAsync.runVoid(() -> managerService.deleteFullMonthReview(monthText));
        });

        btnDeleteGroupReview.setOnAction(_ -> {
            String monthText = txtMonthForGroupReview.getText();
            String payGroup = txtGroupReview.getText();
            boolean ok = DangerConfirmDialog.show(
                    "تأكيد حذف",
                    "سيتم حذف المجموعة بالكامل في حالة الاستمرار " + payGroup,
                    "شهر " + DateUtils.toArabicMonthYear(DateUtils.getFirstDayOfMonth(monthText)));
            if (!ok) {
                SAFNotification.info("تم إلغاء العملية");
                return;
            }
            UiAsync.runVoid(() -> managerService.deletePayGroupReview(monthText, payGroup));
        });

        btnDeleteEmpMonthReview.setOnAction(_ -> {
            String nationalId = txtEmpIdReview.getText();
            String monthText = txtMonthForEmpReview.getText();
            boolean ok = DangerConfirmDialog.show(
                    "تأكيد حذف",
                    "سيتم حذف سجل الموظف للشهر بالكامل في حالة الاستمرار " + nationalId,
                    "شهر " + DateUtils.toArabicMonthYear(DateUtils.getFirstDayOfMonth(monthText)));
            if (!ok) {
                SAFNotification.info("تم إلغاء العملية");
                return;
            }
            UiAsync.runVoid(() -> managerService.deleteEmployeeMonthReview(nationalId, monthText));
        });

        btnDeletePaymentReview.setOnAction(_ -> {
            String nationalId = txtEmpIdPaymentReview.getText();
            String monthText = txtMonthForPaymentReview.getText();
            String payGroup = txtPaymentNameReview.getText();
            boolean ok = DangerConfirmDialog.show(
                    "تأكيد حذف",
                    "سيتم حذف مجموعة من الموظف في حالة الاستمرار " + nationalId + "\n " + payGroup,
                    "شهر " + DateUtils.toArabicMonthYear(DateUtils.getFirstDayOfMonth(monthText)));
            if (!ok) {
                SAFNotification.info("تم إلغاء العملية");
                return;
            }
            UiAsync.runVoid(() -> managerService.deleteEmployeePayGroupReview(nationalId, monthText, payGroup));
        });

        // ReportExternalSubmitter async بالفعل — مش محتاج UiAsync
        btnUpdateAllKeysReview.setOnAction(_ -> managerService.updateKeysReviewAllReport());
        btnUpdateKeysReview.setOnAction(_ -> managerService.updateKeysReviewMonth(txtKeyMonthReview.getText()));
    }

    // ═══════════════════════════════════════════════════════════
    //  SmartSearch bindings — Subscription
    // ═══════════════════════════════════════════════════════════

    void setTxtMonthSearchSub() {
        SmartSearchHelper.bind(txtMonthSub,
                () -> managerService.getAllMonthsChangeCard(),
                val -> txtMonthSub.setText(
                        DateUtils.toArabicMonthYear(DateUtils.getFirstDayOfMonth(val))));

        SmartSearchHelper.bind(
                txtEmpIdSub,
                () -> managerService.getEmployeeInSub(txtEmpIdSub.getText()),
                SearchDialog.builder(EmployeeSearchResult.class)
                        .title("بحث عن موظف")
                        .column("رقم قومى", EmployeeSearchResult::getNational_id)
                        .column("رقم موظف", EmployeeSearchResult::getPay_id)
                        .column("الاسم", EmployeeSearchResult::getEmp_name)
                        .column("الوظيفة", EmployeeSearchResult::getJob)
                        .column("الادارة", EmployeeSearchResult::getPay_management)
                        .column("فئة التعيين", EmployeeSearchResult::getAssignment_class),
                _ -> {
                },
                SmartSearchHelper.FieldBind.of(txtEmpIdSub, EmployeeSearchResult::getNational_id),
                SmartSearchHelper.FieldBind.of(txtEmpNameSub, EmployeeSearchResult::getEmp_name),
                SmartSearchHelper.FieldBind.of(txtEmpCodeSub, EmployeeSearchResult::getPay_id)
        );

        SmartSearchHelper.bind(txtMonthForEmpSub,
                () -> managerService.getEmployeeMonthsSub(txtEmpIdSub.getText()),
                val -> txtMonthForEmpSub.setText(
                        DateUtils.toArabicMonthYear(DateUtils.getFirstDayOfMonth(val))));
    }

    // ═══════════════════════════════════════════════════════════
    //  Buttons — Subscription
    // ═══════════════════════════════════════════════════════════

    void setButtonsActionsSub() {
        btnDeleteMonthSub.setOnAction(_ -> {
            String monthText = txtMonthSub.getText();
            boolean ok = DangerConfirmDialog.show(
                    "تأكيد حذف",
                    "سيتم حذف الشهر بالكامل في حالة الاستمرار",
                    "شهر " + DateUtils.toArabicMonthYear(DateUtils.getFirstDayOfMonth(monthText)));
            if (!ok) {
                SAFNotification.info("تم إلغاء العملية");
                return;
            }
            UiAsync.runVoid(() -> managerService.deleteFullMonthSub(monthText));
        });

        btnDeleteEmpSub.setOnAction(_ -> {
            String nationalId = txtEmpIdSub.getText();
            String monthText = txtMonthForEmpSub.getText();
            boolean ok = DangerConfirmDialog.show(
                    "تأكيد حذف",
                    "سيتم حذف سجل الموظف للشهر بالكامل في حالة الاستمرار " + nationalId,
                    "شهر " + DateUtils.toArabicMonthYear(DateUtils.getFirstDayOfMonth(monthText)));
            if (!ok) {
                SAFNotification.info("تم إلغاء العملية");
                return;
            }
            UiAsync.runVoid(() -> managerService.deleteEmployeeMonthSub(nationalId, monthText));
        });
    }
    // ═══════════════════════════════════════════════════════════
    //  DTO
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