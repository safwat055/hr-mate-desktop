package com.safwat.hr.ui.controls;

import com.safwat.hr.ui.style.Elevation;
import com.safwat.hr.ui.style.Radius;
import com.safwat.hr.ui.style.Theme;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.Optional;

/**
 * HRDialog — Material-style dialog factory.
 *
 * Usage:
 * <pre>
 *   // Confirm dialog
 *   boolean confirmed = HRDialog.confirm("حذف الموظف", "هل أنت متأكد من حذف هذا الموظف؟");
 *
 *   // Message dialog
 *   HRDialog.message("نجاح", "تم حفظ البيانات بنجاح");
 *
 *   // Input dialog
 *   Optional<String> value = HRDialog.input("ملاحظة", "أدخل ملاحظاتك:");
 * </pre>
 */
public final class HRDialog {

    // ─── Confirm ─────────────────────────────────────────────────────

    public static boolean confirm(String title, String message) {
        Dialog<ButtonType> dialog = buildDialog(title);

        Label msg = new Label(message);
        msg.setStyle(
            "-fx-font-size: " + Theme.FONT_SIZE_MD + "px;" +
            "-fx-text-fill: " + Theme.ON_SURFACE + ";" +
            "-fx-wrap-text: true;"
        );
        msg.setWrapText(true);
        msg.setMaxWidth(380);

        Button cancelBtn = new Button("إلغاء");
        Button confirmBtn = new Button("تأكيد");
        HRButton.flat(false,cancelBtn);
        HRButton.primary(confirmBtn);

        HBox actions = new HBox(8, cancelBtn, confirmBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox body = new VBox(16, msg, actions);
        body.setPadding(new Insets(20, 24, 20, 24));
        dialog.getDialogPane().setContent(body);
        dialog.getDialogPane().getButtonTypes().clear();

        cancelBtn.setOnAction(e  -> dialog.setResult(ButtonType.CANCEL));
        confirmBtn.setOnAction(e -> dialog.setResult(ButtonType.OK));

        Optional<ButtonType> result = dialog.showAndWait();
        return result.map(r -> r == ButtonType.OK).orElse(false);
    }

    // ─── Message ─────────────────────────────────────────────────────

    public static void message(String title, String message) {
        Dialog<ButtonType> dialog = buildDialog(title);

        Label msg = new Label(message);
        msg.setStyle(
            "-fx-font-size: " + Theme.FONT_SIZE_MD + "px;" +
            "-fx-text-fill: " + Theme.ON_SURFACE + ";"
        );
        msg.setWrapText(true);
        msg.setMaxWidth(380);

        Button okBtn = new Button("حسناً");
        HRButton.primary(okBtn);
        HBox actions = new HBox(okBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox body = new VBox(16, msg, actions);
        body.setPadding(new Insets(20, 24, 20, 24));
        dialog.getDialogPane().setContent(body);
        dialog.getDialogPane().getButtonTypes().clear();

        okBtn.setOnAction(e -> dialog.setResult(ButtonType.OK));
        dialog.showAndWait();
    }

    // ─── Input ───────────────────────────────────────────────────────

    public static Optional<String> input(String title, String prompt) {
        Dialog<String> dialog = new Dialog<>();
        styleDialogPane(dialog.getDialogPane(), title);

        Label label = new Label(prompt);
        label.setStyle("-fx-font-size: " + Theme.FONT_SIZE_MD + "px;");

        TextField field = new TextField();
        HRTextField.apply(field);

        Button cancelBtn = new Button("إلغاء");
        Button okBtn = new Button("موافق");
        HRButton.flat(false,cancelBtn);
        HRButton.primary(okBtn);
        HBox actions = new HBox(8, cancelBtn, okBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox body = new VBox(12, label, field, actions);
        body.setPadding(new Insets(20, 24, 20, 24));
        dialog.getDialogPane().setContent(body);
        dialog.getDialogPane().getButtonTypes().clear();

        cancelBtn.setOnAction(e -> { dialog.setResult(null); dialog.close(); });
        okBtn.setOnAction(e -> {
            dialog.setResult(field.getText().trim());
            dialog.close();
        });

        return dialog.showAndWait();
    }

    // ─── Internal helpers ────────────────────────────────────────────

    private static <T> Dialog<T> buildDialog(String title) {
        Dialog<T> dialog = new Dialog<>();
        styleDialogPane(dialog.getDialogPane(), title);
        return dialog;
    }

    private static void styleDialogPane(DialogPane pane, String title) {
        pane.setStyle(
            "-fx-background-color: " + Theme.SURFACE + ";" +
            "-fx-background-radius: " + Radius.XL + ";" +
            "-fx-effect: " + Elevation.E4 + ";" +
            "-fx-font-family: '" + Theme.FONT_FAMILY + "';"
        );
        // Title area
        Label titleLabel = new Label(title);
        titleLabel.setStyle(
            "-fx-font-size: " + Theme.FONT_SIZE_XL + "px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: " + Theme.ON_SURFACE + ";" +
            "-fx-padding: 20 24 0 24;"
        );
        pane.setHeader(titleLabel);
    }

    private HRDialog() {}
}
