package com.safwat.hr.modernUi.ui.controls;


import com.safwat.hr.modernUi.ui.style.Theme;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;

/**
 * ─────────────────────────────────────────────────────────────
 * HRTextField — Facade فوق MFXTextField.
 * <p>
 * يستبدل JavaFX TextField بـ MFXTextField مع الـ floating label
 * الخاص بـ Material Design.
 * <p>
 * الاستخدام:
 * <pre>
 *    MFXTextField mfxName = HRTextField.apply(nameField, "الاسم");
 *    MFXTextField mfxEmail = HRTextField.apply(emailField, "البريد الإلكتروني");
 *
 *    // تحديد حالة خطأ
 *    HRTextField.error(mfxName, "هذا الحقل مطلوب");
 *
 *    // إزالة الخطأ
 *    HRTextField.clearError(mfxName);
 *  </pre>
 * ─────────────────────────────────────────────────────────────
 */
public final class HRTextField {

    private HRTextField() {
    }

    /**
     * يستبدل TextField بـ MFXTextField مع floating label.
     *
     * @param original   الحقل الأصلي من FXML
     * @param floatLabel النص الذي يطفو فوق الحقل عند الكتابة
     * @return الـ MFXTextField الجديد (استخدمه للوصول للقيمة لاحقاً)
     */
    public static MFXTextField apply(TextField original, String floatLabel) {
        if (original instanceof MFXTextField mfx) return mfx;

        MFXTextField mfx = new MFXTextField();
        mfx.setFloatingText(floatLabel);
        mfx.setText(original.getText());
        mfx.setId(original.getId());
        mfx.setPromptText(original.getPromptText());
        mfx.setEditable(original.isEditable());
        mfx.setDisable(original.isDisable());
        mfx.setVisible(original.isVisible());

        if (original.getPrefWidth() > 0) mfx.setPrefWidth(original.getPrefWidth());
        if (original.getPrefHeight() > 0) mfx.setPrefHeight(original.getPrefHeight());
        if (original.getMaxWidth() > 0) mfx.setMaxWidth(original.getMaxWidth());

        applyBaseStyle(mfx);
        replaceInParent(original, mfx);
        return mfx;
    }

    /**
     * تطبيق الستايل فقط بدون استبدال — لـ MFXTextField موجودة بالفعل في FXML.
     */
    public static void style(MFXTextField... fields) {
        for (MFXTextField f : fields) applyBaseStyle(f);
    }

    /**
     * وضع الحقل في حالة خطأ (حدود حمراء + رسالة)
     */
    public static void error(MFXTextField field, String message) {
        field.setStyle(field.getStyle() +
                "-mfx-line-color: " + Theme.ERROR + ";" +
                "-mfx-unfocus-line-color: " + Theme.ERROR_LIGHT + ";");
        field.setFloatingTextGap(2);
        field.getProperties().put("hr-error", true);
        // يمكن ربطه بـ Label للرسالة لاحقاً
    }

    // ── Internal ─────────────────────────────────────────────

    /**
     * إزالة حالة الخطأ
     */
    public static void clearError(MFXTextField field) {
        field.getProperties().remove("hr-error");
        applyBaseStyle(field);
    }

    private static void applyBaseStyle(MFXTextField field) {
        field.setStyle(
                "-mfx-line-color: " + Theme.PRIMARY + ";" +
                        "-mfx-unfocus-line-color: " + Theme.DIVIDER + ";" +
                        "-fx-font-family: '" + Theme.FONT_FAMILY + "';" +
                        "-fx-font-size: " + Theme.FONT_MD + "px;"
        );
    }

    private static void replaceInParent(TextField original, MFXTextField mfx) {
        if (original.getParent() instanceof Pane parent) {
            int index = parent.getChildren().indexOf(original);
            parent.getChildren().set(index, mfx);
        }
    }
}
