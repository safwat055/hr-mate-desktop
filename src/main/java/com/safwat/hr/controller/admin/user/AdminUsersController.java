package com.safwat.hr.controller.admin.user;

import com.fasterxml.jackson.core.type.TypeReference;
import com.safwat.hr.controller.admin.system.AppLogBus;
import com.safwat.hr.network.ApiClient;
import com.safwat.hr.network.ApiResponse;
import com.safwat.hr.network.SessionManager;
import com.safwat.hr.network.dto.AdminUserDtos.*;
import com.safwat.hr.ui.util.AlertUtil;
import com.safwat.hr.ui.util.ViewManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class AdminUsersController implements Initializable {

    @FXML
    private TextField txtSearch;
    @FXML
    private Button btnRefresh, btnNewUser, btnNewPermission;
    @FXML
    private Label lblInfo;
    @FXML
    private TableView<UserResponse> table;
    @FXML
    private TableColumn<UserResponse, String> colUsername, colDisplayName, colJobTitle, colPermissions;
    @FXML
    private TableColumn<UserResponse, Boolean> colActive;
    @FXML
    private TableColumn<UserResponse, UserResponse> colActions;

    private final ObservableList<UserResponse> masterData = FXCollections.observableArrayList();
    private FilteredList<UserResponse> filteredData;
    private List<PermissionDto> allPermissions = new ArrayList<>();

    private static final String PROTECTED_ADMIN = "admin";
    private static final String CSS_PATH = "/com/safwat/hr/css/settings.css";

    // ══════════════════════════ Init ══════════════════════════
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        filteredData = new FilteredList<>(masterData, u -> true);
        table.setItems(filteredData);

        colUsername.setCellValueFactory(c ->
                new SimpleStringProperty(nullSafe(c.getValue().getUsername())));
        colDisplayName.setCellValueFactory(c ->
                new SimpleStringProperty(nullSafe(c.getValue().getDisplayName())));
        colJobTitle.setCellValueFactory(c ->
                new SimpleStringProperty(nullSafe(c.getValue().getJobTitle())));
        colPermissions.setCellValueFactory(c ->
                new SimpleStringProperty(formatPermissions(c.getValue())));
        colActive.setCellValueFactory(c ->
                new SimpleBooleanProperty(c.getValue().isActive()));
        colActive.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean active, boolean empty) {
                super.updateItem(active, empty);
                getStyleClass().removeAll("stg-cell-active", "stg-cell-inactive");
                if (empty || active == null) {
                    setText(null);
                } else {
                    setText(active ? "✅ نشط" : "❌ معطّل");
                    getStyleClass().add(active ? "stg-cell-active" : "stg-cell-inactive");
                }
            }
        });
        colActions.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        colActions.setCellFactory(col -> new ActionsCell());

        txtSearch.textProperty().addListener((obs, o, q) ->
                filteredData.setPredicate(u -> {
                    if (q == null || q.isBlank()) return true;
                    String s = q.trim().toLowerCase();
                    return nullSafe(u.getUsername()).toLowerCase().contains(s)
                            || nullSafe(u.getDisplayName()).toLowerCase().contains(s)
                            || nullSafe(u.getJobTitle()).toLowerCase().contains(s);
                }));

        refresh();

    }

    // ══════════════════════════ تحميل البيانات ══════════════════════════
    @FXML
    private void refresh() {
        showInfo("⏳ جاري تحميل البيانات...");
        new Thread(() -> {
            try {
                ApiResponse<List<PermissionDto>> permsResp =
                        ApiClient.getWithTypeRef("/admin/permissions", new TypeReference<>() {
                        });
                ApiResponse<List<UserResponse>> usersResp =
                        ApiClient.getWithTypeRef("/admin/users", new TypeReference<>() {
                        });

                Platform.runLater(() -> {
                    if (permsResp.isSuccess() && permsResp.getData() != null) {
                        allPermissions = new ArrayList<>(permsResp.getData());
                        allPermissions.sort(Comparator.comparing(p -> nullSafe(p.getName()).toLowerCase()));
                    }
                    if (usersResp.isSuccess() && usersResp.getData() != null) {
                        masterData.setAll(usersResp.getData());
                        hideInfo();
                        AppLogBus.getInstance().log("[AdminUsers] ✅ تم تحميل " + masterData.size() + " مستخدم");
                    } else {
                        showInfo("❌ فشل التحميل: " +
                                (usersResp.getMessage() != null ? usersResp.getMessage() : "خطأ غير معروف"));
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showInfo("❌ خطأ في الاتصال: " + e.getMessage()));
            }
        }).start();
    }

    // ══════════════════════════ إنشاء مستخدم ══════════════════════════
    @FXML
    private void createUser() {
        TextField txtUsername = styledField("اسم المستخدم *");
        PasswordField txtPassword = styledPassword("كلمة المرور (6 أحرف على الأقل) *");
        TextField txtDisplay = styledField("الاسم المعروض");
        TextField txtJob = styledField("المسمى الوظيفي");

        VBox permList = buildPermissionCheckboxes(new HashSet<>());
        ScrollPane permScroll = wrapPermissionsScroll(permList);

        VBox content = new VBox(10,
                styledHeader("👤 مستخدم جديد"),
                txtUsername, txtPassword, txtDisplay, txtJob,
                styledSubHeader("الصلاحيات *"),
                permScroll);
        content.setPadding(new Insets(16));
        content.getStyleClass().add("stg-dialog-content");

        Optional<ButtonType> result = buildDialog("إنشاء مستخدم", content).showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        String username = txtUsername.getText() == null ? "" : txtUsername.getText().trim();
        String password = txtPassword.getText() == null ? "" : txtPassword.getText();

        if (username.isEmpty() || password.length() < 6) {
            AlertUtil.showError("بيانات ناقصة",
                    "اسم المستخدم مطلوب وكلمة المرور 6 أحرف على الأقل");
            return;
        }

        Set<Long> permIds = selectedPermissionIds(permList);
        if (permIds.isEmpty()) {
            AlertUtil.showError("بيانات ناقصة", "لازم تختار صلاحية واحدة على الأقل");
            return;
        }

        CreateUserRequest req = new CreateUserRequest();
        req.setUsername(username);
        req.setPassword(password);
        req.setDisplayName(txtDisplay.getText());
        req.setJobTitle(txtJob.getText());
        req.setPermissionIds(permIds);

        runOp("إنشاء المستخدم",
                () -> ApiClient.post("/admin/users", req, UserResponse.class));
    }

    // ══════════════════════════ تعديل صلاحيات ══════════════════════════
    private void editPermissions(UserResponse user) {
        Set<Long> current = user.getPermissions() == null ? Set.of() :
                user.getPermissions().stream()
                        .map(PermissionDto::getId)
                        .collect(Collectors.toSet());

        VBox permList = buildPermissionCheckboxes(current);
        ScrollPane permScroll = wrapPermissionsScroll(permList);

        VBox content = new VBox(10,
                styledHeader("🔑 صلاحيات: " + user.getUsername()),
                permScroll);
        content.setPadding(new Insets(16));
        content.getStyleClass().add("stg-dialog-content");

        Optional<ButtonType> result = buildDialog("تعديل الصلاحيات", content).showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        Set<Long> permIds = selectedPermissionIds(permList);
        if (permIds.isEmpty()) {
            AlertUtil.showError("بيانات ناقصة", "لازم تختار صلاحية واحدة على الأقل");
            return;
        }

        runOp("تحديث الصلاحيات",
                () -> ApiClient.put("/admin/users/" + user.getId() + "/permissions",
                        new UpdatePermissionsRequest(permIds), UserResponse.class));
    }

    // ══════════════════════════ إعادة تعيين كلمة المرور ══════════════════════════
    private void resetPassword(UserResponse user) {
        PasswordField txtNew = styledPassword("كلمة المرور الجديدة (6+)");
        PasswordField txtConfirm = styledPassword("تأكيدها");

        VBox content = new VBox(10,
                styledHeader("🔐 إعادة تعيين كلمة مرور: " + user.getUsername()),
                txtNew, txtConfirm);
        content.setPadding(new Insets(16));
        content.getStyleClass().add("stg-dialog-content");

        Optional<ButtonType> result = buildDialog("إعادة تعيين كلمة المرور", content).showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        String pw = txtNew.getText() == null ? "" : txtNew.getText();
        if (pw.length() < 6 || !pw.equals(txtConfirm.getText())) {
            AlertUtil.showError("بيانات غير صالحة",
                    "كلمة المرور 6 أحرف على الأقل والتأكيد لازم يطابقها");
            return;
        }

        runOp("إعادة تعيين كلمة المرور",
                () -> ApiClient.post("/admin/users/" + user.getId() + "/reset-password",
                        new ResetPasswordRequest(pw), Void.class));
    }

    // ══════════════════════════ تفعيل / تعطيل ══════════════════════════
    private void toggleActive(UserResponse user) {
        boolean activating = !user.isActive();
        String action = activating ? "تفعيل" : "تعطيل";
        if (!AlertUtil.showConfirmation(action + " مستخدم",
                "هل أنت متأكد من " + action + " المستخدم " + user.getUsername() + "؟")) return;

        runOp(action, () -> ApiClient.put(
                "/admin/users/" + user.getId() + "/" + (activating ? "enable" : "disable"),
                null, UserResponse.class));
    }

    // ══════════════════════════ إنشاء صلاحية ══════════════════════════
    @FXML
    private void createPermission() {
        TextField txtName = styledField("اسم الصلاحية إنجليزي CAPS بـ underscores *");
        TextField txtLabel = styledField("المقابل العربي (مثال: تعديل السلم الوظيفي)");

        VBox content = new VBox(10,
                styledHeader("➕ صلاحية جديدة"),
                txtName, txtLabel);
        content.setPadding(new Insets(16));
        content.getStyleClass().add("stg-dialog-content");

        Optional<ButtonType> result = buildDialog("إنشاء صلاحية", content).showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        String name = txtName.getText() == null ? "" : txtName.getText().trim();
        if (name.isEmpty()) {
            AlertUtil.showError("بيانات ناقصة", "اسم الصلاحية مطلوب");
            return;
        }

        Map<String, String> body = new HashMap<>();
        body.put("name", name);
        body.put("label", txtLabel.getText());

        runOp("إنشاء الصلاحية",
                () -> ApiClient.post("/admin/permissions", body, PermissionDto.class));
    }

    // ══════════════════════════ UI Builders ══════════════════════════
    private VBox buildPermissionCheckboxes(Set<Long> selectedIds) {
        VBox list = new VBox(4);
        list.getStyleClass().add("stg-perm-list");

        if (allPermissions.isEmpty()) {
            Label empty = new Label("لا توجد صلاحيات متاحة");
            empty.getStyleClass().add("stg-perm-empty");
            list.getChildren().add(empty);
            return list;
        }

        for (PermissionDto p : allPermissions) {
            CheckBox cb = new CheckBox(formatPermissionLabel(p));
            cb.setUserData(p.getId());
            cb.setSelected(selectedIds.contains(p.getId()));
            cb.getStyleClass().add("stg-checkbox-dark");
            cb.setMaxWidth(Double.MAX_VALUE);
            list.getChildren().add(cb);
        }
        return list;
    }

    private String formatPermissionLabel(PermissionDto p) {
        String name = nullSafe(p.getName());
        String label = nullSafe(p.getLabel());
        if (label.isBlank()) return name;
        return name + "  —  " + label;
    }

    private ScrollPane wrapPermissionsScroll(VBox list) {
        ScrollPane sp = new ScrollPane(list);
        sp.setFitToWidth(true);
        sp.setPrefHeight(240);
        sp.setPrefWidth(420);
        sp.getStyleClass().add("stg-perm-scroll");
        return sp;
    }

    private Set<Long> selectedPermissionIds(VBox list) {
        return list.getChildren().stream()
                .filter(n -> n instanceof CheckBox cb && cb.isSelected())
                .map(n -> (Long) n.getUserData())
                .collect(Collectors.toSet());
    }

    private String formatPermissions(UserResponse u) {
        if (u.getPermissions() == null || u.getPermissions().isEmpty()) return "—";
        int count = u.getPermissions().size();
        if (count == 1) {
            PermissionDto p = u.getPermissions().iterator().next();
            return nullSafe(p.getLabel()).isEmpty()
                    ? nullSafe(p.getName())
                    : nullSafe(p.getLabel());
        }
        return count + " صلاحيات";
    }

    // ══════════════════════════ Dialog Helpers ══════════════════════════
    private Alert buildDialog(String title, Node content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.getDialogPane().setContent(content);
        alert.getDialogPane().getStyleClass().add("stg-dialog");

        URL css = getClass().getResource(CSS_PATH);
        if (css != null) {
            alert.getDialogPane().getStylesheets().add(css.toExternalForm());
        }
        return alert;
    }

    private Label styledHeader(String text) {
        Label lbl = new Label(text);
        lbl.getStyleClass().add("stg-dialog-title");
        return lbl;
    }

    private Label styledSubHeader(String text) {
        Label lbl = new Label(text);
        lbl.getStyleClass().add("stg-dialog-subtitle");
        return lbl;
    }

    private TextField styledField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.getStyleClass().add("stg-field-dark");
        tf.setPrefWidth(420);
        return tf;
    }

    private PasswordField styledPassword(String prompt) {
        PasswordField pf = new PasswordField();
        pf.setPromptText(prompt);
        pf.getStyleClass().add("stg-field-dark");
        pf.setPrefWidth(420);
        return pf;
    }

    // ══════════════════════════ Operations ══════════════════════════
    private void runOp(String opName, Op operation) {
        showInfo("⏳ " + opName + "...");
        new Thread(() -> {
            try {
                ApiResponse<?> resp = operation.execute();
                Platform.runLater(() -> {
                    if (resp.isSuccess()) {
                        AppLogBus.getInstance().log("[AdminUsers] ✅ " + opName);
                        refresh();
                    } else {
                        showInfo("❌ فشل: " +
                                (resp.getMessage() != null ? resp.getMessage() : "خطأ غير معروف"));
                        AlertUtil.showError(opName + " فشل", resp.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showInfo("❌ خطأ في الاتصال: " + e.getMessage());
                    AlertUtil.showError(opName + " فشل", e.getMessage());
                });
            }
        }).start();
    }

    // ══════════════════════════ Info helpers ══════════════════════════
    private void showInfo(String msg) {
        lblInfo.setText(msg);
        lblInfo.setVisible(true);
        lblInfo.setManaged(true);
    }

    private void hideInfo() {
        lblInfo.setVisible(false);
        lblInfo.setManaged(false);
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    @FunctionalInterface
    private interface Op {
        ApiResponse<?> execute() throws Exception;
    }

    // ══════════════════════════ Actions Cell ══════════════════════════
    private final class ActionsCell extends TableCell<UserResponse, UserResponse> {

        private final Button btnPerms = new Button("🔑 صلاحيات");
        private final Button btnReset = new Button("🔐 كلمة مرور");
        private final Button btnToggle = new Button();

        ActionsCell() {
            btnPerms.getStyleClass().add("stg-btn-cell-perm");
            btnReset.getStyleClass().add("stg-btn-cell-edit");
            btnToggle.getStyleClass().add("stg-btn-cell-warn");

            btnPerms.setOnAction(e -> {
                UserResponse u = getItem();
                if (u != null) editPermissions(u);
            });
            btnReset.setOnAction(e -> {
                UserResponse u = getItem();
                if (u != null) resetPassword(u);
            });
            btnToggle.setOnAction(e -> {
                UserResponse u = getItem();
                if (u != null) toggleActive(u);
            });
        }

        @Override
        protected void updateItem(UserResponse user, boolean empty) {
            super.updateItem(user, empty);
            if (empty || user == null) {
                setGraphic(null);
                return;
            }
            boolean protectedAdmin = PROTECTED_ADMIN.equalsIgnoreCase(user.getUsername());
            btnPerms.setDisable(protectedAdmin);
            btnReset.setDisable(protectedAdmin);
            btnToggle.setDisable(protectedAdmin);

            btnToggle.setText(user.isActive() ? "⏸ تعطيل" : "▶ تفعيل");
            btnToggle.getStyleClass().removeAll("stg-btn-cell-warn", "stg-btn-cell-danger");
            btnToggle.getStyleClass().add(
                    user.isActive() ? "stg-btn-cell-danger" : "stg-btn-cell-warn");

            HBox box = new HBox(6, btnPerms, btnReset, btnToggle);
            box.setAlignment(Pos.CENTER_RIGHT);
            setGraphic(box);
        }
    }

    // ══════════════════════════ Static Helpers ══════════════════════════
    public static boolean canOpen() {
        return SessionManager.getInstance().isAdmin();
    }

    public static void open(Stage owner) {
        ViewManager.openIndependentView(
                "/com/safwat/hr/controller/admin/user/AdminUsersView.fxml",
                "👥 إدارة المستخدمين",
                owner,
                Modality.WINDOW_MODAL,
                true,
                null
        );
    }
}