package com.safwat.hr.notification;


import com.safwat.hr.notification.model.HRNotification;
import com.safwat.hr.notification.service.BackgroundServiceSimulator;
import com.safwat.hr.notification.service.NotificationService;
import com.safwat.hr.notification.ui.HRNotificationBell;
import com.safwat.hr.notification.ui.HRToast;
import io.github.palexdev.materialfx.controls.MFXButton;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Stage;

/**
 * ===================================================
 * HRNotificationDemo - تجميع النظام الكامل
 * ===================================================
 * <p>
 * طريقة الاستخدام في تطبيقك:
 * <p>
 * 1. أضف HRNotificationBell في الـ Toolbar:
 * toolbar.getChildren().add(new HRNotificationBell());
 * <p>
 * 2. أرسل إشعاراً من أي خدمة:
 * NotificationService.getInstance().send(
 * HRNotification.builder()
 * .type(NotificationType.SALARY)
 * .title("صرف الرواتب")
 * .message("تم صرف رواتب يناير")
 * .file("/reports/jan.pdf")
 * .build()
 * );
 * <p>
 * 3. اشترك في الأحداث من أي مكون:
 * HREventBus.getInstance().subscribe(
 * NotificationType.LEAVE,
 * n -> refreshLeaveTable()
 * );
 */
public class HRNotificationDemo extends Application {

    private final NotificationService notifService = NotificationService.getInstance();
    private final BackgroundServiceSimulator simulator = new BackgroundServiceSimulator();
    private Stage primaryStage;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

        // الشاشة الرئيسية
        BorderPane root = new BorderPane();
        root.setTop(buildToolbar());
        root.setCenter(buildMainContent());
        root.setStyle("-fx-background-color: #F5F5F5;");

        Scene scene = new Scene(root, 1100, 700);
        stage.setScene(scene);
        stage.setTitle("نظام إدارة الموارد البشرية - HR System");
        stage.show();

        // الاستماع للإشعارات الجديدة وعرض Toast
        notifService.getAll().addListener(
                (javafx.collections.ListChangeListener<HRNotification>) change -> {
                    while (change.next()) {
                        if (change.wasAdded()) {
                            change.getAddedSubList().forEach(n ->
                                    HRToast.show(stage, n)
                            );
                        }
                    }
                }
        );

