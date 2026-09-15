package com.safwat.hr.controller.allowance;


import com.fasterxml.jackson.core.type.TypeReference;

import com.safwat.hr.controller.allowance.AllowanceResultDto.ElementType;
import com.safwat.hr.controller.scale.scale.dto.ScaleDto;
import com.safwat.hr.controller.statutory.StatutoryDialogController;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller الرئيسي لشاشة البدلات.
 *
 * <p>مسؤول عن:
 * <ul>
 *   <li>البحث عن موظف وتحميل بياناته</li>
 *   <li>عرض بيانات الموظف في تاب 1</li>
 *   <li>عرض وإدارة البدلات في تاب 2 (مع تمييز الاستحقاقات/الاستقطاعات)</li>
 *   <li>فتح dialog تعديل الفترات</li>
 *   <li>فتح dialog إدارة قواعد البدلات (allowance_definition)</li>
 *   <li>فتح dialog إدارة الاستقطاعات القانونية (تأمينات/ضريبة/دمغة)</li>
 * </ul>
 *
 * <p><b>عرض جدول البدلات:</b> كل سطر فيه نوع العنصر (استحقاق / استقطاع /
 * تأمينات / ضريبة / دمغة)، والقيمة بتُعرض <b>بالقيمة المطلقة (abs)</b>
 * مع لون مميز — أحمر للاستقطاعات وأخضر للاستحقاقات.
 * ترتيب السطور: استحقاقات فوق، استقطاعات تحت، عناصر العرض (حصة الحكومة)
 * في الآخر. الإجمالي (الصافي) بيتحسب من الباك على القيم بإشاراتها الأصلية.
 */
public class AllowanceFxController implements Initializable {

    // ── شريط البحث ──────────────────────────────────────────────
    @FXML
    private Button btn_statutory;
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
    private TableView<AllowanceResultDto.AllowanceLineDto> table_allowances;
    @FXML
    private TableColumn<AllowanceResultDto.AllowanceLineDto, String> col_nameAr;
    @FXML
    private TableColumn<AllowanceResultDto.AllowanceLineDto, ElementType> col_elementType;
    @FXML
    private TableColumn<AllowanceResultDto.AllowanceLineDto, BigDecimal> col_value;
    @FXML
    private TableColumn<AllowanceResultDto.AllowanceLineDto, LocalDate> col_effectiveFrom;
    @FXML
    private TableColumn<AllowanceResultDto.AllowanceLineDto, AllowanceResultDto.AllowanceLineDto.Source> col_source;
    @FXML
    private TableColumn<AllowanceResultDto.AllowanceLineDto, Void> col_actions;

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

        btn_statutory.setOnAction(e -> openStatutoryDialog());

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
        if (!id.equals(currentNationalId)) {
            cachedScaleDto = null;
            currentResult = null;
        }

