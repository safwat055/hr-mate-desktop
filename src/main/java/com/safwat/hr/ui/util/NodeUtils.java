package com.safwat.hr.ui.util;

import javafx.scene.Node;
import javafx.scene.layout.Region;

/**
 * NodeUtils — مساعدات التعامل مع الـ Nodes.
 *
 * <pre>
 *   NodeUtils.hide(saveBtn, deleteBtn);
 *   NodeUtils.show(saveBtn);
 *   NodeUtils.disable(fields);
 *   NodeUtils.enable(fields);
 *   NodeUtils.setFullWidth(nameField, emailField);
 * </pre>
 */
public final class NodeUtils {

    private NodeUtils() {
    }

    /**
     * إخفاء Nodes وإزالتها من الـ layout
     */
    public static void hide(Node... nodes) {
        for (Node n : nodes) {
            n.setVisible(false);
            n.setManaged(false);
        }
    }

    /**
     * إظهار Nodes
     */
    public static void show(Node... nodes) {
        for (Node n : nodes) {
            n.setVisible(true);
            n.setManaged(true);
        }
    }

    /**
     * تعطيل Nodes
     */
    public static void disable(Node... nodes) {
        for (Node n : nodes) n.setDisable(true);
    }

    /**
     * تفعيل Nodes
     */
    public static void enable(Node... nodes) {
        for (Node n : nodes) n.setDisable(false);
    }

    /**
     * جعل Region تأخذ العرض الكامل
     */
    public static void setFullWidth(Region... regions) {
        for (Region r : regions) {
            r.setMaxWidth(Double.MAX_VALUE);
        }
    }

    /**
     * إضافة CSS class لـ Node
     */
    public static void addStyle(String cssClass, Node... nodes) {
        for (Node n : nodes) {
            if (!n.getStyleClass().contains(cssClass))
                n.getStyleClass().add(cssClass);
        }
    }

    /**
     * إزالة CSS class من Node
     */
    public static void removeStyle(String cssClass, Node... nodes) {
        for (Node n : nodes) n.getStyleClass().remove(cssClass);
    }
}
