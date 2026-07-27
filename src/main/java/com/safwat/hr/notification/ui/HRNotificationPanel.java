package com.safwat.hr.notification.ui;

import com.safwat.hr.model.message.service.MessageClientService;
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
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.util.function.Consumer;

/**
 * =====================================================================
 * HRNotificationPanel
 * =====================================================================
 * لوحة الإشعارات الكاملة التي تظهر عند الضغط على زر الجرس.
 * تعرض إشعارات النظام والرسائل في مكان واحد مع إمكانية التصفية حسب النوع.
 * التبويبات المتاحة:
 * - الكل
 * - إشعارات (مع أنواع فرعية: موظفون، رواتب، إجازات، تدريب، مهام، النظام)
 * - رسائل
 * الاستخدام:
 * HRNotificationPanel panel = new HRNotificationPanel(primaryStage);
 */
public class HRNotificationPanel extends VBox {

    private final NotificationService service = NotificationService.getInstance();
    private final FilteredList<HRNotification> filteredList;
    private final Stage owner;
    private TabFilter activeTab = TabFilter.ALL;
    private Consumer<HRNotification> onOpenMessage;

    /**
     * إنشاء لوحة الإشعارات.
     * تقوم بتهيئة الفلتر، بناء الواجهة، وتطبيق الأنماط.
     *
     * @param owner النافذة الأم التي تظهر اللوحة فوقها
     */
    public HRNotificationPanel(Stage owner) {
        this.owner = owner;
        this.filteredList = new FilteredList<>(service.getAll(), n -> true);
        build();
        setStyle(
                "-fx-background-color:#FFFFFF;" +
                        "-fx-background-radius:12px;" +
                        "-fx-border-color:#E0E0E0;-fx-border-width:0.5px;" +
                        "-fx-border-radius:12px;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.18),20,0,0,6);"
        );
        setPrefWidth(440);
        setMaxHeight(700);
    }

    /**
     * تعيين callback يتم استدعاؤه عند الضغط على "فتح" لرسالة.
     *
     * @param callback الدالة المستدعاة مع الإشعار المختار
     */
    public void setOnOpenMessage(Consumer<HRNotification> callback) {
        this.onOpenMessage = callback;
    }

    /**
     * بناء أقسام اللوحة الأربعة: الرأس، التبويبات، القائمة، التذييل.
     */
    private void build() {
        getChildren().addAll(
                buildHeader(),
                buildTabs(),
                buildList(),
                buildFooter()
        );
    }

    /**
     * بناء رأس اللوحة.
     * يحتوي على عنوان "المركز"، شارة بعدد الإشعارات الجديدة،
     * زر تحديث الرسائل من الخادم، وزر تعليم الكل مقروء.
     *
     * @return HBox يمثل رأس اللوحة
     */
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

        MFXButton refreshBtn = new MFXButton("🔄 تحديث");
        refreshBtn.setStyle(
                "-fx-font-size:14px;-fx-text-fill:#185FA5;" +
                        "-fx-background-color:transparent;-fx-cursor:hand;-fx-padding:0 4 0 4;"
        );
        refreshBtn.setOnAction(e -> {
            MessageClientService.getInstance().refreshAllMessages();
        });
        refreshBtn.setTooltip(new Tooltip("تحديث الرسائل"));

        MFXButton markAllBtn = new MFXButton("تعليم الكل مقروء");
        markAllBtn.setStyle(
                "-fx-font-size:12px;-fx-text-fill:#185FA5;" +
                        "-fx-background-color:transparent;-fx-cursor:hand;"
        );
        markAllBtn.setOnAction(e -> service.markAllAsRead());
        markAllBtn.visibleProperty().bind(service.unreadCountProperty().greaterThan(0));
        markAllBtn.managedProperty().bind(markAllBtn.visibleProperty());

