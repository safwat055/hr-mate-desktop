package com.safwat.hr.modernUi.ui.controls;


import com.safwat.hr.modernUi.ui.style.Theme;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.enums.ButtonType;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;

/**
 * ─────────────────────────────────────────────────────────────
 * HRButton — Facade فوق MFXButton.
 * <p>
 * الـ FXML يحتوي على Button عادي (لأن SceneBuilder لا يدعم
 * MFXButton في بعض الإعدادات). عند التهيئة نستبدله بـ MFXButton
 * ونحتفظ بنفس الـ id والـ onAction والـ text.
 * <p>
 * الاستخدام:
 * <pre>
 *    // في initialize()
 *    HRButton.primary(saveBtn);
 *    HRButton.flat(cancelBtn, printBtn, backBtn);
 *    HRButton.outlined(searchBtn);
 *    HRButton.danger(deleteBtn);
 *    HRButton.success(approveBtn);
 *  </pre>
 * <p>
 * لتغيير المكتبة مستقبلاً: غيّر هذا الملف فقط.
 * ─────────────────────────────────────────────────────────────
 */
public final class HRButton {

    // ── Public API ───────────────────────────────────────────

    private HRButton() {
    }

    /**
     * زر رئيسي مملوء — Save, Submit, Confirm
     */
    public static void primary(Button... buttons) {
        for (Button btn : buttons) {
            MFXButton mfx = toMFX(btn);
            mfx.setButtonType(ButtonType.RAISED);
            applyColor(mfx, Theme.PRIMARY, Theme.ON_PRIMARY);
        }
    }

    /**
     * زر Flat/Text شفاف — Cancel, Back
     */
    public static void flat(Button... buttons) {
        for (Button btn : buttons) {
            MFXButton mfx = toMFX(btn);
            mfx.setButtonType(ButtonType.FLAT);
            applyColor(mfx, "transparent", Theme.PRIMARY);
            mfx.setStyle(mfx.getStyle() +
                    "-mfx-ripple-color: " + hexToRgba(Theme.PRIMARY, 0.12) + ";");
        }
    }

    /**
     * زر Flat/Text شفاف — Cancel, Back
     */
    public static void raised(Button... buttons) {
        for (Button btn : buttons) {
            MFXButton mfx = toMFX(btn);
            mfx.setButtonType(ButtonType.RAISED);
            applyColor(mfx, "transparent", Theme.PRIMARY);
            mfx.setStyle(mfx.getStyle() +
                    "-mfx-ripple-color: " + hexToRgba(Theme.PRIMARY, 0.12) + ";");
        }
    }

    /**
     * زر محدد الحدود — Search, Filter
     */
    public static void outlined(Button... buttons) {
        for (Button btn : buttons) {
            MFXButton mfx = toMFX(btn);
            mfx.setButtonType(ButtonType.FLAT);
            applyColor(mfx, "transparent", Theme.PRIMARY);
            mfx.setStyle(mfx.getStyle() +
                    "-fx-border-color: " + Theme.PRIMARY + ";" +
                    "-fx-border-radius: 6px;" +
                    "-fx-border-width: 1.5;");
        }
    }

    /**
     * زر خطر أحمر — Delete, Remove
     */
    public static void danger(Button... buttons) {
        for (Button btn : buttons) {
            MFXButton mfx = toMFX(btn);
            mfx.setButtonType(ButtonType.RAISED);
            applyColor(mfx, Theme.ERROR, Theme.ON_PRIMARY);
        }
    }

    // ── Core replacement logic ────────────────────────────────

    /**
     * زر نجاح أخضر — Approve, Confirm
     */
    public static void success(Button... buttons) {
        for (Button btn : buttons) {
            MFXButton mfx = toMFX(btn);
            mfx.setButtonType(ButtonType.RAISED);
            applyColor(mfx, Theme.SUCCESS, Theme.ON_PRIMARY);
        }
    }

    // ── Helpers ──────────────────────────────────────────────

    /**
     * يستبدل الـ JavaFX Button بـ MFXButton في نفس موضعه داخل الـ Parent.
     * يحتفظ بـ: text, id, onAction, userData, disable, visible, prefSize.
     */
    public static MFXButton toMFX(Button original) {
        // إذا كان أصلاً MFXButton (استدعاء مكرر) نعيده مباشرة
        if (original instanceof MFXButton mfx) return mfx;

        MFXButton mfx = new MFXButton(original.getText());

        // نقل الخصائص
        mfx.setId(original.getId());
        mfx.setOnAction(original.getOnAction());
        mfx.setUserData(original.getUserData());
        mfx.setDisable(original.isDisable());
        mfx.setVisible(original.isVisible());
        mfx.setMnemonicParsing(false);
        mfx.setAlignment(Pos.CENTER);

        if (original.getPrefWidth() > 0) mfx.setPrefWidth(original.getPrefWidth());
        if (original.getPrefHeight() > 0) mfx.setPrefHeight(original.getPrefHeight());
        if (original.getMaxWidth() > 0) mfx.setMaxWidth(original.getMaxWidth());
        if (original.getMaxHeight() > 0) mfx.setMaxHeight(original.getMaxHeight());

        // استبدال في الـ Parent
        if (original.getParent() instanceof Pane parent) {
            int index = parent.getChildren().indexOf(original);
            parent.getChildren().set(index, mfx);
        }

        return mfx;
    }

    private static void applyColor(MFXButton mfx, String bg, String fg) {
        mfx.setStyle(
                "-fx-background-color: " + bg + ";" +
                        "-fx-text-fill: " + fg + ";" +
                        "-fx-font-family: '" + Theme.FONT_FAMILY + "';" +
                        "-fx-font-size: " + Theme.FONT_MD + "px;" +
                        "-fx-background-radius: 6px;" +
                        "-fx-padding: 8 20 8 20;"
        );
    }

    static String hexToRgba(String hex, double alpha) {
        javafx.scene.paint.Color c = javafx.scene.paint.Color.web(hex);
        return String.format("rgba(%d,%d,%d,%.2f)",
                (int) (c.getRed() * 255),
                (int) (c.getGreen() * 255),
                (int) (c.getBlue() * 255),
                alpha);
    }
}
