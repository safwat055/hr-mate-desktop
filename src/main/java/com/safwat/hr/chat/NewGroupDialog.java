package com.safwat.hr.chat;

import com.safwat.hr.chat.ChatDTOs.UserSearchDTO;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.List;

/**
 * Dialog إنشاء مجموعة — يرجع List<Long> بـ IDs المشاركين + اسم المجموعة.
 * <p>
 * الاستخدام:
 * <pre>
 *   new NewGroupDialog(window).showAndWait()
 *       .ifPresent(result -> {
 *           String name = result.name();
 *           List<Long> ids = result.participantIds();
 *       });
 * </pre>
 */
public class NewGroupDialog extends Dialog<NewGroupDialog.GroupRequest> {

    private final TextField groupNameField = new TextField();
    private final TextField searchField = new TextField();
    private final ListView<UserSearchDTO> searchResults = new ListView<>();
    private final ListView<UserSearchDTO> selectedUsers = new ListView<>();
    private final Label statusLabel = new Label();
    private final ButtonType createBtn = new ButtonType("إنشاء المجموعة", ButtonBar.ButtonData.OK_DONE);
    public NewGroupDialog(Window owner) {
        initOwner(owner);
        setTitle("مجموعة جديدة");
        setHeaderText(null);
        getDialogPane().setPrefWidth(480);

        buildContent();
        setupSearch();
        setupResultConverter();

        // الزر معطل لحد ما يكون فيه اسم + عضو واحد على الأقل
        updateCreateButton();
        groupNameField.textProperty().addListener((o, ov, nv) -> updateCreateButton());
        selectedUsers.getItems().addListener(
                (javafx.collections.ListChangeListener<UserSearchDTO>) c -> updateCreateButton()
        );
    }

    private void buildContent() {
        getDialogPane().getButtonTypes().addAll(createBtn, ButtonType.CANCEL);

        // اسم المجموعة
        groupNameField.setPromptText("اسم المجموعة...");
        groupNameField.setMaxWidth(Double.MAX_VALUE);

        // بحث
        searchField.setPromptText("ابحث عن موظف للإضافة...");
        searchField.setMaxWidth(Double.MAX_VALUE);

        statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #9AA3B0;");
        statusLabel.setText("ابحث وأضف أعضاء المجموعة");

        searchResults.setPrefHeight(150);
        searchResults.setCellFactory(lv -> new UserCell(true));
        searchResults.setPlaceholder(new Label("لا توجد نتائج"));

        // أعضاء مختارين
        Label selectedLabel = new Label("الأعضاء المضافون:");
        selectedLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        selectedUsers.setPrefHeight(100);
        selectedUsers.setCellFactory(lv -> new UserCell(false));
        selectedUsers.setPlaceholder(new Label("لم يتم إضافة أعضاء بعد"));

        VBox content = new VBox(10);
        content.setPadding(new Insets(16));
        content.getChildren().addAll(
                new Label("اسم المجموعة:"), groupNameField,
                new Separator(),
                new Label("إضافة أعضاء:"), searchField, statusLabel, searchResults,
                new Separator(),
                selectedLabel, selectedUsers
        );

        getDialogPane().setContent(content);
        Platform.runLater(groupNameField::requestFocus);
    }

    private void setupSearch() {
        searchField.textProperty().addListener((obs, old, query) -> {
            if (query == null || query.trim().length() < 2) {
                searchResults.getItems().clear();
                statusLabel.setText("اكتب حرفين على الأقل للبحث");
                return;
            }
            statusLabel.setText("جاري البحث...");
            ChatApiService.searchUsers(query.trim()).thenAccept(res ->
                    Platform.runLater(() -> {
                        if (res.isSuccess() && res.getData() != null) {
                            // استثنِ المختارين بالفعل
                            List<Long> alreadyAdded = selectedUsers.getItems().stream()
                                    .map(UserSearchDTO::getId).toList();
                            List<UserSearchDTO> filtered = res.getData().stream()
                                    .filter(u -> !alreadyAdded.contains(u.getId()))
                                    .toList();
                            searchResults.getItems().setAll(filtered);
                            statusLabel.setText(filtered.isEmpty() ? "لا توجد نتائج"
                                    : filtered.size() + " نتيجة — اضغط لإضافة");
                        }
                    })
            );
        });

        // اضغط على نتيجة → أضفها
        searchResults.setOnMouseClicked(e -> {
            UserSearchDTO selected = searchResults.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            selectedUsers.getItems().add(selected);
            searchResults.getItems().remove(selected);
            searchField.clear();
        });
    }

    private void setupResultConverter() {
        setResultConverter(btn -> {
            if (btn == createBtn) {
                List<Long> ids = selectedUsers.getItems().stream()
                        .map(UserSearchDTO::getId)
                        .toList();
                return new GroupRequest(groupNameField.getText().trim(), ids);
            }
            return null;
        });
    }

    private void updateCreateButton() {
        boolean valid = !groupNameField.getText().trim().isEmpty()
                && !selectedUsers.getItems().isEmpty();
        getDialogPane().lookupButton(createBtn).setDisable(!valid);
    }

    public record GroupRequest(String name, List<Long> participantIds) {
    }

    // ── Cell ──────────────────────────────────────────────────────────

    private static class UserCell extends ListCell<UserSearchDTO> {
        private final boolean showAddIcon;
        private final HBox root = new HBox(10);
        private final StackPane avatar = new StackPane();
        private final Label initials = new Label();
        private final VBox info = new VBox(2);
        private final Label name = new Label();
        private final Label meta = new Label();
        private final Button action = new Button();

        UserCell(boolean showAddIcon) {
            this.showAddIcon = showAddIcon;
            avatar.setPrefSize(34, 34);
            avatar.setMinSize(34, 34);
            avatar.setMaxSize(34, 34);
            avatar.setStyle("-fx-background-color: #185FA5; -fx-background-radius: 17;");
            initials.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: white;");
            avatar.getChildren().add(initials);

            name.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
            meta.setStyle("-fx-font-size: 11px; -fx-text-fill: #6B7280;");
            info.getChildren().addAll(name, meta);
            HBox.setHgrow(info, Priority.ALWAYS);

            action.setText(showAddIcon ? "+" : "x");
            action.setStyle("-fx-background-color: " + (showAddIcon ? "#185FA5" : "#E74C3C")
                    + "; -fx-text-fill: white; -fx-background-radius: 12; -fx-min-width: 24; -fx-min-height: 24; -fx-cursor: hand;");

            root.setAlignment(Pos.CENTER_LEFT);
            root.setPadding(new Insets(6, 10, 6, 10));
            root.getChildren().addAll(avatar, info, action);
        }

        @Override
        protected void updateItem(UserSearchDTO u, boolean empty) {
            super.updateItem(u, empty);
            if (empty || u == null) {
                setGraphic(null);
                return;
            }
            initials.setText(u.getAvatarInitials() != null ? u.getAvatarInitials() : "?");
            String color = u.getAvatarColor() != null ? u.getAvatarColor() : "#185FA5";
            avatar.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 17;");
            name.setText(u.getDisplayName() != null ? u.getDisplayName() : u.getUsername());
            String m = "";
            if (u.getJobTitle() != null) m += u.getJobTitle();
            if (u.getDepartmentName() != null) m += " · " + u.getDepartmentName();
            meta.setText(m);
            action.setOnAction(e -> {
                ListView<UserSearchDTO> lv = getListView();
                lv.getItems().remove(u);
            });
            setGraphic(root);
        }
    }
}