        header.getChildren().addAll(title, spacer, badge, refreshBtn, markAllBtn);
        return header;
    }

    /**
     * بناء منطقة التبويبات.
     * تحتوي على صف رئيسي (الكل، إشعارات، رسائل) وصف فرعي يظهر عند اختيار "إشعارات"
     * ويحتوي على أنواع الإشعارات الفرعية.
     *
     * @return VBox يحتوي على صفوف التبويبات
     */
    private VBox buildTabs() {
        VBox wrapper = new VBox(0);
        wrapper.setStyle(
                "-fx-border-color:transparent transparent #EBEBEB transparent;" +
                        "-fx-border-width:0 0 0.5 0;"
        );

        HBox mainRow = new HBox(6);
        mainRow.setPadding(new Insets(10, 12, 6, 12));
        mainRow.setAlignment(Pos.CENTER_LEFT);

        Label tabAll = buildMainTab("الكل", "#185FA5", "#E6F1FB", TabFilter.ALL, mainRow);
        Label tabSys = buildMainTab("إشعارات", "#5F5E5A", "#F1EFE8", TabFilter.SYSTEM_ALL, mainRow);
        Label tabMsg = buildMainTab("رسائل", "#0F6E56", "#E6F5F1", TabFilter.MESSAGES, mainRow);
        mainRow.getChildren().addAll(tabAll, tabSys, tabMsg);

        HBox subRow = new HBox(4);
        subRow.setPadding(new Insets(0, 12, 8, 12));
        subRow.setAlignment(Pos.CENTER_LEFT);
        subRow.setVisible(false);
        subRow.setManaged(false);

        for (NotificationType type : NotificationType.values()) {
            if (type == NotificationType.MESSAGE) continue;
            TabFilter tf = typeToFilter(type);
            Label sub = buildSubTab(type.label, type.color, type.bgColor, tf, subRow);
            subRow.getChildren().add(sub);
        }

        tabSys.setOnMouseClicked(e -> {
            setActiveMainTab(TabFilter.SYSTEM_ALL, mainRow);
            subRow.setVisible(true);
            subRow.setManaged(true);
        });
        tabAll.setOnMouseClicked(e -> {
            setActiveMainTab(TabFilter.ALL, mainRow);
            subRow.setVisible(false);
            subRow.setManaged(false);
        });
        tabMsg.setOnMouseClicked(e -> {
            setActiveMainTab(TabFilter.MESSAGES, mainRow);
            subRow.setVisible(false);
            subRow.setManaged(false);
        });

        wrapper.getChildren().addAll(mainRow, subRow);
        return wrapper;
    }

    /**
     * إنشاء تبويب رئيسي (الكل، إشعارات، رسائل).
     *
     * @param text      نص التبويب
     * @param color     لون النص عند التفعيل
     * @param bg        لون الخلفية عند التفعيل
     * @param filter    قيمة الفلتر المرتبطة
     * @param container الحاوية التي يُضاف إليها التبويب
     * @return Label يمثل التبويب
     */
    private Label buildMainTab(String text, String color, String bg,
                               TabFilter filter, HBox container) {
        Label tab = new Label(text);
        tab.setUserData(filter);
        tab.getProperties().put("color", color);
        tab.getProperties().put("bg", bg);
        applyMainTabStyle(tab, filter == TabFilter.ALL, color, bg);
        tab.setPrefHeight(28);
        return tab;
    }

    /**
     * إنشاء تبويب فرعي (نوع إشعار محدد).
     *
     * @param text      نص التبويب
     * @param color     لون النص عند التفعيل
     * @param bg        لون الخلفية عند التفعيل
     * @param filter    قيمة الفلتر المرتبطة
     * @param container الحاوية التي يُضاف إليها التبويب
     * @return Label يمثل التبويب الفرعي
     */
    private Label buildSubTab(String text, String color, String bg,
                              TabFilter filter, HBox container) {
        Label tab = new Label(text);
        tab.setUserData(filter);
        tab.getProperties().put("color", color);
        tab.getProperties().put("bg", bg);
        applySubTabStyle(tab, false, color, bg);

        tab.setOnMouseClicked(_ -> {
            activeTab = filter;
            applyFilter();
            container.getChildren().forEach(node -> {
                if (node instanceof Label lbl && lbl.getUserData() instanceof TabFilter tf) {
                    String c = (String) lbl.getProperties().get("color");
                    String b = (String) lbl.getProperties().get("bg");
                    applySubTabStyle(lbl, tf == filter, c, b);
                }
            });
        });
        return tab;
    }

    /**
     * تفعيل تبويب رئيسي محدد وتحديث أنماط التبويبات.
     *
     * @param filter    الفلتر المراد تفعيله
     * @param container الحاوية التي تحتوي على التبويبات
     */
    private void setActiveMainTab(TabFilter filter, HBox container) {
        activeTab = filter;
        applyFilter();
        container.getChildren().forEach(node -> {
            if (node instanceof Label lbl && lbl.getUserData() instanceof TabFilter tf) {
                String c = (String) lbl.getProperties().get("color");
                String b = (String) lbl.getProperties().get("bg");
                applyMainTabStyle(lbl, tf == filter, c, b);
            }
        });
    }

    /**
     * تطبيق النمط البصري على تبويب رئيسي.
     *
     * @param tab    عنصر التبويب
     * @param active true إذا كان التبويب نشطاً
     * @param color  لون النص
     * @param bg     لون الخلفية
     */
    private void applyMainTabStyle(Label tab, boolean active, String color, String bg) {
        if (active)
            tab.setStyle(
                    "-fx-background-color:" + bg + ";-fx-text-fill:" + color + ";" +
                            "-fx-font-size:13px;-fx-font-weight:700;" +
                            "-fx-background-radius:8px;-fx-padding:5 14 5 14;-fx-cursor:hand;"
            );
        else
            tab.setStyle(
                    "-fx-background-color:transparent;-fx-text-fill:#666666;" +
                            "-fx-font-size:13px;-fx-background-radius:8px;" +
                            "-fx-padding:5 14 5 14;-fx-cursor:hand;" +
                            "-fx-border-color:#E0E0E0;-fx-border-width:0.5px;-fx-border-radius:8px;"
            );
    }

    /**
     * تطبيق النمط البصري على تبويب فرعي.
     *
     * @param tab    عنصر التبويب
     * @param active true إذا كان التبويب نشطاً
     * @param color  لون النص
     * @param bg     لون الخلفية
     */
    private void applySubTabStyle(Label tab, boolean active, String color, String bg) {
        if (active)
            tab.setStyle(
                    "-fx-background-color:" + bg + ";-fx-text-fill:" + color + ";" +
                            "-fx-font-size:11px;-fx-font-weight:600;" +
                            "-fx-background-radius:6px;-fx-padding:3 8 3 8;-fx-cursor:hand;"
            );
        else
            tab.setStyle(
                    "-fx-background-color:transparent;-fx-text-fill:#888888;" +
                            "-fx-font-size:11px;-fx-background-radius:6px;" +
                            "-fx-padding:3 8 3 8;-fx-cursor:hand;" +
                            "-fx-border-color:#E8E8E8;-fx-border-width:0.5px;-fx-border-radius:6px;"
            );
    }

    /**
     * تطبيق فلتر التبويب النشط على قائمة الإشعارات.
     * يحدد أي الإشعارات تُعرض حسب التبويب المختار.
     */
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

    /**
     * تحويل NotificationType إلى TabFilter المقابل.
     *
     * @param type نوع الإشعار
     * @return قيمة TabFilter
     */
    private TabFilter typeToFilter(NotificationType type) {
        return switch (type) {
            case EMPLOYEE -> TabFilter.EMPLOYEE;
            case SALARY -> TabFilter.SALARY;
            case LEAVE -> TabFilter.LEAVE;
            case TRAINING -> TabFilter.TRAINING;
            case TASK -> TabFilter.TASK;
            default -> TabFilter.SYSTEM_TYPE;
        };
    }

    /**
     * بناء قائمة الإشعارات مع خلية عرض مخصصة.
     *
     * @return ListView جاهز للعرض
     */
    private ListView<HRNotification> buildList() {
        ListView<HRNotification> list = new ListView<>();
        list.setItems(filteredList);
        list.setPrefHeight(500);
        list.setFixedCellSize(130);
        list.setCellFactory(lv -> new NotificationCell());
        list.setStyle("-fx-background-color:transparent;");
        list.setPlaceholder(buildEmptyState());
        VBox.setVgrow(list, Priority.ALWAYS);
        return list;
    }

    /**
     * بناء حالة الفراغ التي تظهر عند عدم وجود إشعارات.
     *
     * @return StackPane يحتوي على رسالة الفراغ
     */
    private StackPane buildEmptyState() {
        Label msg = new Label("لا توجد إشعارات");
        msg.setStyle("-fx-font-size:14px;-fx-text-fill:#AAAAAA;");
        return new StackPane(msg);
    }

    /**
     * بناء تذييل اللوحة.
     * يحتوي على عدد العناصر المعروضة وزر مسح الكل.
     *
     * @return HBox يمثل التذييل
     */
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
                        () -> filteredList.size() + " عنصر", filteredList)
        );
        countLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#AAAAAA;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        MFXButton clearBtn = new MFXButton("مسح الكل");
        clearBtn.setStyle(
                "-fx-font-size:12px;-fx-text-fill:#AA3333;" +
                        "-fx-background-color:transparent;-fx-cursor:hand;"
        );
        clearBtn.setOnAction(_ -> {
            switch (activeTab) {
                case ALL -> service.clearAll();
                case MESSAGES -> service.getAll().removeIf(HRNotification::isMessage);
                case SYSTEM_ALL -> service.getAll().removeIf(n ->
                        n.getCategory() == NotificationCategory.SYSTEM);
                default -> {
                    NotificationType target = filterToType(activeTab);
                    if (target != null)
                        service.getAll().removeIf(n -> n.getType() == target);
                }
            }
        });

        footer.getChildren().addAll(countLbl, spacer, clearBtn);
        return footer;
    }

    /**
     * تحويل TabFilter إلى NotificationType المقابل.
     *
     * @param f قيمة الفلتر
     * @return نوع الإشعار أو null
     */
    private NotificationType filterToType(TabFilter f) {
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

    /**
     * أنواع الفلاتر المتاحة للتبويبات.
     */
    private enum TabFilter {
        ALL, SYSTEM_ALL, MESSAGES,
        EMPLOYEE, SALARY, LEAVE, TRAINING, TASK, SYSTEM_TYPE
    }

    /**
     * خلية عرض مخصصة للإشعار داخل قائمة الإشعارات.
     * تدعم نوعين من العرض: إشعار النظام ورسالة المستخدم.
     */
    private class NotificationCell extends ListCell<HRNotification> {

        /**
         * تحديث محتوى الخلية عند تغير العنصر.
         *
         * @param item  الإشعار المراد عرضه
         * @param empty true إذا كانت الخلية فارغة
         */
        @Override
        protected void updateItem(HRNotification item, boolean empty) {
            super.updateItem(item, empty);
            setText(null);
            if (empty || item == null) {
                setGraphic(null);
                setStyle("-fx-background-color:transparent;");
                return;
            }
            setGraphic(item.isMessage()
                    ? buildMessageCell(item)
                    : buildSystemCell(item));
        }

        /**
         * بناء خلية إشعار النظام.
         * تحتوي على نقطة القراءة، أيقونة النوع، العنوان، الرسالة، الوقت، والإجراءات.
         *
         * @param item إشعار النظام
         * @return HBox يمثل خلية الإشعار
         */
        private HBox buildSystemCell(HRNotification item) {
            Circle dot = buildDot(item.isRead(), item.getType().color);

            Rectangle iconBg = new Rectangle(38, 38);
            iconBg.setArcWidth(8);
            iconBg.setArcHeight(8);
            iconBg.setFill(Color.web(item.getType().bgColor));
            Label iconLbl = new Label(getSystemIcon(item.getType()));
            iconLbl.setStyle(
                    "-fx-font-size:11px;-fx-font-weight:700;" +
                            "-fx-text-fill:" + item.getType().color + ";"
            );
            iconLbl.setMinSize(38, 38);
            iconLbl.setMaxSize(38, 38);
            iconLbl.setAlignment(Pos.CENTER);
            StackPane iconBox = new StackPane(iconBg, iconLbl);
            iconBox.setMinSize(38, 38);
            iconBox.setMaxSize(38, 38);

            Label titleLbl = new Label(item.getTitle());
            titleLbl.setStyle(
                    "-fx-font-size:13px;-fx-font-weight:" +
                            (item.isRead() ? "400" : "700") + ";-fx-text-fill:#1A1A1A;"
            );
            titleLbl.setMaxWidth(260);
            titleLbl.setMinHeight(16);

            Label msgLbl = new Label(item.getMessage());
            msgLbl.setStyle("-fx-font-size:11px;-fx-text-fill:#666666;");
            msgLbl.setMaxWidth(260);
            msgLbl.setMinHeight(14);

            Label timeLbl = new Label(item.getFormattedTime());
            timeLbl.setStyle("-fx-font-size:10px;-fx-text-fill:#AAAAAA;");

            HBox actionsBox = buildSystemActions(item);
            HBox meta = new HBox(8, timeLbl, actionsBox);
            meta.setAlignment(Pos.CENTER_LEFT);

            VBox texts = new VBox(2, titleLbl, msgLbl, meta);
            if (item.hasAttachments())
                texts.getChildren().add(buildAttachmentsRow(item));
            texts.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(texts, Priority.ALWAYS);

            return buildRoot(item, dot, iconBox, texts);
        }

        /**
         * بناء خلية رسالة المستخدم.
         * تحتوي على نقطة القراءة، الصورة الرمزية، اسم المرسل، الموضوع، المعاينة، والوقت.
         *
         * @param item رسالة المستخدم
         * @return HBox يمثل خلية الرسالة
         */
        private HBox buildMessageCell(HRNotification item) {
            Circle dot = buildDot(item.isRead(), "#0F6E56");

            Circle avatarCircle = new Circle(19);
            avatarCircle.setFill(Color.web("#0F6E56"));
            Label avatarLbl = new Label(item.getAvatarInitials());
            avatarLbl.setStyle("-fx-font-size:12px;-fx-font-weight:700;-fx-text-fill:white;");
            StackPane avatarBox = new StackPane(avatarCircle, avatarLbl);
            avatarBox.setMinSize(38, 38);
            avatarBox.setMaxSize(38, 38);

            Label senderLbl = new Label(
                    item.getSenderName() != null ? item.getSenderName() : "مجهول");
            senderLbl.setStyle(
                    "-fx-font-size:13px;-fx-font-weight:" +
                            (item.isRead() ? "400" : "700") + ";-fx-text-fill:#1A1A1A;"
            );
            Region rowSpacer = new Region();
            HBox.setHgrow(rowSpacer, Priority.ALWAYS);
            Label timeLbl = new Label(item.getFormattedTime());
            timeLbl.setStyle("-fx-font-size:10px;-fx-text-fill:#AAAAAA;");
            HBox topRow = new HBox(4, senderLbl, rowSpacer, timeLbl);
            topRow.setAlignment(Pos.CENTER_LEFT);
            topRow.setMaxWidth(270);

            Label subjectLbl = new Label(item.getTitle());
            subjectLbl.setStyle("-fx-font-size:12px;-fx-font-weight:500;-fx-text-fill:#333333;");
            subjectLbl.setMaxWidth(270);
            subjectLbl.setMinHeight(14);

            Label previewLbl = new Label(item.getMessage());
            previewLbl.setStyle("-fx-font-size:11px;-fx-text-fill:#888888;");
            previewLbl.setMaxWidth(270);
            previewLbl.setMinHeight(13);

            HBox bottomRow = new HBox(6);
            bottomRow.setAlignment(Pos.CENTER_LEFT);
            if (item.hasAttachments()) {
                Label attLbl = new Label("[" + item.getAttachments().size() + " مرفق]");
                attLbl.setStyle("-fx-font-size:10px;-fx-text-fill:#0F6E56;");
                bottomRow.getChildren().add(attLbl);
            }
            Region bSpacer = new Region();
            HBox.setHgrow(bSpacer, Priority.ALWAYS);
            MFXButton openBtn = new MFXButton("فتح >");
            openBtn.setStyle(
                    "-fx-font-size:11px;-fx-text-fill:#0F6E56;" +
                            "-fx-background-color:#E6F5F1;-fx-background-radius:6px;" +
                            "-fx-cursor:hand;-fx-padding:2 8 2 8;"
            );
            openBtn.setOnAction(e -> {
                e.consume();
                service.markAsRead(item);
                if (onOpenMessage != null) {
                    onOpenMessage.accept(item);
                }
            });
            bottomRow.getChildren().addAll(bSpacer, openBtn);

            VBox texts = new VBox(2, topRow, subjectLbl, previewLbl, bottomRow);
            texts.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(texts, Priority.ALWAYS);

            return buildRoot(item, dot, avatarBox, texts);
        }

        /**
         * إنشاء نقطة القراءة (dot).
         * ملونة إذا كانت غير مقروءة، وشفافة مع حافة إذا كانت مقروءة.
         *
         * @param isRead حالة القراءة
         * @param color  لون النقطة عند عدم القراءة
         * @return Circle يمثل نقطة القراءة
         */
        private Circle buildDot(boolean isRead, String color) {
            Circle dot = new Circle(5);
            dot.setFill(isRead ? Color.TRANSPARENT : Color.web(color));
            dot.setStroke(isRead ? Color.web("#CCCCCC") : Color.TRANSPARENT);
            dot.setStrokeWidth(isRead ? 1.5 : 0);
            return dot;
        }

        /**
         * بناء الجذر المشترك لخلية الإشعار.
         * يحدد الخلفية والحدود حسب حالة القراءة والأولوية.
         *
         * @param item  الإشعار
         * @param dot   نقطة القراءة
         * @param icon  أيقونة/صورة رمزية
         * @param texts محتوى النصوص
         * @return HBox الجذر النهائي للخلية
         */
        private HBox buildRoot(HRNotification item, Circle dot,
                               StackPane icon, VBox texts) {
            String border = (item.getPriority() == HRNotification.Priority.URGENT)
                    ? "-fx-border-color:#A32D2D transparent #F2F2F2 transparent;" +
                    "-fx-border-width:0 0 0.5 3;"
                    : "-fx-border-color:transparent transparent #F2F2F2 transparent;" +
                    "-fx-border-width:0 0 0.5 0;";

            String bg = item.isRead()
                    ? "#FFFFFF"
                    : (item.isMessage() ? "#F0FAF7" : "#F8F5FF");

            HBox root = new HBox(10, dot, icon, texts);
            root.setAlignment(Pos.CENTER_LEFT);
            root.setPadding(new Insets(10, 14, 10, 12));
            root.setPrefHeight(130);
            root.setStyle(border + "-fx-background-color:" + bg + ";-fx-cursor:hand;");
            root.setOnMouseClicked(e -> service.markAsRead(item));
            return root;
        }

        /**
         * بناء صف الإجراءات لإشعار النظام.
         * يحتوي على زر الإجراء المرتبط بالإشعار (مثل "فتح" أو "عرض").
         *
         * @param item إشعار النظام
         * @return HBox يحتوي على أزرار الإجراءات
         */
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
                    FileOpener.open(item.getActionTarget());
                });
                actions.getChildren().add(btn);
            }
            return actions;
        }

        /**
         * بناء صف المرفقات لإشعار معين.
         * يعرض كل مرفق كزر قابل للضغط لفتحه.
         *
         * @param item الإشعار الذي يحتوي على المرفقات
         * @return HBox يحتوي على أزرار المرفقات
         */
        private HBox buildAttachmentsRow(HRNotification item) {
            HBox row = new HBox(6);
            row.setAlignment(Pos.CENTER_LEFT);
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

        /**
         * ترجع النص المختصر لأيقونة نوع الإشعار.
         *
         * @param type نوع الإشعار
         * @return النص المختصر (مثل EMP, SAL, MSG)
         */
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