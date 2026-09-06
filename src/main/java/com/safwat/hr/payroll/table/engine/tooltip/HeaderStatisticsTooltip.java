package com.safwat.hr.payroll.table.engine.tooltip;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;

import java.util.ArrayList;
import java.util.List;

/**
 * =====================================================
 * HeaderStatisticsTooltip — تولتيب إحصائي على هيدر الأعمدة
 * =====================================================
 * <p>عند تمرير الماوس على هيدر أي عمود بيعرض إحصائيات سريعة (عدد
 * الصفوف/الفارغ/الأرقام/المجموع/المتوسط). منطق معزول تمامًا عن
 * بناء الأعمدة نفسها — ممكن يتفعّل أو يتعطّل بدون ما يأثر على أي
 * حاجة تانية في الجدول.</p>
 * <p>قابل للتفعيل/التعطيل في أي وقت بعد الإنشاء عبر {@link #setEnabled(boolean)}
 * (مفيش داعي يترتبط بلحظة إنشاء الجدول) — بيسجّل كل زوج (هيدر، تولتيب)
 * أول ما يتعمل، وبعدين setEnabled بس بيعمل Tooltip.install/uninstall
 * على كل الأزواج المسجّلة.</p>
 */
public class HeaderStatisticsTooltip {

    private boolean enabled = true;
    private final List<Binding> bindings = new ArrayList<>();

    private record Binding(Node node, Tooltip tooltip) {
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * يفعّل/يعطّل التولتيب الإحصائي فورًا لكل الأعمدة الحالية (والمستقبلية).
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        for (Binding b : bindings) {
            if (b.node() == null) continue;
            if (enabled) {
                Tooltip.install(b.node(), b.tooltip());
            } else {
                b.tooltip().hide();
                Tooltip.uninstall(b.node(), b.tooltip());
            }
        }
    }

