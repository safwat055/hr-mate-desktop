package com.safwat.hr.controller.leave;

import com.safwat.hr.controller.employee.dto.EmployeeSearchResult;
import com.safwat.hr.controller.leave.dto.*;
import com.safwat.hr.controller.leave.service.LeaveApiClient;
import com.safwat.hr.shared.ui.SearchDialog;
import com.safwat.hr.shared.ui.SmartSearchHelper;
import com.safwat.hr.ui.TextFieldSetupHelper;
import com.safwat.hr.ui.UiAsync;
import com.safwat.hr.ui.controls.SAFNotification;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import com.safwat.hr.network.DownloadWithNotification;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
public class LeaveManagementController implements Initializable {

    // ═══════════ FXML — Top ═══════════
    @FXML
    private TextField employeeSearchField;
    @FXML
    private ComboBox<Integer> yearComboBox;
    @FXML
    private Button refreshButton;
    @FXML
    private Button resetBalanceButton;
    @FXML
    private HBox employeeInfoBar;
    @FXML
    private Label employeeNameLabel;
    @FXML
    private Label employeeNationalIdLabel;
    @FXML
    private Label employeeNumberLabel;
    @FXML
    private Label employeeGenderBadge;

    // ═══════════ FXML — بطاقات ═══════════
    @FXML
    private VBox annualCard, casualCard, maternityCard, childCareCard, openEndedCard;
    @FXML
    private Label annualAvailableLabel, annualEntitledLabel,
            annualUsedLabel, annualLateLabel,
            annualRemainingLabel;
    @FXML
    private Label casualValueLabel;
    @FXML
    private ProgressBar casualProgress;
    @FXML
    private Label maternityValueLabel;
    @FXML
    private Label childCareValueLabel, childCareMaxLabel;
    @FXML
    private Label openEndedCountLabel;
    @FXML
    private Button showOpenEndedButton;

    // ═══════════ FXML — Tabs ═══════════
    @FXML
    private TabPane mainTabPane;
    @FXML
    private Tab recordsTab, lateTab, balanceTab;

    // تاب 1
    @FXML
    private ComboBox<LeaveTypeDto> recordsTypeFilter;
    @FXML
    private ComboBox<Integer> recordsYearFilter;
    @FXML
    private ToggleGroup statusToggle;
    @FXML
    private RadioButton statusAllRadio, statusActiveRadio, statusCancelledRadio;
    @FXML
    private Button addRecordButton, editRecordButton, closeRecordButton, deleteRecordButton;
    @FXML
    private TableView<LeaveRecordDto> recordsTable;
    @FXML
    private TableColumn<LeaveRecordDto, String> colType, colStatus, colNotes;
    @FXML
    private TableColumn<LeaveRecordDto, LocalDate> colFrom, colTo;
    @FXML
    private TableColumn<LeaveRecordDto, Integer> colDays, colYear;
    @FXML
    private Label recordsPageLabel;
    @FXML
    private Button recordsPrevButton, recordsNextButton;

    // تاب 2
    @FXML
    private Button addLateButton, editLateButton, deleteLateButton;
    @FXML
    private TableView<LatePermissionDto> lateTable;
    @FXML
    private TableColumn<LatePermissionDto, LocalDate> colLateDate;
    @FXML
    private TableColumn<LatePermissionDto, Object> colLateHours;
    @FXML
    private TableColumn<LatePermissionDto, String> colLateNotes;

    @FXML
    private TableView<LatePermissionMonthDto> lateMonthTable;
    @FXML
    private TableColumn<LatePermissionMonthDto, String> colMonth, colMonthLeftover;
    @FXML
    private TableColumn<LatePermissionMonthDto, Object> colMonthHours, colMonthDeducted;
    @FXML
    private Label lateTotalLabel;
    // تاب 1 — تصدير الإجازات
    @FXML
    private Button exportRecordsButton;
    @FXML
    private TextField exportFromField, exportToField;
    // تاب 3
    @FXML
    private TableView<AnnualBalanceSummaryDto.YearSlice> balanceTable;
    @FXML
    private TableColumn<AnnualBalanceSummaryDto.YearSlice, Integer> colBalanceYear,
            colBalanceEntitled,
            colBalanceUsed,
            colBalanceRemaining;
    @FXML
    private TableColumn<AnnualBalanceSummaryDto.YearSlice, String> colBalanceExpired;
    @FXML
    private Label balanceCurrentYearLabel, balanceTierLabel,
            balanceTotalAvailableLabel;

