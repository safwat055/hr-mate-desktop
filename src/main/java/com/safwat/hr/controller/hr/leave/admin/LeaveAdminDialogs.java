package com.safwat.hr.controller.hr.leave.admin;

import com.safwat.hr.controller.hr.leave.admin.dto.LeaveAdminDtos.*;
import com.safwat.hr.controller.hr.leave.admin.service.LeaveAdminApiClient;
import com.safwat.hr.controller.hr.leave.dto.LeaveTypeDto;
import com.safwat.hr.controller.hr.leave.util.LeaveLabels;
import com.safwat.hr.ui.UiAsync;
import com.safwat.hr.ui.controls.SAFNotification;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public final class LeaveAdminDialogs {

    private static final List<String> CATEGORIES =
            List.of("BALANCED", "PERIODIC", "MILITARY", "SICK", "ABSENCE", "UNPAID");
    private static final List<String> GENDER_RESTRICTIONS = List.of("NONE", "FEMALE_ONLY");
    private static final List<String> PAY_MODES = List.of("PAID", "UNPAID");
    private static final List<String> POOLS = List.of("", "ANNUAL", "CASUAL");
    private static final List<String> GENDER_CONDITIONS = List.of("", "MALE", "FEMALE");

    private LeaveAdminDialogs() {
    }

    // ═══════════════════════════════════════════════════════════
    //  نوع الإجازة
    // ═══════════════════════════════════════════════════════════

    public static Optional<LeaveTypeAdminRequest> typeForm(Window owner, LeaveTypeDto existing) {
        Dialog<LeaveTypeAdminRequest> d = dialog(owner,
                existing == null ? "نوع إجازة جديد" : "تعديل نوع الإجازة");

        TextField code = new TextField(existing == null ? "" : existing.getCode());
        code.setDisable(existing != null);
        code.setPromptText("مثال: FAMILY_CARE");

        TextField name = new TextField(existing == null ? "" : existing.getNameAr());
        ComboBox<String> category = combo(CATEGORIES, existing == null ? "BALANCED" : existing.getCategory());
        ComboBox<String> gender = combo(GENDER_RESTRICTIONS,
                existing == null ? "NONE" : existing.getGenderRestriction());

        CheckBox openEnded = new CheckBox("مفتوحة النهاية");
        openEnded.setSelected(existing != null && existing.isOpenEnded());
        CheckBox overlap = new CheckBox("يسمح بالتداخل");
        overlap.setSelected(existing != null && existing.isOverlapAllowed());
        CheckBox active = new CheckBox("مفعّل");
        active.setSelected(existing == null || existing.isActive());

        GridPane g = grid();
        row(g, 0, "الكود", code);
        row(g, 1, "الاسم بالعربي", name);
        row(g, 2, "الفئة", category);
        row(g, 3, "قيد الجنس", gender);
        g.add(openEnded, 1, 4);
        g.add(overlap, 1, 5);
        g.add(active, 1, 6);
        d.getDialogPane().setContent(g);

        Supplier<LeaveTypeAdminRequest> build = () -> {
            if (name.getText() == null || name.getText().isBlank()) {
                throw new IllegalArgumentException("الاسم مطلوب");
            }
            LeaveTypeAdminRequest r = new LeaveTypeAdminRequest();
            r.setCode(code.getText().trim().toUpperCase());
            r.setNameAr(name.getText().trim());
            r.setCategory(category.getValue());
            r.setGenderRestriction(gender.getValue());
            r.setOpenEnded(openEnded.isSelected());
            r.setOverlapAllowed(overlap.isSelected());
            r.setActive(active.isSelected());
            return r;
        };

        guard(d, build::get);
        d.setResultConverter(bt -> bt == ButtonType.OK ? build.get() : null);
        return d.showAndWait();
    }

    // ═══════════════════════════════════════════════════════════
    //  القانون
    // ═══════════════════════════════════════════════════════════

    public static Optional<LeaveLawRequest> lawForm(Window owner, LeaveLawDto existing) {
        Dialog<LeaveLawRequest> d = dialog(owner,
                existing == null ? "قانون جديد" : "تعديل القانون");

        TextField code = new TextField(existing == null ? "" : existing.getCode());
        code.setDisable(existing != null);
        code.setPromptText("مثال: 155");

        TextField name = new TextField(existing == null ? "" : existing.getNameAr());
        TextField from = new TextField(existing == null ? "" : str(existing.getEffectiveFrom()));
        from.setPromptText("yyyy-MM-dd");
        TextField startMonth = new TextField(existing == null ? "7" : String.valueOf(existing.getYearStartMonth()));
        TextField startDay = new TextField(existing == null ? "1" : String.valueOf(existing.getYearStartDay()));

        GridPane g = grid();
        row(g, 0, "الكود", code);
        row(g, 1, "الاسم", name);
        row(g, 2, "تاريخ النفاذ", from);
        row(g, 3, "بداية السنة — الشهر", startMonth);
        row(g, 4, "بداية السنة — اليوم", startDay);
        d.getDialogPane().setContent(g);

        Supplier<LeaveLawRequest> build = () -> {
            if (name.getText() == null || name.getText().isBlank()) {
                throw new IllegalArgumentException("الاسم مطلوب");
            }
            LeaveLawRequest r = new LeaveLawRequest();
            r.setCode(code.getText().trim().toUpperCase());
            r.setNameAr(name.getText().trim());
            r.setEffectiveFrom(parseDate(from.getText(), "تاريخ النفاذ"));
            r.setYearStartMonth(requireInt(startMonth.getText(), "شهر بداية السنة"));
            r.setYearStartDay(requireInt(startDay.getText(), "يوم بداية السنة"));
            return r;
        };

        guard(d, build::get);
        d.setResultConverter(bt -> bt == ButtonType.OK ? build.get() : null);
        return d.showAndWait();
    }

    // ═══════════════════════════════════════════════════════════
    //  جدول الاستحقاق
    // ═══════════════════════════════════════════════════════════

    public static Optional<AccrualScheduleRequest> scheduleForm(Window owner, LeaveLawDto law) {
        Dialog<AccrualScheduleRequest> d = dialog(owner, "جدول استحقاق جديد — " + law.getNameAr());

        TextField from = new TextField();
        from.setPromptText("yyyy-MM-dd");
        TextField pool = new TextField("ANNUAL");

        List<RuleRow> rows = new ArrayList<>();
        VBox rowsBox = new VBox(6);
        addRow(rows, rowsBox);

        Button addBtn = new Button("+ صف");
        addBtn.setOnAction(e -> addRow(rows, rowsBox));

        Button template = new Button("قالب ق 81");
        template.setOnAction(e -> {
            rows.clear();
            rowsBox.getChildren().clear();
            addRow(rows, rowsBox).set(10, true, null, null, "", 45);
            addRow(rows, rowsBox).set(20, false, 50, null, "", 45);
            addRow(rows, rowsBox).set(30, false, null, 120, "", 30);
            addRow(rows, rowsBox).set(40, false, null, 12, "", 21);
            addRow(rows, rowsBox).set(50, false, null, 6, "", 15);
            addRow(rows, rowsBox).set(99, false, null, null, "", 0);
        });

        GridPane top = grid();
        row(top, 0, "تاريخ السريان", from);
        row(top, 1, "الرصيد", pool);

        VBox content = new VBox(10, top, new HBox(8, addBtn, template), rowsBox);
        content.setPadding(new Insets(12));
        d.getDialogPane().setContent(content);
        d.getDialogPane().setPrefWidth(720);

        Supplier<AccrualScheduleRequest> build = () -> {
            AccrualScheduleRequest r = new AccrualScheduleRequest();
            r.setEffectiveFrom(parseDate(from.getText(), "تاريخ السريان"));

            String p = pool.getText() == null ? "" : pool.getText().trim().toUpperCase();
            r.setPoolCode(p.isEmpty() ? "ANNUAL" : p);

            if (rows.isEmpty()) throw new IllegalArgumentException("لازم صف واحد على الأقل");
            List<AccrualRuleItem> items = new ArrayList<>();
            for (RuleRow row : rows) items.add(row.toItem());
            r.setRules(items);
            return r;
        };

        guard(d, build::get);
        d.setResultConverter(bt -> bt == ButtonType.OK ? build.get() : null);
        return d.showAndWait();
    }

    // ═══════════════════════════════════════════════════════════
    //  النائية
    // ═══════════════════════════════════════════════════════════

    public static Optional<RemoteBonusRequest> remoteForm(Window owner) {
        Dialog<RemoteBonusRequest> d = dialog(owner, "قيمة نائية جديدة");

        TextField from = new TextField();
        from.setPromptText("yyyy-MM-dd");
        TextField days = new TextField("15");

        GridPane g = grid();
        row(g, 0, "تاريخ السريان", from);
        row(g, 1, "عدد الأيام", days);
        d.getDialogPane().setContent(g);

        Supplier<RemoteBonusRequest> build = () -> {
            RemoteBonusRequest r = new RemoteBonusRequest();
            r.setEffectiveFrom(parseDate(from.getText(), "تاريخ السريان"));
            r.setDays(requireInt(days.getText(), "عدد الأيام"));
            return r;
        };

        guard(d, build::get);
        d.setResultConverter(bt -> bt == ButtonType.OK ? build.get() : null);
        return d.showAndWait();
    }

    // ═══════════════════════════════════════════════════════════
    //  قواعد النوع داخل كل قانون
    // ═══════════════════════════════════════════════════════════

    public static void typeRules(Window owner, LeaveTypeDto type,
                                 List<LeaveLawDto> laws, LeaveAdminApiClient api) {
        Dialog<Void> d = new Dialog<>();
        d.initOwner(owner);
        d.setTitle("قواعد: " + type.getNameAr());
        d.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        TableView<LeaveTypeRuleDto> table = new TableView<>();
        table.setPrefHeight(240);
        table.getColumns().add(col("القانون", LeaveTypeRuleDto::getLawCode));
        table.getColumns().add(col("من", r -> str(r.getEffectiveFrom())));
        table.getColumns().add(col("إلى", r -> r.getEffectiveTo() == null ? "مفتوح" : str(r.getEffectiveTo())));
        table.getColumns().add(col("الصرف", r -> LeaveLabels.payMode(r.getPayMode())));
        table.getColumns().add(col("الرصيد", r -> LeaveLabels.pool(r.getPoolCode())));
        table.getColumns().add(col("يقطع الاستحقاق", r -> r.isCountsAgainstAccrual() ? "نعم" : "لا"));
        table.getColumns().add(col("الحدود", r -> limitsText(r.getLimits())));
        table.getColumns().add(col("مستخدمة", r -> r.isInUse() ? "نعم (" + r.getUsageCount() + ")" : "لا"));

        Runnable reload = () -> UiAsync.run(() -> api.listRules(type.getId()),
                list -> table.setItems(FXCollections.observableArrayList(list)));

        Button deleteBtn = new Button("حذف المحدد");
        deleteBtn.setOnAction(e -> {
            LeaveTypeRuleDto sel = table.getSelectionModel().getSelectedItem();
            if (sel == null || !confirm("حذف القاعدة المحددة؟")) return;
            UiAsync.run(() -> {
                        api.deleteRule(type.getId(), sel.getId());
                        return null;
                    },
                    ok -> {
                        SAFNotification.success("تم حذف القاعدة");
                        reload.run();
                    });
        });

        ComboBox<String> lawBox = combo(
                laws.stream().map(LeaveLawDto::getCode).toList(),
                laws.isEmpty() ? "" : laws.get(0).getCode());
        TextField from = new TextField();
        from.setPromptText("yyyy-MM-dd");
        ComboBox<String> pay = combo(PAY_MODES, "PAID");
        ComboBox<String> pool = combo(POOLS, "");
        CheckBox counts = new CheckBox("يقطع الاستحقاق");

        TextField maxPerReq = new TextField();
        TextField maxOcc = new TextField();
        TextField maxMonths = new TextField();
        TextField maxYears = new TextField();
        TextField maxYearDays = new TextField();

        GridPane form = grid();
        row(form, 0, "القانون", lawBox);
        row(form, 1, "من تاريخ", from);
        row(form, 2, "الصرف", pay);
        row(form, 3, "الرصيد", pool);
        form.add(counts, 1, 4);
        row(form, 5, "حد الطلب الواحد", maxPerReq);
        row(form, 6, "حد المرات", maxOcc);
        row(form, 7, "حد شهور المرة", maxMonths);
        row(form, 8, "حد سنين الخدمة", maxYears);
        row(form, 9, "حد أيام السنة", maxYearDays);

        Button addBtn = new Button("إضافة قاعدة");
        addBtn.setOnAction(e -> {
            try {
                LeaveTypeRuleRequest r = new LeaveTypeRuleRequest();
                r.setLawCode(lawBox.getValue());
                r.setEffectiveFrom(parseDate(from.getText(), "تاريخ السريان"));
                r.setPayMode(pay.getValue());
                String p = pool.getValue();
                r.setPoolCode(p == null || p.isEmpty() ? null : p);
                r.setCountsAgainstAccrual(counts.isSelected());

                Map<String, Object> limits = new LinkedHashMap<>();
                putLimit(limits, "maxDaysPerRequest", maxPerReq, "حد الطلب الواحد");
                putLimit(limits, "maxOccurrences", maxOcc, "حد المرات");
                putLimit(limits, "maxMonthsPerOccurrence", maxMonths, "حد شهور المرة");
                putLimit(limits, "maxYearsPerService", maxYears, "حد سنين الخدمة");
                putLimit(limits, "maxDaysPerLeaveYear", maxYearDays, "حد أيام السنة");
                r.setLimits(limits);

                UiAsync.run(() -> api.createRule(type.getId(), r),
                        saved -> {
                            SAFNotification.success("تمت إضافة القاعدة");
                            reload.run();
                        });
            } catch (IllegalArgumentException ex) {
                SAFNotification.warning(ex.getMessage());
            }
        });

        VBox content = new VBox(10, table, deleteBtn, new Label("إضافة قاعدة جديدة"), form, addBtn);
        content.setPadding(new Insets(12));
        d.getDialogPane().setContent(content);

        reload.run();
        d.showAndWait();
    }

    // ═══════════════════════════════════════════════════════════
    //  بيانات الموظف للإجازات
    // ═══════════════════════════════════════════════════════════

    public static void employeeData(Window owner, String nid,
                                    List<LeaveLawDto> laws, LeaveAdminApiClient api) {
        Dialog<Void> d = new Dialog<>();
        d.initOwner(owner);
        d.setTitle("بيانات الإجازات — " + nid);
        d.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        TextArea history = new TextArea();
        history.setEditable(false);
        history.setPrefRowCount(6);

        // ── القانون ──
        ComboBox<LeaveLawDto> lawBox = new ComboBox<>(FXCollections.observableArrayList(laws));
        lawBox.setMaxWidth(Double.MAX_VALUE);
        TextField lawFrom = new TextField();
        lawFrom.setPromptText("yyyy-MM-dd");
        Button saveLaw = new Button("تعيين القانون");
        GridPane lawGrid = grid();
        row(lawGrid, 0, "القانون", lawBox);
        row(lawGrid, 1, "من تاريخ", lawFrom);
        lawGrid.add(saveLaw, 1, 2);

        // ── النائية ──
        CheckBox remote = new CheckBox("محافظة نائية");
        TextField remoteFrom = new TextField();
        remoteFrom.setPromptText("yyyy-MM-dd");
        Button saveRemote = new Button("تحديث النائية");
        GridPane remoteGrid = grid();
        remoteGrid.add(remote, 1, 0);
        row(remoteGrid, 1, "من تاريخ", remoteFrom);
        remoteGrid.add(saveRemote, 1, 2);

        // ── الرصيد المرحّل ──
        TableView<OpeningBalanceDto> openings = new TableView<>();
        openings.setPrefHeight(180);
        openings.getColumns().add(col("السنة", o -> String.valueOf(o.getLeaveYear())));
        openings.getColumns().add(col("المتبقي وقت النقل", o -> String.valueOf(o.getRemainingAtTransfer())));
        openings.getColumns().add(col("المستخدم قبل النقل", o -> String.valueOf(o.getOwnUsedBeforeTransfer())));
        openings.getColumns().add(col("الوحدة", o -> str(o.getSourceUnit())));
        openings.getColumns().add(col("رقم البيان", o -> str(o.getStatementRef())));

        TextField oYear = new TextField();
        oYear.setPromptText("مثال 2025");
        TextField oRemaining = new TextField();
        TextField oOwn = new TextField();
        TextField oUnit = new TextField();
        TextField oRef = new TextField();
        Button saveOpen = new Button("حفظ الرصيد");
        Button deleteOpen = new Button("حذف المحدد");

        GridPane openGrid = grid();
        row(openGrid, 0, "السنة", oYear);
        row(openGrid, 1, "المتبقي وقت النقل", oRemaining);
        row(openGrid, 2, "المستخدم قبل النقل", oOwn);
        row(openGrid, 3, "الوحدة", oUnit);
        row(openGrid, 4, "رقم البيان", oRef);

        // ── الأحداث ──
        Runnable reloadProfile = () -> UiAsync.run(() -> api.getEmployeeLaw(nid),
                p -> history.setText(historyText(p)));
        Runnable reloadOpenings = () -> UiAsync.run(() -> api.listOpenings(nid),
                list -> openings.setItems(FXCollections.observableArrayList(list)));
        Runnable reloadAll = () -> {
            reloadProfile.run();
            reloadOpenings.run();
        };

        saveLaw.setOnAction(e -> {
            try {
                LeaveLawDto law = lawBox.getValue();
                if (law == null) throw new IllegalArgumentException("اختر القانون");
                LawChangeRequest req = new LawChangeRequest();
                req.setLawCode(law.getCode());
                req.setEffectiveFrom(parseDate(lawFrom.getText(), "تاريخ السريان"));
                UiAsync.run(() -> api.setLaw(nid, req), p -> {
                    SAFNotification.success("تم تعيين القانون");
                    reloadAll.run();
                });
            } catch (IllegalArgumentException ex) {
                SAFNotification.warning(ex.getMessage());
            }
        });

        saveRemote.setOnAction(e -> {
            try {
                RemoteChangeRequest req = new RemoteChangeRequest();
                req.setValue(remote.isSelected());
                req.setEffectiveFrom(parseDate(remoteFrom.getText(), "تاريخ السريان"));
                UiAsync.run(() -> api.setRemote(nid, req), p -> {
                    SAFNotification.success("تم تحديث حالة النائية");
                    reloadAll.run();
                });
            } catch (IllegalArgumentException ex) {
                SAFNotification.warning(ex.getMessage());
            }
        });

        saveOpen.setOnAction(e -> {
            try {
                OpeningBalanceRequest req = new OpeningBalanceRequest();
                req.setLeaveYear(requireInt(oYear.getText(), "السنة"));
                req.setPoolCode("ANNUAL");
                req.setRemainingAtTransfer(optInt(oRemaining.getText(), "المتبقي"));
                req.setOwnUsedBeforeTransfer(optInt(oOwn.getText(), "المستخدم قبل النقل"));
                req.setSourceUnit(blankToNull(oUnit.getText()));
                req.setStatementRef(blankToNull(oRef.getText()));
                UiAsync.run(() -> api.saveOpening(nid, req), o -> {
                    SAFNotification.success("تم حفظ الرصيد المرحّل");
                    reloadAll.run();
                });
            } catch (IllegalArgumentException ex) {
                SAFNotification.warning(ex.getMessage());
            }
        });

        deleteOpen.setOnAction(e -> {
            OpeningBalanceDto sel = openings.getSelectionModel().getSelectedItem();
            if (sel == null || !confirm("حذف الرصيد المرحّل لسنة " + sel.getLeaveYear() + "؟")) return;
            UiAsync.run(() -> {
                        api.deleteOpening(nid, sel.getLeaveYear());
                        return null;
                    },
                    ok -> {
                        SAFNotification.success("تم الحذف");
                        reloadAll.run();
                    });
        });

        VBox content = new VBox(12,
                new Label("السجل الحالي"), history,
                new Label("تعيين القانون"), lawGrid,
                new Label("حالة النائية"), remoteGrid,
                new Label("الرصيد المرحّل من الوحدة السابقة"), openings, openGrid,
                new HBox(8, saveOpen, deleteOpen));
        content.setPadding(new Insets(12));

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(620);

        d.getDialogPane().setContent(scroll);
        d.getDialogPane().setPrefWidth(780);

        reloadAll.run();
        d.show();
    }

    // ═══════════════════════════════════════════════════════════
    //  صف شرط في جدول الاستحقاق
    // ═══════════════════════════════════════════════════════════

    private static final class RuleRow {
        final HBox box;
        final TextField priority = new TextField();
        final CheckBox disabled = new CheckBox();
        final TextField ageMin = new TextField();
        final TextField serviceMonths = new TextField();
        final ComboBox<String> gender = new ComboBox<>(FXCollections.observableArrayList(GENDER_CONDITIONS));
        final TextField days = new TextField();

        RuleRow(Runnable onRemove) {
            priority.setPromptText("أولوية");
            priority.setPrefWidth(70);
            ageMin.setPromptText("سن ≥");
            ageMin.setPrefWidth(70);
            serviceMonths.setPromptText("خدمة ≥ شهر");
            serviceMonths.setPrefWidth(100);
            gender.setValue("");
            gender.setPrefWidth(110);
            days.setPromptText("أيام");
            days.setPrefWidth(70);

            Button remove = new Button("✖");
            remove.setOnAction(e -> onRemove.run());

            box = new HBox(8, priority, disabled, new Label("معاق"), ageMin, serviceMonths,
                    gender, days, remove);
            box.setAlignment(Pos.CENTER_LEFT);
        }

        void set(Integer prio, boolean dis, Integer age, Integer svc, String g, Integer dayCount) {
            priority.setText(prio == null ? "" : String.valueOf(prio));
            disabled.setSelected(dis);
            ageMin.setText(age == null ? "" : String.valueOf(age));
            serviceMonths.setText(svc == null ? "" : String.valueOf(svc));
            gender.setValue(g == null ? "" : g);
            days.setText(dayCount == null ? "" : String.valueOf(dayCount));
        }

        AccrualRuleItem toItem() {
            AccrualRuleItem it = new AccrualRuleItem();
            it.setPriority(requireInt(priority.getText(), "الأولوية"));
            it.setDays(requireInt(days.getText(), "الأيام"));

            Map<String, Object> c = new LinkedHashMap<>();
            if (disabled.isSelected()) c.put("disabled", true);
            Integer age = optInt(ageMin.getText(), "السن");
            if (age != null) c.put("ageMin", age);
            Integer svc = optInt(serviceMonths.getText(), "مدة الخدمة");
            if (svc != null) c.put("serviceMonthsMin", svc);
            String g = gender.getValue();
            if (g != null && !g.isEmpty()) c.put("gender", g);
            it.setConditions(c);
            return it;
        }
    }

    private static RuleRow addRow(List<RuleRow> rows, VBox box) {
        RuleRow[] self = new RuleRow[1];
        self[0] = new RuleRow(() -> {
            rows.remove(self[0]);
            box.getChildren().remove(self[0].box);
        });
        rows.add(self[0]);
        box.getChildren().add(self[0].box);
        return self[0];
    }

    // ═══════════════════════════════════════════════════════════
    //  Helpers
    // ═══════════════════════════════════════════════════════════

    private static <T> Dialog<T> dialog(Window owner, String title) {
        Dialog<T> d = new Dialog<>();
        d.initOwner(owner);
        d.setTitle(title);
        d.setHeaderText(null);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        return d;
    }

    /** يمنع إغلاق نافذة الحفظ لو البيانات غلط، ويعرض الرسالة */
    private static void guard(Dialog<?> d, Runnable build) {
        Button ok = (Button) d.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(ActionEvent.ACTION, ev -> {
            try {
                build.run();
            } catch (IllegalArgumentException ex) {
                ev.consume();
                SAFNotification.warning(ex.getMessage());
            }
        });
    }

    private static GridPane grid() {
        GridPane g = new GridPane();
        g.setHgap(10);
        g.setVgap(10);
        g.setPadding(new Insets(12));
        return g;
    }

    private static void row(GridPane g, int r, String label, Node n) {
        g.add(new Label(label), 0, r);
        g.add(n, 1, r);
    }

    private static <T> ComboBox<T> combo(List<T> items, T selected) {
        ComboBox<T> c = new ComboBox<>(FXCollections.observableArrayList(items));
        c.setValue(selected);
        c.setMaxWidth(Double.MAX_VALUE);
        return c;
    }

    private static LocalDate parseDate(String s, String label) {
        if (s == null || s.isBlank()) throw new IllegalArgumentException(label + " مطلوب");
        try {
            return LocalDate.parse(s.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(label + " لازم يكون بصيغة yyyy-MM-dd");
        }
    }

    static int requireInt(String s, String label) {
        if (s == null || s.isBlank()) throw new IllegalArgumentException(label + " مطلوب");
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(label + " لازم يكون رقم صحيح");
        }
    }

    static Integer optInt(String s, String label) {
        if (s == null || s.isBlank()) return null;
        return requireInt(s, label);
    }

    private static void putLimit(Map<String, Object> m, String key, TextField f, String label) {
        Integer v = optInt(f.getText(), label);
        if (v != null) m.put(key, v);
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    static String str(Object o) {
        return o == null ? "—" : o.toString();
    }

    private static String limitsText(Map<String, Object> limits) {
        if (limits == null || limits.isEmpty()) return "—";
        StringBuilder sb = new StringBuilder();
        limits.forEach((k, v) -> {
            if (sb.length() > 0) sb.append(" | ");
            sb.append(k).append("=").append(v);
        });
        return sb.toString();
    }

    private static String historyText(EmployeeLawProfileDto p) {
        StringBuilder sb = new StringBuilder();
        sb.append("تاريخ التعيين: ").append(str(p.getHireDate()))
                .append("   |   تاريخ الاستلام: ").append(str(p.getWorkReceiptDate()))
                .append('\n');
        sb.append("القوانين:\n");
        if (p.getLawHistory() != null) {
            p.getLawHistory().forEach((k, v) ->
                    sb.append("   ").append(k).append(" → ").append(v).append('\n'));
        }
        sb.append("النائية:\n");
        if (p.getRemoteHistory() != null) {
            p.getRemoteHistory().forEach((k, v) ->
                    sb.append("   ").append(k).append(" → ")
                            .append(Boolean.TRUE.equals(v) ? "نائية" : "غير نائية").append('\n'));
        }
        return sb.toString();
    }

    static <S> TableColumn<S, String> col(String title, Function<S, String> f) {
        TableColumn<S, String> c = new TableColumn<>(title);
        c.setCellValueFactory(cd -> new SimpleStringProperty(f.apply(cd.getValue())));
        c.setPrefWidth(120);
        return c;
    }

    static boolean confirm(String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        a.setHeaderText(null);
        return a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }
}