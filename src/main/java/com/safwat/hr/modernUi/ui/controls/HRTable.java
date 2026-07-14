package com.safwat.hr.modernUi.ui.controls;


import com.safwat.hr.modernUi.ui.style.Theme;
import io.github.palexdev.materialfx.controls.MFXTableView;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;

/**
 * ─────────────────────────────────────────────────────────────
 * HRTable — Facade لـ TableView / MFXTableView.
 * <p>
 * ملاحظة: MFXTableView يختلف هيكلياً عن JavaFX TableView —
 * يستخدم MFXTableColumn بدلاً من TableColumn. لذلك نوفر
 * خيارين: تطبيق ستايل على TableView العادية بشكل Material،
 * أو قبول MFXTableView مباشرة.
 * <p>
 * الاستخدام:
 * <pre>
 *    // JavaFX TableView عادية (أسهل للـ FXML)
 *    HRTable.apply(employeeTable);
 *    HRTable.striped(employeeTable);
 *
 *    // MFXTableView (إذا بنيت في الكود)
 *    HRTable.applyMFX(mfxTable);
 *  </pre>
 * ─────────────────────────────────────────────────────────────
 */
public final class HRTable {

    // ── JavaFX TableView (من FXML) ───────────────────────────

    private HRTable() {
    }

    /**
     * ستايل Material مع Row hover
     */
    public static <T> void apply(TableView<T> table) {
        applyBaseStyle(table);
        applyHoverRows(table);
    }

    /**
     * صفوف متناوبة الألوان
     */
    public static <T> void striped(TableView<T> table) {
        applyBaseStyle(table);
        table.setRowFactory(tv -> new TableRow<T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else {
                    String base = (getIndex() % 2 == 0)
                            ? "-fx-background-color: " + Theme.SURFACE + ";"
                            : "-fx-background-color: " + Theme.BACKGROUND + ";";
                    setStyle(base);
                    setOnMouseEntered(e -> setStyle(
                            "-fx-background-color: " + rgba(Theme.PRIMARY, 0.08) + ";"));
                    setOnMouseExited(e -> setStyle(base));
                }
            }
        });
    }

    // ── MFXTableView (مبني في الكود) ─────────────────────────

    /**
     * صفوف مضغوطة
     */
    public static <T> void compact(TableView<T> table) {
        apply(table);
        table.setFixedCellSize(30);
    }

    // ── Internal ─────────────────────────────────────────────

    public static <T> void applyMFX(MFXTableView<T> table) {
        table.setStyle(
                "-fx-background-color: " + Theme.SURFACE + ";" +
                        "-mfx-header-background: " + Theme.PRIMARY + ";" +
                        "-mfx-header-text-fill: " + Theme.ON_PRIMARY + ";" +
                        "-fx-font-family: '" + Theme.FONT_FAMILY + "';" +
                        "-fx-font-size: " + Theme.FONT_MD + "px;"
        );
    }

    private static <T> void applyBaseStyle(TableView<T> table) {
        table.setStyle(
                "-fx-background-color: " + Theme.SURFACE + ";" +
                        "-fx-background-radius: 8px;" +
                        "-fx-border-color: " + Theme.DIVIDER + ";" +
                        "-fx-border-radius: 8px;" +
                        "-fx-border-width: 1;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);" +
                        "-fx-font-family: '" + Theme.FONT_FAMILY + "';" +
                        "-fx-font-size: " + Theme.FONT_MD + "px;"
        );
    }

    private static <T> void applyHoverRows(TableView<T> table) {
        table.setRowFactory(tv -> {
            TableRow<T> row = new TableRow<>();
            row.setOnMouseEntered(e -> {
                if (!row.isEmpty())
                    row.setStyle("-fx-background-color: " + rgba(Theme.PRIMARY, 0.08) + ";");
            });
            row.setOnMouseExited(e -> row.setStyle(""));
            return row;
        });
    }

    private static String rgba(String hex, double alpha) {
        javafx.scene.paint.Color c = javafx.scene.paint.Color.web(hex);
        return String.format("rgba(%d,%d,%d,%.2f)",
                (int) (c.getRed() * 255), (int) (c.getGreen() * 255),
                (int) (c.getBlue() * 255), alpha);
    }
}
