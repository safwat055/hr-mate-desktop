package com.safwat.hr.notification.ui;

import com.safwat.hr.notification.model.HRNotification;
import io.github.palexdev.materialfx.controls.MFXButton;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * =====================================================================
 * HRToast
 * =====================================================================
 * إشعار فوري منبثق يظهر في الزاوية السفلية اليمنى للشاشة ويختفي تلقائياً.
 * يدعم تراكم عدد محدد من الإشعارات في وقت واحد.
 * يعرض نوعين من الإشعارات: إشعارات النظام، ورسائل المستخدمين.
 * <p>
 * الاستخدام:
 * HRToast.show(primaryStage, notification);
 */
public class HRToast {

    private static final int TOAST_WIDTH = 370;
    private static final int DISPLAY_SECS = 5;
    private static final int MAX_VISIBLE = 2;
    private static final double BOTTOM_MARGIN = 24;
    private static final double RIGHT_MARGIN = 24;
    private static final double TOAST_HEIGHT = 82;
    private static final double GAP = 10;

    private static final Deque<Popup> activeToasts = new ArrayDeque<>();

    /**
     * عرض إشعار منبثق جديد.
     * إذا وصل عدد الإشعارات المعروضة للحد الأقصى، يتم إخفاء الأقدم.
     *
     * @param owner النافذة الأم التي يظهر الإشعار فوقها
     * @param n     كائن الإشعار المراد عرضه
     */
    public static void show(Stage owner, HRNotification n) {
        if (activeToasts.size() >= MAX_VISIBLE) {
            Popup oldest = activeToasts.pollFirst();
            if (oldest != null) oldest.hide();
        }

        Popup popup = n.isMessage() ? buildMessageToast(n) : buildSystemToast(n);
        popup.show(owner);
        activeToasts.addLast(popup);
        repositionAll(owner);
        animateIn(popup);
    }

    /**
     * بناء واجهة إشعار النظام.
     * تحتوي على أيقونة النوع، العنوان، الرسالة، والوقت.
     *
     * @param n كائن إشعار النظام
     * @return Popup جاهز للعرض
     */
    private static Popup buildSystemToast(HRNotification n) {
        Popup popup = new Popup();
        popup.setAutoHide(false);

        String color = n.getType().color;
        String bg = n.getType().bgColor;

        Rectangle iconBg = new Rectangle(36, 36);
        iconBg.setArcWidth(8);
        iconBg.setArcHeight(8);
        iconBg.setFill(Color.web(bg));
        Label iconLbl = new Label(getSystemIcon(n.getType()));
        iconLbl.setStyle("-fx-font-size:10px;-fx-font-weight:700;-fx-text-fill:" + color + ";");
        iconLbl.setMinSize(36, 36);
        iconLbl.setMaxSize(36, 36);
        iconLbl.setAlignment(Pos.CENTER);
        StackPane iconBox = new StackPane(iconBg, iconLbl);
        iconBox.setMinSize(36, 36);
        iconBox.setMaxSize(36, 36);

        Label titleLbl = new Label(n.getTitle());
        titleLbl.setStyle("-fx-font-size:13px;-fx-font-weight:700;-fx-text-fill:#1A1A1A;");
        titleLbl.setMaxWidth(240);

        Label msgLbl = new Label(n.getMessage());
        msgLbl.setStyle("-fx-font-size:11px;-fx-text-fill:#555555;");
        msgLbl.setMaxWidth(240);

        Label timeLbl = new Label(n.getFormattedTime());
        timeLbl.setStyle("-fx-font-size:10px;-fx-text-fill:#AAAAAA;");

        VBox texts = new VBox(2, titleLbl, msgLbl, timeLbl);
        texts.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(texts, Priority.ALWAYS);

        HBox root = new HBox(10, iconBox, texts, buildCloseBtn(popup));
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new Insets(12, 14, 10, 12));
        root.setPrefWidth(TOAST_WIDTH);
        root.setMinHeight(TOAST_HEIGHT);
        root.setStyle(
                "-fx-background-color:#FFFFFF;" +
                        "-fx-background-radius:10px;" +
                        "-fx-border-color:" + color + ";" +
                        "-fx-border-width:0 0 0 4px;" +
                        "-fx-border-radius:10px 0 0 10px;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.15),12,0,0,4);"
        );

