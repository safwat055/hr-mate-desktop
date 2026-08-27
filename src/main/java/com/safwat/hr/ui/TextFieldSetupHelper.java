package com.safwat.hr.ui;


import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

/**
 * مساعد عام لإعداد حقول النص (TextField) في JavaFX.
 *
 * <p>يدعم استقبال أكثر من حقل في نفس الاستدعاء (varargs) لتقليل حجم الكود.</p>
 * <ul>
 *   <li><b>تواريخ</b> — يقبل صيغ متعددة → يحول تلقائياً لـ yyyy-MM-dd</li>
 *   <li><b>أعداد صحيحة</b> — أرقام فقط (int / long)</li>
 *   <li><b>أعداد عشرية</b> — أرقام + نقطة عشرية (BigDecimal)</li>
 * </ul>
 */
public class TextFieldSetupHelper {

    // ── صيغ التاريخ المدعومة للإدخال ──
    private static final DateTimeFormatter[] INPUT_DATE_FORMATS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd-MM-yy"),
            DateTimeFormatter.ofPattern("yy-MM-dd"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),


            DateTimeFormatter.ofPattern("d-M-yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
    };

    private static final DateTimeFormatter OUTPUT_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ── Regex patterns ──
    private static final Pattern INTEGER_PATTERN = Pattern.compile("-?\\d*");
    private static final Pattern DECIMAL_PATTERN = Pattern.compile("-?\\d*\\.?\\d*");

    // ═══════════════════════════════════════════════════════════════
    //  1. تواريخ — varargs
    // ═══════════════════════════════════════════════════════════════

    /**
     * يظبط حقول التاريخ (varargs).
     * <p>عند فقدان التركيز: يحول أي صيغة صحيحة لـ yyyy-MM-dd.</p>
     *
     * @param fields حقول التاريخ
     */
    public static void setupDateFields(TextField... fields) {
        for (TextField field : fields) {
            setupDateFieldInternal(field, null);
        }
    }

    /**
     * يظبط حقول التاريخ مع placeholder واحد لكلهم.
     */
    public static void setupDateFields(String placeholder, TextField... fields) {
        for (TextField field : fields) {
            setupDateFieldInternal(field, placeholder);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  2. أعداد صحيحة — varargs
    // ═══════════════════════════════════════════════════════════════

    /**
     * يظبط حقول الأعداد الصحيحة (varargs).
     * <p>يمنع أي حاجة غير الأرقام والإشارة السالبة.</p>
     */
    public static void setupIntegerFields(TextField... fields) {
        for (TextField field : fields) {
            setupIntegerFieldInternal(field, null);
        }
    }

    public static void setupIntegerFields(String placeholder, TextField... fields) {
        for (TextField field : fields) {
            setupIntegerFieldInternal(field, placeholder);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  3. أعداد عشرية — varargs
    // ═══════════════════════════════════════════════════════════════

    /**
     * يظبط حقول الأعداد العشرية (varargs) بدون تحديد scale.
     */
    public static void setupDecimalFields(TextField... fields) {
        for (TextField field : fields) {
            setupDecimalFieldInternal(field, null, -1);
        }
    }

    public static void setupDecimalFields(String placeholder, TextField... fields) {
        for (TextField field : fields) {
            setupDecimalFieldInternal(field, placeholder, -1);
        }
    }

    /**
     * يظبط حقول الأعداد العشرية مع scale محدد.
     *
     * @param scale  عدد الأرقام العشرية (-1 = غير محدود)
     * @param fields الحقول
     */
    public static void setupDecimalFields(int scale, TextField... fields) {
        for (TextField field : fields) {
            setupDecimalFieldInternal(field, null, scale);
        }
    }

    public static void setupDecimalFields(String placeholder, int scale, TextField... fields) {
        for (TextField field : fields) {
            setupDecimalFieldInternal(field, placeholder, scale);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Internal implementations (single field)
    // ═══════════════════════════════════════════════════════════════

    private static void setupDateFieldInternal(TextField field, String placeholder) {
        if (placeholder != null) field.setPromptText(placeholder);

        final String[] lastValidValue = {""};

        field.focusedProperty().addListener((obs, oldFocus, newFocus) -> {
            if (!newFocus) {
                String text = field.getText().trim();
                if (text.isEmpty()) {
                    lastValidValue[0] = "";
                    return;
                }
                LocalDate parsed = parseDateInput(text);
                if (parsed != null) {
                    String formatted = formatDateOutput(parsed);
                    field.setText(formatted);
                    lastValidValue[0] = formatted;
                } else {
                    field.setText(lastValidValue[0]);
                    showDateErrorAlert();
                }
            } else {
                Platform.runLater(field::selectAll);
            }
        });

        field.setOnAction(e -> field.getParent().requestFocus());
    }

    private static void setupIntegerFieldInternal(TextField field, String placeholder) {
        if (placeholder != null) field.setPromptText(placeholder);

        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();
            return INTEGER_PATTERN.matcher(newText).matches() ? change : null;
        };

        field.setTextFormatter(new TextFormatter<>(filter));

        field.focusedProperty().addListener((obs, oldFocus, newFocus) -> {
            if (!newFocus) {
                String text = field.getText().trim();
                if (text.isEmpty() || text.equals("-")) field.setText("");
            } else {
                Platform.runLater(field::selectAll);
            }
        });
    }

    private static void setupDecimalFieldInternal(TextField field, String placeholder, int scale) {
        if (placeholder != null) field.setPromptText(placeholder);

        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();
            if (!DECIMAL_PATTERN.matcher(newText).matches()) return null;
            if (newText.chars().filter(ch -> ch == '.').count() > 1) return null;
            if (scale >= 0) {
                int dotIndex = newText.indexOf('.');
                if (dotIndex != -1 && newText.length() - dotIndex - 1 > scale) return null;
            }
            return change;
        };

        field.setTextFormatter(new TextFormatter<>(filter));

        field.focusedProperty().addListener((obs, oldFocus, newFocus) -> {
            if (!newFocus) {
                String text = field.getText().trim();
                if (text.isEmpty() || text.equals("-") || text.equals(".")) {
                    field.setText("");
                    return;
                }
                if (text.endsWith(".")) {
                    field.setText(text.substring(0, text.length() - 1));
                }
                if (scale >= 0) {
                    try {
                        BigDecimal bd = new BigDecimal(field.getText());
                        field.setText(bd.setScale(scale, RoundingMode.HALF_UP).toPlainString());
                    } catch (NumberFormatException ignored) {
                        field.setText("");
                    }
                }
            } else {
                Platform.runLater(field::selectAll);
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════
    //  Helpers
    // ═══════════════════════════════════════════════════════════════

    public static LocalDate parseDateInput(String text) {
        if (text == null || text.isBlank()) return null;
        text = text.trim();
        for (DateTimeFormatter fmt : INPUT_DATE_FORMATS) {
            try {
                return LocalDate.parse(text, fmt);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    public static String formatDateOutput(LocalDate date) {
        return date != null ? date.format(OUTPUT_DATE_FORMAT) : "";
    }

    private static void showDateErrorAlert() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("تاريخ غير صالح");
            alert.setHeaderText(null);
            alert.setContentText(
                    "الصيغ المقبولة:\n" +
                            "• yyyy-MM-dd    (مثال: 2024-03-15)\n" +
                            "• dd/MM/yyyy    (مثال: 15/03/2024)\n" +
                            "• dd-MM-yyyy    (مثال: 15-03-2024)\n" +
                            "• yyyy/MM/dd    (مثال: 2024/03/15)");
            alert.show();
        });
    }
}