        currentNationalId = id;
        loadAllowances();
    }

    /**
     * إعادة احتساب البدلات بالتاريخ الحالي في date_calculation.
     */
    private void loadAllowances() {
        if (currentNationalId == null) return;

        String dateParam = date_calculation.getValue() != null
                ? "?date=" + date_calculation.getValue().format(DATE_FMT)
                : "";

        showLoading(true);
        clearError();

        FxApiSupport.get(
                "/allowances/employee/" + currentNationalId + dateParam,
                AllowanceResultDto.class,
                result -> {
                    showLoading(false);
                    currentResult = result;
                    fillAllowancesTable(result);
                    btn_reset.setVisible(true);
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
                "/allowances/definitions",
                new TypeReference<List<AllowanceDefinition>>() {
                },
                defs -> {
                    // فلتر البدلات اللي مش موجودة عند الموظف
                    List<String> existing = currentResult != null
                            ? currentResult.allowances().stream()
                            .map(AllowanceResultDto.AllowanceLineDto::code).toList()
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

    private void ensureEmployeeDetailsLoaded() {
        if (cachedScaleDto != null) {
            applyEmployeeDetails(cachedScaleDto);
            return;
        }

        FxApiSupport.get(
                "/salary-scale/" + currentNationalId,
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
        if (dto.getTimeline() != null && !dto.getTimeline().isEmpty()) {
            txt_empDegree.setText(
                    dto.getTimeline().getLast().getDegreeLabel()
            );
        }
    }

    // ════════════════════════════════════════════════════════════
    //  ملء جدول البدلات
    // ════════════════════════════════════════════════════════════

    /**
     * ترتيب السطور: استحقاقات → استقطاعات → عناصر عرض (حكومة).
     * القيم بإشاراتها الأصلية في الـ DTO، والواجهة هي اللي بتعرض abs.
     */
    private void fillAllowancesTable(AllowanceResultDto result) {
        List<AllowanceResultDto.AllowanceLineDto> sorted = result.allowances().stream()
                .sorted(Comparator
                        .comparingInt(this::sortPriority)
                        .thenComparing(AllowanceResultDto.AllowanceLineDto::nameAr))
                .toList();

        table_allowances.setItems(FXCollections.observableArrayList(sorted));

        lbl_total.setText(result.totalAllowances().toPlainString() + " ج");
    }

    // ════════════════════════════════════════════════════════════
    //  إعداد الجدول
    // ════════════════════════════════════════════════════════════

    private void setupTable() {
        // ═══ اسم البدل ═══
        col_nameAr.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(d.getValue().nameAr()));

        // ═══ عمود النوع — badge ملوّن ═══
        col_elementType.setCellValueFactory(d ->
                new javafx.beans.property.SimpleObjectProperty<>(d.getValue().elementType()));
        col_elementType.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(ElementType et, boolean empty) {
                super.updateItem(et, empty);
                if (empty || et == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label badge = new Label(elementTypeLabel(et));
                badge.setStyle(elementTypeBadgeStyle(et));
                badge.setPadding(new Insets(2, 8, 2, 8));
                setGraphic(badge);
                setText(null);
                setStyle("-fx-alignment: CENTER;");
            }
        });

        // ═══ عمود القيمة — abs + لون حسب النوع ═══
        col_value.setCellValueFactory(d ->
                new javafx.beans.property.SimpleObjectProperty<>(d.getValue().value()));
        col_value.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) {
                    setText(null);
                    setStyle("-fx-alignment: CENTER;");
                    return;
                }
                AllowanceResultDto.AllowanceLineDto line =
                        getTableView().getItems().get(getIndex());
                setText(val.abs().toPlainString() + " ج");
                setStyle("-fx-alignment: CENTER; " + valueStyle(line));
            }
        });

        // ═══ من تاريخ ═══
        col_effectiveFrom.setCellValueFactory(d ->
                new javafx.beans.property.SimpleObjectProperty<>(d.getValue().effectiveFrom()));
        col_effectiveFrom.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setText(empty || date == null ? null : date.format(DATE_FMT));
                setStyle("-fx-alignment: CENTER;");
            }
        });

        // ═══ المصدر — badge ملوّن ═══
        col_source.setCellValueFactory(d ->
                new javafx.beans.property.SimpleObjectProperty<>(d.getValue().source()));
        col_source.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(AllowanceResultDto.AllowanceLineDto.Source src, boolean empty) {
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

        // ═══ الإجراءات ═══
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
                    AllowanceResultDto.AllowanceLineDto item = getTableView().getItems().get(getIndex());
                    openOverrideDialog(item);
                });

                btnDelete.setOnAction(e -> {
                    AllowanceResultDto.AllowanceLineDto item = getTableView().getItems().get(getIndex());
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
                AllowanceResultDto.AllowanceLineDto item = getTableView().getItems().get(getIndex());

                boolean excluded = item.source() == AllowanceResultDto.AllowanceLineDto.Source.EXCLUDED;
                boolean displayOnly = item.displayOnly();

                // الأزرار معطّلة للـ EXCLUDED و displayOnly
                btnEdit.setDisable(excluded || displayOnly);
                btnDelete.setDisable(displayOnly);

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

    private void openOverrideDialog(AllowanceResultDto.AllowanceLineDto line) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/safwat/hr/controller/scale/allowance/AllowanceOverrideDialog.fxml")
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

    private void openDefinitionsDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/safwat/hr/controller/scale/allowance/AllowanceDefinitionDialog.fxml")
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

    private void openStatutoryDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/safwat/hr/controller/scale/allowance/StatutoryDialog.fxml"));
            Parent root = loader.load();

            StatutoryDialogController controller = loader.getController();
            controller.init(() -> {
                // لو فيه حاجة محتاجة تتحدث في الشاشة الرئيسية بعد الحفظ
            });

            Stage dialog = new Stage();
            dialog.setTitle("إدارة الاستقطاعات القانونية");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(new Scene(root));
            dialog.showAndWait();
        } catch (IOException ex) {
            showError("خطأ في فتح شاشة الاستقطاعات: " + ex.getMessage());
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

        // نفتح dialog فارغ لإدخال أول فترة — allow الدالة دي مش هتنفع
        // للـ statutory elements (تأمينات/ضريبة/دمغة) لأن دي مش بتتضاف يدوي.
        AllowanceResultDto.AllowanceLineDto dummy = new AllowanceResultDto.AllowanceLineDto(
                selected.getCode(),
                selected.getNameAr(),
                BigDecimal.ZERO,
                LocalDate.now(),
                AllowanceResultDto.AllowanceLineDto.Source.MANUAL,
                List.of(),
                ElementType.ENTITLEMENT,
                false,
                false,
                false
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
                        "/allowances/employee/" + currentNationalId + "/allowance/" + code,
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
                FxApiSupport.delete(
                        "/allowances/employee/" + currentNationalId + "/reset",
                        this::loadAllowances,
                        this::showError
                );
            }
        });
    }

    // ════════════════════════════════════════════════════════════
    //  Helpers — عرض
    // ════════════════════════════════════════════════════════════

    /**
     * ترتيب العرض:
     * 1 = استحقاقات (ENTITLEMENT)
     * 2 = استقطاعات (DEDUCTION / INSURANCE / TAX / STAMP)
     * 3 = عناصر عرض فقط (displayOnly — حصة الحكومة)
     */
    private int sortPriority(AllowanceResultDto.AllowanceLineDto line) {
        if (line.displayOnly()) return 3;
        if (line.elementType() == ElementType.ENTITLEMENT) return 1;
        return 2;
    }

    /**
     * لون القيمة:
     * - استحقاق: أخضر غامق
     * - استقطاع: أحمر غامق
     * - عرض فقط (حكومة): رمادي italic
     */
    private String valueStyle(AllowanceResultDto.AllowanceLineDto line) {
        if (line.displayOnly()) {
            return "-fx-text-fill:#888; -fx-font-style:italic;";
        }
        if (line.value() != null && line.value().signum() < 0) {
            return "-fx-text-fill:#c62828; -fx-font-weight:bold;";
        }
        return "-fx-text-fill:#2e7d32; -fx-font-weight:bold;";
    }

    /**
     * التسمية العربية لنوع العنصر.
     */
    private String elementTypeLabel(ElementType et) {
        return switch (et) {
            case ENTITLEMENT -> "استحقاق";
            case DEDUCTION -> "استقطاع";
            case INSURANCE -> "تأمينات";
            case TAX -> "ضريبة";
            case STAMP -> "دمغة";
        };
    }

    /**
     * ستايل Badge النوع — لون لكل نوع.
     */
    private String elementTypeBadgeStyle(ElementType et) {
        String base = "-fx-background-radius:4; -fx-font-weight:bold; -fx-text-fill:white; -fx-font-size:11;";
        return base + switch (et) {
            case ENTITLEMENT -> "-fx-background-color:#2e7d32;";  // أخضر
            case DEDUCTION -> "-fx-background-color:#c62828;";  // أحمر
            case INSURANCE -> "-fx-background-color:#1565c0;";  // أزرق
            case TAX -> "-fx-background-color:#6a1b9a;";  // بنفسجي
            case STAMP -> "-fx-background-color:#e65100;";  // برتقالي
        };
    }

    private String sourceLabel(AllowanceResultDto.AllowanceLineDto.Source src) {
        return switch (src) {
            case AUTO -> "تلقائي";
            case MANUAL -> "يدوي";
            case EXCLUDED -> "مستثنى";
        };
    }

    private String sourceBadgeStyle(AllowanceResultDto.AllowanceLineDto.Source src) {
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