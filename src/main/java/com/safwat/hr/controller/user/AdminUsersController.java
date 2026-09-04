package com.safwat.hr.controller.user;

import com.fasterxml.jackson.core.type.TypeReference;
import com.safwat.hr.network.ApiClient;
import com.safwat.hr.network.ApiResponse;
import com.safwat.hr.network.SessionManager;
import com.safwat.hr.network.dto.AdminUserDtos.*;
import com.safwat.hr.system.AppLogBus;
import com.safwat.hr.ui.theme.ThemeEventBus;
import com.safwat.hr.ui.util.AlertUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * واجهة إدارة المستخدمين (للأدمن فقط — endpoints تحت /api/admin محمية بـ @PreAuthorize("ADMIN")).
 * <p>
 * المزايا: عرض/بحث، إنشاء مستخدم، تعديل صلاحيات، إعادة تعيين كلمة مرور، تفعيل/تعطيل، إنشاء صلاحية جديدة.
 * كل العناصر JavaFX عادية (Button / TextField / PasswordField / CheckBox).
 */
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

    /**
     * اليوزر admin الأساسي ممنوع تعديله (نفس قاعدة الباك ايند)
     */
    private static final String PROTECTED_ADMIN = "admin";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        filteredData = new FilteredList<>(masterData, u -> true);
        table.setItems(filteredData);

        colUsername.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                nullSafe(c.getValue().getUsername())));
        colDisplayName.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                nullSafe(c.getValue().getDisplayName())));
        colJobTitle.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                nullSafe(c.getValue().getJobTitle())));
        colPermissions.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                formatPermissions(c.getValue())));
        colActive.setCellValueFactory(c -> new javafx.beans.property.SimpleBooleanProperty(c.getValue().isActive()));
        colActive.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean active, boolean empty) {
                super.updateItem(active, empty);
                if (empty || active == null) {
                    setText(null);
                    getStyleClass().removeAll("text-success", "text-danger");
                } else {
                    setText(active ? "✅ نشط" : "❌ معطّل");
                    getStyleClass().setAll(active ? "text-success" : "text-danger");
                }
            }
        });
        colActions.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue()));
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

    // ══════════════ تحميل البيانات ══════════════

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
                        allPermissions.sort(Comparator.comparing(p -> nullSafe(p.getLabel())));
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

    // ══════════════ إنشاء مستخدم ══════════════

    @FXML
    private void createUser() {
        TextField txtUsername = new TextField();
        txtUsername.setPromptText("اسم المستخدم *");
        txtUsername.setPrefWidth(360);
        PasswordField txtPassword = new PasswordField();
        txtPassword.setPromptText("كلمة المرور (6 أحرف على الأقل) *");
        txtPassword.setPrefWidth(360);
        TextField txtDisplay = new TextField();
        txtDisplay.setPromptText("الاسم المعروض");
        txtDisplay.setPrefWidth(360);
        TextField txtJob = new TextField();
        txtJob.setPromptText("المسمى الوظيفي");
        txtJob.setPrefWidth(360);

        FlowPane permPane = buildPermissionCheckboxes(new HashSet<>());

        VBox content = new VBox(10,
                new Label("مستخدم جديد"),
                txtUsername, txtPassword, txtDisplay, txtJob,
                new Label("الصلاحيات *"), permPane);
        content.setPadding(new Insets(16));

        Optional<ButtonType> result = new Alert(Alert.AlertType.CONFIRMATION) {{
            setTitle("إنشاء مستخدم");
            setHeaderText(null);
            getDialogPane().setContent(content);
        }}
                .showAndWait();

        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        String username = txtUsername.getText() == null ? "" : txtUsername.getText().trim();
        String password = txtPassword.getText() == null ? "" : txtPassword.getText();

        if (username.isEmpty() || password.length() < 6) {
            AlertUtil.showError("بيانات ناقصة", "اسم المستخدم مطلوب وكلمة المرور 6 أحرف على الأقل");
            return;
        }

        Set<Long> permIds = selectedPermissionIds(permPane);
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

        runOp("إنشاء المستخدم", () -> ApiClient.post("/admin/users", req, UserResponse.class));
    }

    // ══════════════ تعديل صلاحيات ══════════════

    private void editPermissions(UserResponse user) {
        Set<Long> current = user.getPermissions() == null ? Set.of() :
                user.getPermissions().stream().map(PermissionDto::getId).collect(Collectors.toSet());
        FlowPane permPane = buildPermissionCheckboxes(current);

        VBox content = new VBox(10, new Label("صلاحيات: " + user.getUsername()), permPane);
        content.setPadding(new Insets(16));

        Optional<ButtonType> result = new Alert(Alert.AlertType.CONFIRMATION) {{
            setTitle("تعديل الصلاحيات");
            setHeaderText(null);
            getDialogPane().setContent(content);
        }}
                .showAndWait();

        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        Set<Long> permIds = selectedPermissionIds(permPane);
        if (permIds.isEmpty()) {
            AlertUtil.showError("بيانات ناقصة", "لازم تختار صلاحية واحدة على الأقل");
            return;
        }

        runOp("تحديث الصلاحيات",
                () -> ApiClient.put("/admin/users/" + user.getId() + "/permissions",
                        new UpdatePermissionsRequest(permIds), UserResponse.class));
    }

    // ══════════════ إعادة تعيين كلمة المرور ══════════════

    private void resetPassword(UserResponse user) {
        PasswordField txtNew = new PasswordField();
        txtNew.setPromptText("كلمة المرور الجديدة (6+)");
        txtNew.setPrefWidth(320);
        PasswordField txtConfirm = new PasswordField();
        txtConfirm.setPromptText("تأكيدها");
        txtConfirm.setPrefWidth(320);

        VBox content = new VBox(10, new Label("إعادة تعيين كلمة مرور: " + user.getUsername()), txtNew, txtConfirm);
        content.setPadding(new Insets(16));

        Optional<ButtonType> result = new Alert(Alert.AlertType.CONFIRMATION) {{
            setTitle("إعادة تعيين كلمة المرور");
            setHeaderText(null);
            getDialogPane().setContent(content);
        }}
                .showAndWait();

        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        String pw = txtNew.getText() == null ? "" : txtNew.getText();
        if (pw.length() < 6 || !pw.equals(txtConfirm.getText())) {
            AlertUtil.showError("بيانات غير صالحة", "كلمة المرور 6 أحرف على الأقل والتأكيد لازم يطابقها");
            return;
        }

        runOp("إعادة تعيين كلمة المرور",
                () -> ApiClient.post("/admin/users/" + user.getId() + "/reset-password",
                        new ResetPasswordRequest(pw), Void.class));
    }

    // ══════════════ تفعيل / تعطيل ══════════════

    private void toggleActive(UserResponse user) {
        boolean activating = !user.isActive();
        String action = activating ? "تفعيل" : "تعطيل";
        if (!AlertUtil.showConfirmation(action + " مستخدم",
                "هل أنت متأكد من " + action + " المستخدم " + user.getUsername() + "؟")) return;

        runOp(action, () -> ApiClient.put("/admin/users/" + user.getId() + "/" + (activating ? "enable" : "disable"),
                null, UserResponse.class));
    }

    // ══════════════ إنشاء صلاحية ══════════════

    @FXML
    private void createPermission() {
        TextField txtName = new TextField();
        txtName.setPromptText("اسم الصلاحية إنجليزي CAPS بـ underscores *");
        txtName.setPrefWidth(360);
        TextField txtLabel = new TextField();
        txtLabel.setPromptText("المقابل العربي (مثال: تعديل السلم الوظيفي)");
        txtLabel.setPrefWidth(360);

        VBox content = new VBox(10, new Label("صلاحية جديدة"), txtName, txtLabel);
        content.setPadding(new Insets(16));

        Optional<ButtonType> result = new Alert(Alert.AlertType.CONFIRMATION) {{
            setTitle("إنشاء صلاحية");
            setHeaderText(null);
            getDialogPane().setContent(content);
        }}
                .showAndWait();

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

    // ══════════════ Helpers ══════════════

    /**
     * ينفّذ operation في Thread منفصل ويعمل refresh عند النجاح
     */
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
                        showInfo("❌ فشل: " + (resp.getMessage() != null ? resp.getMessage() : "خطأ غير معروف"));
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

    private FlowPane buildPermissionCheckboxes(Set<Long> selectedIds) {
        FlowPane pane = new FlowPane();
        pane.setHgap(12);
        pane.setVgap(8);
        pane.setPrefWrapLength(420);
        for (PermissionDto p : allPermissions) {
            CheckBox cb = new CheckBox(nullSafe(p.getLabel()) + " (" + nullSafe(p.getName()) + ")");
            cb.setUserData(p.getId());
            cb.setSelected(selectedIds.contains(p.getId()));
            pane.getChildren().add(cb);
        }
        return pane;
    }

    private Set<Long> selectedPermissionIds(FlowPane pane) {
        return pane.getChildren().stream()
                .filter(n -> n instanceof CheckBox cb && cb.isSelected())
                .map(n -> (Long) n.getUserData())
                .collect(Collectors.toSet());
    }

    private String formatPermissions(UserResponse u) {
        if (u.getPermissions() == null || u.getPermissions().isEmpty()) return "—";
        return u.getPermissions().stream()
                .map(p -> nullSafe(p.getLabel()).isEmpty() ? nullSafe(p.getName()) : nullSafe(p.getLabel()))
                .sorted()
                .collect(Collectors.joining("، "));
    }

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

    /**
     * خلايا عمود الإجراءات
     */
    private final class ActionsCell extends TableCell<UserResponse, UserResponse> {
        private final Button btnPerms = new Button("صلاحيات");

        {
            btnPerms.getStyleClass().addAll("btn", "btn-secondary", "btn-sm");
        }

        private final Button btnReset = new Button("كلمة مرور");

        {
            btnReset.getStyleClass().addAll("btn", "btn-secondary", "btn-sm");
        }

        private final Button btnToggle = new Button();

        {
            btnToggle.getStyleClass().add("btn-sm");
        }

        ActionsCell() {
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
            btnToggle.setText(user.isActive() ? "تعطيل" : "تفعيل");
            btnToggle.getStyleClass().removeAll("btn-danger", "btn-success");
            btnToggle.getStyleClass().add(user.isActive() ? "btn-danger" : "btn-success");

            HBox box = new HBox(6, btnPerms, btnReset, btnToggle);
            box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            setGraphic(box);
        }
    }

    /**
     * ✅ فحص سريع قبل فتح الشاشة — من القائمة الرئيسية
     */
    public static boolean canOpen() {
        return SessionManager.getInstance().isAdmin();
    }

    /**
     * فتح الشاشة في Stage مستقل
     */
    public static void open(Stage owner) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    AdminUsersController.class.getResource("/com/safwat/hr/view/user/AdminUsersView.fxml"));
            javafx.scene.Parent root = loader.load();

            Stage stage = new Stage();
            stage.initModality(Modality.WINDOW_MODAL);
            if (owner != null) stage.initOwner(owner);
            stage.setTitle("👥 إدارة المستخدمين");
            stage.setScene(new Scene(root, 1100, 650));
            stage.setMinWidth(900);
            stage.setMinHeight(550);
            ThemeEventBus.register(stage.getScene());
            stage.show();
        } catch (Exception e) {
            AppLogBus.getInstance().log("[AdminUsers] ❌ فشل فتح الشاشة: " + e.getMessage());
            AlertUtil.showError("خطأ", "فشل فتح شاشة إدارة المستخدمين: " + e.getMessage());
        }
    }
}