    public void addStatisticalTooltips(TableView<ObservableList<String>> targetTable) {
        if (targetTable == null) {
            return;
        }

        javafx.application.Platform.runLater(() -> {
            try {
                for (TableColumn<ObservableList<String>, ?> column : targetTable.getColumns()) {
                    addTooltipToColumn(column, targetTable);
                }

                targetTable.getColumns().addListener((ListChangeListener<TableColumn<ObservableList<String>, ?>>) change -> {
                    while (change.next()) {
                        if (change.wasAdded()) {
                            for (TableColumn<ObservableList<String>, ?> newColumn : change.getAddedSubList()) {
                                addTooltipToColumn(newColumn, targetTable);
                            }
                        }
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void addTooltipToColumn(TableColumn<ObservableList<String>, ?> column, TableView<ObservableList<String>> table) {
        if (column == null) {
            return;
        }

        Tooltip tooltip = new Tooltip();
        tooltip.setShowDelay(javafx.util.Duration.seconds(0.5));
        tooltip.setHideDelay(javafx.util.Duration.seconds(0.2));

        tooltip.setStyle(
                "-fx-font-size: 16px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-text-fill: #E4E5E6;"
                        + "-fx-background-color: #000000;"
                        + "-fx-border-color: #6D6E70;"
                        + "-fx-border-width: 1px;"
                        + "-fx-border-radius: 5px;"
                        + "-fx-background-radius: 5px;"
                        + "-fx-padding: 10px;"
                        + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 3);"
        );

        tooltip.setMinWidth(150);
        tooltip.setMaxWidth(400);

        if (column.getGraphic() != null) {
            column.getGraphic().setOnMouseEntered(event -> showStatisticalTooltip(column, table, tooltip));
            column.getGraphic().setOnMouseExited(event -> tooltip.hide());
            registerBinding(column.getGraphic(), tooltip);
        } else {
            setupHeaderMouseListeners(column, table, tooltip);
        }
    }

    private void registerBinding(Node node, Tooltip tooltip) {
        bindings.add(new Binding(node, tooltip));
        if (enabled) {
            Tooltip.install(node, tooltip);
        }
    }

    private javafx.scene.Node getColumnHeader(TableColumn<ObservableList<String>, ?> column) {
        try {
            return column.getTableView().lookup(".column-header[data-column=\"" + column.getId() + "\"]");
        } catch (Exception e) {
            return null;
        }
    }

    private void setupHeaderMouseListeners(TableColumn<ObservableList<String>, ?> column,
                                           TableView<ObservableList<String>> table,
                                           Tooltip tooltip) {
        javafx.application.Platform.runLater(() -> {
            try {
                javafx.scene.Node header = getColumnHeader(column);
                if (header != null) {
                    header.setOnMouseEntered(event -> showStatisticalTooltip(column, table, tooltip));
                    header.setOnMouseExited(event -> tooltip.hide());
                    registerBinding(header, tooltip);
                }
            } catch (Exception e) {
                System.err.println("❌ خطأ في إعداد مستمعي الماوس للعمود: " + e.getMessage());
            }
        });
    }

    private String buildTooltipText(StatisticalInfo stats, String columnName) {
        StringBuilder tooltipText = new StringBuilder();

        tooltipText.append("إحصائيات العمود: ").append(columnName).append("\n\n");
        tooltipText.append(" إجمالي الصفوف: ").append(stats.totalCount).append("\n");
        tooltipText.append(" محتويات: ").append(stats.nonEmptyCount).append("\n");
        tooltipText.append(" أرقام: ").append(stats.numericCount).append("\n");
        tooltipText.append(" نصوص: ").append(stats.nonNumericCount).append("\n");
        tooltipText.append(" فارغ: ").append(stats.emptyCount).append("\n");

        if (stats.missingCount > 0) {
            tooltipText.append("❌ مفقود: ").append(stats.missingCount).append("\n");
        }

        if (stats.numericCount > 0) {
            tooltipText.append("\n المجموع: ").append(String.format("%,.2f", stats.sum)).append("\n");
            tooltipText.append(" المتوسط: ").append(String.format("%,.2f", stats.sum / stats.numericCount));
        }

        return tooltipText.toString();
    }

    private void showStatisticalTooltip(TableColumn<ObservableList<String>, ?> column,
                                        TableView<ObservableList<String>> table,
                                        Tooltip tooltip) {
        if (!enabled) {
            tooltip.hide();
            return;
        }
        try {
            StatisticalInfo stats = calculateColumnStatistics(column, table);
            String columnName = (column.getGraphic() instanceof Label lbl)
                    ? lbl.getText()
                    : column.getText();
            tooltip.setText(buildTooltipText(stats, columnName));
        } catch (Exception e) {
            tooltip.setText("❌ خطأ في حساب الإحصائيات");
            System.err.println("❌ خطأ في عرض التولتيب: " + e.getMessage());
        }
    }

    private StatisticalInfo calculateColumnStatistics(TableColumn<ObservableList<String>, ?> column,
                                                      TableView<ObservableList<String>> table) {
        StatisticalInfo stats = new StatisticalInfo();

        if (column == null || table == null || table.getItems().isEmpty()) {
            return stats;
        }

        int columnIndex = table.getColumns().indexOf(column);
        if (columnIndex < 0) {
            return stats;
        }

        for (ObservableList<String> row : table.getItems()) {
            if (row.size() > columnIndex) {
                String cellValue = row.get(columnIndex);

                if (cellValue != null && !cellValue.trim().isEmpty()) {
                    stats.nonEmptyCount++;
                    try {
                        double numericValue = Double.parseDouble(cellValue.trim());
                        stats.sum += numericValue;
                        stats.numericCount++;
                    } catch (NumberFormatException e) {
                        stats.nonNumericCount++;
                    }
                } else {
                    stats.emptyCount++;
                }
            } else {
                stats.missingCount++;
            }
        }

        stats.totalCount = table.getItems().size();
        return stats;
    }

    private static class StatisticalInfo {
        int totalCount = 0;
        int nonEmptyCount = 0;
        int numericCount = 0;
        int nonNumericCount = 0;
        int emptyCount = 0;
        int missingCount = 0;
        double sum = 0.0;
    }
}