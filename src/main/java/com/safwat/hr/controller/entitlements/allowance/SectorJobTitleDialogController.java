package com.safwat.hr.controller.entitlements.allowance;

import com.fasterxml.jackson.core.type.TypeReference;
import com.safwat.hr.controller.entitlements.allowance.dto.JobTitleDto;
import com.safwat.hr.controller.entitlements.allowance.dto.SectorDto;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class SectorJobTitleDialogController implements Initializable {

    // ── Master ──
    @FXML
    private TableView<SectorDto> table_sectors;
    @FXML
    private TableColumn<SectorDto, String> col_sector_code;
    @FXML
    private TableColumn<SectorDto, String> col_sector_nameAr;
    @FXML
    private TableColumn<SectorDto, Integer> col_sector_order;
    @FXML
    private TableColumn<SectorDto, Boolean> col_sector_active;
    @FXML
    private Button btn_add_sector;
    @FXML
    private Button btn_edit_sector;
    @FXML
    private Button btn_delete_sector;

    // ── Detail ──
    @FXML
    private Label lbl_jobs_title;
    @FXML
    private TableView<JobTitleDto> table_jobs;
    @FXML
    private TableColumn<JobTitleDto, String> col_job_code;
    @FXML
    private TableColumn<JobTitleDto, String> col_job_nameAr;
    @FXML
    private TableColumn<JobTitleDto, String> col_job_nameEn;
    @FXML
    private TableColumn<JobTitleDto, Boolean> col_job_law81;
    @FXML
    private TableColumn<JobTitleDto, Integer> col_job_order;
    @FXML
    private TableColumn<JobTitleDto, Boolean> col_job_active;
    @FXML
    private Button btn_add_job;
    @FXML
    private Button btn_edit_job;
    @FXML
    private Button btn_delete_job;

    // ── Sector Form ──
    @FXML
    private VBox pane_sector_form;
    @FXML
    private Label lbl_sector_form_title;
    @FXML
    private TextField txt_sector_code;
    @FXML
    private TextField txt_sector_nameAr;
    @FXML
    private TextField txt_sector_nameEn;
    @FXML
    private TextField txt_sector_order;
    @FXML
    private CheckBox chk_sector_active;
    @FXML
    private Button btn_save_sector;
    @FXML
    private Button btn_cancel_sector;
    @FXML
    private Label lbl_sector_error;

    // ── Job Form ──
    @FXML
    private VBox pane_job_form;
    @FXML
    private Label lbl_job_form_title;
    @FXML
    private TextField txt_job_code;
    @FXML
    private TextField txt_job_nameAr;
    @FXML
    private TextField txt_job_nameEn;
    @FXML
    private TextField txt_job_order;
    @FXML
    private CheckBox chk_job_law81;
    @FXML
    private CheckBox chk_job_active;
    @FXML
    private Button btn_save_job;
    @FXML
    private Button btn_cancel_job;
    @FXML
    private Label lbl_job_error;

    // ── Footer ──
    @FXML
    private Label lbl_global_msg;
    @FXML
    private Button btn_close;

    // ── State ──
    private Long editingSectorId = null;
    private Long editingJobId = null;
    private SectorDto selectedSector = null;

    // ══════════════════════════════════════════════════════════════
    //  Initialize
    // ══════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupSectorTable();
        setupJobTable();

        btn_add_sector.setOnAction(e -> showSectorForm(null));
        btn_edit_sector.setOnAction(e -> showSectorForm(table_sectors.getSelectionModel().getSelectedItem()));
        btn_delete_sector.setOnAction(e -> handleDeleteSector());
        btn_cancel_sector.setOnAction(e -> hideSectorForm());
        btn_save_sector.setOnAction(e -> handleSaveSector());

        btn_add_job.setOnAction(e -> showJobForm(null));
        btn_edit_job.setOnAction(e -> showJobForm(table_jobs.getSelectionModel().getSelectedItem()));
        btn_delete_job.setOnAction(e -> handleDeleteJob());
        btn_cancel_job.setOnAction(e -> hideJobForm());
        btn_save_job.setOnAction(e -> handleSaveJob());

        btn_close.setOnAction(e -> closeDialog());

        loadSectors();
    }

    // ══════════════════════════════════════════════════════════════
    //  Tables
    // ══════════════════════════════════════════════════════════════

    private void setupSectorTable() {
        col_sector_code.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getCode()));
        col_sector_nameAr.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getNameAr()));
        col_sector_order.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getDisplayOrder()));
        col_sector_active.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getActive()));

        col_sector_active.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? "—" : (v ? "✔" : "✗"));
                setStyle("-fx-alignment: CENTER; -fx-text-fill:" + (Boolean.TRUE.equals(v) ? "#2e7d32" : "#c62828") + ";");
            }
        });
        col_sector_order.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Integer v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? "—" : String.valueOf(v));
                setStyle("-fx-alignment: CENTER;");
            }
        });

        table_sectors.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            selectedSector = sel;
            btn_edit_sector.setDisable(sel == null);
            btn_delete_sector.setDisable(sel == null);
            btn_add_job.setDisable(sel == null);

            if (sel != null) {
                lbl_jobs_title.setText("وظائف القطاع: " + sel.getNameAr());
                loadJobs(sel.getCode());
            } else {
                lbl_jobs_title.setText("وظائف القطاع (اختر قطاعاً أولاً)");
                table_jobs.getItems().clear();
            }
        });
    }

    private void setupJobTable() {
        col_job_code.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getCode()));

        // 🆕 عرض الاسم المجرّد فقط (بدون القطاع)
        col_job_nameAr.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getDisplayName()));

        col_job_nameEn.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getNameEn() != null ? d.getValue().getNameEn() : "—"));
        col_job_order.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getDisplayOrder()));
        col_job_active.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getActive()));
        col_job_law81.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getSubjectToLaw81()));

        col_job_law81.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? "—" : (v ? "✔ خاضع" : "✗ غير خاضع"));
                setStyle("-fx-alignment: CENTER; -fx-text-fill:" + (Boolean.TRUE.equals(v) ? "#6a1b9a" : "#888") + ";");
            }
        });
        col_job_active.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? "—" : (v ? "✔" : "✗"));
                setStyle("-fx-alignment: CENTER; -fx-text-fill:" + (Boolean.TRUE.equals(v) ? "#2e7d32" : "#c62828") + ";");
            }
        });
        col_job_order.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Integer v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? "—" : String.valueOf(v));
                setStyle("-fx-alignment: CENTER;");
            }
        });

        table_jobs.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            btn_edit_job.setDisable(sel == null);
            btn_delete_job.setDisable(sel == null);
        });
    }

    // ══════════════════════════════════════════════════════════════
    //  Load Data
    // ══════════════════════════════════════════════════════════════

    private void loadSectors() {
        FxApiSupport.getList("/sectors",
                new TypeReference<List<SectorDto>>() {
                },
                list -> {
                    table_sectors.setItems(FXCollections.observableArrayList(list));
                    if (!list.isEmpty()) {
                        table_sectors.getSelectionModel().selectFirst();
                    }
                },
                err -> showGlobalMsg("خطأ في تحميل القطاعات: " + err, true));
    }

    private void loadJobs(String sectorCode) {
        if (sectorCode == null || sectorCode.isBlank()) {
            table_jobs.getItems().clear();
            return;
        }
        FxApiSupport.getList("/sectors/" + sectorCode + "/jobs",
                new TypeReference<List<JobTitleDto>>() {
                },
                list -> table_jobs.setItems(FXCollections.observableArrayList(list)),
                err -> showGlobalMsg("خطأ في تحميل الوظائف: " + err, true));
    }

    // ══════════════════════════════════════════════════════════════
    //  Sector Form
    // ══════════════════════════════════════════════════════════════

    private void showSectorForm(SectorDto sector) {
        hideJobForm();
        lbl_sector_error.setVisible(false);

        if (sector == null) {
            editingSectorId = null;
            lbl_sector_form_title.setText("➕ قطاع جديد");
            txt_sector_code.clear();
            txt_sector_code.setDisable(false);
            txt_sector_nameAr.clear();
            txt_sector_nameEn.clear();
            txt_sector_order.setText("100");
            chk_sector_active.setSelected(true);
        } else {
            editingSectorId = sector.getId();
            lbl_sector_form_title.setText("✏ تعديل: " + sector.getNameAr());
            txt_sector_code.setText(sector.getCode());
            txt_sector_code.setDisable(false);
            txt_sector_nameAr.setText(sector.getNameAr());
            txt_sector_nameEn.setText(sector.getNameEn() != null ? sector.getNameEn() : "");
            txt_sector_order.setText(sector.getDisplayOrder() != null
                    ? String.valueOf(sector.getDisplayOrder()) : "100");
            chk_sector_active.setSelected(Boolean.TRUE.equals(sector.getActive()));
        }

        pane_sector_form.setVisible(true);
        pane_sector_form.setManaged(true);
        Platform.runLater(() -> txt_sector_code.requestFocus());
    }

    private void hideSectorForm() {
        pane_sector_form.setVisible(false);
        pane_sector_form.setManaged(false);
        editingSectorId = null;
    }

    private void handleSaveSector() {
        lbl_sector_error.setVisible(false);

        String code = txt_sector_code.getText() != null ? txt_sector_code.getText().trim() : "";
        String nameAr = txt_sector_nameAr.getText() != null ? txt_sector_nameAr.getText().trim() : "";
        String nameEn = txt_sector_nameEn.getText() != null ? txt_sector_nameEn.getText().trim() : "";

        if (code.isBlank()) {
            showSectorError("كود القطاع مطلوب");
            return;
        }
        if (nameAr.isBlank()) {
            showSectorError("اسم القطاع بالعربي مطلوب");
            return;
        }

        Integer order = 100;
        try {
            if (!txt_sector_order.getText().isBlank())
                order = Integer.parseInt(txt_sector_order.getText().trim());
        } catch (NumberFormatException ex) {
            showSectorError("الترتيب لازم يكون رقماً");
            return;
        }

        SectorRequest req = new SectorRequest(
                code, nameAr,
                nameEn.isBlank() ? null : nameEn,
                order,
                chk_sector_active.isSelected(),
                null);

        btn_save_sector.setDisable(true);

        if (editingSectorId == null) {
            FxApiSupport.post("/sectors", req, Boolean.class,
                    saved -> onSectorSaved(),
                    err -> onSectorError(err));
        } else {
            FxApiSupport.put("/sectors/" + editingSectorId, req, Boolean.class,
                    saved -> onSectorSaved(),
                    err -> onSectorError(err));
        }
    }

    private void onSectorSaved() {
        btn_save_sector.setDisable(false);
        hideSectorForm();
        showGlobalMsg("✅ تم الحفظ بنجاح", false);
        loadSectors();
    }

    private void onSectorError(String err) {
        btn_save_sector.setDisable(false);
        showSectorError("خطأ: " + err);
    }

    private void handleDeleteSector() {
        SectorDto sel = table_sectors.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "حذف القطاع \"" + sel.getNameAr() + "\"؟\n" +
                        "⚠ لا يمكن الحذف إذا كان هناك وظائف مرتبطة به.",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("تأكيد الحذف");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                FxApiSupport.delete("/sectors/" + sel.getId(),
                        () -> {
                            showGlobalMsg("🗑 تم حذف القطاع", false);
                            loadSectors();
                        },
                        err -> showGlobalMsg("خطأ: " + err, true));
            }
        });
    }

    // ══════════════════════════════════════════════════════════════
    //  Job Form
    // ══════════════════════════════════════════════════════════════

    private void showJobForm(JobTitleDto job) {
        if (selectedSector == null) {
            showGlobalMsg("اختر قطاعاً أولاً", true);
            return;
        }
        hideSectorForm();
        lbl_job_error.setVisible(false);

        if (job == null) {
            editingJobId = null;
            lbl_job_form_title.setText("➕ وظيفة جديدة في: " + selectedSector.getNameAr());
            txt_job_code.clear();
            txt_job_nameAr.clear();
            txt_job_nameEn.clear();
            txt_job_order.setText("100");
            chk_job_law81.setSelected(false);
            chk_job_active.setSelected(true);
        } else {
            editingJobId = job.getId();
            // 🆕 العنوان يعرض الاسم المجرّد
            lbl_job_form_title.setText("✏ تعديل: " + job.getDisplayName());
            txt_job_code.setText(job.getCode());
            // 🆕 الـ TextField يعرض الاسم المجرّد (بدون القطاع)
            txt_job_nameAr.setText(job.getDisplayName());
            txt_job_nameEn.setText(job.getNameEn() != null ? job.getNameEn() : "");
            txt_job_order.setText(job.getDisplayOrder() != null
                    ? String.valueOf(job.getDisplayOrder()) : "100");
            chk_job_law81.setSelected(Boolean.TRUE.equals(job.getSubjectToLaw81()));
            chk_job_active.setSelected(Boolean.TRUE.equals(job.getActive()));
        }

        pane_job_form.setVisible(true);
        pane_job_form.setManaged(true);
        Platform.runLater(() -> txt_job_code.requestFocus());
    }

    private void hideJobForm() {
        pane_job_form.setVisible(false);
        pane_job_form.setManaged(false);
        editingJobId = null;
    }

    private void handleSaveJob() {
        lbl_job_error.setVisible(false);

        if (selectedSector == null) {
            showJobError("اختر قطاعاً أولاً");
            return;
        }

        String code = txt_job_code.getText() != null ? txt_job_code.getText().trim() : "";
        String nameAr = txt_job_nameAr.getText() != null ? txt_job_nameAr.getText().trim() : "";
        String nameEn = txt_job_nameEn.getText() != null ? txt_job_nameEn.getText().trim() : "";

        if (code.isBlank()) {
            showJobError("كود الوظيفة مطلوب");
            return;
        }
        if (nameAr.isBlank()) {
            showJobError("اسم الوظيفة بالعربي مطلوب");
            return;
        }

        Integer order = 100;
        try {
            if (!txt_job_order.getText().isBlank())
                order = Integer.parseInt(txt_job_order.getText().trim());
        } catch (NumberFormatException ex) {
            showJobError("الترتيب لازم يكون رقماً");
            return;
        }

        JobTitleRequest req = new JobTitleRequest(
                code, nameAr,
                nameEn.isBlank() ? null : nameEn,
                chk_job_law81.isSelected(),
                order,
                chk_job_active.isSelected(),
                null);

        btn_save_job.setDisable(true);

        String base = "/sectors/" + selectedSector.getCode() + "/jobs";
        if (editingJobId == null) {
            FxApiSupport.post(base, req, Boolean.class,
                    saved -> onJobSaved(),
                    err -> onJobError(err));
        } else {
            FxApiSupport.put(base + "/" + editingJobId, req, Boolean.class,
                    saved -> onJobSaved(),
                    err -> onJobError(err));
        }
    }

    private void onJobSaved() {
        btn_save_job.setDisable(false);
        hideJobForm();
        showGlobalMsg("✅ تم الحفظ بنجاح", false);
        if (selectedSector != null) loadJobs(selectedSector.getCode());
    }

    private void onJobError(String err) {
        btn_save_job.setDisable(false);
        showJobError("خطأ: " + err);
    }

    private void handleDeleteJob() {
        JobTitleDto sel = table_jobs.getSelectionModel().getSelectedItem();
        if (sel == null || selectedSector == null) return;

        // 🆕 نعرض الاسم المجرّد في رسالة التأكيد
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "حذف الوظيفة \"" + sel.getDisplayName() + "\"؟",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("تأكيد الحذف");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                FxApiSupport.delete(
                        "/sectors/" + selectedSector.getCode() + "/jobs/" + sel.getId(),
                        () -> {
                            showGlobalMsg("🗑 تم حذف الوظيفة", false);
                            loadJobs(selectedSector.getCode());
                        },
                        err -> showGlobalMsg("خطأ: " + err, true));
            }
        });
    }

    // ══════════════════════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════════════════════

    private void showSectorError(String msg) {
        lbl_sector_error.setText(msg);
        lbl_sector_error.setVisible(true);
    }

    private void showJobError(String msg) {
        lbl_job_error.setText(msg);
        lbl_job_error.setVisible(true);
    }

    private void showGlobalMsg(String msg, boolean error) {
        lbl_global_msg.setText(msg);
        lbl_global_msg.setStyle(error
                ? "-fx-text-fill:#c62828; -fx-font-weight:bold;"
                : "-fx-text-fill:#2e7d32; -fx-font-weight:bold;");
        lbl_global_msg.setVisible(true);

        if (!error) {
            PauseTransition pause = new PauseTransition(Duration.seconds(4));
            pause.setOnFinished(e -> lbl_global_msg.setVisible(false));
            pause.play();
        }
    }

    private void closeDialog() {
        ((Stage) btn_close.getScene().getWindow()).close();
    }

    // ══════════════════════════════════════════════════════════════
    //  Request DTOs (نفس حقول الباك)
    // ══════════════════════════════════════════════════════════════

    public record SectorRequest(
            String code, String nameAr, String nameEn,
            Integer displayOrder, Boolean active, String notes) {
    }

    public record JobTitleRequest(
            String code, String nameAr, String nameEn,
            Boolean subjectToLaw81, Integer displayOrder,
            Boolean active, String notes) {
    }
}