    // ═══════════ State ═══════════
    private final LeaveApiClient api = new LeaveApiClient();
    private EmployeeSearchResult currentEmployee;
    private LeaveSummaryDto currentSummary;
    private List<LeaveTypeDto> leaveTypes = List.of();
    private int currentPage = 0;
    private int totalPages = 1;
    private static final int PAGE_SIZE = 20;

    // أرقام الطلبات: أي رد متأخر من طلب قديم (موظف/سنة سابقة) يُتجاهل
    private long summarySeq = 0;
    private long recordsSeq = 0;
    private long lateSeq = 0;
    // يمنع إعادة التحميل المتكررة لما نعيد بناء عناصر الكومبو برمجيًا
    private boolean updatingFilters = false;

    // ═══════════════════════════════════════════════════════════
    //  Init
    // ═══════════════════════════════════════════════════════════

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupYearCombo();
        setupSearch();
        setupRecordsTable();
        setupLateTable();
        setupLateMonthTable();
        setupBalanceTable();
        setupHandlers();
        wireSelectionListeners();
        hideCardsUntilEmployeeSelected();
        exportRecordsButton.setDisable(true);   // ← جديد
    }

    private void hideCardsUntilEmployeeSelected() {
        for (VBox c : List.of(annualCard, casualCard, maternityCard, childCareCard, openEndedCard)) {
            c.setVisible(false);
            c.setManaged(false);
        }
    }

    private void showAllCards() {
        for (VBox c : List.of(annualCard, casualCard, openEndedCard)) {
            c.setVisible(true);
            c.setManaged(true);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  سنة الرصيد — عرض "2026-2027"
    // ═══════════════════════════════════════════════════════════

    private void setupYearCombo() {
        yearComboBox.setItems(FXCollections.observableArrayList());

        Callback<ListView<Integer>, ListCell<Integer>> factory = lv -> new ListCell<>() {
            @Override
            protected void updateItem(Integer year, boolean empty) {
                super.updateItem(year, empty);
                setText(empty || year == null ? null : formatLeaveYear(year));
            }
        };
        yearComboBox.setCellFactory(factory);
        yearComboBox.setButtonCell(factory.call(null));

        yearComboBox.valueProperty().addListener((obs, o, n) -> {
            if (n != null && currentEmployee != null && !updatingFilters) {
                currentPage = 0;
                reloadAll();
            }
        });
    }

    private void rebuildYearCombo(LocalDate hireDate) {
        int current = currentLeaveYear();
        int startYear;
        if (hireDate == null) {
            startYear = current;
        } else {
            startYear = hireDate.getMonthValue() >= 7
                    ? hireDate.getYear() + 1
                    : hireDate.getYear();
        }
        if (startYear > current) startYear = current;

        // من السنة الحالية نزولًا لسنة التعيين — لا سنوات مستقبلية
        List<Integer> items = new ArrayList<>();
        for (int y = current; y >= startYear; y--) items.add(y);

        Integer previous = yearComboBox.getValue();
        updatingFilters = true;
        try {
            yearComboBox.setItems(FXCollections.observableArrayList(items));
            if (previous != null && items.contains(previous)) {
                yearComboBox.setValue(previous);
            } else {
                yearComboBox.setValue(current);
            }
        } finally {
            updatingFilters = false;
        }
    }

    private int currentLeaveYear() {
        LocalDate d = LocalDate.now();
        return d.getMonthValue() >= 7 ? d.getYear() + 1 : d.getYear();
    }

    /**
     * 2027 → "2026-2027"
     */
    private String formatLeaveYear(int leaveYear) {
        return (leaveYear - 1) + "-" + leaveYear;
    }

    // ═══════════════════════════════════════════════════════════
    //  البحث
    // ═══════════════════════════════════════════════════════════

    private void setupSearch() {
        SearchDialog<EmployeeSearchResult> dialog = SearchDialog
                .builder(EmployeeSearchResult.class)
                .title("اختر موظفًا")
                .searchPlaceholder("اسم / رقم قومي / رقم وظيفي...")
                .column("الاسم", EmployeeSearchResult::fullName)
                .column("الرقم الوظيفي", EmployeeSearchResult::employeeNumber)
                .column("الرقم القومي", EmployeeSearchResult::nationalId);

        SmartSearchHelper.bind(
                employeeSearchField,
                () -> {
                    String q = employeeSearchField.getText();
                    return q == null ? List.of() : api.searchEmployees(q);
                },
                dialog,
                this::onEmployeeSelected,
                SmartSearchHelper.FieldBind.of(employeeSearchField,
                        EmployeeSearchResult::fullName));
    }

    private void onEmployeeSelected(EmployeeSearchResult emp) {
        this.currentEmployee = emp;
        this.currentPage = 0;
        employeeNameLabel.setText(emp.fullName());
        employeeNationalIdLabel.setText(emp.nationalId());
        employeeNumberLabel.setText(emp.employeeNumber());
        employeeInfoBar.setVisible(true);
        employeeInfoBar.setManaged(true);
        showAllCards();
        exportRecordsButton.setDisable(false);  // ← جديد
        clearViews();              // ← امسح أي بيانات قديمة
        rebuildYearCombo(null);
        reloadAll();
    }

    // ═══════════════════════════════════════════════════════════
    //  Reload
    // ═══════════════════════════════════════════════════════════

    private void reloadAll() {
        if (currentEmployee == null || yearComboBox.getValue() == null) return;
        clearViews();
        loadLeaveTypes();
        loadSummary();           // ← بينادي loadRecords() جوّه بعد ملء الفلتر
        loadLatePermissions();
    }

    /**
     * ★ مسح شامل لكل ما هو معروض.
     * يُنادى:
     * <ul>
     *   <li>عند اختيار موظف جديد</li>
     *   <li>قبل كل إعادة تحميل (لسنة مختلفة مثلاً)</li>
     * </ul>
     * <p>يبطل أي طلب HTTP لسه في الطريق عبر زيادة أرقام الـ seq.
     * <p>لا يمسح: اسم الموظف المعروض، السنة المختارة، فلاتر النوع.
     */
    private void clearViews() {
        // إبطال أي طلب لسه شغّال
        summarySeq++;
        recordsSeq++;
        lateSeq++;

        // State
        currentSummary = null;
        currentPage = 0;
        totalPages = 1;

        // الجداول
        recordsTable.setItems(FXCollections.observableArrayList());
        lateTable.setItems(FXCollections.observableArrayList());
        lateMonthTable.setItems(FXCollections.observableArrayList());
        balanceTable.setItems(FXCollections.observableArrayList());

        // البطاقات — كل الـ Labels ترجع "—"
        for (Label l : List.of(
                annualAvailableLabel, annualEntitledLabel,
                annualUsedLabel, annualLateLabel, annualRemainingLabel,
                casualValueLabel,
                maternityValueLabel,
                childCareValueLabel, childCareMaxLabel,
                openEndedCountLabel,
                balanceCurrentYearLabel, balanceTierLabel,
                balanceTotalAvailableLabel,
                lateTotalLabel)) {
            if (l != null) l.setText("—");
        }
        childCareMaxLabel.setText("من 6 سنوات");
        casualProgress.setProgress(0);
        showOpenEndedButton.setDisable(true);

        // Pagination
        recordsPageLabel.setText("—");
        recordsPrevButton.setDisable(true);
        recordsNextButton.setDisable(true);

        // بطاقات الوضع / رعاية طفل — إخفاء مؤقت (هيتحدد من summary الجديد)
        toggleCard(maternityCard, false);
        toggleCard(childCareCard, false);

        // أزرار CRUD — عطّلها
        editRecordButton.setDisable(true);
        deleteRecordButton.setDisable(true);
        closeRecordButton.setDisable(true);
        editLateButton.setDisable(true);
        deleteLateButton.setDisable(true);
    }

    private void loadLeaveTypes() {
        UiAsync.run(api::getLeaveTypes, list -> {
            leaveTypes = list;
            List<LeaveTypeDto> withPlaceholder = new ArrayList<>();
            LeaveTypeDto all = new LeaveTypeDto();
            all.setCode(null);
            all.setNameAr("كل الأنواع");
            withPlaceholder.add(all);
            withPlaceholder.addAll(list);
            updatingFilters = true;
            try {
                recordsTypeFilter.setItems(FXCollections.observableArrayList(withPlaceholder));
                recordsTypeFilter.getSelectionModel().selectFirst();
            } finally {
                updatingFilters = false;
            }
        });
    }

    private void loadSummary() {
        String nid = currentEmployee.nationalId();
        int year = yearComboBox.getValue();
        final long seq = ++summarySeq;
        UiAsync.run(() -> api.getSummary(nid, year), summary -> {
            if (seq != summarySeq) return;
            currentSummary = summary;
            rebuildYearCombo(summary.getHireDate());
            renderCards(summary);
            renderBalanceTab(summary.getAnnual());
            populateRecordsYearFilter(summary.getHireDate());
            renderLateMonthly();
            loadRecords();
        });
    }

    private void loadRecords() {
        String nid = currentEmployee.nationalId();
        Integer year = recordsYearFilter.getValue();
        LeaveTypeDto selectedType = recordsTypeFilter.getValue();
        String typeCode = (selectedType == null) ? null : selectedType.getCode();

        final long seq = ++recordsSeq;
        recordsTable.setItems(FXCollections.observableArrayList());
        UiAsync.run(() -> api.getRecords(nid, typeCode, year, currentPage, PAGE_SIZE), page -> {
            if (seq != recordsSeq) return;
            recordsTable.setItems(FXCollections.observableArrayList(page.items()));
            totalPages = (page.pagination() != null) ? page.pagination().getTotalPages() : 1;
            updateRecordsPagination(page.pagination());
        });
    }

    private void loadLatePermissions() {
        String nid = currentEmployee.nationalId();
        Integer year = yearComboBox.getValue();
        final long seq = ++lateSeq;
        lateTable.setItems(FXCollections.observableArrayList());
        UiAsync.run(() -> api.getLatePermissions(nid, year), list -> {
            if (seq != lateSeq) return;
            lateTable.setItems(FXCollections.observableArrayList(list));
            renderLateMonthly();
        });
    }

    private void renderLateMonthly() {
        if (currentSummary == null || currentSummary.getLateByMonth() == null) {
            lateMonthTable.setItems(FXCollections.observableArrayList());
            lateTotalLabel.setText("—");
            return;
        }
        lateMonthTable.setItems(FXCollections.observableArrayList(
                currentSummary.getLateByMonth()));
        int total = currentSummary.getLateByMonth().stream()
                .mapToInt(LatePermissionMonthDto::getDeductedDays).sum();
        lateTotalLabel.setText("إجمالي الأيام المخصومة من الاعتيادي: " + total);
    }

    // ═══════════════════════════════════════════════════════════
    //  فلتر سنة السجل — كل السنين من التعيين
    // ═══════════════════════════════════════════════════════════

    private void populateRecordsYearFilter(LocalDate hireDate) {
        int current = currentLeaveYear();
        int startYear;
        if (hireDate == null) {
            startYear = current;
        } else {
            startYear = hireDate.getMonthValue() >= 7
                    ? hireDate.getYear() + 1
                    : hireDate.getYear();
        }
        if (startYear > current) startYear = current;

        List<Integer> items = new ArrayList<>();
        items.add(null);   // "الكل"
        for (int y = current; y >= startYear; y--) {
            items.add(y);
        }

        Integer previous = recordsYearFilter.getValue();
        updatingFilters = true;
        try {
            recordsYearFilter.setItems(FXCollections.observableArrayList(items));

            Callback<ListView<Integer>, ListCell<Integer>> factory = lv -> new ListCell<>() {
                @Override
                protected void updateItem(Integer item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setText(null);
                        return;
                    }
                    setText(item == null ? "الكل" : formatLeaveYear(item));
                }
            };
            recordsYearFilter.setCellFactory(factory);
            recordsYearFilter.setButtonCell(factory.call(null));

            recordsYearFilter.setValue(items.contains(previous) ? previous : null);
        } finally {
            updatingFilters = false;
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  البطاقات
    // ═══════════════════════════════════════════════════════════

    private void renderCards(LeaveSummaryDto s) {
        String gender = s.getGender();
        employeeGenderBadge.setText("FEMALE".equals(gender) ? "أنثى" : "ذكر");

        AnnualBalanceSummaryDto a = s.getAnnual();
        if (a != null) {
            annualAvailableLabel.setText(String.valueOf(a.getAvailable()));
            annualEntitledLabel.setText(String.valueOf(a.getEntitledThisYear()));
            annualUsedLabel.setText(String.valueOf(a.getUsed()));
            annualLateLabel.setText(String.valueOf(a.getUsedViaLate()));
            annualRemainingLabel.setText(String.valueOf(a.getCurrentYearRemaining()));
        }

        CasualSummaryDto c = s.getCasual();
        if (c != null) {
            casualValueLabel.setText(c.getUsed() + " / " + c.getEntitled());
            casualProgress.setProgress(c.getEntitled() == 0 ? 0 : (double) c.getUsed() / c.getEntitled());
        }

        boolean female = "FEMALE".equals(gender);
        toggleCard(maternityCard, female);
        toggleCard(childCareCard, female);

        if (female) {
            if (s.getMaternity() != null) {
                maternityValueLabel.setText(s.getMaternity().getOccurrencesUsed()
                        + " / " + s.getMaternity().getOccurrencesMax());
            } else {
                maternityValueLabel.setText("0 / 3");
            }
            if (s.getChildCare() != null) {
                childCareValueLabel.setText(s.getChildCare().getUsedFormatted());
                childCareMaxLabel.setText("من " + s.getChildCare().getMaxFormatted());
            } else {
                childCareValueLabel.setText("0 يوم");
                childCareMaxLabel.setText("من 6 سنوات");
            }
        }

        int openCount = (s.getOpenEnded() == null) ? 0 : s.getOpenEnded().size();
        openEndedCountLabel.setText(String.valueOf(openCount));
        showOpenEndedButton.setDisable(openCount == 0);
    }

    private void toggleCard(VBox card, boolean visible) {
        card.setVisible(visible);
        card.setManaged(visible);
    }

    // ═══════════════════════════════════════════════════════════
    //  تفاصيل الرصيد — كل السنين
    // ═══════════════════════════════════════════════════════════

    private void renderBalanceTab(AnnualBalanceSummaryDto a) {
        if (a == null) return;

        // كل السنين من التعيين (الباك بيبعتهم كلهم)
        balanceTable.setItems(FXCollections.observableArrayList(a.getSlices()));
        balanceTotalAvailableLabel.setText(String.valueOf(a.getAvailable()));
        balanceCurrentYearLabel.setText(formatLeaveYear(a.getLeaveYear()));
        balanceTierLabel.setText(a.getEntitledThisYear() + " يوم/سنة");
    }

    // ═══════════════════════════════════════════════════════════
    //  إعداد الأعمدة
    // ═══════════════════════════════════════════════════════════

    private void setupRecordsTable() {
        colType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getLeaveTypeNameAr()));
        colFrom.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getFromDate()));
        colTo.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getToDate()));
        colDays.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getDays()));
        colYear.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getLeaveYear()));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(
                "CANCELLED".equals(c.getValue().getStatus()) ? "ملغي" : "مسجل"));
        colNotes.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNotes()));

        colYear.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer year, boolean empty) {
                super.updateItem(year, empty);
                setText(empty || year == null ? null : formatLeaveYear(year));
            }
        });

        colTo.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) {
                    setText(null);
                    return;
                }
                setText(v == null ? "مفتوح" : v.toString());
            }
        });

        recordsTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(LeaveRecordDto r, boolean empty) {
                super.updateItem(r, empty);
                if (empty || r == null) {
                    setStyle("");
                    return;
                }
                setStyle("CANCELLED".equals(r.getStatus()) ? "-fx-opacity: 0.5;" : "");
            }
        });
    }

    private void setupLateTable() {
        colLateDate.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getDate()));
        colLateHours.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getHours()));
        colLateNotes.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNotes()));
    }

    private void setupLateMonthTable() {
        colMonth.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMonth()));
        colMonthHours.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getTotalHours()));
        colMonthDeducted.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getDeductedDays()));
        colMonthLeftover.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getLeftoverHours() + " س"));
    }

    private void setupBalanceTable() {
        colBalanceYear.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getLeaveYear()));
        colBalanceEntitled.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getEntitled()));
        colBalanceUsed.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getUsed()));
        colBalanceRemaining.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getRemaining()));
        colBalanceExpired.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().isExpired() ? "منتهي" : "متاح"));

        colBalanceYear.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer year, boolean empty) {
                super.updateItem(year, empty);
                setText(empty || year == null ? null : formatLeaveYear(year));
            }
        });

        balanceTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(AnnualBalanceSummaryDto.YearSlice s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) {
                    setStyle("");
                    return;
                }
                setStyle(s.isExpired() ? "-fx-opacity: 0.5;" : "");
            }
        });
    }

    // ═══════════════════════════════════════════════════════════
    //  Handlers
    // ═══════════════════════════════════════════════════════════

    private void setupHandlers() {
        refreshButton.setOnAction(e -> {
            if (currentEmployee == null) {
                SAFNotification.warning("اختر موظفًا أولًا");
                return;
            }
            reloadAll();
        });

        addRecordButton.setOnAction(e -> openRecordDialog(null));
        editRecordButton.setOnAction(e -> {
            LeaveRecordDto sel = recordsTable.getSelectionModel().getSelectedItem();
            if (sel != null) openRecordDialog(sel);
        });
        closeRecordButton.setOnAction(e -> closeOpenRecord());
        deleteRecordButton.setOnAction(e -> deleteSelectedRecord());

        showOpenEndedButton.setOnAction(e -> {
            mainTabPane.getSelectionModel().select(recordsTab);
            recordsTypeFilter.getSelectionModel().selectFirst();
            recordsYearFilter.setValue(null);
        });

        recordsPrevButton.setOnAction(e -> {
            if (currentPage > 0) {
                currentPage--;
                loadRecords();
            }
        });
        recordsNextButton.setOnAction(e -> {
            if (currentPage < totalPages - 1) {
                currentPage++;
                loadRecords();
            }
        });

        resetBalanceButton.setOnAction(e -> {
            if (currentEmployee == null) {
                SAFNotification.warning("اختر موظفًا أولًا");
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("إعادة بناء الرصيد");
            confirm.setHeaderText("إعادة حساب استحقاق الإجازات لكل السنين؟");
            confirm.setContentText(
                    "سيتم إعادة حساب الرصيد من تاريخ التعيين حتى الآن.\n"
                            + "الإجازات المسجلة لن تُحذف.");
            Optional<ButtonType> r = confirm.showAndWait();
            if (r.isEmpty() || r.get() != ButtonType.OK) return;

            resetBalanceButton.setDisable(true);
            UiAsync.runVoid(
                    () -> api.resetBalance(currentEmployee.nationalId()),
                    () -> {
                        resetBalanceButton.setDisable(false);
                        SAFNotification.success("تم إعادة بناء الرصيد");
                        reloadAll();
                    },
                    ex -> {
                        resetBalanceButton.setDisable(false);
                        SAFNotification.error(UiAsync.friendly(ex));
                    }
            );
        });

        recordsTypeFilter.valueProperty().addListener((obs, o, n) -> {
            if (updatingFilters) return;
            currentPage = 0;
            if (currentEmployee != null) loadRecords();
        });
        recordsYearFilter.valueProperty().addListener((obs, o, n) -> {
            if (updatingFilters) return;
            currentPage = 0;
            if (currentEmployee != null && recordsTypeFilter.getValue() != null) loadRecords();
        });
        statusToggle.selectedToggleProperty().addListener((obs, o, n) -> {
            currentPage = 0;
            if (currentEmployee != null) loadRecords();
        });

        addLateButton.setOnAction(e -> openLateDialog(null));
        editLateButton.setOnAction(e -> {
            LatePermissionDto sel = lateTable.getSelectionModel().getSelectedItem();
            if (sel != null) openLateDialog(sel);
        });
        deleteLateButton.setOnAction(e -> deleteSelectedLate());

        exportRecordsButton.setOnAction(e -> exportLeaveReport());
        TextFieldSetupHelper.setupDateFields(exportFromField,exportToField);
    }

    private void wireSelectionListeners() {
        recordsTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            boolean has = n != null;
            editRecordButton.setDisable(!has);
            deleteRecordButton.setDisable(!has);
            boolean canClose = has
                    && n.isLeaveTypeOpenEnded()
                    && n.getToDate() == null
                    && !"CANCELLED".equals(n.getStatus());
            closeRecordButton.setDisable(!canClose);
        });

        lateTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            boolean has = n != null;
            editLateButton.setDisable(!has);
            deleteLateButton.setDisable(!has);
        });
    }

    private void updateRecordsPagination(com.safwat.hr.network.ApiResponse.PaginationInfo info) {
        if (info == null) {
            recordsPageLabel.setText("—");
            recordsPrevButton.setDisable(true);
            recordsNextButton.setDisable(true);
            return;
        }
        recordsPageLabel.setText("صفحة " + (info.getPage() + 1) + " من " + info.getTotalPages());
        recordsPrevButton.setDisable(info.isFirst());
        recordsNextButton.setDisable(info.isLast());
    }

    // ═══════════════════════════════════════════════════════════
    //  Dialog: إضافة/تعديل إجازة
    // ═══════════════════════════════════════════════════════════

    private void openRecordDialog(LeaveRecordDto existing) {
        if (currentEmployee == null) {
            SAFNotification.warning("اختر موظفًا أولًا");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/safwat/hr/controller/leave/LeaveRecordDialog.fxml"));
            Parent root = loader.load();
            LeaveRecordDialogController ctrl = loader.getController();
            ctrl.init(currentEmployee, leaveTypes, existing);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(existing == null ? "تسجيل إجازة جديدة" : "تعديل إجازة");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            if (ctrl.isSaved()) reloadAll();
        } catch (IOException ex) {
            SAFNotification.error("تعذر فتح نافذة الإجازة: " + ex.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  Dialog: إغلاق إجازة مفتوحة
    // ═══════════════════════════════════════════════════════════

    private void closeOpenRecord() {
        LeaveRecordDto sel = recordsTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("إغلاق إجازة مفتوحة");
        dialog.setHeaderText("إغلاق: " + sel.getLeaveTypeNameAr()
                + " من " + sel.getFromDate());

        ButtonType okBtn = new ButtonType("إغلاق", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okBtn, ButtonType.CANCEL);

        TextField toDateField = new TextField();
        toDateField.setPromptText("yyyy-MM-dd");
        TextFieldSetupHelper.setupDateFields(toDateField);

        VBox box = new VBox(8,
                new Label("تاريخ النهاية:"),
                toDateField,
                new Label("(الصيغة: yyyy-MM-dd)"));
        box.setPadding(new javafx.geometry.Insets(12));
        dialog.getDialogPane().setContent(box);

        dialog.setResultConverter(bt -> bt == okBtn ? toDateField.getText() : null);

        dialog.showAndWait().ifPresent(text -> {
            LocalDate toDate = TextFieldSetupHelper.parseDateInput(text);
            if (toDate == null) {
                SAFNotification.error("تاريخ غير صالح");
                return;
            }
            UiAsync.runVoid(
                    () -> api.closeRecord(sel.getId(), toDate.toString()),
                    () -> {
                        SAFNotification.success("تم إغلاق الإجازة");
                        reloadAll();
                    });
        });
    }
// ═══════════════════════════════════════════════════════════
//  تصدير تقرير الإجازات (PDF)
// ═══════════════════════════════════════════════════════════

    private void exportLeaveReport() {
        if (currentEmployee == null) {
            SAFNotification.warning("اختر موظفًا أولًا");
            return;
        }
        Integer year = yearComboBox.getValue();
        if (year == null) {
            SAFNotification.warning("اختر سنة الرصيد");
            return;
        }

        LocalDate from = TextFieldSetupHelper.parseDateInput(exportFromField.getText());
        LocalDate to = TextFieldSetupHelper.parseDateInput(exportToField.getText());
        if (from == null || to == null) {
            SAFNotification.error("تاريخ الفترة غير صالح (الصيغة: yyyy-MM-dd)");
            return;
        }
        if (to.isBefore(from)) {
            SAFNotification.error("تاريخ النهاية قبل تاريخ البداية");
            return;
        }

        String nid = currentEmployee.nationalId();
        String path = "/leaves/export/balance-report"
                + "?nationalId=" + URLEncoder.encode(nid, StandardCharsets.UTF_8)
                + "&year=" + year
                + "&from=" + from
                + "&to=" + to;

        exportRecordsButton.setDisable(true);
        DownloadWithNotification.downloadPdfToTempAndNotify(
                path,
                "LEAVE_REPORT_" + nid,
                currentEmployee.fullName(),
                "تقرير الإجازات",
                () -> exportRecordsButton.setDisable(false)
        );
    }
    // ═══════════════════════════════════════════════════════════
    //  حذف سجل
    // ═══════════════════════════════════════════════════════════

    private void deleteSelectedRecord() {
        LeaveRecordDto sel = recordsTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("تأكيد الحذف");
        confirm.setHeaderText("حذف السجل؟");
        confirm.setContentText(sel.getLeaveTypeNameAr()
                + " من " + sel.getFromDate()
                + " إلى " + (sel.getToDate() == null ? "مفتوح" : sel.getToDate()));
        Optional<ButtonType> r = confirm.showAndWait();
        if (r.isEmpty() || r.get() != ButtonType.OK) return;


        UiAsync.runVoid(
                () -> api.deleteRecord(sel.getId()),
                () -> {
                    SAFNotification.success("تم الحذف");
                    reloadAll();
                });
    }

    // ═══════════════════════════════════════════════════════════
    //  Dialog: إذن تأخير
    // ═══════════════════════════════════════════════════════════

    private void openLateDialog(LatePermissionDto existing) {
        if (currentEmployee == null) {
            SAFNotification.warning("اختر موظفًا أولًا");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/safwat/hr/controller/leave/LatePermissionDialog.fxml"));
            Parent root = loader.load();
            LatePermissionDialogController ctrl = loader.getController();
            ctrl.init(currentEmployee, existing);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(existing == null ? "إضافة إذن تأخير" : "تعديل إذن تأخير");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            if (ctrl.isSaved()) {
                loadLatePermissions();
                loadSummary();
            }
        } catch (IOException ex) {
            SAFNotification.error("تعذر فتح نافذة الإذن: " + ex.getMessage());
        }
    }

    private void deleteSelectedLate() {
        LatePermissionDto sel = lateTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("تأكيد الحذف");
        confirm.setHeaderText("حذف الإذن؟");
        confirm.setContentText("التاريخ: " + sel.getDate() + " — الساعات: " + sel.getHours());
        Optional<ButtonType> r = confirm.showAndWait();
        if (r.isEmpty() || r.get() != ButtonType.OK) return;

        UiAsync.runVoid(
                () -> api.deleteLate(sel.getId()),
                () -> {
                    SAFNotification.success("تم الحذف");
                    loadLatePermissions();
                    loadSummary();
                });
    }
}