package com.safwat.hr.notification.ui;

import com.safwat.hr.notification.model.HRNotification;
import com.safwat.hr.notification.service.NotificationService;
import com.safwat.hr.notification.util.FileOpener;
import io.github.palexdev.materialfx.controls.MFXButton;
import javafx.beans.binding.Bindings;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

/**
 * لوحة الإشعارات الكاملة - تعرض وتصفي وتدير الإشعارات.
 * تُستخدم كـ Popup منبثق من زر الجرس في الـ Toolbar.
 */
public class HRNotificationPanel extends VBox {

    private final NotificationService service = NotificationService.getInstance();
    private final FilteredList<HRNotification> filteredList;
    private HRNotification.NotificationType activeFilter = null;

    public HRNotificationPanel() {
        filteredList = new FilteredList<>(service.getAll(), n -> true);
        build();
        setStyle(
                "-fx-background-color: #FFFFFF;" +
                        "-fx-background-radius: 12px;" +
                        "-fx-border-color: #E0E0E0;" +
                        "-fx-border-width: 0.5px;" +
                        "-fx-border-radius: 12px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 20, 0, 0, 6);"
        );
        setPrefWidth(420);
        setMaxHeight(680);
    }

    private void build() {
        getChildren().addAll(
                buildHeader(),
                buildFilterTabs(),
                buildList(),
                buildFooter()
        );
    }

