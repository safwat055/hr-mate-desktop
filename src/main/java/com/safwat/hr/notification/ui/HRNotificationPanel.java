package com.safwat.hr.notification.ui;

import com.safwat.hr.notification.model.HRNotification;
import com.safwat.hr.notification.model.HRNotification.Attachment;
import com.safwat.hr.notification.model.HRNotification.NotificationCategory;
import com.safwat.hr.notification.model.HRNotification.NotificationType;
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
import javafx.stage.Stage;

/**
 * لوحة الإشعارات — تعرض إشعارات النظام والرسائل في مكان واحد.
 * كل نوع له خلية عرض مختلفة.
 * <p>
 * التبويبات:
 * الكل | إشعارات | رسائل | [أنواع النظام...]
 */
public class HRNotificationPanel extends VBox {

    private final Stage owner;
    private final NotificationService service = NotificationService.getInstance();
    private final FilteredList<HRNotification> filteredList;
    private TabFilter activeTab = TabFilter.ALL;

    public HRNotificationPanel(Stage owner) {
        this.owner = owner;
        filteredList = new FilteredList<>(service.getAll(), n -> true);
        build();

        setStyle(
                "-fx-background-color:#FFFFFF;" +
                        "-fx-background-radius:12px;" +
                        "-fx-border-color:#E0E0E0;" +
                        "-fx-border-width:0.5px;" +
                        "-fx-border-radius:12px;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.18),20,0,0,6);"
        );
        setPrefWidth(440);
        setMaxHeight(700);
    }

    private void build() {
        getChildren().addAll(
                buildHeader(),
                buildMainTabs(),
                buildList(),
                buildFooter()
        );
    }

