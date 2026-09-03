package com.safwat.hr.controller.chat.ui;


import com.safwat.hr.controller.chat.dto.ChatDTOs;
import com.safwat.hr.controller.chat.service.ChatApiService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * Dialog بحث عن مستخدم لبدء محادثة جديدة.
 * <p>
 * الاستخدام:
 * <pre>
 *   new NewConversationDialog(window)
 *       .showAndWait()
 *       .ifPresent(userId -> { ... });
 * </pre>
 */
public class NewConversationDialog extends Dialog<Long> {

    private final TextField searchField = new TextField();
    private final ListView<ChatDTOs.UserSearchDTO> resultList = new ListView<>();
    private final Label statusLabel = new Label("ابحث باسم الموظف أو اسم المستخدم");
    private final ButtonType startBtn = new ButtonType("بدء المحادثة", ButtonBar.ButtonData.OK_DONE);

    public NewConversationDialog(Window owner) {
        initOwner(owner);
        setTitle("محادثة جديدة");
        setHeaderText(null);

        buildContent();
        setupResultConverter();
        setupSearch();

        // الزر معطل لحد ما المستخدم يختار شخص
        getDialogPane().lookupButton(startBtn).setDisable(true);
        resultList.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, selected) ->
                        getDialogPane().lookupButton(startBtn).setDisable(selected == null)
        );
    }

    private void buildContent() {
        getDialogPane().getButtonTypes().addAll(startBtn, ButtonType.CANCEL);

        searchField.setPromptText("ابحث باسم الموظف...");
        searchField.setPrefWidth(380);

        resultList.setPrefHeight(260);
        resultList.setCellFactory(lv -> new UserSearchCell());
        resultList.setPlaceholder(new Label("لا توجد نتائج"));

        statusLabel.getStyleClass().add("search-status");
        statusLabel.setPadding(new Insets(4, 0, 0, 0));

        VBox content = new VBox(10);
        content.setPadding(new Insets(16));
        content.getChildren().addAll(
                new Label("ابحث عن موظف:"),
                searchField,
                statusLabel,
                resultList
        );

        getDialogPane().setContent(content);

        // Focus على حقل البحث
        Platform.runLater(searchField::requestFocus);
    }

    private void setupSearch() {
        searchField.textProperty().addListener((obs, old, query) -> {
            if (query == null || query.trim().length() < 2) {
                resultList.getItems().clear();
                statusLabel.setText("ابحث باسم الموظف أو اسم المستخدم (حرفان على الأقل)");
                return;
            }

            statusLabel.setText("جاري البحث...");

            ChatApiService.searchUsers(query.trim()).thenAccept(res ->
                    Platform.runLater(() -> {
                        if (res.isSuccess() && res.getData() != null) {
                            resultList.getItems().setAll(res.getData());
                            statusLabel.setText(
                                    res.getData().isEmpty()
                                            ? "لا توجد نتائج"
                                            : "تم العثور على " + res.getData().size() + " نتيجة"
                            );
                        } else {
                            statusLabel.setText("فشل البحث: " + res.getMessage());
                        }
                    })
            );
        });

        // ✅ تم الإصلاح: Double-click يختار مباشرة
        resultList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && resultList.getSelectionModel().getSelectedItem() != null) {
                javafx.scene.Node btn = getDialogPane().lookupButton(startBtn);
                if (btn instanceof Button b && !b.isDisabled()) {
                    b.fire();
                }
            }
        });
    }

    private void setupResultConverter() {
        setResultConverter(btn -> {
            if (btn == startBtn) {
                ChatDTOs.UserSearchDTO selected = resultList.getSelectionModel().getSelectedItem();
                return selected != null ? selected.getId() : null;
            }
            return null;
        });
    }

    // ─────────────────────────────────────────────────────────────────
    //  User Search Cell
    // ─────────────────────────────────────────────────────────────────

    private static class UserSearchCell extends ListCell<ChatDTOs.UserSearchDTO> {

        private final HBox root = new HBox(10);
        private final StackPane avatar = new StackPane();
        private final Label initials = new Label();
        private final VBox info = new VBox(2);
        private final Label nameLabel = new Label();
        private final Label metaLabel = new Label();

        UserSearchCell() {
            avatar.setPrefSize(38, 38);
            avatar.setMinSize(38, 38);
            avatar.setMaxSize(38, 38);
            avatar.getStyleClass().add("conv-avatar");
            initials.getStyleClass().add("conv-avatar-initials");
            avatar.getChildren().add(initials);

            nameLabel.getStyleClass().add("user-search-name");
            metaLabel.getStyleClass().add("user-search-meta");
            info.getChildren().addAll(nameLabel, metaLabel);
            HBox.setHgrow(info, Priority.ALWAYS);

            root.setAlignment(Pos.CENTER_LEFT);
            root.setPadding(new Insets(8, 12, 8, 12));
            root.getChildren().addAll(avatar, info);
        }

        @Override
        protected void updateItem(ChatDTOs.UserSearchDTO user, boolean empty) {
            super.updateItem(user, empty);
            if (empty || user == null) {
                setGraphic(null);
                return;
            }

            initials.setText(user.getAvatarInitials() != null ? user.getAvatarInitials() : "?");
            String color = user.getAvatarColor() != null ? user.getAvatarColor() : "#185FA5";
            avatar.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 19;");

            String display = user.getDisplayName() != null
                    ? user.getDisplayName() : user.getUsername();
            nameLabel.setText(display);

            String meta = "";
            if (user.getJobTitle() != null) meta += user.getJobTitle();
            if (user.getDepartmentName() != null) meta += " · " + user.getDepartmentName();
            metaLabel.setText(meta);

            setGraphic(root);
        }
    }
}