    // ===================== الرأس =====================
    private HBox buildHeader() {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 16, 12, 16));
        header.setStyle("-fx-border-color: transparent transparent #EBEBEB transparent;" +
                "-fx-border-width: 0 0 0.5 0;");

        Label title = new Label("مركز الإشعارات");
        title.setStyle("-fx-font-size:15px;-fx-font-weight:700;-fx-text-fill:#1A1A1A;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label badge = new Label();
        badge.textProperty().bind(
                Bindings.when(service.unreadCountProperty().greaterThan(0))
                        .then(Bindings.concat(service.unreadCountProperty(), " جديد"))
                        .otherwise("")
        );
        badge.visibleProperty().bind(service.unreadCountProperty().greaterThan(0));
        badge.setStyle(
                "-fx-background-color:#FCEBEB;-fx-text-fill:#A32D2D;" +
                        "-fx-font-size:11px;-fx-font-weight:600;" +
                        "-fx-background-radius:10px;-fx-padding:2 8 2 8;"
        );

        MFXButton markAllBtn = new MFXButton("تعليم الكل مقروء");
        markAllBtn.setStyle("-fx-font-size:12px;-fx-text-fill:#185FA5;" +
                "-fx-background-color:transparent;-fx-cursor:hand;");
        markAllBtn.setOnAction(e -> service.markAllAsRead());
        markAllBtn.visibleProperty().bind(service.unreadCountProperty().greaterThan(0));

        header.getChildren().addAll(title, spacer, badge, markAllBtn);
        return header;
    }

    // ===================== تبويبات التصفية =====================
    private HBox buildFilterTabs() {
        HBox tabs = new HBox(6);
        tabs.setPadding(new Insets(10, 12, 8, 12));
        tabs.setStyle("-fx-border-color: transparent transparent #EBEBEB transparent;" +
                "-fx-border-width: 0 0 0.5 0;");
        tabs.setAlignment(Pos.CENTER_LEFT);

        tabs.getChildren().add(buildTab("الكل", null, tabs));
        for (HRNotification.NotificationType type : HRNotification.NotificationType.values()) {
            tabs.getChildren().add(buildTab(type.label, type, tabs));
        }
        return tabs;
    }

    private Label buildTab(String text, HRNotification.NotificationType type, HBox container) {
        Label tab = new Label(text);
        applyTabStyle(tab, type == null, type);

        tab.setOnMouseClicked(e -> {
            activeFilter = type;
            container.getChildren().forEach(node -> {
                if (node instanceof Label lbl) {
                    HRNotification.NotificationType t = (HRNotification.NotificationType) lbl.getUserData();
                    applyTabStyle(lbl, lbl == tab, t);
                }
            });
            filteredList.setPredicate(n -> type == null || n.getType() == type);
        });
        tab.setUserData(type);
        return tab;
    }

    private void applyTabStyle(Label tab, boolean active, HRNotification.NotificationType type) {
        if (active) {
            String color = type != null ? type.color : "#185FA5";
            String bg = type != null ? type.bgColor : "#E6F1FB";
            tab.setStyle(
                    "-fx-background-color:" + bg + ";" +
                            "-fx-text-fill:" + color + ";" +
                            "-fx-font-size:12px;-fx-font-weight:600;" +
                            "-fx-background-radius:6px;-fx-padding:4 10 4 10;-fx-cursor:hand;"
            );
        } else {
            tab.setStyle(
                    "-fx-background-color:transparent;-fx-text-fill:#666666;" +
                            "-fx-font-size:12px;-fx-background-radius:6px;-fx-padding:4 10 4 10;" +
                            "-fx-cursor:hand;-fx-border-color:#E0E0E0;-fx-border-width:0.5px;" +
                            "-fx-border-radius:6px;"
            );
        }
    }

    // ===================== القائمة =====================
    private ListView<HRNotification> buildList() {
        ListView<HRNotification> list = new ListView<>();
        list.setItems(filteredList);
        list.setPrefHeight(480);
        // الحل الرئيسي: ارتفاع ثابت للـ cell يمنع JavaFX من حساب emoji قبل العرض
        list.setFixedCellSize(90);
        list.setCellFactory(lv -> new NotificationCell());
        list.setStyle("-fx-background-color:transparent;");
        list.setPlaceholder(buildEmptyState());
        VBox.setVgrow(list, Priority.ALWAYS);
        return list;
    }

    private StackPane buildEmptyState() {
        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER);
        // نص بدل emoji لتجنب نفس المشكلة في الـ placeholder
        Label icon = new Label("( )");
        icon.setStyle("-fx-font-size:32px;-fx-opacity:0.2;-fx-text-fill:#888888;");
        Label msg = new Label("لا توجد إشعارات");
        msg.setStyle("-fx-font-size:14px;-fx-text-fill:#AAAAAA;");
        box.getChildren().addAll(icon, msg);
        return new StackPane(box);
    }

    // ===================== التذييل =====================
    private HBox buildFooter() {
        HBox footer = new HBox();
        footer.setPadding(new Insets(10, 16, 10, 16));
        footer.setAlignment(Pos.CENTER);
        footer.setStyle("-fx-border-color: #EBEBEB transparent transparent transparent;" +
                "-fx-border-width: 0.5 0 0 0;");

        Label countLbl = new Label();
        countLbl.textProperty().bind(
                Bindings.createStringBinding(
                        () -> filteredList.size() + " إشعار",
                        filteredList
                )
        );
        countLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#AAAAAA;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        MFXButton clearBtn = new MFXButton("مسح الكل");
        clearBtn.setStyle("-fx-font-size:12px;-fx-text-fill:#AA3333;" +
                "-fx-background-color:transparent;-fx-cursor:hand;");
        clearBtn.setOnAction(e -> {
            if (activeFilter == null)
                service.clearAll();
            else
                service.getAll().removeIf(n -> n.getType() == activeFilter);
        });

        footer.getChildren().addAll(countLbl, spacer, clearBtn);
        return footer;
    }

    // ===================== خلية الإشعار =====================
    private class NotificationCell extends ListCell<HRNotification> {

        @Override
        protected void updateItem(HRNotification item, boolean empty) {
            super.updateItem(item, empty);
            setText(null);

            if (empty || item == null) {
                setGraphic(null);
                setStyle("-fx-background-color:transparent;");
                return;
            }

            setGraphic(buildCell(item));
        }

        private HBox buildCell(HRNotification item) {
            // نقطة الحالة
            Circle dot = new Circle(5);
            dot.setFill(item.isRead() ? Color.TRANSPARENT : Color.web(item.getType().color));
            dot.setStroke(item.isRead() ? Color.web("#CCCCCC") : Color.TRANSPARENT);
            dot.setStrokeWidth(item.isRead() ? 1.5 : 0);

            // الأيقونة — نص بدل emoji لتجنب bug الـ PrismTextLayout
            Rectangle iconBg = new Rectangle(38, 38);
            iconBg.setArcWidth(8);
            iconBg.setArcHeight(8);
            iconBg.setFill(Color.web(item.getType().bgColor));
            Label iconLbl = new Label(getIcon(item.getType()));
            iconLbl.setStyle("-fx-font-size:16px;");
            // ارتفاع وعرض ثابت للأيقونة
            iconLbl.setMinSize(38, 38);
            iconLbl.setMaxSize(38, 38);
            iconLbl.setAlignment(Pos.CENTER);
            StackPane iconBox = new StackPane(iconBg, iconLbl);
            iconBox.setMinSize(38, 38);
            iconBox.setMaxSize(38, 38);

            // العنوان
            Label titleLbl = new Label(item.getTitle());
            titleLbl.setStyle("-fx-font-size:13px;-fx-font-weight:" +
                    (item.isRead() ? "400" : "700") + ";-fx-text-fill:#1A1A1A;");
            titleLbl.setMaxWidth(230);
            titleLbl.setMinHeight(18);

            // الرسالة
            Label msgLbl = new Label(item.getMessage());
            msgLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#666666;");
            msgLbl.setMaxWidth(230);
            msgLbl.setWrapText(true);
            msgLbl.setMinHeight(16);

            // الوقت
            Label timeLbl = new Label(item.getFormattedTime());
            timeLbl.setStyle("-fx-font-size:11px;-fx-text-fill:#AAAAAA;");
            timeLbl.setMinHeight(14);

            // أزرار الإجراءات
            HBox actions = new HBox(8);
            actions.setAlignment(Pos.CENTER_LEFT);

            if (item.getActionLabel() != null && !item.getActionLabel().isEmpty()) {
                MFXButton actionBtn = new MFXButton("< " + item.getActionLabel());
                actionBtn.setStyle("-fx-font-size:11px;-fx-text-fill:#185FA5;" +
                        "-fx-background-color:transparent;-fx-cursor:hand;-fx-padding:0;");
                actionBtn.setMinHeight(16);
                actionBtn.setOnAction(e -> {
                    e.consume();
                    service.markAsRead(item);
                });
                actions.getChildren().add(actionBtn);
            }

            if (item.hasFile()) {
                MFXButton fileBtn = new MFXButton("[f] فتح الملف");
                fileBtn.setStyle("-fx-font-size:11px;-fx-text-fill:#0F6E56;" +
                        "-fx-background-color:transparent;-fx-cursor:hand;-fx-padding:0;");
                fileBtn.setMinHeight(16);
                fileBtn.setOnAction(e -> {
                    e.consume();
                    FileOpener.open(item.getFilePath());
                });
                actions.getChildren().add(fileBtn);
            }

            HBox meta = new HBox(8, timeLbl, actions);
            meta.setAlignment(Pos.CENTER_LEFT);

            VBox texts = new VBox(3, titleLbl, msgLbl, meta);
            texts.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(texts, Priority.ALWAYS);

            String border = (item.getPriority() == HRNotification.Priority.URGENT)
                    ? "-fx-border-color:#A32D2D transparent #F2F2F2 transparent;-fx-border-width:0 0 0.5 3;"
                    : "-fx-border-color:transparent transparent #F2F2F2 transparent;-fx-border-width:0 0 0.5 0;";

            String bg = item.isRead() ? "#FFFFFF" : "#F8F5FF";

            HBox root = new HBox(10, dot, iconBox, texts);
            root.setAlignment(Pos.CENTER_LEFT);
            root.setPadding(new Insets(10, 16, 10, 14));
            root.setPrefHeight(90);
            root.setStyle(border + "-fx-background-color:" + bg + ";-fx-cursor:hand;");
            root.setOnMouseClicked(e -> service.markAsRead(item));

            return root;
        }

        private String getIcon(HRNotification.NotificationType type) {
            // نصوص ASCII بدل emoji لتجنب bug PrismTextLayout في JavaFX 25
            return switch (type) {
                case EMPLOYEE -> "EMP";
                case SALARY -> "SAL";
                case LEAVE -> "LVE";
                case TRAINING -> "TRN";
                case TASK -> "TSK";
                case SYSTEM -> "SYS";
            };
        }
    }
}