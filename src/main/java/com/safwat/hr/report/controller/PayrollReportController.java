package com.safwat.hr.report.controller;

import com.safwat.hr.network.ApiClient;
import com.safwat.hr.network.ApiResponse;
import com.safwat.hr.network.dto.AvailableReportInfo;
import com.safwat.hr.network.dto.ReportPayloadResponse;
import com.safwat.hr.payroll.dto.SearchEmp;
import com.safwat.hr.payroll.payments.service.PayrollPaymentsService;
import com.safwat.hr.report.core.DataSourceResolver;
import com.safwat.hr.report.core.ReportContext;
import com.safwat.hr.report.core.ReportSubmissionService;
import com.safwat.hr.report.core.ValidationException;
import com.safwat.hr.report.core.strategies.ReportRegistryFactory;
import com.safwat.hr.report.core.strategies.ReportStrategy;
import com.safwat.hr.report.core.strategies.ReportStrategyRegistry;
import com.safwat.hr.report.core.ui.PayrollUIManager;
import com.safwat.hr.shared.PayrollRequest;
import com.safwat.hr.shared.ui.SearchDialog;
import com.safwat.hr.shared.util.DateUtils;
import com.safwat.hr.ui.controls.SAFNotification;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;

/**
 * الـ Controller الرئيسي لشاشة تقارير الرواتب.
 *
 * <p>يتولى التنسيق بين:
 * <ul>
 *   <li>القوائم المنسدلة (رئيسية + فرعية)</li>
 *   <li>مدير الواجهة {@link PayrollUIManager} — إظهار/إخفاء الحقول</li>
 *   <li>الاستراتيجيات {@link ReportStrategy} — منطق كل تقرير</li>
 *   <li>خدمة الإرسال {@link ReportSubmissionService} — HTTP على خيط خلفي</li>
 * </ul>
 *
 * <p><b>قاعدة ذهبية:</b> الـ Controller لا يعرف شيئًا عن منطق أي تقرير بعينه.
 * كل التخصيص موجود في الاستراتيجية عبر {@link ReportStrategy#onApply}.
 *
 *
 *
 * <hr>
 *
 * <h2>تدفق العمل</h2>
 * <pre>
 * فتح الشاشة
 *   → initialize() : يملأ القوائم ويخفي الحقول
 *
 * اختيار تقرير رئيسي
 *   → hasSubReports = true  : يُظهر ComboBox الفرعيات
 *   → hasSubReports = false : يستدعي applyStrategy() مباشرةً
 *
 * اختيار تقرير فرعي
 *   → applyStrategy() : يُطبِّق UiConfiguration + onApply()
 *
 * الضغط على "استعلام"
 *   → doReport() : يبني ReportContext ← يتحقق ← يبني الطلب ← يُرسِل
 * </pre>
 *
 * <h2>إدارة الملفات</h2>
 * <p>بعض التقارير تتطلب رفع ملفات (مثل Excel). يدير الـ Controller:
 * <ul>
 *   <li>{@link #chooseFiles()} — فتح FileChooser وتخزين المسارات</li>
 *   <li>{@link #previewFiles()} — Dialog لمعاينة الملفات وإزالتها</li>
 *   <li>{@link #clearSelectedFiles()} — تفريغ القائمة عند تغيير التقرير</li>
 * </ul>
 * <p><b>أمان:</b> عند تغيير التقرير (رئيسي أو فرعي) تُفرَّغ قائمة الملفات تلقائيًا
 * لمنع إرسال ملفات خاطئة.
 */
@Slf4j
@Getter
public class PayrollReportController implements Initializable {

    // ═══════════════════════════════════════════════════════════════
    //  Services & State
    // ═══════════════════════════════════════════════════════════════

    /**
     * خدمة دفعات الرواتب — للبحث عن الموظفين.
     */
    private final PayrollPaymentsService paymentsService = PayrollPaymentsService.getInstance();

    /**
     * سجل الاستراتيجيات المسجَّلة.
     */
    private final ReportStrategyRegistry registry;

    /**
     * مدير الواجهة — يتحكم في إظهار/إخفاء الحقول.
     */
    private final PayrollUIManager uiManager;

    /**
     * خدمة إرسال التقارير — على خيط خلفي.
     */
    private final ReportSubmissionService submissionService;

