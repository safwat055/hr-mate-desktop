package com.safwat.hr.shared.ui;

import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.security.SecureRandom;

/**
 * حوار تأكيد العمليات الخطيرة — آمن على JavaFX 25 مع RTL.
 * <p>لا يستخدم wrapText نهائياً لتجنب bug PrismTextLayout.
 */
public class DangerConfirmDialog {

    private static final SecureRandom RANDOM = new SecureRandom();

    public static boolean show(String title, String message, String action) {
        String code = generateCode();

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UTILITY);
        dialog.setTitle("تأكيد " + action);
        dialog.setResizable(false);

        // ── الجذر — ارتفاع ثابت بالكامل ──
        VBox root = new VBox(12);
        root.setAlignment(Pos.CENTER_RIGHT);
        root.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        root.setPadding(new Insets(20, 24, 20, 24));
        root.setPrefWidth(440);
        root.setPrefHeight(360);   // ← ثابت — مفيش حساب تلقائي
        root.setMinHeight(360);
        root.setMaxHeight(360);
        root.setStyle(
                "-fx-background-color: #ffffff;" +
                        "-fx-font-family: 'Segoe UI', Tahoma, Arial, sans-serif;" +
                        "-fx-font-size: 13px;"
        );

        // العنوان
        Label lblTitle = new Label("⚠  " + title);
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        lblTitle.setTextFill(Color.web("#dc2626"));
        lblTitle.setPrefHeight(30);
        lblTitle.setMaxWidth(400);
        lblTitle.setAlignment(Pos.CENTER_RIGHT);

        // ✅ الرسالة — Label بدون wrapText + ارتفاع ثابت
        // النص بيتقسم بـ \n يدوياً
        Label lblMessage = new Label(formatMessage(message));
        lblMessage.setFont(Font.font("Segoe UI", 12));
        lblMessage.setTextFill(Color.web("#475569"));
        lblMessage.setPrefHeight(60);     // ← ثابت
        lblMessage.setMaxHeight(60);
        lblMessage.setMaxWidth(400);
        lblMessage.setAlignment(Pos.TOP_RIGHT);

        // التعليم
        Label lblInstruction = new Label("اكتب الرقم التالي للتأكيد:");
        lblInstruction.setFont(Font.font("Segoe UI", 11));
        lblInstruction.setTextFill(Color.web("#64748b"));
        lblInstruction.setPrefHeight(22);

        // الرمز العشوائي
        Label lblCode = new Label(code);
        lblCode.setFont(Font.font("Consolas", FontWeight.BOLD, 26));
        lblCode.setTextFill(Color.web("#1e293b"));
        lblCode.setStyle(
                "-fx-background-color: #f1f5f9;" +
                        "-fx-padding: 10 20;" +
                        "-fx-background-radius: 6px;" +
                        "-fx-border-color: #e2e8f0;" +
                        "-fx-border-radius: 6px;"
        );
        lblCode.setPrefHeight(50);
        lblCode.setMaxWidth(400);
        lblCode.setAlignment(Pos.CENTER);

        // حقل الإدخال
        TextField txtInput = new TextField();
        txtInput.setPromptText("أدخل الرقم هنا");
        txtInput.setAlignment(Pos.CENTER);
        txtInput.setPrefHeight(44);
        txtInput.setMaxWidth(400);
        txtInput.setFont(Font.font("Consolas", 16));
        txtInput.setStyle(
                "-fx-border-color: #cbd5e1;" +
                        "-fx-border-radius: 6px;" +
                        "-fx-background-radius: 6px;" +
                        "-fx-padding: 8;" +
                        "-fx-text-fill: #1e293b;"
        );

        // رسالة الخطأ
        Label lblError = new Label("الرقم غير صحيح");
        lblError.setFont(Font.font(12));
        lblError.setTextFill(Color.web("#dc2626"));
        lblError.setPrefHeight(20);
        lblError.setVisible(false);

        // الأزرار
        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER_LEFT);
        buttons.setPrefHeight(44);

        Button btnCancel = new Button("إلغاء");
        btnCancel.setPrefHeight(36);
        btnCancel.setStyle(
                "-fx-background-color: #f1f5f9; -fx-text-fill: #475569;" +
                        "-fx-font-weight: bold; -fx-padding: 0 22;" +
                        "-fx-background-radius: 6px; -fx-border-color: #cbd5e1;" +
                        "-fx-border-radius: 6px; -fx-cursor: hand;"
        );
        btnCancel.setOnAction(_ -> dialog.close());

        Button btnConfirm = new Button("تأكيد " + action);
        btnConfirm.setPrefHeight(36);
        btnConfirm.setStyle(
                "-fx-background-color: #dc2626; -fx-text-fill: #ffffff;" +
                        "-fx-font-weight: bold; -fx-padding: 0 22;" +
                        "-fx-background-radius: 6px; -fx-cursor: hand;"
        );
        btnConfirm.setDisable(true);

        buttons.getChildren().addAll(btnCancel, btnConfirm);

        root.getChildren().addAll(lblTitle, lblMessage, lblInstruction, lblCode, txtInput, lblError, buttons);

        // ── المنطق ──
        final boolean[] confirmed = {false};

        txtInput.textProperty().addListener((obs, old, newVal) -> {
            lblError.setVisible(false);
            boolean match = newVal != null && newVal.equals(code);
            btnConfirm.setDisable(!match);
            if (match) {
                txtInput.setStyle(
                        "-fx-border-color: #22c55e; -fx-border-radius: 6px;" +
                                "-fx-background-radius: 6px; -fx-padding: 8;" +
                                "-fx-text-fill: #15803d; -fx-background-color: #f0fdf4;"
                );
            } else {
                txtInput.setStyle(
                        "-fx-border-color: #cbd5e1; -fx-border-radius: 6px;" +
                                "-fx-background-radius: 6px; -fx-padding: 8;" +
                                "-fx-text-fill: #1e293b;"
                );
            }
        });

        btnConfirm.setOnAction(_ -> {
            if (txtInput.getText() != null && txtInput.getText().equals(code)) {
                confirmed[0] = true;
                dialog.close();
            } else {
                lblError.setVisible(true);
                txtInput.selectAll();
            }
        });

        txtInput.setOnAction(_ -> {
            if (!btnConfirm.isDisabled()) btnConfirm.fire();
        });

        Scene scene = new Scene(root);
        dialog.setScene(scene);
        dialog.showAndWait();

        return confirmed[0];
    }

    // يقسم النص الطويل لسطور بـ \n (بدون wrapText)
    private static String formatMessage(String msg) {
        if (msg == null) return "";
        if (msg.length() <= 45) return msg;

        StringBuilder sb = new StringBuilder();
        int start = 0;
        while (start < msg.length()) {
            int end = Math.min(start + 45, msg.length());
            // حاول تقطع عند مسافة
            if (end < msg.length()) {
                int space = msg.lastIndexOf(' ', end);
                if (space > start) end = space;
            }
            sb.append(msg, start, end).append('\n');
            start = end;
            while (start < msg.length() && msg.charAt(start) == ' ') start++;
        }
        return sb.toString().trim();
    }

    private static String generateCode() {
        return String.valueOf(100000 + RANDOM.nextInt(900000));
    }
}