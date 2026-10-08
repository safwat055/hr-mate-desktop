package com.safwat.hr.system.setup;

import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.Pair;

import java.util.Optional;

/**
 * شاشة إدخال بيانات مستخدم PostgreSQL.
 * أي حقل فاضي بياخد القيمة الافتراضية admin.
 * لازم تتنادى من FX thread.
 */
public final class PgCredentialsDialog {

    public static final String DEFAULT_USER = "admin";
    public static final String DEFAULT_PASSWORD = "admin";

    private PgCredentialsDialog() {
    }

    /**
     * @param defaultUser اسم المستخدم المعروض في الحقل (ممكن يكون فاضي)
     * @param title       عنوان الشاشة
     * @param header      النص اللي فوق الحقول
     * @return المستخدم والباسورد، أو empty لو المستخدم ألغى
     */
    public static Optional<Pair<String, String>> ask(String defaultUser, String title, String header) {
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(header);

        ButtonType okType = new ButtonType("متابعة", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);

        TextField user = new TextField(defaultUser == null || defaultUser.isBlank()
                ? DEFAULT_USER : defaultUser.trim());
        user.setPromptText(DEFAULT_USER);

        PasswordField pass = new PasswordField();
        pass.setPromptText(DEFAULT_PASSWORD + " (افتراضي)");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(12));
        grid.addRow(0, new Label("اسم المستخدم:"), user);
        grid.addRow(1, new Label("كلمة المرور:"), pass);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(bt -> {
            if (bt != okType) return null;
            String u = user.getText() == null ? "" : user.getText().trim();
            String p = pass.getText() == null ? "" : pass.getText();
            return new Pair<>(
                    u.isEmpty() ? DEFAULT_USER : u,
                    p.isEmpty() ? DEFAULT_PASSWORD : p);
        });

        return dialog.showAndWait();
    }
}