package com.safwat.hr.modernUi.ui.util;


import com.safwat.hr.modernUi.ui.animation.Shake;
import com.safwat.hr.modernUi.ui.controls.HRTextField;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.validation.Constraint;

/**
 * ─────────────────────────────────────────────────────────────
 * Validation — تحقق من حقول MFXTextField.
 * <p>
 * الاستخدام:
 * <pre>
 *    // تحقق من الحقول الإلزامية
 *    boolean valid = Validation.require(mfxName, mfxPhone);
 *
 *    // تحقق من البريد الإلكتروني
 *    boolean emailOk = Validation.email(mfxEmail);
 *
 *    // مسح كل الأخطاء
 *    Validation.clearAll(mfxName, mfxEmail, mfxPhone);
 *  </pre>
 * ─────────────────────────────────────────────────────────────
 */
public final class Validation {

    private Validation() {
    }

    /**
     * يتحقق أن الحقول غير فارغة.
     * يضع حد أحمر + اهتزاز على الحقول الفارغة.
     *
     * @return true إذا كانت كل الحقول صحيحة
     */
    public static boolean require(MFXTextField... fields) {
        boolean allValid = true;
        for (MFXTextField field : fields) {
            if (field.getText() == null || field.getText().isBlank()) {
                HRTextField.error(field, "هذا الحقل مطلوب");
                Shake.play(field);
                allValid = false;
            } else {
                HRTextField.clearError(field);
            }
        }
        return allValid;
    }

    /**
     * تحقق من صيغة البريد الإلكتروني
     */
    public static boolean email(MFXTextField field) {
        String text = field.getText();
        boolean valid = text != null &&
                text.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$");
        if (!valid) {
            HRTextField.error(field, "بريد إلكتروني غير صحيح");
            Shake.play(field);
        } else {
            HRTextField.clearError(field);
        }
        return valid;
    }

    /**
     * تحقق من أن الحقل يحتوي أرقاماً فقط
     */
    public static boolean numeric(MFXTextField field) {
        String text = field.getText();
        boolean valid = text != null && text.matches("\\d+(\\.\\d+)?");
        if (!valid) {
            HRTextField.error(field, "يُسمح بالأرقام فقط");
            Shake.play(field);
        } else {
            HRTextField.clearError(field);
        }
        return valid;
    }

    /**
     * ربط MFXConstraint من MaterialFX مع الحقل.
     * يوفر تحقق تلقائي reactive عند تغيير النص.
     *
     * <pre>
     *   Validation.bind(nameField, "الاسم مطلوب",
     *       Constraint.of(Severity.ERROR, nameField.textProperty(),
     *           Bindings.createBooleanBinding(
     *               () -> !nameField.getText().isBlank(),
     *               nameField.textProperty())));
     * </pre>
     */
    public static void bind(MFXTextField field, Constraint constraint) {
        field.getValidator().constraint(constraint);
        field.getValidator().validProperty().addListener((obs, wasValid, isValid) -> {
            if (isValid) {
                HRTextField.clearError(field);
            }
        });
    }

    /**
     * مسح حالة الخطأ من جميع الحقول
     */
    public static void clearAll(MFXTextField... fields) {
        for (MFXTextField f : fields) HRTextField.clearError(f);
    }
}
