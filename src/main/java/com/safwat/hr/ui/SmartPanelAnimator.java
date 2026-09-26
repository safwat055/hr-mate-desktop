package com.safwat.hr.ui;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

/**
 * كلاس ذكي لإدارة animations إظهار وإخفاء الـ Panel مع محتواها
 *
 * @author safwat055
 * @version 1.0
 * <p>
 * المميزات: - إدارة تلقائية لـ animations العرض والمحتوى - دعم أي نوع من الـ
 * Panels (AnchorPane, VBox, HBox, etc.) - تحكم كامل في توقيت وسرعة الـ
 * animations - سهولة الاستخدام بثلاث باراميترات فقط
 * <p>
 * مثال استخدام: SmartPanelAnimator animator = new SmartPanelAnimator(panel,
 * contentPane, 150); animator.setExpandedWidth(200);
 * animator.setCollapsedWidth(15);
 */
public class SmartPanelAnimator {

    private final Pane panel;              // الـ Panel الرئيسي (اللي هيتحرك)
    private final Pane contentPane;        // الـ Panel اللي جواه العناصر
    private final int animationDuration;   // مدة الـ animation بالمللي ثانية

    private double expandedWidth = 200.00;  // العرض عند التمدد
    private double collapsedWidth = 60.00;  // العرض عند الانكماش

    private boolean isPanelVisible = false; // حالة الـ Panel الحالية

    // الـ Timelines للـ animations
    private Timeline showPanelAnimation;
    private Timeline hidePanelAnimation;
    private Timeline showContentAnimation;
    private Timeline hideContentAnimation;

    /**
     * constructor الأساسي - يأخذ ثلاثة باراميترات فقط
     *
     * @param panel             الـ Panel الرئيسي اللي عايز تعمل له animation
     * @param contentPane       الـ Panel اللي جواه العناصر اللي عايز تظهرها وتخفيها
     * @param animationDuration مدة الـ animation بالمللي ثانية (يفضل بين
     *                          150-300)
     */
    public SmartPanelAnimator(Pane panel, Pane contentPane, int animationDuration) {
        this.panel = panel;
        this.contentPane = contentPane;
        this.animationDuration = animationDuration;

        // تهيئة الـ animations
        initializeAnimations();

        // تعيين الحالة الابتدائية - panel منكمش ومحتوى مخفي
        setInitialState();
    }

    /**
     * تهيئة جميع الـ animations المطلوبة
     */
    private void initializeAnimations() {
        setupPanelAnimations();
        setupContentAnimations();
    }