    /**
     * الملفات المختارة للتقرير الحالي.
     *
     * <p>يجب أن تكون {@link ArrayList} (mutable) لأن {@link #refreshPreviewList}
     * يحتاج {@code remove(index)}. لا تستخدم {@code List.of()} أو {@code .toList()}.
     */
    private final List<Path> selectedFiles = new ArrayList<>();
    private final List<String> selectedGroups = new ArrayList<>();


    /**
     * الاستراتيجية الحالية. {@code null} إذا اختار تقريرًا حاويًا ولم يختر فرعيًا بعد.
     */
    private ReportStrategy currentStrategy;

    // ═══════════════════════════════════════════════════════════════
    //  FXML Components
    // ═══════════════════════════════════════════════════════════════

    // --- حقول HBox متفرقة ---
    @FXML
    private HBox H_ReportType, H_3, H_4, H_5, H_6;
    @FXML
    private HBox H_Search, H_employee, H_endDate, H_management, H_payGroup, H_startDate, H_files;

    /**
     * حقل HBox لـ ComboBox التقارير الفرعية — يُدار بشكل مستقل عن hideAll().
     */
    @FXML
    private HBox H_report;

    // --- القوائم المنسدلة ---
    @FXML

    private ComboBox<String> combo_Format, combo_report, combo_reportType;

    // --- Labels ---
    @FXML
    private Label lbl_endDate, lbl_name, lbl_nationalId, lbl_payId, lbl_startDate;
    @FXML
    private Label lbl_searchValue, lbl_start, lbl_end, lbl_search, lbl_filesCount;

    // --- حقول الإدخال ---
    @FXML
    private TextField txt_startDate, txt_endDate, txt_search, txt_searchEmp;
    @FXML
    private TextField txt_payGroup, txt_management;

    // --- الـ Container الرئيسي ---
    @FXML
    private VBox mainCont;

    // --- الأزرار ---
    @FXML
    private Button btn_PayGroupSearch, btn_managementSearch;
    @FXML
    private Button btn_searchMonth, btn_searchMonthEnd;
    @FXML
    private Button btn_SearchEmployee, btn_Search, btnDoReport;
    @FXML
    private Button btn_attachFiles, btn_previewFiles, btn_copyLast;
    @FXML
    private TextField txt_reportSearch;
    @FXML
    private Button btn_reportSearch;

    private List<String> availableReports = new ArrayList<>();
    // ═══════════════════════════════════════════════════════════════
    //  Constructor
    // ═══════════════════════════════════════════════════════════════

    public PayrollReportController() {
        this.registry = ReportRegistryFactory.create();
        this.uiManager = new PayrollUIManager(this);
        this.submissionService = new ReportSubmissionService();
    }

    // ═══════════════════════════════════════════════════════════════
    //  Initialization
    // ═══════════════════════════════════════════════════════════════

    /**
     * يغلق الـ Stage الحاوي لأي مكوّن.
     */
    public static void closeStage(Node targetComponent) {
        ((Stage) targetComponent.getScene().getWindow()).close();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        fillFormatCombo();
        loadAvailableReports();
        setupListeners();
        uiManager.hideAll();

        btn_copyLast.setOnAction(_ -> copyLastReport());
    }

    void loadAvailableReports() {
        try {
            availableReports = ApiClient.getAvailableReports()
                    .getData().stream()
                    .map(AvailableReportInfo::getArabicName)
                    .distinct()
                    .sorted()
                    .toList();
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("فشل تحميل قائمة التقارير", e);
        }
    }

    /**
     * يملأ قائمة الصيغ ويختار PDF افتراضياً.
     */
    void fillFormatCombo() {
        combo_Format.getItems().addAll("PDF", "EXCEL");
        combo_Format.getSelectionModel().select(0);
    }

    // ═══════════════════════════════════════════════════════════════
    //  Listeners — التسلسل الرئيسي للتفاعل
    // ═══════════════════════════════════════════════════════════════

