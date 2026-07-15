package com.safwat.hr.ui.util;

import com.safwat.hr.ui.animation.Shake;
import com.safwat.hr.ui.controls.SAFTextField;
import javafx.scene.control.TextField;

/**
 * Validation — form field validation helpers.
 * <p>
 * Usage:
 * <pre>
 *   boolean valid = Validation.require(nameField, emailField);
 *   boolean emailOk = Validation.email(emailField);
 *   Validation.clearAll(nameField, emailField, phoneField);
 * </pre>
 */
public final class Validation {

    private Validation() {
    }

    /**
     * Mark all empty fields as error and shake them. Returns true if all are valid.
     */
    public static boolean require(TextField... fields) {
        boolean allValid = true;
        for (TextField field : fields) {
            if (field.getText() == null || field.getText().isBlank()) {
                SAFTextField.error(field);
                Shake.play(field);
                allValid = false;
            } else {
                SAFTextField.clearError(field);
            }
        }
        return allValid;
    }

    /**
     * Validate email format
     */
    public static boolean email(TextField field) {
        String text = field.getText();
        boolean valid = text != null &&
                text.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$");
        if (!valid) {
            SAFTextField.error(field);
            Shake.play(field);
        } else {
            SAFTextField.clearError(field);
        }
        return valid;
    }

    /**
     * Validate numeric input
     */
    public static boolean numeric(TextField field) {
        String text = field.getText();
        boolean valid = text != null && text.matches("\\d+(\\.\\d+)?");
        if (!valid) {
            SAFTextField.error(field);
            Shake.play(field);
        } else {
            SAFTextField.clearError(field);
        }
        return valid;
    }

    /**
     * Clear error state from all fields
     */
    public static void clearAll(TextField... fields) {
        for (TextField field : fields) {
            SAFTextField.clearError(field);
        }
    }
}
