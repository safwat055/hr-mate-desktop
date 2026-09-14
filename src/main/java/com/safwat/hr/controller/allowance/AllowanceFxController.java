package com.safwat.hr.controller.allowance;


import com.fasterxml.jackson.core.type.TypeReference;
import com.safwat.hr.hrScale.allowance.dto.AllowanceResultDto;
import com.safwat.hr.hrScale.allowance.dto.AllowanceResultDto.AllowanceLineDto;
import com.safwat.hr.hrScale.allowance.dto.AllowanceResultDto.AllowanceLineDto.Source;
import com.safwat.hr.hrScale.allowance.entity.AllowanceDefinition;
import com.safwat.hr.hrScale.scale.ScaleDto;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller الرئيسي لشاشة البدلات.
 *
 * <p>مسؤول عن:
 * <ul>
 *   <li>البحث عن موظف وتحميل بياناته</li>
 *   <li>عرض بيانات الموظف في تاب 1</li>
 *   <li>عرض وإدارة البدلات في تاب 2</li>
 *   <li>فتح dialog تعديل الفترات</li>
 *   <li>فتح dialog إدارة قواعد البدلات (allowance_definition)</li>
 * </ul>
 *
 * <p><b>ملاحظة أداء مهمة:</b> بيانات الموظف الأساسية ({@link ScaleDto}
 * — الاسم، القانون، تاريخ التعيين، الـ timeline...) ثابتة ومش بتتغير
 * حسب "تاريخ الاحتساب" في تاب البدلات — التاريخ بيأثر بس على *نتيجة*
 * احتساب البدلات، مش على بيانات الموظف نفسها. عشان كده بتتحمّل مرة
 * واحدة بس لكل موظف (عند البحث) وتتخزن في {@link #cachedScaleDto}،
 * وأي إعادة احتساب بعد كده (تغيير التاريخ + زر "احتساب") بتستخدم
 * النسخة المخزنة من غير ما تعمل call جديد لـ /api/salary-scale.
 * الكاش بيتشال (يتصفّر) لما نبحث عن موظف تاني برقم قومي مختلف.
 *
 * <p><b>ملاحظة شبكة:</b> كل نداءات الـ API بتعدي على
 * {@link FxApiSupport} (غلاف فوق {@code com.safwat.hr.network.ApiClient}
 * الـ static/CompletableFuture)، مش على أي instance قديم.
 */
public class AllowanceFxController implements Initializable {

    // ── شريط البحث ──────────────────────────────────────────────
    @FXML
    private TextField txt_nationalId;
    @FXML
    private Button btn_search;
    @FXML
    private ProgressIndicator progress_indicator;
    @FXML
    private Label lbl_error;
    @FXML
    private Button btn_reset;
    @FXML
    private Button btn_manageDefinitions;

    // ── تاب بيانات الموظف ───────────────────────────────────────
    @FXML
    private TextField txt_empName;
    @FXML
    private TextField txt_empNationalId;
    @FXML
    private TextField txt_empLawCode;
    @FXML
    private TextField txt_empLaw;
    @FXML
    private TextField txt_empStartDate;
    @FXML
    private TextField txt_empDegree;
    @FXML
    private TextField txt_empBasic30;
    @FXML
    private TextField txt_empBasic30From;
    @FXML
    private TextField txt_empGroup;

    // ── تاب البدلات ─────────────────────────────────────────────
    @FXML
    private DatePicker date_calculation;
    @FXML
    private Button btn_recalculate;
    @FXML
    private ComboBox<AllowanceDefinition> combo_newAllowance;
    @FXML
    private Button btn_addAllowance;
    @FXML
    private Label lbl_total;

    // ── جدول البدلات ────────────────────────────────────────────
    @FXML
    private TableView<AllowanceLineDto> table_allowances;
    @FXML
    private TableColumn<AllowanceLineDto, String> col_nameAr;
    @FXML
    private TableColumn<AllowanceLineDto, BigDecimal> col_value;
    @FXML
    private TableColumn<AllowanceLineDto, LocalDate> col_effectiveFrom;
    @FXML
    private TableColumn<AllowanceLineDto, Source> col_source;
    @FXML
    private TableColumn<AllowanceLineDto, Void> col_actions;

    // ── State ────────────────────────────────────────────────────
    private String currentNationalId;
    private AllowanceResultDto currentResult;
    /**
     * كاش بيانات الموظف الأساسية — بتتحمّل مرة واحدة بس لكل موظف.
     * null = لسه محمّلتش (أو اتشالت بسبب بحث جديد).
     */
    private ScaleDto cachedScaleDto;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ════════════════════════════════════════════════════════════
    //  Initialize
    // ════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        setupCombo();
        date_calculation.setValue(LocalDate.now());

        // Enter في حقل البحث يشغل البحث
        txt_nationalId.setOnAction(e -> handleSearch());
        btn_search.setOnAction(e -> handleSearch());
        btn_recalculate.setOnAction(e -> loadAllowances());
        btn_addAllowance.setOnAction(e -> handleAddAllowance());
        btn_reset.setOnAction(e -> handleReset());
        if (btn_manageDefinitions != null) {
            btn_manageDefinitions.setOnAction(e -> openDefinitionsDialog());
        }
    }

    // ════════════════════════════════════════════════════════════
    //  البحث وتحميل البيانات
    // ════════════════════════════════════════════════════════════

    private void handleSearch() {
        String id = txt_nationalId.getText().trim();
        if (id.isBlank()) {
            showError("أدخل الرقم القومي أولاً");
            return;
        }

        // بحث عن موظف مختلف عن اللي محمّل حاليًا → امسح الكاش القديم
        // (لو نفس الموظف، سيب الكاش زي ما هو، مفيش داعي لإعادة تحميله)
        if (!id.equals(currentNationalId)) {
            cachedScaleDto = null;
            currentResult = null;
        }

        currentNationalId = id;
        loadAllowances();
    }

    /**
     * إعادة احتساب البدلات بالتاريخ الحالي في date_calculation.
     *
     * <p>بتنادي endpoint البدلات بس ({@code /api/allowances/employee/{id}}).
     * بيانات الموظف الأساسية (ScaleDto) بتتحمّل مرة واحدة فقط عبر
     * {@link #ensureEmployeeDetailsLoaded()} — مش بتتحمّل تاني هنا.
     */
    private void loadAllowances() {
        if (currentNationalId == null) return;

        String dateParam = date_calculation.getValue() != null
                ? "?date=" + date_calculation.getValue().format(DATE_FMT)
                : "";

        showLoading(true);
        clearError();

        FxApiSupport.get(
                "/api/allowances/employee/" + currentNationalId + dateParam,
                AllowanceResultDto.class,
                result -> {
                    showLoading(false);
                    currentResult = result;
                    fillAllowancesTable(result);
                    btn_reset.setVisible(true);
                    // حمّل قائمة البدلات المتاحة بعد ما currentResult
                    // اتحدث (مش قبله)، عشان الفلترة تبقى صح
                    loadDefinitions();
                },
                err -> {
                    showLoading(false);
                    showError(err);
                }
        );

        ensureEmployeeDetailsLoaded();
    }

    private void loadDefinitions() {
        FxApiSupport.getList(
                "/api/allowances/definitions",
                new TypeReference<List<AllowanceDefinition>>() {
                },
                defs -> {
                    // فلتر البدلات اللي مش موجودة عند الموظف
                    List<String> existing = currentResult != null
                            ? currentResult.allowances().stream()
                            .map(AllowanceLineDto::code).toList()
                            : List.of();
                    List<AllowanceDefinition> available = defs.stream()
                            .filter(d -> !existing.contains(d.getCode()))
                            .toList();
                    combo_newAllowance.setItems(
                            FXCollections.observableArrayList(available)
                    );
                },
                err -> {
                }
        );
    }

    // ════════════════════════════════════════════════════════════
    //  ملء تاب بيانات الموظف
    // ════════════════════════════════════════════════════════════

    /**
     * يتأكد إن بيانات الموظف الأساسية محمّلة، وميعملش call جديد
     * لو الكاش موجود بالفعل لنفس الموظف.
     */
    private void ensureEmployeeDetailsLoaded() {
        if (cachedScaleDto != null) {
            applyEmployeeDetails(cachedScaleDto);
            return;
        }

        FxApiSupport.get(
                "/api/salary-scale/" + currentNationalId,
                ScaleDto.class,
                dto -> {
                    cachedScaleDto = dto;
                    applyEmployeeDetails(dto);
                },
                err -> {
                }
        );
    }

    private void applyEmployeeDetails(ScaleDto dto) {
        txt_empName.setText(dto.getEmpName());
        txt_empNationalId.setText(dto.getNationalId());
        txt_empLawCode.setText(
                dto.getLawCode() != null
                        ? dto.getLawCode().stripTrailingZeros().toPlainString()
                        : "—"
        );
        txt_empLaw.setText(
                dto.getLaw() != null ? dto.getLaw().toString() : "—"
        );
        txt_empStartDate.setText(
                dto.getStartDate() != null
                        ? dto.getStartDate().format(DATE_FMT)
                        : "—"
        );
        txt_empBasic30.setText(
                dto.getBasic30Value() != null
                        ? dto.getBasic30Value().toPlainString()
                        : "—"
        );
        txt_empBasic30From.setText(
                dto.getBasic30From() != null
                        ? dto.getBasic30From().format(DATE_FMT)
                        : "—"
        );
        txt_empGroup.setText(
                dto.getQualitativeGroup() != null
                        ? dto.getQualitativeGroup()
                        : "—"
        );
        // الدرجة من آخر نقطة في الـ timeline
        if (dto.getTimeline() != null && !dto.getTimeline().isEmpty()) {
            txt_empDegree.setText(
                    dto.getTimeline().getLast().degreeLabel()
            );
        }
    }

    // ════════════════════════════════════════════════════════════
    //  ملء جدول البدلات
    // ════════════════════════════════════════════════════════════

    private void fillAllowancesTable(AllowanceResultDto result) {
        table_allowances.setItems(
                FXCollections.observableArrayList(result.allowances())
        );

        // الإجمالي
        lbl_total.setText(
                result.totalAllowances().toPlainString() + " ج"
        );
    }

    // ════════════════════════════════════════════════════════════
    //  إعداد الجدول
    // ════════════════════════════════════════════════════════════

    private void setupTable() {
        col_nameAr.setCellValueFactory(new PropertyValueFactory<>("nameAr"));

        col_value.setCellValueFactory(new PropertyValueFactory<>("value"));
        col_value.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) {
                    setText(null);
                } else {
                    setText(val.toPlainString() + " ج");
                    setStyle("-fx-alignment: CENTER;");
                }
            }
        });

        col_effectiveFrom.setCellValueFactory(new PropertyValueFactory<>("effectiveFrom"));
        col_effectiveFrom.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setText(empty || date == null ? null : date.format(DATE_FMT));
                setStyle("-fx-alignment: CENTER;");
            }
        });

        // عمود المصدر مع Badge ملوّن
        col_source.setCellValueFactory(new PropertyValueFactory<>("source"));
        col_source.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Source src, boolean empty) {
                super.updateItem(src, empty);
                if (empty || src == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label badge = new Label(sourceLabel(src));
                badge.setStyle(sourceBadgeStyle(src));
                badge.setPadding(new Insets(2, 8, 2, 8));
                setGraphic(badge);
                setText(null);
                setStyle("-fx-alignment: CENTER;");
            }
        });

        // عمود الإجراءات
        col_actions.setCellFactory(tc -> new TableCell<>() {
            private final Button btnEdit = new Button("✏ تعديل");
            private final Button btnDelete = new Button("🗑");

            {
                btnEdit.getStyleClass().add("btn-purple");
                btnEdit.setPrefHeight(26);
                btnEdit.setPrefWidth(75);
                btnDelete.getStyleClass().add("btn-danger");
                btnDelete.setPrefHeight(26);
                btnDelete.setPrefWidth(36);

                btnEdit.setOnAction(e -> {
                    AllowanceLineDto item = getTableView().getItems().get(getIndex());
                    openOverrideDialog(item);
                });

                btnDelete.setOnAction(e -> {
                    AllowanceLineDto item = getTableView().getItems().get(getIndex());
                    handleDeleteAllowance(item.code());
                });
            }

            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                AllowanceLineDto item = getTableView().getItems().get(getIndex());
                // EXCLUDED مش قابل للتعديل
                boolean excluded = item.source() == Source.EXCLUDED;
                btnEdit.setDisable(excluded);

                HBox box = new HBox(6, btnEdit, btnDelete);
                box.setStyle("-fx-alignment: CENTER;");
                setGraphic(box);
            }
        });
    }

    private void setupCombo() {
        combo_newAllowance.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(AllowanceDefinition item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getNameAr());
            }
        });
        combo_newAllowance.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(AllowanceDefinition item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "اختر بدل..." : item.getNameAr());
            }
        });
    }

    // ════════════════════════════════════════════════════════════
    //  فتح Dialog تعديل الفترات
    // ════════════════════════════════════════════════════════════

    private void openOverrideDialog(AllowanceLineDto line) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/AllowanceOverrideDialog.fxml")
            );
            Parent root = loader.load();

            AllowanceOverrideDialogController ctrl = loader.getController();
            ctrl.init(line, currentNationalId, () -> loadAllowances());

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("تعديل فترات: " + line.nameAr());
            dialog.setScene(new Scene(root));
            dialog.setResizable(false);
            dialog.showAndWait();

        } catch (Exception ex) {
            showError("خطأ في فتح نافذة التعديل: " + ex.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════
    //  فتح Dialog إدارة قواعد البدلات
    // ════════════════════════════════════════════════════════════

    /**
     * بيفتح شاشة إدارة قواعد البدلات (allowance_definition) — إضافة
     * بدل جديد بالكامل، إضافة snapshot جديد لكود موجود، تعديل أو حذف
     * snapshot. لما الـ dialog يتقفل، بنعيد تحميل قائمة الـ Dropdown
     * ({@link #loadDefinitions()}) عشان تعكس أي تعديل حصل.
     */
    private void openDefinitionsDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/AllowanceDefinitionDialog.fxml")
            );
            Parent root = loader.load();

            AllowanceDefinitionDialogController ctrl = loader.getController();
            ctrl.init(() -> {
                if (currentNationalId != null) loadDefinitions();
            });

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("إدارة قواعد البدلات");
            dialog.setScene(new Scene(root));
            dialog.setResizable(true);
            dialog.showAndWait();

        } catch (Exception ex) {
            showError("خطأ في فتح شاشة إدارة القواعد: " + ex.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════
    //  عمليات API
    // ════════════════════════════════════════════════════════════

    private void handleAddAllowance() {
        AllowanceDefinition selected = combo_newAllowance.getValue();
        if (selected == null) {
            showError("اختر البدل المراد إضافته");
            return;
        }

        // نفتح dialog فارغ لإدخال أول فترة
        AllowanceLineDto dummy = new AllowanceLineDto(
                selected.getCode(),
                selected.getNameAr(),
                BigDecimal.ZERO,
                LocalDate.now(),
                Source.MANUAL,
                List.of()
        );
        openOverrideDialog(dummy);
    }

    private void handleDeleteAllowance(String code) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "هل تريد حذف البدل من قائمة الموظف؟\nيمكن استرجاعه بعد إعادة الضبط.",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("تأكيد الحذف");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                FxApiSupport.delete(
                        "/api/allowances/employee/" + currentNationalId + "/allowance/" + code,
                        this::loadAllowances,
                        this::showError
                );
            }
        });
    }

    private void handleReset() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "إعادة الضبط ستحذف كل التعديلات اليدوية وتعيد البناء التلقائي.",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("تأكيد إعادة الضبط");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                // ملحوظة: إعادة الضبط بتمسح config البدلات بس (allowances
                // JSONB)، وده مش مرتبط بـ cachedScaleDto (بيانات السلم
                // الوظيفي) — فمش محتاجين نمسح الكاش هنا، البيانات لسه صحيحة.
                FxApiSupport.delete(
                        "/api/allowances/employee/" + currentNationalId + "/reset",
                        this::loadAllowances,
                        this::showError
                );
            }
        });
    }

    // ════════════════════════════════════════════════════════════
    //  Helpers
    // ════════════════════════════════════════════════════════════

    private String sourceLabel(Source src) {
        return switch (src) {
            case AUTO -> "تلقائي";
            case MANUAL -> "يدوي";
            case EXCLUDED -> "مستثنى";
        };
    }

    private String sourceBadgeStyle(Source src) {
        String base = "-fx-background-radius:4; -fx-font-weight:bold; -fx-text-fill:white; -fx-font-size:11;";
        return base + switch (src) {
            case AUTO -> "-fx-background-color:#607d8b;";
            case MANUAL -> "-fx-background-color:#1976d2;";
            case EXCLUDED -> "-fx-background-color:#e65100;";
        };
    }

    private void showLoading(boolean show) {
        progress_indicator.setVisible(show);
        btn_search.setDisable(show);
    }

    private void showError(String msg) {
        lbl_error.setText(msg);
        lbl_error.setVisible(true);
    }

    private void clearError() {
        lbl_error.setText("");
        lbl_error.setVisible(false);
    }
}