    /**
     * يُعدِّد Listeners للقائمتين الرئيسية والفرعية وأزرار الشهر.
     *
     * <p>عند <b>أي</b> تغيير للتقرير (رئيسي أو فرعي) تُفرَّغ قائمة الملفات
     * عبر {@link #clearSelectedFiles()} لضمان عدم إرسال ملفات خاطئة.
     */
    void setupListeners() {
        // ── القائمة الرئيسية ──
        // ── اختيار التقرير الرئيسي (TextField + SearchDialog) ──
        txt_reportSearch.setOnAction(_ -> openReportSearch());      // Enter
        txt_reportSearch.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) openReportSearch();        // ضغطة واحدة
        });
        btn_reportSearch.setOnAction(_ -> openReportSearch());

        // ── القائمة الفرعية ──
        combo_report.getSelectionModel().selectedItemProperty()
                .addListener((_, _, newVal) -> {
                    if (newVal == null) return;

                    clearSelectedFiles();

                    applyStrategy(registry.getByDisplayName(newVal));
                });

        // ── أزرار الشهر المشتركة ──
        setupMonthButton(btn_searchMonth, txt_startDate, lbl_startDate);
        setupMonthButton(btn_searchMonthEnd, txt_endDate, lbl_endDate);

        // ── زر البحث عن موظف ──
        btn_SearchEmployee.setOnAction(_ -> searchEmployee());
    }

    /**
     * يفتح SearchDialog بكل التقارير المتاحة
     */
    @FXML
    void openReportSearch() {
        if (availableReports.isEmpty()) {
            SAFNotification.warning("لا توجد تقارير متاحة");
            return;
        }

        Optional<String> result = SearchDialog.forStrings()
                .title("اختر التقرير")
                .data(availableReports)
                .searchPlaceholder("اكتب اسم التقرير للبحث...")
                .show();

        result.ifPresent(this::selectReport);
    }

    /**
     * يُنفِّذ اختيار تقرير رئيسي — نفس منطق listener الـ ComboBox السابق.
     *
     * @param reportName اسم التقرير المختار من الـ SearchDialog
     */
    private void selectReport(String reportName) {
        txt_reportSearch.setText(reportName);
        clearSelectedFiles();

        ReportStrategy mainStrategy = registry.getByDisplayName(reportName);

        if (mainStrategy.hasSubReports()) {
            uiManager.hideAll();
            combo_report.getItems().clear();
            combo_report.getItems().addAll(
                    registry.getDisplayNamesByCategory(mainStrategy.getMainReport())
            );
            H_report.setManaged(true);
            H_report.setVisible(true);
            currentStrategy = null;
            updateFileAttachmentVisibility();

        } else {
            H_report.setManaged(false);
            H_report.setVisible(false);
            combo_report.getItems().clear();
            combo_report.getSelectionModel().clearSelection();
            applyStrategy(mainStrategy);
        }
    }
    // ═══════════════════════════════════════════════════════════════
    //  Strategy Application
    // ═══════════════════════════════════════════════════════════════

    /**
     * يُطبِّق استراتيجية على الواجهة.
     *
     * <p>نقطة التحكم المركزية — كل تبديل تقرير يمر من هنا.
     */
    private void applyStrategy(ReportStrategy strategy) {
        currentStrategy = strategy;
        uiManager.apply(strategy.getUiConfig(), strategy);
        updateFileAttachmentVisibility();
    }

    // ═══════════════════════════════════════════════════════════════
    //  File Handling — اختيار / معاينة / إزالة
    // ═══════════════════════════════════════════════════════════════

    /**
     * يفتح FileChooser لاختيار ملفات متعددة.
     *
     * <p>تُضاف الملفات إلى {@link #selectedFiles} (لا تستبدل القائمة).
     */
    @FXML
    void chooseFiles() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("اختر الملفات");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel & PDF", "*.xlsx", "*.xls", "*.pdf")
        );

        List<File> files = chooser.showOpenMultipleDialog(mainCont.getScene().getWindow());
        if (files != null && !files.isEmpty()) {
            selectedFiles.addAll(files.stream().map(File::toPath).toList());
            updateFilesLabel();
        }
    }

    /**
     * يفتح Dialog لمعاينة الملفات المختارة وإزالة أي منها.
     *
     * <p>إذا أُفرِغَت القائمة بالكامل يُغلَق الـ Dialog تلقائياً.
     */
    @FXML
    void previewFiles() {
        if (selectedFiles.isEmpty()) {
            SAFNotification.warning("لا توجد ملفات للمعاينة");
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("الملفات المختارة");
        dialog.setHeaderText("مراجعة الملفات قبل الإرسال");

        ListView<HBox> listView = new ListView<>();
        listView.setPrefWidth(450);
        listView.setPrefHeight(Math.min(selectedFiles.size() * 50 + 20, 300));

        refreshPreviewList(listView, dialog);

        dialog.getDialogPane().setContent(listView);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setStyle("-fx-background-color: white; -fx-padding: 15;");
        dialog.showAndWait();
    }

    /**
     * يعيد بناء عناصر ListView لمعاينة الملفات.
     *
     * <p>كل صف يحتوي: (أيقونة + اسم + حجم + زر حذف).
     * عند الحذف يُستدعى {@code selectedFiles.remove(index)} ثم يُعاد بناء القائمة.
     */
    private void refreshPreviewList(ListView<HBox> listView, Dialog<Void> dialog) {
        listView.getItems().clear();

        for (int i = 0; i < selectedFiles.size(); i++) {
            final int index = i;
            Path file = selectedFiles.get(i);

            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(5, 10, 5, 10));

            String icon = file.toString().endsWith(".pdf") ? "📄" : "📊";
            Label nameLabel = new Label(icon + "  " + file.getFileName());
            nameLabel.setMaxWidth(320);
            nameLabel.setEllipsisString("...");
            HBox.setHgrow(nameLabel, Priority.ALWAYS);

            Label sizeLabel = new Label(formatFileSize(file));
            sizeLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11;");

            Button removeBtn = new Button("حذف");
            removeBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
            removeBtn.setOnAction(_ -> {
                selectedFiles.remove(index);   // ✅ ArrayList — mutable
                updateFilesLabel();

                if (selectedFiles.isEmpty()) {
                    dialog.close();
                } else {
                    refreshPreviewList(listView, dialog);
                }
            });

            row.getChildren().addAll(nameLabel, sizeLabel, removeBtn);
            listView.getItems().add(row);
        }
    }

    /**
     * يُنسِّق حجم الملف (B / KB / MB).
     */
    private String formatFileSize(Path file) {
        try {
            long bytes = Files.size(file);
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        } catch (IOException e) {
            return "";
        }
    }

    /**
     * يُفرِّغ قائمة الملفات ويُحدِّث الـ Label.
     * يُستدعى عند تغيير التقرير لضمان عدم إرسال ملفات خاطئة.
     */
    private void clearSelectedFiles() {
        selectedFiles.clear();
        selectedGroups.clear();
        updateFilesLabel();
    }

    /**
     * يُحدِّث نص الـ Label ويُظهر/يُخفي زر المعاينة حسب العدد.
     */
    private void updateFilesLabel() {
        int count = selectedFiles.size();

        if (count == 0) {
            lbl_filesCount.setText("");
            btn_previewFiles.setManaged(false);
            btn_previewFiles.setVisible(false);
        } else {
            lbl_filesCount.setText("تم اختيار " + count + (count == 1 ? " ملف" : " ملفات"));
            btn_previewFiles.setManaged(true);
            btn_previewFiles.setVisible(true);
        }
    }

    /**
     * يُظهر أو يُخفي زر اختيار الملفات حسب التقرير الحالي.
     */
    private void updateFileAttachmentVisibility() {
        boolean needsFiles = currentStrategy != null && currentStrategy.requiresFiles();

        if (H_files != null) {
            H_files.setManaged(needsFiles);
            H_files.setVisible(needsFiles);
        }
        if (!needsFiles) {
            clearSelectedFiles();
        } else {
            updateFilesLabel();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Search Dialogs — أزرار البحث المشتركة
    // ═══════════════════════════════════════════════════════════════

    /**
     * يفتح نافذة بحث شهر ويضع النتيجة في الـ TextField + Label.
     *
     * <p>متاح للاستراتيجيات في {@code onApply} لإعادة الربط بـ Label مختلف.
     */
    public void setupMonthButton(Button btn, TextField field, Label label) {
        btn.setOnAction(_ -> {
            Optional<String> month = SearchDialog.forStrings()
                    .title("اختر شهر")
                    .data(DataSourceResolver.get("monthsYearly"))
                    .show();
            month.ifPresent(m -> setMonthField(field, label, m));
        });

        field.setOnAction(_ -> {
            LocalDate parsed = DateUtils.getFirstDayOfMonth(field.getText());
            if (parsed != null) {
                label.setText(DateUtils.toArabicMonthYear(parsed));
            } else {
                // لو غير صالح — افتح الـ Dialog
                Optional<String> month = SearchDialog.forStrings()
                        .title("اختر شهر")
                        .data(DataSourceResolver.get("monthsYearly"))
                        .show();
                month.ifPresent(m -> setMonthField(field, label, m));
            }
        });
    }

    private void setMonthField(TextField field, Label label, String monthValue) {
        field.setText(monthValue);
        label.setText(DateUtils.toArabicMonthYear(DateUtils.getFirstDayOfMonth(monthValue)));
    }

    /**
     * يفتح نافذة بحث عامة ويضع النتيجة في الـ TextField المستهدف.
     *
     * <p>يُستدعى من {@link PayrollUIManager} أو من الاستراتيجيات مباشرةً.
     */
    public void openSearchDialog(String title, List<String> data, TextField targetField) {
        String currentText = targetField.getText();
        List<String> dialogData = (currentText == null || currentText.isBlank())
                ? data
                : data.stream().filter(s -> s.contains(currentText)).toList();

        if (dialogData.isEmpty()) dialogData = data;

        SearchDialog.forStrings()
                .title(title)
                .data(dialogData)
                .show()
                .ifPresent(targetField::setText);
    }

    /**
     * يبحث عن موظف بالرقم القومي أو الاسم ويملأ Labels.
     */
    public void searchEmployee() {
        PayrollRequest request = PayrollRequest.builder().build();
        request.setSearchValue(txt_searchEmp.getText());
        List<SearchEmp> data = paymentsService.searchInEmployees(request).getData();

        if (data.size() == 1) {
            fillEmployeeLabels(data.getFirst());
            return;
        }

        Optional<SearchEmp> result = SearchDialog.builder(SearchEmp.class)
                .title("نتائج البحث")
                .column("رقم قومي", SearchEmp::getNational_id)
                .column("رقم موظف", SearchEmp::getPay_id)
                .column("اسم", SearchEmp::getEmp_name)
                .data(data)
                .searchPlaceholder("ابحث للتصفية")
                .show();

        result.ifPresent(this::fillEmployeeLabels);
    }

    private void fillEmployeeLabels(SearchEmp emp) {
        lbl_nationalId.setText(emp.getNational_id());
        lbl_payId.setText(emp.getPay_id());
        lbl_name.setText(emp.getEmp_name());
    }

    // ═══════════════════════════════════════════════════════════════
    //  Report Submission — الضغط على "استعلام"
    // ═══════════════════════════════════════════════════════════════

    /**
     * يُنفِّذ عملية إرسال التقرير.
     *
     * <ol>
     *   <li>التحقق من اختيار استراتيجية.</li>
     *   <li>بناء {@link ReportContext} من حقول الـ UI (بما فيها الملفات).</li>
     *   <li>التحقق من صحة المدخلات عبر {@code strategy.validate()}.</li>
     *   <li>بناء الطلب عبر {@code strategy.buildRequest()}.</li>
     *   <li>الإرسال عبر {@link ReportSubmissionService#submit} على خيط خلفي.</li>
     * </ol>
     */
    @FXML
    void doReport() {
        if (currentStrategy == null) {
            SAFNotification.warning("اختر تقرير أولاً");
            return;
        }

        try {
            ReportContext context = ReportContext.builder()
                    .user(ApiClient.getUserName())
                    .reportName(txt_reportSearch.getText())
                    .reportType(combo_reportType.getSelectionModel().getSelectedItem())
                    .startDate(txt_startDate.getText())
                    .endDate(txt_endDate.getText())
                    .management(txt_management.getText())
                    .payGroup(txt_payGroup.getText())
                    .nationalId(lbl_nationalId.getText())
                    .searchValue(txt_search.getText())

                    .format(combo_Format.getSelectionModel().getSelectedItem())
                    .files(new ArrayList<>(selectedFiles)) // نسخة defensive
                    .build();

            currentStrategy.validate(context);

            PayrollRequest request = currentStrategy.buildRequest(context);

            submissionService.submit(request, new ArrayList<>(selectedFiles),
                    reportId -> Platform.runLater(() -> {
                        closeStage(btnDoReport);
                        SAFNotification.success("تم تقديم الطلب بنجاح — رقم الطلب: " + reportId);
                    }),
                    error -> Platform.runLater(() ->
                            SAFNotification.error(error.getMessage())
                    )
            );

        } catch (ValidationException ve) {
            SAFNotification.warning(ve.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Public Helpers — للاستراتيجيات (onApply)
    // ═══════════════════════════════════════════════════════════════

    /**
     * يضبط Label الشهر لـ "بداية التقرير" / "نهاية التقرير".
     */
    public void setStartAndEndActions() {
        setupMonthButton(btn_searchMonth, txt_startDate, lbl_startDate);
        setupMonthButton(btn_searchMonthEnd, txt_endDate, lbl_endDate);
        lbl_start.setText("بداية التقرير");
        lbl_end.setText("نهاية التقرير");
    }

    /**
     * يضبط Label الشهر لـ "اختر شهر".
     */
    public void setChoseMonth() {
        setupMonthButton(btn_searchMonth, txt_startDate, lbl_startDate);
        lbl_start.setText("اختر شهر");
    }

    /**
     * يفعّل زر وحقل البحث عن موظف.
     */
    public void setSearchEmployeeActions() {
        btn_SearchEmployee.setOnAction(_ -> searchEmployee());
        txt_searchEmp.setOnAction(_ -> searchEmployee());
    }

    private void copyLastReport() {
        try {
            ApiResponse<ReportPayloadResponse> data = ApiClient.getReportPayload();
            loadFromPayload(data.getData());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * يملأ الواجهة من بيانات تقرير سابق (للنسخ).
     *
     * <p><b>المنطق:</b>
     * <ol>
     *   <li>نستخدم {@code reportName} للبحث عن <b>الاستراتيجية الرئيسية</b> (بالاسم المعروض)</li>
     *   <li>لو مباشر → نطبقه فوراً</li>
     *   <li>لو حاوٍ → نجهز الـ ComboBox الفرعي، ونبحث عن الفرعي بـ {@code reportCode}</li>
     *   <li>نملأ الحقول من الـ payload</li>
     * </ol>
     */
    public void loadFromPayload(ReportPayloadResponse response) {
        if (response == null || response.payload() == null) {
            SAFNotification.warning("بيانات التقرير فارغة");
            return;
        }

        String reportName = response.reportName(); // الاسم المعروض في الـ response
        String reportCode = response.reportCode();   // الكود الفريد
        Map<String, Object> payload = response.payload();

        // ── 1. البحث عن الاستراتيجية بالاسم ──
        ReportStrategy strategy = registry.getByDisplayName(reportName);
        if (strategy == null) {
            SAFNotification.warning("التقرير غير متاح: " + reportName);
            return;
        }

        // نعرض الاسم في الحقل الرئيسي
        txt_reportSearch.setText(reportName);

        System.out.println(reportName);
        System.out.println(txt_reportSearch.getText());
        Platform.runLater(() -> {
            selectReport(reportName); // نطبق على طول
        });

        // ── 2. لو رئيسي مباشر ──
        if (!"main_direct".equals(strategy.getCategory())) {
            ReportStrategy sub = registry.getByCode(response.reportCode());
            String subReportName = sub.getDisplayName();
            combo_report.setValue(subReportName);
            Platform.runLater(() -> {
                applyStrategy(sub);
            });
        }

        // ── 4. نملأ الحقول والملفات ──
        Platform.runLater(() -> fillFieldsFromPayload(payload));

        SAFNotification.success("تم تحميل بيانات التقرير — راجع الحقول وأرسل");
    }

    // ═══════════════════════════════════════════════════════════════
//  Helpers
// ═══════════════════════════════════════════════════════════════
    private void fillFieldsFromPayload(Map<String, Object> payload) {
        // ── التواريخ ──
        setDateIfNotNull(payload.get("startDate"), txt_startDate, lbl_startDate);
        setDateIfNotNull(payload.get("endDate"), txt_endDate, lbl_endDate);

        setIfNotNull(payload.get("management"), txt_management::setText);
        setIfNotNull(payload.get("payGroup"), txt_payGroup::setText);
        setIfNotNull(payload.get("nationalId"), v -> lbl_nationalId.setText(v));
        setIfNotNull(payload.get("searchValue"), txt_search::setText);
        setIfNotNull(payload.get("reportType"), combo_reportType::setValue);

        Object format = payload.get("format");
        if (format != null) {
            combo_Format.getSelectionModel().select(format.toString());
        }

    }

    /**
     * يملأ حقل التاريخ (TextField + Label) من قيمة payload.
     * القيمة ممكن تكون LocalDate أو String بصيغة ISO.
     */
    private void setDateIfNotNull(Object value, TextField field, Label label) {
        if (value == null) return;
        System.out.println(value);
        // نملأ الـ Label بالشهر العربي
        try {
            LocalDate d = DateUtils.parseDate(value.toString());

            field.setText(DateUtils.toMonthYearNumber(d));
            label.setText(DateUtils.toArabicMonthYear(d));
        } catch (Exception e) {
            label.setText(""); // لو فشل التحويل
        }
    }

    private void setIfNotNull(Object value, Consumer<String> setter) {
        if (value != null) setter.accept(value.toString());
    }

}