        // تشغيل محاكي الخدمات الخلفية
        simulator.start();
    }

    // ===================== الـ Toolbar =====================
    private HBox buildToolbar() {
        HBox toolbar = new HBox();
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(0, 20, 0, 20));
        toolbar.setPrefHeight(56);
        toolbar.setStyle(
                "-fx-background-color: #FFFFFF;" +
                        "-fx-border-color: transparent transparent #E8E8E8 transparent;" +
                        "-fx-border-width: 0 0 1 0;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0, 0, 2);"
        );

        // شعار / اسم التطبيق
        Label logo = new Label("HR System");
        logo.setStyle("-fx-font-size:18px;-fx-font-weight:700;-fx-text-fill:#185FA5;");

        // قائمة التنقل
        HBox nav = new HBox(4);
        nav.setAlignment(Pos.CENTER);
        String[] navItems = {"الرئيسية", "الموظفون", "الرواتب", "الإجازات", "التدريب", "الأداء"};
        for (String item : navItems) {
            Label navBtn = new Label(item);
            navBtn.setStyle(
                    "-fx-font-size:13px;-fx-text-fill:#555555;-fx-cursor:hand;" +
                            "-fx-padding:6 12 6 12;-fx-background-radius:6px;"
            );
            navBtn.setOnMouseEntered(e ->
                    navBtn.setStyle(navBtn.getStyle() + "-fx-background-color:#F0F4FF;-fx-text-fill:#185FA5;"));
            navBtn.setOnMouseExited(e ->
                    navBtn.setStyle("-fx-font-size:13px;-fx-text-fill:#555555;-fx-cursor:hand;" +
                            "-fx-padding:6 12 6 12;-fx-background-radius:6px;"));
            nav.getChildren().add(navBtn);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // زر الجرس
        HRNotificationBell bell = new HRNotificationBell();

        // صورة المستخدم
        Label avatar = new Label("م ع");
        avatar.setStyle(
                "-fx-background-color:#185FA5;-fx-text-fill:white;" +
                        "-fx-font-size:12px;-fx-font-weight:700;" +
                        "-fx-background-radius:20px;-fx-padding:8 10 8 10;-fx-cursor:hand;"
        );

        toolbar.getChildren().addAll(logo, nav, spacer, bell, avatar);
        HBox.setMargin(bell, new Insets(0, 8, 0, 8));
        HBox.setMargin(nav, new Insets(0, 0, 0, 24));
        return toolbar;
    }

    // ===================== المحتوى الرئيسي =====================
    private VBox buildMainContent() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(28));
        content.setAlignment(Pos.TOP_LEFT);

        Label heading = new Label("لوحة اختبار الإشعارات");
        heading.setStyle("-fx-font-size:20px;-fx-font-weight:700;-fx-text-fill:#1A1A1A;");

        Label sub = new Label("اضغط على الأزرار لإرسال إشعارات تجريبية، أو انتظر الإشعارات التلقائية من محاكي الخدمات.");
        sub.setStyle("-fx-font-size:13px;-fx-text-fill:#666666;");
        sub.setWrapText(true);

        // أزرار الاختبار
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);

        addTestButton(grid, 0, 0, "👤 إشعار موظف جديد", HRNotification.NotificationType.EMPLOYEE, HRNotification.Priority.HIGH,
                "تعيين: ليلى عبدالله", "مهندسة تصميم - قسم المنتج - تاريخ البدء: 20 يناير",
                "employee/profile/leila", null);

        addTestButton(grid, 1, 0, "💰 صرف رواتب", HRNotification.NotificationType.SALARY, HRNotification.Priority.HIGH,
                "رواتب فبراير 2026", "تم تحويل رواتب 142 موظف بنجاح",
                "salary/report/feb2026", "/reports/salary_feb2026.pdf");

        addTestButton(grid, 0, 1, "📅 طلب إجازة", HRNotification.NotificationType.LEAVE, HRNotification.Priority.HIGH,
                "طلب إجازة يحتاج موافقة", "فاطمة سعيد - 5 أيام - 1 فبراير إلى 6 فبراير",
                "leave/request/fatima", null);

        addTestButton(grid, 1, 1, "🎓 اكتمال دورة", HRNotification.NotificationType.TRAINING, HRNotification.Priority.NORMAL,
                "دورة Excel المتقدم", "أكمل 8 موظفين الدورة بنجاح - الشهادات جاهزة",
                "training/certs/excel", "/certificates/excel_advanced.zip");

        addTestButton(grid, 0, 2, "✅ مهمة متأخرة", HRNotification.NotificationType.TASK, HRNotification.Priority.URGENT,
                "مراجعة الكشوفات الشهرية", "7 كشوفات تحتاج مراجعة فورية - متأخرة 2 يوم",
                "task/review/payslips", null);

        addTestButton(grid, 1, 2, "⚙️ إشعار نظام", HRNotification.NotificationType.SYSTEM, HRNotification.Priority.LOW,
                "نسخ احتياطي مكتمل", "تم حفظ نسخة احتياطية كاملة من قاعدة البيانات",
                null, "/backup/db_backup_2026.sql");

        // عداد إحصائي
        HBox stats = buildStatsBar();

        content.getChildren().addAll(heading, sub, grid, stats);
        return content;
    }

    private void addTestButton(GridPane grid, int col, int row,
                               String btnLabel,
                               HRNotification.NotificationType type, HRNotification.Priority priority,
                               String title, String message,
                               String actionTarget, String filePath) {
        MFXButton btn = new MFXButton(btnLabel);
        btn.setStyle(
                "-fx-background-color:#FFFFFF;-fx-text-fill:#333333;" +
                        "-fx-font-size:13px;-fx-border-color:#E0E0E0;-fx-border-width:0.5px;" +
                        "-fx-border-radius:8px;-fx-background-radius:8px;" +
                        "-fx-padding:10 18 10 18;-fx-cursor:hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 4, 0, 0, 1);"
        );
        btn.setPrefWidth(260);
        btn.setOnAction(e -> {
            HRNotification.Builder builder = HRNotification.builder()
                    .type(type).priority(priority)
                    .title(title).message(message);
            if (actionTarget != null) builder.action("عرض التفاصيل", actionTarget);
            if (filePath != null) builder.file(filePath);
            notifService.send(builder.build());
        });
        grid.add(btn, col, row);
    }

    private HBox buildStatsBar() {
        HBox bar = new HBox(16);
        bar.setPadding(new Insets(16));
        bar.setStyle("-fx-background-color:#FFFFFF;-fx-background-radius:10px;" +
                "-fx-border-color:#E8E8E8;-fx-border-width:0.5px;-fx-border-radius:10px;");

        String[][] stats = {
                {"إجمالي الإشعارات", "service.getAll().size()"},
                {"غير المقروءة", "service.getUnreadCount()"}
        };

        // إحصاء حقيقي مرتبط بـ Properties
        VBox totalBox = statCard("إجمالي الإشعارات", "0", "#185FA5");
        VBox unreadBox = statCard("غير المقروءة", "0", "#A32D2D");

        // ربط القيم
        notifService.getAll().addListener((javafx.collections.ListChangeListener<HRNotification>) c -> {
            ((Label) ((VBox) totalBox).getChildren().get(1)).setText(
                    String.valueOf(notifService.getAll().size()));
        });
        notifService.unreadCountProperty().addListener((obs, o, nw) -> {
            ((Label) ((VBox) unreadBox).getChildren().get(1)).setText(nw.toString());
        });

        Label infoLbl = new Label("الإشعارات تأتي تلقائياً كل 12 ثانية من محاكي الخدمات");
        infoLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#AAAAAA;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        bar.getChildren().addAll(totalBox, unreadBox, spacer, infoLbl);
        return bar;
    }

    private VBox statCard(String label, String value, String color) {
        Label valLbl = new Label(value);
        valLbl.setStyle("-fx-font-size:22px;-fx-font-weight:700;-fx-text-fill:" + color + ";");
        Label lblLbl = new Label(label);
        lblLbl.setStyle("-fx-font-size:11px;-fx-text-fill:#888888;");
        VBox box = new VBox(2, lblLbl, valLbl);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(8, 16, 8, 16));
        box.setStyle("-fx-background-color:#F8F8F8;-fx-background-radius:8px;");
        return box;
    }

    @Override
    public void stop() {
        simulator.stop();
    }
}