    // ===================== الرأس =====================
    private HBox buildHeader() {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 16, 12, 16));
        header.setStyle(
                "-fx-border-color:transparent transparent #EBEBEB transparent;" +
                        "-fx-border-width:0 0 0.5 0;"
        );

        Label title = new Label("المركز");
        title.setStyle("-fx-font-size:15px;-fx-font-weight:700;-fx-text-fill:#1A1A1A;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // باج الكل
        Label badge = new Label();
        badge.textProperty().bind(
                Bindings.when(service.unreadCountProperty().greaterThan(0))
                        .then(Bindings.concat(service.unreadCountProperty(), " جديد"))
                        .otherwise("")
        );
        badge.visibleProperty().bind(service.unreadCountProperty().greaterThan(0));
        badge.managedProperty().bind(badge.visibleProperty());
        badge.setStyle(
                "-fx-background-color:#FCEBEB;-fx-text-fill:#A32D2D;" +
                        "-fx-font-size:11px;-fx-font-weight:600;" +
                        "-fx-background-radius:10px;-fx-padding:2 8 2 8;"
        );

        MFXButton markAllBtn = new MFXButton("تعليم الكل مقروء");
        markAllBtn.setStyle(
                "-fx-font-size:12px;-fx-text-fill:#185FA5;" +
                        "-fx-background-color:transparent;-fx-cursor:hand;"
        );
        markAllBtn.setOnAction(e -> service.markAllAsRead());
        markAllBtn.visibleProperty().bind(service.unreadCountProperty().greaterThan(0));
        markAllBtn.managedProperty().bind(markAllBtn.visibleProperty());

        header.getChildren().addAll(title, spacer, badge, markAllBtn);
        return header;
    }

    // ===================== التبويبات الرئيسية =====================
    private VBox buildMainTabs() {
        VBox wrapper = new VBox(0);
        wrapper.setStyle(
                "-fx-border-color:transparent transparent #EBEBEB transparent;" +
                        "-fx-border-width:0 0 0.5 0;"
        );

        // الصف الأول: الكل | إشعارات | رسائل
        HBox mainRow = new HBox(6);
        mainRow.setPadding(new Insets(10, 12, 6, 12));
        mainRow.setAlignment(Pos.CENTER_LEFT);

        Label tabAll = buildTab("الكل", "#185FA5", "#E6F1FB", TabFilter.ALL, mainRow, null);
        Label tabSys = buildTab("إشعارات", "#5F5E5A", "#F1EFE8", TabFilter.SYSTEM_ALL, mainRow, null);
        Label tabMsg = buildTab("رسائل", "#0F6E56", "#E6F5F1", TabFilter.MESSAGES, mainRow, null);
        mainRow.getChildren().addAll(tabAll, tabSys, tabMsg);

        // الصف الثاني: أنواع النظام (يظهر فقط لما تبويب إشعارات نشط)
        HBox subRow = new HBox(4);
        subRow.setPadding(new Insets(0, 12, 8, 12));
        subRow.setAlignment(Pos.CENTER_LEFT);
        subRow.setVisible(false);
        subRow.setManaged(false);

        for (NotificationType type : NotificationType.values()) {
            if (type == NotificationType.MESSAGE) continue;
            TabFilter tf = switch (type) {
                case EMPLOYEE -> TabFilter.EMPLOYEE;
                case SALARY -> TabFilter.SALARY;
                case LEAVE -> TabFilter.LEAVE;
                case TRAINING -> TabFilter.TRAINING;
                case TASK -> TabFilter.TASK;
                default -> TabFilter.SYSTEM_TYPE;
            };
            Label sub = buildSmallTab(type.label, type.color, type.bgColor, tf, subRow);
            subRow.getChildren().add(sub);
        }

        // إظهار/إخفاء الصف الثاني حسب التبويب النشط
        tabSys.setOnMouseClicked(e -> {
            setActiveTab(TabFilter.SYSTEM_ALL, mainRow, null);
            subRow.setVisible(true);
            subRow.setManaged(true);
        });
        tabAll.setOnMouseClicked(e -> {
            setActiveTab(TabFilter.ALL, mainRow, null);
            subRow.setVisible(false);
            subRow.setManaged(false);
        });
        tabMsg.setOnMouseClicked(e -> {
            setActiveTab(TabFilter.MESSAGES, mainRow, null);
            subRow.setVisible(false);
            subRow.setManaged(false);
        });

        wrapper.getChildren().addAll(mainRow, subRow);
        return wrapper;
    }

    private void setActiveTab(TabFilter filter, HBox container, HBox subContainer) {
        activeTab = filter;
        applyFilter();
        // تحديث مظهر التبويبات في الـ container
        container.getChildren().forEach(node -> {
            if (node instanceof Label lbl && lbl.getUserData() instanceof TabFilter tf) {
                boolean active = tf == filter;
                String[] data = (String[]) lbl.getProperties().get("colors");
                if (data != null)
                    applyTabStyle(lbl, active, data[0], data[1]);
            }
        });
    }

    private Label buildTab(String text, String color, String bg,
                           TabFilter filter, HBox container, HBox subRow) {
        Label tab = new Label(text);
        tab.setUserData(filter);
        tab.getProperties().put("colors", new String[]{color, bg});
        applyTabStyle(tab, filter == TabFilter.ALL, color, bg);
        tab.setPrefHeight(28);
        return tab;
    }

    private Label buildSmallTab(String text, String color, String bg,
                                TabFilter filter, HBox container) {
        Label tab = new Label(text);
        tab.setUserData(filter);
        tab.getProperties().put("colors", new String[]{color, bg});
        applySmallTabStyle(tab, false, color, bg);

        tab.setOnMouseClicked(e -> {
            activeTab = filter;
            applyFilter();
            container.getChildren().forEach(node -> {
                if (node instanceof Label lbl && lbl.getUserData() instanceof TabFilter tf) {
                    String[] data = (String[]) lbl.getProperties().get("colors");
                    if (data != null)
                        applySmallTabStyle(lbl, tf == filter, data[0], data[1]);
                }
            });
        });
        return tab;
    }

    private void applyTabStyle(Label tab, boolean active, String color, String bg) {
        if (active) {
            tab.setStyle(
                    "-fx-background-color:" + bg + ";" +
                            "-fx-text-fill:" + color + ";" +
                            "-fx-font-size:13px;-fx-font-weight:700;" +
                            "-fx-background-radius:8px;-fx-padding:5 14 5 14;-fx-cursor:hand;"
            );
        } else {
            tab.setStyle(
                    "-fx-background-color:transparent;-fx-text-fill:#666666;" +
                            "-fx-font-size:13px;-fx-background-radius:8px;" +
                            "-fx-padding:5 14 5 14;-fx-cursor:hand;" +
                            "-fx-border-color:#E0E0E0;-fx-border-width:0.5px;-fx-border-radius:8px;"
            );
        }
    }

    private void applySmallTabStyle(Label tab, boolean active, String color, String bg) {
        if (active) {
            tab.setStyle(
                    "-fx-background-color:" + bg + ";-fx-text-fill:" + color + ";" +
                            "-fx-font-size:11px;-fx-font-weight:600;" +
                            "-fx-background-radius:6px;-fx-padding:3 8 3 8;-fx-cursor:hand;"
            );
        } else {
            tab.setStyle(
                    "-fx-background-color:transparent;-fx-text-fill:#888888;" +
                            "-fx-font-size:11px;-fx-background-radius:6px;" +
                            "-fx-padding:3 8 3 8;-fx-cursor:hand;" +
                            "-fx-border-color:#E8E8E8;-fx-border-width:0.5px;-fx-border-radius:6px;"
            );
        }
    }

    private void applyFilter() {
        filteredList.setPredicate(n -> switch (activeTab) {
            case ALL -> true;
            case SYSTEM_ALL -> n.getCategory() == NotificationCategory.SYSTEM;
            case MESSAGES -> n.getCategory() == NotificationCategory.MESSAGE;
            case EMPLOYEE -> n.getType() == NotificationType.EMPLOYEE;
            case SALARY -> n.getType() == NotificationType.SALARY;
            case LEAVE -> n.getType() == NotificationType.LEAVE;
            case TRAINING -> n.getType() == NotificationType.TRAINING;
            case TASK -> n.getType() == NotificationType.TASK;
            case SYSTEM_TYPE -> n.getType() == NotificationType.SYSTEM;
        });
    }

    // ===================== القائمة =====================
    private ListView<HRNotification> buildList() {
        ListView<HRNotification> list = new ListView<>();
        list.setItems(filteredList);
        list.setPrefHeight(500);
        list.setFixedCellSize(96);
        list.setCellFactory(lv -> new NotificationCell());
        list.setStyle("-fx-background-color:transparent;");
        list.setPlaceholder(buildEmptyState());
        VBox.setVgrow(list, Priority.ALWAYS);
        return list;
    }

    private StackPane buildEmptyState() {
        Label msg = new Label("لا توجد إشعارات");
        msg.setStyle("-fx-font-size:14px;-fx-text-fill:#AAAAAA;");
        return new StackPane(msg);
    }

    // ===================== التذييل =====================
    private HBox buildFooter() {
        HBox footer = new HBox();
        footer.setPadding(new Insets(10, 16, 10, 16));
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setStyle(
                "-fx-border-color:#EBEBEB transparent transparent transparent;" +
                        "-fx-border-width:0.5 0 0 0;"
        );

        Label countLbl = new Label();
        countLbl.textProperty().bind(
                Bindings.createStringBinding(
                        () -> filteredList.size() + " عنصر",
                        filteredList
                )
        );
        countLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#AAAAAA;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        MFXButton clearBtn = new MFXButton("مسح الكل");
        clearBtn.setStyle(
                "-fx-font-size:12px;-fx-text-fill:#AA3333;" +
                        "-fx-background-color:transparent;-fx-cursor:hand;"
        );
        clearBtn.setOnAction(e -> {
            switch (activeTab) {
                case ALL -> service.clearAll();
                case MESSAGES -> service.getAll().removeIf(HRNotification::isMessage);
                case SYSTEM_ALL -> service.getAll().removeIf(n ->
                        n.getCategory() == NotificationCategory.SYSTEM);
                default -> {
                    NotificationType target = resolveType(activeTab);
                    if (target != null)
                        service.getAll().removeIf(n -> n.getType() == target);
                }
            }
        });

        footer.getChildren().addAll(countLbl, spacer, clearBtn);
        return footer;
    }

    private NotificationType resolveType(TabFilter f) {
        return switch (f) {
            case EMPLOYEE -> NotificationType.EMPLOYEE;
            case SALARY -> NotificationType.SALARY;
            case LEAVE -> NotificationType.LEAVE;
            case TRAINING -> NotificationType.TRAINING;
            case TASK -> NotificationType.TASK;
            case SYSTEM_TYPE -> NotificationType.SYSTEM;
            default -> null;
        };
    }

    // تصفية مركبة: category + type
    private enum TabFilter {
        ALL, SYSTEM_ALL, MESSAGES,
        EMPLOYEE, SALARY, LEAVE, TRAINING, TASK, SYSTEM_TYPE
    }

    // ===================== خلية الإشعار (مصنع) =====================
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
            // اختار الخلية المناسبة حسب النوع
            setGraphic(item.isMessage()
                    ? buildMessageCell(item)
                    : buildSystemCell(item));
        }

        // =================== خلية إشعار النظام ===================
        private HBox buildSystemCell(HRNotification item) {
            // نقطة القراءة
            Circle dot = new Circle(5);
            dot.setFill(item.isRead()
                    ? Color.TRANSPARENT : Color.web(item.getType().color));
            dot.setStroke(item.isRead() ? Color.web("#CCCCCC") : Color.TRANSPARENT);
            dot.setStrokeWidth(item.isRead() ? 1.5 : 0);

            // أيقونة النوع
            Rectangle iconBg = new Rectangle(38, 38);
            iconBg.setArcWidth(8);
            iconBg.setArcHeight(8);
            iconBg.setFill(Color.web(item.getType().bgColor));
            Label iconLbl = new Label(getSystemIcon(item.getType()));
            iconLbl.setStyle("-fx-font-size:13px;-fx-font-weight:700;" +
                    "-fx-text-fill:" + item.getType().color + ";");
            iconLbl.setMinSize(38, 38);
            iconLbl.setMaxSize(38, 38);
            iconLbl.setAlignment(Pos.CENTER);
            StackPane iconBox = new StackPane(iconBg, iconLbl);
            iconBox.setMinSize(38, 38);
            iconBox.setMaxSize(38, 38);

            // النصوص
            Label titleLbl = new Label(item.getTitle());
            titleLbl.setStyle("-fx-font-size:13px;-fx-font-weight:" +
                    (item.isRead() ? "400" : "700") + ";-fx-text-fill:#1A1A1A;");
            titleLbl.setMaxWidth(280);
            titleLbl.setMinHeight(16);

            Label msgLbl = new Label(item.getMessage());
            msgLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#666666;");
            msgLbl.setMaxWidth(280);
            msgLbl.setWrapText(false);
            msgLbl.setMinHeight(15);

            // الوقت + الإجراءات
            Label timeLbl = new Label(item.getFormattedTime());
            timeLbl.setStyle("-fx-font-size:11px;-fx-text-fill:#AAAAAA;");

            HBox actions = buildSystemActions(item);

            HBox meta = new HBox(8, timeLbl, actions);
            meta.setAlignment(Pos.CENTER_LEFT);

            // المرفقات (لو في مرفق واحد بسيط)
            HBox attachRow = buildAttachmentsRow(item, 260);

            VBox texts = new VBox(2, titleLbl, msgLbl, meta);
            if (item.hasAttachments()) texts.getChildren().add(attachRow);
            texts.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(texts, Priority.ALWAYS);

            return buildRootCell(item, dot, iconBox, texts);
        }

        // =================== خلية رسالة المستخدم ===================
        private HBox buildMessageCell(HRNotification item) {
            // نقطة القراءة
            Circle dot = new Circle(5);
            dot.setFill(item.isRead()
                    ? Color.TRANSPARENT : Color.web("#0F6E56"));
            dot.setStroke(item.isRead() ? Color.web("#CCCCCC") : Color.TRANSPARENT);
            dot.setStrokeWidth(item.isRead() ? 1.5 : 0);

            // صورة رمزية للمرسل
            Circle avatarCircle = new Circle(19);
            avatarCircle.setFill(Color.web("#0F6E56"));
            Label avatarLbl = new Label(item.getAvatarInitials());
            avatarLbl.setStyle(
                    "-fx-font-size:12px;-fx-font-weight:700;-fx-text-fill:white;"
            );
            StackPane avatarBox = new StackPane(avatarCircle, avatarLbl);
            avatarBox.setMinSize(38, 38);
            avatarBox.setMaxSize(38, 38);

            // اسم المرسل + الوقت في نفس الصف
            Label senderLbl = new Label(item.getSenderName() != null
                    ? item.getSenderName() : "مجهول");
            senderLbl.setStyle("-fx-font-size:13px;-fx-font-weight:" +
                    (item.isRead() ? "400" : "700") + ";-fx-text-fill:#1A1A1A;");

            Region rowSpacer = new Region();
            HBox.setHgrow(rowSpacer, Priority.ALWAYS);

            Label timeLbl = new Label(item.getFormattedTime());
            timeLbl.setStyle("-fx-font-size:11px;-fx-text-fill:#AAAAAA;");

            HBox topRow = new HBox(4, senderLbl, rowSpacer, timeLbl);
            topRow.setAlignment(Pos.CENTER_LEFT);
            topRow.setMaxWidth(280);

            // موضوع + معاينة
            Label subjectLbl = new Label(item.getTitle());
            subjectLbl.setStyle("-fx-font-size:12px;-fx-font-weight:500;-fx-text-fill:#333333;");
            subjectLbl.setMaxWidth(280);

            Label previewLbl = new Label(item.getMessage());
            previewLbl.setStyle("-fx-font-size:11px;-fx-text-fill:#888888;");
            previewLbl.setMaxWidth(280);
            previewLbl.setMinHeight(14);

            // مرفقات + زر فتح
            HBox bottomRow = new HBox(8);
            bottomRow.setAlignment(Pos.CENTER_LEFT);

            if (item.hasAttachments()) {
                Label attachLbl = new Label(
                        "[" + item.getAttachments().size() + " مرفق]");
                attachLbl.setStyle("-fx-font-size:11px;-fx-text-fill:#0F6E56;");
                bottomRow.getChildren().add(attachLbl);
            }

            Region bSpacer = new Region();
            HBox.setHgrow(bSpacer, Priority.ALWAYS);

            MFXButton openBtn = new MFXButton("فتح الرسالة >");
            openBtn.setStyle(
                    "-fx-font-size:11px;-fx-text-fill:#0F6E56;" +
                            "-fx-background-color:#E6F5F1;-fx-background-radius:6px;" +
                            "-fx-cursor:hand;-fx-padding:3 8 3 8;"
            );
            openBtn.setOnAction(e -> {
                e.consume();
                service.markAsRead(item);
                openMessageDetail(item);
            });
            bottomRow.getChildren().addAll(bSpacer, openBtn);

            VBox texts = new VBox(2, topRow, subjectLbl, previewLbl, bottomRow);
            texts.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(texts, Priority.ALWAYS);

            return buildRootCell(item, dot, avatarBox, texts);
        }

        // =================== مشترك ===================
        private HBox buildRootCell(HRNotification item,
                                   Circle dot, StackPane icon, VBox texts) {
            String border = (item.getPriority() == HRNotification.Priority.URGENT)
                    ? "-fx-border-color:#A32D2D transparent #F2F2F2 transparent;" +
                    "-fx-border-width:0 0 0.5 3;"
                    : "-fx-border-color:transparent transparent #F2F2F2 transparent;" +
                    "-fx-border-width:0 0 0.5 0;";

            String bg = item.isRead() ? "#FFFFFF" : "#F8F5FF";
            if (item.isMessage() && !item.isRead()) bg = "#F0FAF7";

            HBox root = new HBox(10, dot, icon, texts);
            root.setAlignment(Pos.CENTER_LEFT);
            root.setPadding(new Insets(10, 14, 10, 12));
            root.setPrefHeight(96);
            root.setStyle(border + "-fx-background-color:" + bg + ";-fx-cursor:hand;");
            root.setOnMouseClicked(e -> service.markAsRead(item));
            return root;
        }

        private HBox buildSystemActions(HRNotification item) {
            HBox actions = new HBox(8);
            actions.setAlignment(Pos.CENTER_LEFT);

            if (item.getActionLabel() != null && !item.getActionLabel().isBlank()) {
                MFXButton btn = new MFXButton("< " + item.getActionLabel());
                btn.setStyle(
                        "-fx-font-size:11px;-fx-text-fill:#185FA5;" +
                                "-fx-background-color:transparent;-fx-cursor:hand;-fx-padding:0;"
                );
                btn.setMinHeight(14);
                btn.setOnAction(e -> {
                    e.consume();
                    service.markAsRead(item);
                });
                actions.getChildren().add(btn);
            }
            return actions;
        }

        private HBox buildAttachmentsRow(HRNotification item, double maxWidth) {
            HBox row = new HBox(6);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setMaxWidth(maxWidth);

            for (Attachment att : item.getAttachments()) {
                MFXButton btn = new MFXButton(att.getIcon() + " " + att.getFileName());
                btn.setStyle(
                        "-fx-font-size:10px;-fx-text-fill:#0F6E56;" +
                                "-fx-background-color:#E6F5F1;-fx-background-radius:4px;" +
                                "-fx-cursor:hand;-fx-padding:2 6 2 6;"
                );
                btn.setOnAction(e -> {
                    e.consume();
                    FileOpener.open(att.getFilePath());
                });
                row.getChildren().add(btn);
            }
            return row;
        }

        private void openMessageDetail(HRNotification item) {
            MessageDetailView.show(owner, item);
        }

        private String getSystemIcon(NotificationType type) {
            return switch (type) {
                case EMPLOYEE -> "EMP";
                case SALARY -> "SAL";
                case LEAVE -> "LVE";
                case TRAINING -> "TRN";
                case TASK -> "TSK";
                case SYSTEM -> "SYS";
                case MESSAGE -> "MSG";
            };
        }
    }
}
