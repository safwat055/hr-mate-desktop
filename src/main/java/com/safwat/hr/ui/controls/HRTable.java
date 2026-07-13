package com.safwat.hr.ui.controls;

import com.safwat.hr.ui.style.Elevation;
import com.safwat.hr.ui.style.Radius;
import com.safwat.hr.ui.style.Theme;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;

/**
 * HRTable — Material-style TableView decorator.
 *
 * Usage:
 * <pre>
 *   HRTable.apply(employeesTable);
 *   HRTable.striped(employeesTable);    // zebra rows
 *   HRTable.compact(employeesTable);    // smaller row height
 * </pre>
 */
public final class HRTable {

    public static <T> void apply(TableView<T> table) {
        table.setStyle(
            "-fx-background-color: " + Theme.SURFACE + ";" +
            "-fx-background-radius: " + Radius.LG + ";" +
            "-fx-border-color: " + Theme.DIVIDER + ";" +
            "-fx-border-radius: " + Radius.LG + ";" +
            "-fx-border-width: 1;" +
            "-fx-effect: " + Elevation.E1 + ";" +
            "-fx-font-family: '" + Theme.FONT_FAMILY + "';" +
            "-fx-font-size: " + Theme.FONT_SIZE_MD + "px;"
        );
        applyRowHover(table, false);
    }

    public static <T> void striped(TableView<T> table) {
        apply(table);
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
                        "-fx-background-color: " + hexWithOpacity(Theme.PRIMARY, 0.08) + ";"
                    ));
                    setOnMouseExited(e -> setStyle(base));
                }
            }
        });
    }

    public static <T> void compact(TableView<T> table) {
        apply(table);
        table.setFixedCellSize(32);
    }

    private static <T> void applyRowHover(TableView<T> table, boolean striped) {
        if (!striped) {
            table.setRowFactory(tv -> {
                TableRow<T> row = new TableRow<>();
                row.setOnMouseEntered(e -> {
                    if (!row.isEmpty())
                        row.setStyle("-fx-background-color: " + hexWithOpacity(Theme.PRIMARY, 0.08) + ";");
                });
                row.setOnMouseExited(e -> row.setStyle(""));
                return row;
            });
        }
    }

    private static String hexWithOpacity(String hex, double alpha) {
        javafx.scene.paint.Color c = javafx.scene.paint.Color.web(hex);
        return String.format("rgba(%d,%d,%d,%.2f)",
            (int)(c.getRed()   * 255),
            (int)(c.getGreen() * 255),
            (int)(c.getBlue()  * 255),
            alpha);
    }

    private HRTable() {}
}