        VBox wrapper = new VBox(0, root, buildProgressBar(color));
        popup.getContent().add(wrapper);
        return popup;
    }

    /**
     * بناء واجهة رسالة المستخدم.
     * تحتوي على صورة رمزية للمرسل، الاسم، الموضوع، المعاينة، والمرفقات.
     *
     * @param n كائن رسالة المستخدم
     * @return Popup جاهز للعرض
     */
    private static Popup buildMessageToast(HRNotification n) {
        Popup popup = new Popup();
        popup.setAutoHide(false);

        Circle avatarCircle = new Circle(18);
        avatarCircle.setFill(Color.web("#0F6E56"));
        Label avatarLbl = new Label(n.getAvatarInitials());
        avatarLbl.setStyle("-fx-font-size:11px;-fx-font-weight:700;-fx-text-fill:white;");
        StackPane avatarBox = new StackPane(avatarCircle, avatarLbl);
        avatarBox.setMinSize(36, 36);
        avatarBox.setMaxSize(36, 36);

        Label senderLbl = new Label(
                n.getSenderName() != null ? n.getSenderName() : "رسالة جديدة");
        senderLbl.setStyle("-fx-font-size:13px;-fx-font-weight:700;-fx-text-fill:#0F6E56;");

        Label subjectLbl = new Label(n.getTitle());
        subjectLbl.setStyle("-fx-font-size:12px;-fx-font-weight:500;-fx-text-fill:#1A1A1A;");
        subjectLbl.setMaxWidth(220);

        Label previewLbl = new Label(n.getMessage());
        previewLbl.setStyle("-fx-font-size:11px;-fx-text-fill:#666666;");
        previewLbl.setMaxWidth(220);

        HBox extras = new HBox(6);
        if (n.hasAttachments()) {
            Label attLbl = new Label("[" + n.getAttachments().size() + " مرفق]");
            attLbl.setStyle("-fx-font-size:10px;-fx-text-fill:#0F6E56;");
            extras.getChildren().add(attLbl);
        }

        VBox texts = new VBox(2, senderLbl, subjectLbl, previewLbl, extras);
        texts.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(texts, Priority.ALWAYS);

        HBox root = new HBox(10, avatarBox, texts, buildCloseBtn(popup));
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new Insets(12, 14, 10, 12));
        root.setPrefWidth(TOAST_WIDTH);
        root.setMinHeight(TOAST_HEIGHT);
        root.setStyle(
                "-fx-background-color:#FFFFFF;" +
                        "-fx-background-radius:10px;" +
                        "-fx-border-color:#0F6E56;" +
                        "-fx-border-width:0 0 0 4px;" +
                        "-fx-border-radius:10px 0 0 10px;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.15),12,0,0,4);"
        );

        VBox wrapper = new VBox(0, root, buildProgressBar("#0F6E56"));
        popup.getContent().add(wrapper);
        return popup;
    }

    /**
     * إنشاء زر إغلاق للإشعار.
     *
     * @param popup النافذة المنبثقة المرتبطة بالزر
     * @return زر الإغلاق
     */
    private static MFXButton buildCloseBtn(Popup popup) {
        MFXButton btn = new MFXButton("x");
        btn.setStyle(
                "-fx-background-color:transparent;-fx-text-fill:#AAAAAA;" +
                        "-fx-font-size:13px;-fx-cursor:hand;"
        );
        btn.setPrefSize(24, 24);
        btn.setOnAction(e -> dismissToast(popup));
        return btn;
    }

    /**
     * إنشاء شريط تقدم يتقلص تلقائياً حتى انتهاء مدة عرض الإشعار.
     *
     * @param color لون الشريط
     * @return مستطيل الشريط
     */
    private static Rectangle buildProgressBar(String color) {
        Rectangle bar = new Rectangle(TOAST_WIDTH, 3);
        bar.setFill(Color.web(color));
        bar.setOpacity(0.35);
        bar.setArcWidth(3);
        bar.setArcHeight(3);

        ScaleTransition scale = new ScaleTransition(Duration.seconds(DISPLAY_SECS), bar);
        scale.setFromX(1.0);
        scale.setToX(0.0);
        scale.play();
        return bar;
    }

    /**
     * تشغيل حركة دخول الإشعار (انزلاق + تلاشي).
     *
     * @param popup النافذة المنبثقة المراد تحريكها
     */
    private static void animateIn(Popup popup) {
        javafx.scene.Node root = popup.getContent().get(0);
        root.setTranslateX(TOAST_WIDTH + 30);
        root.setOpacity(0);

        TranslateTransition slide = new TranslateTransition(Duration.millis(260), root);
        slide.setToX(0);
        FadeTransition fade = new FadeTransition(Duration.millis(260), root);
        fade.setToValue(1.0);

        ParallelTransition intro = new ParallelTransition(slide, fade);
        intro.setOnFinished(e -> {
            PauseTransition pause = new PauseTransition(Duration.seconds(DISPLAY_SECS));
            pause.setOnFinished(pe -> dismissToast(popup));
            pause.play();
        });
        intro.play();
    }

    /**
     * إخفاء الإشعار مع حركة خروج (تلاشي + انزلاق).
     *
     * @param popup النافذة المنبثقة المراد إخفاؤها
     */
    private static void dismissToast(Popup popup) {
        if (popup.getContent().isEmpty()) {
            popup.hide();
            return;
        }
        javafx.scene.Node root = popup.getContent().get(0);

        FadeTransition fade = new FadeTransition(Duration.millis(180), root);
        fade.setToValue(0);
        TranslateTransition slide = new TranslateTransition(Duration.millis(180), root);
        slide.setToX(TOAST_WIDTH + 30);

        ParallelTransition out = new ParallelTransition(fade, slide);
        out.setOnFinished(e -> {
            popup.hide();
            activeToasts.remove(popup);
        });
        out.play();
    }

    /**
     * إعادة ترتيب الإشعارات المعروضة على الشاشة بعد إضافة إشعار جديد.
     *
     * @param owner النافذة الأم
     */
    private static void repositionAll(Stage owner) {
        double screenX = owner.getX() + owner.getWidth() - TOAST_WIDTH - RIGHT_MARGIN;
        double screenY = owner.getY() + owner.getHeight();
        int i = 0;
        for (Popup p : activeToasts) {
            double y = screenY - BOTTOM_MARGIN - (TOAST_HEIGHT + GAP) * (i + 1);
            p.setX(screenX);
            p.setY(y);
            i++;
        }
    }

    /**
     * ترجع النص المختصر لأيقونة نوع الإشعار.
     *
     * @param type نوع الإشعار
     * @return النص المختصر (مثل EMP, SAL, MSG)
     */
    private static String getSystemIcon(HRNotification.NotificationType type) {
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