    /**
     * إعداد animations حركة الـ Panel (التوسع والانكماش)
     */
    private void setupPanelAnimations() {
        // Animation لتوسيع الـ Panel
        showPanelAnimation = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(panel.prefWidthProperty(), collapsedWidth),
                        new KeyValue(panel.minWidthProperty(), collapsedWidth),
                        new KeyValue(panel.maxWidthProperty(), collapsedWidth)
                ),
                new KeyFrame(Duration.millis(animationDuration),
                        new KeyValue(panel.prefWidthProperty(), expandedWidth),
                        new KeyValue(panel.minWidthProperty(), expandedWidth),
                        new KeyValue(panel.maxWidthProperty(), expandedWidth)
                )
        );

        // Animation لانكماش الـ Panel
        hidePanelAnimation = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(panel.prefWidthProperty(), expandedWidth),
                        new KeyValue(panel.minWidthProperty(), expandedWidth),
                        new KeyValue(panel.maxWidthProperty(), expandedWidth)
                ),
                new KeyFrame(Duration.millis(animationDuration),
                        new KeyValue(panel.prefWidthProperty(), collapsedWidth),
                        new KeyValue(panel.minWidthProperty(), collapsedWidth),
                        new KeyValue(panel.maxWidthProperty(), collapsedWidth)
                )
        );

        // event عند انتهاء animation التوسع
        showPanelAnimation.setOnFinished(e -> {
            isPanelVisible = true;
            onPanelExpanded();
        });

        // event عند انتهاء animation الانكماش
        hidePanelAnimation.setOnFinished(e -> {
            isPanelVisible = false;
            onPanelCollapsed();
        });
    }

    /**
     * إعداد animations إظهار وإخفاء المحتوى الداخلي
     */
    private void setupContentAnimations() {
        // Animation لإظهار المحتوى
        showContentAnimation = new Timeline();
        // Animation لإخفاء المحتوى
        hideContentAnimation = new Timeline();

        // بناء الـ key frames للمحتوى
        buildContentKeyFrames();

        // event عند انتهاء animation إخفاء المحتوى
        hideContentAnimation.setOnFinished(e -> {
            setContentVisibility(false);
        });
    }

    /**
     * بناء الـ key frames لجميع العناصر في المحتوى
     */
    private void buildContentKeyFrames() {
        // الحصول على جميع العناصر داخل الـ contentPane
        var children = contentPane.getChildren();

        if (children.isEmpty()) {

            return;
        }

        // إعداد animation الإظهار
        KeyFrame showStart = createOpacityKeyFrame(Duration.ZERO, children, 0.0);
        KeyFrame showEnd = createOpacityKeyFrame(Duration.millis(animationDuration / 2), children, 1.0);
        showContentAnimation.getKeyFrames().addAll(showStart, showEnd);

        // إعداد animation الإخفاء
        KeyFrame hideStart = createOpacityKeyFrame(Duration.ZERO, children, 1.0);
        KeyFrame hideEnd = createOpacityKeyFrame(Duration.millis(animationDuration / 2), children, 0.0);
        hideContentAnimation.getKeyFrames().addAll(hideStart, hideEnd);
    }

    /**
     * إنشاء KeyFrame لتعيين opacity لجميع العناصر
     */
    private KeyFrame createOpacityKeyFrame(Duration duration, javafx.collections.ObservableList<Node> nodes, double opacity) {
        KeyValue[] keyValues = new KeyValue[nodes.size()];

        for (int i = 0; i < nodes.size(); i++) {
            keyValues[i] = new KeyValue(nodes.get(i).opacityProperty(), opacity);
        }

        return new KeyFrame(duration, keyValues);
    }

    /**
     * تعيين الحالة الابتدائية للـ Panel والمحتوى
     */
    private void setInitialState() {
        // تعيين أبعاد الـ Panel الابتدائية
        panel.setPrefWidth(collapsedWidth);
        panel.setMinWidth(collapsedWidth);
        panel.setMaxWidth(collapsedWidth);

        // إخفاء المحتوى في البداية
        setContentVisibility(false);
        setContentOpacity(0.0);

        isPanelVisible = false;
    }

    /**
     * تعيين إظهار/إخفاء جميع عناصر المحتوى
     */
    private void setContentVisibility(boolean visible) {
        for (Node node : contentPane.getChildren()) {
            node.setVisible(visible);
        }
    }

    /**
     * تعيين opacity لجميع عناصر المحتوى
     */
    private void setContentOpacity(double opacity) {
        for (Node node : contentPane.getChildren()) {
            node.setOpacity(opacity);
        }
    }

    /**
     * event يتم استدعاؤه عند توسيع الـ Panel بالكامل
     */
    private void onPanelExpanded() {
        System.out.println("✅ الـ Panel تم توسيعه بالكامل");
        // يمكنك إضافة أي logic إضافي هنا
    }

    /**
     * event يتم استدعاؤه عند انكماش الـ Panel بالكامل
     */
    private void onPanelCollapsed() {
        System.out.println("✅ الـ Panel تم انكماشه بالكامل");
        // يمكنك إضافة أي logic إضافي هنا
    }

    // ================== PUBLIC METHODS ================== //

    /**
     * إظهار الـ Panel والمحتوى (للاستخدام في onMouseEntered)
     */
    public void showPanel() {
        if (!isPanelVisible) {
            // إيقاف أي animations جارية
            stopRunningAnimations();

            // إظهار المحتوى أولاً
            setContentVisibility(true);

            // تشغيل animations التوسع والإظهار
            showPanelAnimation.play();
            showContentAnimation.play();
        }
    }

    /**
     * إخفاء الـ Panel والمحتوى (للاستخدام في onMouseExited)
     */
    public void hidePanel() {
        if (isPanelVisible) {
            // إيقاف أي animations جارية
            stopRunningAnimations();

            // تشغيل animations الانكماش والإخفاء
            hidePanelAnimation.play();
            hideContentAnimation.play();
        }
    }

    /**
     * إيقاف جميع الـ animations الجارية
     */
    private void stopRunningAnimations() {
        if (showPanelAnimation.getStatus() == Animation.Status.RUNNING) {
            showPanelAnimation.stop();
        }
        if (hidePanelAnimation.getStatus() == Animation.Status.RUNNING) {
            hidePanelAnimation.stop();
        }
        if (showContentAnimation.getStatus() == Animation.Status.RUNNING) {
            showContentAnimation.stop();
        }
        if (hideContentAnimation.getStatus() == Animation.Status.RUNNING) {
            hideContentAnimation.stop();
        }
    }

    /**
     * تبديل حالة الـ Panel (إظهار/إخفاء)
     */
    public void togglePanel() {
        if (isPanelVisible) {
            hidePanel();
        } else {
            showPanel();
        }
    }

    // ================== SETTERS & GETTERS ================== //

    /**
     * تعيين العرض عند التمدد
     */
    public void setExpandedWidth(double expandedWidth) {
        this.expandedWidth = expandedWidth;
    }

    /**
     * تعيين العرض عند الانكماش
     */
    public void setCollapsedWidth(double collapsedWidth) {
        this.collapsedWidth = collapsedWidth;
        // تحديث الحالة الحالية إذا كان الـ Panel منكمش
        if (!isPanelVisible) {
            panel.setPrefWidth(collapsedWidth);
            panel.setMinWidth(collapsedWidth);
            panel.setMaxWidth(collapsedWidth);
        }
    }

    /**
     * الحصول على حالة الـ Panel الحالية
     */
    public boolean isPanelVisible() {
        return isPanelVisible;
    }

    /**
     * تعطيل/تمكين الـ Panel
     */
    public void setPanelDisabled(boolean disabled) {
        panel.setDisable(disabled);
        panel.setVisible(!disabled);
    }

    /**
     * إعادة تعيين الـ Panel للحالة الابتدائية
     */
    public void reset() {
        stopRunningAnimations();
        setInitialState();
    }
}