package com.safwat.hr.controller.hr.leave.admin;

import com.safwat.hr.controller.hr.leave.admin.dto.LeaveAdminDtos.*;
import com.safwat.hr.controller.hr.leave.admin.service.LeaveAdminApiClient;
import com.safwat.hr.controller.hr.leave.dto.LeaveTypeDto;
import com.safwat.hr.controller.hr.leave.util.LeaveLabels;
import com.safwat.hr.ui.UiAsync;
import com.safwat.hr.ui.controls.SAFNotification;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Window;

import java.net.URL;
import java.time.LocalDate;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LeaveAdminController implements Initializable {

    private static final String POOL = "ANNUAL";

    // ═══════════ Top ═══════════
    @FXML
    private Button btnEmployeeData;
    @FXML
    private TabPane adminTabs;

    // ═══════════ الأنواع ═══════════
    @FXML
    private Button btnNewType, btnEditType, btnToggleType, btnTypeRules;
    @FXML
    private TableView<LeaveTypeDto> typesTable;
    @FXML
    private TableColumn<LeaveTypeDto, String> colTypeCode, colTypeName, colTypeCategory,
            colTypeGender, colTypeOverlap, colTypeActive;

    // ═══════════ القوانين ═══════════
    @FXML
    private Button btnNewLaw, btnEditLaw, btnToggleLaw;
    @FXML
    private TableView<LeaveLawDto> lawsTable;
    @FXML
    private TableColumn<LeaveLawDto, String> colLawCode, colLawName, colLawFrom,
            colLawYearStart, colLawActive, colLawRecords;

    // ═══════════ جداول الاستحقاق ═══════════
    @FXML
    private ComboBox<LeaveLawDto> scheduleLawCombo;
    @FXML
    private Button btnPublishSchedule, btnDeleteSchedule;
    @FXML
    private TableView<AccrualScheduleDto> schedulesTable;
    @FXML
    private TableColumn<AccrualScheduleDto, String> colSchFrom, colSchTo, colSchRules;
    @FXML
    private TableView<AccrualRuleDto> scheduleRulesTable;
    @FXML
    private TableColumn<AccrualRuleDto, String> colRulePriority, colRuleConditions, colRuleDays;

    // ═══════════ النائية ═══════════
    @FXML
    private Button btnAddRemote, btnDeleteRemote;
    @FXML
    private TableView<RemoteBonusDto> remoteTable;
    @FXML
    private TableColumn<RemoteBonusDto, String> colRemoteFrom, colRemoteDays;

    private final LeaveAdminApiClient api = new LeaveAdminApiClient();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTypes();
        setupLaws();
        setupSchedules();
        setupRemote();

        // أزرار الأنواع
        btnNewType.setOnAction(e -> newType());
        btnEditType.setOnAction(e -> editType());
        btnToggleType.setOnAction(e -> toggleType());
        btnTypeRules.setOnAction(e -> openRules());
        btnEditType.disableProperty().bind(typesTable.getSelectionModel().selectedItemProperty().isNull());
        btnToggleType.disableProperty().bind(typesTable.getSelectionModel().selectedItemProperty().isNull());
        btnTypeRules.disableProperty().bind(typesTable.getSelectionModel().selectedItemProperty().isNull());

        // أزرار القوانين
        btnNewLaw.setOnAction(e -> newLaw());
        btnEditLaw.setOnAction(e -> editLaw());
        btnToggleLaw.setOnAction(e -> toggleLaw());
        btnEditLaw.disableProperty().bind(lawsTable.getSelectionModel().selectedItemProperty().isNull());
        btnToggleLaw.disableProperty().bind(lawsTable.getSelectionModel().selectedItemProperty().isNull());

        // أزرار الجداول
        btnPublishSchedule.setOnAction(e -> publishSchedule());
        btnDeleteSchedule.setOnAction(e -> deleteSchedule());
        btnPublishSchedule.disableProperty().bind(scheduleLawCombo.valueProperty().isNull());
        btnDeleteSchedule.disableProperty().bind(schedulesTable.getSelectionModel().selectedItemProperty().isNull());
        scheduleLawCombo.valueProperty().addListener((obs, o, n) -> loadSchedules());
        schedulesTable.getSelectionModel().selectedItemProperty().addListener((obs, o, sel) ->
                scheduleRulesTable.setItems(sel == null
                        ? FXCollections.observableArrayList()
                        : FXCollections.observableArrayList(sel.getRules())));

        // أزرار النائية
        btnAddRemote.setOnAction(e -> addRemote());
        btnDeleteRemote.setOnAction(e -> deleteRemote());
        btnDeleteRemote.disableProperty().bind(remoteTable.getSelectionModel().selectedItemProperty().isNull());

        btnEmployeeData.setOnAction(e -> openEmployee());

        loadTypes();
        loadLaws();
        loadRemote();
    }

    // ═══════════════════════════════════════════════════════════
    //  Setup الأعمدة
    // ═══════════════════════════════════════════════════════════

    private void setupTypes() {
        bind(colTypeCode, LeaveTypeDto::getCode);
        bind(colTypeName, LeaveTypeDto::getNameAr);
        bind(colTypeCategory, t -> LeaveLabels.category(t.getCategory()));
        bind(colTypeGender, t -> "FEMALE_ONLY".equals(t.getGenderRestriction()) ? "إناث فقط" : "بدون قيد");
        bind(colTypeOverlap, t -> t.isOverlapAllowed() ? "نعم" : "لا");
        bind(colTypeActive, t -> t.isActive() ? "مفعّل" : "معطّل");
    }

    private void setupLaws() {
        bind(colLawCode, LeaveLawDto::getCode);
        bind(colLawName, LeaveLawDto::getNameAr);
        bind(colLawFrom, l -> LeaveAdminDialogs.str(l.getEffectiveFrom()));
        bind(colLawYearStart, l -> String.format("%02d/%02d", l.getYearStartMonth(), l.getYearStartDay()));
        bind(colLawActive, l -> l.isActive() ? "مفعّل" : "معطّل");
        bind(colLawRecords, l -> String.valueOf(l.getRecordsCount()));
    }

    private void setupSchedules() {
        bind(colSchFrom, s -> LeaveAdminDialogs.str(s.getEffectiveFrom()));
        bind(colSchTo, s -> s.getEffectiveTo() == null ? "مفتوح" : LeaveAdminDialogs.str(s.getEffectiveTo()));
        bind(colSchRules, s -> String.valueOf(s.getRules() == null ? 0 : s.getRules().size()));

        bind(colRulePriority, r -> String.valueOf(r.getPriority()));
        bind(colRuleConditions, r -> conditionsText(r.getConditions()));
        bind(colRuleDays, r -> String.valueOf(r.getDays()));
    }

    private void setupRemote() {
        bind(colRemoteFrom, r -> LeaveAdminDialogs.str(r.getEffectiveFrom()));
        bind(colRemoteDays, r -> r.getDays() + " يوم");
    }

    private static <S> void bind(TableColumn<S, String> col, Function<S, String> f) {
        col.setCellValueFactory(cd -> new SimpleStringProperty(f.apply(cd.getValue())));
    }

    static String conditionsText(Map<String, Object> c) {
        if (c == null || c.isEmpty()) return "عام (الكل)";
        return c.entrySet().stream()
                .map(e -> switch (e.getKey()) {
                    case "disabled" -> "معاق";
                    case "ageMin" -> "سن ≥ " + e.getValue();
                    case "serviceMonthsMin" -> "خدمة ≥ " + e.getValue() + " شهر";
                    case "gender" -> "نوع: " + e.getValue();
                    default -> e.getKey() + "=" + e.getValue();
                })
                .collect(Collectors.joining(" + "));
    }

    // ═══════════════════════════════════════════════════════════
    //  التحميل
    // ═══════════════════════════════════════════════════════════

    private void loadTypes() {
        UiAsync.run(api::listTypes,
                list -> typesTable.setItems(FXCollections.observableArrayList(list)));
    }

    private void loadLaws() {
        UiAsync.run(api::listLaws, list -> {
            lawsTable.setItems(FXCollections.observableArrayList(list));
            scheduleLawCombo.setItems(FXCollections.observableArrayList(list));
            if (scheduleLawCombo.getValue() == null && !list.isEmpty()) {
                scheduleLawCombo.getSelectionModel().selectFirst();
            }
        });
    }

    private void loadSchedules() {
        LeaveLawDto law = scheduleLawCombo.getValue();
        if (law == null) {
            schedulesTable.getItems().clear();
            scheduleRulesTable.getItems().clear();
            return;
        }
        String code = law.getCode();
        UiAsync.run(() -> api.listSchedules(code, POOL), list -> {
            schedulesTable.setItems(FXCollections.observableArrayList(list));
            scheduleRulesTable.getItems().clear();
        });
    }

    private void loadRemote() {
        UiAsync.run(api::listRemote,
                list -> remoteTable.setItems(FXCollections.observableArrayList(list)));
    }

    // ═══════════════════════════════════════════════════════════
    //  الأنواع
    // ═══════════════════════════════════════════════════════════

    private void newType() {
        LeaveAdminDialogs.typeForm(owner(), null).ifPresent(req ->
                UiAsync.run(() -> api.createType(req), saved -> {
                    SAFNotification.success("تمت إضافة النوع");
                    loadTypes();
                }));
    }

    private void editType() {
        LeaveTypeDto sel = typesTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        LeaveAdminDialogs.typeForm(owner(), sel).ifPresent(req ->
                UiAsync.run(() -> api.updateType(sel.getId(), req), saved -> {
                    SAFNotification.success("تم حفظ التعديل");
                    loadTypes();
                }));
    }

    private void toggleType() {
        LeaveTypeDto sel = typesTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        boolean next = !sel.isActive();
        UiAsync.run(() -> api.setTypeActive(sel.getId(), next), saved -> {
            SAFNotification.success(next ? "تم التفعيل" : "تم التعطيل");
            loadTypes();
        });
    }

    private void openRules() {
        LeaveTypeDto sel = typesTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        LeaveAdminDialogs.typeRules(owner(), sel, lawsTable.getItems(), api);
    }

    // ═══════════════════════════════════════════════════════════
    //  القوانين
    // ═══════════════════════════════════════════════════════════

    private void newLaw() {
        LeaveAdminDialogs.lawForm(owner(), null).ifPresent(req ->
                UiAsync.run(() -> api.createLaw(req), saved -> {
                    SAFNotification.success("تمت إضافة القانون");
                    loadLaws();
                }));
    }

    private void editLaw() {
        LeaveLawDto sel = lawsTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        LeaveAdminDialogs.lawForm(owner(), sel).ifPresent(req ->
                UiAsync.run(() -> api.updateLaw(sel.getCode(), req), saved -> {
                    SAFNotification.success("تم حفظ التعديل");
                    loadLaws();
                }));
    }

    private void toggleLaw() {
        LeaveLawDto sel = lawsTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        boolean next = !sel.isActive();
        UiAsync.run(() -> api.setLawActive(sel.getCode(), next), saved -> {
            SAFNotification.success(next ? "تم التفعيل" : "تم التعطيل");
            loadLaws();
        });
    }

    // ═══════════════════════════════════════════════════════════
    //  جداول الاستحقاق
    // ═══════════════════════════════════════════════════════════

    private void publishSchedule() {
        LeaveLawDto law = scheduleLawCombo.getValue();
        if (law == null) return;
        LeaveAdminDialogs.scheduleForm(owner(), law).ifPresent(req ->
                UiAsync.run(() -> api.publishSchedule(law.getCode(), req), saved -> {
                    SAFNotification.success("تم نشر الجدول");
                    loadSchedules();
                }));
    }

    private void deleteSchedule() {
        LeaveLawDto law = scheduleLawCombo.getValue();
        AccrualScheduleDto sel = schedulesTable.getSelectionModel().getSelectedItem();
        if (law == null || sel == null) return;

        LocalDate from = sel.getEffectiveFrom();
        if (!LeaveAdminDialogs.confirm("حذف أحدث جدول بتاريخ " + from + "؟\n"
                + "الحسابات بتتغير لكل الموظفين اللي على القانون ده، ولازم يتعاد حساب الأرصدة.")) {
            return;
        }
        UiAsync.run(() -> {
                    api.deleteSchedule(law.getCode(), POOL, from);
                    return null;
                },
                ok -> {
                    SAFNotification.success("تم حذف الجدول");
                    loadSchedules();
                });
    }

    // ═══════════════════════════════════════════════════════════
    //  النائية
    // ═══════════════════════════════════════════════════════════

    private void addRemote() {
        LeaveAdminDialogs.remoteForm(owner()).ifPresent(req ->
                UiAsync.run(() -> api.createRemote(req), saved -> {
                    SAFNotification.success("تمت إضافة القيمة");
                    loadRemote();
                }));
    }

    private void deleteRemote() {
        RemoteBonusDto sel = remoteTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        if (!LeaveAdminDialogs.confirm("حذف آخر تغيير للنائية؟")) return;
        Long id = sel.getId();
        UiAsync.run(() -> {
                    api.deleteRemote(id);
                    return null;
                },
                ok -> {
                    SAFNotification.success("تم الحذف");
                    loadRemote();
                });
    }

    // ═══════════════════════════════════════════════════════════
    //  بيانات الموظف
    // ═══════════════════════════════════════════════════════════

    private void openEmployee() {
        TextInputDialog in = new TextInputDialog();
        in.initOwner(owner());
        in.setTitle("بيانات الإجازات");
        in.setHeaderText(null);
        in.setContentText("الرقم القومي للموظف:");
        in.showAndWait()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .ifPresent(nid -> LeaveAdminDialogs.employeeData(owner(), nid, lawsTable.getItems(), api));
    }

    // ═══════════════════════════════════════════════════════════
    //  Helpers
    // ═══════════════════════════════════════════════════════════

    private Window owner() {
        return adminTabs.getScene().getWindow();
    }
}